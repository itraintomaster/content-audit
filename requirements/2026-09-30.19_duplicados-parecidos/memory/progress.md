# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

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
