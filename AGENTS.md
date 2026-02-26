# Kairos - ADHD-Optimized Habit Building System

Multi-node, offline-first habit tracking system targeting Android, Raspberry Pi kiosk, and web.

## Project Structure

```
kairos/
├── kotlin/           # Kotlin Multiplatform (Android + Desktop kiosk)
├── web/              # SvelteKit web application
├── firmware/         # ESP32 presence detection firmware
└── docs/             # Architecture and PRD documentation
```

## Commands

### Kotlin (Android + Desktop)

```bash
cd kotlin

# Run desktop kiosk app
./gradlew :composeApp:run

# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Build all targets
./gradlew build
```

### Web (SvelteKit)

```bash
cd web

# Development server
bun run dev

# Build for production
bun run build

# Type checking
bun run check

# Lint and format
bun run lint
bun run format

# Run unit tests
bun run test:unit

# Run all tests (unit + e2e)
bun run test
```

### ESP32 Firmware

```bash
cd firmware/presence-esp32

# Build (requires ESP-IDF)
idf.py build

# Flash to device
idf.py -p /dev/ttyUSB0 flash

# Monitor serial output
idf.py -p /dev/ttyUSB0 monitor
```

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

- **Kotlin shared module (`:composeApp`)**: Domain entities, event schemas, sync semantics, invariants, scheduling calculations. Pure platform-agnostic logic.
- **Android app**: Mobile client with local DB, background sync, notifications (future WearOS support).
- **Pi Kiosk (JVM)**: Presence-triggered checklist UI, serial ingestion, local SQLite, sync loop.
- **Web (SvelteKit)**: Task CRUD, planning interface, analytics. Online-first, no offline logic.
- **ESP32 firmware**: Debounced presence detection, JSONL protocol over USB CDC.

### Key Architectural Invariants

1. **Kotlin-first contracts**: All domain entities and invariants originate in the Kotlin shared module.
2. **Dependency direction**: Apps → Platform → Shared (never inward to shared).
3. **Sync is idempotent**: Event-driven, eventual consistency with deterministic merge.
4. **Hardware isolation**: Firmware communicates via JSONL protocol; no platform dependencies.
5. **Supabase is canonical**: Local storage for offline, Supabase as source of truth.
6. **No realtime dependency**: Poll-based sync is sufficient.

## JSONL Serial Protocol

ESP32 emits line-delimited JSON over USB CDC (115200 baud, 8N1):

```json
{"type": "presence", "state": "PRESENT", "timestamp_ms": 1700000000000}
{"type": "heartbeat", "timestamp_ms": 1700000005000}
```

- Presence events only on state transition (debounced: 1000ms present, 2000ms absent)
- Heartbeat every 5 seconds for liveness detection
- Device at `/dev/ttyACM0` (typical)

## Design Philosophy

- **Recovery over streaks**: Expect cycling between engagement and disengagement; design for the return.
- **Context over time**: Event-based triggers ("after brushing teeth") over time-based ("at 7:00 AM").
- **Shame-free**: No streak counters, no missed day counts, no judgment.
- **Immediate feedback**: Every interaction provides instant positive feedback.
- **No gamification**: No points, badges, or leaderboards.

## Memory & Context

- **Project memory**: `.factory/memories.md` - Architecture decisions, domain knowledge, and project history
- **Personal preferences**: `~/.factory/memories.md` - Coding style, tool choices, and workflow patterns

Refer to these files before making significant architectural or design decisions.

## Coding Standards

Follow the conventions documented in `.factory/rules/`:

- **Kotlin**: `.factory/rules/kotlin.md` - Kotlin Multiplatform patterns, domain entities, event sourcing
- **Svelte 5**: `.factory/rules/svelte.md` - SvelteKit and Svelte 5 runes, component structure
- **Embedded C**: `.factory/rules/embedded-c.md` - ESP-IDF patterns, JSONL protocol, FreeRTOS
- **Domain**: `.factory/rules/domain.md` - Cross-platform invariants, ADHD-optimized design principles

When working on a file, check the relevant rules first.
