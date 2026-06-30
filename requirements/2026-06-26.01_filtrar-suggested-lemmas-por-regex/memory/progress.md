# Progress — FEAT-SLREGEX (Filtrar Suggested Lemmas por regex)

2026-06-26 — analyst — REQUIREMENT.md inicial creado y validado.
  Estado: borrador completo, 10 reglas, 2 journeys (flow), 2 dudas abiertas.
  Siguiente paso: revisión del usuario; luego arquitectura (@architect) para
  agregar el filtro --regex al criterio de consulta de suggested-lemmas.

2026-06-26 — test-writer — PART A (migration) + PART B (test bodies) COMPLETADOS.
  PART A: todos los constructores 4-arg migrados a 5-arg en 10 archivos de test.
    - refiner-domain: LemmaAbsenceContextSuggestedLemmaQueryPortTest, DefaultSuggestedLemmaQuerySessionFactoryTest,
      BoundSuggestedLemmaQuerySessionTest.
    - revision-infrastructure: DefaultSuggestedLemmaAgentToolTest.
    - audit-cli: FCslatdcJ001, FCslatdcJ002, FCslatdcJ004, FCslatdcJ005, FIncexpJ001, FIncexpJ002, GetCmdTest.
  PART B:
    - R001–R009 implementados en LemmaAbsenceContextSuggestedLemmaQueryPortTest (refiner-domain). TDD: R001 GREEN, R002–R009 RED.
    - R010 implementado en GetCmdTest (audit-cli). RED (--regex aún no agregado a GetCmd).
    - FSlregexJ001JourneyTest (paths 1–3) implementado. RED: path1 y path2 RED, path3 GREEN.
    - FSlregexJ002JourneyTest (path1) implementado. RED.
  Estado: PART A + PART B completados. 315 tests pre-existentes audit-cli GREEN. 110 tests refiner-domain GREEN.
  Siguiente paso: @developer implementa producción (filtro regex en QueryPort + --regex option en GetCmd).

2026-06-26 — developer — Implementación de producción COMPLETA. R001–R009 GREEN (9/9). audit-cli compila.
  Archivos modificados:
    - refiner-domain/.../lemmasuggestion/LemmaAbsenceContextSuggestedLemmaQueryPort.java: regex filter + imports
    - audit-cli/.../commands/GetCmd.java: --regex option, Optional.ofNullable wiring en call() y handleSuggestedLemmas, ejemplo en footer
  8 journey-tests fallan por matchers `null` vs `Optional.empty()`: ver fix-log.md.
  Siguiente paso: @test-writer corrige los 8 matchers en FCslatdcJ001, FCslatdcJ002, FIncexpJ001, FIncexpJ002.
