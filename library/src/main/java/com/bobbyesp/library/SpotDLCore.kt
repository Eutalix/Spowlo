package com.bobbyesp.library

import android.content.Context
import android.util.Log
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.spotifyAppApi
import com.bobbyesp.library.data.local.streams.StreamGobbler
import com.bobbyesp.library.util.exceptions.CanceledException
import com.bobbyesp.library.util.exceptions.SpotDLException
import com.bobbyesp.spotdl_common.Constants
import com.bobbyesp.spotdl_common.SharedPrefsHelper
import com.bobbyesp.spotdl_common.utils.files.FilesUtil.ensure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.util.Collections
import kotlin.math.roundToInt

/**
 * The core abstract class for the SpotDL library.
 * It manages the Python environment, executes spotdl commands, and provides a native
 * interface to the Spotify API for metadata fetching.
 */
abstract class SpotDLCore {
    private var initialized = false
    protected lateinit var binariesDirectory: File
    private var pythonPath: File? = null
    private var ffmpegPath: File? = null
    private lateinit var ENV_LD_LIBRARY_PATH: String
    private lateinit var ENV_SSL_CERT_FILE: String
    private lateinit var ENV_PYTHONHOME: String
    private lateinit var TMPDIR: String

    // Exposes the Spotify API instance to the app module.
    // It's nullable as it's initialized asynchronously.
    val spotifyApi: SpotifyAppApi?
        get() = _spotifyApi
    
    // The internal, mutable instance of the Spotify API client.
    private var _spotifyApi: SpotifyAppApi? = null

    private val pythonLibVersion = "pythonLibVersion"
    private val idProcessMap: MutableMap<String, Process> =
        Collections.synchronizedMap(HashMap<String, Process>())
    internal val isDebug = BuildConfig.DEBUG

    /**
     * Initializes the SpotDL library, setting up Python, FFmpeg, and the Spotify API client.
     * This must be called once, typically in the Application's onCreate method.
     * @param context The application context.
     */
    open fun init(context: Context) {
        if (initialized) return

        val baseDirectory = File(context.noBackupFilesDir, Constants.LIBRARY_NAME).ensure()
        binariesDirectory = File(context.applicationInfo.nativeLibraryDir)
        pythonPath = File(binariesDirectory, Constants.BinariesName.PYTHON)
        ffmpegPath = File(binariesDirectory, Constants.BinariesName.FFMPEG)
        val pythonDir = File(baseDirectory, Constants.DirectoriesName.PYTHON)
        val ffmpegDir = File(baseDirectory, Constants.DirectoriesName.FFMPEG)

        // Set up environment variables required for the embedded Python interpreter to function correctly.
        ENV_LD_LIBRARY_PATH =
            "${pythonDir.absolutePath}/usr/lib:${ffmpegDir.absolutePath}/usr/lib"
        ENV_SSL_CERT_FILE = "${pythonDir.absolutePath}/usr/etc/tls/cert.pem"
        ENV_PYTHONHOME = "${pythonDir.absolutePath}/usr"
        TMPDIR = context.cacheDir.absolutePath
        
        try {
            initPython(context, pythonDir)
        } catch (e: Exception) {
            throw SpotDLException("Error initializing Python environment", e)
        }

        // Asynchronously initialize the Spotify API client to avoid blocking the main thread.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use credentials from BuildConfig if available; otherwise, initialize anonymously.
                if (BuildConfig.SPOTIFY_CLIENT_ID.isNotEmpty() && BuildConfig.SPOTIFY_CLIENT_SECRET.isNotEmpty()) {
                    _spotifyApi = spotifyAppApi(
                        BuildConfig.SPOTIFY_CLIENT_ID,
                        BuildConfig.SPOTIFY_CLIENT_SECRET
                    ).build()
                    Log.d("SpotDLCore", "Spotify API initialized with client credentials.")
                } else {
                     _spotifyApi = spotifyAppApi(
                        // Provide a default public client ID and secret as a fallback.
                        "a11878d655f344e1951f3ada3b3ce196",
                        "a1b02b0c39f042c18d80f74a161f364a"
                    ).build()
                    Log.d("SpotDLCore", "Spotify API initialized anonymously.")
                }
            } catch (e: Exception) {
                Log.e("SpotDLCore", "Failed to initialize Spotify API", e)
            }
        }

        initialized = true
    }
    
    internal abstract fun initPython(appContext: Context, pythonDir: File)

    /**
     * Attempts to destroy a running spotdl process by its unique ID.
     * @param id The process ID assigned during the 'execute' call.
     * @return True if the process was found and destroyed, false otherwise.
     */
    fun destroyProcessById(id: String): Boolean {
        idProcessMap[id]?.let {
            it.destroy()
            idProcessMap.remove(id)
            return true
        }
        return false
    }

    /**
     * Executes a spotdl command.
     * @param request The SpotDLRequest object containing the query and options.
     * @param processId An optional unique ID to track this process for cancellation.
     * @param callback A callback to receive real-time progress updates.
     * @return A SpotDLResponse containing the exit code and output streams.
     */
    @Throws(SpotDLException::class, InterruptedException::class, CanceledException::class)
    fun execute(
        request: SpotDLRequest,
        processId: String? = null,
        callback: ((Float, Long, String) -> Unit)? = null,
    ): SpotDLResponse {
        assertInit()
        if (processId != null && idProcessMap.containsKey(processId)) {
            throw SpotDLException("Process ID '$processId' already exists.")
        }
        
        // Add required options for spotdl to function correctly within the app.
        request.addOption("--ffmpeg", ffmpegPath!!.absolutePath)
        if (!request.hasOption("--cache-path")) {
            request.addOption("--no-cache")
        }
        
        val startTime = System.currentTimeMillis()
        val args = request.buildCommand()
        
        // The command structure for spotdl v4+ is 'python -m spotdl <args>'.
        val command: MutableList<String> = mutableListOf(pythonPath!!.absolutePath, "-m", "spotdl")
        command.addAll(args)
        
        val processBuilder = ProcessBuilder(command)
        processBuilder.environment().apply {
            this["LD_LIBRARY_PATH"] = ENV_LD_LIBRARY_PATH
            this["SSL_CERT_FILE"] = ENV_SSL_CERT_FILE
            this["PATH"] = "${System.getenv("PATH")}:${binariesDirectory.absolutePath}"
            this["PYTHONHOME"] = ENV_PYTHONHOME
            this["TMPDIR"] = TMPDIR
            this["TERM"] = "xterm-256color" // Helps with compatibility for some underlying tools.
        }
        
        val process: Process = try {
            processBuilder.start()
        } catch (e: IOException) {
            throw SpotDLException("Failed to start Python process", e)
        }
        
        processId?.let { idProcessMap[it] = process }
        
        val outBuffer = StringBuilder()
        val errBuffer = StringBuilder()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        var exitCode: Int
        
        try {
            runBlocking {
                val stdOutGobbler = StreamGobbler(outBuffer, process.inputStream)
                val stdErrGobbler = StreamGobbler(errBuffer, process.errorStream)
                
                // A fake progress updater is used as spotdl v4+'s output is not easily machine-readable.
                callback?.let {
                    launch { runFakeProgressUpdater(stdOutGobbler, it) }
                }
                
                stdOutGobbler.join()
                stdErrGobbler.join()
                exitCode = process.waitFor()
            }
        } catch (e: InterruptedException) {
            process.destroy()
            if (processId != null && !idProcessMap.containsKey(processId)) {
                // If the process was removed, it was likely cancelled intentionally.
                throw CanceledException()
            }
            throw e
        } finally {
            coroutineScope.cancel()
            idProcessMap.remove(processId)
        }

        val out = outBuffer.toString()
        val err = errBuffer.toString()

        if (exitCode != 0) {
            if (isDebug) Log.e("SpotDL", "spotdl execution failed with exit code $exitCode:\nOUT: $out\nERR: $err")
            throw SpotDLException(err.ifEmpty { "spotdl failed with exit code $exitCode" })
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        return SpotDLResponse(command, exitCode, elapsedTime, out, err)
    }

    private suspend fun runFakeProgressUpdater(
        gobbler: StreamGobbler,
        callback: (Float, Long, String) -> Unit
    ) {
        var progress = 0f
        val totalDurationEstimate = 30L // 30-second estimate for a single song download.
        while (gobbler.isAlive && progress < 99f) {
            progress += 2f
            val eta = (totalDurationEstimate * (100f - progress) / 100f).coerceAtLeast(0f).toLong()
            val line = "Downloading... ${progress.roundToInt()}%"
            callback(progress / 100f, eta, line)
            delay(500)
        }
    }

    /**
     * Fetches track metadata directly from the Spotify API.
     * @param url The Spotify track URL.
     * @return A 'Track' object from the Spotify API library, or null if an error occurs.
     */
    suspend fun getTrack(url: String): Track? {
        assertInit()
        // Wait for the API to be initialized (it happens asynchronously).
        var attempts = 0
        while (_spotifyApi == null && attempts < 50) { // Max wait 5 seconds
            delay(100)
            attempts++
        }
        return try {
            _spotifyApi?.tracks?.getTrack(url)
        } catch (e: Exception) {
            Log.e("SpotDLCore", "Failed to get track info from Spotify API", e)
            null
        }
    }
    
    /**
     * Searches for tracks on Spotify using the native API.
     * @param query The search term.
     * @return A list of 'Track' objects.
     */
    suspend fun search(query: String): List<Track> {
        assertInit()
        var attempts = 0
        while (_spotifyApi == null && attempts < 50) {
            delay(100)
            attempts++
        }
        return try {
            _spotifyApi?.search?.searchTrack(query)?.items ?: emptyList()
        } catch (e: Exception) {
            Log.e("SpotDLCore", "Failed to search from Spotify API", e)
            emptyList()
        }
    }
    
    /**
     * A helper function to parse the downloaded file path from spotdl's standard output.
     * @param outputText The full standard output from the spotdl process.
     * @param trackName The name of the track, used as a fallback.
     * @return A 'File' object pointing to the downloaded song, or null if not found.
     */
    fun findDownloadedFile(outputText: String, trackName: String): File? {
        // spotdl v4+ prints the path of the downloaded file in a specific format.
        // Example output line: "Downloaded "Song Name" to /path/to/song.mp3"
        val regex = """Downloaded ".*?" to (.*)""".toRegex()
        return outputText.lines().lastOrNull { it.startsWith("Downloaded") }?.let { line ->
            regex.find(line)?.groups?.get(1)?.value?.let { File(it.trim()) }
        }
    }

    @Throws(SpotDLException::class)
    private fun assertInit() {
        check(initialized) { "The SpotDL instance is not initialized. Call SpotDL.init(context) first." }
    }
    
    fun updatePython(appContext: Context, version: String) {
        SharedPrefsHelper.update(appContext, pythonLibVersion, version)
    }

    fun shouldUpdatePython(appContext: Context, version: String): Boolean {
        return version != SharedPrefsHelper[appContext, pythonLibVersion]
    }
}