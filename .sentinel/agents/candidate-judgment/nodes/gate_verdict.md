---
name: gate_verdict
kind: gate
gate: candidate_judgment_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Verdict Shape
description: |
  Valida la forma de verdict.json: objeto cerrado con criterion, outcome y
  detail; `outcome` dentro del enum; `criterion` idéntico al que se pidió
  juzgar; `detail` no vacío. Canonicaliza solo una salida válida y vuelve a
  judge ante cualquier desvío.

  Que `criterion` tenga que coincidir con el pedido no es ceremonia: el
  evaluador de Java se indexa por criterio, y un veredicto que dice juzgar otro
  se archivaría bajo la casilla equivocada sin que nada lo delate.
edges:
  - { to: emit, when: passed }
  - { to: judge, when: failed }
---
