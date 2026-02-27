package com.rghsoftware.kairos.display

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class DisplayModeConfig(
    val manualOverrideDuration: Duration = 5.minutes,
    val autoSwitchCooldown: Duration = 90.seconds,
)
