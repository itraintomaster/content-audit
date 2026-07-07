---
name: sentinel-agent-authoring
description: |
  How to design and write agents for the sentinel-agent framework. Use whenever creating,
  debugging, or improving a .sentinel/agents/ definition — graph agents, react nodes, gate
  scripts, conditional routing, edge routing, retry loops, and prompt engineering for
  tool-using LLM nodes.
  Trigger on: "create an agent", "write an agent", "agent doesn't work", "agent gets stuck",
  "fix my agent", "the react node terminates early", "gate fails", "conditional routes wrong",
  or any mention of .sentinel/agents/ files.
---

# Sentinel Agent Authoring

You are helping design or debug agents for the sentinel-agent framework. This skill captures
hard-won lessons about what makes an agent robust vs. fragile.

## Architecture Overview

A sentinel agent is a **declarative graph** defined in YAML/Markdown under `.sentinel/agents/<name>/`.

```
.sentinel/agents/<name>/
├── agent.md          # Root: name, strategy, initial_node, dependencies
├── nodes/
│   ├── check_foo.md  # Gate node (deterministic bash/python)
│   ├── do_work.md    # React node (LLM with tools)
│   ├── route_next.md # Conditional node (deterministic routing)
│   └── verify.md     # Gate node (verifies outcome)
└── scripts/
    ├── check_foo.sh  # Gate script
    └── verify.sh     # Gate script
```

### Node Kinds

| Kind | Impl | Deterministic? | Use for |
|------|------|----------------|---------|
| `gate` | bash/python script or Java registry | Yes | Verification ONLY — binary passed/failed |
| `script` | bash/python script or Java registry | Yes | Deterministic work (compute, transform, mkdir) — writes data bus, never routes |
| `conditional` | expression / script / one LLM call | Mostly | N-way routing — reads state, decides the edge |
| `react` | LLM + tools loop | No | Complex tasks requiring judgment |
| `llm_call` | Single LLM call | No | Classification, extraction, generation |
| `question` | Pause for user | — | Blocking on human input |

### Key Principle: Gates Are Truth, LLMs Are Effort

The LLM can lie ("done!") or be lazy (skip verification). Gates cannot lie — a `curl -sf` either
returns 200 or it doesn't. **Always verify LLM work with a gate.**

### Key Principle: Verify, Compute, Route Are Three Different Jobs

- A **gate** verifies: binary passed/failed, with `GATE_PASSED`/`GATE_FAILED` events and
  failure propagation. Routing a gate on any other label is deprecated (load-time warning).
- A **script** computes: it writes results to the data bus and always exits `done`. Its
  returned label is ignored — a script that routed would be coupled to one graph's edges.
- A **conditional** routes: it reads the state and picks an edge, emitting `ROUTER_DECIDED`.

To branch on a computed value, compose them:

```
script (writes data.operation_count) → conditional (reads it, routes has_ops/no_ops)
```

---

## Graph Design Patterns

### The Retry Loop (Most Important Pattern)

The most robust agent architecture is: **act → verify → retry on failure**.

```
check_precondition (gate) → do_work (react) → verify_result (gate) → END
                                    ↑                    |
                                    └── (failed) ────────┘
```

**Critical**: the verify gate's `failed` edge routes BACK to the work node, not to `__end__`.
This gives the agent a chance to fix its mistakes. Without this, the agent fails permanently
on the first verification failure.

```yaml
# verify_result.md
edges:
  - { to: __end__, when: passed }
  - { to: do_work, when: failed }    # ← RETRY, not terminate
```

**Without this pattern**, the LLM declares "done" without verifying, the gate catches the
lie, and the run terminates as failed — mission unaccomplished.

### The Fast-Path Skip

For idempotent operations, add a check at the start that skips to END if work is unnecessary.
When the check is a genuine verification ("is the service healthy?"), use a gate:

```yaml
# check_precondition.md (kind: gate)
edges:
  - { to: __end__, when: passed }     # already in desired state
  - { to: do_work, when: failed }     # needs work
```

When the decision reads computed state rather than verifying, use a conditional:

```yaml
# route_precondition.md (kind: conditional)
cases:
  - { when: "data.operation_count > 0", route: already_done }
default: needs_work
edges:
  - { to: __end__, when: already_done }
  - { to: do_work, when: needs_work }
```

This avoids wasting LLM calls when the desired state already exists.

### Budget as Circuit Breaker

The retry loop needs a limit. Set `budgets.max_iterations` on the parent graph to prevent
infinite loops. The graph walker counts iterations and fails with a clear error when exceeded.

```yaml
# On the react node:
budgets:
  max_iterations: 10
```

### Graceful Exhaustion: `when: exhausted`

By default, when a node hits its `max_attempts`, the graph walker **fails the whole run**.
That's the right default — a node that can't succeed after N tries usually means the run can't
succeed. But sometimes retries running out should trigger a *fallback*, not a failure: the run
has already produced something worth keeping, and you'd rather ship the best result so far than
discard everything.

Add a `{ to: <recovery_node>, when: exhausted }` edge. When the node reaches `max_attempts`,
the walker diverts to that node instead of failing:

```yaml
# propose.md — the validity loop (propose ↔ gates) feeds failures back here
edges:
  - { to: soft_check, when: ... }
  # Retries spent: don't fail — fall back to promoting the best valid result.
  - { to: promote, when: exhausted }
```

- **Opt-in and backward-compatible**: a node *without* an `exhausted` edge fails exactly as
  before. Only nodes that declare the edge change behavior.
- **Fires on budget exhaustion, not per-attempt failure**: distinct from `when: failed` (which
  fires on every failed attempt and is how you build retry loops). `exhausted` fires once, when
  the attempt counter reaches the cap.
- **The recovery node must handle the "nothing good yet" case.** Exhaustion can happen before
  any valid result exists (e.g. the very first attempt never validates). The recovery node
  should detect that and fail honestly rather than promote garbage — see the Champion pattern
  below.

#### The Champion Pattern (keep the best, never ship the last)

When a node refines a result across several attempts, the *last* attempt isn't necessarily the
*best* — a later pass can score lower or break entirely. Pair `when: exhausted` with a champion
snapshot so the run always ships its best valid output:

1. After each evaluation, if the new result beats the current champion, snapshot it to a
   separate `champion/` dir (NOT under `staging/`, or the promote walk will pick it up twice)
   and record `champion_score` on the data bus.
2. The terminal `promote` node ships the **champion**, not the live working copy.
3. `promote` fails honestly if no champion exists (nothing ever validated) — including the
   accumulated rejection context for the user.

This decouples "did the last attempt succeed?" from "do we have something worth shipping?" —
the run lands its best validated work whether it exited via success, a spent refine budget, or
an exhausted validity loop.

### History Bounding (Context Cost Control)

A react loop re-sends its **entire** message history to the LLM on every turn. For a
long-lived node — especially an orchestrator that calls `await_children` / `get_child_status`
and folds each big result into context — this grows quadratically: the per-call input climbs
turn after turn, and the bill is dominated by re-sent context, not new work.

The framework bounds this automatically. Every react node has a **default history policy**
(sliding window, keep the last 10 complete tool-call cycles verbatim, truncate older tool
results over 8192 chars). You only declare a `history:` block when the default doesn't fit.

```yaml
# On the react node — override the default:
history:
  strategy: sliding_window           # none | sliding_window | summarize
  keep_recent_cycles: 2              # keep the last N tool-call cycles verbatim
  truncate_tool_results_after: 4096  # shrink tool results larger than this (chars)
```

- **`sliding_window`** (default): pins the system + first user message, keeps the last
  `keep_recent_cycles` cycles verbatim, and truncates large tool results in *older* cycles to a
  head+tail excerpt. No extra LLM calls. The reasoning chain is always preserved — only bulky
  tool *output* is shrunk.
- **`none`**: no bounding (legacy behavior). Use only for short nodes that genuinely need the
  full transcript.
- **`summarize`**: reserved — not implemented yet; falls back to `sliding_window` with a warning.

**When to override**: orchestrators and other coordination-heavy nodes (see Pitfall #8). The
default keeps recent cycles verbatim, but if *recent* tool results are themselves huge
(child-run statuses, large `read_file`s the agent already consumed), lower `keep_recent_cycles`
and `truncate_tool_results_after` for that node.

---

## Writing Gate Scripts

Gates are the backbone of a reliable agent. They provide objective verification.

### Contract

- **stdin**: JSON with `{ "inputs": { ... }, "data": { ... }, "node": { ... } }`
- **stdout**: JSON with `"route"` field (edge label) + optional data fields
- **exit 0**: success (route determines which edge to follow)
- **exit != 0**: fatal error (run halts)
- **`"error"` field in JSON**: logical gate failure (node status = FAILED)

### Routing Gates — DEPRECATED

Gates used to double as N-way routers (`{"route": "healthy"}` / `{"route": "needs_start"}`).
This is deprecated: the loader warns on any gate edge whose `when:` is not `passed`/`failed`,
and it will become an error. Routing pollutes the gate's verification semantics — a
`GATE_FAILED` that is really just "take the other branch" reads as a failure in the run trace
and in metrics.

Use `kind: conditional` instead (see **Writing Conditional Nodes** below). The same
health-check example as a conditional in script mode:

```yaml
# route_health.md
---
name: route_health
kind: conditional
conditional: health_probe
entry_point: ../scripts/health_probe.sh
routes: [healthy, needs_start]
edges:
  - { to: __end__, when: healthy }
  - { to: start_server, when: needs_start }
---
```

The script contract is identical to gates (stdin JSON in, `{"route": ...}` out), but the node
carries routing semantics: it emits `ROUTER_DECIDED` instead of `GATE_PASSED`/`GATE_FAILED`.

### Verification Gates (pass/fail)

```bash
#!/bin/bash
set -euo pipefail

if curl -sf http://localhost:8080/health >/dev/null 2>&1; then
  echo '{"route": "passed", "detail": "Service healthy"}'
else
  echo '{"route": "failed", "error": "Service not responding on port 8080"}'
fi
```

The `"error"` key is what makes the node's PhaseStatus = FAILED. Without it, even
`"route": "failed"` is treated as a successful gate execution that routes via the "failed" edge.
Use the `"error"` key when you want propagation semantics (parent graph can detect child failure).

---

## Writing Conditional Nodes

Conditionals are the framework's only N-way router. Three modes — exactly one per node;
the loader infers the mode from the frontmatter.

### Expression Mode (preferred)

Declarative cases over the run state, evaluated in order, first match wins. Zero processes,
zero LLM calls, validated at load time:

```yaml
---
name: route_by_count
kind: conditional
cases:
  - { when: "data.operation_count > 0", route: has_ops }
  - { when: "inputs.mode == 'dry-run'", route: skip }
default: no_ops                       # optional catch-all; without it, no match = node FAILED
edges:
  - { to: work, when: has_ops }
  - { to: __end__, when: skip }
  - { to: discover, when: no_ops }
---
```

Expression language: paths (`inputs.<key>`, `data.<key>.<nested>`, `q.<questionId>`, or bare
`<key>` resolving inputs-then-data), comparisons (`==`, `!=`, `<`, `<=`, `>`, `>=`), boolean
ops (`&&`, `||`, `!`, parentheses), literals (numbers, `'strings'`, `true`, `false`, `null`).
Numeric-looking strings compare numerically (`inputs.count > 2` works — inputs are always
strings). Missing paths are `null`, so `data.foo != null` is the existence check.

The loader statically validates: expression syntax, every declared route has a matching edge
(or an unconditional fallback), and no edge label is unreachable. A `when: failed` edge is
always allowed (the node can fail, e.g. no case matched and no default).

### Script Mode

For decisions that need to probe the world (run a command, check a port). Same subprocess
contract as gates — stdin JSON in, JSON out — but the output MUST carry `route`:

```yaml
---
name: route_health
kind: conditional
conditional: health_probe             # Java registry id, or subprocess via entry_point
entry_point: ../scripts/health_probe.sh
routes: [healthy, needs_start]        # optional, enables the static edge checks
edges:
  - { to: __end__, when: healthy }
  - { to: start_server, when: needs_start }
---
```

A Java `Script` registered under the id works too: its returned string is the route.
(The same `Script` interface backs `kind: script` nodes, where the return value is ignored.)

### Prompt Mode

For genuinely fuzzy routing (classify a request, triage). One LLM call; the framework appends
an instruction to answer `LABEL: <route>` and rejects anything outside `routes`:

```yaml
---
name: triage
kind: conditional
routes: [bug, feature, question]
edges:
  - { to: fix, when: bug }
  - { to: build, when: feature }
  - { to: answer, when: question }
---

# System prompt

Classify the user's request: {request}
```

### Choosing the Mode

- State already on the bus → **expression**. Cheapest, fully auditable.
- Need to observe the world → **script**. Still deterministic.
- Judgment call → **prompt**. The decision is one LLM call, logged in `ROUTER_DECIDED`
  with the full response as reasoning — never bury routing inside a react node.

---

## Writing React Node Prompts

React nodes are where the LLM does actual work. The prompt determines success or failure.

### The #1 Mistake: Confusing Action with Outcome

**Bad**: "Start the server by running `nohup python3 server.py &`"
The LLM runs the command and says "done" — but the server may not be up yet.

**Good**: "Make sure localhost:8080 responds to health checks. If it doesn't, start it and
verify it's responding before declaring success."

The prompt must define success in **observable terms**, not procedural terms.

### Prompt Structure That Works

```markdown
# System prompt

You are a [role]. Your ONLY job: [observable goal].

Project root: `{project_root}`

## Procedure

1. Check current state: [specific command]
2. If state is already desired: you're done immediately
3. If not: [specific action to take]
4. Wait for effect: `sleep N`
5. **MANDATORY FINAL CHECK** — verify the observable goal:
   [specific verification command]
6. If verification fails: diagnose and retry from step 3

## Rules

- NEVER say "done" until [observable condition] is confirmed in step 5
- If [common error]: [specific fix]
- If already in desired state: done immediately, no action needed
```

### Key Principles

1. **Define success observably**: "curl returns 200", "file exists", "process responds to PID"
2. **Require self-verification**: The LLM must run a final check before terminating
3. **Handle the idempotent case**: If the thing is already done, exit immediately
4. **Give troubleshooting steps**: The LLM doesn't know your system — tell it what to check
5. **Keep tools minimal**: Only give the tools actually needed (usually just `run_shell`)

### What the Framework Does When the LLM Stops

The react loop terminates when the LLM responds **without tool calls**. That response becomes
the node's output. So if the LLM says "I've started the server" without calling `run_shell`
to verify, the loop exits "successfully" but the work may not be done. The verify gate catches this.

---

## Common Failures and Fixes

### "Agent gets stuck / doesn't advance"

**Cause**: `RunShellTool` blocks on background processes. `nohup cmd &` inherits stdout — the
tool waits for all FD holders to close.

**Fix**: The framework's RunShellTool uses async drain with a 30s timeout. If your command
genuinely takes >30s, break it into start + poll steps in the prompt.

### "React node says done but didn't do the work"

**Cause**: LLM terminates the loop by responding without tool calls. It "thinks" it's done.

**Fix**: Add a verification gate after the react node. Route failure back to the react node.
Also improve the prompt to require explicit self-verification before finishing.

### "Gate fails but run terminates instead of retrying"

**Cause**: Failed edge routes to `__end__` instead of back to the work node.

**Fix**: Change the gate's failed edge to route back:
```yaml
edges:
  - { to: __end__, when: passed }
  - { to: work_node, when: failed }   # ← Not __end__
```

### "Agent loops forever"

**Cause**: Retry loop without a budget limit.

**Fix**: Set `budgets.max_iterations` on the react node. The graph walker enforces this.

### "Gate output not parsed correctly"

**Cause**: Script prints non-JSON to stdout (debug output, warnings).

**Fix**: Gate scripts must output ONLY a single JSON line to stdout. Use stderr for debug:
```bash
echo "DEBUG: checking port" >&2   # stderr, ignored
echo '{"route": "passed"}'        # stdout, parsed
```

---

## Agent Definition Reference

### agent.md (root)

```yaml
---
name: my-agent
description: |
  What this agent does. Flow description.
kind: agent
strategy: graph
initial_node: first_node
nodes:
  - ./nodes/first_node.md
  - ./nodes/second_node.md
dependencies:
  - project_root    # Available as {project_root} in prompts
# role: component   # Optional — see "Entrypoints vs Building Blocks" below
---

Optional system-level context for the agent.
```

### Entrypoints vs Building Blocks

The agent list in the web UI splits agents into **entrypoints** (run directly)
and **building blocks** (reusable components composed into another agent), so a
workspace with many small composable agents stays navigable.

This is derived automatically: an agent is a building block when another agent
**statically inlines** one of its node files (e.g. `nodes: [ ../leaf/nodes/step.md ]`).
You don't tag anything — wire up the composition and the split follows.

Override with `role:` in the root frontmatter only when derivation can't see the
relationship:

- `role: component` — the agent is composed **dynamically** (spawned at runtime
  via `launch_agent_run("name", …)` from a React prompt) rather than statically
  inlined. The static scan can't follow a tool call in prose, so declare it.
- `role: entrypoint` — force an agent to stay top-level even though something
  inlines its nodes.

### Gate node

```yaml
---
name: check_something
kind: gate
gate: my_gate_id
entry_point: ../scripts/check_something.sh
label: Check Something
description: What this gate verifies
edges:
  - { to: next_node, when: passed }
  - { to: fix_node, when: failed }    # gates are binary — other labels are deprecated
---
```

### Conditional node

```yaml
---
name: route_something
kind: conditional
label: Route Something
description: What this decision is about
# exactly ONE mode: cases (+default) | conditional (+entry_point, routes) | routes + body prompt
cases:
  - { when: "data.count > 0", route: has_items }
default: empty
edges:
  - { to: process, when: has_items }
  - { to: seed, when: empty }
---
```

### Script node

```yaml
---
name: compute_something
kind: script
script: my_script_id
entry_point: ../scripts/compute.sh    # stdout JSON fields merge into the data bus
label: Compute Something
edges:
  - { to: next_node }                 # scripts never route — exactly one unconditional edge
---
```

### React node

```yaml
---
name: do_work
kind: agent
strategy: react
label: Do Work
description: What this node accomplishes
tools:
  - run_shell
budgets:
  max_iterations: 10
history:                  # Optional — omit to use the default sliding window
  strategy: sliding_window
  keep_recent_cycles: 10
  truncate_tool_results_after: 8192
edges:
  - { to: verify_node }   # Unconditional: always go to verify
---

# System prompt

[LLM instructions here — {project_root} gets interpolated from inputs]
```

### Edge routing rules

- Unconditional edge: `{ to: next }` — always taken (exactly one allowed)
- Conditional edge: `{ to: next, when: label }` — taken when exit label matches
- Terminal edge: `{ to: __end__ }` — exits the graph
- Back-edge: `{ to: previous_node, when: failed }` — creates a retry loop
- Exhaustion edge: `{ to: recovery_node, when: exhausted }` — diverts to a fallback when the
  node hits `max_attempts`, instead of failing the run (see Graceful Exhaustion above)

---

## Critical Pitfalls (Lessons from Production Testing)

These are non-obvious failure modes discovered during systematic agent validation.

### 1. Inputs vs Data Bus: Where Your Values Actually Live

Dependencies declared in `agent.md` arrive in `inputs`, NOT in `data`:

```bash
# WRONG — will be empty string
CONNECTOR_JAR=$(echo "$INPUT" | python3 -c "... d.get('data',{}).get('connector_jar','')")

# RIGHT — check inputs first, then data (for values produced by earlier gates)
CONNECTOR_JAR=$(echo "$INPUT" | python3 -c "... d['inputs'].get('connector_jar', d.get('data',{}).get('connector_jar',''))")
```

**Rule**: if it's a `dependency`, read from `inputs`. If a prior gate produced it, read from `data`.
Always write gates to check both, with `inputs` first.

### 2. Gate Scripts Must Be Zero-Dependency

Never use non-stdlib Python packages in gate scripts. The runner environment may not have them.

```bash
# WRONG — packaging may not be installed, whole script errors silently
python3 -c "from packaging.version import Version; ..."

# RIGHT — use only stdlib
python3 -c "
parts = '$VERSION'.replace('-SNAPSHOT','').split('.')
major, minor, patch = int(parts[0]), int(parts[1]), int(parts[2].split('-')[0])
print('yes' if (major, minor, patch) >= (1, 5, 5) else 'no')
"
```

If a Python import fails, `2>/dev/null || echo "unknown"` silently swallows it and the gate
produces an incorrect route. Hard to debug.

### 3. React Nodes Cannot Write to the Data Bus

The framework does NOT extract structured output from an LLM response into the data bus.
The react node's `output_schema` is documentation only — the data bus won't have those values
after the node runs.

**Implication**: if a verify gate needs data (e.g., list of discovered operations), the gate
must compute it itself. Don't design flows where a gate reads results the react node was
"supposed to" produce.

```
# BAD FLOW: verify reads from data bus that react never populated
check_inputs → run_introspection (react) → verify_discovery (reads data.operations) → END

# GOOD FLOW: verify gate is self-contained
check_inputs → verify_discovery (runs --info directly, checks result) → END
                      ↑                     |
                      └── run_introspection ←┘  (retry: LLM troubleshoots only if gate failed)
```

### 4. Goals: `required_artifacts` ≠ Gate Output

Gate output fields go to the data bus. `required_artifacts` checks the **artifacts map** which
is only populated by explicit `produce_artifact` events (file artifacts). If you need a goal
predicate, use `required_nodes_done`:

```yaml
# WRONG — gate outputs "clone_path" but artifacts map is empty
goal:
  required_artifacts:
    - cloned_connector_path

# RIGHT — sufficient to verify the node completed
goal:
  required_nodes_done:
    - verify_clone
```

### 5. Background Processes Don't Persist After React Nodes

When a react node runs `nohup cmd &` via the shell tool, the process may die when the tool's
subprocess group terminates. Don't verify by checking `ps aux` — verify by testing the
actual capability:

```bash
# WRONG — process may have died between react node and verify gate
if ps aux | grep "my-daemon" | grep -v grep >/dev/null; then ...

# RIGHT — test the actual capability
RESULT=$(java -cp "$RUNNER_JAR" ... --info -o json 2>/dev/null)
if [ "$(echo "$RESULT" | python3 -c '...')" -gt 0 ]; then ...
```

### 6. Graph Budget vs React Budget: Two Levels

| Budget | Set on | Limits | Purpose |
|--------|--------|--------|---------|
| React `budgets.max_iterations` | react node | LLM calls per single invocation | Prevent runaway LLM loops |
| Graph `budgets.max_iterations` | agent.md root | Total node visits across entire run | Prevent infinite retry loops |

**You need BOTH** when you have retry loops. Without the graph budget, a stuck retry loop
(verify → work → verify → work → ...) runs forever.

```yaml
# agent.md
budgets:
  max_iterations: 10    # ← circuit breaker for the whole graph

# react node
budgets:
  max_iterations: 5     # ← limit per invocation of this node
```

### 6b. Dynamic Budgets: Scale the Limit to the Work

A fixed budget is wrong when the workload is variable. An orchestrator that
dispatches one child per discovered operation needs ~5 iterations when there
are 3 operations and ~600 when there are 100 — a single `max_iterations: 220`
is simultaneously too generous for the small connector and too tight for the
large one (the orchestrator stops mid-run, the failure mode behind the
"iteration exhaustion" reports).

Any budget field can be an **arithmetic expression** instead of a fixed number.
It is resolved at runtime against the run's state — inputs plus anything an
upstream gate emitted into state (e.g. `operation_count`):

```yaml
# orchestrate.md — one child per operation
budgets:
  # operation_count is emitted by the discover_operations gate upstream
  max_iterations: "max(80, min(800, 40 + {operation_count} * 6))"
```

- **`{var}`** — a run-state variable (an input, or a value a gate merged into
  state via its JSON stdout). Resolution prefers state `data` over `inputs`.
- **Operators**: `+ - * /`, parentheses, standard precedence.
- **`min(a, b, …)` / `max(a, b, …)`** — the only functions. Use them to put a
  ceiling and a floor on a `base + count * factor` formula: `max(floor, min(cap, …))`.
  The ceiling matters — without it a 100-operation connector would resolve to a
  budget large enough to trigger token explosion.
- **A plain number still works** — `max_iterations: 220` is unchanged.
- Works for every budget field: `max_iterations`, `max_tool_calls`,
  `max_tokens`, `max_wall_seconds`.

**Fail-loud contract**: the expression's grammar is checked when the agent
loads (a malformed formula fails the load, not a run). If, at runtime, a
referenced variable is **not in state** (the emitting gate hasn't run, or the
name is misspelled), the node **fails immediately** with
`references unknown variable '<name>'` — it does *not* silently fall back to a
small or zero budget. So a dynamic budget must reference a variable that an
**upstream** node has already emitted: put the gate that produces
`operation_count` before the node whose budget consumes it.

The run-experience graph shows the resolved number (`×136`) on the node badge,
recomputed as you scrub the timeline — before the gate emits the variable the
badge shows `ƒ` with the raw formula as a tooltip; once `operation_count`
lands, it resolves.

### 7. Verify Gates Should Be Self-Contained

A verify gate that depends on external state (data bus, process tables) is fragile.
The best verify gates are **self-contained**: they run the check themselves and report
pass/fail based on their own observation.

```bash
# Self-contained: runs the actual command and checks the result
INFO_OUTPUT=$(java -cp "$RUNNER_JAR" Runner "$JAR" --info -o json 2>/dev/null)
COUNT=$(echo "$INFO_OUTPUT" | python3 -c "import json,sys; print(len(json.load(sys.stdin).get('operations',[])))")
if [ "$COUNT" -gt 0 ]; then
  echo "{\"route\": \"passed\", \"operation_count\": $COUNT}"
fi
```

### 8. Orchestrators Need an Aggressive History Policy

The default history policy (keep last 10 cycles verbatim) bounds the *tail* of a conversation,
but an orchestrator's *recent* cycles can each carry a huge `await_children` / `get_child_status`
blob. Ten recent cycles × a few hundred KB each = the window itself explodes, even with
truncation working on the older cycles.

Observed in production: an `orchestrate` node still hit a **688K-token** single call under the
default, because its 10 most-recent tool results were the giant ones (the older ones *were*
truncated — the policy ran, it just spared the recent window).

**Fix**: override `history:` on coordination-heavy nodes so even recent cycles stay small.

```yaml
# orchestrate.md — coordination node that dispatches many children
history:
  strategy: sliding_window
  keep_recent_cycles: 2              # not 10 — recent child-statuses are huge and disposable
  truncate_tool_results_after: 4096
```

This is a *config* fix, not a code change. The child-run statuses and file reads the
orchestrator pulled in already live on disk (`results/<op>.json`, the child run dirs), so
truncating them in context loses nothing the agent can't re-read. See **History Bounding**
under Graph Design Patterns.

---

## Checklist: Before Declaring an Agent "Done"

1. ☐ Every react node is followed by a verification gate
2. ☐ Verification gates route failure BACK to the work node (retry loop)
3. ☐ React nodes have `budgets.max_iterations` set (circuit breaker)
4. ☐ Graph-level `budgets.max_iterations` set when retry loops exist
5. ☐ The prompt defines success in observable terms, not procedural
6. ☐ The prompt requires self-verification before the LLM stops
7. ☐ Gate scripts output ONLY JSON to stdout
8. ☐ Gate scripts use `"error"` key for logical failures (not just route name)
9. ☐ Gate scripts use only Python stdlib (no pip packages)
10. ☐ Gate scripts read values from `inputs` first, then `data` fallback
11. ☐ Verify gates are self-contained (compute their own checks, don't read data bus from react)
12. ☐ Fast-path gate at the start skips work when already done
13. ☐ All `{input_name}` placeholders have matching entries in `dependencies:`
14. ☐ Goals use `required_nodes_done`, not `required_artifacts` (unless actual files produced)
15. ☐ Tested with: desired state already exists, nothing exists, partial state
16. ☐ Coordination/orchestrator nodes set an aggressive `history:` policy (low `keep_recent_cycles`)
17. ☐ Checked per-call input token growth doesn't explode on long-lived react nodes
18. ☐ Right kind for the job: gate = verify (passed/failed ONLY), script = compute (never
    routes), conditional = route. No gate edges with non-binary `when:` labels (deprecated)
19. ☐ Conditionals declare their route space (`cases`/`default` or `routes`) so the loader
    statically checks edge coverage; conditional scripts output a `route` field
20. ☐ Variable-workload budgets use an expression (`max(floor, min(cap, base + {count}*factor))`) whose variable is emitted by an UPSTREAM node
