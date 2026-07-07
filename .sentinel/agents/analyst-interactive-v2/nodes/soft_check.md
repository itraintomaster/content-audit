---
name: soft_check
kind: gate
gate: soft_check
label: Soft Check
description: Pure-Python smell detector on the assembled REQUIREMENT.md (no LLM cost).
edges:
  - { to: glossary_sync, when: passed }
  - { to: draft_text, when: failed }
---
