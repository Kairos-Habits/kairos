package com.rghsoftware.kairos.checklist


import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Configuration for the checklist flow behavior.
 */
@OptIn(ExperimentalUuidApi::class)
data class ChecklistFlowConfig(
    val cooldownDuration: Duration = DEFAULT_COOLDOWN_DURATION,
    val autoShowOnPresence: Boolean = true,
) {
    companion object {
        val DEFAULT_COOLDOWN_DURATION: Duration = 90.seconds
    }
}
