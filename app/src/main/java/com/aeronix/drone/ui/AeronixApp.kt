package com.aeronix.drone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aeronix.drone.AeronixViewModel
import com.aeronix.drone.ui.screens.ConnectionScreen
import com.aeronix.drone.ui.screens.ControllerScreen
import com.aeronix.drone.ui.screens.HomeScreen
import com.aeronix.drone.ui.screens.SettingsScreen
import com.aeronix.drone.ui.screens.TelemetryScreen

@Composable
fun AeronixApp(vm: AeronixViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val state by vm.connectionState.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var showConnection by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    BackHandler(enabled = showConnection) { showConnection = false }
    BackHandler(enabled = !showConnection && tab != Tab.Home) { tab = Tab.Home }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Scaffold(
            containerColor = colors.background,
            bottomBar = {
                NavigationBar(containerColor = colors.surfaceContainerLow) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                indicatorColor = colors.primary.copy(alpha = 0.14f),
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                modifier = Modifier.padding(padding),
                label = "tabs",
            ) { current ->
                when (current) {
                    Tab.Home -> HomeScreen(state, settings, onOpenConnection = { showConnection = true }, onNavigate = { tab = it })
                    Tab.Controller -> ControllerScreen(state)
                    Tab.Telemetry -> TelemetryScreen(state)
                    Tab.Settings -> SettingsScreen(settings, state, onUpdate = vm::updateSettings)
                }
            }
        }

        AnimatedVisibility(
            visible = showConnection,
            enter = slideInVertically(tween(260)) { it / 4 } + fadeIn(tween(260)),
            exit = slideOutVertically(tween(200)) { it / 4 } + fadeOut(tween(200)),
        ) {
            ConnectionScreen(
                state = state,
                settings = settings,
                canJoin = vm.canJoinProgrammatically,
                onConnect = vm::connect,
                onJoin = vm::joinAndConnect,
                onDisconnect = vm::disconnect,
                onBack = { showConnection = false },
                modifier = Modifier.background(colors.background),
            )
        }
    }
}
