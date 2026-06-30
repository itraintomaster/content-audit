---
patch: ARCH-SLREGEX-001
requirement: 2026-06-26.01_filtrar-suggested-lemmas-por-regex
generated: 2026-06-26T00:00:00Z
---

# Tech Spec: Filtrar Suggested Lemmas por expresión regular

## Sumar el eje textual `regex` al filtro de CLI `SuggestedLemmasFilter`
`SuggestedLemmasFilter` es el carrier que `GetCmd` puebla desde los flags de la consulta de suggested-lemmas. La feature introduce un nuevo eje de filtrado —el texto del lema— que es ortogonal a los ya existentes (`level`, `partOfSpeech`, `limit`, `includeExposed`), así que pertenece exactamente al mismo record en lugar de a una estructura nueva. Se modela como `Optional<String>` espejando `partOfSpeech`: ausente por defecto, de modo que el comportamiento sin `--regex` queda estructuralmente intacto (F-SLREGEX-R001). El trade-off aceptado es ensanchar deliberadamente este record —documentado como cerrado— igual que hizo FEAT-INCEXP, decisión coherente con que esta feature supera el `OUT` léxico previo.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    models:
      - name: "SuggestedLemmasFilter"
        _change: "modify"
        fields:
          - name: "regex"
            type: "Optional<String>"
            _change: "add"
```

## Propagar `regex` al carrier de dominio `SuggestedLemmaQueryCriteria`
`SuggestedLemmaQueryCriteria` es el carrier que cruza hacia refiner-domain y que el port de consulta lee para aplicar los filtros. El campo debe existir también aquí para que el patrón viaje desde el CLI hasta el punto donde se evalúa contra el texto del lema; sin él, la propagación filter→criteria no tendría dónde transportar el dato. Se modela igual —`Optional<String>`, ausente por defecto— manteniendo la paridad de optionality con `partOfSpeech` y, por carambola, con el campo homónimo recién agregado en el lado del CLI. El filtrado real, la propagación y el parseo del flag son trabajo de Development, no de arquitectura.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    models:
      - name: "SuggestedLemmaQueryCriteria"
        _change: "modify"
        fields:
          - name: "regex"
            type: "Optional<String>"
            _change: "add"
```
