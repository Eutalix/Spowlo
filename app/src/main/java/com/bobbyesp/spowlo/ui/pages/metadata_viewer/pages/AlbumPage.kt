package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.Album
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.CollectionDownloadActions
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.TrackDownload
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil
import com.bobbyesp.spowlo.utils.GeneralTextUtils

@Composable
fun AlbumPage(
    data: Album,
    modifier: Modifier,
    onBack: () -> Unit,
    trackDownloadCallback: (String, String) -> Unit,
    downloadedTrackIds: Set<String> = emptySet(),
    onDownloadTracks: (List<TrackDownload>) -> Unit = {},
    isLoadingTracks: Boolean = false,
    onOpenArtist: (String) -> Unit = {},
    onOpenTrack: ((String) -> Unit)? = null,
) {
    val trackCount = maxOf(data.totalTracks, data.tracks.total)
    val artworkUrl = data.images.firstOrNull()?.url.orEmpty()
    val seed = rememberArtworkSeed(artworkUrl)
    val missingTracks = data.tracks.items.mapNotNull { track ->
        if (track.id in downloadedTrackIds) return@mapNotNull null
        TrackDownload(
            url = track.externalUrls.spotify ?: "https://open.spotify.com/track/${track.id}",
            title = track.name,
            artists = track.artists.joinToString(", ") { it.name },
            durationMs = track.durationMs.toLong(),
            albumName = data.name,
            coverUrl = artworkUrl,
            explicit = track.explicit,
            discNumber = track.discNumber,
            trackNumber = track.trackNumber,
        )
    }
    val pendingDurationMs = data.tracks.items
        .filter { it.id !in downloadedTrackIds }
        .sumOf { it.durationMs.toLong() }
    val totalDurationMs = data.tracks.items.sumOf { it.durationMs.toLong() }
    val downloadedCount = data.tracks.items.count { it.id in downloadedTrackIds }
    val downloadAll: (() -> Unit)? = data.externalUrls.spotify?.let { url ->
        { trackDownloadCallback(url, data.name) }
    }

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
                    MetadataArtistLine(
                        artists = data.artists,
                        onArtistClick = onOpenArtist,
                    )
                    MetadataMetaLine(
                        data.releaseDate.year.takeIf { it > 0 }?.toString().orEmpty(),
                        if (trackCount > 0) {
                            pluralStringResource(R.plurals.song_count, trackCount, trackCount)
                        } else {
                            ""
                        },
                        GeneralTextUtils.formatCollectionDuration(totalDurationMs),
                    )
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
                        idleLabel = stringResource(R.string.download_album_count, trackCount),
                    )
                }
            }
            item {
                MetadataSectionTitle(stringResource(R.string.tracks))
            }
            items(data.tracks.items.size) { index ->
                val track = data.tracks.items[index]
                val taskName = StringBuilder().append(track.name).append(" - ").append(
                    track.artists.joinToString(", ") { it.name }).toString()
                TrackComponent(
                    contentModifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    songName = track.name,
                    artists = track.artists.joinToString(", ") { it.name },
                    spotifyUrl = track.externalUrls.spotify ?: "",
                    isExplicit = track.explicit,
                    isDownloaded = track.id in downloadedTrackIds,
                    trackNumber = (index + 1).toString(),
                    onOpen = {
                        if (onOpenTrack != null) {
                            onOpenTrack(track.id)
                        } else {
                            ChromeCustomTabsUtil.openUrl(track.externalUrls.spotify.orEmpty())
                        }
                    },
                    onDownload = {
                        trackDownloadCallback(
                            track.externalUrls.spotify ?: "",
                            taskName,
                        )
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
