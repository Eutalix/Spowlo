package com.bobbyesp.spowlo.utils

import com.bobbyesp.spowlo.Downloader
import kotlinx.coroutines.flow.first

object RedownloadUtil {

    suspend fun redownload(url: String, name: String): Boolean {
        if (url.isBlank()) return false
        val path = runCatching { existingPathFor(url) }.getOrDefault("")
        return Downloader.redownloadTrack(url, name, path)
    }

    private suspend fun existingPathFor(url: String): String {
        val trackId = SpotifyUrls.trackId(url) ?: return ""
        return DatabaseUtil.getMediaInfo().first()
            .firstOrNull { SpotifyUrls.trackId(it.songUrl) == trackId }
            ?.songPath
            .orEmpty()
    }
}
