package com.bobbyesp.spowlo.ui.components.songs.metadata_viewer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.ActionDialog
import com.bobbyesp.spowlo.ui.components.AnchoredMenu
import com.bobbyesp.spowlo.ui.components.AnchoredMenuItem
import com.bobbyesp.spowlo.ui.components.songs.ExplicitIcon
import com.bobbyesp.spowlo.ui.components.songs.LyricsIcon
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil
import com.bobbyesp.spowlo.utils.RedownloadUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackComponent(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    songName: String,
    artists: String,
    spotifyUrl: String,
    hasLyrics: Boolean = false,
    isExplicit: Boolean = false,
    isDownloaded: Boolean = false,
    imageUrl: String = "",
    trackNumber: String? = null,
    subtitle: String? = null,
    subtitleTrailing: String? = null,
    onOpen: () -> Unit = { ChromeCustomTabsUtil.openUrl(spotifyUrl) },
    onDownload: (() -> Unit)? = null,
) {
    val clipboardManager = LocalClipboardManager.current
    var showDropdown by remember { mutableStateOf(false) }
    var showRedownloadDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .then(contentModifier)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onOpen)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (trackNumber != null) {
                Box(
                    modifier = Modifier.width(30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = trackNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            } else if (imageUrl.isNotEmpty()) {
                AsyncImageImpl(
                    modifier = Modifier
                        .size(48.dp)
                        .aspectRatio(
                            1f, matchHeightConstraintsFirst = true
                        )
                        .clip(MaterialTheme.shapes.small),
                    model = imageUrl,
                    contentDescription = stringResource(id = R.string.track_artwork),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (trackNumber != null || imageUrl.isNotEmpty()) 10.dp else 0.dp,
                        end = 6.dp,
                        top = 6.dp,
                        bottom = 6.dp,
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarqueeText(
                        text = songName,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (isDownloaded && onDownload == null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(id = R.string.downloaded),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle ?: artists,
                        modifier = Modifier.weight(1f, fill = false),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                    )
                    if (!subtitleTrailing.isNullOrBlank()) {
                        Text(
                            text = "·",
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = subtitleTrailing,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    LyricsIcon(visible = hasLyrics)
                    Spacer(modifier = Modifier.width(6.dp))
                    ExplicitIcon(visible = isExplicit)
                }
            }
        }

        if (onDownload != null) {
            val downloadKey = remember(spotifyUrl) {
                Downloader.makeKey(spotifyUrl, spotifyUrl.reversed())
            }
            val taskState = Downloader.mutableTaskList[downloadKey]?.state
            var requested by remember(spotifyUrl) { mutableStateOf(false) }

            LaunchedEffect(requested, taskState) {
                if (!requested) return@LaunchedEffect
                if (taskState != null && taskState !is Downloader.DownloadTask.State.Running) {
                    requested = false
                } else if (taskState == null) {
                    // the task is only created after the metadata lookup, give up if it never starts
                    delay(15_000)
                    if (Downloader.mutableTaskList[downloadKey] == null) requested = false
                }
            }

            val progress = (taskState as? Downloader.DownloadTask.State.Running)?.progress ?: 0f
            val downloadState = when {
                isDownloaded || taskState is Downloader.DownloadTask.State.Completed ->
                    RowDownloadState.Downloaded
                requested || taskState is Downloader.DownloadTask.State.Running ->
                    RowDownloadState.Downloading
                else -> RowDownloadState.Idle
            }

            IconButton(
                onClick = {
                    when (downloadState) {
                        RowDownloadState.Downloaded -> showRedownloadDialog = true
                        RowDownloadState.Downloading -> Unit
                        RowDownloadState.Idle -> {
                            requested = true
                            onDownload()
                        }
                    }
                },
                enabled = downloadState != RowDownloadState.Downloading,
                modifier = Modifier.size(40.dp),
            ) {
                AnimatedContent(
                    targetState = downloadState,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.6f)) togetherWith
                            (fadeOut(tween(120)) + scaleOut(targetScale = 0.6f))
                    },
                    label = "rowDownloadState",
                ) { state ->
                    when (state) {
                        RowDownloadState.Downloaded -> Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(id = R.string.redownload),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )

                        RowDownloadState.Downloading -> if (progress > 0.01f) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            )
                        }

                        RowDownloadState.Idle -> Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = stringResource(id = R.string.download),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Box {
            IconButton(
                onClick = { showDropdown = true },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(id = R.string.more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            AnchoredMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                AnchoredMenuItem(
                    text = stringResource(id = R.string.open_in_spotify),
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                    onClick = {
                        showDropdown = false
                        ChromeCustomTabsUtil.openUrl(spotifyUrl)
                    },
                )
                AnchoredMenuItem(
                    text = stringResource(id = R.string.copy_link),
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                    onClick = {
                        showDropdown = false
                        clipboardManager.setText(AnnotatedString(spotifyUrl))
                    },
                )
            }
        }
    }

    if (showRedownloadDialog) {
        ActionDialog(
            onDismissRequest = { showRedownloadDialog = false },
            icon = Icons.Outlined.Refresh,
            title = pluralStringResource(R.plurals.redownload_confirm_title, 1, 1),
            supportingText = pluralStringResource(R.plurals.redownload_confirm_message, 1),
            confirmText = stringResource(id = R.string.redownload),
            onConfirm = {
                showRedownloadDialog = false
                scope.launch(Dispatchers.IO) { RedownloadUtil.redownload(spotifyUrl, "$songName - $artists") }
            },
        )
    }
}

private enum class RowDownloadState { Idle, Downloading, Downloaded }
