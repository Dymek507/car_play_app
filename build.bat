@echo off
REM Buduje debug APK i (jesli telefon jest podlaczony) instaluje go przez adb.
set "JAVA_HOME=%USERPROFILE%\Android\jdk\jdk-17.0.20.1+1"
set "ANDROID_HOME=%USERPROFILE%\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

cd /d "%~dp0"
call gradlew.bat assembleDebug --console=plain
if errorlevel 1 exit /b 1

echo.
echo APK: %~dp0app\build\outputs\apk\debug\app-debug.apk
echo.
adb devices
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
