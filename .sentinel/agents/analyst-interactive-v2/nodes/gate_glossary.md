---
name: gate_glossary
kind: gate
gate: glossary_validate
label: Gate Glossary
description: Validate the domain-glossary.yaml via the Sentinel CLI's glossary validator.
edges:
  - { to: gate_requirement, when: passed }
  - { to: glossary_sync, when: failed }
---
