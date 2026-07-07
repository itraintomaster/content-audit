---
name: gate_grilled
kind: gate
gate: grill_engaged
label: Gate Grill Engaged
description: |
  Backstop: the grill must interrogate the user at least once. A react node
  completes the moment the LLM emits a final message with no tool call, so
  nothing structurally forces the ask — the model can self-answer every design
  decision and produce design_decisions without ever calling ask_user (observed
  live: the grill closed in 2 calls, no pause). This gate fails when the grill
  closed with zero ask_user questions and routes back to grill, which re-enters
  with the rejection injected so it actually pauses and asks. Passes the moment
  the grill has asked at least one question. Only ever runs after grill, which
  only runs on interactive runs.
edges:
  - { to: draft_text, when: passed }
  - { to: grill, when: failed }
---
