---
patch: FEAT-CERO
requirement: 2026-09-30.29_cada-corrida-desde-cero
generated: 2026-10-01T03:45:00Z
---

# Tech Spec: Cada corrida del motor empieza de cero (FEAT-CERO)

Paquete F, ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. La pregunta del orquestador era qué le queda
propio a FEAT-CERO si el diseño de FEAT-HALL ya construye los analizadores en cada corrida. Respuesta, verificada en el
modelo y en el código de este worktree: ningún cambio de estructura; le quedan el registro de la feature, la prueba que
la fija y una garantía que el developer tiene que cumplir al implementar los proveedores de HALL.

## Dejar la estructura como la dejó FEAT-HALL
El arrastre medido —72,90 en vez de 73,92, y los 55 topics de la primera foto sin bandas— viene de cuatro analizadores
que guardan en campos lo que cuentan (`CocaBucketsAnalyzer`, `LemmaRecurrenceAnalyzer`, `LemmaByLevelAbsenceAnalyzer`,
`LemmaCountAnalyzer`) y que hoy se construyen una sola vez en `Main`. En el modelo aplicado, `IAuditEngine` ya no inyecta
analizadores: pide al catálogo un analizador nuevo por corrida con `AnalyzerProvider.create`, y ninguna implementación
del modelo inyecta un `ContentAnalyzer`. Sus colaboradores no guardan estado entre corridas: la agregación, el
recolector, el calculador de números, el puntuador léxico, el mapeador —que vacía sus mapas en cada `map`— y el libro de
evaluaciones, que lee su archivo en cada consulta y así ve lo que la corrida anterior registró. Agregar una estructura
acá sería duplicar lo que HALL ya garantiza.

```architecture
features:
  - id: FEAT-CERO
    code: F-CERO
modules:
  - name: audit-domain
    _change: modify
```

## Fijar la garantía con la prueba y con una regla para el developer
Lo propio es la prueba, que vale aunque la implementación de HALL se demore: el mismo curso corrido dos veces en el
mismo proceso da, en los 11.760 nodos, lo mismo que una invocación nueva —73,92 en el curso, 96,54 · 94,16 · 79,45 ·
63,51 en los niveles, 57,93 de COCA en B1 y 8,00 de recurrencia— y el primer informe conserva sus diagnósticos de topic
(A1 · Present Simple, 2.366 tokens en cuatro bandas). Se prueba en el motor y en la vista consolidada con pendientes,
donde FEAT-PROYU mide las dos fotos. La regla para el developer: un proveedor no reusa entre dos `create` nada que el
analizador modifique; si hace falta un caché, que sea de datos que no dependen del curso, como el catálogo EVP o los
tokens por oración. La feature se registra para que `feature sync --feature FEAT-CERO` anote J001; su ubicación y las
pruebas son del qa-tester.

```architecture
modules:
  - name: audit-domain
    _change: modify
```

## Declarar las 11 pruebas de FEAT-CERO y ubicar su journey
FEAT-CERO no tiene piezas propias: sus pruebas van en las que ya existen, con los números del 29/9. Se distinguen de la
de F-HALL-R014 en `IAuditEngine`, que pide dos corridas iguales: éstas piden lo mismo que una invocación nueva aunque
antes haya corrido otro curso, y que el primer informe no cambie.

| Pieza | Reglas | Pruebas | Qué fija |
|---|---|---:|---|
| `IAuditEngine` | R001, R002 | 3 | la segunda corrida da 73,92, 96,54 · 94,16 · 79,45 · 63,51, COCA de B1 57,93 y recurrencia 8,00, no 72,90; una palabra que sólo usaba el curso anterior no cuenta; el primer informe conserva los 55 topics (A1 · Present Simple, 2.366 tokens en cuatro bandas) |
| los proveedores de `coca-buckets-distribution`, `lemma-recurrence`, `lemma-absence` y `lemma-count` | R001, R002 | 5 | cada `create` da un analizador sin lo de la corrida anterior: A1 en 100 y B1 en 57,9 de COCA, no 75 y 76,1; la recurrencia en 8,0 %, no 4,0 %; los lemas y las oraciones del curso que mide; COCA escribe en sus topics y no en los del primer informe |
| `DefaultCourseProjector` | R001, R002 | 2 | la foto con pendientes, igual a una invocación nueva; la foto con lo aprobado conserva el COCA de los 55 topics |
| `DefaultImpactPreviewComputer` | R001 | 1 | el impacto de la propuesta k de `revise-instructions`, como si fuera la primera |

Por regla: R001 8 y R002 3. La regla para el developer de este TECH_SPEC —un proveedor no reusa entre dos `create` nada
que el analizador modifique— queda fijada por las cinco de los proveedores.

**El journey.** F-CERO-J001 va a `audit-cli`, en `com.learney.contentaudit.auditcli.commands`, como F-CDIFF-J001: el
operador pide la vista consolidada, y la prueba la compara con una invocación nueva de `analyze`; `GetConsolidatedCmd`,
`DefaultGetConsolidatedCommand` y `AnalyzeCmd` son package-private ahí. `generate` crearía `FCeroJ001JourneyTest` con 2
caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    implementations:
      - name: IAuditEngine
        handwrittenTests:
          - name: "should leave the first report untouched by a second run in the same process, its 55 topics keeping their COCA diagnosis, A1 · Present Simple with 2,366 tokens in four bands"
            traceability: { feature: FEAT-CERO, rule: F-CERO-R002 }
features:
  - id: FEAT-CERO
    code: F-CERO
    journeys:
      - { id: F-CERO-J001, testModule: audit-cli, testPackage: com.learney.contentaudit.auditcli.commands }
```
