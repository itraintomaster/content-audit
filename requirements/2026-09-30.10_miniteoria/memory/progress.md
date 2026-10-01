# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (C) — Definición del juez sin casos del examen (guardián en verde); ejemplos de
  defecto inventados cuando el pozo no tenía uno real. Versión nueva 63f16ebd…, sin examen rendido.

2026-09-30 — developer (C) — C-26: definición de agente en `.sentinel/agents/mini-theory-judge` (evaluate → gate_verdict → emit, el grafo del juez de consigna; `dependencies` en el orden del mapa de contenido; artifact `miniTheoryVerdict`; al agotar intentos emite `{"outputUnusable": true}`). `eval/run_evals.py` en verde sin modelo (43 salidas grabadas) y los 15 veredictos canónicos de sus fixtures los lee el lector Jackson de Java. `eval/` entra en la versión, como en el juez de consigna: tocar una fixture es una versión nueva que rinde el examen.
  Next: el examen fijo con tope (orquestador) y `exam record`; las guías traen ítems del examen fijo con su veredicto (escalado).

2026-09-30 — developer (C) — FEAT-MINIT implementado en ronda1/impl-jueces-minit: constructor del sujeto (M1…, E1…), repartidor, analizador (una consulta por tema con runtime.plan + resolveAll, puntaje/hallazgo/diagnóstico en cada ejercicio, cobertura por temas), proveedor («Miniteoría», 4 reglas PAID_MODEL a la mesa, MINI_THEORY sobre QUIZ) y DefaultMiniTheoryConfig (SHOWN_ONLY; 0,3/0,6/0,8). Las 30 pruebas encendidas y en verde (28 audit-domain con runtime, motor y render reales; 2 audit-application); `mvn -o test` completo en verde. Main sigue sin registrarlo (línea 17 comentada, la enciende el líder).
  Next: escalaciones al líder (la cobertura no tiene dónde contar los ejercicios; el repartidor no tiene el render del alumno); el lector Jackson es de otro equipo.

2026-09-30 — qa-tester — `patch propose` exit 0 (5 altas, 3 cambios); `tech-spec write` exit 0 (+1 fragmento). 28 handwrittenTests nuevas, las 7 reglas [covered]; F-MINIT-J001 en audit-domain, paquete minitheoryengine (3 caminos). Simulado en memoria sobre hoy, sobre 0.2 + 0.3 y sobre 0.2 + 0.3 + A (scratchpad qa-C/sim, sin apply ni generate): las 202 pruebas de C aterrizan, 0 errores de traza, 0 choques de nombre, 36 clases nuevas. No aplicado.
  Next: José aplica sin --as (el parche lleva la estructura del arquitecto), después de 0.2 y 0.3 y con FEAT-JEXAM antes que TRSEN, UNICA, MINIT y MCVALE; feature sync; generate; @test-writer.

2026-09-30 — architect — Parche y TECH_SPEC de FEAT-MINIT: `patch propose`, `patch validate` y `tech-spec write` en 0.
  Requirement corregido como analista con las decisiones de José. Lo compartido, en ronda1/ganchos-C.md (ittm). No aplicado.
  Next: aplicar después de FEAT-JEXAM (usa sus tipos); feature sync; @qa-tester; la guía del agente la escribe quien construye.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-MINIT (paquete C, ronda 1): 7 reglas, 1 journey, 2 dudas propias (las
  comunes viven en FEAT-JEXAM). `requirement validate` OK.
  Next: José contesta DOUBT-MINIT-VARIAS (toca su decisión del 30/9) y DOUBT-MINIT-TEMA-GRANDE; el tipo de tarea por tema
  lo define el paquete E; `feature sync --feature FEAT-MINIT` (J001) al integrar.
