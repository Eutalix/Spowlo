package com.bobbyesp.spowlo.ui.pages.searcher

import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
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
import com.bobbyesp.spowlo.features.spotify_api.data.paging.ArtistsPagingSource
import com.bobbyesp.spowlo.features.spotify_api.data.paging.SimpleAlbumPagingSource
import com.bobbyesp.spowlo.features.spotify_api.data.paging.SimplePlaylistPagingSource
import com.bobbyesp.spowlo.features.spotify_api.data.paging.TrackPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The ViewModel for the SearcherPage.
 * It manages the search query, active search filter, and provides paginated
 * data flows for different types of Spotify content (tracks, albums, etc.).
 */
@HiltViewModel
class SearcherPageViewModel @Inject constructor() : ViewModel() {

    private var searchJob: Job? = null

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    data class ViewState(
        val query: String = "",
        val searchStatus: ViewSearchState = ViewSearchState.Idle,
        val activeSearchType: SpotifySearchType = SpotifySearchType.TRACK,
        val searchedTracks: Flow<PagingData<Track>> = emptyFlow(),
        val searchedAlbums: Flow<PagingData<SimpleAlbum>> = emptyFlow(),
        val searchedPlaylists: Flow<PagingData<SimplePlaylist>> = emptyFlow(),
        val searchedArtists: Flow<PagingData<Artist>> = emptyFlow(),
    )

    fun onQueryChange(newQuery: String) {
        _viewState.update { it.copy(query = newQuery) }
    }

    fun onSearchTypeChange(searchType: SpotifySearchType) {
        if (viewState.value.activeSearchType == searchType) return
        _viewState.update { it.copy(activeSearchType = searchType) }
        // If there's already a query, re-trigger the search for the new type.
        if (viewState.value.query.isNotBlank()) {
            search()
        }
    }

    fun search() {
        searchJob?.cancel() // Cancel any previous search job.
        _viewState.update { it.copy(searchStatus = ViewSearchState.Loading) }
        
        searchJob = viewModelScope.launch {
            val query = viewState.value.query
            if (query.isBlank()) {
                _viewState.update { it.copy(searchStatus = ViewSearchState.Idle) }
                return@launch
            }
            try {
                when (viewState.value.activeSearchType) {
                    SpotifySearchType.TRACK -> {
                        val pager = createPager { TrackPagingSource(query = query) }.flow.cachedIn(viewModelScope)
                        _viewState.update { it.copy(searchedTracks = pager) }
                    }
                    SpotifySearchType.ALBUM -> {
                        val pager = createPager { SimpleAlbumPagingSource(query = query) }.flow.cachedIn(viewModelScope)
                        _viewState.update { it.copy(searchedAlbums = pager) }
                    }
                    SpotifySearchType.PLAYLIST -> {
                        val pager = createPager { SimplePlaylistPagingSource(query = query) }.flow.cachedIn(viewModelScope)
                        _viewState.update { it.copy(searchedPlaylists = pager) }
                    }
                    SpotifySearchType.ARTIST -> {
                        val pager = createPager { ArtistsPagingSource(query = query) }.flow.cachedIn(viewModelScope)
                        _viewState.update { it.copy(searchedArtists = pager) }
                    }
                }
                _viewState.update { it.copy(searchStatus = ViewSearchState.Success) }
            } catch (e: Exception) {
                _viewState.update { it.copy(searchStatus = ViewSearchState.Error(e.message ?: "Unknown error")) }
            }
        }
    }
    
    private fun <T : Any> createPager(pagingSourceFactory: () -> T): Pager<Int, T> where T : androidx.paging.PagingSource<Int, Any> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false, initialLoadSize = 40),
            pagingSourceFactory = { pagingSourceFactory() as androidx.paging.PagingSource<Int, T> }
        )
    }
}

// --- Enums and Sealed Classes for UI State ---

sealed class ViewSearchState {
    object Idle : ViewSearchState()
    object Loading : ViewSearchState()
    object Success : ViewSearchState()
    data class Error(val message: String) : ViewSearchState()
}

enum class SpotifySearchType {
    TRACK, ALBUM, PLAYLIST, ARTIST;

    fun asString(): String = this.name.lowercase()
    
    @Composable
    fun asLocalizedString(): String {
        return when (this) {
            TRACK -> stringResource(R.string.track)
            ALBUM -> stringResource(R.string.album)
            PLAYLIST -> stringResource(R.string.playlist)
            ARTIST -> stringResource(R.string.artist)
        }
    }
}