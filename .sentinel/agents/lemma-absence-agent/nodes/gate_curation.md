---
name: gate_curation
kind: gate
gate: lemma_absence_gate_curation
entry_point: ../scripts/gate_curation.sh
label: Gate Curation Tool Use
description: |
  Verifica (determinista, leyendo events.jsonl del run) que curate_lemmas REALMENTE
  invocó la tool get_suggested_lemmas en su último intento. El framework no fuerza
  tool-choice, así que un react puede responder sin llamar la tool (shortcut) e
  inventar el pool desde el conocimiento del modelo en vez del catálogo del nivel.
  Este gate cierra ese hueco (patrón del skill: react → verify-gate → route-back).

  - passed → finalize_curation (el pool salió del catálogo).
  - failed → curate_lemmas (reintenta; el prompt fuerza la llamada). Al agotar
    curate.max_attempts, el edge `exhausted` de curate cae a finalize_curation
    (fallback a suggestedWords) — el run no haltea.
edges:
  - { to: finalize_curation, when: passed }
  - { to: curate_lemmas, when: failed }
---
