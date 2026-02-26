# Kairos Project Memory

## Project Context
- **Name**: Kairos
- **Type**: ADHD-optimized habit building system
- **Stack**: Kotlin Multiplatform (Android + Desktop), SvelteKit (web), ESP-IDF (firmware), Supabase
- **Started**: February 2024

## Architecture Decisions

### 2024-02: Kotlin Multiplatform as Semantic Core
**Decision**: All domain contracts, invariants, and sync logic in Kotlin shared module
**Reasoning**: Android + future WearOS support, shared logic across platforms, deterministic invariants
**Trade-offs**: Two UI paradigms (Compose + SvelteKit), contract export pipeline required

### 2024-02: Compose Desktop for Kiosk
**Decision**: Use Compose Desktop (JVM) on Raspberry Pi instead of web kiosk
**Reasoning**: Reduced UI paradigm switching, tight integration with serial + SQLite, appliance-style reliability, no browser dependency
**Trade-offs**: JRE dependency on Pi, separate build from web

### 2024-02: SvelteKit for Web
**Decision**: SvelteKit for planning/configuration web interface
**Reasoning**: Modern, stable framework, strong TypeScript support, hosted independently
**Constraints**: Web is online-first, does not own sync semantics, uses generated TypeScript types from Kotlin contracts

### 2024-02: Event-Driven Sync (No Realtime)
**Decision**: Poll-based sync with eventual consistency, no WebSocket/realtime dependency
**Reasoning**: Realtime introduces complexity, eventual consistency sufficient for habit tracking use case
**Implementation**: Each device pushes new events, pulls remote deltas since `last_sync_at`, applies merge rules deterministically

### 2024-02: JSONL Serial Protocol
**Decision**: Line-delimited JSON over USB CDC for ESP32 communication
**Reasoning**: Human-readable, easy to debug, deterministic parsing, no binary framing required
**Details**: 115200 baud, 8N1, presence events debounced in firmware (1000ms present, 2000ms absent)

## Key Architectural Invariants

1. **Kotlin-first contracts**: All domain entities and invariants originate in Kotlin shared module
2. **Dependency direction**: Apps → Platform → Shared (never inward to shared)
3. **Sync is idempotent**: Event-driven, eventual consistency with deterministic merge
4. **Hardware isolation**: Firmware communicates via JSONL; no platform dependencies
5. **Supabase is canonical**: Local storage for offline, Supabase as source of truth
6. **No realtime dependency**: Poll-based sync is sufficient

## Module Boundaries

### In `:shared` (Kotlin commonMain)
- Domain entities (Task, ChecklistSession, ChecklistCompletion, PresenceEvent, SyncState)
- Event schemas
- Invariants and validation
- Sync semantics (idempotency, merge, conflict resolution)
- Scheduling calculations (future)
- State machine definitions (presence: IDLE, PRESENCE_DETECTED, CHECKLIST_ACTIVE, COOLDOWN)

### NOT in `:shared` (platform layers)
- Android notifications
- SQLite drivers
- Serial port libraries
- HTTP client implementations
- UI code / Compose functions

## Domain Knowledge

### Core Insight
> ADHD brains operate on an **interest-based nervous system** rather than importance-based motivation.

### Key Design Principles
- **Recovery over streaks**: Expect cycling between engagement and disengagement
- **Context over time**: Event-based triggers ("after brushing teeth") over time-based ("at 7:00 AM")
- **Shame-free**: No streak counters, no missed day counts, no judgment
- **Immediate feedback**: Every interaction provides instant positive feedback
- **No gamification**: No points, badges, or leaderboards

### Presence Flow
```
ESP32 → PRESENT/ABSENT/HEARTBEAT → Pi Serial Listener →
  Presence State Machine → IDLE | PRESENCE_DETECTED | CHECKLIST_ACTIVE | COOLDOWN
```

## MVP Scope

Included:
- Raspberry Pi 4 kiosk (Compose Desktop)
- ESP32 presence node (LD2410 mmWave)
- Supabase backend
- SvelteKit web app (basic configuration)
- Android app (basic task completion + sync)

Deferred:
- WearOS
- FCM push cancellation
- BLE proximity confirmation
- Computer vision

## Technical Debt
- [ ] Contract type generation from Kotlin to TypeScript (web consumes generated types)
- [ ] Scheduling engine in shared module

## Learnings

- [2026-02-26] Project initialized with AGENTS.md documenting architecture, commands, and design philosophy
