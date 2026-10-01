---
name: gate_verdict
kind: gate
gate: naturalness_yes_no_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Answer Shape
description: |
  Valida verdicts.json contra el esquema cerrado de naturalnessAnswer
  (schema/verdict.schema.json): una sola clave, answer, con yes o no. Acepta
  mayúsculas, espacios y un punto final, y canonicaliza únicamente una salida
  válida —yes o no en minúsculas— en validated-verdict.json. Ante JSON
  malformado, una clave de más o de menos u otra respuesta vuelve a evaluate,
  que recibe el motivo.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
