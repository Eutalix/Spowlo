package com.bobbyesp.spowlo.ui.pages

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import androidx.navigation.navigation
import com.bobbyesp.library.data.remote.SpotDLDependencyUpdater
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests
import com.bobbyesp.spowlo.ui.common.LocalWindowWidthState
import com.bobbyesp.spowlo.ui.common.Route
import com.bobbyesp.spowlo.ui.common.animatedComposable
import com.bobbyesp.spowlo.ui.common.animatedComposableVariant
import com.bobbyesp.spowlo.ui.common.arg
import com.bobbyesp.spowlo.ui.common.slideInVerticallyComposable
import com.bobbyesp.spowlo.ui.dialogs.NotificationPermissionDialog
import com.bobbyesp.spowlo.ui.dialogs.bottomsheets.DownloaderBottomSheet
import com.bobbyesp.spowlo.ui.dialogs.rememberNotificationPermissionRequest
import com.bobbyesp.spowlo.ui.pages.download_tasks.DownloadTasksPage
import com.bobbyesp.spowlo.ui.pages.download_tasks.FullscreenConsoleOutput
import com.bobbyesp.spowlo.ui.pages.downloader.DownloaderPage
import com.bobbyesp.spowlo.ui.pages.downloader.DownloaderViewModel
import com.bobbyesp.spowlo.ui.pages.library.LibraryShelfPage
import com.bobbyesp.spowlo.ui.pages.library.LikedSongsPage
import com.bobbyesp.spowlo.ui.pages.library.UserLibraryPage
import com.bobbyesp.spowlo.ui.pages.library.UserLibraryViewModel
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.SpotifyItemPage
import com.bobbyesp.spowlo.ui.pages.playlist.PlaylistMetadataPage
import com.bobbyesp.spowlo.ui.pages.searcher.SearcherPage
import com.bobbyesp.spowlo.ui.pages.settings.SettingsPage
import com.bobbyesp.spowlo.ui.pages.settings.about.AboutPage
import com.bobbyesp.spowlo.ui.pages.settings.appearance.AppThemePreferencesPage
import com.bobbyesp.spowlo.ui.pages.settings.appearance.AppearancePage
import com.bobbyesp.spowlo.ui.pages.settings.appearance.LanguagePage
import com.bobbyesp.spowlo.ui.pages.settings.cookies.CookieProfilePage
import com.bobbyesp.spowlo.ui.pages.settings.cookies.CookiesSettingsViewModel
import com.bobbyesp.spowlo.ui.pages.settings.cookies.WebViewPage
import com.bobbyesp.spowlo.ui.pages.settings.directories.DownloadsDirectoriesPage
import com.bobbyesp.spowlo.ui.pages.settings.documentation.DocumentationPage
import com.bobbyesp.spowlo.ui.pages.settings.downloader.AudioQualityDialog
import com.bobbyesp.spowlo.ui.pages.settings.downloader.DownloaderSettingsPage
import com.bobbyesp.spowlo.ui.pages.settings.general.GeneralSettingsPage
import com.bobbyesp.spowlo.ui.pages.settings.updater.UpdaterPage
import com.bobbyesp.spowlo.utils.DEPENDENCIES_INITIALIZED
import com.bobbyesp.spowlo.utils.HISTORY_IMPORT_DONE
import com.bobbyesp.spowlo.utils.HistoryImporter
import com.bobbyesp.spowlo.utils.NOTIFICATION_PROMPT_SHOWN
import com.bobbyesp.spowlo.utils.NotificationsUtil
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.UrlValidator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "SpowloDebug"

private const val METADATA_ROUTE_PATTERN = Route.PLAYLIST_PAGE + "/{type}/{id}?account={account}"

private fun metadataRoute(type: String, id: String) = Route.PLAYLIST_PAGE + "/" + type + "/" + id

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialNavigationApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun InitialEntry(
    downloaderViewModel: DownloaderViewModel,
    isUrlShared: Boolean,
    openDownloadsRequest: Int = 0
) {
    //bottom sheet remember state
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)

    val isLandscape = remember { MutableTransitionState(false) }

    val windowWidthState = LocalWindowWidthState.current

    LaunchedEffect(windowWidthState) {
        isLandscape.targetState = windowWidthState == WindowWidthSizeClass.Expanded
    }

    val context = LocalContext.current

    var showDownloaderBottomSheet by remember { mutableStateOf(false) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()

    val cookiesViewModel: CookiesSettingsViewModel = viewModel()
    val onBackPressed: () -> Unit = { navController.popBackStack() }

    // Keeps every metadata page on the back stack, except when re-opening the page already shown.
    val openMetadataPage: (String, String) -> Unit = { type, id ->
        val current = navController.currentBackStackEntry
        val isCurrentPage = current?.destination?.route == METADATA_ROUTE_PATTERN &&
            current.arguments?.getString("type") == type &&
            current.arguments?.getString("id") == id
        navController.navigate(metadataRoute(type, id)) {
            launchSingleTop = isCurrentPage
        }
    }

    LaunchedEffect(openDownloadsRequest) {
        if (openDownloadsRequest > 0) {
            navController.navigate(Route.DownloadTasksNavi) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (PreferencesUtil.getValue(HISTORY_IMPORT_DONE) ||
            !HistoryImporter.hasReadableSource()
        ) {
            return@LaunchedEffect
        }
        HistoryImporter.importExistingFiles()
        PreferencesUtil.updateValue(HISTORY_IMPORT_DONE, true)
    }

    val downloaderState by Downloader.downloaderState.collectAsStateWithLifecycle()
    val processCount by Downloader.processCount.collectAsStateWithLifecycle()
    val hasActiveDownload = downloaderState is Downloader.State.DownloadingSong ||
        downloaderState is Downloader.State.DownloadingPlaylist ||
        processCount > 0

    var showNotificationPrompt by remember { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionRequest { granted ->
        if (!granted) ToastUtil.makeToast(context.getString(R.string.permission_denied))
    }

    LaunchedEffect(hasActiveDownload) {
        if (hasActiveDownload &&
            !PreferencesUtil.getValue(NOTIFICATION_PROMPT_SHOWN) &&
            !NotificationsUtil.notificationsEnabled()
        ) {
            PreferencesUtil.updateValue(NOTIFICATION_PROMPT_SHOWN, true)
            showNotificationPrompt = true
        }
    }

    // A shared or opened Spotify link goes straight to its page instead of the home screen.
    LaunchedEffect(isUrlShared) {
        if (!isUrlShared) return@LaunchedEffect
        val link = UrlValidator.parseSpotifyLink(downloaderViewModel.viewStateFlow.value.url)
        if (link != null) openMetadataPage(link.type, link.id)
        downloaderViewModel.onShareIntentConsumed()
    }
    NavHost(
        modifier = Modifier
            .fillMaxWidth(
                when (LocalWindowWidthState.current) {
                    WindowWidthSizeClass.Compact -> 1f
                    WindowWidthSizeClass.Expanded -> 1f
                    else -> 0.8f
                }
            ),
        navController = navController,
        startDestination = Route.DownloaderNavi,
        route = Route.NavGraph
    ) {
        navigation(startDestination = Route.DOWNLOADER, route = Route.DownloaderNavi) {
            animatedComposable(Route.DOWNLOADER) {
                DownloaderPage(
                    navigateToSettings = {
                        navController.navigate(Route.SETTINGS)
                    },
                    navigateToDownloaderSheet = {
                        showDownloaderBottomSheet = true
                    },
                    onOpenSpotifyItem = { type, id ->
                        openMetadataPage(type, id)
                    },
                    navigateToTasks = {
                        navController.navigate(Route.DownloadTasksNavi)
                    },
                    navigateToLibrary = {
                        navController.navigate(Route.LIBRARY)
                    },
                    navigateToSearch = { query ->
                        if (query.isBlank()) {
                            navController.navigate(Route.SearcherNavi)
                        } else {
                            navController.navigate(
                                Route.SEARCHER + "?query=" + Uri.encode(query)
                            )
                        }
                    },
                    downloaderViewModel = downloaderViewModel,
                    sheetState = sheetState
                )
            }
            animatedComposable(Route.SETTINGS) {
                SettingsPage(
                    navController = navController
                )
            }
            navigation(startDestination = Route.LIBRARY, route = Route.LibraryNavi) {
                animatedComposable(Route.LIBRARY) { entry ->
                    val libraryEntry = remember(entry) {
                        navController.getBackStackEntry(Route.LibraryNavi)
                    }
                    UserLibraryPage(
                        viewModel = viewModel<UserLibraryViewModel>(libraryEntry),
                        onBackPressed = onBackPressed,
                        onOpenPlaylist = { id ->
                            navController.navigate(
                                Route.PLAYLIST_PAGE + "/playlist/" + id + "?account=true",
                                navOptions = navOptions {
                                    launchSingleTop = true
                                    restoreState = true
                                })
                        },
                        onOpenLikedSongs = { navController.navigate(Route.LIKED_SONGS) },
                        onOpenShelf = { shelfId ->
                            navController.navigate(
                                Route.LIBRARY_SHELF + "?shelf=" + Uri.encode(shelfId)
                            )
                        },
                        onOpenTrack = { trackId -> openMetadataPage("track", trackId) },
                        onOpenAlbum = { albumId -> openMetadataPage("album", albumId) },
                        onOpenArtist = { artistId -> openMetadataPage("artist", artistId) },
                    )
                }

                animatedComposable(
                    Route.LIBRARY_SHELF + "?shelf={shelf}",
                    arguments = listOf(navArgument("shelf") { type = NavType.StringType })
                ) { entry ->
                    val libraryEntry = remember(entry) {
                        navController.getBackStackEntry(Route.LibraryNavi)
                    }
                    LibraryShelfPage(
                        shelfId = entry.arguments?.getString("shelf").orEmpty(),
                        viewModel = viewModel<UserLibraryViewModel>(libraryEntry),
                        onBackPressed = onBackPressed,
                        onOpenPlaylist = { id ->
                            navController.navigate(
                                Route.PLAYLIST_PAGE + "/playlist/" + id + "?account=true",
                                navOptions = navOptions {
                                    launchSingleTop = true
                                    restoreState = true
                                })
                        },
                        onOpenTrack = { trackId -> openMetadataPage("track", trackId) },
                        onOpenAlbum = { albumId -> openMetadataPage("album", albumId) },
                        onOpenArtist = { artistId -> openMetadataPage("artist", artistId) },
                    )
                }
            }
            animatedComposable(Route.LIKED_SONGS) {
                LikedSongsPage(
                    onBackPressed = onBackPressed,
                    onOpenTrack = { trackId -> openMetadataPage("track", trackId) },
                )
            }
            animatedComposable(Route.GENERAL_DOWNLOAD_PREFERENCES) {
                GeneralSettingsPage(
                    onBackPressed = onBackPressed
                )
            }
            animatedComposable(Route.DOWNLOAD_DIRECTORY) {
                DownloadsDirectoriesPage {
                    onBackPressed()
                }
            }
            animatedComposable(Route.APPEARANCE) {
                AppearancePage(navController = navController)
            }
            animatedComposable(Route.APP_THEME) {
                AppThemePreferencesPage {
                    onBackPressed()
                }
            }
            animatedComposable(Route.DOWNLOADER_SETTINGS) {
                DownloaderSettingsPage {
                    onBackPressed()
                }
            }
            slideInVerticallyComposable(Route.PLAYLIST_METADATA_PAGE) {
                PlaylistMetadataPage(
                    onBackPressed,
                )
            }
            animatedComposable(Route.COOKIE_PROFILE) {
                CookieProfilePage(
                    cookiesViewModel = cookiesViewModel,
                    navigateToCookieGeneratorPage = { navController.navigate(Route.COOKIE_GENERATOR_WEBVIEW) },
                ) { onBackPressed() }
            }
            animatedComposable(
                Route.COOKIE_GENERATOR_WEBVIEW
            ) {
                WebViewPage(cookiesViewModel) { onBackPressed() }
            }
            animatedComposable(Route.UPDATER_PAGE) {
                UpdaterPage(
                    onBackPressed
                )
            }
            animatedComposable(Route.DOCUMENTATION) {
                DocumentationPage(
                    onBackPressed, navController
                )
            }

            animatedComposable(Route.ABOUT) {
                AboutPage {
                    onBackPressed()
                }
            }

            animatedComposable(Route.LANGUAGES) {
                LanguagePage {
                    onBackPressed()
                }
            }

            //DIALOGS -------------------------------
            dialog(Route.AUDIO_QUALITY_DIALOG) {
                AudioQualityDialog(
                    onBackPressed
                )
            }
        }

        //Can add the downloads history bottom sheet here using `val downloadsHistoryViewModel = hiltViewModel()`
        val searcherRoute = Route.SEARCHER + "?query={query}"

        navigation(startDestination = searcherRoute, route = Route.SearcherNavi) {

            animatedComposableVariant(
                searcherRoute, arguments = listOf(navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                SearcherPage(
                    navController = navController,
                    initialQuery = backStackEntry.arguments?.getString("query").orEmpty(),
                )
            }


            //We create the arguments for the route
            val typeArg = navArgument("type") {
                type = NavType.StringType
            }

            val idArg = navArgument("id") {
                type = NavType.StringType
            }

            val accountArg = navArgument("account") {
                type = NavType.BoolType
                defaultValue = false
            }

            //We create the composable with the route and the arguments
            animatedComposableVariant(
                METADATA_ROUTE_PATTERN, arguments = listOf(typeArg, idArg, accountArg)
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: "SOMETHING WENT WRONG"
                val type =
                    backStackEntry.arguments?.getString("type") ?: "SOMETHING WENT WRONG"
                val account = backStackEntry.arguments?.getBoolean("account") ?: false

                SpotifyItemPage(
                    onBackPressed,
                    id = id,
                    type = type,
                    account = account,
                    onOpenAlbum = { albumId -> openMetadataPage("album", albumId) },
                    onOpenArtist = { artistId -> openMetadataPage("artist", artistId) },
                    onOpenTrack = { trackId -> openMetadataPage("track", trackId) },
                )
            }
        }

        navigation(
            startDestination = Route.DOWNLOAD_TASKS, route = Route.DownloadTasksNavi
        ) {

            animatedComposable(
                Route.FULLSCREEN_LOG arg "logKey",
                arguments = listOf(navArgument("logKey") {
                    type = NavType.StringType
                })
            ) { entry ->

                FullscreenConsoleOutput(
                    onBackPressed = onBackPressed,
                    logKey = entry.arguments?.getString("logKey") ?: ""
                )
            }

            animatedComposable(Route.DOWNLOAD_TASKS) {
                DownloadTasksPage(
                    onGoBack = onBackPressed,
                    onNavigateToLog = { key ->
                        navController.navigate("${Route.FULLSCREEN_LOG}/$key")
                    }
                )
            }
        }
    }

    //INIT SPOTIFY API
    LaunchedEffect(Unit) {
        runCatching {
            SpotifyApiRequests.warmUp()
        }.onFailure {
            it.printStackTrace()
            ToastUtil.makeToast(context.getString(R.string.spotify_api_error))
        }
    }

    // AUTO-UPDATE LOGIC (Unified & Safe)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            SpotDLDependencyUpdater.prunePinnedOverrides(context)

            val isInitialized = PreferencesUtil.getValue(DEPENDENCIES_INITIALIZED)
            val currentVersion = SpotDLDependencyUpdater.getLocalSpotDLVersion(context)
            val dependenciesInstalled = SpotDLDependencyUpdater.areDependenciesInstalled(context)

            Log.d(TAG, ">>> InitialEntry: isInitialized=$isInitialized, installed=$dependenciesInstalled, currentVersion=$currentVersion")

            if (!isInitialized || !dependenciesInstalled) {
                withContext(Dispatchers.Main) {
                    ToastUtil.makeToast("Initializing critical dependencies... Please wait.")
                }
                runCatching {
                    SpotDLDependencyUpdater.updateVolatileDependencies(context)
                    
                    // DOUBLE CHECK: Did it actually work?
                    val installed = SpotDLDependencyUpdater.areDependenciesInstalled(context)
                    Log.d(TAG, ">>> InitialEntry: Post-install check installed=$installed")
                    if (installed) {
                        PreferencesUtil.updateValue(DEPENDENCIES_INITIALIZED, true)
                        // Trigger ViewModel refresh if necessary
                        downloaderViewModel.refreshDependencyState()
                        withContext(Dispatchers.Main) {
                            ToastUtil.makeToast("Initialization complete!")
                        }
                    } else {
                        // Failed to verify install
                        throw Exception("Verification failed")
                    }
                }.onFailure {
                    it.printStackTrace()
                    withContext(Dispatchers.Main) {
                        ToastUtil.makeToast("Initialization failed. Please check internet and try settings.")
                    }
                }
            } else {
                // Regular Check based on Frequency
                val freq = PreferencesUtil.getUpdateFrequency()
                if (freq != PreferencesUtil.FREQ_DISABLED) {
                    val lastCheck = PreferencesUtil.getLastUpdateCheck()
                    val now = System.currentTimeMillis()
                    val daysDiff = TimeUnit.MILLISECONDS.toDays(now - lastCheck)

                    val shouldCheck = when (freq) {
                        PreferencesUtil.FREQ_DAILY -> daysDiff >= 1
                        PreferencesUtil.FREQ_WEEKLY -> daysDiff >= 7
                        PreferencesUtil.FREQ_MONTHLY -> daysDiff >= 30
                        else -> false
                    }
                    
                    Log.d(TAG, ">>> InitialEntry: Checking updates? $shouldCheck (Diff: $daysDiff days)")

                    if (shouldCheck) {
                        runCatching {
                            if (SpotDLDependencyUpdater.areUpdatesAvailable(context)) {
                                // Silent auto-update
                                SpotDLDependencyUpdater.updateVolatileDependencies(context)
                                withContext(Dispatchers.Main) {
                                    ToastUtil.makeToast("Dependencies updated in background.")
                                }
                            }
                            PreferencesUtil.setLastUpdateCheck(now)
                        }
                    }
                }
            }
        }
    }

    if (showDownloaderBottomSheet) {
        val storagePermission = rememberPermissionState(
            permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) { b: Boolean ->
            if (b) {
                downloaderViewModel.startDownloadSong()
            } else {
                ToastUtil.makeToast(R.string.permission_denied)
            }
        }

        val viewModelState = downloaderViewModel.viewStateFlow.collectAsStateWithLifecycle().value
        DownloaderBottomSheet(
            bottomSheetState = sheetState,
            onBackPressed = {
                showDownloaderBottomSheet = false
            },
            url = viewModelState.url,
            onDownloadPressed = {
                if (Build.VERSION.SDK_INT > 29 || storagePermission.status == PermissionStatus.Granted) downloaderViewModel.startDownloadSong()
                else {
                    storagePermission.launchPermissionRequest()
                }
            },
            navigateToTrack = { id -> openMetadataPage("track", id) },
            navigateToPlaylist = { id -> openMetadataPage("playlist", id) },
            navigateToAlbum = { id -> openMetadataPage("album", id) },
            navigateToArtist = { id -> openMetadataPage("artist", id) },
        )
    }

    if (showNotificationPrompt) {
        NotificationPermissionDialog(
            onDismissRequest = { showNotificationPrompt = false },
            onPermissionGranted = {
                showNotificationPrompt = false
                requestNotificationPermission()
            }
        )
    }
}
