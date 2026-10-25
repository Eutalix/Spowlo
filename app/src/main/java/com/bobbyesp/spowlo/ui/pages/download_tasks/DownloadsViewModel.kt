package com.bobbyesp.spowlo.ui.pages.download_tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.utils.DatabaseUtil
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

    fun clickAuthorFilter(index: Int) {
        mutableStateFlow.update {
            it.copy(activeFilterIndex = if (it.activeFilterIndex == index) -1 else index)
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
