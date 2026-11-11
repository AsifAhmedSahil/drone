package com.aeronix.drone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.aeronix.drone.network.ConnectionState
import com.aeronix.drone.ui.NA
import com.aeronix.drone.ui.components.AeroCard
import com.aeronix.drone.ui.components.ChannelBar
import com.aeronix.drone.ui.components.IconBadge
import com.aeronix.drone.ui.components.InfoRow
import com.aeronix.drone.ui.components.Joystick
import com.aeronix.drone.ui.components.SectionLabel
import com.aeronix.drone.ui.components.StatusPill
import com.aeronix.drone.ui.fmtMeters
import com.aeronix.drone.ui.fmtMs
import com.aeronix.drone.ui.fmtPercent
import com.aeronix.drone.ui.statusLabel
import com.aeronix.drone.ui.theme.Aero

/**
 * Controller layout. SAFETY: this version sends NOTHING to the ESP32 from this screen.
 * The optional "touch preview" only moves the sticks on the phone for layout testing.
 * Real flight commands must not be added until a flight controller and failsafes are verified.
 */
@Composable
fun ControllerScreen(state: ConnectionState, modifier: Modifier = Modifier) {
    var preview by remember { mutableStateOf(false) }
    var left by remember { mutableStateOf(Offset.Zero) }
    var right by remember { mutableStateOf(Offset.Zero) }

    val banner: @Composable () -> Unit = {
        AeroCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Lock, tint = Aero.Amber)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("FLIGHT CONTROLS LOCKED", style = MaterialTheme.typography.labelLarge, color = Aero.Amber)
                    Text(
                        "Motor and flight commands are disabled in this version. Nothing is sent to the drone.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    val info: @Composable () -> Unit = {
        val t = state.telemetry
        AeroCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("Flight information")
                StatusPill(state.phase, state.statusLabel())
            }
            Spacer(Modifier.height(4.dp))
            InfoRow("Flight commands", "Disabled")
            InfoRow("Armed state", t?.armed?.let { if (it) "Armed (reported)" else "Disarmed (reported)" })
            InfoRow("Altitude", fmtMeters(t?.altitudeM))
            InfoRow("Battery", fmtPercent(t?.batteryPercent))
            InfoRow("Latency", fmtMs(state.latencyMs), divider = false)
        }
    }

    val channels: @Composable () -> Unit = {
        AeroCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Control inputs")
                Text(
                    if (preview) "LOCAL PREVIEW · NOT SENT" else "NO INPUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (preview) Aero.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChannelBar("THR", if (preview) left.y.let { -it } else null)
                ChannelBar("YAW", if (preview) left.x else null)
                ChannelBar("PIT", if (preview) -right.y else null)
                ChannelBar("ROL", if (preview) right.x else null)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Touch preview", style = MaterialTheme.typography.bodyMedium)
                    Text("Moves the sticks on this phone only, for layout testing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = preview, onCheckedChange = {
                    preview = it
                    if (!it) { left = Offset.Zero; right = Offset.Zero }
                })
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            val stick = minOf(maxHeight - 72.dp, maxWidth * 0.28f)
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Joystick("THROTTLE · YAW", "Left stick", preview, stick, Modifier.weight(1f)) { x, y -> left = Offset(x, -y) }
                Column(Modifier.weight(1.4f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    banner(); info(); channels()
                }
                Joystick("PITCH · ROLL", "Right stick", preview, stick, Modifier.weight(1f)) { x, y -> right = Offset(x, -y) }
            }
        } else {
            val stick = (maxWidth - 32.dp - 16.dp) / 2f
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                banner()
                info()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Joystick("THROTTLE · YAW", "Left stick", preview, stick, Modifier.weight(1f)) { x, y -> left = Offset(x, -y) }
                    Joystick("PITCH · ROLL", "Right stick", preview, stick, Modifier.weight(1f)) { x, y -> right = Offset(x, -y) }
                }
                channels()
            }
        }
    }
}
