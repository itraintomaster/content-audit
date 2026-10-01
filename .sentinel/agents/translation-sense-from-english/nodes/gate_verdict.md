---
name: gate_verdict
kind: gate
gate: translation_sense_from_english_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Verdict Shape
description: |
  Valida verdicts.json contra el esquema cerrado de translationSenseVerdict
  (schema/verdict.schema.json): passes, failures y rationale, sin claves de más
  ni de menos, con sus tipos. Invariantes: passes true si y sólo si failures
  está vacía; una sola entrada por condición; severity HIGH, MEDIUM o LOW,
  nunca BLOCKING (F-TRSEN-R004); LOW sólo y siempre con LEVEL_REGISTER.
  Canonicaliza únicamente una salida válida —enums en mayúsculas, textos en una
  línea— en validated-verdict.json. Ante JSON malformado, tipos inválidos o
  contradicciones vuelve a evaluate, que recibe el motivo.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
