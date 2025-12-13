package com.bobbyesp.library

import android.content.Context
import android.os.Build
import android.util.Log
import com.bobbyesp.library.data.local.streams.StreamGobbler
import com.bobbyesp.library.data.local.streams.StreamProcessExtractor
import com.bobbyesp.library.data.remote.SpotDLDependencyUpdater
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
    private lateinit var spotdlPath: File

    /* ENVIRONMENT VARIABLES */
    private lateinit var ENV_LD_LIBRARY_PATH: String
    private lateinit var ENV_SSL_CERT_FILE: String
    private lateinit var ENV_PYTHONHOME: String
    private lateinit var ENV_PYTHONPATH: String
    private lateinit var HOME: String
    private lateinit var LDFLAGS: String
    private lateinit var TMPDIR: String

    private val pythonLibVersion = "pythonLibVersion"

    //Map of process id associated with the process
    protected open val idProcessMap: MutableMap<String, Process> =
        Collections.synchronizedMap(HashMap<String, Process>())

    internal val isDebug = BuildConfig.DEBUG
    private val TAG = "SpowloDebug"

    open fun init(context: Context) {
        if (initialized) return
        if (isDebug) Log.d(TAG, ">>> SpotDLCore: Initializing...")

        val baseDirectory = File(context.noBackupFilesDir, LIBRARY_NAME).ensure()
        val packagesDir = File(baseDirectory, PACKAGES_ROOT_NAME)

        //Here are all the binaries provided by the jniLibs folder
        binariesDirectory = File(context.applicationInfo.nativeLibraryDir)

        // Dependencies binaries path (.so files)
        pythonPath = File(binariesDirectory, Constants.BinariesName.PYTHON)
        ffmpegPath = File(binariesDirectory, Constants.BinariesName.FFMPEG)

        // Dependencies packages directory (where they are extracted - .zip files)
        val pythonDir = File(packagesDir, Constants.DirectoriesName.PYTHON)
        val ffmpegDir = File(packagesDir, Constants.DirectoriesName.FFMPEG)

        val spotdlDir = File(baseDirectory, Constants.DirectoriesName.SPOTDL)
        spotdlPath = File(spotdlDir, Constants.BinariesName.SPOTDL)

        // Set environment variables
        ENV_LD_LIBRARY_PATH =
            pythonDir.absolutePath + "/usr/lib" + ":" + ffmpegDir.absolutePath + "/usr/lib"
        ENV_SSL_CERT_FILE = pythonDir.absolutePath + "/usr/etc/tls/cert.pem"
        ENV_PYTHONHOME = pythonDir.absolutePath + "/usr"
        
        val usrLib = File(pythonDir, "usr/lib")
        val pythonVerDir = usrLib.listFiles { f -> f.isDirectory && f.name.startsWith("python3") }?.firstOrNull() 
            ?: File(usrLib, "python3.11") // Fallback
        val baseSitePackages = File(pythonVerDir, "site-packages")
        
        // Define Custom Site Packages (Safe from resets)
        val baseLibDir = File(context.noBackupFilesDir, LIBRARY_NAME)
        val customSitePackages = File(baseLibDir, "custom_site_packages")
        
        if (isDebug) {
            Log.d(TAG, ">>> SpotDLCore: Base Python Dir: ${pythonVerDir.absolutePath}")
            Log.d(TAG, ">>> SpotDLCore: Custom SitePackages: ${customSitePackages.absolutePath}")
        }
        
        // Add custom site-packages FIRST so updated libs override built-in ones
        ENV_PYTHONPATH = customSitePackages.absolutePath + ":" + 
                         baseSitePackages.absolutePath + ":" + 
                         pythonVerDir.absolutePath
        
        HOME = baseDirectory.absolutePath
        TMPDIR = context.cacheDir.absolutePath
        LDFLAGS = "-L" + pythonDir.absolutePath + "/usr/lib -rdynamic"

        try {
            initPython(context, pythonDir)
            initSpotDL(context, spotdlDir)
        } catch (e: Exception) {
            Log.e(TAG, ">>> SpotDLCore: Error initializing", e)
            throw SpotDLException("Error initializing SpotDLCore", e)
        }
        initialized = true
        if (isDebug) Log.d(TAG, ">>> SpotDLCore: Initialization Complete.")
    }

    @Throws(IllegalStateException::class)
    abstract fun ensureDependencies(
        appContext: Context,
        skipDependencies: List<Dependency> = emptyList(),
        callback: dependencyDownloadCallback? = null
    ): DownloadedDependencies?

    internal abstract fun initPython(appContext: Context, pythonDir: File)

    @Throws(SpotDLException::class)
    fun initSpotDL(appContext: Context, spotDlDir: File) {
        if (!spotDlDir.exists()) spotDlDir.mkdirs()
        val spotDlBinary = File(spotDlDir, Constants.BinariesName.SPOTDL)
        
        // CHECK IF FILE EXISTS AND SIZE IS 0 (CORRUPT)
        if (!spotDlBinary.exists() || spotDlBinary.length() == 0L) {
            try {
                if (isDebug) Log.d(TAG, ">>> SpotDLCore: Extracting internal spotDL binary...")
                val inputStream =
                    appContext.resources.openRawResource(R.raw.spotdl)
                FileUtils.copyInputStreamToFile(inputStream, spotDlBinary)
            } catch (e: Exception) {
                FileUtils.deleteQuietly(spotDlDir)
                throw SpotDLException("Error extracting SpotDL source files", e)
            }
        }
    }

    /**
     * Checks if a process with the given id is running and destroys it
     * @param id the process id
     * @return true if the process was destroyed successfully, false otherwise
     */
    fun destroyProcessById(id: String): Boolean {
        if (isDebug) {
            Log.d("SpotDL", "Destroying process $id")
        }
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

    @Throws(SpotDLException::class)
    fun updateSpotDL(appContext: Context): UpdateStatus? {
        assertInit()
        return try {
            SpotDLUpdater.update(appContext)
        } catch (e: IOException) {
            throw SpotDLException("Failed to update the spotDL library.", e)
        }
    }

    @Throws(SpotDLException::class)
    suspend fun updatePythonDependencies(appContext: Context): String {
        assertInit()
        return try {
            SpotDLDependencyUpdater.updateVolatileDependencies(appContext)
        } catch (e: Exception) {
            throw SpotDLException("Failed to update Python dependencies.", e)
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
        
        if (!request.hasOption("--cache-path") || request.getOption("--cache-dir") == null || request.hasOption(
                "--use-cache-file"
            )
        ) {
            request.addOption("--no-cache")
        }

        val spotdlResponse: SpotDLResponse
        val process: Process
        val exitCode: Int
        val outBuffer = StringBuilder() //stdout
        val errBuffer = StringBuilder() //stderr
        val startTime = System.currentTimeMillis()
        val args = request.buildCommand()
        val command: MutableList<String?> = ArrayList()

        // --- CRITICAL FIX: INVOKE AS MODULE ---
        // Instead of executing the spotdl binary directly (which forces sys.path prepending),
        // execute python with "-m spotdl" to respect our custom PYTHONPATH.
        command.add(pythonPath!!.absolutePath)
        command.add("-m")
        command.add("spotdl")
        
        command.addAll(args)

        val processBuilder = ProcessBuilder(command)

        processBuilder.environment().apply {
            this["LD_LIBRARY_PATH"] = ENV_LD_LIBRARY_PATH
            this["SSL_CERT_FILE"] = ENV_SSL_CERT_FILE
            this["PATH"] = System.getenv("PATH") + ":" + binariesDirectory.absolutePath
            this["PYTHONHOME"] = ENV_PYTHONHOME
            this["PYTHONPATH"] = ENV_PYTHONPATH
            this["HOME"] = HOME 
            this["LDFLAGS"] = LDFLAGS
            this["TERM"] = "xterm-256color"
            this["FORCE_COLOR"] = "true"
        }
        
        if (isDebug) {
            Log.d(TAG, ">>> EXECUTE CMD: ${command.joinToString(" ")}")
            Log.d(TAG, ">>> EXECUTE ENV PYTHONPATH: $ENV_PYTHONPATH")
        }

        process = try {
            processBuilder.start()
        } catch (e: IOException) {
            throw SpotDLException(e)
        }

        if (processId != null) {
            idProcessMap[processId] = process
        }
        val outStream = process.inputStream
        val errStream = process.errorStream
        val stdOutProcessor = StreamProcessExtractor(outBuffer, outStream, callback)
        val stdErrProcessor = StreamGobbler(errBuffer, errStream)

        exitCode = try {
            stdOutProcessor.join()
            stdErrProcessor.join()
            process.waitFor()
        } catch (e: InterruptedException) {
            process.destroy()
            if (processId != null) idProcessMap.remove(processId)
            throw e
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

        val elapsedTime = System.currentTimeMillis() - startTime
        spotdlResponse = SpotDLResponse(command, exitCode, elapsedTime, out, err)

        return spotdlResponse
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

        if (!request.hasOption("--client-id") || !request.hasOption("--client-secret")) {
            request.addOption("--client-id", BuildConfig.CLIENT_ID)
            request.addOption("--client-secret", BuildConfig.CLIENT_SECRET)
        }
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