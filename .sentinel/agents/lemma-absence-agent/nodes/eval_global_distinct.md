---
name: eval_global_distinct
kind: script
script: lemma_absence_eval_global_distinct
entry_point: ../scripts/eval_global_distinct.sh
label: Eval — Distinción Global vs Todo el Curso
description: |
  Eval #8, bloqueante y DETERMINISTA: el candidato no debe ser casi-idéntico a
  NINGÚN quiz ya existente del curso (corpus pre-computado
  `.content-audit/quiz-corpus.jsonl`) ni a otra propuesta pending del batch
  (glob `.content-audit/revisions/*/`). Complementa eval_distinct, que solo mira
  los quizzes hermanos del mismo knowledge; éste mira TODO el curso + los cambios
  pendientes del batch, para evitar generar quizzes prácticamente iguales entre sí
  (p.ej. 4x "She showers").

  Normaliza al esqueleto de la oración y compara por similitud (difflib + Jaccard,
  umbral 0.85), con pruning por content-words para velocidad. EXCLUYE el propio
  quiz del candidato (mismo esqueleto que el quizSentence original) para NO
  penalizar la edición mínima de Fase 1, que a propósito produce un candidato
  parecido al original. Computa, no rutea: publica `global_distinct_ok` (+ el más
  cercano, su nivel y la similitud) en el data bus; el routing lo decide
  route_verdict tras combinar con los demás evals en `tally`. SOFT-PASS VISIBLE si
  falta el corpus (no bloquea por fallo de infra).
edges:
  - { to: eval_lexical }
---
