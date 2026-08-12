#!/usr/bin/env python3
"""Eval del criterio REAL_WORLD_SENSE (F-QICOR-R016) sobre el prompt real del juez.

R016 (c) y (f) son juicios del prompt y no tienen superficie deterministica en
Java, asi que se cubren aca. Sin dependencias de terceros.

Por defecto valida la forma del corpus y que `nodes/judge.md` siga conociendo el
criterio. Con ``--command`` corre el juez de verdad y lo puntua contra el corpus.

**El prompt se lee de `nodes/judge.md`, no se copia.** Es a proposito: un eval
que lleva su propia copia del prompt valida una version que ya no se usa, y la
unica manera de enterarse es que el defecto llegue al curso. Si alguien edita el
prompt y rompe el criterio, esto lo caza.

    ./run_evals.py                                  # solo estructura y drift
    ./run_evals.py --command claude -p              # corre el juez y puntua
    ./run_evals.py --command claude -p --repeat 3   # defectos intermitentes
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path
from typing import Any

AGENT_DIR = Path(__file__).resolve().parents[1]
JUDGE_NODE = AGENT_DIR / "nodes" / "judge.md"
DEFAULT_FIXTURES = Path(__file__).with_name("fixtures.json")

CRITERION = "REAL_WORLD_SENSE"
OUTCOMES = {"PASSED", "FAILED", "NOT_EVALUABLE"}
CASE_KEYS = {
    "caseId", "origin", "covers", "note", "criterion", "cefrLevel",
    "knowledgeTitle", "knowledgeInstructions", "sentenceMode",
    "originalQuizSentence", "originalTranslation",
    "candidateQuizSentence", "candidateTranslation", "expectedOutcome",
}


class ValidationError(ValueError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


# --------------------------------------------------------------- el prompt real

def load_prompt() -> tuple[str, str]:
    """Devuelve (system, user_template) leidos de nodes/judge.md."""
    raw = JUDGE_NODE.read_text(encoding="utf-8")
    body = re.sub(r"\A---\n.*?\n---\n", "", raw, count=1, flags=re.S)
    parts = re.split(r"^# User prompt template\s*$", body, maxsplit=1, flags=re.M)
    require(len(parts) == 2, "judge.md no tiene la seccion '# User prompt template'")
    head, user = parts
    head_parts = re.split(r"^# System prompt\s*$", head, maxsplit=1, flags=re.M)
    require(len(head_parts) == 2, "judge.md no tiene la seccion '# System prompt'")
    return head_parts[1].strip(), user.strip()


def validate_prompt_knows_criterion(system: str) -> None:
    """Guard de drift: el prompt tiene que seguir conociendo el criterio y sus
    dos aristas dificiles, que son justo las que este eval cubre."""
    require(CRITERION in system,
            f"judge.md ya no menciona {CRITERION}: el criterio absoluto quedaria sin juez")
    require(re.search(r"hueco ya resuelto|hueco resuelto", system),
            "judge.md ya no pide leer el candidato con el hueco resuelto (R016 lectura)")
    require(re.search(r"una sola|alcanza con que", system),
            "judge.md ya no dice que basta UNA respuesta imposible para reprobar (R016 (f))")
    require(re.search(r"trivial", system),
            "judge.md ya no aclara que lo trivial no reprueba (R016 (c))")
    require("cue" in system.lower(),
            "judge.md ya no cubre el caso del cue, que es el que motivo la regla")


# ------------------------------------------------------------------- el corpus

def validate_fixtures(fixtures: dict[str, Any]) -> list[dict[str, Any]]:
    require(fixtures.get("criterion") == CRITERION,
            f"el corpus no declara el criterio {CRITERION}")
    require(fixtures.get("rule") == "F-QICOR-R016", "el corpus no declara su regla")
    cases = fixtures.get("cases")
    require(isinstance(cases, list) and cases, "el corpus no tiene casos")

    seen: set[str] = set()
    for case in cases:
        cid = case.get("caseId", "<sin id>")
        extra = set(case) - CASE_KEYS
        require(not extra, f"{cid}: claves de mas {sorted(extra)}")
        require(not (CASE_KEYS - set(case)), f"{cid}: faltan {sorted(CASE_KEYS - set(case))}")
        require(cid not in seen, f"{cid}: caseId duplicado")
        seen.add(cid)
        require(case["criterion"] == CRITERION, f"{cid}: criterio equivocado")
        require(case["expectedOutcome"] in {"PASSED", "FAILED"},
                f"{cid}: expectedOutcome tiene que ser PASSED o FAILED")
        require(case["origin"] in {"production", "synthetic"},
                f"{cid}: origin tiene que ser production o synthetic")
        require(isinstance(case["covers"], list) and case["covers"],
                f"{cid}: covers vacio")
        for field in ("originalQuizSentence", "candidateQuizSentence",
                      "candidateTranslation", "note"):
            require(str(case[field]).strip(), f"{cid}: '{field}' vacio")
        require(case["candidateQuizSentence"] != case["originalQuizSentence"],
                f"{cid}: el candidato es identico al original, no juzga nada")

    # Composicion: sin las dos puntas el corpus no discrimina, solo confirma.
    outcomes = Counter(c["expectedOutcome"] for c in cases)
    require(outcomes["FAILED"] >= 2,
            "hacen falta al menos 2 casos que reprueben, y de moldes distintos")
    require(outcomes["PASSED"] >= 4,
            "hacen falta al menos 4 casos que pasen, o el eval premia reprobar todo")

    covered = {c for case in cases for c in case["covers"]}
    for criterion in ("R016(c)", "R016(f)"):
        require(criterion in covered,
                f"nadie cubre {criterion}, que es justo lo que este eval existe para cubrir")

    prod = sum(1 for c in cases if c["origin"] == "production")
    require(prod >= 4, "menos de 4 casos de produccion: el corpus se despega de lo real")
    return cases


# -------------------------------------------------------------------- el juez

def ask(command: list[str], system: str, user_tpl: str,
        case: dict[str, Any], timeout: float) -> tuple[str, str]:
    user = (user_tpl
            .replace("{criterion}", case["criterion"])
            .replace("{cefrLevel}", case["cefrLevel"])
            .replace("{knowledgeTitle}", case["knowledgeTitle"])
            .replace("{knowledgeInstructions}", case["knowledgeInstructions"])
            .replace("{sentenceMode}", case["sentenceMode"])
            .replace("{originalQuizSentence}", case["originalQuizSentence"])
            .replace("{originalTranslation}", case["originalTranslation"])
            .replace("{candidateQuizSentence}", case["candidateQuizSentence"])
            .replace("{candidateTranslation}", case["candidateTranslation"]))
    try:
        proc = subprocess.run(command, input=system + "\n\n=====\n\n" + user,
                              capture_output=True, text=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        return "TIMEOUT", ""
    match = re.search(r"\{.*\}", proc.stdout or "", re.S)
    if not match:
        return "NO_JSON", (proc.stdout or proc.stderr or "")[:160]
    try:
        verdict = json.loads(match.group(0))
    except json.JSONDecodeError:
        return "BAD_JSON", match.group(0)[:160]
    outcome = verdict.get("outcome", "?")
    if outcome not in OUTCOMES:
        return "BAD_OUTCOME", str(outcome)[:160]
    if verdict.get("criterion") != CRITERION:
        return "WRONG_CRITERION", str(verdict.get("criterion"))[:160]
    detail = str(verdict.get("detail") or "")
    if not detail.strip():
        return "EMPTY_DETAIL", ""
    return outcome, detail


def score(cases, command, system, user_tpl, repeat, timeout) -> int:
    from concurrent.futures import ThreadPoolExecutor

    misses = 0
    for run in range(1, repeat + 1):
        print(f"\n--- pase {run}/{repeat} " + "-" * 60)
        with ThreadPoolExecutor(max_workers=4) as pool:
            got = list(pool.map(
                lambda c: ask(command, system, user_tpl, c, timeout), cases))
        hits = 0
        for case, (outcome, detail) in zip(cases, got):
            ok = outcome == case["expectedOutcome"]
            hits += ok
            misses += not ok
            mark = "ok " if ok else "XX "
            print(f"{mark}{case['caseId']:<32} esperado={case['expectedOutcome']:<8} "
                  f"veredicto={outcome:<14} {detail[:58]}")
        print(f"    aciertos {hits}/{len(cases)}")
    return misses


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--fixtures", type=Path, default=DEFAULT_FIXTURES)
    parser.add_argument("--repeat", type=int, default=1,
                        help="pases del corpus; sirve para defectos intermitentes")
    parser.add_argument("--timeout", type=float, default=180.0)
    parser.add_argument("--command", nargs=argparse.REMAINDER, default=None,
                        help="CLI del juez; recibe el prompt por stdin (ej: --command claude -p)")
    args = parser.parse_args()

    try:
        system, user_tpl = load_prompt()
        validate_prompt_knows_criterion(system)
        cases = validate_fixtures(json.loads(args.fixtures.read_text(encoding="utf-8")))
    except ValidationError as error:
        print(f"FALLA DE ESTRUCTURA: {error}")
        return 1

    print(f"prompt: {JUDGE_NODE.relative_to(AGENT_DIR.parents[2])} conoce {CRITERION}")
    print(f"corpus: {len(cases)} casos "
          f"({sum(1 for c in cases if c['origin'] == 'production')} de produccion), "
          f"estructura OK")

    if not args.command:
        print("\n(sin --command: no se consulto al juez)")
        return 0

    misses = score(cases, args.command, system, user_tpl, max(1, args.repeat), args.timeout)
    total = len(cases) * max(1, args.repeat)
    print("\n" + "=" * 72)
    print(f"TOTAL: {total - misses}/{total} aciertos")
    return 1 if misses else 0


if __name__ == "__main__":
    sys.exit(main())
