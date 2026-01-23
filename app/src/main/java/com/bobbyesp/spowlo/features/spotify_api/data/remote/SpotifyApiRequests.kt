package com.bobbyesp.spowlo.features.spotify_api.data.remote

import android.util.Log
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.AudioFeatures
import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SpotifyPublicUser
import com.adamratzman.spotify.models.SpotifySearchResult
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.spotifyAppApi
import com.adamratzman.spotify.utils.Market
import com.bobbyesp.spowlo.BuildConfig
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.PreferencesUtil.getString
import com.bobbyesp.spowlo.utils.SPOTIFY_CLIENT_ID
import com.bobbyesp.spowlo.utils.SPOTIFY_CLIENT_SECRET
import com.bobbyesp.spowlo.utils.USE_SPOTIFY_CREDENTIALS
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SpotifyApiRequests {

    private const val TAG = "SpotifyApiRequests"

    private var api: SpotifyAppApi? = null
    private val apiMutex = Mutex()

    // State tracking variables to detect credential changes
    private var lastUseCustomCredentials: Boolean? = null
    private var lastClientId: String? = null
    private var lastClientSecret: String? = null

    /**
     * Resets the API instance.
     * This ensures the API is rebuilt with new credentials when settings change.
     */
    suspend fun resetApi() {
        apiMutex.withLock {
            api = null
            lastUseCustomCredentials = null
            lastClientId = null
            lastClientSecret = null
            Log.d(TAG, "Spotify API instance reset.")
        }
    }

    /**
     * Provides the Spotify API instance, rebuilding it if credentials have changed.
     */
    suspend fun provideSpotifyApi(): SpotifyAppApi {
        val useCustomCredentials = PreferencesUtil.getValue(USE_SPOTIFY_CREDENTIALS)

        val currentClientId = if (useCustomCredentials && SPOTIFY_CLIENT_ID.getString().isNotBlank()) {
            SPOTIFY_CLIENT_ID.getString().trim()
        } else {
            BuildConfig.CLIENT_ID ?: System.getenv("CLIENT_ID") ?: ""
        }

        val currentClientSecret = if (useCustomCredentials && SPOTIFY_CLIENT_SECRET.getString().isNotBlank()) {
            SPOTIFY_CLIENT_SECRET.getString().trim()
        } else {
            BuildConfig.CLIENT_SECRET ?: System.getenv("CLIENT_SECRET") ?: ""
        }

        // Check if we need to rebuild the API (first run or credentials changed)
        val needsRebuild = api == null ||
                lastUseCustomCredentials != useCustomCredentials ||
                lastClientId != currentClientId ||
                lastClientSecret != currentClientSecret

        if (needsRebuild) {
            apiMutex.withLock {
                // Double-check locking pattern
                val stillNeedsRebuild = api == null ||
                        lastUseCustomCredentials != useCustomCredentials ||
                        lastClientId != currentClientId ||
                        lastClientSecret != currentClientSecret

                if (stillNeedsRebuild) {
                    Log.d(TAG, "Building API. Custom Creds: $useCustomCredentials")

                    if (currentClientId.isBlank() || currentClientSecret.isBlank()) {
                        throw IllegalStateException("Spotify Credentials are missing or invalid.")
                    }

                    api = spotifyAppApi(currentClientId, currentClientSecret).build()

                    // Update tracking variables
                    lastUseCustomCredentials = useCustomCredentials
                    lastClientId = currentClientId
                    lastClientSecret = currentClientSecret
                }
            }
        }
        return api!!
    }

    // ========================================================================
    // Search Methods
    // ========================================================================

    suspend fun provideUserSearch(query: String): SpotifyPublicUser? {
        return runCatching {
            provideSpotifyApi().users.getProfile("bobbyesp")
        }.getOrNull()
    }

    suspend fun provideSearchAllTypes(query: String): SpotifySearchResult {
        return runCatching {
            provideSpotifyApi().search.searchAllTypes(
                query,
                limit = 50,
                offset = 0,
                market = Market.US
            )
        }.getOrDefault(SpotifySearchResult())
    }

    suspend fun provideSearchTracks(query: String): List<Track> {
        return runCatching {
            provideSpotifyApi().search.searchTrack(query, limit = 50)
        }.map { it.items }.getOrDefault(emptyList())
    }

    // ========================================================================
    // ID Lookup Methods
    // ========================================================================

    suspend fun provideGetPlaylistById(id: String): Playlist? {
        return runCatching {
            provideSpotifyApi().playlists.getPlaylist(id)
        }.onSuccess {
            Log.d(TAG, "Playlist found: ${it?.name}")
        }.getOrNull()
    }

    suspend fun provideGetTrackById(id: String): Track? {
        return runCatching {
            provideSpotifyApi().tracks.getTrack(id)
        }.getOrNull()
    }

    suspend fun provideGetArtistById(id: String): Artist? {
        return runCatching {
            provideSpotifyApi().artists.getArtist(id)
        }.getOrNull()
    }

    suspend fun providesGetAlbumById(id: String): Album? {
        return runCatching {
            provideSpotifyApi().albums.getAlbum(id)
        }.getOrNull()
    }

    suspend fun providesGetAudioFeatures(id: String): AudioFeatures? {
        return runCatching {
            provideSpotifyApi().tracks.getAudioFeatures(id)
        }.getOrNull()
    }

    // ========================================================================
    // Artist Methods
    // ========================================================================

    suspend fun providesGetArtistTopTracks(id: String): List<Track>? {
        val artist = provideGetArtistById(id)
        return artist?.let {
            runCatching {
                provideSpotifyApi().artists.getArtistTopTracks(id, Market.US)
            }.getOrNull()
        }
    }

    suspend fun providesGetArtistAlbums(id: String): PagingObject<SimpleAlbum>? {
        val artist = provideGetArtistById(id)
        return artist?.let {
            runCatching {
                provideSpotifyApi().artists.getArtistAlbums(
                    artist = id,
                    market = Market.US,
                    limit = 20
                )
            }.getOrNull()
        }
    }
}