package com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.model.SpotifyDataType
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.pages.common_pages.ErrorPage
import com.bobbyesp.spowlo.ui.pages.common_pages.LoadingPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.binders.typeOfSpotifyDataType
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.AlbumPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.ArtistPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.PlaylistViewPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages.TrackPage

/**
 * A Composable screen that acts as a router to display the correct detail page
 * for a given Spotify item (Track, Album, Playlist, or Artist).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyItemPage(
    onBackPressed: () -> Unit,
    playlistPageViewModel: PlaylistPageViewModel = hiltViewModel(),
    id: String,
    type: String,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val viewState by playlistPageViewModel.viewState.collectAsStateWithLifecycle()

    // Trigger the data loading when the Composable enters the composition
    // or when the `id` or `type` changes.
    LaunchedEffect(id, type) {
        playlistPageViewModel.loadData(id, typeOfSpotifyDataType(type))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.metadata_viewer)) },
                navigationIcon = { BackButton(onClick = onBackPressed) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        val pageModifier = Modifier.fillMaxSize().padding(paddingValues)

        // Render the UI based on the current loading state.
        when (val state = viewState.state) {
            is PlaylistDataState.Loading -> {
                LoadingPage()
            }
            is PlaylistDataState.Error -> {
                ErrorPage(
                    onReload = { playlistPageViewModel.loadData(id, typeOfSpotifyDataType(type)) },
                    exception = state.error.message ?: "Unknown error",
                    modifier = pageModifier
                )
            }
            is PlaylistDataState.Loaded -> {
                // When data is loaded, determine which detail page to show.
                when (val data = state.data) {
                    is Album -> AlbumPage(
                        data = data,
                        modifier = pageModifier,
                        onDownloadTrack = { track -> playlistPageViewModel.downloadTrack(track) },
                        onDownloadCollection = { playlistPageViewModel.downloadCollection(data) }
                    )
                    is Artist -> ArtistPage(
                        data = data,
                        modifier = pageModifier,
                        onDownloadTrack = { track -> playlistPageViewModel.downloadTrack(track) }
                    )
                    is Playlist -> PlaylistViewPage(
                        data = data,
                        modifier = pageModifier,
                        onDownloadTrack = { track -> playlistPageViewModel.downloadTrack(track) },
                        onDownloadCollection = { playlistPageViewModel.downloadCollection(data) }
                    )
                    is Track -> TrackPage(
                        data = data,
                        modifier = pageModifier,
                        onDownloadTrack = { track -> playlistPageViewModel.downloadTrack(track) }
                    )
                    else -> {
                        // Handle unexpected data type.
                        ErrorPage(
                            onReload = { playlistPageViewModel.loadData(id, typeOfSpotifyDataType(type)) },
                            exception = "Unsupported data type: ${data::class.java.simpleName}",
                            modifier = pageModifier
                        )
                    }
                }
            }
        }
    }
}