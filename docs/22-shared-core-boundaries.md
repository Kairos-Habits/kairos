# Kairos – Shared Core Boundaries

## 1. Purpose

This document defines what belongs inside the shared Kotlin
Multiplatform module (:shared) and what does not.

Clear boundaries prevent:

- Logic drift
- Platform divergence
- Hidden duplication
- Architectural erosion

The shared module is the semantic spine of Kairos.

---

## 2. What Belongs in :shared

The shared module contains pure, platform-agnostic logic.

### 2.1 Domain Contracts

Entities such as:

- Task
- ChecklistSession
- ChecklistCompletion
- PresenceEvent
- SyncState

These are defined in commonMain.

---

### 2.2 Invariants

Examples:

- Completion must reference valid session
- Mode must be LEAVING or ARRIVING
- Event IDs must be unique per device
- Timestamps must be monotonic per device

All invariant checks live here.

---

### 2.3 Sync Semantics

Shared responsibilities:

- Idempotency rules
- Merge strategy
- Conflict resolution
- Delta application
- Event serialization

Platform layers implement transport,
but logic for merging lives in shared.

---

### 2.4 Scheduling Engine (Future)

Future additions:

- Recurrence calculation
- Next occurrence resolution
- Time window filtering
- Notification eligibility logic

Scheduling logic must remain pure.

---

### 2.5 State Machine Definitions

Presence state transitions:

- IDLE
- PRESENCE_DETECTED
- CHECKLIST_ACTIVE
- COOLDOWN

Transitions defined in shared,
even if execution is platform-triggered.

---

## 3. What Does NOT Belong in :shared

The shared module must not depend on platform APIs.

Excluded:

- Android notifications
- SQLite drivers
- Serial port libraries
- HTTP client implementations
- UI code
- Compose functions

These live in platform or app modules.

---

## 4. Platform Layers

Platform-specific implementations wrap shared interfaces.

Examples:

### Android

- Room or SQLDelight storage
- NotificationManager integration
- WorkManager background sync

### Pi (JVM)

- JDBC or SQLDelight storage
- Serial listener
- Systemd integration

### Web

- No shared code usage
- Uses generated TypeScript types only

---

## 5. Dependency Direction Rule

Dependencies must flow inward:

Apps → Platform → Shared

Never:

Shared → Platform
Shared → Apps

This rule is strict.

---

## 6. Testing Strategy

Shared module must be heavily unit-tested.

Tests must verify:

- Merge correctness
- Conflict resolution
- State machine transitions
- Scheduling calculations

Platform layers test only integration behavior.

---

## 7. Evolution Strategy

When adding features:

1. Define contract in shared.
2. Define invariants in shared.
3. Add scheduling or merge logic in shared.
4. Implement platform adapters.
5. Update web type export.

Never implement logic in app layer first.

---

## 8. Architectural Guardrail

If business logic appears in:

- Android UI
- Kiosk UI
- Web UI

It must be migrated into shared.

The shared module is the system brain.
