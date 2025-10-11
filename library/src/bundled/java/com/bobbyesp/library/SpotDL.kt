package com.bobbyesp.library

import android.content.Context
import android.util.Log
import com.bobbyesp.library.util.exceptions.SpotDLException
import com.bobbyesp.spotdl_common.Constants
import com.bobbyesp.spotdl_common.utils.ZipUtils.unzip
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * The 'bundled' implementation of SpotDLCore.
 * Its primary responsibility is to unzip the embedded Python environment
 * from the 'libpython.zip.so' file into the app's private storage.
 */
object SpotDL : SpotDLCore() {

    /**
     * Initializes the embedded Python environment.
     * It checks if Python is already unzipped and up-to-date. If not, it extracts
     * the 'libpython.zip.so' from the APK's native library directory into the
     * app's private files directory.
     * @param appContext The application context.
     * @param pythonDir The target directory where the Python environment should be extracted.
     */
    @Throws(SpotDLException::class)
    override fun initPython(appContext: Context, pythonDir: File) {
        val pythonLibrary = File(
            binariesDirectory, Constants.LibrariesName.PYTHON
        )
        // Check if the directory doesn't exist or if the library version has been updated.
        if (!pythonDir.exists() || shouldUpdatePython(appContext, pythonLibrary.length().toString())) {
            FileUtils.deleteQuietly(pythonDir) // Clean up any old files.
            pythonDir.mkdirs()
            try {
                if (isDebug) Log.i("SpotDL", "Unzipping Python library...")
                unzip(pythonLibrary, pythonDir)
                if (isDebug) Log.i("SpotDL", "Unzipping finished for the Python library.")
                // Store the current version marker to avoid unnecessary unzipping on next launch.
                updatePython(appContext, pythonLibrary.length().toString())
            } catch (e: Exception) {
                FileUtils.deleteQuietly(pythonDir)
                throw SpotDLException("Failed to initialize Python", e)
            }
        } else {
            if (isDebug) Log.i("SpotDL", "Python library is already initialized and up-to-date.")
        }
    }

    /**
     * Provides a singleton instance of the SpotDL object.
     */
    @JvmStatic
    fun getInstance() = this
}