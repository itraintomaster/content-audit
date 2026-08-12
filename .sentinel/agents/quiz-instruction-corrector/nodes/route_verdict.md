---
name: route_verdict
kind: conditional
label: Emit Or Try Again
description: |
  Si el candidato cumple todos los criterios, se emite (F-QICOR-R013). Si no,
  se vuelve a generar: el budget `max_attempts` de `generate` acota los
  reintentos, y al agotarse su edge `exhausted` lleva a `emit`, que entrega el
  champion (F-QICOR-R012).

  Reintentar vuelve a `generate` y no a `assess`: lo que se reintenta es
  producir otro candidato, no volver a medir el mismo. El generador recibe la
  evaluación previa, así que el intento siguiente ve qué se le reprochó al
  anterior.
cases:
  - { when: "data.all_criteria_met == true", route: emit_now }
default: retry
edges:
  - { to: emit, when: emit_now }
  - { to: generate, when: retry }
---
