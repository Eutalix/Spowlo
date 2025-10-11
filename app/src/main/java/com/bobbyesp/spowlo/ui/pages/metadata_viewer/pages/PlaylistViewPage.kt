package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.PlaylistTrack
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.HorizontalDivider
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.binders.dataStringToString

/**
 * A Composable that displays the detailed view of a Spotify Playlist,
 * including its artwork, metadata, description, and list of tracks.
 */
@Composable
fun PlaylistViewPage(
    data: Playlist,
    modifier: Modifier,
    onDownloadTrack: (PlaylistTrack) -> Unit,
    onDownloadCollection: () -> Unit
) {
    val localConfig = LocalConfiguration.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // --- Playlist Header ---
        item {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val imageSize = (localConfig.screenWidthDp * 0.7f).dp
                    AsyncImageImpl(
                        modifier = Modifier
                            .size(imageSize)
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.medium),
                        model = data.images.firstOrNull()?.url ?: "",
                        contentDescription = stringResource(R.string.playlist_cover),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SelectionContainer {
                        MarqueeText(
                            text = data.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    SelectionContainer {
                        Text(
                            text = data.owner.displayName ?: data.owner.id,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.alpha(alpha = 0.8f)
                        )
                    }
                    SelectionContainer {
                        Text(
                            text = dataStringToString(
                                data = data.type,
                                additional = "${data.followers.total} ${stringResource(R.string.followers).lowercase()}"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alpha(alpha = 0.8f)
                        )
                    }
                    data.description?.let {
                        if (it.isNotEmpty()) {
                            SelectionContainer {
                                Text(
                                    text = it, // HTML parsing can be added here if needed
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.alpha(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // --- "Download All" Button ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onDownloadCollection, // UPDATED: Calls the new collection download callback.
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download full playlist",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        // --- Track List ---
        if (data.tracks.isNotEmpty()) {
            items(data.tracks.items, key = { it.track?.id ?: it.hashCode().toString() }) { playlistTrack ->
                // A playlist track can sometimes be null or a local file. We only show valid web tracks.
                playlistTrack.track?.asTrack?.let { track ->
                    TrackComponent(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        songName = track.name,
                        artists = track.artists.joinToString(", ") { it.name },
                        isExplicit = track.explicit,
                        isPlaylist = true,
                        imageUrl = track.album.images.firstOrNull()?.url ?: "",
                        onClick = { onDownloadTrack(playlistTrack) } // UPDATED: Calls the new track download callback.
                    )
                }
            }
        }

        // Footer note for playlists that might be incomplete.
        item {
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(R.string.uncomplete_playlist),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}