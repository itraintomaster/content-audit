#!/usr/bin/env python3
"""Pruebas sin modelo de los scripts de este juez: gate_verdict y emit.

Nunca consulta un modelo ni corre el grafo de agentes. Hace con cada salida
grabada de evaluate (eval/fixtures.json) lo mismo que el framework de Sentinel:
la escribe en {run_dir}/verdicts.json, corre `bash ../scripts/gate_verdict.sh`
desde nodes/ con el estado del framework por stdin (inputs, data, node, run_id)
y después, igual, `bash ../scripts/emit.sh`. Verifica:

- que el gate acepte la salida válida y la canonice: validated-verdict.json
  igual a lo esperado y conforme a schema/verdict.schema.json;
- que rechace la malformada y la que viola invariantes, con un error que dice
  por qué (es lo que el modelo lee al reintentar), sin dejar un
  validated-verdict.json viejo;
- que emit produzca el artifact canónico o, sin veredicto validado,
  {"outputUnusable": true};
- que el run_dir se resuelva como lo proyecta el runner in-process de
  content-audit (run_dir en inputs) y como el CLI (project_root + el run_id del
  state.json), y que se rechace un run_id ambiguo o inseguro;
- que agent.yaml, los nodos y la guía sigan diciendo lo que espera el evaluador
  de Java: dependencias en su orden, artifact, aristas y placeholders.

La parte sin modelo es la de los cinco jueces de la ronda 1; lo propio de
este está en fixtures.json. Además valida el corpus del modo en vivo,
eval/pares-inventados.json: pares inventados, ninguno del curso, y que ninguna
oración de los ejemplos de la guía aparezca en él (si apareciera, el juez
rendiría con la respuesta a la vista).

Modo en vivo, sólo a pedido y con tope: ``--claude --max N`` le pregunta a
claude -p, como lo llama el proveedor claude-cli de los jueces (``-p
--output-format json --tools "" --safe-mode --no-session-persistence
--system-prompt <guía>`` y, por stdin, el transcript «## User» con la plantilla
rellenada), los primeros N pares del
corpus —o los de ``--cases``—, pasa cada respuesta por el gate de verdad y
puntúa contra lo esperado. Sin ``--max`` no consulta. Nunca sobre el curso.
Sólo biblioteca estándar. Uso, desde la raíz del proyecto:

    python3 .sentinel/agents/near-duplicates-pair-judge/eval/run_evals.py
    python3 .sentinel/agents/near-duplicates-pair-judge/eval/run_evals.py --claude --max 24
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

AGENT_DIR = Path(__file__).resolve().parents[1]
NODES_DIR = AGENT_DIR / "nodes"
FIXTURES = Path(__file__).with_name("fixtures.json")
LIVE_CORPUS = Path(__file__).with_name("pares-inventados.json")
SCHEMA = AGENT_DIR / "schema" / "verdict.schema.json"
RUN_ID = "3ac62128-e81c-4a7e-af94-440b2f1c35fa"
RUN_BASENAME = "20260930T120000-3ac62128"
UNUSABLE = {"outputUnusable": True}


class EvalError(Exception):
    pass


def require(condition, message):
    if not condition:
        raise EvalError(message)


def canon(value):
    """Igualdad JSON: true no es 1 y el orden de las claves no importa."""
    return json.dumps(value, ensure_ascii=False, sort_keys=True)


# ------------------------------------------------------------------ el esquema

SCHEMA_WORDS = {
    "$schema", "$id", "$comment", "title", "description", "type", "enum", "const",
    "required", "properties", "additionalProperties", "items", "minItems", "maxItems",
    "uniqueItems", "minLength", "maxLength", "minimum", "maximum", "allOf", "if", "then",
    "else",
}


def type_matches(value, name):
    if name == "object":
        return isinstance(value, dict)
    if name == "array":
        return isinstance(value, list)
    if name == "string":
        return isinstance(value, str)
    if name == "integer":
        return isinstance(value, int) and not isinstance(value, bool)
    if name == "number":
        return isinstance(value, (int, float)) and not isinstance(value, bool)
    if name == "boolean":
        return isinstance(value, bool)
    if name == "null":
        return value is None
    raise EvalError("tipo de esquema desconocido: %s" % name)


def schema_errors(value, schema, path="$"):
    """El subconjunto de JSON Schema 2020-12 que usan los esquemas de los jueces."""
    unknown = set(schema) - SCHEMA_WORDS
    if unknown:
        raise EvalError("el esquema usa palabras que este validador no conoce: %s" % sorted(unknown))
    if "type" in schema:
        names = schema["type"] if isinstance(schema["type"], list) else [schema["type"]]
        if not any(type_matches(value, name) for name in names):
            return ["%s: se esperaba %s" % (path, " | ".join(names))]
    errors = []
    if "const" in schema and canon(value) != canon(schema["const"]):
        errors.append("%s: se esperaba %s" % (path, canon(schema["const"])))
    if "enum" in schema and canon(value) not in {canon(item) for item in schema["enum"]}:
        errors.append("%s: %s fuera del enum" % (path, canon(value)))
    if isinstance(value, str):
        if len(value) < schema.get("minLength", 0):
            errors.append("%s: más corto que %d" % (path, schema["minLength"]))
        if "maxLength" in schema and len(value) > schema["maxLength"]:
            errors.append("%s: más largo que %d" % (path, schema["maxLength"]))
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        if "minimum" in schema and value < schema["minimum"]:
            errors.append("%s: menor que %s" % (path, schema["minimum"]))
        if "maximum" in schema and value > schema["maximum"]:
            errors.append("%s: mayor que %s" % (path, schema["maximum"]))
    if isinstance(value, list):
        if len(value) < schema.get("minItems", 0):
            errors.append("%s: menos de %d elementos" % (path, schema["minItems"]))
        if "maxItems" in schema and len(value) > schema["maxItems"]:
            errors.append("%s: más de %d elementos" % (path, schema["maxItems"]))
        if schema.get("uniqueItems") and len({canon(item) for item in value}) != len(value):
            errors.append("%s: elementos repetidos" % path)
        if "items" in schema:
            for index, item in enumerate(value):
                errors += schema_errors(item, schema["items"], "%s[%d]" % (path, index))
    if isinstance(value, dict):
        for key in schema.get("required", []):
            if key not in value:
                errors.append("%s: falta %s" % (path, key))
        properties = schema.get("properties", {})
        if schema.get("additionalProperties") is False:
            for key in value:
                if key not in properties:
                    errors.append("%s: sobra %s" % (path, key))
        for key, sub in properties.items():
            if key in value:
                errors += schema_errors(value[key], sub, "%s.%s" % (path, key))
    for sub in schema.get("allOf", []):
        errors += schema_errors(value, sub, path)
    if "if" in schema:
        if not schema_errors(value, schema["if"], path):
            errors += schema_errors(value, schema.get("then", {}), path)
        else:
            errors += schema_errors(value, schema.get("else", {}), path)
    return errors


# ------------------------------------------------------------- la estructura

def read(path):
    return path.read_text(encoding="utf-8")


def split_frontmatter(path):
    match = re.match(r"\A---\n(.*?)\n---\n?(.*)\Z", read(path), re.S)
    require(match is not None, "%s no tiene frontmatter" % path.relative_to(AGENT_DIR))
    return match.group(1), match.group(2)


def yaml_list(text, key):
    """Los ítems «- valor» bajo «key:» en un YAML simple, sin dependencias."""
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if not re.fullmatch(r"\s*%s:\s*" % re.escape(key), line):
            continue
        indent = len(line) - len(line.lstrip())
        items = []
        for following in lines[index + 1:]:
            stripped = following.strip()
            if not stripped or stripped.startswith("#"):
                continue
            depth = len(following) - len(following.lstrip())
            if stripped.startswith("- ") and depth >= indent:
                items.append(re.sub(r"\s+#.*$", "", stripped[2:]).strip())
                continue
            break
        return items
    return None


def check_structure(fixtures):
    agent = fixtures["agent"]
    dependencies = fixtures["dependencies"]
    artifact = fixtures["artifact"]
    require(AGENT_DIR.name == agent, "la carpeta se llama %s y el agente %s" % (AGENT_DIR.name, agent))

    agent_yaml = read(AGENT_DIR / "agent.yaml")
    require(re.search(r"^name: %s$" % re.escape(agent), agent_yaml, re.M) is not None,
            "agent.yaml no se llama %s" % agent)
    require(re.search(r"^initial_node: evaluate$", agent_yaml, re.M) is not None,
            "agent.yaml no arranca en evaluate")
    require(yaml_list(agent_yaml, "nodes")
            == ["./nodes/evaluate.md", "./nodes/gate_verdict.md", "./nodes/emit.md"],
            "agent.yaml no declara los tres nodos evaluate → gate_verdict → emit")
    require(yaml_list(agent_yaml, "dependencies") == dependencies,
            "las dependencies de agent.yaml no son %s, en ese orden" % dependencies)
    require(yaml_list(agent_yaml, "required_artifacts") == [artifact],
            "goal.required_artifacts no es [%s]" % artifact)
    require(yaml_list(agent_yaml, "required_nodes_done") == ["emit"],
            "goal.required_nodes_done no es [emit]")

    front, body = split_frontmatter(NODES_DIR / "evaluate.md")
    require("kind: llm_call" in front, "evaluate no es un llm_call")
    require(re.search(r"^\s*max_attempts: \d+\s*$", front, re.M) is not None,
            "evaluate no acota sus intentos con max_attempts")
    require('pointer: "{run_dir}/verdicts.json"' in front,
            "evaluate no escribe {run_dir}/verdicts.json, que es lo que lee el gate")
    require("{ to: gate_verdict }" in front and "{ to: emit, when: exhausted }" in front,
            "evaluate no va a gate_verdict ni desvía a emit al agotar sus intentos")
    require("when: failed" not in front,
            "evaluate no lleva arista failed: una falla del modelo es de servicio, no outputUnusable")
    parts = re.split(r"^# User prompt template\s*$", body, maxsplit=1, flags=re.M)
    require(len(parts) == 2, "evaluate.md no tiene la sección '# User prompt template'")
    head, template = parts
    system_parts = re.split(r"^# System prompt\s*$", head, maxsplit=1, flags=re.M)
    require(len(system_parts) == 2, "evaluate.md no tiene la sección '# System prompt'")
    system = system_parts[1]
    used = re.findall(r"\{([A-Za-z_][\w.-]*)\}", template)
    require(sorted(used) == sorted(dependencies),
            "la plantilla tiene que usar cada entrada una vez y ninguna otra cosa: usa %s" % used)
    leaked = sorted(set(re.findall(r"\{([A-Za-z_][\w.-]*)\}", system))
                    & (set(dependencies) | {"run_dir", "node_id", "phase_id"}))
    require(not leaked,
            "el system prompt tiene placeholders que el framework reemplazaría: %s" % leaked)
    require(re.search(r"\{(q|artifact|skill|file):", system) is None,
            "el system prompt tiene placeholders {q:/artifact:/skill:/file:}")
    for phrase in fixtures["promptMustMention"]:
        require(phrase in system, "la guía ya no dice %r" % phrase)

    front, _ = split_frontmatter(NODES_DIR / "gate_verdict.md")
    require("kind: gate" in front and "entry_point: ../scripts/gate_verdict.sh" in front,
            "gate_verdict no es el gate de ../scripts/gate_verdict.sh")
    require("gate: %s" % fixtures["gateId"] in front, "gate_verdict no se llama %s" % fixtures["gateId"])
    require("{ to: emit, when: passed }" in front and "{ to: evaluate, when: failed }" in front,
            "gate_verdict no va a emit al pasar y a evaluate al fallar")

    front, _ = split_frontmatter(NODES_DIR / "emit.md")
    require("kind: script" in front and "entry_point: ../scripts/emit.sh" in front,
            "emit no es el script de ../scripts/emit.sh")
    require("script: %s" % fixtures["emitId"] in front, "emit no se llama %s" % fixtures["emitId"])
    require("- kind: %s" % artifact in front and 'pointer: "{run_dir}/%s.json"' % artifact in front,
            "emit no declara el artifact %s" % artifact)

    for name in ("gate_verdict.sh", "emit.sh"):
        require(os.access(AGENT_DIR / "scripts" / name, os.X_OK), "scripts/%s no es ejecutable" % name)


def check_schema(schema, fixtures):
    require(schema.get("additionalProperties") is False, "el esquema no es cerrado")
    require(schema.get("required") == fixtures["topKeys"],
            "el esquema no exige %s, en ese orden" % fixtures["topKeys"])
    require(set(schema.get("properties", {})) == set(fixtures["topKeys"]),
            "las propiedades del esquema no son %s" % fixtures["topKeys"])


# ------------------------------------------------------------------- los casos

def run_script(name, state):
    env = {key: value for key, value in os.environ.items() if key != "SENTINEL_RUN_DIR"}
    completed = subprocess.run(
        ["bash", "../scripts/" + name],
        cwd=str(NODES_DIR),
        input=json.dumps(state, ensure_ascii=False),
        capture_output=True,
        text=True,
        timeout=60,
        env=env,
        check=False,
    )
    require(completed.returncode == 0,
            "%s salió con %d: %s" % (name, completed.returncode, completed.stderr.strip()))
    output = completed.stdout.strip()
    require(output and "\n" not in output,
            "%s tiene que imprimir UNA línea JSON; imprimió %r" % (name, output[:300]))
    parsed = json.loads(output)
    require(isinstance(parsed, dict), "%s no imprimió un objeto JSON" % name)
    return parsed


def run_case(case, fixtures, schema):
    with tempfile.TemporaryDirectory(prefix="judge-eval-") as temp:
        root = Path(temp)
        runs_base = root / ".sentinel" / "runs" / fixtures["agent"]
        run_dir = runs_base / RUN_BASENAME
        run_dir.mkdir(parents=True)
        (run_dir / "state.json").write_text(json.dumps({"runId": RUN_ID}), encoding="utf-8")

        inputs = dict(fixtures["inputs"][case["inputs"]])
        run_id = RUN_ID
        resolution = case.get("resolution", "run_dir")
        if resolution == "run_dir":
            inputs["run_dir"] = str(run_dir)
        elif resolution == "state.json":
            inputs["project_root"] = str(root)
        elif resolution == "ambiguous":
            inputs["project_root"] = str(root)
            twin = runs_base / "20260930T120001-twin"
            twin.mkdir()
            (twin / "state.json").write_text(json.dumps({"runId": RUN_ID}), encoding="utf-8")
        elif resolution == "unsafe-run-id":
            inputs["project_root"] = str(root)
            run_id = "../escape"
        elif resolution != "none":
            raise EvalError("resolution desconocida: %s" % resolution)

        verdicts = run_dir / "verdicts.json"
        if "raw" in case:
            verdicts.write_text(case["raw"], encoding="utf-8")
        elif "verdict" in case:
            verdicts.write_text(json.dumps(case["verdict"], ensure_ascii=False, indent=2),
                                encoding="utf-8")
        validated = run_dir / "validated-verdict.json"
        if case.get("staleValidated"):
            validated.write_text('{"stale": true}', encoding="utf-8")

        state = {
            "inputs": inputs,
            "data": {},
            "node": {"name": "gate_verdict", "kind": "gate", "gate_id": fixtures["gateId"]},
            "run_id": run_id,
        }
        gate = run_script("gate_verdict.sh", state)
        expect = case["expect"]
        canonical = None
        if expect["route"] == "passed":
            require(gate.get("route") == "passed" and "error" not in gate,
                    "se esperaba passed y el gate dijo %s" % canon(gate))
            canonical = json.loads(read(validated))
            wanted = case.get("verdict") if expect.get("canonical") == "same" else expect.get("canonical")
            require(wanted is not None, "el caso no dice qué canónico espera")
            require(canon(canonical) == canon(wanted),
                    "canónico inesperado:\n  dio      %s\n  esperaba %s" % (canon(canonical), canon(wanted)))
            errors = schema_errors(canonical, schema)
            require(not errors, "el canónico no cumple el esquema: %s" % errors)
        else:
            require(gate.get("route") == "failed" and isinstance(gate.get("error"), str)
                    and gate["error"].strip(),
                    "se esperaba failed con su error y el gate dijo %s" % canon(gate))
            for fragment in expect.get("errorContains", []):
                require(fragment in gate["error"], "el error no dice %r: %s" % (fragment, gate["error"]))
            require(not validated.exists(), "el gate falló y quedó un validated-verdict.json")
            if "schemaRejects" in expect:
                require("verdict" in case, "schemaRejects necesita el veredicto como objeto")
                errors = schema_errors(case["verdict"], schema)
                if expect["schemaRejects"]:
                    require(errors, "el esquema acepta un veredicto que el gate rechaza")
                else:
                    require(not errors,
                            "el esquema rechaza lo que sólo se puede ver contra las entradas: %s" % errors)

        if case.get("corruptValidated"):
            validated.write_text("{esto no es JSON", encoding="utf-8")
            canonical = None

        emit_state = dict(state, node={"name": "emit", "kind": "script", "script_id": fixtures["emitId"]})
        emitted = run_script("emit.sh", emit_state)
        artifact_key = fixtures["artifact"]
        require(set(emitted) == {"route", artifact_key} and emitted["route"] == "done",
                "emit tiene que imprimir route=done y %s; imprimió %s" % (artifact_key, canon(emitted)))
        require(isinstance(emitted[artifact_key], str), "el artifact tiene que viajar como texto JSON")
        artifact = json.loads(emitted[artifact_key])
        wanted = canonical if canonical is not None else UNUSABLE
        require(canon(artifact) == canon(wanted),
                "emit produjo %s y se esperaba %s" % (canon(artifact), canon(wanted)))
        return expect["route"]


def snapshot():
    return sorted(str(path.relative_to(AGENT_DIR)) for path in AGENT_DIR.rglob("*"))


# --------------------------------------------------------- el corpus en vivo

LIVE_KEYS = {"caseId", "expected", "category", "title", "first", "second", "cosine", "note"}


def prompt_parts():
    """La guía y la plantilla, leídas de nodes/evaluate.md: nunca una copia."""
    _, body = split_frontmatter(NODES_DIR / "evaluate.md")
    head, template = re.split(r"^# User prompt template\s*$", body, maxsplit=1, flags=re.M)
    system = re.split(r"^# System prompt\s*$", head, maxsplit=1, flags=re.M)[1]
    return system.strip(), template.strip()


def check_live_corpus(corpus, system):
    cases = corpus.get("cases")
    require(isinstance(cases, list) and cases, "el corpus en vivo no tiene casos")
    seen = set()
    for case in cases:
        case_id = case.get("caseId", "<sin id>")
        require(set(case) <= LIVE_KEYS, "%s: claves de más %s" % (case_id, sorted(set(case) - LIVE_KEYS)))
        require({"caseId", "expected", "title", "first", "second"} <= set(case), "%s: faltan claves" % case_id)
        require(case_id not in seen, "%s: caseId repetido" % case_id)
        seen.add(case_id)
        require(case["expected"] in (True, False, None), "%s: expected es true, false o null" % case_id)
        for key in ("first", "second"):
            require("\nResuelto: " in case[key], "%s: %s no trae la línea «Resuelto: …»" % (case_id, key))
        require(case["first"] != case["second"], "%s: los dos ejercicios son iguales tal cual" % case_id)
    same = sum(1 for case in cases if case["expected"] is True)
    other = sum(1 for case in cases if case["expected"] is False)
    require(same >= 15 and other >= 15, "hacen falta al menos 15 pares de cada lado: hay %d y %d" % (same, other))
    # Las oraciones inglesas de los ejemplos de la guía, entre comillas latinas.
    examples = [text for text in re.findall(r"«([^»]+)»", system)
                if " " in text and text.rstrip()[-1:] in ".?!"]
    require(examples, "la guía no tiene ejemplos entre «…»: este control no controlaría nada")
    for case in cases:
        for key in ("first", "second"):
            for example in examples:
                require(example not in case[key],
                        "%s: «%s» es un ejemplo de la guía; el juez rendiría con la respuesta a la vista"
                        % (case["caseId"], example))
    return cases


# ------------------------------------------------------------- el juez de verdad

# Lo que ClaudeCliChatModel.renderTranscript agrega después de los mensajes.
TRANSCRIPT_TAIL = ("Reply with the next assistant turn only. Do NOT quote or repeat the transcript above — never "
                   "emit '## User', '## Assistant' or '## Tool result' headers; output only your own message content.")


def ask_claude(system, user, model, timeout):
    """Como ClaudeCliChatModel del proveedor claude-cli: la guía por --system-prompt y, por stdin, el transcript
    que arma renderTranscript con la plantilla rellenada como único mensaje del usuario."""
    command = ["claude", "-p", "--output-format", "json", "--tools", "", "--safe-mode",
               "--no-session-persistence", "--system-prompt", system]
    if model:
        command += ["--model", model]
    transcript = "## User\n" + user + "\n\n" + TRANSCRIPT_TAIL
    try:
        completed = subprocess.run(command, input=transcript, capture_output=True, text=True, timeout=timeout,
                                   check=False)
    except subprocess.TimeoutExpired:
        return None, "se pasó de %d s" % timeout, None
    try:
        answer = json.loads(completed.stdout)
    except ValueError:
        return None, (completed.stderr or completed.stdout or "sin salida").strip()[:200], None
    if answer.get("is_error"):
        return None, str(answer.get("result") or answer.get("subtype"))[:200], answer
    return answer.get("result") or "", None, answer


def gate_once(fixtures, text):
    """La respuesta, por el gate de verdad: el veredicto canónico o por qué no lo hay."""
    with tempfile.TemporaryDirectory(prefix="judge-live-") as temp:
        run_dir = Path(temp)
        (run_dir / "verdicts.json").write_text(text, encoding="utf-8")
        state = {"inputs": {"run_dir": str(run_dir)}, "data": {},
                 "node": {"name": "gate_verdict", "kind": "gate", "gate_id": fixtures["gateId"]},
                 "run_id": RUN_ID}
        gate = run_script("gate_verdict.sh", state)
        if gate.get("route") != "passed":
            return None, gate.get("error")
        return json.loads(read(run_dir / "validated-verdict.json")), None


def live(args, fixtures, cases):
    system, template = prompt_parts()
    chosen = [case for case in cases if case["expected"] is not None]
    if args.cases:
        wanted = [case_id.strip() for case_id in args.cases.split(",") if case_id.strip()]
        known = {case["caseId"]: case for case in cases}
        missing = [case_id for case_id in wanted if case_id not in known]
        require(not missing, "no están en el corpus: %s" % missing)
        chosen = [known[case_id] for case_id in wanted]
    chosen = chosen[:max(0, args.max)]
    print("\nen vivo: %d consultas a claude -p (tope %d), una por par" % (len(chosen), args.max))
    tally = {"tp": 0, "fn": 0, "fp": 0, "tn": 0, "sin": 0, "dudosos": 0}
    paid = 0
    cost = 0.0
    for case in chosen:
        user = (template.replace("{title}", case["title"]).replace("{first}", case["first"])
                .replace("{second}", case["second"]))
        text, failure, raw = ask_claude(system, user, args.model, args.timeout)
        paid += 1
        if raw is not None:
            cost += float(raw.get("total_cost_usd") or 0.0)
        verdict = None
        if text is not None:
            verdict, failure = gate_once(fixtures, text)
        expected = case["expected"]
        if verdict is None:
            tally["sin"] += 1
            print("--  %-36s esperado=%-6s sin veredicto: %s" % (case["caseId"], expected, failure))
            continue
        said = verdict["sameExercise"]
        if expected is None:
            tally["dudosos"] += 1
            mark = "?? "
        else:
            key = ("tp" if said else "fn") if expected else ("fp" if said else "tn")
            tally[key] += 1
            mark = "ok " if said == expected else "XX "
        print("%s %-36s esperado=%-6s dijo=%-6s %s" % (mark, case["caseId"], expected, said, verdict["reason"][:90]))
    judged = tally["tp"] + tally["fn"] + tally["fp"] + tally["tn"]
    print("\nmismo ejercicio: %d de %d dijo que sí; otro ejercicio: %d de %d dijo que no; sin veredicto %d"
          % (tally["tp"], tally["tp"] + tally["fn"], tally["tn"], tally["tn"] + tally["fp"], tally["sin"]))
    print("aciertos %d de %d; consultas pagas: %d (costo equivalente informado por la CLI: US$ %.2f)"
          % (tally["tp"] + tally["tn"], judged, paid, cost))
    return 1 if tally["fn"] or tally["fp"] or tally["sin"] else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--claude", action="store_true", help="consultar a claude -p con el corpus en vivo")
    parser.add_argument("--max", type=int, default=0, help="tope de consultas pagas; sin él no consulta")
    parser.add_argument("--cases", default=None, help="caseIds del corpus en vivo, separados por comas")
    parser.add_argument("--model", default=None, help="--model de claude -p (CONTENT_AUDIT_JUDGE_MODEL)")
    parser.add_argument("--timeout", type=int, default=180)
    args = parser.parse_args()

    fixtures = json.loads(read(FIXTURES))
    schema = json.loads(read(SCHEMA))
    before = snapshot()
    try:
        check_structure(fixtures)
        check_schema(schema, fixtures)
        system, _ = prompt_parts()
        live_cases = check_live_corpus(json.loads(read(LIVE_CORPUS)), system)
    except EvalError as error:
        print("FALLA DE ESTRUCTURA: %s" % error)
        return 2
    print("estructura: OK (agent.yaml, nodos, guía y placeholders de %s)" % fixtures["agent"])
    print("corpus en vivo: %d pares inventados (%d el mismo, %d otro, %d dudosos), ninguno con un ejemplo de la guía"
          % (len(live_cases), sum(1 for c in live_cases if c["expected"] is True),
             sum(1 for c in live_cases if c["expected"] is False),
             sum(1 for c in live_cases if c["expected"] is None)))

    failures = []
    routes = {"passed": 0, "failed": 0}
    seen = set()
    for case in fixtures["cases"]:
        case_id = case["caseId"]
        try:
            require(case_id not in seen, "caseId repetido")
            seen.add(case_id)
            routes[run_case(case, fixtures, schema)] += 1
            print("ok  %s" % case_id)
        except EvalError as error:
            failures.append(case_id)
            print("XX  %s: %s" % (case_id, error))

    print("gate y emit: %d casos, %d aceptados y %d rechazados por el gate, %d con problemas"
          % (len(fixtures["cases"]), routes["passed"], routes["failed"], len(failures)))

    live_status = 0
    if args.claude and args.max > 0:
        try:
            live_status = live(args, fixtures, live_cases)
        except EvalError as error:
            print("FALLA EN VIVO: %s" % error)
            live_status = 2
    else:
        print("modelo: no se consultó (sólo salidas grabadas de evaluate; en vivo, --claude --max N)")

    if snapshot() != before:
        failures.append("<carpeta del agente>")
        print("XX  la prueba dejó archivos en la carpeta del agente: cambian la versión del juez")
    return 1 if failures else live_status


if __name__ == "__main__":
    sys.exit(main())
