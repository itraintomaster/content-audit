---
name: route_folder
kind: conditional
label: Route Requirement Folder
description: |
  Do we already have a folder source — the requirement_folder input or
  the agent's proposal — or does the LLM still need to propose a name?
  Pure routing (script mode: the decision probes the proposal artifact,
  which expressions cannot see).
conditional: requirement_folder_route
routes: [have_source, need_proposal]
edges:
  - { to: create_folder, when: have_source }
  - { to: propose_folder, when: need_proposal }
---
