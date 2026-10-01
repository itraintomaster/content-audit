# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (impl A) — Prototipo, sin encender, de «la reescritura pide palabras que no salen de la oración de
  partida ni de la pista» (scripts/prototipo_pista_reescritura.py). En las oraciones enteras, cada palabra de contenido de la
  primera respuesta (tokens de spaCy de la oración guardada) tiene que salir del enunciado, la pista o la consigna, por lema,
  con la flexión regular, la derivación en -ly y las formas irregulares; listas cerradas para lo que la reescritura agrega
  sola (auxiliares de la respuesta corta, el perfecto, «how long», pronombres indefinidos, los desplazamientos de tiempo del
  estilo indirecto). Curso del 29/9: 60 marcas entre 3.957 oraciones enteras (A1 10, A2 4, B1 34, B2 12). De los 27 casos
  sin marca de escribir/pista.json marca 11 (10 que hint-coherence no veía: con los 5 de hoy, 15 de 27). Precisión: dos
  muestras de desarrollo (8 de 30 reales la primera, 19 de 30 la segunda, de ahí los arreglos genéricos) y una muestra
  nueva de 30 revisada a mano: 25 reales (83 %); revisadas las 60 marcas finales, 56 reales (93 %). Las 4 falsas: la
  fórmula del pedido (want → «Could I have…?», ×2), el «did» de una pregunta de objeto y «must → had to» del estilo
  indirecto. Pasa el 80 %, pero encenderla necesita un parche: un valor nuevo en HintIssueKind (generado; reusar
  VERB_NOT_ANNOUNCED para sustantivos sería mentir en el diagnóstico) y su prueba declarada. No hace falta un puerto.
  Next: el coordinador decide el parche; el texto de la regla para el REQUIREMENT quedó en el informe.

2026-10-01 — developer (impl A) — R001 pasa a la unión (tipo o tema entero) y «(100%)» cuenta como pista: hint-coherence marca
  48 ejercicios (A1 13, A2 9, B1 20, B2 6): 13 sin pista, 46 de palabras y 4 de sujeto; 31 por regla y 32 a la mesa.
  Examen escribir: 20 de 46 (43 %), 15 de 15 anotados de pista, 2 marcas fuera del oro. La prueba de El posesivo sigue en
  verde y la de R001 suma el caso de la unión (Adverbios de frecuencia).

2026-09-30 — developer (impl A) — hint-coherence construido y encendido en Main (línea 10): DefaultHintReader,
  DefaultHintAnswerCheck, HintCoherenceAnalyzer y su proveedor, con la tabla de formas irregulares instalada. 29 pruebas
  en verde (4 del lector, 12 del chequeo, 9 del analizador, 1 de la ficha, 3 del journey). Sobre el curso del 29/9: 42
  ejercicios con hallazgo (A1 8, A2 8, B1 20, B2 6): 7 sin pista, 46 de palabras, 4 de sujeto, 0 de not; 25 por regla y
  32 a la mesa. Examen escribir: 14 de 46, 9 de los 15 anotados de pista, 2 marcas fuera del oro.

2026-09-30 — qa-tester — patch propose OK, fusionado con el parche del arquitecto (3 altas, 2 cambios, 0 conflictos); `patch validate` y `tech-spec write` OK, con -i contra el modelo después de 0.2 y 0.3. 28 handwrittenTests nuevas (0 portadas: no había tests de estas reglas), las 5 reglas [covered]; F-PISTA-J001 en audit-domain, paquete hintcoherence. Simulación en memoria (scratchpad/qa-A/SimularQA.java): aterrizan las 28, `feature status` OK, generate crearía 3 caminos y 28 stubs en {Impl}Test nuevos, sin choques de nombre. Sin aplicar.
  Next: integrador (A-01, A-16); José aplica sin --as; generate; @test-writer.
  Ojo: con 0.3 aplicado, generate aborta por F-HALL-J001/J002 sin testModule (no es de A: los ubica el qa-tester de 0.3).

2026-09-30 — architect — Parche (3 altas, 2 cambios, 0 conflictos) y TECH_SPEC (5 fragmentos) en exit 0; `patch validate` OK contra
  el modelo después de 0.2 y 0.3. Sin aplicar. Next: José contesta DOUBT-PISTA-CUANDO-MESA y DOUBT-PISTA-LEMATIZADOR; integrador.

2026-09-30 — analyst — Primer pase de la ronda 1 con las decisiones de José: umbral de tres cuartos (R001), tres dudas resueltas y dos
  nuevas (DOUBT-PISTA-CUANDO-MESA, DOUBT-PISTA-LEMATIZADOR). Next: @architect.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-PISTA (paquete A, ronda 1 del brief 089): 5 reglas, 1 journey, 3 dudas.
  Prototipo en el scratchpad (spaCy en_core_web_sm local): 87 hallazgos con umbral de tres cuartos (A1 14, A2 10, B1 39,
  B2 24), 173 con la mitad. En escribir: 21 de 46 imposibles, los 15 anotados de pista por el 077, y 3 marcas nuevas.
  Next: José contesta las dudas (umbral, respuesta a la mesa, transformaciones); el integrador suma la carpeta y corre
  `feature sync --feature FEAT-PISTA`; después @architect, que tiene que resolver los verbos irregulares (ver decisions).
