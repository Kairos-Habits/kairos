package com.rghsoftware.kairos.display

sealed interface PresenceState {
    data object Present : PresenceState
    data object Absent : PresenceState
    data object Unknown : PresenceState
}
