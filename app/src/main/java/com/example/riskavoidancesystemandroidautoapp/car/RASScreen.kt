package com.example.riskavoidancesystemandroidautoapp.car

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.riskavoidancesystemandroidautoapp.R
import androidx.core.content.edit

data class HazardAlert(
    val risk: String,
    val behaviourList: List<String>,
    val direction: String,
    val vehicleDetails: String, // 🚨 Added this field
    val runnable: Runnable
)

class RASScreen(carContext: CarContext) : Screen(carContext) {

    private val activeAlerts = mutableListOf<HazardAlert>()
    private val handler = Handler(Looper.getMainLooper())

    private val riskReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val riskLevel = intent.getStringExtra("risk")?.uppercase()?: "MILD"
            val behaviourList = intent.getStringArrayListExtra("behaviours")?: arrayListOf("STABLE")
            val direction = intent.getStringExtra("direction")?: "Front"
            val carInfo = "${intent.getStringExtra("color")?: ""} ${intent.getStringExtra("make")?: ""} ${intent.getStringExtra("model")?: ""}".trim()

            if (riskLevel!= "MILD") {
                // 🚨 FIX FOR STUCK ARROW: Find the car by its details
                val existing = activeAlerts.find { it.vehicleDetails == carInfo }

                if (existing!= null) {
                    // If car is already showing, swap it with the new direction data
                    activeAlerts.remove(existing)
                    handler.removeCallbacks(existing.runnable)

                    val updated = HazardAlert(riskLevel, behaviourList, direction, carInfo, existing.runnable)
                    activeAlerts.add(0, updated)
                    handler.postDelayed(updated.runnable, 10000)

                    // Triggers redraw to physically move the arrow
                    invalidate()
                    return
                }

                // Normal logic for brand new hazards
                lateinit var newAlert: HazardAlert
                val removalRunnable = Runnable { activeAlerts.remove(newAlert); invalidate() }
                newAlert = HazardAlert(riskLevel, behaviourList, direction, carInfo, removalRunnable)
                activeAlerts.add(0, newAlert)
                handler.postDelayed(removalRunnable, 10000)

                if (activeAlerts.size > 2) { activeAlerts.removeLast() }
                invalidate()
            }
        }
    }

    init {
        val filter = IntentFilter("com.example.RAS_UPDATE")
        ContextCompat.registerReceiver(carContext, riskReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun getArrowResource(direction: String): Int {
        return when(direction) {
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
    }

    override fun onGetTemplate(): Template {
        val paneBuilder = Pane.Builder()

        if (activeAlerts.isEmpty()) {
            val rowBuilder = Row.Builder()
                .setTitle("MILD RISK")
                .addText("HAZARD: Scanning...")
                .addText("DIRECTION: Front")
                .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.arrow_front))
                    .setTint(CarColor.DEFAULT).build())

            paneBuilder.addRow(rowBuilder.build())

        } else {
            activeAlerts.forEachIndexed { index, alert ->

                val riskColor = when(alert.risk) {
                    "HIGH" -> CarColor.RED
                    "MEDIUM" -> CarColor.YELLOW
                    "LOW" -> CarColor.GREEN
                    else -> CarColor.DEFAULT
                }

                val carImage = CarIcon.Builder(IconCompat.createWithResource(carContext, getArrowResource(alert.direction)))
                    .setTint(riskColor)
                    .build()

                // Inside your activeAlerts.forEachIndexed loop in onGetTemplate():
                val combinedBehaviours = alert.behaviourList.joinToString(", ").uppercase()

// Define the hazard row
                val rowBuilder = Row.Builder()
                    .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, getArrowResource(alert.direction)))
                        .setTint(when(alert.risk) {
                            "HIGH" -> CarColor.RED
                            "MEDIUM" -> CarColor.YELLOW
                            "LOW" -> CarColor.GREEN
                            else -> CarColor.DEFAULT
                        }).build())

                if (index == 0) {
                    // NEWEST ALERT: Uses 2 rows total to show the car details separately
                    rowBuilder.setTitle("${alert.risk} RISK")
                        .addText("HAZARD: $combinedBehaviours")
                        .addText("DIRECTION: ${alert.direction}")
                    paneBuilder.addRow(rowBuilder.build())

                    // 🚨 RESTORED CAR ROW: Adding the car details as its own dedicated row
                    val carRow = Row.Builder()
                        .setTitle("Car: ${alert.vehicleDetails}")
                        .build()
                    paneBuilder.addRow(carRow)
                } else {
                    // OLDER ALERTS: 1 row total to avoid the 4-row crash limit
                    rowBuilder.setTitle("${alert.risk}: $combinedBehaviours - ${alert.direction}")
                    paneBuilder.addRow(rowBuilder.build())
                }
            }
        }

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(Header.Builder().setTitle("Integrated RAS").build())
            .build()
    }

    private fun resetCarId() {
        val sharedPref = carContext.getSharedPreferences("VehiclePrefs", Context.MODE_PRIVATE)
        sharedPref.edit { remove("CAR_ID_MAC") }
        screenManager.popToRoot()
    }
}