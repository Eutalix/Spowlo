package com.bobbyesp.spowlo.ui.pages.settings.appearance

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.LocalDarkTheme
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.LargeTopAppBar
import com.bobbyesp.spowlo.ui.components.settings.SettingsDivider
import com.bobbyesp.spowlo.ui.components.settings.SettingsGroup
import com.bobbyesp.spowlo.ui.components.settings.SettingsNewSingleChoiceItem
import com.bobbyesp.spowlo.ui.components.settings.SettingsSectionHeader
import com.bobbyesp.spowlo.ui.components.settings.SettingsSwitch
import com.bobbyesp.spowlo.utils.DarkThemePreference.Companion.FOLLOW_SYSTEM
import com.bobbyesp.spowlo.utils.DarkThemePreference.Companion.OFF
import com.bobbyesp.spowlo.utils.DarkThemePreference.Companion.ON
import com.bobbyesp.spowlo.utils.PreferencesUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemePreferencesPage(onBackPressed: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )
    val darkThemePreference = LocalDarkTheme.current
    val isHighContrastModeEnabled = darkThemePreference.isHighContrastModeEnabled
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dark_theme), fontWeight = FontWeight.Bold
                    )
                }, navigationIcon = {
                    BackButton() {
                        onBackPressed()
                    }
                }, scrollBehavior = scrollBehavior
            )
        }, content = { paddingValues ->
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
                item {
                    SettingsSectionHeader(stringResource(R.string.theme))
                }
                item {
                    SettingsGroup(modifier = Modifier.selectableGroup()) {
                        SettingsNewSingleChoiceItem(
                            text = stringResource(R.string.follow_system),
                            selected = darkThemePreference.darkThemeValue == FOLLOW_SYSTEM,
                            wantsTonalElevation = false,
                        ) { PreferencesUtil.modifyDarkThemePreference(FOLLOW_SYSTEM) }
                        SettingsDivider(indent = 16.dp)
                        SettingsNewSingleChoiceItem(
                            text = stringResource(R.string.on),
                            selected = darkThemePreference.darkThemeValue == ON,
                            wantsTonalElevation = false,
                        ) { PreferencesUtil.modifyDarkThemePreference(ON) }
                        SettingsDivider(indent = 16.dp)
                        SettingsNewSingleChoiceItem(
                            text = stringResource(R.string.off),
                            selected = darkThemePreference.darkThemeValue == OFF,
                            wantsTonalElevation = false,
                        ) { PreferencesUtil.modifyDarkThemePreference(OFF) }
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(R.string.additional_settings))
                }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            onCheckedChange = {
                                PreferencesUtil.modifyDarkThemePreference(isHighContrastModeEnabled = !isHighContrastModeEnabled)
                            },
                            checked = isHighContrastModeEnabled,
                            title = { Text(text = stringResource(R.string.high_contrast)) },
                            icon = Icons.Outlined.Contrast,
                            addTonalElevation = false,
                        )
                    }
                }
            }
        })
}
