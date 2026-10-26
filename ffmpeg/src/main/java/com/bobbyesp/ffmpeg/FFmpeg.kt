package com.bobbyesp.ffmpeg

import android.content.Context
import com.bobbyesp.spotdl_common.SharedPrefsHelper
import com.bobbyesp.library.util.exceptions.SpotDLException
import com.bobbyesp.spotdl_common.utils.ZipUtils.unzipAsset
import org.apache.commons.io.FileUtils
import java.io.File

object FFmpeg {
    private var initialized = false

    @Synchronized
    fun init(appContext: Context) {
        if (initialized) return
        val baseDir = File(appContext.noBackupFilesDir, baseName)
        if (!baseDir.exists()) baseDir.mkdir()
        val packagesDir = File(baseDir, packagesRoot)
        val ffmpegDir = File(packagesDir, ffmegDirName)
        initFFmpeg(appContext, ffmpegDir)
        initialized = true
    }

    private fun initFFmpeg(appContext: Context, ffmpegDir: File) {
        val version = BuildConfig.FFMPEG_BUNDLE_VERSION
        if (!ffmpegDir.exists() || shouldUpdateFFmpeg(appContext, version)) {
            FileUtils.deleteQuietly(ffmpegDir)
            ffmpegDir.mkdirs()
            try {
                unzipAsset(appContext, ffmpegBundleAsset, ffmpegDir)
            } catch (e: Exception) {
                FileUtils.deleteQuietly(ffmpegDir)
                throw SpotDLException("failed to initialize", e)
            }
            updateFFmpeg(appContext, version)
        }
    }

    private fun shouldUpdateFFmpeg(appContext: Context, version: String): Boolean {
        return version != SharedPrefsHelper[appContext, ffmpegLibVersion]
    }

    private fun updateFFmpeg(appContext: Context, version: String) {
        SharedPrefsHelper.update(appContext, ffmpegLibVersion, version)
    }

    @JvmStatic
    fun getInstance() = this

    private const val baseName = "spotdl-android"
    private const val packagesRoot = "packages"
    private const val ffmegDirName = "ffmpeg"
    private const val ffmpegBundleAsset = "libffmpeg.zip"
    private const val ffmpegLibVersion = "ffmpegLibVersion"

}
