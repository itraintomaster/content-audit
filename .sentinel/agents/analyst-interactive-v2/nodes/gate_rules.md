---
name: gate_rules
kind: gate
gate: rule_testability
label: Gate Rule Testability
description: |
  Deterministic, zero-LLM testability linter on the rules artifact (no API
  cost). Rejects non-testable rules — definitional phrasing, removal/existence
  claims, capability-without-output, rules with no observable-invariant
  blockquote — and routes back to draft_rules with the offending rule IDs, so
  a non-testable rule never reaches assembly or the QA phase.
edges:
  - { to: draft_journeys, when: passed }
  - { to: draft_rules, when: failed }
---
