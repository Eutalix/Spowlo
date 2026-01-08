package com.bobbyesp.spowlo.utils

import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.spotifyAppApi
import com.bobbyesp.spowlo.BuildConfig
import com.bobbyesp.spowlo.utils.PreferencesUtil.getString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton object responsible for interacting with the Spotify API.
 * It handles authentication and metadata fetching for tracks.
 */
object SpotifyMetadataFetcher {
    private var api: SpotifyAppApi? = null
    private val apiMutex = Mutex()

    /**
     * Resets the current Spotify API instance.
     *
     * This method must be called whenever the user updates their Spotify credentials
     * (Client ID or Client Secret) in the settings. It clears the cached [api] instance,
     * forcing a re-authentication with the new credentials on the next request.
     */
    suspend fun resetApi() {
        apiMutex.withLock {
            api = null
        }
    }

    /**
     * Retrieves the Spotify API instance, initializing it if necessary.
     *
     * This method uses a double-check locking pattern with a Mutex to ensure thread safety.
     * It prioritizes user-provided credentials from preferences over the default BuildConfig credentials.
     *
     * @return [SpotifyAppApi] An authenticated Spotify API client.
     * @throws IllegalStateException If credentials are missing or invalid.
     */
    private suspend fun getApi(): SpotifyAppApi {
        if (api == null) {
            apiMutex.withLock {
                if (api == null) {
                    val useCustom = PreferencesUtil.getValue(USE_SPOTIFY_CREDENTIALS)
                    
                    // Retrieve and sanitize user credentials if enabled
                    val clientId = if (useCustom && SPOTIFY_CLIENT_ID.getString().isNotBlank()) {
                        SPOTIFY_CLIENT_ID.getString().trim()
                    } else {
                        BuildConfig.CLIENT_ID
                    }

                    val clientSecret = if (useCustom && SPOTIFY_CLIENT_SECRET.getString().isNotBlank()) {
                        SPOTIFY_CLIENT_SECRET.getString().trim()
                    } else {
                        BuildConfig.CLIENT_SECRET
                    }

                    // Validate credentials integrity
                    if (clientId.isBlank() || clientSecret.isBlank() || clientId == "null" || clientSecret == "null") {
                         throw IllegalStateException("Spotify Credentials not configured properly. Please check your settings.")
                    }

                    // Build and authenticate the API client
                    api = spotifyAppApi(clientId, clientSecret).build()
                }
            }
        }
        return api!!
    }

    /**
     * Fetches track metadata from a given Spotify URL.
     *
     * @param spotifyUrl The full URL of the Spotify track.
     * @return [Track] object containing metadata, or null if the fetch fails or URL is invalid.
     */
    suspend fun fetchTrackFromUrl(spotifyUrl: String): Track? {
        return try {
            val normalizedUrl = UrlValidator.normalize(spotifyUrl)
            
            // Extract Track ID from URL
            val trackId = if (normalizedUrl.contains("/track/")) {
                normalizedUrl.substringAfter("/track/").substringBefore("?")
            } else {
                return null
            }

            if (trackId.isBlank()) return null

            // Attempt to fetch track details
            getApi().tracks.getTrack(trackId)
        } catch (e: Exception) {
            // Log the exception for debugging purposes
            e.printStackTrace()
            null
        }
    }
}