# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-05-10 — test-writer — 34 handwrittenTests implementados + journey mocks ajustados. BUILD SUCCESS (263+84 tests, 0 failures).
  LemmaCountAnalyzerTest: 23 tests (R001-R006, R012-R019). EvpThenNlpLemmaCefrLevelResolverTest: 5 tests (R009-R011).
  DefaultLemmaCountConfigLoaderTest: 6 tests (R007 + R008x5). FLcountJ001JourneyTest: 3 mocks ajustados a resolve(LemmaAndPos, Optional<CefrLevel>).
  Clave: buildCourseNode() requiere setDiagnoses(new DefaultCourseDiagnoses()); LemmaCountAnalyzer cast getDiagnoses() a CourseDiagnoses (patron igual a IAuditEngine).
  next: FEAT-LCOUNT con cobertura directa por regla (R001-R019). sentinel report actualizará a PASSING.

2026-05-10 — developer — Producción post-ARCH-LCOUNT-002/003 completada. mvn compile BUILD SUCCESS (audit-domain, audit-application, audit-cli). FLcountJ001JourneyTest: 3/3 PASS. audit-cli: 281 tests, 0 failures.
  Cambios: (1) DefaultLemmaCountConfig.getThreshold() ahora devuelve this.threshold (no DEFAULT_THRESHOLD hardcodeado). (2) DefaultLemmaCountConfigLoader.load() implementado: null->default 4, vacío/decimal/no-parseable/<=0->IllegalArgumentException. (3) EvpThenNlpLemmaCefrLevelResolver actualizado a firma resolve(LemmaAndPos, Optional<CefrLevel>): EVP primero, luego NLP hint, luego empty. (4) LemmaCountAnalyzer call site: resolve(lp, Optional.empty()). (5) Main.java: new DefaultLemmaCountConfigLoader().load(null) en lugar de new DefaultLemmaCountConfig().
  Los 34 UnsupportedOperationException en tests son stubs pendientes de @test-writer (no errores de producción).
  next: @test-writer implementa 34 stubs (LemmaCountAnalyzerTest x21, EvpThenNlpLemmaCefrLevelResolverTest x5, DefaultLemmaCountConfigLoaderTest x6, journey mock fix x2).

2026-05-10 — developer — Wiring CLI completado. mvn test -pl audit-cli PASS (281 tests, 0 failures).
  Main.java: instancia LemmaCountAnalyzer + EvpThenNlpLemmaCefrLevelResolver + DefaultLemmaCountConfig; agrega a contentAnalyzers y describableConfigs.
  FileSystemEvpCatalog.lookupLevel() implementado con reverse index levelByLemma (stub pre-existente que bloqueaba runtime).
  mvn install -pl vocabulary-infrastructure necesario para actualizar el JAR en .m2 (audit-cli.sh usa classpath mixto con .m2 jars).
  next: FEAT-LCOUNT completamente integrado. Invocar: `./audit-cli.sh analyze <course-path>`; el resultado incluye clave "lemma-count" en el reporte.

2026-05-10 — developer — Implementación de producción FEAT-LCOUNT completada. mvn test PASS (235+78, 0 failures).
  Archivos: EvpThenNlpLemmaCefrLevelResolver (R009/R010 latente), LemmaCountAnalyzer (R001-R019 completo),
  CourseDiagnoses+LevelDiagnoses (getLemmaCountDiagnosis agregado), Default*Diagnoses (setters),
  DefaultLemmaCountConfig en audit-application (getThreshold=4, describe() — stub incompleto resuelto).
  FLcountJ001JourneyTest: los 3 paths PASS. Fase de desarrollo COMPLETA.
  next: wiring audit-cli si corresponde; feature lista para integracion.

2026-05-10 — test-writer — Verificado: no existen handwrittenTests declarados en sentinel.yaml para FEAT-LCOUNT (LemmaCountAnalyzer, EvpThenNlpLemmaCefrLevelResolver, DefaultLemmaCountConfig). El unico artefacto de test es F-LCOUNT-J001 (journey), ya implementado. Fase test-writing COMPLETA.
  next: @developer implementa LemmaCountAnalyzer y EvpThenNlpLemmaCefrLevelResolver.

2026-05-10 — test-writer — Implementado FLcountJ001JourneyTest (3 paths). Compila y corre; falla con UnsupportedOperationException de LemmaCountAnalyzer (esperado en TDD). Suite audit-domain: BUILD SUCCESS 235 tests.

2026-05-10 — analyst — Doubts cerrados. R007/R008 aclaradas (ausente -> default 4 / invalido -> falla al cargar) y R010 aclarada (fallback condicional, no error si NLP no aporta CEFR). Open Questions removida. Validador OK.
  next: listo para pasar a la fase de arquitectura con @architect.

2026-05-10 — analyst — REQUIREMENT.md reescrito con el nuevo diseno consensuado (19 reglas, 1 journey flow, 2 doubts abiertas). Validador OK.
  next: el usuario revisa/responde DOUBT-CEFR-FROM-NLP y DOUBT-N-VALIDATION-BEHAVIOR antes de pasar a arquitectura.

2026-05-10 — qa-tester — Propuesto patch (architectural_patch.yaml) que declara testModule/testPackage para F-LCOUNT-J001.
  Decision: testModule=audit-domain, testPackage=com.learney.contentaudit.auditdomain.lemmacount
  (mismo patron que F-LREC-J* y F-LABS-J*: el journey test vive en el paquete java
   del package interno que aloja las implementaciones que ejercita —
   LemmaCountAnalyzer, EvpThenNlpLemmaCefrLevelResolver, LemmaCountConfig).
  next: usuario aplica el patch (sentinel patch apply --requirement-folder ...).

2026-05-11 — qa-tester — Patch combinado de tag-por-regla listo en `architectural_patch.yaml` (modo merge).
  19/19 reglas cubiertas con 34 handwrittenTests. Cero cobertura transitiva.
  Distribución: LemmaCountAnalyzer (R001-R006, R012-R019, 21 tests), EvpThenNlpLemmaCefrLevelResolver (R009-R011, 5 tests), DefaultLemmaCountConfigLoader (R007 + R008, 6 tests).
  Architect entregó ARCH-LCOUNT-002 con seam de carga (LemmaCountConfigLoader) para R008 y nueva firma resolve(LemmaAndPos, Optional<CefrLevel>) para R010.
  Validator: 2 additions, 6 modifications, 0 deletions, 0 conflicts.
  next: usuario aplica el patch. Post-apply: sentinel generate, @test-writer ajusta FLcountJ001JourneyTest mocks por nueva firma resolver, @test-writer implementa 34 stubs nuevos, @developer wireing Main.java al loader.
