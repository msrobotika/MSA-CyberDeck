#include <Arduino.h>
#include <ArduinoJson.h>

// Source candidate only. Never uploaded automatically. No radio or GPIO outputs.
static String requestLine;
static bool discardFrame = false;
static void respond(String command, uint32_t id = 0, bool hasId = false) {
  command.toUpperCase();
  JsonDocument reply;
  reply["sf"] = 1;
  reply["protocol"] = "json-v1";
  reply["cmd"] = command;
  if (hasId) reply["id"] = id;
  const bool known = command == "HELLO" || command == "STATUS" || command == "CAPABILITIES";
  reply["ok"] = known;
  if (!known) reply["error"] = "UNKNOWN_COMMAND";
  if (known) {
    reply["firmware"] = "SuperFlyper Bridge";
    reply["version"] = "0.2.0";
    reply["chip"] = ESP.getChipModel();
    reply["chip_revision"] = ESP.getChipRevision();
    reply["flash_bytes"] = ESP.getFlashChipSize();
    auto caps = reply["capabilities"].to<JsonArray>();
    caps.add("HELLO"); caps.add("STATUS"); caps.add("CAPABILITIES");
  }
  if (command == "STATUS") {
    reply["uptime_ms"] = millis();
    reply["free_heap"] = ESP.getFreeHeap();
    reply["flash_frequency_hz"] = ESP.getFlashChipSpeed();
    reply["flash_mode_code"] = (unsigned int)ESP.getFlashChipMode();
  }
  serializeJson(reply, Serial); Serial.println();
}
static void parseRequest() {
  if (requestLine.startsWith("SF1 ")) {respond(requestLine.substring(4)); return;}
  JsonDocument request;
  if (deserializeJson(request, requestLine, DeserializationOption::NestingLimit(3)) || !request["cmd"].is<const char*>()) {
    Serial.println(F("{\"sf\":1,\"ok\":false,\"error\":\"INVALID_JSON_REQUEST\"}")); return;
  }
  const bool hasId = !request["id"].isNull();
  if (hasId && !request["id"].is<uint32_t>()) {
    Serial.println(F("{\"sf\":1,\"ok\":false,\"error\":\"INVALID_ID\"}")); return;
  }
  respond(request["cmd"].as<String>(), request["id"] | 0U, hasId);
}
void setup() {Serial.begin(115200); requestLine.reserve(256);}
void loop() {
  while (Serial.available()) {
    const char c = (char)Serial.read();
    if (c == '\r') continue;
    if (c == '\n') {
      if (discardFrame) Serial.println(F("{\"sf\":1,\"ok\":false,\"error\":\"FRAME_TOO_LONG\"}"));
      else if (!requestLine.isEmpty()) parseRequest();
      requestLine = ""; discardFrame = false;
    } else if (!discardFrame) {
      if (requestLine.length() < 256) requestLine += c;
      else discardFrame = true;
    }
  }
  delay(1);
}
