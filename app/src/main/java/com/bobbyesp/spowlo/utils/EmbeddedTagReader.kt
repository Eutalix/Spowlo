package com.bobbyesp.spowlo.utils

import android.net.Uri
import android.util.Log
import com.bobbyesp.spowlo.App.Companion.context
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import org.jaudiotagger.tag.images.Artwork
import java.io.File
import java.security.MessageDigest
import java.util.logging.Level
import java.util.logging.Logger

data class EmbeddedTags(
    val title: String = "",
    val artist: String = "",
    val spotifyUrl: String = "",
    val taggedBySpowlo: Boolean = false,
    val artworkUri: String = "",
    val hasArtwork: Boolean = false,
    val hasLyrics: Boolean = false,
    val durationSeconds: Double = 0.0,
)

/**
 * Reads the custom tags Spowlo writes next to the audio: the provenance marker, the Spotify
 * track URL and the embedded cover. Formats jaudiotagger cannot open (Opus, AAC, SAF URIs)
 * come back empty and the importer falls back to [android.media.MediaMetadataRetriever].
 */
object EmbeddedTagReader {
    private const val TAG = "EmbeddedTags"
    private const val MARKER = "SPOWLO"
    private const val M4A_MARKER = "----:com.apple.iTunes:SPOWLO"
    private const val URL = "WOAS"
    private const val M4A_URL = "----:com.apple.iTunes:WOAS"
    private const val ARTWORK_DIRECTORY = "imported-artwork"

    private val SPOTIFY_TRACK_URL = Regex("https?://open\\.spotify\\.com/track/[A-Za-z0-9]+")

    init {
        runCatching { Logger.getLogger("org.jaudiotagger").level = Level.OFF }
    }

    fun read(path: String): EmbeddedTags {
        if (path.startsWith("content://")) return EmbeddedTags()
        return runCatching { readFile(File(path)) }
            .onFailure { Log.w(TAG, "Cannot read tags for $path", it) }
            .getOrDefault(EmbeddedTags())
    }

    private fun readFile(file: File): EmbeddedTags {
        if (!file.isFile) return EmbeddedTags()
        val audio = AudioFileIO.read(file)
        val duration = audio.audioHeader?.trackLength?.toDouble() ?: 0.0
        val tag = audio.tag ?: return EmbeddedTags(durationSeconds = duration)
        val taggedBySpowlo = hasSpowloMarker(tag)
        val artwork = runCatching { tag.firstArtwork }.getOrNull()
        return EmbeddedTags(
            title = tag.getFirst(FieldKey.TITLE).trim(),
            artist = tag.getFirst(FieldKey.ARTIST).trim(),
            spotifyUrl = spotifyTrackUrl(tag),
            taggedBySpowlo = taggedBySpowlo,
            artworkUri = if (taggedBySpowlo) cacheArtwork(file, artwork) else "",
            hasArtwork = artwork != null,
            hasLyrics = runCatching { tag.getFirst(FieldKey.LYRICS) }.getOrDefault("").isNotBlank(),
            durationSeconds = duration,
        )
    }

    private fun hasSpowloMarker(tag: Tag): Boolean {
        if (tag.getFirst(MARKER).isNotBlank()) return true
        if (tag.getFirst(M4A_MARKER).isNotBlank()) return true
        return tag.getFields("TXXX").any { field ->
            val body = (field as? AbstractID3v2Frame)?.body as? FrameBodyTXXX
            body?.description == MARKER && body.text.isNotBlank()
        }
    }

    private fun spotifyTrackUrl(tag: Tag): String {
        val raw = tag.getFirst(URL).ifBlank { tag.getFirst(M4A_URL) }
        return SPOTIFY_TRACK_URL.find(raw)?.value.orEmpty()
    }

    private fun cacheArtwork(file: File, artwork: Artwork?): String {
        val bytes = artwork?.binaryData ?: return ""
        if (bytes.isEmpty()) return ""
        return runCatching {
            val directory = File(context.noBackupFilesDir, ARTWORK_DIRECTORY)
            if (!directory.isDirectory && !directory.mkdirs()) return ""
            val extension = if (artwork.mimeType?.contains("png", true) == true) "png" else "jpg"
            val target = File(directory, "${file.absolutePath.sha256()}.$extension")
            if (target.length() != bytes.size.toLong()) target.writeBytes(bytes)
            Uri.fromFile(target).toString()
        }.getOrElse {
            Log.w(TAG, "Cannot cache artwork of ${file.absolutePath}", it)
            ""
        }
    }

    private fun String.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
}
