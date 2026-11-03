---
name: gate_verdict
kind: gate
gate: mini_theory_judge_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Verdict Shape
description: |
  Valida un objeto cerrado que cubre cada quizId de la entrada, en el mismo
  orden: un assessment sano o uno por cada problema distinto, todos seguidos
  (F-MINIT-R013). Verifica las categorías por quiz, incluidas DIDACTIC_DIRECTION
  y MISSING_ENGLISH_HIGHLIGHT, su severidad fija, la coherencia de holds, que un
  quiz sano no traiga problemas, que no se repita un problema en un quiz y que no
  falte ni se invente ningún id. Canonicaliza únicamente una salida válida en
  validated-verdict.json, con los problemas de cada quiz del más grave al más
  leve; cualquier desvío vuelve a evaluate con el motivo.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
