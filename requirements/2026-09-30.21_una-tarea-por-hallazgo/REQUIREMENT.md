---
feature:
  id: FEAT-TAREA
  code: F-TAREA
  name: Una tarea por hallazgo - el plan de la familia de errores
  priority: critical
---

# Una tarea por hallazgo: el plan de la familia de errores

## TL;DR

**Qué**: El plan convierte cada hallazgo de la familia de errores en una tarea propia, o declara qué otra tarea lo
cubre o por qué no se convirtió; cada tarea nombra su hallazgo, su gravedad, cómo se resuelve y qué puede tocar su arreglo.

**Por qué**: Hoy el plan hace una tarea por analizador y ejercicio y sólo conoce ocho nombres: los once analizadores nuevos
no tendrían tareas, y un ejercicio con cuatro errores quedaría en una sola tarea que nadie puede dar por cerrada.

## Recorrido de uso (estado actual)

Cómo quien corrige el curso intenta hoy pasar de lo que está mal a trabajo concreto:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible; con FEAT-HALL, el informe trae
   hallazgos de forma común.
2. Arma el plan → `PlanCommand.plan(auditId, storageMode, withCorrectionContext)`. ✓ Disponible, pero ⚠️ hace una tarea
   por nodo y analizador con puntaje menor que 1, sin nombrar ningún hallazgo; para un analizador sin tipo de tarea sólo
   declara cuántos nodos no pudo convertir ([F-HALL-R004](#F-HALL-R004)).
3. Lista las tareas → `GetCommand.get("tasks", name, GetTasksFilter)`. ✓ Disponible; filtra por estado, nivel del árbol
   y tipo de diagnóstico. ⚠️ No filtra por analizador, resolución, gravedad, nivel CEFR ni tema.
4. Mira una tarea → `GetCommand.get("task", id, …)`. ✓ Disponible con [contexto de corrección](glossary:Contexto de corrección) para largo, lemas,
   título y consigna. ⚠️ Para los analizadores nuevos no hay nada: ni la evidencia ni qué se puede tocar.

**Punto de fricción**: pasos 2 a 4. El hallazgo ya dice qué está mal, con qué gravedad y cómo se resuelve, pero el plan
lo pierde: agrupa por analizador y ejercicio y no guarda de qué hallazgo salió la tarea.

**Cambio mínimo necesario**: en la familia de errores, `plan` crea una tarea por hallazgo que cuenta como error, con el
hallazgo adentro, y cierra la cuenta con las cubiertas y las no convertidas; `get tasks` filtra por analizador,
resolución, gravedad, nivel y tema; y `get task` muestra el hallazgo y lo que su arreglo puede tocar. Las tareas de
vocabulario quedan como hoy.

## Reglas de Negocio

Acá **tema** es lo que el curso llama *knowledge* (413 en la base del 29/9), como en FEAT-HALL. **Contar como error**
es lo que fija [F-HALL-R008](#F-HALL-R008): todo hallazgo de la familia de errores salvo los que sólo ordenan.

<a id="F-TAREA-R001"></a>
### Rule[F-TAREA-R001] - Cada hallazgo que cuenta como error termina en una tarea, cubierto o declarado
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para cada hallazgo de la familia de errores que cuenta como error, en un nodo que su analizador evaluó, el plan
> tiene exactamente una de tres salidas:
> 1. una [tarea de corrección](glossary:Tarea de corrección) propia, y una sola;
> 2. «cubierto por otra tarea», con el identificador de esa tarea ([F-TAREA-R004](#F-TAREA-R004));
> 3. «no convertido», dentro del recuento de su analizador, con el motivo.
>
> Por analizador, los hallazgos que cuentan como error suman lo mismo que sus tareas, sus cubiertos y sus no
> convertidos. En la familia de errores no hay tarea sin hallazgo.

Extiende [F-HALL-R004](#F-HALL-R004), que ya prohíbe perder un puntaje menor que 1 en silencio: la tercera salida es su
invariante 3, y la segunda es la que pidió el paquete D ([DOUBT-TAREA-COMPARTIDA](#DOUBT-TAREA-COMPARTIDA)). Mantiene
[F-RCLA-R001](#F-RCLA-R001): todo nodo con un hallazgo que cuenta como error tiene puntaje menor que 1 en ese analizador
([F-HALL-R004](#F-HALL-R004), invariante 1), así que nunca hay tarea donde el puntaje es 1.

**Criterio**: el ejercicio 11 de *Afirmativas regulares* (A1, «I ___ a lot of vegetables.») tiene, según el 077,
hallazgos de tres analizadores —pista, una sola respuesta y [traducción](glossary:Traducción) y sentido—: cada hallazgo es
una tarea, todas del mismo ejercicio, y la mesa las resuelve con un solo candidato ([F-MESA-R001](#F-MESA-R001)).

<a id="F-TAREA-R002"></a>
### Rule[F-TAREA-R002] - La tarea nombra su hallazgo, y el mismo análisis da las mismas tareas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Toda tarea de la familia de errores lleva la identidad de su hallazgo —analizador, regla, nodo y lo que marca—, su
> gravedad, su resolución (*regla* o *mesa*) y su costo, más el [nivel CEFR](glossary:Nivel CEFR) y el tema del nodo. Dos planes armados sobre
> el mismo análisis tienen las mismas tareas, con las mismas identidades y en el mismo orden.

La identidad es la de [F-HALL-R014](#F-HALL-R014): no depende de la corrida ni de la hora, así que la tanda puede decir
si el hallazgo de una tarea sigue o no en el análisis siguiente comparando identidades ([F-TANDA-R006](#F-TANDA-R006)).
El [tipo de diagnóstico](glossary:Tipo de diagnóstico) sigue siendo uno por analizador (hoy `QUIZ_INSTRUCTION`; cada analizador nuevo suma el suyo con
su ficha, [F-HALL-R006](#F-HALL-R006)); lo que distingue a dos tareas del mismo tipo en un ejercicio es su hallazgo.

<a id="F-TAREA-R003"></a>
### Rule[F-TAREA-R003] - Una miniteoría con problema da una tarea en su quiz
**Severity**: major | **Validation**: VALIDATED

> Cada hallazgo de `mini-theory` produce una tarea propia sobre el quiz cuyo caso se juzgó. Que otro quiz del tema muestre
> el mismo texto no agrupa las tareas ni declara un hallazgo cubierto: cada una conserva la identidad de su quiz y se
> cierra sólo cuando la miniteoría de ese quiz queda corregida ([F-TAREA-R001](#F-TAREA-R001)).

Mantiene [F-MINIT-R003](#F-MINIT-R003) y [F-MINIT-R007](#F-MINIT-R007): la miniteoría es una corrección específica del
caso de cada quiz, y su hallazgo y reescritura no se propagan por texto compartido. Reemplaza la decisión del 30/9 de
agrupar por texto, corregida por José el 5/10 al aclarar «miniteoría es POR QUIZ».

<a id="F-TAREA-R004"></a>
### Rule[F-TAREA-R004] - Lo que ya marca otra tarea no abre una segunda
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando un hallazgo de la familia de errores marca, en el mismo ejercicio, lo mismo que una tarea que el plan ya tiene
> —la palabra de otro nivel que también marca `lemma-absence`—, el plan no crea otra tarea: declara el hallazgo cubierto
> por esa tarea, y esa tarea se da por resuelta sólo cuando los dos analizadores dejan de marcarlo.

Es la opción A de [DOUBT-TAREA-COMPARTIDA](#DOUBT-TAREA-COMPARTIDA), que José tomó con la recomendación el 30/9: el plan
de vocabulario no cambia ([F-NIVFR-R006](#F-NIVFR-R006)) y la tarea `LEMMA_ABSENCE` de ese ejercicio entra a la tanda
del nivel ([F-TANDA-R001](#F-TANDA-R001)).

<a id="F-TAREA-R005"></a>
### Rule[F-TAREA-R005] - Lo que sólo ordena no es tarea, y el desacuerdo de los jueces va a la bandeja
**Severity**: major | **Validation**: AUTO_VALIDATED

> Un hallazgo que sólo ordena no produce tarea (mantiene [F-HALL-R004](#F-HALL-R004), invariante 2). El hallazgo «los
> jueces no coinciden» tampoco: aparece en la bandeja de la tanda de su nivel como desempate
> ([F-BANDEJA-R002](#F-BANDEJA-R002)), y sólo si José dice que es un error pasa a tener tarea.

Es la decisión de José del 30/9 sobre traducción y sentido: «si los dos de traducción no coinciden, queda marcado para la
bandeja» ([F-TRSEN-R003](#F-TRSEN-R003)).

<a id="F-TAREA-R006"></a>
### Rule[F-TAREA-R006] - Cada tipo de tarea declara lo que su arreglo puede tocar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo tipo de tarea de la familia de errores declara qué partes del ejercicio (o del tema) puede cambiar su arreglo;
> lo que puede cambiar un ejercicio es la unión de lo que declaran sus tareas, y todo lo demás se conserva. Un tipo de
> tarea sin esa declaración nunca se intenta: su tarea queda «sin alcance declarado» en el informe de la tanda.

Mantiene [F-RPRES-R001](#F-RPRES-R001) y [F-RPRES-R002](#F-RPRES-R002): hoy un tipo sin alcance declarado conserva todo,
así que su corrección no podría cambiar nada; la regla lo vuelve visible en vez de gastar intentos.

<details><summary>Qué puede tocar cada arreglo, según la ronda 1 (ilustración: lo fija el paquete de cada analizador)</summary>

| Analizador | Resolución | Lo que puede tocar |
|---|---|---|
| `hint-coherence` | regla; mesa si está mal la respuesta | la pista; en la mesa, también las respuestas y el audio |
| `answer-completeness` | regla | las respuestas aceptadas (agrega con «\|») |
| `multiple-choice-validity` | regla (mayúsculas); mesa (dos correctas; sigue siendo opción múltiple, José 30/9) | las opciones; en la mesa, también el enunciado, la traducción y el audio |
| `data-coherence` | regla; mesa si el audio no coincide | la oración guardada, la «/» de las respuestas, el pedido de regrabar |
| `exact-duplicates` | mesa | el ejercicio que sobra entero, con su audio |
| `translation-sense` | mesa | la traducción, o el inglés con su audio (los dos arreglos) |
| `single-answer` | regla (otra forma); mesa | las respuestas, la pista o la oración con su audio |
| `mini-theory` | mesa, una tarea por quiz con hallazgo | sólo la miniteoría de ese quiz |
| `quiz-instruction` | mesa | lo que alcanza hoy la corrección de consigna ([F-QICOR-R003](#F-QICOR-R003)) |
| `off-level-words` | cubierto por `LEMMA_ABSENCE` | lo que alcanza hoy la [corrección de lemas](glossary:Corrección de lemas) |

</details>

<a id="F-TAREA-R007"></a>
### Rule[F-TAREA-R007] - Las tareas de vocabulario son las de hoy
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Las tareas de los analizadores de vocabulario son exactamente las que da hoy el mismo análisis, con los mismos tipos,
> nodos y cantidades: sobre la base del 29/9, 4.036 (`SENTENCE_LENGTH` 3.051, `LEMMA_ABSENCE` 950,
> `KNOWLEDGE_INSTRUCTIONS_LENGTH` 25, `KNOWLEDGE_TITLE_LENGTH` 6, `COCA_BUCKETS` 3, `LEMMA_RECURRENCE` 1).

Mantiene [F-HALL-R013](#F-HALL-R013) y [F-RCLA-R001](#F-RCLA-R001). «Una tarea por hallazgo» rige sólo en la familia de
errores: en vocabulario cambiaría las 950 tareas de lemas, que hoy son una por ejercicio aunque marque varias palabras.

<a id="F-TAREA-R008"></a>
### Rule[F-TAREA-R008] - Las tareas se buscan por analizador, resolución, gravedad, nivel y tema
**Severity**: major | **Validation**: AUTO_VALIDATED

> `get tasks` acepta, además de sus filtros de hoy, el analizador, la resolución, la gravedad, el nivel CEFR y el tema,
> combinables entre sí y con el orden por prioridad, y el resultado trae de cada tarea su hallazgo. Un valor que no
> existe —un analizador fuera del catálogo, una gravedad fuera de las cuatro— rechaza la consulta.

Mantiene [F-CLIRV-R016](#F-CLIRV-R016) y [F-HALL-R005](#F-HALL-R005) para el nombre del analizador. La prioridad no
cambia de cálculo: sigue saliendo del puntaje, como hoy.

<a id="F-TAREA-R009"></a>
### Rule[F-TAREA-R009] - Una tarea de errores se ve con su hallazgo y con lo que su arreglo puede tocar
**Severity**: major | **Validation**: AUTO_VALIDATED

> `get task` de una tarea de la familia de errores muestra el ejercicio como lo ve el alumno, el hallazgo con su
> evidencia ([F-HALL-R003](#F-HALL-R003)), su gravedad y su resolución, lo que su arreglo puede tocar
> ([F-TAREA-R006](#F-TAREA-R006)) y, si es del tema, los ejercicios que alcanza. Es lo mismo que recibe la mesa
> ([F-MESA-R003](#F-MESA-R003)).

## Contexto

El brief 089 describe el paquete E como «una tarea por hallazgo», y José lo aprobó el 30/9. FEAT-HALL dejó el plan como
estaba —tarea donde el puntaje es menor que 1— y le pasó a este paquete qué tipo de tarea crea cada analizador nuevo y
cómo se corrige ([F-HALL-R004](#F-HALL-R004)); su TECH_SPEC lo repite: el resolvedor de contexto, el revisor, el alcance
de preservación y la vista de `get task` de cada analizador nuevo son de E.

Hoy el plan recorre el árbol, toma cada puntaje menor que 1 y lo traduce a un tipo de diagnóstico con una lista fija de
ocho nombres. Si el nombre no está, la tarea se pierde; FEAT-HALL lo cambia por una declaración. Pero sigue haciendo una
tarea por analizador y ejercicio: en el 077 hay 3.133 cambios en 2.892 ejercicios, y un mismo ejercicio puede tener
un error que se arregla con una regla —la mayúscula de una opción— y otro que necesita la mesa —dos opciones correctas—,
del mismo analizador. Una tarea por hallazgo separa esos caminos y deja contar, al cerrar una tanda, cuántos hallazgos
se arreglaron.

## Alcance

- **Adentro**: tareas de la familia de errores (una por hallazgo, cubiertas y no convertidas), su identidad, su alcance,
  cómo se buscan y cómo se ven.
- **Afuera**: cómo se corrige cada tarea (FEAT-MESA y la tanda, FEAT-TANDA); qué analizadores existen y qué marcan
  (paquetes A, B, C y D); cambiar el plan de vocabulario, su prioridad o sus tipos; los tipos de diagnóstico de cada
  analizador nuevo, que llegan con su ficha y los suma el integrador en el parche de cada ronda.

## User Journeys

### Journey[F-TAREA-J001] - Armar el plan de un análisis con hallazgos de errores
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-TAREA-J001
    name: Armar el plan de un análisis con hallazgos de errores
    flow:
      - id: pedir_plan
        action: "El operador pide el plan de un análisis que trae hallazgos de la familia de errores"
        then: clasificar
      - id: clasificar
        action: "El plan recorre los hallazgos que cuentan como error"
        gate: [F-TAREA-R001, F-TAREA-R005]
        outcomes:
          - when: "Ninguna tarea marca lo mismo en ese ejercicio y su analizador tiene tipo de tarea"
            then: tarea_propia
          - when: "Una tarea que ya existe marca la misma palabra en el mismo ejercicio"
            then: cubierto
          - when: "El analizador todavía no tiene tipo de tarea"
            then: no_convertido
      - id: tarea_propia
        action: "El plan tiene una tarea con la identidad del hallazgo, su gravedad, su resolución y lo que puede tocar"
        gate: [F-TAREA-R002, F-TAREA-R003, F-TAREA-R006]
        then: vocabulario_igual
      - id: cubierto
        action: "El plan declara el hallazgo cubierto por la tarea que ya existe, sin crear otra"
        gate: [F-TAREA-R004]
        then: vocabulario_igual
      - id: no_convertido
        action: "El plan declara el analizador, cuántos hallazgos no convirtió y por qué"
        gate: [F-TAREA-R001]
        then: vocabulario_igual
      - id: vocabulario_igual
        action: "Las tareas de vocabulario son las mismas que da hoy ese análisis, y las de errores suman lo mismo que sus hallazgos"
        gate: [F-TAREA-R007]
        then: buscar
      - id: buscar
        action: "El operador pide las tareas de un tema filtradas por resolución y ve cada una con su hallazgo"
        gate: [F-TAREA-R008, F-TAREA-R009]
        result: success
```

## Open Questions

<a id="DOUBT-TAREA-COMPARTIDA"></a>
### Doubt[DOUBT-TAREA-COMPARTIDA] - Cuando dos analizadores marcan la misma palabra, ¿quién abre la tarea?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): la tarea `LEMMA_ABSENCE` que ya existe; el hallazgo se declara cubierto por ella.
- [ ] Opción B: cada analizador su tarea, y la corrección las junta por ejercicio y palabra.
- [ ] Opción C: el analizador de palabras de otro nivel sólo cuenta y no pide tarea.

**Answer**: **Opción A**. Es la duda homónima del paquete D (FEAT-NIVFR), que José tomó con la recomendación el 30/9
(«por lo demás, continúa»). Se repite acá porque la regla que la cumple es de este paquete: [F-TAREA-R004](#F-TAREA-R004).

## References

- **FEAT-HALL** — Hallazgo, familias, identidad y plan que no pierde puntajes: R001 a R009. Cited by R001, R002, R005,
  R008, R009.
- **FEAT-RCLA, FEAT-RPRES, FEAT-CLIRV** — Tarea sólo con puntaje menor que 1; preservación de lo no declarado; nombre
  único del analizador: R001, R006, R007, R008.
- **FEAT-QICOR** — Lo que alcanza hoy la corrección de consigna: R006.
- **FEAT-NIVFR, FEAT-MINIT, FEAT-TRSEN** (ronda 1, paquetes C y D) — Tarea compartida de vocabulario, miniteoría por quiz y desacuerdo
  de los jueces: R003, R004, R005.
- **FEAT-MESA, FEAT-TANDA, FEAT-BANDEJA** (este paquete) — Quién toma estas tareas y dónde van los desempates: R004,
  R005, R009.
