---
name: route_curate
kind: conditional
label: Route Curation (fast-path skip)
description: |
  La curación de vocabulario corre UNA sola vez por run. En el primer paso no hay
  pool curado todavía → va a curate_lemmas. En los reintentos, load_context ya
  encontró curated_lemmas.json y publicó `curation_cached=true`, así que saltea el
  nodo caro curate_lemmas (no re-consulta la tool) y pasa por finalize_curation,
  que re-publica `curatedWords` desde el pool ya curado antes de generate. Así
  generate siempre recibe el vocabulario curado en el data bus, corra en el primer
  intento o en un reintento; solo se re-construye la oración con el feedback nuevo.
cases:
  - { when: "data.curation_cached == true", route: cached }
default: fresh
edges:
  - { to: finalize_curation, when: cached }
  - { to: curate_lemmas, when: fresh }
---
