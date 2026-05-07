---
patch: ARCH-CDIFF-005
requirement: 2026-05-06.01_consolidated-differential-view
generated: 2026-05-06T22:00:00Z
supersedes: ARCH-CDIFF-004 (aplicado; este patch refactoriza el contrato de NodeImpact a la forma uniforme `field → tripleta`)
---

# Tech Spec: FEAT-CDIFF v3 — refactor a forma uniforme `field → tripleta`

ARCH-CDIFF-001 a 004 ya estan aplicados a `sentinel.yaml`. Esta iteracion (ARCH-CDIFF-005) materializa el refactor del REQUIREMENT.md v3 (Grupo D nuevo: descubrimiento dinamico de fields, exclusiones por rol funcional, anidamiento por path estable, diff de listas por identidad declarada). El cambio es estructuralmente grande pero quirurgicamente acotado: tres modelos nuevos en `consolidatedview` (FieldChange, FieldPath, FieldExclusionRole), un package nuevo `fielddiff` con el motor recursivo y sus dos registros centrales, un campo nuevo en NodeImpact, tres campos viejos eliminados de NodeImpact, StatisticImpact entero eliminado, statisticImpacts eliminado de ConsolidatedView, y el builder + el formatter adaptados.

## Eliminar StatisticImpact y la lista paralela `statisticImpacts`

R009 prohibe explicitamente que la salida tenga una seccion paralela de "estadisticas afectadas": los scores son fields del mapa como cualquier otra hoja escalar. R010 prohibe los deltas precomputados. Conservar `StatisticImpact` con sus cuatro escalares numericos seria una rama paralela del contrato que el consumidor tendria que leer aparte; lo eliminamos en una sola pasada para que la UI tenga **un solo iterador**: el mapa `fieldChanges` de cada `NodeImpact`. El campo `consolidatedView.statisticImpacts` se va; el modelo `StatisticImpact` se va. Cualquier consumidor que necesitaba `acceptedDelta` lo computa como `consolidated - original` sobre la tripleta del field correspondiente (R010 detail 2).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: consolidatedview
        _change: modify
        models:
          - name: StatisticImpact
            _change: delete
          - name: ConsolidatedView
            _change: modify
            fields:
              - { name: statisticImpacts, type: "List<StatisticImpact>", _change: delete }
```

## Refactorizar NodeImpact al mapa uniforme `fieldChanges`

R006 obliga forma uniforme: cada nodo afectado expone un mapa donde la clave identifica un field y el valor es la tripleta `(original, consolidated, pendingProjection)`. Los snapshots crudos de v2 (`consolidated: CourseElementSnapshot`, `pendingProjection: CourseElementSnapshot`) y el `pendingProposalId` singular ya no caben: el texto de la oracion del quiz y el `priority` de un `AbsentLemma` viven bajo la misma forma. R011 (nuevo) pluraliza la trazabilidad de pendientes — un nodo puede acumular varias pendientes aplicables sobre fields distintos, no una sola.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: consolidatedview
        _change: modify
        models:
          - name: NodeImpact
            _change: modify
            fields:
              - { name: consolidated, type: CourseElementSnapshot, _change: delete }
              - { name: pendingProjection, type: CourseElementSnapshot, _change: delete }
              - { name: pendingProposalId, type: String, _change: delete }
              - { name: fieldChanges, type: "Map<String,FieldChange>", _change: add }
              - { name: pendingProposalIds, type: "List<String>", _change: add }
```

## Introducir FieldChange como tripleta polimorfica

R007 obliga las cinco invariantes de la tripleta. La hoja escalar puede ser primitivo boxed (`Integer`, `Double`), `String`, `enum` o cualquier valor "leaf" del recorrido — no hay un tipo Java que cubra los cuatro sin un union type. Java no lo tiene; el carrier mas honesto es `Object` con tres slots, donde `null` significa "ausente en esta foto" (caso real cuando una pendiente saca un `AbsentLemma` de la lista del milestone — sus componentes desaparecen de `pendingProjection`). El formatter es el unico que interpreta el tipo dinamico; el dominio solo lo transporta. Mantener tres records tipados (uno por tipo de leaf) explotaria la API a 4-5 variantes de `FieldChange` y el motor R019 (recorrido recursivo) tendria que multiplexar el tipo en cada hoja — alto coste por cero ganancia de seguridad real.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: consolidatedview
        _change: modify
        models:
          - name: FieldChange
            _change: add
            type: record
            visibility: public
            fields:
              - { name: original, type: Object }
              - { name: consolidated, type: Object }
              - { name: pendingProjection, type: Object }
```

## Capturar el path estable como tipo dedicado FieldPath

R023 obliga clave estable entre fotos y entre nodos comparables. La sintaxis exacta es decision arquitectonica; encerramos esa decision en un record dedicado para que el separador y la representacion vivan en un solo lugar (no como literal disperso en el motor y en el formatter). Forma elegida: dot-separator entre pasos, `[clave=valor]` para listas con identidad (claves naturales en orden lexicografico de nombre), `[indice]` para listas posicionales. Ejemplo: `diagnoses.lemmaAbsence.absentLemmas[lemma=run,pos=VERB].priority`. El consumidor recorre el mapa `Map<String, FieldChange>` con la clave como string crudo; quien quiera operar sobre el path estructuralmente reconstruye el `FieldPath` desde `raw`.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: consolidatedview
        _change: modify
        models:
          - name: FieldPath
            _change: add
            type: record
            visibility: public
            fields:
              - { name: raw, type: String }
```

## Codificar las cinco categorias de exclusion R020 como enum

R020 fija cinco categorias de hojas que NO se emiten aunque difieran. El enum las hace explicitas en el dominio y permite tooling de auditoria sobre el registry: cualquier regla del registry queda atribuida a una de las cinco causas, sin nombres ad-hoc. El enum es publico porque las reglas funcionales mismas (con su rol asociado) viven en el package interno `fielddiff`, pero el rol — la taxonomia conceptual — es parte del contrato del dominio R020.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: consolidatedview
        _change: modify
        models:
          - name: FieldExclusionRole
            _change: add
            type: enum
            visibility: public
            fields:
              - { name: OPAQUE_PERSISTENCE_ID }
              - { name: STRUCTURAL_REFERENCE }
              - { name: TIMESTAMP }
              - { name: INTERNAL_ORDER }
              - { name: NON_SEMANTIC }
```

## Contrato del motor: NodeFieldDiffer

R019 obliga el recorrido recursivo. Lo modelamos como un puerto publico de `consolidatedview` con una sola operacion: dadas las tres fotos del mismo nodo, devolver el mapa `fieldChanges`. La firma fija el contrato observable; la implementacion (que vive en `fielddiff`, internal) es libre de usar reflection sobre records, comparar AuditableEntity y NodeDiagnoses, etc. Si los tres `AuditNode` son `null` el resultado es vacio — el contrato esta diseñado para que el builder pueda invocarlo de forma uniforme aunque algunos nodos no existan en alguna foto.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: consolidatedview
        _change: modify
        interfaces:
          - name: NodeFieldDiffer
            _change: add
            stereotype: port
            visibility: public
            exposes:
              - signature: "diff(AuditNode original, AuditNode consolidated, AuditNode pendingProjection): Map<String,FieldChange>"
```

## Nuevo package fielddiff: motor + dos registros centrales

R020 y R022 piden mecanismos de declaracion de exclusiones y de identidad de listas. Las opciones evaluadas: (a) anotaciones sobre los records existentes, (b) registro central, (c) convencion de nombre. Anotaciones exigen modificar records de `audit-domain` y `course-domain` (FEAT-DLABS / FEAT-DCOCA / FEAT-COCA / FEAT-COURSE) — alto blast radius, pelea con la disciplina del proyecto de mantener los records limpios. Convencion es fragil. **Registro central gana**: dos clases (`DefaultFieldExclusionRegistry`, `DefaultListIdentityRegistry`) que cargan al construirse las reglas seed de los tipos conocidos hoy; consultadas por el motor en cada hoja / lista. El package `fielddiff` (no `strategy`, no `core`: el nombre describe el concepto — diferenciacion a nivel de field) es internal por defecto; solo `NodeFieldDifferFactory` cruza la frontera para que la composition root pueda instanciar el motor.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: fielddiff
        _change: add
        visibility: internal
        interfaces:
          - name: FieldExclusionRegistry
            _change: add
            stereotype: port
            visibility: internal
            exposes:
              - signature: "isExcluded(String declaringTypeName, String fieldName): boolean"
              - signature: "getRole(String declaringTypeName, String fieldName): Optional<FieldExclusionRole>"
          - name: ListIdentityRegistry
            _change: add
            stereotype: port
            visibility: internal
            exposes:
              - signature: "getKeySpec(String declaringTypeName, String fieldName): Optional<ListIdentityKeySpec>"
          - name: NodeFieldDifferFactory
            _change: add
            stereotype: factory
            visibility: public
            exposes:
              - signature: "create(): NodeFieldDiffer"
```

## Carriers internos del recorrido (FieldExclusionRule, ListIdentityRule, ListIdentityKeySpec)

Los tres records son internos al package `fielddiff` porque solo el motor los lee; ningun consumidor externo los consulta. Mantenerlos visibles arriba (en `consolidatedview`) seria filtrar la mecanica del registry al contrato publico. R020 y R022 obligan que la informacion exista — no obligan donde vive.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: fielddiff
        _change: add
        models:
          - name: FieldExclusionRule
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: declaringTypeName, type: String }
              - { name: fieldName, type: String }
              - { name: role, type: FieldExclusionRole }
          - name: ListIdentityKeySpec
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: fieldNames, type: "List<String>" }
          - name: ListIdentityRule
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: declaringTypeName, type: String }
              - { name: fieldName, type: String }
              - { name: keySpec, type: ListIdentityKeySpec }
```

## Implementaciones del motor: recursivo + dos registros + factory seam

`RecursiveNodeFieldDiffer` es la implementacion package-private del recorrido (reflection sobre records, comparacion hoja a hoja, consulta a los dos registros). `DefaultFieldExclusionRegistry` y `DefaultListIdentityRegistry` cargan al construirse las reglas seed para los tipos conocidos (AuditableQuiz/Knowledge/Topic/Milestone/Course; AuditNode; LemmaAbsenceLevelDiagnosis; CocaBucketsLevelDiagnosis; etc.). `DefaultNodeFieldDifferFactory` es la unica clase publica del package — el seam por donde la composition root y el test del builder construyen el motor sin tocar las internals.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: fielddiff
        _change: add
        implementations:
          - name: RecursiveNodeFieldDiffer
            _change: add
            visibility: internal
            requiresInject:
              - { name: exclusionRegistry, type: FieldExclusionRegistry }
              - { name: identityRegistry, type: ListIdentityRegistry }
            implements: ["NodeFieldDiffer"]
          - name: DefaultFieldExclusionRegistry
            _change: add
            visibility: internal
            implements: ["FieldExclusionRegistry"]
          - name: DefaultListIdentityRegistry
            _change: add
            visibility: internal
            implements: ["ListIdentityRegistry"]
          - name: DefaultNodeFieldDifferFactory
            _change: add
            visibility: public
            implements: ["NodeFieldDifferFactory"]
```

## Inyectar NodeFieldDiffer en el builder y en el Config

`DefaultConsolidatedViewBuilder` adquiere una dependencia mas: `nodeFieldDiffer`. Es la frontera entre "que campos cambiaron" (motor) y "que decisiones del plan los provocaron" (builder). El builder ya no recolecta snapshots crudos ni `statisticImpacts`; delega TODO el descubrimiento al motor y se queda con la mitad superior — wiring de aceptadas/pendientes, ejecucion del AuditEngine sobre las dos fotos, resolucion del par activo, manejo de UNAVAILABLE. `ConsolidatedViewBuilderConfig` extiende su shape con el campo equivalente; `DefaultConsolidatedViewBuilderFactory.create()` lo unpackea como hace con los otros 8 puertos. Los handwrittenTests v2 que asumian `consolidated` / `pendingProjection` como CourseElementSnapshot, `pendingProposalId` singular o StatisticImpact se eliminan en bloque — son contrato muerto. La lista de tests post-refactor la disena `@qa-tester`.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: ConsolidatedViewBuilderConfig
        _change: modify
        fields:
          - { name: nodeFieldDiffer, type: NodeFieldDiffer, _change: add }
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultConsolidatedViewBuilder
            _change: modify
            requiresInject:
              - { name: nodeFieldDiffer, type: NodeFieldDiffer, _change: add }
```

## Adaptar el formatter del CLI a la nueva forma de salida

`DefaultConsolidatedViewFormatter` es el unico componente del CLI que tenia que conocer la forma anterior (snapshots crudos + lista paralela `statisticImpacts`). El refactor lo simplifica: itera el mapa `fieldChanges` de cada `NodeImpact` y emite `{ '<path>': { original, consolidated, pendingProjection } }`. Mantiene `activeAuditId`, `activePlanId`, `consolidatedAvailability` + `unavailability` y `nonApplicablePendings` sin cambios. Los tres handwrittenTests v2 que asumian StatisticImpact se borran; el resto sobrevive (R001 raiz, R012 nonApplicablePendings, R013 UNAVAILABLE) porque su contrato no cambio. La forma JSON exacta queda como decision de presentacion del formatter.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: formatting
        _change: modify
        implementations:
          - name: DefaultConsolidatedViewFormatter
            _change: modify
```
