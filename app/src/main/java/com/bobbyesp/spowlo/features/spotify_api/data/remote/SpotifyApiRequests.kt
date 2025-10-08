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
import com.bobbyesp.spowlo.utils.PreferencesUtil // IMPORTANT: Ensure this is your actual preferences utility class
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SpotifyApiRequests {

    private var api: SpotifyAppApi? = null

    @Provides
    @Singleton
    suspend fun provideSpotifyApi(): SpotifyAppApi {
        // If the API is already built and the token is not expired, reuse it.
        if (api?.token?.isExpired == false) {
            return api!!
        }
        // Otherwise, build a new API instance.
        buildApi()
        return api!!
    }

    /**
     * REFACTOR: Implements a layered authentication strategy for robustness.
     * It prioritizes user-provided credentials from settings and falls back to
     * spotdl's anonymous token generation if they are unavailable or fail.
     */
    suspend fun buildApi() {
        // Layer 1: Attempt to use user-provided credentials from settings.
        val userClientId = PreferencesUtil.getString("spotify_client_id", null) // TODO: Adapt preference key if needed
        val userClientSecret = PreferencesUtil.getString("spotify_client_secret", null) // TODO: Adapt preference key if needed

        if (!userClientId.isNullOrBlank() && !userClientSecret.isNullOrBlank()) {
            Log.d("SpotifyApiRequests", "Attempting to build API with user-provided credentials.")
            try {
                val apiBuilder = spotifyAppApi(userClientId, userClientSecret) {
                    api {
                        ktor {
                            install("DefaultRequest") {
                                headers.append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                            }
                        }
                    }
                }.build()
                
                api = apiBuilder
                Log.d("SpotifyApiRequests", "API built successfully with user credentials.")
                return // Success! Exit the function.
            } catch (e: Exception) {
                Log.w("SpotifyApiRequests", "Failed to build API with user credentials. Falling back to anonymous token.", e)
                // If this fails, don't crash. Proceed to Layer 2.
            }
        }

        // Layer 2: Fallback to using spotdl's anonymous token generation.
        Log.d("SpotifyApiRequests", "No user credentials found. Building API using anonymous token from spotdl...")
        try {
            val anonymousToken = SpotDL.getInstance().getAnonymousToken()

            api = spotifyAppApi {
                credentials {
                    accessToken = anonymousToken
                }
                api {
                    ktor {
                        install("DefaultRequest") {
                            headers.append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                        }
                    }
                }
            }.build()

            Log.d("SpotifyApiRequests", "API built successfully with anonymous token.")
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "FATAL: Failed to build API with both user credentials and anonymous token.", e)
            throw e // If even the anonymous method fails, there's a deeper issue.
        }
    }

    //Performs Spotify database query for queries related to user information.
    private suspend fun userSearch(userQuery: String): SpotifyPublicUser? {
        return provideSpotifyApi().users.getProfile(userQuery)
    }

    @Provides
    @Singleton
    suspend fun provideUserSearch(query: String): SpotifyPublicUser? {
        return userSearch("bobbyesp")
    }

    // Performs Spotify database query for queries related to track information.
    suspend fun searchAllTypes(searchQuery: String): SpotifySearchResult {
        kotlin.runCatching {
            provideSpotifyApi().search.searchAllTypes(
                searchQuery,
                limit = 50,
                offset = 0,
                market = Market.US
            )
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error: ${it.message}")
            return SpotifySearchResult()
        }.onSuccess {
            return it
        }
        return SpotifySearchResult()
    }

    @Provides
    @Singleton
    suspend fun provideSearchAllTypes(query: String): SpotifySearchResult {
        return searchAllTypes(query)
    }

    private suspend fun searchTracks(searchQuery: String): List<Track> {
        kotlin.runCatching {
            provideSpotifyApi().search.searchTrack(searchQuery, limit = 50)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error: ${it.message}")
            return listOf()
        }.onSuccess {
            return it.items
        }
        return listOf()
    }

    @Provides
    @Singleton
    suspend fun provideSearchTracks(query: String): List<Track> {
        return searchTracks(query)
    }

    //search by id
    suspend fun getPlaylistById(id: String): Playlist? {
        kotlin.runCatching {
            provideSpotifyApi().playlists.getPlaylist(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error: ${it.message}")
            return null
        }.onSuccess {
            Log.d("SpotifyApiRequests", "Playlist: $it")
            return it
        }
        return null
    }

    @Provides
    @Singleton
    suspend fun provideGetPlaylistById(id: String): Playlist? {
        return getPlaylistById(id)
    }

    suspend fun getTrackById(id: String): Track? {
        kotlin.runCatching {
            provideSpotifyApi().tracks.getTrack(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error: ${it.message}")
            return null
        }.onSuccess {
            return it
        }
        return null
    }

    @Provides
    @Singleton
    suspend fun provideGetTrackById(id: String): Track? {
        return getTrackById(id)
    }

    private suspend fun getArtistById(id: String): Artist? {
        kotlin.runCatching {
            api!!.artists.getArtist(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error: ${it.message}")
            return null
        }.onSuccess {
            return it
        }
        return null
    }

    @Provides
    @Singleton
    suspend fun provideGetArtistById(id: String): Artist? {
        return getArtistById(id)
    }

    suspend fun getAlbumById(id: String): Album? {
        kotlin.runCatching {
            provideSpotifyApi().albums.getAlbum(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error: ${it.message}")
            return null
        }.onSuccess {
            return it
        }
        return null
    }

    @Provides
    @Singleton
    suspend fun providesGetAlbumById(id: String): Album? {
        return getAlbumById(id)
    }

    private suspend fun getAudioFeatures(id: String): AudioFeatures? {
        kotlin.runCatching {
            provideSpotifyApi().tracks.getAudioFeatures(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Error: ${it.message}")
        }.onSuccess {
            return it
        }
        return null
    }

    @Provides
    @Singleton
    suspend fun providesGetAudioFeatures(id: String): AudioFeatures? {
        return getAudioFeatures(id)
    }

    private suspend fun getArtistTopTracks(artistId: String): List<Track>? {
        val artist = provideGetArtistById(artistId)
        return artist?.let {
            kotlin.runCatching {
                provideSpotifyApi().artists.getArtistTopTracks(artistId, Market.US)
            }.onFailure {
                Log.d("SpotifyApiRequests", "Error: ${it.message}")
                null
            }.getOrNull()
        }
    }

    @Provides
    @Singleton
    suspend fun providesGetArtistTopTracks(id: String): List<Track>? {
        return getArtistTopTracks(id)
    }

    private suspend fun getArtistAlbums(artistId: String): PagingObject<SimpleAlbum>? {
        val artist = provideGetArtistById(artistId)
        return artist?.let {
            kotlin.runCatching {
                provideSpotifyApi().artists.getArtistAlbums(artist=artistId, market=Market.US, limit=20)
            }.onFailure {
                Log.d("SpotifyApiRequests", "Error: ${it.message}")
            }.getOrNull()
        }
    }

    @Provides
    @Singleton
    suspend fun providesGetArtistAlbums(id: String): PagingObject<SimpleAlbum>? {
        return getArtistAlbums(id)
    }
}