package com.aeronix.drone.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeronix.drone.data.AppSettings
import com.aeronix.drone.network.ConnectionState
import com.aeronix.drone.network.Phase
import com.aeronix.drone.ui.NA
import com.aeronix.drone.ui.Tab
import com.aeronix.drone.ui.color
import com.aeronix.drone.ui.components.AeroCard
import com.aeronix.drone.ui.components.AeronixLogo
import com.aeronix.drone.ui.components.IconBadge
import com.aeronix.drone.ui.components.InfoRow
import com.aeronix.drone.ui.components.MetricTile
import com.aeronix.drone.ui.components.ScreenColumn
import com.aeronix.drone.ui.components.SectionLabel
import com.aeronix.drone.ui.components.StatusPill
import com.aeronix.drone.ui.description
import com.aeronix.drone.ui.fmtDbm
import com.aeronix.drone.ui.fmtDuration
import com.aeronix.drone.ui.fmtMeters
import com.aeronix.drone.ui.fmtPercent
import com.aeronix.drone.ui.fmtVolts
import com.aeronix.drone.ui.signalQuality
import com.aeronix.drone.ui.statusLabel

@Composable
fun HomeScreen(
    state: ConnectionState,
    settings: AppSettings,
    onOpenConnection: () -> Unit,
    onNavigate: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = state.telemetry
    val statusColor by animateColorAsState(state.phase.color(), label = "heroColor")
    val signalDbm = t?.rssiDbm ?: state.phoneSignalDbm

    ScreenColumn(modifier) {
        // Header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AeronixLogo(MaterialTheme.colorScheme.primary, size = 38.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("AERONIX", style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 4.sp), color = MaterialTheme.colorScheme.onSurface)
                Text("ESP32 DRONE CONTROLLER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(state.phase, state.statusLabel())
        }

        // Connection hero
        AeroCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(if (state.isConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, tint = statusColor, size = 44.dp)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    SectionLabel("ESP32 Wi-Fi link")
                    Text(state.statusLabel(), style = MaterialTheme.typography.headlineSmall, color = statusColor)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(state.description(settings), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onOpenConnection, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(if (state.isConnected) "Manage connection" else if (state.phase == Phase.LOST || state.phase == Phase.FAILED) "Reconnect…" else "Connect to ESP32")
            }
        }

        // Metrics
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricTile(
                Icons.Rounded.BatteryFull, "Battery", fmtPercent(t?.batteryPercent),
                if (t?.batteryVolts != null) fmtVolts(t.batteryVolts) else "No battery telemetry",
                t?.batteryPercent != null, Modifier.weight(1f),
            )
            MetricTile(
                Icons.Rounded.Height, "Altitude", fmtMeters(t?.altitudeM),
                if (t?.altitudeM != null) "Reported by ESP32" else "No altitude sensor data",
                t?.altitudeM != null, Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricTile(
                Icons.Rounded.SignalCellularAlt, "Signal", fmtDbm(signalDbm),
                when {
                    signalDbm == null -> "No signal data"
                    t?.rssiDbm != null -> "ESP32 · ${signalQuality(signalDbm)}"
                    else -> "Phone Wi-Fi · ${signalQuality(signalDbm)}"
                },
                signalDbm != null, Modifier.weight(1f),
            )
            MetricTile(
                Icons.Rounded.Memory, "Device", t?.chip ?: t?.device ?: NA,
                if (t != null) "Uptime ${fmtDuration(t.uptimeMs)}" else "Not connected",
                (t?.chip ?: t?.device) != null, Modifier.weight(1f),
            )
        }

        // Device info
        AeroCard(Modifier.fillMaxWidth()) {
            SectionLabel("Device information")
            Spacer(Modifier.height(4.dp))
            InfoRow("Name", t?.device)
            InfoRow("Firmware", t?.firmware)
            InfoRow("Address", "${settings.host}:${settings.port}")
            InfoRow("Latency", state.latencyMs?.let { "$it ms" })
            InfoRow("Flight commands", "Disabled", divider = false)
        }

        // Quick navigation
        SectionLabel("Quick access")
        listOf(
            Triple(Tab.Controller, "Controller", "Joystick layout · flight controls locked"),
            Triple(Tab.Telemetry, "Telemetry", "Live values reported by the ESP32"),
            Triple(Tab.Settings, "Settings", "Wi-Fi, IP address, theme, reconnection"),
        ).forEach { (tab, title, sub) ->
            AeroCard(Modifier.fillMaxWidth(), onClick = { onNavigate(tab) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(tab.icon)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
