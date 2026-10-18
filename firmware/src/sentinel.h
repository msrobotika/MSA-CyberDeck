#pragma once
#include <stdbool.h>
#include <stdint.h>
#define AP_CAP 32
#define EVENT_CAP 16
#define PAGE_COUNT 5
typedef enum {SEC_OPEN,SEC_WEP,SEC_WPA,SEC_RSN} security_t;
typedef struct {uint8_t bssid[6],channel,security;int8_t rssi;char ssid[33];uint32_t first,last,reports;} ap_t;
typedef struct {uint8_t kind,channel;int8_t rssi;char ssid[33];uint8_t bssid[6];uint32_t when;} sentinel_event_t;
typedef struct {
 bool running,baseline_set;uint8_t channel,selected,count,event_count;char baseline_ssid[33];uint8_t baseline_bssid[6],baseline_security,baseline_channel;
 uint32_t frames,beacons,deauth,disassoc,malformed,dropped,security_mismatches,uptime,heap,last_deauth;
 uint32_t channel_frames[14];ap_t aps[AP_CAP];sentinel_event_t events[EVENT_CAP];
} sentinel_snapshot_t;
void sentinel_start(void);void sentinel_snapshot(sentinel_snapshot_t *out);void sentinel_next(void);bool sentinel_select(unsigned id);bool sentinel_clear(void);bool sentinel_set_running(bool enabled);bool sentinel_baseline_selected(void);bool sentinel_forget_baseline(void);const char *sentinel_security(unsigned value);void sentinel_address(const uint8_t bssid[6],char out[18]);
void serial_blob(const uint8_t *data,unsigned bytes,int width,int height);void screen_start(int profile,bool invert);void screen_page(int page);void screen_profile(int profile,bool invert);bool screen_capture(void);unsigned screen_frames(void);unsigned screen_button_presses(void);unsigned screen_button_holds(void);int screen_button_level(void);int screen_current_page(void);int screen_current_profile(void);bool screen_current_invert(void);
