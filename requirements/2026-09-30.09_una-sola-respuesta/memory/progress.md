# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C) — C-26: definición de agente en `.sentinel/agents/single-answer-judge` (evaluate → gate_verdict → emit, el grafo del juez de consigna; `dependencies` en el orden del mapa de contenido; artifact `singleAnswerVerdict`; al agotar intentos emite `{"outputUnusable": true}`). `eval/run_evals.py` en verde sin modelo (33 salidas grabadas) y los 14 veredictos canónicos de sus fixtures los lee el lector Jackson de Java. `eval/` entra en la versión, como en el juez de consigna: tocar una fixture es una versión nueva que rinde el examen.
  Next: el examen fijo con tope (orquestador) y `exam record`; las guías traen ítems del examen fijo con su veredicto (escalado).

2026-09-30 — developer (C) — Implementado en `ronda1/impl-jueces-unica` (desde e0ad1746): proveedor, analizador (junta CLOZE, plan + resolveAll + cobertura común), clasificador, constructor del sujeto y `DefaultSingleAnswerConfig`. 31 pruebas encendidas y en verde (clasificador 11, sujeto 3, analizador 8, proveedor 4, J001 4, config 1), sin ninguna consulta paga; `mvn -o test` completo en verde: 1.894 pasan, 476 apagadas, VERIFY OK.
  Next: el lector Jackson (otro equipo), el `CorrectorMatcher` real (paquete A) y descomentar la línea 16 de `Main` al integrar.

2026-09-30 — qa-tester — `patch propose` exit 0 (5 altas, 3 cambios); `tech-spec write` exit 0 (+1 fragmento). 28 handwrittenTests nuevas, las 7 reglas [covered]; F-UNICA-J001 en audit-domain, paquete singleanswerengine (4 caminos). Simulado en memoria sobre hoy, sobre 0.2 + 0.3 y sobre 0.2 + 0.3 + A (scratchpad qa-C/sim, sin apply ni generate): las 202 pruebas de C aterrizan, 0 errores de traza, 0 choques de nombre, 36 clases nuevas. No aplicado.
  Next: José aplica sin --as (el parche lleva la estructura del arquitecto), después de 0.2 y 0.3 y con FEAT-JEXAM antes que TRSEN, UNICA, MINIT y MCVALE; feature sync; generate; @test-writer.

2026-09-30 — architect — Parche y TECH_SPEC de FEAT-UNICA: `patch propose`, `patch validate` y `tech-spec write` en 0.
  Requirement corregido como analista con las decisiones de José. Lo compartido, en ronda1/ganchos-C.md (ittm). No aplicado.
  Next: aplicar después de FEAT-JEXAM (usa sus tipos); feature sync; @qa-tester; la guía del agente la escribe quien construye.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-UNICA (paquete C, ronda 1): 7 reglas, 1 journey, 2 dudas propias (las
  comunes viven en FEAT-JEXAM). `requirement validate` OK.
  Next: José contesta DOUBT-UNICA-MC y DOUBT-UNICA-FORMA; acordar con el analista del paquete A la frontera de «la misma en
  otra forma» y el nombre de su analizador de respuestas completas; `feature sync --feature FEAT-UNICA` (J001) al integrar.
