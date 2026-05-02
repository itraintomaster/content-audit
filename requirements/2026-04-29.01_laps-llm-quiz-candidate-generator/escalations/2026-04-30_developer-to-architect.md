# Escalation: Developer → Architect (FEAT-LAGEN)
Date: 2026-04-30

## ESCALATION 1
type: interface_change
location: revision-domain.lemmaabsence.LemmaAbsenceMvpStrategy (constructor)
context: The test-writer wrote `LemmaAbsenceMvpStrategyTest` that constructs `LemmaAbsenceMvpStrategy` with
  a single arg (generator only), then asserts `id.providerId()` is non-null and non-blank (F-LAGEN-R009).
  But `providerId` can only be injected if either the constructor accepts it OR the generator interface exposes it.
  The current generated constructor only has `generator`, so `providerId` would always be null.
proposed_change: Either (a) add `providerId: String` to `requiresInject` of `LemmaAbsenceMvpStrategy` in
  sentinel.yaml — this is the cleanest solution; OR (b) expose the providerId through a separate method on
  the generator interface. Option (a) is preferred.
  Note: I implemented a `withProviderId(String)` fluent setter as a temporary workaround that preserves the
  single-arg constructor while allowing Main.java to set the real providerId. But the test won't see this
  value because it constructs LemmaAbsenceMvpStrategy directly without calling withProviderId().

## ESCALATION 2
type: interface_change
location: revision-domain.LemmaAbsenceProposalStrategy.propose
context: The test-writer wrote `strategy.propose(task, context)` with 2 args at line 97 of
  LemmaAbsenceMvpStrategyTest, but the interface declares `propose(RefinementTask, LemmaAbsenceCorrectionContext): LemmaAbsenceQuizCandidate`
  which is a 2-arg method — HOWEVER the tests also call it vs the generated interface which has 3 args
  in DispatchingReviser. Let me re-check...
  Actually: LemmaAbsenceProposalStrategy.propose(task, context) is already 2 args — the 3-arg propose()
  is on the Reviser interface. LemmaAbsenceProposalStrategy is separate. This is NOT an error.
  Re-checking the test: `strategy.propose(task, context)` at line 97 — this is correct for the
  LemmaAbsenceProposalStrategy interface which takes (RefinementTask, LemmaAbsenceCorrectionContext).
  So this is NOT a compilation error from the interface.

## ESCALATION 3
type: missing_field (record accessor)
location: revision-domain.StrategyId
context: The sentinel.yaml declares `StrategyId` as `type: record` but sentinel generated it as a POJO
  with standard JavaBean getters (`getName()`, `getVersion()`, `getProviderId()`). The test-writer wrote
  tests using record-style accessors `id.name()` and `id.providerId()` which do not exist on the generated class.
  This causes compilation errors in LemmaAbsenceMvpStrategyTest.
proposed_change: Regenerate `StrategyId` as a true Java record (or add delegate methods `name()`,
  `version()`, `providerId()` that call the getters). Since it's @Generated, the architect needs to
  update the sentinel.yaml type definition or sentinel itself needs to generate proper records.
  Alternatively, the test-writer needs to be informed to use getters.

## ESCALATION 4
type: missing_field (record accessor)
location: revision-domain.lemmaabsence.LemmaAbsenceGeneratorResponse
context: Same issue — declared as `type: record` but generated as POJO. However, the CannedLemmaAbsenceQuizCandidateGeneratorTest
  uses `getQuizSentence()` and `getTranslation()` (proper getters) so those 4 tests PASS.
  The LemmaAbsenceMvpStrategyTest also creates `new LemmaAbsenceGeneratorResponse(qs, tr)` in a mock
  setup — that compiles fine with the POJO. No issue here.
  Status: NOT a blocker.

## Summary

Blocking issue: **ESCALATION 3** — `StrategyId.name()` and `StrategyId.providerId()` do not exist
because it was generated as a POJO, not a record. The 2 tests in `LemmaAbsenceMvpStrategyTest` fail
with "Unresolved compilation problem" due to these missing accessor methods.

Non-blocking issue: **ESCALATION 1** — `LemmaAbsenceMvpStrategy` constructor needs `providerId`
injected so that the test's assertion `assertNotNull(id.providerId())` passes after the fix to ESCALATION 3.

All other tests (71 out of 73) pass in revision-domain. Production code compiles cleanly for all modules.
