"""One chosen PC serial port, read-only MSA status query. Not an Android OTG test."""
import argparse
import datetime
import json
import pathlib
import time
import serial


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error("Output already exists; choose a new file to preserve prior observations")
    link = serial.Serial(port=None, baudrate=115200, timeout=0.2, write_timeout=2)
    link.port = args.port
    link.dtr = False
    link.rts = False
    events = []

    def record(direction, data):
        events.append({"format": "msrobotika.usb.log.v1", "at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                       "direction": direction, "data": data})

    try:
        link.open()
        command = '{"cmd":"status"}\n'
        link.write(command.encode("ascii"))
        record("TX", command.rstrip())
        end = time.monotonic() + 8
        frame = bytearray()
        discard = False
        while time.monotonic() < end:
            for value in link.read(4096):
                if value == 10:
                    if not discard and frame:
                        text = frame.decode("utf-8", errors="replace").rstrip("\r")
                        record("RX", text)
                        try:
                            obj = json.loads(text)
                        except ValueError:
                            obj = None
                        if isinstance(obj, dict) and (obj.get("result") == "status" or obj.get("cmd") == "STATUS"):
                            print(json.dumps({"transport": "PC serial; OTG not validated", "port": args.port,
                                              "firmware": obj.get("firmware"), "chip_announced": obj.get("chip"),
                                              "response_received": True}))
                            return
                    frame.clear()
                    discard = False
                elif not discard:
                    if len(frame) >= 4096:
                        frame.clear()
                        discard = True
                    else:
                        frame.append(value)
        raise SystemExit("No compatible status reply; original firmware unchanged")
    finally:
        link.close()
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text("".join(json.dumps(row, ensure_ascii=False) + "\n" for row in events), encoding="utf-8")


if __name__ == "__main__":
    main()
