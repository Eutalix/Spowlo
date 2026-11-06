package com.bobbyesp.spowlo.utils

import android.os.StatFs
import com.bobbyesp.spowlo.App.Companion.audioDownloadDir
import java.io.File

/** Rough download size estimate used to warn before large collections start. */
object DownloadSizeEstimator {

    private const val CONTAINER_OVERHEAD = 1.05

    private val BITRATES =
        longArrayOf(0, 8, 16, 24, 32, 40, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)

    fun estimateBytes(durationMs: Long): Long =
        estimateBytes(durationMs, PreferencesUtil.getAudioFormat(), PreferencesUtil.getAudioQuality())

    fun estimateBytes(durationMs: Long, format: Int, quality: Int): Long {
        if (durationMs <= 0L) return 0L
        val kbps = bitrateFor(format, quality)
        val seconds = durationMs / 1000.0
        return (seconds * kbps * 1000 / 8 * CONTAINER_OVERHEAD).toLong()
    }

    /** Rough average bitrate for a format/quality pair, in kbps. */
    fun bitrateFor(format: Int, quality: Int): Long = when {
        format == 1 -> 900
        format == 5 -> 1411
        quality == 0 -> 128
        quality == 17 -> 256
        quality in 1..16 -> BITRATES[quality]
        else -> 128
    }

    /** `0` when the free space cannot be read, for example on a removable volume. */
    fun freeBytes(): Long = runCatching {
        StatFs(File(audioDownloadDir).path).availableBytes
    }.getOrDefault(0L)

    fun hasEnoughSpace(estimatedBytes: Long, freeBytes: Long = freeBytes()): Boolean =
        estimatedBytes <= 0L || freeBytes <= 0L || estimatedBytes < freeBytes
}
