package com.bobbyesp.spowlo.ui.pages.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.HomeCard
import com.bobbyesp.spowlo.ui.components.BackButton

/** Full listing behind a home shelf's "Show all". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryShelfPage(
    shelfId: String,
    onBackPressed: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenTrack: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    viewModel: UserLibraryViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? UserLibraryState.Ready
    val shelves = if (ready != null) rememberLibraryShelves(ready) else emptyList()
    val shelf = shelves.firstOrNull { it.id == shelfId }
    val profileName = ready?.profile?.name.orEmpty()
    val onOpenCard: (HomeCard) -> Unit = remember(onOpenPlaylist, onOpenTrack, onOpenAlbum, onOpenArtist) {
        { card -> card.open(onOpenPlaylist, onOpenTrack, onOpenAlbum, onOpenArtist) }
    }

    // The shelf can disappear when the feed is refreshed or the account signs out.
    LaunchedEffect(state, shelf) {
        if (state is UserLibraryState.Error || state is UserLibraryState.LoggedOut) {
            onBackPressed()
        } else if (ready != null && shelf == null) {
            onBackPressed()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = shelf?.title ?: stringResource(R.string.your_library),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = { BackButton(onBackPressed) }
            )
        }
    ) { paddingValues ->
        if (shelf == null) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(shelf.cards, key = { "${it.type}-${it.id}" }) { card ->
                    HomeCardItem(
                        card = card,
                        profileName = profileName,
                        modifier = Modifier.fillMaxWidth(),
                        titleStyle = MaterialTheme.typography.bodyLarge,
                        onClick = { onOpenCard(card) }
                    )
                }
            }
        }
    }
}
