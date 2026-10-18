"""Local shape gate. No provider calls; reads only this agent's run artifacts."""
import json
import os
from pathlib import Path
import re
import sys

AGENT = "pragmatism-judge"
ARTIFACT = "pragmatismVerdict"


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


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("clave JSON repetida")
        result[key] = value
    return result


def parse_json(raw):
    return json.loads(raw, object_pairs_hook=unique_object)


def validate(verdict, inputs):
    closed(verdict, ("assessments", "rationale"))
    text(verdict["rationale"])
    expected = parse_json(inputs["acceptedSentences"])
    if not isinstance(expected, list) or not expected or any(not isinstance(s, str) or not s.strip() for s in expected):
        raise ValueError("faltan oraciones terminadas")
    if len(set(expected)) != len(expected):
        raise ValueError("entrada duplicada")
    assessments = verdict["assessments"]
    if not isinstance(assessments, list) or len(assessments) != len(expected):
        raise ValueError("evaluaciones incompletas")
    seen = set()
    for a in assessments:
        closed(a, ("sentence", "status", "communicativePurpose", "plausibleUse", "issue", "severity", "reason", "neededContext"))
        sentence = text(a["sentence"])
        if sentence not in expected or sentence in seen:
            raise ValueError("oración ajena o repetida")
        seen.add(sentence)
        status = a["status"]
        if status not in ("PASS", "FAIL", "NEEDS_CONTEXT", "NOT_APPLICABLE"):
            raise ValueError("estado desconocido")
        text(a["communicativePurpose"], allow_empty=status in ("NEEDS_CONTEXT", "NOT_APPLICABLE"))
        text(a["reason"])
        if status == "PASS":
            text(a["plausibleUse"])
        elif a["plausibleUse"] is not None:
            raise ValueError("sólo PASS declara uso plausible")
        if status == "FAIL":
            if a["issue"] not in ("INCOHERENT_SCENARIO", "UNLICENSED_SCENARIO", "DISCOURSE_MISMATCH") or a["severity"] not in ("HIGH", "MEDIUM"):
                raise ValueError("problema o gravedad desconocidos")
        elif a["issue"] is not None or a["severity"] is not None:
            raise ValueError("problema incompatible con el estado")
        if status == "NEEDS_CONTEXT":
            text(a["neededContext"])
        elif a["neededContext"] is not None:
            raise ValueError("contexto incompatible con el estado")
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
            verdict = validate(parse_json(raw), inputs)
            (run_dir / "validated-verdict.json").write_text(json.dumps(verdict, ensure_ascii=False), encoding="utf-8")
            print(json.dumps({"route": "passed"}))
        else:
            verdict = validate(parse_json((run_dir / "validated-verdict.json").read_text(encoding="utf-8")), inputs)
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
