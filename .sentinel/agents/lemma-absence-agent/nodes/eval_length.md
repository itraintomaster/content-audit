---
name: eval_length
kind: script
script: lemma_absence_eval_length
entry_point: ../scripts/eval_length.sh
label: Eval — Length Non-Regression
description: |
  Eval #3 (R007), bloqueante y DETERMINISTA: la longitud del candidato no
  debe regresar respecto al quiz original pre-corrección. Reusa la misma
  noción de longitud de oración del audit (proxy por conteo de palabras de
  la oración, equivalente al length-analyzer). Computa, no rutea: publica
  `length_ok` (+ detalle) en el data bus; el routing lo decide route_verdict
  tras combinar con los evals de calidad en `tally`.
edges:
  - { to: eval_distinct }
---
