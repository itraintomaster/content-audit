# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (construcción E) — Una tanda con dos o más propuestas no se podía publicar: `preserved()`
  comparaba el curso entero compuesto contra el alcance de cada propuesta. Ahora compara con `withOnly` (el curso como
  abrió más los pasos de ese ejercicio). why: verifyWithin mira todo el curso, y sólo el ejercicio revisado tiene alcance.

2026-10-01 — developer (construcción E) — Pruebas de la tanda sin generate: `TandaFixture` (revision-domain) arma curso,
  análisis, plan, almacenes, mesa, medidor, comparación, muestra y metas a mano; `TandaCli` (audit-cli) hace lo mismo con
  los almacenes de archivos de la tanda y la bandeja bajo un @TempDir. El localizador y la tabla de partes a campos se
  leen por reflexión (`setAccessible`) hasta el parche.

