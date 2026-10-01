# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (paquete B) — GRAMA implementado (26bb9c25 la oración terminada, 90d59f69 el analizador,
  94f7dcb8 el @Generated del journey). Control sobre una copia, LanguageTool 6.8 aprobado sólo en la copia: LanguageTool
  sobre las 12.223 oraciones en unos 21 s; 57 ejercicios con hallazgo (A1 17, A2 10, B1 16, B2 14): english-error 3 bloqueantes,
  22 altas, 13 medias y 7 bajas, final-punctuation 13. medir.py en escribir/: 4 de 47 (8,5 %), 27 marcas en la parte,
  23 fuera del oro: lo que preveía el prototipo.

2026-10-01 — qa-tester (de analista y arquitecto) — R001 inv. 5 escrita (REQUIREMENT, TECH_SPEC y la descripción
  de `finishedsentence`), R003 y el Contexto con la lectura nueva; 4 pruebas más (48): la respuesta en el lugar de la
  lista, su mayúscula, los 40 iguales al audio y …396f sin signo final falso. Backup, `patch propose --replace` (sin
  pérdidas), `tech-spec write` y `patch validate -i` contra mi B02 y M02: exit 0. Simulado en `qa-B2/sim2`. Sin aplicar.

2026-10-01 — qa-tester — 44 handwrittenTests nuevas, las 7 reglas [covered]; F-GRAMA-J001 en audit-domain, paquete
  grammar (3 caminos). `patch propose` con fusión; el bug 08 se llevó domainLinks y glossarySuggestions de
  DefaultFinishedSentenceReader: devueltos desde el parche del arquitecto y re-propuesto con --replace (lo demás, igual).
  `tech-spec write` (+1 fragmento) y `patch validate -i` contra M02 del integrador y contra el modelo con las pruebas de
  MLOCAL: exit 0. Sin aplicar.

2026-09-30 — architect — Sin cambios por la alternativa: Gramática no la necesita (LanguageTool corre en el proceso).
  `patch validate -i` contra el sentinel.yaml de `ronda1/integracion`: exit 0.

2026-09-30 — architect — Diseño propuesto, sin aplicar: `architectural_patch.yaml` (2 altas, 1 cambio, 0 conflictos; `patch
  validate` exit 0) y `TECH_SPEC.md` (7 fragmentos). Dudas resueltas por José y números corregidos en REQUIREMENT
  (`requirement validate` [OK]). Lo compartido, en ittm `…/ronda2/ganchos-B.md`.
  Next: aplicar después de FEAT-MLOCAL (y de la ronda 1 integrada); `feature sync --feature FEAT-GRAMA`; @qa-tester; antes de
  publicar, rendir y registrar el examen de LanguageTool 6.8 (gancho B-19).

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-GRAMA (paquete B, ronda 2 del brief 089): 7 reglas, 1 journey, 2 dudas; las
  dependencias y dónde corre cada herramienta están en FEAT-MLOCAL (2026-09-30.20). `requirement validate` → [OK] sin avisos
  propios. Prototipo en scratchpad/ronda2-B/lt (LanguageTool 6.5 de IntelliJ Grazie, sin instalar nada): 49 ejercicios
  (A1 15, A2 8, B1 16, B2 10), 36 reales, 3 discutibles, 10 falsos; en escribir/gramatica.json 4 de 47.
  Next: José contesta DOUBT-GRAMA-REGLAS y DOUBT-GRAMA-ALCANCE (y las de FEAT-MLOCAL); el integrador suma la carpeta a
  `definitions` y corre `feature sync --feature FEAT-GRAMA` (J001); después @architect.
