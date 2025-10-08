package com.bobbyesp.spowlo.features.spotify_api.data.remote

import android.util.Log
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.AudioFeatures
import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SpotifyPublicUser
import com.adamratzman.spotify.models.SpotifySearchResult
import com.adamratzman.spotify.models.Token
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.spotifyAppApi
import com.adamratzman.spotify.utils.Market
import com.bobbyesp.library.SpotDL
import com.bobbyesp.spowlo.utils.PreferencesUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.engine.okhttp.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpotifyApiRequests {

    private var api: SpotifyAppApi? = null

    @Provides
    @Singleton
    suspend fun provideSpotifyApi(): SpotifyAppApi {
        // A simple check if the token is still valid.
        // If the API fails later, it will be rebuilt on the next call.
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
        buildApi()
        return api!!
    }

    /**
     * FINAL REFACTOR: Implements the layered authentication strategy using the correct
     * builder syntax for the OkHttp engine used by the spotify-web-api-kotlin library.
     */
    suspend fun buildApi() {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"

        // Layer 1: Attempt to use user-provided credentials from settings.
        val userClientId = PreferencesUtil.getString("spotify_client_id", null)
        val userClientSecret = PreferencesUtil.getString("spotify_client_secret", null)

        if (!userClientId.isNullOrBlank() && !userClientSecret.isNullOrBlank()) {
            Log.d("SpotifyApiRequests", "Attempting to build API with user-provided credentials.")
            try {
                api = spotifyAppApi(userClientId, userClientSecret) {
                    // Correct syntax to configure the underlying Ktor OkHttp client.
                    options.ktorEngine = OkHttp.create {
                        addInterceptor { chain ->
                            val original = chain.request()
                            val requestBuilder = original.newBuilder()
                                .header("User-Agent", userAgent)
                            val request = requestBuilder.build()
                            chain.proceed(request)
                        }
                    }
                    options.automaticRefresh = true
                }.build()

                // Perform an initial token refresh to validate credentials.
                api?.refreshToken()
                
                Log.d("SpotifyApiRequests", "API built successfully with user credentials.")
                return // Success!
            } catch (e: Exception) {
                Log.w("SpotifyApiRequests", "Failed to build API with user credentials. Falling back to anonymous token.", e)
            }
        }

        // Layer 2: Fallback to using spotdl's anonymous token generation.
        Log.d("SpotifyApiRequests", "Building API using anonymous token from spotdl...")
        try {
            val anonymousToken = SpotDL.getInstance().getAnonymousToken()

            // The builder for a client-side API (using an existing token) is different.
            api = spotifyAppApi {
                credentials {
                    accessToken = anonymousToken
                }
                options.ktorEngine = OkHttp.create {
                    addInterceptor { chain ->
                        val original = chain.request()
                        val requestBuilder = original.newBuilder()
                            .header("User-Agent", userAgent)
                        val request = requestBuilder.build()
                        chain.proceed(request)
                    }
                }
            }.build()

            Log.d("SpotifyApiRequests", "API built successfully with anonymous token.")
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "FATAL: Failed to build API with both user credentials and anonymous token.", e)
            throw e // If even the anonymous method fails, then there's a deeper issue.
        }
    }
    
    // --- The rest of the file remains unchanged. It will use the `api` instance built above. ---

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