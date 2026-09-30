---
feature:
  id: FEAT-DUPEX
  code: F-DUPEX
  name: Duplicados exactos - dos ejercicios iguales del mismo tema
  priority: high
---

# Duplicados exactos: dos ejercicios iguales del mismo tema

## TL;DR

**Qué**: Un analizador de la familia de errores marca cada ejercicio que repite, tal cual, a otro de su mismo tema: el
alumno escribe dos veces lo mismo.

**Por qué**: Hoy nadie lo mira: el 077 encontró 30 repetidos y content-audit los muestra en verde. La parte exacta es
una regla de segundos; los parecidos, con modelo, son del paquete B.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy encontrar ejercicios repetidos:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)` → `AuditRunner.runAudit(Path, AuditRunRequest)`. ✓ Disponible.
2. Mira el tema de un ejercicio → `AuditNode.scores` y `AuditNode.diagnoses`. ⚠️ Sólo hay vocabulario: ningún analizador
   compara un ejercicio con sus hermanos: «They ___ (not / be) at the park.» está dos veces en *Práctica mixta* y ningún
   número lo muestra.
3. Pide ese analizador solo → `AnalyzeOptions.analyzers`. ⚠️ No existe en `GetCommand.get("analyzers", …)`.

**Punto de fricción**: pasos 2 y 3. No hay ningún analizador que compare ejercicios entre sí.

**Cambio mínimo necesario**: sumar `exact-duplicates` al catálogo de FEAT-HALL: un analizador de errores que deja un
hallazgo en cada copia, con el ejercicio que queda como evidencia. No cambia ningún puntaje de vocabulario.

## Reglas de Negocio

Acá **tema** es lo que el curso llama *knowledge* (413), como en FEAT-HALL. **Ejercicio terminado** es el enunciado
tal como lo ve el alumno, sin la pista, con la primera respuesta aceptada en el hueco; en opción múltiple, con la
opción correcta.

<a id="F-DUPEX-R001"></a>
### Rule[F-DUPEX-R001] - Dos ejercicios del mismo tema que terminan igual son duplicados exactos
**Severity**: major | **Validation**: ASSUMPTION

> Dos ejercicios de un mismo tema son duplicados exactos si su ejercicio terminado es igual comparado como compara el
> corrector: sin mayúsculas, espacios, signos ni tildes. Invariantes:
> 1. no entran la pista, la [traducción](glossary:Traducción), la consigna ni las opciones incorrectas;
> 2. entra todo el texto fijo que ve el alumno, también la oración de partida de una reescritura o un diálogo;
> 3. dos ejercicios iguales de temas distintos no dan hallazgo.

<details><summary>Detalle y casos del curso</summary>

- Duplicado: `67fab6d5993010229534224b` y `67fab6d59930102295342260` (A1, *Práctica mixta*): los dos terminan
  «They weren't at the park.».
- Duplicado aunque las opciones cambien: `67fab6d59930102295343d28` («The show started ___ time.», *in / on*) y
  `67fab6d59930102295344a4c` («The show started ___ .», *in time / on time*), B1, *In time u on time*: los dos terminan
  «The show started on time.».
- No es duplicado: en *Presente simple: so/neither* (B1) once ejercicios terminan en «So am I.», pero cada uno responde
  a otra oración («A: We are at the beach. (I)», «A: I am bored. (I)»): la oración de partida entra (invariante 2).
- No es duplicado exacto: «I don't have ___ time.» y «I didn't have ___ time.» (`67fab6d59930102295342b7f`): es un
  parecido, del paquete B.

Que la pista no entre es [ASSUMPTION]: ver [DOUBT-DUPEX-PISTA](#DOUBT-DUPEX-PISTA). Comparar como el corrector es la
misma normalización de [F-RESPC-R001](#F-RESPC-R001).

</details>

<a id="F-DUPEX-R002"></a>
### Rule[F-DUPEX-R002] - Un hallazgo por copia, nunca en el ejercicio que queda
**Severity**: major | **Validation**: ASSUMPTION

> En un grupo de *n* ejercicios iguales del tema, el primero en el orden del tema queda sin hallazgo y cada uno de los
> otros *n* − 1 lleva uno. Invariantes:
> 1. el grupo suma *n* − 1 ejercicios con error en los números de FEAT-HALL;
> 2. el hallazgo nombra al ejercicio que queda ([F-HALL-R003](#F-HALL-R003), «con qué otro nodo»);
> 3. el orden del tema es el del curso, así que dos corridas marcan las mismas copias ([F-HALL-R014](#F-HALL-R014)).

Cuál queda es [ASSUMPTION]: [DOUBT-DUPEX-CUAL-QUEDA](#DOUBT-DUPEX-CUAL-QUEDA). El 077 marcó la copia sin un criterio
fijo: en un par eligió la de identificador menor y en otro la mayor.

<a id="F-DUPEX-R003"></a>
### Rule[F-DUPEX-R003] - El hallazgo es de gravedad baja, va a la mesa y cuesta segundos
**Severity**: major | **Validation**: AUTO_VALIDATED

> Todo hallazgo de `exact-duplicates` tiene gravedad *baja*, resolución *mesa* y costo *instantáneo*
> ([F-HALL-R002](#F-HALL-R002)), y cuenta como error ([F-HALL-R008](#F-HALL-R008)).

*Baja* porque el alumno puede acertar; es lo que puso el 077 en sus 30. *Mesa* porque arreglarlo es escribir un
ejercicio nuevo: el que propone reemplaza la copia con palabras del semáforo del tema (propuesta, analizador 5). Borrar
la copia no está en ninguna opción: el tema perdería un ejercicio.

<a id="F-DUPEX-R004"></a>
### Rule[F-DUPEX-R004] - La evidencia muestra los dos ejercicios como los ve el alumno
**Severity**: major | **Validation**: AUTO_VALIDATED

> La evidencia de cada hallazgo trae la copia y el ejercicio que queda, cada uno con su enunciado, su pista y, si es de
> opción múltiple, sus opciones; el ejercicio terminado común; y la línea «Idéntico a <id> del mismo tema».

Ejemplo: «They ___ (not / be) at the park. → weren't · idéntico a 67fab6d59930102295342260 del mismo tema: «They weren't
at the park.»». Nunca el formato interno («They ____ [weren't] (not / be)…»), como pide [F-HALL-R003](#F-HALL-R003).

<a id="F-DUPEX-R005"></a>
### Rule[F-DUPEX-R005] - `exact-duplicates` es un analizador de errores que evalúa ejercicios
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `exact-duplicates` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Dos ejercicios del mismo
> tema dicen lo mismo?», familia *errores*, evalúa *ejercicio*, resolución *mesa*, costo *instantáneo*. Un ejercicio
> con hallazgo tiene puntaje 0 en este analizador y uno sin hallazgo, 1 ([F-HALL-R004](#F-HALL-R004)).

El nombre es [ASSUMPTION], en el estilo de los existentes (`sentence-length`, `quiz-instruction`). El puntaje 0 o 1
basta para que el plan tenga tarea sólo donde hay copia; qué tarea es, lo decide el paquete E.

## Contexto

La revisión del 077 marcó 30 ejercicios repetidos dentro de su tema. Parte son idénticos y parte son casi iguales
(«This is his car.» y «That is his car.»). Los idénticos salen con una regla de segundos. Los parecidos necesitan un
modelo local que compare oraciones, y ése es el paquete B.

Base: curso vivo del 29/9 en `db/english-course` (11.287 ejercicios, 413 temas). Medido con un prototipo de la regla en
el scratchpad del analista, sin tocar content-audit:

| Medida | A1 | A2 | B1 | B2 | Total |
|---|---|---|---|---|---|
| Copias exactas dentro del tema (R001, sin pista) | 17 | 2 | 4 | 0 | 23 |
| Marcadas por el 077 (exactas y parecidas) | 17 | 7 | 6 | 0 | 30 |
| Ejercicios por nivel | 2.402 | 2.411 | 4.554 | 1.920 | 11.287 |

Las 23 copias son 23 pares en 14 temas; 10 son de *Doble consonante y e* (A1), como «stop - stopping» dos veces.
Iguales en temas distintos hay 33 grupos con 69 ejercicios, por ejemplo «She dances well (good / well).» en *Adjetivo
o adverbio* y en *¿Good o well?*: quedan afuera (R001, invariante 3).

**El examen.** `escribir/duplicados.json` tiene 15 casos (A1 3, A2 6, B1 6), cortados por tema: 9 son exactos y 6 son
parecidos. El prototipo encuentra los 9 exactos (8 en la copia y 1 en el gemelo, que `medir.py` también cuenta) y no
marca nada más en los 194 temas de esa mitad. La otra mitad tiene 15 casos. **Meta propuesta**: 10 de cada 10 de los
exactos; sobre la categoría entera cerca de 6 de cada 10, porque el resto es de B; y 9 de cada 10 marcas nuevas reales
en una muestra. **Costo**: segundos para el curso entero, sin modelo.

## Alcance

- **Adentro**: la regla exacta dentro del tema, el hallazgo en cada copia y su ficha en el catálogo.
- **Afuera**: los parecidos con un modelo local (paquete B); los iguales entre temas
  ([DOUBT-DUPEX-ENTRE-TEMAS](#DOUBT-DUPEX-ENTRE-TEMAS)); cómo se reemplaza la copia, con la mesa y sus tandas (paquete E);
  y cambiar el curso: analizar no escribe nada ([F-HALL-R015](#F-HALL-R015)).

## User Journeys

### Journey[F-DUPEX-J001] - Encontrar las copias de un tema
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-DUPEX-J001
    name: Encontrar las copias de un tema
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo sólo exact-duplicates"
        then: comparar_tema
      - id: comparar_tema
        action: "El informe compara los ejercicios terminados de cada tema, sin pista y como compara el corrector"
        gate: [F-DUPEX-R001]
        outcomes:
          - when: "Dos ejercicios del tema terminan igual"
            then: marca_copia
          - when: "Hay iguales sólo en temas distintos"
            then: sin_hallazgo
          - when: "Ningún ejercicio del tema se repite"
            then: sin_hallazgo
      - id: marca_copia
        action: "La copia lleva un hallazgo de gravedad baja, para la mesa, con los dos ejercicios como los ve el alumno; el primero del tema queda sin hallazgo"
        gate: [F-DUPEX-R002, F-DUPEX-R003, F-DUPEX-R004]
        then: contar
      - id: contar
        action: "El tema, su nivel y el curso cuentan una vez la copia en los errores de exact-duplicates"
        gate: [F-DUPEX-R005]
        result: success
      - id: sin_hallazgo
        action: "Ningún ejercicio del tema tiene hallazgo de exact-duplicates y todos quedan con puntaje 1"
        gate: [F-DUPEX-R001, F-DUPEX-R005]
        result: success
```

## Open Questions

<a id="DOUBT-DUPEX-PISTA"></a>
### Doubt[DOUBT-DUPEX-PISTA] - ¿La pista entra en la comparación?
**Status**: OPEN

- [ ] Opción A (recomendada): no; se compara lo que el alumno escribe con el texto que lo rodea. Da 23 copias.
- [ ] Opción B: sí; dos ejercicios con distinta pista no son duplicados. Da 22.

**Answer**: Pendiente. Recomiendo A: «rice ___ (no plural form)» y «rice ___» (`67fab6d59930102295342313` y `…2314`,
*Contable o incontable*) piden la misma respuesta con las mismas opciones; la pista sólo lo hace más fácil.

<a id="DOUBT-DUPEX-CUAL-QUEDA"></a>
### Doubt[DOUBT-DUPEX-CUAL-QUEDA] - En un par, ¿cuál lleva el hallazgo?
**Status**: OPEN

- [ ] Opción A (recomendada): la copia es la que viene después en el orden del tema.
- [ ] Opción B: los dos llevan hallazgo.
- [ ] Opción C: el que tenga más hallazgos de otros analizadores.

**Answer**: Pendiente. Recomiendo A: es estable entre corridas y cuenta un error por copia. B cuenta 46 en vez de 23,
y C ata este analizador a los demás.

<a id="DOUBT-DUPEX-ENTRE-TEMAS"></a>
### Doubt[DOUBT-DUPEX-ENTRE-TEMAS] - ¿Y los iguales de temas distintos?
**Status**: OPEN

- [ ] Opción A (recomendada): quedan afuera de este analizador.
- [ ] Opción B: se marcan como «sólo ordena», sin contar como error.
- [ ] Opción C: cuentan como error.

**Answer**: Pendiente. Recomiendo A: la pregunta de la propuesta es «¿dos ejercicios del mismo tema?», y entre temas
repetir puede ser a propósito, como en los repasos (*Clasificación: repaso* repite «cold water» de *Contable o
incontable*). Son 33 grupos: si molestan, B los muestra sin tocar los números.

## References

- **FEAT-HALL** — El hallazgo, su gravedad, evidencia, identidad y conteo, el catálogo y el plan: R001 a R005.
- **FEAT-RESPC** — Cómo compara el corrector: R001.
- **FEAT-QSENT** — La pista entre paréntesis ([F-QSENT-R007](#F-QSENT-R007)) y la primera respuesta
  ([F-QSENT-R018](#F-QSENT-R018)): R001.
- **FEAT-OPMUL** — El ejercicio terminado de una opción múltiple ([F-OPMUL-R004](#F-OPMUL-R004)): R001.
