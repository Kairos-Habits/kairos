#include "sensor_config.h"
#include "esp_log.h"
#include "nvs.h"
#include "nvs_flash.h"

static const char *TAG = "sensor_config";
static sensor_config_t config;
static nvs_handle_t nvs_h;

void sensor_config_init(void) {
  // NVS should already be initialized in app_main
  esp_err_t err = nvs_open("presence", NVS_READWRITE, &nvs_h);
  if (err != ESP_OK) {
    ESP_LOGE(TAG, "Failed to open NVS namespace 'presence': %s",
             esp_err_to_name(err));
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

  // Default sensitivities: 50 for gates 0-5, 0 for gates 6-8
  for (int i = 0; i < MAX_GATES; i++) {
    config.moving_sensitivity[i] = (i < 6) ? 50 : 0;
    config.static_sensitivity[i] = 0;
  }
}

esp_err_t sensor_config_load(void) {
  esp_err_t err;

  err = nvs_get_u8(nvs_h, "motion_thresh", &config.motion_threshold);
  if (err != ESP_OK)
    return err;

  err = nvs_get_u8(nvs_h, "frames_present", &config.frames_present);
  if (err != ESP_OK)
    return err;

  err = nvs_get_u8(nvs_h, "frames_absent", &config.frames_absent);
  if (err != ESP_OK)
    return err;

  err = nvs_get_u8(nvs_h, "max_gate", &config.max_gate);
  if (err != ESP_OK)
    return err;

  size_t len = MAX_GATES;
  err = nvs_get_blob(nvs_h, "moving_sens", config.moving_sensitivity, &len);
  if (err != ESP_OK)
    return err;

  len = MAX_GATES;
  err = nvs_get_blob(nvs_h, "static_sens", config.static_sensitivity, &len);
  if (err != ESP_OK)
    return err;

  ESP_LOGI(TAG, "Config loaded: thresh=%d, present=%d, absent=%d, max_gate=%d",
           config.motion_threshold, config.frames_present, config.frames_absent,
           config.max_gate);
  return ESP_OK;
}

esp_err_t sensor_config_save(void) {
  esp_err_t err = ESP_OK;

  err |= nvs_set_u8(nvs_h, "motion_thresh", config.motion_threshold);
  err |= nvs_set_u8(nvs_h, "frames_present", config.frames_present);
  err |= nvs_set_u8(nvs_h, "frames_absent", config.frames_absent);
  err |= nvs_set_u8(nvs_h, "max_gate", config.max_gate);
  err |=
      nvs_set_blob(nvs_h, "moving_sens", config.moving_sensitivity, MAX_GATES);
  err |=
      nvs_set_blob(nvs_h, "static_sens", config.static_sensitivity, MAX_GATES);
  err |= nvs_commit(nvs_h);

  if (err == ESP_OK) {
    ESP_LOGI(TAG, "Config saved to NVS");
  } else {
    ESP_LOGE(TAG, "Failed to save config: %s", esp_err_to_name(err));
  }
  return err;
}

sensor_config_t *sensor_config_get(void) { return &config; }
