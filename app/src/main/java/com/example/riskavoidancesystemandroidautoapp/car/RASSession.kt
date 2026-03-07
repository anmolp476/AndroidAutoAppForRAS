package com.example.riskavoidancesystemandroidautoapp.car

import android.content.Context
import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.core.content.ContextCompat
import com.example.riskavoidancesystemandroidautoapp.util.RASForegroundService

class RASSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        // We still get sharedPrefs, but we ignore the saved value for the initial screen
        return CarPickerScreen(carContext)
    }
}