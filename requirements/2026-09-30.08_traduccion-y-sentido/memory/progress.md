# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C) — C-26: definición de agente en `.sentinel/agents/translation-sense-from-english` y `.sentinel/agents/translation-sense-from-translation` (evaluate → gate_verdict → emit, el grafo del juez de consigna; `dependencies` en el orden del mapa de contenido; artifact `translationSenseVerdict`; al agotar intentos emite `{"outputUnusable": true}`). `eval/run_evals.py` en verde sin modelo (33 + 33 salidas grabadas) y los 12 + 12 veredictos canónicos de sus fixtures los lee el lector Jackson de Java. `eval/` entra en la versión, como en el juez de consigna: tocar una fixture es una versión nueva que rinde el examen.
  Next: el examen fijo con tope (orquestador) y `exam record`; las guías traen ítems del examen fijo con su veredicto (escalado).

2026-09-30 — developer (C) — FEAT-TRSEN implementado en `ronda1/impl-jueces-trsen`: combinador, constructor del sujeto, analizador, proveedor y `DefaultTranslationSenseConfig`; las 33 pruebas de audit-domain y audit-application encendidas y en verde (combiner 7, subject builder 4, analyzer 13, provider 4, F-TRSEN-J001 4, config 1), con el runtime real, libro en memoria y dos jueces grabados; `mvn -o test` completo en verde (2370, 0 fallas, 474 apagadas; VERIFY OK).
  Decidido al implementar: tope de a pares con parada al primer ejercicio que no entra; un contenido ya agendado en la corrida no se paga dos veces (REUSE_ONLY); FORCE sin tope no se pide si el juez ya tiene veredicto y se pide REUSE_ONLY si no tiene (queda pendiente y su costo se declara); `judges-disagree` lleva la gravedad más alta del juez que reprueba (nunca BLOCKING, LOW si no lista fallas); dos «no» sin ninguna condición = veredicto inconsistente, el ejercicio queda sin evaluar; la evidencia antepone lo que leyeron los jueces (nivel, consigna, ejercicio, oración, traducción).
  Next: el lector Jackson y las dos de `AgentJudgeTest` (otro equipo); descomentar la línea 15 del catálogo en `Main` (integrador).

2026-09-30 — qa-tester — `patch propose` exit 0 (6 altas, 3 cambios contra hoy; AgentJudge cuenta como alta hasta que se aplique FEAT-JEXAM); `tech-spec write` exit 0 (+1 fragmento). 32 handwrittenTests nuevas, las 7 reglas [covered]; F-TRSEN-J001 en audit-domain, paquete translationsenseengine (4 caminos). Simulado en memoria sobre hoy, sobre 0.2 + 0.3 y sobre 0.2 + 0.3 + A (scratchpad qa-C/sim, sin apply ni generate): las 202 pruebas de C aterrizan, 0 errores de traza, 0 choques de nombre, 36 clases nuevas. No aplicado.
  Next: José aplica sin --as (el parche lleva la estructura del arquitecto), después de 0.2 y 0.3 y con FEAT-JEXAM antes que TRSEN, UNICA, MINIT y MCVALE; feature sync; generate; @test-writer.

2026-09-30 — architect — Parche y TECH_SPEC de FEAT-TRSEN: `patch propose`, `patch validate` y `tech-spec write` en 0.
  Requirement corregido como analista con las decisiones de José. Lo compartido, en ronda1/ganchos-C.md (ittm). No aplicado.
  Next: aplicar después de FEAT-JEXAM (usa sus tipos); feature sync; @qa-tester; la guía del agente la escribe quien construye.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-TRSEN (paquete C, ronda 1): 7 reglas, 1 journey, 2 dudas propias (las de
  proveedor, tope, muestra y paralelo viven en FEAT-JEXAM). `requirement validate` OK; único aviso propio: «Translation»
  sin enlace, que sale del nombre `translation-sense`, no del término.
  Next: José contesta DOUBT-TRSEN-COMBINAR y DOUBT-TRSEN-CATALOGO; el integrador suma la carpeta a `definitions` y corre
  `feature sync --feature FEAT-TRSEN` (J001); después @architect.
