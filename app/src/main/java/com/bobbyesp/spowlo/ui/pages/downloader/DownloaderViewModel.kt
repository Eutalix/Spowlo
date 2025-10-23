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
import com.bobbyesp.spowlo.utils.DebugLogger
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
     * Paste-and-download action with diagnostics.
     */
    fun onPasteAndDownload(raw: String) {
        DebugLogger.log("VM", "onPasteAndDownload called, raw='$raw'")
        val normalized = UrlValidator.normalize(raw)
        if (!UrlValidator.isSupported(normalized)) {
            DebugLogger.log("VM", "URL not supported after normalize='$normalized'")
            showErrorMessage(R.string.url_empty)
            return
        }
        updateUrl(normalized)
        val klass = UrlValidator.classify(normalized)
        DebugLogger.log("VM", "URL set='$normalized' classify=$klass")

        if (!App.isInitialized.value) {
            DebugLogger.log("VM", "App not initialized yet")
            ToastUtil.makeToast(R.string.app_is_initializing)
            return
        }

        when (klass) {
            UrlValidator.Type.SpotifyTrack -> {
                val skip = PreferencesUtil.getValue(SKIP_INFO_FETCH)
                DebugLogger.log("VM", "SpotifyTrack, skipInfoFetch=$skip → startDownloadSong")
                startDownloadSong(skipInfoFetch = skip)
            }
            UrlValidator.Type.SpotifyAlbum,
            UrlValidator.Type.SpotifyArtist,
            UrlValidator.Type.SpotifyPlaylist -> {
                DebugLogger.log("VM", "List-type URL → UI opens sheet")
            }
            UrlValidator.Type.Other -> {
                DebugLogger.log("VM", "Other provider → startDownloadSong(skipInfoFetch=true)")
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
        val url = viewStateFlow.value.url
        DebugLogger.log("VM", "requestMetadata url='$url'")
        if (!App.isInitialized.value) {
            DebugLogger.log("VM", "requestMetadata aborted: app initializing")
            ToastUtil.makeToast(R.string.app_is_initializing)
            return
        }

        Downloader.clearErrorState()
        if (!Downloader.isDownloaderAvailable()) return
        if (url.isBlank()) {
            showErrorMessage(R.string.url_empty)
            return
        }
        Downloader.getRequestedMetadata(url)
    }

    fun startDownloadSong(skipInfoFetch: Boolean = false) {
        val url = viewStateFlow.value.url
        DebugLogger.log("VM", "startDownloadSong url='$url' skipInfoFetch=$skipInfoFetch")
        if (!App.isInitialized.value) {
            DebugLogger.log("VM", "startDownloadSong aborted: app initializing")
            ToastUtil.makeToast(R.string.app_is_initializing)
            return
        }

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