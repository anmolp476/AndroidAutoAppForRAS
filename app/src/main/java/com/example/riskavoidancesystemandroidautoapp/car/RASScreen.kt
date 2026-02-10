package com.example.riskavoidancesystemandroidautoapp.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import com.example.riskavoidancesystemandroidautoapp.R

class RASScreen(carContext: CarContext) : Screen(carContext) {

    private var currentRiskLevel = "SEVERE"
    private var hazardType = "Distracted Driver"
    private var direction = "Front-Left"

    override fun onGetTemplate(): Template {
        // FIX 1: Ensure you have an image in res/drawable/ic_car_top_view.xml (or .png)
        // If you don't have one yet, you can use android.R.drawable.ic_menu_compass for testing.
        val carImage = CarIcon.Builder(
            IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_compass)
        ).build()

        // FIX 2: Pane.Builder uses addRow(), not addItem()
        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("$currentRiskLevel RISK")
                    .addText("HAZARD: $hazardType")
                    .addText("DIRECTION: $direction")
                    .setImage(carImage)
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("ACKNOWLEDGE")
                    .setBackgroundColor(CarColor.RED)
                    .setOnClickListener {
                        // This refreshes the UI
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setHeaderAction(Action.APP_ICON)
            .setTitle("Integrated RAS")
            .build()
    }
}