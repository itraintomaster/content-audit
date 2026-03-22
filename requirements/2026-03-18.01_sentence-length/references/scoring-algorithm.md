# Algoritmo de Scoring - SentenceLengthAnalyzer

## Score por Oracion Individual

```
funcion getScore(length, targetMin, targetMax):
    si length >= targetMin Y length <= targetMax:
        retornar 1.0   // Dentro del rango optimo

    si length < targetMin:
        distance = targetMin - length
    sino:
        distance = length - targetMax

    si distance >= 4:
        retornar 0.0   // Demasiado lejos del rango

    retornar max(0.0, 1.0 - (distance / 4.0))  // Interpolacion lineal
```

### Ejemplo visual para A1 (min=5, max=8):

```
Length:  1    2    3    4    5    6    7    8    9    10   11   12
Score: 0.0  0.25 0.5  0.75 1.0  1.0  1.0  1.0  0.75 0.5  0.25 0.0
```

<!-- Sugerencia: El margen de 4 tokens esta hardcodeado. En la migracion, consider hacerlo configurable. -->

## Score por Nivel

El score de un nivel es el **promedio** de los scores de todas sus oraciones validas (excluyendo las marcadas como `noSentence`).

## Score General (overallScore)

Es el **promedio** de los scores de todos los niveles CEFR que tienen al menos una oracion.

## Evaluacion de Progresion

```
funcion evaluateProgression(metricsByLevel):
    averages = promedios de longitud para cada nivel CEFR (en orden A1 -> B2)

    si hay menos de 2 niveles con datos:
        retornar INSUFFICIENT_DATA

    para cada par consecutivo (nivel_i, nivel_i+1):
        si promedio[i+1] > promedio[i]: marcar hasIncrease
        si promedio[i+1] < promedio[i]: marcar hasDecrease

    si solo hasIncrease: retornar POSITIVE
    si solo hasDecrease: retornar REGRESSIVE
    otro caso: retornar STAGNANT
```
