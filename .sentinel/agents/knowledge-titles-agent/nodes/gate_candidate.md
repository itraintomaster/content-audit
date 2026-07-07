---
name: gate_candidate
kind: gate
gate: knowledge_titles_gate_candidate
entry_point: ../scripts/gate_candidate.sh
label: Gate Candidate Shape
description: |
  Normaliza y valida la FORMA del candidato que produjo el react antes de
  evaluarlo: strippea ```fences```, extrae el primer objeto JSON balanceado y
  exige exactamente {title, instructions} (ambos strings no vacíos), sin `$`,
  `*` ni comillas dobles en el título. Si pasa, reescribe candidate.json en su
  forma canónica (los evals leen JSON limpio). Si no, escribe carry_forward.md
  con el motivo y vuelve a load_context para reintentar.
edges:
  - { to: eval_length, when: passed }
  - { to: load_context, when: failed }
---
