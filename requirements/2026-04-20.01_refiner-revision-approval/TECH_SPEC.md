---
patch: ARCH-REVAPR
requirement: 2026-04-20.01_refiner-revision-approval
generated: 2026-04-20T12:00:00Z
---

# Tech Spec: Fase de revision con aprobacion humana

FEAT-REVAPR introduces a human-in-the-loop operating mode on top of the FEAT-REVBYP revision skeleton. The pipeline splits into two phases separated by a persisted `PENDING_APPROVAL` state: `revise task` generates and persists the proposal, and `approve proposal` / `reject proposal` close it later. The design reuses every port from FEAT-REVBYP (`Reviser`, `RevisionValidator`, `RevisionArtifactStore`, `RevisionEngine`, `CourseRepository`) and extends them minimally; the net shape added is one verdict value, one validator impl, two factory seams, one decide-phase port, three new store methods, and two new sealed CLI commands. No changes to `refiner-domain` or to the task status enum.

## Extend the verdict vocabulary with `PENDING_APPROVAL`

R001 makes `PENDING_APPROVAL` a third peer of `APPROVED`/`REJECTED` on the existing `RevisionVerdict` enum. Splitting the pending state into a separate enum would force every existing consumer (artifact printers, JSON serializers, the validator result) to handle two axes — we would gain nothing and break wire compatibility with FEAT-REVBYP artifacts. The bypass validator keeps emitting `APPROVED` unchanged; only the new `HumanApprovalValidator` emits the new value.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "RevisionVerdict"
        _change: "modify"
        fields:
          - name: "PENDING_APPROVAL"
            _change: "add"
            description: "The validator has deferred the decision to a human operator (R007). The artifact is persisted but the course is not written."
```

## Record the decide event inline on the artifact (resolves DOUBT-ARTIFACT-DECISION-RECORD)

The decide phase rewrites the artifact in place (Option A of the doubt): `verdict` flips from `PENDING_APPROVAL` to `APPROVED` or `REJECTED`, and two new fields capture when and why. Appending a separate `<proposalId>.decision.json` would double the files the store must keep consistent, and the "history" it preserves (that the artifact was once pending) is already inferable from `createdAt < decidedAt`. `decisionNote` is a single free-text field reused for both R011 `--note` and R012 `--reason`: both are unstructured operator rationale and distinguishing them by field would create spurious taxonomy. `decidedAt` is null for artifacts born in AUTO mode that never passed through `PENDING_APPROVAL`, which is how a reader tells a human-approved record apart from an auto-approved one without schema drift.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "RevisionArtifact"
        _change: "modify"
        fields:
          - name: "decidedAt"
            type: "Instant"
            _change: "add"
            description: "Timestamp when the verdict transitioned from PENDING_APPROVAL to APPROVED/REJECTED; null while pending or for auto-mode artifacts that never passed through PENDING_APPROVAL"
          - name: "decisionNote"
            type: "String"
            _change: "add"
            description: "Free-text note attached by the operator on approve/reject (R011 --note, R012 --reason); null if not provided"
```

## Introduce `ApprovalMode` as the single degree of freedom exposed to the composition root

The validator selection is a closed set of exactly two choices (`AUTO`, `HUMAN`) fixed at CLI startup (R005/R006). Modelling it as an enum — rather than a boolean or a raw string — keeps exhaustiveness checks honest for the factory's switch and makes future composition-level work (declarative pipelines, per-diagnosis overrides) a matter of adding an enum value or a new port, not reinterpreting a magic string. The enum lives in `revision-domain` because the selection concept is a domain concept; the env-var vocabulary (`"auto"`, `"human"`, case-insensitive) is parsed in the CLI and never crosses the module boundary.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "ApprovalMode"
        _change: "add"
        type: "enum"
        fields:
          - name: "AUTO"
            description: "Bypass validator — auto-approves every proposal (R005, inherited from FEAT-REVBYP R007)"
          - name: "HUMAN"
            description: "Human validator — every proposal becomes PENDING_APPROVAL (R005/R007)"
```

## Add a factory seam for the active validator

`RevisionValidatorFactory` is the one-call construction point that `Main.java` uses to pick between `AutoApproveValidator` and the new `HumanApprovalValidator`. Without the factory, either the CLI must depend directly on the `engine` package (breaking P4 — composition root is the only leak point, but the existing `DefaultRevisionEngineFactory` demonstrates the seam pattern we want to stay consistent with) or every call site must import both concrete validators. The factory is declared `stereotype: "factory"` with `Factory` pattern wiring so ArchUnit enforces that `DefaultRevisionValidatorFactory` is the only public class behind the seam; the `HumanApprovalValidator` collaborator stays package-private alongside `AutoApproveValidator`.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "RevisionValidatorFactory"
        _change: "add"
        stereotype: "factory"
        exposes:
          - signature: "create(ApprovalMode mode): RevisionValidator"
    patterns:
      - type: "Factory"
        interface: "RevisionValidatorFactory"
        implementations: ["DefaultRevisionValidatorFactory"]
    packages:
      - name: "engine"
        _change: "modify"
        implementations:
          - name: "HumanApprovalValidator"
            _change: "add"
            implements: ["RevisionValidator"]
            types: ["Component"]
          - name: "DefaultRevisionValidatorFactory"
            _change: "add"
            visibility: "public"
            implements: ["RevisionValidatorFactory"]
            types: ["Component"]
```

## Expose the decide phase as its own port `ProposalDecisionService`

R011/R012/R013 describe a flow that is distinct from `RevisionEngine.revise`: it takes a `proposalId` (not a `taskId`), operates on an already-persisted artifact, rewrites its verdict, and conditionally runs the apply-path. Folding this into `RevisionEngine` would inflate its contract and confuse traceability — `revise` is about generating a proposal; `approve`/`reject` is about deciding one. Per P6 (one seam per capability), the decide phase gets its own port. `approve` takes a `Path coursePath` because the apply-path still needs to load and rewrite the course; `reject` does not. Both return a `ProposalDecisionOutcome` analogous to `RevisionOutcome` so the CLI can switch on a single enum and emit the correct exit code / message.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "ProposalDecisionService"
        _change: "add"
        stereotype: "port"
        exposes:
          - signature: "approve(String proposalId, Optional<String> planId, Optional<String> note, Path coursePath): ProposalDecisionOutcome"
          - signature: "reject(String proposalId, Optional<String> planId, Optional<String> reason): ProposalDecisionOutcome"
    models:
      - name: "ProposalDecisionOutcomeKind"
        _change: "add"
        type: "enum"
        fields:
          - name: "APPROVED_APPLIED"
            description: "Proposal approved and course rewritten (R011)"
          - name: "APPROVED_APPLY_FAILED"
            description: "Proposal approved and artifact rewritten, but course write failed (R011 step 6, DOUBT-ATOMICITY)"
          - name: "REJECTED"
            description: "Proposal rejected; course untouched (R012)"
          - name: "NOT_FOUND"
            description: "No proposal matches the supplied id (R002/R011/R012)"
          - name: "ALREADY_DECIDED"
            description: "Proposal exists but its verdict is not PENDING_APPROVAL (R013)"
      - name: "ProposalDecisionOutcome"
        _change: "add"
        type: "record"
        fields:
          - { name: "kind", type: "ProposalDecisionOutcomeKind" }
          - { name: "artifact", type: "RevisionArtifact", description: "The artifact after the decision (APPROVED/REJECTED); null for NOT_FOUND" }
          - { name: "errorMessage", type: "String", description: "Human-readable message for NOT_FOUND/ALREADY_DECIDED/APPROVED_APPLY_FAILED" }
```

## Add a second factory seam for the decide phase, reusing `RevisionEngineConfig`

`DefaultProposalDecisionService` needs the same collaborators the propose-phase already wires (`RevisionArtifactStore`, `CourseRepository`, `CourseElementLocator`, `RefinementPlanStore`), so `RevisionEngineConfig` is reused as the carrier — introducing a second near-identical config record would only duplicate defaults. A separate factory (rather than a second method on `RevisionEngineFactory`) keeps P6 honest: one seam per capability. The decide-phase engine itself lives package-private inside the `engine` package; only the factory is public.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "ProposalDecisionServiceFactory"
        _change: "add"
        stereotype: "factory"
        exposes:
          - signature: "create(RevisionEngineConfig config): ProposalDecisionService"
    patterns:
      - type: "Factory"
        interface: "ProposalDecisionServiceFactory"
        implementations: ["DefaultProposalDecisionServiceFactory"]
    packages:
      - name: "engine"
        _change: "modify"
        implementations:
          - name: "DefaultProposalDecisionServiceFactory"
            _change: "add"
            visibility: "public"
            implements: ["ProposalDecisionServiceFactory"]
            types: ["Component"]
          - name: "DefaultProposalDecisionService"
            _change: "add"
            implements: ["ProposalDecisionService"]
            types: ["Component"]
            requiresInject:
              - { name: "artifactStore", type: "RevisionArtifactStore" }
              - { name: "courseRepository", type: "CourseRepository" }
              - { name: "elementLocator", type: "CourseElementLocator" }
              - { name: "refinementPlanStore", type: "RefinementPlanStore", description: "Needed to clear the task back to PENDING on reject (R014) and close it as DONE on approve" }
```

## Extend `RevisionArtifactStore` with proposalId lookup and a pending-proposal predicate

R002 makes `proposal` a first-class resource identified by `proposalId` alone (with optional `--plan` scope, per DOUBT-PROPOSAL-LOOKUP Option B). R010 needs the engine to answer "does this task have a pending proposal?" before generating a new one. Both are queries, not new entities, so they are modeled as three new methods on the existing store rather than a side-car index. `findByProposalId` accepts an `Optional<String> planId` so callers with the plan scope can hit the direct path (`<planId>/<proposalId>.json`), and callers without scope fall back to a directory scan — acceptable because the number of plans is bounded in practice. `list()` returns every known artifact across every plan, enabling `get proposals` without filters.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    interfaces:
      - name: "RevisionArtifactStore"
        _change: "modify"
        exposes:
          - signature: "findByProposalId(String proposalId, Optional<String> planId): Optional<RevisionArtifact>"
            _change: "add"
          - signature: "hasPendingProposalForTask(String planId, String taskId): boolean"
            _change: "add"
          - signature: "list(): List<RevisionArtifact>"
            _change: "add"
```

## Propagate `RevisionArtifactStore` surface changes to the filesystem adapter

The filesystem store is the only adapter today; declaring `_change: "modify"` on `FileSystemRevisionArtifactStore` forces `sentinel generate` to regenerate its `@Generated` scaffolding against the expanded interface surface, so the Developer agent inherits the method stubs rather than having to add them by hand.

```architecture
modules:
  - name: "audit-infrastructure"
    _change: "modify"
    implementations:
      - name: "FileSystemRevisionArtifactStore"
        _change: "modify"
        implements: ["RevisionArtifactStore"]
```

## Preserve `RefinementTaskStatus` unchanged (resolves DOUBT-AWAITING-STATE)

The requirement's DOUBT-AWAITING-STATE explicitly selects Option B: task stays in `PENDING`, and "has pending proposal" is a derived predicate. This patch therefore does NOT touch `RefinementTaskStatus`. The predicate lives on `RevisionArtifactStore.hasPendingProposalForTask`, co-located with the only store that can answer it. R014's "task returns to its previous state on reject" is trivially satisfied because the state was never mutated on the propose phase. R009's "observable distinct from PENDING and DONE" is satisfied at the read layer: `get tasks` can join each task against `hasPendingProposalForTask(planId, taskId)` and render `AWAITING_APPROVAL` as a derived label in the output, without any enum change rippling through existing readers.

## Encode R010 (already-pending and pending-persisted) in `RevisionOutcomeKind`

The propose phase's outcome enum gains two values rather than throwing new exceptions: `PENDING_APPROVAL_PERSISTED` for the happy human-mode path (R008/J001) and `ALREADY_PENDING_DECISION` for R010's refusal to stack pending proposals on the same task. Reusing the outcome-kind switch the CLI already owns for FEAT-REVBYP is cheaper than introducing checked exceptions and keeps the exit-code mapping concentrated in one place (`ReviseCmd.handleOutcome`). `DefaultRevisionEngine` is marked modify so the developer re-implements these branches.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "RevisionOutcomeKind"
        _change: "modify"
        fields:
          - name: "PENDING_APPROVAL_PERSISTED"
            _change: "add"
            description: "Proposal persisted with PENDING_APPROVAL; course untouched (R008)"
          - name: "ALREADY_PENDING_DECISION"
            _change: "add"
            description: "revise rejected because the task already has a pending proposal (R010)"
    packages:
      - name: "engine"
        _change: "modify"
        implementations:
          - name: "DefaultRevisionEngine"
            _change: "modify"
            requiresInject:
              - { name: "refinementPlanStore", type: "RefinementPlanStore" }
              - { name: "auditReportStore", type: "AuditReportStore" }
              - { name: "contextResolver", type: "CorrectionContextResolver<CorrectionContext>" }
              - { name: "reviser", type: "Reviser", description: "Normally a DispatchingReviser" }
              - { name: "validator", type: "RevisionValidator" }
              - { name: "artifactStore", type: "RevisionArtifactStore" }
              - { name: "courseRepository", type: "CourseRepository" }
              - { name: "elementLocator", type: "CourseElementLocator" }
```

## Parse `CONTENT_AUDIT_APPROVAL_MODE` in the `bootstrap` package

`CONTENT_AUDIT_APPROVAL_MODE` is the same kind of concern `DefaultWorkdirResolver` already handles: translate an external (env-var) string into a domain object at startup, fail fast on bad input. Co-locating `DefaultApprovalModeResolver` with the workdir resolver keeps all "env -> domain" parsing under one roof and out of `Main.java`. The resolver is `sealed`: exactly one implementation will ever exist because there is exactly one env-var format. `InvalidApprovalModeException` keeps the failure message centralized so R005's wording ("Allowed: auto, human") is defined in the contract, not scattered across the CLI.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    packages:
      - name: "bootstrap"
        _change: "modify"
        models:
          - name: "InvalidApprovalModeException"
            _change: "add"
            type: "exception"
            extends: "RuntimeException"
            message: "Invalid value for CONTENT_AUDIT_APPROVAL_MODE: '%s'. Allowed: auto, human"
            fields:
              - { name: "value", type: "String", description: "The raw env-var value that failed parsing" }
        interfaces:
          - name: "ApprovalModeResolver"
            _change: "add"
            stereotype: "port"
            sealed: true
            exposes:
              - signature: "resolve(String envValue): ApprovalMode"
                throws: ["InvalidApprovalModeException"]
        implementations:
          - name: "DefaultApprovalModeResolver"
            _change: "add"
            visibility: "public"
            implements: ["ApprovalModeResolver"]
```

## Add sealed `ApproveCommand` and `RejectCommand` interfaces

FEAT-CLIRV established that every top-level verb is a sealed interface so the universe of verbs is closed at compile time. `approve` and `reject` are proposal-only verbs (R015) with single implementations, so both interfaces are sealed with a single permitted class. They are declared as separate interfaces rather than a combined `DecideCommand` because the signatures diverge (`--note` vs `--reason`) and their picocli command names are distinct — merging them would force a resource-discriminator argument on every call and lose R015's "approve cannot be invoked on non-proposal resources" at the type level.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    interfaces:
      - name: "ApproveCommand"
        _change: "add"
        stereotype: "port"
        sealed: true
        exposes:
          - signature: "approve(String resource, String proposalId, String planId, String note): Integer"
      - name: "RejectCommand"
        _change: "add"
        stereotype: "port"
        sealed: true
        exposes:
          - signature: "reject(String resource, String proposalId, String planId, String reason): Integer"
```

## Add `ApproveCmd` and `RejectCmd` as the CLI seams for the decide phase

Both impls carry exactly one collaborator — `ProposalDecisionService` — because the CLI should not know about artifact storage, course repositories, or plan mutation details. They implement `Callable<Integer>` for picocli, keep the resource discriminator positional (consistent with `ReviseCmd` taking `"task"` as its first parameter), and translate `ProposalDecisionOutcomeKind` into exit codes the same way `ReviseCmd.handleOutcome` already does for `RevisionOutcomeKind`.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    packages:
      - name: "commands"
        _change: "modify"
        implementations:
          - name: "ApproveCmd"
            _change: "add"
            implements: ["ApproveCommand"]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            types: ["Component"]
            requiresInject:
              - { name: "decisionService", type: "ProposalDecisionService" }
          - name: "RejectCmd"
            _change: "add"
            implements: ["RejectCommand"]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            types: ["Component"]
            requiresInject:
              - { name: "decisionService", type: "ProposalDecisionService" }
```

## Extend `GetCmd` with the `proposal` / `proposals` resource branch

R002/R003 make `get proposals` and `get proposal <id>` read operations over the new resource, with optional `--plan` and `--status pending|approved|rejected` filters. `GetCommand.get(String resource, String name, GetTasksFilter filter): Integer` already dispatches on a resource string, so no interface change is needed — only a new collaborator injection so `GetCmd` can reach the artifact store. `GetTasksFilter` stays as-is: its existing `planId` and `status` fields carry the two filters R003 requires; the CLI parses case-insensitive `pending|approved|rejected` exactly the way it already parses `pending|completed|skipped` for tasks. Overloading `status`'s vocabulary by resource context is cheap (one more `switch`) and avoids a schema mutation on a carrier consumed by every `get` resource.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    packages:
      - name: "commands"
        _change: "modify"
        implementations:
          - name: "GetCmd"
            _change: "modify"
            implements: ["GetCommand"]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            types: ["Component"]
            requiresInject:
              - { name: "auditReportStore", type: "AuditReportStore" }
              - { name: "refinementPlanStore", type: "RefinementPlanStore" }
              - { name: "analyzerRegistry", type: "AnalyzerRegistry" }
              - { name: "correctionContextResolver", type: "CorrectionContextResolver" }
              - { name: "revisionArtifactStore", type: "RevisionArtifactStore", description: "Needed for the new 'proposal'/'proposals' resource branch (R002/R003/F-REVAPR)" }
```
