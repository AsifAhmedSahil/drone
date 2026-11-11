package com.aeronix.drone.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Controller("Controller", Icons.Rounded.SportsEsports),
    Telemetry("Telemetry", Icons.Rounded.Sensors),
    Settings("Settings", Icons.Rounded.Settings),
}
