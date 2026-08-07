---
name: gate_candidate
kind: gate
gate: quiz_instruction_corrector_gate_candidate
entry_point: ../scripts/gate_candidate.sh
label: Gate Candidate Shape
description: |
  Valida la forma de candidate.json: objeto cerrado con quizSentence,
  translation y rationale, los tres string y ninguno vacío. Canonicaliza solo
  una salida válida. Ante JSON malformado, claves de más o campos vacíos,
  vuelve a generate para una nueva emisión.

  Deliberadamente NO juzga la calidad del candidato: eso es el catálogo de
  criterios de Java (F-QICOR-R004/R005). Este gate solo garantiza que el
  adaptador tenga algo parseable que devolver.
edges:
  - { to: emit, when: passed }
  - { to: generate, when: failed }
---
