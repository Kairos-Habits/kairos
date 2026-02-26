# Domain Rules

Applies to: Cross-platform domain logic and invariants

## Core Invariants

### Completion must reference valid session
**Applies to**: ChecklistCompletion events
**Rule**: A completion event must always reference an existing, active ChecklistSession.

### Mode must be LEAVING or ARRIVING
**Applies to**: Task mode assignment
**Rule**: Every task must be assigned to exactly one mode: LEAVING or ARRIVING.

```kotlin
enum class Mode {
    LEAVING,
    ARRIVING
}
```

### Event IDs must be unique per device
**Applies to**: All event types
**Rule**: Each device generates unique event IDs. Use UUID or device-sequence combination.

### Timestamps must be monotonic per device
**Applies to**: All events from a single device
**Rule**: Events from the same device must have monotonically increasing timestamps.

## Design Principles (ADHD-Optimized)

### No streak counters
**Applies to**: UI, analytics, notifications
**Rule**: Never display or track consecutive day streaks. This triggers shame when broken.

### No missed day counts
**Applies to**: UI, analytics
**Rule**: Never count or display how many days were missed. This is shame-inducing.

### Neutral language only
**Applies to**: All user-facing text
**Rule**: Use neutral, non-judgmental language. No "you failed", "you forgot", "you should have".

```
// ✅ Correct
"Welcome back"
"Ready to continue?"

// ❌ Avoid
"You missed 3 days"
"You broke your streak"
"You should have checked in yesterday"
```

### Partial completion is valid
**Applies to**: Checklist sessions
**Rule**: Completing some tasks is valid and should be recorded positively. No all-or-nothing completion.

### Context-based triggers over time-based
**Applies to**: Task reminders, scheduling
**Rule**: Prefer event-based triggers ("after breakfast", "when leaving") over time-based triggers ("at 7:00 AM").

## Sync Semantics

### Sync must be idempotent
**Applies to**: Sync operations
**Rule**: Running sync multiple times with the same events must produce the same result.

### Merge deterministically
**Applies to**: Conflict resolution
**Rule**: When events conflict, merge using deterministic rules (e.g., last-write-wins with device ID tiebreaker).

### Eventual consistency is acceptable
**Applies to**: All sync operations
**Rule**: Design for eventual consistency. Real-time sync is not required.

## Presence State Machine

### Valid state transitions
**Applies to**: Presence state
**Rule**: Only these transitions are valid:

```
IDLE -> PRESENCE_DETECTED (on PRESENT event)
PRESENCE_DETECTED -> CHECKLIST_ACTIVE (on user interaction)
CHECKLIST_ACTIVE -> COOLDOWN (on completion)
COOLDOWN -> IDLE (on cooldown expiry)
PRESENCE_DETECTED -> IDLE (on ABSENT event, no interaction)
```

### Debounce at hardware boundary
**Applies to**: Presence detection
**Rule**: Debounce PRESENT/ABSENT events in ESP32 firmware. Kiosk should not re-debounce.

## Data Flow

### Events flow one direction
**Applies to**: Event propagation
**Rule**: Events originate on device, flow to Supabase. Query flows from Supabase to device.

### Local storage is cache
**Applies to**: Device storage
**Rule**: Treat local SQLite as a cache. Supabase is authoritative. Devices can rebuild from Supabase.
