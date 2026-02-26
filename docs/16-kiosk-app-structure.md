# Kairos – Kiosk Application Structure

## 1. Overview

This document defines the internal structure of the Raspberry Pi
Compose Desktop kiosk application.

The kiosk is:

- A JVM target inside the KMP workspace
- A thin UI layer over shared core logic
- Responsible for hardware ingestion and local persistence

It must remain modular so UI can evolve independently from logic.

---

## 2. Module Placement

Gradle structure (inside `kotlin/`):

```text
:shared
:androidApp
:kiosk
```

The kiosk module:

- Applies JVM target
- Uses Compose Desktop
- Depends on `:shared`

The kiosk does NOT contain domain contracts.

---

## 3. Internal Layers

Within `:kiosk`, structure should conceptually follow:

### 3.1 UI Layer

Responsibilities:

- Render full-screen Compose UI
- Display checklist
- Display mode selector
- Display diagnostics (optional)
- Emit user intents

UI must not:

- Contain business logic
- Directly mutate database
- Contain sync rules

---

### 3.2 Application Layer

Responsibilities:

- Translate UI intents into domain events
- Orchestrate state machine transitions
- Coordinate sync scheduling

This layer uses shared core modules.

---

### 3.3 Presence Integration Layer

Responsibilities:

- Listen to serial JSON lines
- Parse into `PresenceEvent` objects
- Feed events into presence state machine
- Handle reconnection logic

This layer is JVM-specific.

---

### 3.4 Persistence Layer

Responsibilities:

- Provide local storage implementation
- Store:
      - PresenceEvent
      - ChecklistSession
      - ChecklistCompletion
      - Sync metadata

Storage implementation is platform-specific
but interface contracts live in `:shared`.

---

### 3.5 Sync Layer

Responsibilities:

- Push local events to Supabase
- Pull remote deltas
- Apply merge rules
- Update last_sync_at

Uses shared sync logic.

---

## 4. Presence State Machine

States:

```text
    IDLE
    PRESENCE_DETECTED
    CHECKLIST_ACTIVE
    COOLDOWN
```

Transitions:

```text
    IDLE → PRESENCE_DETECTED (on PRESENT)
    PRESENCE_DETECTED → CHECKLIST_ACTIVE (stable presence)
    CHECKLIST_ACTIVE → COOLDOWN (completion or timeout)
    COOLDOWN → IDLE (after cooldown window)
```

State machine must be:

- Deterministic
- Explicit
- Testable
- Decoupled from UI

---

## 5. Event Flow

Flow within kiosk:

```text
    Serial Listener
        ↓
    PresenceEvent
        ↓
    State Machine
        ↓
    UI Trigger
        ↓
    User Action
        ↓
    ChecklistCompletion Event
        ↓
    Local DB
        ↓
    Sync Worker
```

Each arrow represents an explicit transformation.

---

## 6. Error Handling

Serial Failure:

- Mark presence subsystem degraded
- Continue operating UI manually

Sync Failure:

- Backoff and retry
- No UI interruption

DB Failure:

- Surface fatal error
- Prevent silent corruption

---

## 7. UI Design Constraints

- Touch-first
- Large hit targets
- Minimal navigation depth
- No scrolling where possible
- Clear success confirmation

---

## 8. Diagnostics Screen (Optional)

Recommended to include hidden diagnostics:

- Last presence event timestamp
- Current state machine state
- Sync status
- Serial connection status

Helps during hardware tuning.

---

## 9. Architectural Invariant

The kiosk is a client node.

It must not:

- Contain divergent domain rules
- Duplicate scheduling logic
- Become authoritative over Supabase

All core semantics live in `:shared`.
