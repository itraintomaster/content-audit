# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer + analyst (paquete D, precisión) — Un verificador vio 5 reales en 25 marcas fuera del examen.
  off-level-words suma lecturas regulares (R002 a–f), miniteoría, sufijo del tema y metalenguaje (R003 3–5) y el
  préstamo adaptado (R004), todo en el cuerpo de OffLevelWordsAnalyzer y medido con el puntuador compartido.
  Curso: 170 → 95 ejercicios (9 · 25 · 42 · 19), 175 → 97 palabras; escribir/nivel: 26 de 52 igual, con 34 marcas en la
  parte y 8 fuera del oro (antes 51 y 25); muestra nueva de 30 (semilla 87, fuera del oro de escribir y de las 25 del
  verificador), revisada a mano: 24 reales, 3 falsas (las tres las enseña la teoría del tema) y 3 dudosas. De las 25 del
  verificador quedan 7: mop, plier, rake, mow, wax, lavender, toolbox. Vocabulario 73,92 % y 4.036 tareas igual (J001).
  Parche de la teoría del tema propuesto (architectural_patch.yaml + TECH_SPEC), sin aplicar.
  Next: el integrador decide si lo aplica (adaptar ~314 constructores de AuditableQuiz en pruebas, el mapper y Main);
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
