# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

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
