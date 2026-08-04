<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: agent-runtime-infrastructure (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

Capacidad compartida para correr un agente declarativo de sentinel-agents in-process: carga el grafo de <agentsBaseDir>/<nombre>/, lo ejecuta con AgentRun.runRecording y persiste el run. Existe porque este lanzador ya se escribio dos veces identico dentro de revision-infrastructure (lemmaabsenceagent y knowledgetitleagent) y el juez de consigna seria la tercera copia — y no puede reusar ninguna, porque revision-infrastructure declara allowedClients=[audit-cli] y cuelga de revision-domain, mientras que este consumidor nuevo cuelga de la auditoria. Deliberadamente NO incluye la clasificacion de errores: el clasificador de cada consumidor devuelve su propia taxonomia de fallas y no es compartible sin arrastrar tipos ajenos. Declarado de forma identica en el patch principal a proposito, para que este patch valide y aplique en cualquier orden.


## Models

### AgentGraphRunnerConfig (`record`)

| Field | Type |
|-------|------|
| agentsBaseDir | `Path` |
| runsBaseDir | `Path` |

### AgentDefinitionFound (`record`)

| Field | Type |
|-------|------|
| directory | `Path` |

### AgentDefinitionUnresolved (`record`)

| Field | Type |
|-------|------|
| agentName | `String` |
| searchedIn | `Path` |

## Interfaces

### AgentGraphRunner (port)

Methods:

- `run(String agentName,Map<String,String> inputs,ChatModel chatModel,Map<String,Object> tools): RunState`

### AgentGraphRunnerFactory (factory)

Methods:

- `create(AgentGraphRunnerConfig config): AgentGraphRunner`

### AgentDefinitionLocation

### AgentDefinitionLocator (port)

Methods:

- `locate(String agentName): AgentDefinitionLocation`
- `locateUnderProjectRoot(String projectRoot,String agentName): AgentDefinitionLocation`

### AgentDefinitionLocatorFactory (factory)

Methods:

- `create(AgentGraphRunnerConfig config): AgentDefinitionLocator`

