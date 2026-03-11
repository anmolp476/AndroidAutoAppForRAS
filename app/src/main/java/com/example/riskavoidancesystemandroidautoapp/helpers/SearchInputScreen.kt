package com.example.riskavoidancesystemandroidautoapp.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template

class SearchInputScreen(
    carContext: CarContext,
    private val hint: String,
    private val onResult: (String) -> Unit
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        // Correct Callback structure
        val callback = object : SearchTemplate.SearchCallback {
            override fun onSearchSubmitted(searchText: String) {
                onResult(searchText)
                screenManager.pop()
            }

            override fun onSearchTextChanged(searchText: String) {
                // Not needed for simple input
            }
        }

        // Corrected Method: setSearchHint instead of setHint
        return SearchTemplate.Builder(callback)
            .setSearchHint(hint) // FIXED THIS LINE
            .setShowKeyboardByDefault(true)
            .setHeaderAction(Action.BACK)
            .build()
    }
}