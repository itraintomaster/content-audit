---
name: create_folder
kind: script
script: requirement_folder_create
label: Create Requirement Folder
description: |
  Deterministic work: resolve the folder (input wins over proposal),
  slugify + de-collide proposals, create requirements/<slug>/ and publish
  requirement_folder to the data bus. A failure (junk proposal, IO)
  routes back to propose_folder.
budgets:
  max_attempts: 3
edges:
  - { to: draft_text }
  - { to: propose_folder, when: failed }
---
