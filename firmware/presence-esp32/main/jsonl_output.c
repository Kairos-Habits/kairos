#include "jsonl_output.h"
#include "esp_log.h"
#include <stdio.h>

static const char *TAG = "jsonl";

void jsonl_emit_presence(bool is_present, int64_t timestamp_ms,
                         uint16_t moving_cm, uint16_t static_cm,
                         uint16_t detect_max_cm) {
  const char *state = is_present ? "PRESENT" : "ABSENT";
  printf("{\"type\":\"presence\",\"state\":\"%s\",\"timestamp_ms\":%lld,"
         "\"moving_cm\":%u,\"static_cm\":%u,\"detect_max_cm\":%u}\n",
         state, (long long)timestamp_ms, moving_cm, static_cm, detect_max_cm);
  fflush(stdout);
  ESP_LOGD(TAG, "Emitted presence: %s (moving=%u, static=%u, max=%u)", state,
           moving_cm, static_cm, detect_max_cm);
}

void jsonl_emit_heartbeat(int64_t timestamp_ms) {
  printf("{\"type\":\"heartbeat\",\"timestamp_ms\":%lld}\n",
         (long long)timestamp_ms);
  fflush(stdout);
  ESP_LOGD(TAG, "Emitted heartbeat");
}

void jsonl_emit_settings(sensor_config_t *config) {
  printf("{\"type\":\"settings\","
         "\"motion_threshold\":%u,"
         "\"frames_present\":%u,"
         "\"frames_absent\":%u,"
         "\"sensor\":{\"max_gate\":%u,"
         "\"moving_sensitivity\":[%u,%u,%u,%u,%u,%u,%u,%u,%u],"
         "\"static_sensitivity\":[%u,%u,%u,%u,%u,%u,%u,%u,%u]}}\n",
         config->motion_threshold, config->frames_present,
         config->frames_absent, config->max_gate, config->moving_sensitivity[0],
         config->moving_sensitivity[1], config->moving_sensitivity[2],
         config->moving_sensitivity[3], config->moving_sensitivity[4],
         config->moving_sensitivity[5], config->moving_sensitivity[6],
         config->moving_sensitivity[7], config->moving_sensitivity[8],
         config->static_sensitivity[0], config->static_sensitivity[1],
         config->static_sensitivity[2], config->static_sensitivity[3],
         config->static_sensitivity[4], config->static_sensitivity[5],
         config->static_sensitivity[6], config->static_sensitivity[7],
         config->static_sensitivity[8]);
  fflush(stdout);
  ESP_LOGI(TAG, "Emitted settings");
}

void jsonl_emit_config_result(const char *param, bool success, int value) {
  printf("{\"type\":\"config_result\",\"success\":%s,\"param\":\"%s\","
         "\"value\":%d}\n",
         success ? "true" : "false", param, value);
  fflush(stdout);
  ESP_LOGD(TAG, "Config result: %s=%d (%s)", param, value,
           success ? "ok" : "fail");
}

void jsonl_emit_calibration_status(const char *phase, int progress,
                                   int samples) {
  printf("{\"type\":\"calibration_status\",\"phase\":\"%s\",\"progress\":%d,"
         "\"samples_collected\":%d}\n",
         phase, progress, samples);
  fflush(stdout);
  ESP_LOGD(TAG, "Calibration: %s %d%% samples=%d", phase, progress, samples);
}

void jsonl_emit_sensor_config(uint8_t max_gate, uint8_t *moving,
                              uint8_t *static_sens) {
  printf("{\"type\":\"sensor_config\",\"max_gate\":%u,"
         "\"moving_sensitivity\":[%u,%u,%u,%u,%u,%u,%u,%u,%u],"
         "\"static_sensitivity\":[%u,%u,%u,%u,%u,%u,%u,%u,%u]}\n",
         max_gate, moving[0], moving[1], moving[2], moving[3], moving[4],
         moving[5], moving[6], moving[7], moving[8], static_sens[0],
         static_sens[1], static_sens[2], static_sens[3], static_sens[4],
         static_sens[5], static_sens[6], static_sens[7], static_sens[8]);
  fflush(stdout);
  ESP_LOGD(TAG, "Emitted sensor config");
}
