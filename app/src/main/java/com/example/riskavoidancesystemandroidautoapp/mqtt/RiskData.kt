package com.example.riskavoidancesystemandroidautoapp.mqtt

/**
 * Matches the JSON published by HiveMQ.
 * Note: If 'catBehaviour' is too complex, we can treat it as a String
 * or have your partner simplify it to a single "hazard" type.
 */
data class RiskData(
    val risk: String,             // e.g., "high"
    val catBehaviour: String,     // e.g., "driver distracted..."
    val direction: String?,
    val lat: Double,
    val long: Double,
    val meshId: Int,
    val vehicleMake: String?,
    val vehicleModel: String?,
    val color: String?
)