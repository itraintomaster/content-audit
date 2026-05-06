---
patch: ARCH-CDIFF-004
requirement: 2026-05-06.01_consolidated-differential-view
generated: 2026-05-06T18:00:00Z
supersedes: ARCH-CDIFF-001 / ARCH-CDIFF-002 / ARCH-CDIFF-003 (all applied; narratives archived in git)
---

# Tech Spec: FEAT-CDIFF — ARCH-CDIFF-004 (DefaultAuditRunner cleanup)

ARCH-CDIFF-001 / 002 / 003 ya estan aplicados a `sentinel.yaml`. Este patch limpia un drift independiente de FEAT-CDIFF: `DefaultAuditRunner` declara dos `requiresInject` que NO corresponden a la realidad del codigo:

- `contentAudit: ContentAudit` — `ContentAudit` no es un tipo Java, es el nombre del sistema (`system: ContentAudit` en sentinel.yaml linea 1). El generador emitio un campo `private final ContentAudit contentAudit` y el archivo no compila ("cannot find symbol: class ContentAudit"). Bug pre-existente, descubierto al re-correr `sentinel generate` despues de aplicar ARCH-CDIFF-003.
- `courseMapper: CourseMapper` — el cuerpo de la clase NUNCA referencia este campo. La logica de `runAudit()` y `runDetailedAudit()` solo usa `courseToAuditableMapper.map(courseEntity)`. Es un campo huerfano de alguna iteracion anterior.

Y ademas faltan dos campos que el cuerpo SI usa y los call sites (`Main.java:254` y `DefaultAuditRunnerTest:37`) SI pasan al constructor:

- `allAnalyzers: List<ContentAnalyzer>` — referenciado en `runAudit()` y `runDetailedAudit()` para filtrar analyzers por nombre.
- `scoreAggregator: ScoreAggregator` — usado para construir `IAuditEngine` instances ad-hoc cuando el caller filtra.

## DefaultAuditRunner: alinear requiresInject con el cuerpo y los call sites

El target state son exactamente los 5 argumentos que `Main.java:254-256` y `DefaultAuditRunnerTest:37-38` ya pasan: `(CourseRepository, CourseToAuditableMapper, AuditEngine, List<ContentAnalyzer>, ScoreAggregator)`. Borrar las dos entradas espurias (`contentAudit`, `courseMapper`) y agregar las dos faltantes (`allAnalyzers`, `scoreAggregator`) en una sola pasada deja el `requiresInject` consistente con el codigo. Sintaxis `List<ContentAnalyzer>` ya es valida en sentinel.yaml (lineas 414 y 3554).

```architecture
modules:
  - name: "audit-application"
    _change: "modify"
    implementations:
      - name: "DefaultAuditRunner"
        _change: "modify"
        requiresInject:
          - name: "contentAudit"
            type: "ContentAudit"
            _change: "delete"
          - name: "courseMapper"
            type: "CourseMapper"
            _change: "delete"
          - name: "allAnalyzers"
            type: "List<ContentAnalyzer>"
            _change: "add"
          - name: "scoreAggregator"
            type: "ScoreAggregator"
            _change: "add"
```
