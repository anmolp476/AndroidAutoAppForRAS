package com.example.riskavoidancesystemandroidautoapp.car

import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

import com.example.riskavoidancesystemandroidautoapp.util.RASForegroundService

class RASCarAppService : CarAppService() {

    override fun onCreateSession(): Session {
        return RASSession()
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    // The Absolute Kill Switch
    override fun onDestroy() {
        super.onDestroy()

        // 1. Sever the MQTT pipeline and terminate the background listener
        val serviceIntent = Intent(this, RASForegroundService::class.java)
        stopService(serviceIntent)

        // 2. Annihilate any ghost notifications trapped in the phone's memory
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
}