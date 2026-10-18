"""Inspect a random-UUID BLE/GATT fixture on two explicitly selected owned phones."""
import argparse
import json
from pathlib import Path
import subprocess
import time

APP = "com.msarobotika.cyberdeck"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--peripheral", required=True)
    parser.add_argument("--scanner", required=True)
    args = parser.parse_args()
    assert args.peripheral != args.scanner
    root = Path(__file__).resolve().parents[1]
    adb = root / ".toolchain/sdk/platform-tools/adb.exe"
    folder = root / ".private/android-validation"
    folder.mkdir(parents=True, exist_ok=True)

    def command(serial, *words, **kwargs):
        return subprocess.run([str(adb), "-s", serial, *words], check=True, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE, timeout=15, **kwargs).stdout

    def start(serial, role):
        log = (folder / f"ble-{role}.txt").open("wb")
        proc = subprocess.Popen([str(adb), "-s", serial, "shell", "am", "instrument", "-w", "-r",
                                 "-e", "class", APP + ".BlePairDeviceTest", "-e", "role", role,
                                 APP + ".test/androidx.test.runner.AndroidJUnitRunner"], stdout=log, stderr=subprocess.STDOUT)
        return proc, log

    peripheral = peripheral_log = scanner = scanner_log = None
    try:
        # Normal developer test permissions for this app only; no global setting changes.
        for permission in ("BLUETOOTH_CONNECT", "BLUETOOTH_ADVERTISE"):
            command(args.peripheral, "shell", "pm", "grant", APP, "android.permission." + permission)
        for permission in ("BLUETOOTH_CONNECT", "BLUETOOTH_SCAN"):
            command(args.scanner, "shell", "pm", "grant", APP, "android.permission." + permission)
        command(args.peripheral, "exec-out", "run-as", APP, "rm", "-f", "files/validation/ble-fixture-ready.json")

        peripheral, peripheral_log = start(args.peripheral, "peripheral")
        state = {}
        deadline = time.monotonic() + 25
        while time.monotonic() < deadline and peripheral.poll() is None:
            try:
                state = json.loads(command(args.peripheral, "exec-out", "run-as", APP, "cat", "files/validation/ble-fixture-ready.json"))
                if state.get("ready") or "error" in state:
                    break
            except (subprocess.CalledProcessError, json.JSONDecodeError):
                pass
            time.sleep(.25)
        assert state.get("ready"), "BLE fixture did not start; inspect private log (Bluetooth must be enabled)"
        data = state["uuid"].encode("ascii")
        command(args.scanner, "exec-out", "run-as", APP, "mkdir", "-p", "files/validation")
        command(args.scanner, "exec-in", "run-as", APP, "dd", "of=files/validation/ble-pair-uuid.txt", "bs=1", f"count={len(data)}", input=data)
        scanner, scanner_log = start(args.scanner, "scanner")
        scanner.wait(timeout=45)
        scanner_log.close()
        text = (folder / "ble-scanner.txt").read_text(encoding="utf-8", errors="replace")
        assert "OK (1 test)" in text and "FAILURES!!!" not in text, "BLE scanner fixture failed; inspect private log"
        peripheral.wait(timeout=30)
        peripheral_log.close()
        text = (folder / "ble-peripheral.txt").read_text(encoding="utf-8", errors="replace")
        assert "OK (1 test)" in text and "FAILURES!!!" not in text, "BLE peripheral fixture failed; inspect private log"
        result = command(args.scanner, "exec-out", "run-as", APP, "cat", "files/validation/ble-pair-result.json")
        (folder / "ble-pair-result.json").write_bytes(result)
        print(json.dumps({"result": "passed", "observed": "random-UUID fixture GATT", "characteristicReads": 0, "characteristicWrites": 0}))
    finally:
        for serial, proc, log in ((args.peripheral, peripheral, peripheral_log), (args.scanner, scanner, scanner_log)):
            if proc is not None and proc.poll() is None:
                command(serial, "shell", "am", "force-stop", APP)
                proc.terminate()
            if log is not None:
                log.close()
        # Advertising is only needed by the test fixture, not by the app's inspector.
        command(args.peripheral, "shell", "pm", "revoke", APP, "android.permission.BLUETOOTH_ADVERTISE")


if __name__ == "__main__":
    main()
