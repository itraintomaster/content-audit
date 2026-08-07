# Fix Log

2026-04-30 — qa-tester — First test patch proposed for FEAT-LAGEN had a critical regression: `revision-domain.lemmaabsence` package would have been silently downgraded from `visibility: public` to `internal` because the proposer normalised the field when my YAML input did not declare it. Re-emitted the patch declaring both packages (`lemmaabsence` public, `lagenopenai` internal) IN FULL with `_change: modify` and every sibling repeated verbatim, plus `visibility:` explicit on both. 14 modifications, 0 conflicts.
  why: when `_change: modify` is implied on a package, the proposer rewrites the whole package node from the input — any field absent from input collapses to the framework default. Always re-emit the full package state when proposing tests at impl level inside it, never trust round-trip preservation.

2026-04-30 — test-writer — Contract reality vs DSL declarations:
  (1) `LagenConfig.temperature` is `double` (primitive), NOT nullable `Double` as DSL declared — generated file uses primitive. Tests for "null when absent" rewritten as "0.0 when absent" per actual sentinel value. (2) `LemmaAbsenceMvpStrategy` constructor is (generator, providerId) — DSL only declared `generator` in requiresInject but developer added `providerId` String. Fix: instantiate with PROVIDER_ID in tests. (3) `LemmaAbsenceLlmGenerator` is package-private; constructor is (ChatLanguageModel, promptBuilder, responseParser, errorClassifier, strategyName) — NOT (LagenConfig, ...) as DSL suggested. Tests mock ChatLanguageModel directly using `anyList()` matcher for the `generate(List<ChatMessage>)` overload.
  why: these are generated-file-vs-DSL discrepancies that do not block the test suite but affect how the tests assert. Logged so future sessions do not re-diagnose.

2026-04-30 — test-writer — LangChain4j ChatLanguageModel has multiple generate() overloads; Mockito's `any()` is ambiguous. Fix: use `anyList()` to target `generate(List<ChatMessage>)` specifically in when/verify. Same fix needed for argThat matchers — use typed lambda `(List<ChatMessage> messages) -> ...`.
  why: ambiguous method reference error at compile time; `anyList()` resolves to the correct overload.

2026-04-30 — developer — Blast radius of strategy→lemmaabsence rename. Fixed 4 FEAT-LAPS journey tests:
  (1) Updated imports from revisiondomain.strategy.* to revisiondomain.lemmaabsence.*
  (2) Updated strategy active name from "lemma-absence-mvp" to "lemma-absence-llm" in all 4 files.
  Added single-arg constructor to LemmaAbsenceMvpStrategy for backward compat with journey tests that
  create it without providerId. Two-arg constructor also present for production use + MvpStrategyTest.
  why: architect's decisions.md explicitly called out these 4 journey tests as the developer's blast radius.

2026-04-30 — developer — 3 LemmaAbsenceLlmGeneratorTest failures are JaCoCo/ByteBuddy vs Java 24 issue.
  Tests that use thenThrow(ConnectException) or thenThrow(SocketTimeoutException) fail because JaCoCo
  can't instrument ByteBuddy-generated Mockito proxies (unsupported class file major version 68).
  Not a logic error in production code. Same root cause as pre-existing failures in other modules.
  why: JaCoCo 0.8.12 does not support Java 24 (class file major version 68). Pre-existing infra issue.

2026-08-04 — developer — El algoritmo original del criterio (ahora `DefaultLlmProviderResolver`) se recupero de `git show HEAD:.../DefaultQuizInstructionJudgeProviderResolver.java` — el archivo ya no existia en el working tree (`sentinel generate` lo borro al aplicar ARCH-LAGEN-R015) pero seguia en el ultimo commit. `git show HEAD:<path>` es la via rapida para recuperar el cuerpo de un archivo `@Generated` que un patch acaba de reemplazar, sin tener que re-derivar la logica de memoria.
  why: evita re-inventar (o desviarse de) un algoritmo ya escrito y ya probado por 6 handwrittenTests de R016 que siguen vivos.

2026-08-04 — developer — Para exigir `endpoint` solo cuando la clase de proveedor lo necesita sin duplicar el default "claude-cli" en dos lugares: `DefaultLagenConfigResolver.resolve()` deja de auto-sustituir el proveedor por defecto el mismo y usa directamente `LlmProviderResolved.getProviderName()` (que ya es el nombre efectivo, defaulted adentro del criterio compartido) para `config.setProviderName(...)`.
  why: antes del cambio el resolver Y el criterio tenian, cada uno, su propia constante `"claude-cli"` — exactamente la clase de duplicacion que R015 fue escrita para eliminar.

2026-08-04 — developer — Al reemplazar el tipo de excepcion que lanza un metodo, buscar TODOS los `catch` de la excepcion vieja en el repo (`grep -rn "catch (ViejaExcepcion"`), no solo los tests. `Main.java` tenia DOS `catch (InvalidLagenConfigException e)` en el composition root que dejaron de atrapar `MissingLagenProviderInputException`/`UnsupportedLagenProviderException` (excepciones hermanas, ninguna extiende a la otra) — sin el multi-catch, un operador con el proveedor mal configurado hacia crashear CUALQUIER comando del CLI en el arranque, no solo `revise`. Ningun test existente lo cubria (los tests instancian `DefaultLagenConfigResolver` directo, nunca pasan por `Main.main()`).
  why: cambiar el tipo en el punto donde se lanza sin revisar donde se atrapa cambia un bug silencioso por uno ruidoso; el grep de "quien atrapa esto" es tan necesario como el de "quien lo lanza".

2026-08-04 — developer — Una corrida de `mvn -o test` (reactor completo, sin -pl) lanzada con `nohup ... &` + `run_in_background` puede recibir la notificacion de "completed, exit code 0" ANTES de que el archivo de log termine de escribirse — en un caso el log se corto a mitad de un test run de audit-cli sin ninguna linea de `BUILD SUCCESS/FAILURE`, aun despues de esperar a que el conteo de lineas se "estabilizara" dos veces seguidas. La corrida en foreground (sin `&`, con `timeout` generoso) SI llego limpia a `BUILD SUCCESS` con el mismo comando. Cuando el resultado de una corrida en background no incluye la linea `BUILD SUCCESS`/`BUILD FAILURE`, no confiar en el tally parcial — relanzar en foreground.
  why: perder tiempo diagnosticando un log truncado (buscando procesos zombies, hs_err, etc.) cuando el fix real era simplemente no confiar en la corrida en background para el reporte final.

2026-08-04 — test-writer — `DefaultLagenConfigResolver.resolve()` (produccion) NO lanza `MissingLagenProviderInputException`/`UnsupportedLagenProviderException` para los dos motivos de rechazo de R015 — lanza `InvalidLagenConfigException` para ambos, con `key` fijo en `"CONTENT_AUDIT_LAGEN_ENDPOINT"` (equivocado en el caso de proveedor no soportado, que no tiene nada que ver con el endpoint). Confirmado corriendo `FLagenJ007JourneyTest` contra el jar real (no es suposicion de lectura de codigo): path-2 y path-3 fallan con `Unexpected exception type thrown ... but was: InvalidLagenConfigException`. Los dos tipos nuevos SI existen en sentinel.yaml (audit-cli.bootstrap, visibility:public, confirmado via `grep` directo sobre sentinel.yaml — el resource doc `02-models.md` los marca "internal" pero esta desactualizado, no confiar en el sobre visibility). No se toco el test para aceptar el tipo viejo (Regla 11).
  why: futura sesion de @developer no necesita re-descubrir esto corriendo el test; puede ir directo a re-cablear el resolver para lanzar los 2 tipos declarados.

2026-08-04 — test-writer — Fixtures de nombre de proveedor ya validados contra el `DefaultLlmProviderResolver` REAL (no hace falta mockear `LlmProviderResolver` ni adivinar el algoritmo de `LlmFactory`): `"claude-cli"` → LOCAL_SESSION (es ademas el default del sistema cuando no se declara proveedor); `"some-remote-service"` → REMOTE_SERVICE sin default conocido (direccion indeterminable); `"unknown:provider"` (con ':') → UNSUPPORTED_PROVIDER. Los tres estan tomados de `DefaultQuizInstructionJudgeFactoryTest`/`DefaultLagenConfigResolverTest`, que ya los prueban contra el criterio real — reusarlos evita que los dos consumidores de F-QINST-R016/F-LAGEN-R015 diverjan en el vocabulario de fixtures.
  why: `DefaultLlmProviderResolver` no tiene handwrittenTests propios (es un wrapper sobre una libreria externa) — su comportamiento solo es observable empiricamente via estos tests hermanos, no via sentinel.yaml.
