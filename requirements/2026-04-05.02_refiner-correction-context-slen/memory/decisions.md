# Decisions

2026-04-19 — analyst — Re-homed rule references from `refiner next` to `get tasks` / `get task <id>` after FEAT-CLIRV deleted the old command. Behavioral objective unchanged.
  why: FEAT-CLIRV (kubectl-style restructure) removed the `refiner next` surface; R006/R007/R008 and J001/J002/J003 still cited it literally. Rule and journey objectives are unchanged — only the command-surface citations were swapped to `get task` (single-task lookup) and `get tasks --status pending --sort priority --limit 1` (next-pending equivalent). Historical-context paragraph in "El problema actual" still mentions `refiner next` to explain the prior state; that's allowed.

2026-04-19 — qa-tester — Re-declared 17 handwrittenTests on GetCmd traced to F-RCSL R006/R007/R008 (lost when FEAT-CLIRV deleted RefinerNextCmd). Tests live on GetCmd because the developer wired correctionContextResolver into GetCmd's get() pathway — same behavior, new home. R006: 2 tests (resolver invocation gate); R007: 8 tests (JSON shape); R008: 7 tests (text format). Patch validates (1 mod / 0 conflicts).
  why: deletion of RefinerNextCmd took the test class with it; rules are still active and the behavior was restored, so traceability needs to come back at the new owner.
