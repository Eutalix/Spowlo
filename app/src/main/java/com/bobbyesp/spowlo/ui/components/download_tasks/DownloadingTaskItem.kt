package com.bobbyesp.spowlo.ui.components.download_tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.FlatButtonChip
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import kotlin.math.roundToInt

@Composable
fun DownloadingTaskItem(
    modifier: Modifier = Modifier,
    status: TaskState = TaskState.ERROR,
    progress: Float = .85f,
    url: String = "https://www.example.com",
    header: String = "Faded",
    artist: String = "Alan Walker",
    artworkUrl: String = "",
    progressText: String = "",
    onCopyLog: () -> Unit = {},
    onCopyError: () -> Unit = {},
    onRestart: () -> Unit = {},
    onShowLog: () -> Unit = {},
    onCopyLink: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val accentColor = when (status) {
        TaskState.FINISHED -> MaterialTheme.colorScheme.tertiary
        TaskState.RUNNING -> MaterialTheme.colorScheme.primary
        TaskState.ERROR -> MaterialTheme.colorScheme.error
        TaskState.CANCELED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val labelText = stringResource(
        id = when (status) {
            TaskState.FINISHED -> R.string.status_completed
            TaskState.RUNNING -> R.string.downloading
            TaskState.ERROR -> R.string.error
            TaskState.CANCELED -> R.string.task_canceled
        }
    )

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = CardDefaults.shape,
        onClick = { onShowLog() },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (artworkUrl.isNotBlank()) {
                        AsyncImageImpl(
                            modifier = Modifier.fillMaxSize(),
                            model = artworkUrl,
                            contentDescription = stringResource(R.string.song_cover),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MarqueeText(
                        text = header,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (artist.isNotBlank()) {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (status) {
                                TaskState.FINISHED -> Icons.Filled.CheckCircle
                                TaskState.RUNNING -> Icons.Outlined.Terminal
                                TaskState.ERROR -> Icons.Filled.Error
                                TaskState.CANCELED -> Icons.Filled.Cancel
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (status == TaskState.RUNNING && progress >= 0f) {
                            Text(
                                text = " · ${(progress * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { onShowLog() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Terminal,
                        contentDescription = stringResource(id = R.string.open_log)
                    )
                }
            }

            if (status == TaskState.RUNNING) {
                Spacer(Modifier.height(14.dp))
                if (progress < 0f) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (progressText.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (status == TaskState.ERROR && progressText.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FlatButtonChip(
                    icon = Icons.Outlined.ContentCopy,
                    label = stringResource(id = R.string.copy_log)
                ) { onCopyLog() }
                FlatButtonChip(
                    icon = Icons.Outlined.Link,
                    label = stringResource(id = R.string.copy_link)
                ) { onCopyLink() }
                if (status == TaskState.ERROR) {
                    FlatButtonChip(
                        icon = Icons.Outlined.ErrorOutline,
                        label = stringResource(id = R.string.copy_error_report),
                        iconColor = MaterialTheme.colorScheme.error,
                    ) { onCopyError() }
                    FlatButtonChip(
                        icon = Icons.Outlined.RestartAlt,
                        label = stringResource(id = R.string.restart_task),
                        iconColor = MaterialTheme.colorScheme.secondary,
                    ) { onRestart() }
                }
                if (status == TaskState.RUNNING) {
                    FlatButtonChip(
                        icon = Icons.Outlined.Cancel,
                        label = stringResource(id = R.string.cancel),
                        iconColor = MaterialTheme.colorScheme.secondary,
                    ) { onCancel() }
                }
            }
        }
    }
}

enum class TaskState {
    FINISHED, RUNNING, ERROR, CANCELED
}
