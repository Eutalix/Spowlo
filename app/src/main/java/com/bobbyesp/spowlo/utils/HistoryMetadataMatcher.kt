package com.bobbyesp.spowlo.utils

import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests
import java.util.Locale
import kotlin.math.abs

/**
 * Resolves Spotify metadata from an entry's track URL or a name-and-artist search. Matching is
 * conservative so covers, remixes, or live versions do not replace the original data.
 */
object HistoryMetadataMatcher {
    private val BRACKETS = Regex("\\([^)]*\\)|\\[[^]]*]")
    private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
    private val MULTIPLE_SPACES = Regex("\\s+")

    private const val MIN_SCORE = 4

    suspend fun findTrack(song: DownloadedSongInfo): Track? {
        if (song.songUrl.isNotBlank()) {
            SpotifyMetadataFetcher.fetchTrackFromUrl(song.songUrl)?.let { return it }
        }
        val title = song.songName.trim()
        if (title.isBlank()) return null

        val queries = buildList {
            add(listOf(song.songAuthor.trim(), title).filter { it.isNotBlank() }.joinToString(" "))
            if (song.songAuthor.isNotBlank()) add(title)
        }.distinct()

        queries.forEach { query ->
            val match = runCatching { SpotifyApiRequests.provideSearchTracks(query) }
                .getOrDefault(emptyList())
                .map { it to score(song, it) }
                .filter { it.second >= MIN_SCORE }
                .maxByOrNull { it.second }
                ?.first
            if (match != null) return match
        }
        return null
    }

    fun DownloadedSongInfo.withTrackMetadata(track: Track): DownloadedSongInfo = copy(
        songName = track.name.ifBlank { songName },
        songAuthor = track.artists.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: songAuthor,
        songUrl = track.externalUrls.spotify ?: "https://open.spotify.com/track/${track.id}",
        thumbnailUrl = track.album.images?.firstOrNull()?.url
            ?.takeIf { it.isNotBlank() } ?: thumbnailUrl,
        songDuration = track.durationMs / 1000.0,
    )

    private fun score(song: DownloadedSongInfo, track: Track): Int {
        val title = normalize(song.songName)
        val trackTitle = normalize(track.name)
        val titleScore = when {
            title.isBlank() || trackTitle.isBlank() -> 0
            title == trackTitle -> 4
            title.contains(trackTitle) || trackTitle.contains(title) -> 2
            else -> 0
        }
        if (titleScore == 0) return 0

        val artistScore = when {
            song.songAuthor.isBlank() -> 1
            artistMatches(song.songAuthor, track) -> 2
            else -> 0
        }
        if (artistScore == 0) return 0

        return titleScore + artistScore + durationScore(song.songDuration, track.durationMs)
    }

    private fun durationScore(duration: Double, durationMs: Int): Int {
        if (duration <= 0.0 || durationMs <= 0) return 0
        val difference = abs(duration - durationMs / 1000.0)
        return when {
            difference <= 3 -> 2
            difference <= 10 -> 1
            else -> 0
        }
    }

    private fun artistMatches(author: String, track: Track): Boolean {
        val query = normalize(author)
        if (query.isBlank()) return true
        return track.artists.any { artist ->
            val name = normalize(artist.name)
            name.isNotBlank() && (query == name || query.contains(name) || name.contains(query))
        }
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(BRACKETS, " ")
        .replace(NON_ALPHANUMERIC, " ")
        .trim()
        .replace(MULTIPLE_SPACES, " ")
}
