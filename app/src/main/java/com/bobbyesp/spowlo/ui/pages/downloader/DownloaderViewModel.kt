package com.bobbyesp.spowlo.ui.pages.downloader

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.lifecycle.ViewModel
import com.bobbyesp.library.domain.model.SpotifySong
import com.bobbyesp.spowlo.App
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.Downloader.showErrorMessage
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.UrlValidator // NEW: URL normalize/validate/classify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
class DownloaderViewModel : ViewModel() {

    private val mutableViewStateFlow = MutableStateFlow(ViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()

    private val songInfoFlow = MutableStateFlow(listOf(SpotifySong()))

    data class ViewState(
        val url: String = "",
        val showDownloadSettingDialog: Boolean = false,
        val isUrlSharingTriggered: Boolean = false,
    )

    fun updateUrl(url: String, isUrlSharingTriggered: Boolean = false) =
        mutableViewStateFlow.update {
            it.copy(
                url = url, isUrlSharingTriggered = isUrlSharingTriggered
            )
        }

    /**
     * Paste-and-download action.
     * - Normalizes and validates the pasted URL.
     * - Updates the input field with the normalized URL.
     * - Triggers the appropriate action:
     *   - Spotify track -> start download directly
     *   - Spotify album/artist/playlist -> UI decides (sheet) as it already does
     *   - Others (YouTube, YouTube Music, youtu.be, spotify.link, URIs) -> start download directly
     */
    fun onPasteAndDownload(raw: String) {
        val normalized = UrlValidator.normalize(raw)
        if (!UrlValidator.isSupported(normalized)) {
            showErrorMessage(R.string.invalid_url)
            return
        }
        // Reflect the final URL in the input field
        updateUrl(normalized)

        // Ensure core is initialized before triggering the pipeline
        if (!App.isInitialized.value) {
            ToastUtil.makeToast(R.string.app_is_initializing)
            return
        }

        when (UrlValidator.classify(normalized)) {
            UrlValidator.Type.SpotifyTrack -> {
                startDownloadSong()
            }
            UrlValidator.Type.SpotifyAlbum,
            UrlValidator.Type.SpotifyArtist,
            UrlValidator.Type.SpotifyPlaylist -> {
                // Intentionally no-op here; the UI already opens the settings sheet when needed
            }
            UrlValidator.Type.Other -> {
                startDownloadSong()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun hideDialog(scope: CoroutineScope, isDialog: Boolean, sheetState: SheetState) {
        scope.launch {
            if (isDialog) mutableViewStateFlow.update { it.copy(showDownloadSettingDialog = false) }
            else sheetState.hide()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun showDialog(scope: CoroutineScope, isDialog: Boolean, sheetState: SheetState) {
        scope.launch {
            if (isDialog) mutableViewStateFlow.update { it.copy(showDownloadSettingDialog = true) }
            else sheetState.show()
        }
    }

    fun requestMetadata() {
        // Guard init
        if (!App.isInitialized.value) {
            ToastUtil.makeToast(R.string.app_is_initializing)
            return
        }

        val url = viewStateFlow.value.url
        Downloader.clearErrorState()
        if (!Downloader.isDownloaderAvailable())
            return
        if (url.isBlank()) {
            showErrorMessage(R.string.url_empty)
            return
        }
        Downloader.getRequestedMetadata(url)
    }

    fun startDownloadSong(skipInfoFetch: Boolean = false) {
        // Guard init
        if (!App.isInitialized.value) {
            ToastUtil.makeToast(R.string.app_is_initializing)
            return
        }

        val url = viewStateFlow.value.url
        Downloader.clearErrorState()
        if (!Downloader.isDownloaderAvailable())
            return
        if (url.isBlank()) {
            showErrorMessage(R.string.url_empty)
            return
        }
        Downloader.getInfoAndDownload(url, skipInfoFetch = skipInfoFetch)
    }

    fun goToMetadataViewer(songs: List<SpotifySong>) {
        songInfoFlow.update { songs }
    }

    fun onShareIntentConsumed() {
        mutableViewStateFlow.update { it.copy(isUrlSharingTriggered = false) }
    }
}