#include "mmwave_sensor.h"
#include "driver/uart.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <string.h>

static const char *TAG = "mmwave";

/* Receive buffer for continuous data stream */
static uint8_t rx_buffer[512];
static size_t rx_len = 0;

static bool initialized = false;

/* Command frame header/tail */
static const uint8_t CMD_HEADER[] = {0xFD, 0xFC, 0xFB, 0xFA};
static const uint8_t CMD_TAIL[] = {0x04, 0x03, 0x02, 0x01};

/* Debug: print hex dump of bytes */
static void hex_dump(const char *label, const uint8_t *data, size_t len) {
  if (len == 0)
    return;
  char buf[128];
  int pos = 0;
  pos += snprintf(buf + pos, sizeof(buf) - pos, "%s (%d): ", label, (int)len);
  for (size_t i = 0; i < len && i < 24; i++) {
    pos += snprintf(buf + pos, sizeof(buf) - pos, "%02X ", data[i]);
  }
  if (len > 24)
    pos += snprintf(buf + pos, sizeof(buf) - pos, "...");
  ESP_LOGI(TAG, "%s", buf);
}

/* Send command frame and wait for response */
static esp_err_t send_command(const uint8_t *cmd_data, size_t cmd_len,
                              uint8_t *response, size_t *resp_len,
                              size_t max_resp_len, int timeout_ms) {
  /* Build command frame: header + length(2) + data + tail */
  uint8_t frame[64];
  size_t frame_len = 0;

  /* Header */
  memcpy(frame, CMD_HEADER, 4);
  frame_len = 4;

  /* Length (2 bytes, little endian) - length of cmd_data only */
  uint16_t data_len = cmd_len;
  frame[frame_len++] = data_len & 0xFF;
  frame[frame_len++] = (data_len >> 8) & 0xFF;

  /* Command data */
  memcpy(frame + frame_len, cmd_data, cmd_len);
  frame_len += cmd_len;

  /* Tail */
  memcpy(frame + frame_len, CMD_TAIL, 4);
  frame_len += 4;

  /* Clear receive buffer first */
  uart_flush_input(MMWAVE_UART_NUM);

  /* Send command */
  uart_write_bytes(MMWAVE_UART_NUM, frame, frame_len);
  ESP_LOGD(TAG, "Sent command (%d bytes)", frame_len);

  /* Wait for response with same header/tail format */
  int64_t start = esp_timer_get_time() / 1000;
  size_t rx_pos = 0;

  while ((esp_timer_get_time() / 1000 - start) < timeout_ms) {
    int read = uart_read_bytes(MMWAVE_UART_NUM, response + rx_pos,
                               max_resp_len - rx_pos, pdMS_TO_TICKS(50));
    if (read > 0) {
      rx_pos += read;
      /* Check for complete response (header + length(2) + data + tail) */
      if (rx_pos >= 8) {
        /* Verify header */
        if (response[0] == 0xFD && response[1] == 0xFC && response[2] == 0xFB &&
            response[3] == 0xFA) {
          uint16_t resp_data_len = response[4] | (response[5] << 8);
          size_t expected_len = 4 + 2 + resp_data_len + 4;
          if (rx_pos >= expected_len) {
            /* Verify tail */
            if (response[expected_len - 4] == 0x04 &&
                response[expected_len - 3] == 0x03 &&
                response[expected_len - 2] == 0x02 &&
                response[expected_len - 1] == 0x01) {
              *resp_len = expected_len;
              return ESP_OK;
            }
          }
        }
      }
    }
  }

  return ESP_ERR_TIMEOUT;
}

esp_err_t mmwave_enter_config_mode(bool enter) {
  uint8_t cmd[] = {enter ? 0xFF : 0xFE, 0x00};
  uint8_t response[32];
  size_t resp_len = 0;

  esp_err_t ret = send_command(cmd, sizeof(cmd), response, &resp_len,
                               sizeof(response), 500);
  if (ret == ESP_OK && resp_len >= 10) {
    /* Response status at byte 8 */
    if (response[8] == 0x00) {
      ESP_LOGI(TAG, "%s config mode", enter ? "Entered" : "Exited");
      return ESP_OK;
    }
    ESP_LOGW(TAG, "Config mode command failed, status=0x%02X", response[8]);
    return ESP_ERR_INVALID_RESPONSE;
  }
  ESP_LOGW(TAG, "Config mode no response, ret=%s", esp_err_to_name(ret));
  return ret;
}

esp_err_t mmwave_enable_engineering_mode(bool enable) {
  uint8_t cmd[] = {0x62, 0x00, enable ? 0x01 : 0x00, 0x00};
  uint8_t response[32];
  size_t resp_len = 0;

  esp_err_t ret = send_command(cmd, sizeof(cmd), response, &resp_len,
                               sizeof(response), 500);
  if (ret == ESP_OK && resp_len >= 12) {
    if (response[8] == 0x00) {
      ESP_LOGI(TAG, "Engineering mode %s", enable ? "enabled" : "disabled");
      return ESP_OK;
    }
    ESP_LOGW(TAG, "Engineering mode failed, status=0x%02X", response[8]);
    return ESP_ERR_INVALID_RESPONSE;
  }
  return ret;
}

esp_err_t mmwave_set_gate_sensitivity(int gate, uint8_t moving_threshold,
                                      uint8_t static_threshold) {
  uint8_t cmd[5];
  cmd[0] = 0x64;
  cmd[1] = 0x00;
  cmd[2] = gate & 0xFF;
  cmd[3] = moving_threshold;
  cmd[4] = static_threshold;

  uint8_t response[32];
  size_t resp_len = 0;

  esp_err_t ret = send_command(cmd, sizeof(cmd), response, &resp_len,
                               sizeof(response), 500);
  if (ret == ESP_OK && resp_len >= 12) {
    if (response[8] == 0x00) {
      ESP_LOGI(TAG, "Gate %d sensitivity set: moving=%d, static=%d", gate,
               moving_threshold, static_threshold);
      return ESP_OK;
    }
    ESP_LOGW(TAG, "Set gate sensitivity failed, status=0x%02X", response[8]);
    return ESP_ERR_INVALID_RESPONSE;
  }
  return ret;
}

esp_err_t mmwave_set_max_distance(uint8_t max_gate, uint8_t moving_threshold,
                                  uint8_t static_threshold) {
  uint8_t cmd[5];
  cmd[0] = 0x60;
  cmd[1] = 0x00;
  cmd[2] = max_gate;
  cmd[3] = moving_threshold;
  cmd[4] = static_threshold;

  uint8_t response[32];
  size_t resp_len = 0;

  esp_err_t ret = send_command(cmd, sizeof(cmd), response, &resp_len,
                               sizeof(response), 500);
  if (ret == ESP_OK && resp_len >= 12) {
    if (response[8] == 0x00) {
      ESP_LOGI(TAG, "Max distance set: gate=%d, moving=%d, static=%d", max_gate,
               moving_threshold, static_threshold);
      return ESP_OK;
    }
    ESP_LOGW(TAG, "Set max distance failed, status=0x%02X", response[8]);
    return ESP_ERR_INVALID_RESPONSE;
  }
  return ret;
}

esp_err_t mmwave_init(void) {
  uart_config_t uart_config = {
      .baud_rate = MMWAVE_BAUD_RATE,
      .data_bits = UART_DATA_8_BITS,
      .parity = UART_PARITY_DISABLE,
      .stop_bits = UART_STOP_BITS_1,
      .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
      .source_clk = UART_SCLK_DEFAULT,
  };

  esp_err_t ret =
      uart_driver_install(MMWAVE_UART_NUM, MMWAVE_BUF_SIZE * 2, 0, 0, NULL, 0);
  if (ret != ESP_OK) {
    ESP_LOGE(TAG, "UART driver install failed: %s", esp_err_to_name(ret));
    return ret;
  }

  ret = uart_param_config(MMWAVE_UART_NUM, &uart_config);
  if (ret != ESP_OK) {
    ESP_LOGE(TAG, "UART param config failed: %s", esp_err_to_name(ret));
    uart_driver_delete(MMWAVE_UART_NUM);
    return ret;
  }

  ret = uart_set_pin(MMWAVE_UART_NUM, MMWAVE_TX_PIN, MMWAVE_RX_PIN,
                     UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE);
  if (ret != ESP_OK) {
    ESP_LOGE(TAG, "UART set pin failed: %s", esp_err_to_name(ret));
    uart_driver_delete(MMWAVE_UART_NUM);
    return ret;
  }

  initialized = true;
  ESP_LOGI(TAG, "mmWave sensor initialized on UART%d (TX=%d, RX=%d) @ %d baud",
           MMWAVE_UART_NUM, MMWAVE_TX_PIN, MMWAVE_RX_PIN, MMWAVE_BAUD_RATE);

  /* Wait for sensor to stabilize - it outputs continuous data immediately */
  vTaskDelay(pdMS_TO_TICKS(500));

  /* Clear any buffered data before sending commands */
  uart_flush_input(MMWAVE_UART_NUM);
  vTaskDelay(pdMS_TO_TICKS(100));

  /* Enter configuration mode before sending parameter commands */
  ret = mmwave_enter_config_mode(true);
  if (ret != ESP_OK) {
    ESP_LOGW(TAG, "Failed to enter config mode (continuing anyway)");
  } else {
    /* Wait after entering config mode */
    vTaskDelay(pdMS_TO_TICKS(100));

    /* Enable engineering mode for detailed data */
    ret = mmwave_enable_engineering_mode(true);
    if (ret != ESP_OK) {
      ESP_LOGW(TAG, "Failed to enable engineering mode (continuing anyway)");
    }

    /* Exit configuration mode - skip max distance and gate sensitivity for now
     * TODO: These commands need correct format for LD2410C
     */
    mmwave_enter_config_mode(false);
  }

  ESP_LOGI(TAG, "Sensor configuration complete");
  return ESP_OK;
}

static bool find_frame_header(const uint8_t *data, size_t len, size_t *pos) {
  for (size_t i = 0; i + 3 < len; i++) {
    if (data[i] == 0xF4 && data[i + 1] == 0xF3 && data[i + 2] == 0xF2 &&
        data[i + 3] == 0xF1) {
      *pos = i;
      return true;
    }
  }
  return false;
}

static bool validate_frame_tail(const uint8_t *data, size_t offset) {
  return (data[offset] == 0xF8 && data[offset + 1] == 0xF7 &&
          data[offset + 2] == 0xF6 && data[offset + 3] == 0xF5);
}

esp_err_t mmwave_read_data(mmwave_data_t *data) {
  if (!initialized) {
    ESP_LOGW(TAG, "Sensor not initialized");
    return ESP_ERR_INVALID_STATE;
  }

  memset(data, 0, sizeof(mmwave_data_t));

  /* Read incoming data from continuous stream */
  uint8_t temp_buf[256];
  int read_len = uart_read_bytes(MMWAVE_UART_NUM, temp_buf, sizeof(temp_buf),
                                 pdMS_TO_TICKS(100));

  if (read_len > 0) {
    /* Append to buffer if space available */
    if (rx_len + read_len < sizeof(rx_buffer)) {
      memcpy(rx_buffer + rx_len, temp_buf, read_len);
      rx_len += read_len;
    } else {
      /* Buffer full, reset */
      rx_len = 0;
      memcpy(rx_buffer, temp_buf, read_len);
      rx_len = read_len;
    }
  }

  /* Look for complete frame in buffer */
  size_t header_pos;
  if (!find_frame_header(rx_buffer, rx_len, &header_pos)) {
    /* No header found, keep last few bytes in case of partial header */
    if (rx_len > 4) {
      memmove(rx_buffer, rx_buffer + rx_len - 4, 4);
      rx_len = 4;
    }
    return ESP_ERR_TIMEOUT;
  }

  /* Move data to start of buffer if header not at beginning */
  if (header_pos > 0) {
    memmove(rx_buffer, rx_buffer + header_pos, rx_len - header_pos);
    rx_len -= header_pos;
  }

  /* Need at least header(4) + length(2) + minimal payload + tail(4) = 12 bytes
   */
  if (rx_len < 12) {
    return ESP_ERR_TIMEOUT;
  }

  /* Get payload length (little endian, bytes 4-5) */
  uint16_t payload_len = rx_buffer[4] | (rx_buffer[5] << 8);

  /* Total frame size: header(4) + length(2) + payload + tail(4) */
  size_t frame_size = 4 + 2 + payload_len + 4;

  if (rx_len < frame_size) {
    /* Incomplete frame, wait for more data */
    return ESP_ERR_TIMEOUT;
  }

  /* Validate tail */
  if (!validate_frame_tail(rx_buffer, frame_size - 4)) {
    ESP_LOGW(TAG, "Invalid frame tail, discarding");
    /* Discard header and try again */
    memmove(rx_buffer, rx_buffer + 4, rx_len - 4);
    rx_len -= 4;
    return ESP_ERR_INVALID_RESPONSE;
  }

  /* Parse payload starting at byte 6 */
  const uint8_t *payload = rx_buffer + 6;

  /*
   * LD2410C Data Frame Payload Structure:
   * Byte 0: Data type (0x01 = Engineering mode, 0x02 = Basic info)
   * Byte 1: Head (0xAA)
   * Byte 2: Data type (0x01 again)
   * Byte 3-4: Inner data length (little endian)
   *
   * For Basic Info (type 0x02):
   *   Byte 5: Target status (0x00=none, 0x01=moving, 0x02=static, 0x03=both)
   *   Byte 6-7: Moving distance (little endian, cm)
   *   Byte 8: Moving energy (0-100)
   *   Byte 9-10: Static distance (little endian, cm)
   *   Byte 11: Static energy (0-100)
   *   Byte 12: Detection distance (cm)
   *
   * Engineering mode (type 0x01) has additional gate energy data.
   */

  uint8_t data_type = payload[0];

  /* Look for 0xAA marker */
  int data_start = -1;
  for (int i = 0; i < (int)payload_len - 10; i++) {
    if (payload[i] == 0xAA) {
      data_start = i;
      break;
    }
  }

  if (data_start < 0) {
    ESP_LOGD(TAG, "No 0xAA marker found in payload");
    /* Discard this frame */
    memmove(rx_buffer, rx_buffer + frame_size, rx_len - frame_size);
    rx_len -= frame_size;
    return ESP_ERR_INVALID_RESPONSE;
  }

  const uint8_t *target_data = payload + data_start;

  /* Basic info parsing (data_type 0x02):
   * Offset from 0xAA:
   *   +1: target status (0x00=none, 0x01=moving, 0x02=static, 0x03=both)
   *   +2-3: moving distance (little endian)
   *   +4: moving energy
   *   +5-6: static distance (little endian)
   *   +7: static energy
   *   +8-9: detection distance (little endian)
   */

  int offset = 1; /* Start after 0xAA - read target status directly */

  /* Check if we have enough data */
  if (data_start + offset + 9 > (int)payload_len) {
    ESP_LOGW(TAG, "Payload too short for target data");
    memmove(rx_buffer, rx_buffer + frame_size, rx_len - frame_size);
    rx_len -= frame_size;
    return ESP_ERR_INVALID_RESPONSE;
  }

  /* Parse target status */
  uint8_t target_status = target_data[offset];
  data->state =
      (target_status > 0) ? MMWAVE_STATE_PRESENT : MMWAVE_STATE_ABSENT;
  offset++;

  /* Moving distance (2 bytes, little endian) */
  data->moving_distance = target_data[offset] | (target_data[offset + 1] << 8);
  offset += 2;

  /* Moving energy */
  data->moving_energy = target_data[offset];
  offset++;

  /* Static distance (2 bytes, little endian) */
  data->static_distance = target_data[offset] | (target_data[offset + 1] << 8);
  offset += 2;

  /* Static energy */
  data->static_energy = target_data[offset];
  offset++;

  /* Detection distance (2 bytes, little endian) */
  data->detection_distance =
      target_data[offset] | (target_data[offset + 1] << 8);

  /* Remove processed frame from buffer */
  memmove(rx_buffer, rx_buffer + frame_size, rx_len - frame_size);
  rx_len -= frame_size;

  return ESP_OK;
}

void mmwave_deinit(void) {
  if (initialized) {
    uart_driver_delete(MMWAVE_UART_NUM);
    initialized = false;
    ESP_LOGI(TAG, "mmWave sensor deinitialized");
  }
}
