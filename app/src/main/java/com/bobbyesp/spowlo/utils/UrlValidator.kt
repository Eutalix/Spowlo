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

    fun normalize(url: String): String {
        return url.trim()
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

    fun isSupported(url: String): Boolean = url.isNotBlank()
}