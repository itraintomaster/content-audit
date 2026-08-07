---
patch: ARCH-LAGEN-R015
requirement: 2026-04-29.01_laps-llm-quiz-candidate-generator
generated: 2026-08-04T23:59:00Z
---

# Tech Spec: FEAT-LAGEN — Generador de candidatos respaldado por modelo de lenguaje

> **Como leer este documento.** Las secciones con fence describen `ARCH-LAGEN-R015`, el cambio pendiente. Antes va, en prosa, el razonamiento del diseño original de abril de 2026: sus fences ya no validan contra el patch vigente, asi que se conserva el porque y se deja el detalle de contratos —que se lee en `sentinel.yaml`— afuera. Al final se marca que reemplazo FEAT-LASAG.

## Registro del diseño original (abril 2026)

**Nombrar el sub-dominio, no el patron.** El package de estrategias se llamaba `strategy` —un nombre de patron GoF— y paso a `lemmaabsence`, siguiendo la convencion de los packages hermanos de `audit-domain` (`coca`, `lrec`, `labs`). El renombre se hizo en la misma ventana que el resto del cambio para pagar el churn de imports una sola vez.

**Materializar el generador canned.** Vivia como lambda inline en `Main.java`, una filtracion del SPI hacia el composition root y la razon por la que DOUBT-CANNED-MODE-AVAILABILITY seguia abierta. Como implementacion nombrada e inyectable honra la Opcion B de esa duda —opt-in explicito, nunca default— y recibe su contenido por constructor para que tests y corridas offline elijan su fixture sin subclasear.

**Ubicar el vocabulario de fallas donde se usa.** `LlmGenerationFailureCategory` no tenia **ningun** consumidor en `revision-domain`: ningun port la menciona y `ProposalStrategyFailedException.reason` es un String libre por diseño. Se movio a `revision-infrastructure`, junto al codigo que clasifica errores. El argumento original —co-localizar el vocabulario de fallas con el contrato que modula— no aguanto escrutinio porque la enum no modulaba ningun contrato del dominio.

**Aislar el adapter en su propio modulo.** `revision-infrastructure` nacio para que ningun modulo de dominio viera la libreria del proveedor, con `allowedClients: [audit-cli]` (P8, Qualified Export): es un detalle de implementacion del composition root y de nadie mas.

**Un unico factory seam, con dos operaciones.** `create(LagenConfig)` devuelve el port runtime y `providerIdFor(LagenConfig)` devuelve el identificador `provider:model`, para que el bootstrap estampe la trazabilidad de F-LAGEN-R009 sin re-parsear la configuracion. El engine quedo detras de un package interno (P2): un grafo de colaboradores —prompt, parseo, clasificacion de errores— cada uno testeable por separado y ninguno visible afuera.

**Config Record con perillas nullable.** `LagenConfig` reune las siete perillas de F-LAGEN-R008 en un solo tipo cross-module. Las opcionales son nullable para que el factory sustituya defaults sin obligar a cada caller a elegir uno, y esos defaults se materializaron como `LagenDefaults` en vez de literales escondidos: los tests asertan contra el tipo, no re-tipean el numero, y re-pinearlos toca un solo punto observable.

**Formato de `providerId` cerrado.** `providerName:modelId` literal, sin normalizar case ni whitespace, y rechazo con `InvalidProviderIdException` si alguno de los dos trae `:`, porque eso rompe el round-trip de la trazabilidad: nadie podria re-parsear `lmstudio:gemma:7b` sin ambiguedad. La validacion vive en el factory y no en el constructor del carrier porque es la concatenacion la que introduce la restriccion.

**Modo por env-var, con enum tipada.** `LagenMode` en la raiz del modulo porque lo referencian tanto el resolver (en `bootstrap`) como `Main` (en `commands`). `LLM` por defecto, `CANNED` como opt-in explicito (F-LAGEN-R013). `LagenConfigResolver` interpreta la configuracion en una sola pasada y falla antes de tocar el curso (F-LAGEN-R014).

**Lo que reemplazo FEAT-LASAG.** El engine descrito arriba —el package `lagenopenai` con su prompt builder, su parser, su clasificador de errores y su factory sobre la libreria de chat— fue sustituido por `lemmaabsenceagent`, respaldado por el runtime de agentes declarativos. El razonamiento de encapsulamiento sigue valiendo y la forma se conservo; lo que cambio es el backend. Por eso este documento no reproduce la tabla de mapeo de excepciones a categorias de falla que cerraba el contrato de `DefaultLangChainErrorClassifier`: esa clase ya no existe, y su equivalente vigente es `DefaultAgentRuntimeErrorClassifier`, cuyo contrato vive en FEAT-LASAG.

## Promover el criterio de validez a `agent-runtime-infrastructure`

F-LAGEN-R015 exige que la generacion juzgue su configuracion contra el proveedor declarado **con el mismo criterio** que ya aplica la validacion de consigna, y el analista lo escribio como cita a F-QINST-R016 en vez de repetirlo. La arquitectura tiene que honrar eso: una sola implementacion, no dos que puedan divergir.

El criterio se muda a `agent-runtime-infrastructure` por tres razones que ninguno de los dos consumidores puede ofrecer. Ese modulo ya envuelve la libreria de agentes, que es donde vive el conocimiento sobre clases de proveedor. No depende de nadie, asi que ningun consumidor invierte una dependencia para llegar. Y sus `allowedClients` ya son exactamente los tres modulos que lo necesitan, con lo cual no hubo que tocar ninguna frontera.

La implementacion no se esconde detras de un factory: construirla es trivial y no tiene colaboradores ni grafo que encapsular, asi que un seam seria un componente de mas para ocultar cero.

```architecture
modules:
  - name: agent-runtime-infrastructure
    _change: modify
    packages:
      - name: llmprovider
        _change: add
        visibility: public
        models:
          - name: LlmProviderClass
            _change: add
            type: enum
            fields:
              - { name: LOCAL_SESSION, _change: add }
              - { name: REMOTE_SERVICE, _change: add }
          - name: LlmProviderRejectionKind
            _change: add
            type: enum
            fields:
              - { name: UNSUPPORTED_PROVIDER, _change: add }
              - { name: MISSING_REQUIRED_INPUT, _change: add }
          - name: LlmProviderResolved
            _change: add
            type: record
            implements: ["LlmProviderResolution"]
            fields:
              - { name: providerName, type: String }
              - { name: providerClass, type: LlmProviderClass }
          - name: LlmProviderRejected
            _change: add
            type: record
            implements: ["LlmProviderResolution"]
            fields:
              - { name: providerName, type: String }
              - { name: missingInput, type: String }
              - { name: kind, type: LlmProviderRejectionKind }
        interfaces:
          - name: LlmProviderResolution
            _change: add
            stereotype: port
        implementations:
          - name: DefaultLlmProviderResolver
            _change: add
            visibility: public
            implements: ["LlmProviderResolver"]
```

## Recibir el nombre del dato faltante en vez de fijarlo

El criterio no puede hardcodear como se llama la direccion de servicio. El juez la declara `baseUrl` y la generacion `endpoint`, y F-LAGEN-R015 invariante 3 exige que el reporte nombre el dato faltante **con el mismo nombre con el que el operador lo declara**. Hoy `DefaultQuizInstructionJudgeProviderResolver` lo tiene fijo en una constante; mudarlo asi haria que el criterio mintiera para uno de los dos consumidores, y ese nombre es lo unico que vuelve accionable el error.

Por eso la firma recibe los dos datos que el operador declara mas la etiqueta con la que ese consumidor los llama. Todo lo demas —que clase es cada proveedor, que direccion conoce el sistema de antemano— es conocimiento de la libreria, no del llamador.

```architecture
modules:
  - name: agent-runtime-infrastructure
    _change: modify
    packages:
      - name: llmprovider
        _change: modify
        interfaces:
          - name: LlmProviderResolver
            _change: add
            stereotype: port
            exposes:
              - signature: "resolve(String declaredProviderName, String declaredServiceAddress, String serviceAddressInputName): LlmProviderResolution"
```

## Retirar el vocabulario propio del juez en vez de adaptarlo

`JudgeProviderClass` no es un tipo auxiliar: **es** el criterio, porque sus dos valores enuncian que exige cada clase de proveedor. Dejarlo al lado de `LlmProviderClass` habria creado el segundo enunciado que puede divergir —una clase de proveedor nueva habria que agregarla en los dos—, que es exactamente lo que R015 vino a eliminar. Un adaptador entre cinco tipos y sus cinco espejos habria sido peso cuyo unico trabajo es renombrar.

El juez conserva su invariante mas fuerte: `QuizInstructionAgentJudge` solo se puede construir con un proveedor ya resuelto, asi que la invariante 3 de F-QINST-R016 —con configuracion invalida no se emite ni una sola consulta— sigue garantizada por construccion y no por una guarda que alguien pueda olvidar. Lo unico que cambia es de quien es el tipo que lo garantiza.

```architecture
modules:
  - name: quiz-instruction-infrastructure
    _change: modify
    packages:
      - name: instructionjudge
        _change: modify
        models:
          - { name: JudgeProviderClass, _change: delete }
          - { name: JudgeProviderRejectionKind, _change: delete }
          - { name: JudgeProviderResolved, _change: delete }
          - { name: JudgeProviderRejected, _change: delete }
        interfaces:
          - { name: JudgeProviderResolution, _change: delete }
          - { name: QuizInstructionJudgeProviderResolver, _change: delete }
          - name: QuizInstructionJudgeVersionResolver
            _change: modify
            exposes:
              - signature: "resolve(QuizInstructionJudgeConfig config,JudgeProviderResolved provider): Optional<String>"
                _change: delete
              - signature: "resolve(QuizInstructionJudgeConfig config,LlmProviderResolved provider): Optional<String>"
                _change: add
          - name: JudgeChatModelFactory
            _change: modify
            exposes:
              - signature: "create(QuizInstructionJudgeConfig config,JudgeProviderResolved provider): ChatModel"
                _change: delete
              - signature: "create(QuizInstructionJudgeConfig config,LlmProviderResolved provider): ChatModel"
                _change: add
        implementations:
          - { name: DefaultQuizInstructionJudgeProviderResolver, _change: delete }
          - name: QuizInstructionAgentJudge
            _change: modify
            requiresInject:
              - { name: provider, type: LlmProviderResolved }
          - name: DefaultQuizInstructionJudgeFactory
            _change: modify
            requiresInject:
              - { name: llmProviderResolver, type: LlmProviderResolver }
```

## Volver opcional la direccion de servicio en la configuracion de generacion

El campo se declaraba requerido, y esa es la exigencia que R015 corrige: para un proveedor que se autentica con la sesion local no existe ninguna direccion que declarar, asi que la unica forma de correr era inventar un valor de relleno. Un dato obligatorio que puede tomar un valor falso sin consecuencia —verificado sobre una corrida real con `http://localhost:7400` inexistente— no es un requisito.

Declararla igualmente para un proveedor que no la usa sigue siendo valido y se ignora en silencio: hoy toda instalacion que corre con esa clase de proveedor lleva un valor de relleno puesto unicamente porque el sistema lo exigia, y rechazarlo romperia instalaciones que funcionan.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: lagen
        _change: modify
        models:
          - name: LagenConfig
            _change: modify
            fields:
              - { name: endpoint, type: String, _change: modify }
```

## Reportar los dos rechazos con dos excepciones distintas

R015 invariante 3 exige que el reporte nombre el dato que falta, el proveedor que lo necesita y ofrezca el modo de generacion fija como alternativa. La plantilla de mensaje de una excepcion no puede ramificar, y los dos rechazos informan cosas distintas: cuando el proveedor no es consultable no falta un dato, falta un proveedor. Con una sola excepcion, ese caso imprimiria `falta 'null'`.

Las dos mapean uno a uno contra los dos motivos de rechazo del criterio, asi que el conjunto es exhaustivo por construccion. `InvalidLagenConfigException` queda intacta reportando los valores que no parsean: el reporte accionable de F-LAGEN-R014 no pierde nada, gana un caso que antes caia en un mensaje generico.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        models:
          - name: UnsupportedLagenProviderException
            _change: add
            type: exception
            extends: RuntimeException
            message: "El proveedor '%s' no es uno que el sistema sepa consultar. Declarar uno soportado, o pedir el modo de generacion fija para correr sin ningun proveedor."
            fields:
              - { name: providerName, type: String }
          - name: MissingLagenProviderInputException
            _change: add
            type: exception
            extends: RuntimeException
            message: "No se pudo dirigir la consulta al proveedor '%s': falta '%s'. Declararlo, o pedir el modo de generacion fija para correr sin ningun proveedor."
            fields:
              - { name: providerName, type: String }
              - { name: missingInput, type: String }
```

## Inyectar el criterio en el resolver de configuracion

El punto de aplicacion es uno solo y es este: la configuracion se juzga contra su proveedor en la misma pasada en que se interpreta, sin emitir ninguna consulta, que es lo que F-LAGEN-R014 ya exigia y R015 no relaja. Lo unico que cambia es cual es el conjunto de configuraciones invalidas.

Que el criterio llegue inyectado y no implementado aca es lo que hace que las tres correcciones que consultan un modelo —lemas ausentes, titulos de knowledge y consigna de quiz— se juzguen con el mismo criterio sin que ninguna lo reimplemente: todas consumen esta configuracion, y esta configuracion se valida una sola vez.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        implementations:
          - name: DefaultLagenConfigResolver
            _change: modify
            requiresInject:
              - { name: llmProviderResolver, type: LlmProviderResolver }
```

## Lo que este cambio no re-decide

Tres cosas quedan citadas y no re-enunciadas, porque ya estan decididas en F-QINST-R016 y re-litigarlas seria volver a crear el problema que R015 cierra.

La **credencial** nunca es requisito de disponibilidad: su ausencia no impide dirigir la consulta, y si el servicio la exige el rechazo llega como falla en tiempo de consulta, no como configuracion invalida. El **identificador del modelo** sigue exigible para toda clase de proveedor, porque no es un dato de conexion sino la identidad de lo que produce el contenido, que la trazabilidad de cada propuesta necesita. Y el **proveedor por defecto** cuando no se declara ninguno sigue siendo la duda abierta compartida, con su asuncion vigente; cualquiera sea la respuesta, no cambia el enunciado de R015 ni esta arquitectura.
