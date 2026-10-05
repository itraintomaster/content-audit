# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-04 — analyst — R013–R014 agregadas; Codex/ChatGPT queda por omisión y la elección explícita de proveedor/modelo
  prevalece. DOUBT-JUECES-MODELO-OPENAI resuelta con modelo compartido y override por proceso donde exista la perilla.

2026-10-01 — developer (C, líder) — Examen descontaminado: ninguna definición de agente trae un caso del examen
  (escribir/ + examen fijo), y en mini-theory-judge tampoco un ejercicio de los 61 temas de su examen. Guardián en
  quiz-instruction-infrastructure (AgentDefinitionsExamGuardIntegrationTest + agentguard/examen.json, que arma
  scripts/armar_guardia_del_examen.py): 289 violaciones sobre 71ba5182, 0 hoy. Versiones nuevas (configuración por
  omisión): consigna b7162dcc…, traducción 2e7d443f…+ac7abfd9…, una sola respuesta d6ff57d6…, miniteoría 63f16ebd…,
  opción múltiple 4f8b7cdf…. Ninguna rindió el examen todavía.
  Next: el orquestador corre el guardián con --partes escribir,jueces,examinar; después, el examen fijo.

2026-09-30 — developer (C, líder) — Control sobre una copia (scratchpad impl-C/control), con la base del 29/9 y tope 0:
  sin libro, vocabulario 73,92 % (96,5 · 94,2 · 79,5 · 63,5) y plan 4.036; con el libro real de consigna (15.351 líneas,
  huella 00aeeebd… igual antes y después), mismo vocabulario, 3.920 reusados y 7.367 pendientes, plan 4.046 (las 10 de más
  son QUIZ_INSTRUCTION de veredictos reusados: la base 859e96af da lo mismo, 0 diferencias de puntaje en 11.760 nodos).
  Los cuatro jueces sin examen («sin evaluar», 0 consultas, claude falso nunca llamado); la base 859e96af, con su tope de
  500, sí intentó 1 consulta. Agentes de C-26 en .sentinel/agents (5bfc66b1). `mvn -o test`: 2.375, 2.083 pasan, 292 apagadas.
  Next: el orquestador corre el examen fijo con tope (exam run → medir.py → exam record); José da el OK y el costo.

2026-09-30 — test-writer (C) — Las 46 pruebas de la base común (sesión a la vez, libro y fábrica del examen, tiempos del libro,
  runtime, exportador, registro, config), en verde; la del MARKED destapó que el exportador daba gravedad a un hallazgo que sólo
  ordena: el líder lo arregló (worstSeverity null, como dice el contrato).

2026-09-30 — developer (C, líder) — Base común y juez de consigna construidos en ronda1/impl-jueces (e0ad1746..): sesión en lote
  (resolveAll, recorded, unavailableReason), libro y sesiones del examen, DefaultJudgeRuntime, configuración (4 a la vez, tope 0,
  189/h y 60/h), registro de exámenes en .content-audit/judge-exams/<juez>.jsonl, exportador, DefaultJudgeExamService, ExamCmd
  con @Command y «exam» en Main. El juez de consigna abre por el runtime y resuelve en onCourseComplete; 32 pruebas propias en verde.

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
