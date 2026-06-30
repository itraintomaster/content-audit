# Fix Log — FEAT-SLREGEX

2026-06-26 — developer — DECISIÓN FINAL: GetCmd ahora pasa Optional.ofNullable(regex) (nunca null)
  como 5to arg de SuggestedLemmaQueryCriteria (convención correcta, paridad con partOfSpeech/level).
  Consecuencia: los eq()-matchers con null en FCslatdcJ001, FCslatdcJ002, FIncexpJ001, FIncexpJ002
  fallan porque Objects.equals(null, Optional.empty()) == false. El test-writer debe cambiar
  null → Optional.empty() en esos 8 matchers (5 en FCslatdcJ001/J002, 3 en FIncexpJ001/J002).
  NO se revirtió el cableado a null para complacer los tests rotos.

2026-06-26 — test-writer — (ENTRADA ORIGINAL) CORRECCIÓN: los eq()-matchers de los journey tests
  pre-existentes (FCslatdc*/FIncexp*) deben usar null como 5to arg del criterio.
  NOTA: esta entrada ya no aplica. El developer decidió usar Optional.empty() (ver entrada arriba).
  El test-writer deberá actualizar los matchers de null → Optional.empty().

2026-06-26 — qa-tester — Diseñada la cobertura de tests (patch propuesto, NO aplicado).
  10 handwrittenTests (1 por regla):
    - R001..R009 → LemmaAbsenceContextSuggestedLemmaQueryPort (refiner-domain, pkg lemmasuggestion),
      la superficie observable de la consulta donde FEAT-CSLATDC/FEAT-INCEXP testean sus análogas.
    - R010 (semántica de --regex en --help) → GetCmd (audit-cli, pkg commands) → clase GetCmdTest.
  Placement de los 2 flow-journeys: F-SLREGEX-J001/J002 → audit-cli / com.learney.contentaudit.journeys
    (espeja FCslatdc*/FIncexp* journey tests).
  why: las reglas R001-R009 son del filtrado real (puerto de consulta); R010 es del texto de ayuda CLI.
  nota validación: los impls viven dentro de packages (lemmasuggestion / commands); el patch DEBE
    targetearlos vía modules[].packages[].implementations[], no a module root, o falla la validación.
