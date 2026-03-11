package com.example.riskavoidancesystemandroidautoapp.ui

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.example.riskavoidancesystemandroidautoapp.car.RASScreen
import com.example.riskavoidancesystemandroidautoapp.mqtt.MqttManager
import com.example.riskavoidancesystemandroidautoapp.mqtt.InfoPacket
import com.example.riskavoidancesystemandroidautoapp.util.RASForegroundService

class VehicleSetupScreen(carContext: CarContext, private val carMacId: String) : Screen(carContext) {

    private var ssid = ""
    private var pass = ""
    private var make = ""
    private var model = ""
    private var year = ""

    override fun onGetTemplate(): Template {
        // We use ItemList instead of Pane for interactive, clickable rows
        val listBuilder = ItemList.Builder()

        // 1. HOTSPOT SSID
        listBuilder.addItem(Row.Builder()
            .setTitle("Hotspot SSID: ${ssid.ifEmpty { "Tap to Enter" }}")
            .setOnClickListener {
                screenManager.push(SearchInputScreen(carContext, "Enter SSID") { result ->
                    ssid = result; invalidate()
                })
            }.build())

        // 2. HOTSPOT PASSWORD
        listBuilder.addItem(Row.Builder()
            .setTitle("Hotspot Pass: ${if (pass.isEmpty()) "Tap to Enter" else "********"}")
            .setOnClickListener {
                screenManager.push(SearchInputScreen(carContext, "Enter Password") { result ->
                    pass = result; invalidate()
                })
            }.build())

        // 3. VEHICLE MAKE
        listBuilder.addItem(Row.Builder()
            .setTitle("Vehicle Make: ${make.ifEmpty { "Tap to Enter" }}")
            .setOnClickListener {
                screenManager.push(SearchInputScreen(carContext, "Enter Make (e.g. Toyota)") { result ->
                    make = result; invalidate()
                })
            }.build())

        // 4. VEHICLE MODEL
        listBuilder.addItem(Row.Builder()
            .setTitle("Vehicle Model: ${model.ifEmpty { "Tap to Enter" }}")
            .setOnClickListener {
                screenManager.push(SearchInputScreen(carContext, "Enter Model (e.g. Camry)") { result ->
                    model = result; invalidate()
                })
            }.build())

        // 5. VEHICLE YEAR
        listBuilder.addItem(Row.Builder()
            .setTitle("Vehicle Year: ${year.ifEmpty { "Tap to Enter" }}")
            .setOnClickListener {
                screenManager.push(SearchInputScreen(carContext, "Enter Year (e.g. 2024)") { result ->
                    year = result; invalidate()
                })
            }.build())

        // 6. THE SYNC ACTION (Transformed into an executable list item)
        listBuilder.addItem(Row.Builder()
            .setTitle("➡ SEND TO PI (Start System)")
            .setOnClickListener {
                // Enforce that critical fields are present before execution
                if (ssid.isNotEmpty() && pass.isNotEmpty() && make.isNotEmpty() && model.isNotEmpty()) {

                    // Package the data for the background service
                    val serviceIntent = Intent(carContext, RASForegroundService::class.java).apply {
                        putExtra("MAC_ID", carMacId)
                        putExtra("SSID", ssid)
                        putExtra("PASS", pass)
                        putExtra("MAKE", make)
                        putExtra("MODEL", model)
                        putExtra("YEAR", year)
                    }

                    // Launch the Service with the fresh configuration
                    androidx.core.content.ContextCompat.startForegroundService(carContext, serviceIntent)

                    // Move to the Dashboard
                    screenManager.push(RASScreen(carContext))
                }
            }
            .build())

        // Construct and return the ListTemplate
        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle("Vehicle Configuration")
            .setHeaderAction(Action.BACK)
            .build()
    }
}