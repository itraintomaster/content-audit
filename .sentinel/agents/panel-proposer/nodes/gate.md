---
name: gate
kind: gate
gate: panel_proposer_gate
entry_point: ../scripts/gate.sh
label: Gate Proposal Shape
description: |
  Valida la forma de proposal.json: un objeto con `candidates`, una lista de
  objetos con un `kind` conocido. No juzga la calidad: eso es de content-audit
  y de los jueces (F-MESA-R004 a R006). Ante JSON malformado, vuelve a generate.
edges:
  - { to: emit, when: passed }
  - { to: generate, when: failed }
---
