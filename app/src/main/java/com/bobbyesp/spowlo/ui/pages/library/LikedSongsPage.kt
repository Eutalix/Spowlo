package com.bobbyesp.spowlo.ui.pages.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.CollectionDownloadActions
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.ArtworkShape
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.ColorBackdrop
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.MetadataHero
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.MetadataMetaLine
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.MetadataScreen
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.MetadataSectionTitle
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.rememberArtworkSize
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.TrackDownload
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil

@Composable
fun LikedSongsPage(
    onBackPressed: () -> Unit,
    onOpenTrack: ((String) -> Unit)? = null,
    viewModel: LikedSongsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloadedTrackIds by viewModel.downloadedTrackIds.collectAsStateWithLifecycle()

    val missingTracks = remember(state.tracks, downloadedTrackIds) {
        state.tracks.filter { it.id !in downloadedTrackIds }.map { track ->
            TrackDownload(
                url = track.url,
                title = track.name,
                artists = track.artists,
                durationMs = track.durationMs,
                albumName = track.albumName,
                coverUrl = track.imageUrl,
                explicit = track.explicit,
            )
        }
    }
    val hasMore = !state.endReached

    MetadataScreen(
        topBarTitle = stringResource(R.string.liked_songs),
        onBack = onBackPressed,
        seed = LikedSongsColor,
        backdrop = { ColorBackdrop(LikedSongsColor) },
    ) { paddingValues, listState ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                state.failed -> LibraryErrorContent(
                    message = stringResource(R.string.liked_songs_error),
                    onRetry = viewModel::loadMore
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 24.dp,
                    ),
                ) {
                    item(key = "header") {
                        MetadataHero(
                            title = stringResource(R.string.liked_songs),
                            artwork = { LikedSongsArtwork() },
                        ) {
                            if (state.total > 0) {
                                MetadataMetaLine(
                                    pluralStringResource(
                                        R.plurals.song_count,
                                        state.total,
                                        state.total
                                    )
                                )
                            }
                        }
                    }

                    if (missingTracks.isNotEmpty() || hasMore) {
                        item(key = "actions") {
                            CollectionDownloadActions(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                totalDurationMs = missingTracks.sumOf { it.durationMs },
                                missingTracks = missingTracks,
                                hasMore = hasMore,
                                resolveMissing = viewModel::missingTracks,
                                onDownloadAll = null,
                                onDownloadMissing = viewModel::downloadMissing,
                            )
                        }
                    }

                    item(key = "songs_title") {
                        MetadataSectionTitle(stringResource(R.string.tracks))
                    }

                    itemsIndexed(state.tracks, key = { _, track -> track.id }) { index, track ->
                        if (!state.endReached && index >= state.tracks.size - 3) {
                            LaunchedEffect(index) { viewModel.loadMore() }
                        }
                        TrackComponent(
                            contentModifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            songName = track.name,
                            artists = track.artists,
                            spotifyUrl = track.url,
                            isExplicit = track.explicit,
                            isDownloaded = track.id in downloadedTrackIds,
                            imageUrl = track.imageUrl,
                            onOpen = {
                                if (onOpenTrack != null) onOpenTrack(track.id)
                                else ChromeCustomTabsUtil.openUrl(track.url)
                            },
                            onDownload = {
                                viewModel.downloadTrack(
                                    track.url,
                                    "${track.name} - ${track.artists}"
                                )
                            }
                        )
                    }

                    if (state.isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedSongsArtwork() {
    val size = rememberArtworkSize()
    Box(
        modifier = Modifier
            .size(size)
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .shadow(
                elevation = 18.dp,
                shape = ArtworkShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f),
            )
            .clip(ArtworkShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6),
                        LikedSongsColor,
                        Color(0xFF2A0A6E),
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(64.dp)
        )
    }
}

private val LikedSongsColor = Color(0xFF450AF5)
