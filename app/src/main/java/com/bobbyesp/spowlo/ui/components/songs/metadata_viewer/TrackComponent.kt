package com.bobbyesp.spowlo.ui.components.songs.metadata_viewer

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.PopupProperties
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.ActionDialog
import com.bobbyesp.spowlo.ui.components.songs.ExplicitIcon
import com.bobbyesp.spowlo.ui.components.songs.LyricsIcon
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.ui.pages.settings.about.LocalAsset
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil
import com.bobbyesp.spowlo.utils.RedownloadUtil
import kotlinx.coroutines.Dispatchers
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
            if (imageUrl.isNotEmpty()) {
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
                    .padding(6.dp)
                    .padding(start = if (imageUrl.isNotEmpty()) 10.dp else 0.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    MarqueeText(
                        text = songName,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        basicGradientColor = MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.8f
                        ),
                    )
                    if (isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(id = R.string.downloaded),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    MarqueeText(
                        text = artists,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        basicGradientColor = MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.8f
                        ),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LyricsIcon(visible = hasLyrics)
                    Spacer(modifier = Modifier.width(6.dp))
                    ExplicitIcon(visible = isExplicit)
                }
            }
        }

        if (onDownload != null) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isDownloaded) {
                    IconButton(
                        onClick = { showRedownloadDialog = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(id = R.string.redownload),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                } else {
                    FilledTonalIconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = stringResource(id = R.string.download),
                            modifier = Modifier.size(20.dp),
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
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.more_options),
                    modifier = Modifier.size(20.dp),
                )
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                properties = PopupProperties(
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true,
                    focusable = true,
                ),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(id = R.string.open_in_spotify))
                    }, onClick = {
                        showDropdown = false
                        ChromeCustomTabsUtil.openUrl(spotifyUrl)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = LocalAsset(id = R.drawable.spotify_logo),
                            contentDescription = null,
                        )
                    }
                )

                DropdownMenuItem(
                    onClick = {
                        showDropdown = false
                        clipboardManager.setText(AnnotatedString(spotifyUrl))
                    },
                    text = {
                        Text(text = stringResource(id = R.string.copy_link))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                        )
                    }
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
