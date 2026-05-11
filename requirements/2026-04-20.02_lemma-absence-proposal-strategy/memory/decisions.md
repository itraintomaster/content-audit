# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-11 — qa-tester — Patch FEAT-LAPS: 7 NO_TESTS reglas cubiertas
  - 7 handwrittenTests sobre LemmaAbsenceMvpStrategy (R007/R008/R009/R010/R011/R017/R018).
  - 11 reglas ya PASSING (R001/R002/R003/R005/R006/R012-R016/R019). No tocadas.
  - 1 regla FAILING (R004): 1 test real failing en DefaultProposalStrategySelector
    (audit-cli) sobre default cuando CONTENT_AUDIT_LAPS_STRATEGY unset.
    NO mi lane — deuda de developer/test-writer. Flagged al lead.
  - Validador: 0 additions, 1 modifications, 0 conflicts.
  - 19/19 reglas con tag directo, 3/3 journeys ya PASSING.
  - Zona gris (R010/R017/R018 minor + describen ausencia de comportamiento):
    testables pero frágiles. Si test-writer reporta inconsistent_traceability,
    reformular como Bucket 3 (retirar) o dejar.
  - why: el lead esperaba "12 falsos FAILING" pero el estado real es 1 sola
    regla FAILING con 1 test específico real fallando. Diagnóstico ajustado.

2026-05-11 — qa-tester — Rename de handwrittenTest R004 (mvp → llm) via delete+add
  - DefaultProposalStrategySelector.handwrittenTests tenía: "...default strategy name 'lemma-absence-mvp'"
  - DEFAULT_STRATEGY real en código: "lemma-absence-llm" (renombrado por F-LAGEN-R001).
  - Test .java aserta "lemma-absence-llm" y pasa; sentinel-report marcaba failing por
    desalineamiento del nombre del yaml vs .java.
  - Patch: _change: delete del nombre viejo + add del nombre nuevo. Manejo de rename
    sin primitive renameTo: en el DSL (el merger matchea por name).
  - Validador: 0 additions, 1 modifications, 0 conflicts.
  - Post-apply esperado: R004 pasa de FAILING a PASSING (8/8 tests).
