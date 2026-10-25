package com.bobbyesp.spowlo.features.spotify_api.data.remote.web

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
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
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

    /** Persisted GraphQL query hashes bundled with the current web player build. */
    private val OPERATION_HASHES = mapOf(
        "searchDesktop" to "eef7cc54888d91bdd6802623477873caa3948ae173a0c34fd86827b267e94c03",
        "getTrack" to "fe16b8083eec2305854db612ca75589bcb71e009dc7a4d014f29bd534a4c01f5",
        "getAlbum" to "0b6ce3511e1b02e4a40fa95440f992acbe85080eb308af9194322d708fc1914f",
        "queryArtistOverview" to "9f8134ef565e78621f1e1793555bd6633c5ac144ae0f89604ed3ae3f80b3c8e6",
        "fetchPlaylist" to "21ea11a6ac75a5f3e01fd365d29c18e0a4a8b7dc09f89bbb5d73910190c6acaf",
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    /** Play counts and monthly listeners, only returned by the detail queries. */
    private val playCounts = ConcurrentHashMap<String, Long>()
    private val monthlyListeners = ConcurrentHashMap<String, Long>()

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

    suspend fun getPlaylist(playlistId: String): Playlist {
        val root = graphql(
            "fetchPlaylist",
            JSONObject()
                .put("uri", "spotify:playlist:$playlistId")
                .put("offset", 0)
                .put("limit", PLAYLIST_PAGE_SIZE)
                .put("enableWatchFeedEntrypoint", false),
        )
        val node = root.optJSONObject("data")?.optJSONObject("playlistV2")
            ?: throw SpotifyWebException(200, "Playlist $playlistId was not found")

        return decode(playlistJson(node).toString())
    }

    suspend fun getPlaylistTracks(playlistId: String, limit: Int, offset: Int): PagingObject<PlaylistTrack> {
        val root = graphql(
            "fetchPlaylist",
            JSONObject()
                .put("uri", "spotify:playlist:$playlistId")
                .put("offset", offset)
                .put("limit", limit)
                .put("enableWatchFeedEntrypoint", false),
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

    private suspend fun search(query: String, limit: Int, offset: Int): JSONObject {
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

    private suspend fun graphql(operation: String, variables: JSONObject): JSONObject =
        withContext(Dispatchers.IO) {
            val hash = OPERATION_HASHES[operation]
                ?: throw IllegalArgumentException("Unknown Spotify operation: $operation")

            var session = SpotifyWebAuth.session()
            var response = execute(session, operation, hash, variables)

            // The client token expires independently of the access token.
            if (response.code == 401 || response.code == 400 || response.code == 403) {
                Log.w(TAG, "$operation returned HTTP ${response.code}, refreshing session")
                SpotifyWebAuth.invalidate()
                session = SpotifyWebAuth.session(forceRefresh = true)
                response = execute(session, operation, hash, variables)
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
                put("Authorization", "Bearer ${session.accessToken}")
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

    private fun simpleAlbumJson(node: JSONObject): JSONObject {
        val uri = node.optString("uri")
        val id = node.optString("id").ifBlank { uri.substringAfterLast(':') }
        val cover = node.optJSONObject("coverArt")?.optJSONArray("sources")
            ?: node.optJSONObject("images")?.optJSONArray("items")
                ?.optJSONObject(0)?.optJSONArray("sources")

        val releaseDate = node.optJSONObject("date")?.optString("isoString")
            ?.take(10)
            ?: node.optJSONObject("date")?.optInt("year")?.toString()
            ?: ""

        return JSONObject()
            .put("album_type", node.optString("type", "ALBUM").lowercase())
            .put("total_tracks", node.optInt("totalTracks", node.optJSONObject("tracksV2")?.optInt("totalCount", 0) ?: 0))
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

    private fun simplePlaylistJson(node: JSONObject): JSONObject {
        val uri = node.optString("uri")
        val id = uri.substringAfterLast(':')
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
        val releaseDate = node.optJSONObject("date")?.optString("isoString")?.take(10).orEmpty()
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
