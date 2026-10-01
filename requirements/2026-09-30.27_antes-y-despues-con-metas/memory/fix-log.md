# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer — En las pruebas, `GoalDeclarationSource.read()` y `SealedGoalsStore.read()` chocan en una misma clase (devuelven tipos distintos): dos objetos, uno por puerto. `ArgumentCaptor.captor()` no existe en Mockito 5.11: `ArgumentCaptor.forClass((Class) List.class)`.

2026-10-01 — qa-tester — Copia del parche antes de proponer (bug 08); después, 0 perdidos: domainLinks 2 → 2 y glossarySuggestions 2 → 2. La descripción queda concatenada con «; » (PatchMerger): se propone una sola vez por carpeta.

2026-09-30 — architect — `tech-spec write` se niega a achicar el TECH_SPEC sin `--replace` («Refusing to shrink»); al reescribir una sección con menos líneas, pasar `--replace`.

2026-09-30 — analyst — `requirement validate --file` pide la carpeta del requirement, no el REQUIREMENT.md («Not a directory» si se le pasa el archivo). Se corrió sobre una copia del worktree en el scratchpad.
  why: la regla del paquete: nada de Sentinel que escriba fuera de la carpeta del requirement.

2026-09-30 — analyst — Prototipo del motor sin tocar el worktree: copiar `*/target/classes` del repo principal, el curso y `.cache/spacy-nlp` al scratchpad, y correr con user.dir en el scratchpad (el caché de spaCy vive en `user.dir/.cache/spacy-nlp`). Con el caché, la base mapea en 0,85 s; un curso nuevo tokeniza en 38 s; `runAudit` tarda 0,05–0,1 s.
  why: `analyze` reescribe archivos versionados; el cableado de Main (7 analizadores + LemmaAbsenceScoreAggregator) se reproduce a mano.
