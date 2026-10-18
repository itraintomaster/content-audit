---
name: gate_verdict
kind: gate
gate: pragmatism_judge_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Validate Communicative Verdict
description: |
  Valida esquema cerrado, coherencia de estados y cobertura exacta de las
  oraciones recibidas. Borra el artefacto validado si la salida no es usable.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
