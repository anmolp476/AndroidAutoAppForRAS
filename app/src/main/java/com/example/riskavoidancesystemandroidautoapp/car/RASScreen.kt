package com.example.riskavoidancesystemandroidautoapp.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*

class RASScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {

        val row = Row.Builder()
            .setTitle("No hazards detected")
            .addText("System standing by")
            .build()

        val pane = Pane.Builder()
            .addRow(row)
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Risk Avoidance System")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
