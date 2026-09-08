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
instaluje go. Numer wersji (`versionCode`) to liczba commitów w git – `git` musi być w PATH,
inaczej build użyje wersji 1.

## Konfiguracja telefonu (Galaxy Note 20 Ultra)

1. **Debugowanie USB**: Ustawienia → Informacje o telefonie → Informacje o oprogramowaniu →
   7× tap w „Numer kompilacji”, potem Ustawienia → Opcje programisty → Debugowanie USB.
2. **Android Auto w trybie dewelopera**: Ustawienia → Aplikacje → Android Auto → Ustawienia
   Android Auto (albo otwórz aplikację Android Auto) → zjedź na dół → 10× tap w „Wersja”.
3. Menu ⋮ → **Ustawienia dewelopera** → włącz **„Nieznane źródła”** (bez tego host nie pokaże
   aplikacji spoza Sklepu Play).
4. Zainstaluj APK (`build.bat` albo `adb install -r app-debug.apk`).

## Debugowanie bezprzewodowe (adb przez Wi‑Fi)

Po skonfigurowaniu `build.bat`, `dhu.bat` i `logcat` działają bez kabla – adb nie rozróżnia,
czy telefon jest po USB, czy po Wi‑Fi. Telefon i PC muszą być w tej samej sieci.

Android 11+ (Note 20 Ultra ma Androida 13):

1. Telefon: Ustawienia → Opcje programisty → **Debugowanie bezprzewodowe** → włącz.
2. Pierwszy raz na danym PC: na telefonie **„Sparuj urządzenie za pomocą kodu parowania”**,
   na PC `adb-wifi.bat pair` i przepisz adres IP:port oraz 6‑cyfrowy kod z telefonu.
3. Potem (i przy każdym kolejnym połączeniu): `adb-wifi.bat` i przepisz adres IP:port
   z głównego ekranu „Debugowanie bezprzewodowe” (port jest inny niż przy parowaniu
   i zmienia się po restarcie telefonu).
4. `adb devices` powinno pokazać telefon jako `192.168.x.x:port  device`.

Starsza metoda (dowolny Android, wymaga kabla na start): `adb-wifi.bat usb` – przełącza adb
na porcie 5555 i łączy się po IP telefonu; po tym kabel można odłączyć.

Rozłączenie: `adb-wifi.bat off`. Jeśli połączenie „znika”, najczęstsze przyczyny to
oszczędzanie baterii dla Wi‑Fi, zmiana portu po restarcie albo różne sieci (np. gość / 5 GHz z izolacją klientów).

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

## Autoaktualizacja (bez ręcznego wgrywania APK)

Aplikacja sama sprawdza, czy na GitHubie jest nowsza wersja, pobiera ją i uruchamia
systemowy instalator – nie trzeba za każdym razem podpinać telefonu i wgrywać pliku.

Jak to działa:

1. Każdy push na `main` uruchamia workflow `.github/workflows/release.yml`, który buduje
   podpisany APK i publikuje GitHub Release z tagiem `v1.0.<versionCode>`
   (`versionCode` = liczba commitów w repo, rośnie automatycznie).
   Push na inne gałęzie tylko buduje APK jako artefakt w zakładce Actions.
2. Aplikacja (na telefonie przy starcie, w aucie przy otwarciu ekranu) pyta
   `https://api.github.com/repos/<repo>/releases/latest` i porównuje numer z tagu ze
   swoim `versionCode`.
3. Jeśli jest nowsza wersja, na telefonie pojawia się dialog **Pobierz** → po pobraniu
   przycisk **Zainstaluj** otwiera instalator Androida. W aucie wiersz „Wersja aplikacji”
   pokazuje, że jest aktualizacja (instalacja zawsze odbywa się na telefonie).

Wymagania:

- **Wspólny klucz podpisu** – `app/autotest.jks` (hasło `autotest123`) jest używany zarówno
  przez `build.bat`, jak i przez CI, więc każdy APK da się zainstalować „w miejscu”.
  Jeśli na telefonie jest jeszcze wersja podpisana starym kluczem debug z Android Studio,
  **pierwszy raz trzeba ją odinstalować** (`adb uninstall com.example.autotest`).
- **Zgoda na instalowanie nieznanych aplikacji** – przy pierwszej instalacji Android poprosi
  o włączenie tego dla „Auto Test”; aplikacja sama otwiera odpowiedni ekran ustawień.
- **Repo prywatne** – GitHub nie udostępni wydań bez tokena. Opcje:
  - zrobić repo publiczne (nic więcej nie trzeba konfigurować), albo
  - dodać secret repozytorium `UPDATE_TOKEN` (Settings → Secrets and variables → Actions)
    z fine-grained PAT mającym uprawnienie **Contents: Read** tylko do tego repo.
    CI wkompiluje token do APK (`BuildConfig.UPDATE_TOKEN`) – token trafia więc do
    każdego, kto ma plik APK, dlatego nadawaj mu minimalne uprawnienia.
    Lokalnie: `gradlew assembleDebug -PupdateToken=...`.

Pierwszą wersję z aktualizatorem trzeba raz wgrać ręcznie: pobierz APK z Releases
(albo z artefaktu w Actions) i zainstaluj na telefonie – kolejne wersje aplikacja pobierze sama.

Ręczne sprawdzenie: przycisk **Sprawdź aktualizacje** na telefonie albo dotknięcie wiersza
„Wersja aplikacji” w aucie.

## Logi

```
%USERPROFILE%\Android\Sdk\platform-tools\adb.exe logcat -s CarApp CarAppService AutoTest
```
