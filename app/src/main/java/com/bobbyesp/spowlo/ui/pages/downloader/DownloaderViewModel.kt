package com.bobbyesp.spowlo.ui.pages.downloader

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
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.utils.DatabaseUtil
import com.bobbyesp.spowlo.utils.SpotifyLinkPreview
import com.bobbyesp.spowlo.utils.SpotifyMetadataFetcher
import com.bobbyesp.spowlo.utils.UrlValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
class DownloaderViewModel : ViewModel() {

    private val mutableViewStateFlow = MutableStateFlow(ViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()

    private val songInfoFlow = MutableStateFlow(listOf(SpotifySong()))
    
    private val _isDependenciesReady = MutableStateFlow(false)
    val isDependenciesReady = _isDependenciesReady.asStateFlow()

    val recentDownloads: StateFlow<List<DownloadedSongInfo>> = DatabaseUtil.getMediaInfo()
        .map { songs -> songs.sortedByDescending { it.downloadedAt }.take(RECENT_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    sealed interface LinkPreviewState {
        data object None : LinkPreviewState
        data object Loading : LinkPreviewState
        data class Loaded(val preview: SpotifyLinkPreview) : LinkPreviewState
    }

    private val mutableLinkPreview = MutableStateFlow<LinkPreviewState>(LinkPreviewState.None)
    val linkPreview = mutableLinkPreview.asStateFlow()

    private var previewJob: Job? = null
    private val previewCache = mutableMapOf<String, SpotifyLinkPreview>()

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
            // spotDL imports yt-dlp, ytmusicapi and syncedlyrics at module level,
            // so a missing package means downloads cannot start.
            _isDependenciesReady.emit(SpotDLDependencyUpdater.areDependenciesInstalled(App.context))
        }
    }
    
    // New Method called from UI when update finishes
    fun refreshDependencyState() {
        checkDependencies()
    }

    fun updateUrl(url: String, isUrlSharingTriggered: Boolean = false) {
        val urlChanged = url != viewStateFlow.value.url
        mutableViewStateFlow.update {
            it.copy(url = url, isUrlSharingTriggered = isUrlSharingTriggered)
        }
        if (urlChanged && Downloader.errorState.value.isErrorOccurred()) {
            Downloader.clearErrorState()
        }
        previewLink(url)
    }

    private fun previewLink(url: String) {
        previewJob?.cancel()
        val link = UrlValidator.parseSpotifyLink(url)
        if (link == null) {
            mutableLinkPreview.value = LinkPreviewState.None
            return
        }
        val key = "${link.type}/${link.id}"
        previewCache[key]?.let {
            mutableLinkPreview.value = LinkPreviewState.Loaded(it)
            return
        }
        mutableLinkPreview.value = LinkPreviewState.Loading
        previewJob = viewModelScope.launch {
            delay(PREVIEW_DEBOUNCE_MS)
            val preview = SpotifyMetadataFetcher.fetchLinkPreview(url)
            if (preview != null) previewCache[key] = preview
            mutableLinkPreview.value = preview
                ?.let { LinkPreviewState.Loaded(it) }
                ?: LinkPreviewState.None
        }
    }

    fun validateLinkOrShowError(url: String): Boolean {
        return when {
            url.isBlank() -> {
                showErrorMessage(R.string.url_empty)
                false
            }

            !UrlValidator.isDownloadable(url) -> {
                showErrorMessage(R.string.unsupported_link)
                false
            }

            else -> true
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
        if (UrlValidator.classify(url) != UrlValidator.Type.SpotifyTrack) {
            showErrorMessage(R.string.track_link_required)
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
        if (!validateLinkOrShowError(url)) return
        Downloader.executeParallelDownloadWithUrl(url, name = url.trim())
    }

    fun goToMetadataViewer(songs: List<SpotifySong>) {
        songInfoFlow.update { songs }
    }

    fun onShareIntentConsumed() {
        mutableViewStateFlow.update { it.copy(isUrlSharingTriggered = false) }
    }

    private companion object {
        const val RECENT_LIMIT = 6
        const val PREVIEW_DEBOUNCE_MS = 350L
    }
}
