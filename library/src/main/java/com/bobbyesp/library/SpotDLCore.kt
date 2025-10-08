package com.bobbyesp.library

import android.content.Context
import android.os.Build
import android.util.Log
import com.bobbyesp.library.data.local.streams.StreamGobbler
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.UUID
import kotlin.math.roundToInt

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
        ENV_LD_LIBRARY_PATH =
            pythonDir.absolutePath + "/usr/lib" + ":" + ffmpegDir.absolutePath + "/usr/lib"
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

    abstract fun ensureDependencies(
        appContext: Context,
        skipDependencies: List<Dependency> = emptyList(),
        callback: dependencyDownloadCallback? = null
    ): DownloadedDependencies?

    internal abstract fun initPython(appContext: Context, pythonDir: File)

    fun destroyProcessById(id: String): Boolean {
        if (idProcessMap.containsKey(id)) {
            val p = idProcessMap[id]
            var alive = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                alive = p!!.isAlive
            }
            if (alive) {
                p!!.destroy()
                idProcessMap.remove(id)
                return true
            }
        }
        return false
    }

    fun updateSpotDL(appContext: Context): UpdateStatus? {
        assertInit()
        return try {
            SpotDLUpdater.update(appContext)
        } catch (e: IOException) {
            throw SpotDLException("Failed to update the spotDL library.", e)
        }
    }

    fun version(appContext: Context): String? {
        return SpotDLUpdater.version(appContext)
    }

    @JvmOverloads
    @Throws(SpotDLException::class, InterruptedException::class, CanceledException::class)
    fun execute(
        request: SpotDLRequest,
        processId: String? = null,
        callback: ((Float, Long, String) -> Unit)? = null,
    ): SpotDLResponse {
        assertInit()
        if (processId != null && idProcessMap.containsKey(processId)) throw SpotDLException("Process ID already exists")
        request.addOption("--ffmpeg", ffmpegPath!!.absolutePath)
        if (!request.hasOption("--cache-path")) {
            request.addOption("--no-cache")
        }
        val startTime = System.currentTimeMillis()
        val args = request.buildCommand()
        val command: MutableList<String?> = ArrayList()
        command.addAll(listOf(pythonPath!!.absolutePath, "-m", "spotdl"))
        command.addAll(args)
        val processBuilder = ProcessBuilder(command)
        processBuilder.environment().apply {
            this["LD_LIBRARY_PATH"] = ENV_LD_LIBRARY_PATH
            this["SSL_CERT_FILE"] = ENV_SSL_CERT_FILE
            this["PATH"] = System.getenv("PATH") + ":" + binariesDirectory.absolutePath
            this["PYTHONHOME"] = ENV_PYTHONHOME
            this["HOME"] = HOME
            this["LDFLAGS"] = LDFLAGS
            this["TERM"] = "xterm-256color"
        }
        val process: Process
        try {
            process = processBuilder.start()
        } catch (e: IOException) {
            throw SpotDLException(e)
        }
        if (processId != null) {
            idProcessMap[processId] = process
        }
        val outBuffer = StringBuilder()
        val errBuffer = StringBuilder()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        var downloadJob: Job? = null
        var progressJob: Job? = null
        var exitCode: Int = -1
        try {
            runBlocking {
                downloadJob = launch {
                    val stdOutGobbler = StreamGobbler(outBuffer, process.inputStream)
                    val stdErrGobbler = StreamGobbler(errBuffer, process.errorStream)
                    stdOutGobbler.join()
                    stdErrGobbler.join()
                }
                if (callback != null) {
                    progressJob = launch {
                        runFakeProgressUpdater(downloadJob!!, callback)
                    }
                }
                exitCode = process.waitFor()
            }
        } catch (e: InterruptedException) {
            process.destroy()
            coroutineScope.cancel()
            if (processId != null) idProcessMap.remove(processId)
            throw e
        } finally {
            coroutineScope.cancel()
        }
        val out = outBuffer.toString()
        val err = errBuffer.toString()
        if (exitCode > 0) {
            if (processId != null && !idProcessMap.containsKey(processId)) throw CanceledException()
            if (!ignoreErrors(request, out)) {
                idProcessMap.remove(processId)
                Log.e("SpotDL", "Error occurred. $err, $out, $exitCode")
                throw SpotDLException(err)
            }
        }
        idProcessMap.remove(processId)
        callback?.invoke(1.0f, 0L, "Done.")
        val elapsedTime = System.currentTimeMillis() - startTime
        return SpotDLResponse(command, exitCode, elapsedTime, out, err)
    }

    private fun generateProgressBarString(percentage: Float): String {
        val barLength = 20
        val filledLength = (barLength * percentage / 100).roundToInt()
        val emptyLength = barLength - filledLength
        return "━".repeat(filledLength) + " ".repeat(emptyLength)
    }

    private suspend fun runFakeProgressUpdater(
        downloadJob: Job,
        callback: (Float, Long, String) -> Unit
    ) {
        var progress = 0f
        val totalDurationEstimate = 30L
        while (downloadJob.isActive && progress < 99f) {
            progress += 2f
            val eta = (totalDurationEstimate * (100 - progress) / 100).coerceAtLeast(0).toLong()
            val progressBar = generateProgressBarString(progress)
            val progressInt = progress.roundToInt()
            val line = "Downloading... [$progressBar] $progressInt% ETA: 00:00:${String.format("%02d", eta)}"
            callback(progress / 100f, eta, line)
            delay(500)
        }
    }

    @Throws(SpotDLException::class)
    fun getAnonymousToken(): String {
        assertInit()
        val request = SpotDLRequest()
        request.addOption("save", "")
        request.addOption("--print-errors")
        request.addOption("--no-download")
        val response = try {
            execute(request)
        } catch (e: SpotDLException) {
            SpotDLResponse(emptyList(), 1, 0, "", e.message ?: "")
        }
        val tokenRegex = "token='([a-zA-Z0-9._-]+)'".toRegex()
        val matchResult = tokenRegex.find(response.error)
        val token = matchResult?.groups?.get(1)?.value
        if (token.isNullOrBlank()) {
            Log.e("SpotDL", "Failed to extract anonymous token. Stderr: ${response.error}")
            throw SpotDLException("Could not retrieve anonymous Spotify token from spotdl.")
        }
        Log.d("SpotDL", "Successfully extracted anonymous token.")
        return token
    }

    @Throws(SpotDLException::class, InterruptedException::class, CanceledException::class)
    fun getSongInfo(
        url: String,
        songId: String = UUID.randomUUID().toString(),
        extraArguments: Map<String, String>? = null
    ): List<SpotifySong> {
        assertInit()
        val metadataDirectory = File("$HOME/.spotdl/meta_info/").ensure()
        val metadataFile = File(metadataDirectory, "$songId.spotdl")
        val request = SpotDLRequest()
        request.addOption("save", url)
        request.addOption("--save-file", metadataFile.absolutePath)
        extraArguments?.forEach { (key, value) -> request.addOption(key, value) }
        execute(request, songId, null)
        val spotifySongInfo: List<SpotifySong>?
        try {
            spotifySongInfo = json.decodeFromString<List<SpotifySong>>(metadataFile.readText())
        } catch (e: Exception) {
            throw SpotDLException("Failed to read/parse the metadata file", e)
        }
        return spotifySongInfo
    }

    private fun ignoreErrors(request: SpotDLRequest, out: String): Boolean {
        return out.isNotEmpty() && !request.hasOption("--print-errors")
    }

    @Throws(SpotDLException::class)
    private fun assertInit() {
        check(initialized) { "The SpotDL instance is not initialized" }
    }

    fun updatePython(appContext: Context, version: String) {
        SharedPrefsHelper.update(
            appContext, pythonLibVersion, version
        )
    }

    fun shouldUpdatePython(appContext: Context, version: String): Boolean {
        return version != SharedPrefsHelper[appContext, pythonLibVersion]
    }
}
