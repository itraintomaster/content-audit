---
name: assess
kind: script
script: quiz_instruction_corrector_assess
entry_point: ../scripts/assess.sh
label: Assess Candidate Against The Catalogue
description: |
  Consulta el catálogo de criterios del sistema sobre el candidato recién
  generado (F-QICOR-R015) y deja el resultado en `{run_dir}/assessment.json`.

  **No evalúa nada acá.** Invoca `content-audit assess-candidate`, que resuelve
  el contexto y el snapshot del original con las mismas piezas que usa el motor
  de corrección. Reimplementar un criterio en este script sería tener el
  catálogo dos veces — exactamente lo que F-QICOR-R004 nombra como el peor
  defecto posible, y la razón por la que los criterios viven del lado del
  sistema y este grafo los consulta.

  Le pasa `--plan` y `--task`, no el ejercicio original: el original no viaja
  por el agente y vuelve. Reconstruirlo desde el DSL perdería las partes de
  texto vacías —el formato no puede representarlas— y la edición acotada
  mediría otra cosa (F-QICOR-R010).

  Publica al data bus `eligible`, `satisfied_count`, `rank_key` y
  `all_criteria_met`; el champion lo decide `tally`.
edges:
  - { to: tally }
---
