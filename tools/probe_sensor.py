"""Read-only protocol check. Does not flash, reset, or export observed identifiers."""
import json
import time
import serial

s = serial.Serial()
s.port = "COM4"
s.baudrate = 115200
s.timeout = 0.2
s.dtr = False
s.rts = False
try:
    s.open()
    s.write(json.dumps({"cmd": "status"}).encode() + b"\n")
    end = time.monotonic() + 8
    while time.monotonic() < end:
        line = s.readline(16385)
        try:
            obj = json.loads(line)
        except (ValueError, UnicodeDecodeError):
            continue
        if isinstance(obj, dict) and obj.get("result") == "status":
            print(json.dumps({k: obj[k] for k in ("firmware", "running", "channel", "access_points", "uptime") if k in obj}))
            break
    else:
        print("No compatible status response; firmware unchanged.")
finally:
    s.close()
