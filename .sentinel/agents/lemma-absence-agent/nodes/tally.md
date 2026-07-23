---
name: tally
kind: script
script: lemma_absence_tally
entry_point: ../scripts/tally.sh
label: Tally Verdicts + Champion
description: |
  Combina los evals deterministas de longitud (data.length_ok), distinción
  (data.distinct_ok / data.global_distinct_ok) y no-regresión léxica
  (data.lexical_ok, criterio #7 de R007) con los evals LLM (verdicts.json):
  calcula si pasan TODOS los bloqueantes.
  Mantiene el champion (R009): si el candidato actual supera al mejor visto
  —más bloqueantes pasados, desempate por pragmatismo #4— lo snapshotea.
  En fallo bloqueante escribe carry_forward.md (R010) para el reintento.
  Publica `all_blocking_pass` en el data bus; route_verdict decide.
edges:
  - { to: route_verdict }
---
