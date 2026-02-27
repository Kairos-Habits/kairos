package com.rghsoftware.kairos.sync

/**
 * Represents the sync status of a local entity.
 */
enum class SyncState {
    LOCAL_ONLY,
    PENDING_SYNC,
    SYNCED,
    PENDING_DELETE,
    CONFLICT,
}
