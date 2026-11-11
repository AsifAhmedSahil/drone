package com.aeronix.drone.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BackgroundTheme(val label: String) { PURE_BLACK("Pure black"), CHARCOAL("Charcoal") }
enum class AccentTheme(val label: String) { BLUE("Electric blue"), SILVER("Silver") }

/**
 * Everything the user can change. Defaults match the ESP32 firmware in /firmware.
 * To point the app at different endpoints, change [host], [port] or [statusPath].
 */
data class AppSettings(
    val ssid: String = "DRONE_CONTROLLER",
    val password: String = "drone12345",
    val host: String = "192.168.4.1",
    val port: Int = 80,
    val statusPath: String = "/status",
    val timeoutMs: Int = 2500,
    val pollIntervalMs: Long = 1000,
    val autoReconnect: Boolean = true,
    val background: BackgroundTheme = BackgroundTheme.PURE_BLACK,
    val accent: AccentTheme = AccentTheme.BLUE,
) {
    val baseUrl: String get() = "http://$host${if (port == 80) "" else ":$port"}"
    val statusUrl: String get() = baseUrl + (if (statusPath.startsWith("/")) statusPath else "/$statusPath")
}

/** Persists [AppSettings] in SharedPreferences and exposes them as a StateFlow. */
class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("aeronix_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        write(next)
        _settings.value = next
    }

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            ssid = prefs.getString("ssid", d.ssid) ?: d.ssid,
            password = prefs.getString("password", d.password) ?: d.password,
            host = prefs.getString("host", d.host) ?: d.host,
            port = prefs.getInt("port", d.port),
            statusPath = prefs.getString("statusPath", d.statusPath) ?: d.statusPath,
            timeoutMs = prefs.getInt("timeoutMs", d.timeoutMs),
            pollIntervalMs = prefs.getLong("pollIntervalMs", d.pollIntervalMs),
            autoReconnect = prefs.getBoolean("autoReconnect", d.autoReconnect),
            background = prefs.getEnum("background", d.background),
            accent = prefs.getEnum("accent", d.accent),
        )
    }

    private fun write(s: AppSettings) {
        prefs.edit()
            .putString("ssid", s.ssid)
            .putString("password", s.password)
            .putString("host", s.host)
            .putInt("port", s.port)
            .putString("statusPath", s.statusPath)
            .putInt("timeoutMs", s.timeoutMs)
            .putLong("pollIntervalMs", s.pollIntervalMs)
            .putBoolean("autoReconnect", s.autoReconnect)
            .putString("background", s.background.name)
            .putString("accent", s.accent.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.getEnum(key: String, default: T): T {
        val raw = getString(key, null) ?: return default
        return try {
            enumValueOf<T>(raw)
        } catch (e: IllegalArgumentException) {
            default
        }
    }
}
