# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 (madrugada) — developer + test-writer (paquete D) — La forma derivada, construida sobre ronda2/integracion
  (apply b5724f63, generate 9e93a983 y fd91447c, traídos con el merge a6666899): lookupDerivedFormLevel en
  FileSystemEvpCatalog con sus 3 pruebas (f70bf4ee); su uso en el analizador, como el prototipo, y las NUEVE
  construcciones del proveedor (b4f15968); las 15 pruebas del analizador (6f1028a6). Las 18 pasan, y una mutación de
  cada regla las rompe. Control sobre una copia: 99 (9 · 27 · 43 · 20), escribir 26 de 52, vocabulario 73,86 %.
  Suite completa: 3.103 pruebas, 614 apagadas (B, E y F), 0 fallas y 13 errores, todos stubs de FEAT-CALUM sin escribir
  («Not implemented yet»). Para compilar hubo que cablear a mano lo que el generate de CALUM dejó (el renderer de la
  vista del alumno en DefaultQuizInstructionSubjectBuilder y su fábrica, quizInstructions en
  QuizInstructionCorrectionContext): 25 archivos, NO commiteados (es de ronda2/impl-calum); quedó en el scratchpad
  impl-D2/calum-cableado-local.patch.
  Next: el orquestador junta la rama cuando CALUM compile.

2026-10-01 (noche) — qa-tester (paquete D) — 18 handwrittenTests sumadas al parche de la forma derivada, con
  backup antes del propose (scratchpad impl-D2/backup-parche-v3): 15 de OffLevelWordsAnalyzer (R002 forma derivada,
  base, otra categoría en listas y par americano; R003 sufijo del tema; R004 préstamo) y 3 de FileSystemEvpCatalog
  (R002 el método nuevo, R006 las búsquedas por lema sin cambios). propose fusionado y después --replace sólo para
  poner la descripción al día (mismo contenido); TECH_SPEC reescrito (2 cercos, +la sección de pruebas). Validado
  contra ronda2/integracion (d80f7f6a), con y sin el REQUIREMENT de la rama, y contra el de ronda 1: 0 conflictos.
  Next: el orquestador junta la rama en ronda2/integracion, aplica sin --as (el parche lleva estructura) y genera.

2026-10-01 (noche) — developer + analyst (paquete D) — El orquestador aprobó el parche de la forma derivada y
  decidió que el sufijo del tema cubre la palabra que el EVP trae (92bf8a21): nicely, sadly, warmly, beautifully y
  smoking dejan de marcar. Curso: 99 (9 · 27 · 43 · 20), y no 100: rewarding (C1) está en «-ed vs -ing», que enseña
  $-ing$, así que el mismo criterio la cubre; con o sin el parche son las mismas 99 marcas. Respecto de las 95 vuelven
  allergic, ceramic, recess y diligently, las 4 reales; escribir/nivel 26 de 52, 36 en la parte, 10 fuera del oro.
  Revisión a mano de las 43 marcas fuera del oro que nadie había visto: 38 reales y 5 dudosas; con la muestra
  anterior, 62 de 73 (85 %). La línea principal pasa a ronda2/integracion: el parche se valida contra su sentinel.yaml.
  Next: el orquestador junta la rama en ronda2/integracion, aplica y genera; después, lookupDerivedFormLevel en
  FileSystemEvpCatalog, su uso en el analizador (impl-D2/proto-v2/aplicar.py) y las pruebas declaradas.

2026-10-01 (tarde) — developer + analyst + architect (paquete D, segunda corrección) — Medidas por el orquestador
  contra examinar/, las lecturas perdían casos reales: la base pisaba el nivel propio de la palabra. Sus tres reglas,
  en el cuerpo de OffLevelWordsAnalyzer (3595f477): el sufijo del tema, el par británico, el compuesto y el derivado
  valen sólo fuera del catálogo; el préstamo, sólo sustantivos y nunca uno que la oración usa como verbo; los pares
  americanos, sólo los exactos (salen highway, closet, semester, recess, attorney, stove, comforter, pacifier). Además,
  la otra categoría sólo en una lista de palabras. Curso: 95 → 104 (14 · 27 · 43 · 20), vuelven 9 y no se va ninguna;
  escribir/nivel 26 de 52, 41 en la parte, 15 fuera del oro. Revisadas a mano las 48 marcas fuera del oro que nadie
  había visto (scratchpad impl-D2/revision-48.tsv): 38 reales, 9 dudosas, 1 falsa (smoking); con la muestra anterior,
  62 de 78 (79 %), 66 de 78 si cuentan los 4 adverbios de «Adverbios de modo». Suite completa: 2.408 verdes.
  Parche propuesto, sin aplicar y para ahora: EvpCatalogPort.lookupDerivedFormLevel (las formas derivadas que R040
  deja fuera del lema), inyectado en el proveedor y el analizador; validado contra ronda1/integracion (5258cce3).
  Prototipo: smoking se va, rewarding (C1) entra, 104 igual (13 · 27 · 44 · 20), 63 de 78 (81 %). El parche de la
  teoría, en espera, pasó a en-espera/ con su TECH_SPEC (validado contra el mismo sentinel.yaml).
  Next: el orquestador aplica el de la forma derivada y genera; implementar lookupDerivedFormLevel y su uso (el
  prototipo está en impl-D2/proto-v2/aplicar.py), J001 a 13 · 27 · 44 · 20; @qa-tester declara pruebas para las tres
  reglas y el método; decidir si el sufijo del tema cubre los 4 adverbios que el EVP trae (nicely, sadly, warmly,
  beautifully).

2026-10-01 — developer + analyst (paquete D, precisión) — Un verificador vio 5 reales en 25 marcas fuera del examen.
  off-level-words suma lecturas regulares (R002 a–f), miniteoría, sufijo del tema y metalenguaje (R003 3–5) y el
  préstamo adaptado (R004), todo en el cuerpo de OffLevelWordsAnalyzer y medido con el puntuador compartido.
  Curso: 170 → 95 ejercicios (9 · 25 · 42 · 19), 175 → 97 palabras; escribir/nivel: 26 de 52 igual, con 34 marcas en la
  parte y 8 fuera del oro (antes 51 y 25); muestra nueva de 30 (semilla 87, fuera del oro de escribir y de las 25 del
  verificador), revisada a mano: 24 reales, 3 falsas (las tres las enseña la teoría del tema) y 3 dudosas. De las 25 del
  verificador quedan 7: mop, plier, rake, mow, wax, lavender, toolbox. Vocabulario 73,92 % y 4.036 tareas igual (J001).
  Parche de la teoría del tema propuesto (architectural_patch.yaml + TECH_SPEC), sin aplicar: en espera hasta después
  de la ronda 2, por decisión del orquestador (toca AuditableQuiz, como los parches de la ronda 2).
  Next: después de la ronda 2, aplicarlo y adaptar ~314 constructores de AuditableQuiz en pruebas, el mapper y Main;
  @qa-tester declara pruebas para las lecturas nuevas de R002 a R004 (hoy sin pruebas propias).

2026-09-30 — developer + test-writer (paquete D) — FEAT-NIVFR construido: 39 pruebas sin @Disabled (37 + J001).
  off-level-words puntúa con el SentenceLexicalScorer compartido, cuenta dos niveles arriba (C1=5, C2=6) o fuera del
  EVP en la banda fuerte de LemmaAbsenceConfig, exceptúa lo que el tema enseña (DefaultTopicTeachingResolver) y lo
  que la traducción repite (sólo fuera del EVP); un hallazgo F-NIVFR-R002 media/mesa por palabra; Main lo registra 7.º.
  Control sobre una copia, curso del 29/9: 18 · 33 · 81 · 38 = 170 ejercicios, 175 palabras (158 fuera del EVP, 17
  arriba), los mismos 170 ids del prototipo; examen escribir/nivel: 26 de 52 (7/17 A1, 19/35 A2), 51 marcas en la
  parte y 25 fuera del oro.

2026-09-30 — qa-tester — Ronda 1: `patch propose` exit 0, fusionado con el parche del arquitecto: 37 handwrittenTests nuevos (0 portados) y F-NIVFR-J001 en audit-cli, paquete commands; `tech-spec write` exit 0 (+1 sección). No aplicado.
  Simulado en scratch/qa-D (apply en memoria con la librería del jar, sin CLI apply ni generate), sobre el sentinel.yaml de hoy y sobre 0.2 + 0.3 aplicados antes: 37 pruebas aterrizan, 0 errores de traza, J001 ubicado, `feature status` OK, 7/7 reglas [covered]; generate agregaría 37 stubs y FNivfrJ001JourneyTest con 2 caminos, sin choques de nombre.
  Next: el integrador aplica después de 0.2 y 0.3 y sin `--as` (el parche lleva la estructura del arquitecto); José corre generate; @test-writer implementa.

2026-09-30 — architect — Ronda 1: REQUIREMENT al día con lo que decidió José; parche y TECH_SPEC en exit 0, contra el modelo de 0.2 y 0.3.
  Next: el integrador suma D-01 a D-04 y D-12 a D-14 de ronda1/ganchos-D.md; después `feature sync --feature FEAT-NIVFR` y @qa-tester.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-NIVFR (paquete D, ronda 1): 7 reglas, 1 journey, 5 dudas.
  Medido sobre el análisis 2026-09-30T11-54-02 con un prototipo en Python que reproduce las marcas de lemma-absence en
  11.287 de 11.287 ejercicios (scratchpad de la sesión, no quedó en el repo).
  Next: José contesta las 5 dudas y las de FEAT-HALL; el integrador suma la carpeta a `definitions` y corre
  `feature sync --feature FEAT-NIVFR` (J001); después @architect (analizador nuevo, proveedor y ficha) y @qa-tester.
