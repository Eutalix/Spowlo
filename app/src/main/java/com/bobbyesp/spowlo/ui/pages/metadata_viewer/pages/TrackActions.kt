package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.ui.components.BottomSheet
import com.bobbyesp.spowlo.ui.components.DownloadOptionChip
import com.bobbyesp.spowlo.ui.components.DownloadOptionLabel
import com.bobbyesp.spowlo.ui.components.DownloadOptionShape
import com.bobbyesp.spowlo.ui.components.DownloadPrimaryButton
import com.bobbyesp.spowlo.ui.components.DownloadSheetHeader
import com.bobbyesp.spowlo.ui.components.FORMAT_CHOICES
import com.bobbyesp.spowlo.ui.components.FORMAT_FLAC
import com.bobbyesp.spowlo.ui.components.QUALITY_CHOICES
import com.bobbyesp.spowlo.ui.components.QUALITY_ORIGINAL
import com.bobbyesp.spowlo.ui.icons.YouTubeMusic
import com.bobbyesp.spowlo.utils.DownloadSizeEstimator
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.toFileSizeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

internal val ActionShape = DownloadOptionShape

@Composable
internal fun DownloadActions(
    downloading: Boolean,
    progress: Float?,
    failed: Boolean,
    online: Boolean,
    urlAvailable: Boolean,
    formatIndex: Int,
    qualityIndex: Int,
    durationMs: Long,
    source: String,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onOpenOptions: () -> Unit,
    onChangeSource: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenSpotify: () -> Unit,
) {
    if (failed) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(R.string.download_failed_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = stringResource(R.string.download_failed_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }
        }
    }

    DownloadSplitButton(
        modifier = Modifier.fillMaxWidth(),
        downloading = downloading,
        progress = progress,
        enabled = online && urlAvailable,
        onDownload = onDownload,
        onCancel = onCancel,
        onOpenOptions = onOpenOptions,
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = if (!online) {
            stringResource(R.string.youre_offline)
        } else {
            stringResource(
                R.string.youll_get,
                whatYoullGet(formatIndex, qualityIndex, durationMs),
            )
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (!online) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = YouTubeMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.download_source, source),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onChangeSource,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text(
                text = stringResource(R.string.change),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }

    Spacer(Modifier.height(14.dp))
    LinkButtons(onOpenSpotify = onOpenSpotify, onCopyLink = onCopyLink)
}

@Composable
internal fun SavedActions(
    song: DownloadedSongInfo,
    track: Track,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onShowInFolder: () -> Unit,
    onDelete: () -> Unit,
    onRedownload: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenSpotify: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var detailsVersion by remember { mutableIntStateOf(0) }
    var details by remember(song.id) { mutableStateOf(SavedFileDetails()) }
    var fixingTags by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }

    LaunchedEffect(song.id, song.songPath, detailsVersion) {
        details = withContext(Dispatchers.IO) { readSavedFile(song) }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onPlay,
            shape = ActionShape,
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 54.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.play_file),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box {
            Surface(
                onClick = { showOverflow = true },
                shape = ActionShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(54.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.redownload)) },
                    onClick = {
                        showOverflow = false
                        onRedownload()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        showOverflow = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                )
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(SavedGreen),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.saved_to, folderLabel(song.songPath)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = savedDetailsLine(details, song.downloadedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FileActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Share,
            label = stringResource(R.string.share),
            onClick = onShare,
        )
        FileActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.FolderOpen,
            label = stringResource(R.string.show_in_folder),
            onClick = onShowInFolder,
        )
    }

    Spacer(Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        TagState(
            label = stringResource(R.string.tags),
            present = details.hasTags,
        )
        Spacer(Modifier.width(14.dp))
        TagState(
            label = stringResource(R.string.cover),
            present = details.hasArtwork,
        )
        Spacer(Modifier.width(14.dp))
        TagState(
            label = stringResource(R.string.lyrics),
            present = details.hasLyrics,
        )
        Spacer(Modifier.weight(1f))
        if (!details.hasTags && !song.songPath.startsWith("content://")) {
            if (fixingTags) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(
                    onClick = {
                        fixingTags = true
                        scope.launch {
                            val fixed = runCatching {
                                withContext(Dispatchers.IO) {
                                    Downloader.writeTrackTags(
                                        track,
                                        song.songPath,
                                        song.songName,
                                    )
                                }
                            }.getOrDefault(false)
                            fixingTags = false
                            detailsVersion++
                            ToastUtil.makeToastSuspend(
                                context.getString(
                                    if (fixed) R.string.tags_fixed else R.string.tags_fix_failed
                                )
                            )
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.fix_tags),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    LinkButtons(onOpenSpotify = onOpenSpotify, onCopyLink = onCopyLink)
}

@Composable
private fun DownloadSplitButton(
    modifier: Modifier = Modifier,
    downloading: Boolean,
    progress: Float?,
    enabled: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onOpenOptions: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = tween(durationMillis = 400),
        label = "downloadProgress",
    )
    val active = enabled || downloading
    val container = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = if (active) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(ActionShape)
            .background(container),
    ) {
        if (downloading && animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(content.copy(alpha = 0.16f)),
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = active) { if (downloading) onCancel() else onDownload() }
                    .padding(start = 18.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (downloading) {
                    if (animatedProgress <= 0f) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = content,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            tint = content,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (animatedProgress <= 0f) {
                            stringResource(R.string.preparing)
                        } else {
                            stringResource(
                                R.string.downloading_percent,
                                (animatedProgress * 100).roundToInt(),
                            )
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cancel_download),
                        tint = content,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.download),
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                    )
                }
            }
            if (!downloading) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(content.copy(alpha = 0.22f)),
                )
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .fillMaxHeight()
                        .clickable(enabled = active) { onOpenOptions() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.download_options),
                        tint = content,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FileActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = ActionShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.height(64.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TagState(label: String, present: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (present) Icons.Filled.Check else Icons.Outlined.Remove,
            contentDescription = null,
            tint = if (present) {
                SavedGreen
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            },
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LinkButtons(onOpenSpotify: () -> Unit, onCopyLink: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SecondaryActionButton(
            modifier = Modifier.weight(1f),
            icon = ImageVector.vectorResource(id = R.drawable.spotify_logo),
            label = stringResource(R.string.open_in_spotify),
            onClick = onOpenSpotify,
        )
        SecondaryActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.ContentCopy,
            label = stringResource(R.string.copy_link),
            onClick = onCopyLink,
        )
    }
}

@Composable
private fun SecondaryActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = ActionShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
        modifier = modifier.height(46.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadOptionsSheet(
    formatIndex: Int,
    qualityIndex: Int,
    durationMs: Long,
    providers: List<String>,
    sourceIndex: Int,
    onFormatChange: (Int) -> Unit,
    onQualityChange: (Int) -> Unit,
    onSourceChange: (Int) -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            DownloadSheetHeader(
                title = stringResource(R.string.download_options),
                subtitle = whatYoullGet(formatIndex, qualityIndex, durationMs),
            )

            DownloadOptionLabel(stringResource(R.string.format))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FORMAT_CHOICES.forEach { format ->
                    DownloadOptionChip(
                        label = PreferencesUtil.getAudioFormatDesc(format).uppercase(Locale.ROOT),
                        selected = formatIndex == format,
                        onClick = {
                            onFormatChange(format)
                            if (format == FORMAT_FLAC) onQualityChange(QUALITY_ORIGINAL)
                        },
                    )
                }
            }

            if (formatIndex != FORMAT_FLAC) {
                DownloadOptionLabel(stringResource(R.string.bitrate))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QUALITY_CHOICES.forEach { quality ->
                        DownloadOptionChip(
                            label = if (quality == QUALITY_ORIGINAL) {
                                stringResource(R.string.original)
                            } else {
                                PreferencesUtil.getAudioQualityDesc(quality)
                                    .removeSuffix(" kbps")
                            },
                            selected = qualityIndex == quality,
                            onClick = { onQualityChange(quality) },
                        )
                    }
                }
            }

            if (providers.size > 1) {
                DownloadOptionLabel(stringResource(R.string.download_source_label))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    providers.forEachIndexed { index, provider ->
                        DownloadOptionChip(
                            label = provider,
                            selected = sourceIndex == index,
                            onClick = { onSourceChange(index) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            DownloadPrimaryButton(
                text = stringResource(
                    R.string.download_with_estimate,
                    DownloadSizeEstimator.estimateBytes(durationMs, formatIndex, qualityIndex)
                        .toFileSizeText(),
                ),
                onClick = onDownload,
            )
        }
    }
}

@Composable
internal fun DetailDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    )
}

@Composable
internal fun whatYoullGet(formatIndex: Int, qualityIndex: Int, durationMs: Long): String {
    val format = PreferencesUtil.getAudioFormatDesc(formatIndex).uppercase(Locale.ROOT)
    val quality = when {
        formatIndex == FORMAT_FLAC -> "lossless"
        qualityIndex == QUALITY_ORIGINAL -> "original audio"
        qualityIndex == 0 -> "automatic bitrate"
        else -> PreferencesUtil.getAudioQualityDesc(qualityIndex)
    }
    val size = DownloadSizeEstimator.estimateBytes(durationMs, formatIndex, qualityIndex)
        .toFileSizeText()
    return "$format · $quality · about $size"
}
