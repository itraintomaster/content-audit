# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — test-writer (CJUEZ) — Los 9 esbozos `@Tag("FEAT-CJUEZ")` de `DefaultBatchServiceTest.java` (R001 1,
  R002 4, R003 4) implementados. Compilan; 8 fallan por assertion + 1 NPE (`BatchEstimate.getJudges()` null), todas
  por el puente sin comportamiento nuevo (nada llama todavía a `judgePass`). Resto del archivo (16 pruebas) sigue
  verde. Nuevo doble `ScriptedJudgePass` (batchengine, package-private) graba estimate/judge/judges y devuelve
  cuentas/jueces preparados; se agregó un campo mutable `Service.judgePass` (default `NoJudgesPass`) para poder
  inyectarlo sin tocar las pruebas existentes.
  Next: developer debe implementar `DefaultBatchJudgePass` (hoy 3x UnsupportedOperationException) y cablear sus
  llamadores en `DefaultBatchService.estimate()/close()` y `DefaultBatchCloser.close()` (validación de --budget/--cap,
  changedQuizzes, judgeQueries/judgeHours) para que estos 9 pasen a verde.

2026-10-01 — developer (CJUEZ) — Puente de compilación tras `sentinel generate`: todo el reactor compila (main y
  test), 18 archivos tocados (6 de producción + 12 de pruebas) más `NoJudgesPass.java` nuevo (fixture de prueba,
  package-private, sin @Generated). `DefaultBatchJudgePass` queda con sus 3 `UnsupportedOperationException`, sin
  llamador todavía. 3126/3183 pruebas del reactor completo pasan; fallan exactamente los 57 esbozos de F-CJUEZ (54
  `@Tag("FEAT-CJUEZ")` + 3 de `FCjuezJ001JourneyTest`), todos con «Not implemented yet» y nada más.
  Next: implementar `DefaultBatchJudgePass` (estimate/judge/judges) y el paso 1 del cierre en `DefaultBatchCloser`
  (hoy `close(batch, startCourse, request)` ignora `request`); recién ahí los 57 esbozos dejan de tirar
  UnsupportedOperationException.

2026-10-01 — qa-tester (CJUEZ) — Parche sólo de pruebas propuesto (--replace) y validado: 54 handwrittenTests
  (R001 9, R002 10, R003 20, R004 8, R005 7; 23 en DefaultBatchJudgePass, el resto en el cierre, el servicio, la
  publicación, el informe, DefaultBatchComparator, DefaultTrayService y BatchCmd) y F-CJUEZ-J001 en revision-domain,
  com.learney.contentaudit.revisiondomain.batchengine. propose y validate: 0 altas, 11 cambios, 0 bajas, 0 conflictos.
  Next: el orquestador aplica sin --as y genera; verificar en sentinel.yaml los 54 nombres y la ubicación del journey,
  y que generate materialice los 54 stubs y FCjuezJ001JourneyTest (3 caminos).

2026-10-01 — architect (CJUEZ) — Parche propuesto y validado (0 conflictos) y TECH_SPEC.md escrito, sin pruebas.
  Next: el orquestador aplica y genera; después, @qa-tester declara las pruebas de R001 a R005 y J001.
2026-10-01 — analyst (CJUEZ) — REQUIREMENT.md escrito: R001 a R005, F-CJUEZ-J001 (5 nodos, 3 caminos) y
  DOUBT-CJUEZ-VERSION (abierta, rige A). `feature validate`: OK. `requirement validate`: OK, con un solo aviso
  propio, el término de glosario «Translation», que sale del nombre `translation-sense` (FEAT-FILAS tiene el mismo).
  Next: el arquitecto propone el parche sobre el diseño compartido. José contesta DOUBT-CJUEZ-VERSION y confirma las
  interpretaciones anotadas en decisions.md.
