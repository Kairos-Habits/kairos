package com.rghsoftware.kairos.sync

import kotlinx.datetime.Instant

/**
 * Tracks sync state per device.
 * Stored locally and synced to track last successful sync timestamp.
 */
data class SyncMetadata(
    val deviceId: String,
    val lastSyncAt: Instant?,
    val lastSyncVersion: Long = 0,
    val lastError: String? = null,
    val lastErrorAt: Instant? = null,
) {
    /**
     * Returns true if this device has completed at least one sync.
     */
    fun hasSynced(): Boolean = lastSyncAt != null
}
