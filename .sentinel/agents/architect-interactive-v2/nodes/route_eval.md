---
name: route_eval
kind: conditional
label: Route Eval
description: |
  Quality routing: promote when the composite score reaches the
  threshold (input eval_threshold, default 0.8) or the bounded
  refinement budget is spent; otherwise refine.
conditional: architect_eval_route
routes: [good_enough, needs_refine]
edges:
  - { to: promote, when: good_enough }
  - { to: refine, when: needs_refine }
---
