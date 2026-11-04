package com.bobbyesp.spowlo.ui.pages.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyUserApi
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyUserSession
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.UserPlaylist
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "UserLibraryViewModel"
private const val SIGN_IN_ATTEMPTS = 8
private const val SIGN_IN_RETRY_DELAY_MS = 800L

sealed interface UserLibraryState {
    data object Loading : UserLibraryState
    data object LoggedOut : UserLibraryState
    data object Error : UserLibraryState

    data class Ready(
        val profile: UserProfile,
        val likedCount: Int,
        val mixes: List<UserPlaylist>,
        val playlists: List<UserPlaylist>,
    ) : UserLibraryState
}

class UserLibraryViewModel : ViewModel() {

    private val mutableState = MutableStateFlow<UserLibraryState>(UserLibraryState.Loading)
    val state = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var signInJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        signInJob?.cancel()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = UserLibraryState.Loading
            val token = runCatching { SpotifyUserSession.accessToken() }.getOrNull()
            if (token == null) {
                mutableState.value = if (SpotifyUserSession.hasAccountCookies()) {
                    UserLibraryState.Error
                } else {
                    UserLibraryState.LoggedOut
                }
                return@launch
            }
            mutableState.value = loadLibrary()
        }
    }

    /** The web player writes its cookies just after the redirect, so poll briefly. */
    fun finishSignIn() {
        if (signInJob?.isActive == true) return
        signInJob = viewModelScope.launch {
            repeat(SIGN_IN_ATTEMPTS) {
                val token = runCatching {
                    SpotifyUserSession.accessToken(forceRefresh = true)
                }.getOrNull()
                if (token != null) {
                    loadJob?.cancel()
                    mutableState.value = loadLibrary()
                    return@launch
                }
                delay(SIGN_IN_RETRY_DELAY_MS)
            }
        }
    }

    fun finishLoginScreen() {
        signInJob?.cancel()
        refresh()
    }

    private suspend fun loadLibrary(): UserLibraryState = runCatching {
        val profile = SpotifyUserApi.profile()
        val playlists = SpotifyUserApi.playlists()
        val likedCount = SpotifyUserApi.likedCount()
        UserLibraryState.Ready(
            profile = profile,
            likedCount = likedCount,
            mixes = playlists.filter { it.isSpotifyOwned },
            playlists = playlists.filterNot { it.isSpotifyOwned },
        )
    }.getOrElse { error ->
        Log.w(TAG, "Could not load the account library: ${error.message}")
        val stillSignedIn = runCatching {
            SpotifyUserSession.accessToken(forceRefresh = true)
        }.getOrNull() != null || runCatching {
            SpotifyUserSession.hasAccountCookies()
        }.getOrDefault(false)
        if (stillSignedIn) UserLibraryState.Error else UserLibraryState.LoggedOut
    }
}
