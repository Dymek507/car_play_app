package com.example.autotest

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class CarTestSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = MainScreen(carContext)
}
