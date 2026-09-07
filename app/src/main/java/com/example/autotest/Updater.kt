package com.example.autotest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Autoaktualizacja z GitHub Releases.
 *
 * Workflow w `.github/workflows/release.yml` publikuje po każdym pushu na `main` wydanie
 * z tagiem `v1.0.<versionCode>` i plikiem `.apk`. Aplikacja porównuje ostatni numer z tagu
 * ze swoim `BuildConfig.VERSION_CODE`, pobiera APK do katalogu cache i uruchamia
 * systemowy instalator. Dzięki wspólnemu kluczowi podpisu (autotest.jks) instalacja
 * odbywa się "w miejscu", bez odinstalowywania.
 */
object Updater {

    private const val TAG = "AutoTest"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val apkName: String,
        /** API URL assetu – działa też dla prywatnych repo (z tokenem). */
        val apkApiUrl: String,
        val notes: String,
    )

    sealed class State {
        object Idle : State()
        object Checking : State()
        object UpToDate : State()
        data class Available(val info: UpdateInfo) : State()
        data class Downloading(val info: UpdateInfo, val percent: Int) : State()
        data class Downloaded(val info: UpdateInfo, val file: File) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Idle)
    val state: LiveData<State> get() = _state

    @Volatile
    var lastCheckedAt: Long = 0L
        private set

    val currentVersionName: String get() = BuildConfig.VERSION_NAME
    val currentVersionCode: Int get() = BuildConfig.VERSION_CODE

    /** Sprawdza w tle, czy jest nowsza wersja. Wynik trafia do [state]. */
    fun checkAsync(force: Boolean = false) {
        val current = _state.value
        if (current is State.Checking || current is State.Downloading) return
        if (!force && current is State.Downloaded && current.file.exists()) return
        _state.value = State.Checking
        Thread({
            try {
                val info = fetchLatest()
                lastCheckedAt = System.currentTimeMillis()
                if (info != null && info.versionCode > currentVersionCode) {
                    Log.i(TAG, "Dostępna aktualizacja ${info.versionName} (code ${info.versionCode})")
                    _state.postValue(State.Available(info))
                } else {
                    Log.i(TAG, "Aplikacja aktualna ($currentVersionName)")
                    _state.postValue(State.UpToDate)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sprawdzanie aktualizacji nie powiodło się", e)
                _state.postValue(State.Error(e.message ?: e.javaClass.simpleName))
            }
        }, "update-check").start()
    }

    /** Pobiera APK w tle. Po zakończeniu stan przechodzi w [State.Downloaded]. */
    fun downloadAsync(context: Context, info: UpdateInfo) {
        if (_state.value is State.Downloading) return
        val appContext = context.applicationContext
        _state.value = State.Downloading(info, 0)
        Thread({
            try {
                val file = download(appContext, info) { percent ->
                    _state.postValue(State.Downloading(info, percent))
                }
                _state.postValue(State.Downloaded(info, file))
            } catch (e: Exception) {
                Log.w(TAG, "Pobieranie aktualizacji nie powiodło się", e)
                _state.postValue(State.Error("Pobieranie: " + (e.message ?: e.javaClass.simpleName)))
            }
        }, "update-download").start()
    }

    /** Czy aplikacja może uruchomić instalację z nieznanego źródła (Android 8+). */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** Intent otwierający ustawienie „Instalowanie nieznanych aplikacji” dla tej aplikacji. */
    fun unknownSourcesSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** Uruchamia systemowy instalator dla pobranego APK. */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun reset() {
        if (_state.value !is State.Downloading) _state.value = State.Idle
    }

    // ---------------------------------------------------------------- sieć

    private fun openConnection(url: String, accept: String, auth: Boolean): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("Accept", accept)
        conn.setRequestProperty("User-Agent", "AutoTest-Updater/${BuildConfig.VERSION_NAME}")
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        if (auth && BuildConfig.UPDATE_TOKEN.isNotEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.UPDATE_TOKEN}")
        }
        return conn
    }

    /**
     * Otwiera połączenie i ręcznie podąża za przekierowaniami. Nagłówek Authorization
     * jest wysyłany tylko do api.github.com – nie może trafić do S3, z którego GitHub
     * serwuje pliki (odrzuciłby żądanie z dwoma sposobami uwierzytelnienia).
     */
    private fun connectFollowingRedirects(url: String, accept: String): HttpURLConnection {
        var current = url
        repeat(5) {
            val auth = current.startsWith("https://api.github.com/")
            val conn = openConnection(current, accept, auth)
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                    ?: throw IOException("HTTP $code bez nagłówka Location")
                conn.disconnect()
                current = URL(URL(current), location).toString()
            } else {
                return conn
            }
        }
        throw IOException("Za dużo przekierowań")
    }

    private fun fetchLatest(): UpdateInfo? {
        val url = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest"
        val conn = connectFollowingRedirects(url, "application/vnd.github+json")
        try {
            when (conn.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    // Brak wydań albo repo prywatne bez tokena.
                    if (BuildConfig.UPDATE_TOKEN.isEmpty()) {
                        throw IOException("Brak wydań w ${BuildConfig.UPDATE_REPO} (repo prywatne wymaga tokena)")
                    }
                    return null
                }
                else -> throw IOException("GitHub API: HTTP ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            return parseRelease(json)
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseRelease(json: JSONObject): UpdateInfo? {
        val tag = json.optString("tag_name")
        val versionCode = Regex("(\\d+)\\s*$").find(tag)?.groupValues?.get(1)?.toIntOrNull()
            ?: throw IOException("Tag wydania „$tag” nie kończy się numerem wersji")
        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                return UpdateInfo(
                    versionCode = versionCode,
                    versionName = tag.removePrefix("v"),
                    apkName = name,
                    apkApiUrl = asset.getString("url"),
                    notes = json.optString("body"),
                )
            }
        }
        return null
    }

    private fun download(context: Context, info: UpdateInfo, onProgress: (Int) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "update-${info.versionCode}.apk")
        val tmp = File(dir, target.name + ".part")

        val conn = connectFollowingRedirects(info.apkApiUrl, "application/octet-stream")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${conn.responseCode}")
            }
            val total = conn.contentLengthLong
            var read = 0L
            var lastPercent = -1
            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        read += n
                        if (total > 0) {
                            val percent = (read * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            if (total > 0 && read != total) throw IOException("Niekompletny plik ($read/$total B)")
        } finally {
            conn.disconnect()
        }
        if (!tmp.renameTo(target)) throw IOException("Nie można zapisać $target")
        return target
    }
}
