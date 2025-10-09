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

@Module
@InstallIn(SingletonComponent::class)
object SpotifyApiRequests {

    // singleton client (may be anonymous or credentialed)
    @Volatile
    private var api: SpotifyClientApi? = null

    // mutex to avoid concurrent builds
    private val buildMutex = Mutex()

    /**
     * Provide the SpotifyClientApi singleton.
     * - First call will build using SpotDL anonymous token (if no credentialed client present).
     * - Subsequent calls return cached client.
     */
    @Provides
    @Singleton
    suspend fun provideSpotifyApi(): SpotifyClientApi {
        // fast path
        api?.let { return it }

        // ensure only one builder runs at a time
        return buildMutex.withLock {
            api?.let { return it }

            // try to build using anonymous token from SpotDL
            try {
                Log.d("SpotifyApiRequests", "Building SpotifyClientApi using anonymous token from SpotDL...")
                val anonymousToken = SpotDL.getInstance().getAnonymousToken()
                val token = Token(anonymousToken, "Bearer", 3600)

                api = spotifyClientApi(
                    clientId = null,
                    clientSecret = null,
                    redirectUri = null,
                    token = token
                ) {
                    automaticRefresh = false // anonymous tokens won't auto-refresh
                }.build()

                Log.d("SpotifyApiRequests", "Built anonymous SpotifyClientApi successfully.")
                return api!!
            } catch (e: Exception) {
                Log.w("SpotifyApiRequests", "Anonymous token build failed: ${e.message}. Will rethrow.", e)
                throw e
            }
        }
    }

    /**
     * Force-rebuild the internal client using provided user credentials (clientId/clientSecret/redirectUri).
     * Call this when user provides credentials in the UI.
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
                    // other options may be set here if needed
                }.build()

                Log.d("SpotifyApiRequests", "Rebuilt SpotifyClientApi with user credentials.")
            } catch (e: Exception) {
                Log.e("SpotifyApiRequests", "Failed to build credentialed SpotifyClientApi", e)
                throw e
            }
        }
    }

    /**
     * Clear the current client (useful to force fallback to anonymous token on next request).
     */
    fun clearClient() {
        api = null
    }

    // -------------------------
    // Convenience wrappers (suspend)
    // -------------------------

    private suspend fun userSearch(userQuery: String): SpotifyPublicUser? {
        return provideSpotifyApi().users.getProfile(userQuery)
    }

    @Provides
    @Singleton
    suspend fun provideUserSearch(query: String): SpotifyPublicUser? {
        return userSearch("bobbyesp")
    }

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
}