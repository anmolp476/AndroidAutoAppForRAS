package com.example.riskavoidancesystemandroidautoapp.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.riskavoidancesystemandroidautoapp.mqtt.MqttManager
import com.example.riskavoidancesystemandroidautoapp.mqtt.RiskData

class RASForegroundService : Service() {
    private lateinit var mqttManager: MqttManager

    override fun onCreate() {
        super.onCreate()
        mqttManager = MqttManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Create the notification so Android doesn't kill the process
        val notification = NotificationHelper.createNotification(this)
        startForeground(1, notification)

        // 2. Get your saved Car MAC ID from Step 1
        val sharedPrefs = getSharedPreferences("VehiclePrefs", MODE_PRIVATE)
        val carMacId = sharedPrefs.getString("CAR_ID_MAC", "") ?: ""

        if (carMacId.isNotEmpty()) {
            // 3. Connect to HiveMQ and listen for the Pi's telemetr
            mqttManager.connect(carMacId) { riskData ->
                handleIncomingRisk(riskData)
            }
        }

        return START_STICKY // Tells Android to restart the service if it's killed
    }

    private fun handleIncomingRisk(data: RiskData) {
        val intent = Intent("com.example.RAS_UPDATE").apply {
            putExtra("risk", data.risk)
            putExtra("behaviour", data.catBehaviour)
            // You can add logic here to determine direction based on Pi's coordinates if needed
            putExtra("direction", "Front")
            setPackage(packageName) // Security requirement for Android 16
        }
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mqttManager.disconnect()
        super.onDestroy()
    }
}