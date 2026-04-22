package com.example.riskavoidancesystemandroidautoapp.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.car.app.notification.CarAppExtender
import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.concurrent.thread

object NotificationHelper {

    // Initialize the generator using the ALARM stream for high visibility
    // The value 100 represents the system volume percentage
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
    private const val SERVICE_CHANNEL_ID = "ras_monitoring_channel"
    private const val ALERT_CHANNEL_ID = "ras_hazard_channel"
    private const val ANCHOR_ID = 1

    fun createNotification(context: Context): Notification {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: The silent anchor for the background service
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Vehicle Risk Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )

            // Channel 2: The high-priority channel for dashboard pop-ups
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Active Hazard Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alertChannel)
        }

        // Return the silent anchor
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("RAS Active")
            .setContentText("Connected to Mesh Network")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    // Call this from handleIncomingRisk instead of the old update function
    fun pushHazardAlert(context: Context, riskLevel: String, behavior: String, direction: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Generate a unique ID so each alert stacks independently on the phone
        val uniqueAlertId = System.currentTimeMillis().toInt()

        // The architectural bridge that forces the car to acknowledge the alert
        val carExtender = CarAppExtender.Builder().build()

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("Hazard: $riskLevel")
            .setContentText("$behavior detected at $direction")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Demanded for heads-up display
            .extend(carExtender) // Append the Car App Extender
            .build()

        manager.notify(uniqueAlertId, notification)
    }

    fun playSeveritySound(context: Context, riskLevel: String?) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Setup attributes to tell the car this is a safety alert
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Request Transient Focus to "duck" the music while the siren plays
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(playbackAttributes)
            .build()

        thread {
            val result = audioManager.requestAudioFocus(focusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                try {
                    when (riskLevel?.uppercase()) {
                        "LOW" -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                        "MEDIUM" -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
                        "HIGH", "CRITICAL" -> {
                            // Siren sequence: 3 emergency pulses
                            repeat(3) {
                                toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
                                Thread.sleep(600)
                            }
                        }
                    }
                } finally {
                    audioManager.abandonAudioFocusRequest(focusRequest)
                }
            }
        }
    }
}