---
name: eval_patch
kind: script
script: architect_eval
label: Eval Patch
description: |
  Run sentinel eval on the staged patch. Publishes eval_score +
  eval_findings to the data bus and stages eval-results.json alongside
  the patch. Deterministic A-tier by default; pass input
  eval_llm_judge=true to also run the B-tier LLM-as-judge, which judges
  with the SAME provider+model the agent runs on (e.g. the Claude
  subscription via claude-cli — no separate API key).
edges:
  - { to: route_eval }
  # A hard eval failure (broken CLI) shouldn't discard an already-validated
  # champion from an earlier pass — fall back to promoting it.
  - { to: promote, when: failed }
---
