---
patch: FEAT-CSLATDC
requirement: 2026-05-21.05_consultar-suggested-lemmas-a-trav-s-del-cli
generated: 2026-06-23T00:00:00Z
---

# Tech Spec: F-CSLATDC-R015 — La consulta dinamica opera sobre el analisis fuente de la task

> Este TECH_SPEC es la foto del patch que cierra F-CSLATDC-R015. El diseno original de FEAT-CSLATDC (recurso `suggested-lemmas`, filtros, `SuggestedLemmaQueryPort`, etc.) ya fue aplicado en una iteracion anterior; este patch solo corrige de que analisis se obtiene el universo de candidatos.

## Cambiar la firma del seam bindForTask para recibir el analisis fuente
El `RefinementTask` no transporta `sourceAuditId` ni el `planId` dueño: el binding task→plan→analisis vive fuera de la task, por lo que el factory no tenia de donde resolver el analisis correcto y caia en `loadLatest()` (el bug de R015). Cambiamos la firma para que el seam reciba la identidad del analisis ya resuelta por quien la conoce (el engine en `revise`, el plan dueño de la task en el CLI), eliminando toda resolucion implicita "del mas reciente". Pasamos un `String sourceAuditId` y no un `AuditReport` para mantener el seam liviano y dejar la carga (`auditReportStore.load`) del lado del factory.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    interfaces:
      - name: SuggestedLemmaQuerySessionFactory
        _change: modify
        exposes:
          - signature: "bindForTask(RefinementTask task): SuggestedLemmaQuerySession"
            _change: delete
          - signature: "bindForTask(String sourceAuditId, RefinementTask task): SuggestedLemmaQuerySession"
            _change: add
```

## Quitar RefinementPlanStore del factory
La unica razon por la que `DefaultSuggestedLemmaQuerySessionFactory` inyectaba `RefinementPlanStore` era para hacer `loadLatest()` y derivar el `sourceAuditId` por su cuenta — exactamente la fuente del bug. Con la nueva firma el `sourceAuditId` llega como parametro, asi que el factory solo necesita `auditReportStore.load(sourceAuditId)`. Removemos la dependencia espuria para que la superficie del factory refleje lo que realmente consume y no quede una via para reintroducir `loadLatest()`.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    packages:
      - name: lemmasuggestion
        _change: modify
        implementations:
          - name: DefaultSuggestedLemmaQuerySessionFactory
            _change: modify
            implements: ["SuggestedLemmaQuerySessionFactory"]
            visibility: public
            requiresInject:
              - { name: refinementPlanStore, type: RefinementPlanStore, _change: delete }
```

## Transportar sourceAuditId en LemmaAbsenceCorrectionContext
En el camino `revise`, el generator solo recibe `LemmaAbsenceCorrectionContext` — el `AuditReport` que el engine ya resolvio correctamente muere en el engine y nunca llega al seam. Sumamos `sourceAuditId` al carrier (un `String`, estampado por `LemmaAbsenceContextResolver` al construir el contexto) para que `LemmaAbsenceAgentGenerator` pueda bindear la consulta dinamica al MISMO analisis fuente de la task. Deliberadamente no agregamos `planId` ni el `AuditReport` completo: la consulta solo necesita la identidad del analisis, y un `String` mantiene el carrier chico cuando cruza a JSON en `revise --correction-context`.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: LemmaAbsenceCorrectionContext
        _change: modify
        fields:
          - { name: sourceAuditId, type: String, _change: add }
```
