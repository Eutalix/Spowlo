package com.bobbyesp.spowlo

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.os.Looper
import androidx.core.content.getSystemService
import com.bobbyesp.ffmpeg.FFmpeg
import com.bobbyesp.library.SpotDL
import com.bobbyesp.spowlo.utils.*
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize core utilities and state.
        MMKV.initialize(this)
        context = applicationContext
        packageInfo = packageManager.run {
            if (Build.VERSION.SDK_INT >= 33) getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            else getPackageInfo(packageName, 0)
        }
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        clipboard = getSystemService()!!
        connectivityManager = getSystemService()!!

        // Asynchronously initialize heavy components to avoid blocking the main thread.
        applicationScope.launch(Dispatchers.IO) {
            try {
                SpotDL.getInstance().init(this@App)
                FFmpeg.init(this@App)
            } catch (e: Exception) {
                // Handle critical initialization errors.
                Looper.prepare()
                ToastUtil.makeToast(text = "Critical initialization error: ${e.message}")
                e.printStackTrace()
                clipboard.setPrimaryClip(ClipData.newPlainText("Spowlo Error", e.stackTraceToString()))
            }
        }

        // Load the user-configured download directory.
        audioDownloadDir = AUDIO_DIRECTORY.getString(
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                getString(R.string.app_name)
            ).absolutePath
        )!!
        
        // Create notification channel for Android Oreo and above.
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationsUtil.createNotificationChannel()
        }
        
        // Set up a global uncaught exception handler to log crashes.
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val logfile = createLogFile(this, e.stackTraceToString())
            startCrashReportActivity(logfile)
        }
    }

    private fun startCrashReportActivity(logfilePath: String) {
        val intent = Intent(this, CrashHandlerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("logfile_path", logfilePath)
            // version_report is now passed directly from the activity.
        }
        startActivity(intent)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        lateinit var clipboard: ClipboardManager
        lateinit var audioDownloadDir: String
        lateinit var applicationScope: CoroutineScope
        lateinit var connectivityManager: ConnectivityManager
        lateinit var packageInfo: PackageInfo
        
        // Expose singleton instances for easy access.
        val SpotDl = SpotDL.getInstance()
        val FFMPEG = FFmpeg.getInstance()
        
        fun updateDownloadDir(path: String) {
            audioDownloadDir = path
            PreferencesUtil.encodeString(AUDIO_DIRECTORY, path)
        }

        fun getVersionReport(): String {
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
            val release = if (Build.VERSION.SDK_INT >= 30) Build.VERSION.RELEASE_OR_CODENAME else Build.VERSION.RELEASE
            return "App version: $versionName ($versionCode)\n" +
                    "Device information: Android $release (API ${Build.VERSION.SDK_INT})\n" +
                    "Supported ABIs: ${Build.SUPPORTED_ABIS.contentToString()}\n" +
                    "spotDL version: ${SpotDl.version(context)}"
        }

        fun createLogFile(context: Context, errorReport: String): String {
            val date = Calendar.getInstance().run { "${get(Calendar.DAY_OF_MONTH)}-${get(Calendar.MONTH) + 1}-${get(Calendar.YEAR)}" }
            val fileName = "log_$date.txt"
            val logFile = File(context.filesDir, fileName)
            logFile.writeText(errorReport) // Use writeText for simplicity.
            return logFile.absolutePath
        }
    }
}