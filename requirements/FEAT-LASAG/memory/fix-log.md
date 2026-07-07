# Fix Log — FEAT-LASAG

2026-06-19 — developer — DefaultLemmaAbsenceAgentGeneratorFactory.buildChatModel() now delegates to LlmFactory.build(LlmConfig); default provider is "claude-cli".
  why: Routes to ClaudeCliChatModel (claude -p, subscription auth) by default; explicit providers still work via LlmFactory dispatch.

2026-06-19 — developer — DefaultLagenConfigResolver.resolve() makes CONTENT_AUDIT_LAGEN_PROVIDER optional; absent/blank defaults to "claude-cli".
  why: Zero-config default execution path; test "resolveRaises...ProviderName..." updated to assert new default behavior instead of required-field throw.

2026-06-19 — developer — Main.java broken imports removed + LLM branch replaced with agent factory.
  why: lageninteractive/ and lagenopenai/ packages in revision-infrastructure are empty (classes deleted); new backend is lemmaabsenceagent/DefaultLemmaAbsenceAgentGeneratorFactory.

2026-06-19 — developer — DefaultLagenConfigResolver.resolve() sets maxEvalRetries=3 by default (F-LASAG-R011).
  why: Test expects getMaxEvalRetries()==3 when no env-var present; added KEY_MAX_EVAL_RETRIES + DEFAULT_MAX_EVAL_RETRIES=3 constant following existing pattern.

2026-06-22 — developer — DefaultAgentRuntimeLauncher.launch() rewritten: generates runId, creates RunPersistence, sets workDir=runDir, enriches inputs with run_dir, persists outcome.
  why: (a) run was invisible to sentinel-agent UI (.sentinel/runs/); (b) workDir=agentDir broke subprocess scripts (eval_length/tally/emit no encontraban candidate.json por ruta relativa).

2026-06-26 — developer — SuggestedLemmaReactTool: hand-written @Tool class replaces DefaultSuggestedLemmaAgentTool as the live tool; LemmaAbsenceAgentGenerator swapped to new SuggestedLemmaReactTool(session).
  why: ReactAgentRunner builds tool specs only from @Tool-annotated methods; DefaultSuggestedLemmaAgentTool had no @Tool so the model received zero tools and emitted raw <function_calls> text.
