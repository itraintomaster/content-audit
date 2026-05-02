# Fix Log

2026-04-30 — qa-tester — First test patch proposed for FEAT-LAGEN had a critical regression: `revision-domain.lemmaabsence` package would have been silently downgraded from `visibility: public` to `internal` because the proposer normalised the field when my YAML input did not declare it. Re-emitted the patch declaring both packages (`lemmaabsence` public, `lagenopenai` internal) IN FULL with `_change: modify` and every sibling repeated verbatim, plus `visibility:` explicit on both. 14 modifications, 0 conflicts.
  why: when `_change: modify` is implied on a package, the proposer rewrites the whole package node from the input — any field absent from input collapses to the framework default. Always re-emit the full package state when proposing tests at impl level inside it, never trust round-trip preservation.

2026-04-30 — test-writer — Contract reality vs DSL declarations:
  (1) `LagenConfig.temperature` is `double` (primitive), NOT nullable `Double` as DSL declared — generated file uses primitive. Tests for "null when absent" rewritten as "0.0 when absent" per actual sentinel value. (2) `LemmaAbsenceMvpStrategy` constructor is (generator, providerId) — DSL only declared `generator` in requiresInject but developer added `providerId` String. Fix: instantiate with PROVIDER_ID in tests. (3) `LemmaAbsenceLlmGenerator` is package-private; constructor is (ChatLanguageModel, promptBuilder, responseParser, errorClassifier, strategyName) — NOT (LagenConfig, ...) as DSL suggested. Tests mock ChatLanguageModel directly using `anyList()` matcher for the `generate(List<ChatMessage>)` overload.
  why: these are generated-file-vs-DSL discrepancies that do not block the test suite but affect how the tests assert. Logged so future sessions do not re-diagnose.

2026-04-30 — test-writer — LangChain4j ChatLanguageModel has multiple generate() overloads; Mockito's `any()` is ambiguous. Fix: use `anyList()` to target `generate(List<ChatMessage>)` specifically in when/verify. Same fix needed for argThat matchers — use typed lambda `(List<ChatMessage> messages) -> ...`.
  why: ambiguous method reference error at compile time; `anyList()` resolves to the correct overload.

2026-04-30 — developer — Blast radius of strategy→lemmaabsence rename. Fixed 4 FEAT-LAPS journey tests:
  (1) Updated imports from revisiondomain.strategy.* to revisiondomain.lemmaabsence.*
  (2) Updated strategy active name from "lemma-absence-mvp" to "lemma-absence-llm" in all 4 files.
  Added single-arg constructor to LemmaAbsenceMvpStrategy for backward compat with journey tests that
  create it without providerId. Two-arg constructor also present for production use + MvpStrategyTest.
  why: architect's decisions.md explicitly called out these 4 journey tests as the developer's blast radius.

2026-04-30 — developer — 3 LemmaAbsenceLlmGeneratorTest failures are JaCoCo/ByteBuddy vs Java 24 issue.
  Tests that use thenThrow(ConnectException) or thenThrow(SocketTimeoutException) fail because JaCoCo
  can't instrument ByteBuddy-generated Mockito proxies (unsupported class file major version 68).
  Not a logic error in production code. Same root cause as pre-existing failures in other modules.
  why: JaCoCo 0.8.12 does not support Java 24 (class file major version 68). Pre-existing infra issue.
