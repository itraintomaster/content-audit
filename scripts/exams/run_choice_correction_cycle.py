#!/usr/bin/env python3
"""Bounded synthetic integration experiment. Default actions never invoke a model."""
import argparse
import concurrent.futures
import fcntl
import json
import os
from pathlib import Path
import subprocess
import tempfile
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[2]
EXPERIMENT = ROOT / "analysis/judge-exams/2026-10-09-choice-correction-cycle-v2"
JDK = Path("/Users/josecullen/Library/Java/JavaVirtualMachines/openjdk-25/Contents/Home")
CODEX = Path("/Applications/ChatGPT.app/Contents/Resources/codex-cli/CodexCLI.app/Contents/MacOS/codex")


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w") as out:
        json.dump(value, out, indent=2, ensure_ascii=False)
        out.write("\n")
        out.flush()
        os.fsync(out.fileno())


def physical_count(directory):
    log = directory / "physical-calls.jsonl"
    return sum(bool(line.strip()) for line in log.read_text().splitlines()) if log.exists() else 0


def validate_auth(directory):
    auth = json.loads((directory / "authorization.json").read_text())
    if (auth.get("experimentId") != "choice-correction-cycle-20261009-v2"
            or auth.get("userReply") != "sí" or auth.get("physicalCallCap") != 50
            or auth.get("model") != "gpt-6.1-sol" or auth.get("provider") != "codex-cli"
            or auth.get("reasoningEffort") != "medium"):
        raise ValueError("No hay autorización para este experimento y este tope")
    if physical_count(directory) >= 50:
        raise ValueError("Tope agotado; ninguna recuperación restaura saldo")
    return auth


def compile_harness():
    module_classes = sorted(ROOT.glob("*/target/classes"))
    runtime = (ROOT / "audit-cli/target/runtime-classpath.txt").read_text().strip()
    classpath = ":".join([str(p) for p in module_classes] + [runtime])
    output = ROOT / "audit-cli/target/cycle-classes"
    output.mkdir(parents=True, exist_ok=True)
    subprocess.run([str(JDK / "bin/javac"), "-cp", classpath, "-d", str(output),
                    str(ROOT / "scripts/exams/ChoiceCorrectionCycle.java")], cwd=ROOT, check=True)
    return [str(JDK / "bin/java"), "-cp", str(output) + ":" + classpath, "ChoiceCorrectionCycle"]


def runtime_env(directory, case):
    bindir = directory / "bin"
    bindir.mkdir(exist_ok=True)
    shim = bindir / "codex"
    expected = ROOT / "scripts/exams/capped-codex"
    if shim.is_symlink() and shim.resolve() != expected:
        raise ValueError("Runtime distinto")
    if not shim.exists():
        shim.symlink_to(expected)
    if shim.resolve() != expected:
        raise ValueError("No es el runtime con contador durable")
    return dict(os.environ, PATH=str(bindir) + ":" + os.environ["PATH"],
                CONTENT_AUDIT_CODEX_REAL_BINARY=str(CODEX),
                CONTENT_AUDIT_EXAM_RUNTIME_WRAPPER=str(ROOT / "scripts/codex-runtime/codex"),
                CONTENT_AUDIT_EXAM_CAP_DIR=str(directory), CONTENT_AUDIT_EXAM_PHYSICAL_CAP="50",
                CONTENT_AUDIT_REASONING_EFFORT="medium",
                CONTENT_AUDIT_EXAM_EXPERIMENT_ID="choice-correction-cycle-20261009-v2",
                CONTENT_AUDIT_EXAM_CASE_ID=str(case))


def summary(directory):
    corpus = json.loads((directory / "cases.json").read_text())
    rows = []
    for case in corpus["cases"]:
        result = directory / f'case-{case["caseNumber"]:02}/result.json'
        rows.append(json.loads(result.read_text()) if result.exists()
                    else {"caseNumber": case["caseNumber"], "result": "NOT_CONSOLIDATED"})
    compatible = 0
    for row, case in zip(rows, corpus["cases"]):
        initial = row.get("initial", {})
        kinds = {issue["kind"] for issue in initial.get("issues", [])}
        if initial.get("status") == case["expected"] and (
                (case["expected"] == "PASS" and not kinds)
                or (case["expected"] == "FAIL" and kinds and kinds <= set(case["allowedIssueKinds"]))):
            compatible += 1
    result = {"experimentId": corpus["experimentId"], "physicalCalls": physical_count(directory),
              "physicalCallCap": 50, "consolidatedCases": sum(r["result"] != "NOT_CONSOLIDATED" for r in rows),
              "initialStatusMatches": sum(r.get("initialStatusMatches", False) for r in rows),
              "initialCompatibleCases": compatible,
              "controlsPreserved": sum(r["result"] == "CONTROL_PRESERVED" for r in rows),
              "automaticCyclePasses": sum(r.get("automaticCyclePass", False) for r in rows),
              "manualContentReviewRequired": True, "cases": rows}
    save(directory / "result.json", result)
    print(json.dumps({k: v for k, v in result.items() if k != "cases"}, ensure_ascii=False), flush=True)
    return result


def run(java, numbers, jobs):
    validate_auth(EXPERIMENT)
    # One runner per experiment; each Java worker also owns a per-case lock.
    with (EXPERIMENT / "runner.lock").open("a+") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        save(EXPERIMENT / "runner.json", {"pid": os.getpid(), "startedAt": datetime.now(timezone.utc).isoformat(),
                                          "cases": numbers, "jobs": jobs, "physicalCallsAtStart": physical_count(EXPERIMENT)})
        def one(number):
            destination = EXPERIMENT / f"case-{number:02}"
            destination.mkdir(exist_ok=True)
            # Append logs on recovery, never discard the previous failure evidence.
            with (destination / "runtime.log").open("a") as log:
                result = subprocess.run(java + ["case", str(ROOT), str(EXPERIMENT), str(number)],
                                        cwd=ROOT, env=runtime_env(EXPERIMENT, number), stdout=log, stderr=subprocess.STDOUT)
            print(f"Caso {number}: exit={result.returncode}; llamadas físicas acumuladas={physical_count(EXPERIMENT)}", flush=True)
            return result.returncode
        with concurrent.futures.ThreadPoolExecutor(max_workers=jobs) as pool:
            # Exceptions and failed workers are visible; no automatic relaunch is scheduled.
            codes = list(pool.map(one, numbers))
        result = summary(EXPERIMENT)
        save(EXPERIMENT / "runner-finished.json", {"pid": os.getpid(), "finishedAt": datetime.now(timezone.utc).isoformat(),
                                                   "exitCodes": codes, "physicalCalls": result["physicalCalls"]})
        if any(codes):
            raise SystemExit("Etapa no consolidada; preservada sin reintento automático")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=["preflight", "selftest", "run", "summary"])
    parser.add_argument("--cases", type=int, nargs="+", default=list(range(1, 17)))
    parser.add_argument("--jobs", type=int, default=4, choices=[1, 2, 3, 4])
    args = parser.parse_args()
    if len(set(args.cases)) != len(args.cases) or any(n not in range(1, 17) for n in args.cases):
        parser.error("Casos deben ser únicos, del 1 al 16")
    if args.mode == "summary":
        summary(EXPERIMENT)
        return
    java = compile_harness()
    if args.mode == "run":
        run(java, args.cases, args.jobs)
    elif args.mode == "preflight":
        if physical_count(EXPERIMENT):
            raise SystemExit("No se vuelve a congelar un experimento con gasto")
        subprocess.run(java + ["preflight", str(ROOT), str(EXPERIMENT)], cwd=ROOT, check=True)
    else:
        # Fake evaluator writes only a temporary ledger, never contaminates the real experiment.
        with tempfile.TemporaryDirectory(prefix="contentaudit-cycle-offline-") as temporary:
            directory = Path(temporary)
            save(directory / "cases.json", json.loads((EXPERIMENT / "cases.json").read_text()))
            subprocess.run(java + ["selftest", str(ROOT), str(directory)], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
