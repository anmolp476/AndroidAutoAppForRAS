package com.example.riskavoidancesystemandroidautoapp.car

import android.content.Context
import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.core.content.ContextCompat
import com.example.riskavoidancesystemandroidautoapp.util.RASForegroundService

class RASSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val sharedPref = carContext.getSharedPreferences("VehiclePrefs", Context.MODE_PRIVATE)
        val savedMac = sharedPref.getString("CAR_ID_MAC", null)

        return if (savedMac == null) {
            CarPickerScreen(carContext)
        } else {
            // --- FIX: Start the Service if a car is already paired! ---
            val serviceIntent = Intent(carContext, RASForegroundService::class.java)
            ContextCompat.startForegroundService(carContext, serviceIntent)

            RASScreen(carContext)
        }
    }
}