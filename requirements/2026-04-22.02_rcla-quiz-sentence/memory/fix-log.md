# Fix log — FEAT-RCLAQS

(No entries yet.)

2026-04-23 — developer — CourseToAuditableMapperTest R027 fix: added `when(quizSentenceConverter.serialize(formX)).thenReturn(dslX)` stubs and `verify(serialize)` + `assertEquals(dsl, quiz.getQuizSentence())` assertions. The existing `verifyNoMoreInteractions` now covers both serialize() and toPlainSentences() per quiz.
  why: FEAT-RCLAQS R002 extends R027 — the "exactly once per quiz" contract now covers serialize() too.

2026-04-23 — developer — FRclaJ001JourneyTest path4 fix: added missing `null` 7th arg (quizSentence) to AuditableQuiz constructor call at line 365.
  why: AuditableQuiz gained quizSentence field; old 6-arg constructor no longer exists.

2026-04-23 — developer — GetCmdTest: 9 LemmaAbsenceCorrectionContext constructor calls updated from 9-arg (pre-RCLAQS) to 10-arg (+ null quizSentence). Also reverted 14 broken `new RevisionProposal(, null)` calls back to `new RevisionProposal()`.
  why: Generated model added quizSentence field; existing RCLA tests needed migration. RevisionProposal(, null) was invalid syntax left by prior session.

2026-04-23 — developer — ReviseCmd.java: added NO_ACTIVE_STRATEGY and STRATEGY_FAILED cases to switch on RevisionOutcomeKind.
  why: RevisionOutcomeKind enum gained 2 new values; switch expression requires exhaustive coverage.

2026-04-23 — developer — ReviseCmdTest: reverted spurious `, null` from RevisionProposal constructor call (was 13 args, constructor only has 12).
  why: The @Generated ReviseCmdTest had been modified externally; the extra null broke compilation.

2026-04-22 — test-writer — GetCmdTest: 9 existing RCLA constructor calls needed null 10th arg (quizSentence). 14 RevisionProposal(,null) syntax errors fixed to RevisionProposal(). ReviseCmdTest needed null as 13th arg (strategyId). mvn install revision-domain needed to refresh local repo jar.
  why: stale class files masked pre-existing breakage; only surfaced when test-compile was forced.

2026-04-22 — test-writer — Journey path-2 (R004): observable effect at resolver is same as RCLA R005 (quiz absent from AuditReport) because mapper throws before producing the AuditableQuiz.
  why: R004 failure happens upstream at mapping time; resolver just sees a missing quiz node.

2026-04-22 — qa — proposed 12 handwrittenTests + J001 test placement in a single requirement-scoped patch. Distribution: LemmaAbsenceContextResolver (3: R001/R002/R003), CourseToAuditableMapper (6: R002x2/R003/R004x3), GetCmd (3: R005x3). Flow journey F-RCLAQS-J001 pinned to audit-cli/com.learney.contentaudit.journeys (same package as F-RCLA-J001/J002 per TECH_SPEC convention).
  why: R002 is tagged on both the mapper (derivation site, delegation exclusiva) and the resolver (no-recompute contract) since the rule spans two concerns — where the converter is invoked and where the downstream module must not reinvoke it. R003 has one structural test on the mapper (same FormEntity, same pass) plus one on the resolver (same-carrier copy); the journey gate covers the end-to-end invariant. R004 tested on the mapper because TECH_SPEC moves the failure upstream (serialize throws at mapping time → quiz never enters AuditReport), stricter than FEAT-RCLA R005/R006 but preserving the observable "no correctionContext" effect.
