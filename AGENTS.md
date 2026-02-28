# Kairos - ADHD-Optimized Habit Building System

**Generated:** 2026-02-28 | **Commit:** 7dd282c | **Branch:** main

Multi-node, offline-first habit tracking system targeting Android, Raspberry Pi kiosk, and web.

## Project Structure

```
kairos/
├── kotlin/           # Kotlin Multiplatform (Android + Desktop kiosk)
│   ├── shared/       # Domain entities, sync, display logic (pure Kotlin)
│   ├── kiosk/        # Raspberry Pi desktop app (Compose Desktop)
│   └── androidApp/   # Android mobile app
├── web/              # SvelteKit 5 web application
├── firmware/         # ESP32 presence detection firmware
│   └── presence-esp32/  # mmWave radar + JSONL protocol
└── docs/             # Architecture and PRD documentation
```

## Where to Look

| Task | Location | Notes |
|------|----------|-------|
| Domain entities | `kotlin/shared/src/commonMain/` | Task, Mode, PresenceState |
| Sync logic | `kotlin/shared/.../sync/` | Event-driven, idempotent sync |
| Display state machine | `kotlin/shared/.../display/` | DisplayModeStateMachine, reduce() pattern |
| Serial protocol | `kotlin/kiosk/.../serial/` | JSONL parsing from ESP32 |
| ESP32 firmware | `firmware/presence-esp32/main/` | mmWave sensor, presence debounce |
| Web UI | `web/src/routes/` | SvelteKit routes |
| Rules/conventions | `.factory/rules/` | Kotlin, Svelte, embedded-c, domain |

## Architecture Overview

### System Topology

```
[ESP32 mmWave Radar] --USB CDC/JSONL--> [Raspberry Pi Kiosk]
                                              |
                                    Compose Desktop + SQLite
                                              |
                                       Periodic Sync
                                              |
                                         Supabase
                                              |
                              --------------------
                              |                  |
                         [Android]          [Web App]
                        (Compose)         (SvelteKit)
```

### Component Roles

- **:shared** — Domain entities, event schemas, sync semantics, invariants, scheduling. Pure platform-agnostic Kotlin.
- **:androidApp** — Mobile client with local DB, background sync, notifications.
- **:kiosk** — Presence-triggered checklist UI, serial ingestion, local SQLite, sync loop.
- **Web (SvelteKit)** — Task CRUD, planning interface, analytics. Online-first.
- **ESP32 firmware** — Debounced presence detection, JSONL protocol over USB CDC.

### Key Invariants

1. **Dependency direction**: `:androidApp` and `:kiosk` → `:shared` (never reverse)
2. **Shared is pure**: No platform imports in commonMain; use expect/actual
3. **Sync is idempotent**: Event-driven, eventual consistency, deterministic merge
4. **Hardware isolation**: Firmware uses JSONL protocol; no platform dependencies
5. **Supabase is canonical**: Local SQLite is cache

## JSONL Serial Protocol

ESP32 → Kiosk (115200 baud, 8N1, USB CDC):

```json
{"type": "presence", "state": "PRESENT", "timestamp_ms": 1700000000000, "moving_cm": 150, "static_cm": 0}
{"type": "heartbeat", "timestamp_ms": 1700000005000}
```

- Presence events on state transition only (debounced: 1000ms present, 2000ms absent)
- Heartbeat every 5s for liveness
- Kiosk parses with `ignoreUnknownKeys=true`

## Design Philosophy (ADHD-Optimized)

- **Recovery over streaks** — Design for the return, not the streak
- **Context over time** — Event triggers ("after brushing teeth") not time-based
- **Shame-free** — No streak counters, no missed day counts, no judgment
- **Immediate feedback** — Every interaction provides instant positive feedback
- **No gamification** — No points, badges, or leaderboards

## Commands

### Kotlin
```bash
cd kotlin
./gradlew :shared:assemble      # Build shared module
./gradlew :kiosk:assemble       # Build desktop kiosk
./gradlew :androidApp:assembleDebug  # Build Android APK
./gradlew :kiosk:run            # Run desktop kiosk
```

### Web
```bash
cd web
bun run dev          # Development server
bun run build        # Production build
bun run check        # Type checking
bun run test:unit    # Vitest unit tests
bun run test         # All tests (unit + e2e Playwright)
```

### ESP32 Firmware
```bash
cd firmware/presence-esp32
idf.py build                     # Build (requires ESP-IDF)
idf.py -p /dev/ttyUSB0 flash     # Flash to device
idf.py -p /dev/ttyUSB0 monitor   # Serial monitor
```

## Coding Standards

See `.factory/rules/`:

- **kotlin.md** — KMP patterns, domain entities, event sourcing
- **svelte.md** — Svelte 5 runes, snippets, SvelteKit patterns
- **embedded-c.md** — ESP-IDF patterns, JSONL protocol, FreeRTOS
- **domain.md** — Cross-platform invariants, ADHD design principles

## Anti-Patterns (THIS PROJECT)

- **NEVER** import platform APIs in commonMain (use expect/actual)
- **NEVER** show streak counters, missed day counts, or shame language
- **NEVER** commit generated code (build/generated/ excluded)
- **AVOID** globals dependency in web (suspicious package)

## Notes

- Web uses tabs, single quotes, no trailing commas (Prettier)
- Kotlin uses 4-space indent, max 100 chars, trailing commas allowed
- Detekt warnings treated as errors
- CI: `.github/workflows/ci.yml` runs all three assemble tasks
- Entry points: `kotlin/kiosk/.../Main.kt`, `kotlin/androidApp/.../MainActivity.kt`, `firmware/.../presence_main.c`
