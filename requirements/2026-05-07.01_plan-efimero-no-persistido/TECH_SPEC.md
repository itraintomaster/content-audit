---
patch: ARCH-PLANEF
requirement: 2026-05-07.01_plan-efimero-no-persistido
generated: 2026-05-09T00:00:00Z
---

# Tech Spec: Plan efimero / no persistido (F-PLANEF)

Este documento explica, una decision por seccion, el patch arquitectonico de F-PLANEF. La capa base (F-PLANEF-R001: enum `PlanStorageMode`, firma del verbo `plan` con el modo explicito, renderer efimero de visibilidad interna) ya quedo aplicada en una pasada anterior y no se vuelve a explicar aqui. Lo que sigue cubre F-PLANEF-R002 (contexto de correccion inline opt-in) y resuelve los doubts asociados (DOUBT-CTX-FLAG-SHAPE, DOUBT-CTX-IN-DISK-MODE, DOUBT-CTX-DEFAULT). El patch agrega una capacidad transversal (indice de nodos del audit), un puerto compartido para serializar el contexto, y extiende la firma del verbo `plan` con el flag opt-in.

## Por que extender la firma del verbo `plan` con el opt-in en el contrato

R002 #3 introduce un eje opt-in (contexto inline) que tiene que ser explicito en el contrato igual que el `storageMode` ya es explicito. Eso obliga a que sea un parametro del verbo, no un side-channel (campo del comando, var global, ThreadLocal). La firma final es `plan(String auditId, PlanStorageMode storageMode, boolean withCorrectionContext): Integer`. Asi el contrato sealed declara que toda invocacion del verbo `plan` resuelve modo y opt-in concretos antes de ejecutar; los tests pueden cruzar el verbo con cualquier combinacion `(auditId, storageMode, withCorrectionContext)` sin asumir defaults globales. La firma previa se borra en lugar de sumar una sobrecarga: la sobrecarga genera ambiguedad sobre cual es el path de produccion y no aporta a los tests.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: PlanCommand
        _change: modify
        sealed: true
        exposes:
          - signature: "plan(String auditId, PlanStorageMode storageMode): Integer"
            _change: delete
          - signature: "plan(String auditId, PlanStorageMode storageMode, boolean withCorrectionContext): Integer"
            _change: add
```

## Por que indexar el AuditReport en `audit-domain` (capacidad transversal)

R002 obliga a emitir contexto inline para todas las tareas del plan en una sola invocacion. La implementacion actual de los resolvers (`SentenceLengthContextResolver`, `LemmaAbsenceContextResolver`) usa un `findNode(root, nodeId, target)` recursivo: cada tarea desencadena un recorrido completo del arbol del `AuditReport`. Para planes grandes (decenas o centenas de tareas, miles de nodos) ese costo cuadratico-en-tareas es prohibitivo en una sola pasada. La solucion estructural es construir el indice `(nodeId, AuditTarget) -> AuditNode` una sola vez por `AuditReport` y resolver cada tarea en O(1). El indice es una **capacidad pura sobre la estructura del arbol** (no sabe de tareas, ni de DiagnosisKind, ni de plan), por eso vive en `audit-domain` -el modulo que define `AuditNode` y `AuditReport`-, no en `refiner-domain`. Que viva en el dominio del arbol es lo que permite que el dia que aparezca un tercer consumidor (analyzer, validator, otro resolver) pueda tomarlo sin atravesar `refiner-domain`. La factory aisla a los consumidores de la representacion concreta (Map vs estructura especializada): hoy es un Map; manana puede ser otra cosa sin romper el contrato.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: AuditNodeIndex
        _change: add
        stereotype: port
        visibility: public
        exposes:
          - signature: "find(String nodeId, AuditTarget nodeTarget): Optional<AuditNode>"
      - name: AuditNodeIndexFactory
        _change: add
        stereotype: factory
        visibility: public
        exposes:
          - signature: "build(AuditReport report): AuditNodeIndex"
    patterns:
      - type: Factory
        interface: AuditNodeIndexFactory
        implementations: ["DefaultAuditNodeIndexFactory"]
```

## Por que la implementacion del indice queda detras de un seam con factory y package interno

El indice tiene mas de un colaborador (la factory que recorre el arbol, el `MapAuditNodeIndex` respaldado por un mapa, una `NodeKey` interna). Si todos esos tipos quedaran en el package raiz de `audit-domain` se publicarian sin necesidad: ningun otro modulo construye el indice excepto via la factory. La aplicacion del patron Factory Seam (interfaz publica + factory publica + colaboradores package-private dentro de un package `internal`) cumple P2 (encapsulacion via package), P4 (un solo punto de fuga, la factory) y P5 (contrato/carrier/engine separados). El unico tipo publico del package es `DefaultAuditNodeIndexFactory`, que es lo que el composition root necesita instanciar; el resto -incluido `MapAuditNodeIndex`- queda invisible cross-modulo.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: auditnodeindex
        _change: add
        visibility: internal
        implementations:
          - name: DefaultAuditNodeIndexFactory
            _change: add
            implements: ["AuditNodeIndexFactory"]
            visibility: public
          - name: MapAuditNodeIndex
            _change: add
            implements: ["AuditNodeIndex"]
```

## Por que extender `CorrectionContextResolver` con `resolveWithIndex` en lugar de cambiar `resolve`

Los dos call-sites tienen necesidades distintas. `GetCmd` resuelve **una** tarea sobre un plan persistido cargado por id; ahi el costo del recorrido lineal es marginal y construir un indice solo para una task es desperdicio. El renderer efimero resuelve **todas** las tareas del plan en una sola invocacion; ahi precomputar el indice una vez es la diferencia entre 25 segundos y 50 milisegundos. La opcion alternativa -obligar al call-site single-task a tambien construir el indice- contamina el path mas frecuente del CLI con una construccion innecesaria. Por eso la interfaz queda con dos firmas: `resolve(report, task)` (lineal, single-task) y `resolveWithIndex(index, report, task)` (O(1), bulk). Las tres implementaciones (`SentenceLengthContextResolver`, `LemmaAbsenceContextResolver`, `DispatchingCorrectionContextResolver`) implementan ambas; la nueva delega al indice para localizar el nodo y reusa el resto de la logica de mapeo.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    interfaces:
      - name: CorrectionContextResolver
        _change: modify
        stereotype: port
        typeParameters: ["T extends CorrectionContext"]
        exposes:
          - signature: "resolve(AuditReport report, RefinementTask task): Optional<T>"
          - signature: "resolveWithIndex(AuditNodeIndex nodeIndex, AuditReport report, RefinementTask task): Optional<T>"
            _change: add
    implementations:
      - name: DispatchingCorrectionContextResolver
        _change: modify
        implements: ["CorrectionContextResolver"]
        visibility: public
      - name: SentenceLengthContextResolver
        _change: modify
        implements: ["CorrectionContextResolver"]
        visibility: public
      - name: LemmaAbsenceContextResolver
        _change: modify
        implements: ["CorrectionContextResolver"]
        visibility: public
```

## Por que extraer `CorrectionContextJsonMapper` a un puerto compartido

R002 #1 obliga la equivalencia de schema entre el `correctionContext` que `get task` entrega y el que el plan efimero emite inline. Esa equivalencia es estructural, no nominal: dos consumidores que parsean cualquiera de las dos salidas tienen que ver las mismas claves, los mismos tipos de valores, la misma forma de los sub-objetos. Hoy la logica de mapeo `CorrectionContext -> Map<String,Object>` vive privada en `GetCmd` (`buildSentenceLengthContextMap`, `buildLemmaAbsenceContextMap`). Si el renderer efimero la duplica, la equivalencia funcional pasa a depender de que dos implementaciones se mantengan sincronizadas a mano -un patron que falla la primera vez que alguien agrega un campo a uno y olvida el otro. La extraccion a un puerto package-private en `commands` con una sola implementacion (`DefaultCorrectionContextJsonMapper`) hace que la equivalencia sea estructural: ambos call-sites usan **la misma** instancia, no dos copias del mismo codigo. Esto encaja con P3 (Versatility on Demand): la extension se abre exactamente sobre el eje que la regla del feature pide -un solo serializador-, no antes y no por especulacion.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        interfaces:
          - name: CorrectionContextJsonMapper
            _change: add
            stereotype: port
            visibility: internal
            exposes:
              - signature: "toJsonMap(CorrectionContext context): Map<String,Object>"
        implementations:
          - name: DefaultCorrectionContextJsonMapper
            _change: add
            implements: ["CorrectionContextJsonMapper"]
            visibility: internal
```

## Por que el renderer efimero recibe el AuditReport y las opciones de render

R002 #1 requiere hidratar el contexto cuando la opcion esta activa, lo que necesita acceso al `AuditReport` (la fuente de la que se derivan los `correctionContext`). Hay dos caminos: el renderer carga el report internamente (necesita `AuditReportStore` inyectado) o lo recibe ya cargado del comando que lo orquesta. Elegimos el segundo porque `PlanCmd` ya carga el report (lo necesita para invocar `RefinerEngine.plan`) y volverlo a cargar dentro del renderer duplica I/O sin razon. El record `EphemeralRenderOptions` agrupa las opciones aditivas del modo efimero (hoy solo `withCorrectionContext`, manana otras) para que la firma del puerto no rompa cada vez que se agrega un opt-in. La firma vieja `render(plan): Integer` se borra en lugar de sumar overload por la misma razon que la firma de `PlanCommand`: una sola forma canonica del puerto evita ambiguedad.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: EphemeralRenderOptions
        _change: add
        type: record
        visibility: public
        fields:
          - name: withCorrectionContext
            type: boolean
            description: "F-PLANEF-R002: si true, cada task del plan emitido lleva inline su correctionContext (o correctionContext=null + correctionContextError si no es construible). Default OFF (DOUBT-CTX-DEFAULT opcion A)."
    packages:
      - name: commands
        _change: modify
        interfaces:
          - name: EphemeralPlanRenderer
            _change: modify
            stereotype: port
            visibility: internal
            exposes:
              - signature: "render(RefinementPlan plan): Integer"
                _change: delete
              - signature: "render(RefinementPlan plan, AuditReport report, EphemeralRenderOptions options): Integer"
                _change: add
```

## Por que el renderer inyecta factory + resolver + mapper

El renderer es donde los tres seams confluyen. Necesita `AuditNodeIndexFactory` para construir el indice una vez por invocacion (la construccion es responsabilidad del renderer, no de `PlanCmd`, porque solo el renderer sabe si la opcion esta activa). Necesita `CorrectionContextResolver` -en su forma dispatching- para resolver el contexto via el indice. Necesita `CorrectionContextJsonMapper` para serializar al JSON map exigido por R002 #1. Que los tres se inyecten via constructor (DI manual en el composition root, sin Spring) cumple Rule C del proyecto y deja todo el cableado explicito; el composition root es el unico lugar que ve los tres tipos concretos. Cuando la opcion esta apagada, el renderer rendea el plan al schema base de R001 sin tocar resolver ni mapper -el costo de la opcion off es exactamente cero respecto a F-PLANEF-R001.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: DefaultEphemeralPlanRenderer
            _change: modify
            implements: ["EphemeralPlanRenderer"]
            visibility: internal
            requiresInject:
              - name: auditNodeIndexFactory
                type: AuditNodeIndexFactory
              - name: correctionContextResolver
                type: CorrectionContextResolver
              - name: correctionContextJsonMapper
                type: CorrectionContextJsonMapper
```

## Por que `GetCmd` cambia para inyectar el mapper compartido

`GetCmd` ya sabe serializar `correctionContext` a JSON: tiene `buildSentenceLengthContextMap` y `buildLemmaAbsenceContextMap` privados. Para que la equivalencia de R002 #1 sea estructural (no copiable a mano), `GetCmd` deja de mantener esos metodos y delega al `CorrectionContextJsonMapper` inyectado -la misma instancia que recibe el renderer efimero. El cambio es interno al comando: la firma de `GetCommand.get` no cambia, ningun otro modulo se entera. Lo unico observable es que `GetCmd` aparece con `correctionContextJsonMapper` en su lista de `requiresInject`, y los tests que verifiquen la equivalencia de schema pueden inyectar la misma instancia en ambos comandos y comparar las salidas como cajas negras.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: GetCmd
            _change: modify
            implements: ["GetCommand"]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            requiresInject:
              - name: auditReportStore
                type: AuditReportStore
              - name: refinementPlanStore
                type: RefinementPlanStore
              - name: analyzerRegistry
                type: AnalyzerRegistry
              - name: correctionContextResolver
                type: CorrectionContextResolver
              - name: impactPreviewStore
                type: ImpactPreviewStore
              - name: impactPreviewFormatter
                type: ImpactPreviewFormatter
              - name: correctionContextJsonMapper
                type: CorrectionContextJsonMapper
            types:
              - Component
```

## Por que `PlanCmd` rechaza `withCorrectionContext=true` en modo DISK (DOUBT-CTX-IN-DISK-MODE)

R002 #4 deja como decision de arquitectura que pasa cuando la opcion se activa con `--storage=disk`. Las tres opciones razonables eran: (A) rechazo explicito con error, (B) no-op silencioso, (C) persistir el contexto inline tambien en modo disk. La opcion C cambia el schema del plan persistido y obliga a una migracion -fuera de alcance de este feature. La opcion B es ambigua (el operador piensa que activo algo que no tiene efecto) y rompe la regla durable del proyecto de no introducir comportamientos silenciosos. La opcion A es la unica consistente: el usuario sabe inmediatamente que la combinacion no es valida, sin sorpresas. La validacion vive en `PlanCmd` antes de cargar el report: si `storageMode == DISK && withCorrectionContext` el comando retorna un codigo de error y un mensaje a stderr; ningun calculo se ejecuta. La firma `plan(auditId, storageMode, withCorrectionContext): Integer` que el contrato sealed expone deja esa validacion directamente apuntable desde tests sin pasar por la maquinaria de picocli.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: PlanCmd
            _change: modify
            implements: ["PlanCommand"]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            requiresInject:
              - name: auditReportStore
                type: AuditReportStore
              - name: refinerEngine
                type: RefinerEngine
              - name: refinementPlanStore
                type: RefinementPlanStore
              - name: ephemeralPlanRenderer
                type: EphemeralPlanRenderer
            types:
              - Component
```

## Por que el shape del flag opt-in es booleano (DOUBT-CTX-FLAG-SHAPE) y por que el default es OFF (DOUBT-CTX-DEFAULT)

DOUBT-CTX-FLAG-SHAPE planteaba tres opciones: (A) `--with-correction-context` booleano, (B) reusar `--storage` con sub-opcion (`--storage=none --include=correction-context`), (C) variable de entorno. La opcion B es atractiva en el largo plazo (extensible a otros campos opt-in via `--include=...,...`) pero introduce una segunda gramatica para opciones del verbo `plan` antes de tener una segunda opcion concreta que la justifique -P3 explicito. Si manana aparece un segundo opt-in del modo efimero, abrir esa familia es un cambio aditivo del flag (introducir `--include` y deprecar `--with-correction-context`); hoy es ruido. La opcion C (env var) tiene los mismos problemas que en DOUBT-FLAG-SHAPE: no es per-invocacion, y el cliente del dashboard alterna comportamientos en una sola sesion. Queda la A: booleano simple, descriptivo, idiomatico. **Default OFF** (DOUBT-CTX-DEFAULT opcion A) por dos razones: (1) preserva el contrato de R001 -un cliente que ya parsea el plan efimero sin contexto sigue parseandolo igual cuando R002 esta disponible-, y (2) deja la decision al cliente: el dashboard pasa el flag explicito (la preferencia del operador), y otros clientes que solo necesiten metadata no pagan la latencia de hidratacion de contexto que no van a usar. El flag es solo del modo efimero (DOUBT-CTX-IN-DISK-MODE: rechazo en DISK), asi que no afecta el contrato del modo persistente. Estas decisiones se materializan en `EphemeralRenderOptions.withCorrectionContext` y en la firma `PlanCommand.plan(..., boolean withCorrectionContext)` ya cubiertos arriba; no hay elementos nuevos en el patch para esta seccion.

## Por que NO se generaliza el modo efimero ni el contexto inline a otros verbos

R001 cubre solo `plan` (DOUBT-OTHER-COMMANDS) y R002 cubre solo el contexto del plan. La tentacion es introducir hoy un contrato uniforme — por ejemplo, una capacidad transversal `EphemeralOutputStrategy` aplicable a cualquier verbo que persista, o un mecanismo de contexto inline reusable por `analyze` o `revise`. **Eso es exactamente el over-engineering que el operador rechazo en la primera pasada del feature.** P3 (Versatility on Demand) lo dice explicito: la extensibilidad se abre solo donde aparece evidencia concreta de demanda. El shape del flag (`--storage=<mode>` y `--with-correction-context`) es genericamente reusable si manana se decide repetir el patron en `analyze`, pero cada verbo lo hara en sus propios terminos: que significa "no escribir en disco" en `analyze` (no guardar el `AuditReport`?) o "contexto inline" en `revise` (que contexto?) son decisiones distintas que no son ahora casos reales. Hoy F-PLANEF solo abre el seam en `PlanCommand` y en `EphemeralPlanRenderer` porque solo ahi hay evidencia concreta de demanda. Esta seccion tampoco introduce elementos nuevos en el patch -es una decision negativa sobre que NO incluir.
