package com.aeronix.drone.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aeronix.drone.BuildConfig
import com.aeronix.drone.data.AccentTheme
import com.aeronix.drone.data.AppSettings
import com.aeronix.drone.data.BackgroundTheme
import com.aeronix.drone.network.ConnectionState
import com.aeronix.drone.ui.components.AeroCard
import com.aeronix.drone.ui.components.InfoRow
import com.aeronix.drone.ui.components.ScreenColumn
import com.aeronix.drone.ui.components.SectionLabel
import com.aeronix.drone.ui.fmtMs
import com.aeronix.drone.ui.statusLabel

private val HOST_REGEX = Regex("^[A-Za-z0-9]([A-Za-z0-9.-]{0,251}[A-Za-z0-9])?$")

@Composable
fun SettingsScreen(
    settings: AppSettings,
    state: ConnectionState,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Draft values for the Wi-Fi / endpoint form; saved explicitly with the Save button.
    var ssid by remember(settings.ssid) { mutableStateOf(settings.ssid) }
    var password by remember(settings.password) { mutableStateOf(settings.password) }
    var showPassword by remember { mutableStateOf(false) }
    var host by remember(settings.host) { mutableStateOf(settings.host) }
    var port by remember(settings.port) { mutableStateOf(settings.port.toString()) }
    var path by remember(settings.statusPath) { mutableStateOf(settings.statusPath) }
    var timeout by remember(settings.timeoutMs) { mutableStateOf(settings.timeoutMs.toString()) }

    val ssidError = if (ssid.isBlank() || ssid.length > 32) "1–32 characters" else null
    val passError = if (password.length !in 8..63) "WPA2 password must be 8–63 characters" else null
    val hostError = if (!HOST_REGEX.matches(host.trim())) "Enter an IP address or host name" else null
    val portError = if (port.toIntOrNull() !in 1..65535) "1–65535" else null
    val timeoutError = if (timeout.toIntOrNull() !in 500..10000) "500–10000 ms" else null
    val valid = listOf(ssidError, passError, hostError, portError, timeoutError).all { it == null }
    val dirty = ssid != settings.ssid || password != settings.password || host.trim() != settings.host ||
        port != settings.port.toString() || path != settings.statusPath || timeout != settings.timeoutMs.toString()

    ScreenColumn(modifier) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        // Wi-Fi + endpoint
        AeroCard(Modifier.fillMaxWidth()) {
            SectionLabel("Wi-Fi connection")
            Spacer(Modifier.height(10.dp))
            Field("Wi-Fi name (SSID)", ssid, { ssid = it }, ssidError)
            Spacer(Modifier.height(8.dp))
            Field(
                "Wi-Fi password", password, { password = it }, passError,
                visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Hide" else "Show") } },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Open Android Wi-Fi settings") }
        }

        AeroCard(Modifier.fillMaxWidth()) {
            SectionLabel("ESP32 endpoint")
            Spacer(Modifier.height(10.dp))
            Field("IP address", host, { host = it }, hostError, KeyboardType.Uri)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { Field("Port", port, { port = it }, portError, KeyboardType.Number) }
                Column(Modifier.weight(1f)) { Field("Timeout (ms)", timeout, { timeout = it }, timeoutError, KeyboardType.Number) }
            }
            Spacer(Modifier.height(8.dp))
            Field("Status path", path, { path = it }, null, KeyboardType.Uri)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = dirty && valid,
                    onClick = {
                        onUpdate {
                            it.copy(
                                ssid = ssid.trim(), password = password, host = host.trim(),
                                port = port.toInt(), statusPath = path.trim().ifEmpty { "/status" }.let { p -> if (p.startsWith("/")) p else "/$p" },
                                timeoutMs = timeout.toInt(),
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
                OutlinedButton(
                    onClick = {
                        val d = AppSettings()
                        onUpdate { it.copy(ssid = d.ssid, password = d.password, host = d.host, port = d.port, statusPath = d.statusPath, timeoutMs = d.timeoutMs) }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset defaults") }
            }
            if (state.isConnected) {
                Spacer(Modifier.height(8.dp))
                Text("Changes apply to the next request; reconnect if the link drops.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Connection information
        AeroCard(Modifier.fillMaxWidth()) {
            SectionLabel("Connection information")
            Spacer(Modifier.height(4.dp))
            InfoRow("Status", state.statusLabel())
            InfoRow("Status URL", settings.statusUrl)
            InfoRow("Protocol", "HTTP (WebSocket not enabled)")
            InfoRow("Latency", fmtMs(state.latencyMs), divider = false)
        }

        // Reconnection
        AeroCard(Modifier.fillMaxWidth()) {
            SectionLabel("Reconnection")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-reconnect", style = MaterialTheme.typography.bodyLarge)
                    Text("Retry every 3 s after the link drops.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = settings.autoReconnect, onCheckedChange = { v -> onUpdate { it.copy(autoReconnect = v) } })
            }
            Spacer(Modifier.height(12.dp))
            Text("Update interval", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500L, 1000L, 2000L).forEach { ms ->
                    FilterChip(
                        selected = settings.pollIntervalMs == ms,
                        onClick = { onUpdate { it.copy(pollIntervalMs = ms) } },
                        label = { Text("${ms} ms") },
                    )
                }
            }
        }

        // Theme
        AeroCard(Modifier.fillMaxWidth()) {
            SectionLabel("Theme")
            Spacer(Modifier.height(10.dp))
            Text("Background", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackgroundTheme.entries.forEach { b ->
                    FilterChip(selected = settings.background == b, onClick = { onUpdate { it.copy(background = b) } }, label = { Text(b.label) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Accent", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentTheme.entries.forEach { a ->
                    FilterChip(selected = settings.accent == a, onClick = { onUpdate { it.copy(accent = a) } }, label = { Text(a.label) })
                }
            }
        }

        // About
        AeroCard(Modifier.fillMaxWidth()) {
            SectionLabel("About AERONIX")
            Spacer(Modifier.height(4.dp))
            InfoRow("Version", BuildConfig.VERSION_NAME)
            InfoRow("Platform", "Android · Kotlin · Jetpack Compose")
            InfoRow("Hardware", "ESP32 over Wi-Fi (SoftAP)")
            InfoRow("Flight commands", "Disabled", divider = false)
            Spacer(Modifier.height(8.dp))
            Text(
                "AERONIX is an educational ESP32 drone controller. It only reads status from the ESP32. " +
                    "Motor and flight commands stay disabled until a properly configured flight controller " +
                    "and verified safety systems exist.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    keyboard: KeyboardType = KeyboardType.Text,
    visual: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = visual,
        trailingIcon = trailing,
        modifier = Modifier.fillMaxWidth(),
    )
}
