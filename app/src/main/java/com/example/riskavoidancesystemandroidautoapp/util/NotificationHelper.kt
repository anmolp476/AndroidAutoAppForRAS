package com.example.riskavoidancesystemandroidautoapp.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "ras_monitoring_channel"
    private const val NOTIFICATION_ID = 1

    fun createNotification(context: Context): Notification {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Create the Channel (Only needed once, but safe to call multiple times)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vehicle Risk Monitoring",
                NotificationManager.IMPORTANCE_LOW // Low so it doesn't make a sound every time
            ).apply {
                description = "Running MQTT connection to Raspberry Pi Mesh"
            }
            manager.createNotificationChannel(channel)
        }

        // 2. Build the actual notification
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("RAS Active")
            .setContentText("Connected to Mesh Network")
            // Make sure you have an icon in your res/drawable folder!
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true) // Prevents user from swiping it away
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}