# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (impl A) — Formas falsas del generador que daban hallazgos de más: «let isn't», «are I not», «has supposed»,
  «is ever watched», «would already eaten», «which's», «they've» después de un auxiliar invertido, «'s john not there?». Un salteo
  demasiado ancho para las preguntas negativas bajó el examen de 123 a 113 de 128; se acotó a la negación seguida de un sintagma
  nominal («Isn't John there?») y volvió a 123.
  why: medir contra escribir después de cada ajuste; el salteo ancho tapaba formas que sí faltan.

2026-09-30 — developer (impl A) — El hueco se compara con el texto de al lado (las partes TEXT vecinas, como contextOf de
  evaluation_cloze.go): sin eso «'s» no se lee como «It's» y el corrector nuevo parecía rechazar «is».

2026-09-30 — qa-tester — Para sumar una prueba después del primer propose sin duplicar la descripción (PatchMerger la concatena con «; »), se restauró el parche del arquitecto con `git checkout --` y se volvió a proponer entero: quedó byte a byte igual al ensayo.

2026-09-30 — architect — `patch propose` rechaza `implements: [AnalyzerProvider]` porque el puerto es de 0.3 y no está en sentinel.yaml
  (los tipos en firmas y requiresInject sí pasan). Se validó con `-i` contra el modelo después de 0.2 y 0.3, armado en memoria con
  la librería de Sentinel (PatchApplier + registerRequirementFeature, en scratchpad/ronda1-A/Simular.java, sin `patch apply` ni tocar
  el worktree). El primer propose fallido de RESPC había dejado un baseline de antes de 0.2: se borró y se regeneró con el modelo nuevo.
  why: el diseño es contra el modelo después de 0.2 y 0.3; el arquitecto de D eligió declarar el proveedor como clase y dejar el implements como gancho.

2026-09-30 — analyst — En el prototipo, expandir «'s» probando todas las reglas a la vez daba «is gone» para «'s gone»: hay que tomar
  la primera que aplica (participio → has, si no → is; lo mismo 'd → had/would).
  why: generaba formas «que faltan» falsas; el analizador real tiene que elegir la expansión por contexto.
