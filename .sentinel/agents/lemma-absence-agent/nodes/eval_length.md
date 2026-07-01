---
name: eval_length
kind: script
script: lemma_absence_eval_length
entry_point: ../scripts/eval_length.sh
label: Eval — Length Non-Regression
description: |
  Eval #3 (R007), bloqueante y DETERMINISTA: la longitud del candidato debe
  quedar DENTRO del rango de longitud del NIVEL — SIEMPRE, aunque el audit no
  haya marcado problema de longitud (no-regresión). Reusa el MISMO código que
  el audit vía `content-audit tokens --level <CEFR>`: NlpTokenizer.countTokens
  (spaCy) para el conteo y SentenceLengthConfig.getTargetRange para el rango
  (fuente única). Computa, no rutea: publica `length_ok` (+ detalle) en el data
  bus; el routing lo decide route_verdict tras combinar con los evals de calidad
  en `tally`.
edges:
  - { to: eval_distinct }
---
