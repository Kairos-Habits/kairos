package com.rghsoftware.kairos.display

import kotlin.time.Instant

data class DisplayModeTransition(
    val newState: DisplayModeState,
    val targetDisplayMode: DisplayMode,
    val displayChanged: Boolean,
)

fun DisplayModeState.reduce(
    event: DisplayModeEvent,
    config: DisplayModeConfig,
): DisplayModeTransition {
    val timestamp = when (event) {
        is DisplayModeEvent.PresenceChanged -> event.timestamp
        is DisplayModeEvent.SwipeToggle -> event.timestamp
        is DisplayModeEvent.TimerTick -> event.timestamp
    }

    val newState =
        when (event) {
            is DisplayModeEvent.PresenceChanged -> handlePresenceChanged(event, config, timestamp)
            is DisplayModeEvent.SwipeToggle -> handleSwipeToggle(timestamp, config)
            is DisplayModeEvent.TimerTick -> handleTimerTick(timestamp)
        }

    val displayChanged = newState.displayMode != this.displayMode

    return DisplayModeTransition(
        newState = newState,
        targetDisplayMode = newState.displayMode,
        displayChanged = displayChanged,
    )
}

private fun DisplayModeState.handlePresenceChanged(
    event: DisplayModeEvent.PresenceChanged,
    config: DisplayModeConfig,
    timestamp: Instant,
): DisplayModeState {
    val isManualOverrideActive = manualOverrideUntil != null && timestamp < manualOverrideUntil
    val isCooldownActive = cooldownUntil != null && timestamp < cooldownUntil

    return if (!isManualOverrideActive && !isCooldownActive && event.state == PresenceState.Present && displayMode == DisplayMode.HA) {
        this.copy(
            displayMode = DisplayMode.KIOSK,
            presenceState = event.state,
            cooldownUntil = timestamp + config.autoSwitchCooldown,
        )
    } else {
        this.copy(presenceState = event.state)
    }
}

private fun DisplayModeState.handleSwipeToggle(
    timestamp: Instant,
    config: DisplayModeConfig,
): DisplayModeState {
    val newDisplayMode =
        when (displayMode) {
            DisplayMode.KIOSK -> DisplayMode.HA
            DisplayMode.HA -> DisplayMode.KIOSK
        }

    return this.copy(
        displayMode = newDisplayMode,
        manualOverrideUntil = timestamp + config.manualOverrideDuration,
    )
}

private fun DisplayModeState.handleTimerTick(timestamp: Instant): DisplayModeState {
    var newState = this

    if (manualOverrideUntil != null && timestamp >= manualOverrideUntil) {
        newState = newState.copy(manualOverrideUntil = null)
    }

    if (cooldownUntil != null && timestamp >= cooldownUntil) {
        newState = newState.copy(cooldownUntil = null)
    }

    return newState
}
