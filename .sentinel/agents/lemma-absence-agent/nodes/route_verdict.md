---
name: route_verdict
kind: conditional
label: Route Verdict
description: |
  Si el candidato pasa TODOS los evals bloqueantes, se emite (R007). Si algún
  bloqueante reprobó, se dispara un nuevo run de generación (R008) — el budget
  max_attempts de `generate` acota los reintentos a maxEvalRetries; al agotarse,
  el edge `exhausted` de generate emite el champion (R009).
cases:
  - { when: "data.all_blocking_pass == true", route: emit_now }
default: retry
edges:
  - { to: emit, when: emit_now }
  - { to: generate, when: retry }
---
