<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Sentinel-Governed Project

This project is governed by Sentinel. All architecture rules, module boundaries,
workflow phases, and agent instructions are defined in `AGENTS.md`.

**Read `AGENTS.md` before making any changes.**

## Test Creation

When designing tests for an implementation, invoke the `@qa-tester` agent.
It analyzes contracts and requirements to propose test names with traceability.
Tests are declared as `handwrittenTests` in sentinel.yaml, and `sentinel generate`
creates JUnit stub classes that developers implement by hand.
