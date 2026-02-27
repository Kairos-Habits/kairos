# Presence Sensor Configuration & Calibration Design

Date: 2026-02-27
Status: Draft

## Overview

Add bidirectional configuration/calibration system for the LD2410C mmWave presence sensor, allowing tuning of detection parameters from the kiosk app without reflashing firmware.

## Problem

- Ceiling fans, HVAC vents, and environmental factors cause false presence triggers
- Current hardcoded thresholds require firmware reflash to adjust
- Need way to tune sensitivity for different installation environments

## Design Decisions

- **Full configuration protocol** - expose all parameters, not just presets
- **Separate Settings page** - accessible via button/menu from main screen
- **Dual persistence** - ESP32 NVS survives power cycles, kiosk config for reference/override
- **Landscape layout** - side-by-side (sensor status left, configuration right)
- **Distance labels** - show distance ranges (0-0.75m) instead of gate numbers

---

## 1. Protocol Extensions (JSONL)

### Message Types

**ESP32 → Kiosk:**

```json
// Current settings (sent on connect or on request)
{"type":"settings","motion_threshold":50,"frames_present":5,"frames_absent":15,
 "sensor":{"max_gate":6,"moving_sensitivity":[50,50,50,50,40,40,0,0,0],"static_sensitivity":[0,0,0,0,0,0,0,0,0]}}

// Config command result
{"type":"config_result","success":true,"param":"motion_threshold","value":60}

// Calibration progress
{"type":"calibration_status","phase":"sampling","progress":50,"samples_collected":50}
{"type":"calibration_status","phase":"complete","recommended_threshold":65}

// LD2410C sensor config response
{"type":"sensor_config","max_gate":6,"moving_sensitivity":[50,50,50,50,40,40,0,0,0],"static_sensitivity":[0,0,0,0,0,0,0,0,0]}
```

**Kiosk → ESP32:**

```json
// Request current settings
{"type":"get_settings"}

// Request LD2410C sensor config
{"type":"get_sensor_config"}

// Set software filter parameter
{"type":"set_config","param":"motion_threshold","value":60}
{"type":"set_config","param":"frames_present","value":8}
{"type":"set_config","param":"frames_absent","value":20}

// Set LD2410C hardware parameter
{"type":"set_config","param":"max_gate","value":4}
{"type":"set_config","param":"moving_sensitivity","gate":3,"value":80}
{"type":"set_config","param":"static_sensitivity","gate":0,"value":50}

// Calibration control
{"type":"calibrate","action":"start"}
{"type":"calibrate","action":"cancel"}
{"type":"calibrate","action":"apply"}
```

---

## 2. Kiosk UI (Landscape Touchscreen)

### Layout

```
┌──────────────────────────────────────────────────────────────────────────┐
│ ← Back                                                      Settings      │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌─────────────────────┐   ┌───────────────────────────────────────┐   │
│   │   SENSOR STATUS     │   │           CONFIGURATION               │   │
│   │                     │   │                                       │   │
│   │   State: ABSENT     │   │   ▼ Software Filter                   │   │
│   │   Energy: 0         │   │     Motion: [===========|======] 50   │   │
│   │   Distance: --      │   │     Present: [========|=======] 5     │   │
│   │                     │   │     Absent:  [==============|=] 15    │   │
│   │   ┌───────────────┐ │   │                                       │   │
│   │   │  CALIBRATE    │ │   │   ▼ Sensor Hardware (LD2410C)        │   │
│   │   └───────────────┘ │   │     Max Distance: [=====|===] 4.5m   │   │
│   │                     │   │                                       │   │
│   │   Status: Ready     │   │     Distance Sensitivity:            │   │
│   │   Connection: OK    │   │       0 - 0.75m:  [=====|===] 50     │   │
│   │                     │   │       0.75 - 1.5m: [=====|===] 50    │   │
│   └─────────────────────┘   │       1.5 - 2.25m: [=====|===] 50    │   │
│                             │       2.25 - 3.0m: [=========|] 80   │   │
│                             │       3.0 - 3.75m: [=====|===] 40    │   │
│                             │       3.75 - 4.5m: [=====|===] 40    │   │
│                             │                                       │   │
│                             │   ┌─────────────────────────────────┐ │   │
│                             │   │  APPLY       │       RESET     │ │   │
│                             │   └─────────────────────────────────┘ │   │
│                             └───────────────────────────────────────┘   │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### Touch Target Sizes

- Minimum touch target: 48px (Material Design)
- Primary buttons: 88px height
- Sliders: 44px+ hit area
- Clear visual feedback on press

### UI Elements

**Left Panel - Sensor Status:**
- Current presence state (PRESENT/ABSENT)
- Real-time energy reading
- Detection distance
- CALIBRATE button (large)
- Connection status

**Right Panel - Configuration:**
- Collapsible sections for Software Filter and Sensor Hardware
- Sliders with distance labels (not gate numbers)
- APPLY and RESET buttons

---

## 3. LD2410C Gate Structure

Each gate covers a 75cm distance range:

| Gate | Distance Range | Typical Use |
|------|---------------|-------------|
| 0 | 0 - 0.75m | Very close |
| 1 | 0.75 - 1.5m | Close |
| 2 | 1.5 - 2.25m | Near |
| 3 | 2.25 - 3.0m | Medium-near |
| 4 | 3.0 - 3.75m | Medium |
| 5 | 3.75 - 4.5m | Medium-far |
| 6 | 4.5 - 5.25m | Far |
| 7 | 5.25 - 6.0m | Very far |
| 8 | 6.0 - 6.75m | Maximum |

**Configuration:**
- `max_gate`: Furthest gate to consider for presence (everything beyond is ignored)
- `moving_sensitivity[gate]`: Threshold for motion detection (0-100, lower = more sensitive)
- `static_sensitivity[gate]`: Threshold for stationary presence (0-100)

**UART Commands (via ESP32):**
- Enter config mode: `0xFF 0x00 0x01 0x00` (header + command)
- Exit config mode: `0xFE 0x00`
- Query max gate: `0x00 0x61`
- Set max gate: `0x00 0x60 + data`
- Query gate sensitivity: `0x00 0x63 + gate_number`
- Set gate sensitivity: `0x00 0x64 + gate + moving + static`

---

## 4. Persistence Strategy

### ESP32 NVS

```
namespace: "presence"
keys:
  - motion_threshold (uint8): 50
  - frames_present (uint8): 5
  - frames_absent (uint8): 15
  - max_gate (uint8): 6
  - moving_sensitivity[0-8] (uint8): [50,50,50,50,40,40,0,0,0]
  - static_sensitivity[0-8] (uint8): [0,0,0,0,0,0,0,0,0]
```

### Kiosk Config (YAML)

```yaml
sensor:
  device_path: /dev/ttyACM0
  motion_threshold: 50
  frames_present: 5
  frames_absent: 15
  max_gate: 6
  moving_sensitivity: [50, 50, 50, 50, 40, 40, 0, 0, 0]
  static_sensitivity: [0, 0, 0, 0, 0, 0, 0, 0, 0]
```

### Sync Flow

1. ESP32 boots → loads from NVS → emits `settings` message
2. Kiosk connects → receives `settings` → updates UI
3. User changes setting → kiosk sends `set_config` → ESP32 saves to NVS, writes to LD2410C if needed → confirms with `config_result`
4. Kiosk saves to local config for reference

---

## 5. Calibration Algorithm

### Auto-Calibration Sequence

1. User taps "CALIBRATE"
2. Kiosk sends `{"type":"calibrate","action":"start"}`
3. ESP32 enters calibration mode (10 seconds):
   - Samples `moving_energy` every 100ms (100 samples total)
   - Tracks max and average energy
   - Reports progress every 10 samples: `{"type":"calibration_status","phase":"sampling","progress":10,"samples_collected":10}`
4. ESP32 computes: `recommended_threshold = max_energy_observed + 10` (buffer above noise floor)
5. ESP32 emits: `{"type":"calibration_status","phase":"complete","recommended_threshold":65}`
6. UI shows recommended value as preview, user taps "APPLY" to accept

### Calibration Mode Behavior

- During calibration, sensor continues normal operation
- Calibration only observes, doesn't affect detection
- User should be still and let sensor sample ambient conditions
- Progress shown in left panel status area

---

## 6. Implementation Structure

### ESP32 Firmware Changes

**New files:**
- `sensor_config.c/h` - NVS storage, config parsing, calibration logic
- `ld2410c_config.c/h` - UART commands for LD2410C read/write

**Modified files:**
- `jsonl_output.c/h` - add config message types
- `mmwave_sensor.c` - use config values instead of #defines
- `presence_main.c` - initialize config, handle calibration state
- `CMakeLists.txt` - add new source files

### Kotlin Kiosk Changes

**New files:**
- `SensorConfig.kt` - data class for all settings
- `SensorConfigManager.kt` - bidirectional config commands
- `SettingsScreen.kt` - Compose UI for settings page
- `SettingsViewModel.kt` - state management for settings

**Modified files:**
- `SerialPresenceListener.kt` - handle bidirectional messages, add send method
- `JsonlParser.kt` - parse new message types
- `Main.kt` - add navigation to settings page

---

## 7. Success Criteria

- [ ] Can adjust motion threshold from kiosk UI
- [ ] Can adjust frame counts from kiosk UI
- [ ] Can set max detection distance from kiosk UI
- [ ] Can tune per-gate sensitivities from kiosk UI
- [ ] Auto-calibration suggests appropriate threshold
- [ ] Settings persist across ESP32 power cycles
- [ ] Real-time sensor feedback visible while tuning
- [ ] Touch targets are appropriately sized for finger input
