"""Local shape gate. No provider calls; reads only this agent's run artifacts."""
import json
import os
from pathlib import Path
import re
import sys

AGENT = "choice-quality-judge"
ARTIFACT = "choiceQualityVerdict"


def run_directory(state):
    bags = [state, state.get("inputs") or {}, state.get("data") or {}, state.get("node") or {}]
    for value in [bag.get("run_dir") for bag in bags] + [os.environ.get("SENTINEL_RUN_DIR")]:
        if isinstance(value, str) and value.strip():
            path = Path(value).expanduser().resolve()
            if not path.is_dir():
                raise ValueError("run_dir explícito inexistente")
            return path
    root = next((bag.get("project_root") or bag.get("root") for bag in bags
                 if bag.get("project_root") or bag.get("root")), None)
    run_id = next((bag.get("run_id") for bag in bags if bag.get("run_id")), None)
    if not isinstance(root, str) or not isinstance(run_id, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", run_id):
        raise ValueError("faltan run_dir o project_root/run_id seguros")
    base = (Path(root).expanduser().resolve() / ".sentinel" / "runs" / AGENT).resolve()
    direct = (base / run_id).resolve()
    if direct.parent != base:
        raise ValueError("run_id fuera del directorio de runs")
    if direct.is_dir():
        return direct
    matches = []
    if base.is_dir():
        for child in base.iterdir():
            if child.resolve().parent != base or not child.is_dir():
                continue
            try:
                if json.loads((child / "state.json").read_text()) .get("runId") == run_id:
                    matches.append(child.resolve())
            except (OSError, ValueError, AttributeError):
                pass
    if len(matches) != 1:
        raise ValueError("run_id no identifica exactamente un run")
    return matches[0]


def closed(value, keys):
    if not isinstance(value, dict) or set(value) != set(keys):
        raise ValueError("objeto incompleto o con claves desconocidas")
    return value


def text(value, allow_empty=False):
    if not isinstance(value, str) or (not allow_empty and not value.strip()):
        raise ValueError("texto vacío o de tipo incorrecto")
    return value


def validate(verdict, inputs):
    closed(verdict, ("status", "objective", "introductory", "issues", "rationale"))
    status = verdict["status"]
    if status not in ("PASS", "FAIL", "NEEDS_CONTEXT"):
        raise ValueError("status desconocido")
    text(verdict["objective"], allow_empty=status == "NEEDS_CONTEXT")
    text(verdict["rationale"])
    if not isinstance(verdict["introductory"], bool) or not isinstance(verdict["issues"], list):
        raise ValueError("introductory/issues con tipo incorrecto")
    if (status == "FAIL") != bool(verdict["issues"]):
        raise ValueError("status e issues incoherentes")
    raw = inputs.get("options")
    if not isinstance(raw, str):
        raise ValueError("faltan opciones para validar labels")
    suffix = " (correcta)"
    labels = [line[2:][:-len(suffix)] if line.endswith(suffix) else line[2:]
              for line in raw.splitlines() if line.startswith("- ")]
    if len(labels) < 2 or sum(line.endswith(suffix) for line in raw.splitlines() if line.startswith("- ")) != 1:
        raise ValueError("opciones sin una única clave")
    identities = set()
    for issue in verdict["issues"]:
        closed(issue, ("kind", "severity", "labels", "reason", "misconception"))
        if issue["kind"] not in ("ANSWER_CUE", "IRRELEVANT_DISTRACTORS", "IMPLAUSIBLE_DISTRACTORS"):
            raise ValueError("kind desconocido")
        if issue["severity"] not in ("HIGH", "MEDIUM", "LOW"):
            raise ValueError("severity desconocida")
        text(issue["reason"])
        text(issue["misconception"])
        implicated = issue["labels"]
        if not isinstance(implicated, list) or not implicated:
            raise ValueError("labels debe ser una lista no vacía")
        for label in implicated:
            text(label)
            if label not in labels:
                raise ValueError("label no existe en el ejercicio: " + label)
        if len(set(implicated)) != len(implicated):
            raise ValueError("label repetida")
        identity = (issue["kind"], tuple(sorted(implicated)))
        if identity in identities:
            raise ValueError("problema duplicado")
        identities.add(identity)
    return verdict


def main():
    run_dir = None
    try:
        state = json.load(sys.stdin)
        inputs = state.get("inputs") or {}
        run_dir = run_directory(state)
        if sys.argv[1] == "gate":
            raw = (run_dir / "verdicts.json").read_text(encoding="utf-8").strip()
            if raw.startswith("```json") and raw.endswith("```"):
                raw = raw[7:-3].strip()
            verdict = validate(json.loads(raw), inputs)
            (run_dir / "validated-verdict.json").write_text(json.dumps(verdict, ensure_ascii=False), encoding="utf-8")
            print(json.dumps({"route": "passed"}))
        else:
            verdict = validate(json.loads((run_dir / "validated-verdict.json").read_text(encoding="utf-8")), inputs)
            print(json.dumps({"route": "done", ARTIFACT: json.dumps(verdict, ensure_ascii=False)}, ensure_ascii=False))
    except (OSError, ValueError, TypeError, KeyError, IndexError, AttributeError) as failure:
        if sys.argv[1] == "gate":
            if run_dir is not None:
                (run_dir / "validated-verdict.json").unlink(missing_ok=True)
            print(json.dumps({"route": "failed", "error": str(failure)}, ensure_ascii=False))
        else:
            print(json.dumps({"route": "done", ARTIFACT: json.dumps({"outputUnusable": True})}))


if __name__ == "__main__":
    main()
