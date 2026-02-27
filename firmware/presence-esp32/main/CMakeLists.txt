idf_component_register(SRCS "presence_main.c"
                              "mmwave_sensor.c"
                              "presence_debounce.c"
                              "jsonl_output.c"
                              "sensor_config.c"
                       PRIV_REQUIRES driver esp_timer nvs_flash
                       INCLUDE_DIRS ".")
