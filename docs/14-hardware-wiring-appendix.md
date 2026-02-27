# Hardware Wiring Appendix – ESP32 + mmWave + Pi (USB Serial)

## Goal

Use an ESP32 to interface with a 24 GHz mmWave presence module (UART) and stream **debounced presence events** to a Raspberry Pi over **USB CDC serial**.

## Recommended Parts

- ESP32 dev board with USB (ESP32-S3 preferred, but ESP32 works)
- mmWave presence module: **HLK-LD2410 / LD2410C** (UART)
- Jumper wires, optional small enclosure
- Optional add-ons:
  - Door reed switch (GPIO)
  - PIR sensor (GPIO)
  - BLE beacon (later phase)

## Wiring

### mmWave (UART) → ESP32

Most LD2410-class modules expose:

- `VCC` (often 5V capable; check your module)
- `GND`
- `TX` (module → ESP32 RX)
- `RX` (ESP32 TX → module RX)

**Common wiring:**

- mmWave `TX` → ESP32 `RX` (one of the hardware UART RX pins)
- mmWave `RX` → ESP32 `TX`
- mmWave `GND` → ESP32 `GND`
- mmWave `VCC` → ESP32 `5V` or `3V3` **as required by your module**

> **Important:** Many modules are 5V powered but use 3.3V UART logic. Verify your specific board/module before wiring.

### ESP32 → Pi

- Connect ESP32 to Pi using a USB cable.
- The ESP32 enumerates as a serial device on the Pi (typically `/dev/kairos-presence` or `/dev/ttyUSB0`).

## Serial Protocol (JSONL)

Line-delimited JSON over USB CDC (256000 baud for LD2410C UART, 115200 for USB CDC):

```json
{"type":"presence","state":"PRESENT","timestamp_ms":1700000000000,"moving_cm":150,"static_cm":0}
{"type":"heartbeat","timestamp_ms":1700000005000}
```

**Fields**:
- `type`: "presence" or "heartbeat"
- `state`: "PRESENT" or "ABSENT" (presence events only)
- `timestamp_ms`: milliseconds since boot
- `moving_cm`: distance to moving target in cm
- `static_cm`: distance to static target in cm

## Debounce Defaults

- `PRESENT`: stable presence ≥ 1000 ms
- `ABSENT`: stable absence ≥ 2000 ms
- `HEARTBEAT`: every 5–10 s

## Mounting Guidance (Pets)

- Mount sensor **~1.3–1.6 m high**, angled **5–15° downward**
- Tune zones to reduce near-floor sensitivity
- Use kiosk cooldown (e.g., 90 s) after checklist completion/dismissal

## Test Commands (Pi)

```bash
# Find the serial device
ls -l /dev/ttyACM* /dev/ttyUSB* 2>/dev/null

# Watch JSONL messages (115200 baud for USB CDC)
sudo stty -F /dev/ttyACM0 115200 raw -echo
cat /dev/ttyACM0
```

## Pet Mitigation (Dog-Friendly Setup)

mmWave presence sensors can trigger on pets depending on size, motion, and mounting. For a mid-sized dog, the MVP mitigation is **mechanical + configuration**:

- Mount sensor **~1.3–1.6 m** high, aimed slightly downward (≈5–15°)
- Configure detection zones to focus on the **door/torso region** and avoid very-near floor zones
- Apply ESP32-side debounce (e.g., **present ≥1.0 s**, **absent ≥2.0–3.0 s**)
- Add a kiosk-side **cooldown** (e.g., 90 s) after a checklist completes to prevent retriggers

## Note on Future GPIO Optimization

USB CDC serial is the recommended **MVP** channel because it provides rich debugging and calibration data when tuning sensitivity and placement. If/when tuning stabilizes, you can optionally add a simple GPIO presence line later, but it is not required for correctness.
