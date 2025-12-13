package com.bobbyesp.library.data.remote

import android.content.Context
import android.util.Log
import com.bobbyesp.library.BuildConfig
import com.bobbyesp.spotdl_common.Constants.LIBRARY_NAME
import com.bobbyesp.spotdl_common.utils.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

object SpotDLDependencyUpdater {

    private const val TAG = "SpowloDebug"
    private val mutex = Mutex()
    private val isDebug = BuildConfig.DEBUG

    // List of volatile packages to update via Hotfix.
    private val TARGET_PACKAGES = listOf(
        "ytmusicapi",
        "yt-dlp",
        "spotdl",
        "syncedlyrics",
        "soundcloud-v2"
    )

    /**
     * Updates all volatile packages. Returns a log string.
     * Thread-safe: Only one update can run at a time.
     */
    suspend fun updateVolatileDependencies(context: Context): String {
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val sb = StringBuilder()
                sb.append("Starting dependency check...\n")
                if(isDebug) Log.d(TAG, ">>> DepUpdater: Starting updateVolatileDependencies...")

                val sitePackages = getSitePackagesDir(context)
                if (!sitePackages.exists()) {
                    if(isDebug) Log.d(TAG, ">>> DepUpdater: custom site-packages missing, creating at ${sitePackages.absolutePath}")
                    sitePackages.mkdirs()
                } else {
                    if(isDebug) Log.d(TAG, ">>> DepUpdater: custom site-packages exists at ${sitePackages.absolutePath}")
                }

                var updatesCount = 0

                TARGET_PACKAGES.forEach { pkgName ->
                    try {
                        val result = checkAndInstallPackage(pkgName, sitePackages)
                        sb.append(result).append("\n")
                        if(isDebug) Log.d(TAG, ">>> DepUpdater: Processed $pkgName -> $result")
                        if (result.contains("[Updated]")) {
                            updatesCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if(isDebug) Log.e(TAG, ">>> DepUpdater: Failed to process $pkgName", e)
                        sb.append("[Error] Failed to process $pkgName: ${e.message}\n")
                    }
                }

                if (updatesCount > 0) {
                    sb.append("\n$updatesCount libraries updated successfully.")
                } else {
                    sb.append("\nAll target dependencies are up to date.")
                }

                sb.toString()
            }
        }
    }

    /**
     * Returns the local version of spotdl installed in the site-packages.
     * Returns "0.0.0" if not found.
     */
    fun getLocalSpotDLVersion(context: Context): String {
        val sitePackages = getSitePackagesDir(context)
        val v = getLocalVersion(sitePackages, "spotdl")
        if(isDebug) Log.d(TAG, ">>> DepUpdater: Check local SpotDL version: $v")
        return v
    }

    suspend fun areUpdatesAvailable(context: Context): Boolean {
        if (mutex.isLocked) return false

        return withContext(Dispatchers.IO) {
            val sitePackages = getSitePackagesDir(context)
            // Even if custom dir doesn't exist, we might want to update, but let's check
            if (!sitePackages.exists()) return@withContext true

            TARGET_PACKAGES.any { pkgName ->
                try {
                    val pypiInfo = getPypiInfo(pkgName) ?: return@any false
                    val remoteVersion = pypiInfo.info.version
                    val localVersion = getLocalVersion(sitePackages, pkgName)
                    
                    localVersion != remoteVersion && localVersion != "0.0.0"
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    private fun checkAndInstallPackage(pkgName: String, sitePackagesDir: File): String {
        if(isDebug) Log.d(TAG, ">>> DepUpdater: Checking package $pkgName")
        
        val pypiInfo = getPypiInfo(pkgName) ?: return "[Error] $pkgName: Failed to query PyPI"
        val remoteVersion = pypiInfo.info.version
        val localVersion = getLocalVersion(sitePackagesDir, pkgName)
        
        if(isDebug) Log.d(TAG, ">>> DepUpdater: $pkgName | Local: $localVersion | Remote: $remoteVersion")

        if (localVersion == remoteVersion) {
            return "[OK] $pkgName: Up to date ($localVersion)"
        }

        val wheelUrl = pypiInfo.urls.find { 
            it.filename.contains("py3-none-any") && it.packagetype == "bdist_wheel" 
        }?.url

        if (wheelUrl == null) {
            return "[Warning] $pkgName: No compatible Pure Python wheel found."
        }

        if(isDebug) Log.d(TAG, ">>> DepUpdater: Downloading $pkgName wheel from $wheelUrl")

        val tempFile = File.createTempFile("hotfix_${pkgName}", ".whl")
        try {
            FileUtils.copyURLToFile(URL(wheelUrl), tempFile, 10000, 30000)
            
            if(isDebug) Log.d(TAG, ">>> DepUpdater: Clearing old versions for $pkgName")
            clearOldVersions(sitePackagesDir, pkgName)
            
            if(isDebug) Log.d(TAG, ">>> DepUpdater: Installing new wheel...")
            installWheel(tempFile, sitePackagesDir)
            return "[Updated] $pkgName: ($localVersion -> $remoteVersion)"
        } finally {
            tempFile.delete()
        }
    }

    private fun clearOldVersions(sitePackages: File, pkgName: String) {
        val normalizedName = pkgName.replace("-", "_")
        val possibleNames = listOf(pkgName, normalizedName)

        sitePackages.listFiles { f -> 
            f.isDirectory && f.name.endsWith(".dist-info") && possibleNames.any { name -> f.name.startsWith(name) }
        }?.forEach { 
            if(isDebug) Log.d(TAG, ">>> DepUpdater: Deleting metadata dir: ${it.name}")
            FileUtils.deleteDirectory(it) 
        }

        possibleNames.forEach { name ->
            val pkgDir = File(sitePackages, name)
            if (pkgDir.exists() && pkgDir.isDirectory) {
                if(isDebug) Log.d(TAG, ">>> DepUpdater: Deleting package dir: ${pkgDir.name}")
                FileUtils.deleteDirectory(pkgDir)
            }
        }
    }

    private fun installWheel(wheelFile: File, outputDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(wheelFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val fileName = entry.name
                    if (!fileName.contains("..")) {
                        val targetFile = File(outputDir, fileName)
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            val buffer = ByteArray(4096)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun getSitePackagesDir(context: Context): File {
        val baseLibDir = File(context.noBackupFilesDir, LIBRARY_NAME) 
        // Use a dedicated, safe directory for updates that won't be wiped by Python init
        return File(baseLibDir, "custom_site_packages")
    }

    private fun getLocalVersion(sitePackages: File, pkgName: String): String {
        val normalizedName = pkgName.replace("-", "_")
        val possibleNames = listOf(pkgName, normalizedName)
        
        val distInfoDir = sitePackages.listFiles { f -> 
            f.isDirectory && f.name.endsWith(".dist-info") && possibleNames.any { name -> f.name.startsWith(name) }
        }?.maxByOrNull { it.name }

        if (distInfoDir != null) {
            try {
                val name = distInfoDir.name
                val versionPart = name.substringBefore(".dist-info")
                val version = Regex("(?<=-)([0-9]+\\.[0-9]+(\\.[0-9]+)?.*)").find(versionPart)?.value
                if (version != null) return version
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse version from ${distInfoDir.name}")
            }
        }
        return "0.0.0"
    }

    private fun getPypiInfo(pkgName: String): PyPiResponse? {
        return try {
            val url = URL("https://pypi.org/pypi/$pkgName/json")
            json.decodeFromStream<PyPiResponse>(url.openStream())
        } catch (e: Exception) {
            Log.e(TAG, "Error querying PyPI for $pkgName", e)
            null
        }
    }
}

@Serializable
data class PyPiResponse(
    val info: PyPiInfo,
    val urls: List<PyPiUrl>
)

@Serializable
data class PyPiInfo(
    val version: String
)

@Serializable
data class PyPiUrl(
    val url: String,
    val filename: String,
    @SerialName("packagetype") val packagetype: String
)