package com.bobbyesp.spowlo.ui.components.songs.metadata_viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.ActionDialog
import com.bobbyesp.spowlo.ui.components.ConfirmButton
import com.bobbyesp.spowlo.ui.components.DismissButton
import com.bobbyesp.spowlo.ui.components.SpowloDialog
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.TrackDownload
import com.bobbyesp.spowlo.utils.DownloadSizeEstimator
import com.bobbyesp.spowlo.utils.toFileSizeText
import kotlinx.coroutines.launch

@Composable
fun CollectionDownloadActions(
    totalDurationMs: Long,
    missingTracks: List<TrackDownload>,
    hasMore: Boolean = false,
    resolveMissing: (suspend () -> List<TrackDownload>)? = null,
    onDownloadAll: (() -> Unit)?,
    onDownloadMissing: (List<TrackDownload>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estimateBytes = DownloadSizeEstimator.estimateBytes(totalDurationMs)
    val freeBytes = remember(totalDurationMs) { DownloadSizeEstimator.freeBytes() }

    var pendingBytes by remember { mutableStateOf(0L) }
    var pendingFreeBytes by remember { mutableStateOf(0L) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingConfirm by remember { mutableStateOf<List<TrackDownload>?>(null) }
    var preparing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val requestDownload: (Long, () -> Unit) -> Unit = { durationMs, action ->
        val bytes = DownloadSizeEstimator.estimateBytes(durationMs)
        val free = DownloadSizeEstimator.freeBytes()
        if (DownloadSizeEstimator.hasEnoughSpace(bytes, free)) {
            action()
        } else {
            pendingBytes = bytes
            pendingFreeBytes = free
            pendingAction = action
        }
    }

    val downloadMissing: () -> Unit = {
        if (hasMore && resolveMissing != null) {
            preparing = true
            scope.launch {
                val resolved = runCatching { resolveMissing() }.getOrDefault(emptyList())
                preparing = false
                if (resolved.isNotEmpty()) {
                    if (needsConfirmation(missingTracks.size, resolved.size)) {
                        pendingConfirm = resolved
                    } else {
                        requestDownload(resolved.sumOf { it.durationMs }) {
                            onDownloadMissing(resolved)
                        }
                    }
                }
            }
        } else if (missingTracks.isNotEmpty()) {
            requestDownload(missingTracks.sumOf { it.durationMs }) {
                onDownloadMissing(missingTracks)
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = downloadMissing,
                enabled = !preparing,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            ) {
                if (preparing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.preparing))
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = when {
                            missingTracks.isEmpty() -> stringResource(R.string.download_missing)
                            hasMore -> stringResource(
                                R.string.download_missing_count_more,
                                missingTracks.size,
                            )
                            else -> stringResource(
                                R.string.download_missing_count,
                                missingTracks.size,
                            )
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (onDownloadAll != null) {
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = { requestDownload(totalDurationMs, onDownloadAll) },
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = stringResource(R.string.download_all_songs),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (estimateBytes > 0L) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = if (freeBytes > 0L) {
                    stringResource(
                        R.string.download_estimate,
                        estimateBytes.toFileSizeText(),
                        freeBytes.toFileSizeText()
                    )
                } else {
                    stringResource(R.string.download_estimate_only, estimateBytes.toFileSizeText())
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    pendingAction?.let { action ->
        SpowloDialog(
            onDismissRequest = {
                pendingAction = null
            },
            icon = { Icon(Icons.Outlined.Download, null) },
            title = { Text(stringResource(R.string.not_enough_space_title)) },
            text = {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(
                        R.string.not_enough_space_message,
                        pendingBytes.toFileSizeText(),
                        pendingFreeBytes.toFileSizeText()
                    )
                )
            },
            confirmButton = {
                ConfirmButton(text = stringResource(R.string.download_anyway)) {
                    pendingAction = null
                    action()
                }
            },
            dismissButton = {
                DismissButton {
                    pendingAction = null
                }
            }
        )
    }

    pendingConfirm?.let { resolved ->
        DownloadMissingDialog(
            count = resolved.size,
            bytes = DownloadSizeEstimator.estimateBytes(resolved.sumOf { it.durationMs }),
            onDismiss = { pendingConfirm = null },
            onConfirm = {
                pendingConfirm = null
                requestDownload(resolved.sumOf { it.durationMs }) {
                    onDownloadMissing(resolved)
                }
            },
        )
    }
}

@Composable
private fun DownloadMissingDialog(
    count: Int,
    bytes: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ActionDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Outlined.Download,
        title = stringResource(R.string.download_missing_confirm_title, count),
        supportingText = if (bytes > 0L) {
            stringResource(R.string.download_estimate_only, bytes.toFileSizeText())
        } else {
            null
        },
        confirmText = stringResource(R.string.download),
        onConfirm = onConfirm,
    )
}

private const val CONFIRM_MIN_TRACKS = 20

private fun needsConfirmation(shown: Int, resolved: Int) =
    resolved >= CONFIRM_MIN_TRACKS && resolved >= maxOf(shown, 1) * 2
