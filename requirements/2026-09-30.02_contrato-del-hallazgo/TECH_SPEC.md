---
patch: FEAT-HALL
requirement: 2026-09-30.02_contrato-del-hallazgo
generated: 2026-09-30T15:40:00Z
---

# Tech Spec: Contrato del hallazgo, catálogo de analizadores y números por contexto (FEAT-HALL)

Paquete 0.3 del brief 087. Lo que existe hoy y motiva cada decisión se verificó leyendo el código de este
worktree y el informe `audit-2026-09-30T11-54-02` (la base del 29/9). La regla que ordena todo el diseño:
un analizador nuevo agrega su paquete y su proveedor; lo compartido que todavía hay que tocar queda listado
al final como enganche, para que el integrador lo junte en un solo parche por ronda.

## Dar a todo lo que no pasa una sola forma: el hallazgo
Hoy cada analizador deja un diagnóstico tipado propio y nada común, así que quien muestra tiene que conocerlos
a todos y no hay dónde contar ejercicios con error. `Finding` tiene los ocho campos de F-HALL-R001, iguales para
los diecinueve, y ninguno libre: lo propio de cada analizador sigue en su diagnóstico tipado (DLABS), porque un
mapa o un campo extra acá sería el mapa genérico por la puerta de atrás. Gravedad, resolución y costo son enums
porque R002 fija valores cerrados y la UI agrupa por ellos. La evidencia es texto para personas —partes con su
etiqueta, una línea de observación y los nodos relacionados—, no datos: los datos siguen en el diagnóstico.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: finding
        _change: add
        visibility: public
        models:
          - name: Finding
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: rule, type: String }
              - { name: node, type: FindingNodeRef }
              - { name: severity, type: FindingSeverity }
              - { name: evidence, type: FindingEvidence }
              - { name: resolution, type: FindingResolution }
              - { name: cost, type: AnalysisCost }
              - { name: identity, type: FindingIdentity }
          - name: FindingNodeRef
            _change: add
            type: record
            fields:
              - { name: target, type: AuditTarget }
              - { name: nodeId, type: String }
              - { name: label, type: String }
          - name: FindingEvidence
            _change: add
            type: record
            fields:
              - { name: examined, type: "List<EvidencePart>" }
              - { name: observation, type: String }
              - { name: relatedNodes, type: "List<FindingNodeRef>" }
          - name: EvidencePart
            _change: add
            type: record
            fields:
              - { name: label, type: String }
              - { name: text, type: String }
          - name: FindingSeverity
            _change: add
            type: enum
            fields:
              - { name: BLOCKING }
              - { name: HIGH }
              - { name: MEDIUM }
              - { name: LOW }
          - name: FindingResolution
            _change: add
            type: enum
            fields:
              - { name: RULE }
              - { name: PANEL }
              - { name: RANK_ONLY }
          - name: AnalysisCost
            _change: add
            type: enum
            fields:
              - { name: INSTANT }
              - { name: LOCAL_MODEL }
              - { name: PAID_MODEL }
```

## Separar lo que decide el analizador de lo que estampa el motor
El analizador sólo decide lo que sabe: regla, qué marca, gravedad, resolución y evidencia (`FindingDraft`). El
motor se los pide con `findingsAt(node)` —en lugar de dejar que los escriba donde quiera— y sólo en los nodos
que el analizador evaluó, antes de agregar: así nunca aparece un hallazgo en un contenedor que sólo recibe
promedios, que es justo lo que LABS escribe en temas y topics y COCA en topics sin meta (R001 inv. 2). El motor
estampa analizador, nodo, costo (el de la regla) e identidad, así que R014 y R006 inv. 2 salen por construcción;
la identidad `analizador|regla|nodo|marca` no incluye versión del juez ni huella, y el juez emite un solo hallazgo
por ejercicio porque los códigos de sus violaciones los escribe el modelo y no son estables. Romper el contrato es
un error de programación (`FindingContractViolationException`) y corta la corrida: el aislamiento de
F-QINST-R007 es para fallas del evaluador, no para esto. Los ocho existentes implementan `findingsAt` leyendo su
diagnóstico tipado, sin tocar su cálculo de puntaje (R013).

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: ContentAnalyzer
        _change: modify
        exposes:
          - signature: "findingsAt(AuditNode node): List<FindingDraft>"
            _change: add
    packages:
      - name: finding
        _change: add
        visibility: public
        models:
          - name: FindingDraft
            _change: add
            type: record
            fields:
              - { name: rule, type: String }
              - { name: marker, type: String }
              - { name: severity, type: FindingSeverity }
              - { name: resolution, type: FindingResolution }
              - { name: evidence, type: FindingEvidence }
          - name: FindingIdentity
            _change: add
            type: record
            fields:
              - { name: key, type: String }
              - { name: marker, type: String }
          - name: FindingContractViolationException
            _change: add
            type: exception
            fields:
              - { name: analyzerName, type: String }
              - { name: nodeId, type: String }
              - { name: detail, type: String }
```

## Colgar hallazgos, números y lo sin evaluar del nodo
Van en `AuditNode` y no en `AuditReport` por costo concreto: `AuditNode` se mantiene a mano (sin `@Generated`),
así que sumarle campos no cambia su constructor de siete argumentos (98 construcciones en tests), mientras que
el constructor generado de `AuditReport` tiene 234. `unevaluatedBy` es la única forma de contar «sin evaluar»:
hoy el juez deja el ejercicio pendiente o fallido sin puntaje ni diagnóstico (F-QINST-R004/R007), indistinguible
de uno que no alcanza. Lo marca el analizador (el juez, en la rama de pendiente o fallido de `onQuiz`) o el motor
cuando aísla una falla; no cambia puntaje ni diagnóstico, y el juez sigue sin puntaje ahí.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditNode
        _change: modify
        type: record
        fields:
          - { name: findings, type: "List<Finding>", _change: add }
          - { name: numbers, type: ContextNumbers, _change: add }
          - { name: unevaluatedBy, type: "List<String>", _change: add }
```

## Describir cada analizador en su ficha
La ficha es la entrada del catálogo: se extiende `AnalyzerDescriptor` (dos construcciones) en vez de crear un
segundo tipo que diga lo mismo. `evaluatedTargets` se declara y no se deduce, porque escribir una clave propia no
es evaluar: LABS promedia él mismo temas y topics, y COCA puntúa topics sin meta; por eso las fichas dicen
`sentence-length` QUIZ, `lemma-absence` QUIZ/MILESTONE/COURSE, `coca-buckets-distribution` y `lemma-count`
MILESTONE/COURSE, `lemma-recurrence` COURSE, los de título y consigna KNOWLEDGE y `quiz-instruction` QUIZ. El costo
de la ficha es el mayor de sus reglas y la meta es la que aplica el código (A1 de 3 a 8 tokens, no el 5 a 8 de
F-SLEN-R012: regla 4 del brief 087). La familia es por analizador porque R007 la fija así; si José eligiera la
opción B de DOUBT-FAMILIA-OTRO-NIVEL, la familia pasaría a la regla, que es el único cambio de forma que haría falta.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AnalyzerDescriptor
        _change: modify
        type: record
        fields:
          - { name: question, type: String, _change: add }
          - { name: reads, type: String, _change: add }
          - { name: rules, type: "List<AnalyzerRuleCard>", _change: add }
          - { name: goal, type: String, _change: add }
          - { name: family, type: AnalyzerFamily, _change: add }
          - { name: evaluatedTargets, type: "List<AuditTarget>", _change: add }
          - { name: resolutions, type: "List<FindingResolution>", _change: add }
          - { name: cost, type: AnalysisCost, _change: add }
    packages:
      - name: catalog
        _change: add
        visibility: public
        models:
          - name: AnalyzerFamily
            _change: add
            type: enum
            fields:
              - { name: ERRORS }
              - { name: VOCABULARY }
          - name: AnalyzerRuleCard
            _change: add
            type: record
            fields:
              - { name: id, type: String }
              - { name: description, type: String }
              - { name: cost, type: AnalysisCost }
```

## Registrar cada analizador con un proveedor, y uno solo
Hoy hay dos listas de registro que no coinciden —`contentAnalyzers` para los clásicos y
`EvaluationAnalyzerFactory` para los jueces— y de ahí salen los dos síntomas del requirement: `quiz-instruction`
falta en `get analyzers`, `config` y `stats` (R005) y `--analyzers` sólo filtra jueces (R012). `AnalyzerProvider`
las unifica: nombre, ficha, analizador nuevo por corrida con su política, cómo pasa al plan y su configuración.
Construir de cero en cada corrida arregla además que los cuatro analizadores de curso guardan estado en campos y
no lo reinician, cuando la vista consolidada corre el motor dos veces en el mismo proceso (R014). La fábrica del
juez conserva `analyzerName()` y `create(policy)`, así que sus tests siguen valiendo, y suma lo demás. Vive en la
raíz del módulo junto a `ContentAnalyzer` porque Sentinel sólo resuelve el `implements` de una implementación de
la raíz contra interfaces de la raíz, y tres proveedores viven ahí.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: AnalyzerProvider
        _change: add
        stereotype: factory
        sealed: false
        exposes:
          - signature: "analyzerName(): String"
          - signature: "describe(): AnalyzerDescriptor"
          - signature: "create(EvaluationRunPolicy policy): ContentAnalyzer"
          - signature: "planBinding(): Optional<AnalyzerPlanBinding>"
          - signature: "config(): Optional<SelfDescribingConfig>"
      - name: EvaluationAnalyzerFactory
        _change: delete
    packages:
      - name: quizinstructionengine
        _change: modify
        visibility: public
        implementations:
          - name: DefaultQuizInstructionAnalyzerFactory
            _change: modify
            implements: [AnalyzerProvider]
```

## Darle a cada clásico su proveedor, al lado de su analizador
Cada proveedor vive en el paquete de su analizador, recibe sólo lo que `Main` ya construye (tokenizador,
configuraciones, catálogo EVP, puntuador léxico) y arma los colaboradores sin estado del analizador, como ya hace
la fábrica del juez: `Main` deja de conocer las piezas internas de COCA y de recurrencia (P4). Las ligaduras al plan
reproducen exactamente las listas de hoy, verificado contra la base del 29/9: 3.051 SENTENCE_LENGTH y 950
LEMMA_ABSENCE en QUIZ, 25 y 6 de consigna y título en KNOWLEDGE, 3 COCA_BUCKETS en MILESTONE y COURSE, 1
LEMMA_RECURRENCE en COURSE y ninguna de `lemma-count`: las mismas 4.036 tareas (R013).

```architecture
modules:
  - name: audit-domain
    _change: modify
    implementations:
      - name: SentenceLengthAnalyzerProvider
        _change: add
        visibility: public
        implements: [AnalyzerProvider]
        requiresInject:
          - { name: nlpTokenizer, type: NlpTokenizer }
          - { name: config, type: SentenceLengthConfig }
      - name: KnowledgeTitleLengthAnalyzerProvider
        _change: add
        visibility: public
        implements: [AnalyzerProvider]
      - name: KnowledgeInstructionsLengthAnalyzerProvider
        _change: add
        visibility: public
        implements: [AnalyzerProvider]
    packages:
      - name: coca
        _change: modify
        visibility: internal
        implementations:
          - name: CocaBucketsAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: nlpTokenizer, type: NlpTokenizer }
              - { name: cocaBucketsConfig, type: CocaBucketsConfig }
      - name: lrec
        _change: modify
        visibility: internal
        implementations:
          - name: LemmaRecurrenceAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: lemmaRecurrenceConfig, type: LemmaRecurrenceConfig }
      - name: labs
        _change: modify
        visibility: internal
        implementations:
          - name: LemmaAbsenceAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: evpCatalogPort, type: EvpCatalogPort }
              - { name: lemmaAbsenceConfig, type: LemmaAbsenceConfig }
              - { name: sentenceLexicalScorer, type: SentenceLexicalScorer }
      - name: lemmacount
        _change: modify
        visibility: internal
        implementations:
          - name: LemmaCountAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: evpCatalogPort, type: EvpCatalogPort }
              - { name: lemmaCountConfig, type: LemmaCountConfig }
```

## Hacer del catálogo la única fuente de nombres
`get analyzers`, `config`, `stats`, la validación de `analyze`, el motor y el plan leen el mismo objeto, así que
«figura en el catálogo si y sólo si analyze lo acepta» (R005) sale por construcción. La ficha se valida al armar
el catálogo, al arrancar: una ficha incompleta no llega a producción porque un test que arma el catálogo con los
proveedores reales falla antes de integrar (R006 inv. 1). El orden de registro es el orden del catálogo, de la
corrida y de los hallazgos dentro de un nodo (R014). Las implementaciones van en `findingengine` con
`allowedClients: [audit-cli]` (P8): sólo la raíz de composición las construye; los puertos quedan en la raíz.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: AnalyzerCatalog
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "list(): List<AnalyzerDescriptor>"
          - signature: "find(String analyzerName): Optional<AnalyzerDescriptor>"
          - signature: "provider(String analyzerName): Optional<AnalyzerProvider>"
          - signature: "planBinding(String analyzerName): Optional<AnalyzerPlanBinding>"
    packages:
      - name: catalog
        _change: add
        visibility: public
        models:
          - name: UnknownAnalyzerException
            _change: add
            type: exception
            extends: IllegalArgumentException
            fields:
              - { name: analyzerName, type: String }
          - name: InvalidAnalyzerCardException
            _change: add
            type: exception
            fields:
              - { name: analyzerName, type: String }
              - { name: detail, type: String }
      - name: findingengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultAnalyzerCatalog
            _change: add
            visibility: public
            implements: [AnalyzerCatalog]
            requiresInject:
              - { name: providers, type: "List<AnalyzerProvider>" }
```

## Correr exactamente lo pedido, en una sola pasada
Hoy `runAudit(Path, AuditRunRequest)` corre siempre el motor base con los siete clásicos —por eso `--analyzers` no
los restringe— y aplica al juez después de la agregación, así que su puntaje nunca llega a tema, nivel ni curso
(F-QINST-R004 incumplida; R011). Ahora el motor construye todos los analizadores de la selección antes de recorrer
(una política mal pedida se rechaza sin tocar el árbol), los recorre en orden del catálogo, recoge los hallazgos
de cada uno, agrega una sola vez y calcula los números. `runAudit(course)` sigue siendo la corrida base, los de costo
instantáneo: la vista consolidada y la vista previa de impacto calculan exactamente lo de hoy y nunca le pagan a un
juez. La falla por nodo de un analizador que consulta un modelo se aísla y deja el nodo sin evaluar (F-QINST-R007,
generalizada); la de uno instantáneo sigue propagándose, como hoy. Consecuencia buscada: la proyección de
pendientes de la vista consolidada deja de heredar el estado de la primera corrida y sus números de curso de COCA,
LABS, `lemma-count` y recurrencia pasan a ser los correctos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: AuditEngine
        _change: modify
        exposes:
          - signature: "runAudit(AuditableCourse course, AnalyzerRunSelection selection): AuditReport"
            _change: add
    implementations:
      - name: IAuditEngine
        _change: modify
        requiresInject:
          - { name: contentAnalyzers, type: "List<ContentAnalyzer>", _change: delete }
          - { name: scoreAggregator, type: ScoreAggregator }
          - { name: analyzerCatalog, type: AnalyzerCatalog }
          - { name: findingCollector, type: FindingCollector }
          - { name: contextNumbersCalculator, type: ContextNumbersCalculator }
    packages:
      - name: catalog
        _change: add
        visibility: public
        models:
          - name: AnalyzerRunSelection
            _change: add
            type: record
            fields:
              - { name: analyzers, type: "List<String>" }
              - { name: policies, type: "Map<String,EvaluationRunPolicy>" }
```

## Recoger los hallazgos antes de agregar
El recolector corre después del recorrido de cada analizador y antes de la agregación porque en ese momento los
únicos puntajes del árbol son propios; después de agregar, un analizador que evalúa ejercicio y tema encontraría
en un tema un promedio que nunca evaluó. En vocabulario pide borradores sólo donde el puntaje propio es menor que 1 y
exige al menos uno; en errores los pide en todo nodo evaluado y exige «algún hallazgo que cuenta ⇔ puntaje menor
que 1» (R004 inv. 1 y 2). Valida cada borrador contra la ficha, estampa y ordena por analizador, regla y marca.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: FindingCollector
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "collect(AuditNode root, ContentAnalyzer analyzer, AnalyzerDescriptor card): void"
    packages:
      - name: findingengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultFindingCollector
            _change: add
            visibility: public
            implements: [FindingCollector]
```

## Publicar los números por contexto una sola vez
Hoy hay seis lugares que arman un puntaje general cada uno a su modo: el transformador de la vista del CLI
promedia las 7 claves sin barra (73,9 %); el listado de `get audits`, `get audit`, `analyze -f raw` y el tablero
promedian las 11 con los cuartos COCA (73,4 % en el curso; en el tablero, B2 da 51,0 % en vez de 63,5 %); la vista previa de impacto promedia
todas las dimensiones. El calculador corre una vez dentro del motor, así que todo `AuditReport` —de `analyze`, de la
vista consolidada, de la vista previa, de `stats`— trae el mismo número y esos seis lugares pasan a leerlo (R010).
El puntaje de vocabulario es el promedio simple de los de vocabulario presentes en el nodo, sin sub-métricas ni
jueces: sobre la base del 29/9 da 73,92 % (curso), 96,5 / 94,2 / 79,5 / 63,5 (niveles), idéntico al de hoy (R009,
R013). Los errores se cuentan por ejercicio y se suman hacia arriba; los porcentajes se recalculan en cada nodo. Las
dudas abiertas son locales a este calculador: con DOUBT-EJERCICIO-JUZGADO en A no hace nada más (con B, un campo
más); con DOUBT-HALLAZGO-DE-TEMA en A, el hallazgo de un tema cuenta en cada ejercicio del tema (con B o C, un conteo
más por analizador).

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: ContextNumbersCalculator
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "compute(AuditNode root, List<AnalyzerDescriptor> analyzers): void"
    packages:
      - name: contextnumbers
        _change: add
        visibility: public
        models:
          - name: ContextNumbers
            _change: add
            type: record
            fields:
              - { name: vocabularyScore, type: Double }
              - { name: analyzerScores, type: "List<AnalyzerScore>" }
              - { name: errors, type: ErrorCounts }
          - name: AnalyzerScore
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: family, type: AnalyzerFamily }
              - { name: score, type: Double }
              - { name: subMetrics, type: "List<SubMetricScore>" }
          - name: SubMetricScore
            _change: add
            type: record
            fields:
              - { name: name, type: String }
              - { name: score, type: double }
          - name: ErrorCounts
            _change: add
            type: record
            fields:
              - { name: quizzes, type: int }
              - { name: withAnyError, type: int }
              - { name: anyErrorShare, type: double }
              - { name: bySeverity, type: SeverityCounts }
              - { name: notFullyEvaluated, type: int }
              - { name: analyzers, type: "List<AnalyzerErrorCounts>" }
          - name: AnalyzerErrorCounts
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: reached, type: int }
              - { name: evaluated, type: int }
              - { name: notEvaluated, type: int }
              - { name: withError, type: int }
              - { name: errorShare, type: double }
              - { name: marked, type: int }
          - name: SeverityCounts
            _change: add
            type: record
            fields:
              - { name: blocking, type: int }
              - { name: high, type: int }
              - { name: medium, type: int }
              - { name: low, type: int }
      - name: findingengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultContextNumbersCalculator
            _change: add
            visibility: public
            implements: [ContextNumbersCalculator]
```

## Dejar un resumen chico para quien dibuja
El informe pesa 146 MB porque cada nivel serializa su entidad con todo lo que tiene adentro: medido sobre la base,
el curso aparece cuatro veces y es el 92 % del JSON. El almacén escribe al guardar un resumen sin entidades —ids,
etiquetas, números, hallazgos y lo sin evaluar de cada nodo, unos 10 a 15 MB— en un directorio hermano, para que
`list()` y `loadLatest()` no lo confundan con un informe; `delete` y `prune` lo borran junto con el suyo. Copia, no
calcula (R010). `get audits` y `get audit` lo leen en vez de parsear 146 MB por informe, y el tablero (paquete G) lo
lee directo del disco.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: AuditReportStore
        _change: modify
        exposes:
          - signature: "loadDigest(String id): Optional<AuditDigest>"
            _change: add
    packages:
      - name: contextnumbers
        _change: add
        visibility: public
        models:
          - name: AuditDigest
            _change: add
            type: record
            fields:
              - { name: auditId, type: String }
              - { name: root, type: DigestNode }
          - name: DigestNode
            _change: add
            type: record
            fields:
              - { name: nodeId, type: String }
              - { name: target, type: AuditTarget }
              - { name: label, type: String }
              - { name: numbers, type: ContextNumbers }
              - { name: findings, type: "List<Finding>" }
              - { name: unevaluatedBy, type: "List<String>" }
              - { name: children, type: "List<DigestNode>" }
```

## Que el runner valide y delegue
El runner pasa de seis dependencias a cuatro: ya no arma motores ni conoce jueces. Valida cada nombre pedido,
excluido o con tope contra el catálogo antes de cargar el curso —cargarlo tokeniza con spaCy y tarda—, así que un
nombre desconocido rechaza la corrida con el mensaje de F-CLIRV-R016 y no se escribe informe (R012). Las
sobrecargas viejas `runAudit(Path, Set)` y `runDetailedAudit(Path, String)` conservan su sentido de hoy: sólo corren
analizadores instantáneos, para que ninguna puerta vieja le pague a un juez sin que nadie lo pida (lo que el
arquitecto de FEAT-QINST advirtió el 3/8).

```architecture
modules:
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultAuditRunner
        _change: modify
        requiresInject:
          - { name: courseRepository, type: CourseRepository }
          - { name: courseToAuditableMapper, type: CourseToAuditableMapper }
          - { name: auditEngine, type: AuditEngine }
          - { name: allAnalyzers, type: "List<ContentAnalyzer>", _change: delete }
          - { name: scoreAggregator, type: ScoreAggregator, _change: delete }
          - { name: evaluationAnalyzerFactories, type: "List<EvaluationAnalyzerFactory>", _change: delete }
          - { name: analyzerCatalog, type: AnalyzerCatalog }
```

## Que get analyzers, config y stats lean del catálogo
El registro deja de recibir la lista de clásicos y la lista de configuraciones —la de `Main` no incluía la del juez—
y lee del catálogo: `quiz-instruction` aparece, y `config analyzer` de un analizador sin configuración muestra
«(no configuration)» en vez de «not found» (R005). `stats analyzer` corre sólo el analizador nombrado; si consulta
un modelo pago, con tope 0, que es la opción A de DOUBT-STATS-DE-LOS-JUECES: reutiliza lo registrado y declara lo
que falta. Las opciones B y C son un cambio de una línea en ese comando.

```architecture
modules:
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultAnalyzerRegistry
        _change: modify
        requiresInject:
          - { name: analyzers, type: "List<ContentAnalyzer>", _change: delete }
          - { name: configs, type: "List<SelfDescribingConfig>", _change: delete }
          - { name: analyzerCatalog, type: AnalyzerCatalog }
```

## Que el plan lea las fichas y declare lo que no convierte
Las listas por nivel y el switch por nombre de `DefaultRefinerEngine` son el enganche que perdió en silencio las
tareas del juez hasta F-QINST-R017. Ahora cada proveedor trae su `AnalyzerPlanBinding` y el plan no se edita para
sumar un analizador: queda sólo la constante de `DiagnosisKind`, y si falta, el plan lo declara en vez de descartarlo
(R004 inv. 3). La declaración rige para la familia de errores, así que el plan de vocabulario es el de hoy (R013). Es
un método aparte porque el constructor generado de `RefinementPlan` tiene 151 construcciones.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: UnconvertedScoreCount
        _change: add
        type: record
        fields:
          - { name: analyzer, type: String }
          - { name: nodeCount, type: int }
          - { name: reason, type: String }
    interfaces:
      - name: RefinerEngine
        _change: modify
        exposes:
          - signature: "unconvertedScores(AuditReport report): List<UnconvertedScoreCount>"
            _change: add
    implementations:
      - name: DefaultRefinerEngine
        _change: modify
        requiresInject:
          - { name: analyzerCatalog, type: AnalyzerCatalog }
  - name: audit-domain
    _change: modify
    packages:
      - name: catalog
        _change: add
        visibility: public
        models:
          - name: AnalyzerPlanBinding
            _change: add
            type: record
            fields:
              - { name: taskKind, type: String }
              - { name: taskTargets, type: "List<AuditTarget>" }
```

## Ponerle tope por nombre a cualquier juez
`--instruction-budget` está atado a la constante «quiz-instruction» de `AnalyzeCmd`: cada juez nuevo del paquete C
necesitaría otra opción, otro campo y otro mapeo. `--budget nombre=N` fija el tope con el nombre canónico (R005,
F-QINST-R015), se valida contra el catálogo y se rechaza sobre un analizador que no consulta modelos;
`--instruction-budget` queda como atajo. Las opciones de re-evaluación siguen siendo sólo del juez de consigna: acotar
corridas está fuera del alcance de FEAT-HALL.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: AnalyzeOptions
        _change: modify
        type: record
        fields:
          - { name: analyzerBudgets, type: "Map<String,Integer>", _change: add }
```

## Lo que un analizador nuevo agrega, y los enganches que quedan
Un analizador nuevo agrega su paquete con su analizador, su diagnóstico tipado y su proveedor, y nada más de lo
suyo toca archivos ajenos: el catálogo, `get analyzers`, `config`, `stats`, la validación de `analyze`, el tope, el
motor, los números, el resumen y el plan salen de su ficha. Quedan cuatro enganches, que el integrador junta en un
parche por ronda: (1) su bloque de paquete en `sentinel.yaml`; (2) el slot tipado de su diagnóstico en cada nivel que
diagnostica —la firma en `QuizDiagnoses` (u otro nivel) y el campo en `DefaultQuizDiagnoses`—, que se mantiene porque
el tipado es decisión del proyecto (DLABS); (3) una línea en la lista de proveedores de `Main`; y (4) si produce
tareas, la constante de `DiagnosisKind`. Los jueces con evaluador propio suman su cableado en `Main` y, si traen
biblioteca, su dependencia; la corrección de sus tareas (resolvedor de contexto, revisor, alcance de preservación,
JSON de `get task`) es del paquete E. La feature se registra acá para que `feature sync --feature FEAT-HALL`
pueda anotar J001 y J002 después de aplicar.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: AnalyzerProvider
        _change: add
        stereotype: factory
        sealed: false
        exposes:
          - signature: "analyzerName(): String"
          - signature: "describe(): AnalyzerDescriptor"
          - signature: "create(EvaluationRunPolicy policy): ContentAnalyzer"
          - signature: "planBinding(): Optional<AnalyzerPlanBinding>"
          - signature: "config(): Optional<SelfDescribingConfig>"
features:
  - id: FEAT-HALL
    code: F-HALL
```
