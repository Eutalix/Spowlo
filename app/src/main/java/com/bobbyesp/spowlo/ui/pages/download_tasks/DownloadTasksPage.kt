package com.bobbyesp.spowlo.ui.pages.download_tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.App
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.HorizontalDivider
import com.bobbyesp.spowlo.ui.components.download_tasks.DownloadingTaskItem
import com.bobbyesp.spowlo.ui.components.download_tasks.TaskState
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.utils.DebugLogger
import com.bobbyesp.spowlo.utils.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadTasksPage(
    onGoBack: () -> Unit = {},
    onNavigateToDetail: (Int) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.download_tasks))
                },
                navigationIcon = {
                    BackButton {
                        onGoBack()
                    }
                },
                actions = {
                    // Export diagnostics action
                    IconButton(onClick = {
                        val text = DebugLogger.read()
                        val payload = if (text.isBlank())
                            "No diagnostics yet.\nPath: ${DebugLogger.path()}"
                        else text
                        ShareUtil.shareText(
                            context = App.context,
                            title = "Spowlo diagnostics",
                            text = payload
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.BugReport,
                            contentDescription = stringResource(id = R.string.copy_error_report)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }) { paddings ->
        LazyColumn(
            modifier = Modifier.padding(paddings),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Downloader.mutableTaskList.values.toList()) { task ->
                DownloadingTaskItem(
                    status = task.state.toStatus(),
                    progress = if (task.state is Downloader.DownloadTask.State.Running) (task.state as Downloader.DownloadTask.State.Running).progress else 0f,
                    progressText = task.currentLine,
                    errorText = if (task.state is Downloader.DownloadTask.State.Error) (task.state as Downloader.DownloadTask.State.Error).errorReport else null,
                    url = task.url,
                    header = task.taskName,
                    onCopyError = {
                        // handled by task item itself in Downloader
                    },
                    onCancel = {
                        task.onCancel()
                    },
                    onRestart = {
                        task.onRestart()
                    },
                    onCopyLog = {
                        // handled by task item itself in Downloader
                    },
                    onShowLog = {
                        onNavigateToDetail(task.hashCode())
                    },
                    onCopyLink = {
                        // handled by task item itself in Downloader
                    })
            }
        }
        if (Downloader.mutableTaskList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.no_running_downloads),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp))
                    MarqueeText(
                        text = stringResource(R.string.no_running_downloads_description),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        basicGradientColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun Downloader.DownloadTask.State.toStatus(): TaskState = when (this) {
    Downloader.DownloadTask.State.Completed -> TaskState.FINISHED
    is Downloader.DownloadTask.State.Error -> TaskState.ERROR
    is Downloader.DownloadTask.State.Running -> TaskState.RUNNING
    Downloader.DownloadTask.State.Canceled -> TaskState.CANCELED
}