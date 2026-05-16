---
patch: ARCH-LEMMA-SUGGESTIONS
requirement: 2026-05-15.04_agregar-lemmacount-como-variable-de-suggestedlemmas-en-el-contexto
generated: 2026-05-16T00:00:00Z
---

# Tech Spec: Agregar lemma-count al contexto de suggested lemmas

## Agregar señales de lemma-count a `SuggestedLemma`

El contexto de sugerencias necesita transportar no solo la ausencia de lemas, sino también la señal cuantitativa que explica por qué una sugerencia se considera relevante. Centralizar estos datos en `SuggestedLemma` evita crear un objeto paralelo para la misma decisión y mantiene juntos el valor observado, el umbral aplicado y el resultado derivado. Se permite `null` para preservar compatibilidad con contextos donde la señal aún no esté disponible.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    models:
      - name: "SuggestedLemma"
        _change: "modify"
        fields:
          - name: "lemmaCount"
            type: "Integer"
            description: "Observed lemma-count signal for the target level when available; null means unavailable."
            _change: "add"
          - name: "lemmaCountThreshold"
            type: "Integer"
            description: "Minimum lemma-count threshold used to decide underexposure when available; null means unavailable."
            _change: "add"
          - name: "isUnderexposed"
            type: "Boolean"
            description: "Whether lemmaCount is below lemmaCountThreshold when lemma-count data is available; null means unavailable."
            _change: "add"
```

## Ajustar el resolver de contexto de ausencia de lemas

La responsabilidad de enriquecer el contexto pertenece al resolver que ya conoce la ausencia de lemas y produce el contexto de corrección. Mantener el cambio en `LemmaAbsenceContextResolver` evita introducir otro resolver para una variante pequeña del mismo caso de uso. La decisión conserva la interfaz existente y limita el impacto a la implementación que ya arma este contexto.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    implementations:
      - name: "LemmaAbsenceContextResolver"
        _change: "modify"
        implements:
          - "CorrectionContextResolver"
```

## Extender el mapeo JSON del contexto de corrección

El CLI de auditoría necesita exponer la nueva señal para que el contexto serializado sea verificable y consumible por herramientas externas. Actualizar el mapper existente mantiene una única representación JSON del contexto de corrección, en lugar de bifurcar formatos por tipo de sugerencia. El trade-off es acoplar el mapper al nuevo contrato del modelo, pero eso es consistente con su rol de frontera de serialización.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    packages:
      - name: "commands"
        _change: "modify"
        implementations:
          - name: "DefaultCorrectionContextJsonMapper"
            _change: "modify"
            implements:
              - "CorrectionContextJsonMapper"
```

## Validar estructuralmente el nuevo contexto de ausencia de lemas

La validación estructural debe reconocer los nuevos datos para evitar que contextos válidos sean rechazados o que combinaciones incompletas pasen sin control. Ubicar el ajuste en el validador específico de ausencia de lemas mantiene las reglas cerca del contrato que valida. Esto preserva la separación entre construcción, serialización y validación del contexto.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "contextoverride"
        _change: "modify"
        implementations:
          - name: "LemmaAbsenceContextStructuralValidator"
            _change: "modify"
            implements:
              - "CorrectionContextStructuralValidator"
```