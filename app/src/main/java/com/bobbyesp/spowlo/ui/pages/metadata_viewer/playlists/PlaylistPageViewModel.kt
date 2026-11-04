package com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.spowlo.App.Companion.applicationScope
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyWebApi
import com.bobbyesp.spowlo.features.spotify_api.model.SpotifyDataType
import com.bobbyesp.spowlo.utils.DatabaseUtil
import com.bobbyesp.spowlo.utils.SpotifyUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TrackDownload(val url: String, val name: String, val durationMs: Long = 0L)

class PlaylistPageViewModel @Inject constructor() : ViewModel() {

    private val mutableViewStateFlow = MutableStateFlow(ViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()

    val downloadedTrackIds: StateFlow<Set<String>> = DatabaseUtil.getMediaInfo()
        .map { songs -> songs.mapNotNull { SpotifyUrls.trackId(it.songUrl) }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    data class ViewState(
        val id: String = "",
        val state: PlaylistDataState = PlaylistDataState.Loading,
        val isLoadingTracks: Boolean = false,
    )

    suspend fun loadData(
        id: String,
        type: SpotifyDataType = SpotifyDataType.TRACK,
        account: Boolean = false,
    ) {
        mutableViewStateFlow.update {
            ViewState(id = id, state = PlaylistDataState.Loading)
        }

        val result = runCatching {
            withContext(Dispatchers.IO) {
                when (type) {
                    SpotifyDataType.TRACK -> SpotifyApiRequests.provideGetTrackById(id)
                        ?.let { PlaylistDataState.Loaded(it) }

                    SpotifyDataType.ALBUM -> loadAlbum(id)
                    SpotifyDataType.PLAYLIST -> loadPlaylist(id, account)
                    SpotifyDataType.ARTIST -> SpotifyApiRequests.provideGetArtistById(id)
                        ?.let { PlaylistDataState.Loaded(it) }
                } ?: throw Exception("Error while loading data")
            }
        }

        result.onSuccess { state ->
            publish(id, state, isLoadingTracks = false)
        }.onFailure { exception ->
            Log.e(TAG, "Could not load $type $id", exception)
            publish(
                id,
                PlaylistDataState.Error(Exception(exception.message ?: "Error while loading data")),
                isLoadingTracks = false
            )
        }
    }

    private suspend fun loadPlaylist(id: String, account: Boolean): PlaylistDataState.Loaded {
        var complete = SpotifyApiRequests.provideGetPlaylistById(id, account)
            ?: throw Exception("Error while loading playlist")

        publish(id, PlaylistDataState.Loaded(complete), isLoadingTracks = true)

        val tracks = complete.tracks.items.toMutableList()
        var offset = 0
        while (offset < MAX_TRACKS) {
            val page = SpotifyApiRequests.provideGetPlaylistTracksPage(
                id,
                offset = offset,
                account = account
            )
            if (page.items.isEmpty()) break
            tracks.addAll(page.items)
            offset += SpotifyWebApi.PLAYLIST_PAGE_SIZE
            complete = complete.copy(
                tracks = complete.tracks.copy(
                    items = tracks.toList(),
                    offset = 0,
                    limit = SpotifyWebApi.PLAYLIST_PAGE_SIZE,
                    total = page.total,
                )
            )
            publish(id, PlaylistDataState.Loaded(complete), isLoadingTracks = true)
            if (offset >= page.total) break
        }

        return PlaylistDataState.Loaded(complete)
    }

    private suspend fun loadAlbum(id: String): PlaylistDataState.Loaded {
        var complete = SpotifyApiRequests.providesGetAlbumById(id)
            ?: throw Exception("Error while loading album")

        publish(id, PlaylistDataState.Loaded(complete), isLoadingTracks = true)

        val tracks = complete.tracks.items.toMutableList()
        var offset = 0
        while (offset < MAX_TRACKS) {
            val page = SpotifyApiRequests.providesGetAlbumTracksPage(id, offset = offset)
            if (page.items.isEmpty()) break
            tracks.addAll(page.items)
            offset += SpotifyWebApi.ALBUM_PAGE_SIZE
            complete = complete.copy(
                tracks = complete.tracks.copy(
                    items = tracks.toList(),
                    offset = 0,
                    limit = SpotifyWebApi.ALBUM_PAGE_SIZE,
                    total = page.total,
                )
            )
            publish(id, PlaylistDataState.Loaded(complete), isLoadingTracks = true)
            if (offset >= page.total) break
        }

        return PlaylistDataState.Loaded(complete)
    }

    private fun publish(id: String, state: PlaylistDataState, isLoadingTracks: Boolean) {
        mutableViewStateFlow.update {
            if (it.id == id) it.copy(state = state, isLoadingTracks = isLoadingTracks) else it
        }
    }

    fun downloadTrack(url: String, name: String) {
        Downloader.executeParallelDownloadWithUrl(url, name)
    }

    /** Queues missing tracks one after another so a long playlist does not start every process at once. */
    fun downloadTracks(tracks: List<TrackDownload>) {
        if (tracks.isEmpty()) return
        applicationScope.launch(Dispatchers.IO) {
            tracks.forEach { track ->
                Downloader.executeParallelDownloadWithUrl(track.url, track.name).join()
            }
        }
    }

    private companion object {
        const val TAG = "PlaylistPageViewModel"
        const val MAX_TRACKS = 10_000
    }
}
