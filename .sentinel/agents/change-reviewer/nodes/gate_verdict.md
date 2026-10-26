---
name: gate_verdict
kind: gate
gate: change_reviewer_gate
entry_point: ../scripts/gate_verdict.sh
label: Validate the mark and its exact fragments
edges:
  - { to: emit, when: passed }
  - { to: emit, when: failed }
---
