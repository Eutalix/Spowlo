package com.bobbyesp.spowlo.ui.pages.download_tasks

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.ui.components.AudioFilterChip
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.ConfirmButton
import com.bobbyesp.spowlo.ui.components.DismissButton
import com.bobbyesp.spowlo.ui.components.MultiChoiceItem
import com.bobbyesp.spowlo.ui.components.SpowloDialog
import com.bobbyesp.spowlo.ui.components.download_tasks.DownloadedSongCard
import com.bobbyesp.spowlo.ui.components.download_tasks.DownloadingTaskItem
import com.bobbyesp.spowlo.ui.components.download_tasks.TaskState
import com.bobbyesp.spowlo.ui.dialogs.NotificationPermissionDialog
import com.bobbyesp.spowlo.ui.dialogs.rememberNotificationPermissionRequest
import com.bobbyesp.spowlo.utils.FilesUtil
import com.bobbyesp.spowlo.utils.FilesUtil.getFileSize
import com.bobbyesp.spowlo.utils.HistoryImporter
import com.bobbyesp.spowlo.utils.NOTIFICATION
import com.bobbyesp.spowlo.utils.NOTIFICATION_HINT_DISMISSED
import com.bobbyesp.spowlo.utils.NOTIFICATION_PERMISSION_REQUESTED
import com.bobbyesp.spowlo.utils.NotificationsUtil
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.toFileSizeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadTasksPage(
    onGoBack: () -> Unit = {},
    onNavigateToLog: (String) -> Unit,
    downloadsViewModel: DownloadsViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val viewState by downloadsViewModel.stateFlow.collectAsStateWithLifecycle()
    val songsList by downloadsViewModel.songsListFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val filterSet by downloadsViewModel.filterSetFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    var isSelectEnabled by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateListOf<Int>() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteFile by remember { mutableStateOf(false) }
    var pendingDeleteIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var showStorageAccessDialog by remember { mutableStateOf(false) }
    val metadataProgress by downloadsViewModel.metadataProgress.collectAsStateWithLifecycle()
    val isUpdatingMetadata = metadataProgress.isRunning
    var redownloadTargets by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showRedownloadDialog by remember { mutableStateOf(false) }

    var notificationsOn by remember { mutableStateOf(NotificationsUtil.notificationsEnabled()) }
    var hintDismissed by remember {
        mutableStateOf(PreferencesUtil.getValue(NOTIFICATION_HINT_DISMISSED))
    }
    var permissionRequested by remember {
        mutableStateOf(PreferencesUtil.getValue(NOTIFICATION_PERMISSION_REQUESTED))
    }
    var showNotificationDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsOn = NotificationsUtil.notificationsEnabled()
    }

    val requestNotificationPermission = rememberNotificationPermissionRequest { granted ->
        notificationsOn = NotificationsUtil.notificationsEnabled()
        if (!granted) ToastUtil.makeToast(context.getString(R.string.permission_denied))
    }

    val activeTasks = Downloader.mutableTaskList.values
        .filter { task ->
            task.state is Downloader.DownloadTask.State.Running ||
                task.state is Downloader.DownloadTask.State.Error ||
                task.state is Downloader.DownloadTask.State.Canceled
        }
        .sortedBy { task ->
            when (task.state) {
                is Downloader.DownloadTask.State.Running -> 0
                is Downloader.DownloadTask.State.Error -> 1
                is Downloader.DownloadTask.State.Canceled -> 2
                Downloader.DownloadTask.State.Completed -> 3
            }
        }

    val selectedAuthor = filterSet.getOrNull(viewState.activeFilterIndex)
    val filteredSongs = remember(songsList, selectedAuthor) {
        if (selectedAuthor.isNullOrBlank()) songsList
        else songsList.filter { it.songAuthor == selectedAuthor }
    }

    var fileSizes by remember { mutableStateOf<Map<Int, Long>>(emptyMap()) }
    LaunchedEffect(songsList) {
        val sizes = withContext(Dispatchers.IO) {
            songsList.associate { info ->
                info.id to runCatching { info.songPath.getFileSize() }.getOrDefault(0L)
            }
        }
        fileSizes = sizes
    }

    LaunchedEffect(songsList) {
        val validIds = songsList.map { it.id }.toSet()
        selectedItemIds.retainAll(validIds)
    }

    val visibleIds = filteredSongs.map { it.id }
    val selectedVisibleCount = selectedItemIds.count { it in visibleIds }
    val checkBoxState = when {
        selectedVisibleCount == 0 -> androidx.compose.ui.state.ToggleableState.Off
        selectedVisibleCount == visibleIds.size && visibleIds.isNotEmpty() ->
            androidx.compose.ui.state.ToggleableState.On

        else -> androidx.compose.ui.state.ToggleableState.Indeterminate
    }

    val closeSelectionMode = {
        isSelectEnabled = false
        selectedItemIds.clear()
    }

    val importExistingFiles: () -> Unit = {
        when {
            isImporting -> Unit
            HistoryImporter.needsStorageAccessToScan() -> showStorageAccessDialog = true
            else -> {
                isImporting = true
                scope.launch {
                    val result = HistoryImporter.importExistingFiles()
                    isImporting = false
                    val message = if (result.imported > 0) {
                        context.resources.getQuantityString(
                            R.plurals.imported_songs, result.imported, result.imported
                        )
                    } else {
                        context.getString(R.string.import_no_new_songs)
                    }
                    ToastUtil.makeToast(context, message)
                }
            }
        }
    }

    BackHandler(isSelectEnabled) {
        closeSelectionMode()
    }

    val updateMetadata: (List<Int>) -> Unit = { ids -> downloadsViewModel.updateMetadata(ids) }

    val requestRedownload: (List<Int>) -> Unit = { ids ->
        if (ids.isNotEmpty()) {
            redownloadTargets = ids
            showRedownloadDialog = true
        }
    }

    val playFile = { song: DownloadedSongInfo ->
        FilesUtil.openFile(song.songPath) {
            ToastUtil.makeToastSuspend(context.getString(R.string.file_unavailable))
        }
    }

    val copyLink = { song: DownloadedSongInfo ->
        clipboardManager.setText(AnnotatedString(song.songUrl))
        ToastUtil.makeToast(context, context.getString(R.string.link_copied))
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSelectEnabled) {
                            stringResource(R.string.multiselect_item_count).format(
                                selectedItemIds.size
                            )
                        } else {
                            stringResource(R.string.downloads)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                    )
                }, navigationIcon = {
                    if (isSelectEnabled) {
                        IconButton(onClick = { closeSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    } else {
                        BackButton {
                            onGoBack()
                        }
                    }
                }, actions = {
                    if (!isSelectEnabled) {
                        IconButton(onClick = importExistingFiles, enabled = !isImporting) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.LibraryAdd,
                                    contentDescription = stringResource(R.string.import_existing_files)
                                )
                            }
                        }
                    }
                    if (!isSelectEnabled && songsList.isNotEmpty()) {
                        IconButton(onClick = { isSelectEnabled = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = stringResource(R.string.multiselect_mode)
                            )
                        }
                    }
                }, scrollBehavior = scrollBehavior
            )
        }, bottomBar = {
            AnimatedVisibility(
                isSelectEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    AnimatedVisibility(
                        isUpdatingMetadata,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        MetadataUpdateProgress(progress = metadataProgress)
                    }
                    BottomAppBar {
                        TriStateCheckbox(
                            state = checkBoxState,
                            onClick = {
                                when (checkBoxState) {
                                    androidx.compose.ui.state.ToggleableState.On ->
                                        selectedItemIds.clear()

                                    else -> visibleIds
                                        .filterNot { selectedItemIds.contains(it) }
                                        .forEach { selectedItemIds.add(it) }
                                }
                            },
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.multiselect_item_count).format(
                                selectedItemIds.size
                            ),
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(
                            onClick = { requestRedownload(selectedItemIds.toList()) },
                            enabled = selectedItemIds.isNotEmpty() && !isUpdatingMetadata
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.redownload)
                            )
                        }
                        IconButton(
                            onClick = { updateMetadata(selectedItemIds.toList()) },
                            enabled = selectedItemIds.isNotEmpty() && !isUpdatingMetadata
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = stringResource(R.string.update_metadata)
                            )
                        }
                        IconButton(
                            onClick = {
                                pendingDeleteIds = selectedItemIds.toList()
                                deleteFile = false
                                showDeleteDialog = true
                            },
                            enabled = selectedItemIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(id = R.string.remove)
                            )
                        }
                    }
                }
            }
        }) { paddings ->
        LazyColumn(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeTasks.isEmpty() && songsList.isEmpty()) {
                item {
                    EmptyDownloadsState(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp),
                        isImporting = isImporting,
                        onImport = importExistingFiles
                    )
                }
                return@LazyColumn
            }

            if (!notificationsOn && !hintDismissed) {
                item {
                    NotificationHint(
                        onEnable = {
                            if (Build.VERSION.SDK_INT >= 33 && !permissionRequested) {
                                showNotificationDialog = true
                            } else if (!NotificationsUtil.areNotificationsEnabled()) {
                                PreferencesUtil.updateValue(NOTIFICATION, true)
                                NotificationsUtil.openNotificationSettings()
                            } else {
                                PreferencesUtil.updateValue(NOTIFICATION, true)
                                notificationsOn = true
                            }
                        },
                        onDismiss = {
                            hintDismissed = true
                            PreferencesUtil.updateValue(NOTIFICATION_HINT_DISMISSED, true)
                        }
                    )
                }
            }

            if (activeTasks.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.downloads_in_progress),
                        count = activeTasks.size
                    )
                }
                items(activeTasks, key = { "active_${it.toKey()}" }) { task ->
                    DownloadingTaskItem(
                        status = task.state.toStatus(),
                        progress = if (task.state is Downloader.DownloadTask.State.Running) {
                            task.state.progress
                        } else 0f,
                        progressText = task.currentLine,
                        url = task.url,
                        header = task.taskName,
                        artist = task.artist,
                        artworkUrl = task.artworkUrl,
                        onCopyError = { task.onCopyError(clipboardManager) },
                        onCancel = { task.onCancel() },
                        onRestart = { task.onRestart() },
                        onCopyLog = { task.onCopyLog(clipboardManager) },
                        onShowLog = { onNavigateToLog("active_${task.hashCode()}") },
                        onCopyLink = { task.onCopyUrl(clipboardManager) },
                    )
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.downloads_completed),
                    count = songsList.size
                )
            }

            if (filterSet.size > 1 && !isSelectEnabled) {
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.filter_artist),
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            filterSet.forEachIndexed { index, author ->
                                AudioFilterChip(
                                    selected = viewState.activeFilterIndex == index,
                                    onClick = { downloadsViewModel.clickAuthorFilter(index) },
                                    label = author
                                )
                            }
                        }
                    }
                }
            }

            if (filteredSongs.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        text = stringResource(R.string.downloads_history_empty_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(filteredSongs, key = { it.id }) { song ->
                    DownloadedSongCard(
                        song = song,
                        fileSize = fileSizes[song.id],
                        selectionMode = isSelectEnabled,
                        selected = selectedItemIds.contains(song.id),
                        isUpdatingMetadata = metadataProgress.inProgressId == song.id,
                        metadataUpdateEnabled = !isUpdatingMetadata,
                        onSelect = {
                            if (!isSelectEnabled) isSelectEnabled = true
                            if (!selectedItemIds.remove(song.id)) selectedItemIds.add(song.id)
                        },
                        onPlay = { playFile(song) },
                        onCopyLink = { copyLink(song) },
                        onShowLog = { onNavigateToLog("history_${song.id}") },
                        onShare = {
                            FilesUtil.createIntentForSharingFile(song.songPath)?.let { intent ->
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        context.getString(R.string.share_file)
                                    )
                                )
                            }
                        },
                        onOpenInSpotify = {
                            runCatching { uriHandler.openUri(song.songUrl) }
                                .onFailure {
                                    ToastUtil.makeToastSuspend(
                                        context.getString(R.string.file_unavailable)
                                    )
                                }
                        },
                        onDelete = {
                            pendingDeleteIds = listOf(song.id)
                            deleteFile = false
                            showDeleteDialog = true
                        },
                        onRedownload = { requestRedownload(listOf(song.id)) },
                        onUpdateMetadata = { updateMetadata(listOf(song.id)) },
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        val deleteSizeSum = pendingDeleteIds.sumOf { fileSizes[it] ?: 0L }
        SpowloDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, null) },
            title = { Text(stringResource(R.string.delete_info)) },
            text = {
                Column {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        text = stringResource(R.string.delete_multiple_items_msg).format(
                            pendingDeleteIds.size
                        )
                    )
                    MultiChoiceItem(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        text = if (deleteSizeSum > 0L) {
                            "${stringResource(R.string.delete_file)} (${deleteSizeSum.toFileSizeText()})"
                        } else {
                            stringResource(R.string.delete_file)
                        },
                        checked = deleteFile
                    ) { deleteFile = !deleteFile }
                }
            },
            confirmButton = {
                ConfirmButton {
                    downloadsViewModel.deleteItems(pendingDeleteIds, deleteFile)
                    showDeleteDialog = false
                    pendingDeleteIds = emptyList()
                    if (isSelectEnabled) closeSelectionMode()
                }
            },
            dismissButton = {
                DismissButton {
                    showDeleteDialog = false
                }
            }
        )
    }

    if (showRedownloadDialog) {
        val count = redownloadTargets.size
        SpowloDialog(
            onDismissRequest = {
                showRedownloadDialog = false
                redownloadTargets = emptyList()
            },
            icon = { Icon(Icons.Outlined.Refresh, null) },
            title = {
                Text(pluralStringResource(R.plurals.redownload_confirm_title, count, count))
            },
            text = {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = pluralStringResource(R.plurals.redownload_confirm_message, count)
                )
            },
            confirmButton = {
                ConfirmButton(text = stringResource(R.string.redownload)) {
                    val ids = redownloadTargets
                    showRedownloadDialog = false
                    redownloadTargets = emptyList()
                    closeSelectionMode()
                    downloadsViewModel.redownload(ids)
                }
            },
            dismissButton = {
                DismissButton {
                    showRedownloadDialog = false
                    redownloadTargets = emptyList()
                }
            }
        )
    }

    if (showStorageAccessDialog) {
        SpowloDialog(
            onDismissRequest = { showStorageAccessDialog = false },
            icon = { Icon(Icons.Outlined.Folder, null) },
            title = { Text(stringResource(R.string.import_storage_access_title)) },
            text = {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(R.string.import_storage_access_summary)
                )
            },
            confirmButton = {
                ConfirmButton(text = stringResource(R.string.open_settings)) {
                    showStorageAccessDialog = false
                    FilesUtil.openAllFilesAccessSettings()
                }
            },
            dismissButton = {
                DismissButton {
                    showStorageAccessDialog = false
                }
            }
        )
    }

    if (showNotificationDialog) {
        NotificationPermissionDialog(
            onDismissRequest = { showNotificationDialog = false },
            onPermissionGranted = {
                showNotificationDialog = false
                permissionRequested = true
                requestNotificationPermission()
            }
        )
    }
}

@Composable
private fun NotificationHint(
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.notification_hint_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.notification_hint_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(
                    onClick = onEnable,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(text = stringResource(R.string.enable_notifications_action))
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.dismiss)
                )
            }
        }
    }
}

@Composable
private fun MetadataUpdateProgress(progress: DownloadsViewModel.MetadataProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.updating_metadata),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(
                    R.string.metadata_update_count, progress.completed, progress.total
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = {
                if (progress.total == 0) 0f
                else progress.completed.toFloat() / progress.total
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyDownloadsState(
    modifier: Modifier = Modifier,
    isImporting: Boolean = false,
    onImport: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.DownloadForOffline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.downloads_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.downloads_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onImport, enabled = !isImporting) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.LibraryAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(text = stringResource(R.string.import_existing_files))
        }
    }
}

private fun Downloader.DownloadTask.State.toStatus(): TaskState = when (this) {
    Downloader.DownloadTask.State.Completed -> TaskState.FINISHED
    is Downloader.DownloadTask.State.Error -> TaskState.ERROR
    is Downloader.DownloadTask.State.Running -> TaskState.RUNNING
    Downloader.DownloadTask.State.Canceled -> TaskState.CANCELED
}
