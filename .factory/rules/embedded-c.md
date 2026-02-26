# Embedded C / ESP-IDF Rules

Applies to: `firmware/` directory (ESP32 firmware)

## General C Rules

### Explicit return types
**Applies to**: All functions
**Rule**: Always declare explicit return types. Never rely on implicit int.

```c
// ✅ Correct
static void process_presence_event(void) { }
static esp_err_t init_sensor(void) { }

// ❌ Avoid
static process_presence_event() { }  // implicit int
```

### Static functions for internal use
**Applies to**: Functions not exposed outside compilation unit
**Rule**: Use `static` for all functions that are not part of the public API.

### Const for read-only pointers
**Applies to**: Function parameters, constants
**Rule**: Use `const` for pointers that should not be modified.

```c
// ✅ Correct
static void parse_json(const char *json_str, size_t len);
```

## ESP-IDF Specific

### Use ESP-IDF error handling pattern
**Applies to**: All functions returning esp_err_t
**Rule**: Use `ESP_OK`, `ESP_FAIL`, and `ESP_ERROR_CHECK` consistently.

```c
// ✅ Correct
esp_err_t ret = uart_driver_install(UART_NUM, BUF_SIZE, 0, NULL, 0, 0);
if (ret != ESP_OK) {
    ESP_LOGE(TAG, "UART driver install failed: %s", esp_err_to_name(ret));
    return ret;
}

// Or for fatal errors:
ESP_ERROR_CHECK(uart_driver_install(UART_NUM, BUF_SIZE, 0, NULL, 0, 0));
```

### Use ESP-IDF logging
**Applies to**: All logging
**Rule**: Use `ESP_LOGI`, `ESP_LOGW`, `ESP_LOGE`, `ESP_LOGD` macros with TAG.

```c
// ✅ Correct
static const char *TAG = "presence";

ESP_LOGI(TAG, "Presence detected");
ESP_LOGE(TAG, "Sensor read failed: %d", error_code);
```

### FreeRTOS task structure
**Applies to**: FreeRTOS tasks
**Rule**: Tasks should loop forever with vTaskDelay to yield.

```c
// ✅ Correct
static void presence_task(void *arg) {
    while (1) {
        check_presence();
        vTaskDelay(pdMS_TO_TICKS(100));
    }
    vTaskDelete(NULL);  // Never reached but good practice
}
```

## JSONL Protocol

### JSON format for all output
**Applies to**: Serial output
**Rule**: All serial output must be valid JSON followed by newline.

```c
// ✅ Correct
printf("{\"type\":\"presence\",\"state\":\"PRESENT\",\"timestamp_ms\":%lld}\n", timestamp);

// ❌ Avoid - debug output without JSON format
printf("Presence detected!\n");
```

### Flush after each message
**Applies to**: Serial output
**Rule**: Call `fflush(stdout)` after each JSONL message to ensure delivery.

### Debounce in firmware
**Applies to**: Presence detection
**Rule**: Debounce presence events in firmware, not on the receiving end.

```c
// Presence must be stable >= 1000ms before emitting PRESENT
// Absence must be stable >= 2000ms before emitting ABSENT
#define PRESENCE_DEBOUNCE_MS 1000
#define ABSENCE_DEBOUNCE_MS 2000
```

## Memory Management

### Prefer stack allocation
**Applies to**: Local buffers
**Rule**: Use stack allocation for small, short-lived buffers.

### Heap for large/buffers
**Applies to**: Large buffers, variable-size data
**Rule**: Use heap allocation (malloc/free) for large buffers. Always check for NULL.

```c
// ✅ Correct
char *buffer = malloc(BUF_SIZE);
if (buffer == NULL) {
    ESP_LOGE(TAG, "Failed to allocate buffer");
    return ESP_ERR_NO_MEM;
}
// ... use buffer ...
free(buffer);
```

## Timing

### Use FreeRTOS ticks for timing
**Applies to**: Delays, timeouts
**Rule**: Use `pdMS_TO_TICKS()` to convert milliseconds to ticks.

```c
// ✅ Correct
vTaskDelay(pdMS_TO_TICKS(1000));  // 1 second delay
```

### esp_timer for timestamps
**Applies to**: Timestamps
**Rule**: Use `esp_timer_get_time()` for high-resolution timestamps.

```c
// ✅ Correct
int64_t timestamp_ms = esp_timer_get_time() / 1000;
```
