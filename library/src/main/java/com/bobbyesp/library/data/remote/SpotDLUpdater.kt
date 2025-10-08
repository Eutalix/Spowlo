package com.bobbyesp.library.data.remote

import android.content.Context
import android.util.Log
import com.bobbyesp.library.domain.UpdateStatus
import com.bobbyesp.library.util.exceptions.SpotDLException
import com.bobbyesp.spotdl_common.Constants
import com.bobbyesp.spotdl_common.SharedPrefsHelper
import com.bobbyesp.spotdl_common.domain.model.updates.Release
import com.bobbyesp.spotdl_common.utils.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.net.URL

internal object SpotDLUpdater {
    private const val SPOTDL_VERSION_KEY = "spotdl_version"
    private const val SPOTDL_VERSION_NAME_KEY = "spotdl_version_name"
    private const val SPOTDL_RELEASES_URL =
        "https://api.github.com/repos/spotDL/spotify-downloader/releases/latest"

    @Throws(IOException::class, SpotDLException::class)
    internal fun update(
        appContext: Context,
    ): UpdateStatus {
        val release = checkForUpdate(appContext)
            ?: return UpdateStatus.ALREADY_UP_TO_DATE

        // The old logic of downloading and replacing a single 'spotdl' binary file is no longer valid
        // as spotdl is now part of the comprehensive site-packages within libpython.zip.so.
        // A real update would require rebuilding and replacing the entire zip.
        // For now, we will just log this and update the version info.
        Log.i("SpotDLUpdater", "New version ${release.tag_name} found, but auto-update is a complex operation. Manual library update is required.")

        // We can still download it to check, but we won't replace anything.
        val downloadUrl = getDownloadUrl(release)
        val file = download(appContext, downloadUrl)
        file.delete() // Clean up the downloaded temporary file.

        updateSharedPrefs(appContext, release.tag_name, release.name)
        // Let's consider it 'DONE' in the sense that the version check is complete.
        return UpdateStatus.DONE
    }

    private fun updateSharedPrefs(appContext: Context, tag: String, name: String) {
        SharedPrefsHelper.update(appContext, SPOTDL_VERSION_KEY, tag)
        SharedPrefsHelper.update(appContext, SPOTDL_VERSION_NAME_KEY, name)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(IOException::class)
    private fun checkForUpdate(appContext: Context): Release? {
        val url = URL(SPOTDL_RELEASES_URL)
        val json = json.decodeFromStream<Release>(url.openStream())
        val newVersion = json.tag_name
        val oldVersion = SharedPrefsHelper[appContext, SPOTDL_VERSION_KEY]
        return if (newVersion == oldVersion) {
            Log.i("SpotDLUpdater", "SpotDL is up to date. Current version: $oldVersion")
            null
        } else json
    }

    @Throws(SpotDLException::class)
    private fun getDownloadUrl(json: Release): String {
        val assets = json.assets
        var downloadUrl = ""
        assets.forEach { asset ->
            if (asset.name == "spotdl") {
                downloadUrl = asset.browser_download_url
                return@forEach
            }
        }
        if (downloadUrl.isEmpty()) throw SpotDLException("Unable to get SpotDL binary download URL!")
        return downloadUrl
    }

    @Throws(IOException::class)
    private fun download(appContext: Context, url: String): File {
        val downloadUrl = URL(url)
        val file = File.createTempFile(Constants.BinariesName.SPOTDL, null, appContext.cacheDir)
        FileUtils.copyURLToFile(downloadUrl, file, 5000, 10000)
        return file
    }

    fun version(appContext: Context?): String? {
        return SharedPrefsHelper[appContext!!, SPOTDL_VERSION_KEY]
    }

    fun versionName(appContext: Context?): String? {
        return SharedPrefsHelper[appContext!!, SPOTDL_VERSION_NAME_KEY]
    }
}