---
patch: FEAT-RONDA2
requirement: 2026-09-30.33_integracion-ronda-2
generated: 2026-10-01T02:30:00Z
---

# Tech Spec: Integración de la ronda 2 (FEAT-RONDA2)

Lo que comparten los trece parches de la ronda 2 —B, idioma con herramientas locales; E, corrección por tandas; F, antes
y después— en un solo parche contra el `sentinel.yaml` de `ronda1/integracion`: la ronda 1 aplicada y construida, con
el parche de cierre de la ronda 1 (FEAT-CIERRE, 2026-09-30.32) ya aplicado; por eso esta carpeta es la 33. Sale de los
56 ganchos de `ronda2/ganchos-{B,E,F}.md` y de dos riesgos de calidad que encontró el orquestador al correr `analyze`
sobre una copia. Se aplica primero; los trece se rebasaron encima, en el orden de la
última sección. Lo que no cambia `sentinel.yaml` —el cableado de `Main`, los `pom.xml` y los cuerpos que tocan archivos
comunes— queda en la sección «Lo que se construye sin tocar sentinel.yaml», para que el que construya la integración lo
haga una sola vez.

## Dar a los tres de B su lugar tipado en el ejercicio y en el curso
Es el mismo criterio de la ronda 1 (FEAT-RONDA1): cada analizador guarda su diagnóstico en un lugar con su tipo, no en un
mapa, y las dos interfaces las tocan todos los paquetes, así que el cambio va acá, una sola vez (ganchos B-02 y B-03). En
el ejercicio, uno por analizador; en el curso, una lista común de corridas de herramienta local —como la cobertura común
de los jueces— y la cola de Naturalidad por nivel. Las firmas nombran tipos que declaran los parches de B: por eso
`generate` va con la ronda entera aplicada. `QuizDiagnoses`, `CourseDiagnoses` y sus `Default*` no llevan `@Generated` y
se editan a mano según `sentinel.yaml`, como en la ronda 1.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: QuizDiagnoses
        _change: modify
        exposes:
          - signature: "getGrammarDiagnosis(): Optional<GrammarDiagnosis>"
            _change: add
          - signature: "getNaturalnessDiagnosis(): Optional<NaturalnessDiagnosis>"
            _change: add
          - signature: "getNearDuplicateDiagnosis(): Optional<NearDuplicateDiagnosis>"
            _change: add
      - name: CourseDiagnoses
        _change: modify
        exposes:
          - signature: "getLocalToolRuns(): List<LocalToolRunDiagnosis>"
            _change: add
          - signature: "getNaturalnessQueue(): Optional<NaturalnessQueueDiagnosis>"
            _change: add
```

## Colgar de la ficha las herramientas con que mide
La tarjeta de cada herramienta —nombre, versión, si está, su examen y si es la alternativa con claude -p— va en la ficha
y no en un lugar aparte, porque así el runner rechaza un pedido por nombre leyendo sólo fichas, antes de cargar el curso
(gancho B-05), y la UI la dibuja sin conocer a B (gancho B-01). La lista queda vacía en los diecisiete de las rondas
anteriores. El constructor generado crece de 12 a 13: `generate` rellena con `null` las 17 llamadas de los
proveedores, que hay que cambiar por `List.of()`, y quedan 12 construcciones en pruebas.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AnalyzerDescriptor
        _change: modify
        fields:
          - name: tools
            type: List<LocalToolCard>
            _change: add
```

## Guardar en el informe con qué medida midió cada analizador
La comparación de F muestra aparte un cambio de medida (F-METAS-R007) y la proyección mide con lo mismo que la línea base
(F-PROYU-R003): para eso cada informe tiene que decir, en su raíz, con qué ficha y qué configuración midió cada
analizador (ganchos F-01 a F-04, F-RONDA2-R004). La medida la calcula el catálogo, que tiene la ficha y la configuración
de cada proveedor, y el motor la deja en la raíz al terminar la corrida, en el orden del catálogo. La huella deja afuera
los textos y el estado del examen, y entra la herramienta con que midió: si Ollama no está y Naturalidad mide con
claude -p, es otra medida, y la comparación no tiene que leerlo como un curso que cambió. `AuditNode` no lleva
`@Generated`: el campo se suma a mano, con su getter y su setter, sin tocar sus constructores.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditNode
        _change: modify
        fields:
          - name: measures
            type: List<AnalyzerMeasure>
            _change: add
    interfaces:
      - name: AnalyzerCatalog
        _change: modify
        exposes:
          - signature: "measure(String analyzerName): Optional<AnalyzerMeasure>"
            _change: add
    packages:
      - name: catalog
        _change: modify
        models:
          - name: AnalyzerMeasure
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: fingerprint, type: String }
              - { name: config, type: "Map<String,Object>" }
```

## Frenar sin la lista de lemas o sin spaCy, y guardar los tokens por oración
Dos pedidos del orquestador y un gancho de E y F que caen en el mismo lugar (F-RONDA2-R005 a R007). Hoy `Main` busca la
lista de lemas en `../ittm/…` y, si no está, el procesador de Python asigna puestos de frecuencia vacíos sin fallar: A1 y
A2 dan 50,0 % y el curso 72,75. Encima, el caché de disco guarda el lote con una clave que es sólo el texto, y las
corridas siguientes reusan lo envenenado. El cambio: (1) el caché guarda cada oración aparte y un lote lanza spaCy una
sola vez con las que faltan (E-10 = F-07: 38 s con cualquier oración cambiada; unos 5 a 8 con esto); (2) la clave de
cada oración suma la huella del contenido de la lista y la del procesador, así que lo guardado con la clave vieja no se
lee más; y (3) si falta la lista, falta el procesador o hay oraciones sin caché y spaCy no corre, el tokenizador lanza
`NlpResourcesUnavailableException` y `analyze` lo imprime y sale con código 1, sin informe. Calcular la huella obliga a
leer la lista y el procesador antes de mirar el caché, así que un caché lleno ya no tapa que falten. `FallbackNlpTokenizer`
deja de devolver tokens básicos: el modo reducido de F-NLP-R030 queda reemplazado, con DOUBT-RONDA2-SPACY abierta. Lo
demás es cuerpo en el paquete `spacy`, que es de paquete: `NlpTokenizer` no cambia.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: NlpResourcesUnavailableException
        _change: add
        type: exception
        fields:
          - { name: resource, type: String }
          - { name: path, type: String }
          - { name: detail, type: String }
        message: "Falta %s en '%s': %s"
        extends: RuntimeException
  - name: nlp-infrastructure
    packages:
      - name: spacy
        implementations:
          - name: SpacyProcessRunner
          - name: SpacyNlpTokenizerFactory
```

## Un tipo de tarea por analizador de errores
Es el gancho E-01, más los dos de B (B-18): ocho constantes para A y C y dos para `grammar` y `near-duplicates`, cuyos
hallazgos cuentan como error y se arreglan por regla o en la mesa (F-RONDA2-R008, R009). `naturalness` no tiene: sólo
ordena (F-TAREA-R005). `off-level-words` tampoco: lo cubre `LEMMA_ABSENCE`, como en la ronda 1. Cada proveedor de A y C
pasa a devolver su ligadura con su tipo sobre el ejercicio (E-02, cuerpo); los de B, también. Si José elige la B de
DOUBT-RONDA2-TAREAS-B, salen `GRAMMAR` y `NEAR_DUPLICATES` y el plan declara sus hallazgos como no convertidos. Ningún
`switch` del código sobre `DiagnosisKind` deja de compilar: el censo no encontró uno sin `default`.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: DiagnosisKind
        _change: modify
        type: enum
        fields:
          - { name: HINT_COHERENCE, _change: add }
          - { name: ANSWER_COMPLETENESS, _change: add }
          - { name: MULTIPLE_CHOICE_VALIDITY, _change: add }
          - { name: DATA_COHERENCE, _change: add }
          - { name: EXACT_DUPLICATES, _change: add }
          - { name: TRANSLATION_SENSE, _change: add }
          - { name: SINGLE_ANSWER, _change: add }
          - { name: MINI_THEORY, _change: add }
          - { name: GRAMMAR, _change: add }
          - { name: NEAR_DUPLICATES, _change: add }
```

## Abrir a la mesa lo que dice el audio
La mesa de E mide lo que se escucha de cada candidato con la pieza que armó A para Datos coherentes (`CatalogTopicMeter` y
`PanelRunnerConfig`, FEAT-MESA y FEAT-NOEMP), pero esa pieza es interna de su paquete: así como está, la mesa no compila.
La revisión de visibilidad del modelo final lo encontró, y es lo único que encontró. Se abre la interfaz y su
implementación, que construye `Main` para pasársela a la mesa; no se crea otra: lo que dice el audio sale de un solo lugar.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: datacoherence
        _change: modify
        interfaces:
          - name: SpokenTextBuilder
            _change: modify
            visibility: public
        implementations:
          - name: DefaultSpokenTextBuilder
            _change: modify
            visibility: public
```

## Lo que cada paquete recibe al rebasar
Lo que implementa un contrato de E o inyecta un tipo de F va en el parche que lo declara, no acá: un `implements` hacia
una interfaz que todavía no existe no valida, y un `modify` de algo que no existe entraría vacío. Cada parche se arma
desde su commit de diseño (`integrador-2/rebase.py`), contra el modelo que deja el anterior:

- **METAS:** `DefaultBatchComparator` inyecta `TopicTeachingResolver` de D (gancho F-14), para no devolver un cambio por
  una palabra que el tema enseña, igual que la mesa.
- **TAREA:** `GetCmd` inyecta `ErrorTaskBriefer` (E-07); y once declaraciones de corrección, una por analizador en su
  paquete: las cinco de A (E-03), las tres de C (E-04), la de D (E-05) y las dos de B (F-RONDA2-R009).
- **TANDA:** `BatchServiceConfig` y `DefaultBatchService` reciben el libro de metas, porque abrir una tanda las fija
  (F-METAS-R006, gancho F-10); cinco arreglos por regla, cuatro de A (E-06) y el de `grammar` (F-RONDA2-R009); y el
  paquete `batch` suma `audit-infrastructure` a sus clientes, porque su almacén de archivos implementa `BatchStore`.
- **BANDEJA:** el paquete `tray` suma `audit-infrastructure` a sus clientes, por la misma razón.
- **Tres pérdidas silenciosas del apply, corregidas en cualquier parche:** una interfaz sellada que se modifica sin
  repetir `sealed: true` se abre (bug 09: `GetConsolidatedCommand` en PROYU); un enum modificado sin `type: enum` figura
  como record (bug 03: `RefinementTaskStatus` en TANDA); y el apply reemplaza la lista de patrones de un módulo en vez de
  sumarla: NOEMP, MESA, TANDA y BANDEJA declaraban cada uno su fábrica en `revision-domain`, y al final quedaba sólo la de
  la bandeja. Cada uno lleva ahora la lista entera, seis al final.

```architecture
features:
  - id: FEAT-RONDA2
    code: F-RONDA2
modules:
  - name: refiner-domain
    _change: modify
```

## Lo que se construye sin tocar sentinel.yaml
El resto de los ganchos es código o configuración en archivos comunes, y se hace una vez, con la integración:

- `Main` construye las herramientas de B (LanguageTool, Ollama con `CONTENT_AUDIT_OLLAMA_URL`, la compuerta) y registra
  sus tres proveedores después de los diecisiete (B-07, F-RONDA2-R001); construye lo de F (almacenes de línea base,
  proyecciones, metas y muestras; proyector, comparador, libro de metas, muestra ciega) y registra sus comandos (F-06); y
  lo de E (catálogo de correcciones, arreglos por regla, mesa, tanda, bandeja y sus comandos) (E-09).
- El motor deja la medida de cada analizador en la raíz y la corrida base deja afuera a los que tienen una regla de
  modelo local (F-04, F-RONDA2-R003); el catálogo calcula la medida (F-03).
- El tokenizador: caché por oración con huellas y sin modo reducido (F-RONDA2-R005 a R007). El libro de evaluaciones
  no cambia: 71ba5182 y `resolveAll`, que consulta todo antes de escribir, ya resuelven B-09 (medido en
  `memory/decisions.md`).
- Los proveedores de A y C devuelven su ligadura (E-02); el plan cuenta y declara (E-08); `get tasks` filtra y `get task`
  muestra el detalle (E-07); el runner rechaza lo que pide una herramienta que falta y no tiene alternativa (B-05); y el
  runner NO completa la política de los de B: sin `--budget` llega vacía y la completa el proveedor según la ruta (B-06,
  F-RONDA2-R002). Los dos agentes de la alternativa, `naturalness-yes-no` y `near-duplicates-same-exercise` (B-21), con
  salida determinista (B-22); el ritmo por omisión de los jueces, 176 por hora y no 189 (B-24, opcional).
- `pom.xml`: una sola versión de `opennlp-tools` (B-12, ver `memory/decisions.md`); `herramienta-real` fuera de lo que
  corre por omisión (B-14); `jdk.xml.totalEntitySizeLimit` si LanguageTool lo pide (B-13).
- Fuera de Sentinel: el agente de la mesa en `.sentinel/agents/panel-proposer/` (E-15), `medir.py` (B-15),
  `analysis/metas.json` (F-18), y calibrar Naturalidad en B1 y B2 antes de publicar (B-19).

```architecture
features:
  - id: FEAT-RONDA2
    code: F-RONDA2
modules:
  - name: audit-cli
    _change: modify
```

## Devolver los clientes de los paquetes, en un parche aparte al final
El `patch apply` del jar de hoy descarta los `allowedClients` de un paquete al crearlo y no los cambia al modificarlo
(bug 10). La ronda 2 crea siete paquetes que los declaran, y el paso 16 de la ronda 1, con otros cuatro, sigue pendiente
por lo mismo. `clientes-de-paquetes.yaml`, en esta carpeta, los devuelve a los once con la lista entera: sirve igual si el
arreglo une o reemplaza, y si antes entró o no el paso de la ronda 1. Suma lo que la ronda 2 necesita: `revision-domain`
en `offlevelwords` y `topicwords` (la comparación y la mesa leen lo que el tema enseña y el semáforo) y
`audit-infrastructure` en `batch` y `tray`. La revisión de quién usa cada paquete en el modelo final no encontró otro
cliente. Simulado: con el jar de hoy no deja nada; con el arreglo de dos líneas, los once quedan con sus clientes. No hay
fence: el parche aparte no está en `architectural_patch.yaml`.

## Declarar las 47 pruebas de FEAT-RONDA2
Van sobre implementaciones que existen cuando se aplica este parche, el primero de la ronda. Las de R009 y la parte de B
de R008 caen en piezas que crean TAREA, TANDA y B (`GrammarCorrection`, `NearDuplicatesCorrection`, `GrammarRuleFix` y
los proveedores de B): se proponen después de aplicar, con `--as=qa-tester`, porque sobre algo que todavía no existe
entrarían como implementaciones vacías. Las de R010 y las de `Main` esperan a que B, E y F enciendan lo suyo, como las
cuatro de la ronda 1. La regla del libro de evaluaciones que estuvo en el borrador salió: lo resuelven 71ba5182 y
`resolveAll`.

| Regla | Pruebas | Dónde | Caso real o número |
|---|---:|---|---|
| R001 | 3 | `Main` (2), `get` | los tres de B, «Gramática», «Naturalidad», «Duplicados parecidos», después de los diecisiete |
| R002 | 4 | `analyze`, runner (2), runtime de los jueces | `--budget naturalness=50`; tope de near-duplicates sí, de grammar no; el runner no completa la política; claude -p sin tope pedido no consulta |
| R003 | 3 | motor | naturalness y near-duplicates afuera aunque tenga `same-but-wildcard`; grammar adentro; catorce de veinte |
| R004 | 8 | catálogo (5), motor, almacén (2) | bandas 8 · 6 · 4 · 3 cambian la huella de lemma-count; los textos no; claude -p sí; informe viejo sin medidas |
| R005 | 4 | procesador (3), caché | spaCy una vez con lo que falta; nada si está todo; el orden; los mismos tokens |
| R006 | 4 | procesador | otra lista u otro procesador no reusan; la misma lista en otra ruta sí; la clave vieja no se lee |
| R007 | 6 | fábrica (4), `analyze` (2), `stats analyzer` | «Falta la lista de lemas en …», código 1, sin informe; sin tokens básicos |
| R008 | 11 | proveedores de A y C (8), plan (3) | `HINT_COHERENCE` … `MINI_THEORY`; naturalness sin tarea; off-level-words cubierto |
| R010 | 3 | runner, plan, `analyze` | 73,86 % y 96,02 · 93,60 · 78,87 · 63,86; las 4.036 de vocabulario; los 886 archivos |

```architecture
modules:
  - name: audit-domain
    implementations:
      - name: IAuditEngine
        handwrittenTests:
          - name: "should leave naturalness and near-duplicates out of the base run, even though near-duplicates has the instant rule same-but-wildcard"
            traceability: { feature: FEAT-RONDA2, rule: F-RONDA2-R003 }
    packages:
      - name: findingengine
        implementations:
          - name: DefaultAnalyzerCatalog
            handwrittenTests:
              - name: "should change the fingerprint of lemma-count when its bands go from 4 for all to 8, 6, 4 and 3, and keep the fingerprints of the other analyzers"
                traceability: { feature: FEAT-RONDA2, rule: F-RONDA2-R004 }
  - name: nlp-infrastructure
    packages:
      - name: spacy
        implementations:
          - name: SpacyProcessRunner
            handwrittenTests:
              - name: "should launch spaCy once per batch with only the sentences that are not in the cache"
                traceability: { feature: FEAT-RONDA2, rule: F-RONDA2-R005 }
              - name: "should never read an entry saved with the old key of the whole batch"
                traceability: { feature: FEAT-RONDA2, rule: F-RONDA2-R006 }
```

## Aplicar la ronda en orden, y generar al final
Orden: esta integración; B (MLOCAL, GRAMA, NATUR, DUPAR); F (PROYU, METAS y CIEGA, y después CERO); E (TAREA, NOEMP,
MESA, TANDA, BANDEJA); y los clientes de los paquetes, cuando Sentinel los conserve. B y F antes que E porque E usa sus
tipos y TAREA y TANDA suman piezas en paquetes de B. Simulado con la librería del jar (`integrador-2/cadena.sh`), cada
`patch validate` pasa contra el modelo que deja el anterior, con 0 conflictos; y aplicada con el CLI real sobre una copia
del worktree (`aplicar-ronda-2.sh`), la ronda da un `sentinel.yaml` idéntico byte a byte al simulado. Sobre el modelo
final no hay choques de nombre, tipos sin resolver ni referencias que no se puedan ver, pero `generate` aborta: los trece
J001 de B, E y F tienen flujo y nadie los ubicó todavía. Va primero el qa-tester de cada paquete, con sus pruebas y su
J001, y recién después `generate`, que crea 322 archivos, cambia 85 y deja para adaptar 9 llamados en `Main` y en dos
fábricas de revision-domain y 334 construcciones en pruebas (209 de `RefinementTask` y 78 de `GetTasksFilter`). No hace
falta `feature sync`: cada parche registra su feature con su journey al aplicarse. Esta feature no trae journeys.

```architecture
features:
  - id: FEAT-RONDA2
    code: F-RONDA2
modules:
  - name: audit-domain
    _change: modify
```
