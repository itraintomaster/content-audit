# fix-log

2026-07-22 — developer — Formato elegido para el input outOfCatalogWords: "<lemma> (frequency rank N)" / "<lemma> (no known frequency rank — likely a typo or non-word)".
  why: R013 deja el formato libre; elegido para que el test (assert contains "5432", ausencia de "null" crudo) pase naturalmente y para que el prompt de generate.md pueda mostrar la señal de rank sin parsear nada del lado del agente declarativo.

2026-07-22 — test-writer — R013 tests no fijan el formato exacto del insumo OOC (regla lo deja libre).
  R013 dice explícitamente "la forma exacta del formato ... es decisión de implementación". Aserciones sobre lo esencial: presencia de ambos lemas + rank numérico conocido (5432) + ausencia de "null" crudo (positivo); un valor exactamente "none" en el mapa de inputs (vacío). NO se asumió nombre de key ni separador.
  why: evita atar el test a un formato que @developer todavía no escribió (TDD); ver guía del skill sentinel-test sobre reglas que dejan libertad de formato.

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

2026-06-19 — test-writer — DefaultSuggestedLemmaAgentToolTest: fake de session via Mockito, assertSame del result + verify delegacion unica.
  why: SuggestedLemmaQuerySession no es sealed; Mockito mock + when/verify es el patron mas limpio para este contrato de 1 metodo sin estado.

2026-06-19 — test-writer — DefaultAgentRuntimeErrorClassifier: throwable → categoría mapping elegido para R005/R012.
  R005 (RunState): RunState.empty().withRunStatus(HALTED).withTerminationReason("TIMEOUT") → LLM_TIMEOUT.
    why: terminationReason es la única señal observable en RunState; "TIMEOUT" = HaltReason.TIMEOUT.name().
  R012 (Throwable): TimeoutException→LLM_TIMEOUT, SecurityException (msg 401)→LLM_AUTH_FAILED, ConnectException→LLM_UNREACHABLE, RuntimeException→LLM_OTHER.
    why: LLM_TRANSPORT no existe en el enum; LLM_UNREACHABLE es el nombre real del enum para errores de transporte/conexión.

2026-06-19 — developer — DefaultAgentRuntimeErrorClassifier: SecurityException requiere match de AUTH_PATTERN en el mensaje para clasificar como LLM_AUTH_FAILED (no solo el tipo).
  why: el test pasa SecurityException("sentinel-agent: 401 Unauthorized – invalid API key"); sin el regex check se caería a LLM_OTHER.

2026-06-19 — developer — LemmaAbsenceAgentGenerator: se necesitó añadir import de RunState al file @Generated para que el body compilara.
  why: el stub generado no tenía el import; Sentinel smart-merge preserva el cuerpo, los imports son de la zona de merge también.

2026-06-19 — test-writer — LemmaAbsenceAgentGeneratorTest: 5 stubs implementados (R002-R006).
  Estrategia: Mockito para los interfaces package-private (no hay clases internas anónimas que necesiten el tipo ChatModel). null pasado como ChatModel al constructor (el launcher fake no lo usa). Fakes capturan tools map (R002) via thenAnswer. RunState.empty().withRunStatus(COMPLETED) como estado exitoso.
  PROBLEMA PREEXISTENTE DEL MÓDULO: pom.xml declara langchain4j-core:0.36.2 explícitamente pero el código generado (AgentRuntimeLauncher.java, LemmaAbsenceAgentGenerator.java) usa dev.langchain4j.model.chat.ChatModel que solo existe en langchain4j-core:1.13.0 (transitivo de sentinel-agents, excluido por nearest-wins). El módulo NO compila en clean. Verificado que MI test compila correctamente con javac + classpath que incluye langchain4j-core:1.13.0. @architect debe corregir el pom (excluir la dep directa vieja o actualizar la versión).
  why: clean compile falla en producción, no en el test; la compilación con classpath correcto (1.13.0) pasa sin errores.

2026-06-19 — qa-tester — Re-home de placement journey-tests F-LASAG (ronda 4, NO aplicada).
  Estado previo aplicado: J001-J007+J009 en audit-cli; J008 degradado a prosa por @analyst (ya no en el gate).
  PROBLEMA: los journey-tests del agente (J001-J007) no se pueden testear honestamente en audit-cli — el factory público hardcodea el launcher real y no hay LLM en tests. AgentRuntimeLauncher (internal) y el ctor package-private de LemmaAbsenceAgentGenerator solo son accesibles dentro de revision-infrastructure.lemmaabsenceagent (donde ya viven los handwrittenTests que mockean el launcher).
  RE-HOME: J001-J007 → testModule=revision-infrastructure, testPackage=com.learney.contentaudit.revisioninfrastructure.lemmaabsenceagent (launcher mockeable, outcomes conducibles: éxito/budget/falla-runtime/retry). J009 SE QUEDA en audit-cli/journeys (su superficie es DefaultLagenConfigResolver default-3 + modo CANNED).
  Patrón: "testear el journey donde vive su superficie observable".
  Sentinel aceptó el re-home sin restricción (0 conflicts). handwrittenTests intactos. NO aplicado (lo aplica el usuario + sentinel generate, que reubica los stubs *JourneyTest).

2026-07-22 — qa-tester — handwrittenTests R013+R015 propuestos sobre LemmaAbsenceAgentGenerator (paquete lemmaabsenceagent). NO aplicado.
  R013 (2 tests): OOC formateado con lema + indicación de rango conocido en los insumos de generación; y marcador "none" cuando outOfCatalogWords viene vacío. Superficie observable = mismos insumos/tool que captura el test de R002 vía el launcher fake.
  R015 (1 test): misplaced=[] + OOC no vacío emite exactamente 1 QuizCandidate sin excepción ni cortocircuito.
  Placement: la impl vive en el scope de paquete 'lemmaabsenceagent' — el patch DEBE anidar packages[].implementations[] (module-root fue rechazado por el validador).
  LIMITACIÓN del guard `--as=qa-tester`: rechaza CUALQUIER patch con clave packages[] como "structural", aunque la hoja sea handwrittenTests-only. Para impls package-scoped el wrapper packages[] es obligatorio → el self-apply con guard es inviable. Aplicar sin --as (usuario, per AGENTS.md Rule E) o esperar fix del guard para reconocer handwrittenTests bajo packages.
  why: el guard no distingue "packages como wrapper de direccionamiento" de "packages con cambios estructurales reales".
