# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-11 — analyst — Resolucion 5 ASSUMPTION + J006: 3 buckets aplicados
  Bucket 1 — REFORMULADA como AUTO_VALIDATED (comportamiento observable anclado a contrato):
    - R004 (tokens sin frequencyRank): excluidos del conteo de distribucion. No
      contribuyen al `count` de `BucketResult` ni a `totalTokens`. El porcentaje
      se calcula sobre el total de tokens efectivamente clasificados. Removida
      la coletilla [ASSUMPTION] + el Error message obsoleto (era warning, no error).
      La parte "el sistema deberia reportar la cantidad de tokens excluidos" se
      promovio a Doubt[DOUBT-COCA-EXCLUDED-TOKEN-VISIBILITY] OPEN (no hay campo
      `excludedTokens` en los records hoy).

  Bucket 2 — REFORMULADAS parcialmente como AUTO_VALIDATED + Doubt para el detalle:
    - R017 (asignacion topics a quarters): la propiedad general es observable
      (cada topic en exactamente un quarter, orden ordinal via QuarterResult.index,
      cobertura total). El algoritmo exacto de particion cuando topics no es
      multiplo de 4 se promovio a Doubt[DOUBT-COCA-QUARTER-PARTITION] OPEN
      (4 opciones).
    - R023 (margen cambio significativo): la existencia del margen y su efecto
      de filtrado son observables via ProgressionAssessment.matches. El valor
      exacto se promovio a Doubt[DOUBT-COCA-PROGRESSION-MARGIN] OPEN (3 opciones,
      incluyendo aprovechar el patron resuelto en SLEN DOUBT-EQUAL-AVERAGES).

  Bucket 3 — RETIRADAS como reglas/journey (instruccion literal del lead):
    - R033 (directivas por trimestre): el contrato `ImprovementDirective` solo
      expone `levelName`, no `quarterIndex`. La regla afirmaba comportamiento
      que el contrato actual no soporta. Documentada en Contexto > "Decisiones
      de simplicidad (fuera del alcance de esta version)" como "granularidad
      de las directivas a nivel CEFR unicamente, granularidad por trimestre
      fuera de alcance".
    - R034 (contenido directivas): retirada por instruccion del lead (mi
      recomendacion habia sido reformular como AUTO_VALIDATED anclada al record
      ImprovementDirective). Documentada en "Decisiones de simplicidad" como
      "el contenido exacto de las directivas es responsabilidad del modelo de
      datos ya definido en el sistema".
    - J006 (comparar estrategias LEVELS vs QUARTERS): workflow externo del
      usuario, no journey de sistema (el sistema no compara; el usuario corre
      dos auditorias). Sin superficie testeable.

  IDs retirados (R033, R034, J006) NO se reasignan, para no romper trazabilidad
  con commits historicos.
  Validacion: `sentinel requirement validate` → [OK].
  Reglas numeradas finales: 32 (R001-R032). Journeys finales: 5 (J001-J005).
  Cero `Validation: ASSUMPTION` en reglas vivas.

  why R034 retirada en vez de reformulada: mi recomendacion fue B1 (reformular
  AUTO_VALIDATED anclada a ImprovementDirective), pero el lead pidio bucket 3
  literal. La razon honesta sigue siendo que el contrato existe en sentinel.yaml
  (record ImprovementDirective con 7 campos definidos), pero la decision queda
  documentada bajo "el contenido exacto puede refinarse sin que constituya
  cambio de regla de negocio" — refinamientos de presentacion sin tocar la
  estructura del record.

  Pendiente para architect: sentinel.yaml:10241 lista F-COCA-J006 en
  `definitions/FEAT-COCA/journeys`. Necesita patch para retirarla. Las 2 reglas
  retiradas (R033, R034) no aparecen en `definitions`, solo journeys, asi que
  esa es la unica accion del architect.

  Pendiente para qa-tester:
  - Patch principal con 29 handwrittenTests AUTO_VALIDATED sigue valido (no
    toca ninguna de las reglas retiradas).
  - Para las 3 reglas reformuladas como AUTO_VALIDATED (R004, R017, R023), puede
    emitir un patch adicional con handwrittenTests directos:
    * R004: shouldExcludeTokensWithoutFrequencyRankFromBucketCount...
    * R017: shouldAssignEachTopicToExactlyOneQuarterInOrdinalOrder...
    * R023: shouldNotMarkBandAsChangedWhenVariationIsBelowMargin...
  - Las 3 reglas retiradas + J006 dejan de aparecer como NO_TESTS — no necesitan
    tag, simplemente ya no existen.

2026-05-11 — qa-tester — Patch FEAT-COCA: 29 AUTO_VALIDATED + 5 ASSUMPTION escaladas
  - 29 handwrittenTests cubriendo las 29 reglas AUTO_VALIDATED del feature:
    * 2 sobre DefaultTokenClassifier (R001/R002).
    * 13 sobre CocaBucketsAnalyzer (R005-R013/R015/R016/R019/R027/R028).
    * 3 sobre CocaTokenAccumulationAggregator (R025/R026/R029).
    * 3 sobre DefaultProgressionEvaluator (R021/R022/R024).
    * 3 sobre DefaultImprovementPlanner (R030/R031/R032).
    * 5 sobre DefaultCocaBucketsConfig (R003/R009/R014/R018/R020).
  - 4 journeys cubiertos via co-tag (J001 en R013, J003 en R020 y R021,
    J004 en R030, J005 en R026). J002 sin co-tag explícito (cubierto
    indirectamente). J006 escalada para retirar.
  - 5 reglas ASSUMPTION escaladas a @analyst con 3 buckets:
    * Bucket 1 (reformular AUTO_VALIDATED): R004 (tokens sin freq).
    * Bucket 2 (reformular parcial + Doubt): R017 (partición topics→quarters), R023 (margen progresión).
    * Bucket 3 (retirar): R033, R034, J006.
  - Validador: 0 additions, 6 modifications, 0 conflicts.
  - 29/34 reglas con tag directo, 4/6 journeys con co-tag. Cero transitividad.

2026-05-11 — qa-tester — Patch incremental FEAT-COCA: +3 tests post-analyst (R004/R017/R023)
  - Analyst aplicó los 3 buckets propuestos:
    * R004 → AUTO_VALIDATED (Bucket 1) anclada a BucketResult.count + totalTokens.
    * R017 → AUTO_VALIDATED parcial (Bucket 2) con Doubt[DOUBT-COCA-QUARTER-PARTITION] OPEN.
    * R023 → AUTO_VALIDATED parcial (Bucket 2) con Doubt[DOUBT-COCA-PROGRESSION-MARGIN] OPEN.
    * R033, R034 RETIRADAS (Bucket 3) a "Decisiones de simplicidad".
    * J006 RETIRADO.
  - 3 nuevos handwrittenTests cuidadosamente alineados a la reformulación:
    * R004 sobre DefaultTokenClassifier (exclusión via count/totalTokens).
    * R017 sobre CocaBucketsAnalyzer (propiedad general partición, exonera algoritmo).
    * R023 sobre DefaultProgressionEvaluator (existencia/efecto margen, no valor exacto).
  - Patch consolidado: 32 handwrittenTests totales. Validador limpio.
  - 32/32 reglas con tag directo, 5/5 journeys cubiertos vía co-tags.
