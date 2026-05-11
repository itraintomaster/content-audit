# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-05-11 — analyst — REQUIREMENT.md reformulado para desbloquear FEAT-NLP
  - 5 reglas retiradas: R018, R022, R025, R027, R033 → seccion "Decisiones de
    simplicidad" en Contexto. Sus IDs quedan retirados sin reasignar.
  - 3 reglas reformuladas como AUTO_VALIDATED (con superficie real anclada a
    NlpTokenizerConfig): R021, R030, R031.
  - Journey J002 reformulado como AUTO_VALIDATED (ejecuta R030).
  - Journey J003 retirado (sin contrato del sistema).
  - Nueva Doubt OPEN: DOUBT-NLP-FALLBACK-SOURCE (alternativa de fallback usando
    catalogo enriquecido cuando SpaCy no esta disponible).
  - Validacion: sentinel requirement validate → [OK].
  - Reglas finales: 28 numeradas + 2 journeys.
  - Next: qa-tester puede proponer handwrittenTests directos para R021/R030/R031/J002.
    Architect debe proponer patch para retirar F-NLP-J003 de sentinel.yaml definitions.
