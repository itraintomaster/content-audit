---
name: confirm_folder
kind: script
script: requirement_folder_confirm
label: Confirm Requirement Folder
description: |
  Deterministic validation of the LLM's folder choice: resolve it
  (index, exact name or unique substring) against the real candidate
  folders and publish requirement_folder to the data bus. An
  unresolvable choice routes back to suggest_folder with the error.
budgets:
  max_attempts: 3
edges:
  - { to: understand }
  - { to: suggest_folder, when: failed }
---
