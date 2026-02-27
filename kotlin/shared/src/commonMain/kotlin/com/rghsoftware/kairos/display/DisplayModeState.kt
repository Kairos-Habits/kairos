package com.rghsoftware.kairos.display

import kotlin.time.Instant

data class DisplayModeState(
    val displayMode: DisplayMode = DisplayMode.HA,
    val presenceState: PresenceState = PresenceState.Unknown,
    val manualOverrideUntil: Instant? = null,
    val cooldownUntil: Instant? = null,
)
