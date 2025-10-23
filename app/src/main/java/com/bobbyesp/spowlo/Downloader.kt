package com.bobbyesp.spowlo

import android.app.PendingIntent
import android.util.Log
import androidx.annotation.CheckResult
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.AnnotatedString
import com.bobbyesp.library.SpotDL
import com.bobbyesp.library.SpotDL.getInstance
import com.bobbyesp.library.domain.model.SpotifySong
import com.bobbyesp.library.util.exceptions.CanceledException
import com.bobbyesp.spowlo.App.Companion.applicationScope
import com.bobbyesp.spowlo.App.Companion.context
import com.bobbyesp.spowlo.App.Companion.startService
import com.bobbyesp.spowlo.App.Companion.stopService
import com.bobbyesp.spowlo.database.SongsInfoDao
import com.bobbyesp.spowlo.ui.common.containsEllipsis
import com.bobbyesp.spowlo.utils.DebugLogger
import com.bobbyesp.spowlo.utils.DownloaderUtil
import com.bobbyesp.spowlo.utils.FilesUtil
import com.bobbyesp.spowlo.utils.NotificationsUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.TimeoutException

object Downloader {

    private const val FETCH_TIMEOUT_MS = 30_000L
    private const val DOWNLOAD_TIMEOUT_MS = 120_000L

    sealed class State {
        data class DownloadingPlaylist(
            val currentItem: Int = 0,
            val itemCount: Int = 0,
        ) : State()
        data object DownloadingSong : State()
        data object FetchingInfo : State()
        data object Idle : State()
    }

    fun makeKey(url: String, additionalString: String = UUID.randomUUID().toString()): String =
        "${additionalString}_$url"

    data class ErrorState(
        val errorReport: String = "",
        val errorMessageResId: Int = R.string.unknown_error,
    ) {
        fun isErrorOccurred(): Boolean =
            errorMessageResId != R.string.unknown_error || errorReport.isNotEmpty()
    }

    data class DownloadTask(
        val url: String,
        val consoleOutput: String,
        val state: State,
        val currentLine: String,
        val taskName: String,
    ) {
        fun toKey() = makeKey(url, url.reversed())
        sealed class State {
            data class Error(val errorReport: String) : State()
            object Completed : State()
            object Canceled : State()
            data class Running(val progress: Float) : State()
        }

        override fun hashCode(): Int = (this.url + this.url.reversed()).hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DownloadTask
            return url == other.url &&
                consoleOutput == other.consoleOutput &&
                state == other.state &&
                currentLine == other.currentLine
        }

        fun onCopyLog(clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
            clipboardManager.setText(AnnotatedString(consoleOutput))
            ToastUtil.makeToastSuspend(context.getString(R.string.log_copied))
        }

        fun onCopyUrl(clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
            clipboardManager.setText(AnnotatedString(url))
            ToastUtil.makeToastSuspend(context.getString(R.string.link_copied))
        }

        fun onRestart() {
            applicationScope.launch(Dispatchers.IO) {
                executeParallelDownloadWithUrl(url, name = taskName)
            }
        }

        fun onCopyError(clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
            val errorState = state as? State.Error
            if (errorState != null) {
                clipboardManager.setText(AnnotatedString(errorState.errorReport))
            } else {
                clipboardManager.setText(AnnotatedString("No detailed error report available. Last line: $currentLine"))
            }
            ToastUtil.makeToast(R.string.error_copied)
        }

        fun onCancel() {
            toKey().run {
                SpotDL.getInstance().destroyProcessById(this)
                onProcessCanceled(this)
            }
        }
    }

    data class DownloadTaskItem(
        val info: SpotifySong = SpotifySong(),
        val spotifyUrl: String = "",
        val name: String = "",
        val artist: String = "",
        val duration: Double = 0.0,
        val isExplicit: Boolean = false,
        val hasLyrics: Boolean = false,
        val progress: Float = 0f,
        val progressText: String = "",
        val thumbnailUrl: String = "",
        val taskId: String = "",
        val output: String = "",
    )

    private fun SpotifySong.toTask(preferencesHash: Int): DownloadTaskItem =
        DownloadTaskItem(
            info = this,
            spotifyUrl = this.url,
            name = this.name,
            artist = this.artist,
            duration = this.duration ?: 0.0,
            isExplicit = this.explicit,
            hasLyrics = !this.lyrics.isNullOrEmpty(),
            progress = 0f,
            progressText = "",
            thumbnailUrl = this.cover_url,
            taskId = this.song_id + preferencesHash,
        )

    private var currentJob: Job? = null
    private var downloadResultTemp: Result<List<String>> = Result.failure(Exception())

    private val mutableDownloaderState: MutableStateFlow<State> = MutableStateFlow(State.Idle)
    val downloaderState = mutableDownloaderState.asStateFlow()

    private val mutableTaskState = MutableStateFlow(DownloadTaskItem())
    val taskState = mutableTaskState.asStateFlow()

    private val mutableErrorState = MutableStateFlow(ErrorState())
    val errorState = mutableErrorState.asStateFlow()

    private val mutableProcessCount = MutableStateFlow(0)
    private val processCount = mutableProcessCount.asStateFlow()

    private val mutableQuickDownloadCount = MutableStateFlow(0)

    private lateinit var songsDao: SongsInfoDao
    val mutableTaskList = mutableStateMapOf<String, DownloadTask>()

    fun initialize(dao: SongsInfoDao) {
        songsDao = dao
    }

    init {
        applicationScope.launch {
            downloaderState.combine(processCount) { state, cnt ->
                if (cnt > 0) true else state !is State.Idle
            }.combine(mutableQuickDownloadCount) { isRunning, cnt ->
                if (!isRunning) cnt > 0 else true
            }.collect { if (it) startService() else stopService() }
        }
    }

    fun onTaskStarted(url: String, name: String) =
        DownloadTask(
            url = url,
            consoleOutput = "",
            state = DownloadTask.State.Running(0f),
            currentLine = "",
            taskName = name
        ).run {
            DebugLogger.log("DL", "onTaskStarted url='$url' name='$name'")
            mutableTaskList[this.toKey()] = this
            val key = makeKey(url, url.reversed())
            NotificationsUtil.notifyProgress(
                name + " - " + context.getString(R.string.parallel_download),
                notificationId = key.toNotificationId(),
                progress = (state as DownloadTask.State.Running).progress.toInt(),
                text = currentLine
            )
        }

    fun updateTaskOutput(url: String, line: String, progress: Float, isPlaylist: Boolean = false) {
        val key = makeKey(url, url.reversed())
        val oldValue = mutableTaskList[key] ?: return
        val newValue = oldValue.run {
            if (currentLine == line || line.containsEllipsis() || consoleOutput.contains(line)) return
            when (isPlaylist) {
                true -> copy(
                    consoleOutput = consoleOutput + line + "\n",
                    currentLine = line,
                    state = DownloadTask.State.Running(
                        if (line.contains("Total")) getProgress(line) else (state as DownloadTask.State.Running).progress
                    )
                )
                false -> copy(
                    consoleOutput = consoleOutput + line + "\n",
                    currentLine = line,
                    state = DownloadTask.State.Running(progress)
                )
            }
        }
        mutableTaskList[key] = newValue
    }

    private fun getProgress(line: String): Float {
        val regex = Regex("(\\d+)%")
        val matchResult = regex.find(line)
        return (matchResult?.groupValues?.get(1)?.toFloat() ?: 0f) / 100f
    }

    fun onTaskEnded(url: String, response: String? = null, notificationTitle: String? = null) {
        DebugLogger.log("DL", "onTaskEnded url='$url'")
        val key = makeKey(url, url.reversed())
        NotificationsUtil.finishNotification(
            notificationId = key.toNotificationId(),
            title = notificationTitle,
            text = context.getString(R.string.status_completed),
        )
        mutableTaskList.run {
            val oldValue = get(key) ?: return
            val newValue = oldValue.copy(state = DownloadTask.State.Completed).run {
                response?.let { copy(consoleOutput = response) } ?: this
            }
            this[key] = newValue
        }
        FilesUtil.scanDownloadDirectoryToMediaLibrary(App.audioDownloadDir)
    }

    fun onTaskError(errorReport: String, url: String) =
        mutableTaskList.run {
            DebugLogger.log("DL", "onTaskError url='$url'")
            val key = makeKey(url, url.reversed())
            NotificationsUtil.makeErrorReportNotification(
                notificationId = key.toNotificationId(),
                error = errorReport
            )
            val oldValue = mutableTaskList[key] ?: return
            mutableTaskList[key] = oldValue.copy(
                state = DownloadTask.State.Error(errorReport),
                currentLine = errorReport,
                consoleOutput = oldValue.consoleOutput + "\n" + errorReport
            )
        }

    fun onProcessEnded() = mutableProcessCount.update { it - 1 }
    fun onProcessCanceled(taskId: String) =
        mutableTaskList.run { get(taskId)?.let { this.put(taskId, it.copy(state = DownloadTask.State.Canceled)) } }

    fun isDownloaderAvailable(): Boolean {
        if (downloaderState.value !is State.Idle) {
            ToastUtil.makeToastSuspend(context.getString(R.string.task_running))
            return false
        }
        return true
    }

    @CheckResult
    private fun downloadSong(
        songInfo: SpotifySong,
        preferences: DownloaderUtil.DownloadPreferences = DownloaderUtil.DownloadPreferences()
    ): Result<List<String>> {
        val isDownloadingPlaylist = downloaderState.value is State.DownloadingPlaylist
        mutableTaskState.update { songInfo.toTask(preferencesHash = preferences.hashCode()) }
        val notificationId = preferences.hashCode() + songInfo.song_id.getNumbers()
        if (!isDownloadingPlaylist) updateState(State.DownloadingSong)
        return DownloaderUtil.downloadSong(
            songInfo = songInfo,
            downloadPreferences = preferences,
            taskId = songInfo.song_id + preferences.hashCode()
        ) { progress, _, line ->
            Log.d("Downloader", line)
            mutableTaskState.update { it.copy(progress = progress, progressText = line) }
            NotificationsUtil.notifyProgress(
                notificationId = notificationId,
                progress = progress.toInt(),
                text = line,
                title = songInfo.name
            )
        }.onFailure {
            Log.d("Downloader", "$it")
            if (it is CanceledException) {
                // Still treat as error: ensure UI returns to Idle
                manageDownloadError(
                    it, false, notificationId = notificationId, isTaskAborted = !isDownloadingPlaylist,
                    songName = "${songInfo.artist} - ${songInfo.name}"
                )
                return@onFailure
            }
            manageDownloadError(
                it, false, notificationId = notificationId, isTaskAborted = !isDownloadingPlaylist,
                songName = "${songInfo.artist} - ${songInfo.name}"
            )
        }.onSuccess {
            if (!isDownloadingPlaylist) finishProcessing()
            val text = context.getString(if (it.isEmpty()) R.string.status_completed else R.string.download_finish_notification)
            FilesUtil.createIntentForOpeningFile(it.firstOrNull()).run {
                NotificationsUtil.finishNotification(
                    notificationId,
                    title = "${songInfo.artist} - ${songInfo.name}",
                    text = text,
                    intent = if (this != null) PendingIntent.getActivity(context, 0, this, PendingIntent.FLAG_IMMUTABLE) else null
                )
            }
        }
    }

    fun getInfoAndDownload(
        url: String,
        downloadPreferences: DownloaderUtil.DownloadPreferences = DownloaderUtil.DownloadPreferences(),
        skipInfoFetch: Boolean = false
    ) {
        DebugLogger.log("DL", "getInfoAndDownload url='$url' skipInfoFetch=$skipInfoFetch")
        currentJob = applicationScope.launch(Dispatchers.IO) {
            updateState(State.FetchingInfo)
            if (skipInfoFetch) {
                try {
                    val deferred = async(Dispatchers.IO) {
                        downloadSong(
                            songInfo = SpotifySong(url = url),
                            preferences = downloadPreferences
                        )
                    }
                    DebugLogger.log("DL", "direct download awaiting timeout=${DOWNLOAD_TIMEOUT_MS}ms")
                    val result = withTimeout(DOWNLOAD_TIMEOUT_MS) { deferred.await() }
                    downloadResultTemp = result.onFailure {
                        manageDownloadError(it, isFetchingInfo = true, isTaskAborted = true)
                    }
                } catch (e: TimeoutCancellationException) {
                    val tid = taskState.value.taskId
                    DebugLogger.log("DL", "download TIMEOUT; taskId='$tid' -> destroying if present")
                    if (tid.isNotEmpty()) SpotDL.getInstance().destroyProcessById(tid)
                    manageDownloadError(TimeoutException("Download timed out"), isFetchingInfo = true, isTaskAborted = true)
                }
                return@launch
            } else {
                val procId = UUID.randomUUID().toString()
                DebugLogger.log("DL", "metadata fetch started procId=$procId")
                try {
                    val deferred = async(Dispatchers.IO) {
                        DownloaderUtil.fetchSongInfoFromUrl(url = url, processId = procId)
                    }
                    DebugLogger.log("DL", "awaiting metadata timeout=${FETCH_TIMEOUT_MS}ms")
                    val result = withTimeout(FETCH_TIMEOUT_MS) { deferred.await() }
                    result
                        .onFailure {
                            DebugLogger.logError("DL", it)
                            manageDownloadError(it, isFetchingInfo = true, isTaskAborted = true)
                        }
                        .onSuccess { info ->
                            DebugLogger.log("DL", "metadata success count=${info.size}")
                            for (song in info) {
                                downloadResultTemp = downloadSong(songInfo = song, preferences = downloadPreferences)
                            }
                        }
                } catch (e: TimeoutCancellationException) {
                    DebugLogger.log("DL", "metadata TIMEOUT for procId=$procId, destroying process")
                    SpotDL.getInstance().destroyProcessById(procId)
                    manageDownloadError(TimeoutException("Metadata fetch timed out"), isFetchingInfo = true, isTaskAborted = true)
                }
            }
        }
    }

    fun getRequestedMetadata(
        url: String,
        downloadPreferences: DownloaderUtil.DownloadPreferences = DownloaderUtil.DownloadPreferences()
    ) {
        DebugLogger.log("DL", "getRequestedMetadata url='$url'")
        currentJob = applicationScope.launch(Dispatchers.IO) {
            updateState(State.FetchingInfo)
            val procId = UUID.randomUUID().toString()
            DebugLogger.log("DL", "metadata requested procId=$procId")
            try {
                val deferred = async(Dispatchers.IO) {
                    DownloaderUtil.fetchSongInfoFromUrl(url = url, processId = procId)
                }
                DebugLogger.log("DL", "awaiting metadata timeout=${FETCH_TIMEOUT_MS}ms")
                val result = withTimeout(FETCH_TIMEOUT_MS) { deferred.await() }
                result
                    .onFailure {
                        DebugLogger.logError("DL", it)
                        manageDownloadError(it, isFetchingInfo = true, isTaskAborted = true)
                    }
                    .onSuccess { info ->
                        DebugLogger.log("DL", "metadata success (requested) count=${info.size}")
                        DownloaderUtil.updateSongsState(info)
                        mutableTaskState.update {
                            DownloaderUtil.songsState.value[0].toTask(preferencesHash = downloadPreferences.hashCode())
                        }
                        finishProcessing()
                    }
            } catch (e: TimeoutCancellationException) {
                DebugLogger.log("DL", "metadata TIMEOUT (requested) procId=$procId; destroying process")
                SpotDL.getInstance().destroyProcessById(procId)
                manageDownloadError(TimeoutException("Metadata fetch timed out"), isFetchingInfo = true, isTaskAborted = true)
            }
        }
    }

    private fun updateState(state: State) {
        DebugLogger.log("DL", "state -> ${state::class.simpleName}")
        mutableDownloaderState.update { state }
    }

    fun clearErrorState() = mutableErrorState.update { ErrorState() }

    fun showErrorMessage(resId: Int) {
        ToastUtil.makeToastSuspend(context.getString(resId))
        mutableErrorState.update { ErrorState(errorMessageResId = resId) }
    }

    private fun clearProgressState(isFinished: Boolean) {
        mutableTaskState.update { it.copy(progress = if (isFinished) 100f else 0f, progressText = "") }
        if (!isFinished) downloadResultTemp = Result.failure(Exception())
    }

    private fun finishProcessing() {
        if (downloaderState.value is State.Idle) return
        DebugLogger.log("DL", "finishProcessing: set progress 100 and Idle")
        mutableTaskState.update { it.copy(progress = 100f, progressText = "") }
        clearProgressState(isFinished = true)
        updateState(State.Idle)
        clearErrorState()
    }

    private fun manageDownloadError(
        th: Throwable,
        isFetchingInfo: Boolean,
        isTaskAborted: Boolean = true,
        notificationId: Int? = null,
        songName: String? = null
    ) {
        // Treat CanceledException as an error for state recovery (avoid infinite spinner)
        DebugLogger.logError("DL", th)
        th.printStackTrace()
        val resId = if (isFetchingInfo) R.string.fetch_info_error_msg else R.string.download_error_msg
        ToastUtil.makeToastSuspend(context.getString(resId))
        mutableErrorState.update { ErrorState(errorReport = th.stackTraceToString()) }
        notificationId?.let {
            NotificationsUtil.finishNotification(
                notificationId = notificationId,
                title = songName,
                text = "${context.getString(R.string.download_error_msg)}\n\n${th.message.toString()}"
            )
        }
        if (isTaskAborted) {
            updateState(State.Idle)
            clearProgressState(isFinished = false)
        }
    }

    fun cancelDownload() {
        DebugLogger.log("DL", "cancelDownload called, taskId='${taskState.value.taskId}'")
        ToastUtil.makeToast(context.getString(R.string.task_canceled))
        currentJob?.cancel(CancellationException(context.getString(R.string.task_canceled)))
        updateState(State.Idle)
        clearProgressState(isFinished = false)
        taskState.value.taskId.run {
            SpotDL.getInstance().destroyProcessById(this)
            NotificationsUtil.cancelNotification(this.toNotificationId())
        }
    }

    fun executeParallelDownloadWithUrl(url: String, name: String) = applicationScope.launch(Dispatchers.IO) {
        DownloaderUtil.executeParallelDownload(url, name)
    }

    fun onProcessStarted() = mutableProcessCount.update { it + 1 }
    fun String.toNotificationId(): Int = this.hashCode()
    private fun String.getNumbers(): Int {
        val sb = StringBuilder()
        for (c in this) if (c.isDigit()) sb.append(c)
        return if (sb.isNotEmpty()) sb.toString().toInt() else 0
    }
}