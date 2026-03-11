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

    fun connect(carMacId: String, initialInfo: InfoPacket?, onMessageReceived: (RiskData) -> Unit) {
        val serverUri = "ssl://${MqttSecrets.HIVEMQ_URL}:8883"
        val clientId = "AndroidAuto_$carMacId"

        try {
            mqttClient = MqttAsyncClient(serverUri, clientId, null)
            val options = MqttConnectOptions().apply {
                userName = MqttSecrets.HIVEMQ_USER
                password = MqttSecrets.HIVEMQ_PASS.toCharArray()
                isCleanSession = true
                isAutomaticReconnect = true
                connectionTimeout = 30
                keepAliveInterval = 60
            }

            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("RAS_MQTT", "Successfully connected to HiveMQ cluster")

                    // Ensure the connection exists before transmitting setup data
                    if (initialInfo != null) {
                        sendInfoPacket(carMacId, initialInfo)
                    }

                    // Then open the channel for incoming hazards
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

    fun sendInfoPacket(carMacId: String, info: InfoPacket) {
        // We send to a 'setup' topic so the Pi knows this is a config change, not an alert
        val topic = "ras/setup/$carMacId"
        val jsonString = gson.toJson(info)

        val message = MqttMessage(jsonString.toByteArray()).apply {
            qos = 1  // Use QoS 1 to ensure the Pi definitely gets the password
        }

        try {
            mqttClient?.publish(topic, message)
            Log.d("RAS_MQTT", "Successfully published INFO packet to $topic")
        } catch (e: Exception) {
            Log.e("RAS_MQTT", "Failed to publish INFO: ${e.message}")
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