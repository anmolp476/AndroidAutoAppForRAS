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

// 1. Create a data class to hold the state of EACH individual alert
data class HazardAlert(
    val risk: String,
    val hazard: String,
    val direction: String,
    val runnable: Runnable
)

class RASScreen(carContext: CarContext) : Screen(carContext) {

    // 2. Replace the single variables with a MutableList to track multiple alerts
    private val activeAlerts = mutableListOf<HazardAlert>()
    private val handler = Handler(Looper.getMainLooper())

    private val riskReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val riskLevel = intent.getStringExtra("risk")?.uppercase() ?: "MILD"
            val hazardType = intent.getStringExtra("behaviour") ?: "Stable"
            val direction = intent.getStringExtra("direction") ?: "Front"

            // Only track real dangers. MILD means no threat, so we ignore it and let the 10s timers run.
            if (riskLevel != "MILD") {
                lateinit var newAlert: HazardAlert

                // The runnable that will remove THIS specific alert after 10 seconds
                val removalRunnable = Runnable {
                    activeAlerts.remove(newAlert)
                    invalidate()
                }

                newAlert = HazardAlert(riskLevel, hazardType, direction, removalRunnable)

                // Add the new alert to the TOP of the list (index 0 is always newest)
                activeAlerts.add(0, newAlert)

                // Start the 10-second timer (10000 ms) for this specific alert
                handler.postDelayed(removalRunnable, 25000)

                // 🚨 CRITICAL: Android Auto crashes if a Pane has too many rows (driver distraction rule).
                // We cap the list at 3 active alerts. If a 4th comes in, we kill the oldest one early.
                if (activeAlerts.size > 3) {
                    val oldest = activeAlerts.removeLast()
                    handler.removeCallbacks(oldest.runnable) // Stop its timer so it doesn't trigger an error
                }

                // Redraw the screen to show the new stack
                invalidate()
            }
        }
    }

    init {
        val filter = IntentFilter("com.example.RAS_UPDATE")
        ContextCompat.registerReceiver(carContext, riskReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    // Helper function to keep onGetTemplate clean
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
            // THE SAFE STATE: No active alerts in the list
            val rowBuilder = Row.Builder()
                .setTitle("MILD RISK")
                .addText("HAZARD: Scanning...")
                .addText("DIRECTION: Front")
                .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.arrow_front))
                    .setTint(CarColor.DEFAULT).build())

            paneBuilder.addRow(rowBuilder.build())

        } else {
            // THE DANGER STATE: Loop through our active alerts
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

                val rowBuilder = Row.Builder()

                if (index == 0) {
                    // NEWEST ALERT (Top Row): Show full text details
                    rowBuilder.setTitle("${alert.risk} RISK")
                        .addText("HAZARD: ${alert.hazard}")
                        .addText("DIRECTION: ${alert.direction}")
                } else {
                    // OLDER ALERTS (Bottom Rows): Minimal UI
                    // Note: Android Auto requires every Row to have a Title
                    rowBuilder.setTitle("Previous Hazard: ${alert.direction}")
                }

                rowBuilder.setImage(carImage)
                paneBuilder.addRow(rowBuilder.build())
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