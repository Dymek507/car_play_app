# Auto Test – testowa aplikacja Android Auto

Minimalna aplikacja Android Auto (Car App Library, kategoria IOT), która na ekranie auta
pokazuje, czy telefon połączył się z hostem Android Auto, jaki to host i jaki poziom API.
Na telefonie aktywność pokazuje status połączenia z autem na żywo (`CarConnection`).

## Budowanie

Toolchain jest w katalogu użytkownika (bez Android Studio):

- JDK 17: `%USERPROFILE%\Android\jdk\jdk-17.0.20.1+1`
- Android SDK: `%USERPROFILE%\Android\Sdk` (platform-tools, android-34, build-tools 34, DHU)

```
build.bat
```

Skrypt buduje `app\build\outputs\apk\debug\app-debug.apk` i, jeśli telefon jest w adb,
instaluje go.

## Konfiguracja telefonu (Galaxy Note 20 Ultra)

1. **Debugowanie USB**: Ustawienia → Informacje o telefonie → Informacje o oprogramowaniu →
   7× tap w „Numer kompilacji”, potem Ustawienia → Opcje programisty → Debugowanie USB.
2. **Android Auto w trybie dewelopera**: Ustawienia → Aplikacje → Android Auto → Ustawienia
   Android Auto (albo otwórz aplikację Android Auto) → zjedź na dół → 10× tap w „Wersja”.
3. Menu ⋮ → **Ustawienia dewelopera** → włącz **„Nieznane źródła”** (bez tego host nie pokaże
   aplikacji spoza Sklepu Play).
4. Zainstaluj APK (`build.bat` albo `adb install -r app-debug.apk`).

## Test w samochodzie

Podłącz telefon do auta (kabel / bezprzewodowo). W launcherze Android Auto pojawi się
„Auto Test”. Ekran pokazuje: status, pakiet hosta, poziom Car App API, godzinę startu sesji
i klikalny licznik. Ikona auta w prawym górnym rogu pokazuje toast.

Otwórz też aplikację na telefonie – status powinien zmienić się na
„POŁĄCZONO – Android Auto (projekcja)”.

## Test bez samochodu (Desktop Head Unit)

1. Telefon po USB, debugowanie USB włączone.
2. W Android Auto → Ustawienia dewelopera → **„Uruchom serwer głowicy”** (Start head unit server).
3. Na PC:

```
dhu.bat
```

Otworzy się okno symulujące ekran radia. Aplikacja „Auto Test” będzie w launcherze.

## Logi

```
%USERPROFILE%\Android\Sdk\platform-tools\adb.exe logcat -s CarApp CarAppService AutoTest
```
