package com.bobbyesp.spowlo.utils

import android.util.Log
import com.adamratzman.spotify.models.Track
import com.bobbyesp.library.domain.model.SpotifySong
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyWebApi
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyWebAuth

data class SpotifyCollection(
    val url: String,
    val name: String,
    val artist: String,
    val artworkUrl: String,
    val songs: List<SpotifySong>,
)

/** Fetches track metadata for the downloader through [SpotifyWebApi]. */
object SpotifyMetadataFetcher {

    private const val TAG = "SpotifyMetadataFetcher"

    private const val MAX_TRACKS = 10_000

    /** Drops the cached anonymous session. */
    suspend fun resetApi() {
        SpotifyWebAuth.invalidate()
    }

    /** [Track] metadata for a Spotify URL, or null when it is not a track or the lookup fails. */
    suspend fun fetchTrackFromUrl(spotifyUrl: String): Track? {
        return try {
            val normalizedUrl = UrlValidator.normalize(spotifyUrl)

            val trackId = if (normalizedUrl.contains("/track/")) {
                normalizedUrl.substringAfter("/track/").substringBefore("?").substringBefore("/")
            } else {
                return null
            }

            if (trackId.isBlank()) return null

            val track = SpotifyWebApi.getTrack(trackId)
            Log.d(TAG, "Fetched metadata for ${track.name} (${track.id})")
            track
        } catch (e: Exception) {
            Log.e(TAG, "Could not fetch metadata for $spotifyUrl: ${e.message}", e)
            null
        }
    }

    suspend fun fetchCollectionFromUrl(spotifyUrl: String): SpotifyCollection? {
        val normalizedUrl = UrlValidator.normalize(spotifyUrl)
        return try {
            when (UrlValidator.classify(normalizedUrl)) {
                UrlValidator.Type.SpotifyPlaylist -> fetchPlaylist(normalizedUrl)
                UrlValidator.Type.SpotifyAlbum -> fetchAlbum(normalizedUrl)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not fetch collection for $spotifyUrl: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchPlaylist(url: String): SpotifyCollection {
        val id = url.spotifyIdAfter("/playlist/")
        val playlist = SpotifyWebApi.getPlaylist(id)

        val tracks = mutableListOf<Track>()
        var offset = 0
        while (offset < MAX_TRACKS) {
            val page = SpotifyWebApi.getPlaylistTracks(id, SpotifyWebApi.PLAYLIST_PAGE_SIZE, offset)
            if (page.items.isEmpty()) break
            tracks += page.items.mapNotNull { it.track?.asTrack }
            offset += SpotifyWebApi.PLAYLIST_PAGE_SIZE
            if (offset >= page.total) break
        }

        Log.d(TAG, "Resolved playlist ${playlist.name} with ${tracks.size} songs")
        return SpotifyCollection(
            url = "https://open.spotify.com/playlist/$id",
            name = playlist.name,
            artist = playlist.owner.displayName ?: "",
            artworkUrl = playlist.images.firstOrNull()?.url ?: "",
            songs = tracks.map { it.toSpotifySong() },
        )
    }

    private suspend fun fetchAlbum(url: String): SpotifyCollection {
        val id = url.spotifyIdAfter("/album/")
        val album = SpotifyWebApi.getAlbum(id)

        val tracks = album.tracks.items.toMutableList()
        var offset = 0
        var total = maxOf(album.totalTracks, album.tracks.total)
        while (offset < MAX_TRACKS) {
            val page = SpotifyWebApi.getAlbumTracks(id, SpotifyWebApi.ALBUM_PAGE_SIZE, offset)
            if (page.items.isEmpty()) break
            tracks += page.items
            offset += SpotifyWebApi.ALBUM_PAGE_SIZE
            total = page.total
            if (offset >= total) break
        }

        Log.d(TAG, "Resolved album ${album.name} with ${tracks.size} songs")
        return SpotifyCollection(
            url = "https://open.spotify.com/album/$id",
            name = album.name,
            artist = album.artists.joinToString(", ") { it.name },
            artworkUrl = album.images.firstOrNull()?.url ?: "",
            songs = tracks.map { it.toSpotifySong(album) },
        )
    }

    private fun String.spotifyIdAfter(segment: String): String =
        substringAfter(segment).substringBefore("?").substringBefore("/").trim()
}
