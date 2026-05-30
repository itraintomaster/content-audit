---
patch: ARCH-LAGAG
requirement: 2026-05-29.01_generacion-interactiva-suggested-lemmas
generated: 2026-05-30T01:45:00Z
---

# Tech Spec: Generacion interactiva con consulta de suggested lemmas (FEAT-LAGAG)

Evolucion **aditiva** de F-LAGEN. Los modos single-shot (`LLM`) y `CANNED` quedan intactos: no se toca el puerto `LemmaAbsenceQuizCandidateGenerator`, ni `LemmaAbsenceGeneratorResponse`, ni el `CannedLemmaAbsenceQuizCandidateGenerator`. El modo nuevo es una tercera implementacion del mismo puerto, seleccionada explicitamente por el operador. Esta revision cierra DOUBT-BIND-CALLSITE (la sesion se liga por-task dentro del generador) y resuelve dos bloqueos de compilacion (constructor de `LagenDefaults` y visibilidad de dos clases concretas).

## Exponer una sesion de consulta acotada a la task

F-LAGAG-R002 exige que el modelo pueda pedir mas suggested lemmas para la MISMA task con la semantica de F-CSLATDC, pero `SuggestedLemmaQueryPort.query` requiere `AuditReport` y `RefinementTask` que el `LemmaAbsenceCorrectionContext` no transporta. En vez de filtrar esos objetos hasta el adaptador LLM (que lo acoplaria al dominio de auditoria), se introduce un puerto de **sesion acotada**: la identidad de la task y su `AuditReport` quedan capturados una sola vez, y el generador solo ve la capacidad "mas lemmas para esta task". Vive en `refiner-domain` junto a `SuggestedLemmaQueryPort` y reutiliza su carrier `SuggestedLemmaQueryResult`.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    interfaces:
      - name: "SuggestedLemmaQuerySession"
        _change: "add"
        stereotype: "port"
        visibility: "public"
        exposes:
          - signature: "requestMore(Optional<Integer> limit, Optional<String> partOfSpeech, Optional<CefrLevel> explicitLevel): SuggestedLemmaQueryResult"
```

## Ligar la sesion por taskId con un Abstract Factory

La sesion no puede ligarse en el composition root porque el `AuditReport` y la `RefinementTask` solo existen por-revision; cablearla con `null/null` produce un NPE al primer `requestMore`. El generador si dispone del `taskId` (via `LemmaAbsenceCorrectionContext.getTaskId()`), por eso `bindForTask` se firma con `String taskId`. La impl recarga la task y su `AuditReport` siguiendo el precedente de `GetCmd.handleSuggestedLemmas` (cargar el plan, ubicar la task, recargar el report via `auditReportStore`), lo que explica por que inyecta `RefinementPlanStore` + `AuditReportStore` ademas del puerto.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    interfaces:
      - name: "SuggestedLemmaQuerySessionFactory"
        _change: "add"
        stereotype: "factory"
        visibility: "public"
        exposes:
          - signature: "bindForTask(String taskId): SuggestedLemmaQuerySession"
    packages:
      - name: "lemmasuggestion"
        _change: "modify"
        implementations:
          - name: "DefaultSuggestedLemmaQuerySessionFactory"
            _change: "add"
            visibility: "public"
            implements: ["SuggestedLemmaQuerySessionFactory"]
            requiresInject:
              - { name: "queryPort", type: "SuggestedLemmaQueryPort" }
              - { name: "refinementPlanStore", type: "RefinementPlanStore" }
              - { name: "auditReportStore", type: "AuditReportStore" }
    patterns:
      - type: "Factory"
        interface: "SuggestedLemmaQuerySessionFactory"
        implementations: ["DefaultSuggestedLemmaQuerySessionFactory"]
```

## Mantener BoundSuggestedLemmaQuerySession ligada a (report, task)

`BoundSuggestedLemmaQuerySession` es lo que produce la factory por cada task: el unico punto que conoce a la vez el puerto y los argumentos `AuditReport`/`RefinementTask`, delegando cada `requestMore` en `queryPort.query(report, task, ...)`. Su forma no cambia en esta revision. Es `public` porque la factory (mismo paquete) y los tests la instancian.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    packages:
      - name: "lemmasuggestion"
        _change: "modify"
        implementations:
          - name: "BoundSuggestedLemmaQuerySession"
            _change: "add"
            visibility: "public"
            implements: ["SuggestedLemmaQuerySession"]
            requiresInject:
              - { name: "queryPort", type: "SuggestedLemmaQueryPort" }
              - { name: "report", type: "AuditReport" }
              - { name: "task", type: "RefinementTask" }
```

## Quitar el presupuesto de LagenDefaults para restaurar su constructor 3-arg

El presupuesto de solicitudes es un knob del operador y debe vivir solo en `LagenConfig`. La primera version lo agrego tambien a `LagenDefaults`, lo que dejo su clase regenerada con un unico constructor 4-arg `(double,int,Duration,int)` y rompio tres tests `@Generated` de F-LAGEN que invocan el 3-arg `(double,int,Duration)`. Como el campo ya estaba mergeado en `sentinel.yaml`, "no tocarlo" no alcanza: hay que removerlo explicitamente del modelo. Tras regenerar, `LagenDefaults` vuelve a ofrecer el constructor 3-arg y los tres tests de F-LAGEN compilan sin cambios. El factory interactivo lee el presupuesto de `LagenConfig.getMaxSuggestedLemmaRequests()` (o literal 5), nunca de `LagenDefaults`.

```architecture
modules:
  - name: "revision-infrastructure"
    _change: "modify"
    packages:
      - name: "lagen"
        _change: "modify"
        models:
          - name: "LagenDefaults"
            _change: "modify"
            fields:
              - name: "maxSuggestedLemmaRequests"
                type: "int"
                _change: "delete"
```

## Conservar el presupuesto como perilla del operador en LagenConfig

El knob queda donde corresponde: `LagenConfig`, en linea con las demas perillas de F-LAGEN-R008. Es nullable/0-sentinel; el modo interactivo aplica el default 5 cuando el operador no lo informa, y los modos single-shot y CANNED lo ignoran.

```architecture
modules:
  - name: "revision-infrastructure"
    _change: "modify"
    packages:
      - name: "lagen"
        _change: "modify"
        models:
          - name: "LagenConfig"
            _change: "modify"
            fields:
              - name: "maxSuggestedLemmaRequests"
                type: "Integer"
                _change: "add"
                description: "Presupuesto de solicitudes de suggested lemmas que el modelo puede hacer en una generacion interactiva (F-LAGAG-R004). Perilla configurable en linea con F-LAGEN-R008, resuelta del env var CONTENT_AUDIT_LAGEN_MAX_LEMMA_REQUESTS. Nullable/0 = sentinel; el modo interactivo aplica el default 5 cuando no se informa. Ignorada por los modos LLM single-shot y CANNED."
```

## Extender el enum de fallas con las dos categorias del modo interactivo

F-LAGAG-R004 y F-LAGAG-R005 piden categorias de falla propias "en linea con F-LAGEN-R006". Se agregan dos constantes al `LlmGenerationFailureCategory` existente, que ya viaja en el `reason` de `ProposalStrategyFailedException`, manteniendo un unico vocabulario de fallas para ambos modos LLM.

```architecture
modules:
  - name: "revision-infrastructure"
    _change: "modify"
    packages:
      - name: "lagen"
        _change: "modify"
        models:
          - name: "LlmGenerationFailureCategory"
            _change: "modify"
            fields:
              - name: "LLM_LEMMA_REQUEST_BUDGET_EXHAUSTED"
                _change: "add"
                description: "El modelo agoto el presupuesto de solicitudes de suggested lemmas (LagenConfig.maxSuggestedLemmaRequests) sin entregar un candidato. F-LAGAG-R004. Solo aplica al modo interactivo."
              - name: "LLM_TOOL_CALLING_UNSUPPORTED"
                _change: "add"
                description: "El proveedor activo no es capaz de sostener el intercambio interactivo (no soporta tool-calling / function-calling), por lo que el modelo no puede emitir solicitudes de suggested lemmas. F-LAGAG-R005. Solo aplica al modo interactivo. Falla observable, no cuelgue silencioso."
```

## Hacer publicas las dos clases concretas de lagenopenai que el factory interactivo instancia

`DefaultInteractiveLemmaAbsenceGeneratorFactory` (en `lageninteractive`) actua como mini composition-root del adaptador: instancia `DefaultLemmaAbsenceResponseParser` y `DefaultLangChainErrorClassifier` para reutilizar la misma logica de parseo y clasificacion de errores de F-LAGEN. Esas dos clases eran package-private en `lagenopenai`, por lo que el factory no podia construirlas (4 errores de compilacion). Se las hace `public`. El paquete `lagenopenai` sigue `internal`, asi que publico-dentro-de-internal solo las alcanza el mismo modulo (lageninteractive es hermano), nunca otro modulo: la frontera del modulo se mantiene. Son stateless; instanciarlas en el factory es uso valido.

```architecture
modules:
  - name: "revision-infrastructure"
    _change: "modify"
    packages:
      - name: "lagenopenai"
        _change: "modify"
        implementations:
          - name: "DefaultLemmaAbsenceResponseParser"
            _change: "modify"
            visibility: "public"
            implements: ["LemmaAbsenceResponseParser"]
            types: ["Component"]
          - name: "DefaultLangChainErrorClassifier"
            _change: "modify"
            visibility: "public"
            implements: ["LangChainErrorClassifier"]
            types: ["Component"]
```

## Pasar la factory de sesion (no una sesion) por el seam del generador

El composition root no puede pasar una sesion ya ligada porque no tiene la task. Por eso el factory seam del generador recibe la `SuggestedLemmaQuerySessionFactory`: el CLI construye la factory una vez (con sus stores) y se la entrega; el generador resultante liga la sesion por-task en cada `generate`. `providerIdFor` mantiene la trazabilidad por proveedor de F-LAGEN-R009.

```architecture
modules:
  - name: "revision-infrastructure"
    _change: "modify"
    packages:
      - name: "lagen"
        _change: "modify"
        interfaces:
          - name: "InteractiveLemmaAbsenceGeneratorFactory"
            _change: "add"
            stereotype: "factory"
            visibility: "public"
            exposes:
              - signature: "create(LagenConfig config, SuggestedLemmaQuerySessionFactory sessionFactory): LemmaAbsenceQuizCandidateGenerator"
              - signature: "providerIdFor(LagenConfig config): String"
    patterns:
      - type: "Factory"
        interface: "InteractiveLemmaAbsenceGeneratorFactory"
        implementations: ["DefaultInteractiveLemmaAbsenceGeneratorFactory"]
```

## El generador inyecta la factory y liga la sesion en generate(context)

El cambio central del binding: `InteractiveLemmaAbsenceLlmGenerator` ya no recibe una `SuggestedLemmaQuerySession` pre-ligada en el constructor sino la `SuggestedLemmaQuerySessionFactory`. En `generate(context)` hace `session = sessionFactory.bindForTask(context.getTaskId())` y la usa durante el loop de tool-calling. La firma `generate(LemmaAbsenceCorrectionContext)` no cambia (F-LAGAG-R006). El adaptador sigue dependiendo de `ChatLanguageModel` y reutiliza `LemmaAbsenceResponseParser` y `LangChainErrorClassifier` de F-LAGEN; los tipos de tool-calling de LangChain4j siguen confinados a este paquete interno.

```architecture
modules:
  - name: "revision-infrastructure"
    _change: "modify"
    packages:
      - name: "lageninteractive"
        _change: "add"
        visibility: "internal"
        implementations:
          - name: "DefaultInteractiveLemmaAbsenceGeneratorFactory"
            _change: "add"
            visibility: "public"
            implements: ["InteractiveLemmaAbsenceGeneratorFactory"]
          - name: "InteractiveLemmaAbsenceLlmGenerator"
            _change: "add"
            implements: ["LemmaAbsenceQuizCandidateGenerator"]
            requiresInject:
              - { name: "config", type: "LagenConfig" }
              - { name: "sessionFactory", type: "SuggestedLemmaQuerySessionFactory" }
              - { name: "seedPromptBuilder", type: "InteractiveLemmaAbsencePromptBuilder" }
              - { name: "lemmaRequestExchange", type: "SuggestedLemmaToolExchange" }
              - { name: "responseParser", type: "LemmaAbsenceResponseParser" }
              - { name: "errorClassifier", type: "LangChainErrorClassifier" }
              - { name: "strategyName", type: "String" }
              - { name: "chatModel", type: "ChatLanguageModel" }
```

## Hacer stateless el intercambio de tool-calling

El generador liga la sesion una vez por `generate` y se la pasa al exchange en cada solicitud, por lo que el exchange ya no retiene una sesion. `SuggestedLemmaToolExchange.fetchMore` pasa a recibir la `SuggestedLemmaQuerySession` por parametro y `DefaultSuggestedLemmaToolExchange` deja de inyectarla. Queda un unico dueño de la sesion (el generador). `InteractiveLemmaAbsencePromptBuilder` y el record interno `SuggestedLemmaRequest` no cambian.

```architecture
modules:
  - name: "revision-infrastructure"
    _change: "modify"
    packages:
      - name: "lageninteractive"
        _change: "modify"
        interfaces:
          - name: "InteractiveLemmaAbsencePromptBuilder"
            _change: "add"
            visibility: "internal"
            exposes:
              - signature: "buildSystemPrompt(): String"
              - signature: "buildSeedUserPrompt(LemmaAbsenceCorrectionContext context): String"
          - name: "SuggestedLemmaToolExchange"
            _change: "add"
            visibility: "internal"
            exposes:
              - signature: "fetchMore(SuggestedLemmaQuerySession session, SuggestedLemmaRequest request): SuggestedLemmaQueryResult"
        implementations:
          - name: "DefaultInteractiveLemmaAbsencePromptBuilder"
            _change: "add"
            implements: ["InteractiveLemmaAbsencePromptBuilder"]
          - name: "DefaultSuggestedLemmaToolExchange"
            _change: "add"
            implements: ["SuggestedLemmaToolExchange"]
        models:
          - name: "SuggestedLemmaRequest"
            _change: "add"
            visibility: "internal"
            type: "record"
            fields:
              - { name: "limit", type: "Optional<Integer>", description: "Cantidad maxima de suggested lemmas solicitada por el modelo (F-CSLATDC-R008). Vacio = el adaptador decide default." }
              - { name: "partOfSpeech", type: "Optional<String>", description: "Filtro opcional por part-of-speech solicitado por el modelo (F-CSLATDC-R007)." }
              - { name: "level", type: "Optional<CefrLevel>", description: "CefrLevel explicito solicitado por el modelo (F-CSLATDC-R006); vacio = se aplica el nivel de la task." }
```

## Agregar el modo LLM_INTERACTIVE al selector de modo del CLI

F-LAGAG-R003 exige un modo explicito, reconocible y trazable. El composition root, para este modo, construye `DefaultSuggestedLemmaQuerySessionFactory(port, refinementPlanStore, auditReportStore)` y se la pasa al factory del generador — reemplazando la linea rota `new BoundSuggestedLemmaQuerySession(port, null, null)`. El default observable sigue siendo `LLM` (single-shot, F-LAGEN-R013) y `CANNED` queda intacto.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    models:
      - name: "LagenMode"
        _change: "modify"
        fields:
          - name: "LLM_INTERACTIVE"
            _change: "add"
            description: "Modo de generacion interactiva (FEAT-LAGAG). El CLI cablea InteractiveLemmaAbsenceLlmGenerator desde revision-infrastructure.lageninteractive, inyectandole una SuggestedLemmaQuerySessionFactory (DefaultSuggestedLemmaQuerySessionFactory con port + RefinementPlanStore + AuditReportStore). El generador liga la sesion por-task en generate(context) y el modelo puede solicitar mas suggested lemmas para la misma task durante la generacion (semantica F-CSLATDC) de forma acotada. Modo explicito y opt-in (F-LAGAG-R003); el default observable sigue siendo LLM (single-shot, F-LAGEN-R013) y CANNED queda intacto. StrategyId.name = lemma-absence-llm-interactive."
```
