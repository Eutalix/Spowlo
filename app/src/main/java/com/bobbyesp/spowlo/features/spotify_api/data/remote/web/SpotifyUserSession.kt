package com.bobbyesp.spowlo.features.spotify_api.data.remote.web

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal data class SpotifyUserToken(val accessToken: String, val expiresAtMs: Long)

/**
 * Token of the web player account. Only account screens use it; everything else keeps
 * the anonymous [SpotifyWebAuth] session.
 */
internal object SpotifyUserSession {

    private const val TAG = "SpotifyUserSession"
    private const val COOKIE_URL = "https://open.spotify.com"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/141.0.0.0 Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mutex = Mutex()
    private var cached: SpotifyUserToken? = null
    private var cachedUrl: String? = null
    private var cachedUrlAtMs = 0L

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        CookieManager.getInstance().setAcceptCookie(true)
    }

    suspend fun accessToken(forceRefresh: Boolean = false): String? = mutex.withLock {
        val current = cached
        if (!forceRefresh && current != null &&
            System.currentTimeMillis() < current.expiresAtMs - 60_000
        ) {
            return@withLock current.accessToken
        }

        val fresh = fetchToken()
        cached = fresh
        fresh?.accessToken
    }

    fun invalidate() {
        cached = null
    }

    suspend fun hasAccountCookies(): Boolean =
        withContext(Dispatchers.Main.immediate) { sessionCookies() != null }

    private suspend fun fetchToken(): SpotifyUserToken? {
        val cookie = withContext(Dispatchers.Main.immediate) { sessionCookies() }
        if (cookie == null) {
            Log.d(TAG, "no Spotify account cookies on this device")
            return null
        }

        val url = tokenUrl() ?: return null

        val result = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(
                    Request.Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Referer", "https://open.spotify.com/")
                        .header("Cookie", cookie)
                        .build()
                ).execute().use { it.code to it.body?.string().orEmpty() }
            }.onFailure { Log.w(TAG, "token request failed: ${it.message}") }.getOrNull()
        } ?: return null

        val (code, body) = result
        if (code !in 200..299 || body.isBlank()) {
            Log.w(TAG, "token endpoint returned HTTP $code")
            return null
        }

        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (json.optBoolean("isAnonymous", true)) {
            Log.d(TAG, "account cookies are not signed in")
            return null
        }

        val accessToken = json.optString("accessToken")
        if (accessToken.isBlank()) {
            Log.w(TAG, "token response has no access token")
            return null
        }

        val expiresAt = json.optLong("accessTokenExpirationTimestampMs", 0L)
            .takeIf { it > 0 }
            ?: (System.currentTimeMillis() + 30 * 60 * 1000)
        return SpotifyUserToken(accessToken, expiresAt)
    }

    private fun sessionCookies(): String? {
        if (appContext == null) return null
        val cookies = CookieManager.getInstance().getCookie(COOKIE_URL) ?: return null
        return cookies.takeIf { it.contains("sp_dc=") }
    }

    /** The TOTP stays valid for 30 seconds, so polling can reuse one signed URL. */
    private suspend fun tokenUrl(): String? {
        val now = System.currentTimeMillis()
        cachedUrl?.let { if (now - cachedUrlAtMs < 10_000) return it }

        val fresh = runCatching { SpotifyWebAuth.signedTokenUrl() }.getOrElse {
            Log.w(TAG, "could not build the token URL: ${it.message}")
            return null
        }
        cachedUrl = fresh
        cachedUrlAtMs = now
        return fresh
    }
}
