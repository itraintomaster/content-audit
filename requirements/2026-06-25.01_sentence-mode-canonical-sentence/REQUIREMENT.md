---
feature:
  id: FEAT-SMODE
  code: F-SMODE
  name: Modo de frase del knowledge y frase canonica puntuable
  priority: critical
---

# Modo de frase del knowledge y frase canonica puntuable

## TL;DR

**Que**: Cada knowledge basado en sentencias declara un **modo de frase**
(`REWRITE` o `FILL`) que determina cual es la **frase canonica puntuable** de
sus quizzes: en `FILL` es la oracion completa con el hueco relleno; en
`REWRITE` es solo la oracion respuesta, sin la oracion fuente/prompt.

**Por que**: Hoy la frase canonica se arma concatenando todas las partes del
quiz, lo que en ejercicios de reescritura incluye erroneamente la oracion
fuente, duplica contenido, infla el conteo de tokens y degrada el score de
longitud de frase ([F-SLEN-R002](#F-SLEN-R002)). El modo de frase desambigua que se
puntua y mantiene el score estable a traves de las revisiones.

## Reglas de Negocio

Las reglas se agrupan en tres bloques:

- **Grupo A - Modo de frase del knowledge (R001, R002)**: el modo como
  propiedad declarada de cada knowledge basado en sentencias.
- **Grupo B - Frase canonica puntuable segun el modo (R003, R004, R005)**:
  como el modo selecciona la frase que se tokeniza y puntua.
- **Grupo C - Estabilidad del modo a traves de la revision (R006, R007)**:
  que el modo gobierna tambien la frase canonica del quiz revisado.

---

### Grupo A - Modo de frase del knowledge

<a id="F-SMODE-R001"></a>
### Rule[F-SMODE-R001] - Todo knowledge basado en sentencias tiene un modo de frase
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada knowledge cuyos quizzes son oraciones evaluables declara un modo de
> frase con exactamente uno de dos valores: `REWRITE` (reescritura) o `FILL`
> (relleno). No existe un knowledge basado en sentencias sin modo, ni con un
> valor distinto de esos dos.

<details><summary>Detalle</summary>

El modo es una propiedad **del knowledge**, no de cada quiz individual: todos
los quizzes de un mismo knowledge comparten el mismo tipo pedagogico de
ejercicio. Esto refleja como esta organizado el contenido: un knowledge agrupa
quizzes que ejercitan el mismo concepto con la misma mecanica de respuesta.

Los dos modos corresponden a los dos tipos pedagogicos observados entre los
quizzes basados en sentencias:

| Modo | Tipo pedagogico | Que hace el alumno | Ejemplo de prompt | Respuesta del alumno |
|------|-----------------|--------------------|-------------------|----------------------|
| `REWRITE` | Reescritura / transformacion | Transforma una oracion fuente en otra forma | "You should add the salt to the soup." | "Add the salt to the soup." |
| `FILL` | Relleno / gap-fill | Completa una palabra dentro de una oracion | "She dances ___ (good/well)." | "She dances well." |

Esta clasificacion es limpia y bien definida por knowledge: sobre los datos
actuales, los ~608 knowledges basados en sentencias se reparten en ~211
`REWRITE` y ~397 `FILL`, con un unico caso historicamente mal etiquetado por
inconsistencia de autoria (en realidad `REWRITE`). Que ese caso atipico se
corrija o se trate como excepcion de dato es trabajo de saneamiento de datos,
fuera del alcance funcional de esta regla.

Los knowledges cuyos quizzes **no** son oraciones evaluables (etiquetas, listas
de vocabulario, instrucciones) quedan fuera del alcance de esta regla: ya estan
excluidos del analisis de longitud por [F-SLEN-R001](#F-SLEN-R001) y no necesitan modo de
frase.

**Error**: "El knowledge basado en sentencias '{knowledgeId}' no declara un modo de frase valido (REWRITE o FILL)"

</details>

<a id="F-SMODE-R002"></a>
### Rule[F-SMODE-R002] - El modo de frase es uniforme dentro del knowledge
**Severity**: major | **Validation**: AUTO_VALIDATED

> Todos los quizzes de un mismo knowledge se interpretan bajo el mismo modo de
> frase. El modo no varia quiz a quiz: se declara una sola vez para el
> knowledge y aplica por igual a cada uno de sus quizzes basados en sentencias.

<details><summary>Detalle</summary>

Esta regla fija la granularidad: el modo vive en el knowledge y se hereda hacia
sus quizzes. Un quiz no puede tener un modo propio que contradiga al de su
knowledge. La consecuencia practica es que la frase canonica de cada quiz
(Grupo B) se determina mirando el modo de su knowledge, no una propiedad local
del quiz.

**Error**: N/A (esta regla define la granularidad del modo)

</details>

---

### Grupo B - Frase canonica puntuable segun el modo

<a id="F-SMODE-R003"></a>
### Rule[F-SMODE-R003] - La frase canonica puntuable se determina segun el modo del knowledge
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La frase canonica puntuable de un quiz se selecciona segun el modo de su
> knowledge: en `FILL` es la oracion completa (todas las partes del ejercicio,
> con el hueco relleno en su lugar); en `REWRITE` es **solo** la oracion
> respuesta, sin incluir la oracion fuente/prompt.

<details><summary>Detalle</summary>

La "frase canonica puntuable" es la unica oracion que se tokeniza y puntua por
quiz (la posicion canonica que consume el analisis de longitud, ver
[F-QSENT-R018](#F-QSENT-R018)). Esta regla redefine como se construye esa oracion en
funcion del modo:

| Modo | Frase canonica puntuable | Ejemplo (partes del ejercicio) | Frase puntuada |
|------|--------------------------|--------------------------------|----------------|
| `FILL` | Oracion completa con el hueco relleno | prompt "She dances ___ (good/well)." + respuesta "well" | "She dances well." |
| `REWRITE` | Solo la oracion respuesta | fuente "You should watch the DVD." + respuesta "Watch the DVD." | "Watch the DVD." |

En `FILL` el comportamiento coincide con la derivacion historica: la oracion
completa es el resultado de sustituir la respuesta en el lugar del hueco dentro
del texto del ejercicio, sin hints ([F-QSENT-R019](#F-QSENT-R019)).

En `REWRITE` la oracion fuente/prompt es scaffolding pedagogico que el alumno
lee pero no escribe; lo que se evalua pedagogicamente es la respuesta. Por eso
la oracion fuente **no se incluye** en la frase canonica y **no se puntua**.

**Error**: "No se pudo determinar la frase canonica puntuable del quiz '{quizId}' para el modo {modo} de su knowledge '{knowledgeId}'"

</details>

<a id="F-SMODE-R004"></a>
### Rule[F-SMODE-R004] - En REWRITE la oracion fuente nunca se incluye ni se puntua
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para un quiz de un knowledge en modo `REWRITE`, la oracion fuente/prompt
> queda excluida de la frase canonica puntuable. La frase canonica contiene
> unicamente la oracion respuesta; el conteo de tokens y el score de longitud
> de frase se calculan solo sobre esa respuesta.

<details><summary>Detalle</summary>

Esta regla es el corazon del problema observado. Caso real: un quiz de
reescritura cuya respuesta correcta es "Watch the DVD." (4 tokens, adecuado
para su nivel) se media como "You should watch the DVD. Watch the DVD." (10
tokens) porque la frase canonica concatenaba la oracion fuente con la
respuesta. El resultado era una caida espuria del score de longitud de frase
del 100% al 60%.

Bajo esta regla, la frase canonica del quiz es "Watch the DVD." y se puntua
sobre 4 tokens, sin el arrastre de la oracion fuente.

La exclusion es total: la oracion fuente no aporta tokens, no aparece
concatenada, y no influye en el conteo ni en el delta respecto al rango
objetivo ([F-SLEN-R002](#F-SLEN-R002)).

**Error**: "La frase canonica del quiz '{quizId}' (modo REWRITE) incluyo la oracion fuente; debe contener solo la respuesta"

</details>

<a id="F-SMODE-R005"></a>
### Rule[F-SMODE-R005] - El score de longitud de frase se calcula sobre la frase canonica determinada por el modo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El conteo de tokens y el score de longitud de frase de un quiz
> ([F-SLEN-R002](#F-SLEN-R002), [F-SLEN-R013](#F-SLEN-R013)) se calculan sobre la frase canonica
> puntuable que el modo determino ([F-SMODE-R003](#F-SMODE-R003)), no sobre una
> concatenacion ciega de todas las partes del quiz.

<details><summary>Detalle</summary>

Esta regla ata el modo al analisis de longitud existente sin cambiar como
puntua el analisis. El analisis sigue recibiendo **una** oracion por quiz, la
tokeniza en tokens linguisticos ([F-SLEN-R013](#F-SLEN-R013)) y la puntua por distancia al
rango objetivo de su nivel CEFR ([F-SLEN-R002](#F-SLEN-R002)). Lo unico que cambia es
**cual** es esa oracion: la frase canonica que el modo seleccciona.

Consecuencia sobre el diagnostico tipado: el `tokenCount` que expone el
diagnostico de longitud ([F-DSLEN-R001](#F-DSLEN-R001)) refleja la frase canonica
determinada por el modo. En un quiz `REWRITE`, `tokenCount` cuenta solo la
respuesta.

**Error**: N/A (esta regla ata el modo al calculo de score ya existente)

</details>

---

### Grupo C - Estabilidad del modo a traves de la revision

<a id="F-SMODE-R006"></a>
### Rule[F-SMODE-R006] - La revision que preserva la estructura conserva el modo del knowledge
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cuando un quiz se revisa preservando su estructura (mismo tipo de ejercicio,
> mismos huecos, misma mecanica de respuesta), el quiz revisado pertenece al
> mismo knowledge y por lo tanto se interpreta bajo el **mismo modo de frase**.
> La frase canonica del quiz revisado se determina con ese modo, igual que la
> del quiz original.

<details><summary>Detalle</summary>

Las revisiones automaticas ([F-LAPS-R001](#F-LAPS-R001)) reemplazan el quiz por uno nuevo
que cambia el vocabulario y conserva la estructura del ejercicio. Como la
revision preserva la estructura, el quiz revisado sigue siendo del mismo tipo
pedagogico y vive en el mismo knowledge: su modo de frase no cambia.

La frase canonica del quiz revisado se determina aplicando [F-SMODE-R003](#F-SMODE-R003)
con el modo del knowledge:

- En `FILL`, la frase canonica del quiz revisado es la oracion completa con el
  nuevo hueco relleno.
- En `REWRITE`, la frase canonica del quiz revisado es solo la nueva respuesta;
  la oracion fuente del quiz revisado no se incluye ni se puntua.

**Error**: "El quiz revisado '{quizId}' se evaluo con un modo de frase distinto al de su knowledge '{knowledgeId}'"

</details>

<a id="F-SMODE-R007"></a>
### Rule[F-SMODE-R007] - Una revision REWRITE que conserva la longitud de la respuesta no degrada el score de longitud
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Criterio de aceptacion: para un quiz `REWRITE`, una revision que mantiene la
> longitud (en tokens) de la oracion respuesta produce un quiz revisado con el
> **mismo** score de longitud de frase que el original. El score no se degrada
> por re-incluir o no la oracion fuente, porque la oracion fuente nunca entra
> en la frase canonica ([F-SMODE-R004](#F-SMODE-R004)).

<details><summary>Detalle</summary>

Este es el criterio observable que cierra el problema de motivacion. Antes, una
revision de reescritura que preservaba la estructura producia una caida espuria
del score porque la frase canonica del quiz revisado arrastraba la oracion
fuente. Con el modo aplicado de forma estable a la revision ([F-SMODE-R006](#F-SMODE-R006)),
si la respuesta revisada tiene la misma cantidad de tokens que la respuesta
original, el conteo de tokens de la frase canonica es identico y el score de
longitud de frase no cambia.

Concretamente, partiendo del caso real: quiz original con respuesta
"Watch the DVD." (4 tokens, score 100% para su nivel). Una revision que cambia
el vocabulario manteniendo 4 tokens en la respuesta (por ejemplo
"Watch the film.") produce un quiz revisado cuya frase canonica sigue siendo de
4 tokens y cuyo score de longitud de frase sigue siendo 100%. La oracion fuente
del quiz revisado, sea cual sea, no participa.

La regla no exige que el score quede en 100% en abstracto: exige que **no se
degrade respecto al original cuando la longitud de la respuesta se conserva**.
Si la revision deliberadamente acorta o alarga la respuesta, el score puede
cambiar — pero ese cambio refleja la longitud real de la respuesta, no el
arrastre de la oracion fuente.

**Error**: "La revision REWRITE del quiz '{quizId}' degrado el score de longitud sin cambiar la longitud de la respuesta"

</details>

## Contexto

El sistema ContentAudit puntua la **longitud de frase** de cada quiz basado en
sentencias comparando la cantidad de tokens de su oracion contra un rango
objetivo por nivel CEFR ([F-SLEN-R002](#F-SLEN-R002), [F-SLEN-R012](#F-SLEN-R012)). Cada quiz tiene
una **frase canonica** (la oracion que se tokeniza y puntua), y hoy esa frase
se arma de una unica manera: concatenando todas las partes del ejercicio y
rellenando los huecos con la respuesta canonica
([F-QSENT-R017](#F-QSENT-R017)..[F-QSENT-R019](#F-QSENT-R019), pre-computada desde la fuente de datos en
[F-DBSENT-R001](#F-DBSENT-R001)).

El problema es que esa derivacion uniforme no distingue entre los dos tipos
pedagogicos de ejercicio:

1. **Reescritura (transformacion)**: el alumno transforma una oracion fuente en
   otra forma. Se muestra "You should add the salt to the soup." y el alumno
   escribe el imperativo "Add the salt to the soup.". La frase que debe
   puntuarse es **solo** la respuesta.
2. **Relleno (gap-fill)**: el alumno completa una palabra dentro de una oracion.
   "She dances ___ (good/well)." se resuelve como "She dances well.". La frase
   que debe puntuarse es la oracion **completa** con el hueco relleno.

En reescritura, concatenar todas las partes incluye erroneamente la oracion
fuente, duplica contenido e infla el conteo de tokens. Caso real observado: una
respuesta correcta "Watch the DVD." (4 tokens) se midio como
"You should watch the DVD. Watch the DVD." (10 tokens), bajando el score de
100% a 60% sin que el contenido evaluable hubiera empeorado. El efecto es
especialmente nocivo cuando una revision automatica preserva la estructura del
ejercicio: la revision no introdujo ningun problema, pero el score cae porque
la frase canonica re-incluye la fuente.

Este requerimiento introduce el **modo de frase del knowledge** como la
propiedad que desambigua que oracion es la frase canonica puntuable
([F-SMODE-R003](#F-SMODE-R003)) y garantiza que esa eleccion sea estable a traves de las
revisiones ([F-SMODE-R006](#F-SMODE-R006), [F-SMODE-R007](#F-SMODE-R007)).

### Por que el modo vive en el knowledge

El tipo pedagogico es homogeneo dentro de un knowledge: todos sus quizzes
ejercitan el mismo concepto con la misma mecanica de respuesta. La clasificacion
empirica sobre los datos actuales lo confirma — los knowledges basados en
sentencias se reparten limpiamente entre `REWRITE` y `FILL` con un unico caso
atipico por error de autoria. Declarar el modo por knowledge (y no por quiz)
evita repetir la propiedad miles de veces y elimina la posibilidad de que dos
quizzes del mismo ejercicio se interpreten de forma incoherente
([F-SMODE-R002](#F-SMODE-R002)).

## Alcance

- **En alcance**: declarar el modo de frase (`REWRITE` / `FILL`) por knowledge
  basado en sentencias; determinar la frase canonica puntuable segun el modo;
  calcular el score de longitud de frase sobre esa frase; conservar el modo al
  revisar un quiz preservando su estructura.
- **Fuera de alcance**:
  - **Como se asigna el modo a cada knowledge** (clasificacion automatica,
    heuristica sobre los datos, anotacion manual, saneamiento del caso mal
    etiquetado). Esta regla asume que el modo es una propiedad disponible del
    knowledge; el procedimiento para poblarlo es un trabajo de datos aparte.
  - **Puntuar la oracion fuente de un ejercicio `REWRITE` como dimension
    separada** (por ejemplo, evaluar tambien la complejidad del prompt). Aqui la
    oracion fuente simplemente no se puntua.
  - **Cambiar la formula de scoring de longitud** ([F-SLEN-R002](#F-SLEN-R002)): el scoring
    no cambia; solo cambia cual oracion recibe.
  - **Soporte multi-variante del analisis** ([F-QSENT-R018](#F-QSENT-R018)): el analisis
    sigue puntuando una sola oracion por quiz (la canonica); el modo solo
    determina cual es esa oracion.
  - **Otros analisis distintos de longitud de frase**: el modo determina la
    frase canonica puntuable que consume el analisis de longitud; si otros
    analisis quisieran consumir la misma seleccion, seria un requerimiento
    propio.

## User Journeys

### Journey[F-SMODE-J001] - Puntuar la longitud de frase de un quiz segun el modo de su knowledge
**Validation**: AUTO_VALIDATED

Cubre la seleccion de la frase canonica segun el modo y el calculo del score
sobre esa frase, para los dos modos. Cubre R001-R005.

```yaml
journeys:
  - id: F-SMODE-J001
    name: Puntuar la longitud de frase de un quiz segun el modo de su knowledge
    flow:
      - id: tomar_quiz
        action: "El sistema toma un quiz basado en sentencias para puntuar su longitud de frase"
        then: leer_modo

      - id: leer_modo
        action: "El sistema determina el modo de frase del knowledge al que pertenece el quiz"
        gate: [F-SMODE-R001, F-SMODE-R002]
        outcomes:
          - when: "El modo del knowledge es FILL"
            then: frase_completa
          - when: "El modo del knowledge es REWRITE"
            then: frase_solo_respuesta

      - id: frase_completa
        action: "El sistema construye la frase canonica puntuable como la oracion completa con el hueco relleno"
        gate: [F-SMODE-R003]
        then: puntuar

      - id: frase_solo_respuesta
        action: "El sistema construye la frase canonica puntuable como solo la oracion respuesta, sin la oracion fuente/prompt"
        gate: [F-SMODE-R003, F-SMODE-R004]
        then: puntuar

      - id: puntuar
        action: "El sistema calcula el conteo de tokens y el score de longitud de frase sobre la frase canonica determinada por el modo"
        gate: [F-SMODE-R005]
        result: success
```

### Journey[F-SMODE-J002] - Revisar un quiz REWRITE sin degradar el score de longitud
**Validation**: AUTO_VALIDATED

Cubre la estabilidad del modo a traves de la revision y el criterio de
aceptacion de no-degradacion. Cubre R006-R007.

```yaml
journeys:
  - id: F-SMODE-J002
    name: Revisar un quiz REWRITE sin degradar el score de longitud
    flow:
      - id: revisar_quiz
        action: "El sistema produce un quiz revisado de un quiz REWRITE preservando la estructura del ejercicio"
        then: heredar_modo

      - id: heredar_modo
        action: "El sistema interpreta el quiz revisado bajo el mismo modo de frase del knowledge del quiz original"
        gate: [F-SMODE-R006]
        then: frase_revisada

      - id: frase_revisada
        action: "El sistema construye la frase canonica del quiz revisado con el modo REWRITE: solo la nueva respuesta, sin la oracion fuente"
        gate: [F-SMODE-R003, F-SMODE-R004]
        then: comparar_score

      - id: comparar_score
        action: "El sistema calcula el score de longitud de frase del quiz revisado sobre su frase canonica"
        gate: [F-SMODE-R005]
        outcomes:
          - when: "La respuesta revisada conserva la longitud en tokens de la respuesta original"
            then: score_estable
          - when: "La respuesta revisada cambia la longitud en tokens respecto de la original"
            then: score_refleja_respuesta

      - id: score_estable
        action: "El score de longitud de frase del quiz revisado es identico al del original, sin degradacion por arrastre de la oracion fuente"
        gate: [F-SMODE-R007]
        result: success

      - id: score_refleja_respuesta
        action: "El score de longitud de frase del quiz revisado refleja la longitud real de la nueva respuesta, sin influencia de la oracion fuente"
        gate: [F-SMODE-R004]
        result: success
```

## Open Questions

### Doubt[DOUBT-SMODE-SOURCE] - De donde proviene el modo de frase de cada knowledge?
**Status**: OPEN

El modo de frase es una propiedad limpia por knowledge (R001), pero este
requerimiento no fija como se origina su valor. Sobre los datos actuales la
clasificacion `REWRITE` vs `FILL` se infiere de forma confiable a partir de la
forma del ejercicio (en `REWRITE`, la respuesta es una oracion completa,
capitalizada, multi-palabra y con puntuacion final; en `FILL`, la respuesta es
una palabra o fragmento que se inserta en una oracion). La pregunta es si el
modo se declara como dato explicito de cada knowledge o se infiere de la forma.

- [ ] Opcion A: El modo se declara como propiedad explicita de cada knowledge en
  la fuente de datos. Mas robusto frente a casos atipicos (como el knowledge mal
  etiquetado), pero requiere poblar el dato para los ~608 knowledges.
- [ ] Opcion B: El modo se infiere de la forma del ejercicio mediante un criterio
  deterministico. No requiere poblar dato nuevo, pero hereda el riesgo de
  clasificar mal los casos atipicos.
- [ ] Opcion C: Hibrido — se infiere por defecto y se permite una anotacion
  explicita que sobreescriba la inferencia para los casos atipicos.

**Answer**: Pendiente. No bloquea las reglas de este requerimiento: R001-R007
describen el comportamiento **dado** el modo del knowledge, independientemente
de como se haya poblado. La eleccion del origen del dato es decision de producto
y de saneamiento de datos.

### Doubt[DOUBT-SMODE-NONSENTENCE] - Un knowledge mixto (algunos quizzes no-oracion) sigue teniendo un unico modo?
**Status**: RESOLVED

Un knowledge puede contener quizzes que no son oraciones evaluables (excluidos
del analisis de longitud por [F-SLEN-R001](#F-SLEN-R001)) junto con quizzes que si lo son.
La pregunta era si eso obliga a un modo por quiz.

**Answer**: No. El modo de frase aplica solo a los quizzes basados en sentencias
del knowledge (R001). Los quizzes no-oracion ya estan excluidos del analisis de
longitud y no consumen el modo. El modo sigue siendo unico por knowledge
([F-SMODE-R002](#F-SMODE-R002)) y gobierna unicamente a los quizzes evaluables.

## References

- **FEAT-SLEN** — Define el analisis de longitud de frase: exclusion de quizzes
  no-oracion, conteo de tokens linguisticos, scoring por distancia al rango
  objetivo por nivel CEFR. Este requerimiento determina **cual** oracion recibe
  ese analisis. Citada por [F-SMODE-R004](#F-SMODE-R004),
  [F-SMODE-R005](#F-SMODE-R005) y el Contexto.
- **FEAT-QSENT** — Define la derivacion de la frase plana canonica desde la
  estructura del ejercicio (variante canonica, sin hints, whitespace
  normalizado). Este requerimiento se apoya en esa derivacion para el modo
  `FILL` y la restringe a la respuesta para el modo `REWRITE`. Citada por
  [F-SMODE-R003](#F-SMODE-R003) y el Contexto.
- **FEAT-DBSENT** — La frase plana hoy se carga pre-computada desde la fuente de
  datos. Este requerimiento determina, por modo, cual es la frase puntuable que
  debe corresponder a ese valor. Citada por el Contexto.
- **FEAT-LAPS** — Produce los quizzes revisados que preservan la estructura del
  ejercicio. Este requerimiento exige que la frase canonica del quiz revisado se
  determine con el mismo modo del knowledge. Citada por
  [F-SMODE-R006](#F-SMODE-R006).
- **FEAT-DSLEN** — Expone el diagnostico tipado de longitud (tokenCount, delta,
  etc.). El `tokenCount` reportado refleja la frase canonica determinada por el
  modo. Citada por [F-SMODE-R005](#F-SMODE-R005).
</content>
</invoke>
