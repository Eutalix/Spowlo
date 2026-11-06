package com.bobbyesp.spowlo.features.spotify_api.data.remote.web

import android.util.Log
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
    val albumName: String,
    val imageUrl: String,
    val durationMs: Long,
    val explicit: Boolean,
) {
    val url: String get() = "https://open.spotify.com/track/$id"
}

data class UserTrackPage(val tracks: List<UserTrack>, val total: Int)

enum class HomeCardType { TRACK, ALBUM, ARTIST, PLAYLIST }

/**
 * One square card of a home shelf. [id] is the bare Spotify id, the matching page is chosen
 * from [type].
 */
data class HomeCard(
    val id: String,
    val type: HomeCardType,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
)

data class HomeShelf(
    val id: String,
    val title: String,
    val cards: List<HomeCard>,
)


class SpotifyAccountException(message: String) : IOException(message)

/**
 * Account library through the web player's pathfinder queries. The official Web API answers
 * with HTTP 429 for long stretches, pathfinder does not.
 */
object SpotifyUserApi {

    private const val TAG = "SpotifyUserApi"
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

    /** Shelves of the account home, in the order Spotify returns them. */
    suspend fun home(): List<HomeShelf> {
        ensureToken()
        val home = SpotifyWebApi.accountHome()
        val sections = home.optJSONObject("sectionContainer")
            ?.optJSONObject("sections")
            ?.optJSONArray("items")
            ?: return emptyList()

        return buildList {
            for (index in 0 until sections.length()) {
                val section = sections.optJSONObject(index) ?: continue
                val title = section.optJSONObject("data")
                    ?.optJSONObject("title")
                    ?.optString("transformedLabel")
                    .orEmpty()
                if (title.isBlank()) continue

                val items = section.optJSONObject("sectionItems")?.optJSONArray("items") ?: continue
                val cards = (0 until items.length()).mapNotNull { cardIndex ->
                    parseHomeCard(items.optJSONObject(cardIndex))
                }
                if (cards.isEmpty()) continue

                add(
                    HomeShelf(
                        id = section.optString("uri").ifBlank { "shelf-$index" },
                        title = title,
                        cards = cards,
                    )
                )
            }
        }.also { shelves ->
            Log.d(TAG, "home shelves: " + shelves.joinToString { "${it.title} (${it.cards.size})" })
        }
    }

    private fun parseHomeCard(item: JSONObject?): HomeCard? {
        val content = item?.optJSONObject("content") ?: return null
        val data = content.optJSONObject("data") ?: return null
        val uri = data.optString("uri").ifBlank { item.optString("uri") }
        val id = uri.substringAfterLast(':').takeIf { it.isNotBlank() } ?: return null

        return when (content.optString("__typename")) {
            "TrackResponseWrapper" -> HomeCard(
                id = id,
                type = HomeCardType.TRACK,
                title = data.optString("name"),
                subtitle = artistNames(data.optJSONObject("artists")?.optJSONArray("items")),
                imageUrl = firstSource(data.optJSONObject("albumOfTrack")?.optJSONObject("coverArt")),
            )

            "AlbumResponseWrapper" -> HomeCard(
                id = id,
                type = HomeCardType.ALBUM,
                title = data.optString("name"),
                subtitle = artistNames(data.optJSONObject("artists")?.optJSONArray("items")),
                imageUrl = firstSource(data.optJSONObject("coverArt")),
            )

            "ArtistResponseWrapper" -> HomeCard(
                id = id,
                type = HomeCardType.ARTIST,
                title = data.optJSONObject("profile")?.optString("name").orEmpty(),
                subtitle = "",
                imageUrl = firstSource(data.optJSONObject("visuals")?.optJSONObject("avatarImage")),
            )

            "PlaylistResponseWrapper" -> HomeCard(
                id = id,
                type = HomeCardType.PLAYLIST,
                title = data.optString("name"),
                subtitle = stripMarkup(data.optString("description")).ifBlank {
                    data.optJSONObject("ownerV2")?.optJSONObject("data")?.optString("name").orEmpty()
                },
                imageUrl = firstSource(
                    data.optJSONObject("images")?.optJSONArray("items")?.optJSONObject(0)
                ),
            )

            else -> null
        }
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
        val album = data.optJSONObject("albumOfTrack")

        return UserTrack(
            id = id,
            name = data.optString("name"),
            artists = artistNames(artists),
            albumName = album?.optString("name").orEmpty(),
            imageUrl = firstSource(album?.optJSONObject("coverArt")),
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

    /** Playlist descriptions arrive as HTML fragments (links, line breaks). */
    private fun stripMarkup(raw: String): String = raw
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
}
