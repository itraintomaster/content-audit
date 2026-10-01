---
patch: FEAT-FILAS
requirement: 2026-10-01.02_filas-claras
generated: 2026-10-01T13:40:00Z
---

# Tech Spec: filas claras

La ficha suma una frase y la fila de la comparación suma cinco datos, que decide content-audit y la app sólo pinta. El
comparador recibe el renderizador del alumno para mostrar el ejercicio del ejemplo. Los nombres nuevos no tocan ningún
contrato: viven en el cuerpo de cada ficha. Tampoco cambian la clave de nada: «vocabulario» y «algún error» siguen
siendo el `subject` que nombran las metas.

## Add the explanation to the analyzer card

La frase vive en la ficha, junto al nombre en castellano, porque es del analizador y no de la tabla: la usan la
comparación y `get analyzers`, y un analizador nuevo la trae con su ficha. Como el nombre, no entra en la huella de la
medida (F-RONDA2-R004): cambiar una frase no vuelve a medir la línea base. El catálogo no la exige al arrancar; la prueba
del catálogo real de `Main` cuida que los veinte la traigan.

```architecture
modules:
- name: audit-domain
  models:
  - name: AnalyzerDescriptor
    type: record
    fields:
    - name: displayName
      type: String
      _change: modify
    - name: explanation
      type: String
      _change: add
    _change: modify
  _change: modify
```

## Add the visible name, the direction, the verdict, the explanation and the example to each row

La app hoy busca el nombre de la fila en la ficha y, si no la encuentra, muestra la clave; por eso «vocabulario» y
«algún error» salían en minúscula. Renombrar las claves rompería las metas declaradas en `analysis/metas.json`, los ids
de las metas por defecto y la huella de las metas fijadas. Por eso la fila lleva su nombre visible aparte del `subject`.

`betterWhen` y `changeVerdict` son enums y no booleanos. Así el JSON dice «LOWER» o «NOT_EVALUATED» y la app no
interpreta nada. `NOT_EVALUATED` le gana al número: una fila sin número, o cuyo analizador no evaluó ningún ejercicio
del nodo, nunca se lee como un cero.

El ejemplo es un record con `quizId`, `exercise` y `detail`. Un hallazgo que no es de un ejercicio, como una palabra del
nivel, lo lleva sin ejercicio.

```architecture
modules:
- name: revision-domain
  packages:
  - name: comparison
    models:
    - name: BetterDirection
      type: enum
      fields:
      - name: LOWER
      - name: HIGHER
      _change: add
    - name: ChangeVerdict
      type: enum
      fields:
      - name: IMPROVED
      - name: WORSENED
      - name: UNCHANGED
      - name: NOT_EVALUATED
      _change: add
    - name: RowExample
      type: record
      fields:
      - name: quizId
        type: String
      - name: exercise
        type: String
      - name: detail
        type: String
      _change: add
    - name: ComparisonRow
      type: record
      fields:
      - name: subject
        type: String
        _change: modify
      - name: displayName
        type: String
        _change: add
      - name: betterWhen
        type: BetterDirection
        _change: add
      - name: changeVerdict
        type: ChangeVerdict
        _change: add
      - name: explanation
        type: String
        _change: add
      - name: example
        type: RowExample
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Inject the student view renderer into the comparator

El ejercicio del ejemplo es «como lo ve el alumno», el mismo texto que leen los jueces y la muestra ciega. Se arma con el
renderizador inyectado, como en la bandeja y la muestra ciega, y no con un formato propio. Sin renderizador, el ejemplo
cae al rótulo del nodo del hallazgo.

La elección del ejemplo va en un ayudante del motor, como `ReportTree` y `PendingCopies`. Recorre una vez cada foto y
guarda, por nodo y por fila, el mejor candidato: primero el que la tanda arregla, después el más concreto y el de
identificador menor. No recorre el subárbol por cada fila.

```architecture
modules:
- name: revision-domain
  packages:
  - name: comparisonengine
    implementations:
    - name: DefaultBatchComparator
      requiresInject:
      - name: studentViewRenderer
        type: QuizStudentViewRenderer
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Declare the tests of the new rows

Las pruebas del comparador cubren R001 a R005 sobre la foto en miniatura del curso del 29/9 que ya usa su clase. Las del
formateador fijan que el JSON trae los campos nuevos y el ejemplo `null` explícito: el mapper omite los nulos, y la
fila los incluye a propósito. La prueba del comando produce el JSON de muestra contra el que se hace la app. Las de
`Main` y `GetCmd` miran el catálogo real.

```architecture
modules:
- name: audit-cli
  packages:
  - name: commands
    implementations:
    - name: DefaultCompareCommand
      handwrittenTests:
      - name: "should print with -f json, for a batch of two exercises, the comparison\
          \ of the sample file the review app is built against"
        traceability:
          feature: FEAT-FILAS
          rule: F-FILAS-R006
      _change: modify
    _change: modify
  - name: formatting
    implementations:
    - name: DefaultBatchComparisonFormatter
      handwrittenTests:
      - name: should write the example of a row without one as null in the JSON, never
          leave it out
        traceability:
          feature: FEAT-FILAS
          rule: F-FILAS-R006
      _change: modify
    _change: modify
  _change: modify
```

El resto de los nombres está en `architectural_patch.yaml`. Las cuatro pruebas cuyo nombre citaba un nombre viejo se
renombran en dos parches aparte, uno de FEAT-RONDA1 y otro de FEAT-RONDA2, porque Sentinel acepta un solo feature por
parche. Conservan su trazabilidad.
