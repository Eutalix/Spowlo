package com.bobbyesp.spowlo.ui.pages.download_tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.ui.components.ButtonChip
import com.bobbyesp.spowlo.utils.DatabaseUtil
import com.bobbyesp.spowlo.utils.FilesUtil
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import com.bobbyesp.spowlo.utils.ToastUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenConsoleOutput(
    onBackPressed: () -> Unit,
    logKey: String
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Active tasks live in memory; finished downloads load their log from the database.
    val activeTask = if (logKey.startsWith("active_")) {
        val hashCode = logKey.removePrefix("active_").toIntOrNull()
        Downloader.mutableTaskList.values.find { it.hashCode() == hashCode }
    } else null

    var storedInfo by remember(logKey) { mutableStateOf<DownloadedSongInfo?>(null) }
    var isLoadingLog by remember(logKey) { mutableStateOf(logKey.startsWith("history_")) }

    LaunchedEffect(logKey) {
        if (logKey.startsWith("history_")) {
            storedInfo = logKey.removePrefix("history_").toIntOrNull()
                ?.let { DatabaseUtil.getInfoByIdOrNull(it) }
            isLoadingLog = false
        }
    }

    val logText = activeTask?.consoleOutput ?: storedInfo?.consoleOutput.orEmpty()
    val songName = activeTask?.taskName ?: storedInfo?.songName.orEmpty()
    val songUrl = activeTask?.url ?: storedInfo?.songUrl.orEmpty()
    val filePath = storedInfo?.songPath
    val subtitle = listOfNotNull(
        songName.takeIf { it.isNotBlank() },
        GeneralTextUtils.formatDate(storedInfo?.downloadedAt ?: 0L).takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    val minFontSize = 8
    val maxFontSize = 32
    var mutableFontSize by remember { mutableIntStateOf(14) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.download_log),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }, navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.close))
                    }
                }, scrollBehavior = scrollBehavior
            )
        }, bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    ButtonChip(
                        icon = Icons.Outlined.ContentCopy,
                        label = stringResource(id = R.string.copy_log),
                        enabled = logText.isNotBlank(),
                    ) {
                        clipboardManager.setText(AnnotatedString(logText))
                        ToastUtil.makeToast(R.string.log_copied)
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color = Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = stringResource(id = R.string.font_size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            text = mutableFontSize.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                        ButtonChip(
                            label = "-",
                            onClick = {
                                mutableFontSize =
                                    (mutableFontSize - 2).coerceIn(minFontSize, maxFontSize)
                            }
                        )
                        ButtonChip(
                            label = "+",
                            onClick = {
                                mutableFontSize =
                                    (mutableFontSize + 2).coerceIn(minFontSize, maxFontSize)
                            }
                        )
                    }
                    activeTask?.let { task ->
                        with(task) {
                            if (state is Downloader.DownloadTask.State.Error)
                                ButtonChip(
                                    icon = Icons.Outlined.ErrorOutline,
                                    label = stringResource(id = R.string.copy_error_report),
                                    iconColor = MaterialTheme.colorScheme.error,
                                ) {
                                    onCopyError(clipboardManager)
                                }
                            if (state is Downloader.DownloadTask.State.Canceled)
                                ButtonChip(
                                    icon = Icons.Outlined.RestartAlt,
                                    label = stringResource(id = R.string.restart),
                                ) {
                                    onRestart()
                                }
                        }
                    }
                    if (filePath != null) {
                        ButtonChip(
                            icon = Icons.Outlined.PlayArrow,
                            label = stringResource(id = R.string.play_file),
                        ) {
                            FilesUtil.openFile(filePath) {
                                ToastUtil.makeToastSuspend(context.getString(R.string.file_unavailable))
                            }
                        }
                    }
                    if (songUrl.isNotBlank()) {
                        ButtonChip(
                            icon = Icons.Outlined.Link,
                            label = stringResource(id = R.string.copy_link),
                        ) {
                            clipboardManager.setText(AnnotatedString(songUrl))
                            ToastUtil.makeToast(R.string.link_copied)
                        }
                    }
                }
            }
        }) { paddings ->
        if (isLoadingLog) {
            Box(
                modifier = Modifier
                    .padding(paddings)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val scrollState = rememberScrollState()
        LaunchedEffect(key1 = scrollState.maxValue) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        Column(
            modifier = Modifier
                .padding(paddings)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
                .horizontalScroll(rememberScrollState())
        ) {
            SelectionContainer {
                Text(
                    modifier = Modifier.widthIn(max = 800.dp),
                    text = logText.ifBlank { stringResource(R.string.download_log_empty) },
                    fontSize = mutableFontSize.sp,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}
