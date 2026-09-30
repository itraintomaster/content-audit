# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

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
