package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.bobbyesp.spowlo.App
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.utils.EmbeddedTagReader
import com.bobbyesp.spowlo.utils.EmbeddedTags
import com.bobbyesp.spowlo.utils.FilesUtil.getFileSize
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import com.bobbyesp.spowlo.utils.toFileSizeText
import java.io.File
import java.util.Locale

internal data class SavedFileDetails(
    val format: String = "",
    val bitrateKbps: Int = 0,
    val sizeBytes: Long = 0L,
    val hasTags: Boolean = false,
    val hasArtwork: Boolean = false,
    val hasLyrics: Boolean = false,
)

internal fun readSavedFile(song: DownloadedSongInfo): SavedFileDetails {
    val path = song.songPath
    val tags = runCatching { EmbeddedTagReader.read(path) }.getOrDefault(EmbeddedTags())
    val file = runCatching { File(path) }.getOrNull()
    val sidecarLyrics = file?.parentFile?.listFiles()?.any {
        it.isFile && it.extension.equals("lrc", ignoreCase = true) &&
            it.nameWithoutExtension.equals(file.nameWithoutExtension, ignoreCase = true)
    } ?: false
    return SavedFileDetails(
        format = file?.extension?.uppercase(Locale.ROOT).orEmpty(),
        bitrateKbps = readBitrateKbps(path),
        sizeBytes = runCatching { path.getFileSize() }.getOrDefault(0L),
        hasTags = tags.title.isNotBlank() && tags.artist.isNotBlank(),
        hasArtwork = tags.hasArtwork,
        hasLyrics = tags.hasLyrics || sidecarLyrics,
    )
}

private fun readBitrateKbps(path: String): Int {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            ?.toIntOrNull()?.div(1000) ?: 0
    } catch (_: Exception) {
        0
    } finally {
        runCatching { retriever.release() }
    }
}

@Composable
internal fun savedDetailsLine(details: SavedFileDetails, downloadedAt: Long): String {
    val date = GeneralTextUtils.formatDate(downloadedAt)
    val size = if (details.sizeBytes > 0L) details.sizeBytes.toFileSizeText() else ""
    val format = buildString {
        append(details.format.ifBlank { "Audio" })
        if (details.bitrateKbps > 0) append(" ${details.bitrateKbps}")
    }
    return listOf(format, size, date)
        .filter { it.isNotBlank() }
        .joinToString("  ·  ")
}

internal fun folderLabel(path: String): String {
    if (path.startsWith("content://")) return "your library"
    val root = Environment.getExternalStorageDirectory().absolutePath
    val parent = File(path).parent.orEmpty()
    return parent.removePrefix(root).trim('/').ifBlank { "your download folder" }
}

internal fun openContainingFolder(path: String): Boolean {
    val folder = folderUri(path) ?: return false
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(folder, DocumentsContract.Document.MIME_TYPE_DIR)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (launchIntent(view)) return true
    val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        type = "*/*"
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, folder)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return launchIntent(picker)
}

private fun folderUri(path: String): Uri? {
    if (path.startsWith("content://")) {
        val uri = Uri.parse(path)
        val authority = uri.authority ?: return null
        val id = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return null
        val parentId = id.substringBeforeLast('/', "")
        return runCatching {
            if (parentId.isNotEmpty()) {
                DocumentsContract.buildDocumentUri(authority, parentId)
            } else {
                DocumentsContract.buildRootUri(authority, id.substringBefore(':'))
            }
        }.getOrNull()
    }
    val parent = File(path).parentFile ?: return null
    val root = Environment.getExternalStorageDirectory().absolutePath
    val relative = parent.absolutePath.removePrefix(root).trim('/')
    val documentId = if (relative.isBlank()) "primary:" else "primary:$relative"
    return runCatching {
        DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, documentId)
    }.getOrNull()
}

private fun launchIntent(intent: Intent): Boolean = runCatching {
    App.context.startActivity(intent)
    true
}.onFailure {
    Log.w("Spowlo", "Couldn't open ${intent.data}", it)
}.getOrDefault(false)

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

@Composable
internal fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val manager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }
    var online by remember { mutableStateOf(manager?.activeNetwork != null) }
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online = true
            }

            override fun onLost(network: Network) {
                online = manager?.activeNetwork != null
            }
        }
        runCatching { manager?.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { manager?.unregisterNetworkCallback(callback) } }
    }
    return online
}
