package com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.Track
import com.bobbyesp.library.SpotDL
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.features.spotify_api.model.SpotifyDataType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The ViewModel for the SpotifyItemPage screen.
 * It is responsible for loading metadata for a specific Spotify item (Track, Album, Playlist, Artist)
 * and handling download requests originating from that screen.
 */
@HiltViewModel
class PlaylistPageViewModel @Inject constructor() : ViewModel() {

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private val spotdl = SpotDL.getInstance()

    data class ViewState(
        val state: PlaylistDataState = PlaylistDataState.Loading
    )

    /**
     * Loads the metadata for a given Spotify ID and type.
     * It uses the unified API instance from the SpotDL library.
     */
    fun loadData(id: String, type: SpotifyDataType) {
        _viewState.update { it.copy(state = PlaylistDataState.Loading) }
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    when (type) {
                        SpotifyDataType.TRACK -> spotdl.spotifyApi?.tracks?.getTrack(id)
                        SpotifyDataType.ALBUM -> spotdl.spotifyApi?.albums?.getAlbum(id)
                        SpotifyDataType.PLAYLIST -> spotdl.spotifyApi?.playlists?.getPlaylist(id)
                        SpotifyDataType.ARTIST -> spotdl.spotifyApi?.artists?.getArtist(id)
                    }
                }
                if (data != null) {
                    _viewState.update { it.copy(state = PlaylistDataState.Loaded(data)) }
                } else {
                    throw Exception("Data not found for id: $id, type: $type")
                }
            } catch (e: Exception) {
                Log.e("PlaylistPageViewModel", "Error loading data", e)
                _viewState.update { it.copy(state = PlaylistDataState.Error(e)) }
            }
        }
    }

    /**
     * Initiates a download for a single track.
     * This function fetches the full Track object if needed, then delegates
     * the download call to the central Downloader singleton.
     */
    fun downloadTrack(track: Any) {
        viewModelScope.launch {
            val fullTrack = when (track) {
                is Track -> track // Already a full track object
                is com.adamratzman.spotify.models.SimpleTrack -> {
                    withContext(Dispatchers.IO) { spotdl.getTrack(track.id) }
                }
                is com.adamratzman.spotify.models.PlaylistTrack -> {
                    track.track?.asTrack ?: withContext(Dispatchers.IO) { track.track?.id?.let { spotdl.getTrack(it) } }
                }
                else -> null
            }

            fullTrack?.let {
                Downloader.downloadSong(it)
            }
        }
    }
    
    /**
     * Initiates a download for an entire collection (Album or Playlist).
     */
    fun downloadCollection(collection: Any) {
        val tracks = when (collection) {
            is Album -> collection.tracks.items.mapNotNull { it }
            is Playlist -> collection.tracks.items.mapNotNull { it.track?.asTrack }
            else -> emptyList()
        }

        tracks.forEach { track ->
            track?.let { Downloader.downloadSong(it) }
        }
    }
}

// Sealed class to represent the different UI states for data loading.
sealed class PlaylistDataState {
    object Loading : PlaylistDataState()
    data class Error(val error: Exception) : PlaylistDataState()
    data class Loaded(val data: Any) : PlaylistDataState()
}