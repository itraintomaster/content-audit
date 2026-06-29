---
name: finalize_curation
kind: script
script: lemma_absence_finalize_curation
entry_point: ../scripts/finalize_curation.sh
label: Finalize Curation (validate + publish)
description: |
  Arma el POOL RESULTANTE de la curación, deterministamente, desde el CATÁLOGO:
  lee los tool_result de get_suggested_lemmas (events.jsonl), toma los lemmas EN EL
  ORDEN rankeado por la tool (ausentes primero, luego COCA), filtra por
  `requiredRegex` (red de seguridad) y quita los que el LLM marcó en `exclude` (veto;
  spec en curation_spec.json). El LLM no elige ni reordena: el pool es del catálogo.
  Publica `curatedWords` (coma-separado, en orden) al data bus para generate y escribe
  el pool en curated_lemmas.json (artefacto `curatedLemmas` + marca de curación-hecha
  → los reintentos la saltean vía route_curate). Si la tool no aportó nada, cae a
  suggestedWords para no bloquear. Computa, no rutea.
completion:
  produces:
    # El pool resultante (los lemmas que realmente salieron de la curación, en orden).
    - kind: curatedLemmas
      pointer: "{run_dir}/curated_lemmas.json"
      required: true
edges:
  - { to: generate }
---
