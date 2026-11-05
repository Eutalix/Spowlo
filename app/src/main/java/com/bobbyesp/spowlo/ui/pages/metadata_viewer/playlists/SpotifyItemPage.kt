package com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.model.SpotifyDataType
import com.bobbyesp.spowlo.ui.pages.common_pages.LoadingPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.binders.typeOfSpotifyDataType
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.AlbumPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.ArtistPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.MetadataScreen
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.PlaylistViewPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.TrackPage

@Composable
fun SpotifyItemPage(
    onBackPressed: () -> Unit,
    playlistPageViewModel: PlaylistPageViewModel = viewModel(),
    id: String,
    type: String,
    account: Boolean = false,
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenTrack: ((String) -> Unit)? = null,
) {
    val viewState by playlistPageViewModel.viewStateFlow.collectAsStateWithLifecycle()
    val downloadedTrackIds by playlistPageViewModel.downloadedTrackIds.collectAsStateWithLifecycle()

    LaunchedEffect(id, account) {
        playlistPageViewModel.loadData(id, typeOfSpotifyDataType(type), account)
    }

    with(viewState) {
        when (this.state) {
            is PlaylistDataState.Loading -> {
                LoadingPage()
            }

            is PlaylistDataState.Error -> {
                val message = this.state.error.message.toString()
                MetadataScreen(
                    topBarTitle = stringResource(R.string.metadata_viewer),
                    onBack = onBackPressed,
                    backdrop = {},
                ) { paddingValues, _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            is PlaylistDataState.Loaded -> {
                val stateData = this.state.data

                val pageModifier = Modifier.fillMaxSize()

                val trackDownloadCallback: (url: String, name: String) -> Unit = { url, name ->
                    playlistPageViewModel.downloadTrack(url, name)
                }

                val downloadTracksCallback: (List<TrackDownload>) -> Unit = { tracks ->
                    playlistPageViewModel.downloadTracks(tracks)
                }

                with(stateData) {
                    when (typeOfSpotifyDataType(type)) {
                        SpotifyDataType.ALBUM -> {
                            val album = this as? Album
                            album?.let {
                                AlbumPage(
                                    data = it,
                                    modifier = pageModifier,
                                    onBack = onBackPressed,
                                    trackDownloadCallback = trackDownloadCallback,
                                    downloadedTrackIds = downloadedTrackIds,
                                    onDownloadTracks = downloadTracksCallback,
                                    isLoadingTracks = viewState.isLoadingTracks,
                                    onOpenArtist = onOpenArtist,
                                    onOpenTrack = onOpenTrack,
                                )
                            }
                        }

                        SpotifyDataType.ARTIST -> {
                            val artist = this as? Artist
                            artist?.let {
                                ArtistPage(
                                    data = it,
                                    modifier = pageModifier,
                                    onBack = onBackPressed,
                                    trackDownloadCallback = trackDownloadCallback,
                                    downloadedTrackIds = downloadedTrackIds,
                                    onDownloadTracks = downloadTracksCallback,
                                    onOpenAlbum = onOpenAlbum,
                                    onOpenTrack = onOpenTrack,
                                )
                            }
                        }

                        SpotifyDataType.PLAYLIST -> {
                            val playlist = this as? Playlist
                            playlist?.let {
                                PlaylistViewPage(
                                    data = it,
                                    modifier = pageModifier,
                                    onBack = onBackPressed,
                                    trackDownloadCallback = trackDownloadCallback,
                                    downloadedTrackIds = downloadedTrackIds,
                                    onDownloadTracks = downloadTracksCallback,
                                    isLoadingTracks = viewState.isLoadingTracks,
                                    onOpenTrack = onOpenTrack,
                                )
                            }
                        }

                        SpotifyDataType.TRACK -> {
                            val track = this as? Track
                            track?.let {
                                TrackPage(
                                    data = it,
                                    modifier = pageModifier,
                                    onBack = onBackPressed,
                                    trackDownloadCallback = trackDownloadCallback,
                                    onOpenAlbum = onOpenAlbum,
                                    onOpenArtist = onOpenArtist,
                                    isDownloaded = it.id in downloadedTrackIds,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class PlaylistDataState {
    data object Loading : PlaylistDataState()
    class Error(val error: Exception) : PlaylistDataState()
    class Loaded(val data: Any) : PlaylistDataState()
}
