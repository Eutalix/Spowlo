package com.bobbyesp.spowlo.ui.pages.settings.updater

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.booleanState
import com.bobbyesp.spowlo.ui.common.intState
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.LargeTopAppBar
import com.bobbyesp.spowlo.ui.components.PreferenceInfo
import com.bobbyesp.spowlo.ui.components.settings.SettingsDivider
import com.bobbyesp.spowlo.ui.components.settings.SettingsGroup
import com.bobbyesp.spowlo.ui.components.settings.SettingsNewSingleChoiceItem
import com.bobbyesp.spowlo.ui.components.settings.SettingsSectionHeader
import com.bobbyesp.spowlo.ui.components.settings.SettingsSwitch
import com.bobbyesp.spowlo.ui.dialogs.UpdateDialog
import com.bobbyesp.spowlo.utils.AUTO_UPDATE
import com.bobbyesp.spowlo.utils.PRE_RELEASE
import com.bobbyesp.spowlo.utils.PreferencesUtil.updateBoolean
import com.bobbyesp.spowlo.utils.PreferencesUtil.updateInt
import com.bobbyesp.spowlo.utils.STABLE
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.UPDATE_CHANNEL
import com.bobbyesp.spowlo.utils.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterPage(onBackPressed: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )
    var autoUpdate by AUTO_UPDATE.booleanState
    var updateChannel by UPDATE_CHANNEL.intState
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var latestRelease by remember { mutableStateOf(UpdateUtil.LatestRelease()) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier
        .fillMaxSize()
        .nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        LargeTopAppBar(title = {
            Text(
                modifier = Modifier,
                text = stringResource(id = R.string.auto_update),
                fontWeight = FontWeight.Bold
            )
        }, navigationIcon = {
            BackButton {
                onBackPressed()
            }
        }, scrollBehavior = scrollBehavior
        )
    }, content = { paddings ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 32.dp,
            )
        ) {
            item {
                SettingsSectionHeader(stringResource(id = R.string.auto_update))
            }
            item {
                SettingsGroup {
                    SettingsSwitch(
                        onCheckedChange = {
                            autoUpdate = !autoUpdate
                            AUTO_UPDATE.updateBoolean(autoUpdate)
                        },
                        checked = autoUpdate,
                        title = {
                            Text(
                                text = stringResource(id = R.string.enable_auto_update),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = Icons.Outlined.Update,
                        addTonalElevation = false,
                    )
                }
            }
            item {
                SettingsSectionHeader(stringResource(id = R.string.update_channel))
            }
            item {
                SettingsGroup(modifier = Modifier.selectableGroup()) {
                    SettingsNewSingleChoiceItem(
                        text = stringResource(id = R.string.stable_channel),
                        selected = updateChannel == STABLE,
                        wantsTonalElevation = false,
                    ) {
                        updateChannel = STABLE
                        UPDATE_CHANNEL.updateInt(updateChannel)
                    }
                    SettingsDivider(indent = 16.dp)
                    SettingsNewSingleChoiceItem(
                        text = stringResource(id = R.string.pre_release_channel),
                        selected = updateChannel == PRE_RELEASE,
                        wantsTonalElevation = false,
                    ) {
                        updateChannel = PRE_RELEASE
                        UPDATE_CHANNEL.updateInt(updateChannel)
                    }
                }
            }
            item {
                PreferenceInfo(
                    modifier = Modifier.padding(vertical = 8.dp),
                    text = stringResource(id = R.string.update_channel_desc),
                    applyPaddings = false,
                )
            }
            item {
                var isLoading by remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    ProgressIndicatorButton(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(top = 6.dp)
                            .padding(bottom = 12.dp),
                        text = stringResource(
                            id = R.string.check_for_updates
                        ),
                        icon = Icons.Outlined.Update,
                        isLoading = isLoading
                    ) {
                        if (!isLoading)
                            scope.runCatching {
                                launch {
                                    isLoading = true
                                    withContext(Dispatchers.IO) {
                                        UpdateUtil.checkForUpdate()?.let {
                                            latestRelease = it
                                            showUpdateDialog = true
                                        }
                                            ?: ToastUtil.makeToastSuspend(context.getString(R.string.app_up_to_date))
                                    }
                                    isLoading = false
                                }
                            }.onFailure {
                                it.printStackTrace()
                                ToastUtil.makeToastSuspend(context.getString(R.string.app_update_failed))
                                isLoading = false
                            }
                    }
                }
            }
        }
    })
    if (showUpdateDialog)
        UpdateDialog(onDismissRequest = { showUpdateDialog = false }, latestRelease = latestRelease)
}

@Composable
fun ProgressIndicatorButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        if (isLoading)
            Box(modifier = Modifier.size(18.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center),
                    strokeWidth = 3.dp
                )
            }
        else Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}