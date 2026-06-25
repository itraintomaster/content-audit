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

2026-06-25 — analyst — Nueva regla F-DBSENT-R004 (critical) agregada al REQUIREMENT.md; sin journeys nuevas. Falta: arquitectura/test/impl del lado de escritura (espejo de R001). Proximo paso: @architect evalua si el path de persistencia de curso necesita contrato nuevo o es comportamiento interno de FileSystemCourseRepository.save(); luego @qa-tester propone test de round-trip y @developer implementa la escritura simetrica de sentences.

2026-06-25 — test-writer — Implementados 2 test bodies F-DBSENT-R004 en FileSystemCourseRepositoryTest. Ambos fallan en TDD red (NPE en getSentences() tras reload de targetDir) porque save aun no escribe sentences. Los 3 tests preexistentes del archivo siguen en verde.
  why: test 2 (multi-quiz) construye un segundo milestone con K2/T2/M2 manualmente; sobreescribe _course.json para que ROOT apunte a [M1, M2]. Fixture sigue el mismo patron JSON de writeCourseWithQuiz.

2026-06-25 — developer — F-DBSENT-R004 implementado en FileSystemCourseRepository.writeQuizzes (linea ~577). Los 59 tests del modulo pasan (0 fallos).
  why: en writeQuizzes, la llamada a formToJson devuelve el mapa del form; si el quiz tiene sentences no vacias se inyectan como formJson.put("sentences", ...) antes de q.put("form", formJson). Simetrico con el load que lee form.sentences como primario.
