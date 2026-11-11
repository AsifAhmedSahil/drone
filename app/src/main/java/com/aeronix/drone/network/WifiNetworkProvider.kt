package com.aeronix.drone.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Owns the Android [Network] used to talk to the ESP32.
 *
 * Two modes:
 *  1. Use the Wi-Fi the phone is already on (user joined DRONE_CONTROLLER in system settings).
 *  2. Android 10+: ask the system to join DRONE_CONTROLLER (shows Android's own approval dialog).
 *
 * The request is kept alive while connected, so Android reports when the network is lost
 * ([network] becomes null) and when it comes back.
 */
class WifiNetworkProvider(context: Context) {
    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(ConnectivityManager::class.java)
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)

    private val _network = MutableStateFlow<Network?>(null)
    val network: StateFlow<Network?> = _network.asStateFlow()

    private val _requestFailed = MutableStateFlow(false)
    private var callback: ConnectivityManager.NetworkCallback? = null

    val canJoinProgrammatically: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun isWifiEnabled(): Boolean = wifiManager?.isWifiEnabled == true

    /** Returns the Wi-Fi network, waiting up to [timeoutMs]. Throws [ConnectionException] on failure. */
    suspend fun ensureNetwork(join: WifiCredentials?, timeoutMs: Long): Network {
        _network.value?.let { return it }
        if (callback == null) register(join)

        val result = withTimeoutOrNull(timeoutMs) {
            combine(_network, _requestFailed) { n, failed -> n to failed }
                .first { (n, failed) -> n != null || failed }
        }
        val net = result?.first
        if (net != null) return net

        val rejected = result?.second == true
        release()
        throw if (join != null) {
            ConnectionException(
                ErrorKind.JOIN_REJECTED,
                if (rejected) "Joining ${join.ssid} was cancelled or refused."
                else "Could not join ${join.ssid} in time. Is the ESP32 powered on?"
            )
        } else {
            ConnectionException(ErrorKind.NO_WIFI_NETWORK, "The phone is not connected to a Wi-Fi network.")
        }
    }

    private fun register(join: WifiCredentials?) {
        val builder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            // The ESP32 hotspot has no internet; without this Android would never match it.
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (join != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val spec = try {
                WifiNetworkSpecifier.Builder()
                    .setSsid(join.ssid)
                    .setWpa2Passphrase(join.passphrase)
                    .build()
            } catch (e: IllegalArgumentException) {
                throw ConnectionException(ErrorKind.JOIN_REJECTED, "Invalid Wi-Fi name or password in Settings.")
            }
            builder.setNetworkSpecifier(spec)
        }

        _requestFailed.value = false
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _network.value = network
            }

            override fun onLost(network: Network) {
                if (_network.value == network) _network.value = null
            }

            override fun onUnavailable() {
                _requestFailed.value = true
            }
        }
        try {
            cm.requestNetwork(builder.build(), cb)
        } catch (e: RuntimeException) {
            throw ConnectionException(ErrorKind.NO_WIFI_NETWORK, "Android refused the Wi-Fi request (${e.message}).")
        }
        callback = cb
    }

    /** Wi-Fi signal measured by the phone (dBm), or null if Android does not provide it. */
    fun signalDbm(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val net = _network.value ?: return null
        val s = cm.getNetworkCapabilities(net)?.signalStrength ?: return null
        return if (s == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) null else s
    }

    fun release() {
        callback?.let {
            try {
                cm.unregisterNetworkCallback(it)
            } catch (e: IllegalArgumentException) {
                // already unregistered
            }
        }
        callback = null
        _network.value = null
        _requestFailed.value = false
    }
}
