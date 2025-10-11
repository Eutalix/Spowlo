package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.AudioFeatures
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.HorizontalDivider
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.ExtraInfoCard
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.binders.dataStringToString
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A Composable that displays a detailed view of a single Spotify Track,
 * including its artwork, metadata, and audio features.
 */
@Composable
fun TrackPage(
    data: Track,
    modifier: Modifier,
    onDownloadTrack: (Track) -> Unit,
) {
    val localConfig = LocalConfiguration.current
    var audioFeatures by remember { mutableStateOf<AudioFeatures?>(null) }
    var isLoadingFeatures by remember { mutableStateOf(true) }

    // Fetch the track's audio features when the composable enters the composition.
    LaunchedEffect(data.id) {
        isLoadingFeatures = true
        audioFeatures = withContext(Dispatchers.IO) {
            SpotifyApiRequests.getAudioFeatures(data.id)
        }
        isLoadingFeatures = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // --- Track Header ---
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
                        model = data.album.images.firstOrNull()?.url ?: "",
                        contentDescription = stringResource(R.string.track_artwork),
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
                                data = data.type,
                                additional = data.album.releaseDate?.year?.toString() ?: ""
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alpha(alpha = 0.8f)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
            }
        }

        // --- Download Action ---
        item {
            TrackComponent(
                modifier = Modifier.padding(horizontal = 16.dp),
                songName = data.name,
                artists = data.artists.joinToString(", ") { it.name },
                spotifyUrl = data.externalUrls.spotify ?: "",
                isExplicit = data.explicit,
                onClick = { onDownloadTrack(data) } // UPDATED: Calls the correct download callback.
            )
        }

        // --- Audio Features Section ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoadingFeatures) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        ExtraInfoCard(
                            headlineText = stringResource(R.string.track_popularity),
                            bodyText = data.popularity?.toString() ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ExtraInfoCard(
                            headlineText = stringResource(R.string.track_duration),
                            bodyText = GeneralTextUtils.convertDuration(data.durationMs.toDouble()),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Show additional audio features once they are loaded.
                AnimatedVisibility(visible = audioFeatures != null) {
                    audioFeatures?.let { features ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            ExtraInfoCard(
                                headlineText = stringResource(R.string.loudness),
                                bodyText = "${features.loudness} dB",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            ExtraInfoCard(
                                headlineText = stringResource(R.string.tempo),
                                bodyText = "${features.tempo.toInt()} BPM",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}