package com.bobbyesp.spowlo.ui.components.songs.metadata_viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bobbyesp.spowlo.R
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
        horizontalAlignment = Alignment.End
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = downloadMissing, enabled = !preparing) {
                if (preparing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.preparing))
                } else {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
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
                        }
                    )
                }
            }
            if (onDownloadAll != null) Spacer(Modifier.width(8.dp))
            if (onDownloadAll != null) {
                FilledTonalIconButton(
                    onClick = { requestDownload(totalDurationMs, onDownloadAll) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.download_all_songs),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
        if (estimateBytes > 0L) {
            Text(
                modifier = Modifier.padding(top = 6.dp),
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
            icon = { Icon(Icons.Filled.Download, null) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadMissingDialog(
    count: Int,
    bytes: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.download_missing_confirm_title, count),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (bytes > 0L) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.download_estimate_only, bytes.toFileSizeText()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dismiss))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onConfirm) {
                        Text(stringResource(R.string.download))
                    }
                }
            }
        }
    }
}

private const val CONFIRM_MIN_TRACKS = 20

private fun needsConfirmation(shown: Int, resolved: Int) =
    resolved >= CONFIRM_MIN_TRACKS && resolved >= maxOf(shown, 1) * 2
