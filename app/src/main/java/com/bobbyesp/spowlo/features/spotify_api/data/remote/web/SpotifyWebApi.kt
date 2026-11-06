package com.bobbyesp.spowlo.features.spotify_api.data.remote.web

import android.os.SystemClock
import android.util.Log
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.PlaylistTrack
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SimplePlaylist
import com.adamratzman.spotify.models.SimpleTrack
import com.adamratzman.spotify.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * Spotify metadata through the web player's GraphQL backend instead of the official Web API.
 *
 * The official API rejects shared credentials with QUOTA_EXCEEDED for 24h at a time, so requests
 * go to the web player endpoint and are converted back to the official API's JSON shape.
 */
object SpotifyWebApi {

    private const val TAG = "SpotifyWebApi"
    private const val ENDPOINT = "https://api-partner.spotify.com/pathfinder/v1/query"

    const val PLAYLIST_PAGE_SIZE = 100
    const val ALBUM_PAGE_SIZE = 50
    const val DISCOGRAPHY_PAGE_SIZE = 50
    const val HOME_SECTION_LIMIT = 20

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    /** Play counts and monthly listeners, only returned by the detail queries. */
    private val playCounts = ConcurrentHashMap<String, Long>()
    private val monthlyListeners = ConcurrentHashMap<String, Long>()

    // One response feeds every tab; the web call returns all shelves anyway.
    private const val SEARCH_FIRST_PAGE = 40
    private const val SEARCH_CACHE_MS = 5_000L
    private data class SearchKey(val query: String, val limit: Int, val offset: Int)
    private val searchCache = ConcurrentHashMap<SearchKey, Pair<Long, JSONObject>>()
    private val searchInFlight = ConcurrentHashMap<SearchKey, Deferred<JSONObject>>()
    private val searchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun playCount(trackId: String): Long? = playCounts[trackId]

    fun monthlyListeners(artistId: String): Long? = monthlyListeners[artistId]

    /** Warms the anonymous session up so the first search is not delayed. */
    suspend fun warmUp() {
        runCatching { SpotifyWebAuth.session() }
            .onFailure { Log.w(TAG, "Warm-up failed: ${it.message}") }
    }

    suspend fun searchTracks(query: String, limit: Int, offset: Int): PagingObject<Track> {
        val root = search(query, limit, offset)
        val container = root.optJSONObject("tracksV2") ?: return emptyPage(offset, limit)
        val items = container.optJSONArray("items") ?: JSONArray()

        val tracks = (0 until items.length()).mapNotNull { index ->
            val node = items.optJSONObject(index)
                ?.optJSONObject("item")
                ?.optJSONObject("data")
                ?: return@mapNotNull null
            decodeOrNull<Track>(trackJson(node).toString())
        }

        return paging(tracks, container.optInt("totalCount", tracks.size), offset, limit)
    }

    suspend fun searchAlbums(query: String, limit: Int, offset: Int): PagingObject<SimpleAlbum> {
        val root = search(query, limit, offset)
        val container = root.optJSONObject("albumsV2") ?: return emptyPage(offset, limit)
        val items = container.optJSONArray("items") ?: JSONArray()

        val albums = (0 until items.length()).mapNotNull { index ->
            val node = items.optJSONObject(index)?.optJSONObject("data") ?: return@mapNotNull null
            decodeOrNull<SimpleAlbum>(simpleAlbumJson(node).toString())
        }

        return paging(albums, container.optInt("totalCount", albums.size), offset, limit)
    }

    suspend fun searchArtists(query: String, limit: Int, offset: Int): PagingObject<Artist> {
        val root = search(query, limit, offset)
        val container = root.optJSONObject("artists") ?: return emptyPage(offset, limit)
        val items = container.optJSONArray("items") ?: JSONArray()

        val artists = (0 until items.length()).mapNotNull { index ->
            val node = items.optJSONObject(index)?.optJSONObject("data") ?: return@mapNotNull null
            decodeOrNull<Artist>(artistJson(node).toString())
        }

        return paging(artists, container.optInt("totalCount", artists.size), offset, limit)
    }

    suspend fun searchPlaylists(query: String, limit: Int, offset: Int): PagingObject<SimplePlaylist> {
        val root = search(query, limit, offset)
        val container = root.optJSONObject("playlists") ?: return emptyPage(offset, limit)
        val items = container.optJSONArray("items") ?: JSONArray()

        val playlists = (0 until items.length()).mapNotNull { index ->
            val node = items.optJSONObject(index)?.optJSONObject("data") ?: return@mapNotNull null
            decodeOrNull<SimplePlaylist>(simplePlaylistJson(node).toString())
        }

        return paging(playlists, container.optInt("totalCount", playlists.size), offset, limit)
    }

    // The web player's own ranking, above the per-type shelves.
    suspend fun searchTopResults(query: String): List<SearchTopResult> {
        val root = search(query, SEARCH_FIRST_PAGE, 0)
        val items = root.optJSONObject("topResultsV2")?.optJSONArray("items")
        val ranked = items?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                val entry = array.optJSONObject(index) ?: return@mapNotNull null
                val data = entry.optJSONObject("data") ?: return@mapNotNull null
                decodeTopResult(data, entry.optString("__typename"))
            }
        }.orEmpty()
        if (ranked.isNotEmpty()) return ranked

        // Older bundles don't return topResultsV2, fall back to the first hit of each shelf.
        return buildList {
            root.optJSONObject("tracksV2")?.optJSONArray("items")
                ?.optJSONObject(0)?.optJSONObject("item")?.optJSONObject("data")
                ?.let { decodeOrNull<Track>(trackJson(it).toString()) }
                ?.let { add(SearchTopResult.TrackItem(it)) }

            root.optJSONObject("albumsV2")?.optJSONArray("items")
                ?.optJSONObject(0)?.optJSONObject("data")
                ?.let { decodeOrNull<SimpleAlbum>(simpleAlbumJson(it).toString()) }
                ?.let { add(SearchTopResult.AlbumItem(it)) }

            root.optJSONObject("artists")?.optJSONArray("items")
                ?.optJSONObject(0)?.optJSONObject("data")
                ?.let { decodeOrNull<Artist>(artistJson(it).toString()) }
                ?.let { add(SearchTopResult.ArtistItem(it)) }

            root.optJSONObject("playlists")?.optJSONArray("items")
                ?.optJSONObject(0)?.optJSONObject("data")
                ?.let { decodeOrNull<SimplePlaylist>(simplePlaylistJson(it).toString()) }
                ?.let { add(SearchTopResult.PlaylistItem(it)) }
        }
    }

    private fun decodeTopResult(node: JSONObject, typename: String): SearchTopResult? {
        val uri = node.optString("uri")
        val kind = uri.substringAfter("spotify:", "").substringBefore(':').ifBlank {
            typename.removeSuffix("ResponseWrapper").lowercase()
        }
        val name = node.optString("name")
        return when {
            kind == "track" || kind == "trackunion" ->
                decodeOrNull<Track>(trackJson(node).toString())
                    ?.takeIf { it.name.isNotBlank() }
                    ?.let(SearchTopResult::TrackItem)

            kind == "album" ->
                decodeOrNull<SimpleAlbum>(simpleAlbumJson(node).toString())
                    ?.takeIf { it.name.isNotBlank() || name.isNotBlank() }
                    ?.let(SearchTopResult::AlbumItem)

            kind == "artist" ->
                decodeOrNull<Artist>(artistJson(node).toString())
                    ?.takeIf { it.name.isNotBlank() || name.isNotBlank() }
                    ?.let(SearchTopResult::ArtistItem)

            kind == "playlist" ->
                decodeOrNull<SimplePlaylist>(simplePlaylistJson(node).toString())
                    ?.takeIf { it.name.isNotBlank() || name.isNotBlank() }
                    ?.let(SearchTopResult::PlaylistItem)

            else -> null
        }
    }

    suspend fun getTrack(trackId: String): Track {
        val root = graphql(
            "getTrack",
            JSONObject().put("uri", "spotify:track:$trackId"),
        )
        val node = root.optJSONObject("data")?.optJSONObject("trackUnion")
            ?: throw SpotifyWebException(200, "Track $trackId was not found")

        return decode(trackJson(node).toString())
    }

    suspend fun getAlbum(albumId: String): Album {
        val root = graphql(
            "getAlbum",
            JSONObject()
                .put("uri", "spotify:album:$albumId")
                .put("locale", "")
                .put("offset", 0)
                .put("limit", ALBUM_PAGE_SIZE),
        )
        val node = root.optJSONObject("data")?.optJSONObject("albumUnion")
            ?: throw SpotifyWebException(200, "Album $albumId was not found")

        return decode(albumJson(node).toString())
    }

    suspend fun getAlbumTracks(albumId: String, limit: Int, offset: Int): PagingObject<SimpleTrack> {
        val root = graphql(
            "getAlbum",
            JSONObject()
                .put("uri", "spotify:album:$albumId")
                .put("locale", "")
                .put("offset", offset)
                .put("limit", limit),
        )
        val node = root.optJSONObject("data")?.optJSONObject("albumUnion")
            ?: throw SpotifyWebException(200, "Album $albumId was not found")
        val container = node.optJSONObject("tracksV2") ?: return emptyPage(offset, limit)
        val items = container.optJSONArray("items") ?: JSONArray()

        val tracks = (0 until items.length()).mapNotNull { index ->
            val track = items.optJSONObject(index)?.optJSONObject("track") ?: return@mapNotNull null
            decodeOrNull<SimpleTrack>(simpleTrackJson(track, node).toString())
        }

        return paging(tracks, container.optInt("totalCount", tracks.size), offset, limit)
    }

    suspend fun getArtist(artistId: String): Artist {
        val root = graphql(
            "queryArtistOverview",
            JSONObject()
                .put("uri", "spotify:artist:$artistId")
                .put("locale", "")
                .put("includePrerelease", true),
        )
        val node = root.optJSONObject("data")?.optJSONObject("artistUnion")
            ?: throw SpotifyWebException(200, "Artist $artistId was not found")

        return decode(artistJson(node).toString())
    }

    suspend fun getArtistTopTracks(artistId: String): List<Track> {
        val root = graphql(
            "queryArtistOverview",
            JSONObject()
                .put("uri", "spotify:artist:$artistId")
                .put("locale", "")
                .put("includePrerelease", true),
        )
        val items = root.optJSONObject("data")
            ?.optJSONObject("artistUnion")
            ?.optJSONObject("discography")
            ?.optJSONObject("topTracks")
            ?.optJSONArray("items")
            ?: return emptyList()

        return (0 until items.length()).mapNotNull { index ->
            val track = items.optJSONObject(index)?.optJSONObject("track") ?: return@mapNotNull null
            decodeOrNull<Track>(trackJson(track).toString())
        }
    }

    suspend fun getArtistAlbums(artistId: String): PagingObject<SimpleAlbum> {
        val root = graphql(
            "queryArtistOverview",
            JSONObject()
                .put("uri", "spotify:artist:$artistId")
                .put("locale", "")
                .put("includePrerelease", true),
        )
        val items = root.optJSONObject("data")
            ?.optJSONObject("artistUnion")
            ?.optJSONObject("discography")
            ?.optJSONObject("albums")
            ?.optJSONArray("items")
            ?: return emptyPage(0, 0)

        val albums = mutableListOf<SimpleAlbum>()
        for (groupIndex in 0 until items.length()) {
            val group = items.optJSONObject(groupIndex) ?: continue
            val releases = group.optJSONObject("releases")?.optJSONArray("items") ?: continue
            for (releaseIndex in 0 until releases.length()) {
                val node = releases.optJSONObject(releaseIndex) ?: continue
                runCatching { decode<SimpleAlbum>(simpleAlbumJson(node).toString()) }
                    .getOrNull()
                    ?.let(albums::add)
            }
        }

        return paging(albums, albums.size, 0, albums.size)
    }

    suspend fun getArtistDiscography(
        artistId: String,
        offset: Int,
        limit: Int = DISCOGRAPHY_PAGE_SIZE,
    ): PagingObject<SimpleAlbum> {
        val root = graphql(
            "queryArtistDiscographyAll",
            JSONObject()
                .put("uri", "spotify:artist:$artistId")
                .put("offset", offset)
                .put("limit", limit)
                .put("order", "DATE_DESC"),
        )
        val all = root.optJSONObject("data")
            ?.optJSONObject("artistUnion")
            ?.optJSONObject("discography")
            ?.optJSONObject("all")
            ?: return emptyPage(offset, limit)

        val items = all.optJSONArray("items") ?: return emptyPage(offset, limit)
        val releases = mutableListOf<SimpleAlbum>()
        for (groupIndex in 0 until items.length()) {
            val group = items.optJSONObject(groupIndex) ?: continue
            val groupItems = group.optJSONObject("releases")?.optJSONArray("items") ?: continue
            for (releaseIndex in 0 until groupItems.length()) {
                val node = groupItems.optJSONObject(releaseIndex) ?: continue
                runCatching { decode<SimpleAlbum>(simpleAlbumJson(node).toString()) }
                    .getOrNull()
                    ?.let(releases::add)
            }
        }

        return paging(releases, all.optInt("totalCount", releases.size), offset, limit)
    }

    suspend fun getPlaylist(playlistId: String, account: Boolean = false): Playlist {
        val root = graphql(
            "fetchPlaylist",
            JSONObject()
                .put("uri", "spotify:playlist:$playlistId")
                .put("offset", 0)
                .put("limit", PLAYLIST_PAGE_SIZE)
                .put("enableWatchFeedEntrypoint", false),
            account = account,
        )
        val node = root.optJSONObject("data")?.optJSONObject("playlistV2")
            ?: throw SpotifyWebException(200, "Playlist $playlistId was not found")

        return decode(playlistJson(node).toString())
    }

    data class PlaylistStats(val songCount: Int, val followers: Int)

    /** Search nodes have no counts, so fetch the playlist itself. */
    suspend fun getPlaylistStats(playlistId: String): PlaylistStats {
        val root = graphql(
            "fetchPlaylist",
            JSONObject()
                .put("uri", "spotify:playlist:$playlistId")
                .put("offset", 0)
                .put("limit", 1)
                .put("enableWatchFeedEntrypoint", false),
        )
        val node = root.optJSONObject("data")?.optJSONObject("playlistV2")
            ?: throw SpotifyWebException(200, "Playlist $playlistId was not found")

        return PlaylistStats(
            songCount = node.optJSONObject("content")?.optInt("totalCount", 0) ?: 0,
            followers = node.optInt("followers", 0),
        )
    }

    suspend fun getPlaylistTracks(
        playlistId: String,
        limit: Int,
        offset: Int,
        account: Boolean = false,
    ): PagingObject<PlaylistTrack> {
        val root = graphql(
            "fetchPlaylist",
            JSONObject()
                .put("uri", "spotify:playlist:$playlistId")
                .put("offset", offset)
                .put("limit", limit)
                .put("enableWatchFeedEntrypoint", false),
            account = account,
        )
        val container = root.optJSONObject("data")
            ?.optJSONObject("playlistV2")
            ?.optJSONObject("content")
            ?: return emptyPage(offset, limit)

        val items = container.optJSONArray("items") ?: JSONArray()
        val tracks = (0 until items.length()).mapNotNull { index ->
            val wrapper = playlistTrackJson(items.optJSONObject(index)) ?: return@mapNotNull null
            runCatching { decode<PlaylistTrack>(wrapper.toString()) }.getOrNull()
        }

        return paging(tracks, container.optInt("totalCount", tracks.size), offset, limit)
    }

    /** Account operations below go through pathfinder, which the account token is not rate limited on. */
    suspend fun accountProfile(): JSONObject {
        val root = graphql("profileAttributes", JSONObject(), account = true)
        return root.optJSONObject("data")?.optJSONObject("me")?.optJSONObject("profile")
            ?: throw SpotifyWebException(200, "Spotify did not return the account profile")
    }

    suspend fun accountPlaylists(limit: Int, offset: Int): JSONObject {
        val root = graphql(
            "libraryV3",
            JSONObject()
                .put("filters", JSONArray().put("Playlists"))
                .put("order", JSONObject.NULL)
                .put("textFilter", "")
                .put("features", JSONArray())
                .put("limit", limit)
                .put("offset", offset)
                .put("flatten", false)
                .put("expandedFolders", JSONArray())
                .put("folderUri", JSONObject.NULL)
                .put("includeFoldersWhenFlattening", true)
                .put("withCuration", false),
            account = true,
        )
        return root.optJSONObject("data")?.optJSONObject("me")?.optJSONObject("libraryV3")
            ?: JSONObject()
    }

    suspend fun accountLikedTracks(limit: Int, offset: Int): JSONObject {
        val root = graphql(
            "fetchLibraryTracks",
            JSONObject()
                .put("uri", "spotify:collection:tracks")
                .put("offset", offset)
                .put("limit", limit),
            account = true,
        )
        return root.optJSONObject("data")?.optJSONObject("me")?.optJSONObject("library")
            ?.optJSONObject("tracks")
            ?: JSONObject()
    }

    /**
     * The personalized home feed behind the web player's home tab. Spotify decides the sections,
     * so this is the only way to get the same shelves without scraping the UI.
     */
    suspend fun accountHome(): JSONObject {
        val deviceId = runCatching { SpotifyWebAuth.session().deviceId }.getOrDefault("")
        val root = graphql(
            "home",
            JSONObject()
                .put("includeHomeChapterVideoCards", false)
                .put("homeEndUserIntegration", "INTEGRATION_WEB_PLAYER")
                .put("timeZone", TimeZone.getDefault().id)
                .put("sp_t", deviceId)
                .put("facet", "")
                .put("sectionItemsLimit", HOME_SECTION_LIMIT)
                .put("includeEpisodeContentRatingsV2", false),
            account = true,
        )
        return root.optJSONObject("data")?.optJSONObject("home") ?: JSONObject()
    }

    private suspend fun search(query: String, limit: Int, offset: Int): JSONObject {
        val key = SearchKey(query, limit, offset)
        searchCache[key]?.let { (at, body) ->
            if (SystemClock.elapsedRealtime() - at < SEARCH_CACHE_MS) return body
        }

        val deferred = searchInFlight.compute(key) { _, running ->
            running ?: searchScope.async { fetchSearch(query, limit, offset) }
        }!!
        val body = try {
            deferred.await()
        } catch (exception: Exception) {
            searchInFlight.remove(key, deferred)
            throw exception
        }

        searchInFlight.remove(key, deferred)
        if (searchCache.size > 8) searchCache.clear()
        searchCache[key] = SystemClock.elapsedRealtime() to body
        return body
    }

    private suspend fun fetchSearch(query: String, limit: Int, offset: Int): JSONObject {
        val root = graphql(
            "searchDesktop",
            JSONObject()
                .put("searchTerm", query)
                .put("offset", offset)
                .put("limit", limit)
                .put("numberOfTopResults", 5)
                .put("includeAudiobooks", false)
                .put("includeArtistHasConcertsField", false)
                .put("includePreReleases", true)
                .put("includeLocalConcertsField", false),
        )
        return root.optJSONObject("data")?.optJSONObject("searchV2")
            ?: throw SpotifyWebException(200, "Unexpected search response")
    }

    private suspend fun graphql(
        operation: String,
        variables: JSONObject,
        account: Boolean = false,
    ): JSONObject = withContext(Dispatchers.IO) {
        var hash = SpotifyHashes.hashFor(operation)

        var session = SpotifyWebAuth.session()
        val accountToken = if (account) SpotifyUserSession.accessToken() else null
        var accessToken = accountToken ?: session.accessToken
        var response = execute(session, operation, hash, variables, accessToken)

        // Spotify rotates these hashes with its web player builds. Its also a 400, but
        // refreshing the token wont do anything here.
        if (response.body.contains("PersistedQueryNotFound")) {
            Log.w(TAG, "$operation has a stale hash, pulling the manifest")
            val fresh = SpotifyHashes.refreshStale(operation)
            if (fresh != null && fresh != hash) {
                hash = fresh
                response = execute(session, operation, hash, variables, accessToken)
            }
            if (response.body.contains("PersistedQueryNotFound")) {
                Log.e(TAG, "$operation still has no working hash, the manifest is behind")
                throw SpotifyWebException(response.code, "No working hash for $operation")
            }
        }

        // The client token expires independently of the access token.
        if (response.code == 401 || response.code == 400 || response.code == 403) {
            Log.w(TAG, "$operation returned HTTP ${response.code}, refreshing token")
            accessToken = if (accountToken != null) {
                SpotifyUserSession.accessToken(forceRefresh = true)
                    ?: SpotifyWebAuth.session(forceRefresh = true).accessToken
            } else {
                SpotifyWebAuth.invalidate()
                SpotifyWebAuth.session(forceRefresh = true).accessToken
            }
            response = execute(session, operation, hash, variables, accessToken)
        }

            val body = response.body
            Log.d(TAG, "← $operation HTTP ${response.code} bodyLen=${body.length}")
            if (response.code !in 200..299) {
                Log.e(TAG, "$operation failed with HTTP ${response.code}: ${body.take(300)}")
                throw SpotifyWebException(response.code, body.take(300))
            }

            try {
                JSONObject(body)
            } catch (e: Exception) {
                throw SpotifyWebException(response.code, "Malformed response: ${body.take(200)}", e)
            }
        }

    private suspend fun execute(
        session: SpotifyWebSession,
        operation: String,
        hash: String,
        variables: JSONObject,
        accessToken: String = session.accessToken,
    ): WebResponse {
        val url = "https://api-partner.spotify.com/pathfinder/v1/query?".toHttpUrl().newBuilder()
            .addQueryParameter("operationName", operation)
            .addQueryParameter("variables", variables.toString())
            .addQueryParameter(
                "extensions",
                JSONObject()
                    .put("persistedQuery", JSONObject().put("version", 1).put("sha256Hash", hash))
                    .toString()
            )
            .build()
            .toString()

        Log.d(TAG, "→ $operation")
        return SpotifyWebTransport.fetch(
            url,
            buildMap {
                put("Authorization", "Bearer $accessToken")
                if (session.clientToken.isNotBlank()) put("Client-Token", session.clientToken)
                if (session.deviceId.isNotBlank()) put("Cookie", "sp_t=${session.deviceId}")
                put("Spotify-App-Version", session.clientVersion)
                put("App-Platform", "WebPlayer")
                put("Accept", "application/json")
                put("Accept-Language", "en")
                put("Content-Type", "application/json;charset=UTF-8")
                put("Origin", "https://open.spotify.com")
                put("Referer", "https://open.spotify.com/")
            },
        )
    }

    private inline fun <reified T> decode(payload: String): T = json.decodeFromString(payload)

    /** One malformed item must not fail the whole page. */
    private inline fun <reified T> decodeOrNull(payload: String): T? =
        runCatching { json.decodeFromString<T>(payload) }
            .onFailure { Log.w(TAG, "Skipping unparsable item: ${it.message}") }
            .getOrNull()

    private fun <T : Any> paging(items: List<T>, total: Int, offset: Int, limit: Int) =
        PagingObject<T>(
            href = "",
            items = items,
            limit = limit,
            next = null,
            offset = offset,
            previous = null,
            total = total,
        )

    private fun <T : Any> emptyPage(offset: Int, limit: Int) = paging<T>(emptyList(), 0, offset, limit)

    private fun imagesJson(sources: JSONArray?): JSONArray {
        val result = JSONArray()
        if (sources == null) return result
        for (index in 0 until sources.length()) {
            val source = sources.optJSONObject(index) ?: continue
            result.put(
                JSONObject()
                    .put("height", source.optDouble("height", 0.0))
                    .put("width", source.optDouble("width", 0.0))
                    .put("url", source.optString("url"))
            )
        }
        return result
    }

    private fun artistsJson(artists: JSONArray?): JSONArray {
        val result = JSONArray()
        if (artists == null) return result
        for (index in 0 until artists.length()) {
            val artist = artists.optJSONObject(index) ?: continue
            val uri = artist.optString("uri")
            val id = artist.optString("id").ifBlank { uri.substringAfterLast(':') }
            val name = artist.optJSONObject("profile")?.optString("name").orEmpty()
            result.put(
                JSONObject()
                    .put("external_urls", JSONObject().put("spotify", "https://open.spotify.com/artist/$id"))
                    .put("href", "")
                    .put("id", id)
                    .put("uri", "spotify:artist:$id")
                    .put("name", name)
                    .put("type", "artist")
            )
        }
        return result
    }

    private fun trackArtistsJson(node: JSONObject): JSONArray {
        node.optJSONObject("artists")?.optJSONArray("items")?.let { return artistsJson(it) }

        // The web player's getTrack query exposes firstArtist/otherArtists instead.
        val combined = JSONArray()
        fun append(container: JSONObject?) {
            val items = container?.optJSONArray("items") ?: return
            for (index in 0 until items.length()) {
                items.optJSONObject(index)?.let(combined::put)
            }
        }
        append(node.optJSONObject("firstArtist"))
        append(node.optJSONObject("otherArtists"))
        return artistsJson(combined)
    }

    private fun trackJson(node: JSONObject): JSONObject {
        val uri = node.optString("uri")
        val id = node.optString("id").ifBlank { uri.substringAfterLast(':') }
        node.optString("playcount").toLongOrNull()?.let { playCounts[id] = it }
        val name = node.optString("name")
        val duration = node.optJSONObject("duration")?.optInt("totalMilliseconds")
            ?: node.optJSONObject("trackDuration")?.optInt("totalMilliseconds")
            ?: 0
        val artists = trackArtistsJson(node)
        val album = node.optJSONObject("albumOfTrack")
        val explicit = node.optJSONObject("contentRating")?.optString("label") == "EXPLICIT"

        return JSONObject()
            .put("album", album?.let { simpleAlbumJson(it) } ?: simpleAlbumJson(JSONObject().put("id", "unknown")))
            .put("artists", artists)
            .put("available_markets", JSONArray())
            .put("disc_number", node.optInt("discNumber", 1))
            .put("duration_ms", duration)
            .put("explicit", explicit)
            .put("external_ids", JSONObject())
            .put("external_urls", JSONObject().put("spotify", "https://open.spotify.com/track/$id"))
            .put("href", "")
            .put("id", id)
            .put("is_playable", true)
            .put("name", name)
            .put("popularity", 0.0)
            .put("preview_url", JSONObject.NULL)
            .put("track_number", node.optInt("trackNumber", 1))
            .put("type", "track")
            .put("uri", "spotify:track:$id")
            .put("is_local", false)
    }

    private fun simpleTrackJson(node: JSONObject, album: JSONObject): JSONObject {
        val uri = node.optString("uri")
        val id = uri.substringAfterLast(':')
        node.optString("playcount").toLongOrNull()?.let { playCounts[id] = it }
        val duration = node.optJSONObject("duration")?.optInt("totalMilliseconds")
            ?: node.optJSONObject("trackDuration")?.optInt("totalMilliseconds")
            ?: 0
        val explicit = node.optJSONObject("contentRating")?.optString("label") == "EXPLICIT"

        return JSONObject()
            .put("album", simpleAlbumJson(album))
            .put("artists", artistsJson(node.optJSONObject("artists")?.optJSONArray("items")))
            .put("available_markets", JSONArray())
            .put("disc_number", node.optInt("discNumber", 1))
            .put("duration_ms", duration)
            .put("explicit", explicit)
            .put("external_ids", JSONObject())
            .put("external_urls", JSONObject().put("spotify", "https://open.spotify.com/track/$id"))
            .put("href", "")
            .put("id", id)
            .put("is_playable", true)
            .put("name", node.optString("name"))
            .put("popularity", 0.0)
            .put("preview_url", JSONObject.NULL)
            .put("track_number", node.optInt("trackNumber", 1))
            .put("type", "track")
            .put("uri", "spotify:track:$id")
            .put("is_local", false)
    }

    /** `AlbumResultType` has no EP, and its getter throws on any value outside its list. */
    private fun albumType(raw: String): String = when (raw.lowercase()) {
        "single", "ep" -> "single"
        "compilation" -> "compilation"
        "appears_on" -> "appears_on"
        else -> "album"
    }

    private fun simpleAlbumJson(node: JSONObject): JSONObject {
        val uri = node.optString("uri")
        val id = node.optString("id").ifBlank { uri.substringAfterLast(':') }
        val cover = node.optJSONObject("coverArt")?.optJSONArray("sources")
            ?: node.optJSONObject("images")?.optJSONArray("items")
                ?.optJSONObject(0)?.optJSONArray("sources")

        val releaseDate = node.optJSONObject("date")?.optString("isoString")
            ?.take(10)
            ?.takeIf { it.isNotBlank() }
            ?: node.optJSONObject("date")?.optInt("year")?.toString()
            ?: "0"

        val totalTracks = when {
            node.has("totalTracks") -> node.optInt("totalTracks")
            node.optJSONObject("tracksV2") != null -> node.optJSONObject("tracksV2").optInt("totalCount")
            node.optJSONObject("tracks") != null -> node.optJSONObject("tracks").optInt("totalCount")
            else -> 0
        }

        return JSONObject()
            .put("album_type", albumType(node.optString("type")))
            .put("total_tracks", totalTracks)
            .put("available_markets", JSONArray())
            .put("external_urls", JSONObject().put("spotify", "https://open.spotify.com/album/$id"))
            .put("href", "")
            .put("id", id)
            .put("images", imagesJson(cover))
            .put("name", node.optString("name"))
            .put("release_date", releaseDate)
            .put("release_date_precision", if (releaseDate.length >= 10) "day" else "year")
            .put("type", "album")
            .put("uri", "spotify:album:$id")
            .put("artists", artistsJson(node.optJSONObject("artists")?.optJSONArray("items")))
    }

    private fun artistJson(node: JSONObject): JSONObject {
        val uri = node.optString("uri")
        val id = node.optString("id").ifBlank { uri.substringAfterLast(':') }
        node.optJSONObject("stats")?.optString("monthlyListeners")?.toLongOrNull()
            ?.let { monthlyListeners[id] = it }
        val images = node.optJSONObject("visuals")?.optJSONObject("avatarImage")?.optJSONArray("sources")
            ?: node.optJSONObject("headerImage")?.optJSONArray("sources")

        return JSONObject()
            .put("external_urls", JSONObject().put("spotify", "https://open.spotify.com/artist/$id"))
            .put("followers", JSONObject().put("href", JSONObject.NULL).put("total", node.optJSONObject("stats")?.optInt("followers", 0) ?: 0))
            .put("genres", JSONArray())
            .put("href", "")
            .put("id", id)
            .put("images", imagesJson(images))
            .put("name", node.optJSONObject("profile")?.optString("name").orEmpty())
            .put("popularity", 0.0)
            .put("type", "artist")
            .put("uri", "spotify:artist:$id")
    }

    private fun simplePlaylistJson(rawNode: JSONObject): JSONObject {
        // Some search shelves wrap the playlist entity in another data object.
        val nested = rawNode.optJSONObject("data")
        val node = if (nested != null && rawNode.optString("uri").isBlank()) nested else rawNode
        val uri = node.optString("uri").ifBlank { rawNode.optString("uri") }
        val id = node.optString("id").ifBlank { uri.substringAfterLast(':') }
        val owner = node.optJSONObject("ownerV2")?.optJSONObject("data")
        val ownerUri = owner?.optString("uri").orEmpty()
        val ownerId = ownerUri.substringAfterLast(':').ifBlank { "spotify" }
        val images = node.optJSONObject("images")?.optJSONArray("items")
            ?.optJSONObject(0)?.optJSONArray("sources")

        return JSONObject()
            .put("collaborative", false)
            .put("description", node.optString("description"))
            .put("external_urls", JSONObject().put("spotify", "https://open.spotify.com/playlist/$id"))
            .put("href", "")
            .put("id", id)
            .put("images", imagesJson(images))
            .put("name", node.optString("name"))
            .put("owner", JSONObject()
                .put("external_urls", JSONObject().put("spotify", "https://open.spotify.com/user/$ownerId"))
                .put("href", "")
                .put("id", ownerId)
                .put("uri", "spotify:user:$ownerId")
                .put("display_name", owner?.optString("name").orEmpty())
                .put("type", "user"))
            .put("primary_color", JSONObject.NULL)
            .put("public", JSONObject.NULL)
            .put("snapshot_id", node.optString("revisionId"))
            .put("tracks", JSONObject().put("href", "").put("total", node.optInt("totalCount", 0)))
            .put("type", "playlist")
            .put("uri", "spotify:playlist:$id")
    }

    private fun playlistJson(node: JSONObject): JSONObject {
        val uri = node.optString("uri")
        val id = uri.substringAfterLast(':')
        val base = simplePlaylistJson(node)
        val total = node.optJSONObject("content")?.optInt("totalCount", 0) ?: 0

        return base
            .put("followers", JSONObject().put("href", JSONObject.NULL).put("total", node.optInt("followers", 0)))
            .put("tracks", JSONObject()
                .put("href", "")
                .put("items", JSONArray())
                .put("limit", PLAYLIST_PAGE_SIZE)
                .put("next", JSONObject.NULL)
                .put("offset", 0)
                .put("previous", JSONObject.NULL)
                .put("total", total))
            .put("uri", "spotify:playlist:$id")
    }

    private fun playlistTrackJson(item: JSONObject?): JSONObject? {
        item ?: return null
        val track = item.optJSONObject("itemV2")?.optJSONObject("data") ?: return null
        return JSONObject()
            .put("added_at", item.optJSONObject("addedAt")?.optString("isoString").orEmpty())
            .put("track", trackJson(track))
    }

    private fun albumJson(node: JSONObject): JSONObject {
        val uri = node.optString("uri")
        val id = uri.substringAfterLast(':')
        val releaseDate = node.optJSONObject("date")?.optString("isoString")
            ?.take(10)
            ?.takeIf { it.isNotBlank() }
            ?: node.optJSONObject("date")?.optInt("year")?.toString()
            ?: "0"
        val copyrights = JSONArray()
        val copyrightItems = node.optJSONObject("copyright")?.optJSONArray("items")
        if (copyrightItems != null) {
            for (index in 0 until copyrightItems.length()) {
                val item = copyrightItems.optJSONObject(index) ?: continue
                copyrights.put(
                    JSONObject()
                        .put("text", item.optString("text"))
                        .put("type", item.optString("type"))
                )
            }
        }

        return simpleAlbumJson(node)
            .put("copyrights", copyrights)
            .put("external_ids", JSONObject())
            .put("genres", JSONArray())
            .put("label", node.optString("label"))
            .put("popularity", 0.0)
            .put("release_date", releaseDate)
            .put("release_date_precision", if (releaseDate.length >= 10) "day" else "year")
            .put("tracks", JSONObject()
                .put("href", "")
                .put("items", JSONArray())
                .put("limit", ALBUM_PAGE_SIZE)
                .put("next", JSONObject.NULL)
                .put("offset", 0)
                .put("previous", JSONObject.NULL)
                .put("total", node.optJSONObject("tracksV2")?.optInt("totalCount", 0) ?: 0))
            .put("uri", "spotify:album:$id")
    }
}

sealed interface SearchTopResult {
    data class TrackItem(val item: Track) : SearchTopResult
    data class AlbumItem(val item: SimpleAlbum) : SearchTopResult
    data class ArtistItem(val item: Artist) : SearchTopResult
    data class PlaylistItem(val item: SimplePlaylist) : SearchTopResult
}

val SearchTopResult.name: String
    get() = when (this) {
        is SearchTopResult.TrackItem -> item.name
        is SearchTopResult.AlbumItem -> item.name
        is SearchTopResult.ArtistItem -> item.name
        is SearchTopResult.PlaylistItem -> item.name
    }
