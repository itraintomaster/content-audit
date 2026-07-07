---
name: soft_check_patch
kind: gate
gate: architect_soft_check
label: Soft Check Patch
description: Deterministic smell detection on the staged patch (naming, contract shape).
edges:
  - { to: gate_patch, when: passed }
  - { to: propose, when: failed }
---
