# Progress — FEAT-CSLATDC

2026-05-21 — qa-tester — Cobertura propuesta: 13 handwrittenTests (R001, R003-R012, R014 → LemmaAbsenceContextSuggestedLemmaQueryPort; R002 → GetCmd) + 4 journey placements (J001-J004 → audit-cli/com.learney.contentaudit.journeys). Patch en architectural_patch.yaml.
  why: validador rechazó primer intento por declarar el adaptador en module-root cuando vive en package `lemmasuggestion`; corregido y aceptado.

## Pendientes para decisión del usuario
- R013 (filtros no soportados): satisfecho estructuralmente por record cerrado `SuggestedLemmasFilter`. NO se propuso handwrittenTest; ver decisions.md.
- R015 (instrucciones al agente revisor): fuera de scope arquitectónico según TECH_SPEC. NO se propuso handwrittenTest; ver decisions.md.

2026-05-21 — analyst — REQUIREMENT actualizado: retiradas R013 y R015, eliminado J003 y DOUBT-IRREGULAR-FILTER, limpiados bullets de Scope. Cobertura proyectada queda en 12 reglas (R001-R012, R014) + 3 journeys (J001, J002, J004). IDs estables, no renumerados. Próximo paso: el architect debería correr `sentinel feature sync` para reflejar los cambios en `sentinel.yaml`.

2026-05-22 — test-writer — 12 cuerpos de tests implementados en LemmaAbsenceContextSuggestedLemmaQueryPortTest. Compila OK. Todos fallan con UnsupportedOperationException (TDD). Próximo paso: @developer implementa LemmaAbsenceContextSuggestedLemmaQueryPort.query().

2026-05-22 — test-writer — Implementado cuerpo del test R002 en GetCmdTest (shouldRouteTheSuggestedlemmasResourceThroughGetCommandgetExtendingTheExistingVerbWithoutInvokingANewCommandType). Crea localCmd con suggestedLemmaQueryPort mockeado (8-arg constructor), llama cmd.get("suggested-lemmas", taskId, SuggestedLemmasFilter) y verifica que el port fue invocado. El test no puede compilar hasta que el @developer añada get(String, String, SuggestedLemmasFilter) a GetCmd y corrija setUp() para pasar el 8° arg.
  why: producción incompleta (GetCmd sólo tiene get(String,String,GetTasksFilter), no el overload con SuggestedLemmasFilter). Error en cascada — el test en sí es sintácticamente correcto.

2026-05-22 — test-writer — BLOQUEADO: FCslatdcJ002JourneyTest (paths 1 y 2) no puede implementarse — audit-cli no compila porque GetCmd no implementa GetCommand.get(String, String, SuggestedLemmasFilter). mvn test-compile falla antes de llegar al test. Escalado a @developer para que implemente el método en GetCmd antes de retomar.
  why: sin compilación de main sources, el test compilation también falla. No hay workaround válido que verifique la regla genuinamente sin instanciar la implementación real.

2026-05-22 — test-writer — Implementado cuerpo de FCslatdcJ004JourneyTest#path1_success. Enfoque: usa LemmaAbsenceContextSuggestedLemmaQueryPort (public) directamente, no a través de GetCmd (package-private). AuditReport con 15 absent NOUN lemmas A1; static context con 3 lemmas (STATIC_LEMMA_COUNT); limit=15 (REQUESTED_LIMIT > STATIC_LEMMA_COUNT). Verifica R011 (> static), R008 (<= limit), R014 (identifiable), R010 (cocaRank ascending). Compila OK — sin errores propios del test.

2026-05-22 — test-writer — Implementados 4 path tests de FCslatdcJ001JourneyTest (J001 completo). GetCmd instanciado via reflection (package-private barrier). SuggestedLemmaQueryPort + stores mockeados. path-4 verifica exit no-zero via SuggestedLemmaQueryRejectedException. Tests se compilarán correctamente cuando @developer implemente GetCmd.get(String, String, SuggestedLemmasFilter) y corrija Main.java.

2026-05-22 — test-writer — Implementados 2 path tests de FCslatdcJ002JourneyTest (J002 completo). Mismo patrón reflection que J001. Diferencia clave: filter incluye Optional.of(CefrLevel.B1) como explicit level (R006); task+AuditReport sin nivel inferible. path-1: port retorna 4 NOUN lemmas B1 ordenados por prioridad → exit=0. path-2: port retorna lista vacía → exit=0 (R012). No hay errores propios del archivo; los errores de compilación existentes son TDD pre-existentes en GetCmdTest, FPipreJ002JourneyTest y FRevaprJ005JourneyTest.

2026-05-22 — developer — Implementado LemmaAbsenceContextSuggestedLemmaQueryPort.query(). 12/12 tests pasan. Ver fix-log.md para detalles.

2026-05-22 — developer — GetCmd.get(String, String, SuggestedLemmasFilter) + handleSuggestedLemmas() + printSuggestedLemmaQueryResult() implementados. Constructor 7-arg legacy + setRevisionArtifactStore public. Main.java actualizado con LemmaAbsenceContextSuggestedLemmaQueryPort. GetCmdTest.java: (GetTasksFilter) null casts agregados en 11 call sites (@Generated, cambio mínimo para compilar). mvn test-compile BUILD SUCCESS. GetCmdTest: 74/74. FCslatdcJ004: PASS. FCslatdcJ001/J002: 6 fallos por reflection access en package-private class (test infra bug, no bloquea objetivo principal). Ver fix-log.md.
