# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-11 — qa-tester — Patch FEAT-DLABS: 6 NO_TESTS reglas cubiertas
  - LemmaByLevelAbsenceAnalyzer: 5 tests (R001/R002/R009/R011/R012).
  - LemmaAbsenceDetailedFormatter: 1 test R013 (cross-module audit-cli).
  - R001/R002 mismatch documento vs implementación: documento habla de
    "AnalyzerDiagnosis + mapa", DSL real usa NodeDiagnoses sealed + getters
    tipados. Los tests verifican el comportamiento observable de la
    implementación actual, no la terminología literal del documento.
    Si analyst quiere reformular R001/R002, los tests siguen siendo honestos.
  - Validador: 0 additions, 2 modifications, 0 conflicts.
  - 14/14 reglas con tag directo, cero transitividad.
