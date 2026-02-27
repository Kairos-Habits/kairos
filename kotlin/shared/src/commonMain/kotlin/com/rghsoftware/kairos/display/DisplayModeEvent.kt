package com.rghsoftware.kairos.display

import kotlin.time.Instant

sealed interface DisplayModeEvent {
    data class PresenceChanged(val state: PresenceState, val timestamp: Instant) : DisplayModeEvent

    data class SwipeToggle(val timestamp: Instant) : DisplayModeEvent

    data class TimerTick(val timestamp: Instant) : DisplayModeEvent
}
