package com.bobbyesp.spowlo.ui.pages.settings.cookies

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.utils.COOKIES
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.exportYoutubeCookies
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val YOUTUBE_SIGN_IN_URL = "https://www.youtube.com/"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewPage(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webViewState = rememberWebViewState(YOUTUBE_SIGN_IN_URL)
    val navigator = rememberWebViewNavigator()

    val saveCookiesAndDismiss: () -> Unit = {
        scope.launch {
            val exported = withContext(Dispatchers.IO) {
                context.exportYoutubeCookies()
            }.getOrDefault(0)
            if (exported > 0) {
                PreferencesUtil.updateValue(COOKIES, true)
                ToastUtil.makeToast(context.getString(R.string.cookies_saved))
            } else {
                ToastUtil.makeToast(context.getString(R.string.cookies_not_found))
            }
            onDismissRequest()
        }
    }

    BackHandler(enabled = !navigator.canGoBack) {
        saveCookiesAndDismiss()
    }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(
            title = {
                Text(
                    text = webViewState.pageTitle ?: stringResource(R.string.youtube),
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = saveCookiesAndDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(id = R.string.close)
                    )
                }
            },
            actions = {
                TextButton(onClick = saveCookiesAndDismiss) {
                    Text(stringResource(R.string.done))
                }
            })
    }) { paddingValues ->
        WebView(
            state = webViewState,
            navigator = navigator,
            client = remember { AccompanistWebViewClient() },
            chromeClient = remember { AccompanistWebChromeClient() },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            captureBackPresses = true,
            factory = { webViewContext ->
                WebView(webViewContext).apply {
                    settings.javaScriptEnabled = true
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                }
            }
        )
    }
}
