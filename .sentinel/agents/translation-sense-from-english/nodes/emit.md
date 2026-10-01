---
name: emit
kind: script
script: translation_sense_from_english_emit
entry_point: ../scripts/emit.sh
label: Emit Translation Sense Verdict
description: |
  Produce el artifact final `translationSenseVerdict`: el veredicto canónico que validó
  gate_verdict o, si evaluate agotó sus intentos sin uno válido,
  `{"outputUnusable": true}`. Nunca inventa un veredicto: el evaluador
  devuelve esa salida como OUTPUT_UNUSABLE, no llega al lector ni queda en el
  libro, y el ejercicio queda sin evaluar (F-TRSEN-R005).
completion:
  produces:
    - kind: translationSenseVerdict
      pointer: "{run_dir}/translationSenseVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
