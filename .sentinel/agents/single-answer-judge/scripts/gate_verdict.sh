#!/bin/bash
# Gate binario de forma del juez single-answer-judge.
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

AGENT = "single-answer-judge"
TAG = "single_answer_judge_gate"

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


def without_trailing_commas(text):
    """El texto sin las comas que quedan justo antes de un } o un ], fuera de los textos."""
    kept = []
    in_string = False
    escaped = False
    for index, char in enumerate(text):
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
        elif char == '"':
            in_string = True
        elif char == "," and text[index + 1:].lstrip()[:1] in ("}", "]"):
            continue
        kept.append(char)
    return "".join(kept)


def without_undefined(text):
    """El texto con cada `undefined` suelto cambiado por null, fuera de los textos.

    A veces el juez cierra un objeto con una clave de relleno que ni siquiera
    tiene un valor de JSON ("status_": undefined). Como null, la clave queda
    vacía y la descarta el filtro de claves vacías. Adentro de un texto, la
    palabra no se toca.
    """
    kept = []
    in_string = False
    escaped = False
    index = 0
    while index < len(text):
        char = text[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
        elif char == '"':
            in_string = True
        elif (
            text.startswith("undefined", index)
            and not (index and (text[index - 1].isalnum() or text[index - 1] == "_"))
            and not re.match(r"[A-Za-z0-9_]", text[index + 9:index + 10])
        ):
            kept.append("null")
            index += len("undefined")
            continue
        kept.append(char)
        index += 1
    return "".join(kept)


def loads_lenient(text):
    """json.loads que perdona la coma colgante y el `undefined` suelto.

    A veces el juez deja una coma después de la última clave ("evidence": "…",
    y en la línea siguiente }). Esa coma no dice nada: si el texto no parsea,
    se prueba sin ella. Las comas de adentro de los textos no se tocan.
    """
    try:
        return json.loads(text)
    except ValueError:
        cleaned = without_undefined(without_trailing_commas(text))
        if cleaned == text:
            raise
        return json.loads(cleaned)


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
            current = loads_lenient(text)
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
                            current = loads_lenient(text[start:index + 1])
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


def is_empty(value):
    """null, texto vacío o en blanco, lista u objeto vacíos."""
    if value is None:
        return True
    if isinstance(value, str):
        return not value.strip()
    return isinstance(value, (list, dict)) and not value


NOTHING = ("NONE", "N/A", "NA", "NULL")


def says_nothing(value):
    """Vacío, una marca de «nada» (NONE) o un número suelto."""
    if is_empty(value):
        return True
    if isinstance(value, str):
        return value.strip().upper() in NOTHING
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def is_placeholder(item, texts):
    """Un elemento de relleno: sin ningún texto en `texts` y con el resto sin
    decir nada.

    A veces el juez, sin nada que listar, deja en la lista un elemento así
    ({"code": "NONE", "constraint": "", …}) en vez de dejarla vacía. No dice
    nada: se descarta. Un elemento con algún texto sigue pasando por todas las
    validaciones.
    """
    return (
        isinstance(item, dict)
        and all(is_empty(item.get(key)) for key in texts)
        and all(says_nothing(value) for key, value in item.items() if key not in texts)
    )


def closed_object(value, keys, where):
    """Exactamente esas claves.

    A veces el juez cierra un objeto con una clave de relleno después de la
    última ("severity_note": null, "note": ""). Esa clave no dice nada: no
    cuenta. Una clave de más con contenido sigue frenando.
    """
    if not isinstance(value, dict):
        fail("%s tiene que ser un objeto JSON" % where)
    present = {key for key, item in value.items() if key in keys or not is_empty(item)}
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


# ------------------------------------------------------------------ single-answer
KINDS = ("OFFERED_BY_INSTRUCTION", "DIFFERENT_VALID")
SEVERITIES = ("BLOCKING", "HIGH", "MEDIUM")


def gaps_from_inputs():
    """Los huecos de acceptedAnswers («Hueco 1: a | b»); None si no llegó."""
    raw = inputs.get("acceptedAnswers")
    if not isinstance(raw, str):
        return None
    return {int(match.group(1)) for match in re.finditer(r"^Hueco (\d+):", raw, re.M)}


def corrector_key(text):
    """Lo que compara el corrector: minúsculas, sin espacios, signos,
    apóstrofos, comillas ni tildes. Sirve para no listar dos veces lo mismo."""
    decomposed = unicodedata.normalize("NFD", text.lower())
    plain = "".join(char for char in decomposed if not unicodedata.combining(char))
    return re.sub(r"[\s!?:;,.\-_'\"‘’“”«»`´]", "", plain)


closed_object(verdict, ("alternatives", "rationale"), "el veredicto")
alternatives = verdict["alternatives"]
if not isinstance(alternatives, list):
    fail("alternatives tiene que ser un array")
alternatives = [item for item in alternatives if not is_placeholder(item, ('answer', 'reason'))]

gaps = gaps_from_inputs()
canonical_alternatives = []
seen_answers = set()
for index, item in enumerate(alternatives):
    where = "alternatives[%d]" % index
    closed_object(item, ("answer", "gap", "kind", "severity", "reason"), where)
    answer = one_line(item["answer"], where + ".answer", 200)
    gap = item["gap"]
    if isinstance(gap, bool) or not isinstance(gap, int) or gap < 1:
        fail(where + ".gap tiene que ser el número del hueco, un entero desde 1 (Hueco 1 → 1)")
    if gaps is not None and gap not in gaps:
        fail(
            where + ".gap %d no es un hueco del ejercicio; los huecos son %s"
            % (gap, ", ".join(str(number) for number in sorted(gaps)) or "ninguno")
        )
    kind = enum_value(item["kind"], KINDS, where + ".kind")
    if upper_or_none(item["severity"]) == "LOW":
        fail(where + ".severity LOW no existe en este juez; usá BLOCKING, HIGH o MEDIUM")
    severity = enum_value(item["severity"], SEVERITIES, where + ".severity")
    key = (gap, corrector_key(answer))
    if key in seen_answers:
        fail(
            where + ": «%s» ya está listada para el hueco %d; cada respuesta va una sola vez"
            % (answer, gap)
        )
    seen_answers.add(key)
    canonical_alternatives.append({
        "answer": answer,
        "gap": gap,
        "kind": kind,
        "severity": severity,
        "reason": one_line(item["reason"], where + ".reason", 400),
    })

rationale = one_line(verdict["rationale"], "rationale", 800)

finish({"alternatives": canonical_alternatives, "rationale": rationale})
PY
