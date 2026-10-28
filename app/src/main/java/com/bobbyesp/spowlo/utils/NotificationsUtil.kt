package com.bobbyesp.spowlo.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.graphics.drawable.IconCompat
import com.bobbyesp.spowlo.App.Companion.context
import com.bobbyesp.spowlo.MainActivity
import com.bobbyesp.spowlo.NotificationActionReceiver
import com.bobbyesp.spowlo.NotificationActionReceiver.Companion.ACTION_CANCEL_TASK
import com.bobbyesp.spowlo.NotificationActionReceiver.Companion.ACTION_ERROR_REPORT
import com.bobbyesp.spowlo.NotificationActionReceiver.Companion.ACTION_KEY
import com.bobbyesp.spowlo.NotificationActionReceiver.Companion.ERROR_REPORT_KEY
import com.bobbyesp.spowlo.NotificationActionReceiver.Companion.NOTIFICATION_ID_KEY
import com.bobbyesp.spowlo.NotificationActionReceiver.Companion.TASK_ID_KEY
import com.bobbyesp.spowlo.R
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "NotificationUtil"

@SuppressLint("StaticFieldLeak")
object NotificationsUtil {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val progressStates = ConcurrentHashMap<Int, ProgressState>()
    private const val PROGRESS_MAX = 100
    private const val PROGRESS_INITIAL = 0
    private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    private const val CHANNEL_ID = "download_notification"
    private const val SERVICE_CHANNEL_ID = "download_service"
    private const val FINISHED_CHANNEL_ID = "download_finished"
    private const val NOTIFICATION_GROUP_ID = "spowlo.download.notification"
    private const val DEFAULT_NOTIFICATION_ID = 100
    private const val REQUEST_CODE_OPEN_DOWNLOADS = 2000
    const val SERVICE_NOTIFICATION_ID = 123

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        val name = context.getString(R.string.channel_name)
        val serviceChannelName = context.getString(R.string.service_channel_name)
        val descriptionText = context.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channelGroup =
            NotificationChannelGroup(NOTIFICATION_GROUP_ID, context.getString(R.string.download))
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            group = NOTIFICATION_GROUP_ID
        }
        val serviceChannel =
            NotificationChannel(SERVICE_CHANNEL_ID, serviceChannelName, importance).apply {
                description = context.getString(R.string.service_title)
                group = NOTIFICATION_GROUP_ID
            }
        val finishedChannel = NotificationChannel(
            FINISHED_CHANNEL_ID,
            context.getString(R.string.finished_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.finished_channel_description)
            group = NOTIFICATION_GROUP_ID
        }
        notificationManager.createNotificationChannelGroup(channelGroup)
        notificationManager.createNotificationChannel(channel)
        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(finishedChannel)
    }

    fun notifyProgress(
        title: String,
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        progress: Int? = PROGRESS_INITIAL,
        taskId: String? = null,
        text: String? = null
    ) {
        if (!notificationsEnabled()) return
        val progressValue = progressToPost(notificationId, progress) ?: return

        val pendingIntent = taskId?.let {
            Intent(context.applicationContext, NotificationActionReceiver::class.java)
                .putExtra(TASK_ID_KEY, taskId)
                .putExtra(NOTIFICATION_ID_KEY, notificationId)
                .putExtra(ACTION_KEY, ACTION_CANCEL_TASK).run {
                    PendingIntent.getBroadcast(
                        context.applicationContext,
                        notificationId,
                        this,
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
        }

        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openDownloadsIntent())
            .applyProgress(progressValue, text)
            .run {
                pendingIntent?.let {
                    addAction(
                        R.drawable.outline_cancel_24,
                        context.getString(R.string.cancel),
                        it
                    )
                }
                notificationManager.notify(notificationId, build())
            }
    }

    fun finishNotification(
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        title: String? = null,
        text: String? = null,
        intent: PendingIntent? = null,
    ) {
        if (!notificationsEnabled()) return

        Log.d(TAG, "finishNotification: ")
        notificationManager.cancel(notificationId)
        progressStates.remove(notificationId)
        val builder = NotificationCompat.Builder(context, FINISHED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentIntent(intent ?: openDownloadsIntent())
            .setOngoing(false)
            .setAutoCancel(true)

        text?.let {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(it))
        }

        notificationManager.notify(notificationId, builder.build())
    }

    fun makeServiceNotification(intent: PendingIntent): Notification =
        NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.service_title))
            .setOngoing(true)
            .setContentIntent(intent)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        progressStates.remove(notificationId)
    }

    fun makeErrorReportNotification(
        title: String = context.getString(R.string.download_error_msg),
        notificationId: Int,
        error: String,
    ) {
        if (!notificationsEnabled()) return

        val intent = Intent()
            .setClass(context, NotificationActionReceiver::class.java)
            .putExtra(NOTIFICATION_ID_KEY, notificationId)
            .putExtra(ERROR_REPORT_KEY, error)
            .putExtra(ACTION_KEY, ACTION_ERROR_REPORT)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        notificationManager.cancel(notificationId)
        progressStates.remove(notificationId)
        NotificationCompat.Builder(context, FINISHED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(error)
            .setContentIntent(openDownloadsIntent())
            .setOngoing(false)
            .setAutoCancel(true)
            .addAction(
                R.drawable.outline_content_copy_24,
                context.getString(R.string.copy_error_report),
                pendingIntent
            ).run {
                notificationManager.notify(notificationId, build())
            }
    }

    fun makeNotificationForParallelDownloads(
        notificationId: Int,
        taskId: String,
        progress: Int? = null,
        text: String? = null,
        title: String,
    ) {
        if (!notificationsEnabled()) return
        val progressValue = progressToPost(notificationId, progress) ?: return

        val intent = Intent(context.applicationContext, NotificationActionReceiver::class.java)
            .putExtra(TASK_ID_KEY, taskId)
            .putExtra(NOTIFICATION_ID_KEY, notificationId)
            .putExtra(ACTION_KEY, ACTION_CANCEL_TASK)

        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openDownloadsIntent())
            .applyProgress(progressValue, text)
            .addAction(
                R.drawable.outline_cancel_24,
                context.getString(R.string.cancel),
                pendingIntent
            )
            .run {
                notificationManager.notify(notificationId, build())
            }
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        progressStates.clear()
    }

    fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT <= 24) true else notificationManager.areNotificationsEnabled()
    }

    fun notificationsEnabled(): Boolean =
        PreferencesUtil.getValue(NOTIFICATION) && areNotificationsEnabled()

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Could not open notification settings", it) }
    }

    private fun NotificationCompat.Builder.applyProgress(
        progress: Int,
        text: String? = null
    ): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= 36) {
            setStyle(
                NotificationCompat.ProgressStyle()
                    .setProgress(progress)
                    .setProgressTrackerIcon(
                        IconCompat.createWithResource(
                            context,
                            R.drawable.ic_music_note
                        )
                    )
            )
            setRequestPromotedOngoing(true)
            if (progress > 0) setShortCriticalText("$progress%")
        } else {
            setProgress(PROGRESS_MAX, progress, progress == 0)
            text?.let { setStyle(NotificationCompat.BigTextStyle().bigText(it)) }
        }
        return this
    }

    private fun progressToPost(notificationId: Int, progress: Int?): Int? {
        val now = SystemClock.elapsedRealtime()
        val previous = progressStates[notificationId]
        val value = progress?.coerceIn(0, PROGRESS_MAX) ?: previous?.value ?: 0
        if (previous != null &&
            previous.value == value &&
            now - previous.postedAt < PROGRESS_UPDATE_INTERVAL_MS
        ) return null
        progressStates[notificationId] = ProgressState(value, now)
        return value
    }

    private data class ProgressState(val value: Int, val postedAt: Long)

    private fun openDownloadsIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_DOWNLOADS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_DOWNLOADS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
