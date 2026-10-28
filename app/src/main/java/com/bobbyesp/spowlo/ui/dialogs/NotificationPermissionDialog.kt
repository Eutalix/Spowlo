package com.bobbyesp.spowlo.ui.dialogs

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bobbyesp.spowlo.R
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
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null
            )
        },
        text = {
            Text(text = stringResource(id = R.string.enable_notifications_desc))
        },
        title = { Text(text = stringResource(id = R.string.enable_notifications)) },
        confirmButton = {
            Button(onClick = onPermissionGranted) {
                Text(text = stringResource(id = R.string.enable_notifications_action))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.not_now))
            }
        }
    )
}
