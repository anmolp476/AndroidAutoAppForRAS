package com.example.riskavoidancesystemandroidautoapp.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class RASSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return RASScreen(carContext)
    }
}
