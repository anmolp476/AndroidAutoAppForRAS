package com.example.riskavoidancesystemandroidautoapp.mqtt

import android.content.Context
import android.util.Log
import org.eclipse.paho.client.mqttv3.*

class MqttManager(private val context: Context) {
    private var mqttClient: MqttAsyncClient? = null

    fun connect(carMacId: String, onMessageReceived: (RiskData) -> Unit) {
        val serverUri = "" // Use your cluster URL
        val clientId = "AndroidAuto_$carMacId" // Unique ID for the broker

        mqttClient = MqttAsyncClient(serverUri, clientId, null)

        val options = MqttConnectOptions().apply {
            userName = "your_username"
            password = "your_password".toCharArray()
            isCleanSession = true
            isAutomaticReconnect = true
        }

        mqttClient?.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "Connected successfully")
                // Subscribe to the topic specific to this car's MAC
                subscribeToCarTopic(carMacId, onMessageReceived)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Connection failed: ${exception?.message}")
            }
        })
    }

    private fun subscribeToCarTopic(carMacId: String, onMessageReceived: (RiskData) -> Unit) {
        val topic = "ras/alerts/$carMacId"
        mqttClient?.subscribe(topic, 1) { _, message ->
            // Parse JSON (e.g., using Gson) and trigger the callback
            // For now, we'll assume a helper handles the parsing
            val data = parseJson(message.toString())
            onMessageReceived(data)
        }
    }

    private fun parseJson(json: String): RiskData {
        // Implementation for Gson parsing would go here
        return RiskData("high", "Distracted", 0.0, 0.0, 119, "Nissan", "Versa", "Grey")
    }
}