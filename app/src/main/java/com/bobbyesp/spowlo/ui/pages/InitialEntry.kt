package com.bobbyesp.spowlo.ui.pages

import android.Manifest
import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.bobbyesp.spowlo.ui.common.Route
import com.bobbyesp.spowlo.ui.common.animatedComposable
import com.bobbyesp.spowlo.ui.pages.download_tasks.DownloadTasksPage
import com.bobbyesp.spowlo.ui.pages.downloader.DownloaderPage
import com.bobbyesp.spowlo.ui.pages.history.DownloadsHistoryPage
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.PlaylistPageViewModel
import com.bobbyesp.spowlo.ui.pages.metadata_viewer.playlists.SpotifyItemPage
import com.bobbyesp.spowlo.ui.pages.searcher.SearcherPage
import com.bobbyesp.spowlo.ui.pages.settings.SettingsPage
import com.bobbyesp.spowlo.ui.pages.settings.about.AboutPage
import com.bobbyesp.spowlo.ui.pages.settings.appearance.AppearancePage
import com.bobbyesp.spowlo.ui.pages.settings.downloader.DownloaderSettingsPage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * The main entry point for the app's navigation graph.
 * It sets up the NavHost and defines all the routes and their corresponding Composables.
 */
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class
)
@Composable
fun InitialEntry() {
    val navController = rememberNavController()

    // Request notification permissions on Android 13+ for background tasks.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted && !notificationPermissionState.status.shouldShowRationale) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    // NavHost is the container for all navigation destinations.
    NavHost(
        navController = navController,
        startDestination = Route.DownloaderNavi, // The main screen is the downloader.
        route = Route.NavGraph
    ) {
        // Main navigation graph for the downloader/home section.
        navigation(startDestination = Route.DOWNLOADER, route = Route.DownloaderNavi) {
            composable(Route.DOWNLOADER) {
                // The DownloaderPage now retrieves its own ViewModel instance via Hilt.
                DownloaderPage(
                    navigateToDownloads = { navController.navigate(Route.DOWNLOADS_HISTORY) },
                    navigateToSettings = { navController.navigate(Route.SETTINGS) },
                    navigateToTasks = { navController.navigate(Route.DownloadTasksNavi) },
                    navigateToSearch = { navController.navigate(Route.SearcherNavi) }
                )
            }
            animatedComposable(Route.SETTINGS) {
                SettingsPage(navController = navController)
            }
            animatedComposable(Route.DOWNLOADS_HISTORY) {
                DownloadsHistoryPage(onBackPressed = { navController.popBackStack() })
            }
            animatedComposable(Route.DOWNLOADER_SETTINGS) {
                DownloaderSettingsPage(onBackPressed = { navController.popBackStack() })
            }
            animatedComposable(Route.APPEARANCE) {
                AppearancePage(navController = navController)
            }
            animatedComposable(Route.ABOUT) {
                AboutPage(onBackPressed = { navController.popBackStack() })
            }
            // Add other simple routes from this navigation graph here...
        }

        // Navigation graph for the search feature.
        navigation(startDestination = Route.SEARCHER, route = Route.SearcherNavi) {
            animatedComposable(Route.SEARCHER) {
                SearcherPage(navController = navController)
            }

            // Route for displaying details of a Spotify item (Track, Album, Playlist, Artist).
            val routeWithIdPattern = "${Route.PLAYLIST_PAGE}/{type}/{id}"
            animatedComposable(
                routeWithIdPattern,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val type = backStackEntry.arguments?.getString("type")
                if (!id.isNullOrEmpty() && !type.isNullOrEmpty()) {
                    // The SpotifyItemPage gets its ViewModel from Hilt.
                    val playlistPageViewModel: PlaylistPageViewModel = hiltViewModel()
                    SpotifyItemPage(
                        onBackPressed = { navController.popBackStack() },
                        playlistPageViewModel = playlistPageViewModel,
                        id = id,
                        type = type
                    )
                }
            }
        }

        // Navigation graph for the download tasks/logs screen.
        navigation(startDestination = Route.DOWNLOAD_TASKS, route = Route.DownloadTasksNavi) {
            animatedComposable(Route.DOWNLOAD_TASKS) {
                // The DownloadTasksPage gets its own ViewModel via Hilt.
                DownloadTasksPage(
                    onGoBack = { navController.popBackStack() },
                    onNavigateToDetail = { taskId ->
                        // Your navigation logic to the fullscreen log screen goes here.
                        // e.g., navController.navigate("${Route.FULLSCREEN_LOG}/$taskId")
                    }
                )
            }
            // Define the route for the fullscreen log page here if needed.
        }
    }
}