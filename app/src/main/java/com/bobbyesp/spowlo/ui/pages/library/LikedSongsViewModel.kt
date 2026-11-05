package com.bobbyesp.spowlo.ui.pages.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.spowlo.App.Companion.context
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyUserApi
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.UserTrack
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.TrackDownload
import com.bobbyesp.spowlo.utils.DatabaseUtil
import com.bobbyesp.spowlo.utils.SpotifyUrls
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    private val loadMutex = Mutex()
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
        viewModelScope.launch {
            loadMutex.withLock {
                if (loading || mutableState.value.endReached) return@withLock
                fetchNextPage()
            }
        }
    }

    suspend fun missingTracks(): List<TrackDownload> {
        loadMutex.withLock {
            while (!mutableState.value.endReached) {
                if (!fetchNextPage()) break
            }
        }
        val downloaded = downloadedTrackIds.value
        return mutableState.value.tracks
            .filter { it.id !in downloaded }
            .map { track ->
                TrackDownload(
                    url = track.url,
                    title = track.name,
                    artists = track.artists,
                    durationMs = track.durationMs,
                    albumName = track.albumName,
                    coverUrl = track.imageUrl,
                    explicit = track.explicit,
                )
            }
    }

    private suspend fun fetchNextPage(): Boolean {
        val current = mutableState.value
        loading = true
        if (current.tracks.isEmpty()) {
            mutableState.update { it.copy(isLoading = true, failed = false) }
        } else {
            mutableState.update { it.copy(isLoadingMore = true) }
        }

        return runCatching { SpotifyUserApi.likedTracks(offset = current.tracks.size) }
            .fold(
                onSuccess = { page ->
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
                    true
                },
                onFailure = { error ->
                    Log.w(TAG, "Could not load liked songs: ${error.message}")
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            failed = state.tracks.isEmpty(),
                            endReached = state.tracks.isNotEmpty(),
                        )
                    }
                    false
                },
            ).also {
                loading = false
            }
    }

    fun downloadTrack(url: String, name: String) {
        Downloader.executeParallelDownloadWithUrl(url, name)
    }

    fun downloadMissing(tracks: List<TrackDownload>) {
        if (tracks.isEmpty()) return
        Downloader.downloadTracks(tracks, context.getString(R.string.liked_songs))
    }
}
