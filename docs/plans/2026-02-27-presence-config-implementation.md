# Presence Sensor Configuration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add bidirectional configuration/calibration system for LD2410C mmWave sensor with touchscreen Settings UI.

**Architecture:** ESP32 stores config in NVS, communicates via JSONL protocol. Kiosk app provides landscape Settings page with real-time sensor feedback. Two-layer config: software filter (frame counts) and sensor hardware (gate sensitivities).

**Tech Stack:** ESP-IDF (C), Kotlin Multiplatform (Compose Desktop), jSerialComm

---

## Phase 1: ESP32 Firmware - Config Infrastructure

### Task 1: Create sensor_config module with NVS storage

**Files:**
- Create: `firmware/presence-esp32/main/sensor_config.h`
- Create: `firmware/presence-esp32/main/sensor_config.c`
- Modify: `firmware/presence-esp32/main/CMakeLists.txt`

**Step 1: Create sensor_config.h**

```c
#pragma once

#include <stdint.h>
#include <stdbool.h>

#define MAX_GATES 9

typedef struct {
    // Software filter settings
    uint8_t motion_threshold;
    uint8_t frames_present;
    uint8_t frames_absent;
    
    // LD2410C hardware settings
    uint8_t max_gate;
    uint8_t moving_sensitivity[MAX_GATES];
    uint8_t static_sensitivity[MAX_GATES];
} sensor_config_t;

// Default values
#define DEFAULT_MOTION_THRESHOLD 50
#define DEFAULT_FRAMES_PRESENT 5
#define DEFAULT_FRAMES_ABSENT 15
#define DEFAULT_MAX_GATE 6

void sensor_config_init(void);
sensor_config_t* sensor_config_get(void);
esp_err_t sensor_config_load(void);
esp_err_t sensor_config_save(void);
void sensor_config_set_defaults(void);
```

**Step 2: Create sensor_config.c**

```c
#include "sensor_config.h"
#include "nvs_flash.h"
#include "nvs.h"
#include "esp_log.h"

static const char *TAG = "sensor_config";
static sensor_config_t config;
static nvs_handle_t nvs_handle;

void sensor_config_init(void) {
    esp_err_t err = nvs_open("presence", NVS_READWRITE, &nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to open NVS: %s", esp_err_to_name(err));
        sensor_config_set_defaults();
        return;
    }
    
    if (sensor_config_load() != ESP_OK) {
        ESP_LOGI(TAG, "No saved config, using defaults");
        sensor_config_set_defaults();
        sensor_config_save();
    }
}

void sensor_config_set_defaults(void) {
    config.motion_threshold = DEFAULT_MOTION_THRESHOLD;
    config.frames_present = DEFAULT_FRAMES_PRESENT;
    config.frames_absent = DEFAULT_FRAMES_ABSENT;
    config.max_gate = DEFAULT_MAX_GATE;
    
    for (int i = 0; i < MAX_GATES; i++) {
        config.moving_sensitivity[i] = (i < 6) ? 50 : 0;
        config.static_sensitivity[i] = 0;
    }
}

esp_err_t sensor_config_load(void) {
    esp_err_t err = nvs_get_u8(nvs_handle, "motion_thresh", &config.motion_threshold);
    if (err != ESP_OK) return err;
    
    nvs_get_u8(nvs_handle, "frames_present", &config.frames_present);
    nvs_get_u8(nvs_handle, "frames_absent", &config.frames_absent);
    nvs_get_u8(nvs_handle, "max_gate", &config.max_gate);
    
    size_t len = MAX_GATES;
    nvs_get_blob(nvs_handle, "moving_sens", config.moving_sensitivity, &len);
    nvs_get_blob(nvs_handle, "static_sens", config.static_sensitivity, &len);
    
    ESP_LOGI(TAG, "Config loaded from NVS");
    return ESP_OK;
}

esp_err_t sensor_config_save(void) {
    esp_err_t err = ESP_OK;
    err |= nvs_set_u8(nvs_handle, "motion_thresh", config.motion_threshold);
    err |= nvs_set_u8(nvs_handle, "frames_present", config.frames_present);
    err |= nvs_set_u8(nvs_handle, "frames_absent", config.frames_absent);
    err |= nvs_set_u8(nvs_handle, "max_gate", config.max_gate);
    err |= nvs_set_blob(nvs_handle, "moving_sens", config.moving_sensitivity, MAX_GATES);
    err |= nvs_set_blob(nvs_handle, "static_sens", config.static_sensitivity, MAX_GATES);
    err |= nvs_commit(nvs_handle);
    
    if (err == ESP_OK) {
        ESP_LOGI(TAG, "Config saved to NVS");
    }
    return err;
}

sensor_config_t* sensor_config_get(void) {
    return &config;
}
```

**Step 3: Update CMakeLists.txt**

Add to `firmware/presence-esp32/main/CMakeLists.txt`:
```cmake
sensor_config.c
```

**Step 4: Build and verify compilation**

Run: `cd firmware/presence-esp32 && idf.py build`
Expected: Build succeeds

**Step 5: Commit**

```bash
git add firmware/presence-esp32/main/sensor_config.c firmware/presence-esp32/main/sensor_config.h firmware/presence-esp32/main/CMakeLists.txt
git commit -m "feat(esp32): add sensor_config module with NVS storage"
```

---

### Task 2: Add JSONL config message handling

**Files:**
- Modify: `firmware/presence-esp32/main/jsonl_output.h`
- Modify: `firmware/presence-esp32/main/jsonl_output.c`

**Step 1: Add function declarations to jsonl_output.h**

```c
// Add after existing declarations:

void jsonl_emit_settings(sensor_config_t *config);
void jsonl_emit_config_result(const char *param, bool success, int value);
void jsonl_emit_calibration_status(const char *phase, int progress, int samples);
void jsonl_emit_sensor_config(uint8_t max_gate, uint8_t *moving, uint8_t *static_sens);
```

**Step 2: Implement in jsonl_output.c**

```c
// Add to top of file:
#include "sensor_config.h"

// Add new functions:

void jsonl_emit_settings(sensor_config_t *config) {
    printf("{\"type\":\"settings\","
           "\"motion_threshold\":%u,"
           "\"frames_present\":%u,"
           "\"frames_absent\":%u,"
           "\"sensor\":{\"max_gate\":%u,"
           "\"moving_sensitivity\":[%u,%u,%u,%u,%u,%u,%u,%u,%u],"
           "\"static_sensitivity\":[%u,%u,%u,%u,%u,%u,%u,%u,%u]}}\n",
           config->motion_threshold, config->frames_present, config->frames_absent,
           config->max_gate,
           config->moving_sensitivity[0], config->moving_sensitivity[1],
           config->moving_sensitivity[2], config->moving_sensitivity[3],
           config->moving_sensitivity[4], config->moving_sensitivity[5],
           config->moving_sensitivity[6], config->moving_sensitivity[7],
           config->moving_sensitivity[8],
           config->static_sensitivity[0], config->static_sensitivity[1],
           config->static_sensitivity[2], config->static_sensitivity[3],
           config->static_sensitivity[4], config->static_sensitivity[5],
           config->static_sensitivity[6], config->static_sensitivity[7],
           config->static_sensitivity[8]);
    fflush(stdout);
}

void jsonl_emit_config_result(const char *param, bool success, int value) {
    printf("{\"type\":\"config_result\",\"success\":%s,\"param\":\"%s\",\"value\":%d}\n",
           success ? "true" : "false", param, value);
    fflush(stdout);
}

void jsonl_emit_calibration_status(const char *phase, int progress, int samples) {
    printf("{\"type\":\"calibration_status\",\"phase\":\"%s\",\"progress\":%d,\"samples_collected\":%d}\n",
           phase, progress, samples);
    fflush(stdout);
}

void jsonl_emit_sensor_config(uint8_t max_gate, uint8_t *moving, uint8_t *static_sens) {
    printf("{\"type\":\"sensor_config\",\"max_gate\":%u,"
           "\"moving_sensitivity\":[%u,%u,%u,%u,%u,%u,%u,%u,%u],"
           "\"static_sensitivity\":[%u,%u,%u,%u,%u,%u,%u,%u,%u]}\n",
           max_gate,
           moving[0], moving[1], moving[2], moving[3], moving[4],
           moving[5], moving[6], moving[7], moving[8],
           static_sens[0], static_sens[1], static_sens[2], static_sens[3],
           static_sens[4], static_sens[5], static_sens[6], static_sens[7], static_sens[8]);
    fflush(stdout);
}
```

**Step 3: Build**

Run: `cd firmware/presence-esp32 && idf.py build`
Expected: Build succeeds

**Step 4: Commit**

```bash
git add firmware/presence-esp32/main/jsonl_output.c firmware/presence-esp32/main/jsonl_output.h
git commit -m "feat(esp32): add JSONL config message emission functions"
```

---

### Task 3: Add JSONL command parsing

**Files:**
- Create: `firmware/presence-esp32/main/jsonl_input.h`
- Create: `firmware/presence-esp32/main/jsonl_input.c`
- Modify: `firmware/presence-esp32/main/CMakeLists.txt`

**Step 1: Create jsonl_input.h**

```c
#pragma once

#include <stdbool.h>

typedef enum {
    CMD_NONE,
    CMD_GET_SETTINGS,
    CMD_GET_SENSOR_CONFIG,
    CMD_SET_CONFIG,
    CMD_CALIBRATE_START,
    CMD_CALIBRATE_CANCEL,
    CMD_CALIBRATE_APPLY,
} jsonl_command_t;

typedef struct {
    jsonl_command_t command;
    char param[32];
    int value;
    int gate;  // For gate-specific commands
} jsonl_input_t;

void jsonl_input_init(void);
bool jsonl_input_parse(const char *line, jsonl_input_t *out);
void jsonl_input_process(const char *line);
```

**Step 2: Create jsonl_input.c**

```c
#include "jsonl_input.h"
#include "sensor_config.h"
#include "jsonl_output.h"
#include "esp_log.h"
#include <string.h>
#include <stdlib.h>

static const char *TAG = "jsonl_input";

void jsonl_input_init(void) {
    // No init needed yet
}

static bool parse_string(const char *json, const char *key, char *out, size_t max_len) {
    char search[64];
    snprintf(search, sizeof(search), "\"%s\":\"", key);
    const char *start = strstr(json, search);
    if (!start) return false;
    
    start += strlen(search);
    const char *end = strchr(start, '"');
    if (!end) return false;
    
    size_t len = end - start;
    if (len >= max_len) len = max_len - 1;
    strncpy(out, start, len);
    out[len] = '\0';
    return true;
}

static bool parse_int(const char *json, const char *key, int *out) {
    char search[64];
    snprintf(search, sizeof(search), "\"%s\":", key);
    const char *start = strstr(json, search);
    if (!start) return false;
    
    start += strlen(search);
    *out = atoi(start);
    return true;
}

bool jsonl_input_parse(const char *line, jsonl_input_t *out) {
    memset(out, 0, sizeof(jsonl_input_t));
    
    char type[32];
    if (!parse_string(line, "type", type, sizeof(type))) {
        return false;
    }
    
    if (strcmp(type, "get_settings") == 0) {
        out->command = CMD_GET_SETTINGS;
    } else if (strcmp(type, "get_sensor_config") == 0) {
        out->command = CMD_GET_SENSOR_CONFIG;
    } else if (strcmp(type, "set_config") == 0) {
        out->command = CMD_SET_CONFIG;
        parse_string(line, "param", out->param, sizeof(out->param));
        parse_int(line, "value", &out->value);
        parse_int(line, "gate", &out->gate);
    } else if (strcmp(type, "calibrate") == 0) {
        char action[32];
        if (parse_string(line, "action", action, sizeof(action))) {
            if (strcmp(action, "start") == 0) out->command = CMD_CALIBRATE_START;
            else if (strcmp(action, "cancel") == 0) out->command = CMD_CALIBRATE_CANCEL;
            else if (strcmp(action, "apply") == 0) out->command = CMD_CALIBRATE_APPLY;
        }
    } else {
        return false;
    }
    
    return true;
}

void jsonl_input_process(const char *line) {
    jsonl_input_t cmd;
    if (!jsonl_input_parse(line, &cmd)) {
        return;
    }
    
    sensor_config_t *config = sensor_config_get();
    
    switch (cmd.command) {
        case CMD_GET_SETTINGS:
            jsonl_emit_settings(config);
            break;
            
        case CMD_GET_SENSOR_CONFIG:
            jsonl_emit_sensor_config(config->max_gate, 
                                     config->moving_sensitivity,
                                     config->static_sensitivity);
            break;
            
        case CMD_SET_CONFIG:
            if (strcmp(cmd.param, "motion_threshold") == 0) {
                config->motion_threshold = (uint8_t)cmd.value;
                sensor_config_save();
                jsonl_emit_config_result(cmd.param, true, cmd.value);
            } else if (strcmp(cmd.param, "frames_present") == 0) {
                config->frames_present = (uint8_t)cmd.value;
                sensor_config_save();
                jsonl_emit_config_result(cmd.param, true, cmd.value);
            } else if (strcmp(cmd.param, "frames_absent") == 0) {
                config->frames_absent = (uint8_t)cmd.value;
                sensor_config_save();
                jsonl_emit_config_result(cmd.param, true, cmd.value);
            } else if (strcmp(cmd.param, "max_gate") == 0) {
                config->max_gate = (uint8_t)cmd.value;
                sensor_config_save();
                jsonl_emit_config_result(cmd.param, true, cmd.value);
            } else if (strcmp(cmd.param, "moving_sensitivity") == 0) {
                if (cmd.gate >= 0 && cmd.gate < MAX_GATES) {
                    config->moving_sensitivity[cmd.gate] = (uint8_t)cmd.value;
                    sensor_config_save();
                    jsonl_emit_config_result(cmd.param, true, cmd.value);
                }
            } else if (strcmp(cmd.param, "static_sensitivity") == 0) {
                if (cmd.gate >= 0 && cmd.gate < MAX_GATES) {
                    config->static_sensitivity[cmd.gate] = (uint8_t)cmd.value;
                    sensor_config_save();
                    jsonl_emit_config_result(cmd.param, true, cmd.value);
                }
            } else {
                jsonl_emit_config_result(cmd.param, false, 0);
            }
            break;
            
        case CMD_CALIBRATE_START:
            // Handled in main loop
            break;
            
        default:
            break;
    }
}
```

**Step 3: Update CMakeLists.txt**

Add `jsonl_input.c` to source files.

**Step 4: Build**

Run: `cd firmware/presence-esp32 && idf.py build`
Expected: Build succeeds

**Step 5: Commit**

```bash
git add firmware/presence-esp32/main/jsonl_input.c firmware/presence-esp32/main/jsonl_input.h firmware/presence-esp32/main/CMakeLists.txt
git commit -m "feat(esp32): add JSONL command parsing for config protocol"
```

---

### Task 4: Wire config into presence_main.c

**Files:**
- Modify: `firmware/presence-esp32/main/presence_main.c`

**Step 1: Add includes and use config values**

```c
// Add to includes:
#include "sensor_config.h"
#include "jsonl_input.h"

// In app_main(), after mmwave_sensor_init():
sensor_config_init();
jsonl_input_init();

// Emit settings on startup
jsonl_emit_settings(sensor_config_get());
```

**Step 2: Replace #define constants with config values**

In `presence_update()` function, replace:
```c
// OLD:
if (energy >= MOTION_THRESHOLD && energy > 0) {
// NEW:
sensor_config_t *cfg = sensor_config_get();
if (energy >= cfg->motion_threshold && energy > 0) {

// OLD:
if (consecutive_motion_frames >= FRAMES_TO_PRESENT) {
// NEW:
if (consecutive_motion_frames >= cfg->frames_present) {

// OLD:
if (consecutive_no_motion_frames >= FRAMES_TO_ABSENT) {
// NEW:
if (consecutive_no_motion_frames >= cfg->frames_absent) {
```

**Step 3: Add stdin reading for config commands**

```c
// Add a FreeRTOS task to read stdin and process commands:

static void config_input_task(void *arg) {
    char line[256];
    int idx = 0;
    
    while (1) {
        int c = getchar();
        if (c == EOF || c == '\n' || c == '\r') {
            if (idx > 0) {
                line[idx] = '\0';
                jsonl_input_process(line);
                idx = 0;
            }
        } else if (idx < sizeof(line) - 1) {
            line[idx++] = (char)c;
        }
        vTaskDelay(1);
    }
}

// In app_main(), create the task:
xTaskCreate(config_input_task, "config_input", 4096, NULL, 5, NULL);
```

**Step 4: Build and test**

Run: `cd firmware/presence-esp32 && idf.py -p /dev/ttyACM0 flash monitor`
Expected: Settings emitted on startup, can send commands via stdin

**Step 5: Commit**

```bash
git add firmware/presence-esp32/main/presence_main.c
git commit -m "feat(esp32): wire config into main loop, add stdin command processing"
```

---

### Task 5: Implement calibration mode

**Files:**
- Modify: `firmware/presence-esp32/main/presence_main.c`

**Step 1: Add calibration state variables**

```c
static bool calibrating = false;
static int calibration_samples = 0;
static int calibration_max = 0;
static int calibration_sum = 0;
#define CALIBRATION_TARGET_SAMPLES 100
```

**Step 2: Add calibration logic to presence_update()**

```c
void presence_update(uint16_t moving_energy, uint16_t static_energy, 
                     uint16_t moving_cm, uint16_t static_cm) {
    // Calibration mode
    if (calibrating) {
        calibration_samples++;
        if (moving_energy > calibration_max) {
            calibration_max = moving_energy;
        }
        calibration_sum += moving_energy;
        
        if (calibration_samples % 10 == 0) {
            int progress = (calibration_samples * 100) / CALIBRATION_TARGET_SAMPLES;
            jsonl_emit_calibration_status("sampling", progress, calibration_samples);
        }
        
        if (calibration_samples >= CALIBRATION_TARGET_SAMPLES) {
            calibrating = false;
            int recommended = calibration_max + 10;
            if (recommended > 100) recommended = 100;
            jsonl_emit_calibration_status("complete", 100, recommended);
        }
        return;  // Skip normal detection during calibration
    }
    
    // ... rest of existing function
}
```

**Step 3: Handle calibrate commands in jsonl_input_process()**

Add case for `CMD_CALIBRATE_START` and `CMD_CALIBRATE_APPLY` to set calibration variables.

**Step 4: Build and test**

Run: `cd firmware/presence-esp32 && idf.py -p /dev/ttyACM0 flash monitor`
Test: Send `{"type":"calibrate","action":"start"}` and observe calibration progress

**Step 5: Commit**

```bash
git add firmware/presence-esp32/main/presence_main.c
git commit -m "feat(esp32): implement 10-second auto-calibration mode"
```

---

## Phase 2: Kotlin Kiosk - Config Protocol

### Task 6: Add SensorConfig data class

**Files:**
- Create: `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/SensorConfig.kt`

**Step 1: Create SensorConfig.kt**

```kotlin
package com.rghsoftware.kairos.serial

data class SensorConfig(
    val motionThreshold: Int = 50,
    val framesPresent: Int = 5,
    val framesAbsent: Int = 15,
    val maxGate: Int = 6,
    val movingSensitivity: List<Int> = listOf(50, 50, 50, 50, 40, 40, 0, 0, 0),
    val staticSensitivity: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
) {
    companion object {
        val DEFAULT = SensorConfig()
    }
}

data class CalibrationStatus(
    val phase: String,
    val progress: Int,
    val samplesCollected: Int,
)

data class ConfigResult(
    val success: Boolean,
    val param: String,
    val value: Int,
)
```

**Step 2: Commit**

```bash
git add kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/SensorConfig.kt
git commit -m "feat(kiosk): add SensorConfig data class"
```

---

### Task 7: Update JsonlParser for new message types

**Files:**
- Modify: `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/JsonlParser.kt`

**Step 1: Add parsing for new message types**

```kotlin
// Add to JsonlParser.kt:

fun parse(line: String): PresenceEvent? {
    return when {
        line.contains("\"type\":\"presence\"") -> parsePresence(line)
        line.contains("\"type\":\"heartbeat\"") -> parseHeartbeat(line)
        line.contains("\"type\":\"settings\"") -> parseSettings(line)
        line.contains("\"type\":\"config_result\"") -> parseConfigResult(line)
        line.contains("\"type\":\"calibration_status\"") -> parseCalibrationStatus(line)
        line.contains("\"type\":\"sensor_config\"") -> parseSensorConfig(line)
        else -> null
    }
}

private fun parseSettings(line: String): SettingsEvent? {
    // Parse JSON and return SettingsEvent
}

private fun parseConfigResult(line: String): ConfigResultEvent? {
    // Parse JSON and return ConfigResultEvent
}

private fun parseCalibrationStatus(line: String): CalibrationStatusEvent? {
    // Parse JSON and return CalibrationStatusEvent
}
```

**Step 2: Commit**

```bash
git add kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/JsonlParser.kt
git commit -m "feat(kiosk): add parsing for config/Calibration message types"
```

---

### Task 8: Add bidirectional serial support

**Files:**
- Modify: `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/SerialPresenceListener.kt`

**Step 1: Add sendCommand method**

```kotlin
// Add to SerialPresenceListener class:

private var outputStream: OutputStream? = null

// In readLoop, save outputStream:
outputStream = port.outputStream

fun sendCommand(command: String) {
    try {
        outputStream?.write("$command\n".toByteArray())
        outputStream?.flush()
        println("[SerialPresenceListener] Sent: $command")
    } catch (e: Exception) {
        println("[SerialPresenceListener] Failed to send: ${e.message}")
    }
}

fun sendConfig(param: String, value: Int, gate: Int? = null) {
    val command = if (gate != null) {
        """{"type":"set_config","param":"$param","value":$value,"gate":$gate}"""
    } else {
        """{"type":"set_config","param":"$param","value":$value}"""
    }
    sendCommand(command)
}

fun startCalibration() {
    sendCommand("""{"type":"calibrate","action":"start"}""")
}

fun requestSettings() {
    sendCommand("""{"type":"get_settings"}""")
}
```

**Step 2: Commit**

```bash
git add kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/SerialPresenceListener.kt
git commit -m "feat(kiosk): add bidirectional serial support for config commands"
```

---

### Task 9: Create SensorConfigManager

**Files:**
- Create: `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/SensorConfigManager.kt`

**Step 1: Create config manager**

```kotlin
package com.rghsoftware.kairos.serial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SensorConfigManager(
    private val listener: SerialPresenceListener,
    private val scope: CoroutineScope,
) {
    private val _config = MutableStateFlow(SensorConfig.DEFAULT)
    val config: StateFlow<SensorConfig> = _config.asStateFlow()
    
    private val _calibrationStatus = MutableStateFlow<CalibrationStatus?>(null)
    val calibrationStatus: StateFlow<CalibrationStatus?> = _calibrationStatus.asStateFlow()
    
    init {
        scope.launch {
            listener.events.collect { event ->
                when (event) {
                    is SettingsEvent -> _config.value = event.config
                    is CalibrationStatusEvent -> _calibrationStatus.value = event.status
                    is ConfigResultEvent -> {
                        if (event.success) {
                            requestSettings()
                        }
                    }
                }
            }
        }
    }
    
    fun requestSettings() {
        listener.requestSettings()
    }
    
    fun setMotionThreshold(value: Int) {
        listener.sendConfig("motion_threshold", value)
    }
    
    fun setFramesPresent(value: Int) {
        listener.sendConfig("frames_present", value)
    }
    
    fun setFramesAbsent(value: Int) {
        listener.sendConfig("frames_absent", value)
    }
    
    fun setMaxGate(value: Int) {
        listener.sendConfig("max_gate", value)
    }
    
    fun setMovingSensitivity(gate: Int, value: Int) {
        listener.sendConfig("moving_sensitivity", value, gate)
    }
    
    fun setStaticSensitivity(gate: Int, value: Int) {
        listener.sendConfig("static_sensitivity", value, gate)
    }
    
    fun startCalibration() {
        listener.startCalibration()
    }
}
```

**Step 2: Commit**

```bash
git add kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/serial/SensorConfigManager.kt
git commit -m "feat(kiosk): add SensorConfigManager for config state management"
```

---

## Phase 3: Kotlin Kiosk - Settings UI

### Task 10: Create SettingsScreen composable

**Files:**
- Create: `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/ui/SettingsScreen.kt`

**Step 1: Create settings screen (landscape, touchscreen-optimized)**

```kotlin
package com.rghsoftware.kairos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rghsoftware.kairos.serial.*

@Composable
fun SettingsScreen(
    configManager: SensorConfigManager,
    presenceState: PresenceState,
    onNavigateBack: () -> Unit,
) {
    val config by configManager.config.collectAsState()
    val calibrationStatus by configManager.calibrationStatus.collectAsState()
    
    Row(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // Left panel: Sensor Status
        SensorStatusPanel(
            presenceState = presenceState,
            calibrationStatus = calibrationStatus,
            onCalibrate = { configManager.startCalibration() },
            modifier = Modifier.weight(1f),
        )
        
        // Right panel: Configuration
        ConfigurationPanel(
            config = config,
            onMotionThresholdChange = { configManager.setMotionThreshold(it) },
            onFramesPresentChange = { configManager.setFramesPresent(it) },
            onFramesAbsentChange = { configManager.setFramesAbsent(it) },
            onMaxGateChange = { configManager.setMaxGate(it) },
            onMovingSensitivityChange = { gate, value -> 
                configManager.setMovingSensitivity(gate, value) 
            },
            onNavigateBack = onNavigateBack,
            modifier = Modifier.weight(2f),
        )
    }
}

@Composable
private fun SensorStatusPanel(
    presenceState: PresenceState,
    calibrationStatus: CalibrationStatus?,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("SENSOR STATUS", style = MaterialTheme.typography.h6)
        
        Text("State: ${presenceState.state}")
        Text("Energy: ${presenceState.energy}")
        Text("Distance: ${presenceState.distanceCm?.let { "${it}cm" } ?: "--"}")
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Large calibrate button (88dp height)
        Button(
            onClick = onCalibrate,
            modifier = Modifier.fillMaxWidth().height(88.dp),
        ) {
            Text("CALIBRATE", style = MaterialTheme.typography.h5)
        }
        
        // Calibration status
        calibrationStatus?.let { status ->
            Text("Status: ${status.phase}")
            if (status.phase == "sampling") {
                LinearProgressIndicator(
                    progress = status.progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (status.phase == "complete") {
                Text("Recommended threshold: ${status.samplesCollected}")
            }
        }
    }
}

@Composable
private fun ConfigurationPanel(
    config: SensorConfig,
    onMotionThresholdChange: (Int) -> Unit,
    onFramesPresentChange: (Int) -> Unit,
    onFramesAbsentChange: (Int) -> Unit,
    onMaxGateChange: (Int) -> Unit,
    onMovingSensitivityChange: (Int, Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("CONFIGURATION", style = MaterialTheme.typography.h6)
        
        // Software Filter section
        Text("Software Filter", style = MaterialTheme.typography.subtitle1)
        
        LabeledSlider(
            label = "Motion Threshold",
            value = config.motionThreshold,
            range = 0..100,
            onValueChange = onMotionThresholdChange,
        )
        
        LabeledSlider(
            label = "Frames to Present",
            value = config.framesPresent,
            range = 1..30,
            onValueChange = onFramesPresentChange,
        )
        
        LabeledSlider(
            label = "Frames to Absent",
            value = config.framesAbsent,
            range = 1..50,
            onValueChange = onFramesAbsentChange,
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sensor Hardware section
        Text("Sensor Hardware (LD2410C)", style = MaterialTheme.typography.subtitle1)
        
        LabeledSlider(
            label = "Max Distance (gates)",
            value = config.maxGate,
            range = 1..8,
            onValueChange = onMaxGateChange,
            valueText = "${config.maxGate + 1} gates (${(config.maxGate + 1) * 0.75}m)",
        )
        
        Text("Distance Sensitivity:", style = MaterialTheme.typography.body1)
        
        val distanceRanges = listOf(
            "0 - 0.75m", "0.75 - 1.5m", "1.5 - 2.25m", "2.25 - 3.0m",
            "3.0 - 3.75m", "3.75 - 4.5m", "4.5 - 5.25m", "5.25 - 6.0m", "6.0 - 6.75m"
        )
        
        config.movingSensitivity.forEachIndexed { gate, sensitivity ->
            LabeledSlider(
                label = distanceRanges[gate],
                value = sensitivity,
                range = 0..100,
                onValueChange = { onMovingSensitivityChange(gate, it) },
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f).height(64.dp),
            ) {
                Text("BACK")
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    valueText: String? = null,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label)
            Text(valueText ?: value.toString())
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        )
    }
}
```

**Step 2: Commit**

```bash
git add kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/ui/SettingsScreen.kt
git commit -m "feat(kiosk): add SettingsScreen with landscape touchscreen layout"
```

---

### Task 11: Wire navigation to SettingsScreen

**Files:**
- Modify: `kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/Main.kt`

**Step 1: Add settings screen to navigation**

Add a settings button to the main screen that navigates to `SettingsScreen`. Pass the `SensorConfigManager` and presence state.

**Step 2: Test**

Run: `cd kotlin && ./gradlew :kiosk:run`
Expected: Can navigate to settings, see real-time sensor data, adjust sliders

**Step 3: Commit**

```bash
git add kotlin/kiosk/src/jvmMain/kotlin/com/rghsoftware/kairos/Main.kt
git commit -m "feat(kiosk): wire settings screen navigation"
```

---

## Phase 4: Integration Testing

### Task 12: End-to-end test

**Step 1: Flash ESP32**

Run: `cd firmware/presence-esp32 && idf.py -p /dev/ttyACM0 flash monitor`

**Step 2: Start kiosk app**

Run: `cd kotlin && ./gradlew :kiosk:run`

**Step 3: Verify**

1. Settings emitted on ESP32 startup
2. Kiosk receives and displays settings
3. Adjust slider → ESP32 receives command → confirms → kiosk updates
4. Calibrate button → progress shown → recommended threshold displayed

**Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete presence sensor configuration/calibration system"
```

---

## Success Criteria

- [ ] ESP32 stores config in NVS, loads on boot
- [ ] ESP32 emits settings on connect
- [ ] Kiosk can request settings
- [ ] Kiosk can set motion_threshold, frames_present, frames_absent
- [ ] Kiosk can set max_gate, per-gate sensitivities
- [ ] Calibration mode samples for 10s, recommends threshold
- [ ] Settings UI shows distance labels (not gate numbers)
- [ ] Touch targets are 48px+ for all interactive elements
- [ ] Real-time sensor feedback visible while tuning
