"""Local shape gate. No provider calls; reads only this agent's run artifacts."""
import json
import os
from pathlib import Path
import re
import sys

AGENT = "native-usage-judge"
ARTIFACT = "nativeUsageVerdict"


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


KIND = "native"

def validate(verdict, inputs):
    expected = parse_json(inputs["acceptedSentences"])
    if not isinstance(expected,list) or not expected or any(not isinstance(s,str) or not s.strip() for s in expected) or len(set(expected))!=len(expected):
        raise ValueError("invalid accepted variants")
    if KIND == "native":
        closed(verdict,("assessments","rationale")); text(verdict["rationale"])
        rows=verdict["assessments"]
        if not isinstance(rows,list) or len(rows)!=len(expected): raise ValueError("incomplete coverage")
        seen=set(); states={}
        for a in rows:
            closed(a,("sentence","scope","status","intendedMeaning","troublesomeSpan","issue","severity","reason","neededEvidence","registerConcern","writingConcern"))
            s=text(a["sentence"]); text(a["reason"])
            if s not in expected or s in seen: raise ValueError("foreign/duplicate sentence")
            seen.add(s)
            if a["scope"] not in ("SENTENCE","COMMUNICATIVE_ELLIPSIS","LEXICAL_PHRASE","LABEL_OR_FORM"): raise ValueError("unknown scope")
            st=a["status"]
            if st not in ("PASS","FAIL","REVIEW","NOT_APPLICABLE"):raise ValueError("unknown status")
            if st=="FAIL":
                span=text(a["troublesomeSpan"])
                if span not in s or a["issue"] not in ("GRAMMAR","COLLOCATION","WORD_CHOICE","WORD_ORDER","CALQUE") or a["severity"] not in ("HIGH","MEDIUM"):raise ValueError("invalid defect")
            elif a["issue"] is not None or a["severity"] is not None or (st!="REVIEW" and a["troublesomeSpan"] is not None):raise ValueError("non-failure has defect")
            if st=="REVIEW" and a["troublesomeSpan"] is not None:
                if text(a["troublesomeSpan"]) not in s:raise ValueError("review span is not literal")
            if st=="REVIEW":text(a["neededEvidence"])
            elif a["neededEvidence"] is not None:raise ValueError("unexpected evidence request")
            if st in ("PASS","FAIL"):text(a["intendedMeaning"])
            elif a["intendedMeaning"] is not None:text(a["intendedMeaning"])
            if (a["scope"]=="LABEL_OR_FORM") != (st=="NOT_APPLICABLE"):raise ValueError("invalid label scope")
            for k in ("registerConcern","writingConcern"):
                if a[k] is not None:text(a[k])
            key=s.lower().replace("’","'")
            for contraction,full in [("won't","will not"),("can't","cannot"),("don't","do not"),("doesn't","does not"),("didn't","did not"),("haven't","have not"),("hasn't","has not"),("isn't","is not"),("aren't","are not"),("i'm","i am"),("'re"," are"),("'ll"," will")]:key=key.replace(contraction,full)
            if key in states and states[key]!=st:raise ValueError("contradictory equivalent variants")
            states[key]=st
    else:
        closed(verdict,("quizId","status","targetStructure","affectedSentences","issue","reason","neededEvidence","handoffs","proposedAction","proposedExample"))
        if verdict["quizId"]!=inputs["quizId"]:raise ValueError("foreign quiz")
        text(verdict["reason"]); st=verdict["status"]
        if st not in ("SUITABLE","IMPROVE","REWRITE_REQUIRED","REVIEW","NOT_APPLICABLE"):raise ValueError("unknown status")
        if st!="NOT_APPLICABLE":text(verdict["targetStructure"])
        elif verdict["targetStructure"] is not None:text(verdict["targetStructure"])
        for k in ("affectedSentences","handoffs"):
            if not isinstance(verdict[k],list) or any(not isinstance(v,str) for v in verdict[k]) or len(set(verdict[k]))!=len(verdict[k]):raise ValueError("invalid evidence list")
        if not set(verdict["affectedSentences"])<=set(expected):raise ValueError("foreign affected variants")
        if not set(verdict["handoffs"])<={"NATURALNESS","PRAGMATISM","FORMAT","INSTRUCTION_OR_ANSWER","INPUT"}:raise ValueError("unknown handoff")
        if st in ("IMPROVE","REWRITE_REQUIRED"):
            if verdict["issue"] is None and not verdict["handoffs"]:raise ValueError("no issue or owner")
            if verdict["issue"] is not None and verdict["issue"] not in ("TARGET_ALIGNMENT","OPAQUE_EXAMPLE","INCIDENTAL_STRUCTURE","SPECIAL_SENSE_OR_REGISTER","MODIFIER_SCOPE"):raise ValueError("unknown issue")
            text(verdict["proposedAction"])
        elif verdict["issue"] is not None:raise ValueError("unexpected issue")
        if st=="REVIEW":text(verdict["neededEvidence"])
        elif verdict["neededEvidence"] is not None:raise ValueError("unexpected evidence request")
        if st in ("SUITABLE","NOT_APPLICABLE") and any(verdict[k] is not None for k in ("proposedAction","proposedExample")):raise ValueError("approved replacement")
        for k in ("proposedAction","proposedExample"):
            if verdict[k] is not None:text(verdict[k])
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
