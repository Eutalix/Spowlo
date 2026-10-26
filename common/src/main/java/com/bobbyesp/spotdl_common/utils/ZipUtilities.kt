package com.bobbyesp.spotdl_common.utils

import android.content.Context
import android.system.Os
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

object ZipUtils {
    fun unzip(sourceFile: File?, targetDirectory: File) {
        ZipFile(sourceFile).use { zipFile ->
            val entries = zipFile.entries
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entryDestination = File(targetDirectory, entry.name)
                // prevent zipSlip
                if (!entryDestination.canonicalPath.startsWith(targetDirectory.canonicalPath + File.separator)) {
                    throw IllegalAccessException("Entry is outside of the target dir: " + entry.name)
                }
                if (entry.isDirectory) {
                    entryDestination.mkdirs()
                } else if (entry.isUnixSymlink) {
                    zipFile.getInputStream(entry).use { `in` ->
                        val symlink = IOUtils.toString(`in`, StandardCharsets.UTF_8)
                        Os.symlink(symlink, entryDestination.absolutePath)
                    }
                } else {
                    entryDestination.parentFile?.mkdirs()
                    zipFile.getInputStream(entry).use { `in` ->
                        FileOutputStream(entryDestination).use { out ->
                            IOUtils.copy(
                                `in`, out
                            )
                        }
                    }
                }
            }
        }
    }

    fun unzip(inputStream: InputStream?, targetDirectory: File) {
        ZipArchiveInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipArchiveEntry?
            while (zis.nextZipEntry.also { entry = it } != null) {
                val entryDestination = File(targetDirectory, entry!!.name)
                // prevent zipSlip
                if (!entryDestination.canonicalPath.startsWith(targetDirectory.canonicalPath + File.separator)) {
                    throw IllegalAccessException("Entry is outside of the target dir: " + entry!!.name)
                }
                if (entry!!.isDirectory) {
                    entryDestination.mkdirs()
                } else {
                    entryDestination.parentFile?.mkdirs()
                    FileOutputStream(entryDestination).use { out -> IOUtils.copy(zis, out) }
                }
            }
        }
    }

    /**
     * Extracts a tar.xz archive into [targetDirectory]. Unlike zip, tar stores symlinks and
     * permissions natively, and xz streams, so an asset can be extracted without staging it first.
     */
    fun extractTarXz(inputStream: InputStream, targetDirectory: File) {
        XZCompressorInputStream(BufferedInputStream(inputStream)).use { xz ->
            TarArchiveInputStream(xz).use { tar ->
                var entry = tar.nextEntry as TarArchiveEntry?
                while (entry != null) {
                    val entryDestination = File(targetDirectory, entry.name)
                    // prevent path traversal
                    if (!entryDestination.canonicalPath.startsWith(targetDirectory.canonicalPath + File.separator)) {
                        throw IllegalAccessException("Entry is outside of the target dir: " + entry.name)
                    }
                    when {
                        entry.isDirectory -> entryDestination.mkdirs()
                        entry.isSymbolicLink -> {
                            entryDestination.parentFile?.mkdirs()
                            entryDestination.delete()
                            Os.symlink(entry.linkName, entryDestination.absolutePath)
                        }
                        else -> {
                            entryDestination.parentFile?.mkdirs()
                            FileOutputStream(entryDestination).use { out -> IOUtils.copy(tar, out) }
                        }
                    }
                    entry = tar.nextEntry as TarArchiveEntry?
                }
            }
        }
    }

    fun extractTarXzAsset(context: Context, assetName: String, targetDirectory: File) {
        context.assets.open(assetName).use { extractTarXz(it, targetDirectory) }
    }
}
