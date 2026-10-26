---
name: gate_verdict
kind: gate
gate: answer_giveaway_judge_gate
entry_point: ../scripts/gate_verdict.sh
label: Validate the verdict and the exact fragment
edges:
  - { to: emit, when: passed }
  - { to: emit, when: failed }
---
