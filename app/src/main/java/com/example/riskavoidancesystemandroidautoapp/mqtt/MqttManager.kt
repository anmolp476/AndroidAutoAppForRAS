package com.example.riskavoidancesystemandroidautoapp.mqtt

import android.content.Context
import android.util.Log
import com.example.riskavoidancesystemandroidautoapp.MqttSecrets
import com.example.riskavoidancesystemandroidautoapp.mqtt.RiskData
import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.*

/**
 * Handles the secure connection to HiveMQ Cloud and processes
 * incoming telemetry from the Raspberry Pi mesh network.
 */
class MqttManager(private val context: Context) {
    private var mqttClient: MqttAsyncClient? = null
    private val gson = Gson()

    fun connect(carMacId: String, onMessageReceived: (RiskData) -> Unit) {
        // Use the secure 8883 port required by HiveMQ Cloud
        val serverUri = "ssl://${MqttSecrets.HIVEMQ_URL}:8883"
        val clientId = "AndroidAuto_$carMacId"

        try {
            mqttClient = MqttAsyncClient(serverUri, clientId, null)

            val options = MqttConnectOptions().apply {
                // Pull credentials from your ignored MqttSecrets object
                userName = MqttSecrets.HIVEMQ_USER
                password = MqttSecrets.HIVEMQ_PASS.toCharArray()

                isCleanSession = true
                isAutomaticReconnect = true

                // Set connection timeout to 30 seconds for mobile stability
                connectionTimeout = 30
                keepAliveInterval = 60
            }

            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("RAS_MQTT", "Successfully connected to HiveMQ cluster")
                    subscribeToCarTopic(carMacId, onMessageReceived)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("RAS_MQTT", "Connection failed: ${exception?.message}")
                }
            })
        } catch (e: MqttException) {
            Log.e("RAS_MQTT", "Error creating MQTT client", e)
        }
    }

    private fun subscribeToCarTopic(carMacId: String, onMessageReceived: (RiskData) -> Unit) {
        // Force uppercase and trim to remove hidden spaces
        val cleanMac = carMacId.uppercase().trim()
        val topic = "ras/alerts/$cleanMac"

        Log.d("RAS_MQTT", "Listening for Pi on topic: '$topic'")

        mqttClient?.subscribe(topic, 1) { _, message ->
            try {
                val jsonString = message.payload.decodeToString()
                Log.d("RAS_MQTT", "Received Raw JSON: $jsonString")

                // Parse the actual JSON fields (risk, catBehaviour, meshId, etc.)
                val data = gson.fromJson(jsonString, RiskData::class.java)
                onMessageReceived(data)
            } catch (e: Exception) {
                Log.e("RAS_MQTT", "JSON Parsing Error: ${e.message}")
            }
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            Log.d("RAS_MQTT", "Disconnected safely")
        } catch (e: MqttException) {
            Log.e("RAS_MQTT", "Error during disconnect", e)
        }
    }
}