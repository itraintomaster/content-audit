# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (construcción E) — El informe da cada fila con el nivel de su propio hito, por el nombre del hito
  como lo busca la selección (`DefaultBatchReportBuilder.levelOf`, `BatchReportLevelsIntegrationTest`). why: F compara los
  cuatro niveles y el informe de la tanda de A1 los llamaba a todos «A1» (los 306 ejercicios con error de A2 leían como
  de A1). Un hito que no es un nivel no entra en esa lista.

2026-10-01 — developer (construcción E) — Una tanda con dos o más propuestas no se podía publicar: `preserved()`
  comparaba el curso entero compuesto contra el alcance de cada propuesta. Ahora compara con `withOnly` (el curso como
  abrió más los pasos de ese ejercicio). why: verifyWithin mira todo el curso, y sólo el ejercicio revisado tiene alcance.

2026-10-01 — developer (construcción E) — Pruebas de la tanda sin generate: `TandaFixture` (revision-domain) arma curso,
  análisis, plan, almacenes, mesa, medidor, comparación, muestra y metas a mano; `TandaCli` (audit-cli) hace lo mismo con
  los almacenes de archivos de la tanda y la bandeja bajo un @TempDir. El localizador y la tabla de partes a campos se
  leen por reflexión (`setAccessible`) hasta el parche.

