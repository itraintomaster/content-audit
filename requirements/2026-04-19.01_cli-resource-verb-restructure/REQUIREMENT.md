---
feature:
  id: FEAT-CLIRV
  code: F-CLIRV
  name: CLI restructure to kubectl-style verb-resource grammar
  priority: critical
---

# CLI restructure to kubectl-style verb-resource grammar

The `content-audit` CLI evolved feature-by-feature into a tree of subcommand groups (`refiner plan`, `refiner next`, `refiner list`, `refiner revise`, `analyzer list`, `analyzer config`, `analyzer stats`) plus the standalone `analyze`. This shape has produced several recurring problems in actual operator use, and adding more capabilities on top of it would amplify them. This requirement specifies a one-shot restructure of the CLI surface into a kubectl-style **verb-then-resource** grammar so that future features land naturally instead of accreting another subcommand group per concern.

## Context — what triggered this

Three concrete pains motivate the change:

1. **No way to view existing artifacts without producing more of them.** `content-audit refiner plan` always *creates* a new plan; there is no command that simply lists previously created plans. The same is true for audit reports — `content-audit analyze` always runs a fresh audit and persists it, but there is no command that just enumerates the audits already on disk under `.content-audit/audits/`. The user has accumulated a backlog of artifacts and no way to inspect them short of opening files manually.
2. **Name collision around `list`.** `content-audit refiner list` is documented and behaves as "list the *tasks within a plan*", not "list the *plans*". When the user wanted to enumerate plans, the most obvious command did the wrong thing. Inside a kubectl-style mental model this collision disappears: tasks and plans are different resources, and `get plans` vs. `get tasks` answers each question unambiguously.
3. **Implicit "latest" everywhere.** Several commands silently default to "the latest" plan or audit when no id is provided. This was convenient when there was one of each, but with a backlog it makes it easy to operate on the wrong artifact without realizing it. The new grammar keeps "latest by default" only where it is unambiguous (the bulk-creation domain verbs like `plan` and `analyze` still default to the latest source artifact), and forces an explicit id everywhere else.

A second, orthogonal pain that this requirement also addresses: the CLI cannot currently be exercised end-to-end by automated tests without writing into the developer's real `.content-audit/` workdir. The persistence adapters (`FileSystemAuditReportStore`, `FileSystemRefinementPlanStore`, `FileSystemRevisionArtifactStore`) already accept a `baseDir` constructor argument that points the `.content-audit/` subtree elsewhere, but the CLI's `Main` only wires the no-arg constructors, which fall back to `System.getProperty("user.dir")`. The result is that any shell-driven E2E test pollutes (or is polluted by) the developer's actual artifacts. This requirement adds a rule that the CLI must accept an external override for the workdir so the same binary can run against a sandbox directory.

## The new grammar

The CLI surface becomes:

```
content-audit <verb> [<resource>] [<name>] [flags]
```

where `<verb>` is one of:

- **`get`** — read. Lists when no name is given; shows one when a name is given. Read-only.
- **`delete`** — remove a single resource by id.
- **`prune`** — bulk cleanup with an explicit retention policy (e.g. `--keep N`).
- **`analyze`** — domain verb that runs an audit and *creates* an audit resource.
- **`plan`** — domain verb that derives a refinement plan from an audit and *creates* a plan resource.
- **`revise`** — domain verb that drives the revision workflow on a task.
- **`config`** — domain verb that inspects/modifies analyzer configuration.
- **`stats`** — domain verb that introspects a single analyzer's behavior on a course.

Domain verbs (`analyze`, `plan`, `revise`, `config`, `stats`) are kept as first-class verbs instead of being recast as `create` because they carry domain semantics that "create" would erase. `kubectl` itself coexists with similar domain verbs (`rollout`, `scale`, `cordon`); the precedent is good.

The recognized resources are: **`audits`**, **`plans`**, **`tasks`**, **`analyzers`**.

### Final command surface

```
# Reads
content-audit get audits
content-audit get audit <id>
content-audit get plans
content-audit get plan <id>
content-audit get tasks [--plan <id>] [--status pending|completed|skipped] [--sort priority] [--limit N]
content-audit get task <id>
content-audit get analyzers
content-audit get analyzer <id>

# Deletes
content-audit delete audit <id>
content-audit delete plan <id>

# Bulk cleanup
content-audit prune audits --keep N
content-audit prune plans  --keep N

# Domain verbs (creators / workflows)
content-audit analyze [course-path]
content-audit plan    [--audit <id>]
content-audit revise  task <id>
content-audit config  analyzer <id> [...]
content-audit stats   analyzer <name> [course-path]
```

### What is removed

- The `refiner` subcommand group is gone in its entirety. Its capabilities are redistributed: `refiner plan` becomes the top-level `plan`; `refiner revise` becomes `revise task <id>`; `refiner list` is replaced by `get tasks`; `refiner next` is replaced by `get tasks --status pending --sort priority --limit 1` (no alias is added — the substitution is part of the new grammar).
- The `analyzer` subcommand group is gone. `analyzer list` becomes `get analyzers`; `analyzer config` becomes `config analyzer <id>`; `analyzer stats <name> [course]` becomes the top-level `stats analyzer <name> [course]` (see R021). The underlying behavior of each is preserved unchanged; only the surface address moves.
- No backwards-compatibility shims. The user has confirmed there are no external callers of the old commands; deprecation aliases would only delay the cleanup.

---

## Business Rules

### Group A — Verb semantics

### Rule[F-CLIRV-R001] - `get` is read-only and returns one or many
**Severity**: critical | **Validation**: AUTO_VALIDATED

The `get` verb only reads from persistent storage. It never creates, mutates, or deletes any resource. When invoked without a `<name>` argument, `get` enumerates all instances of the resource. When invoked with a `<name>`, `get` returns the single matching instance. If no matching instance exists, `get` exits with a non-zero status and an error explaining that the named resource was not found.

**Error**: "No <resource> found with id '<id>'"

### Rule[F-CLIRV-R002] - `delete` removes exactly one resource by id, on `audit` and `plan` only
**Severity**: critical | **Validation**: VALIDATED

The `delete` verb removes a single resource identified by its id. The id is mandatory; `delete <resource>` without an id is rejected. After a successful delete, subsequent `get <resource> <id>` for the same id returns the "not found" error of R001. Bulk removal is never performed by `delete` — that is the role of `prune`.

`delete` operates **only** on `audit` and `plan`. It does not operate on `task`: there is no `delete task <id>` command in this iteration. Tasks are derived state inside a plan; mutating a plan in place would be a domain change forbidden by R019. To remove a task, the operator deletes (or regenerates) the owning plan. Invoking `delete task <id>` must be rejected with a usage error. `delete` also does not operate on `analyzer`: analyzers are static registry entries, not stored artifacts.

`delete audit <id>` does **not** cascade. If any stored plan references the named audit via its `sourceAuditId`, the delete must fail with a non-zero exit code and an error that lists the dependent plan ids so the operator can address them explicitly. The operator is expected to delete (or `prune`) the dependent plans first and then re-issue the audit delete. There is no `--cascade` flag in this iteration; refusal is the chosen failure mode (consistent with `delete` removing "exactly one resource by id" — silently removing N also is the contract violation).

**Error**: "Cannot delete <resource>: id is required" / "Cannot delete task: 'delete task' is not supported. Delete or regenerate the owning plan instead." / "Cannot delete audit '<id>': it is referenced by plan(s): <id1>, <id2>, .... Delete or prune those plans first."

### Rule[F-CLIRV-R003] - `prune` performs bulk cleanup under an explicit retention policy
**Severity**: critical | **Validation**: AUTO_VALIDATED

The `prune` verb removes multiple resources at once according to a retention flag. In this iteration the only supported retention policy is `--keep N`, which keeps the **N most recent** resources of the named type and removes the rest. Recency is determined by the resource's creation timestamp (`createdAt` for plans; the audit id timestamp for audits). The flag is mandatory; `prune <resource>` without `--keep` is rejected.

**Error**: "Cannot prune <resource>: --keep N is required"

### Rule[F-CLIRV-R004] - `analyze`, `plan`, `revise`, `config` are preserved as domain verbs
**Severity**: major | **Validation**: AUTO_VALIDATED

The verbs `analyze`, `plan`, `revise`, and `config` are first-class verbs of the CLI. They are not subsumed under a generic `create` verb. Their existing behavior is preserved as currently specified by the features they belong to (FEAT-CLI for `analyze`, FEAT-REVBYP for `revise`, the existing refiner-plan behavior for `plan`, and the existing analyzer-config behavior for `config`). The only change introduced by this requirement is their **placement** in the command tree (top-level instead of nested under `refiner` / `analyzer`), and the explicit shape of their first argument (`revise task <id>`, `config analyzer <id>`).

**Error**: N/A (this rule preserves existing behavior under a new placement)

### Group B — Resource grammar

### Rule[F-CLIRV-R005] - Recognized resources
**Severity**: critical | **Validation**: AUTO_VALIDATED

The CLI recognizes exactly four resources: `audits`, `plans`, `tasks`, `analyzers`. Any other resource name passed to `get`, `delete`, or `prune` is rejected with an error that lists the recognized resources. Future resources are out of scope for this iteration.

**Error**: "Unknown resource '<name>'. Known resources: audits, plans, tasks, analyzers"

### Rule[F-CLIRV-R006] - Singular and plural resource names are interchangeable
**Severity**: major | **Validation**: AUTO_VALIDATED

For every recognized resource, the singular and plural forms are accepted interchangeably by `get`, `delete`, and `prune`. `get audit` is equivalent to `get audits`; `get plan <id>` is equivalent to `get plans <id>`; `delete audits <id>` is equivalent to `delete audit <id>`. This mirrors `kubectl get pod` / `kubectl get pods`. The chosen form does not change the output: when a name is provided, exactly one instance is returned; when no name is provided, a list is returned.

**Error**: N/A (this rule defines an equivalence)

### Rule[F-CLIRV-R007] - Resource ids are presented uniformly across verbs
**Severity**: minor | **Validation**: AUTO_VALIDATED

The id used to address a resource in `get <resource> <id>`, `delete <resource> <id>`, or as the source argument in `--audit <id>` / `--plan <id>` is the same id that the corresponding creator verb prints when it produces the resource (the audit timestamp id from `analyze`, the plan id from `plan`, the task id from a plan's task list). An operator can copy any printed id and use it directly with any of the read or delete verbs.

**Error**: N/A (this rule defines an addressing convention)

### Group C — `get` filter and projection semantics

### Rule[F-CLIRV-R008] - `get tasks` filters by plan, status, target, diagnosis, and result count
**Severity**: critical | **Validation**: AUTO_VALIDATED

`get tasks` accepts the following optional flags, each of which narrows the result set independently:

| Flag | Effect |
|------|--------|
| `--plan <id>` | Restrict the result to tasks belonging to the named plan. If absent, the most recently created plan is used. |
| `--status pending\|completed\|skipped` | Restrict the result to tasks whose status matches the given value (case-insensitive). |
| `--target <enum>` (short: `-t <enum>`) | Restrict the result to tasks whose `nodeTarget` matches the named `AuditTarget` enum value (QUIZ, KNOWLEDGE, TOPIC, MILESTONE, COURSE). Parsing is case-insensitive. If the value is not a valid enum, the command fails with a clear error listing the allowed values. If absent, no target filter is applied. |
| `--diagnosis <enum>` (short: `-d <enum>`) | Restrict the result to tasks whose `diagnosisKind` matches the named `DiagnosisKind` enum value (SENTENCE_LENGTH, LEMMA_ABSENCE, COCA_BUCKETS, etc. — the canonical list lives in the audit / refiner domain; this rule references it by name only). Parsing is case-insensitive. If the value is not a valid enum, the command fails with a clear error listing the allowed values. If absent, no diagnosis filter is applied. |
| `--sort priority` | Order the result by ascending task priority. If absent, the natural order from the plan is used. |
| `--limit N` | Return at most N tasks after filtering and sorting. `N` must be a non-negative integer. |

Combinations are conjunctive. Examples:

- `get tasks --status pending --sort priority --limit 1` returns the single highest-priority pending task in the most recent plan. This combination is the explicit replacement for the removed `refiner next` command — the user has confirmed no alias is added.
- `get tasks --target QUIZ --diagnosis LEMMA_ABSENCE --status pending --sort priority --limit 1` returns the single highest-priority pending task whose target is `QUIZ` and whose diagnosis is `LEMMA_ABSENCE`. This is the direct migration of the old `refiner next -t QUIZ -d LEMMA_ABSENCE` invocation; `--target` and `--diagnosis` (with their short forms `-t` / `-d`) preserve the two filters that the prior command exposed.

**Error**: "Invalid value for --status: '<value>'. Allowed: pending, completed, skipped" / "Invalid value for --target: '<value>'. Allowed: QUIZ, KNOWLEDGE, TOPIC, MILESTONE, COURSE" / "Invalid value for --diagnosis: '<value>'. Allowed: <comma-separated list of DiagnosisKind enum values>"

### Rule[F-CLIRV-R009] - `--limit 0` yields an empty result, not an error
**Severity**: minor | **Validation**: AUTO_VALIDATED

`get tasks --limit 0` returns zero tasks. This is treated as a valid request (the operator wants to suppress output), not as a usage error. The exit status is zero. Negative values for `--limit` are rejected.

**Error**: "Invalid value for --limit: must be zero or positive"

### Rule[F-CLIRV-R010] - When a referenced source resource does not exist, the request fails clearly
**Severity**: major | **Validation**: AUTO_VALIDATED

When a `get tasks --plan <id>` references a plan id that does not exist, the command fails with a non-zero exit code and an explicit "plan not found" message. It must not silently fall back to a different plan. The same principle applies to any flag that selects a source artifact (`plan --audit <id>` for a non-existent audit, etc.).

**Error**: "<Resource> '<id>' not found"

### Rule[F-CLIRV-R011] - Empty result sets are reported, not silenced
**Severity**: minor | **Validation**: AUTO_VALIDATED

When a list operation matches zero resources (no audits exist, no tasks match the filter, no plans match the prune scope, etc.), the CLI prints an explicit human-readable line stating that the result is empty, and exits with status zero. Empty is not an error. The intent is to distinguish "the system found nothing" from "the system did not understand the request".

**Error**: N/A (this rule defines an output convention)

### Rule[F-CLIRV-R012] - `get tasks` without `--plan` resolves to the most recent plan
**Severity**: major | **Validation**: AUTO_VALIDATED

When `get tasks` is invoked without `--plan`, the most recently created plan (by creation timestamp) is used as the implicit source. If no plan exists at all, the command exits with a non-zero status and an explicit message indicating that no plans are available. It must not auto-create one.

**Error**: "No plans available. Run 'content-audit plan' to create one."

### Group D — Domain-verb relocations

### Rule[F-CLIRV-R013] - `analyze` is a top-level verb that creates an audit
**Severity**: critical | **Validation**: AUTO_VALIDATED

`content-audit analyze [course-path]` runs the audit pipeline against the given course path (or the path resolved by the existing course-path resolution rules of FEAT-CLI), persists the resulting audit report under `.content-audit/audits/`, and prints the assigned audit id to standard error or standard output as it does today. The functional behavior of the audit run itself is unchanged; only the placement of the verb at the top of the command tree is asserted here.

**Error**: N/A (the existing FEAT-CLI errors apply unchanged)

### Rule[F-CLIRV-R014] - `plan` is a top-level verb that creates a refinement plan
**Severity**: critical | **Validation**: AUTO_VALIDATED

`content-audit plan` derives a refinement plan from an audit report and persists it under `.content-audit/plans/`. The audit source can be selected with `--audit <id>`; if the flag is absent, the most recent audit is used. The plan id assigned by the system is printed on success. The behavior of the planning logic itself is unchanged from the existing refiner-plan command; only its placement at the top level is asserted here.

**Error**: "Audit '<id>' not found" (when `--audit` references a missing audit), or "No audits available. Run 'content-audit analyze' first." (when no audit exists at all and no `--audit` flag was supplied).

### Rule[F-CLIRV-R015] - `revise` operates on a task, scoped to a plan
**Severity**: critical | **Validation**: VALIDATED

`content-audit revise task <task-id>` runs the revision workflow defined by FEAT-REVBYP against the named task. The task id is the same id reported by `get tasks` and by the plan-task listings.

Task ids are sequential **within** a plan (`task-001`, `task-002`, ...) and are not globally unique across plans. The owning plan is resolved as follows:

1. **Default — no `--plan` flag.** The most recently created plan (by creation timestamp) is used as the implicit source. This mirrors the `get tasks` default of R012, so an operator who just ran `get tasks --status pending --sort priority --limit 1` can copy the printed task id directly into `revise task <id>` without restating which plan it belongs to.
2. **Explicit — `--plan <plan-id>` flag.** The named plan is used as the source. This is the disambiguator for cases where the operator wants to revise a task in an older plan (e.g. they are still working through a backlog of plans).

Failure modes:

- If no plan exists at all (regardless of whether `--plan` was supplied), the command exits with a non-zero status and the message: **"No plans available. Run 'content-audit plan' first."**
- If `--plan <id>` references a plan that does not exist, the command exits with a non-zero status and the message: **"Plan '<id>' not found"**.
- If the task id does not match any task in the resolved plan, the command exits with a non-zero status and the message: **"Task '<task-id>' not found in plan '<plan-id>'"** (where `<plan-id>` is the resolved plan id, whether default or explicit, so the operator can see *which* plan was searched).

The semantics of the revision itself, the artifact persistence, the validator verdict and the exit code mapping defined by FEAT-REVBYP are preserved unchanged.

**Error**: "No plans available. Run 'content-audit plan' first." / "Plan '<id>' not found" / "Task '<task-id>' not found in plan '<plan-id>'"

### Rule[F-CLIRV-R016] - `config` operates on an analyzer
**Severity**: major | **Validation**: AUTO_VALIDATED

`content-audit config analyzer <analyzer-name>` shows the configuration of the named analyzer in the same shape produced by the existing analyzer-config command. The analyzer name is the canonical name reported by `get analyzers`. If the analyzer is not registered, the command exits with a non-zero status and lists the available analyzers in the error output. Configuration mutation is out of scope for this iteration; only the read shape is required.

**Error**: "Analyzer '<name>' not found. Run 'content-audit get analyzers' to see available analyzers."

### Rule[F-CLIRV-R021] - `stats` is a top-level verb that introspects a single analyzer's behavior on a course
**Severity**: major | **Validation**: VALIDATED

`content-audit stats analyzer <analyzer-name> [course-path]` runs the per-analyzer statistics rendering against the named analyzer over the named course (or the path resolved by the existing course-path resolution rules). The functional behavior is preserved verbatim from the existing `analyzer stats <name> [course]` command — the same statistics view, the same input resolution, the same exit-code semantics. The only change is the **placement** of the verb at the top of the command tree (`stats analyzer ...` instead of `analyzer stats ...`), which is consistent with the verb-then-resource grammar of this restructure.

`stats` is kept as a first-class domain verb instead of being folded into `get analyzer <id>` because the two answer different questions: `get analyzer <id>` reads static registry/config information (the analyzer's identity), while `stats analyzer <id> [course]` runs the analyzer over a course and renders behavioral metrics. They are not the same shape and conflating them would be a regression. This is the same reason `analyze` was kept distinct from `create audit` (R004 / R013).

If the named analyzer is not registered, the command fails with the same "analyzer not found" treatment as R016. If the course path cannot be resolved, the command fails with the same treatment as `analyze`.

**Error**: "Analyzer '<name>' not found. Run 'content-audit get analyzers' to see available analyzers." (when the analyzer name is unknown) — other errors inherited from the existing analyzer-stats behavior are preserved unchanged.

### Group E — Sandbox / workdir override

### Rule[F-CLIRV-R017] - The CLI accepts an external override for the workdir via `--workdir` flag and `CONTENT_AUDIT_HOME` env var
**Severity**: critical | **Validation**: VALIDATED

The CLI must support two external mechanisms by which the caller can redirect the location of the `.content-audit/` workdir (which contains `audits/`, `plans/`, and `revisions/`) without modifying the binary:

1. A **global `--workdir <path>` flag** at the root of the command, accepted before the verb (e.g. `content-audit --workdir /tmp/sandbox analyze ...`). The flag is documented in `--help` and is per-invocation.
2. An **environment variable `CONTENT_AUDIT_HOME`** read at CLI startup. Useful for test scripts and shell sessions that want to redirect every invocation without retyping the flag.

The chosen value must be resolved **before** any storage adapter is constructed, so that all three filesystem stores (`FileSystemAuditReportStore`, `FileSystemRefinementPlanStore`, `FileSystemRevisionArtifactStore`) are pointed at the same resolved root.

**Precedence**, from highest to lowest:

1. The `--workdir <path>` flag, if present on the command line.
2. The `CONTENT_AUDIT_HOME` environment variable, if set and non-empty.
3. The default fallback: `.content-audit/` resolved relative to `System.getProperty("user.dir")`, which is the operator's current working directory. This preserves the historical behavior.

The required behavior, regardless of which mechanism supplied the value:

1. If an override is provided (via flag or env var), every read or write performed by the CLI against `.content-audit/...` resolves under the override path. No artifact is read from or written to any other location.
2. If no override is provided, the CLI falls back to `user.dir` per item 3 above.
3. The override applies uniformly to all three stores in a single CLI invocation. It is not possible for the audit store to honor the override while the plan store ignores it — they share one resolved root.
4. If the resolved override path does not exist or is not a writable directory, the CLI exits with a non-zero status and a clear error **before** any store is constructed. It does not silently fall back to `user.dir`; an explicit override that turns out to be invalid is an operator mistake to surface, not to mask.

**Rationale**: this enables real shell-level end-to-end tests of the CLI against fixture courses without polluting the developer's actual `.content-audit/` directory. Two mechanisms cover the two real workflows: per-invocation tests use `--workdir`, while shell sessions and test harnesses that run many commands use `CONTENT_AUDIT_HOME`. The precedence (flag > env > default) mirrors how kubectl handles `KUBECONFIG` vs `--kubeconfig`.

**Error**: "The configured workdir override '<path>' is not a writable directory" (when the override is provided via either mechanism but unusable).

### Rule[F-CLIRV-R018] - The course content path is independent of the workdir override
**Severity**: major | **Validation**: AUTO_VALIDATED

The override defined in R017 applies only to the `.content-audit/` workdir (where audits, plans, and revisions are stored). It does **not** redirect the location of the course content folder that `analyze` reads (the path that today is supplied by the positional argument or by `CONTENT_AUDIT_CONTENT_FOLDER`). The two are deliberately decoupled so a sandbox test can point the workdir at a temp directory while still reading a fixture course from a stable location.

**Error**: N/A (this rule defines a non-overlap)

### Group F — Out of scope

### Rule[F-CLIRV-R019] - No domain logic changes
**Severity**: critical | **Validation**: VALIDATED

This requirement is exclusively a CLI surface restructure. It does not change anything about how audits are computed, how plans are generated, how correction contexts are resolved, how revisions are produced or applied, or how artifacts are stored on disk. The mapping of new verb to existing behavior is one-to-one. If any rule above appears to imply a domain change, that implication is unintended and the existing domain behavior wins.

**Error**: N/A (this rule defines a scope boundary)

### Rule[F-CLIRV-R020] - No backwards-compatibility shims
**Severity**: major | **Validation**: VALIDATED

The old `refiner ...` and `analyzer ...` command groups are removed without leaving deprecation aliases or pass-through shims. Invoking `content-audit refiner plan` (or `content-audit analyzer list`, `content-audit analyzer config <name>`, `content-audit analyzer stats <name> ...`) after this restructure must fail with the standard "unknown command" message produced by the CLI parser, the same as any other typo. The user has explicitly confirmed there are no external callers that would be broken.

This rule removes the **paths** `refiner *` and `analyzer *`, not the underlying capabilities. Each capability is re-homed under the new grammar by R013–R016 and R021. In particular, `stats` survives the dissolution of the `analyzer` group as a top-level domain verb (R021); the only thing that goes away is the `analyzer stats` command path, not the feature.

**Error**: (parser default) "Unknown command 'refiner'" / "Unknown command 'analyzer'"

---

## User Journeys

### Journey[F-CLIRV-J001] - Full happy-path pipeline end-to-end with the new grammar
**Validation**: AUTO_VALIDATED

Exercises the canonical operator flow under the new grammar: run an audit, list audits to confirm, derive a plan, list plans, list tasks, revise the next pending task, and finally prune older plans to clean up. Every step uses one of the new verbs.

```yaml
journeys:
  - id: F-CLIRV-J001
    name: Full happy-path pipeline end-to-end with the new grammar
    flow:
      - id: run_analyze
        action: "The operator invokes 'content-audit analyze <course-path>' against a course"
        gate: [F-CLIRV-R013]
        then: list_audits

      - id: list_audits
        action: "The operator invokes 'content-audit get audits' and the system lists all stored audits including the one just produced"
        gate: [F-CLIRV-R001, F-CLIRV-R005, F-CLIRV-R013]
        then: create_plan

      - id: create_plan
        action: "The operator invokes 'content-audit plan' and the system derives a plan from the most recent audit and prints the new plan id"
        gate: [F-CLIRV-R014]
        then: list_plans

      - id: list_plans
        action: "The operator invokes 'content-audit get plans' and the system lists all stored plans including the one just produced"
        gate: [F-CLIRV-R001, F-CLIRV-R005]
        then: list_tasks

      - id: list_tasks
        action: "The operator invokes 'content-audit get tasks --status pending --sort priority --limit 1' against the most recent plan and the system returns the single highest-priority pending task"
        gate: [F-CLIRV-R008, F-CLIRV-R009, F-CLIRV-R012]
        then: revise_task

      - id: revise_task
        action: "The operator invokes 'content-audit revise task <task-id>' with the id printed in the previous step and the revision workflow runs end-to-end as defined by FEAT-REVBYP"
        gate: [F-CLIRV-R015]
        then: prune_plans

      - id: prune_plans
        action: "The operator invokes 'content-audit prune plans --keep 1' and the system retains only the most recently created plan, removing older ones from .content-audit/plans/"
        gate: [F-CLIRV-R003]
        result: success
```

### Journey[F-CLIRV-J002] - Reading a single resource by id, present and absent
**Validation**: AUTO_VALIDATED

Exercises the contract of `get <resource> <id>`: success when the id matches a stored resource, explicit failure when it does not. Covers the case the user explicitly called out (`get audit <unknown-id>`).

```yaml
journeys:
  - id: F-CLIRV-J002
    name: Reading a single resource by id, present and absent
    flow:
      - id: invoke_get
        action: "The operator invokes 'content-audit get audit <id>' for some id"
        gate: [F-CLIRV-R001]
        outcomes:
          - when: "The id matches a stored audit"
            then: return_one
          - when: "No stored audit matches the id"
            then: return_not_found

      - id: return_one
        action: "The system returns the single matching audit and exits with status zero"
        result: success

      - id: return_not_found
        action: "The system reports that no audit was found with the given id and exits with non-zero status"
        gate: [F-CLIRV-R001]
        result: failure
```

### Journey[F-CLIRV-J003] - Deleting a resource, present and absent
**Validation**: AUTO_VALIDATED

Exercises the contract of `delete <resource> <id>`: success when the id matches a stored resource (and a subsequent `get` confirms the removal), explicit failure when it does not. Covers the case the user explicitly called out (`delete plan <unknown-id>`).

```yaml
journeys:
  - id: F-CLIRV-J003
    name: Deleting a resource, present and absent
    flow:
      - id: invoke_delete
        action: "The operator invokes 'content-audit delete plan <id>' for some id"
        gate: [F-CLIRV-R002]
        outcomes:
          - when: "The id matches a stored plan"
            then: confirm_removed
          - when: "No stored plan matches the id"
            then: report_not_found

      - id: confirm_removed
        action: "A subsequent 'content-audit get plan <id>' for the same id reports that no plan was found"
        gate: [F-CLIRV-R001, F-CLIRV-R002]
        result: success

      - id: report_not_found
        action: "The system reports that no plan was found with the given id and exits with non-zero status"
        gate: [F-CLIRV-R002, F-CLIRV-R010]
        result: failure
```

### Journey[F-CLIRV-J004] - Singular and plural forms produce the same result
**Validation**: AUTO_VALIDATED

Exercises the equivalence asserted by R006 across the three read-affecting verbs.

```yaml
journeys:
  - id: F-CLIRV-J004
    name: Singular and plural forms produce the same result
    flow:
      - id: invoke_form
        action: "The operator invokes the same logical request once using the singular resource form and once using the plural form (e.g. 'get audit' vs 'get audits', 'get plan <id>' vs 'get plans <id>')"
        gate: [F-CLIRV-R006]
        outcomes:
          - when: "The request is a list (no name argument)"
            then: same_list
          - when: "The request addresses one resource by id"
            then: same_one

      - id: same_list
        action: "The system returns the same list under both forms"
        gate: [F-CLIRV-R001, F-CLIRV-R006]
        result: success

      - id: same_one
        action: "The system returns the same single resource under both forms"
        gate: [F-CLIRV-R001, F-CLIRV-R006]
        result: success
```

### Journey[F-CLIRV-J005] - Sandbox workdir override redirects all artifact I/O
**Validation**: VALIDATED

Exercises the sandbox rule R017 from the operator's perspective: when an override is set, every artifact the CLI writes lands inside the override and nothing lands in the operator's `user.dir`.

```yaml
journeys:
  - id: F-CLIRV-J005
    name: Sandbox workdir override redirects all artifact I/O
    flow:
      - id: configure_workdir
        action: "The operator configures an external workdir override pointing to a sandbox directory before invoking the CLI"
        gate: [F-CLIRV-R017]
        outcomes:
          - when: "The override path exists and is writable"
            then: run_pipeline_in_sandbox
          - when: "The override path is not a writable directory"
            then: report_workdir_invalid
          - when: "No override is configured"
            then: run_pipeline_in_user_dir

      - id: run_pipeline_in_sandbox
        action: "The operator invokes 'content-audit analyze ... ; content-audit plan ; content-audit get plans' in sequence; every artifact (audit, plan) is written under the sandbox path's '.content-audit/' subtree and the user.dir's '.content-audit/' subtree is left untouched"
        gate: [F-CLIRV-R017, F-CLIRV-R018]
        result: success

      - id: run_pipeline_in_user_dir
        action: "The operator runs the same sequence without an override; every artifact is written under user.dir's '.content-audit/' subtree, which is the production fallback"
        gate: [F-CLIRV-R017]
        result: success

      - id: report_workdir_invalid
        action: "The system reports that the configured override path is not a writable directory and exits with non-zero status before any store is constructed"
        gate: [F-CLIRV-R017]
        result: failure
```

---

## Out of scope

The following items are deliberately excluded from this iteration. They may be revisited but they are not assumed:

- **Mutating `config`.** R016 only asserts the read shape (`config analyzer <id>` showing config). Setting or overriding analyzer config from the CLI is not in scope.
- **Non-`--keep` retention policies for `prune`** (e.g. `--older-than`, `--status completed`). Out of scope.
- **Resource discovery for `get tasks` across multiple plans.** R012 says `get tasks` defaults to the most recent plan; cross-plan task search (`get tasks --all-plans`, `get tasks --plan a,b,c`) is out of scope.
- **JSON / table output flags for the new verbs.** Existing per-command `--format` flags are preserved where they exist today, but no new `--format` contract is asserted by this requirement. Output formatting is left to the implementation.
- **Backwards-compatibility shims for `refiner *` and `analyzer *`.** R020 explicitly forbids them.
- **Domain logic changes.** R019 explicitly forbids them.
- **`delete task` and other in-place mutations of a plan.** R002 and R019 together rule this out. Tasks are derived state inside a plan; the only way to remove a task is to delete or regenerate the owning plan.
- **`--cascade` for `delete audit`.** R002 requires refusal when dependent plans exist, listing the plan ids. Adding a `--cascade` flag that auto-deletes dependents is out of scope for this iteration.

---

## Open Questions

All previously open doubts on this requirement have been resolved by the user and encoded as concrete rules:

- **DOUBT-WORKDIR-MECHANISM** → resolved by R017 (both `--workdir` flag and `CONTENT_AUDIT_HOME` env var, with flag > env > default precedence).
- **DOUBT-STATS** → resolved by R021 (kept as the top-level domain verb `stats analyzer <name> [course]`) and R020 (the only thing removed is the `analyzer stats` *path*, not the feature).
- **DOUBT-DELETE-CASCADE** → resolved by R002 (no cascade; `delete audit <id>` refuses with the dependent plan ids listed if any plan references it).
- **DOUBT-TASK-MUTATION** → resolved by R002 (no `delete task` command; `delete` operates on `audit` and `plan` only).

---

## ASSUMPTIONS

1. **The id printed by `analyze` is the canonical audit id.** R007 assumes that the timestamp-shaped id printed today by `analyze` (e.g. `2026-04-19T10-30-00`) is the addressable id for `get audit <id>` and `delete audit <id>`. This matches how `FileSystemAuditReportStore` already names its files. If the architecture later changes the id shape, R007 still holds in form: "use the printed id".
2. **Recency for `prune --keep N` uses `createdAt` for plans and the timestamp embedded in the id for audits.** R003 says "most recent" without specifying the timestamp source; the assumption is that each resource type already has an unambiguous creation timestamp on disk and that source is used. No new timestamp field is required.
3. **`get tasks` lookups operate on at most one plan at a time.** R008 and R012 assume task queries are scoped to a single plan (the one named by `--plan` or the most recent). Cross-plan task queries are out of scope (see "Out of scope").
4. **Task ids are sequential per plan, not globally unique — resolved.** Earlier drafts speculated that `revise task <id>` could resolve the owning plan from the task id alone. Verification in `DefaultRefinerEngine.plan()` confirmed task ids are formatted `task-NNN` and reset for every new plan, so the same id can name different tasks across plans. R015 has been updated accordingly: `revise task <id>` defaults to the most recent plan (mirroring R012's `get tasks` default) and accepts `--plan <plan-id>` to disambiguate. This is no longer an assumption; it is the resolved semantics.
5. **The workdir override is read once, at CLI startup.** R017 assumes a single resolved root for the duration of one CLI invocation. Per-store overrides, mid-invocation re-resolution, or store-specific roots are out of scope.
