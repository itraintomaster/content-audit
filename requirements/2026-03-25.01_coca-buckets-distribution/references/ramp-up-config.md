# Configuracion Ramp-Up - CocaBucketsDistribution

## Descripcion

La configuracion ramp-up es una variante del analisis de distribucion COCA disenada para **cursos introductorios** que usan un vocabulario mucho mas restringido. En lugar de las bandas estandar (top1k-top4k), usa bandas mucho mas estrechas.

<!-- Cuidado aqui: Esta configuracion existe en el YAML pero el orquestador principal solo usa la seccion "course". Verificar si hay otro flujo que use ramp-up. -->

## Bandas de Frecuencia Ramp-Up

| Bucket | Valor | Contenido |
|--------|-------|-----------|
| 135 | Top 135 palabras | Las palabras mas basicas del ingles |
| 250 | Top 250 palabras | Vocabulario funcional basico |
| 500 | Top 500 palabras | Vocabulario elemental |
| 1000 | Top 1000+ palabras | Bucket abierto |

## Targets por Unidad

### Unidad 1
| Bucket | Target | Kind |
|--------|--------|------|
| 135 | 80% | atLeast |
| 500 | 7% | atMost |
| 1000 | 0% | atMost |

### Unidad 2
| Bucket | Target | Kind |
|--------|--------|------|
| 135 | 75% | atLeast |
| 500 | 10% | atMost |
| 1000 | 3% | atMost |

### Unidad 3
| Bucket | Target | Kind |
|--------|--------|------|
| 135 | 70% | atLeast |
| 500 | 15% | atMost |
| 1000 | 5% | atMost |

### Unidad 4
| Bucket | Target | Kind |
|--------|--------|------|
| 135 | 65% | atLeast |
| 500 | 20% | atMost |
| 1000 | 10% | atMost |

## YAML Original

```yaml
rampUp:
  buckets:
    values:
      - 135
      - 250
      - 500
      - 1000
    open: true
  unitTargets:
    - unit: 1
      buckets:
        - bucket: 135
          targetPercentage: 80
          kind: atLeast
        - bucket: 500
          targetPercentage: 7
          kind: atMost
        - bucket: 1000
          targetPercentage: 0
          kind: atMost
    - unit: 2
      buckets:
        - bucket: 135
          targetPercentage: 75
          kind: atLeast
        - bucket: 500
          targetPercentage: 10
          kind: atMost
        - bucket: 1000
          targetPercentage: 3
          kind: atMost
    - unit: 3
      buckets:
        - bucket: 135
          targetPercentage: 70
          kind: atLeast
        - bucket: 500
          targetPercentage: 15
          kind: atMost
        - bucket: 1000
          targetPercentage: 5
          kind: atMost
    - unit: 4
      buckets:
        - bucket: 135
          targetPercentage: 65
          kind: atLeast
        - bucket: 500
          targetPercentage: 20
          kind: atMost
        - bucket: 1000
          targetPercentage: 10
          kind: atMost
```
