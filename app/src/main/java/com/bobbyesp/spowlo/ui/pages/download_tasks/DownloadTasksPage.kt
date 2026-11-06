package com.bobbyesp.spowlo.ui.pages.download_tasks

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.ui.components.ActionDialog
import com.bobbyesp.spowlo.ui.components.AnchoredMenu
import com.bobbyesp.spowlo.ui.components.AnchoredMenuItem
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.ConfirmButton
import com.bobbyesp.spowlo.ui.components.DismissButton
import com.bobbyesp.spowlo.ui.components.SpowloDialog
import com.bobbyesp.spowlo.ui.components.download_tasks.ArtistSheet
import com.bobbyesp.spowlo.ui.components.download_tasks.DownloadedSongRow
import com.bobbyesp.spowlo.ui.components.download_tasks.DownloadingTaskItem
import com.bobbyesp.spowlo.ui.components.download_tasks.TaskState
import com.bobbyesp.spowlo.ui.components.download_tasks.artistLabel
import com.bobbyesp.spowlo.ui.components.download_tasks.sortLabel
import com.bobbyesp.spowlo.ui.dialogs.NotificationPermissionDialog
import com.bobbyesp.spowlo.ui.dialogs.rememberNotificationPermissionRequest
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.folderLabel
import com.bobbyesp.spowlo.utils.ExportFormat
import com.bobbyesp.spowlo.utils.FilesUtil
import com.bobbyesp.spowlo.utils.HistoryImporter
import com.bobbyesp.spowlo.utils.LibraryExporter
import com.bobbyesp.spowlo.utils.NOTIFICATION
import com.bobbyesp.spowlo.utils.NOTIFICATION_HINT_DISMISSED
import com.bobbyesp.spowlo.utils.NOTIFICATION_PERMISSION_REQUESTED
import com.bobbyesp.spowlo.utils.NotificationsUtil
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.toFileSizeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadTasksPage(
    onGoBack: () -> Unit = {},
    onNavigateToLog: (String) -> Unit,
    downloadsViewModel: DownloadsViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current

    val library by downloadsViewModel.library.collectAsStateWithLifecycle()
    val artists by downloadsViewModel.artists.collectAsStateWithLifecycle()
    val songsList = library.allSongs
    val fileSizes by downloadsViewModel.fileSizes.collectAsStateWithLifecycle()
    val songDates = remember(songsList) {
        songsList.associate { it.id to it.downloadedAt }
    }

    var showArtistSheet by remember { mutableStateOf(false) }
    var selectionMenuExpanded by remember { mutableStateOf(false) }

    var isSelectEnabled by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateListOf<Int>() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showMissingDeleteDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var restoreScrollPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }

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

    LaunchedEffect(songsList) {
        val validIds = songsList.map { it.id }.toSet()
        selectedItemIds.retainAll(validIds)
    }

    val visibleIds = library.songs.map { it.id }
    val allVisibleSelected =
        visibleIds.isNotEmpty() && visibleIds.all { it in selectedItemIds }

    val toggleSelectAll: () -> Unit = {
        if (allVisibleSelected) {
            selectedItemIds.removeAll(visibleIds.toSet())
        } else {
            visibleIds.forEach { id ->
                if (!selectedItemIds.contains(id)) selectedItemIds.add(id)
            }
        }
    }

    val closeSelectionMode = {
        restoreScrollPosition =
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        isSelectEnabled = false
        selectedItemIds.clear()
    }

    LaunchedEffect(isSelectEnabled, restoreScrollPosition) {
        if (!isSelectEnabled) {
            val target = restoreScrollPosition ?: return@LaunchedEffect
            delay(220)
            if (!listState.isScrollInProgress) {
                listState.scrollToItem(target.first, target.second)
            }
            restoreScrollPosition = null
        }
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
        focusManager.clearFocus()
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

    val songRow: @Composable (DownloadedSongInfo, Boolean) -> Unit = { song, showDivider ->
        DownloadedSongRow(
            song = song,
            fileSize = fileSizes[song.id],
            selectionMode = isSelectEnabled,
            selected = selectedItemIds.contains(song.id),
            isUpdatingMetadata = metadataProgress.inProgressId == song.id,
            metadataUpdateEnabled = !isUpdatingMetadata,
            showDivider = showDivider,
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
                } ?: ToastUtil.makeToast(context, context.getString(R.string.file_unavailable))
            },
            onOpenInSpotify = {
                runCatching { uriHandler.openUri(song.songUrl) }
                    .onFailure {
                        ToastUtil.makeToast(context, context.getString(R.string.link_open_failed))
                    }
            },
            onDelete = {
                pendingDeleteIds = listOf(song.id)
                showDeleteDialog = true
            },
            onRedownload = { requestRedownload(listOf(song.id)) },
            onUpdateMetadata = { updateMetadata(listOf(song.id)) },
        )
    }

    val selectedSongs = songsList.filter { it.id in selectedItemIds }

    val shareSelection: () -> Unit = {
        val intent = FilesUtil.createIntentForSharingFiles(selectedSongs.map { it.songPath })
        if (intent == null) {
            ToastUtil.makeToast(context, context.getString(R.string.file_unavailable))
        } else {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
        }
    }

    val exportSelection: (ExportFormat, Uri) -> Unit = { format, uri ->
        scope.launch {
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(LibraryExporter.build(selectedSongs, format).toByteArray())
                    } ?: error("Could not open $uri")
                }.isSuccess
            }
            val message = if (written) {
                context.resources.getQuantityString(
                    R.plurals.exported_songs, selectedSongs.size, selectedSongs.size
                )
            } else {
                context.getString(R.string.export_failed)
            }
            ToastUtil.makeToast(context, message)
        }
    }

    val exportM3u = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.M3U.mimeType)
    ) { uri -> uri?.let { exportSelection(ExportFormat.M3U, it) } }

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType)
    ) { uri -> uri?.let { exportSelection(ExportFormat.CSV, it) } }

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
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    if (isSelectEnabled) {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            closeSelectionMode()
                        }) {
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
                },
                actions = {
                    if (isSelectEnabled) {
                        IconButton(onClick = toggleSelectAll, enabled = visibleIds.isNotEmpty()) {
                            Icon(
                                imageVector = if (allVisibleSelected) {
                                    Icons.Outlined.Deselect
                                } else {
                                    Icons.Outlined.SelectAll
                                },
                                contentDescription = stringResource(
                                    if (allVisibleSelected) R.string.deselect_all
                                    else R.string.select_all
                                )
                            )
                        }
                    } else {
                        TextButton(
                            onClick = importExistingFiles,
                            enabled = !isImporting,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
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
                            Spacer(Modifier.width(6.dp))
                            Text(text = stringResource(R.string.import_action))
                        }
                        if (songsList.isNotEmpty()) {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                isSelectEnabled = true
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Checklist,
                                    contentDescription = stringResource(R.string.multiselect_mode)
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
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
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .height(64.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    showDeleteDialog = true
                                },
                                enabled = selectedItemIds.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Box {
                                IconButton(onClick = { selectionMenuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = stringResource(R.string.show_more_actions)
                                    )
                                }
                                AnchoredMenu(
                                    expanded = selectionMenuExpanded,
                                    onDismissRequest = { selectionMenuExpanded = false }
                                ) {
                                    AnchoredMenuItem(
                                        text = stringResource(R.string.share),
                                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            shareSelection()
                                        },
                                        enabled = selectedItemIds.isNotEmpty()
                                    )
                                    AnchoredMenuItem(
                                        text = stringResource(R.string.export_playlist),
                                        leadingIcon = { Icon(Icons.Outlined.QueueMusic, null) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            exportM3u.launch(
                                                LibraryExporter.suggestedName(ExportFormat.M3U)
                                            )
                                        },
                                        enabled = selectedItemIds.isNotEmpty()
                                    )
                                    AnchoredMenuItem(
                                        text = stringResource(R.string.export_csv),
                                        leadingIcon = { Icon(Icons.Outlined.Description, null) },
                                        onClick = {
                                            selectionMenuExpanded = false
                                            exportCsv.launch(
                                                LibraryExporter.suggestedName(ExportFormat.CSV)
                                            )
                                        },
                                        enabled = selectedItemIds.isNotEmpty()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddings ->
        Column(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = songsList.isNotEmpty() && !isSelectEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LibrarySearchField(
                    query = library.query,
                    onQueryChange = downloadsViewModel::setQuery,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 2.dp,
                        bottom = 24.dp
                    )
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
                                modifier = Modifier.padding(bottom = 8.dp),
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
                                modifier = Modifier.padding(bottom = 8.dp),
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

                    if (activeTasks.isEmpty() || songsList.isNotEmpty()) {
                        item {
                            LibraryHeader(
                                count = library.songs.size,
                                totalCount = library.totalCount,
                                totalBytes = library.totalBytes,
                                isFiltered = library.isFiltered,
                                sort = library.sort,
                                onSortSelected = downloadsViewModel::setSort
                            )
                        }
                    }

                    if (songsList.isNotEmpty() &&
                        (songsList.size > 1 || library.isFiltered)
                    ) {
                        item {
                            LibraryFilterRow(
                                author = library.author,
                                formats = library.formats,
                                format = library.format,
                                missingCount = library.missingIds.size,
                                missingOnly = library.missingOnly,
                                hasFilters = library.author != null ||
                                    library.format != null ||
                                    library.missingOnly,
                                onArtistClick = { showArtistSheet = true },
                                onFormatSelected = downloadsViewModel::setFormat,
                                onMissingClick = {
                                    downloadsViewModel.setMissingOnly(!library.missingOnly)
                                },
                                onClearFilters = downloadsViewModel::clearFilters,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    if (library.missingIds.isNotEmpty()) {
                        item {
                            MissingFilesBanner(
                                modifier = Modifier.padding(bottom = 8.dp),
                                count = library.missingIds.size,
                                showingMissing = library.missingOnly,
                                onToggle = {
                                    downloadsViewModel.setMissingOnly(!library.missingOnly)
                                },
                                onRemove = { showMissingDeleteDialog = true }
                            )
                        }
                    }

                    if (library.songs.isEmpty()) {
                        item {
                            NoResultsState(
                                onClearFilters = { downloadsViewModel.clearFilters() }
                            )
                        }
                    } else if (library.groupedByDay) {
                        library.sections.forEach { section ->
                            stickyHeader(key = "day_${section.dayStart}") {
                                DayHeader(dayStart = section.dayStart, count = section.songs.size)
                            }
                            itemsIndexed(
                                items = section.songs,
                                key = { _, song -> song.id }
                            ) { index, song ->
                                songRow(song, index != section.songs.lastIndex)
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = library.songs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            songRow(song, index != library.songs.lastIndex)
                        }
                    }
                }

                if (library.songs.size > 8) {
                    FastScroller(
                        listState = listState,
                        songDates = songDates,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        val count = pendingDeleteIds.size
        val singleSong = if (count == 1) {
            songsList.firstOrNull { it.id == pendingDeleteIds.first() }
        } else {
            null
        }
        val title = when {
            singleSong != null -> stringResource(R.string.delete_song_title, singleSong.songName)
            count == 1 -> stringResource(R.string.delete_this_file)
            else -> pluralStringResource(R.plurals.delete_songs_title, count, count)
        }
        val supportingText = when {
            singleSong != null -> {
                val folder = folderLabel(singleSong.songPath)
                val size = fileSizes[singleSong.id] ?: 0L
                if (size > 0L) {
                    stringResource(R.string.delete_song_message, folder, size.toFileSizeText())
                } else {
                    stringResource(R.string.delete_song_message_no_size, folder)
                }
            }

            count == 1 -> stringResource(R.string.delete_this_file_message, stringResource(R.string.downloads))
            else -> pluralStringResource(R.plurals.delete_songs_message, count)
        }
        ActionDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = Icons.Outlined.DeleteOutline,
            title = title,
            supportingText = supportingText,
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                val ids = pendingDeleteIds
                showDeleteDialog = false
                pendingDeleteIds = emptyList()
                if (isSelectEnabled) closeSelectionMode()
                downloadsViewModel.deleteItems(ids, deleteFile = true)
            }
        )
    }

    if (showMissingDeleteDialog) {
        val count = library.missingIds.size
        SpowloDialog(
            onDismissRequest = { showMissingDeleteDialog = false },
            icon = { Icon(Icons.Outlined.ErrorOutline, null) },
            title = { Text(stringResource(R.string.remove_missing_title)) },
            text = {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = pluralStringResource(R.plurals.remove_missing_message, count, count)
                )
            },
            confirmButton = {
                ConfirmButton {
                    showMissingDeleteDialog = false
                    downloadsViewModel.deleteItems(library.missingIds.toList(), deleteFile = false)
                    ToastUtil.makeToast(
                        context,
                        context.resources.getQuantityString(R.plurals.removed_entries, count, count)
                    )
                }
            },
            dismissButton = {
                DismissButton {
                    showMissingDeleteDialog = false
                }
            }
        )
    }

    if (showRedownloadDialog) {
        val count = redownloadTargets.size
        ActionDialog(
            onDismissRequest = {
                showRedownloadDialog = false
                redownloadTargets = emptyList()
            },
            icon = Icons.Outlined.Refresh,
            title = pluralStringResource(R.plurals.redownload_confirm_title, count, count),
            supportingText = pluralStringResource(R.plurals.redownload_confirm_message, count),
            confirmText = stringResource(R.string.redownload),
            onConfirm = {
                val ids = redownloadTargets
                showRedownloadDialog = false
                redownloadTargets = emptyList()
                closeSelectionMode()
                downloadsViewModel.redownload(ids)
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

    if (showArtistSheet) {
        ArtistSheet(
            artists = artists,
            selected = library.author,
            onSelect = {
                downloadsViewModel.setAuthor(it)
                showArtistSheet = false
            },
            onDismiss = { showArtistSheet = false }
        )
    }
}

@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_downloads),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FastScroller(
    listState: LazyListState,
    songDates: Map<Int, Long>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { 44.dp.toPx() }
    var trackHeight by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var bubbleHeight by remember { mutableIntStateOf(0) }

    val totalItems by remember {
        derivedStateOf { listState.layoutInfo.totalItemsCount }
    }
    val progress by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty() || info.totalItemsCount <= 1) {
                0f
            } else {
                val visibleCount = visible.size
                val lastIndex = visible.last().index
                val denominator = (info.totalItemsCount - visibleCount).coerceAtLeast(1)
                ((lastIndex + 1 - visibleCount).toFloat() / denominator).coerceIn(0f, 1f)
            }
        }
    }
    val bubbleDay by remember(songDates) {
        derivedStateOf {
            if (!dragging) return@derivedStateOf null
            listState.layoutInfo.visibleItemsInfo.firstNotNullOfOrNull { item ->
                when (val key = item.key) {
                    is Int -> songDates[key]?.takeIf { it > 0L }
                    is String -> key.removePrefix("day_").toLongOrNull()?.takeIf { it > 0L }
                    else -> null
                }
            }
        }
    }

    val jumpTo: (Float) -> Unit = jump@{ y ->
        if (trackHeight <= 0 || totalItems <= 1) return@jump
        val fraction = (y / trackHeight).coerceIn(0f, 1f)
        val index = (fraction * (totalItems - 1)).roundToInt()
        scope.launch { listState.scrollToItem(index) }
    }

    Box(
        modifier = modifier
            .width(140.dp)
            .fillMaxHeight(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(16.dp)
                .fillMaxHeight()
                .onSizeChanged { trackHeight = it.height }
                .pointerInput(totalItems) {
                    detectTapGestures { offset -> jumpTo(offset.y) }
                }
                .pointerInput(totalItems) {
                    // Do not consume the initial down: the system back gesture owns the screen edge.
                    // Only take over once the finger has actually moved vertically.
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            jumpTo(offset.y)
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onVerticalDrag = { change, _ ->
                            jumpTo(change.position.y)
                            change.consume()
                        },
                    )
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(if (dragging) 6.dp else 4.dp)
                    .height(44.dp)
                    .offset {
                        val travel = (trackHeight - thumbHeightPx).coerceAtLeast(0f)
                        IntOffset(0, (travel * progress).roundToInt())
                    }
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (dragging) 0.72f else 0.3f
                        )
                    )
            )
        }

        val day = bubbleDay
        if (dragging && day != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        val travel = (trackHeight - thumbHeightPx).coerceAtLeast(0f)
                        val thumbTop = travel * progress
                        IntOffset(
                            x = -28.dp.roundToPx(),
                            y = (thumbTop + thumbHeightPx / 2f - bubbleHeight / 2f).roundToInt(),
                        )
                    }
                    .onSizeChanged { bubbleHeight = it.height },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = formatBubbleDate(day),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private val bubbleDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun formatBubbleDate(dayStart: Long): String =
    Instant.ofEpochMilli(dayStart)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(bubbleDateFormatter)

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
private fun MissingFilesBanner(
    count: Int,
    showingMissing: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = pluralStringResource(R.plurals.missing_files_title, count, count),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.missing_files_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                Row {
                    TextButton(
                        onClick = onToggle,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(
                            text = stringResource(
                                if (showingMissing) R.string.show_all_files
                                else R.string.review_missing_files
                            )
                        )
                    }
                    TextButton(
                        onClick = onRemove,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(text = stringResource(R.string.remove_entries))
                    }
                }
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
            .padding(top = 8.dp, bottom = 4.dp),
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
private fun LibraryHeader(
    count: Int,
    totalCount: Int,
    totalBytes: Long,
    isFiltered: Boolean,
    sort: LibrarySort,
    onSortSelected: (LibrarySort) -> Unit,
) {
    val sizeText = if (totalBytes > 0L) totalBytes.toFileSizeText() else null
    val countLabel = if (isFiltered) {
        stringResource(R.string.library_count_filtered, count, totalCount)
    } else {
        totalCount.toString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.downloads_completed),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = buildString {
                append(countLabel)
                if (sizeText != null) {
                    append(" · ")
                    append(sizeText)
                }
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        SortControl(sort = sort, onSelect = onSortSelected)
    }
}

@Composable
private fun SortControl(
    sort: LibrarySort,
    onSelect: (LibrarySort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Sort,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = sortLabel(sort),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        AnchoredMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LibrarySort.entries.forEach { option ->
                AnchoredMenuItem(
                    text = sortLabel(option),
                    leadingIcon = {
                        if (option == sort) {
                            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                        } else {
                            Spacer(Modifier.size(18.dp))
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun DayHeader(dayStart: Long, count: Int) {
    val today = LocalDate.now()
    val date = if (dayStart <= 0L) null
    else Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).toLocalDate()
    val label = when {
        date == null -> stringResource(R.string.unknown_date)
        date == today -> stringResource(R.string.today)
        date == today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> remember(date) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 4.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun NoResultsState(onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.no_results_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.no_results_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onClearFilters) {
            Text(text = stringResource(R.string.clear_filters))
        }
    }
}

@Composable
private fun LibraryFilterRow(
    author: String?,
    formats: List<LibraryFormat>,
    format: String?,
    missingCount: Int,
    missingOnly: Boolean,
    hasFilters: Boolean,
    onArtistClick: () -> Unit,
    onFormatSelected: (String?) -> Unit,
    onMissingClick: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            item(key = "artist") {
                val selected = author != null
                FilterChip(
                    selected = selected,
                    onClick = onArtistClick,
                    label = {
                        Text(
                            text = if (author == null) stringResource(R.string.artist)
                            else artistLabel(author),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                )
            }
            if (formats.size > 1) {
                item(key = "format") {
                    FormatFilterChip(
                        formats = formats,
                        selected = format,
                        onSelect = onFormatSelected
                    )
                }
            }
            if (missingCount > 0) {
                item(key = "missing") {
                    FilterChip(
                        selected = missingOnly,
                        onClick = onMissingClick,
                        label = {
                            Text(
                                text = stringResource(R.string.missing_count, missingCount),
                                fontWeight = if (missingOnly) FontWeight.SemiBold
                                else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }
        if (hasFilters) {
            TextButton(
                onClick = onClearFilters,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = stringResource(R.string.clear))
            }
        }
    }
}

@Composable
private fun FormatFilterChip(
    formats: List<LibraryFormat>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = {
                Text(
                    text = selected ?: stringResource(R.string.format),
                    fontWeight = if (selected != null) FontWeight.SemiBold
                    else FontWeight.Normal
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null
                )
            }
        )
        AnchoredMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AnchoredMenuItem(
                text = stringResource(R.string.all_formats),
                leadingIcon = {
                    if (selected == null) {
                        Icon(Icons.Outlined.Check, null)
                    } else {
                        Spacer(Modifier.size(18.dp))
                    }
                },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            formats.forEach { entry ->
                AnchoredMenuItem(
                    text = "${entry.name} (${entry.count})",
                    leadingIcon = {
                        if (selected == entry.name) {
                            Icon(Icons.Outlined.Check, null)
                        } else {
                            Spacer(Modifier.size(18.dp))
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(entry.name)
                    }
                )
            }
        }
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
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DownloadForOffline,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(20.dp))
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
