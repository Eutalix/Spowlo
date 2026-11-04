package com.bobbyesp.spowlo.ui.pages.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.UserPlaylist
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.UserProfile
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.pages.searcher.SearchingResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLibraryPage(
    onBackPressed: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenLikedSongs: () -> Unit,
    viewModel: UserLibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLogin by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is UserLibraryState.Ready) showLogin = false
    }

    BackHandler(showLogin) {
        showLogin = false
        viewModel.finishLoginScreen()
    }

    if (showLogin) {
        SpotifyLoginPage(
            onClose = {
                showLogin = false
                viewModel.finishLoginScreen()
            },
            onPageLoaded = { viewModel.finishSignIn() },
        )
        return
    }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.your_library)) },
            navigationIcon = { BackButton(onBackPressed) }
        )
    }) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (val current = state) {
                UserLibraryState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                UserLibraryState.LoggedOut -> SignInPrompt(onSignIn = { showLogin = true })
                UserLibraryState.Error -> LibraryErrorContent(
                    message = stringResource(R.string.library_error),
                    onRetry = viewModel::refresh
                )

                is UserLibraryState.Ready -> LibraryContent(
                    state = current,
                    onOpenLikedSongs = onOpenLikedSongs,
                    onOpenPlaylist = onOpenPlaylist,
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    state: UserLibraryState.Ready,
    onOpenLikedSongs: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "account") { AccountHeader(state.profile) }
        item(key = "liked") { LikedSongsRow(state.likedCount, onOpenLikedSongs) }

        if (state.mixes.isNotEmpty()) {
            item(key = "mixes_header") { SectionTitle(stringResource(R.string.made_for_you)) }
            items(state.mixes, key = { "mix-${it.id}" }) { playlist ->
                PlaylistRow(playlist, onOpenPlaylist)
            }
        }

        item(key = "playlists_header") { SectionTitle(stringResource(R.string.your_playlists)) }

        if (state.playlists.isEmpty()) {
            item(key = "playlists_empty") {
                Text(
                    text = stringResource(R.string.no_playlists_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            items(state.playlists, key = { "playlist-${it.id}" }) { playlist ->
                PlaylistRow(playlist, onOpenPlaylist)
            }
        }
    }
}

@Composable
private fun AccountHeader(profile: UserProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (profile.imageUrl.isNotEmpty()) {
                AsyncImageImpl(
                    modifier = Modifier.fillMaxSize(),
                    model = profile.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null)
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LikedSongsRow(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(Color(0xFF450AF5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.liked_songs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = pluralStringResource(R.plurals.song_count, count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaylistRow(playlist: UserPlaylist, onOpenPlaylist: (String) -> Unit) {
    SearchingResult(
        modifier = Modifier.fillMaxWidth(),
        insideModifier = Modifier.padding(vertical = 4.dp),
        artworkUrl = playlist.imageUrl,
        name = playlist.name,
        artists = playlist.owner,
        type = stringResource(R.string.playlist),
        onClick = { onOpenPlaylist(playlist.id) },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SignInPrompt(onSignIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.library_sign_in_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSignIn) {
            Text(stringResource(R.string.sign_in_with_spotify))
        }
    }
}

@Composable
internal fun LibraryErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
