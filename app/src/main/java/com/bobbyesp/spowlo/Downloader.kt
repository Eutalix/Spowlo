package com.bobbyesp.spowlo

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.adamratzman.spotify.models.Track
import com.bobbyesp.library.SpotDL
import com.bobbyesp.library.SpotDLRequest
import com.bobbyesp.library.util.exceptions.CanceledException
import com.bobbyesp.spowlo.App.Companion.applicationScope
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.database.SongsInfoDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * A singleton object that manages the entire download lifecycle.
 * It acts as a central state holder and orchestrator for all download tasks,
 * interfacing with the SpotDL library and exposing state to the UI.
 */
object Downloader {

    private fun makeKey(url: String): String = "${UUID.randomUUID()}_$url"

    /**
     * Represents the state of a single download task in the UI list.
     * @param taskId A unique ID for this task instance.
     * @param track The Spotify Track object containing all metadata.
     * @param progress A float from 0.0 to 1.0 representing download progress.
     * @param progressText The latest log line from the spotdl process.
     * @param state The current lifecycle state of the task.
     */
    data class DownloadTask(
        val taskId: String,
        val track: Track,
        val progress: Float = 0f,
        val progressText: String = "Waiting in queue...",
        val state: State = State.QUEUED
    ) {
        sealed class State {
            object QUEUED : State()
            data class RUNNING(val progress: Float) : State()
            object COMPLETED : State()
            object CANCELED : State()
            data class ERROR(val message: String) : State()
        }
    }

    // This map holds all active and completed download tasks for the UI.
    // It's a `mutableStateMapOf` so that Compose UIs can react to its changes.
    val tasks = mutableStateMapOf<String, DownloadTask>()

    // StateFlow to indicate if any download is currently in progress.
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val spotdl = SpotDL.getInstance()
    
    // Hilt entry point to get a DAO instance from the Application context.
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloaderEntryPoint {
        fun songsInfoDao(): SongsInfoDao
    }

    /**
     * The main entry point for starting a song download.
     * @param track The Spotify Track object to be downloaded.
     * @param outputFormat The desired audio format (e.g., "mp3", "flac").
     */
    fun downloadSong(track: Track, outputFormat: String = "mp3") {
        val taskId = makeKey(track.uri.uri)
        val task = DownloadTask(taskId = taskId, track = track)
        
        // Add the task to the UI list in a 'QUEUED' state.
        tasks[taskId] = task

        // Launch the download on the IO dispatcher to avoid blocking the main thread.
        applicationScope.launch(Dispatchers.IO) {
            _isDownloading.update { true }
            
            try {
                // Build the request for the spotdl library using the modern syntax.
                val request = SpotDLRequest()
                    .addUrl(track.uri.uri) // Use the track's URI as the query.
                    .addOption("--output", App.audioDownloadDir) // Use the user-configured download directory.
                    .addOption("--format", outputFormat)

                // Execute the download command and listen for progress updates via the callback.
                val response = spotdl.execute(request, taskId) { progress, eta, line ->
                    // Update the task's progress in the UI map on the main thread.
                    // This will efficiently trigger a recomposition in any observing Compose UI.
                    launch(Dispatchers.Main) {
                        tasks[taskId] = tasks[taskId]?.copy(
                            state = DownloadTask.State.RUNNING(progress),
                            progress = progress,
                            progressText = line.trim()
                        )
                    }
                }
                
                // Once the spotdl process completes, check its exit code.
                if (response.exitCode == 0) {
                    val downloadedFile = spotdl.findDownloadedFile(response.output, track.name)
                    if (downloadedFile != null) {
                        launch(Dispatchers.Main) {
                            tasks[taskId] = tasks[taskId]?.copy(state = DownloadTask.State.COMPLETED, progress = 1.0f, progressText = "Completed!")
                        }

                        // Save the downloaded song info to the Room database.
                        val songInfo = DownloadedSongInfo(
                            id = 0, // Autogenerated by Room
                            songName = track.name,
                            songAuthor = track.artists.joinToString { it.name },
                            songUrl = track.uri.uri,
                            thumbnailUrl = track.album.images.firstOrNull()?.url ?: "",
                            songPath = downloadedFile.absolutePath,
                            songDuration = track.durationMs.toDouble() / 1000.0,
                        )
                        saveToDatabase(songInfo)
                    } else {
                        throw Exception("Downloaded file could not be found in spotdl's output.")
                    }
                } else {
                    throw Exception(response.error.ifEmpty { "spotdl exited with code ${response.exitCode}" })
                }
            } catch (e: CanceledException) {
                launch(Dispatchers.Main) { tasks[taskId] = tasks[taskId]?.copy(state = DownloadTask.State.CANCELED) }
                Log.d("Downloader", "Task $taskId was canceled by the user.")
            } catch (e: CancellationException) {
                 launch(Dispatchers.Main) { tasks[taskId] = tasks[taskId]?.copy(state = DownloadTask.State.CANCELED) }
                 Log.d("Downloader", "Task $taskId was canceled by coroutine cancellation.")
            } catch (e: Exception) {
                launch(Dispatchers.Main) { tasks[taskId] = tasks[taskId]?.copy(state = DownloadTask.State.ERROR(e.message ?: "Unknown error")) }
                Log.e("Downloader", "Download failed for task $taskId", e)
            } finally {
                // After the task finishes (or fails), check if there are any other running tasks
                // before setting the global state to idle.
                if (tasks.values.none { it.state is DownloadTask.State.RUNNING || it.state is DownloadTask.State.QUEUED }) {
                    _isDownloading.update { false }
                }
            }
        }
    }
    
    /**
     * Public function to cancel a specific download task by its ID.
     */
    fun cancelDownload(taskId: String) {
        spotdl.destroyProcessById(taskId)
    }

    // Helper function to save song metadata to the database.
    private suspend fun saveToDatabase(songInfo: DownloadedSongInfo) {
        try {
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                App.context,
                DownloaderEntryPoint::class.java
            )
            hiltEntryPoint.songsInfoDao().insertAll(songInfo)
            Log.d("Downloader", "Successfully saved song info to the database.")
        } catch (e: Exception) {
            Log.e("Downloader", "Failed to save song info to the database.", e)
        }
    }
}