package com.bobbyesp.spowlo.features.spotify_api.data.remote.web

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Raised when the Spotify web player backend rejects a request. */
class SpotifyWebException(
    val statusCode: Int,
    message: String,
    cause: Throwable? = null,
) : Exception("Spotify web request failed (HTTP $statusCode): $message", cause)

internal data class SpotifyWebSession(
    val accessToken: String,
    val clientToken: String,
    val clientVersion: String,
    val deviceId: String,
    val expiresAtMs: Long,
)

/**
 * Anonymous web player session: appServerConfig, TOTP, access token, client token.
 * Same flow as spotDL (SpotipyFree/SpotAPI); no account or developer app needed.
 */
internal object SpotifyWebAuth {

    private const val TAG = "SpotifyWebAuth"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

    private const val SECRETS_URL =
        "https://code.thetadev.de/ThetaDev/spotify-secrets/raw/branch/main/secrets/secretDict.json"

    /** Used when the community secret list cannot be reached (spotAPI's version 18 fallback). */
    private val FALLBACK_SECRET = intArrayOf(
        44, 55, 47, 42, 70, 40, 34, 114, 76, 74, 50, 111, 120, 97,
        75, 76, 94, 102, 43, 69, 49, 120, 118, 80, 64, 78,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    /** Plain HTTP/1.1 fallback used only for `clienttoken`. */
    private val http1Client = client.newBuilder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .build()

    private val mutex = Mutex()
    private var session: SpotifyWebSession? = null

    suspend fun session(forceRefresh: Boolean = false): SpotifyWebSession = mutex.withLock {
        val cached = session
        if (!forceRefresh && cached != null && System.currentTimeMillis() < cached.expiresAtMs - 60_000) {
            return@withLock cached
        }

        Log.d(TAG, "Refreshing anonymous web player session (force=$forceRefresh)")
        val fresh = withContext(Dispatchers.IO) { fetchSession() }
        session = fresh
        Log.d(TAG, "Session ready (clientVersion=${fresh.clientVersion})")
        fresh
    }

    fun invalidate() {
        session = null
    }

    private fun fetchSession(): SpotifyWebSession {
        val (html, deviceId) = fetchWebPlayerConfig()

        val configMatch = Regex("""<script id="appServerConfig" type="text/plain">([^<]+)</script>""")
            .find(html)
            ?: throw SpotifyWebException(0, "Could not read appServerConfig from open.spotify.com")

        val configJson = String(
            Base64.decode(configMatch.groupValues[1], Base64.DEFAULT),
            Charsets.UTF_8,
        )
        val clientVersion = JSONObject(configJson).optString("clientVersion")
        if (clientVersion.isBlank()) {
            throw SpotifyWebException(0, "Spotify did not provide a web player client version")
        }

        val (version, secret) = totpSecret()
        val code = generateTotp(secret)

        val tokenUrl = "https://open.spotify.com/api/token?" +
            "reason=init&productType=web-player&totp=$code&totpVer=$version&totpServer=$code"

        val tokenResponse = client.newCall(
            Request.Builder()
                .url(tokenUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://open.spotify.com/")
                .apply { if (deviceId.isNotBlank()) header("Cookie", "sp_t=$deviceId") }
                .build()
        ).execute()

        val tokenBody = tokenResponse.body?.string().orEmpty()
        Log.d(
            TAG,
            "token response <- code=${tokenResponse.code} protocol=${tokenResponse.protocol} " +
                "bodyLen=${tokenBody.length}",
        )
        if (!tokenResponse.isSuccessful) {
            throw SpotifyWebException(tokenResponse.code, tokenBody.take(300))
        }

        val tokenJson = JSONObject(tokenBody)
        val accessToken = tokenJson.optString("accessToken")
        val clientId = tokenJson.optString("clientId")
        Log.d(TAG, "Anonymous token acquired (anonymous=${tokenJson.optBoolean("isAnonymous")})")
        val expiresAt = tokenJson.optLong("accessTokenExpirationTimestampMs", 0L)
            .takeIf { it > 0 }
            ?: (System.currentTimeMillis() + 30 * 60 * 1000)

        if (accessToken.isBlank() || clientId.isBlank()) {
            throw SpotifyWebException(
                tokenResponse.code,
                tokenJson.optString("_notes").ifBlank { "anonymous token missing from response" }
            )
        }

        val clientToken = runCatching { fetchClientToken(clientVersion, clientId, deviceId) }
            .onFailure { Log.w(TAG, "clienttoken failed, continuing without it: ${it.message}") }
            .getOrDefault("")

        return SpotifyWebSession(
            accessToken = accessToken,
            clientToken = clientToken,
            clientVersion = clientVersion,
            deviceId = deviceId,
            expiresAtMs = expiresAt,
        )
    }

    private fun fetchWebPlayerConfig(): Pair<String, String> {
        val response = client.newCall(
            Request.Builder()
                .url("https://open.spotify.com/")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
        ).execute()

        val html = response.body?.string().orEmpty()
        if (!response.isSuccessful || html.isBlank()) {
            throw SpotifyWebException(response.code, "Could not load the Spotify web player")
        }

        val deviceId = response.headers.values("Set-Cookie")
            .asSequence()
            .mapNotNull { Regex("""(?:^|;\s*)sp_t=([^;]+)""").find(it)?.groupValues?.get(1) }
            .firstOrNull()
            .orEmpty()

        return html to deviceId
    }

    private fun fetchClientToken(clientVersion: String, clientId: String, deviceId: String): String {
        val payload = JSONObject()
            .put(
                "client_data",
                JSONObject()
                    .put("client_version", clientVersion)
                    .put("client_id", clientId)
                    .put(
                        "js_sdk_data",
                        JSONObject()
                            .put("device_brand", "unknown")
                            .put("device_model", "unknown")
                            .put("os", "windows")
                            .put("os_version", "NT 10.0")
                            .put("device_id", deviceId)
                            .put("device_type", "computer")
                    )
            )
            .toString()

        val request = Request.Builder()
            .url("https://clienttoken.spotify.com/v1/clienttoken")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Origin", "https://open.spotify.com")
            .header("Referer", "https://open.spotify.com/")
            .apply { if (deviceId.isNotBlank()) header("Cookie", "sp_t=$deviceId") }
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        // OkHttp gets an empty 400 from clienttoken's edge, so try the platform stack first.
        runCatching { fetchClientTokenViaUrlConnection(payload, deviceId) }
            .onSuccess { return it }
            .onFailure { Log.w(TAG, "clienttoken urlconnection attempt failed: ${it.message}") }

        var lastError: SpotifyWebException? = null
        for (useHttp2 in listOf(true, false)) {
            val callClient = if (useHttp2) client else http1Client
            val response = callClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            Log.d(TAG, "clienttoken <- ${response.code} ${response.protocol}")

            tokenFromBody(response.code, body)?.let { return it }
            lastError = SpotifyWebException(response.code, body.take(300))
        }

        throw lastError ?: SpotifyWebException(0, "client token request failed")
    }

    /** Returns the granted token, or null when the response carries no token. */
    private fun tokenFromBody(code: Int, body: String): String? {
        if (code !in 200..299 || body.isBlank()) return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (json.optString("response_type") != "RESPONSE_GRANTED_TOKEN_RESPONSE") return null
        return json.optJSONObject("granted_token")?.optString("token")?.takeIf { it.isNotBlank() }
    }

    /** Same request through `java.net`, whose TLS stack differs from OkHttp's. */
    private fun fetchClientTokenViaUrlConnection(payload: String, deviceId: String): String {
        val connection = (URL("https://clienttoken.spotify.com/v1/clienttoken")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            setRequestProperty("Origin", "https://open.spotify.com")
            setRequestProperty("Referer", "https://open.spotify.com/")
            if (deviceId.isNotBlank()) setRequestProperty("Cookie", "sp_t=$deviceId")
        }

        connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val body = runCatching {
            (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                .orEmpty()
        }.getOrDefault("")
        Log.d(TAG, "clienttoken via java.net <- $code")

        return tokenFromBody(code, body)
            ?: throw SpotifyWebException(code, body.take(300))
    }

    private fun totpSecret(): Pair<Int, IntArray> {
        return try {
            val response = client.newCall(
                Request.Builder().url(SECRETS_URL).header("User-Agent", USER_AGENT).build()
            ).execute()

            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isBlank()) {
                throw SpotifyWebException(response.code, "secret list unavailable")
            }

            val json = JSONObject(body)
            val version = json.keys().asSequence()
                .mapNotNull { it.toIntOrNull() }
                .maxOrNull()
                ?: 18

            val array = json.getJSONArray(version.toString())
            version to IntArray(array.length()) { array.getInt(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to the bundled TOTP secret: ${e.message}")
            18 to FALLBACK_SECRET
        }
    }

    /**
     * Spotify derives the TOTP key by XOR-scrambling each secret byte with its index
     * and concatenating the decimal representation of the result.
     */
    private fun generateTotp(secret: IntArray): String {
        val key = secret.mapIndexed { index, byte ->
            byte xor ((index % 33) + 9)
        }.joinToString("").toByteArray(Charsets.US_ASCII)

        val counter = System.currentTimeMillis() / 1000L / 30L
        val counterBytes = ByteArray(8) { i -> ((counter shr (56 - 8 * i)) and 0xFF).toByte() }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(counterBytes)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        return (binary % 1_000_000).toString().padStart(6, '0')
    }
}
