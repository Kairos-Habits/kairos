package com.rghsoftware.kairos.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Platform-agnostic interface for sync operations.
 * Implementations handle local storage and remote sync via Supabase.
 */
interface SyncRepository {
    /**
     * Appends a new event to the local pending_changes queue.
     * Status is set to LOCAL_ONLY initially.
     */
    suspend fun appendEvent(event: SyncEvent)

    /**
     * Returns all events with status LOCAL_ONLY or PENDING_SYNC.
     */
    suspend fun getPendingEvents(limit: Int = SyncConfig.MAX_BATCH_SIZE): List<PendingChangeEvent>

    /**
     * Pushes pending events to Supabase and updates their status to SYNCED.
     * Returns the number of events successfully synced.
     */
    suspend fun pushPending(): Int

    /**
     * Pulls remote events created since the given timestamp.
     * Events are merged into local state via SyncEngine.
     */
    suspend fun pullSince(since: Instant): List<SyncEvent>

    /**
     * Gets the current sync metadata for this device.
     */
    suspend fun getSyncMetadata(): SyncMetadata?

    /**
     * Updates sync metadata after a successful sync.
     */
    suspend fun updateSyncMetadata(metadata: SyncMetadata)

    /**
     * Performs a full sync cycle: push pending, pull deltas, merge.
     * Returns sync result with counts.
     */
    suspend fun sync(): SyncResult

    /**
     * Observes pending event count for UI display.
     */
    fun observePendingCount(): Flow<Int>
}

/**
 * Represents a pending change awaiting sync.
 */
data class PendingChangeEvent(
    val id: String,
    val event: SyncEvent,
    val status: SyncState,
    val version: Long,
    val createdAt: Instant,
    val retryCount: Int = 0,
)

/**
 * Result of a sync operation.
 */
data class SyncResult(
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val mergedCount: Int = 0,
    val errorCount: Int = 0,
    val errors: List<String> = emptyList(),
) {
    val isSuccess: Boolean
        get() = errorCount == 0
}
