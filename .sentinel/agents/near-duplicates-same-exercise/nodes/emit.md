---
name: emit
kind: script
script: near_duplicates_same_exercise_emit
entry_point: ../scripts/emit.sh
label: Emit Same Exercise Verdict
description: |
  Produce el artifact final `sameExerciseVerdict`: el veredicto canónico que
  validó gate_verdict o, si evaluate agotó sus intentos sin uno válido,
  `{"outputUnusable": true}`. Nunca inventa un veredicto: el evaluador devuelve
  esa salida como OUTPUT_UNUSABLE, no llega al libro y el par queda sin juzgar
  (F-DUPAR-R008).
completion:
  produces:
    - kind: sameExerciseVerdict
      pointer: "{run_dir}/sameExerciseVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
