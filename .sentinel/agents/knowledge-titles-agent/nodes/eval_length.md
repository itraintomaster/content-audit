---
name: eval_length
kind: script
script: knowledge_titles_eval_length
entry_point: ../scripts/eval_length.sh
label: Eval — Weighted Lengths
description: |
  Evals #1 y #2, bloqueantes y DETERMINISTAS: longitud PONDERADA del título
  ≤ 28 y de las instructions ≤ 70 (soft limit). Replica EXACTAMENTE la función
  de pesos de KnowledgeTitleLengthAnalyzer / KnowledgeInstructionsLengthAnalyzer
  ($ * → 0.0; i , . → 0.5; f t " → 0.7; resto 1.0) para que un candidato que
  pasa acá puntúe 1.0 en el audit. Computa, no rutea: publica title_length_ok /
  instructions_length_ok (+ medidas) al data bus; route_verdict decide tras tally.
edges:
  - { to: eval_redundancy }
---
