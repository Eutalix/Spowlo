package com.bobbyesp.spowlo.ui.pages.history

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.ConfirmButton
import com.bobbyesp.spowlo.ui.components.DismissButton
import com.bobbyesp.spowlo.ui.components.LargeTopAppBar
import com.bobbyesp.spowlo.ui.components.MultiChoiceItem
import com.bobbyesp.spowlo.ui.components.SpowloDialog
import com.bobbyesp.spowlo.ui.components.history.HistoryMediaItem
import com.bobbyesp.spowlo.utils.FilesUtil
import com.bobbyesp.spowlo.utils.FilesUtil.getFileSize
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import com.bobbyesp.spowlo.utils.toFileSizeText
import kotlinx.coroutines.launch

/**
 * A Composable screen that displays the user's download history.
 * It retrieves a list of downloaded songs from the local database via the
 * [DownloadsHistoryViewModel] and displays them in a grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsHistoryPage(
    downloadsHistoryViewModel: DownloadsHistoryViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val songsList by downloadsHistoryViewModel.songsListFlow.collectAsState(initial = emptyList())
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()

    var isSelectEnabled by remember { mutableStateOf(false) }
    var showRemoveMultipleItemsDialog by remember { mutableStateOf(false) }
    val selectedItemIds = remember(songsList, isSelectEnabled) { mutableStateListOf<Int>() }

    val selectedFilesCount = selectedItemIds.size
    val selectedFileSizeSum by remember(selectedItemIds.size) {
        derivedStateOf {
            selectedItemIds.sumOf { id ->
                songsList.find { it.id == id }?.songPath?.getFileSize() ?: 0L
            }
        }
    }
    
    val checkBoxState by remember(selectedItemIds.size, songsList.size) {
        derivedStateOf {
            when {
                selectedItemIds.isEmpty() -> ToggleableState.Off
                selectedItemIds.size == songsList.size -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
        }
    }

    // When multi-select mode is enabled, the back button should disable it.
    BackHandler(isSelectEnabled) {
        isSelectEnabled = false
        selectedItemIds.clear()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.downloads_history)) },
                navigationIcon = { BackButton(onClick = onBackPressed) },
                actions = {
                    IconToggleButton(
                        checked = isSelectEnabled,
                        onCheckedChange = {
                            isSelectEnabled = it
                            if (!it) selectedItemIds.clear() // Clear selection when disabling
                        },
                        enabled = songsList.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.Checklist, stringResource(R.string.multiselect_mode))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BottomAppBar {
                    TriStateCheckbox(
                        state = checkBoxState,
                        onClick = {
                            if (checkBoxState == ToggleableState.On) {
                                selectedItemIds.clear()
                            } else {
                                selectedItemIds.clear()
                                selectedItemIds.addAll(songsList.map { it.id })
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = stringResource(R.string.select_all) }
                    )
                    Text(
                        text = stringResource(R.string.multiselect_item_count, selectedFilesCount),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showRemoveMultipleItemsDialog = true },
                        enabled = selectedItemIds.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.remove))
                    }
                }
            }
        }
    ) { innerPaddings ->
        if (songsList.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize().padding(innerPaddings).padding(16.dp),
                text = stringResource(R.string.downloads_history_empty_state)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.padding(innerPaddings),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(songsList, key = { it.id }) { song ->
                    HistoryMediaItem(
                        songName = song.songName,
                        author = song.songAuthor,
                        artworkUrl = song.thumbnailUrl,
                        songPath = song.songPath,
                        fileType = song.songPath.substringAfterLast('.').uppercase(),
                        songFileSize = song.songPath.getFileSize(),
                        songDuration = GeneralTextUtils.convertDuration(song.songDuration),
                        songSpotifyUrl = song.songUrl,
                        isSelectEnabled = { isSelectEnabled },
                        isSelected = { selectedItemIds.contains(song.id) },
                        onSelect = {
                            if (selectedItemIds.contains(song.id)) {
                                selectedItemIds.remove(song.id)
                            } else {
                                selectedItemIds.add(song.id)
                            }
                        },
                        onClick = {
                            FilesUtil.openFile(song.songPath) {
                                Log.e("DownloadsHistoryPage", "Error opening file", it)
                            }
                        },
                        onLongClick = { downloadsHistoryViewModel.showDrawer(scope, song) }
                    )
                }
            }
        }
    }

    // This handles the bottom sheet for single-item actions.
    DownloadHistoryBottomDrawer()

    // This handles the dialog for multi-item deletion.
    if (showRemoveMultipleItemsDialog) {
        var deleteFile by remember { mutableStateOf(false) }
        SpowloDialog(
            onDismissRequest = { showRemoveMultipleItemsDialog = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, null) },
            title = { Text(stringResource(R.string.delete_info)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.delete_multiple_items_msg, selectedFilesCount),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    MultiChoiceItem(
                        text = "${stringResource(R.string.delete_file)} (${selectedFileSizeSum.toFileSizeText()})",
                        checked = deleteFile,
                        onClick = { deleteFile = !deleteFile },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            },
            confirmButton = {
                ConfirmButton {
                    downloadsHistoryViewModel.removeItems(selectedItemIds, deleteFile)
                    showRemoveMultipleItemsDialog = false
                    isSelectEnabled = false
                }
            },
            dismissButton = {
                DismissButton { showRemoveMultipleItemsDialog = false }
            }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier, text: String) {
    OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DownloadForOffline,
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}