---
name: gate_verdict
kind: gate
gate: mini_theory_judge_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Verdict Shape
description: |
  Valida verdicts.json contra el esquema cerrado de miniTheoryVerdict
  (schema/verdict.schema.json): assessments y rationale, sin claves de más ni
  de menos, con sus tipos. Invariantes cruzadas con las entradas: miniTheory es
  una M de miniTheories y cada M aparece al menos una vez; exercises son E de
  exercises; con CONTRADICTED_BY_EXERCISE o NOT_COVERING_EXERCISE, sólo
  ejercicios que muestran esa M (F-MINIT-R003). Invariantes propias: holds true
  si y sólo si problem, severity y counterexample son null, y es la única
  entrada de su M; la severity sale del problema (F-MINIT-R004: falsa o
  contradicha HIGH, exagerada MEDIUM, que no cubre LOW; nunca BLOCKING); una
  entrada por M y problema; FALSE_RULE y OVERSTATED no van juntos. Canonicaliza
  únicamente una salida válida —enums en mayúsculas, «M3»/«E4» a números,
  exercises sin repetir y en orden, textos en una línea— en
  validated-verdict.json. Ante JSON malformado, tipos inválidos o
  contradicciones vuelve a evaluate, que recibe el motivo.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
