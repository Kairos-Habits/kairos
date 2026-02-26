# Kairos – Serial JSONL Protocol Specification

## 1. Overview

This document defines the serial protocol between:

- ESP32 presence node
- Raspberry Pi kiosk

The protocol is:

- Line-delimited JSON (JSONL)
- UTF-8 encoded
- Newline terminated
- Stateless per message
- Append-only event stream

No additional framing or CRC is required.
USB CDC provides transport integrity.

---

## 2. Transport Layer

Physical Layer:

- USB CDC (virtual serial)

Serial Configuration:

- Baud rate: 115200
- 8N1
- Non-blocking read

Device expected at:

- /dev/ttyACM0 (typical)

---

## 3. Message Format

Each message is a single JSON object terminated by a newline.

Example structure:

{
  "type": "presence",
  "state": "PRESENT",
  "timestamp_ms": 1700000000000
}

Fields:

type:
    "presence" | "heartbeat"

state:
    "PRESENT" | "ABSENT"
    (Only for type = "presence")

timestamp_ms:
    Milliseconds since epoch (device time)

---

## 4. Presence Event Semantics

The ESP32 must emit events only on state transition.

Example sequence:

PRESENT emitted once when presence stable >= threshold.
ABSENT emitted once when absence stable >= threshold.

Repeated PRESENT messages during steady state are not allowed.

---

## 5. Heartbeat

Heartbeat messages:

{
  "type": "heartbeat",
  "timestamp_ms": 1700000005000
}

Heartbeat interval:

- Every 5 seconds (recommended)

Purpose:

- Detect disconnection
- Confirm liveness
- Assist diagnostics

Heartbeat does not affect state machine.

---

## 6. Debounce Requirements (Firmware)

Presence must:

- Remain stable >= 1000 ms before emitting PRESENT
- Remain absent >= 2000 ms before emitting ABSENT

Debounce must occur in firmware.

The Pi must not implement redundant debounce logic.

---

## 7. Error Handling (Kiosk Side)

If JSON parsing fails:

- Log error
- Discard malformed line
- Continue listening

If serial disconnect occurs:

- Retry periodically
- Enter degraded mode
- Do not crash kiosk UI

---

## 8. Time Source

ESP32 timestamp_ms may drift.

Kiosk may:

- Record local_received_timestamp
- Use kiosk clock as authoritative for sync events

Firmware timestamps are primarily diagnostic.

---

## 9. Extensibility

Future message types may include:

- "zone_presence"
- "signal_strength"
- "diagnostic"

New fields must be additive.
Old clients must ignore unknown fields.

---

## 10. Design Constraints

- Human-readable
- Easy to debug via serial monitor
- Minimal complexity
- Deterministic parsing
- No binary framing required
