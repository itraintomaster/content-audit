---
patch: FEAT-RECUR
requirement: 2026-09-30.16_recurrencia
generated: 2026-09-30T20:00:00Z
---

# Tech Spec: Recurrencia visible, por palabra y por nivel (FEAT-RECUR)

Paquete D, ronda 1. `lemma-recurrence` sigue con sus umbrales (50 y 1.000), su tope de 2.000, su orden A1 → B2 y su
puntaje de curso, 0,08; José decidió el 30/9 que no cuenta las palabras vistas una sola vez y que por nivel es detalle, no
puntaje. Diseñado contra el modelo de 0.2 y 0.3: **vienen de 0.3** `ContentAnalyzer.findingsAt`, el hallazgo y el
`LemmaRecurrenceAnalyzerProvider` con su ficha (COURSE, sin cambios de forma). Lo compartido, el slot en `CourseDiagnoses`
y la línea del formateador en `Main`, está en `ronda1/ganchos-D.md` (D-10 y D-11). Medido con un prototipo que reproduce el
análisis 2026-09-30T11-54-02 exacto: 91.873 posiciones, 2.791 lemas, 1.750 que vuelven, 134 · 1.570 · 46.

## Publicar en el curso lo que el analizador ya calcula
Hoy el analizador arma sus estadísticas por lema y su resumen, escribe el 0,08 y los tira: por eso la vista del tablero dice
«No lemma recurrence data available» (R003). Lo propio de un analizador va en su diagnóstico tipado (F-HALL-R001 inv. 3), así
que el resultado entero pasa a un diagnóstico de curso, con un solo slot nuevo en `CourseDiagnoses`. El detalle por nivel
viaja adentro del mismo diagnóstico en vez de ocupar otro slot en cada nivel: es un cálculo del curso, y la vista de G
muestra la recurrencia en el curso.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lrec
        _change: modify
        visibility: internal
        models:
          - name: LemmaRecurrenceCourseDiagnosis
            _change: add
            type: record
            fields:
              - { name: result, type: LemmaRecurrenceResult }
              - { name: levels, type: "List<LevelRecurrence>" }
features:
  - id: FEAT-RECUR
    code: F-RECUR
```

## Sumar a cada palabra su hueco más largo y sus niveles
La desviación que José preguntó dice si los huecos son parejos, pero no se lee en la pantalla; el hueco más largo, con el
nivel donde empieza y el nivel donde termina, dice lo mismo en palabras: «suppose» vuelve en promedio cada 696, pero tiene
un hueco de 27.379 de B1 a B2. El analizador ya recorre los ejercicios en orden y conoce el milestone de cada uno, así que
anota el nivel de cada posición sin pedir nada nuevo; su constructor (10 construcciones) no cambia. `LemmaStats` se
construye en un solo lugar del código y en un test, así que sumarle campos es barato.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lrec
        _change: modify
        visibility: internal
        models:
          - name: LemmaStats
            _change: modify
            fields:
              - { name: longestGap, type: int, _change: add }
              - { name: longestGapFromLevel, type: CefrLevel, _change: add }
              - { name: longestGapToLevel, type: CefrLevel, _change: add }
              - { name: levels, type: "List<CefrLevel>", _change: add }
```

## Medir la recurrencia dentro de cada nivel, como detalle y no como puntaje
Cada nivel se mide con sus propios ejercicios y los mismos umbrales (R004): A1 450 lemas que vuelven y 39,6 % normales, A2
752 y 20,6 %, B1 1.139 y 14,1 %, B2 819 y 20,1 %. Queda como datos del diagnóstico y nunca como clave de puntaje del nivel:
una clave «lemma-recurrence/…» en el milestone subiría al curso por la agregación genérica, que completa lo que al padre le
falta, y sería un número más que nadie pidió; así el vocabulario de cada nivel no puede tocarlo (José, 30/9). Con la
recurrencia como puntaje, los niveles bajaban de 96,5 a 88,4 en A1: esa puerta queda cerrada.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lrec
        _change: modify
        visibility: internal
        models:
          - name: LevelRecurrence
            _change: add
            type: record
            fields:
              - { name: level, type: CefrLevel }
              - { name: positions, type: int }
              - { name: returningLemmas, type: int }
              - { name: normalCount, type: int }
              - { name: subExposedCount, type: int }
              - { name: overExposedCount, type: int }
              - { name: normalShare, type: double }
```

## Dejar un hallazgo por lema fuera de rango, en el curso
Con la ficha de 0.3 (`lemma-recurrence` evalúa sólo COURSE), `findingsAt` en el curso devuelve un borrador por lema poco
visto o sobre-expuesto: 1.570 más 46, 1.616 en total. Regla F-RECUR-R005, marca el lema (la clave del analizador no lleva
categoría), gravedad baja, resolución «sólo ordena», así que no bajan ningún puntaje ni piden tareas. La evidencia es la
medida junto a su meta: «movie: 90 veces, vuelve cada 1.005 palabras (meta: más de 50 y hasta 1.000); su hueco más largo,
6.510, va de A1 a A2». Los lemas vistos una vez siguen afuera (F-LREC-R006): ya son hallazgos de `lemma-count`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lrec
        _change: modify
        visibility: internal
        models:
          - name: LemmaStats
            _change: modify
            fields:
              - { name: longestGap, type: int, _change: add }
              - { name: longestGapFromLevel, type: CefrLevel, _change: add }
              - { name: longestGapToLevel, type: CefrLevel, _change: add }
```

## Mostrarlo en stats analyzer, leído del informe
`stats analyzer lemma-recurrence` muestra cada lema y cada nivel con un formateador detallado, como ya tienen
`lemma-absence` y las bandas COCA: lee el diagnóstico del curso y no recalcula nada (F-HALL-R010). Registrarlo es una línea
en el mapa de formateadores de `Main` (enganche D-11). `get audit` sigue mostrando sólo el resumen del curso, como con todo
analizador, y la vista del tablero lee el mismo diagnóstico del informe (paquete G).

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: formatting
        _change: modify
        implementations:
          - name: LemmaRecurrenceDetailedFormatter
            _change: add
            types: [Component]
            visibility: public
            implements: [DetailedFormatter]
```

## Fijar que nada cambia de número
Para @qa-tester (R001, R002, R004): con posiciones sintéticas, el diagnóstico publicado tiene que cerrar sus sumas, normales
más poco vistos más sobre-expuestos igual a lemas analizados, en el curso y en cada nivel, y el puntaje del curso tiene que
ser el mismo con y sin publicar el diagnóstico; ningún milestone recibe una clave de `lemma-recurrence`. Sobre el curso real,
la prueba de F-HALL-R013 del integrador fija 0,08, el 73,9 % y la única tarea LEMMA_RECURRENCE del plan. El lematizador
compartido casi no la toca: corregido, vuelven 1.744 lemas, 134 normales, y el puntaje sigue en 0,08.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lrec
        _change: modify
        visibility: internal
        models:
          - name: LemmaRecurrenceCourseDiagnosis
            _change: add
            type: record
            fields:
              - { name: result, type: LemmaRecurrenceResult }
              - { name: levels, type: "List<LevelRecurrence>" }
```
