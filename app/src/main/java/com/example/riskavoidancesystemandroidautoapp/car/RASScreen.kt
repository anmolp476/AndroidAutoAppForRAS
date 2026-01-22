package com.example.riskavoidancesystemandroidautoapp.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row

class RASScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate() =
        ListTemplate.Builder()
            .setTitle("Risk Avoidance System")
            .setSingleList(
                ItemList.Builder()
                    .addItem(
                        Row.Builder()
                            .setTitle("System Status")
                            .addText("RAS Connected")
                            .build()
                    )
                    .addItem(
                        Row.Builder()
                            .setTitle("Last Alert")
                            .addText("No active hazards")
                            .build()
                    )
                    .build()
            )
            .build()
}
