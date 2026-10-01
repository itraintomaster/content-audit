---
patch: FEAT-TAREA
requirement: 2026-09-30.21_una-tarea-por-hallazgo
generated: 2026-10-01T03:00:00Z
---

# Tech Spec: Una tarea por hallazgo (FEAT-TAREA)

Paquete E, ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. FEAT-HALL dejó el plan decidiendo por puntaje y
le pasó a E qué tarea crea cada analizador y cómo se corrige; este parche es esa parte: el origen de cada tarea, la
cuenta que cierra, la declaración de lo que cada arreglo puede tocar y la vista que comparten `get task` y la mesa. Lo
compartido que no trae —las constantes de `DiagnosisKind`, las declaraciones de los analizadores de A, C y D, `GetCmd`,
`PlanCmd` y `Main`— está en `ganchos-E.md`, uno por línea. Rebasado por el integrador de la ronda 2 sobre el `sentinel.yaml` de `ronda1/integracion` (la ronda 1 aplicada y construida) y sobre FEAT-RONDA2, que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño. Trae ahora lo que estaba en los ganchos E-03 a E-05 y E-07: ver la última sección.

## Declarar, por analizador, cómo se corrige lo que marca
El plan, la mesa y la tanda necesitan saber lo mismo de cada analizador de errores: qué partes del ejercicio puede
tocar su arreglo según se resuelva por regla o en la mesa (R006), qué otro analizador cubre lo que marca (R004), si sus
hallazgos se juntan en una tarea por tema (R003), qué reglas suyas son un desacuerdo para la bandeja (R005) y si pide
los dos arreglos de traducción e inglés (F-MESA-R003). Es un puerto que implementa cada analizador en su paquete, con el
molde de `AnalyzerProvider`: sumar un analizador suma su declaración y una línea en `Main`, y no toca el plan. No va en
`AnalyzerProvider` porque cambiarlo obligaría a tocar los diecisiete proveedores de 0.3 y de la ronda 1. Va en la raíz
porque las implementaciones viven en paquetes y así lo resuelve Sentinel.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: AnalyzerCorrection
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "analyzerName(): String"
          - signature: "editScope(FindingResolution resolution): List<ExercisePart>"
          - signature: "coveringAnalyzer(): Optional<String>"
          - signature: "groupsByTopic(): boolean"
          - signature: "tieRules(): List<String>"
          - signature: "offersTwoFixes(): boolean"
```

## Nombrar con un vocabulario cerrado lo que un arreglo toca
Una lista de rutas del JSON del curso sería frágil y cada analizador la escribiría distinta; un enum de partes del
ejercicio es lo que la tabla de R006 ya usa en palabras. `revision-domain` traduce cada parte a sus campos una sola vez
(ver «Traducir partes a campos»). `AUDIO` no es un campo sino un permiso: un arreglo sin él no puede cambiar lo que se
escucha (F-NOEMP-R004). `FORM_KIND` y `TOPIC_INSTRUCTIONS` existen sólo para el encargo de «Contracciones»
(F-MESA-R015), y `WHOLE_EXERCISE` para reescribir un duplicado sin borrarlo, porque una tanda no puede quitar
ejercicios (F-NOEMP-R007).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: correction
        _change: add
        visibility: public
        models:
          - name: ExercisePart
            _change: add
            type: enum
            fields:
              - { name: STATEMENT }
              - { name: HINT }
              - { name: ANSWERS }
              - { name: OPTIONS }
              - { name: FORM_KIND }
              - { name: TRANSLATION }
              - { name: MINI_THEORY }
              - { name: INSTRUCTIONS }
              - { name: TOPIC_INSTRUCTIONS }
              - { name: STORED_SENTENCE }
              - { name: AUDIO }
              - { name: WHOLE_EXERCISE }
```

## Juntar las declaraciones en un catálogo que falla al armarse
El plan, `get task`, la mesa y la tanda buscan la declaración por el nombre canónico del analizador, así que hace falta
un índice y uno solo. Se valida al construirse, contra el catálogo de analizadores: una declaración que nombra un
analizador que no existe, o que lo cubre con uno que no existe, rompe en el arranque y no en medio de una tanda. Las
dos declaraciones de analizadores que ya existían las escribe E en su propio paquete: `quiz-instruction`, con el
alcance de la corrección de consigna (F-QICOR-R003), y `lemma-absence`, cuya tarea entra a la tanda sólo cuando cubre
un hallazgo de errores. El paquete del motor se abre sólo a la raíz de composición, como `findingengine`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: AnalyzerCorrectionCatalog
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "find(String analyzerName): Optional<AnalyzerCorrection>"
          - signature: "list(): List<AnalyzerCorrection>"
    packages:
      - name: correctionengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultAnalyzerCorrectionCatalog
            _change: add
            visibility: public
            implements: [AnalyzerCorrectionCatalog]
            requiresInject:
              - { name: corrections, type: "List<AnalyzerCorrection>" }
              - { name: analyzerCatalog, type: AnalyzerCatalog }
          - name: QuizInstructionCorrection
            _change: add
            visibility: public
            implements: [AnalyzerCorrection]
          - name: LemmaAbsenceCorrection
            _change: add
            visibility: public
            implements: [AnalyzerCorrection]
```

## Guardar en la tarea de qué hallazgo salió
R002 pide que la tarea lleve la identidad de su hallazgo, su gravedad, su resolución, su costo, su nivel y su tema, y
que la tanda pueda decir si sigue en el análisis siguiente. Se agrega un solo campo a `RefinementTask`, nulo en las
tareas de vocabulario para que sigan siendo las de hoy (R007), con el hallazgo entero en vez de copiar sus ejes. La
alternativa —un archivo aparte junto al plan— no rompe constructores, pero deja dos fuentes que se pueden desalinear.
El costo es mecánico y conocido: `RefinementTask` se construye 206 veces en pruebas y una en código, y cada una suma un
argumento nulo al final; el constructor de `RefinementPlan` (153) no se toca. Una tarea del tema nombra los ejercicios
que alcanza (R003) y una que cubre lista los hallazgos cubiertos: queda resuelta sólo cuando ninguno aparece (R004).

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: RefinementTask
        _change: modify
        fields:
          - name: origin
            type: TaskOrigin
            _change: add
      - name: TaskOrigin
        _change: add
        type: record
        fields:
          - { name: finding, type: Finding }
          - { name: cefrLevel, type: CefrLevel }
          - { name: knowledgeId, type: String }
          - { name: knowledgeLabel, type: String }
          - { name: reachedQuizIds, type: "List<String>" }
          - { name: coveredFindings, type: "List<Finding>" }
```

## Cerrar la cuenta de hallazgos contra tareas
R001 exige que, por analizador, los hallazgos que cuentan sumen lo mismo que sus tareas, sus cubiertos y sus no
convertidos. `unconvertedScores` de 0.3 cuenta nodos, no hallazgos, así que la cuenta de R001 es un método aparte que el
comando `plan` imprime; `RefinementPlan` no cambia. El plan lee de la declaración qué cubre a qué y qué se agrupa por
tema: así queda resuelto el gancho D-04 de la ronda 1 (las palabras de otro nivel, cubiertas por la tarea de
`lemma-absence` del mismo ejercicio y la misma palabra) con una declaración de D y no con una regla general que
declararía cubierto por la tarea de largo lo que el largo no arregla. El orden de las tareas sigue saliendo del puntaje
(R008); a igual puntaje se desempata por nodo e identidad del hallazgo, que es lo que hace iguales dos planes del mismo
análisis (R002).

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: PlanFindingTally
        _change: add
        type: record
        fields:
          - { name: analyzer, type: String }
          - { name: countingFindings, type: int }
          - { name: ownTasks, type: int }
          - { name: covered, type: int }
          - { name: unconverted, type: int }
          - { name: unconvertedReason, type: String }
    interfaces:
      - name: RefinerEngine
        _change: modify
        exposes:
          - signature: "findingTally(AuditReport report, RefinementPlan plan): List<PlanFindingTally>"
            _change: add
    implementations:
      - name: DefaultRefinerEngine
        _change: modify
        requiresInject:
          - name: analyzerCorrectionCatalog
            type: AnalyzerCorrectionCatalog
            _change: add
```

## Armar una sola vez lo que se sabe de una tarea
`get task` y la mesa reciben lo mismo (R009, F-MESA-R003): el ejercicio como lo ve el alumno, el hallazgo con su
evidencia, lo cubierto, lo que el arreglo puede tocar y, en una tarea del tema, los ejercicios que alcanza. Un puerto
propio evita que la CLI y la mesa lo armen cada una a su modo. Usa el renderizador «como lo ve el alumno» que declara
el integrador de la ronda 1 (gancho A-01) y busca el nodo por índice, sin recorrer 11.760 nodos por tarea. Una tarea sin
origen no tiene vista de errores y devuelve vacío; un tipo sin declaración trae `scopeDeclared` en falso, que la tanda
convierte en «sin alcance declarado» (R006).

```architecture
modules:
  - name: refiner-domain
    _change: modify
    packages:
      - name: errortask
        _change: add
        visibility: public
        models:
          - name: ErrorTaskBrief
            _change: add
            type: record
            fields:
              - { name: task, type: RefinementTask }
              - { name: finding, type: Finding }
              - { name: coveredFindings, type: "List<Finding>" }
              - { name: editScope, type: "List<ExercisePart>" }
              - { name: scopeDeclared, type: boolean }
              - { name: quizView, type: QuizStudentView }
              - { name: reachedQuizViews, type: "List<QuizStudentView>" }
        interfaces:
          - name: ErrorTaskBriefer
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "brief(AuditReport report, RefinementTask task): Optional<ErrorTaskBrief>"
        implementations:
          - name: DefaultErrorTaskBriefer
            _change: add
            visibility: public
            implements: [ErrorTaskBriefer]
            requiresInject:
              - { name: analyzerCorrectionCatalog, type: AnalyzerCorrectionCatalog }
              - { name: auditNodeIndexFactory, type: AuditNodeIndexFactory }
              - { name: quizStudentViewRenderer, type: QuizStudentViewRenderer }
```

## Traducir partes a campos y conservar todo lo demás
R006 dice que lo que un ejercicio puede cambiar es la unión de lo que declaran sus tareas, y que todo lo demás se
conserva: es F-RPRES-R001 con otra llave. Hoy `CorrectionScope` responde por tipo de diagnóstico y sólo conoce dos; una
sobrecarga por conjunto de partes deja la tabla de partes a campos en un solo lugar, y `PreservationCheck` gana una
verificación contra un alcance dado, que es lo que la tanda usa al publicar un ejercicio con varias tareas. La
miniteoría se permite en todos los ejercicios del tema que muestran el texto, y la consigna del tema una vez: son las
excepciones a F-RPRES-R003 que José aceptó el 30/9 (F-MESA-R012, F-MESA-R015). `approve` sigue usando la firma de hoy.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: preservation
        _change: modify
        visibility: public
        interfaces:
          - name: CorrectionScope
            _change: modify
            exposes:
              - signature: "changeableFields(Set<ExercisePart> parts, AuditTarget target): Set<ScopedField>"
                _change: add
          - name: PreservationCheck
            _change: modify
            exposes:
              - signature: "verifyWithin(CourseEntity courseBefore, CourseEntity courseAfter, CourseElementSnapshot revised, Set<ScopedField> changeable): List<PreservationViolation>"
                _change: add
```

## Filtrar las tareas por su hallazgo
R008 suma cinco filtros a `get tasks`. Van juntos en un registro opcional dentro de `GetTasksFilter`, para que nulo
quiera decir «como hoy» sin cinco argumentos sueltos; el costo es un argumento nulo en sus 78 construcciones de prueba.
Los valores se validan contra el catálogo y los enums —un analizador que no existe da el mensaje de F-CLIRV-R016— en
`GetCmd`, que también toca el paquete D: por eso su cambio está en los ganchos (E-07) y no en este parche.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: GetTasksFilter
        _change: modify
        fields:
          - name: findingFilter
            type: TaskFindingFilter
            _change: add
      - name: TaskFindingFilter
        _change: add
        type: record
        fields:
          - { name: analyzer, type: "Optional<String>" }
          - { name: resolution, type: "Optional<FindingResolution>" }
          - { name: severity, type: "Optional<FindingSeverity>" }
          - { name: level, type: "Optional<CefrLevel>" }
          - { name: knowledgeId, type: "Optional<String>" }
```

## Registrar FEAT-TAREA y dejar las pruebas sin modelos
La feature se registra para que `feature sync --feature FEAT-TAREA` anote J001 después de aplicar; su ubicación es del
qa-tester. Ninguna prueba de este parche consulta un modelo: el plan se prueba sobre informes armados a mano con los
hallazgos del ejercicio 11 de *Afirmativas regulares* (pista, una sola respuesta y traducción), una miniteoría de *In
time u on time* repetida en sus ejercicios y la palabra de otro nivel de «Where is my wallet?», y con el informe del
29/9 para las 4.036 tareas de vocabulario de R007. Cambios de cuerpo que el parche no declara: el plan y su cuenta en
`DefaultRefinerEngine`, la tabla de partes a campos en `DefaultCorrectionScope`, la verificación nueva en
`DefaultPreservationCheck` y las 206 + 78 construcciones con un argumento más.

```architecture
features:
  - id: FEAT-TAREA
    code: F-TAREA
modules:
  - name: audit-cli
    _change: modify
```

## Lo que suma la integración de la ronda 2
Cada analizador declara cómo se corrige lo que marca en una clase de su paquete que implementa `AnalyzerCorrection`
(R006): las cinco de A (gancho E-03), las tres de C (E-04), la de D (E-05) y, desde FEAT-RONDA2, las dos de B. Van en
este parche y no en el de la integración porque implementan una interfaz que declara éste; en sus paquetes, porque lo
que puede tocar un arreglo es del analizador. Las tablas de cada una están en R006 y en F-RONDA2-R009: la de
`off-level-words` no tiene alcance propio y nombra a `lemma-absence` como cobertura; la de traducción desempata con
`judges-disagree` y ofrece los dos arreglos; la de miniteoría agrupa por tema. `GetCmd` recibe el armado del detalle
de una tarea para `get task` y los filtros de `get tasks` (E-07): suma un argumento a su constructor.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: hintcoherence
        _change: modify
        implementations:
          - { name: HintCoherenceCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: answercompleteness
        _change: modify
        implementations:
          - { name: AnswerCompletenessCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: multiplechoicevalidity
        _change: modify
        implementations:
          - { name: MultipleChoiceValidityCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: datacoherence
        _change: modify
        implementations:
          - { name: DataCoherenceCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: exactduplicates
        _change: modify
        implementations:
          - { name: ExactDuplicatesCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: translationsenseengine
        _change: modify
        implementations:
          - { name: TranslationSenseCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: singleanswerengine
        _change: modify
        implementations:
          - { name: SingleAnswerCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: minitheoryengine
        _change: modify
        implementations:
          - { name: MiniTheoryCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: offlevelwords
        _change: modify
        implementations:
          - { name: OffLevelWordsCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: grammar
        _change: modify
        implementations:
          - { name: GrammarCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
      - name: nearduplicates
        _change: modify
        implementations:
          - { name: NearDuplicatesCorrection, _change: add, visibility: public, implements: [AnalyzerCorrection] }
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: GetCmd
            _change: modify
            requiresInject:
              - { name: errorTaskBriefer, type: ErrorTaskBriefer, _change: add }
```

## Declarar las 49 pruebas y ubicar el journey
Las 49 son nuevas y cubren las nueve reglas (R001 6, R002 3, R003 4, R004 4, R005 2, R006 16, R007 3, R008 5, R009 6).
Van sobre lo que existe cuando se aplica este parche: el plan (`DefaultRefinerEngine`, 18), la declaración de cada
analizador (14 en 12 implementaciones: el catálogo, las de consigna y lemas y las nueve de A, C y D), el armado del
detalle (`DefaultErrorTaskBriefer`, 5), la preservación por partes (`DefaultCorrectionScope` y `DefaultPreservationCheck`,
4), `PlanCmd` (1, la cuenta) y `GetCmd` (7, los filtros y `get task`). Ninguna consulta un modelo: el plan se arma sobre
informes hechos a mano y, para R007, sobre el del 29/9.

**De dónde sale cada prueba.** El ejercicio 11 de *Afirmativas regulares* (`…1ecd`, A1), «I ___ a lot of vegetables.»:
pista alta (19 de 20 la llevan), una sola respuesta bloqueante (eat, like, buy y want encajan) y traducción, tres tareas
de un ejercicio. La miniteoría de «$In time$ u $on time$» (B1, `…d37d`), la misma en sus 40 ejercicios: una tarea del
tema con uno propio y 39 cubiertos. «Contraste inicial» (A1): ocho miniteorías, la de los verbos de estado en nueve
ejercicios. «She spread the warm blue quilt on the bed.» (A2, `…484a`): spread y quilt son B2, las marcan
`lemma-absence` y `off-level-words`, y la tarea `LEMMA_ABSENCE` del ejercicio cubre las dos (D-04, sin regla general que
cubra con la del largo). Las declaraciones siguen la tabla de R006 con un caso de cada analizador: «They ___ (not / be)
from Ireland.» (`…25ac`), «___ a child? (He)» (`…1e87`), «This idea ___ (not / sound) very good.» (`…2657`). Y las 4.036
tareas de vocabulario del 29/9, iguales una por una.

**Lo que no se declara o espera.** Que el catálogo de declaraciones rompa al armarse con un analizador que no existe no
tiene regla: no lleva prueba. Las declaraciones de `grammar` y `near-duplicates` son de F-RONDA2-R009 y esperan al apply,
como dejó el integrador. Tres partes de reglas de este parche las cumplen piezas de parches posteriores y se prueban allí
con su propia regla: lo que cubre una tarea se da por resuelto en la tanda (F-TANDA-R006, inv. 2), el desacuerdo de los
jueces aparece en la bandeja (F-BANDEJA-R002) y la mesa recibe el mismo detalle (F-MESA-R003). Una sola queda sin regla
propia donde vive: que un tipo sin declaración «nunca se intenta» lo cumple `DefaultTopicPanel`, que crea FEAT-MESA; su
prueba, trazada a F-TAREA-R006, va después de aplicar.

**El journey** va a `audit-cli`, en `com.learney.contentaudit.auditcli.commands`: sus pasos son `plan`, `get tasks` y
`get task`, y `PlanCmd` y `GetCmd` son package-private ahí, como en F-HALL-J001 y F-JEXAM-J001. `generate` crearía
`FTareaJ001JourneyTest` con 3 caminos; el de «no convertido» usa un analizador de reglas de prueba sin tipo de tarea,
como el de `FHallJ001JourneyTest`.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    implementations:
      - name: DefaultRefinerEngine
        _change: modify
        handwrittenTests:
          - name: "should open one task for each of the three counting findings of «I ___ a lot of vegetables.» (A1, Afirmativas regulares), from hint-coherence, single-answer and translation-sense, all three on the same exercise"
            traceability: { feature: FEAT-TAREA, rule: F-TAREA-R001 }
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: GetCmd
            _change: modify
            handwrittenTests:
              - name: "should reject 'get tasks' filtered by an analyzer outside the catalog with 'Analyzer 'hint-coherense' not found. Run 'content-audit get analyzers' to see available analyzers.' and exit non-zero"
                traceability: { feature: FEAT-TAREA, rule: F-TAREA-R008 }
features:
  - id: FEAT-TAREA
    code: F-TAREA
    journeys:
      - { id: F-TAREA-J001, testModule: audit-cli, testPackage: com.learney.contentaudit.auditcli.commands }
```
