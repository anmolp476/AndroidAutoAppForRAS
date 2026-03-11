package com.example.riskavoidancesystemandroidautoapp.mqtt

data class InfoPacket(
    val payloadType: String = "INFO",
    val ssid: String,
    val pass: String,
    val vehicleMake: String,
    val vehicleModel: String,
    val vehicleYear: String
)