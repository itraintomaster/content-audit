<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Sentinel-Governed Project

This project is governed by Sentinel. All architecture rules, module boundaries,
workflow phases, and agent instructions are defined in `AGENTS.md`.

**Read `AGENTS.md` before making any changes.**

## Test Creation

When creating tests for any implementation, use the `sentinel-test-loop` skill.
This skill orchestrates a quality-controlled loop: the qa-tester proposes tests,
the test-reviewer verifies them against sentinel.yaml and REQUIREMENT.md, and
corrections are applied before generating the final patch. Never invoke qa-tester
alone — always go through the loop.
