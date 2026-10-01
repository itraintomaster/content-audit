---
patch: FEAT-CJUEZ
requirement: 2026-10-01.03_cierre-con-jueces
generated: 2026-10-01T14:49:41Z
---

# Tech Spec: cierre con jueces

Hoy `batch close` no le paga a ningún juez: la comparación de F corre a los jueces de claude -p con tope 0 y sólo reusa
lo que está en el libro. El cierre suma un paso nuevo, antes de comparar, que juzga con cada juez lo que la tanda cambia,
antes y después, dentro del tope que pide José; la estimación usa el mismo paso sin consultar. Que el veredicto nombre al
juez sin aprobar (R004) no cambia ningún contrato: lo hace el comparador con lo que ya recibe, la foto proyectada, el
catálogo y el nombre visible de FEAT-FILAS. No hay dependencias nuevas entre módulos: todo lo que usa el paso sale de
audit-domain, del que revision-domain ya depende.

## Agregar el pedido de cierre

El tope de un cierre no es el de `batch run`: aquél es de la mesa y éste es de los jueces, y cada cierre trae el suyo,
porque lo que pagó un cierre anterior ya está en el libro (R003 inv. 6). Por eso es un record propio y no un campo más de
la tanda: va de `batch close` al servicio y del servicio al cierre, y queda guardado en el cierre para el informe. Sin
ningún tope es un ensayo, como hoy (R003 inv. 1); el reajuste de José y la bandeja cierran con el pedido vacío.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batch
    models:
    - name: BatchCloseRequest
      type: record
      fields:
      - name: judgeBudgets
        type: "Map<String,Integer>"
      - name: queryCap
        type: Integer
      _change: add
    _change: modify
  _change: modify
```

## Agregar la cuenta del juez

La cuenta es la misma en la estimación, en el cierre y en `batch report`, así que es un solo record en el paquete público
`batch`, el que lee audit-cli. Lleva el nombre único, el que nombra `--budget`, y el visible, el que lee José; separa lo
gastado, lo reusado y lo que falta porque R002 y R005 los piden por separado. `local` marca al juez que consultó su
modelo o su herramienta local: se muestra igual, pero no gasta topes ni suma a los totales, porque es gratis
(F-MLOCAL-R008). `approved` es falso sólo para un juez sin aprobar: una herramienta local que no está instalada no lo
está, y su motivo va en `reason`.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batch
    models:
    - name: BatchJudgeAccount
      type: record
      fields:
      - name: judge
        type: String
      - name: displayName
        type: String
      - name: unit
        type: AuditTarget
      - name: approved
        type: boolean
      - name: examStatus
        type: JudgeExamStatus
      - name: reason
        type: String
      - name: units
        type: int
      - name: budget
        type: Integer
      - name: queriesSpent
        type: int
      - name: queriesReused
        type: int
      - name: queriesMissing
        type: int
      - name: floorsDecided
        type: int
      - name: floorsUndecided
        type: int
      - name: unavailableReason
        type: String
      - name: local
        type: boolean
      _change: add
    _change: modify
  _change: modify
```

## Guardar el pedido y las cuentas en cada cierre

`BatchClosing` ya es el registro de cada cierre dentro de la tanda: guardar ahí el pedido y las cuentas deja a la vista lo
que pasó en ese cierre sin volver a correr nada, y `batch report` lo lee del último. Los campos van al final para no mover
los que ya están. La tienda de la tanda guarda los campos nuevos sin cambios, y una tanda guardada antes los lee nulos.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batch
    models:
    - name: BatchClosing
      type: record
      fields:
      - name: request
        type: BatchCloseRequest
        _change: add
      - name: judges
        type: List<BatchJudgeAccount>
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Sumar los jueces a la estimación

`batch estimate` tiene que decir, antes de pagar, cuánto falta por juez y cuánto suman los aprobados (R002). El
estimador de la mesa no cambia de contrato: el servicio llena los cuatro campos nuevos con lo que le da el paso de los
jueces. `changedQuizzes` es la misma cuenta con que el paso elige qué juzgar (R001), así que conviene que los dos la saquen
de un mismo ayudante del paquete, como `BatchBook`: el «sobre los N ejercicios» de la salida nunca tiene que decir otra
cosa que lo que se juzga. Las horas siguen la cuenta de `estimatedHours`: 19 s por consulta, con las consultas a la vez de
la tanda.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batch
    models:
    - name: BatchEstimate
      type: record
      fields:
      - name: changedQuizzes
        type: int
        _change: add
      - name: judges
        type: List<BatchJudgeAccount>
        _change: add
      - name: judgeQueries
        type: int
        _change: add
      - name: judgeHours
        type: double
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Sumar al informe las cuentas del último cierre

R005 pide que `batch report` dé las mismas cuentas que el cierre. El armado del informe las copia del último cierre de
la tanda, así que `BatchReportBuilder` no cambia de contrato. Lo que se les pagó a los jueces al cerrar también entra en
las consultas por puesto del informe (F-TANDA-R010), del lado de los jueces.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batch
    models:
    - name: BatchReport
      type: record
      fields:
      - name: judges
        type: List<BatchJudgeAccount>
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Pasar el pedido al servicio de la tanda

`close()` pasa a recibir el pedido porque es la única entrada de `batch close`. El servicio rechaza ahí, antes de
consultar nada, un nombre que no es de un juez de la tanda o un tope negativo (R003 inv. 4), contra la lista de jueces
que le da el paso. La bandeja cierra con el pedido vacío, y un pedido nulo vale como vacío.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batch
    interfaces:
    - name: BatchService
      stereotype: port
      exposes:
      - signature: "close(): BatchClosing"
        _change: delete
      - signature: "close(BatchCloseRequest request): BatchClosing"
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Entregar el catálogo al servicio de la tanda

El paso de los jueces necesita el catálogo para saber quiénes de la tanda son jueces, en qué orden y con qué nombre
visible. La fábrica arma el paso desde la configuración, como ya hace con el motor y con el mapper, así que el catálogo
entra ahí, al final. `Main` le pasa el mismo catálogo que ya le da a la mesa.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batch
    models:
    - name: BatchServiceConfig
      type: record
      fields:
      - name: analyzerCatalog
        type: AnalyzerCatalog
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Crear el paso de los jueces

Es un colaborador del motor de la tanda, como el estimador o el cierre: interno de `batchengine` y sin nada a la vista
fuera del paquete. Recibe el curso compuesto como argumento en vez de armarlo, así no depende del libro de cambios y
sirve igual para estimar desde el servicio y para pagar desde el cierre. `estimate` y `judge` devuelven la misma cuenta,
pero sólo `judge` paga; `judges` es la lista contra la que se valida `--budget`.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batchengine
    interfaces:
    - name: BatchJudgePass
      visibility: internal
      exposes:
      - signature: "estimate(CorrectionBatch batch, CourseEntity startCourse, CourseEntity composedCourse): List<BatchJudgeAccount>"
      - signature: "judge(CorrectionBatch batch, CourseEntity startCourse, CourseEntity composedCourse, BatchCloseRequest request): List<BatchJudgeAccount>"
      - signature: "judges(CorrectionBatch batch): List<String>"
      _change: add
    _change: modify
  _change: modify
```

## Implementar el paso sobre el motor del análisis

El paso corre el mismo motor que `analyze`, con `AnalyzerRunSelection` y `EvaluationRunPolicy` como ya existen, sobre
recortes del curso que conservan los ancestros reales, como hace `CatalogTopicMeter` en la mesa. Un juez de ejercicio lee
el ejercicio con su nivel y su tema, y la miniteoría el tema entero: las huellas del recorte son las del curso entero, y
lo que se juzga en el recorte lo reusa después la comparación. Por eso no cambia nada de audit-domain ni del libro, y el
paso nunca escribe el curso ni el historial (R001 inv. 4). Un ensayo con tope 0 para todos dice qué se reusa y qué falta;
en `judge`, cada juez aprobado y no local corre solo con lo que le queda de su tope, primero el antes y después el
después, y una falla del servicio lo corta (R003 inv. 5).

```architecture
modules:
- name: revision-domain
  packages:
  - name: batchengine
    implementations:
    - name: DefaultBatchJudgePass
      implements:
      - BatchJudgePass
      requiresInject:
      - name: auditEngine
        type: AuditEngine
      - name: courseMapper
        type: CourseMapper
      - name: analyzerCatalog
        type: AnalyzerCatalog
      _change: add
    _change: modify
  _change: modify
```

## Pagar a los jueces al cerrar

El cierre paga antes del bucle de la compuerta, así la comparación ya encuentra en el libro lo que se juzgó y puede
seguir sin tope para claude -p, como hoy (F-PROYU-R003). Al final, un ensayo sin consultas sobre lo que quedó dice lo que
falta de verdad: lo que un juez local juzgó al comparar ya no falta, y lo que la compuerta devolvió ya no cuenta. Los
pisos de cada juez salen de la última comparación. `judgePass` va al final del constructor; la fábrica arma un solo paso
y se lo da al cierre y al servicio.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batchengine
    interfaces:
    - name: BatchCloser
      visibility: internal
      exposes:
      - signature: "close(CorrectionBatch batch, CourseEntity startCourse): CorrectionBatch"
        _change: delete
      - signature: "close(CorrectionBatch batch, CourseEntity startCourse, BatchCloseRequest request): CorrectionBatch"
        _change: add
      _change: modify
    implementations:
    - name: DefaultBatchCloser
      requiresInject:
      - name: judgePass
        type: BatchJudgePass
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Darle el paso al servicio

El servicio usa el paso para estimar (R002) y para validar el pedido de cierre (R003 inv. 4), nunca para pagar, que es
del cierre. Va al final del constructor, como en el cierre.

```architecture
modules:
- name: revision-domain
  packages:
  - name: batchengine
    implementations:
    - name: DefaultBatchService
      requiresInject:
      - name: judgePass
        type: BatchJudgePass
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Recibir los topes en batch close

`closeBatch` recibe los topes ya leídos: el mapa de `--budget juez=N,…` y el número de `--cap`, el mismo `--cap` que ya
usa `batch run`. La línea de comandos rechaza con exit 2 lo que no se lee como un número de 0 o más; un nombre que no es
de un juez de la tanda lo rechaza el servicio. `BatchCommand` sigue sellado.

```architecture
modules:
- name: audit-cli
  interfaces:
  - name: BatchCommand
    stereotype: port
    sealed: true
    exposes:
    - signature: "closeBatch(): Integer"
      _change: delete
    - signature: "closeBatch(Map<String,Integer> judgeBudgets, Integer queryCap): Integer"
      _change: add
    _change: modify
  _change: modify
```

Después del generate dejan de compilar, y hay que ponerlos al día: los constructores con todos los campos de
`BatchClosing`, `BatchEstimate`, `BatchReport` y `BatchServiceConfig`; los de `DefaultBatchCloser` y
`DefaultBatchService`; las llamadas a `close()` del servicio y a `close(batch, course)` del cierre; y las
implementaciones de `BatchService` que no son la del motor (la perezosa de `Main` y dos de prueba en audit-cli).
