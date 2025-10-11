package com.bobbyesp.spowlo.ui.pages.downloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamratzman.spotify.models.Track
import com.bobbyesp.library.SpotDL
import com.bobbyesp.spowlo.Downloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * A data class representing the UI state for the main downloader screen.
 * It holds the information needed to render the view, such as loading status,
 * fetched track metadata, or any error messages.
 */
data class DownloaderUiState(
    val isLoading: Boolean = false,
    val spotifyTrack: Track? = null,
    val errorMessage: String? = null,
    val sharedUrl: String = "" // Holds the URL passed via an intent
)

/**
 * The ViewModel for the DownloaderPage.
 * It acts as a bridge between the UI and the underlying business logic (Downloader singleton)
 * and data sources (:library's SpotDL instance).
 */
@HiltViewModel
class DownloaderViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DownloaderUiState())
    val uiState: StateFlow<DownloaderUiState> = _uiState.asStateFlow()

    // Expose the tasks map from the singleton for other UIs (like DownloadTasksPage) to observe.
    val downloadTasks = Downloader.tasks

    private val spotdl = SpotDL.getInstance()

    /**
     * Called from MainActivity when a URL is shared with the app via an Intent.
     * It updates the state and automatically triggers fetching the track info.
     */
    fun updateUrl(url: String, isFromShare: Boolean = false) {
        _uiState.value = _uiState.value.copy(sharedUrl = url)
        if (isFromShare) {
            fetchTrackInfo(url)
        }
    }

    /**
     * Fetches track metadata from the Spotify API using the native library call.
     * @param url The Spotify track URL to fetch.
     */
    fun fetchTrackInfo(url: String) {
        _uiState.value = DownloaderUiState(isLoading = true)
        viewModelScope.launch {
            // Perform the network request on the IO dispatcher.
            val track = withContext(Dispatchers.IO) {
                spotdl.getTrack(url)
            }
            // Update the UI state with the result.
            if (track != null) {
                _uiState.value = DownloaderUiState(spotifyTrack = track)
            } else {
                _uiState.value = DownloaderUiState(errorMessage = "Track not found or invalid URL.")
            }
        }
    }

    /**
     * Delegates the download request to the central Downloader singleton.
     * @param track The Track object to be downloaded.
     */
    fun startDownload(track: Track) {
        Downloader.downloadSong(track)
    }

    /**
     * Delegates the cancellation request to the Downloader singleton.
     * @param taskId The unique ID of the task to cancel.
     */
    fun cancelDownload(taskId: String) {
        Downloader.cancelDownload(taskId)
    }

    /**
     * Resets the UI state to its initial, empty state.
     */
    fun clearTrackInfo() {
        _uiState.value = DownloaderUiState()
    }
}