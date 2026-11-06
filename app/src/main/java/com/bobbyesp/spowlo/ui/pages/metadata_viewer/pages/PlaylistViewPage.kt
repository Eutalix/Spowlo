package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.Playlist
import com.bobbyesp.spowlo.App
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.CollectionDownloadActions
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.TrackDownload
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil
import com.bobbyesp.spowlo.utils.GeneralTextUtils

@Composable
fun PlaylistViewPage(
    data: Playlist,
    modifier: Modifier,
    onBack: () -> Unit,
    trackDownloadCallback: (String, String) -> Unit,
    downloadedTrackIds: Set<String> = emptySet(),
    onDownloadTracks: (List<TrackDownload>) -> Unit = {},
    isLoadingTracks: Boolean = false,
    onOpenTrack: ((String) -> Unit)? = null,
) {
    val trackCount = data.tracks.total
    val artworkUrl = data.images.firstOrNull()?.url.orEmpty()
    val seed = rememberArtworkSeed(artworkUrl)
    val missingTracks = remember(data, downloadedTrackIds) {
        data.tracks.items.mapNotNull { item ->
            val track = item.track?.asTrack ?: return@mapNotNull null
            if (track.id in downloadedTrackIds) return@mapNotNull null
            TrackDownload(
                url = track.externalUrls.spotify ?: "https://open.spotify.com/track/${track.id}",
                title = track.name,
                artists = track.artists.joinToString(", ") { it.name },
                durationMs = track.durationMs.toLong(),
                albumName = track.album.name,
                coverUrl = track.album.images?.firstOrNull()?.url.orEmpty(),
                explicit = track.explicit,
                discNumber = track.discNumber,
                trackNumber = track.trackNumber,
            )
        }
    }
    val pendingDurationMs = remember(data, downloadedTrackIds) {
        data.tracks.items.mapNotNull { item ->
            item.track?.asTrack?.takeIf { it.id !in downloadedTrackIds }
        }.sumOf { it.durationMs.toLong() }
    }
    val totalDurationMs = remember(data) {
        data.tracks.items.mapNotNull { it.track?.asTrack }
            .sumOf { it.durationMs.toLong() }
    }
    val downloadedCount = remember(data, downloadedTrackIds) {
        data.tracks.items.count { item ->
            item.track?.asTrack?.id in downloadedTrackIds
        }
    }
    val downloadAll: (() -> Unit)? = data.externalUrls.spotify?.let { url ->
        { trackDownloadCallback(url, data.name) }
    }
    var descriptionExpanded by rememberSaveable(data.id) { mutableStateOf(false) }
    var descriptionOverflows by remember(data.id) { mutableStateOf(false) }

    MetadataScreen(
        topBarTitle = data.name,
        onBack = onBack,
        modifier = modifier,
        seed = seed,
        backdrop = { ArtworkBackdrop(artworkUrl) },
        actions = { MetadataOverflowMenu(data.externalUrls.spotify.orEmpty()) },
    ) { paddingValues, listState ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.Top,
        ) {
            item {
                MetadataHero(
                    title = data.name,
                    artwork = {
                        if (artworkUrl.isNotEmpty()) CollectionArtwork(artworkUrl, listState)
                    },
                ) {
                    MetadataOwnerLine(
                        name = data.owner.displayName ?: data.owner.id,
                        imageUrl = data.owner.images?.firstOrNull()?.url.orEmpty(),
                    )
                    MetadataMetaLine(
                        if ((data.followers.total ?: 0) > 0) {
                            "${GeneralTextUtils.formatCompactCount((data.followers.total ?: 0).toLong())} ${
                                App.context.getString(R.string.followers).lowercase()
                            }"
                        } else {
                            ""
                        },
                        if (trackCount > 0) {
                            pluralStringResource(R.plurals.song_count, trackCount, trackCount)
                        } else {
                            ""
                        },
                        GeneralTextUtils.formatCollectionDuration(totalDurationMs),
                    )
                    data.description?.takeIf { it.isNotBlank() }?.let { description ->
                        val linkColor = MaterialTheme.colorScheme.primary
                        Text(
                            text = remember(description, linkColor) {
                                AnnotatedString.fromHtml(
                                    description,
                                    linkStyles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = linkColor,
                                        )
                                    )
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = {
                                if (!descriptionExpanded) {
                                    descriptionOverflows = it.hasVisualOverflow
                                }
                            },
                        )
                        if (descriptionOverflows || descriptionExpanded) {
                            Text(
                                text = stringResource(
                                    if (descriptionExpanded) R.string.less else R.string.more
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { descriptionExpanded = !descriptionExpanded }
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            if (data.externalUrls.spotify != null || missingTracks.isNotEmpty()) {
                item {
                    CollectionDownloadActions(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        totalDurationMs = pendingDurationMs,
                        missingTracks = missingTracks,
                        onDownloadAll = downloadAll,
                        onDownloadMissing = onDownloadTracks,
                        downloadFullCollection = downloadedCount == 0,
                        idleLabel = stringResource(R.string.download_playlist_count, trackCount),
                    )
                }
            }
            item {
                MetadataSectionTitle(stringResource(R.string.tracks))
            }
            items(data.tracks.items.size) { index ->
                val track = data.tracks.items[index]
                val actualTrack = track.track?.asTrack
                val taskName = StringBuilder().append(actualTrack?.name).append(" - ")
                    .append(actualTrack?.artists?.joinToString(", ") { it.name }).toString()
                TrackComponent(
                    contentModifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    songName = actualTrack?.name ?: App.context.getString(R.string.unknown),
                    artists = actualTrack?.artists?.joinToString(", ") { it.name } ?: "",
                    spotifyUrl = actualTrack?.externalUrls?.spotify ?: "",
                    isExplicit = actualTrack?.explicit ?: false,
                    isDownloaded = actualTrack?.id?.let { it in downloadedTrackIds } == true,
                    imageUrl = actualTrack?.album?.images?.getOrNull(0)?.url ?: "",
                    onOpen = {
                        if (actualTrack != null) {
                            if (onOpenTrack != null) {
                                onOpenTrack(actualTrack.id)
                            } else {
                                ChromeCustomTabsUtil.openUrl(actualTrack.externalUrls.spotify.orEmpty())
                            }
                        }
                    },
                    onDownload = {
                        if (actualTrack != null) {
                            trackDownloadCallback(
                                actualTrack.externalUrls.spotify ?: "",
                                taskName,
                            )
                        }
                    },
                )
            }
            if (isLoadingTracks) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
