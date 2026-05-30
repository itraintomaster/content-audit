# Fix Log — FEAT-DBSENT

2026-05-20 — qa-tester — Propose handwrittenTests para R001 (3), R002 (3), R003 (1) y _change: delete sobre 3 tests del mapper (F-QSENT-R026 x2, F-QSENT-R027).
  why: F-DBSENT-R002 contradice F-QSENT-R027 (mapper ya no debe invocar toPlainSentences). Los 3 tests del mapper que verificaban derivacion runtime (variantes/hints/single-invocation) dejan de aplicar porque la correccion vive ahora en el dato persistido, no en el mapper. F-QSENT-R026/R027 siguen vigentes en su REQUIREMENT pero observados en otro lugar (DefaultQuizSentenceConverter, donde ya estan cubiertos por R017-R019). Los tests F-RCLAQS-R002/R003/R004 sobre serialize() no se tocan: el mapper sigue llamando serialize por FEAT-RCLAQS-R002.

2026-05-21 — qa-tester — Re-validacion patch FEAT-DBSENT post `sentinel feature sync`. `sentinel patch propose --replace` reporta "0 additions, 3 modifications, 0 deletions, 0 conflicts". Los 3 `_change: delete` apuntan a tests existentes en sentinel.yaml lineas 3936/3941/3945 (mapper) que aun tienen rule F-QSENT-R026/R027 huerfanas. Patch listo para `sentinel patch apply`.
  why: R026/R027 fueron retiradas formalmente en FEAT-QSENT REQUIREMENT.md (nota explicita en linea 319) y desaparecieron de features[].rules; el patch sigue siendo coherente porque los 3 tests huerfanos viven en handwrittenTests del mapper y se los borra alli mismo.

2026-05-21 — test-writer — Test 3 (batch key) requiere lenient() en el stub de analyzeTokensBatch porque Mockito strict mode rechaza stubs no consumidos.
  why: la produccion actual llama analyzeTokensBatch con un argumento diferente al esperado; lenient() evita PotentialStubbingProblem y deja que verify() capture el fallo real cuando la produccion se implemente.

2026-05-21 — test-writer — 3 tests de F-DBSENT-R001 implementados en FileSystemCourseRepositoryTest. Bloqueante: FileSystemCourseRepository.java linea 327 usa el constructor de 20 args (sin sentences) y no compila contra el QuizTemplateEntity actualizado. Ademas se corrigio FileSystemCourseRepositoryIntegrationTest.java linea 850 (mismo problema, ya resuelto con null).
  why: @developer debe actualizar FileSystemCourseRepository: (1) extraer sentences de form.sentences en el JSON, (2) pasar la lista al constructor de 21 args. Sin ese cambio el modulo no compila y los tests no pueden ejecutarse.

2026-05-21 — developer — Extraer sentences de formMap con cast @SuppressWarnings("unchecked") retorna null cuando ausente. Pasar al constructor en posicion 21.
  why: patron identico a extractOidListNullable; null es el comportamiento correcto segun REQUIREMENT (no-null out-of-scope).
