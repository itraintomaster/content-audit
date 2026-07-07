---
name: route_refine
kind: conditional
label: Route Refinement
description: |
  After the first assembled draft: go to the interactive `grill` (detailed
  how-alignment) the first time through, then straight to `soft_check` once the
  grill produced its design_decisions (so the re-draft is not re-grilled).
  Skips the grill on non-interactive runs. Pure routing (script mode: it probes
  the design_decisions artifact + the interactive flag, which expressions can't
  see).
conditional: requirement_refine_route
routes: [refine, proceed]
edges:
  - { to: grill, when: refine }
  - { to: soft_check, when: proceed }
---
