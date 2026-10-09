package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.CocAppDatabase
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpgradeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_UPGRADE_FINISHED) {
            val upgradeId = intent.getStringExtra(NotificationHelper.EXTRA_UPGRADE_ID) ?: ""
            val buildingName = intent.getStringExtra(NotificationHelper.EXTRA_BUILDING_NAME) ?: "Building"
            val level = intent.getIntExtra(NotificationHelper.EXTRA_LEVEL, 1)
            val builderIndex = intent.getIntExtra(NotificationHelper.EXTRA_BUILDER_INDEX, 1)

            val helper = NotificationHelper(context)
            helper.showFinishNotification(
                builderIndex = builderIndex,
                buildingName = buildingName,
                targetLevel = level,
                notificationId = upgradeId.hashCode()
            )

            // Optionally mark in DB as completed
            if (upgradeId.isNotEmpty()) {
                val db = CocAppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    db.upgradeDao().markCompleted(upgradeId)
                }
            }
        }
    }
}
