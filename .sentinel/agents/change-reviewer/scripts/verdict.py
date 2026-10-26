"""Local shape gate. No provider calls; reads only this agent's run artifacts."""
import json
import os
from pathlib import Path
import re
import sys

AGENT = "change-reviewer"
ARTIFACT = "changeReviewVerdict"


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


KEYS = ("quizId", "mark", "problems", "reason")
PROBLEM_KEYS = ("where", "fragment", "what")
WHERE = ("INSTRUCTIONS", "EXERCISE", "ANSWERS", "OPTIONS", "TRANSLATION", "MINI_THEORY")


def validate(verdict, inputs):
    closed(verdict, KEYS)
    if verdict["quizId"] != inputs["quizId"]:
        raise ValueError("marca de otro ejercicio")
    text(verdict["reason"])
    if verdict["mark"] not in ("GOOD", "DOUBTFUL", "WORSE"):
        raise ValueError("marca desconocida")
    problems = verdict["problems"]
    if not isinstance(problems, list):
        raise ValueError("problemas inválidos")
    if verdict["mark"] == "GOOD" and problems:
        raise ValueError("una marca GOOD trae problemas")
    if verdict["mark"] != "GOOD" and not problems:
        raise ValueError("una marca DOUBTFUL o WORSE sin problemas")
    # The fragment is copied as it is, from production or from now: it is what José looks for.
    sides = (inputs.get("before") or "") + "\n" + (inputs.get("after") or "")
    for problem in problems:
        closed(problem, PROBLEM_KEYS)
        if problem["where"] not in WHERE:
            raise ValueError("lugar desconocido")
        if text(problem["fragment"]).strip() not in sides:
            raise ValueError("el fragmento no está en ninguno de los dos lados")
        text(problem["what"])
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
