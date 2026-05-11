# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-11 — qa-tester — Patch FEAT-CLI: 5 NO_TESTS reglas + 2 NO_TESTS journeys cubiertos
  - AnalyzeCmd: 6 tests (R002/R003/R004 + J002/J003 cobertura combinada).
  - Main: 1 test estructural-integrador R006 (ensamblaje manual sin DI).
  - DefaultAuditRunner: 1 test R005 (contrato runAudit(Path) → AuditReport).
  - Validador: 0 additions, 3 modifications, 0 conflicts.
  - 6/6 reglas + 3/3 journeys con tag directo, cero transitividad.
  - why: AnalyzeCmd es el subcommand donde se materializan los argumentos, formato
    y exit code; Main es el ensamblaje DI manual; DefaultAuditRunner expone el
    método runAudit. Tres impls distintas, cada una con superficie observable
    propia de las reglas que les tocan.
