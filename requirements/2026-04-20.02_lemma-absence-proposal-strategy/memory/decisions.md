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

2026-07-29 — analyst — R013 y R014 reformuladas (ids conservados) como caso particular de F-RPRES-R001 (requirements/2026-07-29.01_preservacion-del-contenido-no-revisado).
  why: entre las dos autorizaban formalmente un dano real (2658 quizzes revisados perdieron tipo de formulario, peso, etiquetas y el texto vacio del hueco). El agujero estaba en el 2do item de R013: "estructura del ejercicio (quizForm)" se leia como el formulario completo. Ahora R013 dice "composicion del enunciado" (partes TEXT/CLOZE + respuesta + variantes) y delimita explicito que atributos NO entran; R014 pasa de lista enumerada a complemento ("todo lo que no esta en R013 se preserva"), con la lista vieja degradada a ejemplos.
  ojo: el cambio ACOTA lo permitido, no amplia — los tests vigentes tagueados R013/R014 (DefaultLemmaAbsenceProposalDeriverTest, DefaultImpactPreviewComputerTest, FLapsJ001/J002JourneyTest) siguen valiendo sin retag. Se conservo explicita la estabilidad de identidad del elemento y la exclusion de propuestas estructurales, porque FEAT-PIPRE y FEAT-CDIFF heredan de R014 justamente eso. Ningun id cambio; hace falta `sentinel feature sync`.
