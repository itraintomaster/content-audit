# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

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
