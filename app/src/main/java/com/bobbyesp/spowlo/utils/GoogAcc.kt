package com.bobbyesp.spowlo.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "GoogAcc"
private const val ACCOUNT_URL = "https://myaccount.google.com/"
private const val BRIDGE = "SpowloGoogleAccount"

/** Reads the signed-in Google address in an offscreen WebView. */
object GoogAcc {

    private val SCAN_SCRIPT = """
        (function () {
          var ticks = 0;
          function report(email) {
            try { $BRIDGE.result(email); } catch (e) {}
          }
          function scan() {
            var text = document.body ? document.body.innerText : '';
            var match = text ? text.match(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/) : null;
            if (match) { report(match[0]); return; }
            if (++ticks > 12) {
              try { $BRIDGE.debug(document.title + ' | ' + location.href + ' | ' + text.slice(0, 160)); } catch (e) {}
              report('');
              return;
            }
            setTimeout(scan, 700);
          }
          scan();
        })();
    """

    suspend fun detectEmail(context: Context): String? = withContext(Dispatchers.Main.immediate) {
        val result = CompletableDeferred<String>()
        val view = createWebView(context, result)
        Log.d(TAG, "Loading $ACCOUNT_URL")
        try {
            view.loadUrl(ACCOUNT_URL)
            withTimeoutOrNull(20_000) { result.await() }?.takeIf { it.isNotBlank() }
        } finally {
            view.destroy()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(
        context: Context,
        result: CompletableDeferred<String>,
    ): WebView = WebView(context.applicationContext).apply {
        settings.javaScriptEnabled = true
        addJavascriptInterface(Bridge(result), BRIDGE)
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "Page finished: $url")
                view?.evaluateJavascript(SCAN_SCRIPT, null)
            }
        }
    }

    private class Bridge(private val result: CompletableDeferred<String>) {

        @JavascriptInterface
        fun result(email: String) {
            Log.d(TAG, if (email.isBlank()) "No address found" else "Address found: $email")
            result.complete(email)
        }

        @JavascriptInterface
        fun debug(value: String) {
            Log.d(TAG, "Page state: $value")
        }
    }
}
