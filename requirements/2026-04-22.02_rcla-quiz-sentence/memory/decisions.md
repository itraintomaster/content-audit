# Decisions — FEAT-RCLAQS

2026-04-22 — analyst — scope locked to CorrectionContext field addition only. No changes to FEAT-RCLA rules/journeys, no changes to FEAT-LAPS, no changes to FEAT-QSENT, no changes to course-domain model. Explicitly isolated delta.
  why: RCLA is shipped (48 tests green); QSENT is shipped (~30 tests green); LAPS is spec-complete but blocked on this. Touching any of them would expand blast radius for a one-field addition.

2026-04-22 — analyst — `sentence` (plain) is preserved, NOT deprecated in this iteration. Both `sentence` and `quizSentence` coexist.
  why: deprecation requires consumer migration; no consumer audit has been done. Recommended DOUBT-DEPRECATE-SENTENCE Opcion C (defer) until FEAT-LAPS ships and real usage is observable.

2026-04-22 — analyst — consistency invariant R003 framed as "both fields derived from the same FormEntity in the same derivation step", leveraging FEAT-QSENT R020 (equivalence of plain-sentence between routes).
  why: avoids imposing a new verification mechanism; the invariant falls out of FEAT-QSENT naturally if architecture wires derivation through a single pass over the same FormEntity.

2026-04-22 — analyst — failure mode (R004) reuses FEAT-RCLA R005/R006 existing mechanism (context not built, correctionContextError surfaced). No new failure vocabulary introduced.
  why: keeps the CorrectionContext contract consistent: either the context is fully built or it is not built at all; partial contexts are not admitted.

2026-04-22 — analyst — observability scope (R005) limited to JSON output. Text format (FEAT-RCLA R009) is not extended in this requirement.
  why: FEAT-LAPS consumes the context programmatically, not via text format; forcing a text-format decision here would overreach into presentation decisions that belong to architecture/product.

Open doubts:
- DOUBT-DEPRECATE-SENTENCE (OPEN): whether to deprecate the plain `sentence` once `quizSentence` is available. Recommendation: defer.

2026-04-22 — architect — chose mapper-stamp approach: `AuditableQuiz` gains a `quizSentence` String field, populated by `CourseToAuditableMapper` via the already-injected `QuizSentenceConverter` in the same pass that fills `sentences` (FEAT-QSENT R027). `LemmaAbsenceCorrectionContext` gains a `quizSentence` field that the resolver copies verbatim from the carrier.
  why: avoids leaking course-domain types into refiner-domain (P5 Contract/Carrier/Engine — audit-domain is the carrier). R003 consistency becomes structural (same FormEntity, same serialize() call). R004 failure is pre-empted by FEAT-QSENT invariants at mapping time — stricter than RCLA R005/R006 but preserves the observable effect (no correctionContext for the task).

2026-04-22 — architect — REJECTED: inject QuizSentenceConverter into LemmaAbsenceContextResolver. Would require adding course-domain to refiner-domain.dependsOn AND threading FormEntity through the audit pipeline (AuditableQuiz doesn't carry it). Violates hexagonal layering and contradicts REQUIREMENT Assumption 1.
  why: documenting the rejected alternative so future architects don't re-open it.

2026-04-22 — architect — GetCmd JSON output (R005) not modeled in the patch. `GetCmd.buildLemmaAbsenceContextMap` is a hand-written mapping method (not Sentinel-generated); the architectural surface (a new getter on the record) is sufficient. The `map.put("quizSentence", ...)` line is implementation-level.
  why: avoid over-specifying; no sentinel element changes for GetCmd. QA/dev will add the map.put call when consuming the new field.

2026-04-22 — architect — FEAT-LAPS consumption update (DefaultLemmaAbsenceProposalDeriver / LemmaAbsenceMvpStrategy reading ctx.quizSentence) is OUT OF SCOPE for this feature per REQUIREMENT "No se toca FEAT-LAPS". Flagged as pending for a follow-up if/when LAPS wants to switch its input source from `sentence` to `quizSentence`.
  why: REQUIREMENT explicitly bounds the change to exposing the field; LAPS consumes it via contract when it decides to.
