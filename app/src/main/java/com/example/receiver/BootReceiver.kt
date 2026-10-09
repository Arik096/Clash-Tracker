package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.CocAppDatabase
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = CocAppDatabase.getDatabase(context)
            val notificationHelper = NotificationHelper(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeUpgrades = db.upgradeDao().getActiveUpgrades().first()
                    val now = System.currentTimeMillis()
                    for (upgrade in activeUpgrades) {
                        if (upgrade.endTimeMillis > now) {
                            notificationHelper.scheduleUpgradeNotification(upgrade)
                        } else {
                            db.upgradeDao().markCompleted(upgrade.id)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
