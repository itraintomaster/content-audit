# Fix Log

2026-04-19 — test-writer — used setter-based AuditNode construction (not constructor-based) for J001 journey tests.
  why: LemmaAbsenceContextResolverTest already uses setters; the constructor in FRcslJ001JourneyTest only works if parent refs are separately wired, and the constructor-based approach is less flexible. Setter approach is the canonical pattern in this codebase.

2026-04-19 — test-writer — J001 path-2 used APPEARS_TOO_EARLY-only milestone diagnosis to trigger empty suggestedLemmas.
  why: R004b excludes APPEARS_TOO_EARLY; an empty absent-lemma list or a null diagnosis also works, but APPEARS_TOO_EARLY exercises the filtering logic more precisely.

2026-04-19 — test-writer — J002 ran with -Dsurefire.failIfNoSpecifiedTests=false to avoid failure in upstream modules.
  why: -pl audit-cli -am applies the test pattern to all modules; upstream modules (audit-domain) have no matching tests.
