package com.bobbyesp.spowlo.features.spotify_api.data.remote

import android.util.Log
import com.adamratzman.spotify.*
import com.adamratzman.spotify.models.*
import com.bobbyesp.library.SpotDL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpotifyApiRequests {

    private var api: SpotifyClientApi? = null

    /**
     * Provides a Spotify API instance.
     * - If an instance exists, it's checked for validity with a lightweight call.
     * - Otherwise, a new client is built using the robust anonymous token from SpotDL.
     */
    @Provides
    @Singleton
    suspend fun provideSpotifyApi(): SpotifyClientApi {
        if (api != null) {
            try {
                // A lightweight, inexpensive API call to check if the token is still active.
                api!!.browse.getAvailableGenreSeeds()
                return api!!
            } catch (e: SpotifyException.AuthenticationException) {
                Log.w("SpotifyApiRequests", "Token expired or invalid. Rebuilding API.")
            } catch (e: Exception) {
                Log.w("SpotifyApiRequests", "API check failed, rebuilding. Reason: ${e.message}")
            }
        }

        buildApiUsingAnonymousToken()
        return api!!
    }

    /**
     * Builds the API client using the anonymous token retrieved from SpotDL.
     * This approach is simple and robust, avoiding complex Ktor configurations
     * that were causing CI build failures.
     */
    private suspend fun buildApiUsingAnonymousToken() {
        try {
            Log.d("SpotifyApiRequests", "Attempting to get anonymous token from SpotDL...")
            val anonymousToken = SpotDL.getInstance().getAnonymousToken()
            val token = Token(anonymousToken, "Bearer", 3600) // 3600 seconds = 1 hour validity

            Log.d("SpotifyApiRequests", "Building client API with fetched token.")
            // Build a client-side API using the existing token.
            // We let the library choose the appropriate default client for Android.
            api = spotifyClientApi(token = token) {
                options.automaticRefresh = false // Cannot refresh an anonymous token.
                options.defaultUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
            }.build()

            Log.d("SpotifyApiRequests", "API built successfully with anonymous token.")
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "FATAL: Failed to build API using anonymous token.", e)
            throw e // Re-throw so the app knows initialization failed.
        }
    }
    
    // --- ALL ORIGINAL HELPER METHODS ARE PRESERVED BELOW ---
    // They now use the correctly built `api` instance via `provideSpotifyApi()`.

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

    @Provides
    @Singleton
    suspend fun provideSearchAllTypes(query: String): SpotifySearchResult {
        return searchAllTypes(query)
    }

    private suspend fun searchTracks(searchQuery: String): List<Track> {
        return kotlin.runCatching {
            provideSpotifyApi().search.searchTrack(searchQuery, limit = 50).items
        }.getOrElse {
            Log.d("SpotifyApiRequests", "Error searching tracks: ${it.message}")
            listOf()
        }
    }

    @Provides
    @Singleton
    suspend fun provideSearchTracks(query: String): List<Track> {
        return searchTracks(query)
    }

    suspend fun getPlaylistById(id: String): Playlist? {
        return kotlin.runCatching {
            provideSpotifyApi().playlists.getPlaylist(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting playlist by ID: ${it.message}")
        }.getOrNull()
    }

    @Provides
    @Singleton
    suspend fun provideGetPlaylistById(id: String): Playlist? {
        return getPlaylistById(id)
    }

    suspend fun getTrackById(id: String): Track? {
        return kotlin.runCatching {
            provideSpotifyApi().tracks.getTrack(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting track by ID: ${it.message}")
        }.getOrNull()
    }

    @Provides
    @Singleton
    suspend fun provideGetTrackById(id: String): Track? {
        return getTrackById(id)
    }

    private suspend fun getArtistById(id: String): Artist? {
        return kotlin.runCatching {
            provideSpotifyApi().artists.getArtist(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting artist by ID: ${it.message}")
        }.getOrNull()
    }

    @Provides
    @Singleton
    suspend fun provideGetArtistById(id: String): Artist? {
        return getArtistById(id)
    }

    suspend fun getAlbumById(id: String): Album? {
        return kotlin.runCatching {
            provideSpotifyApi().albums.getAlbum(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting album by ID: ${it.message}")
        }.getOrNull()
    }

    @Provides
    @Singleton
    suspend fun providesGetAlbumById(id: String): Album? {
        return getAlbumById(id)
    }

    private suspend fun getAudioFeatures(id: String): AudioFeatures? {
        return kotlin.runCatching {
            provideSpotifyApi().tracks.getAudioFeatures(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting audio features: ${it.message}")
        }.getOrNull()
    }

    @Provides
    @Singleton
    suspend fun providesGetAudioFeatures(id: String): AudioFeatures? {
        return getAudioFeatures(id)
    }

    private suspend fun getArtistTopTracks(artistId: String): List<Track>? {
        return kotlin.runCatching {
            provideSpotifyApi().artists.getArtistTopTracks(artistId, Market.US)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting artist top tracks: ${it.message}")
        }.getOrNull()
    }

    @Provides
    @Singleton
    suspend fun providesGetArtistTopTracks(id: String): List<Track>? {
        return getArtistTopTracks(id)
    }

    private suspend fun getArtistAlbums(artistId: String): PagingObject<SimpleAlbum>? {
        return kotlin.runCatching {
            provideSpotifyApi().artists.getArtistAlbums(artist = artistId, market = Market.US, limit = 20)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error getting artist albums: ${it.message}")
        }.getOrNull()
    }

    @Provides
    @Singleton
    suspend fun providesGetArtistAlbums(id: String): PagingObject<SimpleAlbum>? {
        return getArtistAlbums(id)
    }
}