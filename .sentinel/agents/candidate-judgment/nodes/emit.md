---
name: emit
kind: script
script: candidate_judgment_emit
entry_point: ../scripts/emit.sh
label: Emit Criterion Verdict
description: |
  Produce el artifact final `criterionVerdict` con el veredicto canónico
  validado.

  Si judge agotó sus intentos sin emitir JSON válido, emite `NOT_EVALUABLE` con
  el motivo — nunca `FAILED`. La distinción es la que pidió R006: reprobar
  afirma que el candidato empeoró algo, y una falla de infraestructura no
  afirma nada sobre el candidato. Degradar a `FAILED` descartaría candidatos
  buenos y le diría al operador que la corrección es imposible cuando lo que
  pasó es que el juez no contestó.
completion:
  produces:
    - kind: criterionVerdict
      pointer: "{run_dir}/criterionVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
