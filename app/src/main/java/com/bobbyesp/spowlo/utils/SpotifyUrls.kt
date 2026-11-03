package com.bobbyesp.spowlo.utils

object SpotifyUrls {
    private val TRACK_ID = Regex("""track[/:]([A-Za-z0-9]{22})""")

    /** Accepts open.spotify.com links and spotify: URIs. */
    fun trackId(url: String): String? =
        TRACK_ID.find(url)?.groupValues?.getOrNull(1)
}
