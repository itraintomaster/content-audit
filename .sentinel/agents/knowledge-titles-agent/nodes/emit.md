---
name: emit
kind: script
script: knowledge_titles_emit
entry_point: ../scripts/emit.sh
label: Emit Title Candidate
description: |
  Salida del run: emite el artifact `titleCandidate` con el CHAMPION (mejor
  candidato visto), como {knowledgeId, title, instructions}. Best-effort: si
  el run llegó acá por `exhausted`, se emite igual el mejor visto. Si no hay
  NINGÚN candidato emitible (nunca se validó ninguno), no emite el campo →
  el artifact requerido no se produce → el goal del run falla honestamente.
completion:
  produces:
    - kind: titleCandidate
      pointer: "{run_dir}/titleCandidate.json"
edges:
  - { to: __end__ }
---
