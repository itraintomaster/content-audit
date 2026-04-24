# Decisions — FEAT-QSENT

2026-04-20 — analyst — DOUBT-OPTIONS-MULTIPLE RESOLVED: all entries + all `|` splits are equivalent variants of the single correct answer. No semantic distinction between multiple entries and pipe-splits. Serialization unifies all variants into one `[a|b|c]` block.

2026-04-20 — analyst — DOUBT-HINT-EXTRACTION RESOLVED: hints are formally extractable by the parser but remain stored inside `text` of TEXT parts (no model change). Preserved in `quizSentence` and `sentenceParts`; removed in plain sentence.

2026-04-20 — analyst — DOUBT-MAPPER-DELEGATION RESOLVED: mapper is an eager delegator — one call per quiz, result stamped into `AuditableQuiz`. No lazy evaluation, no cache.

2026-04-20 — analyst — plain sentence redefined as `List<String>`, one per variant, canonical at index 0. Audit consumes list[0]; multi-variant analysis is future evolution of the audit, out of scope for QSENT.

2026-04-20 — analyst — whitespace rule: canonical serialization emits single spaces between text and markers; parser is tolerant to runs; equivalence is whitespace-normalized. Round-trip invariants weakened to whitespace-normalized equivalence (was byte-exact).

2026-04-20 — analyst — plain sentence declared strictly unidirectional (lossy). Reconstruction of quizSentence/sentenceParts from plain is out of scope.

2026-04-20 — analyst — buildSentence migration fixes TWO bugs: (1) pipe variants leaked as literals; (2) hints left in analyzer input. Both fixes ship together with the migration.

Open doubts remaining: DOUBT-ESCAPE-CHARS (mechanism for reserved chars), DOUBT-CONVERTER-LOCATION (architectural form of the public API).

2026-04-20 — architect — DOUBT-CONVERTER-LOCATION RESOLVED: Option B (domain service) inside a dedicated public package `quizsentence` in course-domain. Engine collaborators (parser, serializer, plainDeriver, whitespaceNormalizer) hidden in a sibling `quizsentenceengine` internal package. Factory Seam (`QuizSentenceConverterFactory` + `DefaultQuizSentenceConverterFactory`) is the only cross-module construction path.
  why: keeps entity records as pure data; canonical Public-Port-Hidden-Adapter + Factory Seam matches existing style (nlp-infrastructure / revision-domain/engine).

2026-04-20 — architect — DOUBT-ESCAPE-CHARS RESOLVED: Option D (prohibition + fail-fast). No escape mechanism. Converter raises QuizSentenceSerializationException / QuizSentenceParseException when `[`, `]` or `____` appear literally in TEXT.
  why: real corpus (db/english-course) has zero literal brackets or four-underscore sequences; escape grammar is complexity without demand; can be retrofitted if a future dataset needs it.

2026-04-20 — architect — AuditableQuiz carrier change: renamed `sentence: String` → `sentences: List<String>` (not just retyped). Audit consumers read `sentences().get(0)` at call sites; no interface signature changes.
  why: name documents the list nature; silent retype would produce subtle bugs where legacy callers still expected a single-sentence string.

2026-04-20 — architect — QSENT design simplified: removed QuizSentenceConverterFactory, DefaultQuizSentenceConverterFactory, record QuizSentence, record PlainSentences. Converter signatures now use String and List<String> directly. DefaultQuizSentenceConverter is visibility: public so the composition root (audit-cli bootstrap) instantiates it directly via new; collaborators stay package-private in quizsentenceengine.
  why: QSENT is a stateless pure function with zero alternative implementations and no costly state — a factory decides nothing (unlike nlp-infrastructure which caches SpaCy state, or revision-domain/engine which selects pluggable strategies). Wrapper records over String and List<String> added ceremony at every call site without carrying new invariants.

2026-04-20 — qa-tester — testModule/testPackage for F-QSENT-J001 set to course-domain / com.learney.contentaudit.coursedomain.quizsentenceengine.
  why: the journey flow ends-to-end needs to construct DefaultQuizSentenceConverter with its package-private collaborators (QuizSentenceSerializer, QuizSentenceParser, PlainSentenceDeriver, WhitespaceNormalizer). Placing the test in quizsentence (public) would lose package-private visibility of the collaborators.

2026-04-20 — qa-tester — R023 (plain-sentence unidireccional) tagged on a reflection-style test "should expose only the three forward conversions". R023 is a contract/shape rule, not a behavior rule; without this test the rule would be uncovered.
  why: user memory rule forbids transitive coverage. The rule has no natural behavioral test; a reflective interface-shape test is the minimal direct tag.

2026-04-20 — qa-tester — R025 (public domain functionality) tagged on a cross-module consumability test, not on ArchUnit. R025 is critical and needs an explicit test per user memory rule (no transitive coverage).

2026-04-20 — architect — post-apply amendment: propuesto patch para declarar testModule: course-domain y testPackage: com.learney.contentaudit.coursedomain.quizsentenceengine en el journey F-QSENT-J001, eliminando el warning de sentinel generate que impedia generar el stub del journey test.
  why: la decision de ubicacion ya estaba tomada por qa-tester (collaborators package-private en quizsentenceengine); solo faltaba reflejarla en sentinel.yaml. Sin estos campos, generate skipea el stub.
