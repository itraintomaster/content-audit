# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (impl A) — Los tokens de la respuesta se ubican por la secuencia de sus palabras cortadas como
  spaCy (haven't → have + n't) dentro de la oración guardada; buscarlos por pertenencia tomaba el «have» del texto fijo
  («You have a headache. You ___ (not / play)…») como verbo sin anunciar.
2026-09-30 — developer (impl A) — «'m» de una oración guardada con espacio («I 'm especially…») sale de spaCy como «'» y
  «m»: la palabra de la pista se busca también en las palabras de la respuesta, con lo contraído leído por su lema.
2026-09-30 — developer (impl A) — «UK»/«US» son notas sólo en mayúscula: en minúscula «us» es pronombre («(us / ourselves)»).

2026-09-30 — architect — `patch propose` rechaza `implements: [AnalyzerProvider]` porque el puerto es de 0.3 y no está en sentinel.yaml
  (los tipos en firmas y requiresInject sí pasan). Se validó con `-i` contra el modelo después de 0.2 y 0.3, armado en memoria con
  la librería de Sentinel (PatchApplier + registerRequirementFeature, en scratchpad/ronda1-A/Simular.java, sin `patch apply` ni tocar
  el worktree). El primer propose fallido de RESPC había dejado un baseline de antes de 0.2: se borró y se regeneró con el modelo nuevo.
  why: el diseño es contra el modelo después de 0.2 y 0.3; el arquitecto de D eligió declarar el proveedor como clase y dejar el implements como gancho.

2026-09-30 — analyst — En el prototipo, lematizar la respuesta sola («hated», «'s») da basura; lematizar la oración terminada
  (form.sentences[0]) y sumar la forma flexionada regular (-s, -ed, -ing, -er, -est, -ly, con e/y/consonante doble) lo arregló.
