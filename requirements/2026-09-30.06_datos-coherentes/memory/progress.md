# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (impl A) — Con la tabla del corrector en producción (#1191) los números no se mueven: 102 ejercicios,
  9 oraciones de producción, 85 audios con barra, 7 audios distintos y 1 sin clave; examen escribir 39 de 39.

2026-09-30 — developer (impl A) — data-coherence construido y encendido en Main (línea 13): DefaultSpokenTextBuilder (la regla
  del 24/8, tts_text y spoken_from_choice_parts), el analizador, su ficha y los dos adaptadores de sólo lectura
  (FileSystemAudioRecordingRegistry, FileSystemProductionSentenceSource, en streaming). db/grabaciones.json armado con
  scripts/armar_grabaciones.py: 11.487 claves, 2.958 de la regrabación del 24/8 y 8.529 del generador de marzo de 2025.
  26 pruebas en verde (analizador 15, texto hablado 2, ficha 1, journey 5, adaptadores 3). Curso del 29/9: 102 ejercicios
  (A1 3, A2 52, B1 43, B2 4): R001 9 (todas de la copia de producción, A2 5 y B1 4; 0 en el árbol), R002 85 audios con
  barra, R003 7, R004 1. Examen escribir: 39 de 39, 9 marcas fuera del oro (las mismas del prototipo del analista).
  Next: paquete E regraba y actualiza el registro en el mismo paso.

2026-09-30 — qa-tester — patch propose OK, fusionado con el parche del arquitecto (4 altas, 2 cambios, 0 conflictos); `patch validate` y `tech-spec write` OK, con -i contra el modelo después de 0.2 y 0.3. 21 handwrittenTests nuevas (0 portadas: no había tests de estas reglas), las 5 reglas [covered]; F-DATOS-J001 en audit-domain, paquete datacoherence. Simulación en memoria (scratchpad/qa-A/SimularQA.java): aterrizan las 21, `feature status` OK, generate crearía 5 caminos y 21 stubs en {Impl}Test nuevos, sin choques de nombre. Sin aplicar.
  Next: integrador (A-01, A-02, A-03, A-16); José aplica sin --as; generate; @test-writer.
  Ojo: con 0.3 aplicado, generate aborta por F-HALL-J001/J002 sin testModule (no es de A: los ubica el qa-tester de 0.3).

2026-09-30 — architect — Parche (4 altas, 2 cambios, 0 conflictos) y TECH_SPEC (4 fragmentos) en exit 0; `patch validate` OK. Sin
  aplicar. Next: integrador (AuditableQuiz.answerAudioKey y el renderizador son ganchos); feature sync; @qa-tester.

2026-09-30 — analyst — Primer pase de la ronda 1 con las decisiones de José: las tres dudas resueltas con A; R001 lee las dos oraciones
  guardadas. Next: @architect (registro de grabaciones y copia de producción como datos de entrada).

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-DATOS (paquete A, ronda 1 del brief 089): 5 reglas, 1 journey, 3 dudas.
  Prototipo en el scratchpad con el manifiesto del 24/8 (sólo lectura) y el generador de marzo 2025 (ittm): 85 audios con
  barra (A2 45, B1 37, B2 3), 7 audios distintos, 1 sin audio, 0 oraciones guardadas mal en el árbol y 9 en la copia de
  producción. En escribir: 34 de 39 con el árbol; 39 de 39 leyendo la copia de producción; 9 marcas nuevas, todas reales.
  Next: José contesta las dudas (sobre todo DOUBT-DATOS-REGISTRO: el registro de grabaciones es un dato nuevo para
  content-audit); el integrador suma la carpeta y corre `feature sync --feature FEAT-DATOS`; después @architect.
