import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// versionCode = liczba commitów w git (rośnie automatycznie z każdym commitem).
// Można nadpisać: -PversionCode=123 albo zmienną środowiskową AUTOTEST_VERSION_CODE.
fun gitCommitCount(): Int {
    val forced = (project.findProperty("versionCode") as String?)
        ?: System.getenv("AUTOTEST_VERSION_CODE")
    forced?.toIntOrNull()?.let { return it }
    return try {
        val out = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = out
            isIgnoreExitValue = true
        }
        out.toString().trim().toIntOrNull() ?: 1
    } catch (e: Exception) {
        1
    }
}

val appVersionCode = gitCommitCount()
val appVersionName = "1.0.$appVersionCode"

// Repo GitHub, z którego aplikacja pobiera aktualizacje (Releases).
val updateRepo = (project.findProperty("updateRepo") as String?)
    ?: System.getenv("UPDATE_REPO")
    ?: "Dymek507/car_play_app"

// Opcjonalny token GitHub (fine-grained PAT, "Contents: read") – potrzebny tylko, gdy repo jest prywatne.
// Przekazywany z CI jako secret UPDATE_TOKEN; lokalnie: -PupdateToken=... Nigdy nie commituj tokena.
val updateToken = (project.findProperty("updateToken") as String?)
    ?: System.getenv("UPDATE_TOKEN")
    ?: ""

// Wspólny klucz podpisu dla buildów lokalnych i z CI – bez tego Android nie pozwoli
// zaktualizować aplikacji "w miejscu" (inny podpis = trzeba odinstalować).
val keystoreFile = file(System.getenv("AUTOTEST_KEYSTORE") ?: "autotest.jks")
val keystorePassword = System.getenv("AUTOTEST_KEYSTORE_PASSWORD") ?: "autotest123"
val keystoreAlias = System.getenv("AUTOTEST_KEY_ALIAS") ?: "autotest"
val keystoreKeyPassword = System.getenv("AUTOTEST_KEY_PASSWORD") ?: keystorePassword

android {
    namespace = "com.example.autotest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.autotest"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "UPDATE_REPO", "\"$updateRepo\"")
        buildConfigField("String", "UPDATE_TOKEN", "\"${updateToken.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    signingConfigs {
        create("shared") {
            storeFile = keystoreFile
            storePassword = keystorePassword
            keyAlias = keystoreAlias
            keyPassword = keystoreKeyPassword
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.core:core-ktx:1.13.1")
}
