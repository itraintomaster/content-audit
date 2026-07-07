# Decisions — FEAT-LASAG

2026-06-19 — developer — LLM branch now uses DefaultLemmaAbsenceAgentGeneratorFactory (sentinel-agent pattern, takes sessionFactory).
  why: Old DefaultLemmaAbsenceLlmGeneratorFactory (lagenopenai) was deleted; new agent backend replaces it.

2026-06-19 — developer — sessionFactory is always built in LLM branch (not just interactive).
  why: LemmaAbsenceAgentGeneratorFactory.create() requires a SuggestedLemmaQuerySessionFactory parameter; agent backend uses it for tool-calling.
