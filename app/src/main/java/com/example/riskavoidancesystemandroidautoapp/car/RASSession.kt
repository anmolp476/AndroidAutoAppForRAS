package com.example.riskavoidancesystemandroidautoapp.car

import android.content.Context
import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class RASSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val sharedPref = carContext.getSharedPreferences("VehiclePrefs", Context.MODE_PRIVATE)
        val savedMac = sharedPref.getString("CAR_ID_MAC", null)

        //if we dont have hte mac address yet go to the car picker
        //otw just go to the regular ras dashboard screen
        return if (savedMac == null) {
            CarPickerScreen(carContext)
        } else {
            RASScreen(carContext)
        }
    }
}