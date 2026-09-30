# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — José aprobó el diseño de FEAT-HALL y eligió la recomendación en las seis dudas: A en
  DOUBT-EJERCICIO-JUZGADO, DOUBT-HALLAZGO-DE-TEMA, DOUBT-GRAVEDAD-EXISTENTES, DOUBT-FAMILIA-OTRO-NIVEL y
  DOUBT-STATS-DE-LOS-JUECES, y «no por ahora» en el cajón tipado (duda 3 de la ficha). Detalle en decisions.md.
  REQUIREMENT: las cinco dudas RESOLVED (2026-09-30), R008 sin [ASSUMPTION] y el cajón anotado en el Alcance. FICHA:
  `dudas: 0` y la sección 7 con lo decidido. `requirement validate` → [OK], con la misma salida que antes (44 avisos
  del glosario compartido y «Lemma» sin enlace).
  Next: aplicar este parche recién después del de FEAT-OPMUL (0.2), revalidándolo contra el sentinel.yaml nuevo; sumar
  la carpeta a `definitions` (el parche no la suma), `feature sync --feature FEAT-HALL` (J001, J002), `sentinel generate`,
  @qa-tester y @developer. La promoción al glosario que esperaba la aprobación ya se puede hacer; acá no se tocó
  domain-glossary.yaml.

2026-09-30 — architect — Diseño de FEAT-HALL propuesto, sin aplicar: `architectural_patch.yaml` (validado: 16 altas, 16 cambios, 1 baja, 0 conflictos;
  `patch validate` también OK), `TECH_SPEC.md` (16 fragmentos, todos subconjunto del parche) y `FICHA.md` (3 dudas para José).
  El parche registra `features: FEAT-HALL` (sin journeys). No toca el modelo del curso ni los elementos del parche de 0.2 (FEAT-OPMUL).
  Next: José aprueba y contesta; el integrador aplica después de 0.2 y revalida; luego `feature sync --feature FEAT-HALL`
  (J001, J002), `sentinel generate`, @qa-tester y @developer (cambios de cuerpo listados en decisions.md).

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-HALL (paquete 0.3 del brief 087 (hoy 089)): 15 reglas, 2 journeys, 5 dudas.
  Validación: `sentinel requirement validate --file requirements/2026-09-30.02_contrato-del-hallazgo` → [OK]; único aviso
  propio: el término de glosario «Lemma» sin enlace (sale de los nombres `lemma-*`, no bloquea).
  Next: José aprueba el contrato y contesta las 5 dudas. Queda para quien integre, porque tocan sentinel.yaml y acá estaba
  prohibido: sumar la carpeta a `definitions`, correr `feature sync --feature FEAT-HALL` (registra J001 y J002) y el
  diseño de @architect. Después, @qa-tester.
  Glosario: promover «Hallazgo», «Gravedad», «Familia de analizadores» y «Tema» (= knowledge) cuando José apruebe; no
  se tocó domain-glossary.yaml para no chocar con los paquetes en paralelo.
