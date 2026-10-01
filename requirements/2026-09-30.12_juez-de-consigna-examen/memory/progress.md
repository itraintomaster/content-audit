# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C) — quiz-instruction-infrastructure implementado (rama ronda1/impl-jueces-infra): AgentJudge (sesión verificada una vez por instancia, sincronizada; sin artifact → explain del terminationReason), DefaultAgentJudgeFactory, su resolver de versión (mismo digest que el de consigna), DefaultJudgeProviderReadinessProbe (claude auth status inyectado, tope 20 s) y los 4 lectores Jackson; el de consigna usa la misma verificación sin cambiar id, versión ni entradas.
  20 pruebas encendidas y en verde (JEXAM 14, TRSEN 3, UNICA 1, MINIT 1, MCVALE 1); `mvn -o test` verde: 1.883 pasan, 487 apagadas, VERIFY OK. Versión fijada del de consigna: e3a546dc… (DefaultQuizInstructionJudgeFactoryTest).

2026-09-30 — qa-tester — `patch propose` exit 0, fusionado con el parche del arquitecto (26 altas, 17 cambios); `tech-spec write` exit 0 (+1 fragmento). 90 handwrittenTests nuevas, R001 a R011 [covered]; F-JEXAM-J001 en audit-cli, paquete commands (2 caminos). Simulado en memoria sobre hoy, sobre 0.2 + 0.3 y sobre 0.2 + 0.3 + A (scratchpad qa-C/sim, sin apply ni generate): las 202 pruebas de C aterrizan, 0 errores de traza, 0 choques de nombre, 36 clases nuevas. R012 sin prueba propia. No aplicado.
  Next: José aplica sin --as (el parche lleva la estructura del arquitecto), después de 0.2 y 0.3 y con FEAT-JEXAM antes que TRSEN, UNICA, MINIT y MCVALE; feature sync; generate; @test-writer.

2026-09-30 — architect — Parche y TECH_SPEC de FEAT-JEXAM: `patch propose` 0, `patch validate` 0 (26 altas, 16
  cambios), `tech-spec write` 0 (14 fragmentos). Requirement corregido como analista con las decisiones de José (seis dudas
  RESOLVED, R010–R012 nuevas, DOUBT-EXAMEN-Y-CANDIDATOS nueva). Ganchos en docs/propuestas/tablero-corregir-sin-empeorar/
  ronda1/ganchos-C.md (ittm). No aplicado.
  Next: José aplica 0.2, 0.3 y el integrado; este parche antes que TRSEN, UNICA, MINIT y MCVALE; feature sync; @qa-tester.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-JEXAM (paquete C, ronda 1): 9 reglas, 1 journey, 6 dudas; `requirement validate` OK, 263 líneas
  (más de las 220 pedidas porque aloja lo común y las 4 dudas de proveedor, tope, muestra y paralelo que no se repiten). Aloja lo común
  a los cinco jueces (examen fijo, costo pendiente, conjunto, consultas a la vez, candidatos de la mesa) y lo que cambia
  del juez de consigna sin tocar lo que lee. Sin tocar sentinel.yaml ni requirements existentes.
  Next: José contesta las dudas; el integrador suma la carpeta a `definitions` y corre `feature sync --feature FEAT-JEXAM`
  (J001); después @architect. Promover al glosario «Juez», «Examen fijo» y «Versión del juez» cuando José apruebe.
