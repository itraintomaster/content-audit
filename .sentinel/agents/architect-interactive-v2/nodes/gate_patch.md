---
name: gate_patch
kind: gate
gate: architect_patch_validate
label: Gate Patch
description: |
  Validate the staged patch via the Sentinel CLI (patch propose) against
  the run's validation overlay — the project is never touched.
edges:
  - { to: tech_spec, when: passed }
  - { to: propose, when: failed }
---
