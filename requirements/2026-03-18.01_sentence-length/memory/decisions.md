# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-11 — analyst — Resolucion DOUBT-LEVEL-STATS-LOCATION: opcion C (retiro)
  Decision: retirar las 6 reglas bloqueadas (R006, R007, R010, R011, R014, R015) y
  los 2 journeys que dependen exclusivamente de ellas (J002, J003). Reformular J001
  y J005 quitando referencias a las reglas retiradas. Marcar DOUBT-LEVEL-STATS-LOCATION,
  DOUBT-PROGRESSION-GAPS y DOUBT-EQUAL-AVERAGES como RESOLVED apuntando al retiro.

  Razones que pesaron:
  - Casi 2 meses con DOUBT-LEVEL-STATS-LOCATION OPEN sin que nadie identifique el
    componente owner del "procesamiento posterior".
  - Grep exhaustivo: ningun otro feature en `requirements/` y nada en `sentinel.yaml`
    consume R006/R007/R010/R011/R014/R015. FEAT-RCLALEN y FEAT-LAGEN trabajan
    sobre `SentenceLengthDiagnosis` por quiz (tokenCount, targetMin, targetMax,
    cefrLevel, delta, toleranceMargin), no sobre stats agregadas por nivel.
  - Opcion B (anclar a IAuditEngine/IScoreAggregator) descartada porque
    `AuditReport`/`AuditNode` solo exponen `scores: Map<String, Double>`. No hay
    forma honesta de anclar mean-length / OPTIMO-DEFICIENTE / progresion /
    recomendaciones a la API actual.
  - Opcion A (definir owner con architect) seria churn arquitectonico motivado
    por reglas que no tienen demanda real concreta. Empujar architecture nueva
    para nadie es invertir el costo en el orden equivocado.

  Donde queda lo retirado:
  - Nueva seccion en Contexto: "Analisis de nivel y progresion: fuera de alcance MVP".
    Explica por que se retiro, donde residiria el componente owner si reaparece la
    demanda, y enumera explicitamente los IDs retirados como punto de partida para
    una futura iteracion.
  - DOUBT-EQUAL-AVERAGES preserva como referencia la respuesta tentativa (umbral
    50% del avance esperado entre puntos medios de rangos objetivo). Si la
    funcionalidad reaparece, esa respuesta puede servir de base.
  - IDs R006, R007, R010, R011, R014, R015, J002, J003 NO se reasignan.

  R009 se mantiene tal como esta (Validation: ASSUMPTION minor, ya tiene tests
  PASSING en sentinel.yaml). Mismo patron que F-SLEN-R009 venia funcionando
  desde antes — la frase "margen fijo de 4 tokens" es testeable a traves del
  comportamiento de scoring de R002 (a 4 tokens fuera del rango la puntuacion es 0.0).
  NO confundir con el patron R004/R007 de KTLEN — esas decian "no es configurable"
  sin comportamiento; R009 dice "el margen es 4" con comportamiento observable.

  Estado final: 10 reglas (R001, R002, R003, R004, R005, R008, R009, R012, R013,
  R016) y 3 journeys (J001, J004, J005). Validacion: `sentinel requirement
  validate` → [OK].

  Pendiente para architect: `sentinel.yaml:10048-10056` lista J001-J005 en
  `definitions/FEAT-SLEN/journeys`. Necesita patch para retirar J002 y J003
  (no es lane de analyst). Las reglas retiradas no aparecen en `definitions`,
  solo journeys, asi que esa es la unica accion del architect.

  Pendiente para qa-tester:
  - El patch parcial ya propuesto (R003, R004, R005, R008, R013, R016 + co-tag J004)
    sigue siendo valido — ninguna de las reglas retiradas estaba en el.
  - Las 6 reglas retiradas + J002 + J003 dejan de aparecer como NO_TESTS — no
    necesitan tag, no son `pending-anything`, simplemente ya no existen.
  - J001 y J005 reformulados pueden recibir handwrittenTests si los necesitas;
    estan tageados a R001/R002/R013/R016 (J001) y R002/R016 (J005), reglas que
    ya estan o estaran tageadas.

  why: la pregunta de los buckets que me planteaste estaba bien encuadrada. La
  respuesta correcta no era hacer A (empujar architecture) ni inventar superficie
  donde no la hay (B). C es la decision honesta cuando: (i) la demanda concreta
  no aparecio, (ii) los consumidores reales trabajan sobre otra capa de
  abstraccion (diagnosticos por quiz, no stats por nivel), y (iii) el coste de
  agregar architecture es alto comparado con el valor incremental conocido.

2026-05-11 — qa-tester — Patch parcial FEAT-SLEN + escalación a analyst
  - 6 NO_TESTS reglas testeables HOY: R003/R004/R005/R008/R013/R016.
    R013 sobre SentenceLengthAnalyzer (tokens lingüísticos).
    R003/R004/R005/R008/R016 cross-feature sobre IAuditEngine (agregación
    descripta como "comportamiento del motor genérico" en REQUIREMENT.md
    línea 133).
    R016 co-tag con J004 (navegar jerarquía).
  - 6 reglas bloqueadas por DOUBT-LEVEL-STATS-LOCATION OPEN:
    R006/R007/R010/R011/R014/R015. Todas dependen del "procesamiento posterior"
    no asignado a ningún componente en sentinel.yaml. Sin superficie observable
    no se puede emitir test honesto.
  - 4 journeys bloqueadas (J001/J002/J003/J005). J002/J003 dependen 100% de
    bloqueadas; J001/J005 mezclan testeables y bloqueadas.
  - Escalación enviada a @analyst con 3 caminos:
    A) Cerrar Doubt + agregar superficie (LevelStatsCalculator o similar) via architect.
    B) Reformular sobre IAuditEngine/IScoreAggregator — descartado, no hay superficie.
    C) Bucket 3 (retirar) si las stats son post-MVP.
  - Validador: 0 additions, 2 modifications, 0 conflicts.
  - 10/16 reglas con tag directo después del apply parcial.
