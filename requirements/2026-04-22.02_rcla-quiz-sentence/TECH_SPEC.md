---
patch: FEAT-RCLAQS
requirement: 2026-04-22.02_rcla-quiz-sentence
generated: 2026-04-22T00:00:00Z
---

# Tech Spec: Expose quizSentence on LEMMA_ABSENCE CorrectionContext

This spec extends the LEMMA_ABSENCE `CorrectionContext` with a `quizSentence` field (FEAT-QSENT DSL of the original quiz), keeping the existing `sentence` (plain) untouched per R001. The derivation is delegated exclusively to the public `QuizSentenceConverter` from FEAT-QSENT (R002), stamped once on `AuditableQuiz` during course-to-auditable mapping (same single pass that already populates `sentences`), so consistency with the plain `sentence` is a structural consequence (R003) and atomic failure on invalid `FormEntity` reuses the existing FEAT-QSENT/FEAT-RCLA failure mechanism (R004).

## Stamp quizSentence on AuditableQuiz at mapping time

The `AuditableQuiz` carrier is the right place to materialize the DSL: FEAT-QSENT R027 already committed `CourseToAuditableMapper` to invoke `QuizSentenceConverter` exactly once per quiz to derive `sentences`. Adding `quizSentence` as a sibling field captured in the **same** invocation guarantees R003 (same `FormEntity`, same derivation step) by construction, with zero additional coordination cost. A second `serialize(form)` call in that same pass yields the DSL; both outputs travel together on the carrier, so every downstream reader sees a consistent pair without needing to hold the `FormEntity` or re-run the converter.

This choice keeps `refiner-domain` isolated from `course-domain` — the resolver receives fully-materialized data and never touches the converter itself. The converter's existing injection point in `audit-application` (from FEAT-QSENT) stays unchanged; only the mapper's internal logic gains a second output write. Since `QuizSentenceConverter.serialize` throws `QuizSentenceSerializationException` on invalid `sentenceParts`, R004 falls out of the existing FEAT-QSENT invariants: the quiz never makes it into the `AuditReport`, and the downstream LEMMA_ABSENCE task is consequently never produced — the observable effect ("task shown without `correctionContext`") is preserved, just enforced earlier in the pipeline than FEAT-RCLA R005/R006.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditableQuiz
        _change: modify
        fields:
          - name: quizSentence
            type: String
            description: "FEAT-QSENT DSL serialization of the original quiz FormEntity (R001/R002). Stamped eagerly by CourseToAuditableMapper in the same pass that fills `sentences` (FEAT-QSENT R027), guaranteeing quizSentence and sentences[0] are derived from the same FormEntity in the same derivation step (R003). Null only if the course predates FEAT-QSENT — never null for quizzes that pass the mapper."
            _change: add
```

## Add quizSentence to LemmaAbsenceCorrectionContext

The `CorrectionContext` is the contract FEAT-LAPS consumes (per FEAT-LAPS R007); the DSL belongs on the LEMMA_ABSENCE variant and **nowhere else** for now — other diagnosis kinds are out of scope per the REQUIREMENT "Alcance deliberado" and "No se tocan otros DiagnosisKind" sections. The field is a plain `String` copy — `LemmaAbsenceContextResolver` reads `AuditableQuiz.quizSentence` the same way it already reads `sentences[0]` for `sentence`, and passes both through to the record constructor. No injection of `QuizSentenceConverter` into the resolver, no new port on `refiner-domain`, no change to `dependsOn`.

The existing `sentence` plain field is **preserved verbatim** (REQUIREMENT "Limitaciones de alcance" — DOUBT-DEPRECATE-SENTENCE deferred). R003 consistency is free because both fields originate from the same `QuizSentenceConverter` invocation on the same `FormEntity` (already established by FEAT-QSENT R020 and R027). JSON observability (R005) in `audit-cli` `GetCmd.buildLemmaAbsenceContextMap` is a hand-written mapping — the architectural surface already expresses what needs to appear (a new getter on the record); the actual `map.put("quizSentence", ctx.getQuizSentence())` line is implementation-level and not part of the patch.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: LemmaAbsenceCorrectionContext
        _change: modify
        fields:
          - name: quizSentence
            type: String
            description: "FEAT-QSENT DSL of the original quiz (R001). Copied verbatim from AuditableQuiz.quizSentence by LemmaAbsenceContextResolver; never recomputed in this module. Coexists with `sentence` (plain) per R001 — both correspond to the same quiz and the same derivation step (R003). Null when the upstream AuditableQuiz was built before FEAT-QSENT was shipped."
            _change: add
```

## Alternatives considered and rejected

**Inject `QuizSentenceConverter` directly into `LemmaAbsenceContextResolver`.** This would require `refiner-domain` to add `course-domain` to its `dependsOn`, plus propagate the `FormEntity` all the way from the course loader through the audit pipeline down to the resolver (the current `AuditableQuiz` only carries plain sentences, not `FormEntity`). That leak of course-structure types into the audit/refiner chain violates hexagonal layering and contradicts REQUIREMENT Assumption 1 ("same source that feeds `sentence` can feed `quizSentence`"). The mapper-stamp approach uses the already-established carrier seam.

**Add `QuizSentenceConverter` as a new port on `refiner-domain` with an adapter.** Overkill for this delta: the converter is already publicly exposed by `course-domain.quizsentence` and already injected at the composition root — a parallel port would duplicate existing seams (violating P6 "One Seam per Capability") without buying extensibility that the REQUIREMENT explicitly rules out of scope.
