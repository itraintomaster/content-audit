# Progress — FEAT-INCEXP

2026-06-18 — analyst — Creado REQUIREMENT.md inicial (evolución aditiva de FEAT-CSLATDC). 11 reglas (R001–R011), 2 journeys de flujo (J001, J002), 3 doubts. Validación CLI: [OK] (sólo warnings de glosario compartido, sin errores).
  next: revisión del usuario sobre reglas/journeys y resolución de las 3 dudas abiertas (duplicados estáticos, default/máximo de limit, granularidad de prioridad entre ya expuestos).
2026-06-18 — developer — Producción implementada. BoundSuggestedLemmaQuerySession migrado a requestMore(SuggestedLemmaQueryCriteria). LemmaAbsenceContextSuggestedLemmaQueryPort migrado a query(SuggestedLemmaQueryCriteria) + routing a inventarios. UnderexposedLemmaInventory y LevelWideLemmaInventory implementados. GetCmd: opción --include-exposed + criteria builder. Main.java: static factory create() en el port + wiring. revision-infrastructure/DefaultSuggestedLemmaToolExchange también migrado. Build: mvn -pl refiner-domain,audit-cli -am compile VERDE.
  next: @test-writer migra tests existentes a nueva firma + implementa handwrittenTests de FEAT-INCEXP.

2026-06-18 — test-writer — TRABAJO 1 completo: migrados todos los tests existentes a SuggestedLemmaQueryCriteria (BoundSuggestedLemmaQuerySessionTest, LemmaAbsenceContextSuggestedLemmaQueryPortTest setUp + 12 tests CSLATDC, FCslatdcJ001/J002/J004JourneyTest, GetCmdTest). TRABAJO 2 completo: implementados 11 handwrittenTests INCEXP (R001–R011) en LemmaAbsenceContextSuggestedLemmaQueryPortTest + 2 journey tests (FIncexpJ001JourneyTest 3 paths, FIncexpJ002JourneyTest 1 path). Total: 310 tests, 0 failures, 0 errors.
  next: producción ya implementada — suite completa verde.
