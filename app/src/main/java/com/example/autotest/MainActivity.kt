package com.example.autotest

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.car.app.connection.CarConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val log = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)
        val logView = findViewById<TextView>(R.id.log)

        CarConnection(this).type.observe(this) { type ->
            val text = when (type) {
                CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> "NIE POŁĄCZONO"
                CarConnection.CONNECTION_TYPE_PROJECTION -> "POŁĄCZONO – Android Auto (projekcja)"
                CarConnection.CONNECTION_TYPE_NATIVE -> "POŁĄCZONO – Android Automotive OS"
                else -> "Nieznany typ ($type)"
            }
            status.text = "Status: $text"
            val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            log.insert(0, "$ts  $text\n")
            logView.text = log.toString()
        }
    }
}
