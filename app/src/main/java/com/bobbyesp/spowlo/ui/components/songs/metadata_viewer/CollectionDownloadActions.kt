package com.bobbyesp.spowlo.ui.components.songs.metadata_viewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.ConfirmButton
import com.bobbyesp.spowlo.ui.components.DismissButton
import com.bobbyesp.spowlo.ui.components.SpowloDialog
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.TrackDownload
import com.bobbyesp.spowlo.utils.DownloadSizeEstimator
import com.bobbyesp.spowlo.utils.toFileSizeText

@Composable
fun CollectionDownloadActions(
    totalDurationMs: Long,
    missingTracks: List<TrackDownload>,
    onDownloadAll: (() -> Unit)?,
    onDownloadMissing: (List<TrackDownload>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estimateBytes = DownloadSizeEstimator.estimateBytes(totalDurationMs)
    val freeBytes = remember(totalDurationMs) { DownloadSizeEstimator.freeBytes() }

    var pendingBytes by remember { mutableStateOf(0L) }
    var pendingFreeBytes by remember { mutableStateOf(0L) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (missingTracks.isNotEmpty()) {
                FilledTonalButton(onClick = {
                    requestDownload(missingTracks.sumOf { it.durationMs }) {
                        onDownloadMissing(missingTracks)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.download_missing_count, missingTracks.size))
                }
                if (onDownloadAll != null) Spacer(Modifier.width(8.dp))
            }
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
}
