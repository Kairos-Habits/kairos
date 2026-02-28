# Firmware — ESP32 Presence Detection

**Generated:** 2026-02-28 | **Commit:** 7dd282c

ESP32 mmWave radar presence detection with JSONL serial protocol.

## Structure

```
firmware/presence-esp32/
├── main/
│   ├── presence_main.c      # Entry point (app_main)
│   ├── mmwave_sensor.c/h    # LD2410 radar driver
│   ├── presence_debounce.c/h # State debounce logic
│   ├── jsonl_output.c/h     # JSONL protocol output
│   ├── jsonl_input.c/h      # JSONL config input
│   └── sensor_config.c/h    # Runtime configuration
├── CMakeLists.txt           # ESP-IDF build config
└── sdkconfig                # ESP32-S3 settings
```

## Where to Look

| Task | Location |
|------|----------|
| Entry point | `presence_main.c` — app_main() |
| Sensor driver | `mmwave_sensor.c` — UART commands |
| Debounce logic | `presence_debounce.c` — 1s/2s timing |
| JSONL output | `jsonl_output.c` — Protocol formatting |

## JSONL Protocol

All output is line-delimited JSON over USB CDC (115200 baud):

```c
// Presence event (debounced)
printf("{\"type\":\"presence\",\"state\":\"PRESENT\",\"timestamp_ms\":%lld}\n", ts);
fflush(stdout);  // ALWAYS flush after each message

// Heartbeat (every 5s)
printf("{\"type\":\"heartbeat\",\"timestamp_ms\":%lld}\n", ts);
fflush(stdout);
```

Message types: `presence`, `heartbeat`, `settings`, `sensor_config`, `config_result`, `calibration_status`

## Debounce Timing

- PRESENT requires 1000ms stable detection
- ABSENT requires 2000ms stable absence
- Debounce happens in firmware, not kiosk

## Commands

```bash
idf.py build                     # Build firmware
idf.py -p /dev/ttyUSB0 flash     # Flash to ESP32
idf.py -p /dev/ttyUSB0 monitor   # Serial monitor
```

## Anti-Patterns (THIS MODULE)

- **NEVER** output non-JSON to stdout (breaks kiosk parser)
- **NEVER** skip `fflush(stdout)` after messages
- **NEVER** use blocking delays in sensor task (use vTaskDelay)
- **NEVER** hardcode sensor thresholds (use runtime config)

## ESP-IDF Conventions

- Use `ESP_LOGI/ESP_LOGE/ESP_LOGW` with TAG for logging
- Return `esp_err_t` from init functions
- Use `pdMS_TO_TICKS()` for FreeRTOS timing
- Use `esp_timer_get_time()` for timestamps
