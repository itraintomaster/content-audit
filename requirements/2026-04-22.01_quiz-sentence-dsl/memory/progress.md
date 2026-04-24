# Progress — FEAT-QSENT

2026-04-20 — analyst — refined draft per user feedback. Renumbered rules due to group E rewrite and group B addition; closed 3 doubts; validation OK.
  why: draft had wrong semantics for multi-entry options and missing whitespace/hint formalization.

2026-04-20 — architect — proposed ARCH-QSENT patch + TECH_SPEC.md. Touches course-domain (new quizsentence + quizsentenceengine packages), audit-domain (AuditableQuiz.sentences), audit-application (mapper injection). Both DOUBT-ESCAPE-CHARS and DOUBT-CONVERTER-LOCATION resolved. Ready for @qa-tester.

2026-04-20 — architect — simplified ARCH-QSENT patch + TECH_SPEC.md per user feedback. Removed factory (interface + impl) and both wrapper records; native String / List<String> in signatures; DefaultQuizSentenceConverter public, collaborators package-private. Patch validated (2 additions, 5 modifications, 0 conflicts); tech-spec validated (5 fences, all subset). Ready for @qa-tester.

2026-04-20 — qa-tester — proposed handwrittenTests patch for FEAT-QSENT. 30 tests across DefaultQuizSentenceConverter (27) and CourseToAuditableMapper (3). Journey F-QSENT-J001 (flow) placed in course-domain / quizsentenceengine package. Validated OK (5 modifications, 0 conflicts). Awaiting apply.

2026-04-22 — test-writer — implemented 3 CourseToAuditableMapper tests for FEAT-QSENT (R026 x2, R027 x1). Added QuizSentenceConverter mock via Mockito; stubbed toPlainSentences(FormEntity). Added imports for audit-domain POJOs (AuditableQuiz, AuditableMilestone, AuditableTopic, AuditableKnowledge) and QuizSentenceConverter. Compiles clean in audit-application.

2026-04-22 — test-writer — Implemented all 28 bodies in DefaultQuizSentenceConverterTest. All 28 compile and run; 2 structural tests (R023, R025) pass immediately. 26 fail with UnsupportedOperationException from production stubs — correct TDD state. No Jackson dependency needed; R028 uses 7 manually transcribed real-fixture FormEntity instances covering all observed patterns (simple, pipe, hint, multi-entry).

2026-04-20 — test-writer — Implemented both path bodies for FQsentJ001JourneyTest. path-1 (success): fixture [TEXT "He", CLOZE("is|'s"), TEXT " (to be) great."], exercises full cycle serialize→parse→toPlainSentences, verifies R021/R020/R018. path-2 (failure): CLOZE with null options + CLOZE with empty options, both throw QuizSentenceSerializationException (R013/R024). Both tests pass (2/2 green).
