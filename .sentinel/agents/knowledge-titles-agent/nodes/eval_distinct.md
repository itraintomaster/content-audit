---
name: eval_distinct
kind: script
script: knowledge_titles_eval_distinct
entry_point: ../scripts/eval_distinct.sh
label: Eval — Title Distinctness
description: |
  Eval #5, bloqueante y DETERMINISTA: el título candidato debe distinguirse de
  los títulos de los ejercicios hermanos del mismo topic (el alumno elige el
  ejercicio por su título). Similitud de secuencia (difflib) + Jaccard de
  tokens sobre el título normalizado; si el hermano más parecido supera el
  umbral 0.85, distinct_ok=False. Los hermanos son los labels ACTUALES de la
  db (la de-dup entre renombres de un batch no se orquesta acá). Computa, no
  rutea: publica distinct_ok (+ detalle) al data bus.
edges:
  - { to: eval_quality }
---
