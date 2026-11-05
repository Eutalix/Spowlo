package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.ReleaseDate
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests
import com.bobbyesp.spowlo.ui.components.ActionDialog
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import com.bobbyesp.spowlo.utils.RedownloadUtil
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun TrackPage(
    data: Track,
    modifier: Modifier,
    onBack: () -> Unit,
    trackDownloadCallback: (String, String) -> Unit,
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    isDownloaded: Boolean = false,
) {
    var trackData by rememberSaveable(stateSaver = TrackSaver) {
        mutableStateOf(data)
    }

    LaunchedEffect(data.id) {
        if (trackData.id != data.id) trackData = data
    }

    val artists = trackData.artists.joinToString(", ") { it.name }
    val artworkUrl = trackData.album.images.firstOrNull()?.url.orEmpty()
    val seed = rememberArtworkSeed(artworkUrl)
    val spotifyUrl = trackData.externalUrls.spotify.orEmpty()
    val playCount = remember(trackData.id) { SpotifyApiRequests.getTrackPlayCount(trackData.id) }
    val releaseDate = formatReleaseDate(trackData.album.releaseDate)
    var showRedownloadDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MetadataScreen(
        topBarTitle = trackData.name,
        onBack = onBack,
        modifier = modifier,
        seed = seed,
        backdrop = { ArtworkBackdrop(artworkUrl) },
    ) { paddingValues, listState ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item {
                MetadataHero(
                    title = trackData.name,
                    artwork = {
                        if (artworkUrl.isNotEmpty()) CollectionArtwork(artworkUrl)
                    },
                ) {
                    MetadataArtistLine(
                        artists = trackData.artists,
                        onArtistClick = onOpenArtist,
                    )
                    MetadataTags(
                        listOf(
                            stringResource(R.string.track),
                            trackData.album.releaseDate?.year
                                ?.takeIf { it > 0 }
                                ?.toString()
                                .orEmpty(),
                            if (trackData.explicit) {
                                stringResource(R.string.explicit)
                            } else {
                                ""
                            },
                        )
                    ) {
                        if (isDownloaded) {
                            MetadataTag(
                                text = stringResource(R.string.downloaded),
                                icon = Icons.Filled.CheckCircle,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            if (isDownloaded) {
                                showRedownloadDialog = true
                            } else {
                                trackDownloadCallback(spotifyUrl, "$artists - ${trackData.name}")
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) {
                                Icons.Outlined.Refresh
                            } else {
                                Icons.Outlined.Download
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(
                                if (isDownloaded) R.string.redownload else R.string.download
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = { ChromeCustomTabsUtil.openUrl(spotifyUrl) },
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            modifier = Modifier.size(22.dp),
                            imageVector = ImageVector.vectorResource(id = R.drawable.spotify_logo),
                            contentDescription = stringResource(R.string.open_in_spotify),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = { GeneralTextUtils.copyToClipboardAndNotify(spotifyUrl) },
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.copy_link),
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetadataStat(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Timer,
                        value = GeneralTextUtils.convertDuration(trackData.durationMs.toDouble()),
                        label = stringResource(R.string.track_duration),
                    )
                    playCount?.let {
                        MetadataStat(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Headphones,
                            value = formatCompactCount(it),
                            label = stringResource(R.string.track_plays),
                        )
                    }
                }
            }

            item {
                MetadataSectionTitle(stringResource(R.string.track_details))
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MetadataDetailRow(
                        label = stringResource(R.string.album),
                        value = trackData.album.name,
                        onClick = { onOpenAlbum(trackData.album.id) },
                    )
                    if (releaseDate.isNotBlank()) {
                        MetadataDetailRow(
                            label = stringResource(R.string.release_date),
                            value = releaseDate,
                        )
                    }
                    if (trackData.trackNumber > 0) {
                        val total = trackData.album.totalTracks ?: 0
                        MetadataDetailRow(
                            label = stringResource(R.string.track_number),
                            value = if (total > 0) {
                                "${trackData.trackNumber} / $total"
                            } else {
                                trackData.trackNumber.toString()
                            },
                        )
                    }
                    if (trackData.discNumber > 1) {
                        MetadataDetailRow(
                            label = stringResource(R.string.disc_number),
                            value = trackData.discNumber.toString(),
                        )
                    }
                }
            }
        }
    }

    if (showRedownloadDialog) {
        ActionDialog(
            onDismissRequest = { showRedownloadDialog = false },
            icon = Icons.Outlined.Refresh,
            title = pluralStringResource(R.plurals.redownload_confirm_title, 1, 1),
            supportingText = pluralStringResource(R.plurals.redownload_confirm_message, 1),
            confirmText = stringResource(R.string.redownload),
            onConfirm = {
                showRedownloadDialog = false
                scope.launch(Dispatchers.IO) { RedownloadUtil.redownload(spotifyUrl, "$artists - ${trackData.name}") }
            },
        )
    }
}

private fun formatReleaseDate(date: ReleaseDate?): String {
    if (date == null || date.year <= 0) return ""
    val month = date.month?.takeIf { it in 1..12 } ?: return date.year.toString()
    val day = date.day?.takeIf { it in 1..31 }
    val localDate = runCatching { LocalDate.of(date.year, month, day ?: 1) }.getOrNull()
        ?: return date.year.toString()
    val formatter = if (day == null) {
        DateTimeFormatter.ofPattern("LLLL yyyy")
    } else {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
    return localDate.format(formatter.withLocale(Locale.getDefault()))
}

private fun formatCompactCount(count: Long): String = when {
    count >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", count / 1_000_000_000.0)
    count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 10_000 -> String.format(Locale.US, "%.0fK", count / 1_000.0)
    else -> count.toString()
}

@ExperimentalSerializationApi
object TrackSaver : Saver<Track, String> {
    override fun restore(value: String): Track {
        return Json.decodeFromString(value)
    }

    override fun SaverScope.save(value: Track): String {
        return Json.encodeToString(value)
    }
}

