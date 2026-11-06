package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.AlbumResultType
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SimpleTrack
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyWebApi
import com.bobbyesp.spowlo.ui.components.MetadataResultRow
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.CollectionDownloadActions
import com.bobbyesp.spowlo.ui.components.songs.metadata_viewer.TrackComponent
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.TrackDownload
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class ArtistSong(
    val id: String,
    val name: String,
    val artists: String,
    val url: String,
    val durationMs: Long,
    val explicit: Boolean,
    val albumId: String = "",
    val albumName: String = "",
    val coverUrl: String = "",
    val discNumber: Int = 1,
    val trackNumber: Int = 0,
)

private const val MAX_DISCOGRAPHY = 1000
private const val ALBUMS_PREVIEW = 6
private const val SINGLES_PREVIEW = 5

private fun SimpleTrack.toArtistSong(release: SimpleAlbum) = ArtistSong(
    id = id,
    name = name,
    artists = artists.joinToString(", ") { it.name },
    url = externalUrls.spotify ?: "https://open.spotify.com/track/$id",
    durationMs = durationMs.toLong(),
    explicit = explicit,
    albumId = release.id,
    albumName = release.name,
    coverUrl = release.images.firstOrNull()?.url.orEmpty(),
    discNumber = discNumber,
    trackNumber = trackNumber,
)

private fun Track.toArtistSong() = ArtistSong(
    id = id,
    name = name,
    artists = artists.joinToString(", ") { it.name },
    url = externalUrls.spotify ?: "https://open.spotify.com/track/$id",
    durationMs = durationMs.toLong(),
    explicit = explicit,
    albumId = album.id,
    albumName = album.name,
    coverUrl = album.images?.firstOrNull()?.url.orEmpty(),
    discNumber = discNumber,
    trackNumber = trackNumber,
)

private fun ArtistSong.toTrackDownload() = TrackDownload(
    url = url,
    title = name,
    artists = artists,
    durationMs = durationMs,
    albumName = albumName,
    coverUrl = coverUrl,
    explicit = explicit,
    discNumber = discNumber,
    trackNumber = trackNumber,
)

private fun ArtistSong.albumTitle() = albumName.ifBlank { artists }

@Composable
fun ArtistPage(
    data: Artist,
    modifier: Modifier,
    onBack: () -> Unit,
    trackDownloadCallback: (String, String) -> Unit,
    downloadedTrackIds: Set<String> = emptySet(),
    onDownloadTracks: (List<TrackDownload>) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    onOpenTrack: ((String) -> Unit)? = null,
) {
    val topSongs = remember { mutableStateListOf<ArtistSong>() }
    val moreSongs = remember { mutableStateListOf<ArtistSong>() }
    val releases = remember { mutableStateListOf<SimpleAlbum>() }
    val albums = remember { mutableStateListOf<SimpleAlbum>() }
    val singles = remember { mutableStateListOf<SimpleAlbum>() }
    var selectedTab by rememberSaveable(data.id) { mutableIntStateOf(0) }
    var showAllSongs by rememberSaveable { mutableStateOf(false) }
    var showAllAlbums by rememberSaveable { mutableStateOf(false) }
    var showAllSingles by rememberSaveable { mutableStateOf(false) }
    var loadingTopSongs by remember { mutableStateOf(true) }
    var loadingMoreSongs by remember { mutableStateOf(false) }
    var discographyLoaded by remember { mutableStateOf(false) }
    var moreSongsLoaded by remember { mutableStateOf(false) }
    val loadMutex = remember { Mutex() }

    LaunchedEffect(data.id) {
        topSongs.clear()
        moreSongs.clear()
        releases.clear()
        albums.clear()
        singles.clear()
        showAllSongs = false
        showAllAlbums = false
        showAllSingles = false
        loadingTopSongs = true
        loadingMoreSongs = false
        discographyLoaded = false
        moreSongsLoaded = false

        SpotifyApiRequests.providesGetArtistTopTracks(data.id).orEmpty()
            .mapTo(topSongs) { it.toArtistSong() }
        loadingTopSongs = false

        val seenReleases = HashSet<String>()
        var offset = 0
        while (offset < MAX_DISCOGRAPHY) {
            val page = SpotifyApiRequests.providesGetArtistDiscography(data.id, offset)
            if (page.items.isEmpty()) break

            page.items.filter { seenReleases.add(it.id) }.forEach { release ->
                releases += release
                when (release.albumType) {
                    AlbumResultType.Album -> albums += release
                    AlbumResultType.Single -> singles += release
                    else -> Unit
                }
            }

            offset += SpotifyWebApi.DISCOGRAPHY_PAGE_SIZE
            if (offset >= page.total) break
        }
        val releaseNames = releases.associate { it.id to it.name }
        topSongs.forEachIndexed { index, song ->
            if (song.albumName.isBlank()) {
                releaseNames[song.albumId]?.let { topSongs[index] = song.copy(albumName = it) }
            }
        }
        discographyLoaded = true
    }

    val loadAllSongs: suspend () -> Unit = {
        loadMutex.withLock {
            if (!moreSongsLoaded) {
                loadingMoreSongs = true
                try {
                    val seen = HashSet<String>()
                    topSongs.forEach { seen.add(it.id) }
                    moreSongs.forEach { seen.add(it.id) }
                    for (chunk in releases.chunked(4)) {
                        val loaded = coroutineScope {
                            chunk.map { release ->
                                async(Dispatchers.IO) { release to albumTracks(release.id) }
                            }.awaitAll()
                        }
                        loaded.forEach { (release, tracks) ->
                            tracks.forEach { track ->
                                if (seen.add(track.id)) moreSongs += track.toArtistSong(release)
                            }
                        }
                    }
                    moreSongsLoaded = true
                } finally {
                    loadingMoreSongs = false
                }
            }
        }
    }

    LaunchedEffect(showAllSongs, discographyLoaded, data.id) {
        if (showAllSongs && discographyLoaded) loadAllSongs()
    }

    val songs = if (showAllSongs) topSongs + moreSongs else topSongs
    val missingTracks = (topSongs + moreSongs).asSequence()
        .filter { it.id !in downloadedTrackIds }
        .map { it.toTrackDownload() }
        .toList()
    val hasMoreSongs = !moreSongsLoaded && releases.isNotEmpty()

    val artworkUrl = data.images.getOrNull(0)?.url.orEmpty()
    val seed = rememberArtworkSeed(artworkUrl)
    val monthlyListeners = SpotifyApiRequests.getArtistMonthlyListeners(data.id)

    MetadataScreen(
        topBarTitle = data.name,
        onBack = onBack,
        modifier = modifier,
        seed = seed,
        backdrop = { ArtworkBackdrop(artworkUrl) },
        actions = { MetadataOverflowMenu(data.externalUrls.spotify.orEmpty()) },
    ) { paddingValues, listState ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item {
                MetadataHero(
                    title = data.name,
                    artwork = {
                        if (artworkUrl.isNotEmpty()) {
                            CollectionArtwork(artworkUrl, listState, CircleShape)
                        }
                    },
                ) {
                    MetadataMetaLine(
                        "${GeneralTextUtils.formatCompactCount((data.followers.total ?: 0).toLong())} ${
                            stringResource(id = R.string.artist_followers).lowercase()
                        }",
                        monthlyListeners?.let {
                            "${GeneralTextUtils.formatCount(it)} ${
                                stringResource(id = R.string.artist_monthly_listeners).lowercase()
                            }"
                        }.orEmpty(),
                    )
                }
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    containerColor = Color.Transparent,
                    divider = {},
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.artist_releases)) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tracks)) },
                    )
                }
            }

            if (selectedTab == 0) {
                if (albums.isNotEmpty()) {
                    item {
                        MetadataSectionTitle(stringResource(id = R.string.artist_albums))
                    }
                    val visibleAlbums =
                        if (showAllAlbums) albums else albums.take(ALBUMS_PREVIEW)
                    items(visibleAlbums, key = { "album-${it.id}" }) { album ->
                        ReleaseRow(album, onOpenAlbum)
                    }
                    if (albums.size > ALBUMS_PREVIEW) {
                        item {
                            SongsToggle(
                                expanded = showAllAlbums,
                                onToggle = { showAllAlbums = !showAllAlbums },
                                showAllRes = R.string.show_all_albums,
                                showFewerRes = R.string.show_fewer_albums,
                            )
                        }
                    }
                }

                if (singles.isNotEmpty()) {
                    item {
                        MetadataSectionTitle(stringResource(id = R.string.artist_singles))
                    }
                    val visibleSingles =
                        if (showAllSingles) singles else singles.take(SINGLES_PREVIEW)
                    items(visibleSingles, key = { "single-${it.id}" }) { single ->
                        ReleaseRow(single, onOpenAlbum)
                    }
                    if (singles.size > SINGLES_PREVIEW) {
                        item {
                            SongsToggle(
                                expanded = showAllSingles,
                                onToggle = { showAllSingles = !showAllSingles },
                                showAllRes = R.string.show_all_singles,
                                showFewerRes = R.string.show_fewer_singles,
                            )
                        }
                    }
                }

                if (!discographyLoaded) {
                    item {
                        LoadingRow()
                    }
                }
            } else {
                if (missingTracks.isNotEmpty() || hasMoreSongs) {
                    item {
                        CollectionDownloadActions(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            totalDurationMs = missingTracks.sumOf { it.durationMs },
                            missingTracks = missingTracks,
                            hasMore = hasMoreSongs,
                            resolveMissing = {
                                loadAllSongs()
                                (topSongs + moreSongs)
                                    .filter { it.id !in downloadedTrackIds }
                                    .distinctBy { it.id }
                                    .map { it.toTrackDownload() }
                            },
                            onDownloadAll = null,
                            onDownloadMissing = onDownloadTracks,
                        )
                    }
                }

                itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                    TrackComponent(
                        contentModifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        songName = song.name,
                        artists = song.artists,
                        subtitle = song.albumTitle(),
                        subtitleTrailing = GeneralTextUtils.formatTrackDuration(song.durationMs),
                        spotifyUrl = song.url,
                        isExplicit = song.explicit,
                        isDownloaded = song.id in downloadedTrackIds,
                        imageUrl = song.coverUrl,
                        onOpen = {
                            if (onOpenTrack != null) onOpenTrack(song.id)
                            else ChromeCustomTabsUtil.openUrl(song.url)
                        },
                        onDownload = {
                            trackDownloadCallback(song.url, "${song.name} - ${song.artists}")
                        }
                    )
                }

                if (releases.isNotEmpty() || moreSongs.isNotEmpty()) {
                    item {
                        SongsToggle(
                            expanded = showAllSongs,
                            onToggle = { showAllSongs = !showAllSongs },
                        )
                    }
                }

                if (loadingTopSongs || loadingMoreSongs) {
                    item {
                        LoadingRow()
                    }
                }
            }
        }
    }
}

private suspend fun albumTracks(albumId: String): List<SimpleTrack> {
    val tracks = mutableListOf<SimpleTrack>()
    var offset = 0
    while (offset < 200) {
        val page = SpotifyApiRequests.providesGetAlbumTracksPage(
            albumId,
            SpotifyWebApi.ALBUM_PAGE_SIZE,
            offset,
        )
        if (page.items.isEmpty()) break
        tracks += page.items
        offset += SpotifyWebApi.ALBUM_PAGE_SIZE
        if (offset >= page.total) break
    }
    return tracks
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.loading),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SongsToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
    showAllRes: Int = R.string.show_all_songs,
    showFewerRes: Int = R.string.show_fewer_songs,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(if (expanded) showFewerRes else showAllRes),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ReleaseRow(
    release: SimpleAlbum,
    onOpenAlbum: (String) -> Unit,
) {
    val trackCount = release.totalTracks ?: 0
    val subtitle = listOfNotNull(
        release.releaseDate?.year?.takeIf { it > 0 }?.toString(),
        if (trackCount > 0) {
            pluralStringResource(R.plurals.song_count, trackCount, trackCount)
        } else {
            null
        },
    ).joinToString(" · ")
    MetadataResultRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        artworkUrl = release.images.firstOrNull()?.url.orEmpty(),
        name = release.name,
        subtitle = subtitle,
        showDivider = true,
        onClick = { onOpenAlbum(release.id) },
    )
}
