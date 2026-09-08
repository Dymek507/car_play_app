@echo off
REM Laczy adb z telefonem przez Wi-Fi, zeby build.bat / dhu.bat / logcat dzialaly bez kabla.
REM
REM   adb-wifi.bat          - polacz z telefonem (Android 11+, "Debugowanie bezprzewodowe")
REM   adb-wifi.bat pair     - pierwsze parowanie kodem (raz na komputer)
REM   adb-wifi.bat usb      - starsza metoda: telefon po kablu, przelacz adb na TCP 5555, odlacz kabel
REM   adb-wifi.bat off      - rozlacz wszystkie polaczenia Wi-Fi
set "ANDROID_HOME=%USERPROFILE%\Android\Sdk"
set "PATH=%ANDROID_HOME%\platform-tools;%PATH%"

if /i "%~1"=="pair" goto pair
if /i "%~1"=="usb"  goto usb
if /i "%~1"=="off"  goto off

:connect
echo Telefon: Ustawienia -^> Opcje programisty -^> Debugowanie bezprzewodowe (wlacz).
echo Na tym ekranie jest "Adres IP i port", np. 192.168.1.20:37123.
echo (Jesli komputer nie byl jeszcze sparowany, uruchom najpierw: adb-wifi.bat pair)
echo.
set /p ADDR=Adres IP:port: 
adb connect %ADDR%
goto status

:pair
echo Telefon: Debugowanie bezprzewodowe -^> "Sparuj urzadzenie za pomoca kodu parowania".
echo Pojawi sie 6-cyfrowy kod oraz adres IP:port (INNY port niz do laczenia).
echo.
set /p PAIRADDR=Adres IP:port do parowania: 
set /p CODE=Kod parowania: 
adb pair %PAIRADDR% %CODE%
if errorlevel 1 (
  echo Parowanie nie powiodlo sie. Sprawdz, czy PC i telefon sa w tej samej sieci Wi-Fi.
  exit /b 1
)
echo.
echo Sparowano. Teraz podaj adres z glownego ekranu "Debugowanie bezprzewodowe".
goto connect

:usb
echo Podlacz telefon kablem USB (debugowanie USB wlaczone), a potem nacisnij Enter.
pause >nul
adb devices
adb tcpip 5555
if errorlevel 1 exit /b 1
for /f "tokens=2" %%i in ('adb shell ip -f inet addr show wlan0 ^| findstr /r "inet "') do set IPCIDR=%%i
for /f "tokens=1 delims=/" %%i in ("%IPCIDR%") do set IP=%%i
if "%IP%"=="" (
  echo Nie udalo sie odczytac IP telefonu. Sprawdz w Ustawienia -^> Wi-Fi i uruchom: adb connect IP:5555
  exit /b 1
)
echo Mozesz odlaczyc kabel. Lacze z %IP%:5555 ...
adb connect %IP%:5555
goto status

:off
adb disconnect
goto status

:status
echo.
echo Podlaczone urzadzenia:
adb devices
echo.
echo Test: adb shell getprop ro.product.model
adb shell getprop ro.product.model
