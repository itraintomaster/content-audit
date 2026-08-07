---
name: emit
kind: script
script: quiz_instruction_corrector_emit
entry_point: ../scripts/emit.sh
label: Emit Correction Candidate
description: |
  Produce el artifact final `correctionCandidate` con el candidato canónico
  validado.

  Si generate agotó sus intentos sin emitir JSON válido, este nodo NO inventa un
  candidato: emite el artifact con `generated: false` y el motivo. Un candidato
  fabricado acá entraría al catálogo de criterios como si fuera una propuesta
  real, y el operador terminaría revisando algo que nadie propuso.
completion:
  produces:
    - kind: correctionCandidate
      pointer: "{run_dir}/correctionCandidate.json"
      required: true
edges:
  - { to: __end__ }
---
