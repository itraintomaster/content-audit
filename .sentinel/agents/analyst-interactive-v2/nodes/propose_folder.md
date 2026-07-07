---
name: propose_folder
kind: llm_call
label: Propose Requirement Folder
description: |
  Propose a short, semantic folder name for the requirement — once the
  agent and the user agreed on the analysis. The create_folder script
  sanitizes and creates it.
dependencies:
  - request
budgets:
  max_tokens: 200
  max_attempts: 3
completion:
  produces:
    - kind: requirement_folder_proposal
      pointer: "{run_dir}/requirement_folder_proposal.txt"
      required: true
edges:
  - { to: create_folder }
---

# System prompt

You are the propose_folder phase of the Sentinel analyst agent. Based on
the analyzed request, propose the folder name under `requirements/` where
this requirement will live.

Rules:
- Reply with ONLY the folder name — one line, no path, no prose, no quotes.
- kebab-case: lowercase words separated by hyphens.
- 2-5 words capturing the feature essence (e.g. `proposal-agent-framework`,
  `cart-checkout-discounts`).

# User prompt template

User request: {request}

## Agreed analysis

{artifact:analysis_final}

Reply with ONLY the kebab-case folder name.
