---
name: emit
kind: script
script: panel_proposer_emit
entry_point: ../scripts/emit.sh
label: Emit Panel Proposal
description: |
  Produce el artifact `panelProposal` con la propuesta validada. Si generate
  agotó sus intentos sin una propuesta bien formada, entrega una propuesta sin
  candidatos: la mesa la cuenta como un intento rechazado, nunca inventa uno.
completion:
  produces:
    - kind: panelProposal
      pointer: "{run_dir}/panelProposal.json"
      required: true
edges:
  - { to: __end__ }
---
