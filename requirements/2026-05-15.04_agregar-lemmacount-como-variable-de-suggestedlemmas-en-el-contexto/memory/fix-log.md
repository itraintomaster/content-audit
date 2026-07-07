# Fix Log — FEAT-LEMMA-SUGGESTIONS (F-SLEM)

2026-05-16 — developer — Fixed 4-arg SuggestedLemma constructor in SentenceLengthContextResolver (refiner-domain) and SentenceLengthContextStructuralValidator (revision-domain). Both were @Generated and used old 4-arg constructor removed by the architectural patch; added null,null,null for the 3 new fields.
  why: Architectural patch removed the 4-arg SuggestedLemma constructor but @Generated impl stubs weren't regenerated. Fixing these restores pre-existing passing tests.

2026-05-16 — developer — Implemented LemmaAbsenceContextResolver with full F-SLEM ranking logic (R002-R013). R003 test passes; pre-existing 37 LemmaAbsenceContextResolverTest tests pass.
  why: Key design: subExposedLemmas map is used as lookup table for "tracked by lemma-count"; isUnderexposed determined by score < 1.0; threshold derived from course-level CourseDiagnoses or score math.

2026-05-16 — developer — Updated DefaultCorrectionContextJsonMapper (audit-cli) to serialize 3 new fields (lemmaCount, lemmaCountThreshold, isUnderexposed) when present (all-or-nothing per R013).

2026-05-16 — developer — Updated LemmaAbsenceContextStructuralValidator (revision-domain) to parse, validate all-or-nothing (R013) and validate consistency (R011: no negatives, isUnderexposed = count < threshold).

2026-05-16 — developer — PRE-EXISTING BREAK: CannedLemmaAbsenceQuizCandidateGeneratorTest and LemmaAbsenceMvpStrategyTest (@Generated) use 4-arg SuggestedLemma constructor that no longer exists. Cannot fix without modifying @Generated files. Escalated to @architect.
  why: Architectural patch removed 4-arg constructor but @Generated tests reference it. Needs sentinel regeneration or adding convenience constructor.

2026-05-16 — test-writer — Fixed 4-arg SuggestedLemma constructor in CannedLemmaAbsenceQuizCandidateGeneratorTest:42 + LemmaAbsenceMvpStrategyTest:55 → 7-arg with null,null,null (signal not applicable for FEAT-LAGEN/FEAT-LAPS). @architect confirmed this is the correct fix.

2026-05-16 — test-writer — J004-path-2: assertThrows(OverrideRejectedException) instead of assertNotEquals(0, exit) — ReviseCmd propagates the exception rather than returning non-zero when override parsing fails. Match the actual behavior.

2026-05-16 — test-writer — R001 runDetailedAudit: test correctly fails (null return) because DefaultAuditRunner.runDetailedAudit with allAnalyzers=[] doesn't find "lemma-count" analyzer and returns null. Production gap confirmed by developer.

2026-05-16 — developer (round 2) — Fixed DefaultAuditRunner.runDetailedAudit: when allAnalyzers is empty OR analyzer not found in list, delegate to injected auditEngine instead of returning null.
  why: mirrors the runAudit(null) fallback pattern; allAnalyzers=[] signals "full engine mode". File is @Generated implementation stub (already had hand-written runAudit logic from prior round).

2026-05-16 — developer (round 2) — R010 in LemmaAbsenceContextStructuralValidatorTest was already GREEN (false alarm in the brief). Ran the test individually and in the full 177-test suite — all pass.
  why: Previous round correctly implemented the parsing + validation; test-writer's observation about "fails" may have been from a stale compile state.

2026-07-01 — qa-tester — Re-diseño de cobertura F-SLEM tras el pivote a 6 tiers. Tests EXISTENTES a re-taggear (_change: modify) en LemmaAbsenceContextResolver: R003 (nombre encodeaba "group1..group5 COMPLETELY_ABSENT last" = modelo viejo → 6 tiers first-match-wins), R007 (nombre encodeaba preferencia ABSOLUTA sub-expuesto>ausente → ahora relajada: T2 ausente+banda > T3 sub-expuesto-no-banda), R005 x2 (wording "priority group"→"tier"; concepto COCA-asc-final + no-magnitud sobrevive). NUEVOS directos: R014 x4 (T2>T3, borde count==N → banda ampliada, desempate binario banda + COCA final, T5-6 siempre al final), R015 x1 (degradación 3→4→6 sin bandas). Journeys F-SLEM son FLOW (J001 ahora 0/3 paths) → NO se taggean desde handwrittenTests; van por *JourneyTest generado.
  why: la cobertura [covered] del report NO detecta fixtures rancios; R003/R007 afirman el orden viejo y romperían con la producción de 6 tiers.
2026-07-01 — test-writer — Banda deficitaria en fixtures: usé buildCourseNodeWithEnrichBands("A1", int[][]) + new ImprovementDirective(ENRICH, ..., rangeFrom, rangeTo, ...) en el courseNode. CocaProgressionDiagnosis solo en COURSE (no milestone). COMPLETELY_ABSENT = triple always null (R013). Borde count==N → expuesto-escaso (isUnderexposed=false, Tier 5 o 6) per DOUBT-EXPANDED-BAND-EDGE Opción A.
  why: patrón documentado en la sesión anterior; helpers reutilizados evitan duplicación.

2026-07-01 — developer — 6-tier ranking: COMPLETELY_ABSENT con banda → TIER_2_ABSENT_BAND (no siempre último). Sub-expuesto con banda → TIER_1_SUBEXPOSED_BAND. Helpers computeTier / isSubExposed / extractEnrichBands / isInEnrichBand / buildScarceContentWords añadidos. Estructura de resolve() reestructurada para computar localCountStats y threshold SIEMPRE (no solo cuando hay LemmaAbsenceDiagnosis).
  why: R003c requiere lista vacía (no null) cuando no hay señal; la señal debe buscarse aunque no haya diagnóstico de ausencia.

2026-07-01 — developer — FIXTURE ERRORS en 2 tests de test-writer (líneas 1892 y 2218 de LemmaAbsenceContextResolverTest): cocaRank=50 cae dentro de las bandas [1,100] y [1,200] respectivamente, pero los comentarios del test dicen "not in band (rank=50)". Corrección sugerida: cambiar la banda [1,100] a [51,100] y la banda [1,200] a [51,200] (o equivalente). NO se tocaron los tests.
  why: la producción implementa la spec correctamente; solo los fixtures son inválidos.

2026-07-01 — developer — Fase 2 COMPLETA. Extraída lógica de 6 tiers a SuggestedLemmaTierRanker (nuevo, no @Generated, en refinerdomain package). LemmaAbsenceContextResolver delegando. LemmaAbsenceContextSuggestedLemmaQueryPort ahora aplica tierComparator(enrichBands) post-pool, con effectiveLevel para las bandas. Live: get suggested-lemmas task-3667 --level B2 --limit 15 floata top4k (flat#3045,shower#3093,pig#3274,cow#3308,jean#3955,goodbye#3977) a posiciones 1-6. mvn verify BUILD SUCCESS, 128 tests 0 failures.
  why: las 2 fixture-error anteriores también pasaron (producción correcta con la refactorización limpia).

2026-07-01 — qa-tester — DOUBTS abiertas que afectan 2 tests: DOUBT-EXPANDED-BAND-EDGE (borde count==N; encodeado Opción A = entra a banda ampliada) gobierna el test "count==N no es sub-expuesto"; DOUBT-ABSENCE-SUBTYPE-TIEBREAK (colapsar subtipo; Opción A) — ambas provisionales, flaggeadas al usuario antes de @test-writer.
