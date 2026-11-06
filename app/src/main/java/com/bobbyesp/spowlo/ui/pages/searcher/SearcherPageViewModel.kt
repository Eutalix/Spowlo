package com.bobbyesp.spowlo.ui.pages.searcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SimplePlaylist
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.features.spotify_api.data.paging.ArtistsPagingSource
import com.bobbyesp.spowlo.features.spotify_api.data.paging.SimpleAlbumPagingSource
import com.bobbyesp.spowlo.features.spotify_api.data.paging.SimplePlaylistPagingSource
import com.bobbyesp.spowlo.features.spotify_api.data.paging.TrackPagingSource
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SearchTopResult
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyWebApi
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.name
import com.bobbyesp.spowlo.utils.DatabaseUtil
import com.bobbyesp.spowlo.utils.SpotifyUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class SearcherPageViewModel @Inject constructor() : ViewModel() {

    private var searchJob: Job? = null

    private val mutableViewStateFlow = MutableStateFlow(ViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()

    /** Download history keyed by Spotify track ID. */
    val downloadedSongs: Flow<Map<String, DownloadedSongInfo>> = DatabaseUtil.getMediaInfo()
        .map { songs ->
            songs.mapNotNull { song -> SpotifyUrls.trackId(song.songUrl)?.let { it to song } }
                .toMap()
        }

    val downloadedTrackIds = downloadedSongs
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Track IDs per album, for the "3/11" hint.
    private val mutableAlbumTrackIds = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val albumTrackIds = mutableAlbumTrackIds.asStateFlow()
    private val albumLoads = ConcurrentHashMap.newKeySet<String>()

    private val mutableArtistFollowers = MutableStateFlow<Map<String, Long>>(emptyMap())
    val artistFollowers = mutableArtistFollowers.asStateFlow()
    private val followerLoads = ConcurrentHashMap.newKeySet<String>()

    private val mutablePlaylistStats = MutableStateFlow<Map<String, SpotifyWebApi.PlaylistStats>>(emptyMap())
    val playlistStats = mutablePlaylistStats.asStateFlow()
    private val playlistStatLoads = ConcurrentHashMap.newKeySet<String>()

    private val albumLoadSemaphore = Semaphore(3)
    private val artistLoadSemaphore = Semaphore(2)
    private val playlistLoadSemaphore = Semaphore(3)

    data class ViewState(
        val query: String = "",
        val viewState: ViewSearchState = ViewSearchState.Idle,
        val activeSearchType: SpotifySearchType = SpotifySearchType.ALL,
        val searchedTracks: Flow<PagingData<Track>> = emptyFlow(),
        val searchedAlbums: Flow<PagingData<SimpleAlbum>> = emptyFlow(),
        val searchedPlaylists: Flow<PagingData<SimplePlaylist>> = emptyFlow(),
        val searchedArtists: Flow<PagingData<Artist>> = emptyFlow(),
        val topResults: List<SearchTopResult> = emptyList(),
        val totals: Map<SpotifySearchType, Int> = emptyMap(),
    )

    fun selectSearchType(searchType: SpotifySearchType) {
        mutableViewStateFlow.update { it.copy(activeSearchType = searchType) }
    }

    fun updateSearchText(text: String) {
        mutableViewStateFlow.update { it.copy(query = text) }
    }

    fun search(
        searchType: SpotifySearchType = viewStateFlow.value.activeSearchType,
    ) {
        val query = viewStateFlow.value.query.trim()
        if (query.isEmpty()) return

        searchJob?.cancel()
        mutableViewStateFlow.update {
            it.copy(
                activeSearchType = searchType,
                totals = emptyMap(),
                topResults = emptyList(),
            )
        }
        updateViewState(ViewSearchState.Loading)
        searchJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Keep each tab's pager alive so switching tabs doesn't refetch.
                    loadTracks(query)
                    loadAlbums(query)
                    loadPlaylists(query)
                    loadArtists(query)
                    val top = runCatching { SpotifyWebApi.searchTopResults(query) }
                        .getOrDefault(emptyList())
                    mutableViewStateFlow.update {
                        it.copy(topResults = rankTopResults(top, query))
                    }
                }
                updateViewState(ViewSearchState.Success)
            } catch (e: Exception) {
                updateViewState(ViewSearchState.Error(e.localizedMessage ?: "Error"))
            }
        }
    }

    fun ensureAlbumTracksLoaded(albumId: String) {
        if (albumId.isBlank() || mutableAlbumTrackIds.value.containsKey(albumId)) return
        if (!albumLoads.add(albumId)) return
        viewModelScope.launch(Dispatchers.IO) {
            val ids = runCatching {
                albumLoadSemaphore.withPermit {
                    val result = mutableSetOf<String>()
                    var offset = 0
                    while (offset < 500) {
                        val page = SpotifyApiRequests.providesGetAlbumTracksPage(
                            albumId,
                            SpotifyWebApi.ALBUM_PAGE_SIZE,
                            offset,
                        )
                        if (page.items.isEmpty()) break
                        page.items.mapTo(result) { it.id }
                        offset += SpotifyWebApi.ALBUM_PAGE_SIZE
                        if (offset >= page.total) break
                    }
                    result
                }
            }.getOrDefault(emptySet())
            mutableAlbumTrackIds.update { it + (albumId to ids) }
            albumLoads.remove(albumId)
        }
    }

    fun ensureArtistFollowersLoaded(artistId: String) {
        if (artistId.isBlank() || mutableArtistFollowers.value.containsKey(artistId)) return
        if (!followerLoads.add(artistId)) return
        viewModelScope.launch(Dispatchers.IO) {
            val followers = runCatching {
                artistLoadSemaphore.withPermit {
                    SpotifyApiRequests.provideGetArtistById(artistId)?.followers?.total?.toLong()
                }
            }.getOrNull()
            followerLoads.remove(artistId)
            if (followers != null && followers > 0) {
                mutableArtistFollowers.update { it + (artistId to followers) }
            }
        }
    }

    fun ensurePlaylistStatsLoaded(playlistId: String) {
        if (playlistId.isBlank() || mutablePlaylistStats.value.containsKey(playlistId)) return
        if (!playlistStatLoads.add(playlistId)) return
        viewModelScope.launch(Dispatchers.IO) {
            val stats = runCatching {
                playlistLoadSemaphore.withPermit {
                    SpotifyWebApi.getPlaylistStats(playlistId)
                }
            }.getOrNull()
            playlistStatLoads.remove(playlistId)
            if (stats != null) {
                mutablePlaylistStats.update { it + (playlistId to stats) }
            }
        }
    }

    private fun loadTracks(query: String) {
        val pager = Pager(
            config = SEARCH_PAGING_CONFIG,
            pagingSourceFactory = {
                TrackPagingSource(
                    spotifyApi = null,
                    query = query,
                    onTotalLoaded = { total -> storeTotal(SpotifySearchType.TRACK, total) },
                )
            }
        ).flow.cachedIn(viewModelScope)
        mutableViewStateFlow.update { it.copy(searchedTracks = pager) }
    }

    private fun loadAlbums(query: String) {
        val pager = Pager(
            config = SEARCH_PAGING_CONFIG,
            pagingSourceFactory = {
                SimpleAlbumPagingSource(
                    spotifyApi = null,
                    query = query,
                    onTotalLoaded = { total -> storeTotal(SpotifySearchType.ALBUM, total) },
                )
            },
        ).flow.cachedIn(viewModelScope)
        mutableViewStateFlow.update { it.copy(searchedAlbums = pager) }
    }

    private fun loadPlaylists(query: String) {
        val pager = Pager(
            config = SEARCH_PAGING_CONFIG,
            pagingSourceFactory = {
                SimplePlaylistPagingSource(
                    spotifyApi = null,
                    query = query,
                    onTotalLoaded = { total -> storeTotal(SpotifySearchType.PLAYLIST, total) },
                )
            },
        ).flow.cachedIn(viewModelScope)
        mutableViewStateFlow.update { it.copy(searchedPlaylists = pager) }
    }

    private fun loadArtists(query: String) {
        val pager = Pager(
            config = SEARCH_PAGING_CONFIG,
            pagingSourceFactory = {
                ArtistsPagingSource(
                    spotifyApi = null,
                    query = query,
                    onTotalLoaded = { total -> storeTotal(SpotifySearchType.ARTIST, total) },
                )
            },
        ).flow.cachedIn(viewModelScope)
        mutableViewStateFlow.update { it.copy(searchedArtists = pager) }
    }

    private fun storeTotal(type: SpotifySearchType, total: Int) {
        mutableViewStateFlow.update { it.copy(totals = it.totals + (type to total)) }
    }

    private fun updateViewState(searchViewState: ViewSearchState) {
        mutableViewStateFlow.update {
            it.copy(
                viewState = searchViewState,
            )
        }
    }

    // Exact matches first; when the query is an album name, the track loses to the album.
    private fun rankTopResults(
        results: List<SearchTopResult>,
        query: String,
    ): List<SearchTopResult> {
        if (results.isEmpty()) return results
        val exact = results.filter { it.name.equals(query, ignoreCase = true) }
        if (exact.isEmpty()) return results
        val ordered = exact.sortedBy { it is SearchTopResult.TrackItem }
        return ordered + results.filterNot { it in exact }
    }

    private companion object {
        val SEARCH_PAGING_CONFIG = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
            initialLoadSize = 40,
        )
    }
}

sealed class ViewSearchState {
    data object Idle : ViewSearchState()
    data object Loading : ViewSearchState()
    data object Success : ViewSearchState()
    data class Error(val error: String) : ViewSearchState()
}

enum class SpotifySearchType {
    ALL,
    TRACK,
    ALBUM,
    PLAYLIST,
    ARTIST;

    fun asString(): String {
        return when (this) {
            ALL -> "all"
            TRACK -> "track"
            ALBUM -> "album"
            PLAYLIST -> "playlist"
            ARTIST -> "artist"
        }
    }

    fun String.asSpotifySearchType(): SpotifySearchType {
        return when (this) {
            "track" -> TRACK
            "album" -> ALBUM
            "playlist" -> PLAYLIST
            "artist" -> ARTIST
            else -> TRACK
        }
    }

    @Composable
    fun asLocalizedString(): String {
        return when (this) {
            ALL -> stringResource(id = R.string.all)
            ALBUM -> stringResource(id = R.string.albums)
            PLAYLIST -> stringResource(id = R.string.playlists)
            TRACK -> stringResource(id = R.string.tracks)
            ARTIST -> stringResource(id = R.string.artists)
        }
    }
}
