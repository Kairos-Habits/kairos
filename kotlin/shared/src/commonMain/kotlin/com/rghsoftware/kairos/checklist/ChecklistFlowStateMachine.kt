package com.rghsoftware.kairos.checklist

import com.rghsoftware.kairos.domain.Mode
import com.rghsoftware.kairos.domain.SessionId
import com.rghsoftware.kairos.domain.TaskId
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Result of a state transition in the checklist flow.
 */
@OptIn(ExperimentalUuidApi::class)
data class ChecklistFlowTransition(
    val newState: ChecklistFlowState,
    val effect: TransitionEffect? = null,
)

/**
 * Side effects that should be performed after a state transition.
 */
sealed interface TransitionEffect {
    data class SessionStarted(
        val sessionId: SessionId,
        val mode: Mode,
        val timestamp: Instant,
    ) : TransitionEffect
    data class TaskCompletionToggled(
        val sessionId: SessionId,
        val taskId: TaskId,
        val isCompleted: Boolean,
        val timestamp: Instant,
    ) : TransitionEffect
    data class SessionFinished(
        val sessionId: SessionId,
        val mode: Mode,
        val completedTaskIds: Set<TaskId>,
        val timestamp: Instant,
    ) : TransitionEffect
}
/**
 * Reduces the current state with an event to produce a new state.
 * Pure function - no side effects.
 */
@OptIn(ExperimentalUuidApi::class)
fun ChecklistFlowState.reduce(
    event: ChecklistFlowEvent,
    config: ChecklistFlowConfig,
): ChecklistFlowTransition {
    val timestamp = extractTimestamp(event)
    return when (event) {
        is ChecklistFlowEvent.PresenceChanged ->
            handlePresenceChanged(event, config, timestamp)
        is ChecklistFlowEvent.ModeSelected ->
            handleModeSelected(event, timestamp)
        is ChecklistFlowEvent.TaskToggled ->
            handleTaskToggled(event, timestamp)
        is ChecklistFlowEvent.SessionCompleted ->
            handleSessionCompleted(event, config, timestamp)
        is ChecklistFlowEvent.TimerTick ->
            handleTimerTick(event, config)
        is ChecklistFlowEvent.Cancelled ->
            handleCancelled(timestamp)
    }
}
private fun extractTimestamp(event: ChecklistFlowEvent): Instant =
    when (event) {
        is ChecklistFlowEvent.PresenceChanged -> event.timestamp
        is ChecklistFlowEvent.ModeSelected -> event.timestamp
        is ChecklistFlowEvent.TaskToggled -> event.timestamp
        is ChecklistFlowEvent.SessionCompleted -> event.timestamp
        is ChecklistFlowEvent.TimerTick -> event.timestamp
        is ChecklistFlowEvent.Cancelled -> event.timestamp
    }
@OptIn(ExperimentalUuidApi::class)
private fun ChecklistFlowState.handlePresenceChanged(
    event: ChecklistFlowEvent.PresenceChanged,
    config: ChecklistFlowConfig,
    timestamp: Instant,
): ChecklistFlowTransition {
    if (!event.isPresent) {
        return when (sessionState) {
            is SessionState.Cooldown -> ChecklistFlowTransition(newState = this)
            else -> ChecklistFlowTransition(
                newState = ChecklistFlowState(
                    sessionState = SessionState.Idle,
                    cooldownUntil = cooldownUntil,
                ),
            )
        }
    }
    return when (sessionState) {
        is SessionState.Idle -> {
            val isInCooldown = cooldownUntil != null && timestamp < cooldownUntil
            if (isInCooldown || !config.autoShowOnPresence) {
                ChecklistFlowTransition(newState = this)
            } else {
                ChecklistFlowTransition(
                    newState = ChecklistFlowState(
                        sessionState = SessionState.SelectingMode,
                        cooldownUntil = null,
                    ),
                )
            }
        }
        else -> ChecklistFlowTransition(newState = this)
    }
}
@OptIn(ExperimentalUuidApi::class)
private fun ChecklistFlowState.handleModeSelected(
    event: ChecklistFlowEvent.ModeSelected,
    timestamp: Instant,
): ChecklistFlowTransition {
    if (sessionState !is SessionState.SelectingMode) {
        return ChecklistFlowTransition(newState = this)
    }
    val sessionId = SessionId(Uuid.random().toString())
    val newSessionState = SessionState.InSession(
        sessionId = sessionId,
        mode = event.mode,
        completedTaskIds = emptySet(),
        sessionStartedAt = timestamp,
    )
    return ChecklistFlowTransition(
        newState = ChecklistFlowState(
            sessionState = newSessionState,
            cooldownUntil = null,
        ),
        effect = TransitionEffect.SessionStarted(
            sessionId = sessionId,
            mode = event.mode,
            timestamp = timestamp,
        ),
    )
}
@OptIn(ExperimentalUuidApi::class)
private fun ChecklistFlowState.handleTaskToggled(
    event: ChecklistFlowEvent.TaskToggled,
    timestamp: Instant,
): ChecklistFlowTransition {
    val inSession = sessionState as? SessionState.InSession
        ?: return ChecklistFlowTransition(newState = this)
    val newCompletedIds = if (event.taskId in inSession.completedTaskIds) {
        inSession.completedTaskIds - event.taskId
    } else {
        inSession.completedTaskIds + event.taskId
    }
    val newSessionState = inSession.copy(completedTaskIds = newCompletedIds)
    return ChecklistFlowTransition(
        newState = ChecklistFlowState(
            sessionState = newSessionState,
            cooldownUntil = cooldownUntil,
        ),
        effect = TransitionEffect.TaskCompletionToggled(
            sessionId = inSession.sessionId,
            taskId = event.taskId,
            isCompleted = event.taskId !in inSession.completedTaskIds,
            timestamp = timestamp,
        ),
    )
}
@OptIn(ExperimentalUuidApi::class)
private fun ChecklistFlowState.handleSessionCompleted(
    event: ChecklistFlowEvent.SessionCompleted,
    config: ChecklistFlowConfig,
    timestamp: Instant,
): ChecklistFlowTransition {
    val inSession = sessionState as? SessionState.InSession
        ?: return ChecklistFlowTransition(newState = this)
    val cooldownState = SessionState.Cooldown(
        previousSessionId = inSession.sessionId,
        mode = inSession.mode,
    )
    return ChecklistFlowTransition(
        newState = ChecklistFlowState(
            sessionState = cooldownState,
            cooldownUntil = timestamp + config.cooldownDuration,
        ),
        effect = TransitionEffect.SessionFinished(
            sessionId = inSession.sessionId,
            mode = inSession.mode,
            completedTaskIds = inSession.completedTaskIds,
            timestamp = timestamp,
        ),
    )
}
@OptIn(ExperimentalUuidApi::class)
private fun ChecklistFlowState.handleTimerTick(
    event: ChecklistFlowEvent.TimerTick,
    config: ChecklistFlowConfig,
): ChecklistFlowTransition {
    if (cooldownUntil != null && event.timestamp >= cooldownUntil) {
        return ChecklistFlowTransition(
            newState = ChecklistFlowState(
                sessionState = SessionState.Idle,
                cooldownUntil = null,
            ),
        )
    }
    return ChecklistFlowTransition(newState = this)
}
@OptIn(ExperimentalUuidApi::class)
private fun ChecklistFlowState.handleCancelled(
    timestamp: Instant,
): ChecklistFlowTransition {
    return ChecklistFlowTransition(
        newState = ChecklistFlowState(
            sessionState = SessionState.Idle,
            cooldownUntil = null,
        ),
    )
}
