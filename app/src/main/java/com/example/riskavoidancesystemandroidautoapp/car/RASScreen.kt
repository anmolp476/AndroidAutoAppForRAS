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

            if (riskLevel!= "MILD") {
                lateinit var newAlert: HazardAlert

                val removalRunnable = Runnable {
                    activeAlerts.remove(newAlert)
                    invalidate()
                }

                newAlert = HazardAlert(riskLevel, behaviourList, direction, removalRunnable)
                activeAlerts.add(0, newAlert)
                handler.postDelayed(removalRunnable, 1000)

                if (activeAlerts.size > 3) {
                    val oldest = activeAlerts.removeLast()
                    handler.removeCallbacks(oldest.runnable)
                }

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

                val rowBuilder = Row.Builder()
                val combinedBehaviours = alert.behaviourList.joinToString(", ").uppercase()

                if (index == 0) {
                    rowBuilder.setTitle("${alert.risk} RISK")
                        .addText("HAZARD: $combinedBehaviours")
                        .addText("DIRECTION: ${alert.direction}")
                } else {
                    rowBuilder.setTitle("${alert.risk}: $combinedBehaviours - ${alert.direction}")
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