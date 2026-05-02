---
patch: F-LAGEN
requirement: 2026-04-29.01_laps-llm-quiz-candidate-generator
generated: 2026-04-30T17:45:00Z
---

# Tech Spec: FEAT-LAGEN — Generador de candidatos respaldado por LLM para LEMMA_ABSENCE

## Renombrar `revision-domain.strategy` a `revision-domain.lemmaabsence`
El package previo se llamaba con el nombre de un patrón GoF (`strategy`) en lugar de describir el sub-dominio que encapsula. Como hoy alberga exclusivamente piezas LEMMA_ABSENCE-específicas — el port `LemmaAbsenceQuizCandidateGenerator`, la estrategia MVP, el carrier `LemmaAbsenceGeneratorResponse` y el generador canned — el nombre se reemplaza por `lemmaabsence`, siguiendo la convención del módulo hermano `audit-domain` (`coca`, `lrec`, `labs`). Aprovechamos la misma ventana de cambio que abre F-LAGEN (visibility flip + un componente nuevo) para que el churn de imports se pague una sola vez. Los cuatro componentes (3 preexistentes en la baseline + 1 que agrega esta feature: `CannedLemmaAbsenceQuizCandidateGenerator`) se relocalizan en bloque al nuevo package, todos con `visibility: public` (cierra el flip que originalmente motivó el cambio: el composition root y `revision-infrastructure` necesitan instanciar tanto la estrategia MVP como el generador canned).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: strategy
        _change: delete
      - name: lemmaabsence
        _change: add
        visibility: public
        models:
          - name: LemmaAbsenceGeneratorResponse
            _change: add
            type: record
            visibility: public
            fields:
              - { name: quizSentence, type: String }
              - { name: translation, type: String }
        interfaces:
          - name: LemmaAbsenceQuizCandidateGenerator
            _change: add
            stereotype: port
            visibility: public
            exposes:
              - signature: "generate(LemmaAbsenceCorrectionContext context): LemmaAbsenceGeneratorResponse"
        implementations:
          - name: LemmaAbsenceMvpStrategy
            _change: add
            visibility: public
            implements: ["LemmaAbsenceProposalStrategy"]
            requiresInject:
              - { name: generator, type: LemmaAbsenceQuizCandidateGenerator }
```

## Mover el generador canned dentro de `revision-domain.lemmaabsence` como `CannedLemmaAbsenceQuizCandidateGenerator`
Hoy el generador canned vive como un lambda inline dentro de `audit-cli/Main.java` — una filtración de las responsabilidades del SPI hacia el composition root, y la razón por la que DOUBT-CANNED-MODE-AVAILABILITY seguía abierta. Materializarlo como una implementación nombrada, pública e inyectable honra la Opción B de esa duda (queda como opt-in explícito, nunca default) y le permite a `audit-cli.bootstrap` cablearlo por el mismo camino factory-style que usa para todas las demás estrategias LAPS. Los parámetros del constructor llevan el contenido canned para que tests y corridas offline elijan su propio fixture sin necesidad de hacer subclases.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: lemmaabsence
        _change: add
        implementations:
          - name: CannedLemmaAbsenceQuizCandidateGenerator
            _change: add
            visibility: public
            implements: ["LemmaAbsenceQuizCandidateGenerator"]
            requiresInject:
              - { name: cannedQuizSentence, type: String }
              - { name: cannedTranslation, type: String }
```

## Auditoría de superficie pública del package `lemmaabsence`
Tras crear el package quedaba la pregunta de si su superficie pública (3 modelos/ports + 2 implementaciones públicas) era estrictamente necesaria o si parte se podía esconder. La auditoría componente-por-componente confirma que cuatro de los cinco son obligatoriamente públicos y el quinto (`LlmGenerationFailureCategory`) no pertenece a este package. **`LemmaAbsenceGeneratorResponse`** es un carrier (P5): aparece como tipo de retorno del port y como valor que `audit-cli.Main.java` y los 5 journey tests de F-LAPS construyen directamente — reducirlo rompe el contrato. **`LemmaAbsenceQuizCandidateGenerator`** es el port hexagonal: lo implementan tanto el generador canned como el adapter LLM en `revision-infrastructure`, lo inyecta `LemmaAbsenceMvpStrategy` y lo construye el composition root como lambda en cada test — no admite reducción. **`LemmaAbsenceMvpStrategy`** y **`CannedLemmaAbsenceQuizCandidateGenerator`** podrían en teoría esconderse detrás de un Factory Seam, pero ambos son construcciones de un solo argumento (la estrategia inyecta el generator; el canned recibe 2 strings) — un factory agregaría +2 componentes (interface + impl) por cada uno para esconder uno solo. La asimetría con el factory LLM (que sí cablea un grafo: HTTP client + prompt builder + parser + classifier) es justificada por asimetría de complejidad real, no falta de simetría arquitectónica. La única reubicación que sí vale la pena es la enum, descrita en la siguiente sección.

## Mover `LlmGenerationFailureCategory` a `revision-infrastructure.lagen`
La auditoría de superficie reveló que el único consumidor real de la enum es `LangChainErrorClassifier` (en `revision-infrastructure.lagenopenai`): el adapter clasifica el `Throwable` en una categoría y antepone el nombre de esa categoría como prefijo del `String reason` que viaja en `ProposalStrategyFailedException`. **Cero consumidores en `revision-domain`** — ningún port ni modelo de dominio menciona la enum, y `ProposalStrategyFailedException.reason` sigue siendo un `String` libre por diseño (FEAT-LAPS R015). El argumento original "co-localizar el vocabulario de fallas con el contrato que modula" no aguanta el scrutinio: la enum no modula ningún contrato del dominio, modula el formato del prefix string de un adapter de infraestructura. Mover la enum a `revision-infrastructure.lagen` co-localiza el vocabulario con el factory que clasifica errores, deja `revision-domain` libre de vocabulario LLM, y mantiene la enum visible para el package interno `lagenopenai` (mismo módulo) y para futuros consumidores del CLI sin cruzar dominios.

```architecture
modules:
  - name: revision-infrastructure
    _change: add
    packages:
      - name: lagen
        _change: add
        models:
          - name: LlmGenerationFailureCategory
            _change: add
            type: enum
            visibility: public
            fields:
              - { name: LLM_UNREACHABLE }
              - { name: LLM_TIMEOUT }
              - { name: LLM_AUTH_FAILED }
              - { name: LLM_RESPONSE_EMPTY }
              - { name: LLM_RESPONSE_MALFORMED }
              - { name: LLM_OTHER }
```

## Dependencias LangChain4j auto-contenidas + decisión de API flavor
Con el bug del formato de patches de Sentinel ya corregido, esta propuesta declara los artefactos LangChain4j que necesita directamente en el bloque `dependencies:` del patch, en vez de pedirle al operador que edite `sentinel.yaml` a mano. Se declaran dos aliases porque los tipos que tocamos están repartidos en dos artefactos: `langchain4j-open-ai:0.36.2` expone solo los tipos `OpenAi*` (el cliente del modelo de chat y su builder), mientras que todo lo que usamos de `dev.langchain4j.data.message` y `dev.langchain4j.model.output` vive en `langchain4j-core:0.36.2` (que `langchain4j-open-ai` ya trae como transitiva). Partir los aliases mantiene cada bloque `provides:` honesto sobre qué JAR exporta los tipos listados — verificado empíricamente inspeccionando ambos JARs con `jar tf` más `javap` — y evita dar la falsa impresión de que `langchain4j-open-ai` exporta `ChatLanguageModel` directamente. Las listas `provides:` están acotadas al **API flavor A** — la superficie fluent legacy `ChatLanguageModel.generate(List<ChatMessage>): Response<AiMessage>` — cerrando la duda lado-arquitecto DOUBT-LANGCHAIN4J-API-FLAVOR. Tres razones llevaron a esa elección: (1) el adapter solo necesita un round-trip con un mensaje de sistema y uno de usuario — el bundleo de parámetros de `ChatRequest` no aporta nada en nuestra forma; (2) el flavor A es la superficie más estable en 0.36.2, y las garantías de structured-output del flavor más nuevo están explícitamente fuera de alcance (REQUIREMENT.md Asunción 6 descarta cualquier necesidad de JSON-mode, function-calling o streaming); (3) mantener angosta la superficie arquitectónica — `ChatMessage`, `Response`, más los tres subtipos de mensaje — evita arrastrar `ChatRequest`/`ChatResponse` al bloque `provides:` del patch, lo que mantiene el contrato de dependencias minimal y más fácil de auditar.

```architecture
modules:
  - name: revision-infrastructure
    _change: add
    uses: ["langchain4j-openai", "langchain4j-core"]
dependencies:
  - alias: langchain4j-openai
    _change: add
    artifact: dev.langchain4j:langchain4j-open-ai:0.36.2
    scope: compile
    provides:
      - { type: OpenAiChatModel, package: dev.langchain4j.model.openai }
      - { type: OpenAiChatModel.OpenAiChatModelBuilder, package: dev.langchain4j.model.openai }
  - alias: langchain4j-core
    _change: add
    artifact: dev.langchain4j:langchain4j-core:0.36.2
    scope: compile
    provides:
      - { type: ChatLanguageModel, package: dev.langchain4j.model.chat }
      - { type: ChatMessage, package: dev.langchain4j.data.message }
      - { type: SystemMessage, package: dev.langchain4j.data.message }
      - { type: UserMessage, package: dev.langchain4j.data.message }
      - { type: AiMessage, package: dev.langchain4j.data.message }
      - { type: Response, package: dev.langchain4j.model.output }
```

## Crear `revision-infrastructure` como módulo adapter LangChain4j
El adapter LLM es una preocupación de infraestructura que depende de `revision-domain` (el port) y `refiner-domain` (el carrier `LemmaAbsenceCorrectionContext` que consume). Siguiendo la convención `*-infrastructure` del proyecto, lo alojamos en su propio módulo para que los módulos de dominio nunca vean LangChain4j. `allowedClients: [audit-cli]` (P8 Qualified Export) impone que este módulo sea un detalle de implementación del composition root del CLI, y nada más — ningún otro módulo puede depender de él, descartando acoplamientos cross-application accidentales. El módulo declara los dos aliases de LangChain4j (`langchain4j-openai` para el cliente OpenAI-compatible + builder, `langchain4j-core` para los tipos de chat-message y response que consume el API fluent legacy).

```architecture
modules:
  - name: revision-infrastructure
    _change: add
    description: "Infrastructure adapter for the revision phase. Provides the LLM-backed implementation of LemmaAbsenceQuizCandidateGenerator (revision-domain.lemmaabsence port) using LangChain4j against any OpenAI-compatible HTTP endpoint (LM Studio, vLLM, OpenAI cloud, Ollama via openai compat, etc.). Exposes a single Factory Seam (LemmaAbsenceLlmGeneratorFactory + LagenConfig carrier + LlmGenerationFailureCategory enum) so the composition root wires the adapter with one call. The adapter uses LangChain4j's legacy chat-message API flavor, generate(List<ChatMessage>), because F-LAGEN needs explicit system/user messages and a simple text response without exposing ChatRequest/ChatResponse in the architectural surface. The adapter, prompt builder, response parser and error classifier all live in an internal package; only the factory class is public. allowedClients=[audit-cli] enforces that this module is an implementation detail of the CLI composition root only (P8 Qualified Export)."
    dependsOn: ["revision-domain", "refiner-domain"]
    allowedClients: ["audit-cli"]
    uses: ["langchain4j-openai", "langchain4j-core"]
```

## Exponer un único Factory Seam en `revision-infrastructure.lagen`
El composition root tiene que construir un `LemmaAbsenceQuizCandidateGenerator` totalmente cableado en una sola llamada, y además poblar `StrategyId.providerId` (F-LAGEN-R009) desde la misma configuración. Exponemos dos operaciones en el factory: `create(LagenConfig)` devuelve el port runtime, `providerIdFor(LagenConfig)` devuelve el identificador `provider:model` correspondiente (D6) para que `audit-cli.bootstrap` pueda estampar la estrategia sin re-parsear la config. Ambos viven en el package público `lagen` junto con el carrier `LagenConfig`; el adapter y sus colaboradores se quedan ocultos en el package internal hermano.

```architecture
modules:
  - name: revision-infrastructure
    _change: add
    packages:
      - name: lagen
        _change: add
        visibility: public
        interfaces:
          - name: LemmaAbsenceLlmGeneratorFactory
            _change: add
            stereotype: factory
            visibility: public
            exposes:
              - signature: "create(LagenConfig config): LemmaAbsenceQuizCandidateGenerator"
              - signature: "providerIdFor(LagenConfig config): String"
    patterns:
      - type: Factory
        interface: LemmaAbsenceLlmGeneratorFactory
        implementations: ["DefaultLemmaAbsenceLlmGeneratorFactory"]
```

## Definir `LagenConfig` como Config Record carrier público
F-LAGEN-R008 lista siete perillas que el operador debe poder ajustar sin recompilar. El factory las recibe como un único record para que la superficie cross-module siga siendo un solo tipo; los campos requeridos (provider, model, endpoint) son no-null, las perillas opcionales (apiKey, temperature, maxTokens, timeout) son nullable para que el factory pueda sustituir defaults. `apiKey` es nullable para honrar la Asunción 3 (el default local no requiere credenciales); `temperature` es nullable para que el factory pueda aplicar un default no-cero (Asunción 4) sin obligar a cada caller a elegir uno.

```architecture
modules:
  - name: revision-infrastructure
    _change: add
    packages:
      - name: lagen
        _change: add
        models:
          - name: LagenConfig
            _change: add
            type: record
            visibility: public
            fields:
              - { name: providerName, type: String }
              - { name: modelId, type: String }
              - { name: endpoint, type: String }
              - { name: apiKey, type: String }
              - { name: temperature, type: Double }
              - { name: maxTokens, type: Integer }
              - { name: timeout, type: Duration }
```

## Ocultar el engine LangChain4j en `revision-infrastructure.lagenopenai`
El adapter es un grafo de colaboradores (cableado del cliente HTTP, template de prompt, validación de forma JSON, clasificación de errores) — exactamente la situación que pide P2: encapsular dentro de un package cuyo único seam público es el factory implementation. Partir el armado de prompt, el parseo de respuesta y la clasificación de errores en sus propias interfaces internal mantiene cada preocupación testeable de forma aislada (el QA agent va a aseverar F-LAGEN-R005 sobre el parser, las categorías F-LAGEN-R006 sobre el classifier, y F-LAGEN-R003/R004 sobre el prompt builder) sin exponer ninguna de ellas. Solo `DefaultLemmaAbsenceLlmGeneratorFactory` es `visibility: public` — todos los colaboradores son package-private para que la elección de LangChain4j pueda ser cambiada sin romper la superficie cross-module.

```architecture
modules:
  - name: revision-infrastructure
    _change: add
    packages:
      - name: lagenopenai
        _change: add
        visibility: internal
        models:
          - name: LemmaAbsenceLlmRawResponse
            _change: add
            type: record
            fields:
              - { name: rawText, type: String }
        interfaces:
          - name: LemmaAbsencePromptBuilder
            _change: add
            stereotype: service
            visibility: internal
            exposes:
              - signature: "buildSystemPrompt(): String"
              - signature: "buildUserPrompt(LemmaAbsenceCorrectionContext context): String"
          - name: LemmaAbsenceResponseParser
            _change: add
            stereotype: service
            visibility: internal
            exposes:
              - signature: "parse(LemmaAbsenceLlmRawResponse raw, String taskId, String strategyName): LemmaAbsenceGeneratorResponse"
          - name: LangChainErrorClassifier
            _change: add
            stereotype: service
            visibility: internal
            exposes:
              - signature: "classify(Throwable cause): LlmGenerationFailureCategory"
        implementations:
          - name: DefaultLemmaAbsenceLlmGeneratorFactory
            _change: add
            visibility: public
            implements: ["LemmaAbsenceLlmGeneratorFactory"]
          - name: LemmaAbsenceLlmGenerator
            _change: add
            implements: ["LemmaAbsenceQuizCandidateGenerator"]
            requiresInject:
              - { name: config, type: LagenConfig }
              - { name: promptBuilder, type: LemmaAbsencePromptBuilder }
              - { name: responseParser, type: LemmaAbsenceResponseParser }
              - { name: errorClassifier, type: LangChainErrorClassifier }
              - { name: strategyName, type: String }
          - name: DefaultLemmaAbsencePromptBuilder
            _change: add
            implements: ["LemmaAbsencePromptBuilder"]
          - name: DefaultLemmaAbsenceResponseParser
            _change: add
            implements: ["LemmaAbsenceResponseParser"]
          - name: DefaultLangChainErrorClassifier
            _change: add
            implements: ["LangChainErrorClassifier"]
```

## Cablear `audit-cli` para depender de `revision-infrastructure`
El composition root del CLI es el único módulo legalmente autorizado para instanciar factories concretos desde módulos de infraestructura (P4). Agregar `revision-infrastructure` a `audit-cli.dependsOn` hace al nuevo factory alcanzable desde `Main.java`; la declaración `allowedClients: [audit-cli]` sobre `revision-infrastructure` mantiene a todo el resto fuera del cuadro.

```architecture
modules:
  - name: audit-cli
    _change: modify
    dependsOn: ["revision-infrastructure"]
```

## Agregar enum `LagenMode` a `audit-cli`
D2 confirmó configuración por env-vars sin archivos de config. El switch de modo (`CONTENT_AUDIT_LAGEN_MODE=llm|canned`) necesita una representación tipada sobre la cual ramifiquen tanto el resolver como el cableado del bootstrap. El default es `LLM` (F-LAGEN-R002); `CANNED` es el opt-in explícito que materializa la Opción B de DOUBT-CANNED-MODE-AVAILABILITY. Lo ubicamos en la raíz del módulo porque tanto el resolver (en el package `bootstrap`) como `Main.java` (en el package `commands`) necesitan referenciarlo.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: LagenMode
        _change: modify
        type: enum
        visibility: public
        fields:
          - { name: LLM }
          - { name: CANNED }
```

## Agregar `LagenModeResolver` a `audit-cli.bootstrap`
Espeja el patrón existente de `ApprovalModeResolver` / `ProposalStrategySelector` en el mismo package: un port sealed + una implementación default que convierte una sola string de env-var en la enum tipada, lanzando una excepción tipada ante input inválido. Sellar el port prohíbe terceras implementaciones accidentales y le da al test writer un set exhaustivo de casos.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        models:
          - name: InvalidLagenModeException
            _change: add
            type: exception
            extends: RuntimeException
            message: "Invalid value for CONTENT_AUDIT_LAGEN_MODE: '%s'. Allowed: llm, canned"
            fields:
              - { name: value, type: String }
        interfaces:
          - name: LagenModeResolver
            _change: add
            stereotype: port
            sealed: true
            exposes:
              - signature: "resolve(String envValue): LagenMode"
        implementations:
          - name: DefaultLagenModeResolver
            _change: add
            visibility: public
            implements: ["LagenModeResolver"]
```

## Agregar `LagenConfigResolver` a `audit-cli.bootstrap`
Parsea las env-vars `CONTENT_AUDIT_LAGEN_*` (provider, model, endpoint, apiKey, temperature, maxTokens, timeout) hacia un carrier `LagenConfig`. Pasamos el mapa de env como un argumento explícito `Map<String,String>` en lugar de leer `System.getenv()` directamente para que el resolver sea unit-testable de forma aislada. La `InvalidLagenConfigException` lleva tanto el nombre de la env-var ofensora como un detalle del error de parseo, así el CLI puede imprimir un mensaje accionable y salir con código distinto de cero antes de que se intente cualquier round-trip al LLM.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        models:
          - name: InvalidLagenConfigException
            _change: add
            type: exception
            extends: RuntimeException
            message: "Invalid LAGEN configuration (%s): %s"
            fields:
              - { name: key, type: String }
              - { name: detail, type: String }
        interfaces:
          - name: LagenConfigResolver
            _change: add
            stereotype: port
            sealed: true
            exposes:
              - signature: "resolve(Map<String,String> env): LagenConfig"
        implementations:
          - name: DefaultLagenConfigResolver
            _change: add
            visibility: public
            implements: ["LagenConfigResolver"]
```
