# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C) — Implementado en `ronda1/impl-jueces-mcvale` (desde e0ad1746): sujeto (title, instructions, exercise, options), clasificador (forma con `sameAnswer` en el contexto del hueco → droppedAsForm; «(correcta)» bloqueante; 1/0,6/0,3/0), corrida en dos tiempos sobre el runtime común (JUDGED/PENDING/FAILED/NOT_AVAILABLE; `judgmentOf` da null para lo que no se pidió o el examen no juzga, y resuelve antes si falta), fábrica bajo `multiple-choice-validity` y la configuración. 23 pruebas encendidas y en verde; `mvn -o test` completo en verde.
  F-MCVALE-J001 escrito contra el contrato de A y apagado («Espera el analizador de FEAT-OMVAL…»): pasó los 4 caminos contra un analizador mínimo de A, revertido.
  Next: el integrador junta A y C y enciende J001; el lector Jackson (`JacksonChoiceAlternativeVerdictReaderTest`) es de infraestructura.

2026-09-30 — qa-tester — `patch propose` exit 0 (5 altas, 3 cambios); `tech-spec write` exit 0 (+1 fragmento). 24 handwrittenTests nuevas, las 7 reglas [covered]; F-MCVALE-J001 en audit-domain, paquete multiplechoicevalidity (4 caminos). Simulado en memoria sobre hoy, sobre 0.2 + 0.3 y sobre 0.2 + 0.3 + A (scratchpad qa-C/sim, sin apply ni generate): las 202 pruebas de C aterrizan, 0 errores de traza, 0 choques de nombre, 36 clases nuevas. No aplicado.
  Next: José aplica sin --as (el parche lleva la estructura del arquitecto), después de 0.2 y 0.3 y con FEAT-JEXAM antes que TRSEN, UNICA, MINIT y MCVALE; feature sync; generate; @test-writer.

2026-09-30 — architect — Parche y TECH_SPEC de FEAT-MCVALE: `patch propose`, `patch validate` y `tech-spec write` en 0.
  Requirement corregido como analista con las decisiones de José. Lo compartido, en ronda1/ganchos-C.md (ittm). No aplicado.
  Next: aplicar después de FEAT-JEXAM (usa sus tipos); feature sync; @qa-tester; la guía del agente la escribe quien construye.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-MCVALE (paquete C, ronda 1): 7 reglas, 1 journey, 1 duda propia (las
  comunes viven en FEAT-JEXAM). `requirement validate` OK.
  Next: José contesta DOUBT-MCVALE-ANALIZADOR; acordar con el analista del paquete A el nombre de su analizador de opción
  múltiple válida y que su ficha declare costo modelo pago; `feature sync --feature FEAT-MCVALE` (J001) al integrar.
