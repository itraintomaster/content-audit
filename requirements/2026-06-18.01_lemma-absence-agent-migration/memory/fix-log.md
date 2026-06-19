# fix-log
2026-06-19 — qa-tester — Cobertura F-LASAG propuesta (QA-only, NO aplicada).
  9 handwrittenTests cubren R001-R006, R011, R012 sobre el seam Java (DefaultSuggestedLemmaAgentTool, LemmaAbsenceAgentGenerator, DefaultAgentRuntimeErrorClassifier, DefaultLagenConfigResolver).
  ESCALADO a @analyst: R007/R008/R009/R010 sin superficie observable en Java — viven enteras en el grafo declarativo (.sentinel/agents/); launch() encapsula react+evals+retry+carry-forward y devuelve un RunState final. No se forzaron tags.
  Parciales (mitad observable cubierta, mitad graph-internal flagged): R004 (conteo ≤N interno), R012 (distinción de fase / no-retry interno).
  Sin journey-gates: FEAT-LASAG no tiene journeys sincronizados en sentinel.yaml (solo id+code). J001-J009 son flow-based; requieren `sentinel feature sync` del @analyst antes de placement.
  why: el requirement declara el grafo declarativo como frontera no-JUnit; cobertura transitiva no cuenta (regla del proyecto).

2026-06-19 — qa-tester — Placement de journey-tests F-LASAG (ronda 2, NO aplicada).
  R007-R010 ahora VALIDATED (eval harness declarativo) → fuera del set JUnit. Mis 9 handwrittenTests siguen cubriendo TODAS las reglas AUTO_VALIDATED (R001-R006, R011, R012). OK.
  Placement (audit-cli / com.learney.contentaudit.journeys, convención sibling LAPS/INCEXP/CSLATDC) SOLO a journeys cuyo gate-set es 100% AUTO_VALIDATED: J002, J003, J007, J009.
  Sin placement (declarativo): J001, J004, J005, J006, J008.
  DOS correcciones al análisis del coordinador (no las forcé, las reporto):
    - J001: el coordinador lo marcó JUnit-testable, pero su nodo `evaluar` gatea [R007] (VALIDATED). Un journey-test debe assertear cada gate de nodo; R007 no es observable en JUnit → J001 NO es JUnit-testable limpio. → declarativo.
    - J007: el coordinador lo marcó No-JUnit ("R012 interno"), pero R012 es AUTO_VALIDATED y el gate-set de J007 es puramente [R012] (mitad observable: error de runtime en evals → ProposalStrategyFailedException categoría preservada). → JUnit-testable, con placement.
  Side-effect del propose: pruned orphan journeys F-LAGAG-J001..J003 y F-LAGEN-J001..J006 (REQUIREMENT.md ya no los lista; apuntaban a lageninteractive/lagenopenai borrados). Resuelve mi side-note de ronda 1.
  Forma de patch: handwrittenTests + features[].journeys[].placement conviven en un patch requirement-scoped; validó 0 conflicts. El guard "handwrittenTests-only" aplica a patches scope sentinel.yaml, no al requirement-scoped con placement legítimo.

2026-06-19 — qa-tester — Placement de journey-tests F-LASAG (ronda 3, NO aplicada).
  APRENDIZAJE: el generador EXIGE testModule/testPackage en TODO journey con flow. No existe "declarativo sin placement". Un journey-test asserta el OUTCOME del path, no cada gate; un gate VALIDATED (R007-R010) es trazabilidad, no algo que el journey-test asserta — válido atravesarlo.
  Reevalué los 5 sin placement por OUTCOME observable/alcanzable en el seam generate():
    - J001 success (candidato emitido, R003): observable → PLACED.
    - J004 success (candidato emitido tras retry K): outcome alcanzable, mecánica interna trazada a VALIDATED → PLACED.
    - J005 success (best-effort emite mejor candidato): outcome alcanzable → PLACED.
    - J006 success (deseable-negativo no bloquea, emite): outcome alcanzable → PLACED.
    - J007 failure (error runtime evals → exception categoría preservada): observable y distinto → PLACED.
    - J008 failure (length-block, candidato bloqueado): NO PLACED → ESCALADO a @analyst.
      why: el único outcome de J008 es result:failure por length-block, pero R009 (best-effort) garantiza que un veredicto de eval NUNCA lanza ProposalStrategyFailedException — emite el mejor candidato. En el seam generate() ese "failure" es inalcanzable; un journey-test solo podría assertear "se emitió un candidato" (cobertura falsa) o un failure que no ocurre. → pedí degradar J008 a escenario en prosa (sacarlo de journeys:).
  Patch: 9 handwrittenTests + 8 placements (J001-J007 sin J008, + J009). Validó 0 conflicts. NO aplicado.
  NOTA: el patch con 8 placements NO desbloquea `sentinel generate` hasta que @analyst quite J008 de journeys: (sigue siendo flow sin placement). Estado intermedio correcto.
