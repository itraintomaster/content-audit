# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (impl A) — Con la tabla del corrector en producción (#1191) las copias siguen siendo 23 (A1 17, A2 2,
  B1 4); la prueba de los 14 temas corre con esa tabla.

2026-09-30 — developer (impl A) — exact-duplicates construido y encendido en Main (línea 14): el analizador compara en
  onKnowledge cada ejercicio terminado (FILL, sin la pista, con la primera respuesta o la correcta) con los anteriores del
  tema con CorrectorMatcher.sameAnswer; el primero queda (1) y cada copia lleva 0 y un hallazgo bajo para la mesa que nombra
  al que queda. 17 pruebas en verde (analizador 13, ficha 1, journey 3); la de las 23 copias lee los 14 temas de
  db/english-course. Curso del 29/9: 23 copias (A1 17, A2 2, B1 4), en 2,5 s. Examen escribir: 8 de 15, 9 de 15 contando la
  marca en el gemelo (los 9 exactos; los 6 que faltan son parecidos, del paquete B), 1 marca fuera del oro: el gemelo 2260.
  Next: paquete E reemplaza cada copia en la mesa.

2026-09-30 — qa-tester — patch propose OK, fusionado con el parche del arquitecto (1 alta, 1 cambio, 0 conflictos); `patch validate` y `tech-spec write` OK, con -i contra el modelo después de 0.2 y 0.3. 14 handwrittenTests nuevas (0 portadas: no había tests de estas reglas), las 5 reglas [covered]; F-DUPEX-J001 en audit-domain, paquete exactduplicates. Simulación en memoria (scratchpad/qa-A/SimularQA.java): aterrizan las 14, `feature status` OK, generate crearía 3 caminos y 14 stubs en {Impl}Test nuevos, sin choques de nombre. Sin aplicar.
  Next: integrador (A-01); José aplica sin --as; generate; @test-writer.
  Ojo: con 0.3 aplicado, generate aborta por F-HALL-J001/J002 sin testModule (no es de A: los ubica el qa-tester de 0.3).

2026-09-30 — architect — Parche (1 alta, 1 cambio, 0 conflictos) y TECH_SPEC (3 fragmentos) en exit 0; `patch validate` OK. Sin
  aplicar. Next: integrador; feature sync; @qa-tester.

2026-09-30 — analyst — Primer pase de la ronda 1 con las decisiones de José: las tres dudas resueltas con A. Next: @architect.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-DUPEX (paquete A, ronda 1 del brief 089): 5 reglas, 1 journey, 3 dudas.
  Medido con un prototipo en el scratchpad (no en content-audit): 23 copias exactas en 14 temas (A1 17, A2 2, B1 4, B2 0);
  en escribir encuentra los 9 exactos de 15 (los otros 6 son parecidos, paquete B) sin marcas nuevas.
  Next: José contesta las 3 dudas; el integrador suma la carpeta a `definitions` y corre `feature sync --feature FEAT-DUPEX`
  (prohibido acá tocar sentinel.yaml); después @architect.
