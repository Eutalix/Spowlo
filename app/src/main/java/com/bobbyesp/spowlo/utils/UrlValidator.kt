package com.bobbyesp.spowlo.utils

import java.util.Locale

object UrlValidator {
    enum class Type {
        SpotifyTrack,
        SpotifyAlbum,
        SpotifyArtist,
        SpotifyPlaylist,
        Other
    }

    data class SpotifyLink(val type: String, val id: String)

    private val spotifyLinkPattern = Regex(
        "spotify:(track|album|playlist|artist):([A-Za-z0-9]+)" +
            "|https?://(?:open|play)\\.spotify\\.com/(?:intl-[a-z-]+/)?(track|album|playlist|artist)/([A-Za-z0-9]+)",
        RegexOption.IGNORE_CASE
    )

    fun normalize(url: String): String {
        val trimmed = url.trim()
        val schemeFixed = when {
            trimmed.startsWith("https://", ignoreCase = true) -> "https://" + trimmed.substring(8)
            trimmed.startsWith("http://", ignoreCase = true) -> "http://" + trimmed.substring(7)
            else -> trimmed
        }
        return schemeFixed
            .replace("https://open.spotify.com/intl-[a-z]+/".toRegex(), "https://open.spotify.com/")
    }

    fun classify(url: String): Type {
        val normalized = normalize(url).lowercase(Locale.ROOT)
        return when {
            normalized.contains("/track/") -> Type.SpotifyTrack
            normalized.contains("/album/") -> Type.SpotifyAlbum
            normalized.contains("/artist/") -> Type.SpotifyArtist
            normalized.contains("/playlist/") -> Type.SpotifyPlaylist
            else -> Type.Other
        }
    }

    fun isDownloadable(url: String): Boolean = when (classify(url)) {
        Type.SpotifyTrack, Type.SpotifyAlbum, Type.SpotifyPlaylist -> true
        Type.SpotifyArtist, Type.Other -> false
    }

    /** Returns the Spotify item behind [text], if it contains a supported link or URI. */
    fun parseSpotifyLink(text: String): SpotifyLink? {
        val match = spotifyLinkPattern.find(text) ?: return null
        val (type, id) = if (match.groupValues[1].isNotEmpty()) {
            match.groupValues[1] to match.groupValues[2]
        } else {
            match.groupValues[3] to match.groupValues[4]
        }
        return SpotifyLink(type.lowercase(Locale.ROOT), id)
    }
}
