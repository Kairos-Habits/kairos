#pragma once

#include <stdbool.h>
#include "esp_err.h"

typedef enum {
    CMD_NONE,
    CMD_GET_SETTINGS,
    CMD_GET_SENSOR_CONFIG,
    CMD_SET_CONFIG,
    CMD_CALIBRATE_START,
    CMD_CALIBRATE_CANCEL,
    CMD_CALIBRATE_APPLY,
} jsonl_command_type_t;

typedef struct {
    jsonl_command_type_t command;
    char param[32];
    int value;
    int gate;  // For gate-specific commands (-1 if not used)
} jsonl_input_t;

/**
 * Initialize the JSONL input parser.
 */
void jsonl_input_init(void);

/**
 * Parse a JSONL command line.
 * @param line the input line to parse
 * @param out pointer to store parsed command
 * @return true if successfully parsed, false otherwise
 */
bool jsonl_input_parse(const char *line, jsonl_input_t *out);

/**
 * Process a JSONL command line and execute the command.
 * @param line the input line to process
 */
void jsonl_input_process(const char *line);

/**
 * Check if calibration mode is active.
 * @return true if calibrating
 */
bool jsonl_is_calibrating(void);

/**
 * Get the recommended threshold from calibration.
 * @return recommended threshold (only valid after calibration completes)
 */
int jsonl_get_calibration_result(void);

/**
 * Reset calibration state.
 */
void jsonl_calibration_reset(void);
