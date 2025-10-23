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
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.SKIP_INFO_FETCH
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.UrlValidator
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
            it.copy(url = url, isUrlSharingTriggered = isUrlSharingTriggered)
        }

    /**
     * Paste-and-download action.
     * - Normalizes and validates the pasted URL.
     * - Updates the input field with the normalized URL.
     * - Triggers the appropriate action:
     *   - Spotify track -> start download directly if SKIP_INFO_FETCH is true; otherwise use normal flow
     *   - Spotify album/artist/playlist -> UI decides (settings sheet)
     *   - Other (YouTube, YouTube Music, youtu.be, spotify.link, URIs) -> start download directly (skipInfoFetch=true)
     */
    fun onPasteAndDownload(raw: String) {
        val normalized = UrlValidator.normalize(raw)
        if (!UrlValidator.isSupported(normalized)) {
            showErrorMessage(R.string.url_empty)
            return
        }
        updateUrl(normalized)

        if (!App.isInitialized.value) {
            ToastUtil.makeToast(R.string.app_is_initializing)
            return
        }

        when (UrlValidator.classify(normalized)) {
            UrlValidator.Type.SpotifyTrack -> {
                val skip = PreferencesUtil.getValue(SKIP_INFO_FETCH)
                startDownloadSong(skipInfoFetch = skip)
            }
            UrlValidator.Type.SpotifyAlbum,
            UrlValidator.Type.SpotifyArtist,
            UrlValidator.Type.SpotifyPlaylist -> {
                // No-op here; the UI already opens the settings sheet when needed
            }
            UrlValidator.Type.Other -> {
                // Direct download for non-Spotify URLs: skip metadata fetch
                startDownloadSong(skipInfoFetch = true)
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
        if (!App.isInitialized.value) {
            ToastUtil.makeToast(R.string.app_is_initializing)
            return
        }

        val url = viewStateFlow.value.url
        Downloader.clearErrorState()
        if (!Downloader.isDownloaderAvailable()) return
        if (url.isBlank()) {
            showErrorMessage(R.string.url_empty)
            return
        }
        Downloader.getRequestedMetadata(url)
    }

    fun startDownloadSong(skipInfoFetch: Boolean = false) {
        if (!App.isInitialized.value) {
            ToastUtil.makeToast(R.string.app_is_initializing)
            return
        }

        val url = viewStateFlow.value.url
        Downloader.clearErrorState()
        if (!Downloader.isDownloaderAvailable()) return
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