package com.rghsoftware.kairos.checklist

import com.rghsoftware.kairos.domain.Mode
import com.rghsoftware.kairos.domain.SessionId
import com.rghsoftware.kairos.domain.TaskId
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents the current state of the checklist flow.
 * This is the primary state used by the kiosk UI to determine what to display.
 */
@OptIn(ExperimentalUuidApi::class)
data class ChecklistFlowState(
    val sessionState: SessionState = SessionState.Idle,
    val cooldownUntil: Instant? = null,
) {
    val isActive: Boolean
        get() = sessionState !is SessionState.Idle && sessionState !is SessionState.Cooldown

    val isInCooldown: Boolean
        get() = sessionState is SessionState.Cooldown
}
sealed interface SessionState {
    data object Idle : SessionState
    data object SelectingMode : SessionState
    @OptIn(ExperimentalUuidApi::class)
    data class InSession(
        val sessionId: SessionId,
        val mode: Mode,
        val completedTaskIds: Set<TaskId> = emptySet(),
        val sessionStartedAt: Instant,
    ) : SessionState {
        val completedCount: Int
            get() = completedTaskIds.size
    }
    data class Cooldown(
        val previousSessionId: SessionId,
        val mode: Mode,
    ) : SessionState
}
