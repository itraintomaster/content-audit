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

2026-06-19 — architect — Patch + TECH_SPEC entregados (no aplicados).
  Diseño: paquete interno lemmaabsenceagent reemplaza lagenopenai+lageninteractive. Seam público único LemmaAbsenceAgentGeneratorFactory. Adaptador LemmaAbsenceAgentGenerator implementa el puerto existente sin cambiar firma.
  Colaboradores package-private: AgentRuntimeLauncher (aísla AgentRun/AgentRegistry/ExecutionContext, devuelve RunState), SuggestedLemmaAgentTool (delega en SuggestedLemmaQuerySession, F-CSLATDC), AgentRuntimeErrorClassifier (RunState/Throwable -> LlmGenerationFailureCategory -> ProposalStrategyFailedException con categoría), AgentCandidateParser (RunState exitoso -> LemmaAbsenceGeneratorResponse; reemplaza al LemmaAbsenceResponseParser borrado con lagenopenai).
  why: el parser de respuesta vivía en lagenopenai (borrado); el adaptador del agente necesita su propio parser dentro del nuevo paquete — fix en la 2da ronda de propose (merge).
  TECH_SPEC.md: 9 fences, español.
