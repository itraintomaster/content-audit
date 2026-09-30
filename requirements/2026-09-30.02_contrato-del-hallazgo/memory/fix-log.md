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

2026-09-30 — qa-tester — `patch propose` sin --replace fusiona con PatchMerger, que al tocar una implementación descarta sus `domainLinks` y `glossarySuggestions` (mergeImplementations pasa null). Aquí DefaultRefinerEngine perdía «Tarea de corrección / produces»: se devolvió desde el parche del arquitecto y se reescribió con `--replace`. Reportado a Sentinel: `~/projects/sentinel/.bugs/2026-09-30-08-patch-merger-descarta-domainlinks.md`.
  why: propose no avisa; se ve sólo comparando el parche fusionado con el de HEAD.

2026-09-30 — qa-tester — Un fence de TECH_SPEC que sólo trae `features:` falla en `tech-spec write` («no architectural content»); hay que sumarle un módulo del parche con `_change: modify`, como hizo 0.2.

2026-09-30 — developer — `mvn -o test -pl X` sin `-am` resuelve los módulos hermanos desde ~/.m2, con jars viejos: da errores falsos (FormEntity). Siempre el reactor completo o `-pl X -am`, y `-Dmaven.test.failure.ignore=true` si arriba quedan stubs.
2026-09-30 — test-writer — AuditNode como clave de `Map.of` o HashMap da StackOverflowError: su hashCode recorre parent y children. Usar IdentityHashMap.
2026-09-30 — test-writer — El juez reutiliza el veredicto de un contenido idéntico (huella): en una prueba con varios ejercicios, cada uno con su texto, o todos reciben el mismo veredicto.
2026-09-30 — test-writer — El id de un análisis tiene resolución de segundos: dos `save` en el mismo segundo se pisan. Leer el informe antes de la segunda corrida o usar directorios de trabajo distintos.
2026-09-30 — test-writer — En Java 25, JaCoCo 0.8.12 llena la salida de «Unsupported class file major version» e IllegalClassFormatException: es ruido; contar por el resumen de Maven o los .txt de surefire.
