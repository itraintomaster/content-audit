# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (impl A) — R003 marcaba 14 en vez de 7: faltaba portar spoken_from_choice_parts entero, con el cue
  que va después del hueco («Mark saw ___ him / himself in the mirror.» dice «Mark saw himself in the mirror.»). Portado
  tal cual, con la respuesta como parámetro, quedan los 7 del REQUIREMENT.

2026-09-30 — architect — `patch propose` rechaza `implements: [AnalyzerProvider]` porque el puerto es de 0.3 y no está en sentinel.yaml
  (los tipos en firmas y requiresInject sí pasan). Se validó con `-i` contra el modelo después de 0.2 y 0.3, armado en memoria con
  la librería de Sentinel (PatchApplier + registerRequirementFeature, en scratchpad/ronda1-A/Simular.java, sin `patch apply` ni tocar
  el worktree). El primer propose fallido de RESPC había dejado un baseline de antes de 0.2: se borró y se regeneró con el modelo nuevo.
  why: el diseño es contra el modelo después de 0.2 y 0.3; el arquitecto de D eligió declarar el proveedor como clase y dejar el implements como gancho.
