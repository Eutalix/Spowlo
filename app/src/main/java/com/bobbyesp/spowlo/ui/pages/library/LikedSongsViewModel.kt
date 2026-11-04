package com.bobbyesp.spowlo.ui.pages.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.spowlo.App.Companion.applicationScope
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyUserApi
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.UserTrack
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.TrackDownload
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

private const val TAG = "LikedSongsViewModel"

class LikedSongsViewModel : ViewModel() {

    data class State(
        val tracks: List<UserTrack> = emptyList(),
        val total: Int = 0,
        val isLoading: Boolean = true,
        val isLoadingMore: Boolean = false,
        val failed: Boolean = false,
        val endReached: Boolean = false,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    private var loading = false

    val downloadedTrackIds: StateFlow<Set<String>> = DatabaseUtil.getMediaInfo()
        .map { songs -> songs.mapNotNull { SpotifyUrls.trackId(it.songUrl) }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        loadMore()
    }

    fun loadMore() {
        val current = mutableState.value
        if (loading || current.endReached) return
        loading = true
        if (current.tracks.isEmpty()) {
            mutableState.update { it.copy(isLoading = true, failed = false) }
        } else {
            mutableState.update { it.copy(isLoadingMore = true) }
        }

        viewModelScope.launch {
            runCatching { SpotifyUserApi.likedTracks(offset = current.tracks.size) }
                .onSuccess { page ->
                    mutableState.update { state ->
                        val tracks = state.tracks + page.tracks
                        state.copy(
                            tracks = tracks,
                            total = page.total,
                            isLoading = false,
                            isLoadingMore = false,
                            failed = false,
                            endReached = page.tracks.isEmpty() || tracks.size >= page.total,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Could not load liked songs: ${error.message}")
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            failed = state.tracks.isEmpty(),
                            endReached = state.tracks.isNotEmpty(),
                        )
                    }
                }
            loading = false
        }
    }

    fun downloadTrack(url: String, name: String) {
        Downloader.executeParallelDownloadWithUrl(url, name)
    }

    fun downloadTracks(tracks: List<TrackDownload>) {
        if (tracks.isEmpty()) return
        applicationScope.launch(Dispatchers.IO) {
            tracks.forEach { track ->
                Downloader.executeParallelDownloadWithUrl(track.url, track.name).join()
            }
        }
    }
}
