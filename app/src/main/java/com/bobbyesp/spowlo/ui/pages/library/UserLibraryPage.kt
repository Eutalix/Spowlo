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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.HomeCard
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.HomeCardType
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.HomeShelf
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.UserProfile
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.components.BackButton

private val ShelfArtworkShape = RoundedCornerShape(10.dp)
private val LikedSongsBrush = Brush.linearGradient(
    listOf(Color(0xFF7C5CFF), Color(0xFF4A1FD6))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLibraryPage(
    onBackPressed: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenLikedSongs: () -> Unit,
    onOpenShelf: (String) -> Unit,
    onOpenTrack: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
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
                    onOpenShelf = onOpenShelf,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenTrack = onOpenTrack,
                    onOpenAlbum = onOpenAlbum,
                    onOpenArtist = onOpenArtist,
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    state: UserLibraryState.Ready,
    onOpenLikedSongs: () -> Unit,
    onOpenShelf: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenTrack: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val shelves = rememberLibraryShelves(state)
    val onOpenCard: (HomeCard) -> Unit = remember(onOpenPlaylist, onOpenTrack, onOpenAlbum, onOpenArtist) {
        { card -> card.open(onOpenPlaylist, onOpenTrack, onOpenAlbum, onOpenArtist) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item(key = "account") { AccountHeader(state.profile) }
        item(key = "liked") { LikedSongsRow(state.likedCount, onOpenLikedSongs) }

        if (shelves.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.no_playlists_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }
        }

        items(shelves, key = { "shelf-${it.id}" }) { shelf ->
            ShelfSection(
                shelf = shelf,
                profileName = state.profile.name,
                onShowAll = { onOpenShelf(shelf.id) },
                onOpenCard = onOpenCard,
            )
        }
    }
}

internal fun HomeCard.open(
    onOpenPlaylist: (String) -> Unit,
    onOpenTrack: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    when (type) {
        HomeCardType.PLAYLIST -> onOpenPlaylist(id)
        HomeCardType.TRACK -> onOpenTrack(id)
        HomeCardType.ALBUM -> onOpenAlbum(id)
        HomeCardType.ARTIST -> onOpenArtist(id)
    }
}

@Composable
private fun ShelfSection(
    shelf: HomeShelf,
    profileName: String,
    onShowAll: () -> Unit,
    onOpenCard: (HomeCard) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ShelfHeader(title = shelf.title, onShowAll = onShowAll)
        ShelfRow(
            cards = shelf.cards,
            profileName = profileName,
            onOpenCard = onOpenCard,
        )
    }
}

@Composable
private fun ShelfHeader(
    title: String,
    onShowAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.show_all),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onShowAll)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ShelfRow(
    cards: List<HomeCard>,
    profileName: String,
    onOpenCard: (HomeCard) -> Unit,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val cardWidth = (screenWidth * 0.38f).coerceIn(124f, 172f).dp

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cards, key = { "${it.type}-${it.id}" }) { card ->
            HomeCardItem(
                card = card,
                profileName = profileName,
                modifier = Modifier.width(cardWidth),
                onClick = { onOpenCard(card) }
            )
        }
    }
}

@Composable
internal fun HomeCardItem(
    card: HomeCard,
    profileName: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    onClick: () -> Unit,
) {
    val shape = if (card.type == HomeCardType.ARTIST) CircleShape else ShelfArtworkShape
    val subtitle = card.subtitle.takeUnless { it == profileName }.orEmpty()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImageImpl(
                modifier = Modifier.fillMaxSize(),
                model = card.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = card.title,
            style = titleStyle,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
                .size(44.dp)
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
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(ShelfArtworkShape)
                .background(LikedSongsBrush),
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
    }
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
