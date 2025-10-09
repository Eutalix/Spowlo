package com.bobbyesp.spowlo.features.spotify_api.data.remote

import android.util.Log
import com.adamratzman.spotify.*
import com.adamratzman.spotify.models.*
import com.adamratzman.spotify.utils.Market
import com.bobbyesp.library.SpotDL
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Centralized Spotify API provider used by the app.
 *
 * Behavior:
 *  - Builds a SpotifyClientApi on first use using SpotDL anonymous token (so app works without user credentials).
 *  - Exposes helper methods used across the UI (names preserved for compatibility).
 *  - Exposes `setUserCredentialsAndRebuild()` to rebuild the client with user-provided credentials.
 *  - Exposes `clearClient()` to drop current client and fallback to anonymous on next call.
 */

@Module
@InstallIn(SingletonComponent::class)
object SpotifyApiRequests {

    // Cached client (may be anonymous token client or credentialed)
    @Volatile
    private var api: SpotifyClientApi? = null

    // Prevent concurrent builds
    private val buildMutex = Mutex()

    /**
     * Provide the singleton SpotifyClientApi. Builds lazily using SpotDL anonymous token if needed.
     */
    @Provides
    @Singleton
    suspend fun provideSpotifyApi(): SpotifyClientApi {
        api?.let { return it } // fast path

        return buildMutex.withLock {
            api?.let { return it } // double-check

            try {
                Log.d("SpotifyApiRequests", "Building SpotifyClientApi using SpotDL anonymous token...")
                val anonymousToken = SpotDL.getInstance().getAnonymousToken()
                val token = Token(anonymousToken, "Bearer", 3600)

                api = spotifyClientApi(
                    clientId = null,
                    clientSecret = null,
                    redirectUri = null,
                    token = token
                ) {
                    automaticRefresh = false // anonymous tokens can't be refreshed automatically
                }.build()

                Log.d("SpotifyApiRequests", "Built anonymous SpotifyClientApi successfully.")
                return api!!
            } catch (e: Exception) {
                Log.e("SpotifyApiRequests", "Failed to build anonymous SpotifyClientApi", e)
                throw e
            }
        }
    }

    /**
     * Rebuild internal client using user's credentials.
     * Call this when the user provides clientId/clientSecret (optional redirectUri).
     */
    suspend fun setUserCredentialsAndRebuild(clientId: String, clientSecret: String, redirectUri: String?) {
        buildMutex.withLock {
            try {
                Log.d("SpotifyApiRequests", "Rebuilding SpotifyClientApi with user credentials...")
                api = spotifyClientApi(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    redirectUri = redirectUri
                ) {
                    automaticRefresh = true
                }.build()
                Log.d("SpotifyApiRequests", "Rebuilt credentialed SpotifyClientApi.")
            } catch (e: Exception) {
                Log.e("SpotifyApiRequests", "Failed to rebuild SpotifyClientApi with user credentials", e)
                throw e
            }
        }
    }

    /**
     * Drop the cached client so next call falls back to anonymous token build.
     */
    fun clearClient() {
        api = null
    }

    // -----------------------
    // Core helpers (suspend)
    // -----------------------

    suspend fun searchAllTypes(searchQuery: String): SpotifySearchResult {
        return kotlin.runCatching {
            provideSpotifyApi().search.searchAllTypes(
                searchQuery,
                limit = 50,
                offset = 0,
                market = Market.US
            )
        }.getOrElse {
            Log.d("SpotifyApiRequests", "Error searching all types: ${it.message}")
            SpotifySearchResult()
        }
    }

    suspend fun searchTracks(searchQuery: String): List<Track> {
        return kotlin.runCatching {
            provideSpotifyApi().search.searchTrack(searchQuery, limit = 50).items
        }.getOrElse {
            Log.d("SpotifyApiRequests", "Error searching tracks: ${it.message}")
            listOf()
        }
    }

    suspend fun getPlaylistById(id: String): Playlist? {
        return kotlin.runCatching {
            provideSpotifyApi().playlists.getPlaylist(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting playlist by ID: ${it.message}")
        }.getOrNull()
    }

    suspend fun getTrackById(id: String): Track? {
        return kotlin.runCatching {
            provideSpotifyApi().tracks.getTrack(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting track by ID: ${it.message}")
        }.getOrNull()
    }

    suspend fun getArtistById(id: String): Artist? {
        return kotlin.runCatching {
            provideSpotifyApi().artists.getArtist(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting artist by ID: ${it.message}")
        }.getOrNull()
    }

    suspend fun getAlbumById(id: String): Album? {
        return kotlin.runCatching {
            provideSpotifyApi().albums.getAlbum(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting album by ID: ${it.message}")
        }.getOrNull()
    }

    suspend fun getArtistTopTracks(artistId: String): List<Track>? {
        return kotlin.runCatching {
            provideSpotifyApi().artists.getArtistTopTracks(artistId, Market.US)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting artist top tracks: ${it.message}")
        }.getOrNull()
    }

    suspend fun getArtistAlbums(artistId: String): PagingObject<SimpleAlbum>? {
        return kotlin.runCatching {
            provideSpotifyApi().artists.getArtistAlbums(artist = artistId, market = Market.US, limit = 20)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting artist albums: ${it.message}")
        }.getOrNull()
    }

    suspend fun getAudioFeatures(id: String): AudioFeatures? {
        return kotlin.runCatching {
            provideSpotifyApi().tracks.getAudioFeatures(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting audio features: ${it.message}")
        }.getOrNull()
    }

    // -----------------------
    // Compatibility wrappers
    // These functions preserve older names used across the UI. They simply delegate
    // to the canonical implementations above so we don't need to change many UI files.
    // -----------------------

    // Provide / Provides naming (both variants) used in codebase
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

    // For backward compatibility: a method named provideUserSearch existed previously in some code
    @Provides
    @Singleton
    suspend fun provideUserSearch(query: String): SpotifyPublicUser? {
        // older callers sometimes call with "bobbyesp" — keep compatibility by returning profile
        return kotlin.runCatching {
            provideSpotifyApi().users.getProfile(query)
        }.getOrNull()
    }
}