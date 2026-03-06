package com.example.riskavoidancesystemandroidautoapp.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import com.example.riskavoidancesystemandroidautoapp.R
import android.content.Context
import androidx.core.content.edit

class RASScreen(carContext: CarContext) : Screen(carContext) {

    private var mqttRiskLevel = "MODERATE"
    private var mqttHazardType = "Distracted Driver"
    private var mqttDirection = "Back-Right"

    override fun onGetTemplate(): Template {
        val riskColor = when(mqttRiskLevel) {
            "SEVERE" -> CarColor.RED
            "MODERATE" -> CarColor.YELLOW
            "MILD" -> CarColor.GREEN
            else -> CarColor.DEFAULT
        }

        // Map MQTT direction to local drawable resources
        val directionRes = when(mqttDirection) {
            "Front" -> R.drawable.arrow_front
            "Front-Right" -> R.drawable.arrow_frontright
            "Right" -> R.drawable.arrow_right
            "Back-Right" -> R.drawable.arrow_backright
            "Back" -> R.drawable.arrow_back
            "Back-Left" -> R.drawable.arrow_backleft
            "Left" -> R.drawable.arrow_left
            "Front-Left" -> R.drawable.arrow_frontleft
            else -> null
        }

        val paneBuilder = Pane.Builder()

        val rowBuilder = Row.Builder()
            .setTitle("$mqttRiskLevel RISK")
            .addText("HAZARD: $mqttHazardType")
            .addText("DIRECTION: $mqttDirection")

        // Set the Icon if it exists
        directionRes?.let {
            val carImage = CarIcon.Builder(IconCompat.createWithResource(carContext, it))
                .setTint(riskColor)
                .build()
            rowBuilder.setImage(carImage)
        }

        paneBuilder.addRow(rowBuilder.build())
            .addAction(
                Action.Builder()
                    .setTitle("ACKNOWLEDGE")
                    .setBackgroundColor(riskColor)
                    .setOnClickListener {
                        // Demo reset logic
                        carContext.finishCarApp()
                    }
                    .build()
            )
            // Added a "RESET ID" button for your testing
            .addAction(
                Action.Builder()
                    .setTitle("RESET CAR ID")
                    .setOnClickListener {
                        resetCarId()
                    }
                    .build()
            )

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(Header.Builder()
                .setTitle("Integrated RAS")
                .setStartHeaderAction(Action.APP_ICON)
                .build())
            .build()
    }

    private fun resetCarId() {
        val sharedPref = carContext.getSharedPreferences("VehiclePrefs", Context.MODE_PRIVATE)
        sharedPref.edit { remove("CAR_ID_MAC") }
        screenManager.popToRoot() // Go back to the Picker
    }
}