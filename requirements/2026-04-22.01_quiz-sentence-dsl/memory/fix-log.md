# Fix Log — FEAT-QSENT

2026-04-22 — test-writer — AuditableQuiz is a generated POJO (not a Java record); accessors are getSentences(), not sentences(). Used getSentences() in all 3 new tests.
  why: Sentinel generates JavaBean-style classes, not Java records, even though sentinel.yaml describes them as type: record.

2026-04-22 — test-writer — QuizSentenceConverter lives in com.learney.contentaudit.coursedomain.quizsentence (public package in course-domain). Import needed explicitly since CourseToAuditableMapperTest only had com.learney.contentaudit.coursedomain.* wildcard (covers entity classes, not the quizsentence subpackage).

2026-04-22 — test-writer — R028 fixture approach: no Jackson in course-domain test scope. Used 7 manually transcribed FormEntity instances from real db/english-course files instead of file-scanning. Covers all real patterns: single-option, pipe-variant, hint in TEXT, multi-entry options. Each fixture exercises R021+R022+R020 in one loop.
