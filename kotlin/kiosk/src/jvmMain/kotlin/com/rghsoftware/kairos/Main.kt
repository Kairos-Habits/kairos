package com.rghsoftware.kairos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.rghsoftware.kairos.display.DisplayModeConfig
import com.rghsoftware.kairos.display.DisplayModeEvent
import com.rghsoftware.kairos.display.DisplayModeState
import com.rghsoftware.kairos.display.PresenceState
import com.rghsoftware.kairos.display.reduce
import com.rghsoftware.kairos.serial.SensorConfigManager
import com.rghsoftware.kairos.serial.SerialConfig
import com.rghsoftware.kairos.serial.SerialPresenceListener
import com.rghsoftware.kairos.ui.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Instant

private enum class Screen {
    MAIN,
    SETTINGS,
}

fun main() =
    application {
        val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        val listener = remember { SerialPresenceListener(SerialConfig.DEFAULT, scope) }
        val configManager = remember { SensorConfigManager(scope) }

        var displayState by remember { mutableStateOf(DisplayModeState()) }
        var currentScreen by remember { mutableStateOf(Screen.MAIN) }
        var currentEnergy by remember { mutableIntStateOf(0) }
        var distanceCm by remember { mutableIntStateOf(0) }
        var connectionStatus by remember { mutableStateOf("Connecting...") }

        LaunchedEffect(Unit) {
            configManager.setListener(listener)
            listener.start()
            listener.events.collectLatest { event ->
                when (event) {
                    is com.rghsoftware.kairos.serial.PresenceEvent.Presence -> {
                        val presenceState =
                            when (event.state) {
                                com.rghsoftware.kairos.serial.PresenceState.PRESENT -> PresenceState.Present
                                com.rghsoftware.kairos.serial.PresenceState.ABSENT -> PresenceState.Absent
                            }
                        val displayEvent =
                            DisplayModeEvent.PresenceChanged(
                                state = presenceState,
                                timestamp = Instant.fromEpochMilliseconds(event.timestampMs),
                            )
                        displayState =
                            displayState.reduce(
                                event = displayEvent,
                                config = DisplayModeConfig(),
                            ).newState
                        currentEnergy = event.movingCm
                        distanceCm =
                            if (event.state == com.rghsoftware.kairos.serial.PresenceState.PRESENT) {
                                maxOf(event.movingCm, event.staticCm)
                            } else {
                                0
                            }
                        connectionStatus = "Connected"
                    }
                    is com.rghsoftware.kairos.serial.PresenceEvent.Heartbeat -> {
                        connectionStatus = "Connected"
                    }
                }
            }
        }

        LaunchedEffect(currentScreen) {
            if (currentScreen == Screen.SETTINGS) {
                configManager.requestSettings()
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "kairos",
        ) {
            MaterialTheme {
                when (currentScreen) {
                    Screen.MAIN -> {
                        MainScreen(
                            displayState = displayState,
                            onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                        )
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            configManager = configManager,
                            presenceState = displayState.presenceState,
                            currentEnergy = currentEnergy,
                            distanceCm = distanceCm,
                            connectionStatus = connectionStatus,
                            onNavigateBack = { currentScreen = Screen.MAIN },
                        )
                    }
                }
            }
        }
    }

@Composable
private fun MainScreen(
    displayState: DisplayModeState,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            Button(onClick = onNavigateToSettings) {
                Text("Settings")
            }
        },
    ) { padding ->
        App(
            displayState = displayState,
            modifier = Modifier.padding(padding),
        )
    }
}
