"""Local hardware validation; preserves monitor state and never flashes/NVS-writes."""
import json
import time
from pathlib import Path
import serial
from PIL import Image

class Sensor:
    def __init__(self):
        self.s = serial.Serial()
        self.s.port = "COM4"
        self.s.baudrate = 115200
        self.s.timeout = 0.2
        self.s.dtr = False
        self.s.rts = False
        self.s.open()
        self.s.reset_input_buffer()

    def message(self, timeout=10):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            line = self.s.readline(16385)
            if not line.endswith(b"\n"):
                continue
            try:
                obj = json.loads(line)
                if isinstance(obj, dict):
                    return obj
            except (ValueError, UnicodeDecodeError):
                continue
        raise TimeoutError("No protocol response")

    def command(self, cmd, **kwargs):
        self.s.write(json.dumps({"cmd": cmd, **kwargs}).encode() + b"\n")
        return self.message()

    def capture(self, target):
        header = self.command("capture")
        w, h, n = (header.get(k, 0) for k in ("width", "height", "bytes"))
        if header.get("result") != "capture_begin" or not (0 < w <= 320 and 0 < h <= 320 and n == w*h*2 and n <= 108800):
            raise ValueError("Invalid capture geometry")
        data = bytearray()
        end = time.monotonic() + 20
        while len(data) < n and time.monotonic() < end:
            data.extend(self.s.read(min(4096, n - len(data))))
        if len(data) != n or self.message().get("result") != "capture_end":
            raise ValueError("Incomplete framebuffer")
        rgb = bytearray()
        for i in range(0, n, 2):
            p = data[i] << 8 | data[i+1]
            rgb.extend((((p >> 11) & 31)*255//31, ((p >> 5) & 63)*255//63, (p & 31)*255//31))
        Image.frombytes("RGB", (w,h), bytes(rgb)).save(target)

out = Path(".private/sensor-validation")
out.mkdir(parents=True, exist_ok=True)
device = Sensor()
initial = None
try:
    initial = device.command("status")
    assert initial.get("firmware") == "MSA-WiFi-Sentinel-0.2-CyberDeck", "Unexpected firmware"
    assert initial.get("passive") is True and initial.get("injection_enabled") is False
    time.sleep(2)
    assert device.command("aps").get("result") == "aps_begin"
    aps = []
    while True:
        obj = device.message()
        if obj.get("result") == "aps_end":
            break
        assert obj.get("record") == "ap" and len(aps) < 32
        aps.append(obj)
    (out / "networks.json").write_text(json.dumps(aps, ensure_ascii=False, indent=2), encoding="utf-8")
    assert device.command("monitor", enabled=False).get("result") == "monitor_changed"
    first = device.command("status")
    time.sleep(2)
    second = device.command("status")
    assert second["running"] is False and first["frames"] == second["frames"]
    assert device.command("monitor", enabled=True).get("result") == "monitor_changed"
    time.sleep(3)
    resumed = device.command("status")
    assert resumed["running"] is True and resumed["frames"] > second["frames"]
    for i, name in enumerate(("radar", "networks", "fingerprint", "alerts", "evidence")):
        assert device.command("page", index=i).get("result") == "page_set"
        time.sleep(0.35)
        device.capture(out / (name + ".png"))
    result = {
        "firmware": resumed["firmware"], "networks": len(aps),
        "frames": resumed["frames"], "pause_resume": True,
        "captures": "5 framebuffer PNGs, 320x170", "passive": resumed["passive"],
        "injection_enabled": resumed["injection_enabled"],
        "button_presses": resumed["button_presses"],
        "android_otg_tested": False,
    }
    (out / "result.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps(result))
finally:
    if initial:
        try:
            device.command("monitor", enabled=initial.get("running", True))
            device.command("page", index=initial.get("page", 0))
        except Exception:
            pass
    device.s.close()
