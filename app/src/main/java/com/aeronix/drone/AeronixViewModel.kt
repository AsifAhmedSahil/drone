package com.aeronix.drone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aeronix.drone.data.AppSettings
import com.aeronix.drone.data.SettingsRepository
import com.aeronix.drone.network.ConnectionState
import com.aeronix.drone.network.Esp32Api
import com.aeronix.drone.network.Esp32Connection
import com.aeronix.drone.network.WifiNetworkProvider
import kotlinx.coroutines.flow.StateFlow

/** Single entry point the UI talks to. Screens receive plain state + lambdas, nothing else. */
class AeronixViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)
    private val wifi = WifiNetworkProvider(app)
    private val connection = Esp32Connection(wifi, Esp32Api(), { repo.settings.value }, viewModelScope)

    val settings: StateFlow<AppSettings> = repo.settings
    val connectionState: StateFlow<ConnectionState> = connection.state
    val canJoinProgrammatically: Boolean get() = wifi.canJoinProgrammatically

    fun connect() = connection.connect(joinNetwork = false)
    fun joinAndConnect() = connection.connect(joinNetwork = true)
    fun disconnect() = connection.disconnect()
    fun updateSettings(transform: (AppSettings) -> AppSettings) = repo.update(transform)

    override fun onCleared() {
        connection.disconnect()
    }
}
