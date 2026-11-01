package com.bobbyesp.spowlo.ui.pages.download_tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.spowlo.App.Companion.applicationScope
import com.bobbyesp.spowlo.App.Companion.context
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.utils.DatabaseUtil
import com.bobbyesp.spowlo.utils.HistoryMetadataMatcher
import com.bobbyesp.spowlo.utils.HistoryMetadataMatcher.withTrackMetadata
import com.bobbyesp.spowlo.utils.ToastUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsViewModel : ViewModel() {

    private val mediaInfoFlow = DatabaseUtil.getMediaInfo()

    val songsListFlow = mediaInfoFlow.map { infoList ->
        infoList.sortedWith(
            compareByDescending<DownloadedSongInfo> { it.downloadedAt }
                .thenByDescending { it.id }
        )
    }

    val filterSetFlow = mediaInfoFlow.map { infoList ->
        infoList.map { it.songAuthor }.filter { it.isNotBlank() }.distinct()
    }

    private val mutableStateFlow = MutableStateFlow(ViewState())
    val stateFlow = mutableStateFlow.asStateFlow()

    data class ViewState(
        val activeFilterIndex: Int = -1
    )

    data class MetadataProgress(
        val total: Int = 0,
        val completed: Int = 0,
        val inProgressId: Int? = null,
    ) {
        val isRunning: Boolean get() = total > 0 && completed < total
    }

    private val _metadataProgress = MutableStateFlow(MetadataProgress())
    val metadataProgress = _metadataProgress.asStateFlow()

    fun clickAuthorFilter(index: Int) {
        mutableStateFlow.update {
            it.copy(activeFilterIndex = if (it.activeFilterIndex == index) -1 else index)
        }
    }

    fun updateMetadata(ids: List<Int>) {
        if (ids.isEmpty() || _metadataProgress.value.isRunning) return
        viewModelScope.launch(Dispatchers.IO) {
            var updated = 0
            try {
                _metadataProgress.value = MetadataProgress(total = ids.size)
                ids.forEach { id ->
                    _metadataProgress.update { it.copy(inProgressId = id) }
                    val song = DatabaseUtil.getInfoByIdOrNull(id)
                    val track = song?.let { HistoryMetadataMatcher.findTrack(it) }
                    if (song != null && track != null) {
                        Downloader.writeTrackTags(track, song.songPath, song.songName)
                        DatabaseUtil.updateSongInfo(song.withTrackMetadata(track))
                        updated++
                    }
                    _metadataProgress.update {
                        it.copy(completed = it.completed + 1, inProgressId = null)
                    }
                }
            } finally {
                _metadataProgress.value = MetadataProgress()
            }
            ToastUtil.makeToastSuspend(context, metadataResultMessage(updated, ids.size))
        }
    }

    private fun metadataResultMessage(updated: Int, total: Int): String = when {
        updated == 0 -> context.getString(R.string.no_spotify_matches)
        updated < total -> context.getString(R.string.metadata_updated_partial, updated, total)
        else -> context.resources.getQuantityString(R.plurals.metadata_updated, updated, updated)
    }

    fun redownload(ids: List<Int>) {
        if (ids.isEmpty()) return
        applicationScope.launch(Dispatchers.IO) {
            ToastUtil.makeToastSuspend(
                context,
                context.resources.getQuantityString(
                    R.plurals.redownload_started, ids.size, ids.size
                )
            )
            var completed = 0
            ids.forEach { id ->
                val song = DatabaseUtil.getInfoByIdOrNull(id) ?: return@forEach
                val track = HistoryMetadataMatcher.findTrack(song) ?: return@forEach
                val url = track.externalUrls.spotify ?: return@forEach
                if (song.songUrl.isBlank() || song.thumbnailUrl.isBlank()) {
                    DatabaseUtil.updateSongInfo(song.withTrackMetadata(track))
                }
                if (Downloader.redownloadTrack(url, song.songName, song.songPath)) completed++
            }
            if (completed < ids.size) {
                ToastUtil.makeToastSuspend(
                    context,
                    if (completed == 0) {
                        context.getString(R.string.no_spotify_matches)
                    } else {
                        context.getString(R.string.redownload_partial, completed, ids.size)
                    }
                )
            }
        }
    }

    fun deleteItems(ids: Collection<Int>, deleteFile: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.IO) {
                DatabaseUtil.deleteInfoListByIdList(ids.toList(), deleteFile)
            }
        }
    }
}
