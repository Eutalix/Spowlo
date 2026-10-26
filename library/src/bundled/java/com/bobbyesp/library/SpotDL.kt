package com.bobbyesp.library

import android.content.Context
import android.util.Log
import com.bobbyesp.library.SpotDL.isDebug
import com.bobbyesp.library.util.exceptions.SpotDLException
import com.bobbyesp.spotdl_common.domain.Dependency
import com.bobbyesp.spotdl_common.domain.model.DownloadedDependencies
import com.bobbyesp.spotdl_common.utils.ZipUtils.unzipAsset
import com.bobbyesp.spotdl_common.utils.dependencies.dependencyDownloadCallback
import org.apache.commons.io.FileUtils
import java.io.File

object SpotDL : SpotDLCore() {
    override fun ensureDependencies(
        appContext: Context,
        skipDependencies: List<Dependency>,
        callback: dependencyDownloadCallback?
    ): DownloadedDependencies? = null

    /**
     * Initializes Python.
     * @param appContext the application context
     * @param pythonDir the directory where Python is located
     */
    @Throws(SpotDLException::class)
    override fun initPython(appContext: Context, pythonDir: File) {
        val version = BuildConfig.PYTHON_BUNDLE_VERSION
        if (!pythonDir.exists() || shouldUpdatePython(appContext, version)) {
            FileUtils.deleteQuietly(pythonDir)
            pythonDir.mkdirs()
            try {
                if(isDebug) Log.i("SpotDL", "Unzipping Python library")
                unzipAsset(appContext, PYTHON_BUNDLE_ASSET, pythonDir)
                updatePython(appContext, version)
                if(isDebug) Log.i("SpotDL", "Unzipped finished for the Python library")
            } catch (e: Exception) {
                FileUtils.deleteQuietly(pythonDir)
                throw SpotDLException("Failed to initialize Python", e)
            }
        } else {
            if(isDebug) Log.i("SpotDL", "Python library already exists or doesn't need to be updated")
        }
    }

    private const val PYTHON_BUNDLE_ASSET = "libpython.zip"

    @JvmStatic
    fun getInstance() = this
}
