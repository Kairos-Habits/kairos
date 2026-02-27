## Feature 03: Presence Detection (Hardware Integration)

### Summary
Integrate ESP32 mmWave sensor (HLK-LD2410C) with kiosk via JSONL over USB CDC for debounced presence detection.

### Hardware
- **ESP32**: Waveshare ESP32-S3-Zero
- **mmWave Sensor**: HLK-LD2410C
- **Communication**: UART (TX/RX) at 256000 baud for full sensor data

### Current State
- **ESP32 firmware**: Presence detection implemented with UART communication
- **Kotlin shared**: `PresenceState` and `DisplayModeStateMachine` already implemented and tested
- **Kiosk**: Serial listener connected to DisplayModeStateMachine

---

### Implementation Plan

#### 1. ESP32 Firmware (`firmware/presence-esp32/main/`)

**Modular C files**:
- `presence_main.c` - Entry point, FreeRTOS task orchestration
- `mmwave_sensor.c/h` - LD2410C UART driver (256000 baud, TX: GPIO4, RX: GPIO5)
- `presence_debounce.c/h` - Debounce logic (1000ms present, 2000ms absent)
- `jsonl_output.c/h` - JSONL protocol formatting

**JSONL Protocol**:
```json
{"type":"presence","state":"PRESENT","timestamp_ms":1700000000000,"moving_cm":150,"static_cm":0}
{"type":"heartbeat","timestamp_ms":1700000005000}
```

**Key Implementation Details**:
- UART1 for mmWave (TX: GPIO4, RX: GPIO5)
- USB CDC for host communication
- FreeRTOS tasks: sensor polling (100ms), heartbeat (5s)
- Use `esp_timer_get_time()` for timestamps
- ESP-IDF logging with TAG pattern

#### 2. Kiosk Serial Listener (`kotlin/kiosk/src/jvmMain/`)

**Dependencies**:
- `jSerialComm` (lightweight, cross-platform)
- `kotlinx-serialization-json` (JSONL parsing)

**Files**:
- `serial/SerialConfig.kt` - Device path, baud rate config
- `serial/JsonlParser.kt` - Parse JSONL to `PresenceEvent` sealed class
- `serial/PresenceEvent.kt` - Mirror of firmware events (with moving_cm, static_cm)
- `serial/SerialPresenceListener.kt` - Coroutine-based serial reader

**Flow**:
```
SerialPresenceListener reads JSONL → JsonlParser → Flow<PresenceEvent>
                                                  → DisplayModeStateMachine
```

**Error handling**:
- Device not found: log warning, retry every 5s
- Disconnect during operation: log, attempt reconnect
- Parse errors: log and skip malformed lines

#### 3. Integration with State Machine

**Wire into `Main.kt`**:
- Collect `PresenceEvent` flow from serial listener
- Map to `DisplayModeEvent.PresenceChanged`
- Reduce events through `DisplayModeStateMachine`
- Expose `StateFlow<DisplayModeState>` to Compose UI

---

### Files Changed/Created

**Firmware**:
- `firmware/presence-esp32/main/presence_main.c`
- `firmware/presence-esp32/main/mmwave_sensor.c/h` (UART driver for LD2410C)
- `firmware/presence-esp32/main/presence_debounce.c/h`
- `firmware/presence-esp32/main/jsonl_output.c/h`
- `firmware/presence-esp32/main/CMakeLists.txt`

**Kotlin kiosk**:
- `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/SerialConfig.kt`
- `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/PresenceEvent.kt`
- `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/JsonlParser.kt`
- `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/SerialPresenceListener.kt`
- `kotlin/kiosk/build.gradle.kts`
- `kotlin/gradle/libs.versions.toml`
- `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/Main.kt`
- `kotlin/shared/src/commonMain/kotlin/com/rghsoftware/kairos/App.kt`

---

### Acceptance Criteria
- [x] Stable presence detection (no flapping)
- [x] JSONL protocol: `presence` and `heartbeat` messages
- [x] Debounce: 1000ms present, 2000ms absent in firmware
- [x] Serial disconnect handled gracefully with reconnection
- [x] Events flow to `DisplayModeStateMachine`
- [ ] Firmware builds with `idf.py build` (requires ESP-IDF)
- [x] Kiosk builds with `./gradlew :kiosk:assemble`