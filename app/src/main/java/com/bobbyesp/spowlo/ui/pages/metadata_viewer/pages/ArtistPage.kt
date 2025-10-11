package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.HorizontalDivider
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.ui.theme.harmonizeWithPrimary
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A Composable that displays the detailed view of a Spotify Artist,
 * including their artwork, metadata, and a list of their top tracks.
 */
@Composable
fun ArtistPage(
    data: Artist,
    modifier: Modifier,
    onDownloadTrack: (Track) -> Unit
) {
    val localConfig = LocalConfiguration.current
    var topTracks by remember { mutableStateOf<List<Track>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch the artist's top tracks when the composable enters the composition.
    LaunchedEffect(data.id) {
        isLoading = true
        topTracks = withContext(Dispatchers.IO) {
            SpotifyApiRequests.getArtistTopTracks(data.id)
        }
        isLoading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // --- Artist Header ---
        item {
            Column {
                data.images.firstOrNull()?.url?.let { imageUrl ->
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
                            model = imageUrl,
                            contentDescription = stringResource(R.string.artist_artwork),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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
                            text = "${stringResource(R.string.artist_followers)}: ${data.followers.total}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alpha(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SelectionContainer {
                        Text(
                            text = "${stringResource(R.string.artist_popularity)}: ${data.popularity}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alpha(alpha = 0.8f)
                        )
                    }
                    
                    data.externalUrls.spotify?.let { url ->
                        FilledTonalButton(
                            modifier = Modifier.padding(top = 12.dp),
                            onClick = { ChromeCustomTabsUtil.openUrl(url) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(red = 30, green = 215, blue = 96).harmonizeWithPrimary(),
                            ),
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = ImageVector.vectorResource(id = R.drawable.spotify_logo),
                                contentDescription = "Open artist in Spotify"
                            )
                        }
                    }
                }
            }
        }

        // --- Top Tracks Section ---
        item {
            Text(
                text = stringResource(R.string.artist_top_tracks),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            topTracks?.let { tracks ->
                items(tracks, key = { it.id }) { track ->
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
}