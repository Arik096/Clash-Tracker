package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.UpgradeTask
import com.example.receiver.UpgradeAlarmReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "coc_upgrade_alerts"
        const val CHANNEL_NAME = "Clash Upgrade Alerts"
        const val ACTION_UPGRADE_FINISHED = "com.example.clash.ACTION_UPGRADE_FINISHED"
        const val EXTRA_UPGRADE_ID = "extra_upgrade_id"
        const val EXTRA_BUILDING_NAME = "extra_building_name"
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_BUILDER_INDEX = "extra_builder_index"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when Clash of Clans builder upgrades complete"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleUpgradeNotification(task: UpgradeTask) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, UpgradeAlarmReceiver::class.java).apply {
            action = ACTION_UPGRADE_FINISHED
            putExtra(EXTRA_UPGRADE_ID, task.id)
            putExtra(EXTRA_BUILDING_NAME, task.buildingName)
            putExtra(EXTRA_LEVEL, task.toLevel)
            putExtra(EXTRA_BUILDER_INDEX, task.builderIndex)
        }

        val requestCode = task.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = task.endTimeMillis
        if (triggerTime <= System.currentTimeMillis()) {
            // Already finished or immediate
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // If exact alarm permission is restricted on Android 12+, fallback to inexact
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelUpgradeNotification(task: UpgradeTask) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, UpgradeAlarmReceiver::class.java).apply {
            action = ACTION_UPGRADE_FINISHED
        }
        val requestCode = task.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showFinishNotification(
        builderIndex: Int,
        buildingName: String,
        targetLevel: Int,
        notificationId: Int
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Builder #$builderIndex Upgrade Completed! 🔨")
            .setContentText("$buildingName is now Level $targetLevel! Builder is idle — Log in now to assign the next upgrade!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$buildingName reached Level $targetLevel! Builder #$builderIndex is now free.\nLog in now to queue your next prioritized building and prevent your village storages from overflowing!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission might not be granted yet
        }
    }

    fun showTestNotification() {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Clash Tracker: Test Notification ⚔️")
            .setContentText("Upgrade push notifications are active! You will be alerted when builders finish.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9999, notification)
        } catch (_: SecurityException) {
        }
    }
}
