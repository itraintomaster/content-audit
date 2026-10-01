# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (paquete B) — Examen rendido y registrado en `~/projects/learney/content-audit-corridas/2026-10-01-examen-idioma/`:
  «nomic-embed-text@0a109f422b47 · clustering-1» REPROBADO por la parte 2. Parte 1: los 6 casi iguales de escribir/ y
  el de examinar/. Parte 3: dos corridas del curso entero, cada una con sus 4.633 vectores, dan los mismos 11.287
  veredictos (45 copias: 25 del comodín, 20 por parecido). Parte 2, muestra ciega de 40 + 40 (semilla 87; los no
  marcados, de 71 pares de 0,90 a 0,95 con la misma respuesta), revisada sin la clave por el paquete B, no por José:
  marcados 39 de 40 son el mismo ejercicio; no marcados, 20 de 40 (tope 8). Contando como distintos los 5 dudosos
  quedan 15. Se escapan cosas que el comodín no cubre —otro nombre («Michael» / «He»), otro sujeto («My neighbors» /
  «I»), negación por pregunta, «another tea» / «tea»— y paráfrasis; bajar el umbral no alcanza: entre 0,90 y 0,95 hay
  tantos iguales como distintos, de punta a punta.
  Next: José revisa la muestra (`revision/near-duplicates-muestra-ciega.json`, unos 20 min; si dice otra cosa se
  registra de nuevo) y decide; ampliar el comodín es la próxima versión, con su examen y una muestra nueva.

2026-10-01 — developer (paquete B) — DUPAR implementado (11fa7369): los 43 stubs sin @Disabled; la alternativa
  near-duplicates-same-exercise (059ae1ac). Control sobre una copia con nomic-embed-text aprobado sólo ahí: 4.658
  vectores (20,7 MB) en ~60 s la primera vez y 1 s después; 45 copias (A1 23, A2 9, B1 12, B2 1): 25 por el comodín y 20 por parecido. medir.py en
  escribir/, duplicados: 15 de 15 con exact-duplicates; near-duplicates solo, 8 de 15 (los 6 casi iguales y 2 exactos
  que también son casi copias).

2026-10-01 — qa-tester — 38 handwrittenTests nuevas, las 8 reglas [covered]; F-DUPAR-J001 en audit-domain, paquete
  nearduplicates (5 caminos). `patch propose` con fusión sin pérdidas, `tech-spec write` (+1 fragmento) y `patch validate
  -i` contra M04 del integrador y contra el modelo con las pruebas de MLOCAL, GRAMA y NATUR: exit 0. Sin aplicar.

2026-09-30 — architect — Parche y TECH_SPEC con la alternativa (6 fragmentos), rebasados sobre `ronda1/integracion`;
  propose, tech-spec y validate en exit 0. Next: integrador de la ronda 2; José contesta DOUBT-DUPAR-PARECIDO-TEXTO y
  DOUBT-DUPAR-A-MEDIAS.

2026-09-30 — analyst — REQUIREMENT v2 con la alternativa de claude -p (R008 nueva; R002, R004 a R007 y J001 ajustados;
  dos dudas abiertas). `requirement validate` [OK]. Next: @architect.

2026-09-30 — architect — Diseño propuesto, sin aplicar: `architectural_patch.yaml` (1 alta, 1 cambio, 0 conflictos; `patch
  validate` exit 0) y `TECH_SPEC.md` (5 fragmentos). Dudas resueltas por José en REQUIREMENT (`requirement validate` [OK]).
  Lo compartido, en ittm `…/ronda2/ganchos-B.md`.
  Next: aplicar después de FEAT-MLOCAL y de la ronda 1 integrada (`CorrectorMatcher`, `QuizStudentViewRenderer`); `feature
  sync --feature FEAT-DUPAR`; @qa-tester; registrar el examen de nomic-embed-text antes de publicar (gancho B-19).

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-DUPAR (paquete B, ronda 2): 7 reglas, 1 journey, 2 dudas. `requirement
  validate` → [OK] sin avisos propios. Prototipo en scratchpad/ronda2-B/dup (nomic-embed-text en Ollama, ya instalado, 11.287
  ejercicios en ~4 min; 200.953 pares del mismo tema): 45 pares, 44 copias (A1 23, A2 9, B1 12, B2 0); a mano, 31 iguales,
  10 dudosos, 4 no; los 6 casi iguales de escribir/duplicados.json encontrados.
  Next: José contesta DOUBT-DUPAR-CUENTA y DOUBT-DUPAR-UMBRAL; el integrador suma la carpeta y corre
  `feature sync --feature FEAT-DUPAR` (J001); acordar con el paquete A que los exactos quedan en FEAT-DUPEX.
