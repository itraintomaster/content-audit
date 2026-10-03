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
  Un campo extra que viene vacío (null o texto vacío) no dice nada: se descarta
  antes de validar.
  Una coma colgante antes de cerrar un objeto o una lista tampoco: se lee el
  JSON sin ella.
  Un elemento de relleno en una lista, sin texto y con lo demás vacío o NONE,
  tampoco: se descarta.
  Un `undefined` suelto, que no es JSON, se lee como null, y la clave de
  relleno que lo traía se descarta.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
