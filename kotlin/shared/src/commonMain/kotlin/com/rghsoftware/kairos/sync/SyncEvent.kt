package com.rghsoftware.kairos.sync

import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Base sealed interface for all syncable domain events.
 * Events are immutable, append-only records that form the sync log.
 */
@OptIn(ExperimentalUuidApi::class)
sealed interface SyncEvent {
    val eventId: Uuid
    val timestamp: Instant
    val deviceId: String

    /**
     * Event type discriminator for serialization.
     */
    val eventType: String

    data class TaskCompleted(
        override val eventId: Uuid = Uuid.random(),
        override val timestamp: Instant = Instant.DISTANT_PAST,
        override val deviceId: String,
        val taskId: String,
        val completedAt: Instant,
    ) : SyncEvent {
        override val eventType: String = "TaskCompleted"
    }

    data class ChecklistSessionStarted(
        override val eventId: Uuid = Uuid.random(),
        override val timestamp: Instant = Instant.DISTANT_PAST,
        override val deviceId: String,
        val sessionId: String,
        val triggeredBy: String,
    ) : SyncEvent {
        override val eventType: String = "ChecklistSessionStarted"
    }

    data class ChecklistSessionCompleted(
        override val eventId: Uuid = Uuid.random(),
        override val timestamp: Instant = Instant.DISTANT_PAST,
        override val deviceId: String,
        val sessionId: String,
        val completedAt: Instant,
    ) : SyncEvent {
        override val eventType: String = "ChecklistSessionCompleted"
    }

    /**
     * A task was toggled (completed or uncompleted) during a session.
     */
    data class TaskToggledInSession(
        override val eventId: Uuid = Uuid.random(),
        override val timestamp: Instant = Instant.DISTANT_PAST,
        override val deviceId: String,
        val sessionId: String,
        val taskId: String,
        val isCompleted: Boolean,
    ) : SyncEvent {
        override val eventType: String = "TaskToggledInSession"
    }
}
