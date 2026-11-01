package com.bobbyesp.spowlo.utils

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.bobbyesp.spowlo.App
import com.bobbyesp.spowlo.App.Companion.context
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.utils.PreferencesUtil.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Adds audio files that already exist in the configured download directories to the
 * download history. Files are matched by path, so running this repeatedly is safe.
 */
object HistoryImporter {
    private const val TAG = "HistoryImporter"
    private const val IMPORTED_EXTRACTOR = "Imported"
    private const val SPOWLO_EXTRACTOR = "Spowlo"

    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "opus", "ogg", "flac", "wav")

    data class ImportResult(val imported: Int, val scanned: Int)

    /** True when at least one configured directory can be read, so an import is worth running. */
    fun hasReadableSource(): Boolean {
        val audioDir = File(App.audioDownloadDir)
        if (audioDir.isDirectory && audioDir.canRead()) return true
        if (!PreferencesUtil.getValue(SDCARD_DOWNLOAD)) return false
        val uri = SDCARD_URI.getString()
        return uri.isNotBlank() &&
                DocumentFile.fromTreeUri(context, Uri.parse(uri))?.canRead() == true
    }

    /** True when the configured folder can only be listed after granting all files access. */
    fun needsStorageAccessToScan(): Boolean {
        if (FilesUtil.hasAllFilesAccess()) return false
        if (PreferencesUtil.getValue(SDCARD_DOWNLOAD)) {
            val uri = SDCARD_URI.getString()
            if (uri.isNotBlank() &&
                DocumentFile.fromTreeUri(context, Uri.parse(uri))?.canRead() == true
            ) return false
        }
        return true
    }

    suspend fun importExistingFiles(): ImportResult = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<Candidate>()
        collectFiles(App.audioDownloadDir, candidates)
        if (PreferencesUtil.getValue(SDCARD_DOWNLOAD)) {
            collectFromTree(SDCARD_URI.getString(), candidates)
        }
        if (candidates.isEmpty()) return@withContext ImportResult(imported = 0, scanned = 0)

        runCatching {
            val knownPaths = DatabaseUtil.getMediaPaths().toMutableSet()
            var imported = 0
            candidates.forEach { candidate ->
                if (candidate.path in knownPaths) {
                    upgradeImported(candidate)
                    return@forEach
                }
                if (DatabaseUtil.insertImportedInfo(candidate.toDownloadedSongInfo())) {
                    knownPaths += candidate.path
                    imported++
                }
            }
            ImportResult(imported = imported, scanned = candidates.size)
        }.getOrElse {
            Log.e(TAG, "Failed to import existing files", it)
            ImportResult(imported = 0, scanned = candidates.size)
        }
    }

    private data class Candidate(
        val path: String,
        val fileName: String,
        val lastModified: Long,
    )

    private fun collectFiles(root: String, out: MutableList<Candidate>) {
        if (root.isBlank()) return
        val directory = File(root)
        if (!directory.isDirectory) return
        runCatching {
            directory.walkTopDown().forEach { file ->
                if (file.isFile && isAudioName(file.name)) {
                    out += Candidate(file.absolutePath, file.name, file.lastModified())
                }
            }
        }.onFailure { Log.w(TAG, "Cannot scan $root", it) }
    }

    private fun collectFromTree(treeUri: String, out: MutableList<Candidate>) {
        if (treeUri.isBlank()) return
        val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return
        runCatching {
            collectDocuments(root, out)
        }.onFailure { Log.w(TAG, "Cannot scan $treeUri", it) }
    }

    private fun collectDocuments(dir: DocumentFile, out: MutableList<Candidate>) {
        dir.listFiles().forEach { document ->
            when {
                document.isDirectory -> collectDocuments(document, out)

                document.isFile &&
                        (isAudioName(document.name) || document.type?.startsWith("audio/") == true) ->
                    out += Candidate(
                        path = document.uri.toString(),
                        fileName = document.name.orEmpty(),
                        lastModified = document.lastModified(),
                    )
            }
        }
    }

    private fun isAudioName(name: String?): Boolean =
        name != null && !name.startsWith(".") &&
                name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS

    private data class ReadMetadata(
        val title: String,
        val artist: String,
        val durationSeconds: Double,
    )

    private val EMPTY_METADATA = ReadMetadata(title = "", artist = "", durationSeconds = 0.0)

    private fun Candidate.toDownloadedSongInfo(): DownloadedSongInfo {
        val (fileNameArtist, fileNameTitle) = parseFileName(fileName)
        val metadata = readMetadata(path)
        val tags = EmbeddedTagReader.read(path)
        return DownloadedSongInfo(
            songName = tags.title.ifBlank { metadata.title }.ifBlank { fileNameTitle }.ifBlank { fileName },
            songAuthor = tags.artist.ifBlank { metadata.artist }.ifBlank { fileNameArtist },
            songUrl = tags.spotifyUrl,
            thumbnailUrl = tags.artworkUri,
            songPath = path,
            songDuration = metadata.durationSeconds,
            extractor = if (tags.taggedBySpowlo) SPOWLO_EXTRACTOR else IMPORTED_EXTRACTOR,
            downloadedAt = lastModified.takeIf { it > 0L } ?: System.currentTimeMillis(),
        )
    }

    /** Fills in entries imported before Spowlo read its own tags, without touching real downloads. */
    private suspend fun upgradeImported(candidate: Candidate) {
        val existing = DatabaseUtil.getInfoByPath(candidate.path) ?: return
        if (existing.extractor != IMPORTED_EXTRACTOR) return
        if (existing.songUrl.isNotBlank() && existing.thumbnailUrl.isNotBlank()) return

        val tags = EmbeddedTagReader.read(candidate.path)
        if (!tags.taggedBySpowlo && tags.spotifyUrl.isBlank()) return

        DatabaseUtil.updateSongInfo(
            existing.copy(
                songUrl = existing.songUrl.ifBlank { tags.spotifyUrl },
                thumbnailUrl = existing.thumbnailUrl.ifBlank { tags.artworkUri },
                extractor = if (tags.taggedBySpowlo) SPOWLO_EXTRACTOR else existing.extractor,
            )
        )
    }

    /** Splits the default `{artists} - {title}` output name into an artist and a title. */
    private fun parseFileName(fileName: String): Pair<String, String> {
        val base = fileName.substringBeforeLast('.', fileName).trim()
        val separator = base.indexOf(" - ")
        return if (separator > 0) {
            base.substring(0, separator).trim() to base.substring(separator + 3).trim()
        } else {
            "" to base
        }
    }

    private fun readMetadata(path: String): ReadMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            if (path.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(path))
            } else {
                retriever.setDataSource(path)
            }
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            ReadMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    .orEmpty().trim(),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    .orEmpty().trim(),
                durationSeconds = durationMs / 1000.0,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read metadata for $path", e)
            EMPTY_METADATA
        } finally {
            runCatching { retriever.release() }
        }
    }
}
