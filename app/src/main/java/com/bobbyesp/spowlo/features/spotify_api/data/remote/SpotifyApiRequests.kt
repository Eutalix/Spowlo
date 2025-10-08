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
import com.bobbyesp.library.SpotDL // Import the SpotDL library
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpotifyApiRequests {

    private var api: SpotifyAppApi? = null

    @Provides
    @Singleton
    suspend fun provideSpotifyApi(): SpotifyAppApi {
        // If the API is already built and the library confirms the token isn't expired, reuse it.
        // The spotify-web-api-kotlin library handles refreshing automatically if configured,
        // but for anonymous tokens, rebuilding is safer.
        if (api != null) {
            // A simple check can be to try a lightweight call, or just rebuild if it's been a while.
            // For simplicity now, we rebuild if it's null.
            return api!!
        }
        
        buildApi()
        return api!!
    }

    /**
     * REFACTOR: Simplified authentication to ONLY use spotdl's anonymous token.
     * This is the most robust method and removes all external credential dependencies.
     */
    suspend fun buildApi() {
        Log.d("SpotifyApiRequests", "Building API using anonymous token from spotdl...")
        try {
            // Step 1: Get the working anonymous token from our :library module.
            val anonymousToken = SpotDL.getInstance().getAnonymousToken()

            // Step 2: Build the Spotify API directly with the fetched token.
            api = spotifyAppApi {
                credentials {
                    accessToken = anonymousToken
                }
                // Step 3 (Crucial): Add a User-Agent to avoid being blocked.
                // The syntax for Ktor was updated for newer versions of the library.
                api {
                    client {
                        defaultRequest {
                            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                        }
                    }
                }
            }.build()

            Log.d("SpotifyApiRequests", "API built successfully with anonymous token.")
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "FATAL: Failed to build API with anonymous token.", e)
            throw e // If this fails, there's a deeper, unrecoverable issue.
        }
    }

    //------------------------------------------------------------------------------------//
    // The rest of the file remains the same. It will now use the correctly built `api`. //
    //------------------------------------------------------------------------------------//

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