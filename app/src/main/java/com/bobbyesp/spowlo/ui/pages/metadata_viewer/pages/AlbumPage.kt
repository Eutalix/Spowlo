package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.SimpleTrack
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.HorizontalDivider
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.binders.dataStringToString

/**
 * A Composable that displays the detailed view of a Spotify Album,
 * including its artwork, metadata, and a list of its tracks.
 */
@Composable
fun AlbumPage(
    data: Album,
    modifier: Modifier,
    onDownloadTrack: (SimpleTrack) -> Unit,
    onDownloadCollection: () -> Unit
) {
    val localConfig = LocalConfiguration.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // --- Album Header ---
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
                        contentDescription = stringResource(R.string.album_artwork),
                        contentScale = ContentScale.Crop,
                    )
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    SelectionContainer {
                        MarqueeText(
                            text = data.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SelectionContainer {
                        Text(
                            text = data.artists.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.alpha(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SelectionContainer {
                        Text(
                            text = dataStringToString(
                                data = data.type, additional = data.releaseDate.year.toString(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alpha(alpha = 0.8f)
                        )
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
                            contentDescription = "Download full album",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        // --- Track List ---
        if (data.tracks.isNotEmpty()) {
            items(data.tracks.items, key = { it.id }) { track ->
                TrackComponent(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    songName = track.name,
                    artists = track.artists.joinToString(", ") { it.name },
                    isExplicit = track.explicit,
                    onClick = { onDownloadTrack(track) } // UPDATED: Calls the new track download callback.
                )
            }
        }
    }
}