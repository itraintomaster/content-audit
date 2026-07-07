# Progress — FEAT-LEMMA-SUGGESTIONS (F-SLEM)

2026-05-16 — analyst — Closed all four open doubts (DOUBT-SUGGESTED-LEMMA-FIELDS, DOUBT-LEMMA-COUNT-UNAVAILABLE, DOUBT-COMPLETELY-ABSENT-EXPOSURE, DOUBT-ERROR-TEXT). Added rule F-SLEM-R013 to formalize the observable shape of the three lemma-count values inside suggestedLemmas. Updated R007 and R012 to explicitly state the "three values not informed" contract. Updated R010 to reference R013. Added R013 to journey gates J001/assert_ranked_suggestions, J001/assert_suggestions_without_signal, J004/accept_override. Validator: OK.
  why: test-writer needs a deterministic observable contract for R007 and R012 before implementing.

2026-05-16 — qa-tester — Proposed 3 handwrittenTests for F-SLEM-R013 (the only gap after analyst's R013 round): 2 on LemmaAbsenceContextResolver (system-derived all-or-nothing + informed-triple consistency) and 1 on LemmaAbsenceContextStructuralValidator (override rejection on partial triple). Patch validated: 0 additions, 2 modifications, 0 conflicts. Not applied (Rule E).
  why: R013 has two distinct enforcement surfaces (system emission + external-override parsing); transitive coverage via J001/J004 gates is not enough per feedback_no_transitive_coverage.

2026-05-16 — developer — Loaded all contracts and read all 3 module AGENTS.md. Context complete. In standby until @test-writer pings with first failing test (R003 — ranking por grupos).
  why: Coordinator instruction to wait for TDD handoff from test-writer before implementing production.

2026-05-16 — developer — PRE-EXISTING BREAK DETECTED: SentenceLengthContextResolver.java (@Generated) and LemmaAbsenceContextResolver.java (@Generated) still use 4-arg SuggestedLemma constructor, but the architectural patch updated SuggestedLemma to only have 7-arg constructor. This causes runtime errors in SentenceLengthContextResolverTest (72 errors). Will fix LemmaAbsenceContextResolver as part of F-SLEM implementation; SentenceLengthContextResolver break needs architect review (it's @Generated and should not be touched by developer).
  why: SuggestedLemma model changed but generated impl stubs weren't re-generated; must update as part of production impl.

2026-05-16 — test-writer — Implemented R003 stub (shouldRankSuggestedLemmasIn...) on LemmaAbsenceContextResolverTest. Compiles OK. Fails as expected (production returns 3 lemmas; test expects 5 covering all priority groups). Added lemmacount imports to test file. Fixture: 2-milestone tree (A1 quiz level + A2 for APPEARS_TOO_EARLY target-level evaluation), threshold=3, shuffled COCA ranks across groups to prove group priority overrides COCA.
  next: implement R007 → R005 (x2) → R006 → R004 → R002 (x2) → R013 (x2) → R012

2026-05-16 — developer (round 2) — Closed 2 remaining production gaps:
  Gap 1 (R010): LemmaAbsenceContextStructuralValidatorTest#R010 was already GREEN — validator impl was correct. Test passes individually and in the full 177-test suite.
  Gap 2 (R001 runDetailedAudit): Fixed DefaultAuditRunner.runDetailedAudit to fall back to injected auditEngine when allAnalyzers is empty or analyzer not found, instead of returning null. DefaultAuditRunnerTest: 11/11 pass.
  why: allAnalyzers=[] means "use the injected engine" — same pattern as runAudit(null/empty) per architect's documented description.

2026-05-16 — test-writer — BATCH COMPLETE. Implemented ALL 21 stubs:
  - LemmaAbsenceContextResolverTest: R002(x2), R003, R004, R005(x2), R006, R007, R012, R013(x2) — all pass
  - LemmaAbsenceContextStructuralValidatorTest: R010, R011(x2), R013 — R010 FAILS (production not yet accepting valid triple)
  - DefaultAuditRunnerTest: R001(x2) — runAudit PASSES; runDetailedAudit FAILS (returns null, production gap)
  - PlanCmdTest: R008 — PASSES
  - ReviseCmdTest: R009 — PASSES
  - FSlemJ001JourneyTest: path-1, path-2 — both PASS
  - FSlemJ002JourneyTest: path-1 — PASSES
  - FSlemJ003JourneyTest: path-1 — PASSES
  - FSlemJ004JourneyTest: path-1, path-2 — both PASS
  Also fixed 4-arg SuggestedLemma constructor in CannedLemmaAbsenceQuizCandidateGeneratorTest + LemmaAbsenceMvpStrategyTest.
  Remaining failing: LemmaAbsenceContextStructuralValidatorTest#R010 + DefaultAuditRunnerTest#runDetailedAuditR001 (production gaps, correct TDD).

2026-07-01 — analyst — Refinamiento ADITIVO del REQUIREMENT (desempate por bandas de frecuencia COCA deficitarias del nivel). Solo se editó REQUIREMENT.md (NO sync, NO patch, NO lock). Nuevas reglas F-SLEM-R014 y F-SLEM-R015; enmendada F-SLEM-R005; ampliado Scope IN/OUT, TL;DR, Context, References. Journey tocado: F-SLEM-J001 (ahora 3 ramas / más paths; gate R014 en assert_ranked_suggestions, gate R015 en la nueva rama assert_ranked_suggestions_coca_only). J002/J003/J004 SIN cambios. Reglas y journeys existentes intactos y sin renumerar. Doubts abiertas: DOUBT-BAND-DEFICIT-MARKER (recomendación Opción B: no agregar campo). Validate read-only: [OK].
  next: pendiente `sentinel feature sync` (lo corre el usuario/architect, NO el analyst). Luego re-diseño de tests: @qa-tester debe proponer handwrittenTests directos para R014 y R015 (feedback_no_transitive_coverage). J001 pasó de linear/2-ramas a 3-ramas → nuevos paths en FSlemJ001JourneyTest al regenerar.

2026-07-01 — test-writer — Completado el re-diseño de tests en LemmaAbsenceContextResolverTest (clase hand-written, 57 tests, 9 fallan = TDD esperado):
  ELIMINADOS: 4 métodos obsoletos (modelo 5-grupos — R003 viejo x1, R005 viejo x2, R007 viejo x1).
  CREADOS + IMPLEMENTADOS (B): 3 nuevos (R005 x2 wording "tier", R007 relajada T2>T3 fixture banda).
  IMPLEMENTADOS (C): 5 stubs R014/R015 (T2>T3, count==N→T5/6, banda binaria, T5/6 siempre al final, degradación 3→4→6).
  IMPLEMENTADOS (D): 4 stubs R003b/R003c (filtro count<N, campos lemma/pos/count/threshold, vacío sin señal, vacío ninguno scarce).
  Todos compilan. Los 9 fallos son fallos de producción (código no actualizado), no errores de fixture.
  Importación añadida: NlpToken.
  Helpers reutilizados: buildCourseNodeWithEnrichBands, buildMilestoneNode/TopicNode/KnowledgeNode/QuizNode.
  next: @developer debe actualizar LemmaAbsenceContextResolver para 6-tier ranking + scarceContentWords.

2026-07-01 — developer — Implementación Fase 2 completa en LemmaAbsenceContextResolver: 6-tier ranking (R003/R014/R015) + scarceContentWords (R003b/R003c). 128 tests, 126 pasan. 2 fallan por errores de fixture en los tests del @test-writer (bandas [1,100] y [1,200] incluyen cocaRank=50 que se documenta como "not in band"). Escalado al test-writer para corrección de fixture.
  why: la producción es correcta per spec; los fixtures contradicen los comentarios de los tests.

2026-07-01 — analyst — PIVOTE del ranking a SEIS TIERS (banda deficitaria = clave secundaria fuerte, no desempate). SUPERSEDES el modelo anterior de "5 grupos R003 intactos". Solo REQUIREMENT.md editado (NO sync, NO patch, NO lock). Reescritas R003 (tabla de 6 tiers) y R014 (def formal + cortes por N + una sola función de prioridad sin branch de banda). Enmendadas R005 (COCA=desempate final DENTRO del tier), R007 (relajación: ausente detrás del sub-expuesto solo dentro del mismo estado de banda; T2>T3 deliberado), R012 y R015 (degradación = misma función con tiers de banda vacíos → 3→4→6). R004/R013 INTACTAS. Actualizados TL;DR, Context (nueva subsección "Esquema de seis tiers" + relación includeExposed), Scope IN/OUT, References (+FEAT-INCEXP). J001 assert nodes reescritos (tiers; estructura/gates iguales). Dos DOUBTS nuevas ABIERTAS: DOUBT-ABSENCE-SUBTYPE-TIEBREAK y DOUBT-EXPANDED-BAND-EDGE (ambas encodeadas Opción A provisional). Validate read-only: [OK] (solo WARN de glosario preexistentes).
  next: usuario confirma las 2 doubts (subtipo colapsado sí/no; borde count==N). Luego `sentinel feature sync` (usuario/architect). Re-diseño de tests: los tests de R003/R005/R007 que fijaban el orden por "5 grupos" van a romper con el orden por tiers (esp. el fixture de R003 con COCA shuffled) → @qa-tester + @test-writer deben re-derivar fixtures al orden de 6 tiers, y agregar cobertura para el invariante T2>T3 (ausente-deficitario > sub-expuesto-no-deficitario) y para el borde count==N.

2026-07-01 — test-writer — Implementado FSlemJ001JourneyTest#path3 (rama "lemma-count sin bandas"): sub-expuesto "run" (tier 3) antes de COMPLETELY_ABSENT "jump" (tier 4); triple informado para "run", null para "jump". Gates R003/R005/R006/R013/R015. BUILD: 325/325 PASS.
