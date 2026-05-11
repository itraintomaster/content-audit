# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-11 — qa-tester — Patch handwrittenTests R001-R004 propuesto sobre SentenceLengthAnalyzer
  - 4 reglas NO_TESTS cubiertas con 4 tests directos sobre SentenceLengthAnalyzer
    (todos audit-domain, module root).
  - R004 (estructural: nuevo método en QuizDiagnoses) cubierto con test
    conductual end-to-end (emit→readback via la interface) en lugar de un
    test estructural reflexivo. La signature en sí está garantizada por
    compilación + DSL declarativo.
  - Patch validado: 0 additions, 1 modifications, 0 conflicts. Reportado al lead.
  - why: SentenceLengthAnalyzer es la superficie observable donde se materializa
    el ciclo emit/exclude/readback de los diagnósticos tipados. Tests sobre
    DefaultQuizDiagnoses serían triviales (record + Optional), no aportan valor.
