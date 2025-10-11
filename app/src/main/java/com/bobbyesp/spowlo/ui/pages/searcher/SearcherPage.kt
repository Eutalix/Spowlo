package com.bobbyesp.spowlo.ui.pages.searcher

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.common.Route
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.HorizontalDivider
import com.bobbyesp.spowlo.ui.components.QueryTextBox
import com.bobbyesp.spowlo.ui.components.others.shimmer.cards.HorizontalSongCardShimmer
import com.bobbyesp.spowlo.ui.ext.loadStateContent

@Composable
fun SearcherPage(
    navController: NavController,
    viewModel: SearcherPageViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val onItemClick: (String, String) -> Unit = { type, id ->
        navController.navigate(Route.PLAYLIST_PAGE + "/$type/$id")
    }

    val searchTypes = SpotifySearchType.values()

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton { navController.popBackStack() }
                QueryTextBox(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    query = viewState.query,
                    onValueChange = viewModel::onQueryChange,
                    onSearchCallback = viewModel::search
                )
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(searchTypes) { type ->
                    SearchTypeChip(
                        searchType = type,
                        isActive = viewState.activeSearchType == type,
                        onClick = { viewModel.onSearchTypeChange(type) }
                    )
                }
            }
            HorizontalDivider()
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (viewState.searchStatus) {
                    is ViewSearchState.Idle -> {
                        Text(
                            text = stringResource(R.string.search_for_music),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    is ViewSearchState.Loading -> {
                        CircularProgressIndicator()
                    }
                    is ViewSearchState.Success -> {
                        val paginatedTracks = viewState.searchedTracks.collectAsLazyPagingItems()
                        val paginatedAlbums = viewState.searchedAlbums.collectAsLazyPagingItems()
                        val paginatedPlaylists = viewState.searchedPlaylists.collectAsLazyPagingItems()
                        val paginatedArtists = viewState.searchedArtists.collectAsLazyPagingItems()
                        
                        Crossfade(targetState = viewState.activeSearchType, label = "SearchTypeCrossfade") { searchType ->
                            when (searchType) {
                                SpotifySearchType.TRACK -> ResultsList(
                                    paginatedItems = paginatedTracks,
                                    itemName = { it.name },
                                    itemSubtext = { it.artists.joinToString { artist -> artist.name } },
                                    itemArtworkUrl = { it.album.images.firstOrNull()?.url ?: "" },
                                    onItemClick = { onItemClick(searchType.asString(), it.id) }
                                )
                                SpotifySearchType.ALBUM -> ResultsList(
                                    paginatedItems = paginatedAlbums,
                                    itemName = { it.name },
                                    itemSubtext = { it.artists.joinToString { artist -> artist.name } },
                                    itemArtworkUrl = { it.images.firstOrNull()?.url ?: "" },
                                    onItemClick = { onItemClick(searchType.asString(), it.id) }
                                )
                                SpotifySearchType.PLAYLIST -> ResultsList(
                                    paginatedItems = paginatedPlaylists,
                                    itemName = { it.name },
                                    itemSubtext = { it.owner.displayName ?: "N/A" },
                                    itemArtworkUrl = { it.images.firstOrNull()?.url ?: "" },
                                    onItemClick = { onItemClick(searchType.asString(), it.id) }
                                )
                                SpotifySearchType.ARTIST -> ResultsList(
                                    paginatedItems = paginatedArtists,
                                    itemName = { it.name },
                                    itemSubtext = { "${it.followers.total} followers" },
                                    itemArtworkUrl = { it.images.firstOrNull()?.url ?: "" },
                                    onItemClick = { onItemClick(searchType.asString(), it.id) }
                                )
                            }
                        }
                    }
                    is ViewSearchState.Error -> {
                        Text((viewState.searchStatus as ViewSearchState.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T : Any> ResultsList(
    paginatedItems: LazyPagingItems<T>,
    itemName: (T) -> String,
    itemSubtext: (T) -> String,
    itemArtworkUrl: (T) -> String,
    onItemClick: (T) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = paginatedItems.itemCount,
            key = paginatedItems.itemKey(),
            contentType = paginatedItems.itemContentType()
        ) { index ->
            paginatedItems[index]?.let { item ->
                SearchResultItem(
                    name = itemName(item),
                    subtext = itemSubtext(item),
                    artworkUrl = itemArtworkUrl(item),
                    onClick = { onItemClick(item) }
                )
            }
        }
        loadStateContent(paginatedItems, itemCount = 10) {
            HorizontalSongCardShimmer(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTypeChip(
    searchType: SpotifySearchType,
    isActive: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = { Text(text = searchType.asLocalizedString()) },
    )
}

@Composable
private fun SearchResultItem(
    name: String,
    subtext: String,
    artworkUrl: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImageImpl(
            modifier = Modifier
                .size(48.dp)
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small),
            model = artworkUrl,
            contentDescription = "Artwork for $name",
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}