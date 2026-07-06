---
name: eval_distinct
kind: script
script: lemma_absence_eval_distinct
entry_point: ../scripts/eval_distinct.sh
label: Eval — Distinción vs Quizzes del Ejercicio
description: |
  Eval #7 (R013), bloqueante y DETERMINISTA: el candidato no debe ser demasiado
  parecido a un quiz que ya existe en el mismo ejercicio (los quizzes hermanos,
  que llegan aplanados como input `exerciseQuizzes`). Normaliza ambos al
  esqueleto de la oración y compara por similitud (difflib + Jaccard); si el más
  parecido supera el umbral, marca `distinct_ok=False`. Computa, no rutea:
  publica `distinct_ok` (+ el hermano más cercano y la similitud) en el data bus;
  el routing lo decide route_verdict tras combinar con los demás evals en `tally`.
edges:
  - { to: eval_global_distinct }
---
