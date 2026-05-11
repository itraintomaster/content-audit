# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-05-10 — test-writer — Implementó 3 journey stubs en IAuditEngineTest.java (J001, J002, J004)
  Stubs tenían UnsupportedOperationException → propiedad de test-writer (no MODIFIED). 3 tests pasan. sentinel verify VERIFY OK.
  Regla aprendida: *SentinelTest.java con cuerpos {} son de sentinel (MODIFIED si se tocan). Solo UnsupportedOperationException stubs son de test-writer.

2026-05-10 — test-writer — Implementó 1 stub en KnowledgeInstructionsLengthAnalyzerTest.java (F-KTLEN-R005)
  shouldDistinguishThreeScoringRanges... → 4 aserciones de boundary conditions (70→1.0, 71→0.5, 100→0.5, 101→0.0). 264 tests, 0 fallos.

2026-05-10 — test-writer — Los 4 stubs de KnowledgeTitleLengthAnalyzerSentinelTest y 8 de KnowledgeInstructionsLengthAnalyzerSentinelTest NO implementados
  Blocker: son `tests: _change: add` en sentinel.yaml con cuerpos {} en archivos @Generated. Necesitan `sentinel generate` para obtener imports correctos. Sin generate, cualquier modificación = VERIFY FAILED.

2026-05-11 — developer — Diagnóstico nocturno: BUILD FAILURE por 3 stubs sin implementar en IAuditEngineTest.
  Producción de IAuditEngine, KnowledgeTitleLengthAnalyzer, KnowledgeInstructionsLengthAnalyzer: CORRECTA.
  Stubs pendientes (J001, J002, J004) derivados a @test-writer para implementación de bodies.
  Bloqueante: mvn test -pl audit-domain falla con 3 UnsupportedOperationException en IAuditEngineTest.java.
  next: @test-writer implementa bodies; developer verifica BUILD verde post-implementación.

2026-05-10 — analyst — REQUIREMENT.md reformulado para desbloquear FEAT-KTLEN
  - R004 (pesos no configurables) y R007 (limites instrucciones no configurables) retirados
    como reglas numeradas. Movidos a Contexto > "Decisiones de simplicidad". Ver decisions.md
    para razonamiento completo y patron general aplicable a otros features.
  - R005 reformulado: removido el Error de configuracion invalida (`soft < hard`) imposible
    de fire con limites hardcoded. R005 ahora es la "definicion de los umbrales 70/100"
    observable via R006.
  - 6 reglas numeradas restantes: R001, R002, R003, R005, R006, R008. Todas testeables.
  - sentinel requirement validate: [OK].
  - Next: qa-tester puede cerrar R004/R007 como ya-no-existen (no marcarlos NO_TESTS).
    Proceder con re-tag de los 5 tests mis-tagged (R005→R006) segun nuevo fraseo.
    Lead decide si necesita correr `sentinel generate` para regenerar requirement.yaml.

2026-05-10 — test-writer — Implementó bodies de 12 stubs vacíos en SentinelTest files
  - KnowledgeTitleLengthAnalyzerSentinelTest: 4 tests (pesos 28.5→0.5, 29→0.0, 35→0.0, 70→0.0)
  - KnowledgeInstructionsLengthAnalyzerSentinelTest: 8 tests (R005/R006 scoring por rangos)
  - Todos 12 compilan y pasan; suite completa de audit-domain: 263 tests, 0 fallos.

2026-05-11 — qa-tester — Diagnóstico FEAT-KTLEN para night-fix Task #1
  - 6/8 reglas PASSING (R001, R002, R003, R005, R006, R008).
  - R004 y R007 son ASSUMPTION sobre invariantes de implementación
    ("pesos/limites no configurables") sin superficie observable: los
    valores son `private static final`, sin constructor/setter/Config interface.
    Escalado a @architect con 3 opciones (ArchUnit / reclasificar / eliminar).
  - 5 mis-tags detectados en handwrittenTests de KnowledgeInstructionsLengthAnalyzer:
    tests de scoring tageados a R005 (limites=datos) cuando deberían R006 (mapeo→score).
    Re-tag pendiente hasta resolver bloqueo en architect (R005 hereda mismo problema
    de superficie si limites permanecen hardcoded).
  - J001/J002/J004 NO_TESTS: son journeys de uso del reporte completo, superficie real
    es IAuditEngine (que hoy tiene handwrittenTests:[]). Esperando decisión del lead
    si proponer tests integradores ahí o dejar journeys NO_TESTS hasta otra fase.
  - Tests "engañosos" en .java (lines 304, 317 de KnowledgeTitleLengthAnalyzerTest.java):
    nombres prometen scores que el código no asierta. Deuda de test-writer.

2026-05-11 — qa-tester — Patch parcial FEAT-KTLEN propuesto (journeys J001/J002/J004)
  - 3 handwrittenTests integradores sobre IAuditEngine (audit-domain), no sobre
    los analyzers individuales (esos serían transitividad encubierta).
  - Validado: 0 additions, 1 modifications, 0 conflicts.
  - Path: requirements/2026-03-22.01_knowledge-titles-length/architectural_patch.yaml
  - R004/R005/R007 quedan pending-analyst (no van en este patch).
  - Re-tags R005→R006 también pending hasta decisión analyst (mover sin reclasificar
    deja R005 en NO_TESTS, violación regla del usuario).
  - Tests engañosos R003 (nombre miente sobre score 0.75/0.5) derivados a test-writer
    por instrucción del lead.
  - why: lead ya marcó FEAT-KTLEN como "ArchUnit aceptado" pero architect lo rechazó;
    se ruteó a analyst para resolver el desacuerdo. Mientras tanto se entrega lo no
    controvertido.

2026-05-11 — qa-tester — Patch FEAT-KTLEN consolidado (journeys + re-tags) entregado al lead
  - Mergeado en architectural_patch.yaml: 3 handwrittenTests journey + 5 re-tags.
  - Validador: 0 additions, 1 modifications, 0 conflicts.
  - R005 cae a NO_TESTS post-apply (intencional, honesto) → architect lo suma
    al paquete `pending-analyst` con R004/R007.
  - Architect confirmó: test "should use weighted character length..." se re-tagea
    a R002 (sistema de pesos) en lugar de R006 — el `,` peso 0.5 hace que 80 comas
    pesen 40, esto es R002 puro. Journey J003 se mantiene.
  - Mi parte de Task #1 (QA design) entregada. Próximo: lead aplica patch +
    `sentinel generate` + asigna a test-writer para 3 stubs integradores honestos.

2026-05-11 — qa-tester — Patch correctivo R005 listo tras edit del analyst
  - Re-evalué los 5 re-tags a la luz del nuevo R005 (umbrales 70/100) vs R006 (mapeo 3 niveles + null/empty).
  - Patch correctivo: re-revertir 4 tests R006→R005 (los que verifican fronteras exactas 70/71/100/101).
  - 1 test sigue en R002+J003 (sin cambio, es claramente sistema de pesos).
  - R006 queda con 9 tests directos (mapeo general + null/empty) — sigue PASSING.
  - R005 pasa a PASSING con 4 tests boundary directos.
  - Patch estructural R004/R005/R007 ABORTADO definitivamente (analyst retiró R004 y R007).
  - Comando apply emitido al lead: `sentinel patch apply --as=qa-tester ...`
  - why: la nueva redacción de R005 distingue claramente "umbrales como valores" (R005)
    de "mapeo de 3 niveles + null/empty" (R006); cada test verifica solo uno de los dos.

2026-05-11 — qa-tester — Patch FINAL FEAT-KTLEN entregado (--replace, 1 modification)
  - Lead eligió Lectura B (tag directo para R005).
  - Patch final con --replace contiene:
    * 3 handwrittenTests journey en IAuditEngine (J001, J002, J004).
    * 4 re-tags R005→R006 (boundary tests vuelven al mapeo general).
    * 1 re-tag R005→R002+J003 (sistema de pesos).
    * 1 handwrittenTest NUEVO dedicado a R005: "should distinguish three scoring
      ranges 1.0 at-or-below-70 0.5 above-70-up-to-100 0.0 above-100 at the
      declared weighted-char thresholds" — body por test-writer.
  - Cero referencias a R004/R007 (analyst los retiró).
  - Comando apply emitido al lead con --as=qa-tester role guard.
  - Task #1 cerrada de mi lane (queda apply + generate + test-writer).
