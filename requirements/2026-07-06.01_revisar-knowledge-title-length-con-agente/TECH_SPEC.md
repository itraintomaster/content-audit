---
patch: ARCH-KTLR
requirement: 2026-07-06.01_revisar-knowledge-title-length-con-agente
generated: 2026-07-06T21:05:00Z
---

# Tech Spec: Revision de longitud de titulo de knowledge con propuesta del agente

## Resolver el DOUBT-SNAPSHOT-POLIMORFICO con un snapshot discriminado por nodeTarget (Opcion A)

El carrier `CourseElementSnapshot` era quiz-only (`quiz: QuizTemplateEntity`, "null otherwise"). Elegimos la **Opcion A**: un mismo snapshot que porta tambien `knowledge`, discriminado por el `nodeTarget` que el record ya tiene. Rechazamos la Opcion B (tipo/ruta separada) porque duplicaria `RevisionProposal`, `RevisionArtifact`, `Reviser`, `RevisionEngine` y toda la maquinaria de decision, multiplicando los puntos de regresion sobre F-KTLR-R008. Con Opcion A el camino QUIZ queda literalmente sin tocar (el discriminante enruta), y el knowledge viaja completo para que la escritura reemplace solo label+instructions preservando el resto (F-KTLR-R006, R007).

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: CourseElementSnapshot
        _change: modify
        fields:
          - { name: knowledge, type: KnowledgeEntity, _change: add }
```

## Enrutar la escritura al curso por nodeTarget en el locator existente

`DefaultCourseElementLocator` es el unico seam de escritura al curso y hoy hace no-op para todo target != QUIZ. Lo modificamos para que `snapshot()`/`replace()` enruten por `nodeTarget`: KNOWLEDGE localiza el knowledge por id y reescribe label+instructions preservando id, quizzes, orden y jerarquia; QUIZ conserva su comportamiento actual. Concentrar el cambio en esta sola clase (dentro del package interno `engine`) es lo que hace verificable F-KTLR-R008 sin duplicar logica: el mismo test de quiz sigue verde por construccion.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultCourseElementLocator
            _change: modify
            implements: ["CourseElementLocator"]
```

## Resolver un contexto dedicado del knowledge y enchufarlo al dispatcher

La resolucion de contexto hoy solo cubre SENTENCE_LENGTH y LEMMA_ABSENCE; para KNOWLEDGE_TITLE_LENGTH devuelve vacio y la revision no arranca. Agregamos `KnowledgeTitleCorrectionContext` (record que implementa el marcador `CorrectionContext`) con el minimo que la propuesta necesita — titulo actual, instructions actuales, label del topic, limites y idiomas esperados (F-KTLR-R002) — y un `KnowledgeTitleContextResolver` que se registra como tercera rama del `DispatchingCorrectionContextResolver`. El resolver es el punto donde la ausencia del knowledge se detecta antes de invocar al agente (F-KTLR-R001, R011).

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: KnowledgeTitleCorrectionContext
        _change: add
        type: record
        implements: ["CorrectionContext"]
        fields:
          - { name: knowledgeId, type: String }
          - { name: currentTitle, type: String }
          - { name: currentInstructions, type: String }
          - { name: topicLabel, type: String }
    implementations:
      - name: KnowledgeTitleContextResolver
        _change: add
        implements: ["CorrectionContextResolver"]
        visibility: public
      - name: DispatchingCorrectionContextResolver
        _change: modify
        requiresInject:
          - { name: knowledgeTitleResolver, type: KnowledgeTitleContextResolver }
        implements: ["CorrectionContextResolver"]
```

## Espejar la SPI de propuesta de lemma-absence sobre el sub-dominio del titulo

Replicamos la anatomia probada de `lemmaabsence` (strategy + registry sellado + deriver + carrier) en un package publico `knowledgetitle` cuyo nombre describe el sub-dominio, no el patron. El port `KnowledgeTitleCandidateGenerator` es el seam que el adapter agente implementa; `KnowledgeTitleAgentStrategy` delega en el y estampa el providerId, igual que `LemmaAbsenceMvpStrategy`. El carrier `KnowledgeTitleCandidate {title, instructions}` mantiene el vocabulario del agente (`title`); la traduccion `title -> label` es responsabilidad explicita del deriver, no del carrier (F-KTLR-R003, R004, R005).

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: KnowledgeTitleProposalStrategy
        _change: add
        stereotype: port
        exposes:
          - signature: "propose(RefinementTask task, KnowledgeTitleCorrectionContext context): KnowledgeTitleCandidate"
      - name: KnowledgeTitleProposalStrategyRegistry
        _change: add
        stereotype: service
        sealed: true
        exposes:
          - signature: "active(): Optional<KnowledgeTitleProposalStrategy>"
    packages:
      - name: knowledgetitle
        _change: add
        visibility: public
        models:
          - name: KnowledgeTitleCandidate
            _change: add
            type: record
            fields:
              - { name: title, type: String }
              - { name: instructions, type: String }
        interfaces:
          - name: KnowledgeTitleCandidateGenerator
            _change: add
            stereotype: port
            visibility: public
            exposes:
              - signature: "generate(KnowledgeTitleCorrectionContext context): KnowledgeTitleGeneratorResponse"
        implementations:
          - name: KnowledgeTitleAgentStrategy
            _change: add
            implements: ["KnowledgeTitleProposalStrategy"]
            visibility: public
```

## Materializar el mapeo title->label en un deriver sellado

El deriver es el unico lugar donde el vocabulario del agente (`title`) se traduce al del dominio del curso (`KnowledgeEntity.label`). `derive(before, candidate)` copia el knowledge de `before`, setea `label = candidate.title` e `instructions = candidate.instructions` y preserva bit a bit todo lo demas (id, code, parentId, slug, quizTemplates, sentenceMode, order), lo que hace determinista y verificable F-KTLR-R007. Lo declaramos `sealed` porque el universo de derivadores es cerrado y conocido — mismo criterio que `LemmaAbsenceProposalDeriver`.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: KnowledgeTitleProposalDeriver
        _change: add
        stereotype: service
        sealed: true
        exposes:
          - signature: "derive(CourseElementSnapshot before, KnowledgeTitleCandidate candidate): CourseElementSnapshot"
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultKnowledgeTitleProposalDeriver
            _change: add
            implements: ["KnowledgeTitleProposalDeriver"]
            visibility: public
```

## Enrutar KNOWLEDGE_TITLE_LENGTH con un Reviser dedicado en el byKind existente

En vez de acoplar mas inyecciones al `DispatchingReviser` (que ya conoce el pipeline de lemma), agregamos un `KnowledgeTitleReviser` dedicado que se registra en el `Map<DiagnosisKind,Reviser> byKind` que FEAT-REVBYP dejo como seam de extension. El reviser resuelve la estrategia activa via el registry, obtiene el candidato y llama al deriver; si no hay estrategia activa o el agente falla, no construye propuesta (F-KTLR-R003, R010). Esto deja el pipeline de lemma-absence intacto: la nueva ruta no comparte codigo con el, solo el mecanismo de registro por kind.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        models:
          - name: KnowledgeTitleProposalStrategyRegistryConfig
            _change: add
            type: record
            fields:
              - { name: registered, type: "List<KnowledgeTitleProposalStrategy>" }
              - { name: activeName, type: String }
        implementations:
          - name: KnowledgeTitleReviser
            _change: add
            implements: ["Reviser"]
            requiresInject:
              - { name: strategyRegistry, type: KnowledgeTitleProposalStrategyRegistry }
              - { name: deriver, type: KnowledgeTitleProposalDeriver }
          - name: DefaultKnowledgeTitleProposalStrategyRegistry
            _change: add
            implements: ["KnowledgeTitleProposalStrategyRegistry"]
            visibility: public
```

## Espejar el factory seam del adapter agente para las fallas de runtime

El adapter vive en `revision-infrastructure` como espejo de `lagen`/`lemmaabsenceagent`: un unico factory seam publico (`KnowledgeTitleAgentGeneratorFactory`) construye el generador respaldado por el agente declarativo `knowledge-titles-agent`, y todo el motor — launcher del runtime, parser de candidatos, clasificador de errores — es package-private. El clasificador traduce fallas de runtime a `LlmGenerationFailureCategory` y de ahi a `ProposalStrategyFailedException`, que es lo que hace observable F-KTLR-R010 (la falla ocurre antes de tener propuesta, no se persiste artefacto). Reusa `ChatModel` y `RunState` ya declarados; no introduce dependencias nuevas.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    patterns:
      - type: Factory
        interface: KnowledgeTitleAgentGeneratorFactory
        implementations: ["DefaultKnowledgeTitleAgentGeneratorFactory"]
    packages:
      - name: knowledgetitleagent
        _change: add
        visibility: public
        interfaces:
          - name: KnowledgeTitleAgentGeneratorFactory
            _change: add
            stereotype: factory
            visibility: public
            exposes:
              - signature: "create(LagenConfig config): KnowledgeTitleCandidateGenerator"
          - name: KnowledgeTitleAgentErrorClassifier
            _change: add
            visibility: internal
            exposes:
              - signature: "classify(RunState runState): LlmGenerationFailureCategory"
        implementations:
          - name: DefaultKnowledgeTitleAgentGeneratorFactory
            _change: add
            implements: ["KnowledgeTitleAgentGeneratorFactory"]
            visibility: public
          - name: KnowledgeTitleAgentGenerator
            _change: add
            implements: ["KnowledgeTitleCandidateGenerator"]
            requiresInject:
              - { name: chatModel, type: ChatModel }
              - { name: strategyName, type: String }
