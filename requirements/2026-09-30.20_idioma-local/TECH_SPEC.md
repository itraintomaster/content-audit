---
patch: FEAT-MLOCAL
requirement: 2026-09-30.20_idioma-local
generated: 2026-09-30T23:55:00Z
---

# Tech Spec: Idioma local, herramientas y modelos que corren en la máquina (FEAT-MLOCAL)

Paquete B de la ronda 2. Este parche trae las herramientas —LanguageTool, llama3 8B y nomic-embed-text— y lo común a
los tres analizadores de idioma; los analizadores están en los parches de FEAT-GRAMA, FEAT-NATUR y FEAT-DUPAR, que se
aplican después de éste. Rebasado el 30/9 a la noche sobre el `sentinel.yaml` de `ronda1/integracion` (0.2, 0.3 y la
ronda 1 aplicados), con la decisión de José: «sin Ollama no se deja sin evaluar; recordá que tenemos claude -p».
`sentinel-baseline.yaml` queda como la foto de la rama `ronda2/idioma`, sin la ronda 1.

**Lo que se usa de C sin declararlo de nuevo**: el runtime de los jueces (`JudgeRuntime`: abre las sesiones con el
freno del examen, el tope 0 sin pedido y las 4 a la vez, planea qué pide cada unidad y declara cobertura y costo), sus
estados de examen (`JudgeExamStatus`), el evaluador de claude -p (`AgentJudgeFactory` con un `AgentJudgeSpec`, que
verifica la sesión antes de la primera consulta) y el comando `exam`. Lo que toca archivos compartidos —la ficha, las
ranuras, `Main`, las definiciones de agentes— va, uno por línea, en
`docs/propuestas/tablero-corregir-sin-empeorar/ronda2/ganchos-B.md` de ittm.

## Nombrar la herramienta, su versión y si se puede usar ahora

Las tres herramientas son distintas —una biblioteca dentro del proceso y dos modelos en un servicio local— pero las
reglas de FEAT-MLOCAL les piden lo mismo: nombre y versión a la vista (R002) y saber antes de empezar si falta (R003).
`LocalTool` es esa pregunta común. `status()` tiene que ser barato, sin cargar reglas ni modelos, porque lo llama la
ficha y el catálogo arma las fichas cada vez que arranca la CLI; el sondeo de Ollama se hace una vez por proceso y con
tope de 2 segundos. El mismo registro describe a la alternativa en su tarjeta, sin que la alternativa sea un `LocalTool`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: localtools
        _change: add
        visibility: public
        models:
          - name: LocalToolStatus
            _change: add
            type: record
            fields:
              - { name: tool, type: String }
              - { name: version, type: String }
              - { name: available, type: boolean }
              - { name: unavailableReason, type: String }
              - { name: installHint, type: String }
        interfaces:
          - name: LocalTool
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "status(): LocalToolStatus"
```

## Un puerto por lo que se le pide a cada herramienta

Cada analizador le pide una cosa distinta a su herramienta: marcas de LanguageTool en una variante del inglés, la
probabilidad de «sí» y «no» en el primer token, o el vector de una oración. Son tres capacidades y tres puertos, y
ninguno sabe de negocio: el cruce de las dos variantes, el umbral de cada nivel y el comodín viven en cada analizador.
Por eso `GrammarChecker` recibe la variante en vez de devolver ya cruzado: que la marca la den el americano y el
británico en el mismo lugar es la regla R002 de Gramática, no una propiedad de LanguageTool. Los tres extienden
`LocalTool`, así que quien tiene el puerto tiene también su estado.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: localtools
        _change: add
        visibility: public
        models:
          - name: EnglishVariant
            _change: add
            type: enum
            fields:
              - { name: AMERICAN }
              - { name: BRITISH }
          - name: GrammarMatch
            _change: add
            type: record
            fields:
              - { name: ruleId, type: String }
              - { name: categoryId, type: String }
              - { name: start, type: int }
              - { name: end, type: int }
              - { name: message, type: String }
              - { name: suggestions, type: "List<String>" }
          - name: YesNoProbabilities
            _change: add
            type: record
            fields:
              - { name: yesProbability, type: double }
              - { name: noProbability, type: double }
          - name: SentenceEmbedding
            _change: add
            type: record
            fields:
              - { name: values, type: "List<Double>" }
        interfaces:
          - name: GrammarChecker
            _change: add
            stereotype: port
            sealed: false
            extends: [LocalTool]
            exposes:
              - signature: "check(String sentence,EnglishVariant variant): List<GrammarMatch>"
          - name: YesNoModel
            _change: add
            stereotype: port
            sealed: false
            extends: [LocalTool]
            exposes:
              - signature: "ask(String system,String question): YesNoProbabilities"
          - name: EmbeddingModel
            _change: add
            stereotype: port
            sealed: false
            extends: [LocalTool]
            exposes:
              - signature: "embed(String text): SentenceEmbedding"
```

## Decidir en un solo lugar si publica la herramienta local, con el examen de los jueces

R003 y R006 dicen lo mismo para los tres: sin herramienta o sin examen aprobado, lo que depende de ella queda sin
evaluar, con el motivo; si la corrida es el examen, publica, porque es lo que se mide. `LocalToolGate.decide` lo toma
una vez por corrida, en el `create(policy)` de cada proveedor. El examen ya no se lee del registro a mano ni con un
estado propio: `DefaultLocalToolGate` le pregunta a `JudgeRuntime.standing` y usa `JudgeExamStatus`, así un registro
TAKEN o una versión sin enunciar se leen igual que para los jueces y el motivo es el mismo texto. La decisión trae
además `localPolicy`, la política con que se consulta el modelo local: la de la corrida o, sin pedido, sin tope y de a
una. Es el único lugar donde vive el gancho B-06: el tope 0 de los jueces no alcanza a los modelos locales, y la
alternativa nunca usa esta política.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: localtools
        _change: add
        visibility: public
        models:
          - name: LocalToolGateDecision
            _change: add
            type: record
            fields:
              - { name: publishes, type: boolean }
              - { name: status, type: LocalToolStatus }
              - { name: examinedVersion, type: String }
              - { name: examState, type: JudgeExamStatus }
              - { name: examRun, type: boolean }
              - { name: reason, type: String }
              - { name: localPolicy, type: EvaluationRunPolicy }
        interfaces:
          - name: LocalToolGate
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "decide(String analyzerName,LocalTool tool,String methodId,EvaluationRunPolicy policy): LocalToolGateDecision"
      - name: localtoolsengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultLocalToolGate
            _change: add
            visibility: public
            implements: [LocalToolGate]
            requiresInject:
              - { name: judgeRuntime, type: JudgeRuntime }
```

## Sin Ollama, la alternativa: claude -p por el runtime de los jueces

José decidió el 30/9 a la noche que sin Ollama Naturalidad y Duplicados parecidos le preguntan a claude -p (R007). La
alternativa no se modela acá: es un `Evaluator` de C, hecho en `Main` con `AgentJudgeFactory` y un `AgentJudgeSpec`
por analizador (gancho B-21), que cada proveedor recibe ya construido, como reciben sus evaluadores los jueces de C.
Si `decide` dice que el modelo local no está, el proveedor abre con `JudgeRuntime.open(nombre, [alternativa], política
de la corrida)`: el runtime pone el tope 0 sin pedido (R008, F-JEXAM-R010), frena una versión sin examen, verifica la
sesión antes de la primera consulta, consulta 4 a la vez y declara lo que falta y lo que cuesta. Con el modelo local
abre igual por el runtime, con su evaluador y `localPolicy`: un solo camino de sesiones, freno y cobertura para las dos
rutas, y ninguno propio de B.

## Mostrar la herramienta, la alternativa y sus exámenes en la ficha

R002 pide que la ficha nombre la herramienta con su versión y R006, que el resultado del examen quede junto a la
versión. `LocalToolCard` es esa tarjeta; el proveedor la arma en `describe()` con `card(...)` y, si tiene alternativa,
suma la de claude -p con `alternativeCard(...)`, que lee la versión del evaluador sin consultarlo y su examen del
runtime. `alternative` separa las dos: la de claude -p sólo se usa sin el modelo local y sólo consulta con tope. Con
las dos tarjetas en la ficha (gancho B-01), el runner rechaza un pedido por nombre sólo si ninguna está disponible
(gancho B-05) y la interfaz de G las dibuja sin conocer a nadie.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: localtools
        _change: add
        visibility: public
        models:
          - name: LocalToolCard
            _change: add
            type: record
            fields:
              - { name: status, type: LocalToolStatus }
              - { name: examinedVersion, type: String }
              - { name: examState, type: JudgeExamStatus }
              - { name: examSummary, type: String }
              - { name: alternative, type: boolean }
        interfaces:
          - name: LocalToolGate
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "card(String analyzerName,LocalTool tool,String methodId): LocalToolCard"
              - signature: "alternativeCard(String analyzerName,Evaluator alternative): LocalToolCard"
```

## Dejar en el informe con qué midió cada corrida

R002 inv. 3 pide que, si un hallazgo cambia sin que cambie su ejercicio, el informe muestre que cambió la versión o la
herramienta. Cada corrida deja, en el curso, una fila por analizador de idioma con la herramienta local, la versión
examinada, si publicó y por qué no, y si usó la alternativa (gancho B-03). La cobertura salió de esta fila: los dos de
modelo abren por el runtime de los jueces, así que su cobertura, el examen de la versión consultada, lo que falta y su
costo van en la ranura común de C (`CourseDiagnoses.getJudgeCoverages`, C-04), con el mismo nombre; duplicarla acá
sería tener dos verdades para lo mismo.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: localtools
        _change: add
        visibility: public
        models:
          - name: LocalToolRunDiagnosis
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: status, type: LocalToolStatus }
              - { name: examinedVersion, type: String }
              - { name: examState, type: JudgeExamStatus }
              - { name: published, type: boolean }
              - { name: reason, type: String }
              - { name: alternative, type: boolean }
```

## Rechazar antes de cargar el curso lo que se pidió y no puede correr

Pedido por su nombre, sin herramienta y sin alternativa, `analyze` rechaza antes de empezar y no escribe informe (R003
inv. 1). El runner ya valida los nombres contra el catálogo antes de cargar el curso, porque cargarlo tokeniza con spaCy
y tarda; con las tarjetas de cada ficha puede hacer lo mismo con las herramientas. Con la alternativa disponible no se
rechaza (inv. 2): sin tope, el runtime reusa lo que claude -p ya contestó y declara el costo de lo que falta, como con
cualquier juez. Sin selección no se lanza nunca.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: localtools
        _change: add
        visibility: public
        models:
          - name: LocalToolUnavailableException
            _change: add
            type: exception
            extends: RuntimeException
            message: "Analyzer '%s' needs %s, which is not available: %s."
            fields:
              - { name: analyzerName, type: String }
              - { name: requiredTool, type: String }
              - { name: installHint, type: String }
```

## Guardar cada número y cada respuesta en el libro que ya existe

R002 inv. 2 pide reusar mientras no cambien contenido y versión. Los dos de modelo abren sus sesiones con
`JudgeRuntime.open`, así que todo va al libro de evaluaciones de FEAT-EVCOST. Con el modelo local, la versión examinada
va en el id del evaluador: cada versión tiene su archivo y ninguna reusa lo de otra, que es lo que R002 pide y el libro
no hace solo (F-EVCOST-R006). Con claude -p rige la regla de los jueces: el evaluador es el del agente y reusa el último
veredicto de ese contenido; Naturalidad pone su pregunta dentro del contenido, así que cambiar la pregunta vuelve a
consultar. El costo de leer el libro lo resolvió la ronda 1: `FileSystemEvaluationLedger` lee cada archivo una vez
mientras no cambie (71ba5182), y `resolveAll` decide qué consulta antes de escribir, así que los 46 MB de vectores se leen
una vez por corrida (gancho B-09).

## Un módulo para las herramientas, que sólo habla con esta máquina

LanguageTool y el cliente de Ollama son infraestructura: en audit-domain quedan sólo los puertos. Un módulo nuevo y no
`nlp-infrastructure`, que es la tokenización de spaCy detrás de su propia fábrica. `allowedClients: [audit-cli]`
porque sólo la raíz de composición lo construye (P8), y audit-cli pasa a depender de él. R001 queda por construcción:
LanguageTool corre dentro del proceso, y una dirección de Ollama que no es de esta máquina no abre ninguna conexión y
deja los modelos no disponibles, con el motivo; no es una excepción, para que un mal valor en el entorno no tire
comandos que no usan modelos. «off» los apaga a pedido: es como se rinde el examen de la alternativa en una máquina que
tiene Ollama. claude -p no vive acá: es de los jueces, en quiz-instruction-infrastructure.

```architecture
modules:
  - name: audit-cli
    _change: modify
    dependsOn: [local-language-infrastructure]
  - name: local-language-infrastructure
    _change: add
    dependsOn: [audit-domain]
    allowedClients: [audit-cli]
    uses: [jackson-databind, languagetool]
    models:
      - name: OllamaEndpoint
        _change: add
        type: record
        fields:
          - { name: baseUrl, type: String }
          - { name: timeoutSeconds, type: int }
```

## LanguageTool 6.8 dentro del proceso

José eligió LanguageTool 6.x como biblioteca. La vigente es la 6.8 (`org.languagetool:language-en:6.8`, del 5/5/2026
según `maven-metadata.xml`; el índice de búsqueda de Maven Central todavía dice 6.6), compilada para Java 17 como el
proyecto y con licencia LGPL 2.1. Los números del requirement se midieron con la 6.5 sin agrupador; por eso la 6.8
rinde su examen antes de publicar. Dos instancias, americana y británica, creadas la primera vez que se usan: cruzarlas
es la regla R002, y crearlas cuesta alrededor de un segundo que no tiene que pagar cada arranque de la CLI. Dos riesgos
para el developer, con su gancho: la 6.8 trae `opennlp-tools` 1.9.4 y langchain4j 1.13.0 trae la 2.5.4 (B-12), y con
Java 24 o más su `grammar.xml` supera `jdk.xml.totalEntitySizeLimit`, que el adaptador sube si nadie lo fijó (B-13).

```architecture
dependencies:
  - alias: languagetool
    artifact: org.languagetool:language-en:6.8
    scope: compile
    provides:
      - { type: JLanguageTool, package: org.languagetool }
      - { type: Language, package: org.languagetool }
      - { type: AmericanEnglish, package: org.languagetool.language }
      - { type: BritishEnglish, package: org.languagetool.language }
      - { type: RuleMatch, package: org.languagetool.rules }
      - { type: Rule, package: org.languagetool.rules }
      - { type: Category, package: org.languagetool.rules }
      - { type: CategoryId, package: org.languagetool.rules }
modules:
  - name: local-language-infrastructure
    _change: add
    packages:
      - name: languagetool
        _change: add
        visibility: public
        implementations:
          - name: LanguageToolGrammarChecker
            _change: add
            visibility: public
            implements: [GrammarChecker]
```

## Los dos modelos de Ollama detrás de una fábrica

Los adaptadores de llama3 y de nomic-embed-text comparten el cliente HTTP y el sondeo de `/api/tags`, que da la huella
del modelo y con ella la versión. Esa gráfica queda en el paquete y sólo la fábrica es pública (P2, P5); es una sola
fábrica y no dos porque es una sola herramienta sirviendo dos clases de modelo, y el modelo que usa cada analizador lo
elige la raíz de composición (`llama3:8b`, `nomic-embed-text`: gancho B-07). La pregunta de sí o no va a
`/api/generate` con logprobs de un token y sus 20 alternativas; el vector, a `/api/embed`.

```architecture
modules:
  - name: local-language-infrastructure
    _change: add
    interfaces:
      - name: OllamaModelFactory
        _change: add
        stereotype: factory
        sealed: false
        exposes:
          - signature: "yesNoModel(OllamaEndpoint endpoint,String model): YesNoModel"
          - signature: "embeddingModel(OllamaEndpoint endpoint,String model): EmbeddingModel"
    patterns:
      - type: Factory
        interface: OllamaModelFactory
        implementations: [DefaultOllamaModelFactory]
    packages:
      - name: ollama
        _change: add
        visibility: public
        models:
          - name: OllamaTokenProbability
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: token, type: String }
              - { name: logprob, type: double }
        interfaces:
          - name: OllamaApi
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "probe(String model): LocalToolStatus"
              - signature: "firstTokenProbabilities(String model,String system,String prompt): List<OllamaTokenProbability>"
              - signature: "embed(String model,String input): List<Double>"
        implementations:
          - name: DefaultOllamaModelFactory
            _change: add
            visibility: public
            implements: [OllamaModelFactory]
          - name: HttpOllamaApi
            _change: add
            implements: [OllamaApi]
            requiresInject:
              - { name: endpoint, type: OllamaEndpoint }
          - name: OllamaYesNoModel
            _change: add
            implements: [YesNoModel]
            requiresInject:
              - { name: api, type: OllamaApi }
              - { name: model, type: String }
          - name: OllamaEmbeddingModel
            _change: add
            implements: [EmbeddingModel]
            requiresInject:
              - { name: api, type: OllamaApi }
              - { name: model, type: String }
```

## Cómo se prueba sin llamar a las herramientas ni a claude

Ninguna prueba común llama a LanguageTool, a Ollama ni a claude. Los analizadores se prueban con dobles de
`GrammarChecker`, `YesNoModel` y `EmbeddingModel`, cargados con respuestas del prototipo del analista
(`scratchpad/ronda2-B/`), y con un `Evaluator` doble para la alternativa, con la forma que emite su agente, que cuenta
cuántas veces lo llaman, sobre el `DefaultJudgeRuntime` de C con libros en memoria. Los adaptadores de Ollama se prueban
con un doble de `OllamaApi`, y `HttpOllamaApi` contra un `HttpServer` del JDK en 127.0.0.1 que devuelve respuestas
grabadas: así se prueba también que una dirección no local o «off» no abre ninguna conexión. `DefaultLocalToolGate` se
prueba con un registro de exámenes en memoria detrás del runtime: sin examen, reprobado, rendido sin medir, aprobado,
la corrida de examen y la política sin tope ni pedido. La prueba que usa las herramientas de verdad va aparte, con la
etiqueta `herramienta-real`, y se saltea con `Assumptions` si `status()` dice que no están; surefire la excluye por
omisión (gancho B-14). Ninguna prueba consulta a claude -p de verdad.

## Registrar FEAT-MLOCAL

El parche registra la feature con su código para que `feature sync --feature FEAT-MLOCAL` anote J001 después de
aplicar; la ubicación del journey es del qa-tester.

```architecture
modules:
  - name: audit-domain
    _change: modify
features:
  - id: FEAT-MLOCAL
    code: F-MLOCAL
```
