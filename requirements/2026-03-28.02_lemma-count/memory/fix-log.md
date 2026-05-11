# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-05-10 — test-writer — FLcountJ001JourneyTest: mockear LemmaCefrLevelResolver directamente en LemmaCountAnalyzer (no usar EvpThenNlpLemmaCefrLevelResolver concreto).
  why: EvpThenNlpLemmaCefrLevelResolver no tiene puerto NLP declarado; el path-2 (NLP fallback) no es testeable a traves del impl concreto sin modificar contratos. El mock simula el escenario correctamente desde la perspectiva del analyzer.
