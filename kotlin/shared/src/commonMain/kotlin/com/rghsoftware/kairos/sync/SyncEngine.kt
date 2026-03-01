package com.rghsoftware.kairos.sync

import kotlin.uuid.ExperimentalUuidApi

/**
 * Deterministic merge logic for sync events.
 * Events are ordered by timestamp, with ties broken by event ID.
 */
@OptIn(ExperimentalUuidApi::class)
object SyncEngine {
    /**
     * Merges local and remote events deterministically.
     * Returns the merged list in chronological order.
     */
    fun merge(
        localEvents: List<SyncEvent>,
        remoteEvents: List<SyncEvent>,
    ): List<SyncEvent> {
        val allEvents = (localEvents + remoteEvents)
            .distinctBy { it.eventId }
            .sortedWith(compareBy({ it.timestamp }, { it.eventId.toString() }))
        return allEvents
    }

    /**
     * Detects conflicts between local and remote events.
     * A conflict occurs when the same entity has different events at overlapping times.
     */
    fun detectConflicts(
        localEvents: List<SyncEvent>,
        remoteEvents: List<SyncEvent>,
    ): List<SyncConflict> {
        val localByEntity = localEvents.groupBy { it.entityId }
        val remoteByEntity = remoteEvents.groupBy { it.entityId }

        return localByEntity.keys.intersect(remoteByEntity.keys).mapNotNull { entityId ->
            val local = localByEntity[entityId] ?: emptyList()
            val remote = remoteByEntity[entityId] ?: emptyList()

            if (hasConflict(local, remote)) {
                SyncConflict(
                    entityId = entityId,
                    localEvents = local,
                    remoteEvents = remote,
                )
            } else {
                null
            }
        }
    }

    private fun hasConflict(
        local: List<SyncEvent>,
        remote: List<SyncEvent>,
    ): Boolean {
        val localIds = local.map { it.eventId }.toSet()
        val remoteIds = remote.map { it.eventId }.toSet()
        return localIds != remoteIds
    }

    /**
     * Resolves a conflict using last-write-wins strategy.
     */
    fun resolveConflict(conflict: SyncConflict): SyncEvent? {
        val allEvents = conflict.localEvents + conflict.remoteEvents
        return allEvents.maxByOrNull { it.timestamp }
    }
}

/**
 * Represents a detected sync conflict.
 */
data class SyncConflict(
    val entityId: String,
    val localEvents: List<SyncEvent>,
    val remoteEvents: List<SyncEvent>,
)

/**
 * Extension to extract entity ID from sync events.
 * Each event type defines its primary entity.
 */
private val SyncEvent.entityId: String
    get() = when (this) {
        is SyncEvent.TaskCompleted -> taskId
        is SyncEvent.ChecklistSessionStarted -> sessionId
        is SyncEvent.ChecklistSessionCompleted -> sessionId
        is SyncEvent.TaskToggledInSession -> "$sessionId:$taskId"
    }
