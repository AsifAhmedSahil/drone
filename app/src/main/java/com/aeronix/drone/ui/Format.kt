package com.aeronix.drone.ui

import androidx.compose.ui.graphics.Color
import com.aeronix.drone.data.AppSettings
import com.aeronix.drone.network.ConnectionState
import com.aeronix.drone.network.ErrorKind
import com.aeronix.drone.network.Phase
import com.aeronix.drone.ui.theme.Aero
import java.util.Locale

/** Shown wherever the ESP32 has not reported a value. We never substitute a made-up number. */
const val NA = "—"

fun fmtVolts(v: Double?) = v?.let { String.format(Locale.US, "%.2f V", it) } ?: NA
fun fmtPercent(p: Int?) = p?.let { "$it%" } ?: NA
fun fmtMeters(m: Double?) = m?.let { String.format(Locale.US, "%.1f m", it) } ?: NA
fun fmtDbm(d: Int?) = d?.let { "$it dBm" } ?: NA
fun fmtMs(ms: Long?) = ms?.let { "$it ms" } ?: NA
fun fmtBytes(b: Long?) = b?.let { String.format(Locale.US, "%.0f KB", it / 1024.0) } ?: NA

fun fmtDuration(ms: Long?): String {
    if (ms == null || ms < 0) return NA
    val s = ms / 1000
    return when {
        s >= 3600 -> "${s / 3600}h ${(s % 3600) / 60}m"
        s >= 60 -> "${s / 60}m ${s % 60}s"
        else -> "${s}s"
    }
}

fun fmtAgo(thenMs: Long?, nowMs: Long): String {
    if (thenMs == null) return NA
    val s = ((nowMs - thenMs) / 1000).coerceAtLeast(0)
    return if (s < 2) "just now" else "${s}s ago"
}

fun signalQuality(dbm: Int?): String? = when {
    dbm == null -> null
    dbm >= -55 -> "Excellent"
    dbm >= -67 -> "Good"
    dbm >= -75 -> "Fair"
    else -> "Weak"
}

fun Phase.color(): Color = when (this) {
    Phase.CONNECTED -> Aero.Green
    Phase.CONNECTING, Phase.RECONNECTING -> Aero.Amber
    Phase.DISCONNECTED, Phase.LOST, Phase.FAILED -> Aero.Red
}

fun ConnectionState.statusLabel(): String = when (phase) {
    Phase.DISCONNECTED -> "Disconnected"
    Phase.CONNECTING -> "Connecting…"
    Phase.RECONNECTING -> "Reconnecting…"
    Phase.CONNECTED -> "Connected"
    Phase.LOST -> "Connection lost"
    Phase.FAILED -> "Connection failed"
}

fun ConnectionState.description(s: AppSettings): String = when (phase) {
    Phase.DISCONNECTED -> "Join the ${s.ssid} Wi-Fi, then tap Connect."
    Phase.CONNECTING -> step ?: "Connecting…"
    Phase.RECONNECTING -> "Signal dropped. Trying again (attempt ${retry + 1})…"
    Phase.CONNECTED -> "Receiving data from ${s.host} · ${fmtMs(latencyMs)}"
    Phase.LOST -> error?.message ?: "Lost contact with the ESP32."
    Phase.FAILED -> error?.message ?: "Could not reach the ESP32."
}

fun ErrorKind.hint(ssid: String): String = when (this) {
    ErrorKind.WIFI_OFF -> "Turn Wi-Fi on, then try again."
    ErrorKind.NO_WIFI_NETWORK -> "Open Wi-Fi settings and join $ssid, or use “Join $ssid automatically”."
    ErrorKind.JOIN_REJECTED -> "Check the ESP32 is powered, and the Wi-Fi name and password in Settings."
    ErrorKind.WIFI_LOST -> "Move closer to the ESP32 and make sure it is still powered."
    ErrorKind.TIMEOUT -> "The ESP32 may be busy or out of range. Move closer and retry."
    ErrorKind.UNREACHABLE -> "Check the ESP32 IP address in Settings. If asked “Stay connected?” answer Yes."
    ErrorKind.HTTP_ERROR -> "The ESP32 is reachable but the endpoint path is wrong. Check Settings."
    ErrorKind.BAD_RESPONSE -> "The firmware must return JSON. See the ESP32 guide."
}
