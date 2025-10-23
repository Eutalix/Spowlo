package com.bobbyesp.library

import android.content.Context
import android.os.Build
import android.util.Log
import com.bobbyesp.library.data.local.streams.StreamGobbler
import com.bobbyesp.library.data.local.streams.StreamProcessExtractor
import com.bobbyesp.library.data.remote.SpotDLUpdater
import com.bobbyesp.library.domain.UpdateStatus
import com.bobbyesp.library.domain.model.SpotifySong
import com.bobbyesp.library.util.exceptions.CanceledException
import com.bobbyesp.library.util.exceptions.SpotDLException
import com.bobbyesp.spotdl_common.Constants
import com.bobbyesp.spotdl_common.Constants.LIBRARY_NAME
import com.bobbyesp.spotdl_common.Constants.PACKAGES_ROOT_NAME
import com.bobbyesp.spotdl_common.SharedPrefsHelper
import com.bobbyesp.spotdl_common.domain.Dependency
import com.bobbyesp.spotdl_common.domain.model.DownloadedDependencies
import com.bobbyesp.spotdl_common.utils.dependencies.dependencyDownloadCallback
import com.bobbyesp.spotdl_common.utils.files.FilesUtil.ensure
import com.bobbyesp.spotdl_common.utils.json
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.UUID

abstract class SpotDLCore {
    private var initialized = false
    protected lateinit var binariesDirectory: File
    private var pythonPath: File? = null
    private var ffmpegPath: File? = null
    private lateinit var ENV_LD_LIBRARY_PATH: String
    private lateinit var ENV_SSL_CERT_FILE: String
    private lateinit var ENV_PYTHONHOME: String
    private lateinit var HOME: String
    private lateinit var LDFLAGS: String
    private lateinit var TMPDIR: String
    private val pythonLibVersion = "pythonLibVersion"
    protected open val idProcessMap: MutableMap<String, Process> =
        Collections.synchronizedMap(HashMap<String, Process>())
    internal val isDebug = BuildConfig.DEBUG

    open fun init(context: Context) {
        if (initialized) return
        val baseDirectory = File(context.noBackupFilesDir, LIBRARY_NAME).ensure()
        val packagesDir = File(baseDirectory, PACKAGES_ROOT_NAME)
        binariesDirectory = File(context.applicationInfo.nativeLibraryDir)
        pythonPath = File(binariesDirectory, Constants.BinariesName.PYTHON)
        ffmpegPath = File(binariesDirectory, Constants.BinariesName.FFMPEG)
        val pythonDir = File(packagesDir, Constants.DirectoriesName.PYTHON)
        val ffmpegDir = File(packagesDir, Constants.DirectoriesName.FFMPEG)
        ENV_LD_LIBRARY_PATH = pythonDir.absolutePath + "/usr/lib" + ":" + ffmpegDir.absolutePath + "/usr/lib"
        ENV_SSL_CERT_FILE = pythonDir.absolutePath + "/usr/etc/tls/cert.pem"
        ENV_PYTHONHOME = pythonDir.absolutePath + "/usr"
        HOME = baseDirectory.absolutePath
        TMPDIR = context.cacheDir.absolutePath
        LDFLAGS = "-L" + pythonDir.absolutePath + "/usr/lib -rdynamic"
        try {
            initPython(context, pythonDir)
        } catch (e: Exception) {
            throw SpotDLException("Error initializing SpotDLCore", e)
        }
        initialized = true
    }

    @Throws(IllegalStateException::class)
    abstract fun ensureDependencies(
        appContext: Context,
        skipDependencies: List<Dependency> = emptyList(),
        callback: dependencyDownloadCallback? = null
    ): DownloadedDependencies?

    internal abstract fun initPython(appContext: Context, pythonDir: File)

    fun destroyProcessById(id: String): Boolean {
        if (isDebug) Log.d("SpotDL", "Destroying process $id")
        if (idProcessMap.containsKey(id)) {
            val p = idProcessMap[id]
            var alive = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) alive = p!!.isAlive
            if (alive) {
                p!!.destroy()
                idProcessMap.remove(id)
                return true
            }
        }
        return false
    }

    @Throws(SpotDLException::class)
    fun updateSpotDL(appContext: Context): UpdateStatus? {
        assertInit()
        return try {
            SpotDLUpdater.update(appContext)
        } catch (e: IOException) {
            throw SpotDLException("Failed to update the spotDL library.", e)
        }
    }

    fun version(appContext: Context): String? = SpotDLUpdater.version(appContext)

    @JvmOverloads
    @Throws(SpotDLException::class, InterruptedException::class, CanceledException::class)
    fun execute(
        request: SpotDLRequest,
        processId: String? = null,
        callback: ((Float, Long, String) -> Unit)? = null
    ): SpotDLResponse {
        assertInit()
        if (processId != null && idProcessMap.containsKey(processId)) {
            throw SpotDLException("Process ID already exists")
        }
        request.addOption("--ffmpeg", ffmpegPath!!.absolutePath)
        val args = request.buildCommand()
        val command = mutableListOf<String?>(pythonPath!!.absolutePath, "-m", "spotdl").apply {
            addAll(args)
        }
        val processBuilder = ProcessBuilder(command)
        processBuilder.environment().apply {
            this["LD_LIBRARY_PATH"] = ENV_LD_LIBRARY_PATH
            this["SSL_CERT_FILE"] = ENV_SSL_CERT_FILE
            this["PATH"] = System.getenv("PATH") + ":" + binariesDirectory.absolutePath
            this["PYTHONHOME"] = ENV_PYTHONHOME
            this["HOME"] = HOME
            this["LDFLAGS"] = LDFLAGS
            this["TERM"] = "xterm-256color"
            this["FORCE_COLOR"] = "true"
        }
        val process = try {
            processBuilder.start()
        } catch (e: IOException) {
            throw SpotDLException(e)
        }
        processId?.let { idProcessMap[it] = process }

        val outBuffer = StringBuilder()
        val errBuffer = StringBuilder()

        val stdOutProcessor: Thread = if (callback != null) {
            StreamProcessExtractor(outBuffer, process.inputStream, callback)
        } else {
            StreamGobbler(outBuffer, process.inputStream)
        }
        val stdErrProcessor = StreamGobbler(errBuffer, process.errorStream)

        val exitCode = try {
            stdOutProcessor.join()
            stdErrProcessor.join()
            process.waitFor()
        } catch (e: InterruptedException) {
            process.destroy()
            processId?.let { idProcessMap.remove(it) }
            throw e
        }

        // Remove id from map after the process ends
        processId?.let { idProcessMap.remove(it) }

        val out = outBuffer.toString()
        val err = errBuffer.toString()

        if (exitCode > 0) {
            if (!ignoreErrors(request, err)) {
                if (isDebug) Log.e("SpotDL", "Non-zero exit. EXIT=$exitCode, STDERR=$err, STDOUT=$out")
                throw SpotDLException(err.ifBlank { out })
            }
        }

        return SpotDLResponse(command, exitCode, System.currentTimeMillis() - 0L, out, err)
    }

    @Throws(SpotDLException::class, InterruptedException::class, CanceledException::class)
    fun getSongInfo(
        url: String,
        songId: String = UUID.randomUUID().toString(),
        extraArguments: Map<String, String>? = null
    ): List<SpotifySong> {
        assertInit()

        val request = SpotDLRequest().apply {
            setOperation("save")
            urls = listOf(url)

            // REQUIRED for spotdl v4: output JSON; '-' means stdout
            addOption("--save-file", "-")

            // Helpful for diagnostics
            addOption("--log-level", "DEBUG")
            addOption("--print-errors")

            extraArguments?.forEach { (key, value) -> addOption(key, value) }
            if (!hasOption("--client-id") || !hasOption("--client-secret")) {
                addOption("--client-id", BuildConfig.CLIENT_ID)
                addOption("--client-secret", BuildConfig.CLIENT_SECRET)
            }
        }

        val response = execute(request, songId, null)
        val jsonText = response.output

        try {
            if (jsonText.isBlank()) {
                throw SpotDLException(
                    "Spotdl returned empty metadata. See error report for details. Raw Error:\n---\n${response.error}\n---"
                )
            }

            return if (jsonText.trim().startsWith("[")) {
                json.decodeFromString<List<SpotifySong>>(jsonText)
            } else {
                val singleSong = json.decodeFromString<SpotifySong>(jsonText)
                listOf(singleSong)
            }
        } catch (e: Exception) {
            throw SpotDLException(
                "Failed to parse spotdl's JSON output. Raw output below:\n---\n$jsonText\n--- \n\nRaw Error:\n---\n${response.error}\n---",
                e
            )
        }
    }

    private fun ignoreErrors(request: SpotDLRequest, err: String): Boolean =
        err.isBlank() && !request.hasOption("--print-errors")

    @Throws(SpotDLException::class)
    private fun assertInit() = check(initialized) { "The SpotDL instance is not initialized" }

    fun updatePython(appContext: Context, version: String) =
        SharedPrefsHelper.update(appContext, pythonLibVersion, version)

    fun shouldUpdatePython(appContext: Context, version: String): Boolean =
        version != SharedPrefsHelper[appContext, pythonLibVersion]
}