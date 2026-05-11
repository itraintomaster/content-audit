# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-05-11 — analyst — REQUIREMENT.md reformulado para desbloquear FEAT-COCA
  - Bucket 1 reformulada: R004 (AUTO_VALIDATED + Doubt[DOUBT-COCA-EXCLUDED-TOKEN-VISIBILITY]).
  - Bucket 2 reformuladas: R017 (+ Doubt[DOUBT-COCA-QUARTER-PARTITION]), R023
    (+ Doubt[DOUBT-COCA-PROGRESSION-MARGIN]).
  - Bucket 3 retiradas: R033, R034, J006. Movidas a Contexto > "Decisiones de
    simplicidad (fuera del alcance de esta version)".
  - Estado final: 32 reglas (R001-R032) + 5 journeys (J001-J005). Cero ASSUMPTION
    en reglas vivas.
  - Validacion: sentinel requirement validate → [OK].
  - 3 nuevos Doubts OPEN agregados al inicio de Open Questions.
  - Next: qa-tester puede emitir patch adicional para R004/R017/R023 (3 handwrittenTests
    directos). Architect debe proponer patch retirando F-COCA-J006 de
    sentinel.yaml:10241 (definitions/FEAT-COCA/journeys). Las 2 reglas retiradas
    no aparecen en definitions.
