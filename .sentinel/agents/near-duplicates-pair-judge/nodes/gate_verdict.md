---
name: gate_verdict
kind: gate
gate: near_duplicates_pair_judge_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Verdict Shape
description: |
  Valida verdicts.json contra el esquema cerrado de sameExerciseVerdict
  (schema/verdict.schema.json): sameExercise y reason, sin claves de más ni de
  menos, con sus tipos —sameExercise true o false, nunca «yes»—. Canonicaliza
  únicamente una salida válida —la razón en una línea— en
  validated-verdict.json. Ante JSON malformado o tipos inválidos vuelve a
  evaluate, que recibe el motivo.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
