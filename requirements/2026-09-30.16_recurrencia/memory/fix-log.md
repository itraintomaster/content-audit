# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — qa-tester — Si este parche se aplica antes que 0.3, `LemmaRecurrenceAnalyzerProvider` entra como implementación vacía (sólo nombre y prueba): propose y validate no lo avisan.
  why: PatchApplier agrega toda implementación sin `_change` que no existe; con 0.3 aplicado antes, se fusiona.

2026-09-30 — qa-tester — `stats analyzer` no pasa por los DetailedFormatter: StatsAnalyzerCmd arma su vista con AnalyzerStatsTransformer, que no lee diagnósticos.
  why: leído en el código de audit-cli; D-11 sólo alcanza a `analyze --detailed`.
