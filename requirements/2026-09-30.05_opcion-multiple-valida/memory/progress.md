# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (impl A) — Con la tabla del corrector en producción (#1191, desde el 1/10/2026) R002 cuenta los 28
  ejercicios con dos incorrectas que son la misma respuesta (38 pares: A1 20 en Elegir la forma natural, A2 8 en
  Obligación): 354 ejercicios con hallazgo (A1 109, A2 115, B1 87, B2 43), 344 de mayúscula, 38 de opciones iguales y 2 de
  artículo. El examen escribir sigue en 186 de 186.

2026-09-30 — developer (impl A) — multiple-choice-validity construido y encendido en Main (línea 12) con la fábrica «no
  disponible» (UnavailableChoiceAlternativeJudgeRunFactory): DefaultOptionRuleCheck, el analizador en dos tiempos (reglas y
  pedido al juez en onQuiz; puntaje, diagnóstico, «a medias» y cobertura en onCourseComplete) y su ficha de cinco reglas,
  costo modelo pago. 30 pruebas en verde (reglas 12, analizador 10, ficha 1, fábrica 1, journey 6). Curso del 29/9 con la
  copia de hoy: 326 ejercicios (A1 89, A2 107, B1 87, B2 43): 344 de mayúscula (304 al abrir, de ellos 69 con guion, y 40 con
  «i»), 2 de artículo; con la tabla del corrector nuevo suman los 28 de dos incorrectas iguales (38 pares) y quedan 354.
  Examen escribir: 186 de 186, 10 marcas fuera del oro (las mismas del prototipo del analista).
  Next: al juntar con C, Main vuelve a choiceAlternativeJudgeRunFactory (C-15) y se mide el juez con su examen.

2026-09-30 — qa-tester — patch propose OK, fusionado con el parche del arquitecto (3 altas, 1 cambio, 0 conflictos); `patch validate` y `tech-spec write` OK, con -i contra el modelo después de 0.2 y 0.3. 24 handwrittenTests nuevas (0 portadas: no había tests de estas reglas), las 5 reglas [covered]; F-OMVAL-J001 en audit-domain, paquete multiplechoicevalidity. Simulación en memoria (scratchpad/qa-A/SimularQA.java): aterrizan las 24, `feature status` OK, generate crearía 6 caminos y 24 stubs en {Impl}Test nuevos, sin choques de nombre. Sin aplicar.
  Next: integrador (A-01, A-07, A-08, A-15); José aplica sin --as; generate; @test-writer.
  Ojo: con 0.3 aplicado, generate aborta por F-HALL-J001/J002 sin testModule (no es de A: los ubica el qa-tester de 0.3).

2026-09-30 — architect — Parche (3 altas, 1 cambio, 0 conflictos) y TECH_SPEC (5 fragmentos) en exit 0; `patch validate` OK. Dos dudas
  nuevas en el REQUIREMENT (DOUBT-OMVAL-JUEZ-A-MEDIAS, DOUBT-OMVAL-CORRIDA-BASE). Next: José contesta; el orquestador reconcilia A-07 con C.

2026-09-30 — analyst — Primer pase de la ronda 1 con las decisiones de José: R002 suma contraída/larga y grafías (0 con la correcta,
  28 con dos incorrectas), cuatro dudas resueltas. Next: @architect, con el lugar por donde entran los veredictos del juez de C.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-OMVAL (paquete A, ronda 1 del brief 089): 5 reglas, 1 journey, 4 dudas.
  Prototipo en el scratchpad: R003 marca 324 (A1 89, A2 107, B1 87, B2 41) contra 315 del 077; en escribir encuentra
  los 186 y marca 10 más con la misma falta. R001 y R002 dan 0; R004 da 2 (B2).
  Next: José contesta las dudas; el paquete C tiene que usar el mismo nombre para su juez (DOUBT-OMVAL-JUEZ); el integrador
  suma la carpeta y corre `feature sync --feature FEAT-OMVAL`; después @architect. Depende de FEAT-OPMUL (rama ola0-deuda-mc).
