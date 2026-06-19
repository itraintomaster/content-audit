# decisions

2026-06-19 — architect — Runtime seam de sentinel-agent descubierto (incógnito central de F-LASAG).
  Punto de entrada de producción: `com.sentinel.agents.framework.runner.AgentRun.run(Node rootAgent, Map<String,String> inputs, ExecutionContext ctx): RunState`.
  Carga del grafo declarativo: `AgentRegistry.discover(startDir)` + `registry.load(nameOrAbbrev): Node` (o `AgentLoader.loadFromDirectory(.sentinel/agents/<name>)`).
  ExecutionContext (record) lleva: ChatModel (dev.langchain4j.model.chat.ChatModel), Map<String,Object> tools, gates, scripts, workDir, dispatcher (NodeRunners).
  Resultado: RunState.runStatus() == COMPLETED|HALTED; HALTED+HaltReason.ERROR es la falla; artifacts() = Map<String,ArtifactPointer>.
  Tool in-process: ExecutionContext.tools es Map<String,Object> de POJOs @Tool de langchain4j (builtins como ReadFileTool son objetos Java planos). NO obliga a subprocess: un módulo Java puede inyectar su propio tool object. -> la única tool de F-LASAG (suggested-lemmas) se expone como POJO @Tool in-process.
  why: el requirement declara el seam de runtime como la duda central; queda resuelto leyendo sentinel-agents.

2026-06-19 — architect — La tool NO es GetCommand.get (devuelve Integer/exit-code y vive en audit-cli, composition root).
  La semántica F-CSLATDC ya está como puerto reutilizable en refiner-domain.lemmasuggestion: SuggestedLemmaQueryPort / SuggestedLemmaQuerySession / SuggestedLemmaQuerySessionFactory (los usa hoy el backend interactivo lageninteractive). El tool object del agente delega ahí, sin duplicar F-CSLATDC ni invocar el verbo CLI.
  why: R001/R002 hablan de la semántica de la consulta read-only, no del verbo CLI literal; reusar el puerto evita acoplar revision-infrastructure a audit-cli.

2026-06-19 — architect — DOUBT-TRAZABILIDAD-AGENTE queda ABIERTA (no se resuelve en este patch, por pedido explícito).

2026-06-19 — analyst — Clasificación de Validation para reglas sin superficie JUnit (R007-R010).
  El DSL com.sentinel.dsl.ValidationStatus admite SOLO {VALIDATED, AUTO_VALIDATED, ASSUMPTION} (verificado por javap sobre el jar). No existe un valor para "validado por el harness de evals del propio agente / fuera del suite JUnit".
  Decisión: R007-R010 → VALIDATED, con nota explícita en metadata "(validada por el harness de evals del agente, no por el suite JUnit)". Razón: AUTO_VALIDATED implica cobertura por el suite JUnit, que aquí no existe (qa-tester ya rehusó taggearlas); VALIDATED = confirmación humana del usuario sobre la conducta del harness, es el valor más fiel disponible. El "por qué no AUTO_VALIDATED" queda en el Detail de cada regla.
  why: pedido explícito de usar la clasificación correcta del DSL y dejar explícito el porqué cuando no hay un valor específico.

2026-06-19 — analyst — `sentinel feature sync` es aditivo-only; NO poda journeys huérfanos.
  Sync agrega journeys nuevos a features[].journeys[] pero deja los que ya no están en REQUIREMENT.md con el aviso "will be pruned at next generate". La poda real (y por tanto la eliminación de los 9 testPackage refs colgantes a lagenopenai/lageninteractive) ocurre en `sentinel generate`, que regenera código/contratos/tests — fuera del lane del analyst. Escalado a @architect.
  why: el task esperaba que el sync eliminara los refs; el comportamiento real del CLI lo difiere a generate.

2026-06-19 — analyst — Formato de journeys discoverable: 1 heading `### Journey[...]` + 1 fence yaml single-entry por journey.
  El bloque combinado (9 journeys en un único fence `journeys:`) que tenía F-LASAG era leído como 0 journeys por el parser de `feature sync`. El formato estándar del proyecto (ver FEAT-CDIFF, FEAT-LAGEN) es un fence por journey bajo su heading. Reescrito; los 9 se registraron.
  why: regla operativa para futuros REQUIREMENT.md con múltiples flow-journeys en este proyecto.

2026-06-19 — analyst — J008 degradado a prosa (inconsistencia con R009; bloqueaba `sentinel generate`).
  F-LASAG-J008 ("eval no-regresión de length bloquea un candidato más corto") tenía único terminal result:failure, pero R009 (best-effort) garantiza que un veredicto de eval NUNCA propaga ProposalStrategyFailedException al consumidor — al agotar maxEvalRetries se emite el mejor candidato (success). El failure de J008 es inalcanzable en generate(); cualquier journey-test sería cobertura falsa. Removido del REQUIREMENT.md; escenario ya cubierto por R007 (eval #3) + nota agregada en su Detail. IDs restantes SIN renumerar: J001-J007, J009 estables (J009 NO pasa a J008) para no romper placement/traceability del patch QA.
  `sentinel feature sync` (aditivo-only) NO podó J008 de sentinel.yaml: lo flaggea "will be pruned at next generate". sentinel.yaml sigue con 9 (incl. J008) hasta que @architect corra `sentinel generate`. NO hand-edité sentinel.yaml (journeys derivados + single-writer @architect).
  why: pedido del coordinador; J008 contradecía R009 y rompía generate.

2026-06-19 — architect — Patch + TECH_SPEC entregados (no aplicados).
  Diseño: paquete interno lemmaabsenceagent reemplaza lagenopenai+lageninteractive. Seam público único LemmaAbsenceAgentGeneratorFactory. Adaptador LemmaAbsenceAgentGenerator implementa el puerto existente sin cambiar firma.
  Colaboradores package-private: AgentRuntimeLauncher (aísla AgentRun/AgentRegistry/ExecutionContext, devuelve RunState), SuggestedLemmaAgentTool (delega en SuggestedLemmaQuerySession, F-CSLATDC), AgentRuntimeErrorClassifier (RunState/Throwable -> LlmGenerationFailureCategory -> ProposalStrategyFailedException con categoría), AgentCandidateParser (RunState exitoso -> LemmaAbsenceGeneratorResponse; reemplaza al LemmaAbsenceResponseParser borrado con lagenopenai).
  why: el parser de respuesta vivía en lagenopenai (borrado); el adaptador del agente necesita su propio parser dentro del nuevo paquete — fix en la 2da ronda de propose (merge).
  TECH_SPEC.md: 9 fences, español.

2026-06-19 — architect — Limpieza de 2 orphans post-aplicación de F-LASAG (patch propuesto, NO aplicado).
  Orphan 1: pattern Factory de InteractiveLemmaAbsenceGeneratorFactory quedó colgando tras borrar su interface/impl/paquete.
  Orphan 2: descripción del paquete lagen seguía apuntando a 'lagenopenai' (borrado) → corregida a 'lemmaabsenceagent'.
  HALLAZGO (limitación Sentinel, verificada en código): el bloque `patterns` NO honra `_change: delete` por elemento. PatchMerger:112 y PatchApplier:151 hacen reemplazo TOTAL: `!incoming.patterns().isEmpty() ? incoming.patterns() : existing.patterns()`. Por eso el orphan se creó en el patch original (la lista entrante traía orphan-con-delete + nuevo, y el delete fue ignorado al reemplazar la lista verbatim).
  why: para retirar un pattern hay que enviar la lista COMPLETA de patterns deseados (sin el orphan), no un `_change: delete`. Usé --replace para arrancar limpio.

2026-06-19 — architect — Fix de conflicto langchain4j (blocker de compilación de revision-infrastructure).
  Causa raíz (verificada con mvn dependency:tree + jars en .m2): langchain4j-openai y langchain4j-core declarados en 0.36.2; el código nuevo de lemmaabsenceagent usa dev.langchain4j.model.chat.ChatModel (1.x), que llega transitivo desde sentinel-agents (langchain4j 1.13.0). Maven "nearest wins" → 0.36.2 directo gana; 0.36.x no tiene ChatModel (era ChatLanguageModel). No compila.
  Blast radius (verificado): único uso de langchain4j en producción = los 3 archivos nuevos de lemmaabsenceagent, todos con ChatModel. CERO usos de ChatLanguageModel/OpenAiChatModel/message-types 0.36.x en main (el código viejo lagenopenai/lageninteractive fue borrado). Deps 0.36.2 stale.
  Versión elegida: 1.13.0 para ambos aliases — matchea EXACTAMENTE lo que pulle sentinel-agents (sentinel-agents/pom.xml langchain4j.version=1.13.0; tree muestra langchain4j:1.13.0). Todos los jars 1.13.0 están en .m2. Elegir 1.13.0 evita re-introducir conflicto.
  Nota Maven Central Solr: el índice select solo muestra hasta 1.0.0 para ambos artefactos, pero el tree resuelto y los jars en .m2 prueban que 1.13.0 existe y resuelve (BUILD SUCCESS). El tree es ground truth, no el índice Solr.
  Cambios de provides 0.36→1.13: ChatLanguageModel (dev.langchain4j.model.chat) DESAPARECE en 1.13.0 → se reemplaza por ChatModel (mismo paquete). El resto (OpenAiChatModel+builder dev.langchain4j.model.openai; ChatMessage/SystemMessage/UserMessage/AiMessage dev.langchain4j.data.message; Response dev.langchain4j.model.output) conserva paquete.
