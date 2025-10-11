package com.bobbyesp.spowlo.ui.pages.history

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.spowlo.App
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.database.SongsInfoDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The ViewModel for the DownloadsHistoryPage.
 * It provides a flow of downloaded songs from the database and handles
 * actions like showing details or deleting items.
 */
@HiltViewModel
class DownloadsHistoryViewModel @Inject constructor(
    private val songsInfoDao: SongsInfoDao // Hilt will provide the DAO instance.
) : ViewModel() {

    // A flow of all downloaded song info from the database, ordered with newest first.
    val songsListFlow: Flow<List<DownloadedSongInfo>> = songsInfoDao.getAllMedia()
        .map { it.reversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State for the bottom sheet that shows details for a single song.
    private val _detailViewState = MutableStateFlow(SongDetailViewState())
    val detailViewState = _detailViewState.asStateFlow()

    fun hideDrawer(scope: CoroutineScope) {
        if (_detailViewState.value.drawerState.isVisible) {
            scope.launch {
                _detailViewState.value.drawerState.hide()
            }
        }
    }

    fun showDrawer(scope: CoroutineScope, item: DownloadedSongInfo) {
        scope.launch {
            _detailViewState.update { SongDetailViewState(item) }
            _detailViewState.value.drawerState.show()
        }
    }

    fun removeItem(deleteFile: Boolean) {
        viewModelScope.launch {
            songsInfoDao.deleteInfoListByIdList(listOf(detailViewState.value.id), deleteFile)
        }
    }

    // New function to handle multi-item deletion.
    fun removeItems(ids: List<Int>, deleteFiles: Boolean) {
        viewModelScope.launch {
            songsInfoDao.deleteInfoListByIdList(ids, deleteFiles)
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    data class SongDetailViewState(
        val id: Int = 0,
        val title: String = "",
        val author: String = "",
        val url: String = "",
        val artworkUrl: String = "",
        val path: String = "",
        val drawerState: ModalBottomSheetState = ModalBottomSheetState(
            ModalBottomSheetValue.Hidden,
            isSkipHalfExpanded = true,
            density = Density(context = App.context)
        ),
    ) {
        constructor(info: DownloadedSongInfo) : this(
            info.id,
            info.songName,
            info.songAuthor,
            info.songUrl,
            info.thumbnailUrl,
            info.songPath
        )
    }
}