package com.bobbyesp.spowlo.ui.pages.settings.cookies

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.ActionDialog
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.PreferenceInfo
import com.bobbyesp.spowlo.ui.components.settings.SettingsDivider
import com.bobbyesp.spowlo.ui.components.settings.SettingsGroup
import com.bobbyesp.spowlo.ui.components.settings.SettingsItemNew
import com.bobbyesp.spowlo.ui.components.settings.SettingsSectionHeader
import com.bobbyesp.spowlo.ui.components.settings.SettingsSwitch
import com.bobbyesp.spowlo.utils.COOKIES
import com.bobbyesp.spowlo.utils.GoogAcc
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.clearYoutubeCookies
import com.bobbyesp.spowlo.utils.hasSavedYoutubeCookies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookiesSettingsPage(
    navigateToYoutubeSignIn: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCookieEnabled by remember { mutableStateOf(PreferencesUtil.getValue(COOKIES)) }
    var hasCookies by remember { mutableStateOf(context.hasSavedYoutubeCookies()) }
    var account by remember { mutableStateOf(PreferencesUtil.getGoogleAccountEmail()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasCookies = context.hasSavedYoutubeCookies()
        isCookieEnabled = PreferencesUtil.getValue(COOKIES)
        account = PreferencesUtil.getGoogleAccountEmail()
        refreshKey++
    }

    LaunchedEffect(refreshKey) {
        if (!hasCookies) return@LaunchedEffect
        val email = GoogAcc.detectEmail(context)
        if (email != null) {
            PreferencesUtil.setGoogleAccountEmail(email)
            account = email
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = {
                Text(
                    modifier = Modifier,
                    text = stringResource(id = R.string.cookies), fontWeight = FontWeight.Bold
                )
            }, navigationIcon = {
                BackButton {
                    onBackPressed()
                }
            }, scrollBehavior = scrollBehavior)
        },
    )
    { paddingValues ->
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
                SettingsSectionHeader(stringResource(R.string.cookies))
            }
            item {
                SettingsGroup {
                    SettingsSwitch(
                        onCheckedChange = { enabled ->
                            if (enabled && !hasCookies) {
                                navigateToYoutubeSignIn()
                            } else {
                                isCookieEnabled = enabled
                                PreferencesUtil.updateValue(COOKIES, enabled)
                            }
                        },
                        checked = isCookieEnabled,
                        title = {
                            Text(
                                text = stringResource(R.string.use_cookies),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        description = {
                            Text(text = stringResource(R.string.use_cookies_desc))
                        },
                        icon = if (!isCookieEnabled) Icons.Outlined.Cookie else Icons.Filled.Cookie,
                        addTonalElevation = false,
                    )
                }
            }

            item {
                SettingsSectionHeader(stringResource(R.string.youtube_account))
            }
            item {
                SettingsGroup {
                    SettingsItemNew(
                        title = {
                            Text(
                                text = account?.let { stringResource(R.string.signed_in_as, it) }
                                    ?: stringResource(
                                        if (hasCookies) R.string.signed_in_youtube
                                        else R.string.sign_in_youtube
                                    ),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        description = if (account != null || hasCookies) {
                            {
                                Text(text = stringResource(R.string.sign_in_youtube_signed_in))
                            }
                        } else {
                            null
                        },
                        icon = Icons.Outlined.AccountCircle,
                        addTonalElevation = false,
                        onClick = { navigateToYoutubeSignIn() },
                    )
                    SettingsDivider()
                    SettingsItemNew(
                        title = {
                            Text(
                                text = stringResource(R.string.clear_all_cookies),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = Icons.Outlined.DeleteForever,
                        enabled = hasCookies,
                        addTonalElevation = false,
                        onClick = { showDeleteDialog = true },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                PreferenceInfo(
                    text = stringResource(id = R.string.cookies_scope_hint)
                )
            }
        }

    }
    if (showDeleteDialog) {
        ActionDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = Icons.Outlined.DeleteForever,
            title = stringResource(R.string.clear_all_cookies),
            supportingText = stringResource(R.string.clear_all_cookies_desc),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                showDeleteDialog = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        context.clearYoutubeCookies()
                    }
                    PreferencesUtil.updateValue(COOKIES, false)
                    PreferencesUtil.clearGoogleAccountEmail()
                    hasCookies = false
                    isCookieEnabled = false
                    account = null
                    ToastUtil.makeToast(context.getString(R.string.cookies_deleted))
                }
            },
        )
    }
}
