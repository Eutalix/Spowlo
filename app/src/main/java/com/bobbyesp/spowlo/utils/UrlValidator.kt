package com.bobbyesp.spowlo.utils

/**
 * URL normalization, validation, and classification helper.
 * Centralizes handling of Spotify/YouTube URLs and common clipboard artifacts.
 */
object UrlValidator {

    // Accepts: http(s), spotify: URIs, spotify.link, open.spotify.com, youtube.com, youtu.be, music.youtube.com
    private val supported = Regex(
        pattern = """^\s*(?:https?://|spotify:|spotify\.link/|(?:www\.)?(?:open\.spotify\.com|music\.youtube\.com|youtu\.be|youtube\.com)/).+""",
        option = RegexOption.IGNORE_CASE
    )

    /**
     * Normalize raw clipboard text:
     * - Trim
     * - Remove Android intent wrapper markers
     * - Remove surrounding angle brackets (formatted text)
     * - Strip common trailing punctuation from messengers
     */
    fun normalize(raw: String): String {
        var s = raw.trim()
            .removePrefix("intent:")
            .removeSuffix("#Intent;end")
            .removeSurrounding("<", ">")
        s = s.trimEnd('.', ',', ';', ')', ']')
        return s
    }

    /**
     * Check if the given string is a supported URL after normalization.
     */
    fun isSupported(raw: String): Boolean = supported.containsMatchIn(normalize(raw))

    /**
     * High-level URL types important to the UI.
     */
    enum class Type { SpotifyTrack, SpotifyAlbum, SpotifyPlaylist, SpotifyArtist, Other }

    /**
     * Classify URL to drive the UI flow (download directly vs. open settings sheet).
     * - Spotify types are matched via open.spotify.com paths and spotify: URIs
     * - Everything else (YouTube/YT Music/spotify.link/etc.) is Other
     */
    fun classify(raw: String): Type {
        val url = normalize(raw)
        val track    = Regex("""^https?://open\.spotify\.com/track/[^/?#]+""", RegexOption.IGNORE_CASE)
        val album    = Regex("""^https?://open\.spotify\.com/album/[^/?#]+""", RegexOption.IGNORE_CASE)
        val playlist = Regex("""^https?://open\.spotify\.com/playlist/[^/?#]+""", RegexOption.IGNORE_CASE)
        val artist   = Regex("""^https?://open\.spotify\.com/artist/[^/?#]+""", RegexOption.IGNORE_CASE)

        return when {
            track.containsMatchIn(url)    || url.startsWith("spotify:track:", true)    -> Type.SpotifyTrack
            album.containsMatchIn(url)    || url.startsWith("spotify:album:", true)    -> Type.SpotifyAlbum
            playlist.containsMatchIn(url) || url.startsWith("spotify:playlist:", true) -> Type.SpotifyPlaylist
            artist.containsMatchIn(url)   || url.startsWith("spotify:artist:", true)   -> Type.SpotifyArtist
            else -> Type.Other
        }
    }
}