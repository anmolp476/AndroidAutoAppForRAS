package com.example.riskavoidancesystemandroidautoapp.util

import android.app.Service
import android.content.Intent
import android.location.Location // Critical import for spatial mathematics
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.riskavoidancesystemandroidautoapp.mqtt.InfoPacket
import com.example.riskavoidancesystemandroidautoapp.mqtt.MqttManager
import com.example.riskavoidancesystemandroidautoapp.mqtt.RiskData
import com.google.android.gms.location.*

class RASForegroundService : Service() {
    private lateinit var mqttManager: MqttManager

    // The Location Hardware Hooks
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // The Spatial Anchor
    private var currentLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        mqttManager = MqttManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.createNotification(this)
        startForeground(1, notification)

        val carMacId = intent?.getStringExtra("MAC_ID") ?: return START_STICKY

        val infoPacket = InfoPacket(
            payloadType = "INFO",
            ssid = intent.getStringExtra("SSID") ?: "",
            pass = intent.getStringExtra("PASS") ?: "",
            vehicleMake = intent.getStringExtra("MAKE") ?: "",
            vehicleModel = intent.getStringExtra("MODEL") ?: "",
            vehicleYear = intent.getStringExtra("YEAR") ?: ""
        )

        // Ignite the Dual-Channel Network
        mqttManager.connect(carMacId, infoPacket) { riskData ->
            handleIncomingRisk(riskData)
        }

        // Ignite the Spatial Tracking Engine
        startLocationTracking()

        return START_STICKY
    }

    private fun startLocationTracking() {
        Log.d("RAS_ForegroundService", "STARTING LOCATION TRACKING")

        //polling every 5 seconds
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(50f) // Keep commented out for stationary testing
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // Lock the newest coordinate into memory for relative direction math
                    currentLocation = location

                    Log.d("RAS_ForegroundService", "Location logged: ${location.latitude}/${location.longitude}")
                    mqttManager.updateSpatialSubscription(location.latitude, location.longitude)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("RAS_LOCATION", "Fatal: Location permission denied by system.")
        }
    }

    private fun handleIncomingRisk(data: RiskData) {
        // Intercept the raw coordinates and calculate the relative direction locally
        val calculatedDirection = calculateRelativeDirection(data.lat, data.long)

        val intent = Intent("com.example.RAS_UPDATE").apply {
            putExtra("risk", data.risk)
            putExtra("behaviour", data.catBehaviour)
            putExtra("direction", calculatedDirection)
            setPackage(packageName)
        }
        sendBroadcast(intent)

        Log.d("RAS_ForegroundService", "riskLevel: ${data.risk} behaviour: ${data.catBehaviour} direction: $calculatedDirection")

        NotificationHelper.pushHazardAlert(
            context = this,
            riskLevel = data.risk ?: "UNKNOWN",
            behavior = data.catBehaviour ?: "Hazard",
            direction = calculatedDirection ?: "Front"
        )
    }

    // The Spatial Calculation Engine
    private fun calculateRelativeDirection(targetLat: Double?, targetLon: Double?): String {
        // If the payload lacks coordinates, or we lack our own location, we cannot compute
        if (targetLat == null || targetLon == null || currentLocation == null) {
            return "System Alert"
        }

        val myLoc = currentLocation!!

        // A device must be physically moving to establish a heading.
        // If stationary, the math collapses. We default to a neutral alert.
        if (!myLoc.hasBearing()) {
            return "Nearby"
        }

        val targetLoc = Location("").apply {
            latitude = targetLat
            longitude = targetLon
        }

        // Calculate absolute angles
        val targetBearing = myLoc.bearingTo(targetLoc)
        val myHeading = myLoc.bearing

        // Normalize the relative angle to a clean 0-360 degree circle
        var relativeAngle = (targetBearing - myHeading) % 360
        if (relativeAngle < 0) {
            relativeAngle += 360
        }

        // Map the angle to the 8 rigid sectors
        return when (relativeAngle) {
            in 337.5..360.0, in 0.0..<22.5 -> "Front"
            in 22.5..<67.5 -> "Front-Right"
            in 67.5..<112.5 -> "Right"
            in 112.5..<157.5 -> "Back-Right"
            in 157.5..<202.5 -> "Back"
            in 202.5..<247.5 -> "Back-Left"
            in 247.5..<292.5 -> "Left"
            in 292.5..<337.5 -> "Front-Left"
            else -> "System Alert"
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Sever the GPS hardware connection to prevent battery drain
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        mqttManager.disconnect()
        super.onDestroy()
    }
}