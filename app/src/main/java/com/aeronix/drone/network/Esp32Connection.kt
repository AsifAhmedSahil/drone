package com.aeronix.drone.network

import com.aeronix.drone.data.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Connection state machine:
 *
 *   DISCONNECTED -> CONNECTING -> CONNECTED -(drop)-> LOST -> RECONNECTING -> CONNECTED ...
 *                       \-> FAILED (first attempt did not work; user presses Retry)
 *
 * While CONNECTED it polls the ESP32 status endpoint. [MAX_MISSED_POLLS] failed polls in a row,
 * or Android reporting the Wi-Fi network as gone, count as a lost connection.
 */
class Esp32Connection(
    private val wifi: WifiNetworkProvider,
    private val api: Esp32Api,
    private val settings: () -> AppSettings,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()
    private var job: Job? = null

    /** [joinNetwork] = true asks Android to join the configured SSID (Android 10+). */
    fun connect(joinNetwork: Boolean = false) {
        job?.cancel()
        if (joinNetwork) wifi.release()
        job = scope.launch { supervise(joinNetwork) }
    }

    fun disconnect() {
        job?.cancel()
        job = null
        wifi.release()
        _state.value = ConnectionState()
    }

    private suspend fun supervise(join: Boolean) {
        var hadLink = false
        var retries = 0
        var joinPending = join

        while (true) {
            try {
                val s = settings()
                _state.update {
                    it.copy(
                        phase = if (hadLink) Phase.RECONNECTING else Phase.CONNECTING,
                        error = if (hadLink) it.error else null,
                        retry = retries,
                        step = if (joinPending) "Asking Android to join ${s.ssid}…" else "Looking for the Wi-Fi network…",
                    )
                }
                if (!wifi.isWifiEnabled()) {
                    throw ConnectionException(ErrorKind.WIFI_OFF, "Wi-Fi is switched off.")
                }
                val creds = if (joinPending && wifi.canJoinProgrammatically) WifiCredentials(s.ssid, s.password) else null
                val network = wifi.ensureNetwork(creds, if (creds != null) JOIN_TIMEOUT_MS else FIND_TIMEOUT_MS)
                joinPending = false

                _state.update { it.copy(step = "Contacting the ESP32 at ${s.host}…") }
                publish(api.fetchStatus(network, s))
                hadLink = true
                retries = 0

                var missed = 0
                while (true) {
                    delay(settings().pollIntervalMs)
                    val current = wifi.network.value
                        ?: throw ConnectionException(ErrorKind.WIFI_LOST, "The Wi-Fi network disappeared.")
                    try {
                        publish(api.fetchStatus(current, settings()))
                        missed = 0
                    } catch (e: ConnectionException) {
                        if (++missed >= MAX_MISSED_POLLS) throw e
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e as? ConnectionException
                    ?: ConnectionException(ErrorKind.UNREACHABLE, e.message ?: "Unexpected error.")

                if (!hadLink) {
                    wifi.release()
                    _state.value = ConnectionState(phase = Phase.FAILED, error = error)
                    return
                }
                // Keep the network request alive: Android re-delivers the network when it returns.
                _state.value = ConnectionState(phase = Phase.LOST, error = error, retry = retries)
                if (!settings().autoReconnect || retries >= MAX_AUTO_RETRIES) return
                retries++
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    private fun publish(r: StatusReading) {
        val now = System.currentTimeMillis()
        val signal = wifi.signalDbm()
        _state.update {
            it.copy(
                phase = Phase.CONNECTED,
                error = null,
                telemetry = r.telemetry,
                latencyMs = r.latencyMs,
                phoneSignalDbm = signal,
                connectedSinceMs = it.connectedSinceMs ?: now,
                lastUpdateMs = now,
                retry = 0,
                step = null,
            )
        }
    }

    private companion object {
        const val MAX_MISSED_POLLS = 3
        const val MAX_AUTO_RETRIES = 20
        const val RECONNECT_DELAY_MS = 3_000L
        const val FIND_TIMEOUT_MS = 5_000L
        const val JOIN_TIMEOUT_MS = 30_000L
    }
}
