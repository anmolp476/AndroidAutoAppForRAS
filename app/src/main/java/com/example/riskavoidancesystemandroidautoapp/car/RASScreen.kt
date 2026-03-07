package com.example.riskavoidancesystemandroidautoapp.car

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.riskavoidancesystemandroidautoapp.R
import androidx.core.content.edit

class RASScreen(carContext: CarContext) : Screen(carContext) {

    private var mqttRiskLevel = "MILD"
    private var mqttHazardType = "Scanning..."
    private var mqttDirection = "Front"

    // 1. Define the Receiver to catch data from the Foreground Service
    private val riskReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mqttRiskLevel = intent.getStringExtra("risk")?.uppercase() ?: "MILD"
            mqttHazardType = intent.getStringExtra("behaviour") ?: "Stable"
            mqttDirection = intent.getStringExtra("direction") ?: "Front"

            // This triggers the car head unit to call onGetTemplate() again
            invalidate()
        }
    }

    init {
        // Register the receiver when the screen is created
        val filter = IntentFilter("com.example.RAS_UPDATE")
        ContextCompat.registerReceiver(carContext, riskReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onGetTemplate(): Template {
        // Your existing color logic remains identical
        val riskColor = when(mqttRiskLevel) {
            "HIGH" -> CarColor.RED
            "MEDIUM" -> CarColor.YELLOW
            "LOW" -> CarColor.GREEN
            else -> CarColor.DEFAULT
        }

        // Your existing directionRes mapping remains identical
        val directionRes = when(mqttDirection) {
            "Front" -> R.drawable.arrow_front
            "Right" -> R.drawable.arrow_right
            "Left" -> R.drawable.arrow_left
            "Back" -> R.drawable.arrow_back
            "Back-Left" -> R.drawable.arrow_backleft
            "Back-Right" -> R.drawable.arrow_backright
            "Front-Right" -> R.drawable.arrow_frontright
            "Front-Left" -> R.drawable.arrow_frontleft
            else -> R.drawable.arrow_front
        }

        val paneBuilder = Pane.Builder()
        val rowBuilder = Row.Builder()
            .setTitle("$mqttRiskLevel RISK")
            .addText("HAZARD: $mqttHazardType")
            .addText("DIRECTION: $mqttDirection")

        directionRes.let {
            val carImage = CarIcon.Builder(IconCompat.createWithResource(carContext, it))
                .setTint(riskColor)
                .build()
            rowBuilder.setImage(carImage)
        }

        // Rest of your Pane and Action building code...
        return PaneTemplate.Builder(paneBuilder.addRow(rowBuilder.build()).build())
            .setHeader(Header.Builder().setTitle("Integrated RAS").build())
            .build()
    }

    private fun resetCarId() {
        val sharedPref = carContext.getSharedPreferences("VehiclePrefs", Context.MODE_PRIVATE)
        sharedPref.edit { remove("CAR_ID_MAC") }
        screenManager.popToRoot()
    }
}