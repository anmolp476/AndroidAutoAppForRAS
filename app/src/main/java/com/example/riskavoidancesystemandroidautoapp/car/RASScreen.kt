package com.example.riskavoidancesystemandroidautoapp.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import com.example.riskavoidancesystemandroidautoapp.R

class RASScreen(carContext: CarContext) : Screen(carContext) {

    private var mqttRiskLevel = "MODERATE"
    private var mqttHazardType = "Distracted Driver"
    private var mqttDirection = "Back-Right"

    override fun onGetTemplate(): Template {
        val riskColor = when(mqttRiskLevel) {
            "SEVERE" -> CarColor.RED
            "MODERATE" -> CarColor.YELLOW // CAL has limited default colors; use YELLOW or Custom for Orange
            "MILD" -> CarColor.GREEN
            else -> CarColor.DEFAULT
        }

        val direction = when(mqttDirection) {
            "Front" -> R.drawable.arrow_front
            "Front-Right" -> R.drawable.arrow_frontright
            "Right" -> R.drawable.arrow_right
            "Back-Right" -> R.drawable.arrow_backright
            "Back" -> R.drawable.arrow_back
            "Back-Left" -> R.drawable.arrow_backleft
            "Left" -> R.drawable.arrow_left
            "Front-Left" -> R.drawable.arrow_frontleft
            else -> CarColor.DEFAULT
        }

        val carImage = CarIcon.Builder(
            IconCompat.createWithResource(carContext, direction as Int)
        )   .setTint(riskColor)
            .build()

        return PaneTemplate.Builder(
            Pane.Builder()
                .addRow(
                    Row.Builder()
                        .setTitle("$mqttRiskLevel RISK")
                        .addText("HAZARD: $mqttHazardType")
                        .addText("DIRECTION: $mqttDirection")
                        // Note: In some CAL versions, setImage on a Row
                        // always places it to the left of the text.
                        .setImage(carImage)
                        .build()
                )
                .addAction(
                    Action.Builder()
                        .setTitle("ACKNOWLEDGE")
                        .setBackgroundColor(riskColor)
                        .setOnClickListener {
                            mqttRiskLevel = "NO RISK" // Demo state change
                            mqttHazardType = "No Hazard"
                            mqttDirection = "No Hazard"
                            carContext.finishCarApp()
                        }
                        .build()
                )
                .build()
        ).setTitle("Integrated RAS").build()
    }
}