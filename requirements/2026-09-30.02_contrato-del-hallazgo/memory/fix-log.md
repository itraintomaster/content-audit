# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — architect — Al aplicar, `requiresInject` se fusiona por nombre (unión, `PatchApplier.mergeImplementation`): para sacar una inyección hay que listarla con `_change: delete`; hecho en `IAuditEngine`, `DefaultAuditRunner` y `DefaultAnalyzerRegistry`.
  why: `patch propose` no lo detecta; sin el delete, `DefaultAuditRunner` quedaba inyectando `List<EvaluationAnalyzerFactory>` después de borrar la interfaz.

2026-09-30 — architect — `patch propose` no resuelve el `implements` de una implementación de la raíz del módulo contra interfaces de un paquete («implements unknown interface»); de paquete a raíz sí funciona. Por eso `AnalyzerProvider`, `AnalyzerCatalog`, `FindingCollector` y `ContextNumbersCalculator` viven en la raíz.
  why: tres proveedores (SL, KTL, KIL) están en la raíz, como sus analizadores.

2026-09-30 — architect — En el parche, `throws:` en una firma se descarta en silencio; la clave que conserva es `throwsExceptions:`. Los modelos no aceptan `description` (sólo sus campos).
  why: la referencia del DSL dice `throws`; el archivo escrito lo perdía sin error.

2026-09-30 — analyst — Una línea de prosa que empieza con «+ » se renderiza como viñeta: la cuenta del 73,9 % se partió así y se reacomodó.
  why: al cortar párrafos largos a mano, revisar que ninguna línea empiece con «+», «-» o «1.».
