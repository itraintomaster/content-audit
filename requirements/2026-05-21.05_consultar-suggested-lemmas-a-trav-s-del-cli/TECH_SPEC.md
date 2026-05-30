---
patch: FEAT-CSLATDC
requirement: 2026-05-21.05_consultar-suggested-lemmas-a-trav-s-del-cli
generated: 2026-05-21T23:00:00Z
---

# Tech Spec: Consulta de Suggested Lemmas a través del CLI

## Resumen de la decisión arquitectónica

Esta iteración corrige los dos hallazgos materiales detectados por el eval previo:

1. **`SuggestedLemmasFilter` ya no queda huérfano.** Se agrega un overload `get(String resource, String name, SuggestedLemmasFilter filter): Integer` al sealed interface `GetCommand`, en convivencia con la firma original que recibe `GetTasksFilter`. El call site existente no se rompe. La elección entre overload y filter polimórfico se discute más abajo.
2. **El adaptador ya no depende del cap fijo de 10 del resolver.** `LemmaAbsenceContextSuggestedLemmaQueryPort` deja de inyectar `CorrectionContextResolver<LemmaAbsenceCorrectionContext>` y declara explícitamente que implementa su propia generación parametrizable por `limit`. La regla F-CSLATDC-R011 (consulta no limitada al top estático) se satisface estructuralmente porque el cap del resolver ya no acota el resultado del puerto.

Lo demás del diseño se conserva: nombre `SuggestedLemmaQueryPort`, paquete `lemmasuggestion`, excepción `SuggestedLemmaQueryRejectedException`, record `SuggestedLemmaQueryResult` con `cefrLevel` efectivo, y reutilización del mismo orden funcional de ranking que el resolver ya aplica.

## Por qué overload en lugar de filter polimórfico

Comparé las dos opciones contra las convenciones del módulo:

- **Overload (elegida).** Todos los demás commands del proyecto (`AnalyzeCommand`, `DeleteCommand`, `PlanCommand`, `PruneCommand`, `ReviseCommand`, `ApproveCommand`, `RejectCommand`) tienen una sola firma con parámetros directos (String, primitivos, enums, modelos planos). Ninguno usa filter polimórfico ni sealed hierarchy de inputs. `GetCommand` es el único con un agregador (`GetTasksFilter`). Sumar un segundo overload conserva el patrón: cada resource lleva el shape de filtros que necesita. No rompe ningún call site existente.
- **Filter polimórfico (descartada).** Cambiar la firma única a `get(String resource, String name, GetFilter filter)` con `GetFilter` sealed permitiendo `GetTasksFilter` y `SuggestedLemmasFilter` exige tocar `GetTasksFilter` (debe implementar `GetFilter`), modificar todos los call sites de `get("tasks", ...)`, y agregar un nuevo modelo abstracto. Es generalización para un cliente único — violaría P3 (versatility on demand): no hay todavía un tercer filter pendiente que justifique abstraer ahora.

F-CSLATDC-R002 dice literalmente "extender el verbo existente get"; un overload es una extensión natural del verbo, mientras que cambiar la signature única es una modificación que rompe el contrato existente.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    interfaces:
      - name: "GetCommand"
        _change: "modify"
        sealed: true
        exposes:
          - signature: "get(String resource, String name, SuggestedLemmasFilter filter): Integer"
            _change: "add"
```

## Crear `SuggestedLemmasFilter` con campos cerrados

El record sólo admite los tres campos enumerados; cualquier filtro adicional como `isIrregular` es estructuralmente imposible de pasar al comando. F-CSLATDC-R013 ("rechazar filtros fuera de alcance") queda satisfecha por construcción y queda documentada explícitamente en la descripción del record con su código de regla. El `limit` no está acotado por el cap del resolver (ver más abajo); la documentación lo deja explícito para anclar la satisfacción de R011 al filtro.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    models:
      - name: "SuggestedLemmasFilter"
        _change: "add"
        type: "record"
        visibility: "public"
        description: "Filtro de la consulta GetCommand.get(\"suggested-lemmas\", taskId, ...). F-CSLATDC-R013 (rechazar filtros fuera de alcance como isIrregular) se cumple por construcción: el record es cerrado y sólo admite los tres campos enumerados; cualquier filtro adicional es estructuralmente imposible de pasar al comando."
        fields:
          - name: "limit"
            type: "Optional<Integer>"
            description: "Cantidad máxima de SuggestedLemma a devolver (F-CSLATDC-R008). Vacío = el adaptador decide default; cero = lista vacía válida (F-CSLATDC-R009). El adaptador NO está limitado por el cap interno de 10 del LemmaAbsenceContextResolver: lo que se solicite (dentro de límites razonables) se respeta (F-CSLATDC-R011)."
          - name: "partOfSpeech"
            type: "Optional<String>"
            description: "Filtro opcional por part-of-speech (F-CSLATDC-R007). Vacío = sin filtro de POS."
          - name: "level"
            type: "Optional<CefrLevel>"
            description: "CefrLevel explícito (F-CSLATDC-R006). Vacío = el adaptador intenta inferir desde la task (F-CSLATDC-R004); si no puede inferir y no se informó, la consulta es rechazada con SuggestedLemmaQueryRejectedException (F-CSLATDC-R005)."
```

## Re-listar los nueve injects de `GetCmd` con `_change: add`

La sintaxis defensiva que ya funcionó en la iteración anterior se mantiene: nueve entradas, cada una con `_change: "add"`, para evitar que un `_change: modify` con lista parcial sea interpretado como reemplazo total. La nueva entrada `suggestedLemmaQueryPort` se incorpora al final del listado.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    packages:
      - name: "commands"
        _change: "modify"
        implementations:
          - name: "GetCmd"
            _change: "modify"
            implements: ["GetCommand"]
            requiresInject:
              - name: "auditReportStore"
                type: "AuditReportStore"
                _change: "add"
              - name: "refinementPlanStore"
                type: "RefinementPlanStore"
                _change: "add"
              - name: "analyzerRegistry"
                type: "AnalyzerRegistry"
                _change: "add"
              - name: "correctionContextResolver"
                type: "CorrectionContextResolver"
                _change: "add"
              - name: "revisionArtifactStore"
                type: "RevisionArtifactStore"
                description: "Needed for the 'proposal'/'proposals' resource branch (R002/R003/F-REVAPR)"
                _change: "add"
              - name: "impactPreviewStore"
                type: "ImpactPreviewStore"
                _change: "add"
              - name: "impactPreviewFormatter"
                type: "ImpactPreviewFormatter"
                _change: "add"
              - name: "correctionContextJsonMapper"
                type: "CorrectionContextJsonMapper"
                description: "Mismo serializador que usa el renderer efimero. Garantiza que get task y plan efimero produzcan el mismo schema JSON para correctionContext."
                _change: "add"
              - name: "suggestedLemmaQueryPort"
                type: "SuggestedLemmaQueryPort"
                description: "Resuelve el overload get(resource, name, SuggestedLemmasFilter) cuando resource == \"suggested-lemmas\". Delega en refiner-domain (F-CSLATDC-R001, F-CSLATDC-R002)."
                _change: "add"
```

## Modelar el resultado público con el `cefrLevel` efectivo

El record carga `taskId`, el `cefrLevel` efectivamente aplicado y la `List<SuggestedLemma>` ordenada por prioridad. Incluir el `cefrLevel` es obligatorio para R014 (trazabilidad observable de qué nivel se usó cuando el sistema infirió). La lista vacía representa el caso legítimo de R012; los rechazos funcionales (R005, R013 residual) viajan por excepción. No se incluye `message` de respuesta porque ningún caso success necesita texto libre.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    models:
      - name: "SuggestedLemmaQueryResult"
        _change: "add"
        type: "record"
        visibility: "public"
        description: "Resultado público de SuggestedLemmaQueryPort.query(...). Incluye el CefrLevel efectivamente aplicado para trazabilidad del agente revisor (F-CSLATDC-R003, F-CSLATDC-R014)."
        fields:
          - name: "taskId"
            type: "String"
            description: "Identificador de la RefinementTask consultada."
          - name: "cefrLevel"
            type: "CefrLevel"
            description: "Nivel CEFR efectivamente aplicado al filtrar (F-CSLATDC-R003, F-CSLATDC-R004, F-CSLATDC-R006). Trazabilidad obligatoria para el agente revisor."
          - name: "suggestedLemmas"
            type: "List<SuggestedLemma>"
            description: "Lemmas ya filtrados por nivel + partOfSpeech y ordenados por prioridad (F-CSLATDC-R010, F-CSLATDC-R014). Lista vacía es resultado válido cuando no hay coincidencias (F-CSLATDC-R009, F-CSLATDC-R012)."
```

## Distinguir rechazo funcional con una excepción de dominio

R005 (no se puede inferir level y no se informó) es un rechazo funcional, no un resultado vacío. La excepción de dominio preserva `taskId` y un `reason` libre para que el adaptador CLI produzca un mensaje claro (copy exacto abierto en DOUBT-EXACT-ERROR-COPY). La regla R013 (filtros no soportados) está cubierta principalmente por construcción del record, pero la excepción queda disponible para cualquier residual semántico (por ejemplo, validación adicional sobre rangos del `limit`).

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    models:
      - name: "SuggestedLemmaQueryRejectedException"
        _change: "add"
        type: "exception"
        visibility: "public"
        extends: "RuntimeException"
        message: "Suggested lemma query rejected for task %s: %s"
        description: "Rechazo funcional explícito de la consulta. Cubre F-CSLATDC-R005 (no se puede inferir level y no se informó) y cualquier residual de F-CSLATDC-R013 que escape al filtro estructural."
        fields:
          - name: "taskId"
            type: "String"
            description: "Task que originó la consulta rechazada."
          - name: "reason"
            type: "String"
            description: "Motivo funcional legible (texto exacto pendiente — DOUBT-EXACT-ERROR-COPY del requirement)."
```

## Declarar el puerto público sin acoplarlo al cap del resolver

La descripción del puerto deja explícito por qué NO se delega en `LemmaAbsenceContextResolver`: ese resolver cappea la lista a 10 (`SUGGESTED_LEMMAS_LIMIT` en `LemmaAbsenceContextResolver.java`) en sus dos paths (con y sin lemma-count signal), y delegar en él haría imposible satisfacer F-CSLATDC-R011 (consulta no limitada al top estático del `correctionContext`). El adaptador implementa su propia generación con la misma lógica funcional pero parametrizable. Esta es la corrección material respecto de la iteración anterior, donde el adaptador delegaba en el resolver y heredaba el cap.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    interfaces:
      - name: "SuggestedLemmaQueryPort"
        _change: "add"
        stereotype: "port"
        visibility: "public"
        description: "Puerto público para resolver la consulta de SuggestedLemmas anclada a una RefinementTask. NO delega en LemmaAbsenceContextResolver porque ese contexto cappea suggestedLemmas a 10 y violaría F-CSLATDC-R011 (consulta no limitada al top estático). El adaptador implementa su propia generación parametrizable por limit, reutilizando los diagnósticos disponibles en AuditReport (F-CSLATDC-R001, F-CSLATDC-R002, F-CSLATDC-R011)."
        exposes:
          - signature: "query(AuditReport report, RefinementTask task, Optional<Integer> limit, Optional<String> partOfSpeech, Optional<CefrLevel> explicitLevel): SuggestedLemmaQueryResult"
```

## Aislar la implementación sin la inyección del resolver

El adaptador `LemmaAbsenceContextSuggestedLemmaQueryPort` pierde la inyección de `CorrectionContextResolver<LemmaAbsenceCorrectionContext>` (marcada `_change: "delete"` en el patch). Ya no necesita esa dependencia porque deja de delegar en el camino capped del resolver: opera directamente sobre el `AuditReport` y la `RefinementTask` que recibe como parámetros del puerto. La lógica funcional (localizar quiz node, inferir nivel desde milestone ancestor, generar candidatos LEMMA_ABSENCE y rankear con el orden F-SLEM-R003..R007) es responsabilidad del developer, que puede extraer un helper privado en `refiner-domain` y compartirlo con `LemmaAbsenceContextResolver` para evitar duplicación — esa refactorización interna no requiere cambio arquitectónico adicional.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    packages:
      - name: "lemmasuggestion"
        _change: "modify"
        description: "Adaptador interno del puerto de consulta de SuggestedLemmas. Encapsula la generación, filtrado y ranking de candidatos sin pasar por el cap fijo del LemmaAbsenceContextResolver, para que el limit solicitado por el agente sea respetado (F-CSLATDC-R011)."
        visibility: "internal"
        implementations:
          - name: "LemmaAbsenceContextSuggestedLemmaQueryPort"
            _change: "modify"
            visibility: "public"
            implements: ["SuggestedLemmaQueryPort"]
            description: "Implementación única del puerto. Opera sobre el escenario LEMMA_ABSENCE: localiza el quiz node en AuditReport, infiere el CefrLevel desde el milestone ancestor (con el explicitLevel del usuario como override), genera y rankea los candidatos aplicando el mismo orden funcional que F-SLEM-R003..R007, filtra por partOfSpeech cuando se informe y respeta el limit solicitado por el agente sin tope fijo de 10. Sin requiresInject: opera sobre el AuditReport y la RefinementTask que recibe como parámetros del puerto; el developer puede extraer un helper privado compartido con LemmaAbsenceContextResolver para evitar duplicación."
            requiresInject:
              - name: "lemmaAbsenceContextResolver"
                type: "CorrectionContextResolver<LemmaAbsenceCorrectionContext>"
                _change: "delete"
```

## Mapeo de reglas a elementos del patch

El DSL de Sentinel no soporta un campo `traceability` en elementos arquitectónicos (sólo en `handwrittenTests`/`declarativeTests`). El mapeo se hace por referencia textual en las `description` de cada elemento. Estado por regla:

| Regla | Cobertura arquitectónica |
|---|---|
| R001 — `get("suggested-lemmas", taskId, …)` devuelve suggested lemmas | `SuggestedLemmaQueryPort.description`, `requiresInject.suggestedLemmaQueryPort.description` |
| R002 — usar verbo `get` existente | `GetCommand` overload + `SuggestedLemmaQueryPort.description` |
| R003 — filtrado obligatorio por level | `SuggestedLemmaQueryResult.cefrLevel.description` |
| R004 — inferencia de level desde la task | `SuggestedLemmasFilter.level.description`, `SuggestedLemmaQueryResult.cefrLevel.description` |
| R005 — validación cuando no hay level disponible | `SuggestedLemmaQueryRejectedException.description`, `SuggestedLemmasFilter.level.description` |
| R006 — level explícito cuando no puede inferirse | `SuggestedLemmasFilter.level.description`, `SuggestedLemmaQueryResult.cefrLevel.description` |
| R007 — filtro por partOfSpeech | `SuggestedLemmasFilter.partOfSpeech.description` |
| R008 — límite máximo solicitado | `SuggestedLemmasFilter.limit.description` |
| R009 — lista parcial sin error | `SuggestedLemmasFilter.limit.description`, `SuggestedLemmaQueryResult.suggestedLemmas.description` |
| R010 — ordenamiento por prioridad | `SuggestedLemmaQueryResult.suggestedLemmas.description` |
| R011 — consulta no limitada al top estático | `SuggestedLemmaQueryPort.description`, paquete `lemmasuggestion.description`, `SuggestedLemmasFilter.limit.description` |
| R012 — tasks sin suggested lemmas aplicables | `SuggestedLemmaQueryResult.suggestedLemmas.description` |
| R013 — filtros no soportados | `SuggestedLemmasFilter.description` (satisfacción estructural), `SuggestedLemmaQueryRejectedException.description` (residual) |
| R014 — resultado mínimo observable | `SuggestedLemmaQueryResult.suggestedLemmas.description`, `cefrLevel.description` |
| R015 — instrucciones al agente revisor | **Fuera del scope arquitectónico.** Es una obligación de documentación / prompt template para el agente revisor, no tiene superficie observable en código. Se materializa actualizando el prompt del agente cuando se implemente la feature. |

## Lo que NO declara el patch y por qué

- **Rama `"suggested-lemmas"` en el dispatch interno de `GetCmd`**: el DSL no ofrece primitiva para declarar sub-resources dentro de una signature. El despacho por `String resource` es una decisión de implementación que `@developer` resolverá agregando la rama al switch.
- **Wiring del composition root**: `Main.java` no está modelado como implementation en `sentinel.yaml`. La construcción manual del adaptador y su pasada al constructor de `GetCmd` queda en código no-sentinel.
- **`throws` del puerto**: el DSL serializa `throws` pero ningún signature actual del proyecto lo usa; la intención de lanzar `SuggestedLemmaQueryRejectedException` queda documentada en las descripciones del puerto y del exception model.

## Coordinación con `@analyst`

El REQUIREMENT.md tiene el texto literal `GetCommand.get("suggested-lemmas", <taskId>, GetTasksFilter(limit, partOfSpeech, level))` que quedó obsoleto: la firma adoptada usa `SuggestedLemmasFilter`. Recomendación al usuario: pedir a `@analyst` que actualice las menciones literales en REQUIREMENT.md (rules R001, R002, R006, R007, R008, R011, R013, R015 y los flows de los journeys J001..J004) reemplazando `GetTasksFilter(...)` por `SuggestedLemmasFilter(...)`. El cambio es nominal — la intención funcional es la misma.

## Dudas arquitectónicas abiertas

- **DOUBT-LEVEL-CONFLICT**: si el cliente informa `explicitLevel` que contradice el inferido, esta propuesta asume que el explícito gana. Pendiente confirmar con el analista.
- **DOUBT-LIMIT-DEFAULT-MAX**: el puerto recibe `Optional<Integer>`; el adaptador debe decidir un default cuando está vacío y, probablemente, un techo razonable distinto al cap fijo del resolver para evitar respuestas desmedidas.
- **DOUBT-STATIC-DUPLICATES**: la consulta dinámica puede devolver los mismos lemmas que el `correctionContext` estático. La propuesta no deduplica; si el analista quiere exclusión, la firma del puerto necesita el conjunto previo.
