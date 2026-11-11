package com.aeronix.drone.network

/**
 * Values reported by the ESP32. Every field is nullable on purpose: a field is null when the
 * firmware did not send it (sensor not fitted). The UI shows "—" for null and never invents data.
 */
data class Telemetry(
    val device: String? = null,
    val firmware: String? = null,
    val chip: String? = null,
    val uptimeMs: Long? = null,
    val freeHeapBytes: Long? = null,
    val rssiDbm: Int? = null,
    val batteryVolts: Double? = null,
    val batteryPercent: Int? = null,
    val altitudeM: Double? = null,
    val armed: Boolean? = null,
)

enum class Phase { DISCONNECTED, CONNECTING, RECONNECTING, CONNECTED, LOST, FAILED }

enum class ErrorKind { WIFI_OFF, NO_WIFI_NETWORK, JOIN_REJECTED, WIFI_LOST, TIMEOUT, UNREACHABLE, HTTP_ERROR, BAD_RESPONSE }

class ConnectionException(val kind: ErrorKind, message: String) : Exception(message)

data class ConnectionState(
    val phase: Phase = Phase.DISCONNECTED,
    val error: ConnectionException? = null,
    val telemetry: Telemetry? = null,
    val latencyMs: Long? = null,
    /** Signal strength of the phone's Wi-Fi link, measured by Android (not by the ESP32). */
    val phoneSignalDbm: Int? = null,
    val connectedSinceMs: Long? = null,
    val lastUpdateMs: Long? = null,
    val retry: Int = 0,
    val step: String? = null,
) {
    val isConnected: Boolean get() = phase == Phase.CONNECTED
    val isBusy: Boolean get() = phase == Phase.CONNECTING || phase == Phase.RECONNECTING
}

data class StatusReading(val telemetry: Telemetry, val latencyMs: Long)

data class WifiCredentials(val ssid: String, val passphrase: String)
