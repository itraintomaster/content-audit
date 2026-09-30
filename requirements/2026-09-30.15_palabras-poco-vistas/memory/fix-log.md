# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — qa-tester — Si este parche se aplica antes que 0.3, `LemmaCountAnalyzerProvider` entra como implementación vacía (sólo nombre y pruebas): propose y validate no lo avisan (lo cuentan como adición).
  why: PatchApplier agrega toda implementación sin `_change` que no existe; con 0.3 aplicado antes, se fusiona.
