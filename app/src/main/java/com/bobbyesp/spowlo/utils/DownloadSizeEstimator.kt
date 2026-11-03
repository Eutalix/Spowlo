package com.bobbyesp.spowlo.utils

import android.os.StatFs
import com.bobbyesp.spowlo.App.Companion.audioDownloadDir
import com.bobbyesp.spowlo.utils.PreferencesUtil.getValue
import java.io.File

/** Rough download size estimate used to warn before large collections start. */
object DownloadSizeEstimator {

    private const val CONTAINER_OVERHEAD = 1.05

    private val BITRATES =
        longArrayOf(0, 8, 16, 24, 32, 40, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)

    fun estimateBytes(durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        val kbps = estimatedBitrate()
        val seconds = durationMs / 1000.0
        return (seconds * kbps * 1000 / 8 * CONTAINER_OVERHEAD).toLong()
    }

    /** `0` when the free space cannot be read, for example on a removable volume. */
    fun freeBytes(): Long = runCatching {
        StatFs(File(audioDownloadDir).path).availableBytes
    }.getOrDefault(0L)

    fun hasEnoughSpace(estimatedBytes: Long, freeBytes: Long = freeBytes()): Boolean =
        estimatedBytes <= 0L || freeBytes <= 0L || estimatedBytes < freeBytes

    private fun estimatedBitrate(): Long {
        if (getValue(ORIGINAL_AUDIO)) return 256
        when (PreferencesUtil.getAudioFormat()) {
            1 -> return 900
            5 -> return 1411
        }
        val quality = PreferencesUtil.getAudioQuality()
        return when (quality) {
            0 -> 128
            17 -> 256
            in 1..16 -> BITRATES[quality]
            else -> 128
        }
    }
}
