#!/usr/bin/env python3
"""Resume un informe de `analyze` en estadísticas de curso y de nivel, o compara dos.

Lee el JSON que deja `analyze` en .content-audit/audits/ y saca, por nivel CEFR:

  - puntaje de cada analizador
  - largo de oración: ejercicios medidos, tokens promedio, dentro del rango,
    cortas y largas (y cuántas se pasan de la tolerancia)
  - bandas COCA: % de tokens en top1k y en top4k contra su objetivo, y el puntaje
    de cada cuarto del nivel
  - lemas ausentes: esperados, ausentes (del todo, tarde, temprano) y por
    prioridad; ejercicios con palabras de otro nivel o fuera de catálogo
  - conteo de lemas: lemas distintos y cuántos aparecen en menos oraciones que el umbral

y, a nivel curso, la recurrencia de lemas (sólo existe como puntaje de curso).

Uso:
  python3 scripts/audit_stats.py <audit.json>                  # tabla
  python3 scripts/audit_stats.py <audit.json> --json           # JSON
  python3 scripts/audit_stats.py <antes.json> <despues.json>   # antes, después y diferencia
"""

import argparse
import json
import statistics
import sys
from collections import Counter, OrderedDict

ANALYZERS = ("sentence-length", "coca-buckets-distribution", "lemma-absence", "lemma-count",
             "lemma-recurrence", "knowledge-title-length", "knowledge-instructions-length",
             "quiz-instruction")


def walk(node, target):
    if node.get("target") == target:
        yield node
    for child in node.get("children") or []:
        yield from walk(child, target)


def level_stats(milestone):
    diag = milestone.get("diagnoses") or {}
    la = diag.get("lemmaAbsenceDiagnosis") or {}
    level = la.get("level") or (milestone.get("entity") or {}).get("label") or "?"
    out = OrderedDict(level=level)
    out["scores"] = OrderedDict((a, milestone["scores"][a]) for a in ANALYZERS if a in milestone.get("scores", {}))

    quizzes = list(walk(milestone, "QUIZ"))
    knowledges = list(walk(milestone, "KNOWLEDGE"))
    out["knowledges"] = len(knowledges)
    out["quizzes"] = len(quizzes)

    lengths = [((q.get("diagnoses") or {}).get("sentenceLengthDiagnosis")) for q in quizzes]
    lengths = [d for d in lengths if d]
    if lengths:
        tokens = [d["tokenCount"] for d in lengths]
        out["sentenceLength"] = OrderedDict([
            ("measured", len(lengths)),
            ("targetRange", [lengths[0]["targetMin"], lengths[0]["targetMax"]]),
            ("meanTokens", round(statistics.mean(tokens), 2)),
            ("medianTokens", statistics.median(tokens)),
            ("inRange", sum(1 for d in lengths if d["targetMin"] <= d["tokenCount"] <= d["targetMax"])),
            ("tooShort", sum(1 for d in lengths if d["tokenCount"] < d["targetMin"])),
            ("tooLong", sum(1 for d in lengths if d["tokenCount"] > d["targetMax"])),
            ("beyondTolerance", sum(1 for d in lengths if d["delta"] >= d["toleranceMargin"])),
        ])

    coca = diag.get("cocaBucketsDiagnosis") or {}
    if coca:
        out["coca"] = OrderedDict([
            ("totalTokens", coca.get("totalTokens")),
            ("bands", [OrderedDict([("band", b["bandName"]), ("pct", round(b["percentage"], 1)),
                                    ("target", round(b["targetPercentage"], 1)), ("assessment", b["assessment"])])
                       for b in coca.get("buckets") or []]),
            ("quarterScores", [round(q["score"], 3) for q in coca.get("quarters") or []]),
        ])

    if la:
        types = Counter(a["absenceType"] for a in la.get("absentLemmas") or [])
        quiz_la = [((q.get("diagnoses") or {}).get("lemmaAbsenceDiagnosis") or {}) for q in quizzes]
        out["lemmaAbsence"] = OrderedDict([
            ("expected", la.get("totalExpected")),
            ("absent", la.get("totalAbsent")),
            ("absentPct", round(la.get("absencePercentage") or 0, 1)),
            ("coverageTarget", la.get("coverageTarget")),
            ("listedByType", OrderedDict(sorted(types.items()))),
            ("priority", OrderedDict([("high", la.get("highPriorityCount")), ("medium", la.get("mediumPriorityCount")),
                                      ("low", la.get("lowPriorityCount"))])),
            ("quizzesWithMisplaced", sum(1 for d in quiz_la if d.get("misplacedLemmaCount"))),
            ("quizzesWithOutOfCatalog", sum(1 for d in quiz_la if d.get("outOfCatalogWordCount"))),
        ])

    lc = (diag.get("lemmaCountDiagnosis") or {}).get("levelResult") or {}
    if lc:
        out["lemmaCount"] = OrderedDict([("lemmas", lc.get("totalLemmas")),
                                         ("subExposed", len(lc.get("subExposedLemmas") or []))])

    title = [k["scores"].get("knowledge-title-length") for k in knowledges]
    instr = [k["scores"].get("knowledge-instructions-length") for k in knowledges]
    out["knowledgesTitleBelow1"] = sum(1 for s in title if s is not None and s < 1)
    out["knowledgesInstructionsBelow1"] = sum(1 for s in instr if s is not None and s < 1)
    return out


def summarize(path):
    with open(path, encoding="utf-8") as f:
        root = json.load(f)["root"]
    course = OrderedDict()
    course["scores"] = OrderedDict((k, v) for k, v in root.get("scores", {}).items())
    course["quizzes"] = sum(1 for _ in walk(root, "QUIZ"))
    course["knowledges"] = sum(1 for _ in walk(root, "KNOWLEDGE"))
    coca = (root.get("diagnoses") or {}).get("cocaBucketsDiagnosis") or {}
    course["cocaProgression"] = [OrderedDict([("band", p["bandName"]), ("actual", p["actualProgression"]),
                                              ("expected", p["expectedProgression"])])
                                 for p in coca.get("progressionAssessments") or []]
    return OrderedDict([("audit", path), ("course", course), ("levels", [level_stats(m) for m in root["children"]])])


def pct(value):
    return "—" if value is None else "%.1f%%" % (value * 100)


def rows(summary):
    """Filas (etiqueta, valor numérico) planas, para imprimir y para comparar."""
    out = [("curso · " + k, v) for k, v in summary["course"]["scores"].items()]
    out.append(("curso · ejercicios", summary["course"]["quizzes"]))
    out.append(("curso · knowledges", summary["course"]["knowledges"]))
    for lvl in summary["levels"]:
        name = lvl["level"]
        out += [("%s · %s" % (name, k), v) for k, v in lvl["scores"].items()]
        out += [("%s · ejercicios" % name, lvl["quizzes"]), ("%s · knowledges" % name, lvl["knowledges"])]
        sl = lvl.get("sentenceLength")
        if sl:
            out += [("%s · largo: tokens promedio" % name, sl["meanTokens"]),
                    ("%s · largo: dentro del rango %s" % (name, sl["targetRange"]), sl["inRange"]),
                    ("%s · largo: cortas" % name, sl["tooShort"]),
                    ("%s · largo: largas" % name, sl["tooLong"]),
                    ("%s · largo: fuera de la tolerancia" % name, sl["beyondTolerance"])]
        coca = lvl.get("coca")
        if coca:
            for band in coca["bands"]:
                out.append(("%s · COCA %s %% (objetivo %s)" % (name, band["band"], band["target"]), band["pct"]))
        la = lvl.get("lemmaAbsence")
        if la:
            out += [("%s · lemas esperados" % name, la["expected"]), ("%s · lemas ausentes" % name, la["absent"]),
                    ("%s · ejercicios con palabras de otro nivel" % name, la["quizzesWithMisplaced"]),
                    ("%s · ejercicios con palabras fuera de catálogo" % name, la["quizzesWithOutOfCatalog"])]
        lc = lvl.get("lemmaCount")
        if lc:
            out += [("%s · lemas distintos" % name, lc["lemmas"]),
                    ("%s · lemas subexpuestos" % name, lc["subExposed"])]
    return out


def fmt(label, value):
    if isinstance(value, float) and value <= 1.0 and "·" in label and not any(
            w in label for w in ("promedio", "COCA")):
        return pct(value)
    return str(value)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("audits", nargs="+", help="uno o dos informes de analyze")
    p.add_argument("--json", action="store_true", help="imprime el resumen en JSON")
    args = p.parse_args()
    if len(args.audits) > 2:
        sys.exit("Se comparan como mucho dos informes.")

    summaries = [summarize(a) for a in args.audits]
    if args.json:
        print(json.dumps(summaries if len(summaries) > 1 else summaries[0], ensure_ascii=False, indent=2))
        return
    if len(summaries) == 1:
        for label, value in rows(summaries[0]):
            print("%-58s %s" % (label, fmt(label, value)))
        return
    before, after = (dict(rows(s)) for s in summaries)
    print("%-58s %12s %12s %10s" % ("", "antes", "después", "dif."))
    for label, _ in rows(summaries[1]):
        a, b = before.get(label), after.get(label)
        diff = "" if a is None or b is None else ("%+.1f pts" % ((b - a) * 100) if fmt(label, b).endswith("%")
                                                  else "%+g" % round(b - a, 2))
        print("%-58s %12s %12s %10s" % (label, "—" if a is None else fmt(label, a), fmt(label, b), diff))


if __name__ == "__main__":
    main()
