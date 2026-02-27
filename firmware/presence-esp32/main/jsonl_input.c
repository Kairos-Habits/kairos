#include "jsonl_input.h"
#include "esp_log.h"
#include "jsonl_output.h"
#include "sensor_config.h"
#include <stdlib.h>
#include <string.h>

static const char *TAG = "jsonl_in";

/* Calibration constants */
#define CALIBRATION_DURATION_SAMPLES 100 // 10 seconds at 100ms per sample
#define CALIBRATION_SAFETY_MARGIN 10     // Add to max observed energy

static bool calibrating = false;
static int calibration_result = 0;
static int calibration_samples = 0;
static uint8_t calibration_max_energy = 0;

void jsonl_input_init(void) {
  calibrating = false;
  calibration_result = 0;
  calibration_samples = 0;
  calibration_max_energy = 0;
}

static bool parse_string(const char *json, const char *key, char *out,
                         size_t max_len) {
  char search[64];
  snprintf(search, sizeof(search), "\"%s\":\"", key);
  const char *start = strstr(json, search);
  if (!start)
    return false;

  start += strlen(search);
  const char *end = strchr(start, '"');
  if (!end)
    return false;

  size_t len = end - start;
  if (len >= max_len)
    len = max_len - 1;
  strncpy(out, start, len);
  out[len] = '\0';
  return true;
}

static bool parse_int(const char *json, const char *key, int *out) {
  char search[64];
  snprintf(search, sizeof(search), "\"%s\":", key);
  const char *start = strstr(json, search);
  if (!start)
    return false;

  start += strlen(search);
  // Skip whitespace
  while (*start == ' ' || *start == '\t')
    start++;

  *out = atoi(start);
  return true;
}

bool jsonl_input_parse(const char *line, jsonl_input_t *out) {
  memset(out, 0, sizeof(jsonl_input_t));
  out->gate = -1; // Default: no gate specified

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
      if (strcmp(action, "start") == 0)
        out->command = CMD_CALIBRATE_START;
      else if (strcmp(action, "cancel") == 0)
        out->command = CMD_CALIBRATE_CANCEL;
      else if (strcmp(action, "apply") == 0)
        out->command = CMD_CALIBRATE_APPLY;
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
    jsonl_emit_sensor_config(config->max_gate, config->moving_sensitivity,
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
      } else {
        jsonl_emit_config_result(cmd.param, false, 0);
      }
    } else if (strcmp(cmd.param, "static_sensitivity") == 0) {
      if (cmd.gate >= 0 && cmd.gate < MAX_GATES) {
        config->static_sensitivity[cmd.gate] = (uint8_t)cmd.value;
        sensor_config_save();
        jsonl_emit_config_result(cmd.param, true, cmd.value);
      } else {
        jsonl_emit_config_result(cmd.param, false, 0);
      }
    } else {
      jsonl_emit_config_result(cmd.param, false, 0);
    }
    break;

  case CMD_CALIBRATE_START:
    calibrating = true;
    calibration_samples = 0;
    calibration_max_energy = 0;
    calibration_result = 0;
    ESP_LOGI(TAG, "Calibration started - sampling ambient for 10 seconds");
    break;

  case CMD_CALIBRATE_CANCEL:
    calibrating = false;
    calibration_result = 0;
    calibration_samples = 0;
    calibration_max_energy = 0;
    ESP_LOGI(TAG, "Calibration cancelled");
    break;

  case CMD_CALIBRATE_APPLY:
    if (calibration_result > 0) {
      config->motion_threshold = (uint8_t)calibration_result;
      sensor_config_save();
      jsonl_emit_config_result("motion_threshold", true, calibration_result);
      ESP_LOGI(TAG, "Applied calibration result: %d", calibration_result);
    }
    calibrating = false;
    calibration_result = 0;
    calibration_samples = 0;
    calibration_max_energy = 0;
    break;

  default:
    break;
  }
}

bool jsonl_is_calibrating(void) { return calibrating; }

int jsonl_get_calibration_result(void) { return calibration_result; }

void jsonl_calibration_reset(void) {
  calibrating = false;
  calibration_result = 0;
  calibration_samples = 0;
  calibration_max_energy = 0;
}

void jsonl_calibration_sample(uint8_t energy) {
  if (!calibrating)
    return;

  // Track max energy
  if (energy > calibration_max_energy) {
    calibration_max_energy = energy;
  }

  calibration_samples++;

  // Report progress every 10 samples (1 second)
  if (calibration_samples % 10 == 0) {
    int progress = (calibration_samples * 100) / CALIBRATION_DURATION_SAMPLES;
    jsonl_emit_calibration_status("sampling", progress, calibration_samples);
  }

  // Check if calibration complete
  if (calibration_samples >= CALIBRATION_DURATION_SAMPLES) {
    // Compute recommended threshold: max + margin, clamped to 0-100
    int threshold = calibration_max_energy + CALIBRATION_SAFETY_MARGIN;
    if (threshold > 100)
      threshold = 100;
    if (threshold < 1)
      threshold = 1;

    calibration_result = threshold;
    calibrating = false; // Stop sampling

    jsonl_emit_calibration_status("complete", 100, threshold);
    ESP_LOGI(TAG,
             "Calibration complete: max_energy=%d, recommended_threshold=%d",
             calibration_max_energy, threshold);
  }
}

bool jsonl_calibration_is_complete(void) {
  return calibration_result > 0 && !calibrating;
}
