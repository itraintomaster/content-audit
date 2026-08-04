# Decisions — FEAT-DBSENT

2026-05-20 — architect — Patch minimalista: solo se agrega `sentences: List<String>` a `QuizTemplateEntity`. Los cambios a `FileSystemCourseRepository.load()` y `CourseToAuditableMapper.mapQuiz()` son internos a implementaciones, no tocan contratos ni `requiresInject`; quedan para `@developer`.
  why: lo arquitectonicamente nuevo es solo el campo del modelo; el resto es comportamiento de impl ya existente.

2026-05-20 — architect — Se conserva `QuizSentenceConverter.toPlainSentences` intacto (opcion (a) del usuario).
  why: contrato util como fallback para fuentes sin campo persistido; tests FEAT-QSENT R017-R019 lo siguen cubriendo; retirarlo implicaria tocar requirement de otra feature sin necesidad.

2026-05-20 — architect — Skip de @analyst registrado. Dos gaps documentados en TECH_SPEC y REQUIREMENT (Notas): (1) ausencia de invariante "sentences no-null" en QuizTemplateEntity cargado; (2) discriminador pattern A/B vive solo como heuristica offline, no esta modelado en codigo.
  why: el usuario lo decidio explicitamente; quedan registrados para futura reapertura por @analyst.

2026-06-25 — analyst — Agregada R004 (critical): el lado de ESCRITURA es espejo de R001. Persistir un curso debe preservar las plain sentences de cada quiz; round-trip cargar->guardar->cargar idempotente respecto de sentences.
  why (2026-07-29, nota posterior): la generalizacion de R004 al resto del contenido NO vive aca; se creo FEAT-RPRES (requirements/2026-07-29.01_preservacion-del-contenido-no-revisado) porque DBSENT declara su alcance en un unico dato. R004 queda como esta.
  why: bug detectado — al persistir un curso las plain sentences se omitian; como aplicar una revision reescribe el CURSO COMPLETO (no quirurgico), un solo guardado borraba sentences de TODOS los quizzes -> sentence-length scoring en cero -> perdida de datos masiva. `feature list` confirma 4 reglas; validate [OK]; sync sin cambios (0 journeys, reglas se leen de REQUIREMENT.md).
