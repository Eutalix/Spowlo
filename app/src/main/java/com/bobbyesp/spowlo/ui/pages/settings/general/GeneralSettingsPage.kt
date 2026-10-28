package com.bobbyesp.spowlo.ui.pages.settings.general

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.PrintDisabled
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.booleanState
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.LargeTopAppBar
import com.bobbyesp.spowlo.ui.components.PreferenceSubtitle
import com.bobbyesp.spowlo.ui.components.SingleChoiceItem
import com.bobbyesp.spowlo.ui.components.settings.ElevatedSettingsCard
import com.bobbyesp.spowlo.ui.components.settings.SettingsItemNew
import com.bobbyesp.spowlo.ui.components.settings.SettingsSwitch
import com.bobbyesp.spowlo.ui.dialogs.NotificationPermissionDialog
import com.bobbyesp.spowlo.ui.dialogs.rememberNotificationPermissionRequest
import com.bobbyesp.spowlo.utils.CONFIGURE
import com.bobbyesp.spowlo.utils.DEBUG
import com.bobbyesp.spowlo.utils.ENABLE_NEWS
import com.bobbyesp.spowlo.utils.INCOGNITO_MODE
import com.bobbyesp.spowlo.utils.NOTIFICATION
import com.bobbyesp.spowlo.utils.NOTIFICATION_PERMISSION_REQUESTED
import com.bobbyesp.spowlo.utils.NotificationsUtil
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import kotlinx.coroutines.DelicateCoroutinesApi

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun GeneralSettingsPage(
    onBackPressed: () -> Unit,
    viewModel: GeneralSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true })

    var displayErrorReport by DEBUG.booleanState

    var enableNews by remember {
        mutableStateOf(PreferencesUtil.getValue(ENABLE_NEWS))
    }

    var useNotifications by remember {
        mutableStateOf(
            PreferencesUtil.getValue(NOTIFICATION)
        )
    }

    var permissionRequested by remember {
        mutableStateOf(
            PreferencesUtil.getValue(NOTIFICATION_PERMISSION_REQUESTED)
        )
    }

    var notificationsAllowed by remember {
        mutableStateOf(NotificationsUtil.areNotificationsEnabled())
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = NotificationsUtil.areNotificationsEnabled()
    }

    var configureBeforeDownload by remember {
        mutableStateOf(
            PreferencesUtil.getValue(CONFIGURE)
        )
    }

    var incognitoMode by remember {
        mutableStateOf(
            PreferencesUtil.getValue(INCOGNITO_MODE)
        )
    }

    var showNotificationDialog by remember { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionRequest { granted ->
        notificationsAllowed = NotificationsUtil.areNotificationsEnabled()
        if (granted) {
            useNotifications = true
        } else {
            ToastUtil.makeToast(context.getString(R.string.permission_denied))
        }
    }

    var showFreqDialog by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier
        .fillMaxSize()
        .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = {
                Text(
                    text = stringResource(id = R.string.general), fontWeight = FontWeight.Bold
                )
            }, navigationIcon = {
                BackButton { onBackPressed() }
            }, scrollBehavior = scrollBehavior
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .padding(it)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                
                item {
                    ElevatedSettingsCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Main Update Action
                            SettingsItemNew(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (uiState.status != GeneralSettingsViewModel.UpdateStatus.Updating) {
                                        viewModel.performUpdate()
                                    }
                                    if (uiState.status == GeneralSettingsViewModel.UpdateStatus.Error || uiState.status == GeneralSettingsViewModel.UpdateStatus.Updated) {
                                        if (uiState.updateLog.isNotEmpty()) ToastUtil.makeToastSuspend(uiState.updateLog)
                                    }
                                },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(id = R.string.spotdl_version),
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (uiState.updateAvailable) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Outlined.Warning,
                                                contentDescription = "Update Available",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                icon = when(uiState.status) {
                                    GeneralSettingsViewModel.UpdateStatus.Updating -> Icons.Outlined.Sync 
                                    GeneralSettingsViewModel.UpdateStatus.Updated -> Icons.Outlined.CheckCircle
                                    else -> Icons.Outlined.Info
                                },
                                description = {
                                    when (uiState.status) {
                                        GeneralSettingsViewModel.UpdateStatus.Updating -> Text("Updating dependencies...")
                                        GeneralSettingsViewModel.UpdateStatus.Updated -> Text(uiState.currentVersion, color = MaterialTheme.colorScheme.primary)
                                        GeneralSettingsViewModel.UpdateStatus.Error -> Text("Error: Tap to see log", color = MaterialTheme.colorScheme.error)
                                        else -> {
                                            if (uiState.updateAvailable) {
                                                Text("Update available", color = MaterialTheme.colorScheme.error)
                                            } else {
                                                Text(uiState.currentVersion)
                                            }
                                        }
                                    }
                                },
                                highlightIcon = uiState.updateAvailable
                            )

                            // Settings Gear for Frequency
                            IconButton(
                                onClick = { showFreqDialog = true },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Update Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    PreferenceSubtitle(text = stringResource(id = R.string.general_settings))
                }
                
                item{
                    SettingsSwitch(
                        onCheckedChange = {
                            displayErrorReport = !displayErrorReport
                            PreferencesUtil.updateValue(DEBUG, displayErrorReport)
                        },
                        checked = displayErrorReport,
                        title = {
                            Text(
                                text = stringResource(R.string.print_details),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = if (displayErrorReport) Icons.Outlined.Print else Icons.Outlined.PrintDisabled,
                        description = { Text(text = stringResource(R.string.print_details_desc)) },
                        modifier = Modifier.clip(
                            RoundedCornerShape(
                                topStart = 8.dp, topEnd = 8.dp,
                                bottomEnd = 0.dp, bottomStart = 0.dp
                            )
                        ),
                    )
                }

                item {
                    SettingsSwitch(
                        onCheckedChange = {
                            enableNews = !enableNews
                            PreferencesUtil.updateValue(ENABLE_NEWS, enableNews)
                        },
                        checked = enableNews,
                        title = {
                            Text(
                                text = stringResource(R.string.enable_news_title),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = Icons.Outlined.Campaign,
                        description = { Text(text = stringResource(R.string.enable_news_summary)) }
                    )
                }

                item {
                    SettingsSwitch(
                        onCheckedChange = {
                            if (useNotifications && notificationsAllowed) {
                                useNotifications = false
                                PreferencesUtil.updateValue(NOTIFICATION, false)
                                NotificationsUtil.cancelAllNotifications()
                            } else if (!notificationsAllowed) {
                                if (Build.VERSION.SDK_INT >= 33 && !permissionRequested) {
                                    showNotificationDialog = true
                                } else {
                                    PreferencesUtil.updateValue(NOTIFICATION, true)
                                    NotificationsUtil.openNotificationSettings()
                                }
                            } else {
                                useNotifications = true
                                PreferencesUtil.updateValue(NOTIFICATION, true)
                            }
                        },
                        checked = useNotifications && notificationsAllowed,
                        title = {
                            Text(
                                text = stringResource(R.string.use_notifications),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = if (useNotifications && notificationsAllowed) {
                            Icons.Outlined.NotificationsActive
                        } else {
                            Icons.Outlined.NotificationsOff
                        },
                        description = {
                            Text(
                                text = stringResource(
                                    if (permissionRequested && !notificationsAllowed) {
                                        R.string.notifications_blocked_desc
                                    } else {
                                        R.string.use_notifications_desc
                                    }
                                )
                            )
                        },
                    )
                }
                item {
                    SettingsSwitch(
                        onCheckedChange = {
                            incognitoMode = !incognitoMode
                            PreferencesUtil.updateValue(INCOGNITO_MODE, incognitoMode)
                        },
                        checked = incognitoMode,
                        title = {
                            Text(
                                text = stringResource(R.string.incognito_mode),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = if (incognitoMode) Icons.Outlined.HistoryToggleOff else Icons.Outlined.History,
                        description = {
                            Text(text = stringResource(R.string.incognito_mode_desc))
                        },
                    )
                }
                item {
                    SettingsSwitch(
                        onCheckedChange = {
                            configureBeforeDownload = !configureBeforeDownload
                            PreferencesUtil.updateValue(CONFIGURE, configureBeforeDownload)
                        },
                        checked = configureBeforeDownload,
                        title = {
                            Text(
                                text = stringResource(R.string.pre_configure_download),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = Icons.Outlined.Construction,
                        description = {
                            Text(text = stringResource(R.string.pre_configure_download_desc))
                        },
                        clipCorners = false,
                        modifier = Modifier.clip(
                            RoundedCornerShape(
                                bottomStart = 8.dp, bottomEnd = 8.dp
                            )
                        ),
                    )
                }
            }
        }
    )
    
    if (showFreqDialog) {
        UpdateFrequencyDialog(
            currentFreq = viewModel.getFrequency(),
            onDismiss = { showFreqDialog = false },
            onSelect = { freq ->
                viewModel.setFrequency(freq)
                showFreqDialog = false
            }
        )
    }

    if (showNotificationDialog) {
        NotificationPermissionDialog(onDismissRequest = {
            showNotificationDialog = false
        }, onPermissionGranted = {
            showNotificationDialog = false
            permissionRequested = true
            requestNotificationPermission()
        })
    }
}

@Composable
fun UpdateFrequencyDialog(
    currentFreq: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Frequency") },
        text = {
            Column {
                SingleChoiceItem(text = "Daily (Default)", selected = currentFreq == PreferencesUtil.FREQ_DAILY) { onSelect(PreferencesUtil.FREQ_DAILY) }
                SingleChoiceItem(text = "Weekly", selected = currentFreq == PreferencesUtil.FREQ_WEEKLY) { onSelect(PreferencesUtil.FREQ_WEEKLY) }
                SingleChoiceItem(text = "Monthly", selected = currentFreq == PreferencesUtil.FREQ_MONTHLY) { onSelect(PreferencesUtil.FREQ_MONTHLY) }
                SingleChoiceItem(text = "Disabled", selected = currentFreq == PreferencesUtil.FREQ_DISABLED) { onSelect(PreferencesUtil.FREQ_DISABLED) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}