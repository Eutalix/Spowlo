package com.bobbyesp.spowlo.ui.pages.downloader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.songs.SongCard

/**
 * The main screen of the application for downloading songs.
 * This Composable is stateless in nature, observing its state from the [DownloaderViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderPage(
    downloaderViewModel: DownloaderViewModel = hiltViewModel(),
    navigateToSettings: () -> Unit,
    navigateToDownloads: () -> Unit,
    navigateToTasks: () -> Unit,
    navigateToSearch: () -> Unit,
) {
    val uiState by downloaderViewModel.uiState.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    
    // When a URL is shared with the app, this effect will update the input field.
    LaunchedEffect(uiState.sharedUrl) {
        if (uiState.sharedUrl.isNotEmpty()) {
            urlInput = uiState.sharedUrl
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = navigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                actions = {
                    IconButton(onClick = navigateToSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.searcher))
                    }
                    IconButton(onClick = navigateToTasks) {
                        Icon(Icons.Filled.Terminal, contentDescription = stringResource(R.string.download_tasks))
                    }
                    IconButton(onClick = navigateToDownloads) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = stringResource(R.string.downloads_history))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Enter Spotify Track URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { downloaderViewModel.fetchTrackInfo(urlInput) },
                enabled = !uiState.isLoading && urlInput.isNotBlank()
            ) {
                Text("Fetch Track Info")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display a loading indicator when fetching metadata.
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            // Display an error message if fetching fails.
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }

            // Once a track is successfully fetched, display its information in a SongCard.
            uiState.spotifyTrack?.let { track ->
                Text("Track Found:", style = MaterialTheme.typography.titleMedium)
                // The SongCard is adapted to accept a `Track` object.
                // Clicking it will trigger the download via the ViewModel.
                SongCard(
                    track = track,
                    onClick = {
                        downloaderViewModel.startDownload(track)
                    }
                )
            }
        }
    }
}