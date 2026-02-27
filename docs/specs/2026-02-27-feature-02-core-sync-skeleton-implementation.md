## Core Sync Skeleton Implementation Spec

### Overview
Implement event-sourced sync layer in `:shared` module with Supabase backend, following existing patterns from `DisplayModeStateMachine`.

### Architecture

**1. Domain Events (new files in `:shared/commonMain`)**
```
com.rghamilton3.workspace.kairos.sync/
├── SyncEvent.kt              # Sealed interface for all sync events
├── SyncState.kt              # Sync status enum (LOCAL_ONLY, PENDING_SYNC, SYNCED, CONFLICT)
├── SyncMetadata.kt           # last_sync_at tracking
├── SyncRepository.kt         # Platform-agnostic interface
├── SyncEngine.kt             # Deterministic merge logic
└── SyncConfig.kt             # Configuration constants
```

**2. Event Types (following DisplayModeEvent pattern)**
```kotlin
sealed interface SyncEvent {
    data class TaskCompleted(val taskId: String, val completedAt: Instant) : SyncEvent
    data class ChecklistSessionStarted(val sessionId: String, val triggeredBy: String) : SyncEvent
    data class ChecklistSessionCompleted(val sessionId: String) : SyncEvent
}
```

**3. Supabase Dependencies (add to `:shared/build.gradle.kts`)**
```kotlin
implementation("io.github.jan-tennert.supabase:postgrest-kt:2.x.x")
implementation("io.github.jan-tennert.supabase:auth-kt:2.x.x")
// Ktor engines per platform source set
```

**4. Platform Implementations**
- `androidMain`: Ktor CIO engine, Room for local queue
- `jvmMain`: Ktor CIO engine, SQLite JDBC for local queue

**5. Sync Flow**
1. Local write → Append to `pending_changes` (status: LOCAL_ONLY)
2. Background sync → Push unsynced events to Supabase
3. Pull deltas since `last_sync_at`
4. Deterministic merge by event timestamp
5. Update status to SYNCED, increment version

### Acceptance Criteria Mapping
- ✅ Local append-only: `pending_changes` table with monotonic version
- ✅ Push unsynced: `SyncRepository.pushPending()`
- ✅ Pull deltas: `SyncRepository.pullSince(lastSyncAt)`
- ✅ Deterministic merge: `SyncEngine.merge()` orders by timestamp
- ✅ No duplicates: Idempotent upsert by event ID
- ✅ Offline support: Local queue persists, syncs when online

### Implementation Order
1. Add Supabase dependencies to `:shared`
2. Create domain events and state in `:shared/commonMain`
3. Define `SyncRepository` interface
4. Implement `SyncEngine` with merge logic
5. Add platform-specific storage (start with JVM for kiosk testing)
6. Wire up sync loop in `:kiosk` app

### Questions for Decision
- Local storage: Use SQLDelight (multiplatform) vs platform-specific (Room/JDBC)?
- Sync trigger: Manual, timer-based (every 5 min), or connectivity-based?
- Conflict resolution: Last-write-wins or custom per-entity?