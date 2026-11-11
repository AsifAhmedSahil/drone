# AERONIX – Build, Install, Share & ESP32 Setup

Native Android app (Kotlin + Jetpack Compose + Material 3) for an ESP32 educational drone.
**Flight/motor commands are disabled in this version.** The app only reads status from the ESP32.

## 1. Build the APK

### Option A – Android Studio (easiest)
1. Install Android Studio (Koala or newer). Open the `AERONIX` folder (**File ▸ Open**).
2. Wait for Gradle sync.
3. Select the **release** variant (**Build ▸ Select Build Variant ▸ `release`**), then run
   **Build ▸ Build Bundle(s) / APK(s) ▸ Build APK(s)**. The release variant is already signed
   with the project key, so it installs on any phone.
4. Output: `app/build/outputs/apk/release/app-release.apk`
   (debug variant: `app/build/outputs/apk/debug/app-debug.apk`, larger, also installable).

### Option B – Command line (Android SDK + JDK 17 installed)
```bash
./gradlew assembleRelease      # Linux / macOS
gradlew.bat assembleRelease    # Windows
```

### Option C – No Android Studio: build in the cloud with GitHub
1. Create a GitHub repository and push this folder.
2. Open **Actions ▸ Build AERONIX APK ▸ Run workflow**.
3. When it finishes, download **AERONIX-APK** from the run page. It contains `AERONIX_DroneController_v1.0.apk`.

> The release key (`app/aeronix-release.jks`, password `aeronix123`) is for sideloading and learning.
> For Google Play, create your own private keystore and change `signingConfigs` in `app/build.gradle.kts`.

## 2. Install on a Samsung / Android phone
1. Copy or download the APK to the phone and tap it.
2. If asked, allow **Install unknown apps** for the app you opened it from (Files, WhatsApp, Chrome), then **Install**.
3. If Google Play Protect warns about an unrecognised app: **More details ▸ Install anyway**.
   (Normal for any APK not from the Play Store.)

Requires Android 8.0 (API 26) or newer.

## 3. Send the APK on WhatsApp
1. Rename it to `AERONIX_DroneController_v1.0.apk`.
2. In WhatsApp open the chat ▸ **Attach (paperclip / +) ▸ Document** (not Gallery) ▸ pick the file ▸ Send.
3. If WhatsApp refuses the `.apk`, zip it first (`AERONIX.zip`) and ask the receiver to unzip it, or share via Google Drive.
4. The receiver taps the file, allows installs from WhatsApp, and installs.

## 4. Connect to the ESP32
1. Flash `firmware/aeronix_esp32_status/aeronix_esp32_status.ino` (Arduino IDE, any ESP32 board, no extra libraries).
2. Power the ESP32. It creates Wi-Fi **DRONE_CONTROLLER**, password **drone12345**, IP **192.168.4.1**.
3. In the app: **Home ▸ Connect to ESP32**, then either
   - **Join DRONE_CONTROLLER automatically** (Android 10+, Android shows an approval dialog), or
   - **Open Wi-Fi settings**, join the network yourself (answer **Yes** to “no internet, stay connected?”), come back, tap **Connect**.
4. Mobile data can stay on. The app sends its requests directly over the ESP32 Wi-Fi.

Permissions used: `INTERNET`, `ACCESS_NETWORK_STATE`, `CHANGE_NETWORK_STATE`, `ACCESS_WIFI_STATE`.
No location permission is needed.

## 5. ESP32 endpoint contract (edit in app **Settings**, or in firmware)

| Setting | Default | Where |
|---|---|---|
| IP / port | `192.168.4.1` : `80` | Settings ▸ ESP32 endpoint |
| Status path | `/status` | Settings ▸ ESP32 endpoint |
| Timeout | 2500 ms | Settings ▸ ESP32 endpoint |
| Poll interval | 1000 ms | Settings ▸ Reconnection |

`GET /status` must return JSON. **Every key is optional.** Send `null` (or omit it) for anything you
do not really measure; the app shows “—” instead of inventing a value.

```json
{
  "device": "AERONIX-ESP32",
  "firmware": "1.0.0",
  "chip": "ESP32-D0WD-V3",
  "uptime_ms": 123456,
  "free_heap": 210000,
  "rssi_dbm": null,
  "battery_v": 3.92,
  "battery_pct": 78,
  "altitude_m": null,
  "armed": false
}
```

To add a real sensor: read it in `handleStatus()` in the `.ino` and pass `true` + the value to `addNumber(...)`.

## 6. Code map
```
app/src/main/java/com/aeronix/drone/
  MainActivity.kt, AeronixViewModel.kt
  data/AppSettings.kt            settings + persistence
  network/WifiNetworkProvider.kt Wi-Fi network request / loss detection
  network/Esp32Api.kt            HTTP GET + JSON parsing (read-only)
  network/Esp32Connection.kt     connect / poll / lost / auto-reconnect state machine
  ui/theme, ui/components        palette, cards, joystick, status pill
  ui/screens                     Home, Connection, Controller, Telemetry, Settings
```
WebSocket can be added later as a second transport next to `Esp32Api`, without touching the UI.

## 7. Safety
Do not add motor or flight commands until a real flight controller, failsafes (signal-loss disarm,
low-battery cut-off) and bench tests with propellers removed are in place.
