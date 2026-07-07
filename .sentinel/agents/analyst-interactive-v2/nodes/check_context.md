---
name: check_context
kind: gate
gate: context_summary_check
label: Check Context Summary
description: |
  Deterministic structure check on the context summary: all required
  sections present. Catches the react node closing with prose instead of
  the summary document; failure routes back to gather_context with the
  missing sections as error detail.
edges:
  - { to: analysis_draft, when: passed }
  - { to: gather_context, when: failed }
---
