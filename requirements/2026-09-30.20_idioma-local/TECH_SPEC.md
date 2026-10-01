---
patch: FEAT-MLOCAL
requirement: 2026-09-30.20_idioma-local
generated: 2026-09-30T21:30:00Z
---

# Tech Spec: Idioma local, herramientas y modelos que corren en la máquina (FEAT-MLOCAL)

Paquete B de la ronda 2, diseñado contra el `sentinel.yaml` de la rama `ronda2/idioma`, que ya tiene aplicados 0.2
(FEAT-OPMUL) y 0.3 (FEAT-HALL). Este parche trae las herramientas —LanguageTool, llama3 8B y nomic-embed-text— y lo
común a los tres analizadores de idioma; los analizadores están en los parches de FEAT-GRAMA, FEAT-NATUR y FEAT-DUPAR,
que se aplican después de éste. Lo que toca archivos compartidos (la ficha, las ranuras de diagnóstico, el runner,
`Main`, el libro de evaluaciones) no está acá: va, uno por línea, en
`docs/propuestas/tablero-corregir-sin-empeorar/ronda2/ganchos-B.md` de ittm.

De la ronda 1 usa una sola pieza: el registro de exámenes por versión de FEAT-JEXAM (`JudgeExamRegistry`, paquete C).
`patch propose` y `patch validate` no resuelven tipos, así que el parche valida hoy; `generate` necesita la ronda 1
integrada antes.

## Nombrar la herramienta, su versión y si se puede usar ahora

Las tres herramientas son distintas —una biblioteca dentro del proceso y dos modelos en un servicio local— pero las
reglas de FEAT-MLOCAL les piden lo mismo: nombre y versión a la vista (R002) y saber antes de empezar si falta (R003).
`LocalTool` es esa pregunta común. `status()` tiene que ser barato, sin cargar reglas ni modelos, porque lo llama la
ficha y el catálogo arma las fichas cada vez que arranca la CLI; el sondeo de Ollama se hace una vez por proceso y con
tope de 2 segundos.

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

## Decidir en un solo lugar si un analizador publica

R003 y R006 dicen lo mismo para los tres: sin herramienta o sin examen aprobado, sus ejercicios quedan sin evaluar,
con el motivo; si la corrida es el examen, publica, porque es lo que se mide. Escrita tres veces, esa decisión
divergiría; `LocalToolGate` la toma una vez por corrida, en el `create(policy)` de cada proveedor, y el analizador sólo
obedece. Lo que rinde examen es la versión de la herramienta más la del método del analizador —la pregunta, el prefijo,
el filtro—, así que cambiar el prompt también pide un examen nuevo. El registro es el de los jueces (FEAT-JEXAM), con
el nombre del analizador y esa versión: un segundo registro sería duplicar lo que la ronda 1 ya diseñó, y la
dependencia queda concentrada en `DefaultLocalToolGate`, que vive con `allowedClients: [audit-cli]` como los motores de
0.3.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: localtools
        _change: add
        visibility: public
        models:
          - name: LocalToolExamState
            _change: add
            type: enum
            fields:
              - { name: APPROVED }
              - { name: FAILED }
              - { name: NOT_TAKEN }
          - name: LocalToolGateDecision
            _change: add
            type: record
            fields:
              - { name: publishes, type: boolean }
              - { name: status, type: LocalToolStatus }
              - { name: examinedVersion, type: String }
              - { name: examState, type: LocalToolExamState }
              - { name: examRun, type: boolean }
              - { name: reason, type: String }
        interfaces:
          - name: LocalToolGate
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "decide(String analyzerName,LocalTool tool,String methodId,EvaluationRunPolicy policy): LocalToolGateDecision"
              - signature: "card(String analyzerName,LocalTool tool,String methodId): LocalToolCard"
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
              - { name: examRegistry, type: JudgeExamRegistry }
```

## Mostrar la herramienta y su examen en la ficha

R002 pide que la ficha nombre la herramienta con su versión y R006, que el resultado del examen quede junto a la
versión, en la ficha. `LocalToolCard` es esa tarjeta, y el proveedor la arma en `describe()` con `card(...)`. El campo
que la cuelga de `AnalyzerDescriptor` es compartido y va como gancho B-01; con él, el runner también puede rechazar un
pedido por nombre antes de cargar el curso leyendo sólo fichas (gancho B-05), y la interfaz de G la dibuja sin conocer
a nadie.

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
              - { name: examState, type: LocalToolExamState }
              - { name: examSummary, type: String }
```

## Dejar en el informe con qué midió cada corrida

R002 inv. 3 pide que, si un hallazgo cambia sin que cambie su ejercicio, el informe muestre que cambió la versión. Por
eso cada corrida deja, en el curso, una fila por analizador de idioma con la herramienta, la versión examinada, si
publicó y por qué no, y la cobertura del libro: la vista consolidada compara las filas por `analyzer` y la diferencia de
versión aparece sola. Una lista común y no una ranura por analizador, como la de los jueces de C: el gancho B-03 suma
una sola ranura.

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
              - { name: examState, type: LocalToolExamState }
              - { name: published, type: boolean }
              - { name: reason, type: String }
              - { name: coverage, type: EvaluationCoverage }
```

## Rechazar antes de cargar el curso lo que se pidió y no puede correr

Pedido por su nombre y sin herramienta, `analyze` rechaza antes de empezar y no escribe informe (R003 inv. 1). El
runner ya valida los nombres contra el catálogo antes de cargar el curso, porque cargarlo tokeniza con spaCy y tarda;
con la tarjeta de cada ficha puede hacer lo mismo con la herramienta. La excepción lleva el mensaje del requirement y
se lanza desde el runner (gancho B-05); sin selección no se lanza nunca: el analizador queda sin evaluar y los demás
corren.

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

## Guardar cada número de modelo en el libro que ya existe

R002 inv. 2 pide guardar el número de cada ejercicio con su contenido y su versión, y reusarlo mientras no cambien: con
llama3, una corrida larga y una corta difirieron hasta 0,025, lo bastante para mover una marca de borde. Es el libro de
evaluaciones de FEAT-EVCOST, sin costo: `naturalness` y `near-duplicates` abren una sesión con su propio `Evaluator` y
tope sin límite. Como el libro no hace de la versión una parte de la identidad (F-EVCOST-R006) y R002 sí lo pide para
los modelos locales, la versión examinada va en el id del evaluador: cada versión tiene su archivo y ninguna reusa lo de
otra. Hay un costo que este parche no puede arreglar: `FileSystemEvaluationLedger` relee el archivo entero en cada
consulta; con los 11.287 vectores de `near-duplicates` (unos 46 MB) eso es inusable, y el gancho B-09 le pide un índice
en memoria, sin cambiar su contrato.

## Un módulo para las herramientas, que sólo habla con esta máquina

LanguageTool y el cliente de Ollama son infraestructura: en audit-domain quedan sólo los puertos. Un módulo nuevo y no
`nlp-infrastructure`, que es la tokenización de spaCy detrás de su propia fábrica. `allowedClients: [audit-cli]`
porque sólo la raíz de composición lo construye (P8), y audit-cli pasa a depender de él; nadie más lo necesita, así que
va en este parche y no como gancho. R001 queda por construcción: LanguageTool corre dentro del proceso, y una dirección
de Ollama que no es de esta máquina no abre ninguna conexión y deja los modelos no disponibles, con el motivo; no es
una excepción, para que un mal valor en el entorno no tire comandos que no usan modelos.

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

## Cómo se prueba sin llamar a las herramientas

Ninguna prueba común llama a LanguageTool ni a Ollama. Los analizadores se prueban con dobles de `GrammarChecker`,
`YesNoModel` y `EmbeddingModel`, cargados con respuestas del prototipo del analista (`scratchpad/ronda2-B/`). Los
adaptadores de Ollama se prueban con un doble de `OllamaApi`, y `HttpOllamaApi` contra un `HttpServer` del JDK en
127.0.0.1 que devuelve respuestas grabadas: así se prueba también que una dirección no local no abre ninguna conexión.
`DefaultLocalToolGate` se prueba con un registro de exámenes en memoria: sin examen, reprobado, aprobado y la corrida de
examen. La prueba que usa las herramientas de verdad va aparte, con la etiqueta `herramienta-real`, y se saltea con
`Assumptions` si `status()` dice que no están; surefire la excluye por omisión (gancho B-14).

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
