@echo off
REM Diagnostyka polaczenia telefonu z PC (USB lub Wi-Fi) i stanu aplikacji Auto Test.
REM Uruchom i wklej caly wynik.
setlocal
set "ANDROID_HOME=%USERPROFILE%\Android\Sdk"
set "PATH=%ANDROID_HOME%\platform-tools;%PATH%"
set "PKG=com.example.autotest"

echo ============================================
echo  1. adb
echo ============================================
where adb 2>nul
if errorlevel 1 (
  echo BLAD: brak adb w %ANDROID_HOME%\platform-tools
  echo Zainstaluj platform-tools albo popraw sciezke w tym skrypcie.
  goto end
)
adb version | findstr /i "version"

echo.
echo ============================================
echo  2. Podlaczone urzadzenia
echo ============================================
adb devices -l

for /f "skip=1 tokens=1,2" %%a in ('adb devices') do (
  if "%%b"=="device"       set "SERIAL=%%a"
  if "%%b"=="unauthorized" set "UNAUTH=%%a"
  if "%%b"=="offline"      set "OFFLINE=%%a"
)

if defined UNAUTH (
  echo.
  echo BLAD: urzadzenie %UNAUTH% jest "unauthorized".
  echo Na telefonie pojawil sie dialog "Zezwolic na debugowanie USB?" - zaakceptuj
  echo i zaznacz "Zawsze zezwalaj z tego komputera". Jesli dialogu nie ma:
  echo   Opcje programisty -^> "Cofnij autoryzacje debugowania USB", odlacz i podlacz kabel.
  goto end
)
if defined OFFLINE (
  echo.
  echo BLAD: urzadzenie %OFFLINE% jest "offline". Odlacz i podlacz kabel,
  echo albo uruchom: adb kill-server ^&^& adb start-server
  goto end
)
if not defined SERIAL (
  echo.
  echo BLAD: adb nie widzi zadnego urzadzenia. Sprawdz po kolei:
  echo   - kabel obsluguje dane, nie tylko ladowanie ^(oryginalny Samsung: OK^),
  echo   - Ustawienia -^> Opcje programisty -^> Debugowanie USB: WLACZONE,
  echo   - po podlaczeniu kabla: powiadomienie USB -^> tryb "Przesylanie plikow / MTP",
  echo   - inny port USB, najlepiej bezposrednio w plycie, bez huba,
  echo   - Windows: sterownik Samsung USB / Menedzer urzadzen bez zoltych wykrzyknikow.
  goto end
)

echo.
echo ============================================
echo  3. Telefon
echo ============================================
echo Serial:  %SERIAL%
for /f "delims=" %%i in ('adb -s %SERIAL% shell getprop ro.product.manufacturer') do echo Producent: %%i
for /f "delims=" %%i in ('adb -s %SERIAL% shell getprop ro.product.model')        do echo Model:     %%i
for /f "delims=" %%i in ('adb -s %SERIAL% shell getprop ro.build.version.release') do echo Android:   %%i
for /f "delims=" %%i in ('adb -s %SERIAL% shell getprop ro.build.version.sdk')     do echo API:       %%i

echo.
echo ============================================
echo  4. Aplikacja Auto Test (%PKG%)
echo ============================================
adb -s %SERIAL% shell pm list packages | findstr /c:"%PKG%" >nul
if errorlevel 1 (
  echo Nie zainstalowana. Zainstaluj: build.bat
  echo albo pobierz APK z https://github.com/Dymek507/car_play_app/releases/latest
) else (
  echo Zainstalowana:
  adb -s %SERIAL% shell dumpsys package %PKG% | findstr /i "versionCode versionName firstInstallTime lastUpdateTime"
  echo.
  echo Uwaga: jesli instalacja nowej wersji konczy sie INSTALL_FAILED_UPDATE_INCOMPATIBLE,
  echo stara wersja ma inny podpis - odinstaluj: adb uninstall %PKG%
)

echo.
echo ============================================
echo  5. Android Auto
echo ============================================
adb -s %SERIAL% shell pm list packages | findstr /c:"com.google.android.projection.gearhead" >nul
if errorlevel 1 (
  echo Android Auto NIE jest zainstalowane - zainstaluj ze Sklepu Play.
) else (
  echo Android Auto zainstalowane:
  adb -s %SERIAL% shell dumpsys package com.google.android.projection.gearhead | findstr /i "versionName"
)

echo.
echo ============================================
echo  6. Test zapisu/odczytu przez kabel
echo ============================================
adb -s %SERIAL% shell echo OK-shell
adb -s %SERIAL% shell date

echo.
echo Gotowe. Jesli wszystko powyzej jest OK:
echo   build.bat      - zbuduj i zainstaluj aplikacje
echo   dhu.bat        - symulator ekranu auta
echo   adb logcat -s CarApp CarAppService AutoTest

:end
endlocal
