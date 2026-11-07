package com.bobbyesp.spowlo.ui.pages.searcher

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SimplePlaylist
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SearchTopResult
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyWebApi.PlaylistStats
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.common.Route
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.HorizontalDivider
import com.bobbyesp.spowlo.ui.components.MetadataResultRow
import com.bobbyesp.spowlo.ui.components.QueryTextBox
import com.bobbyesp.spowlo.ui.components.others.shimmer.cards.HorizontalSongCardShimmer
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.bobbyesp.spowlo.ui.ext.loadStateContent
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import com.bobbyesp.spowlo.utils.UrlValidator
import kotlinx.coroutines.delay

private const val ALL_PREVIEW = 4

@Composable
fun SearcherPage(
    viewModel: SearcherPageViewModel = viewModel(),
    navController: NavController,
    initialQuery: String = "",
) {
    val viewState by viewModel.viewStateFlow.collectAsStateWithLifecycle()
    val downloadedTrackIds by viewModel.downloadedTrackIds.collectAsStateWithLifecycle()
    val albumTrackIds by viewModel.albumTrackIds.collectAsStateWithLifecycle()
    val artistFollowers by viewModel.artistFollowers.collectAsStateWithLifecycle()
    val playlistStats by viewModel.playlistStats.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    val (query, onValueChange) = rememberSaveable(key = "searchQuery_searchPage") {
        mutableStateOf(initialQuery)
    }

    LaunchedEffect(query) {
        viewModel.updateSearchText(query)
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            viewModel.updateSearchText(initialQuery)
            viewModel.search()
        }
    }

    val openItem: (String, String) -> Unit = { type, id ->
        navController.navigate(Route.PLAYLIST_PAGE + "/" + type + "/" + id)
    }

    val onQueryChange: (String) -> Unit = { newQuery ->
        val wasLink = UrlValidator.parseSpotifyLink(query) != null
        onValueChange(newQuery)
        if (!wasLink) {
            UrlValidator.parseSpotifyLink(newQuery)?.let { link ->
                keyboardController?.hide()
                openItem(link.type, link.id)
            }
        }
    }

    val paginatedTracks = viewState.searchedTracks.collectAsLazyPagingItems()
    val paginatedAlbums = viewState.searchedAlbums.collectAsLazyPagingItems()
    val paginatedPlaylists = viewState.searchedPlaylists.collectAsLazyPagingItems()
    val paginatedArtists = viewState.searchedArtists.collectAsLazyPagingItems()

    Scaffold(modifier = Modifier.fillMaxSize()) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton { navController.popBackStack() }
                QueryTextBox(
                    modifier = Modifier.weight(1f),
                    query = query,
                    onValueChange = onQueryChange,
                    onSearchCallback = {
                        val link = UrlValidator.parseSpotifyLink(query)
                        if (link != null) {
                            openItem(link.type, link.id)
                        } else {
                            viewModel.search()
                        }
                    },
                    requestFocusOnStart = initialQuery.isBlank(),
                )
            }

            SearchTabs(
                modifier = Modifier.padding(top = 2.dp),
                selected = viewState.activeSearchType,
                onSelect = viewModel::selectSearchType,
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = viewState.viewState) {
                    ViewSearchState.Idle -> IdleMessage()

                    ViewSearchState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )

                    ViewSearchState.Success -> when (viewState.activeSearchType) {
                        SpotifySearchType.ALL -> AllResults(
                            modifier = Modifier.fillMaxSize(),
                            tracks = paginatedTracks,
                            albums = paginatedAlbums,
                            playlists = paginatedPlaylists,
                            artists = paginatedArtists,
                            topResults = viewState.topResults,
                            totals = viewState.totals,
                            downloadedTrackIds = downloadedTrackIds,
                            albumTrackIds = albumTrackIds,
                            artistFollowers = artistFollowers,
                            playlistStats = playlistStats,
                            onSelectType = viewModel::selectSearchType,
                            onOpen = openItem,
                            onLoadAlbumTracks = viewModel::ensureAlbumTracksLoaded,
                            onLoadArtistFollowers = viewModel::ensureArtistFollowersLoaded,
                            onLoadPlaylistStats = viewModel::ensurePlaylistStatsLoaded,
                            onDownloadTrack = ::downloadTrack,
                        )

                        SpotifySearchType.TRACK -> PagedResults(
                            paged = paginatedTracks,
                            total = viewState.totals[SpotifySearchType.TRACK],
                        ) { track ->
                            TrackRow(
                                track = track,
                                isDownloaded = track.id in downloadedTrackIds,
                                onOpen = { openItem(SpotifySearchType.TRACK.asString(), track.id) },
                                onDownload = { downloadTrack(track) },
                            )
                        }

                        SpotifySearchType.ALBUM -> PagedResults(
                            paged = paginatedAlbums,
                            total = viewState.totals[SpotifySearchType.ALBUM],
                        ) { album ->
                            AlbumRow(
                                album = album,
                                downloadedCount = albumTrackIds[album.id]
                                    ?.count { it in downloadedTrackIds } ?: 0,
                                trackCount = albumTrackIds[album.id]?.size
                                    ?.takeIf { it > 0 } ?: (album.totalTracks ?: 0),
                                onOpen = {
                                    openItem(SpotifySearchType.ALBUM.asString(), album.id)
                                },
                                onLoadTracks = { viewModel.ensureAlbumTracksLoaded(album.id) },
                            )
                        }

                        SpotifySearchType.PLAYLIST -> PagedResults(
                            paged = paginatedPlaylists,
                            total = viewState.totals[SpotifySearchType.PLAYLIST],
                        ) { playlist ->
                            PlaylistRow(
                                playlist = playlist,
                                stats = playlistStats[playlist.id],
                                onLoadStats = {
                                    viewModel.ensurePlaylistStatsLoaded(playlist.id)
                                },
                                onOpen = {
                                    openItem(SpotifySearchType.PLAYLIST.asString(), playlist.id)
                                },
                            )
                        }

                        SpotifySearchType.ARTIST -> PagedResults(
                            paged = paginatedArtists,
                            total = viewState.totals[SpotifySearchType.ARTIST],
                        ) { artist ->
                            ArtistRow(
                                artist = artist,
                                followers = artistFollowers[artist.id],
                                onLoadFollowers = {
                                    viewModel.ensureArtistFollowersLoaded(artist.id)
                                },
                                onOpen = {
                                    openItem(SpotifySearchType.ARTIST.asString(), artist.id)
                                },
                            )
                        }
                    }

                    is ViewSearchState.Error -> Text(
                        text = state.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun downloadTrack(track: Track) {
    val artists = track.artists.joinToString(", ") { it.name }
    val url = track.externalUrls.spotify ?: "https://open.spotify.com/track/${track.id}"
    Downloader.executeParallelDownloadWithUrl(url, "${track.name} - $artists")
}

@Composable
private fun SearchTabs(
    selected: SpotifySearchType,
    onSelect: (SpotifySearchType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        SpotifySearchType.entries.forEach { type ->
            val active = type == selected
            val color by animateColorAsState(
                targetValue = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "searchTabColor",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(type) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = type.asLocalizedString(),
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = color,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                )
            }
        }
    }
}

@Composable
private fun <T : Any> PagedResults(
    paged: LazyPagingItems<T>,
    total: Int?,
    item: @Composable (T) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
    ) {
        item(key = "count") {
            ResultCount(total)
        }
        items(
            count = paged.itemCount,
            key = { index -> index },
        ) { index ->
            paged[index]?.let { item(it) }
        }
        loadStateContent(paged, itemCount = 8) {
            HorizontalSongCardShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }
        if (paged.itemCount == 0 &&
            paged.loadState.refresh is LoadState.NotLoading &&
            (total ?: 0) == 0
        ) {
            item(key = "empty") { EmptyResults() }
        }
    }
}

@Composable
private fun AllResults(
    modifier: Modifier,
    tracks: LazyPagingItems<Track>,
    albums: LazyPagingItems<SimpleAlbum>,
    playlists: LazyPagingItems<SimplePlaylist>,
    artists: LazyPagingItems<Artist>,
    topResults: List<SearchTopResult>,
    totals: Map<SpotifySearchType, Int>,
    downloadedTrackIds: Set<String>,
    albumTrackIds: Map<String, Set<String>>,
    artistFollowers: Map<String, Long>,
    playlistStats: Map<String, PlaylistStats>,
    onSelectType: (SpotifySearchType) -> Unit,
    onOpen: (String, String) -> Unit,
    onLoadAlbumTracks: (String) -> Unit,
    onLoadArtistFollowers: (String) -> Unit,
    onLoadPlaylistStats: (String) -> Unit,
    onDownloadTrack: (Track) -> Unit,
) {
    val loading = listOf(
        tracks.loadState.refresh,
        albums.loadState.refresh,
        playlists.loadState.refresh,
        artists.loadState.refresh,
    ).any { it is LoadState.Loading }
    val loadedItems = tracks.itemCount + albums.itemCount + playlists.itemCount + artists.itemCount
    val top = topResults.firstOrNull()

    LaunchedEffect(top) {
        delay(600)
        when (top) {
            is SearchTopResult.AlbumItem -> onLoadAlbumTracks(top.item.id)
            is SearchTopResult.ArtistItem -> onLoadArtistFollowers(top.item.id)
            is SearchTopResult.PlaylistItem -> onLoadPlaylistStats(top.item.id)
            else -> Unit
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
    ) {
        if (top != null) {
            item(key = "top-header") {
                SectionHeader(stringResource(R.string.top_result))
            }
            item(key = "top-result") {
                TopResultCard(
                    result = top,
                    albumTrackCount = (top as? SearchTopResult.AlbumItem)
                        ?.let { albumTrackIds[it.item.id]?.size?.takeIf { size -> size > 0 } },
                    artistFollowers = (top as? SearchTopResult.ArtistItem)
                        ?.let { artistFollowers[it.item.id] },
                    playlistStats = (top as? SearchTopResult.PlaylistItem)
                        ?.let { playlistStats[it.item.id] },
                    onOpen = { top.open(onOpen) },
                )
            }
        }

        if (tracks.itemCount > 0) {
            item(key = "tracks-header") {
                SectionHeader(
                    title = stringResource(R.string.tracks),
                    count = totals[SpotifySearchType.TRACK],
                    onSeeAll = { onSelectType(SpotifySearchType.TRACK) },
                )
            }
            items(
                count = minOf(tracks.itemCount, ALL_PREVIEW),
                key = { "track-$it" },
            ) { index ->
                val track = tracks[index] ?: return@items
                TrackRow(
                    track = track,
                    isDownloaded = track.id in downloadedTrackIds,
                    onOpen = { onOpen(SpotifySearchType.TRACK.asString(), track.id) },
                    onDownload = { onDownloadTrack(track) },
                )
            }
        }

        if (albums.itemCount > 0) {
            item(key = "albums-header") {
                SectionHeader(
                    title = stringResource(R.string.albums),
                    count = totals[SpotifySearchType.ALBUM],
                    onSeeAll = { onSelectType(SpotifySearchType.ALBUM) },
                )
            }
            items(
                count = minOf(albums.itemCount, ALL_PREVIEW),
                key = { "album-$it" },
            ) { index ->
                val album = albums[index] ?: return@items
                AlbumRow(
                    album = album,
                    downloadedCount = albumTrackIds[album.id]
                        ?.count { it in downloadedTrackIds } ?: 0,
                    trackCount = albumTrackIds[album.id]?.size
                        ?.takeIf { it > 0 } ?: (album.totalTracks ?: 0),
                    onOpen = { onOpen(SpotifySearchType.ALBUM.asString(), album.id) },
                    onLoadTracks = { onLoadAlbumTracks(album.id) },
                )
            }
        }

        if (playlists.itemCount > 0) {
            item(key = "playlists-header") {
                SectionHeader(
                    title = stringResource(R.string.playlists),
                    count = totals[SpotifySearchType.PLAYLIST],
                    onSeeAll = { onSelectType(SpotifySearchType.PLAYLIST) },
                )
            }
            items(
                count = minOf(playlists.itemCount, ALL_PREVIEW),
                key = { "playlist-$it" },
            ) { index ->
                val playlist = playlists[index] ?: return@items
                PlaylistRow(
                    playlist = playlist,
                    stats = playlistStats[playlist.id],
                    onLoadStats = { onLoadPlaylistStats(playlist.id) },
                    onOpen = { onOpen(SpotifySearchType.PLAYLIST.asString(), playlist.id) },
                )
            }
        }

        if (artists.itemCount > 0) {
            item(key = "artists-header") {
                SectionHeader(
                    title = stringResource(R.string.artists),
                    count = totals[SpotifySearchType.ARTIST],
                    onSeeAll = { onSelectType(SpotifySearchType.ARTIST) },
                )
            }
            items(
                count = minOf(artists.itemCount, ALL_PREVIEW),
                key = { "artist-$it" },
            ) { index ->
                val artist = artists[index] ?: return@items
                ArtistRow(
                    artist = artist,
                    followers = artistFollowers[artist.id],
                    onLoadFollowers = { onLoadArtistFollowers(artist.id) },
                    onOpen = { onOpen(SpotifySearchType.ARTIST.asString(), artist.id) },
                )
            }
        }

        if (loadedItems == 0 && top == null) {
            if (loading) {
                items(count = 3) {
                    HorizontalSongCardShimmer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
            } else {
                item(key = "empty") { EmptyResults() }
            }
        }
    }
}

@Composable
private fun ResultCount(total: Int?) {
    if (total == null || total <= 0) return
    Text(
        text = pluralStringResource(
            R.plurals.search_result_count,
            total,
            GeneralTextUtils.formatCount(total.toLong()),
        ),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val countColor = MaterialTheme.colorScheme.onSurfaceVariant
        val countSize = MaterialTheme.typography.bodySmall.fontSize
        Text(
            text = buildAnnotatedString {
                append(title)
                if (count != null && count > 0) {
                    withStyle(
                        SpanStyle(
                            color = countColor,
                            fontSize = countSize,
                            fontWeight = FontWeight.Normal,
                        )
                    ) {
                        append(" · ${GeneralTextUtils.formatCount(count.toLong())}")
                    }
                }
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        if (onSeeAll != null) {
            Text(
                text = stringResource(R.string.see_all),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isDownloaded: Boolean,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
) {
    val artists = track.artists.joinToString(", ") { it.name }
    val subtitle = listOf(artists, track.album.name)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    Column(modifier = Modifier.fillMaxWidth()) {
        TrackComponent(
            contentModifier = Modifier.fillMaxWidth(),
            songName = track.name,
            artists = artists,
            subtitle = subtitle,
            subtitleTrailing = GeneralTextUtils.formatTrackDuration(track.durationMs.toLong()),
            spotifyUrl = track.externalUrls.spotify.orEmpty(),
            isExplicit = track.explicit,
            isDownloaded = isDownloaded,
            imageUrl = track.album.images?.firstOrNull()?.url.orEmpty(),
            onOpen = onOpen,
            onDownload = onDownload,
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 58.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun AlbumRow(
    album: SimpleAlbum,
    downloadedCount: Int,
    trackCount: Int,
    onOpen: () -> Unit,
    onLoadTracks: () -> Unit,
) {
    LaunchedEffect(album.id) {
        if (album.id.isNotBlank()) {
            // Give fast scrolls a moment before fetching.
            delay(600)
            onLoadTracks()
        }
    }

    val indicator: (@Composable () -> Unit)? = when {
        downloadedCount <= 0 || trackCount <= 0 -> null
        downloadedCount >= trackCount -> {
            {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.downloaded),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        else -> {
            {
                Text(
                    text = "$downloadedCount/$trackCount",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    MetadataResultRow(
        name = album.name,
        subtitle = albumSubtitle(album, trackCount),
        artworkUrl = album.images.firstOrNull()?.url.orEmpty(),
        trailing = indicator,
        showDivider = true,
        onClick = onOpen,
    )
}

@Composable
private fun PlaylistRow(
    playlist: SimplePlaylist,
    stats: PlaylistStats?,
    onLoadStats: () -> Unit,
    onOpen: () -> Unit,
) {
    LaunchedEffect(playlist.id) {
        if (playlist.id.isNotBlank()) {
            delay(600)
            onLoadStats()
        }
    }

    val official = playlist.owner.id.equals("spotify", ignoreCase = true)

    MetadataResultRow(
        name = playlist.name,
        subtitle = playlistSubtitle(playlist, stats),
        artworkUrl = playlist.images.firstOrNull()?.url.orEmpty(),
        trailing = if (official) {
            {
                Icon(
                    painter = painterResource(R.drawable.spotify_logo),
                    contentDescription = stringResource(R.string.palette_spotify),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
        showDivider = true,
        onClick = onOpen,
    )
}

@Composable
private fun playlistSubtitle(playlist: SimplePlaylist, stats: PlaylistStats?): String {
    val songCount = stats?.songCount?.takeIf { it > 0 } ?: playlist.tracks.total
    val followers = stats?.followers ?: 0
    val owner = playlist.owner.displayName?.takeIf { it.isNotBlank() } ?: playlist.owner.id

    return buildString {
        append(stringResource(R.string.by_owner, owner))
        if (songCount > 0) {
            append(" · ")
            append(pluralStringResource(R.plurals.song_count, songCount, songCount))
        }
        if (followers > 0) {
            append(" · ")
            append(GeneralTextUtils.formatCompactCount(followers.toLong()))
            append(" ")
            append(stringResource(R.string.followers).lowercase())
        }
    }
}

@Composable
private fun ArtistRow(
    artist: Artist,
    followers: Long?,
    onLoadFollowers: () -> Unit,
    onOpen: () -> Unit,
) {
    val knownFollowers = followers ?: (artist.followers.total ?: 0).toLong()

    LaunchedEffect(artist.id) {
        if (followers == null && artist.id.isNotBlank()) {
            delay(600)
            onLoadFollowers()
        }
    }

    MetadataResultRow(
        name = artist.name,
        subtitle = artistSubtitle(knownFollowers),
        artworkUrl = artist.images.firstOrNull()?.url.orEmpty(),
        artworkShape = CircleShape,
        showDivider = true,
        onClick = onOpen,
    )
}

@Composable
private fun artistSubtitle(followers: Long): String = if (followers > 0) {
    "${GeneralTextUtils.formatCompactCount(followers)} ${
        stringResource(R.string.followers).lowercase()
    }"
} else {
    ""
}

@Composable
private fun TopResultCard(
    result: SearchTopResult,
    albumTrackCount: Int?,
    artistFollowers: Long?,
    playlistStats: PlaylistStats?,
    onOpen: () -> Unit,
) {
    var title = ""
    var artwork = ""
    var subtitle = ""
    var circular = false
    var official = false

    when (result) {
        is SearchTopResult.TrackItem -> {
            val track = result.item
            title = track.name
            artwork = track.album.images?.firstOrNull()?.url.orEmpty()
            subtitle = listOf(
                track.artists.joinToString(", ") { it.name },
                track.album.name,
            ).filter { it.isNotBlank() }.joinToString(" · ")
        }

        is SearchTopResult.AlbumItem -> {
            val album = result.item
            val trackCount = albumTrackCount ?: album.totalTracks ?: 0
            title = album.name
            artwork = album.images.firstOrNull()?.url.orEmpty()
            subtitle = listOfNotNull(
                album.artists.joinToString(", ") { it.name }.ifBlank { null },
                album.releaseDate?.year?.takeIf { it > 0 }?.toString(),
                trackCount.takeIf { it > 0 }?.let {
                    pluralStringResource(R.plurals.song_count, it, it)
                },
            ).joinToString(" · ")
        }

        is SearchTopResult.ArtistItem -> {
            val artist = result.item
            title = artist.name
            artwork = artist.images.firstOrNull()?.url.orEmpty()
            circular = true
            subtitle = artistSubtitle(
                artistFollowers ?: (artist.followers.total ?: 0).toLong()
            )
        }

        is SearchTopResult.PlaylistItem -> {
            val playlist = result.item
            title = playlist.name
            artwork = playlist.images.firstOrNull()?.url.orEmpty()
            official = playlist.owner.id.equals("spotify", ignoreCase = true)
            subtitle = playlistSubtitle(playlist, playlistStats)
        }
    }

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImageImpl(
                modifier = Modifier
                    .size(64.dp)
                    .clip(if (circular) CircleShape else MaterialTheme.shapes.small),
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                isPreview = false,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                MarqueeText(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp,
                    )
                }
            }
            if (official) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    painter = painterResource(R.drawable.spotify_logo),
                    contentDescription = stringResource(R.string.palette_spotify),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun albumSubtitle(album: SimpleAlbum, trackCount: Int): String {
    return listOfNotNull(
        album.artists.joinToString(", ") { it.name }.ifBlank { null },
        album.releaseDate?.year?.takeIf { it > 0 }?.toString(),
        trackCount.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.song_count, it, it)
        },
    ).joinToString(" · ")
}

private fun SearchTopResult.open(onOpen: (String, String) -> Unit) {
    when (this) {
        is SearchTopResult.TrackItem -> onOpen("track", item.id)
        is SearchTopResult.AlbumItem -> onOpen("album", item.id)
        is SearchTopResult.ArtistItem -> onOpen("artist", item.id)
        is SearchTopResult.PlaylistItem -> onOpen("playlist", item.id)
    }
}

@Composable
private fun IdleMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.search),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyResults() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.searcher_page_no_results),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.no_results_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}
