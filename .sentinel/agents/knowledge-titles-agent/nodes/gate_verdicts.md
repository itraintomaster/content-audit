---
name: gate_verdicts
kind: gate
gate: knowledge_titles_gate_verdicts
entry_point: ../scripts/gate_verdicts.sh
label: Gate Verdicts Shape
description: |
  Valida la FORMA del verdicts.json del juez antes de que tally lo combine:
  parse tolerante (fences, prosa, llaves de cierre faltantes) + las 4 claves
  del catálogo con sus tipos (pass booleano en #6/#7/#8, score numérico en
  #9). Si pasa, canonicaliza el archivo; si no, failed → eval_quality
  re-emite. Evita el modo de fallo observado en lemma-absence: un JSON
  malformado del juez contaba todos los evals como reprobados → reintento
  espurio con feedback falso.
edges:
  - { to: tally, when: passed }
  - { to: eval_quality, when: failed }
---
