package com.aeronix.drone.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.aeronix.drone.network.ConnectionState
import com.aeronix.drone.ui.NA
import com.aeronix.drone.ui.components.AeroCard
import com.aeronix.drone.ui.components.IconBadge
import com.aeronix.drone.ui.components.InfoRow
import com.aeronix.drone.ui.components.ScreenColumn
import com.aeronix.drone.ui.components.SectionLabel
import com.aeronix.drone.ui.components.StatusPill
import com.aeronix.drone.ui.components.rememberNowMs
import com.aeronix.drone.ui.fmtAgo
import com.aeronix.drone.ui.fmtBytes
import com.aeronix.drone.ui.fmtDbm
import com.aeronix.drone.ui.fmtDuration
import com.aeronix.drone.ui.fmtMeters
import com.aeronix.drone.ui.fmtMs
import com.aeronix.drone.ui.fmtPercent
import com.aeronix.drone.ui.fmtVolts
import com.aeronix.drone.ui.signalQuality
import com.aeronix.drone.ui.statusLabel

@Composable
fun TelemetryScreen(state: ConnectionState, modifier: Modifier = Modifier) {
    val t = state.telemetry
    val now = rememberNowMs()

    ScreenColumn(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Telemetry", style = MaterialTheme.typography.headlineSmall)
            StatusPill(state.phase, state.statusLabel())
        }

        if (!state.isConnected) {
            AeroCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Info)
                    Text(
                        "No live data. Values appear as “$NA” until the ESP32 reports them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }

        // Battery
        AeroCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.BatteryFull)
                SectionLabel("Battery", Modifier.padding(start = 12.dp))
            }
            Spacer(Modifier.height(12.dp))
            val progress by animateFloatAsState((t?.batteryPercent ?: 0) / 100f, tween(500), label = "battery")
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(8.dp))
            InfoRow("Voltage", fmtVolts(t?.batteryVolts))
            InfoRow("Percentage", fmtPercent(t?.batteryPercent), divider = false)
        }

        // Flight data
        AeroCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Height)
                SectionLabel("Flight data", Modifier.padding(start = 12.dp))
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("Altitude", fmtMeters(t?.altitudeM))
            InfoRow("Armed state", t?.armed?.let { if (it) "Armed" else "Disarmed" }, divider = false)
        }

        // Signal
        AeroCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.SignalCellularAlt)
                SectionLabel("Signal strength", Modifier.padding(start = 12.dp))
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("ESP32-reported", fmtDbm(t?.rssiDbm))
            InfoRow("Phone Wi-Fi link", fmtDbm(state.phoneSignalDbm))
            InfoRow("Quality", signalQuality(t?.rssiDbm ?: state.phoneSignalDbm))
            InfoRow("Latency", fmtMs(state.latencyMs), divider = false)
        }

        // Wi-Fi
        AeroCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Wifi)
                SectionLabel("Wi-Fi connection", Modifier.padding(start = 12.dp))
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("Status", state.statusLabel())
            InfoRow("Last update", if (state.lastUpdateMs != null) fmtAgo(state.lastUpdateMs, now) else null)
            InfoRow("Connected for", state.connectedSinceMs?.let { fmtDuration(now - it) }, divider = false)
        }

        // Device
        AeroCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Memory)
                SectionLabel("Device status", Modifier.padding(start = 12.dp))
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("Name", t?.device)
            InfoRow("Chip", t?.chip)
            InfoRow("Firmware", t?.firmware)
            InfoRow("Uptime", t?.uptimeMs?.let { fmtDuration(it) })
            InfoRow("Free memory", t?.freeHeapBytes?.let { fmtBytes(it) }, divider = false)
        }
    }
}
