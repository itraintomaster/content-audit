# progress

2026-07-23 — developer — Eval #7 (no-regresión léxica, criterio #7 de R007) implementado en el grafo declarativo. NO Java tocado — usa el comando `lexis` de FEAT-CLEX ya existente y verificado.
  Archivos nuevos: `.sentinel/agents/lemma-absence-agent/nodes/eval_lexical.md` (kind script), `.sentinel/agents/lemma-absence-agent/scripts/eval_lexical.sh`.
  Archivos tocados: `agent.yaml` (nodo registrado + insertado en la cadena entre eval_global_distinct y eval_quality + comentarios de flujo/contrato/dependencies actualizados), `nodes/eval_global_distinct.md` (edge retargeteado a eval_lexical), `nodes/tally.md` + `scripts/tally.sh` (combina `data.lexical_ok` en `blocking`, agrega reason al log de fallo y bloque de carry-forward nombrando la palabra infractora + su nivel).
  Mecánica: compara `flags(inputs.quizSentence)` (original) vs `flags(candidate.json.quizSentence)` (candidato, ya canónico post-gate_candidate) vía `content-audit lexis --quiz-sentence ... --level ...` (FEAT-CLEX, mismo JSON `{misplacedWords,outOfCatalogWords}`). Bloquea solo si `candidato ⊄ original` (lema NUEVO); residuo preexistente no bloquea.
  Robustez (pedido explícito del task): fallo de `lexis` (rc!=0/salida no parseable) NO soft-pasa (a diferencia de eval_length) — bloquea con motivo técnico, dispara retry de generación por el mecanismo existente (R008), nunca falla el consumidor (R009 intacto). cefrLevel ausente/inválido SÍ soft-pasa (sin señal, no fallo de herramienta).
  Verificado a mano (sin LLM, casos reales via audit-cli.sh lexis + invocación directa del script con stdin fabricado): remitted→transferred BLOQUEA nombrando "transfer es B1, el quiz es A1"; remitted→sent PASA; candidato==original PASA trivial; simulación completa de tally.sh confirma blocking 7/8 + carry_forward.md con el mensaje correcto.
  HALLAZGO reportado (no corregido, fuera de alcance): las claves internas `eval7_distinct`/`eval8_global_distinct` del grafo son numeración LEGACY anterior al catálogo actual de R007 (predatan la inserción de #6 y de este #7 real) — no corresponden a ninguno de los 7 criterios oficiales de R007, son gates de anti-duplicación aparte. Documentado con comentarios en agent.yaml/tally.sh; NO renombrado (fuera del alcance de esta tarea, riesgo de blast radius innecesario).

2026-07-22 — analyst — Agregado criterio #7 (no-regresión léxica) al catálogo de evals R007 + DOUBT-EVAL-LEXICO-CLI. NO regla nueva, NO journey nuevo, NO handwrittenTest nuevo (harness-validated como #3/#6). Estado: REQUIREMENT.md + glosario editados y validados ([OK]/PASSED). Pendiente equipo: resolver DOUBT-EVAL-LEXICO-CLI (¿la consulta léxica del análisis merece regla propia en la feature de `content-audit tokens`?). Si Opción A, el arquitecto define el comando y su contrato observable; ver decisions.md.

2026-07-22 — developer — R013/R014/R015 implementadas. 24/24 verde (los 21 previos + 3 nuevos).
  LemmaAbsenceAgentGenerator.buildInputs: nuevo input `outOfCatalogWords` (join ", " de "<lemma> (frequency rank N)" / "<lemma> (no known frequency rank — likely a typo or non-word)"; "none" si vacío/null), análogo a misplacedLemmas. Javadoc de clase actualizado (2 bloques: contrato de inputs + full input contract de buildInputs).
  generate.md (nodo declarativo): (a) outOfCatalogWords agregado a dependencies; (b) expuesto en el user prompt template junto a misplacedLemmas con guía inline (typo→ortografía si es del nivel; genuina/rara→equivalente del nivel); (c) regla dura #2 (edición mínima) y sus 2 ecos (sección "Qué cambiar" + self-check) reescritos: palabras objetivo = UNIÓN misplacedLemmas ∪ outOfCatalogWords, con la guía de 2 casos OOC y las mismas restricciones de colocación/objeto.
  Revisado curate_lemmas.md y evals/gates: NINGUNO además de generate.md referencia misplacedLemmas de forma que rompa OOC-only, pero curate_lemmas.md (paso 1, clasificación de qué vocabulario pide el ejercicio) mira knowledge/instructions + quizSentence + misplacedLemmas — NO mira outOfCatalogWords. Para una tarea OOC-only (misplaced=[]) la curación pierde la misma señal que generate tenía antes del fix (ve "misplaced: none" y no sabe qué POS/regex pedir a la tool salvo por knowledge/instructions genéricos). NO se tocó (fuera del alcance R013/R014/R015 tal como pedido) — reportado al usuario/orquestador como hallazgo a decidir (¿nueva regla o ampliar R014 a curate_lemmas?).
  Verificación: JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn test -pl revision-infrastructure -Dexec.skip=true -DskipPitest=true -Djacoco.skip=true → BUILD SUCCESS, Tests run: 24, Failures: 0, Errors: 0.

2026-07-22 — test-writer — 3 stubs R013(x2)+R015 implementados en LemmaAbsenceAgentGeneratorTest.
  Captura inputs de launch() vía arg index 1 (Map<String,String>), análogo a como R002 capturaba arg index 3 (tools). Helper nuevo contextWith(misplaced, outOfCatalogWords) reusando el shape de validContext().
  R013-positivo (adore rank 5432, sipps rank null; misplaced no vacío para no confundir el "none"): combinedInputs debe contener "adore"+"5432"+"sipps" y NO "null" crudo. FALLA hoy (esperado): el item OOC no se transmite todavía.
  R013-vacío (misplaced no vacío, OOC vacío): algún valor del inputs map debe ser literalmente "none". FALLA hoy (esperado).
  R015 (misplaced=[] + OOC no vacío): generate() debe emitir 1 QuizCandidate sin excepción. PASA (el flujo actual no cortocircuita con misplaced vacío).
  Tests run: 8, Failures: 2 (ambos R013, esperados), Errors: 0.

2026-06-19 — test-writer — FLasagJ001JourneyTest y FLasagJ002JourneyTest implementados y VERDE.
  Patrón: idéntico a LemmaAbsenceAgentGeneratorTest (Mockito mocks de AgentRuntimeLauncher + AgentCandidateParser + AgentRuntimeErrorClassifier + SuggestedLemmaQuerySessionFactory; null para ChatModel). J001: mock launcher devuelve COMPLETED, parser devuelve candidato → assertNotNull result/quizSentence/translation. J002: budget N=1, launcher devuelve COMPLETED → assertNotNull sin excepción. Tests run: 2, Failures: 0, Errors: 0.

2026-06-19 — architect — Patch F-LASAG propuesto (NO aplicado) y validado.
  Salida: "Validated: 2 additions, 4 modifications, 4 deletions, 0 conflicts."
  Proposal: requirements/2026-06-18.01_lemma-absence-agent-migration/architectural_patch.yaml
  Baseline congelado: .../sentinel-baseline.yaml
  Reemplaza lagenopenai+lageninteractive por paquete interno lemmaabsenceagent; agrega dep com.sentinel:sentinel-agents:0.0.1-SNAPSHOT; retira LagenMode.LLM_INTERACTIVE; agrega maxEvalRetries a LagenConfig.
  Siguiente: escribir TECH_SPEC.md (español). Pendiente externo: @qa-tester para handwrittenTests; el grafo declarativo del agente (.sentinel/agents/) lo construye sentinel-agent fuera de sentinel.yaml.

2026-06-19 — analyst — Requirement-domain: reformulación de validación + retiro de journeys absorbidos + sync F-LASAG.
  R007-R010 → Validation VALIDATED (+nota "validada por harness de evals, no por suite JUnit"): el DSL ValidationStatus solo tiene VALIDATED|AUTO_VALIDATED|ASSUMPTION; no hay valor para "validado por evals del agente". AUTO_VALIDATED afirmaría cobertura JUnit (falsa aquí); VALIDATED = confirmado por humano/harness, el más fiel. Blockquotes describen conducta de calidad/reintento observada por el consumidor (lenguaje funcional); la superficie de validación (harness de evals) solo se nombra en los Detail.
  Mitades precisadas: R004 (conteo ≤N interno=graph, "agotar no es falla"=AUTO_VALIDATED), R011 (default 3=AUTO_VALIDATED, "CANNED ignora maxEvalRetries"=interno), R012 (categoría+falla inmediata=AUTO_VALIDATED, distinción de fase/no-retry=interno). Las 3 quedan AUTO_VALIDATED (su mitad observable la cubren los 9 handwrittenTests; NO tocados).
  Journeys F-LAGEN-J001..J006 y F-LAGAG-J001..J003 retirados de sus REQUIREMENT.md (reemplazados por nota de absorción a F-LASAG). `sentinel feature sync` es ADITIVO: NO podó los 9 refs colgantes a lagenopenai/lageninteractive; dice "will be pruned at next generate". Poda = `sentinel generate` (fuera de lane analyst). ESCALADO a @architect.
  F-LASAG: el bloque combinado de 9 journeys en un solo fence yaml leía 0 journeys en el parser. Reescrito a formato estándar del proyecto (1 `### Journey[...]` + 1 fence single-entry por journey). Sync registró los 9 (AUTO_VALIDATED, sin testModule/testPackage → placement = qa-tester).
  Bug latente corregido: J004 tenía un ciclo (evaluar→reintentar→evaluar); V-MBT-08 prohíbe ciclos. Desenrollado a evaluar_primer_intento→reintentar→evaluar_reintento (DAG). Nunca se detectó antes porque el parser jamás leyó el bloque combinado.

2026-06-19 — architect — Patch de limpieza de orphans propuesto (NO aplicado).
  Salida: "Validated: 0 additions, 1 modifications, 0 deletions, 0 conflicts."
  architectural_patch.yaml ahora contiene SOLO la limpieza (reemplazo de la lista patterns + fix descripción lagen). Los patches de diseño previos ya están en .applied-patches/.
  TECH_SPEC.md (220 líneas / 9 fences) se DEJÓ intacto como foto del diseño F-LASAG: el CLI rechazó encogerlo a 2 fences (guard anti-shrink) y es correcto no degradar la foto del diseño por un cleanup menor. La razón del cleanup vive en la description del patch y en decisions.md.

2026-06-19 — test-writer — Implementado stub R011 en DefaultLagenConfigResolverTest.
  Cuerpo: env = fullEnv() (no incluye env-var de eval-retries); resolver.resolve(env); assertEquals(3, config.getMaxEvalRetries()).
  NO se referencia KEY_MAX_EVAL_RETRIES (aún no existe en DefaultLagenConfigResolver): la ausencia del env-var en fullEnv() es suficiente para el escenario de la regla.
  Estado de compilación: audit-cli NO compila hoy — revision-infrastructure falla por langchain4j ChatModel ausente del pom (pendiente @developer); LagenConfig.jar no tiene getMaxEvalRetries() todavía. Test correcto contra el spec; verificación final ocurre post @developer.

2026-06-19 — test-writer — Cuerpo de DefaultSuggestedLemmaAgentToolTest implementado.
  Fake: Mockito mock de SuggestedLemmaQuerySession; verifica que la tool delega en session.requestMore(criteria) exactamente una vez y devuelve su resultado (assertSame).
  Build: falla preexistente ChatModel no encontrado en langchain4j-core:0.36.2 en AgentRuntimeLauncher/LemmaAbsenceAgentGenerator; independiente del test.

2026-06-19 — test-writer — Cuerpos R005+R012 en DefaultAgentRuntimeErrorClassifierTest implementados.
  R005: classify(RunState.empty().withRunStatus(HALTED).withTerminationReason("TIMEOUT")) == LLM_TIMEOUT.
  R012: classify(Throwable) con 4 throwables (TimeoutException→LLM_TIMEOUT, SecurityException 401→LLM_AUTH_FAILED, ConnectException→LLM_UNREACHABLE, RuntimeException→LLM_OTHER).
  Nota: REQUIREMENT dice "LLM_TRANSPORT" pero el contrato generado tiene LLM_UNREACHABLE; usé LLM_UNREACHABLE (el nombre real del enum).
  Compilación: DefaultAgentRuntimeErrorClassifierTest sin errores; LemmaAbsenceAgentGeneratorTest tiene fallas preexistentes (ChatModel no encontrado + SuggestedLemmaQuerySession.requestMore() faltante); pendiente @developer.

2026-06-19 — test-writer — LemmaAbsenceAgentGeneratorTest: 5 stubs (R002-R006) implementados con Mockito.
  Mockito permite mockear AgentRuntimeLauncher sin importar ChatModel. null pasado como ChatModel en el constructor (fakes no lo usan). Compilación verificada manualmente con javac + langchain4j-core:1.13.0: 0 errores. mvn test-compile falla porque pom.xml declara langchain4j-core:0.36.2 (no tiene ChatModel) como dep directa que excluye la versión 1.13.0 transitiva de sentinel-agents. @architect debe corregir el pom (excluir o actualizar la dep directa a 1.13.0).

2026-06-19 — developer — Implementación production completa de lemmaabsenceagent (6 clases). 8/8 tests VERDE.
  DefaultSuggestedLemmaAgentTool: delega session.requestMore(criteria). DefaultAgentRuntimeErrorClassifier: clasifica RunState por terminationReason + Throwable por cadena de causas (TIMEOUT/UNREACHABLE/AUTH/OTHER). DefaultAgentCandidateParser: lee artifact "quizCandidate" (JSON quizSentence+translation desde file path). DefaultAgentRuntimeLauncher: AgentLoader.loadFromDirectory + AgentRun.run con ExecutionContext. LemmaAbsenceAgentGenerator: single-launch + classify-on-exception + no-retry. DefaultLemmaAbsenceAgentGeneratorFactory: ChatModel via OpenAiChatModel.builder() 1.13.0 + wires all deps.

2026-06-19 — architect — Fix langchain4j 0.36.2→1.13.0 propuesto (NO aplicado).
  Salida: "Validated: 0 additions, 1 modifications, 0 deletions, 0 conflicts." + WARNING de modify de 2 deps.
  architectural_patch.yaml ahora contiene SOLO el fix de versiones (langchain4j-openai + langchain4j-core a 1.13.0; ChatLanguageModel→ChatModel en provides). El cleanup de orphans previo ya fue reemplazado (--replace).
  PENDIENTE: este patch + el cleanup de orphans NO están aplicados aún. Si el usuario quiere ambos en una sola pasada, hay que re-proponer combinando (el --replace del fix descartó el cleanup del proposal). Avisar al coordinador/usuario.

2026-06-19 — test-writer — FLasagJ009JourneyTest (path-1) implementado y VERDE.
  Paso 1 (assert_default): resolver.resolve(env sin CONTENT_AUDIT_LAGEN_MAX_EVAL_RETRIES) → assertEquals(3, getMaxEvalRetries()).
  Paso 2 (ejecutar_canned): config.setMaxEvalRetries(99); CannedLemmaAbsenceQuizCandidateGenerator.generate(context) → assertEquals candidato fijo.
  Fix: KEY_* son package-private → literales "CONTENT_AUDIT_LAGEN_*" en vez de DefaultLagenConfigResolver.KEY_*.
  Tests run: 1, Failures: 0, Errors: 0. BUILD SUCCESS.

2026-06-19 — test-writer — FLasagJ004JourneyTest (path-1 + path-2), FLasagJ005JourneyTest (path-1), FLasagJ006JourneyTest (path-1) implementados y VERDES (4/4).
  Patrón: mock launcher COMPLETED + parser devuelve candidato fijo → assertNotNull result/quizSentence/translation. Mecánica interna (retry/best-effort/deseable) es graph-internal; el seam asserta solo el outcome "se emite un QuizCandidate sin excepción".
  J004-path-2: todos los bloqueantes pasan en primer intento (mismo patrón, sin lógica de retry visible).

2026-06-19 — test-writer — FLasagJ003JourneyTest + FLasagJ007JourneyTest implementados y VERDES (2/2).
  Patrón: mock AgentRuntimeLauncher.launch() arroja RuntimeException → classifier.classify(ex) devuelve LLM_TIMEOUT → assertThrows(ProposalStrategyFailedException) con reason.contains("LLM_TIMEOUT") + verify(launcher, times(1)).
  J003 cubre el path de error en fase de generación (R005+R006); J007 cubre el path de error en fase de evals (R012), mismo seam observable (un único launch que falla → excepción con categoría preservada, sin retry).
