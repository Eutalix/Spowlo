package com.bobbyesp.spowlo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.os.LocaleListCompat
import com.bobbyesp.spowlo.ui.common.LocalDarkTheme
import com.bobbyesp.spowlo.ui.common.LocalDynamicColorSwitch
import com.bobbyesp.spowlo.ui.common.SettingsProvider
import com.bobbyesp.spowlo.ui.pages.InitialEntry
import com.bobbyesp.spowlo.ui.pages.downloader.DownloaderViewModel
import com.bobbyesp.spowlo.ui.theme.SpowloTheme
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.matchUrlFromSharedText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * The main and only activity of the application.
 * It serves as the entry point, handles incoming intents (like shared URLs),
 * and sets up the main Composable content tree.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Retrieve the DownloaderViewModel using Hilt's `by viewModels()` delegate.
    // This instance is used to communicate intent data (shared URLs) from the Android
    // framework layer to the UI/business logic layer.
    private val downloaderViewModel: DownloaderViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set the app's language based on user preferences.
        runBlocking {
            if (Build.VERSION.SDK_INT < 33) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(PreferencesUtil.getLanguageConfiguration())
                )
            }
        }

        // Set the main Composable content for the activity.
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            // SettingsProvider wraps the entire app to provide theme and window size info.
            SettingsProvider(windowSizeClass.widthSizeClass, windowSizeClass.heightSizeClass) {
                SpowloTheme(
                    darkTheme = LocalDarkTheme.current.isDarkTheme(),
                    isHighContrastModeEnabled = LocalDarkTheme.current.isHighContrastModeEnabled,
                    isDynamicColorEnabled = LocalDynamicColorSwitch.current,
                ) {
                    // InitialEntry is the root of the navigation graph.
                    // It no longer needs ViewModels passed as parameters, as screens
                    // will retrieve their own instances using `hiltViewModel()`.
                    InitialEntry()
                }
            }
        }

        // Handle the intent that started the activity (e.g., from a share sheet or a URL click).
        handleShareIntent(intent)
    }

    /**
     * This is called when the activity is already running and receives a new intent.
     * It's crucial for handling shared URLs when the app is in the background.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    /**
     * Parses incoming intents to find Spotify URLs from ACTION_SEND (text sharing)
     * or ACTION_VIEW (opening a link). It then passes the found URL to the DownloaderViewModel.
     */
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        Log.d(TAG, "Handling intent: $intent")

        var sharedUrl: String? = null
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                // When a user clicks a spotify.com link.
                sharedUrl = intent.dataString
            }
            Intent.ACTION_SEND -> {
                // When a user shares text to the app.
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedContent ->
                    // Clear the intent extra to prevent re-processing on configuration change.
                    intent.removeExtra(Intent.EXTRA_TEXT)
                    sharedUrl = matchUrlFromSharedText(sharedContent)
                }
            }
        }

        sharedUrl?.let {
            Log.d(TAG, "Found shared URL: $it")
            // Pass the URL to the ViewModel. The `isFromShare=true` flag tells the
            // ViewModel to immediately fetch the track info. The UI will then react
            // to the state change in the ViewModel.
            downloaderViewModel.updateUrl(it, isFromShare = true)
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        fun setLanguage(locale: String) {
            Log.d(TAG, "Setting language to: $locale")
            val localeListCompat = if (locale.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(locale)
            }
            App.applicationScope.launch(Dispatchers.Main) {
                AppCompatDelegate.setApplicationLocales(localeListCompat)
            }
        }
    }
}