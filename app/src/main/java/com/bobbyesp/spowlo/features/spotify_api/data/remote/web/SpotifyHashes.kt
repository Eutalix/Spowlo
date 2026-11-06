package com.bobbyesp.spowlo.features.spotify_api.data.remote.web

import android.util.Log
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pathfinder persisted query hashes. Spotify rotates them with every web player build, so the
 * shipped map is only a floor and a small manifest in the repo is checked on top of it.
 */
internal object SpotifyHashes {

    private const val TAG = "SpotifyHashes"
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/Eutalix/Spowlo/main/docs/spotify-hashes.json"
    private const val MAX_AGE_MS = 12 * 60 * 60 * 1000L

    private const val KEY_MANIFEST = "spotify_hashes_manifest"
    private const val KEY_FETCHED_AT = "spotify_hashes_fetched_at"
    private const val KEY_ETAG = "spotify_hashes_etag"

    private val digest = Regex("[0-9a-f]{64}")

    private val bundled = mapOf(
        "searchDesktop" to "eef7cc54888d91bdd6802623477873caa3948ae173a0c34fd86827b267e94c03",
        "getTrack" to "fe16b8083eec2305854db612ca75589bcb71e009dc7a4d014f29bd534a4c01f5",
        "getAlbum" to "0b6ce3511e1b02e4a40fa95440f992acbe85080eb308af9194322d708fc1914f",
        "queryArtistOverview" to "9f8134ef565e78621f1e1793555bd6633c5ac144ae0f89604ed3ae3f80b3c8e6",
        "queryArtistDiscographyAll" to "5e07d323febb57b4a56a42abbf781490e58764aa45feb6e3dc0591564fc56599",
        "fetchPlaylist" to "21ea11a6ac75a5f3e01fd365d29c18e0a4a8b7dc09f89bbb5d73910190c6acaf",
        "profileAttributes" to "08ffb4730af3746e04a8301396f20875dbbce10c75243803091a9274eacc8ac0",
        "libraryV3" to "390c78e5b951029bad359785e69b07b536a509c581cbcd0aded5e5067f187455",
        "fetchLibraryTracks" to "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240",
        "home" to "5fb7da7a03ee2776c0670856609cb85a189a3cecc36cc160b91cde79063b9cf1",
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val primed = AtomicBoolean(false)
    private val kv by lazy { MMKV.defaultMMKV() }

    @Volatile
    private var overrides: Map<String, String> = emptyMap()

    fun hashFor(operation: String): String {
        prime()
        return overrides[operation] ?: bundled[operation]
            ?: error("Unknown Spotify operation: $operation")
    }

    /** Forgets a hash that just got rejected and pulls the manifest again. */
    suspend fun refreshStale(operation: String): String? {
        overrides = overrides - operation
        fetch(force = true)
        return overrides[operation]
    }

    private fun prime() {
        if (!primed.compareAndSet(false, true)) return
        overrides = readCache()
        if (!overrides.isEmpty()) Log.d(TAG, "Loaded ${overrides.size} hashes from cache")
        if (isStale()) scope.launch { fetch(force = false) }
    }

    private suspend fun fetch(force: Boolean) = mutex.withLock {
        if (!force && !isStale()) return@withLock
        val fresh = withContext(Dispatchers.IO) {
            runCatching { download(force) }
                .onFailure { Log.w(TAG, "Could not download the hash manifest: ${it.message}") }
                .getOrNull()
        }
        if (fresh == null) {
            // back off for half an hour instead of hammering on every launch
            kv.encode(KEY_FETCHED_AT, System.currentTimeMillis() - MAX_AGE_MS + 30 * 60 * 1000L)
            return@withLock
        }

        overrides = fresh
        if (fresh.isNotEmpty()) {
            Log.d(TAG, "Manifest applied (${fresh.size} hashes)")
        }
    }

    private fun download(force: Boolean): Map<String, String>? {
        val connection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            if (!force) {
                kv.decodeString(KEY_ETAG)?.let { setRequestProperty("If-None-Match", it) }
            }
        }

        return try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                kv.encode(KEY_FETCHED_AT, System.currentTimeMillis())
                overrides
            } else if (code == HttpURLConnection.HTTP_OK) {
                val parsed = parse(connection.inputStream.readBytes().toString(Charsets.UTF_8))
                if (parsed != null) {
                    val json = JSONObject()
                    parsed.forEach { (key, value) -> json.put(key, value) }
                    kv.encode(KEY_MANIFEST, json.toString())
                    kv.encode(KEY_FETCHED_AT, System.currentTimeMillis())
                    kv.encode(KEY_ETAG, connection.getHeaderField("ETag").orEmpty())
                }
                parsed
            } else {
                Log.w(TAG, "Hash manifest returned HTTP $code")
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readCache(): Map<String, String> {
        val stored = kv.decodeString(KEY_MANIFEST) ?: return emptyMap()
        return parse(stored).orEmpty()
    }

    private fun parse(body: String): Map<String, String>? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (root.optInt("schema") != 1) return null
        val operations = root.optJSONObject("operations") ?: return null

        val parsed = buildMap {
            operations.keys().forEach { key ->
                val value = operations.optString(key)
                if (key in bundled && digest.matches(value)) put(key, value)
            }
        }
        return parsed.takeIf { it.isNotEmpty() }
    }

    private fun isStale(): Boolean {
        val fetchedAt = kv.decodeLong(KEY_FETCHED_AT, 0L)
        return System.currentTimeMillis() - fetchedAt > MAX_AGE_MS
    }
}
