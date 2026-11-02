package com.bobbyesp.spowlo.utils

import com.bobbyesp.spowlo.database.DownloadedSongInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val mimeType: String, val extension: String) {
    M3U("audio/x-mpegurl", "m3u"),
    CSV("text/csv", "csv"),
}

/** Turns library entries into a playlist or spreadsheet the user can take elsewhere. */
object LibraryExporter {

    fun build(songs: List<DownloadedSongInfo>, format: ExportFormat): String = when (format) {
        ExportFormat.M3U -> buildM3u(songs)
        ExportFormat.CSV -> buildCsv(songs)
    }

    fun suggestedName(format: ExportFormat): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        return "spowlo-downloads-$stamp.${format.extension}"
    }

    private fun buildM3u(songs: List<DownloadedSongInfo>): String = buildString {
        appendLine("#EXTM3U")
        songs.forEach { song ->
            appendLine("#EXTINF:${song.songDuration.toInt()},${song.songAuthor} - ${song.songName}")
            appendLine(song.songPath)
        }
    }

    private fun buildCsv(songs: List<DownloadedSongInfo>): String = buildString {
        appendLine("title,artist,duration_seconds,url,downloaded_at,path")
        songs.forEach { song ->
            appendLine(
                listOf(
                    song.songName,
                    song.songAuthor,
                    song.songDuration.toInt().toString(),
                    song.songUrl,
                    if (song.downloadedAt <= 0L) ""
                    else SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT)
                        .format(Date(song.downloadedAt)),
                    song.songPath,
                ).joinToString(",") { escape(it) }
            )
        }
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}
