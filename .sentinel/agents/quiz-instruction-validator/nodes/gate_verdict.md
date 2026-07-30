---
name: gate_verdict
kind: gate
gate: quiz_instruction_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Verdict Shape
description: |
  Valida verdicts.json contra el esquema cerrado y sus invariantes cruzadas.
  Canonicaliza únicamente una salida válida. Ante JSON malformado, campos extra,
  tipos inválidos o contradicciones, vuelve a evaluate para una nueva emisión.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
