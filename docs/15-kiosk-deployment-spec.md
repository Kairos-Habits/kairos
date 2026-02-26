# Kairos – Kiosk Deployment Specification

## 1. Overview

This document defines the deployment strategy for the Raspberry Pi 4
front-door kiosk running the Compose Desktop application.

The kiosk is a dedicated appliance-style node responsible for:

- Presence-triggered checklist display
- Local SQLite persistence
- Sync with Supabase
- Deterministic state machine execution

It must operate reliably even without internet connectivity.

---

## 2. Hardware Requirements

- Raspberry Pi 4 (4GB or 8GB recommended)
- 15" HDMI touchscreen
- USB touch interface
- ESP32 presence node (USB CDC)
- 5V 3A power supply (stable)

Optional:

- Wall mount enclosure
- UPS HAT for graceful shutdown

---

## 3. Operating System

Recommended:

- Raspberry Pi OS (64-bit)
- Minimal desktop install

Rationale:

- Stable GL stack for Compose Desktop
- Simplified GPU acceleration
- Systemd support

---

## 4. Runtime Architecture

Boot sequence:

1. Raspberry Pi boots.
2. Systemd starts kiosk service.
3. Kiosk initializes:
       - Serial listener
       - Local DB
       - Sync scheduler
       - Presence state machine
4. Fullscreen Compose UI launches.

System topology on Pi:

```text
Compose Desktop App
    ├── Serial Listener
    ├── Presence State Machine
    ├── Local SQLite
    ├── Sync Worker
    └── UI Layer
```

No browser required.

---

## 5. Systemd Service

Kiosk runs as a systemd service:

- Restart on crash
- Restart on failure
- Delayed start until network available (optional)

Recommended configuration:

- Restart=always
- RestartSec=5
- WorkingDirectory=/opt/kairos

---

## 6. Serial Configuration

ESP32 connects via USB CDC.

Expected device:

- `/dev/ttyACM0` (or similar)

Configuration:

- Baud rate: 115200
- Line-based JSON
- Non-blocking read loop

Failure handling:

- Detect disconnection
- Retry periodically
- Log error state

---

## 7. Display Configuration

Kiosk should:

- Run in fullscreen
- Hide cursor
- Disable screen sleep
- Disable DPMS power saving

Optional:

- Autohide taskbar
- Lock keyboard shortcuts

---

## 8. Local Storage

Database:

- SQLite
  - File location: `/var/lib/kairos/kiosk.db`

Tables include:

- `tasks`
- `checklist_sessions`
- `checklist_completions`
- `presence_events`
- `sync_state`

Backups optional but recommended.

---

## 9. Sync Worker

Sync loop:

- Runs every N seconds (configurable)
- Pushes unsynced events
- Pulls deltas since last_sync_at
- Applies deterministic merge

Failure handling:

- Exponential backoff
- No UI blocking
- Local operations unaffected

---

## 10. Logging

Recommended logging targets:

- systemd journal
- Rotating log file

Log categories:

- Serial
- State machine transitions
- Sync operations
- Errors

---

## 11. Update Strategy

Because this is a personal project:

Option A:

- SCP new build
- Restart service

Option B:

- Self-update script

No OTA complexity required.

---

## 12. Security Considerations

- Device located inside home
- No public ports exposed
- Supabase keys stored securely
- File permissions restricted

No direct inbound connections required.

---

## 13. Upgrade Path

If moving to Jetson Nano:

- Same Compose Desktop app (JVM)
- Same KMP shared core
- Replace only hardware layer
- Optional camera integration

No architectural changes required.
