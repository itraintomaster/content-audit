---
name: gate_requirement
kind: gate
gate: requirement_validate
label: Gate Requirement
description: Validate the REQUIREMENT.md via the Sentinel CLI's rule validator.
edges:
  - { to: promote, when: passed }
  - { to: draft_text, when: failed }
---
