package com.example.riskavoidancesystemandroidautoapp.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class RASCarAppService : CarAppService() {

    override fun onCreateSession(): Session {
        return RASSession()
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
}
