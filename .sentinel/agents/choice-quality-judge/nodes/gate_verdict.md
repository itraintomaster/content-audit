---
name: gate_verdict
kind: gate
gate: choice_quality_judge_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Validate Distractor Quality Verdict
description: |
  Valida tipos, esquema cerrado, coherencia de status/issues y pertenencia
  exacta de labels a las opciones recibidas. No hace juicios pedagógicos.
  Elimina el veredicto validado anterior si la salida es inválida.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
