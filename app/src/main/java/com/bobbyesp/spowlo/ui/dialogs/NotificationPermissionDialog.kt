package com.bobbyesp.spowlo.ui.dialogs

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.ActionDialog
import com.bobbyesp.spowlo.utils.NOTIFICATION
import com.bobbyesp.spowlo.utils.NOTIFICATION_PERMISSION_REQUESTED
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberNotificationPermissionRequest(
    onResult: (Boolean) -> Unit = {}
): () -> Unit {
    val permissionState = if (Build.VERSION.SDK_INT >= 33) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { granted ->
            if (granted) PreferencesUtil.updateValue(NOTIFICATION, true)
            onResult(granted)
        }
    } else null

    return {
        PreferencesUtil.updateValue(NOTIFICATION_PERMISSION_REQUESTED, true)
        PreferencesUtil.updateValue(NOTIFICATION, true)
        permissionState?.launchPermissionRequest()
    }
}

@Composable
@Preview
fun NotificationPermissionDialog(
    onDismissRequest: () -> Unit = {},
    onPermissionGranted: () -> Unit = {}
) {
    ActionDialog(
        onDismissRequest = onDismissRequest,
        icon = Icons.Outlined.NotificationsActive,
        title = stringResource(id = R.string.enable_notifications),
        supportingText = stringResource(id = R.string.enable_notifications_desc),
        confirmText = stringResource(id = R.string.enable_notifications_action),
        onConfirm = onPermissionGranted,
        dismissText = stringResource(id = R.string.not_now),
    )
}
