package com.aeronix.drone.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aeronix.drone.data.AppSettings
import com.aeronix.drone.network.ConnectionState
import com.aeronix.drone.network.Phase
import com.aeronix.drone.ui.color
import com.aeronix.drone.ui.components.AeroCard
import com.aeronix.drone.ui.components.IconBadge
import com.aeronix.drone.ui.components.InfoRow
import com.aeronix.drone.ui.components.ScreenColumn
import com.aeronix.drone.ui.components.SectionLabel
import com.aeronix.drone.ui.components.rememberNowMs
import com.aeronix.drone.ui.description
import com.aeronix.drone.ui.fmtAgo
import com.aeronix.drone.ui.fmtDuration
import com.aeronix.drone.ui.fmtMs
import com.aeronix.drone.ui.hint
import com.aeronix.drone.ui.statusLabel

/** Dedicated connection screen, opened from Home. */
@Composable
fun ConnectionScreen(
    state: ConnectionState,
    settings: AppSettings,
    canJoin: Boolean,
    onConnect: () -> Unit,
    onJoin: () -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val color by animateColorAsState(state.phase.color(), label = "connColor")
    val now = rememberNowMs()
    val pulse = rememberInfiniteTransition(label = "ring")
    val ringAlpha by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "ringAlpha")

    Column(modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Connection", style = MaterialTheme.typography.titleMedium)
        }

        ScreenColumn(Modifier.weight(1f)) {
            // Status indicator
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(112.dp).alpha(if (state.isBusy) ringAlpha else 1f),
                    contentAlignment = Alignment.Center,
                ) {
                    IconBadge(if (state.isConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, tint = color, size = 112.dp)
                }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.statusLabel(), style = MaterialTheme.typography.headlineMedium, color = color)
                Spacer(Modifier.height(6.dp))
                Text(
                    state.description(settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Error + what to do about it
            val err = state.error
            if (err != null && (state.phase == Phase.FAILED || state.phase == Phase.LOST)) {
                AeroCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Warning, tint = MaterialTheme.colorScheme.error)
                        Text("What went wrong", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(err.message ?: "Unknown error", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(err.kind.hint(settings.ssid), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Actions
            when {
                state.isConnected -> OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Disconnect") }
                state.isBusy -> OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Cancel") }
                else -> {
                    val retry = state.phase == Phase.LOST || state.phase == Phase.FAILED
                    Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        if (retry) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                        }
                        Text(if (retry) "Reconnect" else "Connect")
                    }
                }
            }
            if (!state.isConnected && !state.isBusy) {
                if (canJoin) {
                    OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("Join ${settings.ssid} automatically")
                    }
                }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("Open Wi-Fi settings") }
            }

            // How-to
            AeroCard(Modifier.fillMaxWidth()) {
                SectionLabel("Before you connect")
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Power on the ESP32 drone.",
                    "Join Wi-Fi “${settings.ssid}” (password: ${settings.password}).",
                    "If Android says “no internet, stay connected?” choose Yes.",
                    "Tap Connect. Mobile data can stay on — AERONIX talks to the ESP32 directly.",
                ).forEachIndexed { i, step ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("${i + 1}.", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 10.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Details
            AeroCard(Modifier.fillMaxWidth()) {
                SectionLabel("Details")
                Spacer(Modifier.height(4.dp))
                InfoRow("Target", settings.statusUrl)
                InfoRow("Protocol", "HTTP polling (every ${settings.pollIntervalMs} ms)")
                InfoRow("Latency", state.latencyMs?.let { fmtMs(it) })
                InfoRow("Last data", fmtAgo(state.lastUpdateMs, now).takeIf { state.lastUpdateMs != null })
                InfoRow("Connected for", state.connectedSinceMs?.let { fmtDuration(now - it) }, divider = false)
            }
        }
    }
}
