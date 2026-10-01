---
name: emit
kind: script
script: mini_theory_judge_emit
entry_point: ../scripts/emit.sh
label: Emit Mini Theory Verdict
description: |
  Produce el artifact final `miniTheoryVerdict`: el veredicto canónico que validó
  gate_verdict o, si evaluate agotó sus intentos sin uno válido,
  `{"outputUnusable": true}`. Nunca inventa un veredicto: el evaluador
  devuelve esa salida como OUTPUT_UNUSABLE, no llega al lector ni queda en el
  libro, y el tema queda sin evaluar (F-MINIT-R005).
completion:
  produces:
    - kind: miniTheoryVerdict
      pointer: "{run_dir}/miniTheoryVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
