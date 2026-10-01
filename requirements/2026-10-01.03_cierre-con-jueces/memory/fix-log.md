# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (CJUEZ) — `mvn test -pl revision-domain,audit-cli -am` saltea `audit-cli` por completo
  (SKIPPED) aunque se pase `-fae`: como `revision-domain` tiene fallos reales de test (los 57 esbozos, no sólo de
  compilación), Maven saltea cualquier módulo que dependa de uno fallado, con o sin `-fae` (`-fae` sólo sigue con
  módulos independientes del que falló). Para ver el resultado de `audit-cli` en la misma corrida hace falta sumar
  `-Dmaven.test.failure.ignore=true` (no instala nada, sólo evita que el build aborte; surefire sigue reportando cada
  fallo igual).
2026-10-01 — developer (CJUEZ) — `BatchClosing`/`BatchEstimate`/`BatchReport`/`BatchServiceConfig`: `generate` ya
  había actualizado solo las construcciones de producción que no cambiaban de método (`DefaultBatchCostEstimator`,
  `DefaultBatchReportBuilder`) con los campos nuevos al final en null/0; sólo faltaban los call-sites de las pruebas,
  mismo patrón (null para `judges`/`request`, 0/0.0 para los contadores agregados al final).
  why: evita tocar dos veces lo que el smart-merge de sentinel ya había resuelto.
