# Decisions

2026-04-20 — analyst — Validator selection via env var CONTENT_AUDIT_APPROVAL_MODE (auto|human), not CLI flag.
  why: Prevents operators from running with inconsistent modes within one workdir; default value left OPEN as DOUBT.

2026-04-20 — analyst — `approve` / `reject` are proposal-only verbs (R015).
  why: Keep kubectl-style grammar but not all verbs apply to all resources; future resources (PipelineConfig) may widen this.

2026-04-20 — analyst — Re-revising a task with a PENDING proposal is rejected, not silently superseded (R010).
  why: Safer default — no silent loss of pending decisions; DOUBT-SUPERSEDE tracks whether to add `--supersede` later.

2026-04-20 — analyst — Deciding an already-decided proposal is an error, not a no-op (R013).
  why: Protects the audit trail — a decided proposal may have already affected the course; rewriting it decouples artifact history from real world effect.

2026-04-20 — analyst — `delete proposal` / `prune proposals` explicitly OUT of scope (R004).
  why: Proposals are audit artifacts; removal would erase the trace of revision attempts. Revisit later.

2026-04-20 — analyst — Declared as open for architect: DOUBT-ARTIFACT-DECISION-RECORD, DOUBT-AWAITING-STATE, DOUBT-PROPOSAL-LOOKUP.
  why: All three are storage/state-shape decisions that belong in sentinel.yaml, not in REQUIREMENT.md.

2026-04-20 — analyst — Declared as open for user: DOUBT-APPROVAL-MODE-DEFAULT, DOUBT-SUPERSEDE.
  why: Both are product/UX defaults — no single correct answer without user input.

2026-04-20 — architect — DOUBT-ARTIFACT-DECISION-RECORD = Option A (rewrite in place).
  why: Adding two fields (decidedAt, decisionNote) to RevisionArtifact is cheaper than a side-car decision file; createdAt<decidedAt preserves the "was pending" inference.

2026-04-20 — architect — DOUBT-AWAITING-STATE = Option B (no new enum value).
  why: RefinementTaskStatus untouched; hasPendingProposalForTask on RevisionArtifactStore is the derived predicate. "AWAITING_APPROVAL" is a read-side label in get tasks, not a persisted state.

2026-04-20 — architect — DOUBT-PROPOSAL-LOOKUP = Option B (optional --plan scope).
  why: RevisionArtifactStore.findByProposalId(String, Optional<String>) — direct path when plan is known, subdirectory scan otherwise. No auxiliary index file (Option C) in this iteration.

2026-04-20 — architect — ApprovalMode as sealed enum (AUTO, HUMAN) in revision-domain.
  why: Env-var vocabulary lives in audit-cli.bootstrap; only the enum crosses module boundaries. Exhaustive switch in DefaultRevisionValidatorFactory.

2026-04-20 — architect — Two separate factory seams (RevisionValidatorFactory, ProposalDecisionServiceFactory).
  why: P6 one-seam-per-capability; both live in the engine package alongside DefaultRevisionEngineFactory. ProposalDecisionServiceFactory reuses RevisionEngineConfig (same collaborators).

2026-04-20 — architect — decisionNote is a single field used for both --note (approve) and --reason (reject).
  why: Both are unstructured operator rationale; splitting fields would invent a taxonomy with no semantic payoff.

2026-04-20 — architect — R010 surfaced as RevisionOutcomeKind.ALREADY_PENDING_DECISION, not an exception.
  why: Reuses the exit-code switch already owned by ReviseCmd.handleOutcome; checked exceptions would duplicate the error-handling surface.

2026-04-20 — architect — GetTasksFilter unchanged; status field is resource-contextual (pending|completed|skipped for tasks, pending|approved|rejected for proposals).
  why: Avoids a schema mutation on a carrier used by every get resource; the case-insensitive vocabulary switch is one local branch in GetCmd.

2026-04-20 — architect — ApproveCommand/RejectCommand declared sealed with single permits (ApproveCmd, RejectCmd).
  why: Consistent with FEAT-CLIRV sealed verb hierarchy; signatures diverge (--note vs --reason) so a combined DecideCommand would lose R015's type-level scoping.
