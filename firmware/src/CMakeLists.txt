idf_component_register(SRCS "main.c" "monitor.c" "display.c" INCLUDE_DIRS "." REQUIRES nvs_flash esp_wifi esp_event esp_netif driver esp_lcd json esp_timer)
