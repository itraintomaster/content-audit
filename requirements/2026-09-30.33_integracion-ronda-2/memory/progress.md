# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (lo compartido) — FEAT-RONDA2 construido en ronda2/integracion (1e7d5130..405e3de2): compila y
  verde sobre el generate crudo (dbdf904a). Tests: 3.072, 2.458 en verde, 614 apagados, 0 fallas: los 608 stubs de B
  (175), E (273) y F (160) con @Disabled("FEAT-RONDA2: espera a …"), 5 de FEAT-RONDA2 que esperan los proveedores de B
  (Main ×2, get analyzers, el 73,86 % y los 886 archivos con los veinte) y la de F-NLP-R030, que R007 reemplaza. 42 de
  las 47 de FEAT-RONDA2 pasan. `sentinel verify` OK. Control sobre copias (scratchpad/impl-ronda2-comun/control): los
  11.760 nodos publican lo mismo que la ronda 1 (puntajes, hallazgos, números, diagnósticos); vocabulario 73,86 % y
  96,02 · 93,60 · 78,87 · 63,86; 1.364 ejercicios con algún error; plan con las mismas 4.036 tareas de vocabulario y las
  10 de consigna, una por una, más 1.275 de errores nuevas (R008); el curso, 886 archivos idénticos; 0 consultas. El
  caché por oración: 40 s en frío, 6,1 s con todo en el caché y 8,4 s con una oración cambiada; sin ../ittm, exit 1 y
  «Falta la lista de lemas en '…'».
  Próximo: B, E y F construyen lo suyo, sacan sus @Disabled y encienden sus líneas de Main; el qa-tester da de baja o
  re-etiqueta la prueba de F-NLP-R030; R009 y la parte de B de R008 se proponen sobre las piezas de B y E.

2026-10-01 — integrador — Entró en ronda1/integracion el cierre de la ronda 1 (FEAT-CIERRE, 2026-09-30.32, aplicado y
  generado hasta 35172b1c): se juntó en ronda2/integracion y la cadena se rehízo encima con `rehacer-ronda-2.sh`, un solo
  comando. La carpeta de esta integración pasó de .32 a .33 (CIERRE tomó la 32 a las 23:36) y su sentinel-baseline.yaml se
  rehízo con la base nueva (huella 34709717). Resultado igual que antes: 15 de 15 validate, 0 conflictos, 0 choques,
  0 referencias que no se ven; generate aborta por los trece J001; con ellos, 322 archivos nuevos y 92 cambiados, 9
  llamados en main, 334 construcciones en pruebas y 17 fichas que generate rellena con tools null (todos los proveedores).

2026-10-01 — integrador (arquitecto + qa-tester) — `patch propose --replace` de FEAT-RONDA2 (2 altas, 31 cambios, 0
  conflictos) con sus 47 pruebas (9 de las 10 reglas; R009 cae en piezas de E y B y va después de aplicar) y `tech-spec
  write` (10 fences), contra el sentinel.yaml de ronda1/integracion (huella 2d1b1d37). Los trece de B, F y E rebasados
  desde su commit de diseño (B ae83b2d0, E 70c50c1a, F cfb6b20a), cada uno contra el modelo que deja el anterior:
  14 de 14 propose, tech-spec y validate en exit 0, 0 conflictos, 0 pérdidas al proponer (comparar.py). Paso 15:
  clientes-de-paquetes.yaml, once paquetes; con el jar de hoy no deja nada (bug 10).
  Simulado en memoria (integrador-2/cadena.sh y revisar.sh): 0 choques de nombre, 0 tipos sin resolver, 0 referencias que no
  se ven; `generate` aborta por los trece J001 sin ubicar de B, E y F; con ellos, crearía 322 archivos y cambiaría 85; deja
  sin compilar 11 llamados en main y 334 construcciones en pruebas (209 RefinementTask, 78 GetTasksFilter).
  Próximo: el orquestador corre integrador-2/aplicar-ronda-2.sh; los qa-testers de B, E y F ubican sus J001; generate.

2026-10-01 — integrador (analista + arquitecto) — REQUIREMENT.md de FEAT-RONDA2: 10 reglas, sin journeys, dos dudas para
  José (DOUBT-RONDA2-SPACY y DOUBT-RONDA2-TAREAS-B, las dos con A). Sale de los 56 ganchos de B, E y F y del riesgo de
  calidad del orquestador (lista de lemas y caché de tokens).
  Próximo: proponer el parche y rebasar los trece con integrador-2/rehacer-ronda-2.sh.
