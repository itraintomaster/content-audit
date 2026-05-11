# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-05-10 — test-writer — FLcountJ001JourneyTest: mockear LemmaCefrLevelResolver directamente en LemmaCountAnalyzer (no usar EvpThenNlpLemmaCefrLevelResolver concreto).
  why: EvpThenNlpLemmaCefrLevelResolver no tiene puerto NLP declarado; el path-2 (NLP fallback) no es testeable a traves del impl concreto sin modificar contratos. El mock simula el escenario correctamente desde la perspectiva del analyzer.

2026-05-10 — test-writer — LemmaCountAnalyzerTest: buildCourseNode() necesita setDiagnoses(new DefaultCourseDiagnoses()).
  why: LemmaCountAnalyzer.onCourseComplete() hace cast de getDiagnoses() a CourseDiagnoses para setLemmaCountDiagnosis(). Sin inicializar, getDiagnoses() retorna null y la diagnosis no se setea. Patron igual al que usa IAuditEngine (linea 45).

2026-05-11 — qa-tester — En este repo, los `gate:` que están dentro del flow YAML de una journey en REQUIREMENT.md son decorativos: el reporter NO los lee para calcular cobertura de reglas. Sólo `handwrittenTests[].traceability.rule` produce el estado PASSING en `.sentinel/sentinel-report.yaml`.
  why: confirmado comparando FEAT-LREC (R002/R003/R007/R013/R015 PASSING via handwrittenTests; el resto NO_TESTS pese a estar en gates del flow) y FEAT-LABS (toda cobertura via handwrittenTests). Cualquier estrategia futura "tag-por-regla" necesita handwrittenTest directo por regla — los gates en REQUIREMENT.md son documentación del flujo, no fuente de cobertura.
