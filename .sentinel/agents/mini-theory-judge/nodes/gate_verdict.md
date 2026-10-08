---
name: gate_verdict
kind: gate
gate: mini_theory_judge_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Verdict Shape
description: |
  Valida un objeto cerrado con exactamente un assessment por quizId de la
  entrada, en el mismo orden. Verifica las cuatro categorías por quiz, su
  severidad fija, la coherencia de holds y que no falte, se repita ni se invente
  ningún id. Canonicaliza únicamente una salida válida en
  validated-verdict.json; cualquier desvío vuelve a evaluate con el motivo.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
