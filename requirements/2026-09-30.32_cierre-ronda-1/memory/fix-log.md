# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (C) — `mvn -o test -pl <módulo>` sin -am toma los módulos hermanos del ~/.m2 (viejos) y
  no compila contra el contrato nuevo: correr pruebas sueltas siempre con `-am`.

2026-09-30 — qa-tester (C) — El aviso «4 reglas sin prueba en el parche» de apply es un punto ciego de
  TestCoverageValidator: sólo mira `tests` de las implementaciones de la raíz del módulo, no `handwrittenTests` ni las de
  los paquetes. La traza real la confirma DryRunGenerate: traceabilityErrors=0.
