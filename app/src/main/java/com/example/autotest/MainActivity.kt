package com.example.autotest

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.car.app.connection.CarConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val log = StringBuilder()

    private lateinit var updateStatus: TextView
    private lateinit var updateButton: Button
    private lateinit var updateProgress: ProgressBar

    /** Aktualizacja, o której już zapytaliśmy w dialogu – żeby nie pytać w kółko. */
    private var promptedVersionCode = -1

    /** Wysłaliśmy użytkownika do ustawień „nieznane źródła” – po powrocie od razu instalujemy. */
    private var awaitingInstallPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)
        val logView = findViewById<TextView>(R.id.log)
        val version = findViewById<TextView>(R.id.version)
        updateStatus = findViewById(R.id.update_status)
        updateButton = findViewById(R.id.update_button)
        updateProgress = findViewById(R.id.update_progress)

        version.text = getString(R.string.version_fmt, Updater.currentVersionName, Updater.currentVersionCode)

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

        Updater.state.observe(this) { render(it) }
        updateButton.setOnClickListener { onUpdateButton(Updater.state.value) }

        // Automatyczne sprawdzenie przy każdym uruchomieniu aplikacji.
        Updater.checkAsync()
    }

    private fun render(state: Updater.State?) {
        updateProgress.visibility = View.GONE
        updateButton.isEnabled = true
        when (state) {
            null, Updater.State.Idle -> {
                updateStatus.text = getString(R.string.update_idle)
                updateButton.text = getString(R.string.update_check)
            }
            Updater.State.Checking -> {
                updateStatus.text = getString(R.string.update_checking)
                updateButton.text = getString(R.string.update_check)
                updateButton.isEnabled = false
            }
            Updater.State.UpToDate -> {
                updateStatus.text = getString(R.string.update_up_to_date)
                updateButton.text = getString(R.string.update_check)
            }
            is Updater.State.Available -> {
                updateStatus.text = getString(R.string.update_available, state.info.versionName)
                updateButton.text = getString(R.string.update_download)
                if (promptedVersionCode != state.info.versionCode) {
                    promptedVersionCode = state.info.versionCode
                    askToDownload(state.info)
                }
            }
            is Updater.State.Downloading -> {
                updateStatus.text = getString(R.string.update_downloading, state.info.versionName, state.percent)
                updateButton.text = getString(R.string.update_download)
                updateButton.isEnabled = false
                updateProgress.visibility = View.VISIBLE
                updateProgress.progress = state.percent
            }
            is Updater.State.Downloaded -> {
                updateStatus.text = getString(R.string.update_downloaded, state.info.versionName)
                updateButton.text = getString(R.string.update_install)
            }
            is Updater.State.Error -> {
                updateStatus.text = getString(R.string.update_error, state.message)
                updateButton.text = getString(R.string.update_check)
            }
        }
    }

    private fun onUpdateButton(state: Updater.State?) {
        when (state) {
            is Updater.State.Available -> Updater.downloadAsync(this, state.info)
            is Updater.State.Downloaded -> tryInstall(state)
            else -> Updater.checkAsync(force = true)
        }
    }

    private fun askToDownload(info: Updater.UpdateInfo) {
        val notes = info.notes.trim().takeIf { it.isNotEmpty() }?.let { "\n\n$it" } ?: ""
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_dialog_title, info.versionName))
            .setMessage(getString(R.string.update_dialog_msg, Updater.currentVersionName, info.versionName) + notes)
            .setPositiveButton(R.string.update_download) { _, _ -> Updater.downloadAsync(this, info) }
            .setNegativeButton(R.string.update_later, null)
            .show()
    }

    private fun tryInstall(state: Updater.State.Downloaded) {
        if (!state.file.exists()) {
            Toast.makeText(this, R.string.update_file_missing, Toast.LENGTH_SHORT).show()
            Updater.reset()
            Updater.checkAsync(force = true)
            return
        }
        if (!Updater.canInstall(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.update_perm_title)
                .setMessage(R.string.update_perm_msg)
                .setPositiveButton(R.string.update_perm_open) { _, _ ->
                    awaitingInstallPermission = true
                    startActivity(Updater.unknownSourcesSettingsIntent(this))
                }
                .setNegativeButton(R.string.update_later, null)
                .show()
            return
        }
        Updater.install(this, state.file)
    }

    override fun onResume() {
        super.onResume()
        // Po powrocie z ustawień „Nieznane źródła” od razu uruchamiamy instalator.
        if (awaitingInstallPermission) {
            awaitingInstallPermission = false
            val state = Updater.state.value
            if (state is Updater.State.Downloaded && Updater.canInstall(this) && state.file.exists()) {
                Updater.install(this, state.file)
            }
        }
    }
}
