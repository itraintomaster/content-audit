---
patch: ARCH-QSENT
requirement: 2026-04-22.01_quiz-sentence-dsl
generated: 2026-04-20T19:00:00Z
---

# Tech Spec: Formalize quizSentence as a first-class concept of course-domain

## Host the DSL contract in a dedicated public package of course-domain
The converter is a pure stateless transformation over `FormEntity` (records with no behavior). Placing conversion methods on `FormEntity` / `SentencePartEntity` would pollute the data model with parsing logic that has nothing to do with course structure, and would force every record to depend on an exception type it has no business throwing. A dedicated domain service inside a new `quizsentence` public package keeps the entities as pure data and matches the existing style of the codebase (services live as interfaces + impls, entities are plain records). This resolves `DOUBT-CONVERTER-LOCATION` as Option B: domain service in a dedicated public package, implementation hidden in a sibling internal package.

```architecture
modules:
  - name: course-domain
    _change: modify
    packages:
      - name: quizsentence
        _change: add
        visibility: public
        interfaces:
          - name: QuizSentenceConverter
            _change: add
            stereotype: service
            visibility: public
            exposes:
              - signature: "serialize(FormEntity form): String"
                throws: [QuizSentenceSerializationException]
              - signature: "parse(String quizSentence): FormEntity"
                throws: [QuizSentenceParseException]
              - signature: "toPlainSentences(FormEntity form): List<String>"
                throws: [QuizSentenceSerializationException]
```

## Use String and List<String> directly in the signatures — no wrapper records
An earlier iteration introduced `QuizSentence(String value)` and `PlainSentences(List<String> variants)` records. Both were removed: they wrapped a single field without carrying any invariant the raw type does not already carry. `QuizSentence` added one read-only wrapper around a `String`, forcing consumers through `.value()` for zero type-safety gain; `PlainSentences` did the same over `List<String>`. The invariants that matter (whitespace-canonical DSL, canonical at index 0, non-empty list, hints stripped) are documented as interface-contract preconditions / postconditions enforced by `QuizSentenceConverter` and its exceptions. Cheap ceremony is not cheap — it shows up at every call site. The fence above carries only the three methods with their native types.

## Surface two domain exceptions for the two failure modes
R013, R016 and R024 demand explicit, atomic failure in two distinct flows: input-data violations when going from `FormEntity` to DSL (TEXT with options, CLOZE without options), and grammar violations when going from DSL back to `FormEntity`. Splitting them gives callers a clean way to react (a malformed stored quiz is a data-repair problem; a malformed user-supplied DSL is a validation problem) and keeps error messages targeted. Both extend `RuntimeException` consistent with `CourseValidationException` elsewhere in course-domain.

```architecture
modules:
  - name: course-domain
    _change: modify
    packages:
      - name: quizsentence
        _change: add
        models:
          - name: QuizSentenceSerializationException
            _change: add
            type: exception
            extends: RuntimeException
            message: "No es posible serializar a quizSentence: %s (posicion %s)"
            fields:
              - { name: reason, type: String, description: "Human-readable cause (R013, R024)." }
              - { name: position, type: String, description: "Index of the offending sentencePart or 'n/a'." }
          - name: QuizSentenceParseException
            _change: add
            type: exception
            extends: RuntimeException
            message: "No es posible parsear el quizSentence: %s (posicion %d)"
            fields:
              - { name: reason, type: String, description: "Human-readable cause (R016, R024)." }
              - { name: position, type: int, description: "Character offset in the input where the grammar violation was detected." }
```

## Resolve DOUBT-ESCAPE-CHARS: prohibit reserved chars, validate at convert-time
The real corpus (`db/english-course/**/quizzes.json`) contains no `[`, `]` or `____` inside any TEXT — these characters simply don't occur in educational English sentences. Introducing an escape mechanism (backslash, doubling, entities) adds grammar complexity and a new class of bugs (escape of escape, unbalanced escapes) for zero current demand. We adopt Option D: the converter prohibits literal occurrences of `[`, `]`, and the four-underscore sequence in TEXT content and raises `QuizSentenceSerializationException` / `QuizSentenceParseException` when detected, as R009 / R024 require. If a future corpus ever needs literal brackets, an escape mechanism can be added without breaking existing data — the prohibition is the conservative starting point. No additional schema is needed in the patch; the behavior is embodied by the exception types above and validated by the converter.

## Hide the engine in an internal package with a single public seam — no factory
`QuizSentenceConverter` has a graph of collaborators (serializer, parser, plain-sentence deriver, whitespace normalizer). Exposing those as public classes would leak implementation details and let other modules bypass the port. We place all collaborators inside an `internal` `quizsentenceengine` package with default (package-private) visibility. The only public class in the engine is `DefaultQuizSentenceConverter` itself — the composition root (audit-cli bootstrap) wires `new DefaultQuizSentenceConverter(new QuizSentenceSerializer(), new QuizSentenceParser(), new PlainSentenceDeriver(...))` once and injects the resulting `QuizSentenceConverter` into `CourseToAuditableMapper`. No factory interface is introduced: QSENT is a stateless pure function with zero alternative implementations and no costly state to cache (unlike `nlp-infrastructure`, which caches a SpaCy model, or `revision-domain/engine`, which selects among pluggable revisers). A factory would decide nothing. Satisfies P2 (package as encapsulation unit), P3 (versatility only where demand exists — there is none here), P4 (composition root is the only caller that sees the concrete type) and P5 (contract in `quizsentence`, engine in `quizsentenceengine`).

```architecture
modules:
  - name: course-domain
    _change: modify
    packages:
      - name: quizsentenceengine
        _change: add
        visibility: internal
        implementations:
          - name: DefaultQuizSentenceConverter
            _change: add
            visibility: public
            implements: [QuizSentenceConverter]
            requiresInject:
              - { name: serializer, type: QuizSentenceSerializer }
              - { name: parser, type: QuizSentenceParser }
              - { name: plainDeriver, type: PlainSentenceDeriver }
          - name: QuizSentenceSerializer
            _change: add
          - name: QuizSentenceParser
            _change: add
          - name: PlainSentenceDeriver
            _change: add
          - name: WhitespaceNormalizer
            _change: add
```

## Change AuditableQuiz.sentence (String) to AuditableQuiz.sentences (List<String>)
R017 redefines the plain sentence as an ordered list of variants, canonical at index 0 (R018). The audit-domain carrier must carry the new shape. We rename the field from `sentence` to `sentences` so the list nature is visible at the call site (`quiz.sentences().get(0)` reads as "the canonical variant"); keeping the name `sentence` with a `List<String>` type would be a silent breakage for anyone reading code. The current audit behavior is preserved: all analyzers and the `SentenceLengthCorrectionContext` resolver keep their `String sentence` field and will read `quiz.sentences().get(0)` at call sites (an implementation-level change, not an architectural one — no interface signatures change). Multi-variant analysis is deliberately deferred (see `Limitaciones de alcance` and assumption 6 in REQUIREMENT.md).

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditableQuiz
        _change: modify
        fields:
          - { name: sentence, type: String, _change: delete }
          - { name: sentences, type: "List<String>", description: "Ordered list of plain sentences derived from the quiz sentenceParts (FEAT-QSENT R017). Index 0 is the canonical variant (R018), which is the single source consumed by current analyzers. Extra variants are materialized eagerly for future multi-variant analysis.", _change: add }
```

## Wire the converter into CourseToAuditableMapper as an eager delegator
R027 mandates the mapper becomes a one-line eager delegator: call the public converter once per quiz, stamp the result in `AuditableQuiz`. We inject `QuizSentenceConverter` (the interface — the concrete `DefaultQuizSentenceConverter` is constructed once in the composition root and passed through). This satisfies R025 (conversion is public `course-domain` functionality) and R027 (no re-implementation, no lazy evaluation). `audit-application` already `dependsOn: [course-domain]`, so no module-boundary change is needed. The legacy private `buildSentence` disappears with the migration (R026: both bugs — pipe variants leaked as literals, hints left in analyzer input — are fixed inside the new converter, not in the old method).

```architecture
modules:
  - name: audit-application
    _change: modify
    implementations:
      - name: CourseToAuditableMapper
        _change: modify
        implements: [CourseMapper]
        types: [Component]
        requiresInject:
          - { name: nlpTokenizer, type: NlpTokenizer }
          - { name: quizSentenceConverter, type: QuizSentenceConverter }
```

## Migration order and downstream impact on existing analyzers
The patch is atomic at the architectural level, but the implementation rollout has an order: (1) `course-domain` gains the new packages and types; (2) the engine implementations are written and a single `DefaultQuizSentenceConverter` instance is wired in the composition root (audit-cli bootstrap); (3) `AuditableQuiz.sentences` replaces `sentence`; (4) every consumer of `quiz.sentence()` (the four `ContentAnalyzer` implementations and `SentenceLengthContextResolver`) updates its call site to `quiz.sentences().get(0)`; (5) `CourseToAuditableMapper.buildSentence` is deleted. Steps 3 through 5 are coupled: they compile together. The `SentenceLengthCorrectionContext.sentence` field in refiner-domain keeps its `String` shape — the resolver reads index 0 of the new list and stores it. No interface signature in audit-domain or refiner-domain changes, so ArchUnit rules stay green.
