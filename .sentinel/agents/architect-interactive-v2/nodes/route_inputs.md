---
name: route_inputs
kind: conditional
label: Route Inputs
description: |
  Do we already know which requirement this architecture work belongs
  to (requirement_folder input), or must we ask the user? Pure routing.
conditional: architect_inputs_route
routes: [have_folder, need_folder]
edges:
  - { to: understand, when: have_folder }
  - { to: list_candidates, when: need_folder }
---
