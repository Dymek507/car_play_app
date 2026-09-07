package com.example.autotest

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class CarTestService : CarAppService() {

    // Test app: accept any host (Android Auto, DHU, Automotive OS emulator).
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = CarTestSession()
}
