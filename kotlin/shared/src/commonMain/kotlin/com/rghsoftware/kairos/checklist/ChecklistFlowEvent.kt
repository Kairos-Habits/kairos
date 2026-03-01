package com.rghsoftware.kairos.checklist

import com.rghsoftware.kairos.domain.Mode
import com.rghsoftware.kairos.domain.SessionId
import com.rghsoftware.kairos.domain.TaskId
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Events that trigger state transitions in the checklist flow.
 */
sealed interface ChecklistFlowEvent {
    /**
     * Presence state changed from the sensor.
     * @property isPresent Whether presence is currently detected
     * @property timestamp When the presence change occurred
     */
    data class PresenceChanged(
        val isPresent: Boolean,
        val timestamp: Instant,
    ) : ChecklistFlowEvent

    /**
     * User selected a mode (LEAVING or ARRIVING) to start a session.
     * @property mode The selected mode
     * @property timestamp When the selection was made
     */
    data class ModeSelected(
        val mode: Mode,
        val timestamp: Instant,
    ) : ChecklistFlowEvent

    /**
     * User toggled a task's completion status.
     * @property taskId The task that was toggled
     * @property timestamp When the toggle occurred
     */
    data class TaskToggled(
        val taskId: TaskId,
        val timestamp: Instant,
    ) : ChecklistFlowEvent

    /**
     * User explicitly completed the session (e.g., "Done" button).
     * @property timestamp When the session was completed
     */
    data class SessionCompleted(
        val timestamp: Instant,
    ) : ChecklistFlowEvent

    /**
     * Timer tick for checking cooldown expiry.
     * @property timestamp Current time
     */
    data class TimerTick(
        val timestamp: Instant,
    ) : ChecklistFlowEvent

    /**
     * User cancelled out of the flow (e.g., "Skip" or navigate away).
     * @property timestamp When the cancellation occurred
     */
    data class Cancelled(
        val timestamp: Instant,
    ) : ChecklistFlowEvent
}
