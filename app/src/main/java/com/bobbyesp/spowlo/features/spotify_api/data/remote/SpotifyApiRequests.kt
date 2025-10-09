package com.bobbyesp.spowlo.features.spotify_api.data.remote

import android.util.Log
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.spotifyClientApi
import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.models.*
import com.adamratzman.spotify.utils.Market
import com.bobbyesp.library.SpotDL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpotifyApiRequests {

    // Shared API instance — built on demand
    private var api: SpotifyClientApi? = null

    /**
     * Provide a SpotifyClientApi instance.
     *
     * NOTE: this method is marked suspend in your project layout — keep consistent with callers.
     * It performs a lightweight probe to validate existing token and rebuilds the client when needed.
     */
    @Provides
    @Singleton
    suspend fun provideSpotifyApi(): SpotifyClientApi {
        if (api != null) {
            try {
                // Lightweight probe to ensure token still works
                api!!.browse.getAvailableGenreSeeds()
                return api!!
            } catch (e: SpotifyException.AuthenticationException) {
                Log.w("SpotifyApiRequests", "Token expired or invalid. Rebuilding API.")
            } catch (e: Exception) {
                Log.w("SpotifyApiRequests", "API validation failed, rebuilding. Reason: ${e.message}")
            }
        }

        buildApiUsingAnonymousToken()
        return api!!
    }

    /**
     * Build the API client using an anonymous token obtained from SpotDL.
     * Uses the spotifyClientApi builder that accepts an existing Token.
     */
    private suspend fun buildApiUsingAnonymousToken() {
        try {
            Log.d("SpotifyApiRequests", "Attempting to obtain anonymous token from SpotDL...")
            val anonymousToken = SpotDL.getInstance().getAnonymousToken()

            // Build a Token object (accessToken, tokenType, expiresIn)
            val token = Token(anonymousToken, "Bearer", 3600)

            Log.d("SpotifyApiRequests", "Building SpotifyClientApi with anonymous token...")

            api = spotifyClientApi(
                clientId = null,
                clientSecret = null,
                redirectUri = null,
                token = token
            ) {
                // Keep configuration minimal and compatible with the library defaults.
                // Anonymous token: no automatic refresh.
                automaticRefresh = false
                // NOTE: do not attempt to set non-existing properties (e.g. userAgent) here —
                // that caused unresolved reference errors in CI.
            }.build()

            Log.d("SpotifyApiRequests", "SpotifyClientApi built successfully.")
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "FATAL: failed to build SpotifyClientApi with anonymous token.", e)
            throw e
        }
    }

    // ---------------------
    // Convenience wrappers
    // ---------------------

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
    suspend fun provideGetAlbumById(id: String): Album? {
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
    suspend fun provideGetAudioFeatures(id: String): AudioFeatures? {
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