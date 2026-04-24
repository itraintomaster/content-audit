# Progress — FEAT-RCLAQS

2026-04-22 — analyst — created micro-requirement FEAT-RCLAQS as delta on shipped FEAT-RCLA. Adds `quizSentence` field (FEAT-QSENT DSL) to LEMMA_ABSENCE CorrectionContext, keeps existing `sentence` (plain) untouched, delegates derivation to FEAT-QSENT public converter over the same FormEntity of the original quiz. 5 rules (R001-R005) + 1 flow journey (J001, 3 paths). Unblocks FEAT-LAPS R007 (DOUBT-CANDIDATE-NOTATION already resolved on FEAT-LAPS side; only gap left was the CorrectionContext field itself).
  why: FEAT-QSENT explicitly scoped this out as "micro-update posterior" and FEAT-LAPS declared hard dependency on it; shipping as isolated delta avoids retouching FEAT-RCLA rules/journeys already validated with 48 tests green.

2026-04-23 — developer — implemented FEAT-RCLAQS production code. 3 files changed:
  1. CourseToAuditableMapper: added `quizSentenceByQuiz` cache, calls `serialize(form)` once per quiz in same deriveSentences pass, stamps `quizSentence` on AuditableQuiz.
  2. LemmaAbsenceContextResolver: extracts `quiz.getQuizSentence()` verbatim (no converter re-invocation), passes it as 10th arg to LemmaAbsenceCorrectionContext constructor.
  3. GetCmd.buildLemmaAbsenceContextMap: adds `map.put("quizSentence", ctx.getQuizSentence())` first in the map (before `sentence`).
  Also fixed: CourseToAuditableMapperTest (R027 test updated to stub+verify serialize()), FRclaJ001JourneyTest (AuditableQuiz 6-arg → 7-arg), GetCmdTest (9 RCLA tests: LemmaAbsenceCorrectionContext 9-arg → 10-arg with null), ReviseCmd.java (added NO_ACTIVE_STRATEGY and STRATEGY_FAILED switch cases), ReviseCmdTest/GetCmdTest pre-existing RevisionProposal(,null) syntax reverted.
  Compile: BUILD SUCCESS for audit-domain, audit-application, refiner-domain, audit-cli.
  Tests: audit-domain+audit-application+refiner-domain all pass (77+57+? tests). audit-cli: GetCmdTest 58 pass, FRclaJ001 4 pass, FRclaJ002 1 pass, RCSL/DLABS journeys pass. Pre-existing failures remain in FRevaprJ*/FClirvJ* (LemmaAbsenceProposalStrategyRegistry/RevisionProposal@Generated mismatch) and StatsAnalyzer/DefaultWorkdir env tests — these predate this session.
  next: @test-writer to implement FEAT-RCLAQS tests.

2026-04-22 — test-writer — implemented all 12 handwritten test stubs + 3 journey paths for FEAT-RCLAQS.
  LemmaAbsenceContextResolverTest: 3 new tests (R001/R002/R003). All PASS.
  CourseToAuditableMapperTest: 6 new tests (R002x2/R003/R004x3). All PASS.
  GetCmdTest: 3 new tests (R005x3). All PASS. Also fixed 9 pre-existing 9-arg→10-arg constructor breaks + 14 RevisionProposal(,null) syntax errors (pre-existing) + ReviseCmdTest missing StrategyId arg.
  FRclaqsJ001JourneyTest: created (3 paths). All PASS.
  Total new tests: 15. Pre-existing FRevaprJ003/J004/J005 CLI binary integration failures remain (unrelated, predated this session).
