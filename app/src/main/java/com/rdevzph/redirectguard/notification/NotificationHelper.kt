package com.rdevzph.redirectguard.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rdevzph.redirectguard.MainActivity
import com.rdevzph.redirectguard.R
import com.rdevzph.redirectguard.RedirectGuardApplication
import com.rdevzph.redirectguard.model.RedirectAction
import com.rdevzph.redirectguard.service.RedirectGuardAccessibilityService

object NotificationHelper {

    private const val CHANNEL_STATUS = "redirectguard_status_channel"
    private const val CHANNEL_ALERTS = "redirectguard_alerts_channel"

    private const val NOTIFICATION_ID_STATUS = 1001
    private const val NOTIFICATION_ID_ALERT_BASE = 2000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS,
                "Protection Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing status when Redirect Guard is actively protecting against unwanted redirects"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Redirect Interceptions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when an unauthorized app redirect is blocked or prompted"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(statusChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    fun updateStatusNotification(context: Context) {
        val prefs = RedirectGuardApplication.instance.preferencesManager
        val isServiceRunning = RedirectGuardAccessibilityService.isAccessibilityEnabled(context)
        val isGuardEnabled = prefs.isGuardEnabled.value
        val showNotification = prefs.showStatusNotification.value

        val notificationManager = NotificationManagerCompat.from(context)

        if (!showNotification || !isServiceRunning || !isGuardEnabled || !notificationManager.areNotificationsEnabled()) {
            notificationManager.cancel(NOTIFICATION_ID_STATUS)
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val guardAll = prefs.guardAllExternalApps.value
        val guardedCount = prefs.guardedPackages.value.size
        val targetInfo = if (guardAll) {
            "all external apps"
        } else {
            "$guardedCount guarded apps"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_redirectguard)
            .setContentTitle("Redirect Guard is active")
            .setContentText("Protecting you from unwanted redirects ($targetInfo)")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_STATUS, notification)
        } catch (e: SecurityException) {
            // Android 13+ permission not granted
        }
    }

    fun postInterceptedNotification(
        context: Context,
        sourceAppName: String,
        targetAppName: String,
        action: RedirectAction
    ) {
        val prefs = RedirectGuardApplication.instance.preferencesManager
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Unwanted Redirect Intercepted"
        val message = when (action) {
            RedirectAction.BLOCKED -> "Blocked redirect from $sourceAppName to $targetAppName."
            RedirectAction.PROMPTED -> "Confirmation requested for redirect from $sourceAppName to $targetAppName."
            RedirectAction.ALLOWED -> "Allowed redirect to $targetAppName."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_redirectguard)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            val notificationId = NOTIFICATION_ID_ALERT_BASE + (System.currentTimeMillis() % 1000).toInt()
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Android 13+ permission not granted
        }

        updateStatusNotification(context)
    }

    fun cancelStatusNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_STATUS)
    }
}
