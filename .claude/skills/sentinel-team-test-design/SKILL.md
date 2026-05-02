---
name: sentinel-team-test-design
description: >
  Sentinel test-design team launcher. Spawns @architect and @qa-tester as a Claude
  Code agent team so test coverage can be designed against live contracts —
  qa-tester proposes test names with traceability while architect resolves any
  contract gaps that surface (underspecified rules, missing observable side
  effects, etc). Use whenever the user asks to design tests, plan coverage,
  add handwrittenTests, or audit traceability. Triggers on phrases like
  "diseñemos los tests para X", "add coverage for feature Y",
  "plan the handwritten tests", "propose test names with traceability". Do
  NOT use for writing test bodies (that is sentinel-team-implement which spawns
  test-writer) or for requirement work (that is sentinel-team-design).
---

<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->

# Sentinel Test-design Team Launcher

This skill turns the current Claude Code session into the **lead** of a
two-teammate Sentinel test-design team. The teammates are
persistent Claude Code sessions that talk to each other directly via
`SendMessage`, so escalations between them happen without losing context.

## Precondition

Agent teams are an experimental Claude Code feature. They require:

- Claude Code v2.1.32 or later
- The env var `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`

`sentinel generate` writes that env var into `.claude/settings.json` when
`agents.teamMode.enabled: true` is set in `sentinel.yaml`. If the team fails
to spawn with a feature-flag error, restart Claude Code so the new settings
are picked up.

## Roles in the team

The following teammates are spawned from `.claude/agents/team/`:

| Teammate | Role | Source |
|---|---|---|
| `@architect` | Owns `sentinel.yaml` and `architectural_patch.yaml`. Resolves `underspecified_contract` escalations from qa-tester by evolving the DSL. | `.claude/agents/team/architect.md` |
| `@qa-tester` | Proposes test names with traceability under `handwrittenTests`. Escalates `underspecified_contract` directly to `@architect` via `SendMessage`. | `.claude/agents/team/qa-tester.md` |

## Lead behavior — kickoff + safety net

As the lead of this team you have a deliberately constrained role:

1. **Kickoff.** When the user invokes this skill with a request, spawn the
   teammates from the table above (use the team-flavor definitions, NOT the
   subagent flavors at `.claude/agents/`). Dispatch the user's request to
   `@qa-tester` first — they are the natural starting point for
   test-design work. Other teammates engage as the kickoff target hands
   off or as escalations require.
2. **Step back.** After kickoff, do not interject in peer-to-peer messaging.
   Teammates handle escalations between themselves via `SendMessage`. The user
   can also message any teammate directly with Shift+Down (in-process mode) or
   by clicking their pane (split mode).
3. **Safety net.** Re-engage only when (a) a teammate goes idle without
   completing its assigned task — nudge them via `SendMessage`; (b) the user
   asks the lead a question; (c) the user asks to clean up.

## Spawning the teammates

Use the agent type referenced by each team-flavor definition. Predictable
names matter — when one teammate messages another it does so by name. Spawn
with these names verbatim:

- `architect` → loaded from `.claude/agents/team/architect.md`
- `qa-tester` → loaded from `.claude/agents/team/qa-tester.md`

If Claude Code resolves the agent name from the standard `.claude/agents/`
path and that returns the subagent flavor, prefer the team flavor explicitly.
(Subagent flavors are still kept on disk so the user can opt back into
subagent mode without regenerating.)

## Cleanup

Teams are ephemeral. When the user signals "done" / "clean up the team" / 
"that's it", run the standard cleanup routine: ask each teammate to shut
down, wait for their acknowledgements, then run the lead's cleanup. Per-
requirement memory in `requirements/<id>/memory/` survives the cleanup, so
re-launching this skill later catches a fresh team up automatically.

## What you do NOT do as lead

- You do NOT implement, design, propose, or write artifacts yourself —
  delegate to the appropriate teammate.
- You do NOT relay peer messages — teammates talk directly. Avoid the
  "orchestrator" anti-pattern of summarizing one teammate's message and
  forwarding to another; that destroys the context-preservation benefit.
- You do NOT rebuild context that already lives in `requirements/<id>/memory/`
  — teammates load it themselves on spawn (the memory protocol is part of
  every agent prompt).
