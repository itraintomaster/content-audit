---
name: emit
kind: script
script: single_answer_judge_emit
entry_point: ../scripts/emit.sh
label: Emit Single Answer Verdict
description: |
  Produce el artifact final `singleAnswerVerdict`: el veredicto canónico que validó
  gate_verdict o, si evaluate agotó sus intentos sin uno válido,
  `{"outputUnusable": true}`. Nunca inventa un veredicto: el evaluador
  devuelve esa salida como OUTPUT_UNUSABLE, no llega al lector ni queda en el
  libro, y el ejercicio queda sin evaluar (F-UNICA-R005).
completion:
  produces:
    - kind: singleAnswerVerdict
      pointer: "{run_dir}/singleAnswerVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
