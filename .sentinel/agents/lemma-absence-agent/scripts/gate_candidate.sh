#!/bin/bash
# Normaliza + valida la FORMA del candidato del react (antes de evaluarlo).
# - strippea ```fences``` y extrae el primer objeto JSON balanceado.
# - exige {quizSentence, translation} no vacíos, sin `[[...]]`, sin schemas ajenos.
# - si pasa: reescribe candidate.json en forma canónica -> route passed.
# - si no: route failed -> route_verdict/back-edge reintenta generate.
# Solo stdlib. Debug a stderr; UNA línea JSON a stdout.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, re, sys, os

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

cand_path = os.path.join(run_dir, "candidate.json")

def fail(msg):
    sys.stderr.write("gate_candidate: " + msg + "\n")
    print(json.dumps({"route": "failed"}))
    sys.exit(0)

try:
    with open(cand_path, "r", encoding="utf-8") as f:
        raw = f.read()
except Exception as e:
    fail("candidate.json unreadable: " + str(e))

text = raw.strip()
# Strip a leading ```json / ``` fence and trailing ``` if present.
if text.startswith("```"):
    text = re.sub(r"^```[a-zA-Z0-9]*\s*", "", text)
    text = re.sub(r"\s*```$", "", text).strip()

# Extract the first balanced {...} object (tolerate prose around it).
obj = None
start = text.find("{")
if start != -1:
    depth = 0
    for i in range(start, len(text)):
        c = text[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                frag = text[start:i+1]
                try:
                    obj = json.loads(frag)
                except Exception:
                    obj = None
                break

if not isinstance(obj, dict):
    fail("no JSON object found in candidate")

qs = obj.get("quizSentence")
tr = obj.get("translation")

if not isinstance(qs, str) or not qs.strip():
    fail("missing/empty quizSentence")
if not isinstance(tr, str) or not tr.strip():
    fail("missing/empty translation")
# Reject the [[...]] double-bracket marker (wrong DSL).
if "[[" in qs:
    fail("quizSentence uses forbidden [[...]] double brackets")
# Reject foreign schemas the model sometimes invents.
foreign = {"exercise", "gaps", "answerKey", "passage", "options"}
if foreign.intersection(obj.keys()):
    fail("foreign schema keys present: " + ",".join(sorted(foreign.intersection(obj.keys()))))
# Must contain at least one single-bracket answer slot `[...]`.
if "[" not in qs or "]" not in qs:
    fail("quizSentence has no [answer] bracket")

# Canonicalize: rewrite candidate.json with exactly the two fields.
canonical = {"quizSentence": qs, "translation": tr}
with open(cand_path, "w", encoding="utf-8") as f:
    json.dump(canonical, f, ensure_ascii=False)

print(json.dumps({"route": "passed"}))
PY
