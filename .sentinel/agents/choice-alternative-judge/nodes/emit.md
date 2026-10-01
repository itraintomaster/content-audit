---
name: emit
kind: script
script: choice_alternative_judge_emit
entry_point: ../scripts/emit.sh
label: Emit Choice Alternative Verdict
description: |
  Produce el artifact final `choiceAlternativeVerdict`: el veredicto canónico que validó
  gate_verdict o, si evaluate agotó sus intentos sin uno válido,
  `{"outputUnusable": true}`. Nunca inventa un veredicto: el evaluador
  devuelve esa salida como OUTPUT_UNUSABLE, no llega al lector ni queda en el
  libro, y el ejercicio queda sin evaluar (F-MCVALE-R005).
completion:
  produces:
    - kind: choiceAlternativeVerdict
      pointer: "{run_dir}/choiceAlternativeVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
