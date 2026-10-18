#!/usr/bin/env python3
"""Optional per-recovery serialization and spacing; never retries a model."""
import argparse
import fcntl
import json
import math
import signal
import subprocess
import time
from pathlib import Path


def configured_gap(directory, fallback):
    """Read only this recovery's optional cadence; never change unrelated jobs."""
    configuration = directory / "cadence.json"
    if not configuration.exists():
        return fallback
    value = json.loads(configuration.read_text())
    gap = value.get("minimumGapSeconds") if isinstance(value, dict) else None
    if not isinstance(value, dict) or value.get("version") != 1 or type(gap) is not int or gap not in {5, 15, 30}:
        raise ValueError("La pausa de recuperación debe ser de 5, 15 o 30 segundos.")
    return gap


def run(command, directory, gap):
    if not command or not math.isfinite(gap) or gap < 0 or gap > 60:
        raise ValueError("Cadencia inválida: indicá una pausa de 0 a 60 segundos y un programa.")
    directory.mkdir(parents=True, exist_ok=True)
    child = None

    def stop(signum, _frame):
        if child and child.poll() is None:
            child.terminate()
            try:
                child.wait(timeout=5)
            except subprocess.TimeoutExpired:
                child.kill()
                child.wait()
        raise SystemExit(128 + signum)

    previous = signal.signal(signal.SIGTERM, stop)
    try:
        with (directory / "pace.lock").open("a+") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX)
            gap = configured_gap(directory, gap)
            lock.seek(0)
            value = lock.read().strip()
            last_finished = float(value) if value else 0
            if not math.isfinite(last_finished):
                raise ValueError("La marca de cadencia no es válida; revisala antes de continuar.")
            time.sleep(max(0, min(gap, last_finished + gap - time.time())))
            try:
                child = subprocess.Popen(command)
                return child.wait()
            finally:
                lock.seek(0); lock.truncate()
                lock.write(str(time.time())); lock.flush()
    finally:
        signal.signal(signal.SIGTERM, previous)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--minimum-gap", type=float, required=True)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ["--"] else args.command
    try:
        return run(command, args.directory, args.minimum_gap)
    except (ValueError, OSError) as error:
        parser.exit(64, str(error) + "\n")


if __name__ == "__main__":
    raise SystemExit(main())
