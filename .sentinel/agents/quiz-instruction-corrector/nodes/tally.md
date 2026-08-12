---
name: tally
kind: script
script: quiz_instruction_corrector_tally
entry_point: ../scripts/tally.sh
label: Keep The Best Candidate Seen
description: |
  Mantiene el champion (F-QICOR-R013): si el candidato actual supera al mejor
  visto, lo snapshotea en `{run_dir}/champion.json` junto con su evaluación.

  **La comparación es una sola línea a propósito.** El orden completo —
  elegibilidad primero, después cuántos criterios cumple, después la precedencia
  por daño al alumno, la longitud última y sin sumar — vive en Java, en
  `CandidateRanking`, y viaja hasta acá resumido en `rankKey`. Reimplementar ese
  orden en bash lo dejaría sin superficie testeable y sin garantía de que las
  dos copias coincidan.

  **Ante clave igual gana el que ya estaba.** Eso es lo que hace que gastar más
  intentos nunca cambie el resultado: un candidato equivalente al campeón no lo
  destrona, así que el desenlace no depende de cuántas veces se reintentó.

  Publica `all_criteria_met` al data bus; `route_verdict` decide si emitir o
  volver a generar.
edges:
  - { to: route_verdict }
---
