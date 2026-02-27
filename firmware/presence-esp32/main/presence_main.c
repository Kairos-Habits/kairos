/*
 * Kairos Presence Detection - ESP32 Firmware
 *
 * Reads mmWave presence sensor (LD2410C) via UART and emits debounced presence
 * events over USB CDC serial in JSONL format.
 *
 * Protocol:
 *   {"type":"presence","state":"PRESENT","timestamp_ms":1234567890}
 *   {"type":"heartbeat","timestamp_ms":1234567895}
 *   {"type":"settings","motion_threshold":50,"frames_present":5,...}
 *
 * Debounce: 1000ms for presence, 2000ms for absence
 * Heartbeat: every 5 seconds
 *
 * Configuration via stdin JSONL commands:
 *   {"type":"get_settings"}
 *   {"type":"set_config","param":"motion_threshold","value":50}
 *
 * Filter: Only trigger on MOVING targets (status 1 or 3), ignore static-only
 * (status 2) to reduce false positives from walls/furniture.
 */

#include "esp_log.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "jsonl_input.h"
#include "jsonl_output.h"
#include "mmwave_sensor.h"
#include "presence_debounce.h"
#include "sensor_config.h"
#include <stdbool.h>
#include <stdio.h>
#include <string.h>

static const char *TAG = "presence";

/* Task intervals */
#define SENSOR_POLL_INTERVAL_MS 100
#define HEARTBEAT_INTERVAL_MS 5000
#define STDIN_POLL_INTERVAL_MS 50

/* Debounce configuration (now from config, these are fallbacks) */
#define DEFAULT_PRESENCE_DEBOUNCE_MS 1000
#define DEFAULT_ABSENCE_DEBOUNCE_MS 3000

/* Debouncer state */
static presence_debouncer_t debouncer;

/* Get current timestamp in milliseconds */
static int64_t get_timestamp_ms(void) { return esp_timer_get_time() / 1000; }

/* Sensor polling task */
static void sensor_task(void *arg) {
  ESP_LOGI(TAG, "Sensor task started");

  bool last_filtered_state = false;
  int motion_count = 0;
  int no_motion_count = 0;

  while (1) {
    sensor_config_t *config = sensor_config_get();

    mmwave_data_t sensor_data;
    esp_err_t ret = mmwave_read_data(&sensor_data);

    if (ret == ESP_OK) {
      int64_t now_ms = get_timestamp_ms();

      /* Check if this frame has motion using config threshold */
      bool has_motion = sensor_data.moving_energy >= config->motion_threshold;

      /* Update counters */
      if (has_motion) {
        motion_count++;
        no_motion_count = 0;
      } else {
        no_motion_count++;
        motion_count = 0;
      }

      /* Determine filtered state based on consecutive frames from config */
      bool filtered_present;
      if (last_filtered_state) {
        /* Currently PRESENT - need sustained absence to switch */
        filtered_present = (no_motion_count < config->frames_absent);
      } else {
        /* Currently ABSENT - need sustained presence to switch */
        filtered_present = (motion_count >= config->frames_present);
      }

      /* Log when filtered state changes */
      if (filtered_present != last_filtered_state) {
        ESP_LOGI(TAG,
                 "Filtered state: %s->%s (energy=%d, motion=%d, no_motion=%d)",
                 last_filtered_state ? "PRESENT" : "ABSENT",
                 filtered_present ? "PRESENT" : "ABSENT",
                 sensor_data.moving_energy, motion_count, no_motion_count);
        last_filtered_state = filtered_present;
      }

      bool state_changed =
          presence_debouncer_update(&debouncer, filtered_present, now_ms);

      if (state_changed) {
        bool debounced_state = presence_debouncer_get_state(&debouncer);
        jsonl_emit_presence(
            debounced_state, now_ms, sensor_data.moving_distance,
            sensor_data.static_distance, sensor_data.detection_distance);
        ESP_LOGI(
            TAG, "Presence state changed: %s (moving=%dcm@%d, static=%dcm)",
            debounced_state ? "PRESENT" : "ABSENT", sensor_data.moving_distance,
            sensor_data.moving_energy, sensor_data.static_distance);
      }
    } else {
      ESP_LOGW(TAG, "Failed to read sensor: %s", esp_err_to_name(ret));
    }

    vTaskDelay(pdMS_TO_TICKS(SENSOR_POLL_INTERVAL_MS));
  }
}

/* Heartbeat task for liveness detection */
static void heartbeat_task(void *arg) {
  ESP_LOGI(TAG, "Heartbeat task started");

  while (1) {
    int64_t now_ms = get_timestamp_ms();
    jsonl_emit_heartbeat(now_ms);

    vTaskDelay(pdMS_TO_TICKS(HEARTBEAT_INTERVAL_MS));
  }
}

/* Stdin command processing task */
static void stdin_task(void *arg) {
  ESP_LOGI(TAG, "Stdin command task started");

  char line_buf[256];
  int line_pos = 0;

  while (1) {
    int c = getchar();
    if (c != EOF) {
      if (c == '\n' || c == '\r') {
        if (line_pos > 0) {
          line_buf[line_pos] = '\0';
          ESP_LOGD(TAG, "Received command: %s", line_buf);
          jsonl_input_process(line_buf);
          line_pos = 0;
        }
      } else if (line_pos < (int)sizeof(line_buf) - 1) {
        line_buf[line_pos++] = (char)c;
      }
    }

    vTaskDelay(pdMS_TO_TICKS(STDIN_POLL_INTERVAL_MS));
  }
}

void app_main(void) {
  ESP_LOGI(TAG, "Kairos Presence Detection starting...");

  /* Initialize sensor config from NVS */
  sensor_config_init();
  sensor_config_t *config = sensor_config_get();
  ESP_LOGI(
      TAG, "Config loaded: threshold=%d, frames_present=%d, frames_absent=%d",
      config->motion_threshold, config->frames_present, config->frames_absent);

  /* Initialize JSONL input parser */
  jsonl_input_init();

  /* Initialize debouncer */
  presence_debouncer_init(&debouncer, DEFAULT_PRESENCE_DEBOUNCE_MS,
                          DEFAULT_ABSENCE_DEBOUNCE_MS);
  ESP_LOGI(TAG, "Debouncer initialized (present=%dms, absent=%dms)",
           DEFAULT_PRESENCE_DEBOUNCE_MS, DEFAULT_ABSENCE_DEBOUNCE_MS);

  /* Initialize mmWave sensor */
  esp_err_t ret = mmwave_init();
  if (ret != ESP_OK) {
    ESP_LOGE(TAG, "Failed to initialize mmWave sensor: %s",
             esp_err_to_name(ret));
    /* Continue anyway - will retry in sensor task */
  }

  /* Emit initial heartbeat to confirm startup */
  jsonl_emit_heartbeat(get_timestamp_ms());

  /* Create sensor polling task */
  xTaskCreate(sensor_task, "sensor_task", 4096, NULL, 5, NULL);

  /* Create heartbeat task */
  xTaskCreate(heartbeat_task, "heartbeat_task", 2048, NULL, 3, NULL);

  /* Create stdin command task */
  xTaskCreate(stdin_task, "stdin_task", 4096, NULL, 4, NULL);

  ESP_LOGI(TAG, "Tasks started, monitoring presence...");
}
