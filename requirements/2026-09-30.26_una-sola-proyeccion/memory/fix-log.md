# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — Para sumar una prueba a un parche ya fusionado sin concatenar otra vez la descripción: copia del fusionado y del parche del arquitecto (bug 08), el del arquitecto de vuelta desde git y propose del parche de pruebas entero. Comparado con el fusionado anterior: 0 perdidos y sólo la prueba nueva.

2026-10-01 — qa-tester — Antes de proponer, copia del parche y del TECH_SPEC (bug 08); comparado después con comparar.py del integrador: 0 perdidos, domainLinks 1 → 1 y glossarySuggestions 2 → 2 (están en modelos y campos, que el parche de pruebas no toca).
  why: la fusión los pierde sólo en las implementaciones que toca; el parche del qa-tester no declara patterns, así que el bug 11 tampoco aplica.
2026-10-01 — qa-tester — «reglas sin prueba en el parche (aviso)» de Aplicar sale 7 aunque las 7 tengan prueba: TestCoverageValidator cuenta sólo DeclarativeTest. La cobertura de handwrittenTests se mira con Revisar, sección 5.

2026-09-30 — analyst — `requirement validate --file` pide la carpeta del requirement, no el REQUIREMENT.md («Not a directory» si se le pasa el archivo). Se corrió sobre una copia del worktree en el scratchpad.
  why: la regla del paquete: nada de Sentinel que escriba fuera de la carpeta del requirement.

2026-09-30 — analyst — Prototipo del motor sin tocar el worktree: copiar `*/target/classes` del repo principal, el curso y `.cache/spacy-nlp` al scratchpad, y correr con user.dir en el scratchpad (el caché de spaCy vive en `user.dir/.cache/spacy-nlp`). Con el caché, la base mapea en 0,85 s; un curso nuevo tokeniza en 38 s; `runAudit` tarda 0,05–0,1 s.
  why: `analyze` reescribe archivos versionados; el cableado de Main (7 analizadores + LemmaAbsenceScoreAggregator) se reproduce a mano.
