# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

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
