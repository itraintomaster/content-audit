---
name: gate_verdict
kind: gate
gate: native_usage_judge_gate
entry_point: ../scripts/gate_verdict.sh
label: Validate exact coverage and states
edges:
  - { to: emit, when: passed }
  - { to: emit, when: failed }
---
