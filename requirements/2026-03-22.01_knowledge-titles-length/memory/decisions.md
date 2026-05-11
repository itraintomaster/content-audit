# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-10 — analyst — Resolucion bloqueo R004/R007 (y reformulacion de R005): opcion C reframing
  - Decision: R004 y R007 retirados como reglas numeradas. Ya no aparecen en REQUIREMENT.md.
    Su contenido (no-configurabilidad de pesos y limites) se movio a Contexto > "Decisiones
    de simplicidad (fuera del alcance de esta version)". Razon: no eran reglas de negocio
    sino decisiones de scope sobre lo que el sistema NO expone. No describian comportamiento
    observable adicional al ya cubierto por R001/R002/R003/R005/R006.
  - R005 reformulado: removido el "Error" de validacion de configuracion (`soft < hard`)
    porque los limites son hardcoded y ese error nunca puede fire. R005 ahora afirma
    explicitamente que los limites 70/100 son observables a traves del scoring de R006,
    y referencia "Decisiones de simplicidad" para la nota de no-configurabilidad.
  - IDs R004 y R007 quedan retirados (no se reasignan a otras reglas) para no romper
    trazabilidad con commits historicos. Esta convencion esta documentada en una nota
    al inicio de la seccion de Reglas de Negocio.
  - Reglas numeradas restantes: R001, R002, R003, R005, R006, R008 (6 reglas, todas
    testeables via scoring/identificacion del analizador).
  - Patron general aplicable a otros features: reglas con `Validation: ASSUMPTION` que
    declaran no-configurabilidad de un parametro son redundantes si el valor del parametro
    ya esta cubierto por una regla de comportamiento. Migrarlas a la seccion de Contexto
    como decision de scope. Mantenerlas como regla numerada SOLO si el valor especifico
    no aparece en ninguna otra regla testeable.
  - why: la regla del usuario es "cada regla numerada DEBE tener test directo". Una regla
    cuyo unico contenido es "X no es configurable" carece de superficie observable: el
    sistema no expone NADA. Documentarla como regla numerada y luego excusar la ausencia
    de test mediante marcadores especiales (ASSUMPTION_ONLY, STRUCTURAL_INVARIANT) seria
    inventar semantica que oculta la realidad — que no es una regla de negocio.
  - Confirmacion para qa-tester: las 5 reglas restantes que estaban PASSING siguen PASSING.
    Las 2 que estaban en NO_TESTS pending-analyst (R004 y R007) ya no existen.
    El re-tag pendiente de los 5 tests de KnowledgeInstructionsLengthAnalyzer (tageados a
    R005 cuando deberian ser R006) sigue siendo valido y deberia procederse: R005 quedo
    como "definicion de los limites" (umbrales 70/100 y sus rangos), R006 sigue siendo
    "puntuacion segun rango". Tests de scoring → R006; tests que verifican que la
    instruccion en exactamente 70 cae en el rango "1.0" o que en exactamente 100 cae en
    "0.5" pueden quedarse en R005 como verificacion de los umbrales (frontera inclusiva).

2026-05-11 — qa-tester + architect — R004 y R007 son ASSUMPTION estructurales pending-analyst
  - R004 ("pesos no configurables") y R007 ("limites instrucciones no configurables")
    son invariantes ESTRUCTURALES sobre código `@Generated`, no comportamiento observable.
    Los analyzers son output de Sentinel (`com.sentinel.SentinelEngine`); los valores
    fijos son consecuencia de `implementationLogic` en sentinel.yaml, no decisiones
    runtime testeables.
  - Architect rechazó ArchUnit (introspección reflexiva = test forzado encubierto)
    y rechazó tests transitivos.
  - Por regla operativa del usuario: invariantes estructurales no necesitan
    handwrittenTest siempre que la regla esté FRASEADA como estructural en REQUIREMENT.md.
  - Acción correcta: @analyst debe (a) agregar marcador formal tipo `Validation:
    ASSUMPTION_ONLY` con semántica reconocida por Sentinel para no contar como gate
    NO_TESTS, o (b) confirmar que `Validation: ASSUMPTION` + severity `minor` ya
    debería excluirlas y el bug está en el reporter.
  - Estado provisorio mientras no responda analyst: NO_TESTS marcado explícitamente
    `pending-analyst`. NO inventar handwrittenTests ni archUnitRule.
  - why: 3 opciones (A:ArchUnit / B:reclasificar / C:eliminar) — A invade ownership
    de architect y produce test-forzado; C pierde contexto; B es correcta pero
    requiere lane de @analyst (REQUIREMENT.md es write-only para analyst).

2026-05-11 — analyst (autonomous edit detected) — R004 y R007 RETIRADOS del REQUIREMENT.md
  - Verificado con `git diff` (working tree, no committed yet).
  - R004 y R007 ya no son reglas numeradas; su contenido pasó a sección
    "Decisiones de simplicidad (fuera del alcance de esta version)" en Contexto.
    Quedan como prosa/decisión documentada, no entran al gate NO_TESTS.
  - R005 reformulada: sigue siendo regla pero ahora dice explícitamente
    "observable a través del comportamiento de puntuación descrito en R006".
    Se eliminó el error message "soft < hard" (era dead code).
  - IDs R004 y R007 retirados pero los demás mantienen numeración original
    (trazabilidad con commits históricos).
  - why: el analyst encontró la formulación más limpia para el problema
    estructural sin necesitar marcador formal `ASSUMPTION_ONLY` ni eliminar
    contexto — simplemente sacar las reglas del catálogo numerado.
  - Patch estructural (R004/R005/R007 ArchUnit-style) autorizado por lead
    queda ABORTADO: validador rechaza R004 y R007 (correctamente, ya no existen).
  - R005: pending decisión lead — interpretación A (transitivo a R006) vs
    B (tag directo con test "should distinguish three scoring ranges...").
