package com.example.riskavoidancesystemandroidautoapp.mqtt

data class RiskData(
    val risk: String,             // Kept as String for HIGH/MEDIUM/LOW
    val catBehaviours: List<String>, // Changed to List and plural to match your log!
    val direction: String?,
    val lat: Double,
    val lon: Double,              // Changed from 'long' to 'lon' to match your log!
    val meshId: Int?,
    val vehicleMake: String?,
    val vehicleModel: String?,
    val color: String?
)