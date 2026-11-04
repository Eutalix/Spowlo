package com.bobbyesp.spowlo.features.spotify_api.data.remote.web

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class UserProfile(
    val id: String,
    val name: String,
    val imageUrl: String,
)

data class UserPlaylist(
    val id: String,
    val name: String,
    val owner: String,
    val imageUrl: String,
    val isSpotifyOwned: Boolean,
)

data class UserTrack(
    val id: String,
    val name: String,
    val artists: String,
    val imageUrl: String,
    val durationMs: Long,
    val explicit: Boolean,
) {
    val url: String get() = "https://open.spotify.com/track/$id"
}

data class UserTrackPage(val tracks: List<UserTrack>, val total: Int)

class SpotifyAccountException(message: String) : IOException(message)

/**
 * Account library through the web player's pathfinder queries. The official Web API answers
 * with HTTP 429 for long stretches, pathfinder does not.
 */
object SpotifyUserApi {

    const val PAGE_SIZE = 50
    private const val MAX_PLAYLISTS = 1000

    suspend fun profile(): UserProfile {
        ensureToken()
        val profile = SpotifyWebApi.accountProfile()
        return UserProfile(
            id = profile.optString("username").ifBlank { profile.optString("accountId") },
            name = profile.optString("name").ifBlank { profile.optString("username") },
            imageUrl = firstSource(profile.optJSONObject("avatar")),
        )
    }

    suspend fun playlists(): List<UserPlaylist> {
        ensureToken()
        val result = mutableListOf<UserPlaylist>()
        var offset = 0
        while (offset < MAX_PLAYLISTS) {
            val page = SpotifyWebApi.accountPlaylists(PAGE_SIZE, offset)
            val items = page.optJSONArray("items") ?: break

            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index)
                    ?.optJSONObject("item")
                    ?.optJSONObject("data")
                    ?: continue
                if (item.optString("__typename") == "Folder") continue
                parsePlaylist(item)?.let(result::add)
            }

            offset += PAGE_SIZE
            val total = page.optInt("totalCount", result.size)
            if (items.length() < PAGE_SIZE || offset >= total) break
        }
        return result
    }

    suspend fun likedCount(): Int {
        ensureToken()
        return SpotifyWebApi.accountLikedTracks(1, 0).optInt("totalCount", 0)
    }

    suspend fun likedTracks(offset: Int, limit: Int = PAGE_SIZE): UserTrackPage {
        ensureToken()
        val page = SpotifyWebApi.accountLikedTracks(limit, offset)
        val items = page.optJSONArray("items") ?: JSONArray()
        val tracks = (0 until items.length()).mapNotNull { index ->
            parseTrack(items.optJSONObject(index)?.optJSONObject("track"))
        }
        return UserTrackPage(tracks, page.optInt("totalCount", tracks.size))
    }

    private suspend fun ensureToken() {
        if (SpotifyUserSession.accessToken() == null) {
            throw SpotifyAccountException("No Spotify account is connected")
        }
    }

    private fun parsePlaylist(json: JSONObject): UserPlaylist? {
        val uri = json.optString("uri")
        val id = uri.substringAfterLast(':').takeIf { it.isNotBlank() } ?: return null
        val owner = json.optJSONObject("ownerV2")?.optJSONObject("data")
        val ownerId = owner?.optString("id").orEmpty()
        val ownerUri = owner?.optString("uri").orEmpty()
        val cover = json.optJSONObject("images")
            ?.optJSONArray("items")
            ?.optJSONObject(0)

        return UserPlaylist(
            id = id,
            name = json.optString("name"),
            owner = owner?.optString("name").orEmpty().ifBlank { ownerId },
            imageUrl = firstSource(cover),
            isSpotifyOwned = ownerId.equals("spotify", ignoreCase = true) ||
                ownerUri.endsWith(":spotify") ||
                uri.startsWith("spotify:playlist:37i9dQ"),
        )
    }

    private fun parseTrack(wrapper: JSONObject?): UserTrack? {
        wrapper ?: return null
        val id = wrapper.optString("_uri").substringAfterLast(':').takeIf { it.isNotBlank() }
            ?: return null
        val data = wrapper.optJSONObject("data") ?: return null
        val artists = data.optJSONObject("artists")?.optJSONArray("items")

        return UserTrack(
            id = id,
            name = data.optString("name"),
            artists = artistNames(artists),
            imageUrl = firstSource(
                data.optJSONObject("albumOfTrack")?.optJSONObject("coverArt")
            ),
            durationMs = data.optJSONObject("duration")?.optLong("totalMilliseconds", 0L) ?: 0L,
            explicit = data.optJSONObject("contentRating")?.optString("label") == "EXPLICIT",
        )
    }

    private fun artistNames(artists: JSONArray?): String {
        if (artists == null) return ""
        val names = buildList {
            for (index in 0 until artists.length()) {
                artists.optJSONObject(index)
                    ?.optJSONObject("profile")
                    ?.optString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
        return names.joinToString(", ")
    }

    /** Pathfinder wraps every image list as `{ sources: [...] }`. */
    private fun firstSource(node: JSONObject?): String =
        node?.optJSONArray("sources")?.optJSONObject(0)?.optString("url").orEmpty()
}
