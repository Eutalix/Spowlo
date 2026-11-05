package com.bobbyesp.spowlo.ui.pages.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.MATCH_ALL
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Aod
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.LocalDarkTheme
import com.bobbyesp.spowlo.ui.common.Route
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.LargeTopAppBar
import com.bobbyesp.spowlo.ui.components.PreferencesHintCard
import com.bobbyesp.spowlo.ui.components.settings.SettingsChevron
import com.bobbyesp.spowlo.ui.components.settings.SettingsDivider
import com.bobbyesp.spowlo.ui.components.settings.SettingsGroup
import com.bobbyesp.spowlo.ui.components.settings.SettingsItemNew
import com.bobbyesp.spowlo.ui.components.settings.SettingsSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavController) {
    val context = LocalContext.current
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var showBatteryHint by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !pm.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                false
            }
        )
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showBatteryHint = !pm.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = { BackButton { navController.popBackStack() } },
                scrollBehavior = scrollBehavior
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 32.dp,
                )
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (context.packageManager.queryIntentActivities(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS),
                            MATCH_ALL
                        ).isNotEmpty()
                    )
                        item {
                            AnimatedVisibility(
                                visible = showBatteryHint, exit = shrinkVertically() + fadeOut()
                            ) {
                                PreferencesHintCard(
                                    title = stringResource(R.string.battery_configuration),
                                    icon = Icons.Rounded.EnergySavingsLeaf,
                                    description = stringResource(R.string.battery_configuration_desc),
                                    isDarkTheme = LocalDarkTheme.current.isDarkTheme()
                                ) {
                                    launcher.launch(
                                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        })
                                    showBatteryHint =
                                        !pm.isIgnoringBatteryOptimizations(context.packageName)
                                }
                            }
                        }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.section_app))
                }
                item {
                    SettingsGroup {
                        SettingsItemNew(
                            title = {
                                Text(
                                    text = stringResource(id = R.string.general_settings),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = { Text(text = stringResource(id = R.string.general_settings_desc)) },
                            icon = Icons.Outlined.SettingsApplications,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            highlightIcon = true,
                            onClick = {
                                navController.navigate(Route.GENERAL_DOWNLOAD_PREFERENCES) {
                                    launchSingleTop = true
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsItemNew(
                            title = {
                                Text(
                                    text = stringResource(id = R.string.appearance),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = { Text(text = stringResource(id = R.string.appearance_settings)) },
                            icon = Icons.Outlined.Aod,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            highlightIcon = true,
                            onClick = {
                                navController.navigate(Route.APPEARANCE) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.downloads))
                }
                item {
                    SettingsGroup {
                        SettingsItemNew(
                            title = {
                                Text(
                                    text = stringResource(id = R.string.downloader),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = { Text(text = stringResource(id = R.string.downloader_settings_desc)) },
                            icon = Icons.Outlined.Download,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            highlightIcon = true,
                            onClick = {
                                navController.navigate(Route.DOWNLOADER_SETTINGS) {
                                    launchSingleTop = true
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsItemNew(
                            title = {
                                Text(
                                    text = stringResource(id = R.string.download_directory),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = { Text(text = stringResource(id = R.string.download_directory_desc)) },
                            icon = Icons.Outlined.Folder,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            highlightIcon = true,
                            onClick = {
                                navController.navigate(Route.DOWNLOAD_DIRECTORY) {
                                    launchSingleTop = true
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsItemNew(
                            title = {
                                Text(
                                    text = stringResource(id = R.string.cookies),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = { Text(text = stringResource(id = R.string.cookies_desc)) },
                            icon = Icons.Outlined.Cookie,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            highlightIcon = true,
                            onClick = {
                                navController.navigate(Route.COOKIE_PROFILE) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.other))
                }
                item {
                    SettingsGroup {
                        SettingsItemNew(
                            title = {
                                Text(
                                    text = stringResource(id = R.string.updates_channels),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = { Text(text = stringResource(id = R.string.updates_channels_desc)) },
                            icon = Icons.Outlined.Update,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            highlightIcon = true,
                            onClick = {
                                navController.navigate(Route.UPDATER_PAGE) {
                                    launchSingleTop = true
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsItemNew(
                            title = {
                                Text(
                                    text = stringResource(id = R.string.about),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = { Text(text = stringResource(id = R.string.about_page)) },
                            icon = Icons.Outlined.Info,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            highlightIcon = true,
                            onClick = {
                                navController.navigate(Route.ABOUT) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}
