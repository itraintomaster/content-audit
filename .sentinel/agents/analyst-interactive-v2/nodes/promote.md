---
name: promote
kind: script
script: requirement_promote
label: Promote To Project
description: |
  Transactional finish: every gate passed, so the staged outputs
  ({run_dir}/staging — the requirement folder and the merged glossary)
  are moved into the project. Until this node runs, a failed or
  abandoned run leaves the project completely untouched.
edges:
  - { to: __end__ }
---
