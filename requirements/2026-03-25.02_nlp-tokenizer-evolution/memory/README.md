# Memory — 2026-03-25.02_nlp-tokenizer-evolution

Persistent notes that Sentinel agents (analyst, architect, developer, qa, test-writer)
read at the start of a session and append to at the end. The goal is that an agent picking
up this requirement a week later knows what happened without re-reading diffs.

## Files

| File | Owner(s) | Contents |
|------|----------|----------|
| `progress.md` | any agent | current state, last action, next step |
| `decisions.md` | architect, analyst | architectural decisions, escalation resolutions |
| `fix-log.md` | developer, qa, test-writer | fixes that worked, patterns, gotchas |

## Entry format

```
YYYY-MM-DD — <agent-role> — <what happened / decision / fix>
  why: <one line — the non-obvious reason, skippable if obvious>
```

## Discipline

- Keep entries short (1–3 lines). Full prose belongs in `REQUIREMENT.md` or `TECH_SPEC.md`.
- Do NOT log routine reads/greps. Only log things a future session would want to know.
- If a file exceeds ~200 lines, summarize the oldest half into a single `## Archived` section.

This directory is committed to git — treat entries like code review material.
