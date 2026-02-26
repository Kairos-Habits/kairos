# Front Door Habit Gate – Architecture Update

## 1. Overview

This document defines the front-door kiosk subsystem within Kairos.

It incorporates:

- Raspberry Pi 4 as dedicated kiosk device
- 15" HDMI touchscreen (USB touch input)
- ESP32 sensor node
- 24 GHz mmWave radar (LD2410 class)
- Compose Desktop application (JVM)
- Local SQLite storage
- Supabase backend (eventual consistency)

The kiosk is a first-class system node, not merely a display.

---

## 2. System Topology

Physical flow:

    [ mmWave Radar ]
            |
         UART
            |
         [ ESP32 ]
            |
     USB CDC (JSONL)
            |
    [ Raspberry Pi 4 ]
            |
     Compose Desktop Kiosk
            |
        Local SQLite
            |
        Sync Worker
            |
         Supabase

Optional future enhancement:

    Add Jetson Nano for CV confirmation
    OR replace Pi with Jetson

Sensor interface remains unchanged.

---

## 3. Responsibilities by Device

### 3.1 ESP32 – Sensor Node

Responsibilities:

- Interface with mmWave radar via UART
- Apply debounce + smoothing
- Emit edge-triggered presence events
- Provide periodic heartbeat

Output format: JSONL over USB CDC.

Example message:

    {
      "type": "presence",
      "state": "PRESENT",
      "timestamp_ms": 1700000000000
    }

Debounce rules:

- Presence stable >= 1000 ms before emitting PRESENT
- Absence stable >= 2000 ms before emitting ABSENT
- Emit only on state transitions

Mounting assumptions:

- Height: 1.3–1.6 m
- Tilt: 5–15 degrees downward
- Zones configured to ignore floor reflections

Firmware is fully isolated from UI logic.

---

### 3.2 Raspberry Pi 4 – Kiosk Node

Responsibilities:

- Fullscreen Compose Desktop UI
- Serial ingestion
- Presence state machine execution
- Checklist session management
- Local SQLite storage
- Periodic sync with Supabase

The kiosk does NOT:

- Parse radar-level signals
- Contain divergent domain rules
- Own canonical state

---

## 4. Presence State Machine

States:

- IDLE
- PRESENCE_DETECTED
- CHECKLIST_ACTIVE
- COOLDOWN

Transitions:

IDLE → PRESENCE_DETECTED  
    On PRESENT event

PRESENCE_DETECTED → CHECKLIST_ACTIVE  
    When presence stable threshold satisfied

CHECKLIST_ACTIVE → COOLDOWN  
    On completion or timeout

COOLDOWN → IDLE  
    After cooldown duration (default 90 seconds)

Rules:

- PRESENT events ignored during COOLDOWN
- ABSENT does not immediately dismiss checklist
- State transitions must be deterministic

State machine definition resides in shared module.

---

## 5. Checklist Flow

Initial interaction:

1. Presence detected.
2. Kiosk displays mode selection:
       LEAVING
       ARRIVING
3. User selects mode.
4. Checklist rendered.
5. User completes tasks.
6. Session recorded.
7. Enter cooldown.

Local entities recorded:

- PresenceEvent
- ChecklistSession
- ChecklistCompletion

Events are append-only.

---

## 6. Data Model Additions (Kiosk Context)

PresenceEvent:

- id
- state (PRESENT | ABSENT)
- timestamp

ChecklistSession:

- id
- started_at
- ended_at
- mode (LEAVING | ARRIVING)

ChecklistCompletion:

- id
- task_id
- session_id
- completed_at
- context (LEAVING | ARRIVING)

These sync upstream via shared sync engine.

---

## 7. Sync Model

Kiosk operates offline-first.

Sync loop:

- Push unsynced completion events
- Pull task updates since last_sync_at
- Apply merge rules deterministically

No Supabase Realtime required.

Network failure does not block UI.

---

## 8. Failure Handling

### Serial Disconnect

- Detect loss of heartbeat
- Log warning
- Continue kiosk operation
- Display optional diagnostic indicator

### Network Loss

- Queue events locally
- Retry sync with backoff
- Do not block checklist flow

### Application Crash

- Systemd restarts service
- Local DB preserves state

---

## 9. Jetson Nano Upgrade Path

If advanced features required:

Use cases:

- Computer vision person detection
- Multi-sensor fusion
- Identity confirmation

Two options:

1. Replace Pi with Jetson
2. Keep Pi for UI and add Jetson as auxiliary node

In both cases:

- ESP32 interface unchanged
- Shared Kotlin core unchanged
- Supabase sync unchanged

Modularity preserved.

---

## 10. Design Principles Reinforced

- Sensor layer isolated
- Deterministic state machine
- Debounce at hardware boundary
- UI decoupled from core logic
- Offline-first behavior
- Eventual consistency
- Gradual complexity growth

---

## 11. Open Decisions

- Final mmWave model selection
- Cooldown duration tuning
- BLE proximity confirmation (phase 2)
- Door reed switch integration
- CV confirmation threshold (future)
