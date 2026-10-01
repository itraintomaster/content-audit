---
feature:
  id: FEAT-CERO
  code: F-CERO
  name: Cada corrida del motor empieza de cero
  priority: critical
---

# Cada corrida empieza de cero: el motor no arrastra nada de la corrida anterior

## TL;DR

**Qué**: Toda corrida del motor dentro de una misma invocación da lo mismo que una invocación nueva sobre ese curso, y no
toca los informes de las corridas anteriores.

**Por qué**: Cuatro analizadores de curso guardan lo que cuentan y nunca lo vacían: el mismo curso corrido dos veces
seguidas da 72,9 % en vez de 73,9 %, y la segunda corrida vacía los diagnósticos COCA de los 55 topics de la primera.

## Recorrido de uso (estado actual)

Cómo corre hoy el motor cada verbo que lo usa:

1. Analizar el curso → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible: una corrida por invocación,
   73,9 % sobre la base del 29/9.
2. Ver estadísticas de un analizador → `StatsAnalyzerCommand.showStats(analyzerName, coursePath)`. ✓ Disponible: una
   corrida por invocación.
3. Ver el curso con lo aprobado y lo pendiente → `GetConsolidatedCommand.getConsolidated(coursePath, format)`. ⚠️ Con
   pendientes aplicables el motor corre dos veces en la misma invocación: la foto con pendientes sale corrida y la foto
   con lo aprobado pierde sus diagnósticos COCA de topic.
4. Corregir consignas en tanda → `ReviseInstructionsCommand.reviseInstructions(planId, maxCorrections, coursePath)`.
   ⚠️ Calcula el impacto de cada propuesta con una corrida del motor, todas en la misma invocación: desde la segunda
   propuesta, el impacto sale corrido.

**Punto de fricción**: pasos 3 y 4. El resultado de una corrida depende de lo que corrió antes en el mismo proceso.

**Cambio mínimo necesario**: que cada corrida arranque con los analizadores vacíos. El diseño de FEAT-HALL ya los
construye de nuevo en cada corrida; este requirement fija la prueba y vale aunque ese diseño se demore.

## Reglas de Negocio

<a id="F-CERO-R001"></a>
### Rule[F-CERO-R001] - El resultado de una corrida depende sólo de su curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Dentro de una misma invocación, una corrida del motor sobre un curso da en todo nodo los mismos puntajes, diagnósticos
> y hallazgos que una invocación nueva sobre ese curso con los mismos veredictos registrados, sin importar cuántas
> corridas hubo antes ni sobre qué cursos.

<details><summary>Detail</summary>

**Criterio**, sobre la base del 29/9 (análisis 2026-09-30T11-54-02, sin el juez):

| Corrida | Curso | A1 | A2 | B1 | B2 | COCA B1 | Recurrencia |
|---|---:|---:|---:|---:|---:|---:|---:|
| Primera, o una invocación nueva | 73,92 | 96,54 | 94,16 | 79,45 | 63,51 | 57,93 | 8,00 |
| Segunda, hoy | 72,90 | 92,38 | 89,99 | 82,48 | 66,70 | 76,07 | 4,00 |
| Segunda, con esta regla | 73,92 | 96,54 | 94,16 | 79,45 | 63,51 | 57,93 | 8,00 |

Puntaje de vocabulario de cada nodo ([F-HALL-R009](#F-HALL-R009)), en por ciento. La segunda corrida de hoy cambia 60
nodos (el curso, los 4 niveles y los 55 topics); temas y ejercicios no cambian. Lo mismo vale para la proyección con
pendientes: da lo mismo que una invocación nueva sobre el curso con esas pendientes escritas, se haya corrido antes o no
el curso con lo aprobado. Y para el impacto de la propuesta k de una tanda de consignas: da lo mismo que si fuera la
primera.

Lleva a una misma invocación el determinismo de [F-HALL-R014](#F-HALL-R014), que pide dos corridas iguales pero no
dice si pueden compartir proceso. Mantiene [F-CDIFF-R007](#F-CDIFF-R007): las fotos salen del motor, y por eso tienen que
salir bien.

</details>

<a id="F-CERO-R002"></a>
### Rule[F-CERO-R002] - Una corrida no cambia los informes anteriores
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Correr el motor no modifica ningún informe que otra corrida produjo antes en la misma invocación: sus puntajes,
> diagnósticos y hallazgos quedan como estaban.

**Criterio**: después de la segunda corrida, los 55 topics del primer informe conservan su diagnóstico COCA (en A1 ·
Present Simple, 2.366 tokens en cuatro bandas). Hoy quedan con 0 tokens y sin bandas, porque la segunda corrida los
vuelve a escribir. En la vista consolidada, eso vacía los diagnósticos COCA de topic de la foto con lo aprobado.

## Contexto

La vista consolidada (FEAT-CDIFF) corre el motor dos veces cuando hay pendientes: una sobre el curso con lo aprobado y
otra sobre el curso con lo pendiente. Los analizadores se construyen una sola vez por proceso, en el arranque, y cuatro
de ellos guardan lo que cuentan en campos que nunca vacían:

| Analizador | Qué arrastra | Efecto medido en la segunda corrida |
|---|---|---|
| `coca-buckets-distribution` | la lista de topics de cada nivel y sus conteos | duplica los topics: los tokens se suman a los de la primera corrida y los cuartos 3 y 4 quedan vacíos; A1 pasa de 100 a 75, B1 de 57,9 a 76,1 |
| `lemma-recurrence` | la posición global y las posiciones de cada [lema](glossary:Lema) | sigue contando desde donde terminó la primera: 8,0 % pasa a 4,0 % |
| `lemma-absence` | los lemas presentes en cada nivel | la segunda corrida ve la unión de los dos cursos: una palabra que la propuesta saca sigue presente |
| `lemma-count` | las oraciones de cada lema | lo mismo: una oración que perdió la palabra la sigue contando |

Medido el 30/9 con un prototipo que cablea el motor como el CLI, sobre una copia del curso en el scratchpad: el mismo
curso, corrido dos veces con los mismos analizadores, da las dos filas de la tabla de R001; con analizadores nuevos, las
dos corridas son idénticas. Confirmado con `get-consolidated` del CLI sobre un workdir de prueba con cuatro pendientes
sembradas: la foto con pendientes da COCA 0,75 en A1 y A2, 0,761 en B1 y 0,513 en B2 (el análisis da 1,0 · 1,0 · 0,579
· 0,322), y los 55 topics salen como cambiados, con 0 tokens COCA en la foto con lo aprobado aunque no hay nada
aprobado. En la muestra medida, `lemma-absence` y `lemma-count` no movieron números, porque las
propuestas no sacaron la última aparición de ninguna palabra; el arrastre está en el código.

Dónde pasa hoy: `get consolidated` con pendientes (el dashboard muestra la foto con pendientes en los paneles de nivel,
topic y tema) y el impacto de cada propuesta de `revise-instructions`, que corre hasta 20 tareas por invocación. `analyze`,
`stats` y `revise` de a una tarea corren el motor una vez y no están afectados.

## Alcance

- **Adentro**: R001 y R002, para los siete analizadores de hoy y los que se sumen.
- **Afuera**: cómo se logra (construir los analizadores en cada corrida, como diseña FEAT-HALL, o vaciarlos al empezar);
  qué muestra la vista consolidada (FEAT-PROYU); los números del impacto por propuesta más allá del arrastre (FEAT-PROYU).

## User Journeys

### Journey[F-CERO-J001] - Pedir la vista consolidada de un par activo
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-CERO-J001
    name: Pedir la vista consolidada de un par activo
    flow:
      - id: pedir
        action: "El operador pide la vista consolidada del par activo"
        outcomes:
          - when: "El plan activo tiene pendientes aplicables, así que el motor corre dos veces en la misma invocación"
            then: dos_fotos
          - when: "El plan activo no tiene pendientes aplicables"
            then: una_foto
      - id: dos_fotos
        action: "La foto con pendientes da los mismos números que una invocación nueva sobre ese curso, y la foto con lo aprobado conserva sus diagnósticos COCA de topic"
        gate: [F-CERO-R001, F-CERO-R002]
        result: success
      - id: una_foto
        action: "La foto con lo aprobado da los mismos números que analyze sobre ese curso"
        gate: [F-CERO-R001]
        result: success
```

## References

- **FEAT-HALL** — Determinismo e identidad del hallazgo ([F-HALL-R014](#F-HALL-R014)) y el puntaje de vocabulario
  ([F-HALL-R009](#F-HALL-R009)); su diseño construye cada analizador por corrida. Citado por R001.
- **FEAT-CDIFF** — Las tres fotos salen del motor ([F-CDIFF-R007](#F-CDIFF-R007)). Citado por R001 y R002.
