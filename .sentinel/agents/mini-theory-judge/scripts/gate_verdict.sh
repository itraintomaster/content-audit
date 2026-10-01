#!/bin/bash
# Gate binario de forma del juez mini-theory-judge.
# stdin: estado del framework. stdout: UNA línea JSON. Solo Python stdlib.
#
# Lee {run_dir}/verdicts.json (la salida de evaluate), la valida contra el
# esquema cerrado de schema/verdict.schema.json y las invariantes cruzadas con
# las entradas, y escribe la versión canónica en validated-verdict.json. Ante
# cualquier desvío imprime {"route": "failed", "error": ...}: el framework
# vuelve a evaluate y le muestra el error al modelo.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json
import os
import re
import sys
import unicodedata
from pathlib import Path

AGENT = "mini-theory-judge"
TAG = "mini_theory_judge_gate"

state = json.loads(sys.argv[1])
inputs = state.get("inputs", {}) or {}
data = state.get("data", {}) or {}
node = state.get("node", {}) or {}


def resolve_run_dir():
    """Resuelve el directorio del run sin asumir que run_dir viene proyectado.

    El runner in-process de content-audit agrega run_dir a las entradas; el CLI
    proyecta run_id arriba de todo y project_root/root en inputs. Un run_dir
    explícito tiene precedencia.
    """
    for raw in (
        state.get("run_dir"),
        inputs.get("run_dir"),
        data.get("run_dir"),
        node.get("run_dir"),
        os.environ.get("SENTINEL_RUN_DIR"),
    ):
        if isinstance(raw, str) and raw.strip():
            path = Path(raw).expanduser().resolve()
            if not path.is_dir():
                return None, "run_dir explícito no existe o no es directorio: %s" % path
            return path, None

    project_raw = (
        inputs.get("project_root") or inputs.get("root")
        or data.get("project_root") or data.get("root")
        or state.get("project_root") or state.get("root")
    )
    run_id = (
        state.get("run_id") or inputs.get("run_id")
        or data.get("run_id") or node.get("run_id")
    )
    if not isinstance(project_raw, str) or not project_raw.strip():
        return None, "faltan run_dir y project_root/root para resolver el run"
    if (
        not isinstance(run_id, str)
        or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", run_id)
        or Path(run_id).name != run_id
    ):
        return None, "run_id ausente o inseguro"

    project_root = Path(project_raw).expanduser().resolve()
    if not project_root.is_dir():
        return None, "project_root/root no existe o no es directorio: %s" % project_root
    runs_base = (project_root / ".sentinel" / "runs" / AGENT).resolve()
    if not runs_base.is_dir():
        return None, "directorio base de runs no existe: %s" % runs_base

    direct = (runs_base / run_id).resolve()
    if direct.parent != runs_base:
        return None, "run_id escaparía del directorio de runs"
    if direct.is_dir():
        return direct, None

    # El run_id proyectado por el subprocess es el UUID de RunState, no el
    # basename con fecha que usa RunPersistence.
    matches = []
    for child in runs_base.iterdir():
        try:
            resolved = child.resolve()
        except OSError:
            continue
        if resolved.parent != runs_base or not resolved.is_dir():
            continue
        try:
            with (resolved / "state.json").open("r", encoding="utf-8") as handle:
                persisted = json.load(handle)
        except (OSError, ValueError):
            continue
        if isinstance(persisted, dict) and persisted.get("runId") == run_id:
            matches.append(resolved)

    if len(matches) != 1:
        return None, (
            "run_id %s coincide con %d state.json bajo %s; se requiere exactamente uno"
            % (run_id, len(matches), runs_base)
        )
    return matches[0], None


run_dir, run_dir_error = resolve_run_dir()
source_path = run_dir / "verdicts.json" if run_dir else None
validated_path = run_dir / "validated-verdict.json" if run_dir else None


def fail(reason):
    if validated_path is not None:
        try:
            validated_path.unlink()
        except FileNotFoundError:
            pass
    sys.stderr.write("[%s] FAILED: %s\n" % (TAG, reason))
    print(json.dumps({"route": "failed", "error": reason}, ensure_ascii=False))
    raise SystemExit(0)


if run_dir_error:
    fail(run_dir_error)


def parse_json_value(value):
    """Desenvuelve hasta 3 niveles de JSON serializado en un string.

    Tolera un fence ```json y prosa alrededor del objeto: rechazarlos manda a
    reintentar una respuesta que estaba bien, y se paga otra consulta para
    llegar al mismo lugar.
    """
    current = value
    for _ in range(3):
        if not isinstance(current, str):
            return current
        text = current.strip()
        if text.startswith("```"):
            text = re.sub(r"^```[a-zA-Z0-9_-]*\s*", "", text)
            text = re.sub(r"\s*```$", "", text).strip()
        try:
            current = json.loads(text)
            continue
        except ValueError:
            start = text.find("{")
            if start < 0:
                return None
            depth = 0
            in_string = False
            escaped = False
            for index in range(start, len(text)):
                char = text[index]
                if in_string:
                    if escaped:
                        escaped = False
                    elif char == "\\":
                        escaped = True
                    elif char == '"':
                        in_string = False
                    continue
                if char == '"':
                    in_string = True
                elif char == "{":
                    depth += 1
                elif char == "}":
                    depth -= 1
                    if depth == 0:
                        try:
                            current = json.loads(text[start:index + 1])
                        except ValueError:
                            return None
                        break
            else:
                return None
    return current


if source_path is None or not source_path.is_file():
    fail("verdicts.json no existe en %s" % (source_path or "<run_dir desconocido>"))

try:
    raw_output = source_path.read_text(encoding="utf-8")
except OSError as exc:
    fail("verdicts.json ilegible: %s" % exc)

verdict = parse_json_value(raw_output)
if not isinstance(verdict, dict):
    fail("la salida no es un objeto JSON: devolvé sólo el objeto del veredicto, sin prosa")


def closed_object(value, keys, where):
    if not isinstance(value, dict):
        fail("%s tiene que ser un objeto JSON" % where)
    present = set(value)
    if present != set(keys):
        missing = sorted(set(keys) - present)
        extra = sorted(present - set(keys))
        fail(
            "%s no respeta el objeto cerrado (%s); faltan=%s sobran=%s"
            % (where, ", ".join(keys), missing, extra)
        )
    return value


def one_line(value, where, maximum):
    """Texto no vacío en una sola línea: colapsa espacios y saltos."""
    if not isinstance(value, str):
        fail("%s tiene que ser un string" % where)
    cleaned = " ".join(value.split())
    if not cleaned:
        fail("%s está vacío" % where)
    if len(cleaned) > maximum:
        fail("%s tiene %d caracteres; el máximo es %d" % (where, len(cleaned), maximum))
    return cleaned


def enum_value(value, allowed, where):
    """Valor de un enum, sin importar mayúsculas; vuelve en mayúsculas."""
    if isinstance(value, str) and value.strip().upper() in allowed:
        return value.strip().upper()
    fail(
        "%s %s fuera del enum; vale %s"
        % (where, json.dumps(value, ensure_ascii=False), " | ".join(allowed))
    )


def boolean(value, where):
    if not isinstance(value, bool):
        fail("%s tiene que ser true o false" % where)
    return value


def is_blank(value):
    return value is None or (isinstance(value, str) and not value.strip())


def upper_or_none(value):
    return value.strip().upper() if isinstance(value, str) else None


def finish(canonical):
    try:
        validated_path.write_text(
            json.dumps(canonical, ensure_ascii=False, separators=(",", ":")),
            encoding="utf-8",
        )
    except OSError as exc:
        fail("no se pudo escribir validated-verdict.json: %s" % exc)
    print(json.dumps({"route": "passed", "detail": "veredicto bien formado"}, ensure_ascii=False))


# -------------------------------------------------------------------- mini-theory
PROBLEMS = ("FALSE_RULE", "OVERSTATED", "CONTRADICTED_BY_EXERCISE", "NOT_COVERING_EXERCISE")
SEVERITIES = ("HIGH", "MEDIUM", "LOW")
# F-MINIT-R004: la gravedad sale del problema.
SEVERITY_FOR = {
    "FALSE_RULE": "HIGH",
    "CONTRADICTED_BY_EXERCISE": "HIGH",
    "OVERSTATED": "MEDIUM",
    "NOT_COVERING_EXERCISE": "LOW",
}
# F-MINIT-R003: estos cuentan sólo en los ejercicios nombrados, que tienen que
# mostrar la miniteoría.
ONLY_WHERE_SHOWN = ("CONTRADICTED_BY_EXERCISE", "NOT_COVERING_EXERCISE")


def mini_theories_from_inputs():
    """{n: ejercicios que muestran Mn, o None si la línea no los dice}.

    Las líneas son «M1: <texto> (ejercicios: E1, E4)»; el texto puede tener
    paréntesis propios, así que la lista se toma del final de cada entrada.
    None si la entrada no llegó.
    """
    raw = inputs.get("miniTheories")
    if not isinstance(raw, str):
        return None
    starts = list(re.finditer(r"^M(\d+): ", raw, re.M))
    found = {}
    for position, match in enumerate(starts):
        end = starts[position + 1].start() if position + 1 < len(starts) else len(raw)
        entry = raw[match.start():end].rstrip()
        shown = re.search(r"\(ejercicios: (E\d+(?:, E\d+)*)\)$", entry)
        found[int(match.group(1))] = (
            {int(label[1:]) for label in shown.group(1).split(", ")} if shown else None
        )
    return found


def exercises_from_inputs():
    """Los números de exercises («E1: <enunciado> → <respuestas>»); None si no llegó."""
    raw = inputs.get("exercises")
    if not isinstance(raw, str):
        return None
    return {int(match.group(1)) for match in re.finditer(r"^E(\d+): ", raw, re.M)}


def reference(value, prefix, where):
    """Un número de miniteoría o de ejercicio: 3, o «M3» / «E3» como en las entradas."""
    if isinstance(value, bool):
        fail("%s tiene que ser un número entero" % where)
    if isinstance(value, int):
        number = value
    elif isinstance(value, str) and re.fullmatch(r"\s*%s?\s*\d+\s*" % prefix, value, re.I):
        number = int(re.search(r"\d+", value).group(0))
    else:
        fail("%s tiene que ser un número entero (%s3 → 3)" % (where, prefix))
    if number < 1:
        fail("%s tiene que ser 1 o más" % where)
    return number


def labels(prefix, numbers):
    return ", ".join("%s%d" % (prefix, number) for number in sorted(numbers)) or "ninguna"


closed_object(verdict, ("assessments", "rationale"), "el veredicto")
assessments = verdict["assessments"]
if not isinstance(assessments, list):
    fail("assessments tiene que ser un array")

theories = mini_theories_from_inputs()
exercise_numbers = exercises_from_inputs()

canonical_assessments = []
entries_by_theory = {}
for index, item in enumerate(assessments):
    where = "assessments[%d]" % index
    closed_object(
        item,
        ("miniTheory", "holds", "problem", "severity", "counterexample", "exercises", "explanation"),
        where,
    )
    number = reference(item["miniTheory"], "M", where + ".miniTheory")
    if theories is not None and number not in theories:
        fail(
            "%s.miniTheory M%d no es una miniteoría del tema; son %s"
            % (where, number, labels("M", theories))
        )
    holds = boolean(item["holds"], where + ".holds")

    if not isinstance(item["exercises"], list):
        fail(where + ".exercises tiene que ser un array de números de ejercicio")
    listed = sorted({
        reference(value, "E", "%s.exercises[%d]" % (where, position))
        for position, value in enumerate(item["exercises"])
    })
    if exercise_numbers is not None:
        outside = [n for n in listed if n not in exercise_numbers]
        if outside:
            fail(
                "%s.exercises nombra ejercicios que no están en el tema: %s"
                % (where, labels("E", outside))
            )

    if holds:
        for key in ("problem", "severity", "counterexample"):
            if not is_blank(item[key]):
                fail("%s: con holds true, %s va null" % (where, key))
        problem = severity = counterexample = None
    else:
        if is_blank(item["problem"]):
            fail(where + ": con holds false, problem dice qué tiene: " + " | ".join(PROBLEMS))
        problem = enum_value(item["problem"], PROBLEMS, where + ".problem")
        if upper_or_none(item["severity"]) == "BLOCKING":
            fail(
                where + ".severity BLOCKING no existe en este juez: la miniteoría se ve "
                "después de contestar"
            )
        severity = enum_value(item["severity"], SEVERITIES, where + ".severity")
        if severity != SEVERITY_FOR[problem]:
            fail(
                "%s: %s va con severity %s, no %s"
                % (where, problem, SEVERITY_FOR[problem], severity)
            )
        if is_blank(item["counterexample"]):
            fail(where + ": con holds false, counterexample es una oración que muestra el problema")
        counterexample = one_line(item["counterexample"], where + ".counterexample", 300)
        if not listed:
            fail(where + ": con holds false, exercises nombra los ejercicios involucrados")
        shown = theories.get(number) if theories is not None else None
        if problem in ONLY_WHERE_SHOWN and shown is not None:
            not_shown = [n for n in listed if n not in shown]
            if not_shown:
                fail(
                    "%s: %s nombra sólo ejercicios que muestran M%d (%s), y %s no la "
                    "muestra; si la contradice un ejercicio que no la muestra, la regla "
                    "es FALSE_RULE"
                    % (where, problem, number, labels("E", shown), labels("E", not_shown))
                )

    entries_by_theory.setdefault(number, []).append((holds, problem))
    canonical_assessments.append({
        "miniTheory": number,
        "holds": holds,
        "problem": problem,
        "severity": severity,
        "counterexample": counterexample,
        "exercises": listed,
        "explanation": one_line(item["explanation"], where + ".explanation", 600),
    })

for number in sorted(entries_by_theory):
    entries = entries_by_theory[number]
    if any(holds for holds, _ in entries) and len(entries) > 1:
        fail(
            "M%d tiene una entrada con holds true y otras más: si es cierta y nada la "
            "contradice, va una sola entrada" % number
        )
    problems = [problem for holds, problem in entries if not holds]
    repeated = sorted({problem for problem in problems if problems.count(problem) > 1})
    if repeated:
        fail(
            "M%d repite el problema %s: va una entrada por problema, con todos sus ejercicios"
            % (number, repeated[0])
        )
    if "FALSE_RULE" in problems and "OVERSTATED" in problems:
        fail("M%d no puede ser FALSE_RULE y OVERSTATED a la vez: elegí la que corresponde" % number)

if theories is not None:
    missing = sorted(set(theories) - set(entries_by_theory))
    if missing:
        fail(
            "faltan miniteorías en assessments: %s; cada M del tema lleva al menos una entrada"
            % labels("M", missing)
        )

rationale = one_line(verdict["rationale"], "rationale", 1200)

finish({"assessments": canonical_assessments, "rationale": rationale})
PY
