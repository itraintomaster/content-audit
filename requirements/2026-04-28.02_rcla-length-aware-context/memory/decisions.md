# Decisions

2026-04-28 — analyst — fallback when SentenceLengthDiagnosis is absent: omit numeric fields, set lengthDirection=UNKNOWN, do not fail.
  why: longitude is an enricher; the central LEMMA_ABSENCE info still allows correction.

2026-04-28 — analyst — lengthDirection derived in backend (SHORTEN/LENGTHEN/KEEP_SAME/UNKNOWN), not delegated to LLM.
  why: deterministic prompt; CLI can show a human-readable label.

2026-04-28 — analyst — postponed course-level length trend stats (avg/p50/p90) to a separate feature.
  why: would require new aggregator/diagnosis; out of scope for this iteration.

2026-04-28 — analyst — kept three simple directions (no NEAR_MAX/NEAR_MIN variants).
  why: LLM can infer edge proximity from tokenCount/targetMin/targetMax it already receives.
