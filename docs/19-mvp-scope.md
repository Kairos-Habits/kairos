# Kairos – MVP Scope

## 1. Purpose

This document defines the minimum viable system required
to validate the Kairos architecture and front-door habit flow.

The MVP focuses on:

- Presence-triggered checklist
- Local event persistence
- Deterministic sync
- Basic task configuration

No advanced scheduling or push notifications required.

---

## 2. Devices Included in MVP

Included:

- Raspberry Pi 4 (Compose Desktop kiosk)
- ESP32 presence node (LD2410 class)
- Supabase backend
- SvelteKit web app (basic configuration)
- Android app (basic task completion + sync)

Not included:

- WearOS
- FCM push cancellation
- BLE proximity confirmation
- Door reed switch
- Computer vision

---

## 3. MVP Feature Set

### 3.1 Presence Detection

- mmWave radar connected to ESP32
- Debounced PRESENT / ABSENT emission
- JSONL over USB
- Kiosk state machine activation

---

### 3.2 Kiosk Checklist

- Two large buttons:
      LEAVING
      ARRIVING
- Display 3–8 tasks per mode
- Record ChecklistSession
- Record ChecklistCompletion events
- Cooldown window after completion

No adaptive ordering required yet.

---

### 3.3 Local Storage

Kiosk must persist:

- PresenceEvent
- ChecklistSession
- ChecklistCompletion
- Sync metadata

Storage: SQLite

---

### 3.4 Sync

Kiosk and Android must:

- Push new completion events
- Pull remote task updates
- Use last_sync_at timestamp
- Merge deterministically

Eventual consistency acceptable.

No realtime.

---

### 3.5 Web Application

Web must support:

- Task CRUD
- Mode assignment (LEAVING / ARRIVING)
- Basic history view

Web is online-first.
No offline logic required.

---

### 3.6 Android Application

Android must support:

- Viewing tasks
- Marking completion
- Local storage
- Sync push/pull

Notifications optional in MVP.

---

## 4. Explicitly Deferred

The following are NOT MVP:

- Recurrence engine
- Time-based scheduling
- Analytics dashboards
- Adaptive ordering
- BLE integration
- Multi-sensor fusion
- CV confirmation
- Cross-device notification cancellation

---

## 5. Acceptance Criteria

MVP is considered complete when:

1. Presence triggers kiosk checklist reliably.
2. Completing checklist creates events locally.
3. Events sync to Supabase.
4. Android reflects completions after sync.
5. Web reflects updated task list.
6. System functions without internet temporarily.

---

## 6. Architectural Constraints

- Contracts defined in Kotlin shared module.
- Android application in separate subproject (AGP 9 compatible).
- Compose Desktop kiosk in JVM target.
- Web app uses generated TypeScript types.
- Firmware isolated under firmware/ directory.

---

## 7. Exit Condition for MVP

System is stable when:

- 7 consecutive days of usage without data loss.
- No duplicate event creation.
- No inconsistent task state across devices.
- Presence detection false positives < acceptable threshold.
