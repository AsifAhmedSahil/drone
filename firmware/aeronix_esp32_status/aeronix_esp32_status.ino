/*
 * AERONIX – ESP32 status firmware (read-only, safe)
 *
 * Creates the Wi-Fi access point the AERONIX app connects to and serves ONE JSON endpoint:
 *
 *     GET http://192.168.4.1/status
 *
 * HONESTY RULE: a value is only sent if it is really measured. Anything not measured is sent
 * as null, and the app then shows "—". Do not put placeholder numbers in here.
 *
 * This sketch does NOT drive motors and does NOT accept flight commands.
 *
 * Board: any ESP32 (Arduino-ESP32 core 2.x or 3.x). No extra libraries needed.
 */

#include <WiFi.h>
#include <WebServer.h>

// ---- Wi-Fi access point (must match the app's Settings) ----
const char *AP_SSID = "DRONE_CONTROLLER";
const char *AP_PASSWORD = "drone12345";   // 8..63 characters

// ---- Identity ----
const char *DEVICE_NAME = "AERONIX-ESP32";
const char *FIRMWARE_VERSION = "1.0.0";

// ---- Optional battery measurement ----
// Wire the battery through a voltage divider to an ADC1 pin (GPIO 32–39) and set the pin and
// the divider ratio. Leave BATTERY_ADC_PIN at -1 if no battery sensor is fitted:
// the app will then show "—" for voltage and percentage.
const int BATTERY_ADC_PIN = -1;
const float BATTERY_DIVIDER_RATIO = 2.0f;  // (R1 + R2) / R2 of your divider
const float BATTERY_EMPTY_V = 3.3f;        // e.g. 1S LiPo empty
const float BATTERY_FULL_V = 4.2f;         // e.g. 1S LiPo full

WebServer server(80);

// Appends  "key":value  (or null) to the JSON string.
void addNumber(String &json, const char *key, bool valid, double value, int decimals = 0) {
  json += "\"";
  json += key;
  json += "\":";
  if (valid) json += String(value, decimals); else json += "null";
}

void handleStatus() {
  bool haveBattery = false;
  float volts = 0;
  int percent = 0;
  if (BATTERY_ADC_PIN >= 0) {
    volts = analogReadMilliVolts(BATTERY_ADC_PIN) / 1000.0f * BATTERY_DIVIDER_RATIO;
    haveBattery = true;
    float p = (volts - BATTERY_EMPTY_V) / (BATTERY_FULL_V - BATTERY_EMPTY_V) * 100.0f;
    percent = (int)constrain(p, 0.0f, 100.0f);
  }

  String json = "{";
  json += "\"device\":\"" + String(DEVICE_NAME) + "\",";
  json += "\"firmware\":\"" + String(FIRMWARE_VERSION) + "\",";
  json += "\"chip\":\"" + String(ESP.getChipModel()) + "\",";
  addNumber(json, "uptime_ms", true, (double)millis()); json += ",";
  addNumber(json, "free_heap", true, (double)ESP.getFreeHeap()); json += ",";
  // The ESP32 acting as an access point has no measurement of the phone's signal: send null.
  addNumber(json, "rssi_dbm", false, 0); json += ",";
  addNumber(json, "battery_v", haveBattery, volts, 2); json += ",";
  addNumber(json, "battery_pct", haveBattery, percent); json += ",";
  // No altimeter fitted: send null until you add a real barometer and read it here.
  addNumber(json, "altitude_m", false, 0); json += ",";
  json += "\"armed\":false";
  json += "}";

  server.sendHeader("Cache-Control", "no-store");
  server.send(200, "application/json", json);
}

void setup() {
  Serial.begin(115200);
  if (BATTERY_ADC_PIN >= 0) analogSetPinAttenuation(BATTERY_ADC_PIN, ADC_11db);

  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(IPAddress(192, 168, 4, 1), IPAddress(192, 168, 4, 1), IPAddress(255, 255, 255, 0));
  WiFi.softAP(AP_SSID, AP_PASSWORD);
  Serial.print("AP started. IP: ");
  Serial.println(WiFi.softAPIP());

  server.on("/status", HTTP_GET, handleStatus);
  server.begin();
}

void loop() {
  server.handleClient();
}
