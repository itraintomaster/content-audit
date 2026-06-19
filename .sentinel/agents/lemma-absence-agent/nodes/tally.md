---
name: tally
kind: script
script: lemma_absence_tally
entry_point: ../scripts/tally.sh
label: Tally Verdicts + Champion
description: |
  Combina el eval determinista de longitud (data.length_ok) con los evals
  LLM (verdicts.json): calcula si pasan TODOS los bloqueantes (#1,#2,#3,#5).
  Mantiene el champion (R009): si el candidato actual supera al mejor visto
  —más bloqueantes pasados, desempate por pragmatismo #4— lo snapshotea.
  En fallo bloqueante escribe carry_forward.md (R010) para el reintento.
  Publica `all_blocking_pass` en el data bus; route_verdict decide.
edges:
  - { to: route_verdict }
---
