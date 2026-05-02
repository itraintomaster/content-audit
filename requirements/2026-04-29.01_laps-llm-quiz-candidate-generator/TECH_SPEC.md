---
patch: F-LAGEN
requirement: 2026-04-29.01_laps-llm-quiz-candidate-generator
generated: 2026-04-30T19:30:00Z
---

# Tech Spec: FEAT-LAGEN — Generador de candidatos respaldado por LLM para LEMMA_ABSENCE

## Renombrar `revision-domain.strategy` a `revision-domain.lemmaabsence`
El package previo se llamaba con el nombre de un patrón GoF (`strategy`) en lugar de describir el sub-dominio que encapsula. Como hoy alberga exclusivamente piezas LEMMA_ABSENCE-específicas — el port `LemmaAbsenceQuizCandidateGenerator`, la estrategia MVP, el carrier `LemmaAbsenceGeneratorResponse` y el generador canned — el nombre se reemplaza por `lemmaabsence`, siguiendo la convención del módulo hermano `audit-domain` (`coca`, `lrec`, `labs`). Aprovechamos la misma ventana de cambio que abre F-LAGEN (visibility flip + un componente nuevo) para que el churn de imports se pague una sola vez. Los cuatro componentes (3 preexistentes en la baseline + 1 que agrega esta feature: `CannedLemmaAbsenceQuizCandidateGenerator`) se relocalizan en bloque al nuevo package, todos con `visibility: public` (cierra el flip que originalmente motivó el cambio: el composition root y `revision-infrastructure` necesitan instanciar tanto la estrategia MVP como el generador canned).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: lemmaabsence
        _change: modify
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
        _change: modify
        implementations:
          - name: CannedLemmaAbsenceQuizCandidateGenerator
            _change: add
            visibility: public
            implements: ["LemmaAbsenceQuizCandidateGenerator"]
            requiresInject:
              - { name: cannedQuizSentence, type: String }
              - { name: cannedTranslation, type: String }
```

## Mover `LlmGenerationFailureCategory` a `revision-infrastructure.lagen`
La auditoría de superficie reveló que el único consumidor real de la enum es `LangChainErrorClassifier` (en `revision-infrastructure.lagenopenai`): el adapter clasifica el `Throwable` en una categoría y antepone el nombre de esa categoría como prefijo del `String reason` que viaja en `ProposalStrategyFailedException`. **Cero consumidores en `revision-domain`** — ningún port ni modelo de dominio menciona la enum, y `ProposalStrategyFailedException.reason` sigue siendo un `String` libre por diseño (FEAT-LAPS R015). Mover la enum a `revision-infrastructure.lagen` co-localiza el vocabulario con el factory que clasifica errores, deja `revision-domain` libre de vocabulario LLM, y mantiene la enum visible para el package interno `lagenopenai` (mismo módulo) y para futuros consumidores del CLI sin cruzar dominios.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lagen
        _change: modify
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

## Crear `revision-infrastructure` como módulo adapter LangChain4j
El adapter LLM es una preocupación de infraestructura que depende de `revision-domain` (el port) y `refiner-domain` (el carrier `LemmaAbsenceCorrectionContext` que consume). Siguiendo la convención `*-infrastructure` del proyecto, lo alojamos en su propio módulo para que los módulos de dominio nunca vean LangChain4j. `allowedClients: [audit-cli]` (P8 Qualified Export) impone que este módulo sea un detalle de implementación del composition root del CLI, y nada más — ningún otro módulo puede depender de él, descartando acoplamientos cross-application accidentales. El módulo declara los dos aliases de LangChain4j (`langchain4j-openai` para el cliente OpenAI-compatible + builder, `langchain4j-core` para los tipos de chat-message y response que consume el API fluent legacy).

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    description: "Infrastructure adapter for the revision phase."
    dependsOn: ["revision-domain", "refiner-domain"]
    allowedClients: ["audit-cli"]
    uses: ["langchain4j-openai", "langchain4j-core"]
```

## Exponer un único Factory Seam en `revision-infrastructure.lagen`
El composition root tiene que construir un `LemmaAbsenceQuizCandidateGenerator` totalmente cableado en una sola llamada, y además poblar `StrategyId.providerId` (F-LAGEN-R009) desde la misma configuración. Exponemos dos operaciones en el factory: `create(LagenConfig)` devuelve el port runtime, `providerIdFor(LagenConfig)` devuelve el identificador `provider:model` correspondiente para que `audit-cli.bootstrap` pueda estampar la estrategia sin re-parsear la config. Ambos viven en el package público `lagen` junto con el carrier `LagenConfig`; el adapter y sus colaboradores se quedan ocultos en el package internal hermano.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lagen
        _change: modify
        visibility: public
        interfaces:
          - name: LemmaAbsenceLlmGeneratorFactory
            _change: modify
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
    _change: modify
    packages:
      - name: lagen
        _change: modify
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

## Contrato cerrado: defaults numéricos del factory materializados en `LagenDefaults`
QA flagged que las firmas de tres tests del factory (`temperature`, `maxTokens`, `timeout` cuando los campos correspondientes de `LagenConfig` son null) decían "non-zero" / "sensible default" / "default timeout" sin pinear el número. Cierro el contrato con valores explícitos y los materializo como un record nominado `LagenDefaults` en el package `lagen`, no como literales escondidos dentro del factory. Razones: (1) los tests pueden asertar igualdad contra `LagenDefaults` (un getter por campo) en lugar de re-tipear `0.7` (acopla el test a la decisión, no al literal); (2) cualquier futura iteración que quiera re-pinear los defaults toca un único punto observable; (3) `LagenDefaults` queda público por la misma razón que `LagenConfig`: es contrato cross-module que `audit-cli` y los tests pueden leer. Los valores elegidos son `temperature=0.7` (cumple Asunción 4: no-cero / no-determinístico, alineado con la práctica usual de LangChain4j para chat), `maxTokens=2048` (suficiente para una respuesta JSON con un `quizSentence` cloze + traducción al español sin truncar), `timeout=Duration.ofSeconds(30)` (alineado con el ejemplo `LLM_TIMEOUT: deadline 30s exceeded` de REQUIREMENT.md R006). El developer DEBE inicializar la única instancia de `LagenDefaults` con esos valores exactos; el QA puede leer los getters y asertar igualdad.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lagen
        _change: modify
        models:
          - name: LagenDefaults
            _change: add
            type: record
            visibility: public
            fields:
              - { name: temperature, type: double }
              - { name: maxTokens, type: int }
              - { name: timeout, type: Duration }
```

## Contrato cerrado: formato de `providerId` y rechazo ante `:` embebido
QA flagged que la signature `providerIdFor(LagenConfig): String` no declaraba el formato del retorno ni el comportamiento ante caracteres ambiguos. Cierro el contrato así: el formato canónico es `providerName:modelId` con `:` literal como separador (ya pinneado en decisions.md línea 27); el factory **no** normaliza case ni whitespace (`LMStudio:gemma-3-4b-IT` y `lmstudio:gemma-3-4b-it` producen providerIds distintos — el operador es responsable de la consistencia, igual que con cualquier otra env-var); el factory **rechaza con `InvalidProviderIdException`** si `providerName` o `modelId` contienen `:` ellos mismos, porque eso rompe el round-trip de la trazabilidad (un consumidor del artefacto archivado no podría re-parsear `lmstudio:gemma:7b` sin ambigüedad). La excepción carga ambos campos para que el mensaje al operador sea accionable. La validación está en el factory (no en el constructor de `LagenConfig`) porque `LagenConfig` puede tener otros usos donde el `:` no sea relevante; es la concatenación la que introduce la restricción.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lagen
        _change: modify
        models:
          - name: InvalidProviderIdException
            _change: add
            type: exception
            extends: RuntimeException
            visibility: public
            message: "providerName and modelId must not contain ':' (got providerName='%s', modelId='%s'). The ':' is reserved as the providerId separator (provider:model)."
            fields:
              - { name: providerName, type: String }
              - { name: modelId, type: String }
```

## Contrato cerrado: tabla de mapeo de `DefaultLangChainErrorClassifier.classify`
QA flagged que la signature `classify(Throwable cause): LlmGenerationFailureCategory` no declaraba qué Throwables corresponden a qué categoría. Cierro el contrato así, en orden de evaluación (el classifier itera la cadena de causas con `Throwable.getCause()` y devuelve la primera categoría matcheada; si nada matchea devuelve `LLM_OTHER`):

| Detector | Categoría | Justificación |
|---|---|---|
| Hay `java.net.ConnectException`, `java.net.UnknownHostException` o `java.net.NoRouteToHostException` en la cadena de causas | `LLM_UNREACHABLE` | Comunicación TCP/DNS no establecida — no hay forma de que el proveedor haya recibido la consulta. |
| Hay `java.net.SocketTimeoutException` o `java.util.concurrent.TimeoutException` en la cadena de causas | `LLM_TIMEOUT` | Conexión establecida pero deadline agotado antes de respuesta — coincide con la categoría que F-LAGEN-R008 menciona como observable. |
| La excepción raíz es `dev.ai4j.openai4j.OpenAiHttpException` (o subtipo) y su `statusCode()` es `401` o `403` | `LLM_AUTH_FAILED` | LangChain4j legacy expone el status HTTP del proveedor por esta excepción cuando usa el cliente OpenAI-compatible. |
| El `getMessage()` de la excepción raíz matchea `(?i)\b(unauthorized\|forbidden\|invalid api key\|401\|403)\b` | `LLM_AUTH_FAILED` | Heurística de fallback para proveedores que NO emiten `OpenAiHttpException` (p.ej. wrappers cloud que envuelven el error con su propia clase). Necesario porque LangChain4j 0.36.2 no estandariza un `AuthException` cross-provider. |
| Cualquier otro `Throwable` no-null | `LLM_OTHER` | Catch-all explícito; nunca devuelve null para una entrada no-null. |

Tres decisiones intencionales sobre las que QA preguntó: (1) **matching por tipo de Throwable, no por HTTP status genérico** — porque el legacy `generate(List<ChatMessage>)` no expone uniformemente el status; sólo `OpenAiHttpException` lo hace explícito y eso queda como detector preferente con la heurística regex como fallback. (2) `LLM_RESPONSE_EMPTY` y `LLM_RESPONSE_MALFORMED` **no aparecen en este classifier** — los emite el `LemmaAbsenceResponseParser` sobre el texto crudo del modelo antes de llegar acá, confirmado por la firma `parse(LemmaAbsenceLlmRawResponse, String, String): LemmaAbsenceGeneratorResponse`. (3) **No agrego un modelo `Map<Class<? extends Throwable>, LlmGenerationFailureCategory>`** porque la lógica del classifier no es una tabla pura: incluye iteración sobre la cadena de causas, inspección de un statusCode tipado, y matching regex sobre el message — un map estático no representaría fielmente el algoritmo.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lagenopenai
        _change: modify
        interfaces:
          - name: LangChainErrorClassifier
            _change: add
            stereotype: service
            visibility: internal
            exposes:
              - signature: "classify(Throwable cause): LlmGenerationFailureCategory"
```

## Ocultar el engine LangChain4j en `revision-infrastructure.lagenopenai`
El adapter es un grafo de colaboradores (cableado del cliente HTTP, template de prompt, validación de forma JSON, clasificación de errores) — exactamente la situación que pide P2: encapsular dentro de un package cuyo único seam público es el factory implementation. Partir el armado de prompt, el parseo de respuesta y la clasificación de errores en sus propias interfaces internal mantiene cada preocupación testeable de forma aislada (el QA agent va a aseverar F-LAGEN-R005 sobre el parser, las categorías F-LAGEN-R006 sobre el classifier, y F-LAGEN-R003/R004 sobre el prompt builder) sin exponer ninguna de ellas. Solo `DefaultLemmaAbsenceLlmGeneratorFactory` es `visibility: public` — todos los colaboradores son package-private para que la elección de LangChain4j pueda ser cambiada sin romper la superficie cross-module.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lagenopenai
        _change: modify
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
D2 confirmó configuración por env-vars sin archivos de config. El switch de modo (`CONTENT_AUDIT_LAGEN_MODE=llm|canned`) necesita una representación tipada sobre la cual ramifiquen tanto el resolver como el cableado del bootstrap. El default es `LLM` (F-LAGEN-R013); `CANNED` es el opt-in explícito que materializa la Opción B de DOUBT-CANNED-MODE-AVAILABILITY. Lo ubicamos en la raíz del módulo porque tanto el resolver (en el package `bootstrap`) como `Main.java` (en el package `commands`) necesitan referenciarlo.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: LagenMode
        _change: add
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
