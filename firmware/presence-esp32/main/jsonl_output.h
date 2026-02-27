#ifndef JSONL_OUTPUT_H
#define JSONL_OUTPUT_H

#include "sensor_config.h"
#include <stdbool.h>
#include <stdint.h>

/**
 * Output JSONL messages over USB CDC.
 * All output is line-delimited JSON followed by newline.
 */

/**
 * Emit a presence event.
 * @param is_present true for PRESENT, false for ABSENT
 * @param timestamp_ms timestamp in milliseconds
 * @param moving_cm moving target distance in cm (0 if unknown)
 * @param static_cm static target distance in cm (0 if unknown)
 * @param detect_max_cm maximum detection distance configured on sensor
 */
void jsonl_emit_presence(bool is_present, int64_t timestamp_ms,
                         uint16_t moving_cm, uint16_t static_cm,
                         uint16_t detect_max_cm);

/**
 * Emit a heartbeat event for liveness detection.
 * @param timestamp_ms timestamp in milliseconds
 */
void jsonl_emit_heartbeat(int64_t timestamp_ms);

/**
 * Emit current sensor settings.
 * @param config pointer to current config
 */
void jsonl_emit_settings(sensor_config_t *config);

/**
 * Emit config command result.
 * @param param parameter name that was set
 * @param success true if operation succeeded
 * @param value the value that was set
 */
void jsonl_emit_config_result(const char *param, bool success, int value);

/**
 * Emit calibration status update.
 * @param phase "sampling" or "complete"
 * @param progress 0-100 percentage
 * @param samples number of samples collected (or recommended threshold when
 * complete)
 */
void jsonl_emit_calibration_status(const char *phase, int progress,
                                   int samples);

/**
 * Emit LD2410C sensor hardware config.
 * @param max_gate maximum detection gate
 * @param moving moving sensitivity array (MAX_GATES elements)
 * @param static_sens static sensitivity array (MAX_GATES elements)
 */
void jsonl_emit_sensor_config(uint8_t max_gate, uint8_t *moving,
                              uint8_t *static_sens);

#endif /* JSONL_OUTPUT_H */
