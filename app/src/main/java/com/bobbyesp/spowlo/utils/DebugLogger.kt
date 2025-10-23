package com.bobbyesp.spowlo.utils

import com.bobbyesp.spowlo.App
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight file logger for diagnostics.
 * Writes to App.filesDir/spowlo_debug.log without extra permissions.
 */
object DebugLogger {
    private val logFile by lazy { File(App.context.filesDir, "spowlo_debug.log") }
    private val lock = Any()
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun log(tag: String, message: String) {
        write("${ts()} [$tag] $message")
    }

    fun logError(tag: String, throwable: Throwable) {
        write("${ts()} [$tag] ERROR: ${throwable.message}\n${throwable.stackTraceToString()}")
    }

    fun clear() = synchronized(lock) {
        if (logFile.exists()) logFile.delete()
    }

    fun read(): String = synchronized(lock) {
        if (logFile.exists()) logFile.readText() else ""
    }

    fun path(): String = logFile.absolutePath

    private fun ts(): String = dateFmt.format(Date())

    private fun write(line: String) = synchronized(lock) {
        rotateIfNeeded()
        logFile.appendText(line + "\n")
    }

    private fun rotateIfNeeded(maxBytes: Long = 200 * 1024) {
        if (logFile.exists() && logFile.length() > maxBytes) {
            val bak = File(logFile.parentFile, "spowlo_debug_${System.currentTimeMillis()}.log")
            logFile.copyTo(bak, overwrite = true)
            logFile.writeText("")
        }
    }
}