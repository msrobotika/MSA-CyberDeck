#include <Arduino.h>

// Read-only SF1 serial protocol over the board's USB-UART.
// No radios, GPIO outputs, Wi-Fi associations or packet transmission.
static String requestLine;
static bool discardFrame = false;

static void respond(const String &command) {
  if (command == "SF1 HELLO") {
    Serial.println(F("{\"sf\":1,\"ok\":true,\"cmd\":\"HELLO\",\"device\":\"ESP32\",\"firmware\":\"SuperFlyper Bridge\",\"version\":\"0.1.0\"}"));
  } else if (command == "SF1 STATUS") {
    Serial.printf("{\"sf\":1,\"ok\":true,\"cmd\":\"STATUS\",\"uptime_ms\":%lu,\"free_heap\":%u,\"flash_bytes\":%u}\n",
      (unsigned long)millis(), (unsigned int)ESP.getFreeHeap(), (unsigned int)ESP.getFlashChipSize());
  } else if (command == "SF1 CAPABILITIES") {
    Serial.println(F("{\"sf\":1,\"ok\":true,\"cmd\":\"CAPABILITIES\",\"capabilities\":[\"HELLO\",\"STATUS\",\"CAPABILITIES\"]}"));
  } else {
    Serial.println(F("{\"sf\":1,\"ok\":false,\"error\":\"UNKNOWN_COMMAND\"}"));
  }
}

void setup() {
  Serial.begin(115200);
  requestLine.reserve(64);
}

void loop() {
  while (Serial.available()) {
    const char c = (char)Serial.read();
    if (c == '\r') continue;
    if (c == '\n') {
      if (discardFrame) {
        Serial.println(F("{\"sf\":1,\"ok\":false,\"error\":\"FRAME_TOO_LONG\"}"));
      } else if (!requestLine.isEmpty()) {
        respond(requestLine);
      }
      requestLine = "";
      discardFrame = false;
    } else if (!discardFrame) {
      if (requestLine.length() < 64) requestLine += c;
      else discardFrame = true;
    }
  }
}
