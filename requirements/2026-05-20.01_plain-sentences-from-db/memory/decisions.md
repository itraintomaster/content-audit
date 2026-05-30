# Decisions — FEAT-DBSENT

2026-05-20 — architect — Patch minimalista: solo se agrega `sentences: List<String>` a `QuizTemplateEntity`. Los cambios a `FileSystemCourseRepository.load()` y `CourseToAuditableMapper.mapQuiz()` son internos a implementaciones, no tocan contratos ni `requiresInject`; quedan para `@developer`.
  why: lo arquitectonicamente nuevo es solo el campo del modelo; el resto es comportamiento de impl ya existente.

2026-05-20 — architect — Se conserva `QuizSentenceConverter.toPlainSentences` intacto (opcion (a) del usuario).
  why: contrato util como fallback para fuentes sin campo persistido; tests FEAT-QSENT R017-R019 lo siguen cubriendo; retirarlo implicaria tocar requirement de otra feature sin necesidad.

2026-05-20 — architect — Skip de @analyst registrado. Dos gaps documentados en TECH_SPEC y REQUIREMENT (Notas): (1) ausencia de invariante "sentences no-null" en QuizTemplateEntity cargado; (2) discriminador pattern A/B vive solo como heuristica offline, no esta modelado en codigo.
  why: el usuario lo decidio explicitamente; quedan registrados para futura reapertura por @analyst.
