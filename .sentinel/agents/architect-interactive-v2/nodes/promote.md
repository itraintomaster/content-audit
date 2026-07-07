---
name: promote
kind: script
script: architect_promote
label: Promote To Project
description: |
  Transactional finish: gates and the eval bar passed — move the staged
  outputs ({run_dir}/staging: architectural_patch.yaml, TECH_SPEC.md,
  eval-results.json) into the requirement folder. Until this node runs,
  a failed or abandoned run leaves the project completely untouched.
edges:
  - { to: __end__ }
---
