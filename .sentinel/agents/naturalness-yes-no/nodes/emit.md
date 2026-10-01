---
name: emit
kind: script
script: naturalness_yes_no_emit
entry_point: ../scripts/emit.sh
label: Emit Naturalness Answer
description: |
  Produce el artifact final `naturalnessAnswer`: la respuesta canónica que
  validó gate_verdict o, si evaluate agotó sus intentos sin una válida,
  `{"outputUnusable": true}`. Nunca inventa una respuesta: el evaluador
  devuelve esa salida como OUTPUT_UNUSABLE, no llega al libro y el ejercicio
  queda sin evaluar.
completion:
  produces:
    - kind: naturalnessAnswer
      pointer: "{run_dir}/naturalnessAnswer.json"
      required: true
edges:
  - { to: __end__ }
---
