# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-09 — test-writer — J001 journey tests use LemmaRecurrenceAnalyzer pipeline (onQuiz → onCourseComplete) with mocked collaborators, same pattern as LemmaRecurrenceAnalyzerIntegrationTest.
  why: journey is a full pipeline audit; asserting on courseNode.getScores() is the observable contract boundary.

2026-05-09 — test-writer — J004 journey tests use LemmaRecurrenceResult/LemmaStats directly (no analyzer instance needed).
  why: J004 is a user-reading journey over audit output data; the system's observable boundary is the result DTOs. stdDev is informative only (R011) so ExposureStatus remains NORMAL even when stdDev is high.
