# Fix Log

(empty — no implementation/test fixes yet)

2026-04-20 — qa-tester — Merged 7 gap-filling handwrittenTests (R001/R004x2/R006x2/R016/R017) into architectural_patch.yaml; all 17 REVAPR rules now have at least one directly-tagged handwrittenTest in sentinel.yaml (verified by grep).
  why: User rejected transitive coverage; every rule now carries explicit test tagging.

2026-04-20 — qa-tester — ESCALATION: patch apply silently dropped `testModule`/`testPackage` on all 5 FEAT-REVAPR journeys (J001..J005). Patch file contains them; sentinel.yaml does NOT. Other features' placements survived (e.g. F-REVBYP, F-CLIRV, F-RCLA) — REVAPR-specific silent drop. Do NOT rerun apply to work around; this is a framework-level bug (see `.bugs/` entry on silent no-ops).
  why: Per qa-tester protocol Step 7, flag discrepancy and stop. test-writer cannot rely on journey placement until framework bug is resolved or user re-applies with a fix.

2026-04-20 — test-writer — R009: task status assertion uses RefinementTaskStatus.PENDING (not a new AWAITING_APPROVAL value).
  why: DOUBT-AWAITING-STATE resolved as Option B — no new enum value added; "awaiting approval" is derived from hasPendingProposalForTask predicate. The engine saves the plan with task still PENDING; test verifies that.

2026-04-20 — test-writer — R010: stubs auditReportStore.load because engine loads the plan+report before checking hasPendingProposalForTask; without it the engine short-circuits to CONTEXT_UNAVAILABLE before the pending check.
  why: If DefaultRevisionEngine checks hasPendingProposalForTask BEFORE loading the report, the stub is superfluous but harmless. If it checks AFTER, this stub is required for the guard to be reached. Either way the test is correct — it isolates the pending-check behavior.

2026-04-20 — test-writer — DefaultProposalDecisionServiceTest: doThrow().when() required for void CourseRepository.save.
  why: Mockito.when(void-call).thenThrow() is a compile error ("not applicable for void"). Use doThrow(ex).when(mock).save(args) instead.

2026-04-20 — test-writer — FileSystemRevisionArtifactStoreTest: buildArtifact helper updated from 4-arg to 6-arg RevisionArtifact constructor (added null decidedAt, null decisionNote).
  why: RevisionArtifact gained decidedAt+decisionNote fields in FEAT-REVAPR architecture patch; old 4-arg call failed to compile. Null values are correct for auto-approved artifacts with no separate decision record.

2026-04-20 — test-writer — GetCmdTest REVAPR tests: @Mock RevisionArtifactStore added; setUp left with 4-arg constructor (matches current production); mocks stubbed via list(), listByPlan(planId), findByProposalId(id, Optional). GetTasksFilter.planId/status carry the --plan/--status proposal filters.
  why: GetCommand interface only exposes get(String, String, GetTasksFilter); no separate proposals filter type. Filter reuse (planId + status) is the only path through the declared contract.

2026-04-20 — developer — findByProposalId: when planId absent, lists plan subdirs sorted, checks for <proposalId>.json in each; returns first hit. When planId present, delegates to load(). hasPendingProposalForTask: scans planDir files, reads each artifact, matches taskId + PENDING_APPROVAL verdict. list(): lists all planDirs, delegates to listByPlan() for each.
  why: All patterns reuse existing loadFromFile() + listByPlan() helpers; no Jackson config changes needed.
