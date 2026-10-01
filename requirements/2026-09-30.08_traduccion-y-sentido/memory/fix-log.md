# Fix Log

Fixes that worked, with a short why. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C) — Una prueba de descarte borrada del árbol de fuentes sigue en `target/test-classes` y surefire la corre en el `mvn -o test` siguiente: borrar también sus `.class` (`find . -name 'Tmp*' -delete`).
