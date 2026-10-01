# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (paquete B) — NATUR implementado (3fcb571c): los 42 stubs del dominio y el del resolvedor,
  sin @Disabled; la alternativa naturalness-yes-no (059ae1ac). Control sobre una copia, llama3:8b aprobado sólo ahí: 11.105
  consultas en ~14 min (75 ms cada una), 240 marcas (A1 117 con 9,07; A2 123 con 10,0; B1 y B2 sin umbral), una
  segunda corrida igual en 4 s sin consultar nada. Pendiente: calibrar B1 y B2 y rendir el examen (B-19);
  la medida de la cola del 5 % contra traduccion_sentido no está en medir.py (sólo la categoría naturalidad).

2026-10-01 — qa-tester (de analista y arquitecto) — Umbral de A2 en 10,0 (REQUIREMENT, TECH_SPEC y dos pruebas
  renombradas) y una prueba más (39): pregunta por «He did something to help.», nunca por «He did to help.». Backup,
  `patch propose --replace` (sin pérdidas), `tech-spec write` y `patch validate -i` contra mi modelo encadenado
  (`qa-B2/sim2/B03`) y M03: exit 0. Sin aplicar.

2026-10-01 — qa-tester — 38 handwrittenTests nuevas, las 8 reglas [covered]; F-NATUR-J001 en audit-domain, paquete
  naturalness (5 caminos). `patch propose` con fusión sin pérdidas, `tech-spec write` (+1 fragmento) y `patch validate -i`
  contra M03 del integrador y contra el modelo con las pruebas de MLOCAL y GRAMA: exit 0. Sin aplicar.

2026-09-30 — architect — Parche y TECH_SPEC con la alternativa (7 fragmentos), rebasados sobre `ronda1/integracion`;
  propose, tech-spec y validate en exit 0. Next: integrador de la ronda 2; José contesta DOUBT-NATUR-ALTERNATIVA.

2026-09-30 — analyst — REQUIREMENT v2 con la alternativa de claude -p (R008 nueva, R002 a R007 y J001 ajustados,
  DOUBT-NATUR-ALTERNATIVA abierta). `requirement validate` [OK]. Next: @architect.

2026-09-30 — architect — Diseño propuesto, sin aplicar: `architectural_patch.yaml` (1 alta, 1 cambio, 0 conflictos; `patch
  validate` exit 0) y `TECH_SPEC.md` (7 fragmentos). Dudas resueltas por José y umbral de A1 corregido en REQUIREMENT
  (`requirement validate` [OK]). Lo compartido, en ittm `…/ronda2/ganchos-B.md`.
  Next: aplicar después de FEAT-MLOCAL y FEAT-GRAMA; `feature sync --feature FEAT-NATUR`; @qa-tester; calibrar B1 y B2 y
  registrar el examen de llama3:8b antes de publicar (gancho B-19).

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-NATUR (paquete B, ronda 2): 7 reglas, 1 journey, 3 dudas. `requirement validate`
  → [OK] sin avisos propios. Prototipos en scratchpad/ronda2-B/nat: score_tf2.py + eval2.py (SLOR, 19 min A1–A2) y
  judge_yesno.py + eval_yn.py (pregunta sí/no, 6 min A1–A2). Dos corridas seguidas de sí/no dan lo mismo; contra la
  corrida larga, diferencias de hasta 0,025 (por eso F-MLOCAL-R002 inv. 2 guarda y reusa el número).
  Next: José contesta DOUBT-NATUR-METODO (recomendado A, la pregunta), DOUBT-NATUR-MARCA y DOUBT-NATUR-UMBRAL; el integrador
  suma la carpeta y corre `feature sync --feature FEAT-NATUR` (J001); después @architect.
