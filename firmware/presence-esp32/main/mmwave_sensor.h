#ifndef MMWAVE_SENSOR_H
#define MMWAVE_SENSOR_H

#include "esp_err.h"
#include <stdbool.h>
#include <stdint.h>

/**
 * HLK-LD2410C mmWave presence sensor driver (UART mode).
 *
 * Uses UART to communicate with LD2410C for full sensor data including:
 * - Moving target distance and energy
 * - Static target distance and energy
 * - Presence detection state
 *
 * Default wiring:
 *   LD2410C TX  -> ESP32 RX (MMWAVE_RX_PIN)
 *   LD2410C RX  -> ESP32 TX (MMWAVE_TX_PIN)
 *   LD2410C GND -> ESP32 GND
 *   LD2410C VCC -> 5V
 */

/* UART configuration */
#define MMWAVE_UART_NUM UART_NUM_1
#define MMWAVE_TX_PIN 4
#define MMWAVE_RX_PIN 5
#define MMWAVE_BAUD_RATE 256000
#define MMWAVE_BUF_SIZE 256

/* Presence states */
typedef enum {
  MMWAVE_STATE_ABSENT = 0,
  MMWAVE_STATE_PRESENT = 1,
} mmwave_presence_state_t;

/* Sensor data structure */
typedef struct {
  mmwave_presence_state_t state;
  uint16_t moving_distance;    /* Distance in cm to moving target */
  uint8_t moving_energy;       /* Energy level 0-100 for moving target */
  uint16_t static_distance;    /* Distance in cm to static target */
  uint8_t static_energy;       /* Energy level 0-100 for static target */
  uint16_t detection_distance; /* Configured detection distance */
} mmwave_data_t;

/**
 * Initialize the mmWave sensor UART.
 *
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t mmwave_init(void);

/**
 * Enter or exit configuration mode.
 * Must call this before setting gate sensitivity or max distance.
 *
 * @param enter true to enter config mode, false to exit
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t mmwave_enter_config_mode(bool enter);

/**
 * Enable/disable engineering mode for detailed gate data.
 *
 * @param enable true to enable, false to disable
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t mmwave_enable_engineering_mode(bool enable);

/**
 * Set sensitivity threshold for a specific gate.
 *
 * @param gate Gate number (0-7, each ~75cm)
 * @param moving_threshold Moving target threshold (0-100, higher = less
 * sensitive)
 * @param static_threshold Static target threshold (0-100, higher = less
 * sensitive)
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t mmwave_set_gate_sensitivity(int gate, uint8_t moving_threshold,
                                      uint8_t static_threshold);

/**
 * Set maximum detection distance and default thresholds.
 *
 * @param max_gate Maximum gate (0-7), detection distance = (max_gate+1)*75cm
 * @param moving_threshold Moving threshold for farthest gate
 * @param static_threshold Static threshold for farthest gate
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t mmwave_set_max_distance(uint8_t max_gate, uint8_t moving_threshold,
                                  uint8_t static_threshold);

/**
 * Read presence data from the sensor via UART.
 *
 * @param data output: sensor data including presence state and distances
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t mmwave_read_data(mmwave_data_t *data);

/**
 * Deinitialize the mmWave sensor.
 */
void mmwave_deinit(void);

#endif /* MMWAVE_SENSOR_H */
