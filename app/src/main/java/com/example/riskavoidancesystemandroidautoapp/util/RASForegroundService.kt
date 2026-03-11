package com.example.riskavoidancesystemandroidautoapp.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.riskavoidancesystemandroidautoapp.mqtt.InfoPacket
import com.example.riskavoidancesystemandroidautoapp.mqtt.MqttManager
import com.example.riskavoidancesystemandroidautoapp.mqtt.RiskData

class RASForegroundService : Service() {
    private lateinit var mqttManager: MqttManager

    override fun onCreate() {
        super.onCreate()
        mqttManager = MqttManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.createNotification(this)
        startForeground(1, notification)

        val carMacId = intent?.getStringExtra("MAC_ID") ?: return START_STICKY

        // Assemble the configuration payload from the Intent
        val infoPacket = InfoPacket(
            payloadType = "INFO",
            ssid = intent.getStringExtra("SSID") ?: "",
            pass = intent.getStringExtra("PASS") ?: "",
            vehicleMake = intent.getStringExtra("MAKE") ?: "",
            vehicleModel = intent.getStringExtra("MODEL") ?: "",
            vehicleYear = intent.getStringExtra("YEAR") ?: ""
        )

        // Initiate the master connection sequence
        mqttManager.connect(carMacId, infoPacket) { riskData ->
            handleIncomingRisk(riskData)
        }

        return START_STICKY
    }

    private fun handleIncomingRisk(data: RiskData) {
        // 1. Send the data to the Android Auto Dashboard
        val intent = Intent("com.example.RAS_UPDATE").apply {
            putExtra("risk", data.risk)
            putExtra("behaviour", data.catBehaviour)
            putExtra("direction", data.direction ?: "Front")
            setPackage(packageName)
        }
        sendBroadcast(intent)

        // 2. Update the Phone's Notification Tray
        NotificationHelper.pushHazardAlert(
            context = this,
            riskLevel = data.risk ?: "UNKNOWN",
            behavior = data.catBehaviour ?: "Hazard",
            direction = data.direction ?: "Front"
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mqttManager.disconnect()
        super.onDestroy()
    }
}