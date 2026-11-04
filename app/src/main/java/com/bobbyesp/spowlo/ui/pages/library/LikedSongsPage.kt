package com.bobbyesp.spowlo.ui.pages.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.HorizontalDivider
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.CollectionDownloadActions
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.TrackDownload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedSongsPage(
    onBackPressed: () -> Unit,
    viewModel: LikedSongsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloadedTrackIds by viewModel.downloadedTrackIds.collectAsStateWithLifecycle()

    val missingTracks = remember(state.tracks, downloadedTrackIds) {
        state.tracks.filter { it.id !in downloadedTrackIds }.map { track ->
            TrackDownload(
                url = track.url,
                name = "${track.name} - ${track.artists}",
                durationMs = track.durationMs,
            )
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.liked_songs), maxLines = 1)
                    if (state.total > 0) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.song_count,
                                state.total,
                                state.total
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = { BackButton(onBackPressed) }
        )
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                state.failed -> LibraryErrorContent(
                    message = stringResource(R.string.liked_songs_error),
                    onRetry = viewModel::loadMore
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (missingTracks.isNotEmpty()) {
                        item(key = "actions") {
                            CollectionDownloadActions(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                totalDurationMs = missingTracks.sumOf { it.durationMs },
                                missingTracks = missingTracks,
                                onDownloadAll = null,
                                onDownloadMissing = viewModel::downloadTracks,
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    itemsIndexed(state.tracks, key = { _, track -> track.id }) { index, track ->
                        if (!state.endReached && index >= state.tracks.size - 3) {
                            LaunchedEffect(index) { viewModel.loadMore() }
                        }
                        TrackComponent(
                            contentModifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            songName = track.name,
                            artists = track.artists,
                            spotifyUrl = track.url,
                            isExplicit = track.explicit,
                            isPlaylist = true,
                            isDownloaded = track.id in downloadedTrackIds,
                            imageUrl = track.imageUrl,
                            onClick = {
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
