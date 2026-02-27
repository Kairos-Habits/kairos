package com.rghsoftware.kairos.serial

import kotlinx.datetime.Instant

sealed interface PresenceEvent {
    val timestampMs: Long

    data class Presence(
        val state: PresenceState,
        override val timestampMs: Long,
        val movingCm: Int = 0,
        val staticCm: Int = 0,
    ) : PresenceEvent {
        val instant: Instant get() = Instant.fromEpochMilliseconds(timestampMs)
    }

    data class Heartbeat(
        override val timestampMs: Long,
    ) : PresenceEvent {
        val instant: Instant get() = Instant.fromEpochMilliseconds(timestampMs)
    }
}

enum class PresenceState {
    PRESENT,
    ABSENT,
    ;

    companion object {
        fun fromString(value: String): PresenceState? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
