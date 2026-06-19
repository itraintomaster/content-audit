---
patch: FEAT-LASAG
requirement: 2026-06-18.01_lemma-absence-agent-migration
generated: 2026-06-19T11:45:00Z
---

# Tech Spec: Agente sentinel-agent para generación de candidatos de lemma-absence

## Retirar el valor LLM_INTERACTIVE del enum LagenMode
La ruta interactiva (FEAT-LAGAG) deja de existir como backend separado: el agente la absorbe junto al single-shot bajo `LagenMode.LLM`. Eliminamos el constante del enum para que `LagenModeResolver` ya no pueda resolver `lemma-absence-llm-interactive` y el composition root quede con dos ramas reales (`LLM` → agente, `CANNED` → fixture offline). Como `LagenMode` es un archivo `@Generated`, el retiro se modela aquí y se regenera; no se edita a mano.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: LagenMode
        type: enum
        _change: modify
        fields:
          - { name: LLM_INTERACTIVE, _change: delete }
```

## Agregar maxEvalRetries a LagenConfig
El lazo de reintentos por veredicto bloqueante (R008) necesita una cota configurable que viva fuera del grafo declarativo, junto al resto de las perillas LLM. La colocamos en `LagenConfig` —el único carrier que el seam ya consume— en lugar de inventar un record nuevo (P3: no generalizar sin un eje de extensión concreto). Es la contraparte de `maxSuggestedLemmaRequests`: nullable/0 como sentinel, default 3 resuelto por `LagenConfigResolver`, e ignorada por `CANNED` (R011).

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lagen
        _change: modify
        models:
          - name: LagenConfig
            type: record
            _change: modify
            fields:
              - { name: maxEvalRetries, type: Integer, _change: add }
```

## Unificar las dos factories LLM en un único seam de agente
Hoy el paquete público `lagen` expone dos factories divergentes —una para el single-shot, otra para el interactivo— cada una con su propio wiring y `StrategyId.name`. Las reemplazamos por un único `LemmaAbsenceAgentGeneratorFactory` (Factory Seam, P6: una capacidad, un seam): el composition root arma el backend LLM con una sola llamada, pasando la `LagenConfig` y el `SuggestedLemmaQuerySessionFactory` que liga la tool por task. El contrato producido sigue siendo el puerto `LemmaAbsenceQuizCandidateGenerator` sin cambios (FEAT-LAPS R001–R003).

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    patterns:
      - type: Factory
        interface: InteractiveLemmaAbsenceGeneratorFactory
        implementations: [DefaultInteractiveLemmaAbsenceGeneratorFactory]
        _change: delete
      - type: Factory
        interface: LemmaAbsenceAgentGeneratorFactory
        implementations: [DefaultLemmaAbsenceAgentGeneratorFactory]
        _change: add
    packages:
      - name: lagen
        _change: modify
        interfaces:
          - name: LemmaAbsenceLlmGeneratorFactory
            _change: delete
          - name: InteractiveLemmaAbsenceGeneratorFactory
            _change: delete
          - name: LemmaAbsenceAgentGeneratorFactory
            stereotype: factory
            visibility: public
            _change: add
            exposes:
              - signature: "create(LagenConfig config, SuggestedLemmaQuerySessionFactory sessionFactory): LemmaAbsenceQuizCandidateGenerator"
              - signature: "providerIdFor(LagenConfig config): String"
```

## Retirar los paquetes lagenopenai y lageninteractive
Los dos motores LLM previos quedan obsoletos: el agente unifica ambas rutas. Los eliminamos por completo (con sus adaptadores, prompt builders, parsers y clasificadores) para que no sobreviva código muerto ni dos provenances paralelas. El borrado es explícito porque el merge del patch preserva lo no mencionado; sin estos `_change: delete` los paquetes colisionarían con el nuevo motor en `sentinel generate`.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lagenopenai
        _change: delete
      - name: lageninteractive
        _change: delete
```

## Crear el paquete interno lemmaabsenceagent (Public Port, Hidden Adapter)
Reemplazamos los dos motores por un único paquete interno que encapsula todo el backend agéntico detrás del seam público. Solo `DefaultLemmaAbsenceAgentGeneratorFactory` es pública (la instancia una sola vez `audit-cli.bootstrap`); el adaptador y sus colaboradores son package-private, así ningún otro módulo puede saltarse el factory (P2/P4/P5). El `LemmaAbsenceAgentGenerator` implementa el puerto existente sin cambiar la firma y queda como `Component` para DI manual en el composition root.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lemmaabsenceagent
        _change: add
        visibility: internal
        implementations:
          - name: DefaultLemmaAbsenceAgentGeneratorFactory
            visibility: public
            _change: add
            implements: [LemmaAbsenceAgentGeneratorFactory]
            types: [Component]
          - name: LemmaAbsenceAgentGenerator
            _change: modify
            implements: [LemmaAbsenceQuizCandidateGenerator]
            types: [Component]
            requiresInject:
              - { name: config, type: LagenConfig }
              - { name: agentRuntimeLauncher, type: AgentRuntimeLauncher }
              - { name: sessionFactory, type: SuggestedLemmaQuerySessionFactory }
              - { name: errorClassifier, type: AgentRuntimeErrorClassifier }
              - { name: candidateParser, type: AgentCandidateParser }
              - { name: chatModel, type: ChatModel }
              - { name: strategyName, type: String }
```

## Aislar el runtime de sentinel-agent detrás del port AgentRuntimeLauncher
El lanzamiento del agente declarativo —cargar el grafo por nombre, construir el `ExecutionContext` (chat-model + tools) y llamar `AgentRun.run(...)`— es el único punto que toca los tipos del runtime de sentinel-agent. Lo escondemos tras un port interno fino para que el adaptador sea testeable con un launcher falso y para que la dependencia del framework no se filtre al resto del motor. El método devuelve el `RunState` crudo; la interpretación (éxito vs. falla, candidato emitido) vive en los colaboradores siguientes. El nombre del agente declarativo es una constante interna del adaptador, no una perilla de `LagenConfig`.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lemmaabsenceagent
        _change: modify
        interfaces:
          - name: AgentRuntimeLauncher
            visibility: internal
            _change: add
            exposes:
              - signature: "launch(String agentName, Map<String,String> inputs, ChatModel chatModel, Map<String,Object> tools): RunState"
        implementations:
          - name: DefaultAgentRuntimeLauncher
            _change: add
            implements: [AgentRuntimeLauncher]
            types: [Component]
```

## Exponer la única tool como objeto Java in-process que reusa F-CSLATDC
La única tool del agente lista/filtra `suggestedWords`. En vez de duplicar la consulta o invocar el verbo CLI `GetCommand.get` (que devuelve un exit-code y vive en el composition root), modelamos un objeto-tool in-process que delega en `SuggestedLemmaQuerySession` —la semántica read-only F-CSLATDC que ya consumía el backend interactivo—. El runtime de sentinel-agent acepta tools como POJOs en su `Map<String,Object>`, así que este objeto se inyecta directo sin pasar por un subprocess. La sesión se liga por task vía el `SuggestedLemmaQuerySessionFactory` que recibe el adaptador.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lemmaabsenceagent
        _change: modify
        interfaces:
          - name: SuggestedLemmaAgentTool
            visibility: internal
            _change: add
            exposes:
              - signature: "fetchSuggestedLemmas(SuggestedLemmaQueryCriteria criteria): SuggestedLemmaQueryResult"
        implementations:
          - name: DefaultSuggestedLemmaAgentTool
            _change: add
            implements: [SuggestedLemmaAgentTool]
            requiresInject:
              - { name: session, type: SuggestedLemmaQuerySession }
```

## Mapear fallas de runtime a categoría preservada y parsear el candidato del RunState
Dos responsabilidades cierran el borde Java→agente. El `AgentRuntimeErrorClassifier` traduce una falla del runtime (timeout/auth/transporte/otro, leída del `RunState` halted o de la excepción) a `LlmGenerationFailureCategory`, de donde el adaptador la propaga como `ProposalStrategyFailedException` con categoría preservada (R005/R012, honrando FEAT-LAPS R015). El `AgentCandidateParser` extrae el candidato emitido del `RunState` exitoso como `LemmaAbsenceGeneratorResponse`; reemplaza al `LemmaAbsenceResponseParser` que desapareció con `lagenopenai`. El gate estructural de FEAT-LAPS sigue fuera del agente, sin cambios.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lemmaabsenceagent
        _change: modify
        interfaces:
          - name: AgentRuntimeErrorClassifier
            visibility: internal
            _change: add
            exposes:
              - signature: "classify(RunState runState): LlmGenerationFailureCategory"
              - signature: "classify(Throwable cause): LlmGenerationFailureCategory"
          - name: AgentCandidateParser
            visibility: internal
            _change: add
            exposes:
              - signature: "parse(RunState runState, String taskId, String strategyName): LemmaAbsenceGeneratorResponse"
        implementations:
          - name: DefaultAgentRuntimeErrorClassifier
            _change: add
            implements: [AgentRuntimeErrorClassifier]
          - name: DefaultAgentCandidateParser
            _change: add
            implements: [AgentCandidateParser]
```

## Declarar la dependencia del framework sentinel-agent
El adaptador necesita los tipos del runtime para lanzar el agente in-process. Declaramos `com.sentinel:sentinel-agents:0.0.1-SNAPSHOT` con los tipos del seam que el módulo referencia en firmas e inyecciones: el punto de entrada `AgentRun`, el carrier de contexto `ExecutionContext`, la definición de grafo `Node`, la carga por nombre (`AgentRegistry`/`AgentLoader`), el resultado (`RunState`/`RunStatus`/`ArtifactPointer`) y el `ChatModel` de langchain4j. Es un SNAPSHOT instalado en el repo local, no en Maven Central; queda solo en `revision-infrastructure`, que conserva `allowedClients: [audit-cli]` para que el framework no se filtre más allá del composition root.

```architecture
dependencies:
  - alias: sentinel-agents
    artifact: com.sentinel:sentinel-agents:0.0.1-SNAPSHOT
    scope: compile
    provides:
      - { type: AgentRun, package: com.sentinel.agents.framework.runner }
      - { type: ExecutionContext, package: com.sentinel.agents.framework.runner }
      - { type: Node, package: com.sentinel.agents.framework.definition }
      - { type: AgentRegistry, package: com.sentinel.agents.framework.registry }
      - { type: AgentLoader, package: com.sentinel.agents.framework.loader }
      - { type: RunState, package: com.sentinel.agents.framework.state }
      - { type: RunStatus, package: com.sentinel.agents.framework.state }
      - { type: ArtifactPointer, package: com.sentinel.agents.framework.state }
      - { type: ChatModel, package: dev.langchain4j.model.chat }
modules:
  - name: revision-infrastructure
    _change: modify
    uses: [sentinel-agents]
```
