@file:OptIn(ExperimentalFoundationApi::class)

package com.bobbyesp.spowlo.ui.components.download_tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.FlatButtonChip
import com.bobbyesp.spowlo.ui.components.songs.CustomTag
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import com.bobbyesp.spowlo.utils.toFileSizeText

@Composable
fun DownloadedSongCard(
    modifier: Modifier = Modifier,
    song: DownloadedSongInfo,
    fileSize: Long?,
    selectionMode: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onCopyLink: () -> Unit,
    onShowLog: () -> Unit,
    onShare: () -> Unit,
    onOpenInSpotify: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isFileAvailable = fileSize == null || fileSize > 0L
    val fileSizeText = (fileSize ?: 0L).toFileSizeText()
    val fileFormat = song.songPath.substringAfterLast('.', "").uppercase()
        .ifBlank { song.extractor }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
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
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        shape = CardDefaults.shape,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (selectionMode) {
                Checkbox(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 4.dp),
                    checked = selected,
                    onCheckedChange = { onSelect() }
                )
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (song.thumbnailUrl.isNotBlank()) {
                    AsyncImageImpl(
                        modifier = Modifier.fillMaxSize(),
                        model = song.thumbnailUrl,
                        contentDescription = stringResource(R.string.song_cover),
                        contentScale = ContentScale.Crop
                    )
                }
                FilledIconButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(30.dp),
                    onClick = onPlay,
                    enabled = isFileAvailable,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(R.string.play_file)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp)
            ) {
                MarqueeText(
                    text = song.songName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (song.songAuthor.isNotBlank()) {
                    Text(
                        text = song.songAuthor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(enabled = song.songUrl.isNotBlank(), onClick = onCopyLink),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        text = song.songUrl.ifBlank { stringResource(R.string.song_url) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        modifier = Modifier.size(28.dp),
                        onClick = onCopyLink,
                        enabled = song.songUrl.isNotBlank()
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.copy_link)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomTag(
                        text = GeneralTextUtils.convertDuration(song.songDuration)
                    )
                    if (fileFormat.isNotBlank()) CustomTag(text = fileFormat)
                    if (fileSize != null && fileSize > 0L) CustomTag(text = fileSizeText)
                    if (!isFileAvailable) {
                        Text(
                            text = stringResource(R.string.unavailable),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.show_more_actions)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open_in_spotify)) },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) },
                        onClick = {
                            menuExpanded = false
                            onOpenInSpotify()
                        },
                        enabled = song.songUrl.isNotBlank()
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_file)) },
                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        },
                        enabled = isFileAvailable
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            if (!selectionMode) {
                FlatButtonChip(
                    icon = Icons.Outlined.PlayArrow,
                    label = stringResource(R.string.play_file),
                    onClick = onPlay
                )
                FlatButtonChip(
                    icon = Icons.Outlined.ContentCopy,
                    label = stringResource(R.string.copy_link),
                    onClick = onCopyLink
                )
                FlatButtonChip(
                    icon = Icons.Outlined.Terminal,
                    label = stringResource(R.string.open_log),
                    onClick = onShowLog
                )
                FlatButtonChip(
                    icon = Icons.Outlined.Share,
                    label = stringResource(R.string.share_file),
                    onClick = onShare
                )
            }
        }
    }
}
