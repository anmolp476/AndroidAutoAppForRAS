package com.example.riskavoidancesystemandroidautoapp.car

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.riskavoidancesystemandroidautoapp.util.RASForegroundService

class CarPickerScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        //we need to check perms for devices that dont use the same version as my phone
        val bluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }

        val hasPermission = ContextCompat.checkSelfPermission(carContext, bluetoothPermission) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return MessageTemplate.Builder("Permission Required")
                .setHeader(Header.Builder()
                    .setTitle("Setup Required")
                    .setStartHeaderAction(Action.APP_ICON)
                    .build())
                .setDebugMessage("Please grant Bluetooth permissions on your phone to continue.")
                .build()
        }

        //get all the currently connected devices
        val bluetoothManager = carContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val pairedDevices = bluetoothManager.adapter?.bondedDevices

        val listBuilder = ItemList.Builder()

        if (pairedDevices.isNullOrEmpty()) {
            return MessageTemplate.Builder("No Devices Found")
                .setHeader(Header.Builder()
                    .setTitle("Pairing Needed")
                    .setStartHeaderAction(Action.APP_ICON)
                    .build())
                .setDebugMessage("Please pair your phone with your car's Bluetooth in Android Settings first.")
                .build()
        }

        //populate the list with all the devices
        pairedDevices.forEach { device ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(device.name ?: "Unknown Device")
                    .addText("MAC: ${device.address}")
                    .setOnClickListener { saveCarId(device.address) }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(Header.Builder()
                .setTitle("Select Your Car")
                .setStartHeaderAction(Action.APP_ICON)
                .build())
            .build()
    }

    private fun saveCarId(macAddress: String) {
        val sharedPref = carContext.getSharedPreferences("VehiclePrefs", Context.MODE_PRIVATE)

        sharedPref.edit {
            // Clear old data first to avoid topic 'ghosting'
            clear()
            putString("CAR_ID_MAC", macAddress)
        }

        // Launch the Service with the fresh MAC ID
        val serviceIntent = Intent(carContext, RASForegroundService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(carContext, serviceIntent)

        screenManager.push(RASScreen(carContext))
    }
}