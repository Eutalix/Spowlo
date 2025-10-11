package com.bobbyesp.spowlo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bobbyesp.spowlo.utils.NotificationsUtil

/**
 * A BroadcastReceiver to handle actions from notifications, such as cancelling a download.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionReceiver"
        const val ACTION_CANCEL_TASK = "com.bobbyesp.spowlo.ACTION_CANCEL_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        when (intent.action) {
            ACTION_CANCEL_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

                if (!taskId.isNullOrEmpty()) {
                    Log.d(TAG, "Received request to cancel task with ID: $taskId")
                    
                    // Call the corrected cancel method from the new Downloader singleton.
                    Downloader.cancelDownload(taskId)
                    
                    // Dismiss the corresponding notification.
                    NotificationsUtil.cancelNotification(notificationId)
                }
            }
        }
    }
}