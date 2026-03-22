# Tipos de Ausencia de Lemas

## Resumen Visual

```
Esperado en A2, busco en todo el curso:

COMPLETELY_ABSENT:    No aparece en ningun nivel
                      A1[ ] A2[ ] B1[ ] B2[ ]

APPEARS_TOO_EARLY:    Aparece en nivel(es) anterior(es) al esperado
                      A1[x] A2[ ] B1[ ] B2[ ]

APPEARS_TOO_LATE:     Aparece en nivel(es) posterior(es) al esperado
                      A1[ ] A2[ ] B1[x] B2[ ]

SCATTERED_PLACEMENT:  Aparece en niveles no adyacentes / mezclados
                      A1[x] A2[ ] B1[ ] B2[x]
```

## Detalle de Cada Tipo

### COMPLETELY_ABSENT
- **Display**: "Completely Absent"
- **Prioridad por defecto**: HIGH
- **Impact score**: 1.0 (maximo impacto)
- **Descripcion**: El lema esperado para este nivel no aparece en ninguna oracion del curso

### APPEARS_TOO_EARLY
- **Display**: "Appears Too Early"
- **Prioridad por defecto**: MEDIUM
- **Impact score**: 0.6
- **Descripcion**: El lema aparece en el curso pero en un nivel anterior al esperado. Ejemplo: Una palabra esperada en B1 que ya se introduce en A1.

<!-- Sugerencia: Esto no siempre es un problema. Si una palabra es comun, puede aparecer naturalmente en niveles inferiores. Pero si aparece SOLO en niveles inferiores y NO en el nivel esperado, puede indicar que no se refuerza donde deberia. -->

### APPEARS_TOO_LATE
- **Display**: "Appears Too Late"
- **Prioridad por defecto**: MEDIUM
- **Impact score**: 0.8
- **Descripcion**: El lema aparece en el curso pero en un nivel posterior al esperado. Ejemplo: Una palabra A1 que recien aparece en B2.

<!-- Cuidado aqui: Esto es mas grave que APPEARS_TOO_EARLY porque significa que el estudiante no tuvo acceso a vocabulario basico cuando lo necesitaba. -->

### SCATTERED_PLACEMENT
- **Display**: "Scattered Placement"
- **Prioridad por defecto**: LOW
- **Impact score**: 0.4
- **Descripcion**: El lema aparece en multiples niveles sin un patron claro de progresion.

## Algoritmo de Clasificacion

```
funcion determineLevelSpecificAbsence(expectedLevel, presentInLevels):
    si presentInLevels esta vacio:
        retornar COMPLETELY_ABSENT

    expectedOrder = orden numerico del nivel esperado (A1=1, A2=2, B1=3, B2=4)

    allBefore = todos los niveles presentes son < expectedOrder
    allAfter  = todos los niveles presentes son > expectedOrder

    si allBefore:
        retornar APPEARS_TOO_EARLY
    si allAfter:
        retornar APPEARS_TOO_LATE
    si_no:
        retornar SCATTERED_PLACEMENT
```
