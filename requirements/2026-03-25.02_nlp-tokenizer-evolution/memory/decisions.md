# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-11 — qa-tester — Patch FEAT-NLP: 23 AUTO_VALIDATED + 8 ASSUMPTION escaladas
  - 23 reglas AUTO_VALIDATED cubiertas con handwrittenTests directos:
    * 19 sobre SpacyNlpTokenizer (Grupos A/B/C/D/F).
    * 3 sobre CachedNlpTokenizer (Grupo E).
    * 1 sobre CourseToAuditableMapper (R010 + journey J001 cross-cutting).
  - 8 reglas con Validation: ASSUMPTION escaladas a architect (mismo patrón
    FEAT-KTLEN): R018, R021, R022, R025, R027, R030, R031, R033.
    Propuesta: rutear a analyst para mover a "Decisiones de simplicidad"
    o reformular como observables (R030/R031 podrían serlo).
  - J002 y J003 también con Validation: ASSUMPTION — pending-analyst.
  - Validador: 0 additions, 3 modifications, 0 conflicts.
  - 23/31 reglas testeables con tag directo, 8 reglas en standby pending-analyst.
