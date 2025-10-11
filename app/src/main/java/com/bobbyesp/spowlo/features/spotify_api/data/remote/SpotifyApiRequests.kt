package com.bobbyesp.spowlo.features.spotify_api.data.remote

import android.util.Log
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.AudioFeatures
import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SpotifySearchResult
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.utils.Market
import com.bobbyesp.library.SpotDL
import kotlinx.coroutines.delay

/**
 * Centralized Spotify API provider for the app module.
 *
 * This object is now a simple proxy that retrieves the already-initialized
 * SpotifyAppApi instance from the :library module's SpotDLCore.
 * This approach unifies API access across the entire application, removing redundant
 * client initializations and resolving previous conflicts.
 */
object SpotifyApiRequests {

    /**
     * Safely retrieves the SpotifyAppApi instance from the SpotDL library.
     * It includes a brief waiting period to handle cases where the app requests
     * the API before the library has finished its asynchronous initialization.
     * @return The initialized SpotifyAppApi instance.
     * @throws IllegalStateException if the API is not available after a timeout.
     */
    private suspend fun provideSpotifyApi(): SpotifyAppApi {
        var api = SpotDL.getInstance().spotifyApi
        var attempts = 0
        while (api == null && attempts < 50) { // Wait up to 5 seconds.
            delay(100)
            api = SpotDL.getInstance().spotifyApi
            attempts++
        }
        return api ?: throw IllegalStateException("Spotify API from :library is not available or failed to initialize.")
    }

    // -----------------------------------------------------------------
    // Core helper functions to interact with the Spotify API.
    // These wrap the API calls in try-catch blocks for robustness.
    // -----------------------------------------------------------------

    suspend fun searchAllTypes(searchQuery: String): SpotifySearchResult {
        return try {
            provideSpotifyApi().search.searchAllTypes(
                query = searchQuery,
                limit = 50,
                offset = 0,
                market = Market.FROM_TOKEN
            )
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "Error searching all types", e)
            SpotifySearchResult(tracks = null, artists = null, albums = null, playlists = null, shows = null, episodes = null)
        }
    }
    
    suspend fun getTrackById(id: String): Track? {
        return try {
            provideSpotifyApi().tracks.getTrack(id)
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "Error getting track by ID", e)
            null
        }
    }

    suspend fun getAlbumById(id: String): Album? {
        return try {
            provideSpotifyApi().albums.getAlbum(id)
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "Error getting album by ID", e)
            null
        }
    }

    suspend fun getPlaylistById(id: String): Playlist? {
        return try {
            provideSpotifyApi().playlists.getPlaylist(id)
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "Error getting playlist by ID", e)
            null
        }
    }
    
    suspend fun getArtistById(id: String): Artist? {
        return try {
            provideSpotifyApi().artists.getArtist(id)
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "Error getting artist by ID", e)
            null
        }
    }

    suspend fun getArtistTopTracks(artistId: String): List<Track>? {
        return try {
            provideSpotifyApi().artists.getArtistTopTracks(artistId)
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "Error getting artist top tracks", e)
            null
        }
    }

    suspend fun getArtistAlbums(artistId: String): PagingObject<SimpleAlbum>? {
        return try {
            provideSpotifyApi().artists.getArtistAlbums(artist = artistId, market = Market.FROM_TOKEN, limit = 20)
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "Error getting artist albums", e)
            null
        }
    }

    suspend fun getAudioFeatures(id: String): AudioFeatures? {
        return try {
            provideSpotifyApi().tracks.getAudioFeatures(id)
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "Error getting audio features", e)
            null
        }
    }
    
    // -----------------------------------------------------------------------------------
    // Compatibility wrappers: These functions preserve older names used throughout the UI.
    // They simply delegate to the canonical implementations above, so we don't need
    // to refactor every single UI file immediately.
    // -----------------------------------------------------------------------------------

    suspend fun provideGetTrackById(id: String): Track? = getTrackById(id)
    suspend fun provideGetAlbumById(id: String): Album? = getAlbumById(id)
    suspend fun provideGetPlaylistById(id: String): Playlist? = getPlaylistById(id)
    suspend fun provideGetArtistById(id: String): Artist? = getArtistById(id)
    suspend fun provideGetArtistTopTracks(id: String): List<Track>? = getArtistTopTracks(id)
    suspend fun provideGetAudioFeatures(id: String): AudioFeatures? = getAudioFeatures(id)

    suspend fun providesGetTrackById(id: String): Track? = getTrackById(id)
    suspend fun providesGetAlbumById(id: String): Album? = getAlbumById(id)
    suspend fun providesGetPlaylistById(id: String): Playlist? = getPlaylistById(id)
    suspend fun providesGetArtistById(id: String): Artist? = getArtistById(id)
    suspend fun providesGetArtistTopTracks(id: String): List<Track>? = getArtistTopTracks(id)
    suspend fun providesGetAudioFeatures(id: String): AudioFeatures? = getAudioFeatures(id)
}