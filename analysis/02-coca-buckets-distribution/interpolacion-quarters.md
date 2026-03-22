# Interpolacion Lineal de Quarters

## Como funciona

Cuando la estrategia es `quarters`, el sistema necesita targets para 4 trimestres (Q1-Q4) pero la configuracion solo define targets para el **initial quarter** (Q1) y el **last quarter** (Q4). Los trimestres intermedios (Q2, Q3) se calculan por interpolacion lineal.

## Formula

```
Para cada bucket en un nivel dado:
    target_Q1 = initialQuarter.target
    target_Q4 = lastQuarter.target

    target_Q2 = target_Q1 + (target_Q4 - target_Q1) * (1/3)
    target_Q3 = target_Q1 + (target_Q4 - target_Q1) * (2/3)
```

## Ejemplo: A1, bucket top1k

```
Q1 (initial): target = 90%
Q4 (last):    target = 75%

Q2: 90 + (75 - 90) * (1/3) = 90 - 5.0 = 85.0%
Q3: 90 + (75 - 90) * (2/3) = 90 - 10.0 = 80.0%
Q4: 75.0%
```

Resultado: 90% -> 85% -> 80% -> 75%

## Ejemplo: A1, bucket top4k

```
Q1 (initial): target = 0%
Q4 (last):    target = 2%

Q2: 0 + (2 - 0) * (1/3) = 0.67%
Q3: 0 + (2 - 0) * (2/3) = 1.33%
Q4: 2%
```

Resultado: 0% -> 0.67% -> 1.33% -> 2%

## Asignacion de Oraciones a Quarters

Las oraciones se asignan a quarters segun su posicion ordinal dentro de los topics del nivel. Los topics se dividen equitativamente en 4 grupos.

<!-- Sugerencia: La forma exacta de asignar oraciones a quarters depende de como se organizan los topics dentro de un nivel. Si un nivel tiene 12 topics, se asignan 3 por quarter. Si tiene 5, la distribucion no es uniforme. Verificar el codigo del Analyzer para la logica exacta. -->

## Implementacion en Java (LevelBucketTargets)

```java
public List<QuarterBucketsTarget> getAllQuarterBucketTargets() {
    List<QuarterBucketsTarget> result = new ArrayList<>();
    result.add(initialQuarter);  // Q1

    // Interpolar Q2 y Q3
    for (int i = 1; i <= 2; i++) {
        double factor = (double) i / 3.0;
        List<BucketTarget> interpolated = new ArrayList<>();
        for (int j = 0; j < initialQuarter.buckets().size(); j++) {
            BucketTarget initial = initialQuarter.buckets().get(j);
            BucketTarget last = lastQuarter.buckets().get(j);
            double target = linearInterpolation(
                initial.targetPercentage(), last.targetPercentage(), factor
            );
            interpolated.add(new BucketTarget(initial.name(), target, initial.kind()));
        }
        result.add(new QuarterBucketsTarget(interpolated));
    }

    result.add(lastQuarter);  // Q4
    return result;
}
```

<!-- Cuidado aqui: La interpolacion usa el "kind" del initialQuarter para todos los quarters intermedios. Si el kind cambia entre Q1 y Q4, los trimestres intermedios heredan el kind de Q1. -->
