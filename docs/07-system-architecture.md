# Kairos – System Architecture

## 1. Architectural Overview

Kairos is a multi-node, offline-first system composed of:

- Android mobile client
- Raspberry Pi kiosk (Compose Desktop)
- SvelteKit web application
- ESP32 sensor node(s)
- Supabase backend (canonical store)

The architecture is centered around:

- Kotlin-first domain contracts
- Append-only event model
- Deterministic sync
- Explicit state machines

---

## 2. High-Level Topology

System topology:

```text
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
       Periodic Sync
            |
         Supabase
            |
     --------------------
     |                  |
 [ Android ]        [ Web App ]
 (Compose)         (SvelteKit)
```

Notes:

- Web app does not maintain authoritative local state.
- Android and Pi maintain local storage.
- Supabase is canonical but not realtime-dependent.

---

## 3. Kotlin Multiplatform Spine

The Kotlin workspace is the semantic core.

Gradle modules (inside `kotlin/`):

```text
    :shared
        - contracts
        - invariants
        - sync
        - scheduling

    :androidApp
        - Android entrypoint
        - UI
        - Notification integration

    :kiosk
        - JVM target
        - Compose Desktop UI
        - Serial ingestion
        - Local DB
        - Sync loop
```

Key rule:

> `com.android.application` is applied ONLY in `:androidApp`
> `org.jetbrains.kotlin.multiplatform` is applied in `:shared`

This ensures AGP 9 compatibility.

---

## 4. Shared Core Responsibilities

The `:shared` module owns:

- Domain entities
- Event schemas
- Idempotency rules
- Merge semantics
- Scheduling calculations
- Validation invariants

It does NOT own:

- UI
- Android notifications
- Serial parsing
- Storage implementation details

Those belong to platform layers.

---

## 5. Sync Model

Sync model is event-driven and eventual.

Each device:

1. Writes events locally
2. Pushes new events upstream
3. Pulls remote deltas since `last_sync_at`
4. Applies merge rules deterministically

Event categories:

- Task completion
- Checklist session start/end
- Presence events (optional upstream)
- Configuration changes

No realtime WebSocket dependency required.

---

## 6. Presence Flow (Kiosk Node)

Presence flow:

```text
    ESP32 emits:
        PRESENT
        ABSENT
        HEARTBEAT

    Pi Serial Listener →
        Presence State Machine →
            IDLE
            PRESENCE_DETECTED
            CHECKLIST_ACTIVE
            COOLDOWN
```

Checklist activation is deterministic and debounced at hardware boundary.

---

## 7. Web Application Role

The SvelteKit web app:

- Uses generated TypeScript types from Kotlin contracts
- Performs CRUD operations
- Displays history and analytics
- Does not contain core sync logic

Web is a client of the system, not the system itself.

---

## 8. Android Role

Android app:

- Shares domain + sync core
- Maintains local DB
- Performs background sync
- Delivers notifications
- May integrate FCM later

WearOS will be added as an Android module variant later.

---

## 9. Upgrade Path – Jetson Nano

If computer vision is added:

- Option A:
  - Replace Pi with Jetson Nano
- Option B:
  - Keep Pi for UI
  - Add Jetson as auxiliary vision node

Sensor boundary remains unchanged.
Shared core remains unchanged.

---

## 10. Architectural Invariants

- Contracts originate in Kotlin shared module.
- Sync must be idempotent.
- Hardware layer is isolated.
- UI layer is replaceable.
- Supabase is canonical store.
- Eventual consistency is acceptable.
