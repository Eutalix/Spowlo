package com.bobbyesp.spowlo.ui.components.download_tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.LocalDarkTheme
import com.bobbyesp.spowlo.ui.components.FlatButtonChip
import com.bobbyesp.spowlo.ui.components.text.AutoResizableText
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.ui.theme.harmonizeWithPrimary
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.kyant.monet.dynamicColorScheme

// This enum is used by the UI layer to represent the visual state of a task.
enum class TaskState { FINISHED, RUNNING, ERROR, CANCELED }

// A specific color palette for "success" states.
val greenTonalPalettes = Color.Green.toTonalPalettes()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadingTaskItem(
    modifier: Modifier = Modifier,
    status: TaskState,
    progress: Float,
    url: String,
    header: String,
    progressText: String,
    onCopyLog: (() -> Unit)? = null,
    onCopyError: (() -> Unit)? = null,
    onRestart: (() -> Unit)? = null,
    onShowLog: () -> Unit,
    onCopyLink: (() -> Unit)? = null,
    onCancel: () -> Unit
) {
    // This provides a green color scheme for the "Completed" state.
    CompositionLocalProvider(LocalTonalPalettes provides greenTonalPalettes) {
        val greenScheme = dynamicColorScheme(!LocalDarkTheme.current.isDarkTheme())
        val accentColor = when (status) {
            TaskState.FINISHED -> greenScheme.primary
            TaskState.RUNNING -> MaterialTheme.colorScheme.primary
            TaskState.ERROR -> MaterialTheme.colorScheme.error.harmonizeWithPrimary()
            TaskState.CANCELED -> Color.Gray.harmonizeWithPrimary()
        }
        val containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

        Surface(
            modifier = modifier,
            color = containerColor,
            shape = CardDefaults.shape,
            onClick = onShowLog,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) { },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display an icon corresponding to the current task state.
                    TaskStatusIcon(status = status, progress = progress, accentColor = accentColor)

                    Column(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .weight(1f)
                    ) {
                        MarqueeText(
                            text = header,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            color = contentColor,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onShowLog,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Outlined.Terminal, contentDescription = stringResource(R.string.open_log))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.Black.copy(alpha = 0.8f)),
                ) {
                    AutoResizableText(
                        text = progressText,
                        modifier = Modifier.padding(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White,
                        maxLines = 1
                    )
                }

                // Display action buttons based on the task state.
                val clipboardManager = LocalClipboardManager.current
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    onCopyLog?.let {
                        FlatButtonChip(icon = Icons.Outlined.ContentCopy, label = stringResource(R.string.copy_log)) { it() }
                    }
                    onCopyLink?.let {
                        FlatButtonChip(icon = Icons.Outlined.Link, label = stringResource(R.string.copy_link)) { clipboardManager.setText(AnnotatedString(url)) }
                    }
                    if (status == TaskState.ERROR) {
                        onCopyError?.let {
                            FlatButtonChip(icon = Icons.Outlined.ErrorOutline, label = stringResource(R.string.copy_error_report), iconColor = MaterialTheme.colorScheme.error) { it() }
                        }
                        onRestart?.let {
                            FlatButtonChip(icon = Icons.Outlined.RestartAlt, label = stringResource(R.string.restart_task), iconColor = MaterialTheme.colorScheme.secondary) { it() }
                        }
                    }
                    if (status == TaskState.RUNNING) {
                        FlatButtonChip(icon = Icons.Outlined.Cancel, label = stringResource(R.string.cancel), iconColor = MaterialTheme.colorScheme.secondary) { onCancel() }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskStatusIcon(status: TaskState, progress: Float, accentColor: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "TaskProgressAnimation"
    )

    Box(modifier = Modifier.padding(8.dp).size(24.dp)) {
        when (status) {
            TaskState.FINISHED -> Icon(Icons.Filled.CheckCircle, stringResource(R.string.status_completed), tint = accentColor)
            TaskState.RUNNING -> CircularProgressIndicator(progress = { animatedProgress }, strokeWidth = 3.dp, color = accentColor)
            TaskState.ERROR -> Icon(Icons.Filled.Error, stringResource(R.string.error), tint = accentColor)
            TaskState.CANCELED -> Icon(Icons.Filled.Cancel, stringResource(R.string.task_canceled), tint = accentColor)
        }
    }
}