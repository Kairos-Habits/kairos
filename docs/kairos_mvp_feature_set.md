# Kairos -- MVP Feature Set

Ordered by Implementation Sequence

------------------------------------------------------------------------

# 1. Project Restructure (AGP 9 Migration) [Completed]

## Summary

Restructure the Kotlin project to comply with AGP 9 by separating
Android application logic from the Kotlin Multiplatform shared module.

## Scope

- Create :shared KMP library module
- Create :androidApp pure Android application module
- Convert :kiosk to JVM-only Compose Desktop app
- Ensure dependency direction: androidApp/kiosk → shared

## Acceptance Criteria

- All modules build independently
- No com.android.application in shared
- Gradle 9 + AGP 9 working correctly

------------------------------------------------------------------------

# 2. Core Sync Skeleton

## Summary

Implement minimal event-based sync between devices and Supabase.

## Scope

- Local append-only event recording
- Push unsynced events upstream
- Pull deltas since last_sync_at
- Deterministic merge logic

## Acceptance Criteria

- Task completion syncs between Android and Kiosk
- No duplicate event application
- Offline operation supported

------------------------------------------------------------------------

# 3. Presence Detection (Hardware Integration)

## Summary

Integrate ESP32 mmWave sensor with kiosk via JSONL over USB CDC.

## Scope

- JSONL protocol
- Debounced PRESENT / ABSENT emission
- Heartbeat support
- Serial listener in kiosk

## Acceptance Criteria

- Stable presence detection
- No flapping
- Serial disconnect handled gracefully

------------------------------------------------------------------------

# 4. Display Mode State Machine (HA ↔ Kiosk)

## Summary

Implement a shared, deterministic state machine controlling display mode
switching.

## Default Configuration

- Manual override: 5 minutes
- Auto-switch cooldown: 90 seconds
- No automatic return to HA

## Functional Requirements

- HA → Kiosk on stable presence
- Swipe always toggles display
- Manual override blocks auto-switching
- Cooldown prevents flapping

## Acceptance Criteria

- All transitions unit tested
- Pure shared implementation
- No UI-layer policy logic

------------------------------------------------------------------------

# 5. Kiosk Checklist Flow

## Summary

Implement presence-triggered checklist sessions.

## Scope

- LEAVING / ARRIVING selection
- ChecklistSession creation
- ChecklistCompletion recording
- Cooldown state

## Acceptance Criteria

- Presence triggers checklist display
- Sessions recorded locally
- Completion events sync correctly

------------------------------------------------------------------------

# 6. Web Task Configuration (SvelteKit)

## Summary

Provide online-first task CRUD and history viewing.

## Scope

- Create / edit / delete tasks
- Assign LEAVING / ARRIVING modes
- View completion history

## Acceptance Criteria

- Changes propagate to Android and Kiosk via sync
- Generated TypeScript types from Kotlin contracts

------------------------------------------------------------------------

# 7. Android Task Interaction

## Summary

Provide Android client for task viewing and completion.

## Scope

- Display task list
- Mark completion
- Background sync

## Acceptance Criteria

- Completion syncs upstream
- State consistent across devices

------------------------------------------------------------------------

# Deferred (Post-MVP)

- BLE proximity confirmation
- Door sensor integration
- Auto-return to HA
- Adaptive task ordering
- Analytics dashboards
- CV integration (Jetson Nano)

------------------------------------------------------------------------

End of Document
