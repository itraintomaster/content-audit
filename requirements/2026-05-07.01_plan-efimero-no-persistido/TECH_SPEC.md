---
patch: FEAT-PLANEF
requirement: 2026-05-07.01_plan-efimero-no-persistido
generated: 2026-05-08T00:00:00Z
---

# Tech Spec: Plan efimero / no persistido (F-PLANEF)

Este documento explica, una decision por seccion, el patch arquitectonico que satisface F-PLANEF-R001 ("el comando `plan` admite un modo de invocacion que no escribe en disco y emite el plan por stdout en JSON con el mismo schema que un plan persistido"). El alcance es deliberadamente chico: una regla, un journey, cero modulos nuevos, cero dependencias nuevas, cero cambios de boundary.

## Modelar el modo de storage como enum, no como booleano

El operador sugirio el shape `--storage=none` precisamente porque deja la puerta abierta a otros valores en el futuro (`memory`, `tmp`) sin un cambio de contrato del usuario. Modelarlo como enum (`PlanStorageMode` con `DISK` y `EPHEMERAL`) captura esa decision en el contrato: cualquier extension futura es agregar un valor al enum, no introducir un flag nuevo. Un booleano `--no-persist` cerraria esa via y obligaria a romper compatibilidad mas adelante. La var de entorno se descarta porque el modo es per-invocacion (el cliente del dashboard alterna entre invocaciones persistentes y efimeras dentro de la misma sesion), no per-proceso. El enum vive en `audit-cli` porque es un detalle del verbo CLI; el dominio no lo necesita.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: PlanStorageMode
        _change: add
        type: enum
        visibility: public
        fields:
          - name: DISK
            description: "Persistent path: el plan se guarda via RefinementPlanStore en .content-audit/refinement-plans/. Comportamiento previo a F-PLANEF, sigue siendo el default cuando el operador no pasa --storage."
          - name: EPHEMERAL
            description: "Modo efimero (F-PLANEF-R001). El comando plan calcula el plan, NO lo persiste, y emite la representacion JSON completa por stdout. Mensajes informativos van a stderr."
```

## Cambiar la firma del verbo `plan` para llevar el modo en el contrato

R001 invariante #3 dice que la opcion es ortogonal al resto de los flags y que la unica diferencia es donde va el resultado. Eso obliga a que el modo sea un parametro **explicito** del verbo, no un side-channel (campo del comando, var global, ThreadLocal). La firma cambia de `plan(String auditId): Integer` a `plan(String auditId, PlanStorageMode storageMode): Integer`. Asi el contrato sealed declara que toda invocacion del verbo `plan` resuelve un modo concreto antes de ejecutar; los tests pueden cruzar el verbo con cualquier combinacion `(auditId, storageMode)` sin asumir defaults globales. La firma vieja se borra en lugar de sumar una sobrecarga: la sobrecarga genera ambiguedad sobre cual es el path de produccion y no aporta a los tests.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: PlanCommand
        _change: modify
        sealed: true
        exposes:
          - signature: "plan(String auditId): Integer"
            _change: delete
          - signature: "plan(String auditId, PlanStorageMode storageMode): Integer"
            _change: add
```

## Separar el rendering JSON del comando con un puerto interno apuntable

R001 invariante #2 ("emite el plan por stdout en JSON con el mismo schema que un plan persistido") es una propiedad **del output**, no del comando completo. Si dejamos esa logica como un metodo privado de `PlanCmd`, el unico camino para testearla es un end-to-end del verbo, lo que mezcla "no llamo a save" (invariante 1) con "emite el JSON correcto" (invariante 2) en un solo test. Extraemos un puerto chico, `EphemeralPlanRenderer`, con una sola operacion `render(RefinementPlan): Integer`. El renderer queda en el package `commands` con `visibility: internal` (interface y impl package-private) porque es un detalle del CLI: ningun otro modulo lo necesita ver, pero es directamente apuntable desde tests del mismo package. Su unica implementacion `DefaultEphemeralPlanRenderer` serializa el plan al mismo schema JSON que `FileSystemRefinementPlanStore` produce, garantizando que el cliente que parsea planes persistidos parsea los efimeros sin cambios.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        visibility: internal
        interfaces:
          - name: EphemeralPlanRenderer
            _change: add
            stereotype: port
            visibility: internal
            exposes:
              - signature: "render(RefinementPlan plan): Integer"
        implementations:
          - name: DefaultEphemeralPlanRenderer
            _change: add
            implements: ["EphemeralPlanRenderer"]
            visibility: internal
```

## Inyectar el renderer en `PlanCmd` y dejar el dispatch en el comando

`PlanCmd` orquesta los tres pasos del verbo: cargar el `AuditReport`, derivar el `RefinementPlan` con `RefinerEngine`, y entregar el resultado. La decision sobre que hacer con el plan derivado (persistir via `RefinementPlanStore.save` o renderear a stdout via `EphemeralPlanRenderer.render`) es un dispatch trivial sobre el `PlanStorageMode` recibido. **No** se introduce un `InMemoryRefinementPlanStore` que sea no-op en `save`: eso enmascara la invariante (el test tendria que verificar "el adapter es del tipo correcto" en vez de "save no se llama") y rompe la simetria con los otros stores de filesystem. El path efimero **no llama** a `RefinementPlanStore.save`, lo que vuelve a R001 invariante #1 ("ningun archivo cambia") directamente verificable con un mock que cuente invocaciones de `save`. Los mensajes informativos del modo persistente (banner, table headers) ya van a stdout porque ahi viven hoy; en el modo efimero ese stream se reserva para el JSON y cualquier mensaje auxiliar va a stderr (DOUBT-DIAGNOSTIC-OUTPUT).

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
                description: "Renderer que serializa el RefinementPlan a stdout en JSON cuando storageMode = EPHEMERAL. Solo se invoca en el path efimero; en el path persistente se ignora."
            types:
              - Component
```

## Por que NO se generaliza el modo efimero a otros verbos

R001 cubre solo `plan`. Otros verbos (`analyze`, `revise`) tambien escriben artefactos y podrian beneficiarse del mismo patron en el futuro (DOUBT-OTHER-COMMANDS). La tentacion es introducir hoy un contrato uniforme — por ejemplo, una capacidad transversal `EphemeralOutputStrategy` aplicable a cualquier verbo que persista. **Eso es exactamente el over-engineering que el operador rechazo en la primera pasada del feature.** P3 (Versatility on Demand) lo dice explicito: "When extensibility becomes real on a specific axis, open the seam on that axis alone. Uniform extensibility everywhere is a mistake." El shape del flag (`--storage=<mode>`) es genericamente reusable si manana se decide repetir el patron en `analyze`, pero cada verbo lo hara en sus propios terminos: que significa "no escribir en disco" en `analyze` (no guardar el `AuditReport`?) o en `revise` (no actualizar el `RevisionArtifact`?) son decisiones distintas que no son ahora casos reales. Hoy F-PLANEF solo abre la seam en `PlanCommand` porque solo ahi hay evidencia concreta de demanda.
