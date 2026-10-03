---
name: gate_verdict
kind: gate
gate: choice_alternative_judge_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Verdict Shape
description: |
  Valida verdicts.json contra el esquema cerrado de choiceAlternativeVerdict
  (schema/verdict.schema.json): correctIsValid, validAlternatives,
  correctProblem y rationale, sin claves de más ni de menos, con sus tipos.
  Invariantes: cada label es una de las opciones que llegaron en options y no
  la correcta, una sola vez; severity HIGH o MEDIUM (F-MCVALE-R003; la clave
  invertida la vuelve bloqueante el clasificador); correctIsValid false si y
  sólo si correctProblem dice por qué. Canonicaliza únicamente una salida
  válida —la label tal cual está en las opciones, enums en mayúsculas, textos
  en una línea, correctProblem null cuando la correcta vale— en
  validated-verdict.json. Ante JSON malformado, tipos inválidos o
  contradicciones vuelve a evaluate, que recibe el motivo.
  Una clave de más que viene vacía (null o texto en blanco) no dice nada y no
  cuenta; con contenido, frena.
  Una coma colgante antes de cerrar un objeto o una lista tampoco: se lee el
  JSON sin ella.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
