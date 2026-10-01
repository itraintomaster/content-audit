# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (paquete D) — La prueba F-HALL-R003 de lemma-recurrence usaba lemas «w0»…«w99»: con la opción A un
  lema con cifras no es palabra y el puntaje daba 0,0. El fixture usa ahora «waa»…«wdv» (letras), mismo sentido.

2026-09-30 — qa-tester — Si este parche se aplica antes que 0.3, `LemmaRecurrenceAnalyzerProvider` entra como implementación vacía (sólo nombre y prueba): propose y validate no lo avisan.
  why: PatchApplier agrega toda implementación sin `_change` que no existe; con 0.3 aplicado antes, se fusiona.

2026-09-30 — qa-tester — `stats analyzer` no pasa por los DetailedFormatter: StatsAnalyzerCmd arma su vista con AnalyzerStatsTransformer, que no lee diagnósticos.
  why: leído en el código de audit-cli; D-11 sólo alcanza a `analyze --detailed`.
