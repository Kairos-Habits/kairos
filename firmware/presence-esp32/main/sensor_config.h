#pragma once

#include <stdint.h>
#include <stdbool.h>
#include "esp_err.h"

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
