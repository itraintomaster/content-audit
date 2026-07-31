---
patch: ARCH-002
requirement: 2026-07-29.03_evaluaciones-costosas-reutilizables
generated: 2026-07-30T00:00:00Z
---

# Tech Spec: Consolidacion del runtime de grafos declarativos

## Nota de alcance y de anclaje

Este patch **no implementa ninguna regla de FEAT-EVCOST**. Vive en esta carpeta por dos razones
practicas: es la unica del par EVCOST/QINST que no tenia patch propio, y un patch por carpeta es
la unica forma de mantenerlo separado y aplicable de forma independiente del patch principal
(que vive en `requirements/2026-07-29.02_validacion-de-consigna-de-quiz/`). Si preferis moverlo
a una carpeta de requerimiento propia cuando `@analyst` la cree, el contenido se traslada tal
cual.

Es la contracara de una decision que quedo registrada al disenar el patch principal: el
lanzamiento in-process de un grafo de sentinel-agents estaba escrito **dos veces identico**
dentro de `revision-infrastructure`, y el juez de consigna habria sido la tercera copia. El
patch principal introduce el modulo compartido y engancha ahi solo al consumidor nuevo; este
patch migra a los dos legados.

## Orden de aplicacion

**Orden recomendado: este patch DESPUES del principal.**

Los dos patches declaran `agent-runtime-infrastructure` de forma **identica**, incluida su lista
de tres `allowedClients`. Eso es deliberado y tiene una consecuencia util: el segundo en
aplicarse es un no-op sobre ese modulo, y el resultado final es el mismo en cualquiera de los
dos ordenes. Es la unica forma de no depender de la semantica de `_change: add` sobre un elemento
que ya existe, que en este proyecto es un no-op silencioso.

Que pasa si se aplica **solo**: crea `agent-runtime-infrastructure` por su cuenta y la
consolidacion queda completa y coherente — los dos consumidores legados quedan migrados y el
duplicado eliminado. Lo unico que falta en ese escenario es el consumidor nuevo (el juez de
consigna), que lo trae el patch principal. No hay estado intermedio roto en ninguno de los dos
ordenes.

```architecture
modules:
  - name: agent-runtime-infrastructure
    _change: add
    layer: infrastructure
    dependsOn: []
    uses: [sentinel-agents, langchain4j-core]
    allowedClients: [quiz-instruction-infrastructure, audit-cli, revision-infrastructure]
    interfaces:
      - name: AgentGraphRunnerFactory
        _change: add
        stereotype: factory
        exposes:
          - signature: "create(AgentGraphRunnerConfig config): AgentGraphRunner"
```

## Absorber las dos variantes de resolucion de rutas en la config del runner

Los dos lanzadores duplicados no eran byte-identicos: comparten el algoritmo completo
—`AgentLoader.loadFromDirectory`, `AgentRun.runRecording`, `RunPersistence`, el mismo formato de
`runId`, el mismo `enriched.put("run_dir", ...)`— y difieren en un solo punto, la resolucion del
directorio base. El de lemma-absence resuelve relativo al CWD; el de knowledge-title resuelve
relativo a `inputs["project_root"]` cuando esta presente.

Extraer solo el denominador comun habria regresionado a uno de los dos. Por eso el contrato del
runner declara una cadena de resolucion de tres pasos —config, luego `project_root`, luego CWD—
que es la **union** de los dos comportamientos actuales: cada consumidor sigue obteniendo
exactamente las rutas que obtenia, sin que su factory tenga que cambiar nada.

```architecture
modules:
  - name: agent-runtime-infrastructure
    _change: modify
    models:
      - name: AgentGraphRunnerConfig
        _change: add
        type: record
        fields:
          - { name: agentsBaseDir, type: Path }
          - { name: runsBaseDir, type: Path }
    interfaces:
      - name: AgentGraphRunner
        _change: add
        stereotype: port
        exposes:
          - signature: "run(String agentName,Map<String,String> inputs,ChatModel chatModel,Map<String,Object> tools): RunState"
    packages:
      - name: graphexecution
        _change: add
        visibility: public
        implementations:
          - name: DefaultAgentGraphRunnerFactory
            _change: add
            visibility: public
            implements: [AgentGraphRunnerFactory]
          - name: DefaultAgentGraphRunner
            _change: add
            implements: [AgentGraphRunner]
```

## Borrar la implementacion duplicada y conservar el puerto local de cada consumidor

Esta es la decision central del patch y la que lo hace seguro. La opcion obvia —borrar tambien
las interfaces `AgentRuntimeLauncher` y `KnowledgeTitleAgentRuntimeLauncher` y hacer que los
generators inyecten `AgentGraphRunner` directamente— habria cambiado el `requiresInject` de
`LemmaAbsenceAgentGenerator` y de `KnowledgeTitleAgentGenerator`, y con el la firma del
constructor que **nueve handwrittenTests** de FEAT-LASAG y FEAT-KTLR ejercitan mockeando el
lanzador. Habria convertido una consolidacion en un re-tagging masivo de tests verdes, con el
arbol ya sin compilar por FEAT-RPRES.

Se elimina entonces solo lo que estaba genuinamente duplicado: las **implementaciones**. Cada
paquete conserva su interfaz local y gana un adaptador delgado que delega en el runner
compartido — el de knowledge-title pasando un mapa de tools vacio, porque su puerto no lo
declara. El algoritmo de 40 lineas queda en un unico lugar; lo que sobrevive por consumidor son
tres lineas de delegacion. Ningun generator cambia, ningun test cambia de firma.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    dependsOn: [revision-domain, refiner-domain, agent-runtime-infrastructure]
    packages:
      - name: lemmaabsenceagent
        _change: modify
        implementations:
          - name: DefaultAgentRuntimeLauncher
            _change: delete
          - name: SharedRuntimeAgentLauncher
            _change: add
            implements: [AgentRuntimeLauncher]
            requiresInject:
              - { name: agentGraphRunner, type: AgentGraphRunner }
      - name: knowledgetitleagent
        _change: modify
        implementations:
          - name: DefaultKnowledgeTitleAgentRuntimeLauncher
            _change: delete
          - name: SharedRuntimeKnowledgeTitleLauncher
            _change: add
            implements: [KnowledgeTitleAgentRuntimeLauncher]
            requiresInject:
              - { name: agentGraphRunner, type: AgentGraphRunner }
```

## Dejar los clasificadores de error donde estan

`AgentRuntimeErrorClassifier` y `KnowledgeTitleAgentErrorClassifier` tambien estan duplicados
—misma logica de timeout / auth / transporte / otro— y **no** se consolidan. La razon es de
contrato y no de prolijidad: los dos devuelven `LlmGenerationFailureCategory`, un tipo del
paquete `lagen` de revision-infrastructure, y el consumidor nuevo necesita su propio
`EvaluationFailureKind`. Compartir el clasificador obligaria a mover
`LlmGenerationFailureCategory` a un modulo neutro, y ese tipo esta en la superficie publica que
`audit-cli` ya consume.

Consolidarlos requeriria una taxonomia neutra de fallas de runtime mas un mapeo por consumidor:
tres piezas nuevas para deduplicar una funcion de clasificacion por strings. No se paga hoy.
Queda anotado como deuda consciente, no como olvido.
