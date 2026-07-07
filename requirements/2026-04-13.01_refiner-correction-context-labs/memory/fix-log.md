# Fix Log

2026-04-19 — test-writer — used setter-based AuditNode construction (not constructor-based) for J001 journey tests.
  why: LemmaAbsenceContextResolverTest already uses setters; the constructor in FRcslJ001JourneyTest only works if parent refs are separately wired, and the constructor-based approach is less flexible. Setter approach is the canonical pattern in this codebase.

2026-04-19 — test-writer — J001 path-2 used APPEARS_TOO_EARLY-only milestone diagnosis to trigger empty suggestedLemmas.
  why: R004b excludes APPEARS_TOO_EARLY; an empty absent-lemma list or a null diagnosis also works, but APPEARS_TOO_EARLY exercises the filtering logic more precisely.

2026-04-19 — test-writer — J002 ran with -Dsurefire.failIfNoSpecifiedTests=false to avoid failure in upstream modules.
  why: -pl audit-cli -am applies the test pattern to all modules; upstream modules (audit-domain) have no matching tests.

2026-07-01 — qa-tester — Cobertura directa scarceContentWords (Fase 2). R003b (2 tests) + R003c (2 tests) en LemmaAbsenceContextResolver (es quien estampa scarceContentWords, ver desc del campo en sentinel.yaml): inclusión count<threshold / exclusión count>=threshold + forma {lemma,pos,count,threshold}; lista vacía en los 3 casos de degradación (sin lemma-count / sin palabras bajo umbral / sin content-words). R008 (1 test) + R009 (2 tests) en GetCmd (superficie observable JSON/texto): scarceContentWords siempre presente en JSON ([] si vacía); sección "Scarce content words (do not remove)" en texto y "(none)" si vacía. Los ~11 tests R008/R009 previos NO tocan scarceContentWords → se agregan nuevos, no _change:modify (cada uno cubre un campo distinto; contaminar los viejos sería re-scoping deshonesto).
  why: memoria feedback_no_transitive_coverage — cada regla nueva exige test directo; el render de scarceContentWords no está cubierto por los tests de misplacedLemmas/suggestedLemmas.
