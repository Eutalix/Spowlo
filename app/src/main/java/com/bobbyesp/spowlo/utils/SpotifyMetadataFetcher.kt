package com.bobbyesp.spowlo.utils

import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.spotifyAppApi
import com.bobbyesp.spowlo.BuildConfig
import com.bobbyesp.spowlo.utils.PreferencesUtil.getString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SpotifyMetadataFetcher {
    private var api: SpotifyAppApi? = null
    private val apiMutex = Mutex()

    private suspend fun getApi(): SpotifyAppApi {
        if (api == null) {
            apiMutex.withLock {
                if (api == null) {
                    val clientId = if (PreferencesUtil.getValue(USE_SPOTIFY_CREDENTIALS) && SPOTIFY_CLIENT_ID.getString().isNotEmpty()) {
                        SPOTIFY_CLIENT_ID.getString()
                    } else {
                        BuildConfig.CLIENT_ID
                    }

                    val clientSecret = if (PreferencesUtil.getValue(USE_SPOTIFY_CREDENTIALS) && SPOTIFY_CLIENT_SECRET.getString().isNotEmpty()) {
                        SPOTIFY_CLIENT_SECRET.getString()
                    } else {
                        BuildConfig.CLIENT_SECRET
                    }

                    if (clientId.isBlank() || clientSecret.isBlank() || clientId == "null" || clientSecret == "null") {
                         throw IllegalStateException("Spotify Credentials not configured properly.")
                    }

                    api = spotifyAppApi(clientId, clientSecret).build()
                }
            }
        }
        return api!!
    }

    suspend fun fetchTrackFromUrl(spotifyUrl: String): Track? {
        return try {
            val normalizedUrl = UrlValidator.normalize(spotifyUrl)
            
            val trackId = if (normalizedUrl.contains("/track/")) {
                normalizedUrl.substringAfter("/track/").substringBefore("?")
            } else {
                return null
            }

            if (trackId.isBlank()) return null

            getApi().tracks.getTrack(trackId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}