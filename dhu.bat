@echo off
REM Uruchamia Desktop Head Unit (symulator ekranu auta) polaczony z telefonem przez USB.
REM Wymaga: telefon podlaczony po USB z debugowaniem USB, Android Auto w trybie dewelopera,
REM na telefonie: menu Android Auto -> "Uruchom serwer glowicy" (Start head unit server).
set "ANDROID_HOME=%USERPROFILE%\Android\Sdk"
set "PATH=%ANDROID_HOME%\platform-tools;%PATH%"

adb forward tcp:5277 tcp:5277
if errorlevel 1 (
  echo Brak telefonu w adb. Podlacz telefon i wlacz debugowanie USB.
  exit /b 1
)
"%ANDROID_HOME%\extras\google\auto\desktop-head-unit.exe"
