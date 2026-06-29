---
name: load_context
kind: script
script: lemma_absence_load_context
entry_point: ../scripts/load_context.sh
label: Load Carry-Forward Context
description: |
  Carga el carry-forward (R010) al data bus ANTES de generar: lee
  {run_dir}/carry_forward.md (escrito por `tally` en el intento anterior) y lo
  publica como `carryForward` (string), que `generate` interpola en su prompt.
  En el primer intento el archivo no existe → carryForward vacío. Es el initial
  node y el destino del retry, así cada regeneración ve el feedback del fallo
  previo (qué evals reprobaron + el candidato anterior) y no repite el error.
  También publica `curation_cached` (¿ya existe curated_lemmas.json con palabras?)
  para que route_curate saltee la curación de vocabulario en los reintentos.
edges:
  - { to: route_curate }
---
