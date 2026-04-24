---
patch: ARCH-LAPS
requirement: 2026-04-20.02_lemma-absence-proposal-strategy
generated: 2026-04-23T00:00:00Z
---

# Tech Spec: Primera estrategia real de propuesta para LEMMA_ABSENCE

FEAT-LAPS es el primer cambio real de contenido de la pipeline. Conceptualmente enchufa una estrategia de propuesta en el punto de extension `Reviser` de FEAT-REVBYP, reutiliza el flujo de validacion/persistencia/aplicacion de FEAT-REVBYP y FEAT-REVAPR sin cambios, y consume el `QuizSentenceConverter` que FEAT-QSENT acaba de publicar en `course-domain`. La arquitectura esta acotada deliberadamente a LEMMA_ABSENCE (YAGNI); la generalizacion a otros `DiagnosisKind` se hara cuando aparezca el segundo. El patch tiene 14 altas y 6 modificaciones: un port de estrategia, un registry, un deriver deterministico, dos carriers (candidate + identity), dos excepciones, un branch nuevo en el dispatcher, dos campos nuevos en `RevisionEngineConfig`, dos outcomes, y un selector CLI para elegir la estrategia activa por variable de entorno.

## Declarar `LemmaAbsenceProposalStrategy` como port de la estrategia (no un port generico)

R004 exige un punto de extension "estrategia de propuesta", R007 fija el input (`CorrectionContext` de FEAT-RCLA), R009/R011 fijan el output (candidato estructurado). Una version polimorfica `ProposalStrategy<C extends CorrectionContext, R>` generalizaria a priori todos los `DiagnosisKind`, pero esta iteracion trata estrictamente uno. P3 ("versatility on demand") descarta la generalizacion especulativa: el port es explicitamente sobre `LemmaAbsenceCorrectionContext` y `LemmaAbsenceQuizCandidate`. Cuando entre el segundo `DiagnosisKind` con estrategia propia, el refactor a un port generico sera evidente y guiado por evidencia, no por adivinacion. No se declara `sealed`: R004 quiere admitir multiples estrategias registradas (MVP hoy, proveedores alternativos manana) sin un permits que obligue a modificar el port.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: LemmaAbsenceProposalStrategy
        _change: add
        stereotype: port
        exposes:
          - signature: "id(): StrategyId"
          - signature: "handles(DiagnosisKind kind): boolean"
          - signature: "propose(RefinementTask task, LemmaAbsenceCorrectionContext context): LemmaAbsenceQuizCandidate"
            throws: [ProposalStrategyFailedException]
```

## `StrategyId` como carrier de identidad (resuelve DOUBT-STRATEGY-METADATA)

R005 obliga a persistir la identidad de la estrategia en cada propuesta. El minimo son `name` + `version`. El doubt pregunta que mas conviene guardar; el caso pragmatico es un `providerId` opaco (por ejemplo `"llm:anthropic"`, `"fixture:golden"`) que distingue al *origen efectivo* del candidato sin comprometerse con una taxonomia de proveedores. Quedan fuera (por ahora) prompt-hashes y duraciones: se pueden agregar luego como campos nullables sin romper wire-compat. El record es publico al root del modulo porque atraviesa `RevisionProposal` (persistido) y `LemmaAbsenceProposalStrategy.id()` (leido por el dispatcher y el selector CLI).

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: StrategyId
        _change: add
        type: record
        fields:
          - { name: name, type: String, description: "Symbolic identifier, e.g. 'lemma-absence-mvp' (F-LAPS-R005)." }
          - { name: version, type: String, description: "Version string, e.g. '1.0.0'; distinguishes revisions of the same name over time (F-LAPS-R005)." }
          - { name: providerId, type: String, description: "Opaque provider id for the underlying candidate generator, e.g. 'llm:anthropic', 'fixture:golden'. Nullable when the strategy has no external provider. Resolves DOUBT-STRATEGY-METADATA as the minimum-beyond-name+version metadata." }
```

## `LemmaAbsenceQuizCandidate` como carrier del candidato (resuelve DOUBT-CANDIDATE-NOTATION)

R009/R011/R019 describen los cuatro elementos conceptuales del candidato: oracion con blanks, respuesta correcta por blank, variantes aceptadas, traduccion. FEAT-QSENT ya define una DSL textual para los tres primeros: `"He ____ [is|'s] (to be) great."`. El candidato entonces se modela como **dos** campos, no cuatro: el `quizSentence` (que lleva los tres primeros en una sola cadena formal, re-usando el parser publico de `QuizSentenceConverter`) y la `translation` en un campo aparte porque es un texto paralelo en otro idioma. Esta eleccion colapsa el problema de "como representamos variantes y blanks" al contrato que FEAT-QSENT ya validata y testea; las invariantes de la DSL (grupos B/F de QSENT) se heredan automaticamente. Resuelve DOUBT-CANDIDATE-NOTATION como la opcion A del doubt (DSL textual ya formalizada).

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: LemmaAbsenceQuizCandidate
        _change: add
        type: record
        fields:
          - { name: quizSentence, type: String, description: "Candidate exercise expressed in the FEAT-QSENT quizSentence DSL: blanks ('____'), answer blocks ([variant1|variant2]), hints in parentheses. Carries the exercise sentence + correct answers + accepted variants in one formal string (F-LAPS-R009, resolves DOUBT-CANDIDATE-NOTATION)." }
          - { name: translation, type: String, description: "Spanish translation of the new exercise (F-LAPS-R009). May differ from elementBefore.translation when the candidate changed the sentence meaning (F-LAPS-R013)." }
```

## `LemmaAbsenceProposalDeriver` como paso deterministico separado de la estrategia (R012, R019)

R012 fija que la derivacion del `elementAfter` a partir del candidato es deterministica, y R019 dice explicitamente que la estrategia **no** construye `elementAfter`: solo emite el candidato. Modelar estos dos pasos como el mismo port sepultaria el contrato: el llamador no podria probar independientemente "la estrategia acepta este contexto" y "la derivacion construye este elementAfter". Los separamos como dos ports distintos, y declaramos al deriver `sealed`: la derivacion es una funcion pura de dominio sin espacio plausible para implementaciones alternativas (P7). Recibe el `CourseElementSnapshot` de antes para copiar intactos los campos preservados por R014 (identificadores, instrucciones, theoryId, metadata estructural), y emite un nuevo snapshot con la `form` reconstruida desde el quizSentence (via `QuizSentenceConverter.parse`) y la `translation` sobreescrita.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: LemmaAbsenceProposalDeriver
        _change: add
        stereotype: service
        sealed: true
        exposes:
          - signature: "derive(CourseElementSnapshot before, LemmaAbsenceQuizCandidate candidate): CourseElementSnapshot"
            throws: [ProposalDerivationException]
```

## Implementar el deriver usando `QuizSentenceConverter` de course-domain

La implementacion del deriver vive en el package `engine` de `revision-domain` (package-private, oculto tras el factory seam que FEAT-REVBYP ya tiene), e inyecta el `QuizSentenceConverter` publico de `course-domain` (FEAT-QSENT). La derivacion es: (1) `converter.parse(candidate.quizSentence())` -> `FormEntity`; (2) `new QuizTemplateEntity(...)` copiando del `elementBefore.quiz()` todos los campos R014 (id, oidId, kind, knowledgeId, title, instructions, theoryId, topicName, difficulty, retries) y sustituyendo `form` por el parseado y `translation` por el del candidato; (3) `new CourseElementSnapshot(before.nodeTarget(), before.nodeId(), nuevoQuiz)`. Las fallas de parse (DSL invalida) salen como `QuizSentenceParseException` de QSENT y el deriver las re-envuelve en `ProposalDerivationException` para que el dispatcher las clasifique como `STRATEGY_FAILED`. `revision-domain` ya `dependsOn: [course-domain]` desde FEAT-REVBYP, no se toca la frontera de modulos.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultLemmaAbsenceProposalDeriver
            _change: add
            implements: [LemmaAbsenceProposalDeriver]
            types: [Component]
            requiresInject:
              - { name: quizSentenceConverter, type: QuizSentenceConverter, description: "FEAT-QSENT: parses the candidate DSL into a FormEntity. The deriver composes the new QuizTemplateEntity by copying everything from elementBefore.quiz except form and translation (F-LAPS-R012, R013, R014)." }
```

## Registry + config record (resuelve DOUBT-STRATEGY-REGISTRY)

R004 habla de "estrategias registradas" sin definir donde viven. La opcion mas barata compatible con el resto de la arquitectura es: un `LemmaAbsenceProposalStrategyRegistry` con una unica implementacion en el package `engine`, que recibe por constructor un `LemmaAbsenceProposalStrategyRegistryConfig` (lista de estrategias + nombre de la activa). Esto resuelve DOUBT-STRATEGY-REGISTRY como "registro en codigo, construido en el composition root" — no se introduce un archivo declarativo en esta iteracion porque hay una sola estrategia concreta y expandir a N es aditivo. El registry se declara `sealed` (P7): el universo de implementaciones esta cerrado en tiempo de compilacion. Expone `active()` (para el dispatcher), `byName()` (para el selector) y `listAll()` (para que el selector pueda reportar los nombres registrados en caso de misconfig).

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: LemmaAbsenceProposalStrategyRegistry
        _change: add
        stereotype: service
        sealed: true
        exposes:
          - signature: "active(): Optional<LemmaAbsenceProposalStrategy>"
          - signature: "byName(String name): Optional<LemmaAbsenceProposalStrategy>"
          - signature: "listAll(): List<StrategyId>"
    packages:
      - name: engine
        _change: modify
        models:
          - name: LemmaAbsenceProposalStrategyRegistryConfig
            _change: add
            type: record
            fields:
              - { name: registered, type: "List<LemmaAbsenceProposalStrategy>", description: "All strategies known to the registry (F-LAPS-R004)." }
              - { name: activeName, type: String, description: "Name of the active strategy; resolved via byName(). Nullable -> active() returns empty -> dispatcher returns NO_ACTIVE_STRATEGY for LEMMA_ABSENCE (F-LAPS-R006)." }
        implementations:
          - name: DefaultLemmaAbsenceProposalStrategyRegistry
            _change: add
            implements: [LemmaAbsenceProposalStrategyRegistry]
            types: [Component]
            requiresInject:
              - { name: config, type: LemmaAbsenceProposalStrategyRegistryConfig }
```

## Dispatcher routing: LEMMA_ABSENCE sale del fallback bypass (R002, R006)

El `DispatchingReviser` de FEAT-REVBYP resolvia cualquier `DiagnosisKind` no registrado al `IdentityReviser` fallback. R002 cambia eso para LEMMA_ABSENCE: ya no cae al bypass; se enruta por el registry. R006 precisa que si no hay estrategia activa, la revision falla explicitamente. El dispatcher se extiende con dos colaboradores (`strategyRegistry` + `deriver`), y su logica interna adquiere una rama nueva: si `task.diagnosisKind() == LEMMA_ABSENCE`, consulta `registry.active()`; si esta vacia emite outcome `NO_ACTIVE_STRATEGY`; si esta presente, invoca `strategy.propose(task, context)`, captura `ProposalStrategyFailedException` como outcome `STRATEGY_FAILED`, pasa el candidato al deriver, captura `ProposalDerivationException` tambien como `STRATEGY_FAILED`, y finalmente arma el `RevisionProposal` con el `elementAfter` derivado y `strategyId = strategy.id()`. Para los demas `DiagnosisKind`, el dispatcher sigue funcionando exactamente como FEAT-REVBYP (byKind + fallback). Esta rama no introduce un port nuevo en `Reviser`: la logica es de flujo, y el dispatcher es el unico que la observa. `DefaultRevisionEngine` consume el `RevisionOutcomeKind` como hoy y mapea los dos valores nuevos a exit codes en la CLI.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DispatchingReviser
            _change: modify
            implements: [Reviser]
            types: [Component]
            requiresInject:
              - { name: byKind, type: "Map<DiagnosisKind,Reviser>", description: "Revisers registered per DiagnosisKind (FEAT-REVBYP R003). Preserved for non-LEMMA_ABSENCE kinds." }
              - { name: fallback, type: IdentityReviser, description: "Bypass fallback for unmatched DiagnosisKinds OTHER THAN LEMMA_ABSENCE (F-LAPS-R002, R006)." }
              - { name: strategyRegistry, type: LemmaAbsenceProposalStrategyRegistry, description: "Resolves the active LemmaAbsenceProposalStrategy; LEMMA_ABSENCE tasks route here instead of to fallback (F-LAPS-R002)." }
              - { name: deriver, type: LemmaAbsenceProposalDeriver, description: "Derives elementAfter deterministically from a LemmaAbsenceQuizCandidate (F-LAPS-R012)." }
```

## Dos nuevos outcomes en `RevisionOutcomeKind` para las fallas pre-propuesta

R015 y R016 prohiben que una falla de estrategia contamine el vocabulario de veredictos (`RevisionVerdict.APPROVED/REJECTED/PENDING_APPROVAL`). Reciclar una de esas palabras seria una violacion directa. En cambio, `RevisionOutcomeKind` — el enum *de flujo*, no de veredicto — ya tiene siete valores que cubren aborts pre-propuesta (`NO_REVISER`, `CONTEXT_UNAVAILABLE`, `ELEMENT_NOT_FOUND`). Agregamos dos peers: `NO_ACTIVE_STRATEGY` (R006) y `STRATEGY_FAILED` (R015). El `RevisionOutcome` envuelve el outcome con `artifact = null` para ambos valores (no se persiste nada), y el CLI mapea los dos a exit codes distintos para que el operador pueda scriptear. Este enfoque es consistente con como FEAT-REVAPR agrego `PENDING_APPROVAL_PERSISTED` y `ALREADY_PENDING_DECISION`: el enum crece, todo lo demas se beneficia automaticamente.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: RevisionOutcomeKind
        _change: modify
        fields:
          - name: NO_ACTIVE_STRATEGY
            _change: add
            description: "A LEMMA_ABSENCE task reached the dispatcher but no LemmaAbsenceProposalStrategy was active. No artifact, no course write, task unchanged (F-LAPS-R006, F-LAPS-J004)."
          - name: STRATEGY_FAILED
            _change: add
            description: "The active strategy could not produce a candidate (empty output, provider down, uninterpretable response) or its candidate could not be derived. No artifact, no course write, task unchanged (F-LAPS-R015, F-LAPS-R016, F-LAPS-J003)."
```

## `RevisionProposal.strategyId` como campo nullable para no romper FEAT-REVBYP

R005 pide la identidad de la estrategia en la propuesta. El campo nuevo `strategyId: StrategyId` es nullable: se llena cuando la propuesta salio de una estrategia de LEMMA_ABSENCE, y queda null cuando salio del bypass (para los `DiagnosisKind` restantes). Nullable es preferible a "tag discriminator" porque preserva wire-compat con los artefactos ya serializados por FEAT-REVBYP/FEAT-REVAPR (los readers leen un campo faltante como null; no hay cambio de esquema enemigo). `reviserKind` se queda: FEAT-REVBYP lo uso para distinguir bypass de reviser reales, y sigue teniendo sentido cuando quede como `"bypass"` para otros diagnosticos.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: RevisionProposal
        _change: modify
        fields:
          - name: strategyId
            type: StrategyId
            _change: add
            description: "Identity of the strategy that produced this proposal (F-LAPS-R005); null when produced by the bypass reviser for non-LEMMA_ABSENCE diagnostics (FEAT-REVBYP)."
```

## Dos excepciones separadas para los dos modos de falla

Falla durante la generacion del candidato (`ProposalStrategyFailedException`) y falla durante la derivacion (`ProposalDerivationException`) son conceptualmente distintas: la primera indica que la estrategia no pudo emitir nada; la segunda, que lo emitido es sintacticamente invalido (DSL malformada, traduccion vacia). Separarlas permite que el dispatcher loguee mensajes precisos y que los tests de cada rama se escriban sin mockear la otra. Ambas se consolidan al mismo outcome (`STRATEGY_FAILED`) porque el operador no necesita distinguirlas a nivel de exit code — ambos casos requieren la misma accion: inspeccionar el log de la invocacion. Las mensajes llevan el nombre de la estrategia y el taskId para trazabilidad, consistente con el template de `%s` formatting que ya usa el resto del dominio.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: ProposalStrategyFailedException
        _change: add
        type: exception
        extends: RuntimeException
        message: "La estrategia de propuesta '%s' no pudo generar un candidato de quiz para la tarea '%s': %s"
        fields:
          - { name: strategyName, type: String, description: "Name of the strategy that failed (F-LAPS-R015)." }
          - { name: taskId, type: String, description: "Id of the task the strategy was attempting." }
          - { name: reason, type: String, description: "Human-readable cause (empty generator response, provider down, etc.)." }
      - name: ProposalDerivationException
        _change: add
        type: exception
        extends: RuntimeException
        message: "No se pudo derivar elementAfter desde el candidato de la estrategia '%s' en la tarea '%s': %s"
        fields:
          - { name: strategyName, type: String, description: "Name of the strategy whose candidate was rejected by the deriver." }
          - { name: taskId, type: String, description: "Id of the task." }
          - { name: reason, type: String, description: "Human-readable cause (invalid quizSentence grammar, empty translation, etc.)." }
```

## Extender `RevisionEngineConfig` con registry + deriver

El factory de FEAT-REVBYP (`DefaultRevisionEngineFactory`) ya arma todos los colaboradores del engine desde un unico `RevisionEngineConfig`. Si el dispatcher gana `strategyRegistry` + `deriver` en sus dependencias, esos dos deben llegar al factory por el unico canal que el factory tiene: el config record. Los campos son nullables: si el caller no los pasa, el factory construye un registry vacio (y toda invocacion LEMMA_ABSENCE termina en `NO_ACTIVE_STRATEGY`, que es el comportamiento correcto para un sistema sin estrategias configuradas) y un `DefaultLemmaAbsenceProposalDeriver` con un `QuizSentenceConverter` tomado tambien del config. No se introduce un segundo factory (P6: un seam por capacidad) porque el engine sigue siendo el mismo engine de FEAT-REVBYP/REVAPR; LAPS solo extiende sus entradas.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: RevisionEngineConfig
        _change: modify
        fields:
          - name: lemmaAbsenceStrategyRegistry
            type: LemmaAbsenceProposalStrategyRegistry
            _change: add
            description: "Registry of registered LEMMA_ABSENCE strategies. Nullable -> the factory wires an empty registry, in which case every LEMMA_ABSENCE task resolves to NO_ACTIVE_STRATEGY (F-LAPS-R006)."
          - name: lemmaAbsenceProposalDeriver
            type: LemmaAbsenceProposalDeriver
            _change: add
            description: "Deterministic deriver used when a LEMMA_ABSENCE candidate must be converted into a CourseElementSnapshot (F-LAPS-R012). Nullable -> the factory wires DefaultLemmaAbsenceProposalDeriver."
```

## Package `strategy` (publico) para la SPI de estrategias concretas

La estrategia concreta (MVP) vive en un package `strategy` publico de `revision-domain`, separado del package `engine` (que queda para los internals del motor: dispatcher, registry impl, deriver impl). La justificacion es P5 (contract / carrier / engine, pero con tres scopes distintos): el root expone los ports (`LemmaAbsenceProposalStrategy`), el package `strategy` expone las implementaciones concretas de esos ports (que el composition root necesita instanciar y meter en el registry), y el package `engine` oculta los motores del pipeline (dispatcher, registry impl, deriver impl). El package es `public` porque el composition root (`audit-cli`) necesita `new LemmaAbsenceMvpStrategy(generator)`; una visibilidad mas baja (internal) forzaria que la construccion viva en `revision-domain`, lo cual obligaria a este modulo a conocer el generador concreto (violacion de P4).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: strategy
        _change: add
        visibility: public
        description: "Proposal-strategy SPI for LEMMA_ABSENCE: houses concrete strategies and the candidate-generator port they delegate to. Public so the composition root can instantiate strategies and pass them into the registry builder."
```

## `LemmaAbsenceQuizCandidateGenerator` como port del proveedor concreto (TBD por el usuario)

La estrategia MVP no implementa por si misma la generacion del candidato (no llama al LLM, no lee fixtures): delega en un port `LemmaAbsenceQuizCandidateGenerator`. Esta separacion cumple tres cosas: (1) deja la decision del proveedor (Anthropic / OpenAI / local / fixture) fuera del patch arquitectonico — es una **decision del usuario** que no pertenece a este tech spec; (2) permite que el test suite de QA use un generator de fixture sin tocar la estrategia; (3) mantiene la estrategia provider-agnostica, lo que facilita que en el futuro una segunda estrategia (ensemble, multi-LLM) reuse el mismo port. El adapter concreto del generator NO esta en este patch — su modulo destino (nuevo infrastructure module? subpackage? fixture-only en test scope?) es un punto abierto para la iteracion de implementacion. El port acepta el contexto completo y emite el raw output en un `LemmaAbsenceGeneratorResponse` que la estrategia luego envuelve en `LemmaAbsenceQuizCandidate` + stamp de `StrategyId`.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: strategy
        _change: modify
        models:
          - name: LemmaAbsenceGeneratorResponse
            _change: add
            type: record
            fields:
              - { name: quizSentence, type: String, description: "Candidate expressed in the FEAT-QSENT quizSentence DSL; raw generator output before wrapping." }
              - { name: translation, type: String, description: "Spanish translation of the candidate." }
        interfaces:
          - name: LemmaAbsenceQuizCandidateGenerator
            _change: add
            stereotype: port
            visibility: public
            exposes:
              - signature: "generate(LemmaAbsenceCorrectionContext context): LemmaAbsenceGeneratorResponse"
                throws: [ProposalStrategyFailedException]
```

## `LemmaAbsenceMvpStrategy`: un solo colaborador, sin reintentos ni validacion

R008 dice "un candidato por invocacion"; R010 dice "no-deterministica por defecto, sin reintentos automaticos"; R017/R018 dicen "sin validacion post-hoc de CEFR ni de largo". Esto colapsa la MVP a una clase con una sola dependencia (el generator), una sola pasada, y stamping de la identidad. Mas ceremonia no agrega nada: no hay loops de retry, no hay validadores internos, no hay cache. El `types: [Component]` anota la clase para el scanner de Sentinel; el `visibility: public` le permite al composition root instanciarla (consistente con el seam-de-estrategia decidido en el package `strategy`).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: strategy
        _change: modify
        implementations:
          - name: LemmaAbsenceMvpStrategy
            _change: add
            visibility: public
            implements: [LemmaAbsenceProposalStrategy]
            types: [Component]
            requiresInject:
              - { name: generator, type: LemmaAbsenceQuizCandidateGenerator }
```

## `ProposalStrategySelector` en `bootstrap` (resuelve DOUBT-STRATEGY-SELECTION)

DOUBT-STRATEGY-SELECTION ofrece cuatro opciones; elegimos A (env-var en startup) porque (1) es el mismo patron que FEAT-REVAPR ya usa (`CONTENT_AUDIT_APPROVAL_MODE`), reutilizable conceptualmente por el operador; (2) no requiere cambios a `RefinementTask` (opcion B), lo que evitaria un cambio al plan ya persistido; (3) no satura cada invocacion de `revise` con un flag (opcion C). El selector vive en el package `bootstrap` de `audit-cli` junto con `DefaultApprovalModeResolver` y `DefaultWorkdirResolver`, cerrado al composition root. `sealed: true` porque hay exactamente un formato de env-var, exactamente una implementacion. Recibe el env-var crudo y el registry, valida que el nombre exista (con `listAll()` para el error message), devuelve el nombre resuelto (no el objeto estrategia — eso lo hace el composition root via `registry.byName(nombre)`); unset -> default `"lemma-absence-mvp"` (la unica estrategia registrada en MVP).

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        models:
          - name: InvalidProposalStrategyException
            _change: add
            type: exception
            extends: RuntimeException
            message: "Invalid value for CONTENT_AUDIT_LAPS_STRATEGY: '%s'. Registered: %s"
            fields:
              - { name: value, type: String, description: "The raw env-var value that failed parsing." }
              - { name: registered, type: String, description: "Comma-separated list of registered strategy names (LemmaAbsenceProposalStrategyRegistry.listAll())." }
        interfaces:
          - name: ProposalStrategySelector
            _change: add
            stereotype: port
            sealed: true
            exposes:
              - signature: "select(String envValue, LemmaAbsenceProposalStrategyRegistry registry): String"
                throws: [InvalidProposalStrategyException]
        implementations:
          - name: DefaultProposalStrategySelector
            _change: add
            visibility: public
            implements: [ProposalStrategySelector]
```

## Pendientes declarados (decisiones de usuario y de QA)

Cuatro puntos quedan **deliberadamente abiertos** en este patch y deben resolverse fuera del tech spec:

1. **Proveedor concreto del generator.** El adapter que implementa `LemmaAbsenceQuizCandidateGenerator` no esta en este patch. El usuario tiene que decidir: LLM en linea (Anthropic / OpenAI / Azure), modelo local (Ollama / llama.cpp), o solo fixtures para habilitar QA end-to-end sin un proveedor real. Esa decision incluye el modulo destino (probablemente `revision-infrastructure` nuevo, o un subpackage de `audit-infrastructure`) y las dependencias externas (SDK del proveedor). Hasta que se resuelva, la MVP **no puede correr en produccion**; solo el fixture generator (escribible por QA) permite exit-to-exit.

2. **DOUBT-PROMPT-PERSISTENCE** — resuelto como opcion A (no persistimos el input exacto; el `CorrectionContext` es recuperable del plan + audit de origen). Si esto resulta insuficiente para debugging real, agregar un campo `inputSnapshot: LemmaAbsenceCorrectionContext` al `RevisionProposal` es aditivo y no rompe nada.

3. **DOUBT-FAILURE-TRACEABILITY** — resuelto como opcion A (cero rastro persistente; solo stdout/stderr). Esto se puede revertir a opcion B (`_failures.log`) si el operador reporta que pierde demasiada informacion. No hay impacto arquitectonico hoy: el reporting de falla ocurre a nivel CLI.

4. **Tests.** Este patch no declara `handwrittenTests`. La arquitectura esta lista para que `@qa-tester` agregue cobertura de: (a) `LemmaAbsenceMvpStrategy` con un generator stub (F-LAPS-R007, R008); (b) `DefaultLemmaAbsenceProposalDeriver` con fixtures QSENT reales (R012, R013, R014); (c) branch nuevo en `DispatchingReviser` (R002, R006, R015); (d) `DefaultLemmaAbsenceProposalStrategyRegistry` (R004); (e) `DefaultProposalStrategySelector` (DOUBT-STRATEGY-SELECTION); (f) journeys F-LAPS-J001..J005 end-to-end con un generator fixture.

