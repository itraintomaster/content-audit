# Progress — FEAT-DBSENT

2026-05-21 — test-writer — Implementando 3 test bodies en FileSystemCourseRepositoryTest para F-DBSENT-R001.
  why: stubs generados por Sentinel lanzan UnsupportedOperationException. Se usa @TempDir + JSON fixtures reales, mismo patron que FileSystemCourseRepositoryIntegrationTest.

2026-05-21 — test-writer — Implementacion completa de los 3 tests. Fixtures: form.sentences en JSON para que el repo lo cargue en QuizTemplateEntity.sentences.
  why: en el JSON real (db/quiz-templates.json) sentences vive dentro de form; el repositorio lee de ahi y puebla QuizTemplateEntity.sentences.

2026-05-21 — test-writer — Implementado body de F-DBSENT-R003 en DefaultQuizSentenceConverterTest (línea ~833). Test compila y pasa (1/1).
  why: test guard que certifica que toPlainSentences sigue disponible como operación pública funcional en el contrato del converter, sin replicar invariantes R017-R019.

2026-05-21 — test-writer — Implementados 3 test bodies F-DBSENT-R002 en CourseToAuditableMapperTest. Los 3 fallan con assertions reales; 13 tests preexistentes siguen pasando.
  why: TDD completo. Test 3 usa lenient() para evitar PotentialStubbingProblem con analyzeTokensBatch mientras la produccion no use sentences.get(0) como clave.

2026-05-21 — developer — F-DBSENT-R001 implementado en FileSystemCourseRepository. Los 57 tests del modulo pasan (0 fallos).
  why: extraido sentences desde formMap via extractStringList (nuevo helper privado, linea ~679); pasado como arg 21 al constructor de QuizTemplateEntity. Campo ausente devuelve null (consistente con extractOidListNullable).
