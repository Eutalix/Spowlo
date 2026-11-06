@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.bobbyesp.spowlo.ui.components.download_tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.AnchoredMenu
import com.bobbyesp.spowlo.ui.components.AnchoredMenuItem
import com.bobbyesp.spowlo.utils.toFileSizeText
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DownloadedSongRow(
    modifier: Modifier = Modifier,
    song: DownloadedSongInfo,
    fileSize: Long?,
    selectionMode: Boolean,
    selected: Boolean,
    isUpdatingMetadata: Boolean,
    metadataUpdateEnabled: Boolean,
    showDivider: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onCopyLink: () -> Unit,
    onShowLog: () -> Unit,
    onShare: () -> Unit,
    onOpenInSpotify: () -> Unit,
    onDelete: () -> Unit,
    onRedownload: () -> Unit,
    onUpdateMetadata: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isFileAvailable = fileSize == null || fileSize > 0L
    val fileSizeText = (fileSize ?: 0L).toFileSizeText()
    val fileFormat = song.songPath.substringAfterLast('.', "").uppercase()
    val artist = song.songAuthor.trim().ifBlank { stringResource(R.string.unknown_artist) }
    val formatLabel = formatWithBitrate(fileFormat, fileSize, song.songDuration)
    val rowColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.background
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    var menuExpanded by remember { mutableStateOf(false) }
    val unavailableLabel = stringResource(R.string.unavailable)
    val errorColor = MaterialTheme.colorScheme.error

    val dismissStateRef = remember { mutableStateOf<SwipeToDismissBoxState?>(null) }
    var rowWidth by remember { mutableFloatStateOf(0f) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.Settled) {
                true
            } else {
                // Ignore velocity-only flicks: the row has to actually travel most of the way.
                val offset = dismissStateRef.value
                    ?.let { runCatching { it.requireOffset() }.getOrNull() }
                offset != null && rowWidth > 0f && abs(offset) >= rowWidth * 0.6f
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.6f },
    )
    SideEffect { dismissStateRef.value = dismissState }
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onShare()
                dismissState.reset()
            }

            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismissState.reset()
            }

            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.onSizeChanged { rowWidth = it.width.toFloat() },
            enableDismissFromStartToEnd = !selectionMode,
            enableDismissFromEndToStart = !selectionMode,
            backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowColor)
                    .combinedClickable(
                        onClick = { if (selectionMode) onSelect() else onPlay() },
                        onClickLabel = stringResource(
                            if (selectionMode) R.string.multiselect_mode else R.string.play_file
                        ),
                        onLongClick = {
                            if (!selectionMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelect()
                            }
                        },
                        onLongClickLabel = stringResource(R.string.multiselect_mode)
                    )
                    .padding(start = 14.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = selectionMode,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    enter = expandHorizontally(animationSpec = tween(180)) + fadeIn(),
                    exit = shrinkHorizontally(animationSpec = tween(180)) + fadeOut(),
                ) {
                    Checkbox(
                        modifier = Modifier.padding(end = 6.dp),
                        checked = selected,
                        onCheckedChange = { onSelect() }
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    if (song.thumbnailUrl.isNotBlank()) {
                        AsyncImageImpl(
                            modifier = Modifier.fillMaxSize(),
                            model = song.thumbnailUrl,
                            contentDescription = stringResource(R.string.song_cover),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (isUpdatingMetadata) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.songName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildAnnotatedString {
                            append(artist)
                            append(" · ")
                            append(compactDuration(song.songDuration))
                            if (formatLabel.isNotBlank()) {
                                append(" · ")
                                append(formatLabel)
                            }
                            if (fileSize != null && fileSize > 0L) {
                                append(" · ")
                                append(fileSizeText)
                            }
                            if (!isFileAvailable) {
                                append(" · ")
                                withStyle(SpanStyle(color = errorColor)) {
                                    append(unavailableLabel)
                                }
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        ),
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AnimatedVisibility(
                    visible = !selectionMode,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    enter = expandHorizontally(animationSpec = tween(180)) + fadeIn(),
                    exit = shrinkHorizontally(animationSpec = tween(180)) + fadeOut(),
                ) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.show_more_actions),
                                tint = secondaryColor,
                            )
                        }
                        AnchoredMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            AnchoredMenuItem(
                                text = stringResource(R.string.open_in_spotify),
                                leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenInSpotify()
                                },
                                enabled = song.songUrl.isNotBlank()
                            )
                            AnchoredMenuItem(
                                text = stringResource(R.string.copy_link),
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                onClick = {
                                    menuExpanded = false
                                    onCopyLink()
                                },
                                enabled = song.songUrl.isNotBlank()
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            AnchoredMenuItem(
                                text = stringResource(R.string.share_file),
                                leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                },
                                enabled = isFileAvailable
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            AnchoredMenuItem(
                                text = stringResource(R.string.open_log),
                                leadingIcon = { Icon(Icons.Outlined.Terminal, null) },
                                onClick = {
                                    menuExpanded = false
                                    onShowLog()
                                }
                            )
                            AnchoredMenuItem(
                                text = stringResource(R.string.redownload),
                                leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                                onClick = {
                                    menuExpanded = false
                                    onRedownload()
                                }
                            )
                            AnchoredMenuItem(
                                text = stringResource(
                                    if (isUpdatingMetadata) R.string.updating_metadata
                                    else R.string.update_metadata
                                ),
                                leadingIcon = {
                                    if (isUpdatingMetadata) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Outlined.AutoAwesome, null)
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    onUpdateMetadata()
                                },
                                enabled = metadataUpdateEnabled
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            AnchoredMenuItem(
                                text = stringResource(R.string.delete),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                textColor = MaterialTheme.colorScheme.error,
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val isDelete = direction == SwipeToDismissBoxValue.EndToStart
    val container = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }
    val content = if (isDelete) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(container),
        contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            Icon(
                imageVector = if (isDelete) Icons.Outlined.DeleteOutline else Icons.Outlined.Share,
                contentDescription = null,
                tint = content,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .size(22.dp),
            )
        }
    }
}

private val BITRATE_STEPS = intArrayOf(64, 96, 128, 160, 192, 224, 256, 320)
private val LOSSY_EXTENSIONS = setOf("MP3", "M4A", "AAC", "OPUS", "OGG")

/**
 * Average bitrate is not stored, so derive it from size and duration and snap it to the
 * closest standard rate. Lossless formats keep just the format name.
 */
private fun formatWithBitrate(format: String, fileSize: Long?, duration: Double): String {
    if (format !in LOSSY_EXTENSIONS || fileSize == null || fileSize <= 0L || duration <= 0.0) {
        return format
    }
    val kbps = (fileSize * 8.0 / duration / 1000.0).roundToInt()
    val nearest = BITRATE_STEPS.minByOrNull { abs(it - kbps) } ?: return format
    return "$format $nearest"
}

/** Drops the leading zero from the minutes so the meta line stays on one row. */
private fun compactDuration(seconds: Double): String {
    val total = seconds.roundToInt().coerceAtLeast(0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}
