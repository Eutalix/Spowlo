package com.bobbyesp.spowlo.features.spotify_api.data.remote.web

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/** Minimal response envelope used by the transport. */
internal data class WebResponse(val code: Int, val body: String)

/**
 * Runs Spotify web requests from a hidden WebView on open.spotify.com because
 * Spotify rejects Android's TLS fingerprints; java.net is the fallback.
 */
internal object SpotifyWebTransport {

    private const val TAG = "SpotifyWebTransport"
    private const val PAGE = "https://open.spotify.com/"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/141.0.0.0 Safari/537.36"
    private const val BRIDGE = "SpowloBridge"

    private val appContext = AtomicReference<Context?>(null)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<WebResponse>>()
    private val webViewLock = Any()
    private var webView: WebView? = null
    private var pageReady = CompletableDeferred<Unit>()
    @Volatile
    private var webViewUnavailable = false

    fun init(context: Context) {
        appContext.compareAndSet(null, context.applicationContext)
    }

    suspend fun fetch(
        url: String,
        headers: Map<String, String>,
    ): WebResponse {
        if (!webViewUnavailable) {
            try {
                return fetchWithWebView(url, headers)
            } catch (cancelled: CancellationException) {
                // A superseded search is not a broken WebView.
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "WebView transport failed, falling back to java.net: ${error.message}")
                webViewUnavailable = true
            }
        }
        return withContext(Dispatchers.IO) { fetchWithUrlConnection(url, headers) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun fetchWithWebView(url: String, headers: Map<String, String>): WebResponse =
        withContext(Dispatchers.Main.immediate) {
            // Share one WebView (and its readiness signal) across concurrent first requests.
            val (view, ready) = synchronized(webViewLock) {
                (webView ?: createWebView() ?: throw IllegalStateException("no WebView")) to pageReady
            }
            if (!ready.isCompleted) {
                withTimeout(20_000) { ready.await() }
            }

            val id = UUID.randomUUID().toString()
            val result = CompletableDeferred<WebResponse>()
            pending[id] = result

            val headerJson = JSONObject().apply { headers.forEach { (k, v) -> put(k, v) } }
            val script = "(function(){" +
                "var id=${JSONObject.quote(id)};" +
                "fetch(${JSONObject.quote(url)},{method:'GET',headers:$headerJson,credentials:'omit'})" +
                ".then(function(r){return r.text().then(function(t){$BRIDGE.onResult(id,r.status,t);});})" +
                ".catch(function(e){$BRIDGE.onError(id,String(e));});" +
                "})()"

            view.evaluateJavascript(script, null)

            try {
                withTimeout(30_000) { result.await() }
            } finally {
                pending.remove(id)
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView? {
        val context = appContext.get() ?: return null
        pageReady = CompletableDeferred()
        val view = WebView(context)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.userAgentString = USER_AGENT
        view.addJavascriptInterface(Bridge(), BRIDGE)
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "WebView page ready: $url")
                pageReady.complete(Unit)
            }
        }
        Log.d(TAG, "Creating hidden WebView")
        // The base URL gives fetch() Spotify's origin without loading the whole SPA.
        view.loadDataWithBaseURL(
            PAGE,
            "<html><body>Spowlo</body></html>",
            "text/html",
            "UTF-8",
            null,
        )
        webView = view
        return view
    }

    private class Bridge {
        @JavascriptInterface
        fun onResult(id: String, status: Int, body: String) {
            pending.remove(id)?.complete(WebResponse(status, body))
        }

        @JavascriptInterface
        fun onError(id: String, message: String) {
            pending.remove(id)?.completeExceptionally(IllegalStateException(message))
        }
    }

    private fun fetchWithUrlConnection(
        url: String,
        headers: Map<String, String>,
    ): WebResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }

            val code = connection.responseCode
            val body = runCatching {
                (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    .orEmpty()
            }.getOrDefault("")
            WebResponse(code, body)
        } finally {
            connection.disconnect()
        }
    }
}
