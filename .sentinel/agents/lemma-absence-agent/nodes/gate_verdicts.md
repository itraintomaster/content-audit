---
name: gate_verdicts
kind: gate
gate: lemma_absence_gate_verdicts
entry_point: ../scripts/gate_verdicts.sh
label: Gate Verdicts Shape
description: |
  Valida la FORMA del verdicts.json que produjo el juez (eval_quality) ANTES de
  que tally lo combine: parsea con tolerancia (fences/prosa/llaves faltantes) y
  exige las 5 claves top-level (eval1_sense, eval2_solvable, eval4_pragmatism,
  eval5_translation, eval6_finite_reuse) con su forma mínima. Si pasa, reescribe
  verdicts.json canónico (tally lee JSON limpio). Si no, vuelve a eval_quality
  para que el juez re-emita (para eso existe su max_attempts) — sin este gate,
  un JSON malformado del juez llegaba a tally, parseaba como {} y TODOS los
  evals LLM se contaban reprobados: carry-forward falso acusando al candidato
  («no tiene sentido», «traducción infiel») y reintento de generate desperdiciado.
edges:
  - { to: tally, when: passed }
  # failed → el juez re-emite. Su budget max_attempts acota el lazo; al agotarse,
  # el edge exhausted de eval_quality degrada a tally (que trata la ausencia de
  # veredictos como infra, no como culpa del candidato).
  - { to: eval_quality, when: failed }
---
