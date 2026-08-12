---
name: emit
kind: script
script: quiz_instruction_corrector_emit
entry_point: ../scripts/emit.sh
label: Emit Correction Candidate
description: |
  Produce el artifact final `correctionCandidate` con **el mejor candidato
  visto** (F-QICOR-R013), no con el último generado. Se llega acá por dos
  caminos —el candidato cumplió todo, o `generate` agotó sus intentos— y los
  dos entregan el champion, así que la salida no depende de por dónde se entró.

  Con él viajan los criterios que **no** cumplió y su medición de longitud
  (F-QICOR-R014/R011): entregar el mejor disponible es la decisión; entregarlo
  sin decir qué le falta sigue prohibido.

  Si no hubo ningún candidato entregable —ninguno emitió JSON válido, o ninguno
  cumplió la consigna ni constituyó un cambio real— este nodo NO inventa uno:
  emite `generated: false` con el motivo. Un candidato fabricado acá, o el
  último generado aunque siga incumpliendo, reabriría el mismo diagnóstico y le
  quemaría al operador una revisión.
completion:
  produces:
    - kind: correctionCandidate
      pointer: "{run_dir}/correctionCandidate.json"
      required: true
edges:
  - { to: __end__ }
---
