package com.rghsoftware.kairos.sync

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Configuration constants for sync behavior.
 */
object SyncConfig {
    /**
     * Default interval between sync attempts.
     */
    val SYNC_INTERVAL: Duration = 5.minutes

    /**
     * Maximum number of events to sync in a single batch.
     */
    const val MAX_BATCH_SIZE: Int = 100

    /**
     * Maximum retries for failed sync attempts.
     */
    const val MAX_RETRIES: Int = 3

    /**
     * Supabase table names.
     */
    const val TABLE_PENDING_CHANGES: String = "pending_changes"
    const val TABLE_SYNC_METADATA: String = "sync_metadata"
    const val TABLE_TASKS: String = "tasks"
    const val TABLE_CHECKLIST_SESSIONS: String = "checklist_sessions"

    /**
     * Event version for schema evolution.
     */
    const val EVENT_SCHEMA_VERSION: Int = 1
}
