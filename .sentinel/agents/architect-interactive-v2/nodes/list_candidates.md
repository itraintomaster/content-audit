---
name: list_candidates
kind: script
script: requirement_candidates_list
label: List Requirement Candidates
description: |
  Scan requirements/ and publish requirement_candidates (newest first)
  for the suggest_folder phase. Halts if the project has no
  requirements — the analyst must run first.
edges:
  - { to: suggest_folder }
---
