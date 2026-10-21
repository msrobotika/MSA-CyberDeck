"""Run one fixture-only TLS transfer between two explicitly selected Android devices.

ADB orchestrates installation/tests and copies an invitation, never the test payload.
Invitations remain in app-private storage and are never printed.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import time

APP = "com.msarobotika.cyberdeck"
TEST = APP + ".test/androidx.test.runner.AndroidJUnitRunner"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--receiver", required=True)
    parser.add_argument("--sender", required=True)
    args = parser.parse_args()
    assert args.receiver != args.sender
    root = Path(__file__).resolve().parents[1]
    adb = root / ".toolchain/sdk/platform-tools/adb.exe"
    output = root / ".private/android-validation"
    output.mkdir(parents=True, exist_ok=True)

    def command(serial, *words, **kwargs):
        return subprocess.run([str(adb), "-s", serial, *words], check=True,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=15, **kwargs).stdout

    for serial in (args.receiver, args.sender):
        assert command(serial, "get-state").strip() == b"device", "Selected phone is not authorized"

    def start(serial, role):
        log = (output / f"two-phone-{role}.txt").open("wb")
        proc = subprocess.Popen([str(adb), "-s", serial, "shell", "am", "instrument", "-w", "-r",
                                 "-e", "class", APP + ".CrossPhoneDeviceTest", "-e", "role", role, TEST],
                                stdout=log, stderr=subprocess.STDOUT)
        return proc, log

    command(args.receiver, "exec-out", "run-as", APP, "rm", "-f", "files/validation/two-phone-receiver.json")
    receiver, receiver_log = start(args.receiver, "receiver")
    sender = None
    sender_log = None
    try:
        deadline = time.monotonic() + 30
        invitation = ""
        while time.monotonic() < deadline and receiver.poll() is None:
            try:
                state = json.loads(command(args.receiver, "exec-out", "run-as", APP, "cat",
                                           "files/validation/two-phone-receiver.json"))
                invitation = state.get("invitation", "")
                if invitation and state.get("active"):
                    break
            except (subprocess.CalledProcessError, json.JSONDecodeError):
                pass
            time.sleep(.25)
        assert invitation, "Receiver did not prepare a session; inspect its private test log"
        command(args.sender, "exec-out", "run-as", APP, "mkdir", "-p", "files/validation")
        command(args.sender, "exec-in", "run-as", APP, "dd", "of=files/validation/cross-phone-invite.txt", "bs=1", f"count={len(invitation.encode('utf-8'))}",
                input=invitation.encode("utf-8"))
        sender, sender_log = start(args.sender, "sender")
        sender.wait(timeout=120)
        sender_log.flush()
        sender_text = (output / "two-phone-sender.txt").read_text(encoding="utf-8", errors="replace")
        assert "OK (1 test)" in sender_text and "FAILURES!!!" not in sender_text, "Sender fixture failed; inspect private log"
        receiver.wait(timeout=120)
        receiver_log.close()
        sender_log.close()
        for role in ("receiver", "sender"):
            text = (output / f"two-phone-{role}.txt").read_text(encoding="utf-8", errors="replace")
            assert "OK (1 test)" in text and "FAILURES!!!" not in text, f"{role} fixture failed; inspect private log"
        states = {}
        for serial, role in ((args.receiver, "receiver"), (args.sender, "sender")):
            data = json.loads(command(serial, "exec-out", "run-as", APP, "cat", f"files/validation/two-phone-{role}.json"))
            data.pop("invitation", None)
            states[role] = data
        expected = hashlib.sha256(bytes(i % 251 for i in range(1024 * 1024))).hexdigest()
        assert states["receiver"]["saved"] is True
        assert all(state["expectedSha256"] == expected for state in states.values())
        result = {"bytes": 1024 * 1024, "sha256": expected, "states": states,
                  "transport": "Direct local TLS sockets; no ADB forwarding/reverse or payload copy"}
        (output / "two-phone-result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
        print(json.dumps({"result": "passed", "bytes": result["bytes"], "sha256": expected}))
    finally:
        for serial, proc, log in ((args.receiver, receiver, receiver_log), (args.sender, sender, sender_log)):
            if proc is not None and proc.poll() is None:
                command(serial, "shell", "am", "force-stop", APP)
                proc.terminate()
            if log is not None:
                log.close()


if __name__ == "__main__":
    main()
