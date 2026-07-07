---
name: assemble
kind: script
script: requirement_assemble
label: Assemble REQUIREMENT.md
description: |
  Deterministic stitch (no LLM): splice the rules and journeys partials into
  the prose skeleton at their markers, producing the final REQUIREMENT.md in
  staging. The rules already passed their testability gate — nothing is
  allowed to re-touch and re-break them here.
completion:
  produces:
    - kind: requirement_md
      pointer: "{run_dir}/staging/{requirement_folder}/REQUIREMENT.md"
      required: true
edges:
  - { to: route_refine }
---
