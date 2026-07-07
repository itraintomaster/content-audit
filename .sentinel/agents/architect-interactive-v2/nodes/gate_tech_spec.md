---
name: gate_tech_spec
kind: gate
gate: architect_tech_spec_validate
label: Gate Tech Spec
description: |
  Validate the staged TECH_SPEC.md via the Sentinel CLI (tech-spec
  write) against the validation overlay.
edges:
  - { to: eval_patch, when: passed }
  - { to: tech_spec, when: failed }
---
