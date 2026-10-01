# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — test-writer (paquete D) — «Las primeras de B1 son feeling, political…» (R004) no sale con el puesto del
  catálogo: feeling es 853 en el EVP (136 era el de su primera oración) y little (ADV, 238) y face (VERB, 369) abren la
  lista. La prueba fija el bloque de 54 (1.000 más comunes, 1 oración) en orden de puesto y que los siete están en él.

2026-09-30 — qa-tester — Si este parche se aplica antes que 0.3, `LemmaCountAnalyzerProvider` entra como implementación vacía (sólo nombre y pruebas): propose y validate no lo avisan (lo cuentan como adición).
  why: PatchApplier agrega toda implementación sin `_change` que no existe; con 0.3 aplicado antes, se fusiona.
