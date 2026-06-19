# fix-log
2026-06-19 — qa-tester — Cobertura F-LASAG propuesta (QA-only, NO aplicada).
  9 handwrittenTests cubren R001-R006, R011, R012 sobre el seam Java (DefaultSuggestedLemmaAgentTool, LemmaAbsenceAgentGenerator, DefaultAgentRuntimeErrorClassifier, DefaultLagenConfigResolver).
  ESCALADO a @analyst: R007/R008/R009/R010 sin superficie observable en Java — viven enteras en el grafo declarativo (.sentinel/agents/); launch() encapsula react+evals+retry+carry-forward y devuelve un RunState final. No se forzaron tags.
  Parciales (mitad observable cubierta, mitad graph-internal flagged): R004 (conteo ≤N interno), R012 (distinción de fase / no-retry interno).
  Sin journey-gates: FEAT-LASAG no tiene journeys sincronizados en sentinel.yaml (solo id+code). J001-J009 son flow-based; requieren `sentinel feature sync` del @analyst antes de placement.
  why: el requirement declara el grafo declarativo como frontera no-JUnit; cobertura transitiva no cuenta (regla del proyecto).
