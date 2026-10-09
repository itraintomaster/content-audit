---
name: emit
kind: script
script: choice_quality_judge_emit
entry_point: ../scripts/emit.sh
label: Emit Distractor Quality Verdict
description: |
  Emite sólo un veredicto validado. Si no hay uno, outputUnusable: true;
  nunca fabrica PASS ni vuelve una salida inválida una consulta consolidada.
completion:
  produces:
    - kind: choiceQualityVerdict
      pointer: "{run_dir}/choiceQualityVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
