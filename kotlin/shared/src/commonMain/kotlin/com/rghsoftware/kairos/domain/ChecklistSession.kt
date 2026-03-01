package com.rghsoftware.kairos.domain

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
/**
 * Value class for Session identifiers.
 * Prevents ID mix-ups between different entity types.
 */
@JvmInline
value class SessionId(val value: String)

/**
 * Represents a single checklist session triggered by presence detection.
 * Sessions track which tasks were completed during this interaction.
 *
 * @property id Unique identifier for this session
 * @property mode The context (LEAVING or ARRIVING) for this session
 * @property startedAt When the session was initiated
 * @property completedAt When the session was finished (null if in progress)
 * @property completedTaskIds Set of task IDs that were marked complete during this session
 */
@OptIn(ExperimentalUuidApi::class)
data class ChecklistSession(
    val id: SessionId = SessionId(Uuid.random().toString()),
    val mode: Mode,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val completedTaskIds: Set<TaskId> = emptySet(),
) {
    /**
     * Whether this session has been completed.
     */
    val isCompleted: Boolean
        get() = completedAt != null

    /**
     * Returns a new session with the task toggle applied.
     * Adding/removes the task from completedTaskIds.
     */
    fun toggleTask(taskId: TaskId): ChecklistSession {
        val newCompletedIds = if (taskId in completedTaskIds) {
            completedTaskIds - taskId
        } else {
            completedTaskIds + taskId
        }
        return copy(completedTaskIds = newCompletedIds)
    }

    /**
     * Returns a completed version of this session.
     */
    fun complete(at: Instant): ChecklistSession =
        copy(completedAt = at)
}
