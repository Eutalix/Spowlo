package com.bobbyesp.spowlo.ui.pages.downloader

import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.library.data.remote.SpotDLDependencyUpdater
import com.bobbyesp.library.domain.model.SpotifySong
import com.bobbyesp.spowlo.App
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.Downloader.showErrorMessage
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.SKIP_INFO_FETCH
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
    
    private val _isDependenciesReady = MutableStateFlow(false)
    val isDependenciesReady = _isDependenciesReady.asStateFlow()

    data class ViewState(
        val url: String = "",
        val showDownloadSettingDialog: Boolean = false,
        val isUrlSharingTriggered: Boolean = false,
    )
    
    init {
        checkDependencies()
    }

    private fun checkDependencies() {
        viewModelScope.launch {
            // Check if spotdl package is installed
            val version = SpotDLDependencyUpdater.getLocalSpotDLVersion(App.context)
            if (version != "0.0.0") {
                _isDependenciesReady.emit(true)
            } else {
                _isDependenciesReady.emit(false)
            }
        }
    }
    
    // New Method called from UI when update finishes
    fun refreshDependencyState() {
        checkDependencies()
    }

    fun updateUrl(url: String, isUrlSharingTriggered: Boolean = false) =
        mutableViewStateFlow.update {
            it.copy(url = url, isUrlSharingTriggered = isUrlSharingTriggered)
        }

    fun onPasteAndDownload(raw: String) {
        if (!_isDependenciesReady.value) {
            // Re-check one last time in case it just finished
            checkDependencies()
            if (!_isDependenciesReady.value) {
                showErrorMessage("Critical dependencies are missing. Please update spotDL in Settings > General.")
                return
            }
        }
        
        Log.d("DownloaderViewModel", "onPasteAndDownload called, raw='$raw'")
        val normalized = UrlValidator.normalize(raw)
        
        if (!UrlValidator.isSupported(normalized)) {
            Log.d("DownloaderViewModel", "URL not supported after normalize='$normalized'")
            showErrorMessage(R.string.url_empty)
            return
        }
        
        updateUrl(normalized)
        val klass = UrlValidator.classify(normalized)
        Log.d("DownloaderViewModel", "URL set='$normalized' classify=$klass")

        when (klass) {
            UrlValidator.Type.SpotifyTrack -> {
                val skip = PreferencesUtil.getValue(SKIP_INFO_FETCH)
                startDownloadSong(skipInfoFetch = skip)
            }
            UrlValidator.Type.SpotifyAlbum,
            UrlValidator.Type.SpotifyArtist,
            UrlValidator.Type.SpotifyPlaylist -> {
                // UI handles opening sheet
            }
            UrlValidator.Type.Other -> {
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
        if (!_isDependenciesReady.value) {
            showErrorMessage("Dependencies missing. Please update in Settings.")
            return
        }
        scope.launch {
            if (isDialog) mutableViewStateFlow.update { it.copy(showDownloadSettingDialog = true) }
            else sheetState.show()
        }
    }

    fun requestMetadata() {
        if (!_isDependenciesReady.value) {
            showErrorMessage("Dependencies missing. Please update in Settings.")
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
        if (!_isDependenciesReady.value) {
            showErrorMessage("Dependencies missing. Please update in Settings.")
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