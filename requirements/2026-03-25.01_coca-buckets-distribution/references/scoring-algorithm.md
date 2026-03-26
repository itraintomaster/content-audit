# Algoritmo de Scoring - LemmaCocaBucketsDistribution

## Score por Bucket Individual (BucketResult)

```
funcion calculateScore(percentage, targetPercentage, kind, optimalRange, adequateRange):
    diff = |percentage - targetPercentage|

    // Determinar estado
    si kind == "atLeast":
        si percentage >= targetPercentage - optimalRange:
            status = OPTIMAL
        si_no si percentage >= targetPercentage - adequateRange:
            status = ADEQUATE
        si_no:
            status = DEFICIENT
    si_no (atMost):
        si percentage <= targetPercentage + optimalRange:
            status = OPTIMAL
        si_no si percentage <= targetPercentage + adequateRange:
            status = ADEQUATE
        si_no:
            status = EXCESSIVE

    // Calcular score
    segun status:
        OPTIMAL:   score = 1.0
        ADEQUATE:  score = 0.8
        DEFICIENT: score = max(0.0, 0.8 - (diff - adequateRange) * 0.04)
        EXCESSIVE: score = max(0.0, 0.8 - (diff - adequateRange) * 0.04)
```

<!-- Sugerencia: El factor 0.04 en la degradacion del score es arbitrario. Cada punto porcentual extra de distancia resta 0.04 del score base de 0.8. Esto significa que a 20% de distancia del rango adecuado, el score llega a 0. -->

## Score por Quarter

Promedio de los scores de todos los buckets que tienen targets en ese quarter.

## Score por Nivel

Promedio ponderado entre:
- Score de los buckets del nivel
- Score de los quarters (si aplica estrategia quarters)
- Score de los topics dentro del nivel

## Score General (overallScore)

Promedio de los scores de todos los niveles.

## Evaluacion de Progresion

```
funcion getActualProgression(porcentajes_por_nivel):
    si hay menos de 2 niveles: retornar STATIC

    incrementos = 0
    decrementos = 0

    para cada par consecutivo de niveles:
        si porcentaje[i+1] > porcentaje[i] + margen:
            incrementos++
        si porcentaje[i+1] < porcentaje[i] - margen:
            decrementos++

    si solo incrementos: retornar ASCENDING
    si solo decrementos: retornar DESCENDING
    si ambos: retornar IRREGULAR
    si ninguno: retornar STATIC
```

## Assessment por Bucket

Cada bucket genera una recomendacion textual:

| Estado | Recomendacion |
|--------|---------------|
| OPTIMAL | "Distribution is within the optimal range" |
| ADEQUATE | "Distribution is acceptable but could be improved" |
| DEFICIENT | "Increase words in this frequency band" |
| EXCESSIVE | "Reduce words in this frequency band" |
