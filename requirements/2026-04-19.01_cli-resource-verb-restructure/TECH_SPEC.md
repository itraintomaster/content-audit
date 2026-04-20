---
patch: FEAT-CLIRV
requirement: 2026-04-19.01_cli-resource-verb-restructure
generated: 2026-04-19T20:00:00Z
---

# Tech Spec: CLI restructure to kubectl-style verb-resource grammar

This spec describes how the `audit-cli` module is restructured to support the kubectl-style `<verb> <resource>` surface defined by FEAT-CLIRV. All changes are confined to `audit-cli`: domain modules, application, and infrastructure adapters are untouched (R019, R020). The patch is one-shot — the old `Refiner*` and `Analyzer*` command interfaces and their picocli implementations are removed without shims, and the new flat verb-shaped contracts are introduced alongside a workdir resolver consumed by `Main`.

## Flatten the top-level CLI surface to verbs

The CLI tree changes from a two-level shape (`refiner plan`, `analyzer config`, ...) to a flat verb-first shape (`get`, `delete`, `prune`, `analyze`, `plan`, `revise`, `config`, `stats`) directly under `ContentAuditCmd`. Every former subcommand is re-homed as a top-level sealed `*Command` interface; intermediate grouping shells (`RefinerCmd`, `AnalyzerCmd`) are deleted with no replacement (R020). Each command stays sealed with no permits because the universe of CLI verbs is fixed at design time and there is no plugin point — exhaustiveness over the sealed set is the goal.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: RefinerPlanCommand
        _change: delete
      - name: RefinerNextCommand
        _change: delete
      - name: RefinerListCommand
        _change: delete
      - name: RefinerReviseCommand
        _change: delete
      - name: AnalyzerListCommand
        _change: delete
      - name: AnalyzerConfigCommand
        _change: delete
      - name: AnalyzerStatsCommand
        _change: delete
      - name: GetCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "get(String resource, String name, GetTasksFilter filter): Integer"
      - name: DeleteCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "delete(String resource, String id): Integer"
      - name: PruneCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "prune(String resource, int keep): Integer"
      - name: PlanCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "plan(String auditId): Integer"
      - name: ReviseCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "revise(String taskId, String planId): Integer"
      - name: ConfigAnalyzerCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "showConfig(String analyzerName): Integer"
      - name: StatsAnalyzerCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "showStats(String analyzerName, String coursePath): Integer"
```

## Model `GetCommand` as a single dispatcher across all four resources

`get` operates on four resources (`audits`, `plans`, `tasks`, `analyzers`) and accepts both singular and plural forms (R005, R006). Modeling it as four parallel `Get*Command` interfaces would force resource-name normalization, the singular/plural equivalence, and the unknown-resource error into the picocli wiring layer — none of which is interesting CLI tree logic. A single dispatcher with the signature `get(resource, name, filter)` keeps R005 and R006 in one place inside `GetCmd`, and lets `GetTasksFilter` carry every filter without growing a separate command interface for tasks. The trade-off accepted is a single method that switches on the resource string; we judge that locality of resource-specific behavior is more valuable than four near-empty interfaces.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: GetCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "get(String resource, String name, GetTasksFilter filter): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: GetCmd
            _change: add
            types: [Component]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            implements: [GetCommand]
            requiresInject:
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: analyzerRegistry, type: AnalyzerRegistry }
              - { name: correctionContextResolver, type: CorrectionContextResolver }
```

`GetCmd` injects four collaborators. The first three (`auditReportStore`, `refinementPlanStore`, `analyzerRegistry`) cover the four resources. The fourth (`correctionContextResolver`) was added in a follow-up remediation iteration (FEAT-RCSL/FEAT-RCLA): when `get tasks` enriches its output with the per-task correction context, it dispatches through the resolver instead of teaching the CLI about every diagnosis kind. The resolver's `Optional<T>` return shape lets `GetCmd` print the context only for tasks that have one, without branching on diagnosis kind in the CLI.

## Extend `GetTasksFilter` with `target` and `diagnosisKind` to restore the dropped `refiner next` filters

A migration gap surfaced after the kubectl-style restructure landed: the old `refiner next` command exposed `--target <enum>` (`-t`) and `--diagnosis <enum>` (`-d`) filters that narrow the candidate task set by `nodeTarget` and `diagnosisKind`. The first cut of `GetTasksFilter` carried only `planId`, `status`, `sortByPriority`, and `limit`, so the analyst's update to R008 (which restored `--target` and `--diagnosis` as flags on `get tasks`) had no carrier to reach `GetCmd.get()`. Two `Optional<>` fields plug the gap directly. The fields are typed as `Optional<AuditTarget>` and `Optional<DiagnosisKind>` rather than `Optional<String>` because both enums are already cross-module-importable from `audit-cli` (the module already depends on `audit-domain` for `AuditTarget` and on `refiner-domain` for `DiagnosisKind`) — so no new dependency edges are needed and the case-insensitive parsing required by R008 happens at the picocli boundary, not inside the dispatcher. The `GetCommand.get(resource, name, GetTasksFilter)` signature is unchanged: adding fields to the carrier doesn't touch the contract.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: GetTasksFilter
        _change: add
        type: record
        fields:
          - { name: planId, type: "Optional<String>", description: "Restrict to tasks belonging to this plan id; if empty, the most recent plan is used (R012)" }
          - { name: status, type: "Optional<String>", description: "Restrict to tasks whose status matches this value, case-insensitive (pending/completed/skipped) — R008" }
          - { name: sortByPriority, type: boolean, description: "If true, order results by ascending task priority — R008" }
          - { name: limit, type: "Optional<Integer>", description: "Cap the result count after filter+sort; 0 yields empty (R009); negative is rejected" }
          - { name: target, type: "Optional<AuditTarget>", description: "Restrict to tasks whose nodeTarget matches this AuditTarget enum value; absent = no filter (R008)" }
          - { name: diagnosisKind, type: "Optional<DiagnosisKind>", description: "Restrict to tasks whose diagnosisKind matches this DiagnosisKind enum value; absent = no filter (R008)" }
```

## Carve `DeleteCommand` and `PruneCommand` as separate write-side dispatchers

Both `delete` and `prune` are write operations but their shapes diverge from `get` in incompatible ways. `delete` restricts its resource set to `{audit, plan}` with refusal semantics (R002 — no `delete task`, no `delete analyzer`, and `delete audit <id>` refuses if dependent plans reference it instead of cascading). `prune` takes an `int keep` retention count rather than an id, indexes recency by `createdAt` for plans and by the timestamp embedded in the audit id, and operates only on `{audits, plans}` (R003). Overloading `GetCommand` with these would obscure the read/write boundary and force `GetCommand` to expose state-mutating semantics it deliberately does not have (R001 keeps `get` strictly read-only). Two separate sealed dispatchers keep the verb intent explicit at the type level.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: DeleteCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "delete(String resource, String id): Integer"
      - name: PruneCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "prune(String resource, int keep): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: DeleteCmd
            _change: add
            types: [Component]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            implements: [DeleteCommand]
            requiresInject:
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
          - name: PruneCmd
            _change: add
            types: [Component]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            implements: [PruneCommand]
            requiresInject:
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
```

`DeleteCmd` and `PruneCmd` both inject the same two stores because the cascade-refusal check in `delete audit` needs to scan plans for dependent `sourceAuditId` references, and `prune` needs to enumerate both stores. R019 forbids domain logic changes, so neither command can reach into the refiner engine — they operate against the persistence ports only.

## Promote `plan` to a top-level domain verb

`plan` (formerly `refiner plan`) creates a refinement plan from an audit report and is the only command that does so. The `--audit <id>` flag selects a specific audit; if absent, the most recent audit is used (R014). The verb is preserved over a generic `create plan` because the planning algorithm carries domain semantics that "create" would erase (R004), the same justification kubectl uses for `rollout` and `scale`. Behavior of the planning logic itself is unchanged from the deleted `RefinerPlanCommand`; only the placement and the deletion of the intermediate `refiner` group are new.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: PlanCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "plan(String auditId): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: PlanCmd
            _change: add
            types: [Component]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            implements: [PlanCommand]
            requiresInject:
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinerEngine, type: RefinerEngine }
              - { name: refinementPlanStore, type: RefinementPlanStore }
```

## Carry an optional `planId` on `ReviseCommand` to disambiguate sequential task ids

`ReviseCommand.revise(taskId, planId): Integer` carries both ids deliberately. `DefaultRefinerEngine.plan()` formats task ids as `task-001`, `task-002`, ... and resets the counter for every new plan, so the same `task-001` exists in every plan and the bare-task-id form would silently revise the wrong plan's task. The signature mirrors R012's `get tasks` default: a null/blank `planId` resolves to the most recent plan (so the kubectl-style `revise task <id>` UX still works), and a non-blank `planId` must resolve to an existing plan (R015 spells out the three failure modes). `ReviseCmd` injects `refinementPlanStore` directly because the plan-resolution logic — "most recent plan", or "named plan or fail" — is owned by the CLI, not the revision engine.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: ReviseCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "revise(String taskId, String planId): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: ReviseCmd
            _change: add
            types: [Component]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            implements: [ReviseCommand]
            requiresInject:
              - { name: revisionEngine, type: RevisionEngine }
              - { name: refinementPlanStore, type: RefinementPlanStore }
```

## Rename analyzer-tail commands to verb-first names

`AnalyzerConfigCommand` and `AnalyzerStatsCommand` become `ConfigAnalyzerCommand` and `StatsAnalyzerCommand`. The rename is not cosmetic: the new grammar reads `<verb> <resource>` at every level of the system, so the type names follow the same shape as the picocli command paths (`config analyzer <name>`, `stats analyzer <name>`). `stats` survives the dissolution of the `analyzer` group as a top-level domain verb (R021) — folding it into `get analyzer <id>` would conflate behavioral metrics with static registry config, which is precisely why R004 keeps domain verbs first-class. `StatsAnalyzerCmd` retains the existing fallback to `CONTENT_AUDIT_CONTENT_FOLDER` env var when no positional course-path is supplied; the underlying behavior is preserved verbatim.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: ConfigAnalyzerCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "showConfig(String analyzerName): Integer"
      - name: StatsAnalyzerCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "showStats(String analyzerName, String coursePath): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: ConfigAnalyzerCmd
            _change: add
            types: [Component]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            implements: [ConfigAnalyzerCommand]
            requiresInject:
              - { name: analyzerRegistry, type: AnalyzerRegistry }
          - name: StatsAnalyzerCmd
            _change: add
            types: [Component]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            implements: [StatsAnalyzerCommand]
            requiresInject:
              - { name: analyzerRegistry, type: AnalyzerRegistry }
              - { name: analyzerStatsTransformer, type: AnalyzerStatsTransformer }
              - { name: auditRunner, type: AuditRunner }
```

## Add the `bootstrap` package for workdir resolution

R017 introduces an external override for the `.content-audit/` workdir via a `--workdir` flag and `CONTENT_AUDIT_HOME` env var, with precedence `flag > env > user.dir`. The resolver must run *before* any of the three `FileSystem*Store` adapters is constructed, so all three end up pointing at the same resolved root; mid-construction re-resolution or per-store overrides are explicitly out of scope. A new `bootstrap` package owns this concern as a Public Port / Hidden Adapter pair: `WorkdirResolver` is the sealed port (closed set — there is no plugin need today, so sealing gives the compiler exhaustiveness), `DefaultWorkdirResolver` is the only permitted implementation. Sealing aligns with P7 (sealed by default for closed sets) and lets a future env-stub adapter join the seal explicitly if integration tests ever need one. `InvalidWorkdirException` carries the R017 error template (`"The configured workdir override '%s' is not a writable directory"`) so the fail-fast path on an invalid override is built into the type itself, not duplicated at every callsite. The package keeps the `bootstrap` concern out of `commands`, where it would otherwise leak alongside the picocli verb tree.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: add
        description: "CLI bootstrap concerns — workdir resolution from --workdir flag, CONTENT_AUDIT_HOME env var, or user.dir fallback (R017). Public package because Main (in the commands package) and external test launchers both need to resolve a workdir before constructing the persistence stores."
        visibility: internal
        models:
          - name: InvalidWorkdirException
            _change: add
            type: exception
            extends: RuntimeException
            message: "The configured workdir override '%s' is not a writable directory"
            fields:
              - { name: path, type: String, description: "The override value that failed validation" }
        interfaces:
          - name: WorkdirResolver
            _change: add
            stereotype: port
            sealed: true
            exposes:
              - signature: "resolve(String flagValue): Path"
        implementations:
          - name: DefaultWorkdirResolver
            _change: add
            visibility: public
            implements: [WorkdirResolver]
```

`DefaultWorkdirResolver` is `visibility: public` because `Main` (the composition root in the `commands` package) needs to instantiate it before picocli runs — the resolved path is passed to all three `FileSystem*Store` constructors. R018 keeps the override decoupled from the course content folder: the resolver returns a single `Path` and never consults `CONTENT_AUDIT_CONTENT_FOLDER`, so a sandbox test can redirect artifact I/O while still reading a fixture course from a stable location.

## Remove the old `Refiner*` and `Analyzer*` interfaces with no shims

R020 mandates a one-shot removal: every interface that addressed the old command shape is deleted in the same patch that introduces the new verbs. The list — `RefinerPlanCommand`, `RefinerNextCommand`, `RefinerListCommand`, `RefinerReviseCommand`, `AnalyzerListCommand`, `AnalyzerConfigCommand`, `AnalyzerStatsCommand` — captures every sealed port that participated in the prior tree. The corresponding picocli implementations (`RefinerPlanCmd`, `RefinerNextCmd`, `RefinerListCmd`, `RefinerReviseCmd`, `AnalyzerListCmd`, `AnalyzerConfigCmd`, `AnalyzerStatsCmd`) and the two intermediate group shells (`RefinerCmd`, `AnalyzerCmd`) are removed in lockstep. No deprecation aliases are added; the user has confirmed there are no external callers, and a parallel surface would only delay the cleanup. Capabilities are not lost: `refiner plan` re-homes as top-level `plan`, `refiner revise` as `revise task <id>`, `refiner list` as `get tasks`, `refiner next` as `get tasks --status pending --sort priority --limit 1`, `analyzer list` as `get analyzers`, `analyzer config` as `config analyzer <id>`, and `analyzer stats` as `stats analyzer <name> [course]` (R013–R016, R021).

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: RefinerPlanCommand
        _change: delete
      - name: RefinerNextCommand
        _change: delete
      - name: RefinerListCommand
        _change: delete
      - name: RefinerReviseCommand
        _change: delete
      - name: AnalyzerListCommand
        _change: delete
      - name: AnalyzerConfigCommand
        _change: delete
      - name: AnalyzerStatsCommand
        _change: delete
```
