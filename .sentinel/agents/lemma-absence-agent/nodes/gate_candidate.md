---
name: gate_candidate
kind: gate
gate: lemma_absence_gate_candidate
entry_point: ../scripts/gate_candidate.sh
label: Gate Candidate Shape
description: |
  Normaliza y valida la FORMA del candidato que produjo el react antes de
  evaluarlo: strippea ```fences```, extrae el primer objeto JSON, y exige
  exactamente {quizSentence, translation} (ambos no vacíos), sin `[[...]]` y
  sin schemas ajenos (exercise/gaps/answerKey). Si pasa, reescribe
  candidate.json en su forma canónica (los nodos de eval leen JSON limpio).
  Si no, vuelve a `generate` para reintentar (acotado por max_attempts/R008).
edges:
  - { to: eval_length, when: passed }
  # failed → load_context (no directo a generate): el gate escribió carry_forward.md
  # con el motivo del rechazo de formato, y load_context lo carga para el reintento.
  - { to: load_context, when: failed }
---
