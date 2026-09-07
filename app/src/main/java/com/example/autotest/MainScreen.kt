package com.example.autotest

import android.os.Build
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainScreen(carContext: CarContext) : Screen(carContext) {

    private var clicks = 0
    private val startedAt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    override fun onGetTemplate(): Template {
        val host = carContext.hostInfo
        val hostName = host?.packageName ?: "nieznany"
        val hostUid = host?.uid?.toString() ?: "-"

        val list = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Status: POŁĄCZONO")
                    .addText("Ekran renderowany przez hosta Android Auto")
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Host")
                    .addText("$hostName (uid $hostUid)")
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Car App API level")
                    .addText("Host: ${carContext.carAppApiLevel} | Telefon: Android ${Build.VERSION.RELEASE} (${Build.MODEL})")
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Sesja uruchomiona")
                    .addText(startedAt)
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Licznik kliknięć: $clicks")
                    .addText("Dotknij, aby zwiększyć")
                    .setOnClickListener {
                        clicks++
                        invalidate()
                    }
                    .build()
            )
            .build()

        val toastAction = Action.Builder()
            .setIcon(
                CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car)).build()
            )
            .setOnClickListener {
                CarToast.makeText(carContext, "Działa! Kliknięć: $clicks", CarToast.LENGTH_SHORT).show()
            }
            .build()

        return ListTemplate.Builder()
            .setTitle("Auto Test")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list)
            .setActionStrip(ActionStrip.Builder().addAction(toastAction).build())
            .build()
    }
}
