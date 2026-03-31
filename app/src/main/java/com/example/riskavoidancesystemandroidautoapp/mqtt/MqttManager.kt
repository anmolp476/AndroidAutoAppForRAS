package com.example.riskavoidancesystemandroidautoapp.mqtt

import android.content.Context
import android.util.Log
import com.example.riskavoidancesystemandroidautoapp.MqttSecrets
import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.*
import ch.hsr.geohash.GeoHash

class MqttManager(private val context: Context) {
    private var mqttClient: MqttAsyncClient? = null
    private val gson = Gson()

    // Spatial State Tracking (Phase 2)
    private var currentGeohashTopic: String? = null
    private var messageCallback: ((RiskData) -> Unit)? = null

    // The Temporal Shield Anchor
    private var connectionTime: Long = 0

    // --- THE WAITING ROOM (NEW) ---
    // These hold the GPS coordinates if they arrive before the network is ready
    private var pendingLatitude: Double? = null
    private var pendingLongitude: Double? = null

    fun connect(carMacId: String, initialInfo: InfoPacket?, onMessageReceived: (RiskData) -> Unit) {
        if (mqttClient?.isConnected == true) {
            Log.w("RAS_MQTT", "Connection already exists. Blocking redundant startup command.")
            return
        }

        this.messageCallback = onMessageReceived
        val serverUri = "ssl://${MqttSecrets.HIVEMQ_URL}:8883"
        val clientId = "AndroidAuto_$carMacId"

        try {
            mqttClient = MqttAsyncClient(serverUri, clientId, null)
            val options = MqttConnectOptions().apply {
                userName = MqttSecrets.HIVEMQ_USER
                password = MqttSecrets.HIVEMQ_PASS.toCharArray()
                isCleanSession = true
            }

            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("RAS_MQTT", "Successfully connected to HiveMQ cluster")
                    connectionTime = System.currentTimeMillis()

                    // --- PHASE 1: THE HARDWARE HANDSHAKE ---
                    val cleanMac = carMacId.uppercase().trim()
                    val phase1Topic = "incidents/reports"

                    Log.d("RAS_MQTT", "Phase 1: Occupying static hardware channel: $phase1Topic")

                    mqttClient?.subscribe(phase1Topic, 1) { _, message ->
                        val payload = message.payload.decodeToString()
                        Log.d("RAS_MQTT", "Phase 1 Intercept: $payload")
                    }

                    if (initialInfo != null) {
                        sendInfoPacket(phase1Topic, initialInfo)
                    }

                    // --- THE UNLEASH (NEW) ---
                    // The network is finally ready. Check if the GPS dropped a coordinate while we were building the bridge.
                    if (pendingLatitude != null && pendingLongitude != null) {
                        Log.w("RAS_MQTT", "Network online. Releasing buffered Phase 2 coordinate.")
                        updateSpatialSubscription(pendingLatitude!!, pendingLongitude!!)

                        // Empty the waiting room
                        pendingLatitude = null
                        pendingLongitude = null
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("RAS_MQTT", "Connection failed: ${exception?.message}")
                }
            })
        } catch (e: MqttException) {
            Log.e("RAS_MQTT", "Error creating MQTT client", e)
        }
    }

    // --- PHASE 2: THE SPATIAL TELEMETRY ---
    fun updateSpatialSubscription(latitude: Double, longitude: Double) {

        // --- THE TRAP (NEW) ---
        // If the client isn't connected yet, lock the coordinate in the waiting room and ABORT.
        if (mqttClient?.isConnected != true) {
            Log.w("RAS_MQTT", "Network not ready. Buffering Phase 2 coordinate.")
            pendingLatitude = latitude
            pendingLongitude = longitude
            return // CRITICAL: This stops the red error from happening.
        }

        val newGeohash = GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, 7)
        val newTopic = "risk/geohash/7/$newGeohash"

        if (currentGeohashTopic == newTopic) return

        currentGeohashTopic?.let { oldTopic ->
            Log.d("RAS_MQTT", "Phase 2: Leaving zone. Unsubscribing from: $oldTopic")
            try { mqttClient?.unsubscribe(oldTopic) } catch (e: Exception) {}
        }

        Log.d("RAS_MQTT", "Phase 2: Entering new zone. Subscribing to: $newTopic")
        try {
            mqttClient?.subscribe(newTopic, 1) { _, message ->
                val timeSinceConnect = System.currentTimeMillis() - connectionTime
                if (timeSinceConnect < 3000) return@subscribe

                try {
                    val jsonString = message.payload.decodeToString()
                    Log.d("RAS_MQTT", "Phase 2 Hazard Intercepted: $jsonString")

                    val data = gson.fromJson(jsonString, RiskData::class.java)
                    messageCallback?.invoke(data)
                } catch (e: Exception) {
                    Log.e("RAS_MQTT", "Phase 2 JSON Parsing Error: ${e.message}")
                }
            }
            currentGeohashTopic = newTopic
        } catch (e: Exception) {
            Log.e("RAS_MQTT", "Failed to subscribe to Phase 2 zone: ${e.message}")
        }
    }

    private fun sendInfoPacket(topic: String, info: InfoPacket) {
        val jsonString = gson.toJson(info)
        val message = MqttMessage(jsonString.toByteArray()).apply { qos = 1 }

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