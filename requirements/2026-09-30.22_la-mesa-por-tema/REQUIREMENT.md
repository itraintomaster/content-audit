---
feature:
  id: FEAT-MESA
  code: F-MESA
  name: La mesa por tema - uno propone, content-audit mide, los jueces juzgan y un script decide
  priority: critical
---

# La mesa por tema: uno propone, content-audit mide, los jueces juzgan y un script decide

## TL;DR

**Qué**: Dentro de una tanda, una mesa por tema corrige juntos los ejercicios del tema que necesitan propuesta: revalida,
uno propone, content-audit mide, los jueces del catálogo juzgan el ejercicio como queda y un script decide; varias mesas
corren a la vez.

**Por qué**: Hoy se corrige de a una tarea, con criterios que comparan contra el original y lo dan por bueno, a 17–35
ejercicios por hora; los unos 1.900 ejercicios que necesitan propuesta llevarían semanas y nadie juzgaría lo que queda.

## Recorrido de uso (estado actual)

Cómo quien corrige intenta hoy arreglar un ejercicio que un analizador marcó:

1. Pide una propuesta para una tarea → `ReviseCommand.revise(taskId, planId, …)`. ✓ Disponible, una tarea por vez, con un
   revisor por [tipo de diagnóstico](glossary:Tipo de diagnóstico). ⚠️ No hay revisor para los analizadores nuevos.
2. Corrige varias de una vez → `ReviseInstructionsCommand.reviseInstructions(planId, maxCorrections, coursePath)`. ✓
   Disponible sólo para la consigna, una tarea después de otra, con tope de 20 por corrida.
3. Mide un candidato → `AssessCandidateCommand.assessCandidate(planId, taskId, …)`. ✓ Disponible, pero ⚠️ sus criterios
   comparan contra el original ([F-QICOR-R004](#F-QICOR-R004)): la traducción «fiel» del candidato se mide contra la del
   original, que puede estar mal.
4. Corrige una opción múltiple → ⚠️ se rechaza siempre ([F-OPMUL-R006](#F-OPMUL-R006)).

**Punto de fricción**: pasos 1 a 4. No hay quien tome todos los errores de un ejercicio a la vez, ni jueces que lean el
ejercicio como queda, ni forma de correr varios temas en paralelo.

**Cambio mínimo necesario**: dentro de una tanda ([F-TANDA-R005](#F-TANDA-R005)), una mesa por tema toma las tareas de
mesa del tema, revalida, pide candidatos, los mide con los analizadores del catálogo, los hace juzgar por sus jueces y
decide con reglas fijas. `revise`, `revise-instructions` y `assess-candidate` siguen como hoy fuera de las tandas.

## Reglas de Negocio

Una **mesa** tiene cinco puestos y nadie juzga lo suyo: **propone** un agente, **mide** content-audit, **juzgan** los
jueces del catálogo, **refuta** quien busca un contraejemplo y **decide** un script. **Candidato** es el ejercicio
entero como quedaría: enunciado con su pista, respuestas u opciones, [traducción](glossary:Traducción), miniteoría y
consigna.

<a id="F-MESA-R001"></a>
### Rule[F-MESA-R001] - La mesa toma un tema entero, y un tema tiene una sola mesa
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una mesa toma todas las tareas de resolución *mesa* de un tema que están en la tanda. Invariantes:
> 1. por cada ejercicio con tareas produce candidatos del ejercicio entero, y cada candidato intenta resolver todas las
>    tareas de ese ejercicio a la vez;
> 2. una tarea del tema, como la de la miniteoría, se trabaja una sola vez ([F-TAREA-R003](#F-TAREA-R003));
> 3. un tema nunca tiene dos mesas a la vez, y cada mesa ve el tema con lo que ya aceptó en esa tanda.

Tres tareas de un ejercicio —pista, una sola respuesta y traducción, como el 11 de *Afirmativas regulares*— son un solo
candidato, no tres cambios que se pisan ([F-CDIFF-R012](#F-CDIFF-R012) marcaría no aplicable al segundo).

<a id="F-MESA-R002"></a>
### Rule[F-MESA-R002] - Antes de proponer, la mesa revalida
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Antes de pedir un candidato, la mesa vuelve a medir el ejercicio como está ahora con cada analizador que lee algo que
> sus tareas pueden cambiar, reutilizando los veredictos por la huella del contenido. Si el hallazgo de una tarea ya no
> está, esa tarea termina como «ya no se sostiene», sin candidato y sin gastar un intento; si no queda ninguna, el
> ejercicio no se toca.

Generaliza [F-QICOR-R009](#F-QICOR-R009) a todo analizador. Un ejercicio que nadie cambió reutiliza sus veredictos
([F-EVCOST-R003](#F-EVCOST-R003)) y no cuesta consultas; uno que una regla de la misma tanda ya tocó sí se vuelve a
juzgar. Esa medición del original es también contra la que se compara cada candidato ([F-NOEMP-R001](#F-NOEMP-R001)).

<a id="F-MESA-R003"></a>
### Rule[F-MESA-R003] - Quien propone ve el tema entero como lo ve el alumno
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Quien propone recibe, para cada ejercicio de su mesa: el tema entero como lo ve el alumno, con lo ya aceptado en la
> tanda; los hallazgos del ejercicio con su evidencia; el [nivel CEFR](glossary:Nivel CEFR); lo que cada tarea permite
> tocar ([F-TAREA-R006](#F-TAREA-R006)); el semáforo de palabras del tema ([F-SEMAF-R001](#F-SEMAF-R001)); y, desde el
> segundo intento, los motivos del rechazo anterior. Si el inglés y la traducción no dicen lo mismo, entrega dos
> candidatos: uno que cambia sólo la traducción y otro que cambia el inglés.

Los dos arreglos son la regla que dejó el 077 («si inglés y traducción no coinciden se arman los dos arreglos»): en las
verduras, cambiar sólo la traducción no alcanzó y cambiar el inglés sí. Quien propone nunca recibe veredictos de un
candidato que no sea suyo ni juzga lo que propone.

<a id="F-MESA-R004"></a>
### Rule[F-MESA-R004] - Content-audit mide cada candidato con la misma vara del análisis
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada candidato se mide con todos los analizadores instantáneos del catálogo sobre el ejercicio como queda, con la
> misma medición que `analyze`: el mismo candidato da los mismos hallazgos y puntajes que daría el ejercicio si
> estuviera en el curso.

Mantiene [F-QICOR-R015](#F-QICOR-R015) («los criterios se consultan, no se reimplementan») y extiende la consulta léxica
del análisis ([F-CLEX-R006](#F-CLEX-R006)): largo, palabras marcadas y las reglas del paquete A salen de la misma
medición que el curso.

<a id="F-MESA-R005"></a>
### Rule[F-MESA-R005] - Los jueces juzgan el ejercicio como queda, cada uno solo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo juez del catálogo que lee algo que el candidato cambió lo juzga, con su versión habilitada y en un contexto
> limpio: recibe sólo lo que lee en el curso, nunca el original, las razones de quien propone ni otros veredictos. Su
> veredicto queda por la huella de lo que lee y, si el candidato entra al curso, se reutiliza sin consultar. Un juez que
> no lee nada de lo que cambió no se consulta.

Mantiene [F-JEXAM-R005](#F-JEXAM-R005), [F-JEXAM-R003](#F-JEXAM-R003) (una versión sin examen aprobado no consulta) y lo
que lee cada juez ([F-TRSEN-R001](#F-TRSEN-R001), [F-UNICA-R001](#F-UNICA-R001), [F-MCVALE-R001](#F-MCVALE-R001),
[F-MINIT-R001](#F-MINIT-R001), [F-JEXAM-R007](#F-JEXAM-R007)). Si sólo cambia la traducción, juzgan los dos de
traducción y sentido; si cambia el inglés de un CLOZE, también el de una sola respuesta y el de consigna: cuatro
consultas, las que estimó el paquete C.

<a id="F-MESA-R006"></a>
### Rule[F-MESA-R006] - Decide un script: resuelve, no empeora y le gana al original
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La decisión no la toma un modelo: sale sólo de las mediciones y los veredictos. Un candidato se acepta si:
> 1. resuelve al menos una tarea del ejercicio: el analizador que la marcó ya no la marca;
> 2. no empeora nada del ejercicio ([F-NOEMP-R001](#F-NOEMP-R001) a [F-NOEMP-R004](#F-NOEMP-R004)).
>
> Con 1 y 2, el candidato tiene menos errores que el original por gravedad. Si se aceptan varios, gana el que deja menos
> hallazgos, por gravedad; a igualdad, el que no cambia lo que se escucha; después, el que queda en el rango de largo; y
> si siguen empatados, el primero. Las mismas mediciones dan siempre la misma decisión, con sus motivos.

Es la vara absoluta de la propuesta («acepta si pasa los bloqueantes, no empeora nada y le gana al original»), no la
comparativa de [F-QICOR-R013](#F-QICOR-R013), que sigue valiendo en `revise-instructions`. El orden de desempate es
[ASSUMPTION]: prefiere no regrabar porque cada audio nuevo es trabajo fuera de content-audit.

<a id="F-MESA-R007"></a>
### Rule[F-MESA-R007] - Hasta tres intentos; si nadie gana, queda el original y va a la bandeja
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un candidato rechazado vuelve a quien propone con los criterios que no pasó y su evidencia. Cada ejercicio tiene hasta
> tres intentos; agotados sin un candidato aceptado, el ejercicio queda como estaba, sus tareas siguen abiertas y la
> bandeja recibe el original, el candidato que menos falló con lo que no pasó y los motivos de cada intento.

Mantiene el tope de [DOUBT-REINTENTOS](#DOUBT-REINTENTOS) de FEAT-QICOR (3) y el espíritu de
[DOUBT-SIN-CORRECCION](#DOUBT-SIN-CORRECCION): lo mejor que se logró no se tira, se muestra con lo que falla y decide
José ([F-BANDEJA-R003](#F-BANDEJA-R003)). A diferencia de `revise-instructions`, la mesa no lo acepta sola.

<a id="F-MESA-R008"></a>
### Rule[F-MESA-R008] - Si los dos jueces de un criterio no coinciden, decide José
**Severity**: major | **Validation**: AUTO_VALIDATED

> Si los dos jueces de traducción y sentido no coinciden sobre un candidato, ese candidato no se acepta ni se rechaza:
> va a la bandeja como desempate con los dos veredictos, y el ejercicio espera la respuesta de José sin gastar más
> intentos.

Es la decisión de José del 30/9 para el análisis ([F-TRSEN-R003](#F-TRSEN-R003)), aplicada igual al candidato: el
desacuerdo no se resuelve a favor de nadie.

<a id="F-MESA-R009"></a>
### Rule[F-MESA-R009] - La mesa no escribe el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo candidato aceptado queda como una [propuesta de revisión](glossary:Propuesta de revisión) pendiente de la tanda,
> con su artefacto; mientras la tanda no se publica, los archivos del curso quedan idénticos byte a byte.

Mantiene [F-REVBYP-R008](#F-REVBYP-R008), [F-REVBYP-R014](#F-REVBYP-R014) y [F-REVAPR-R007](#F-REVAPR-R007): el artefacto
guarda el antes y el después, y el curso cambia sólo al publicar ([F-TANDA-R009](#F-TANDA-R009)).

<a id="F-MESA-R010"></a>
### Rule[F-MESA-R010] - Varias mesas a la vez, sin pasarse de las consultas pedidas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una tanda corre hasta N mesas a la vez y, entre todas, hasta C consultas a modelos en curso. Invariantes:
> 1. nunca hay más de N temas abiertos ni más de C consultas en curso;
> 2. tras una falla del servicio no empieza ninguna consulta más; las que estaban en curso se registran si el modelo
>    respondió, y los ejercicios sin decisión quedan «sin intentar», sin gastar intentos;
> 3. con los mismos candidatos y veredictos, las decisiones son las mismas con cualquier N y C.

Mantiene [F-JEXAM-R006](#F-JEXAM-R006) para cada juez. N y C los elige José ([DOUBT-TEMAS-A-LA-VEZ](#DOUBT-TEMAS-A-LA-VEZ)).

<a id="F-MESA-R011"></a>
### Rule[F-MESA-R011] - Una opción múltiple sale de la mesa como opción múltiple, con una sola correcta
**Severity**: critical | **Validation**: ASSUMPTION

> Un candidato de una opción múltiple sigue siendo de opción múltiple, con exactamente una opción correcta y la misma
> cantidad de opciones; cambia la oración o la opción que sobra, y cada opción nueva, puesta en el hueco, no suma
> palabras rojas. Lo juzgan las reglas de `multiple-choice-validity` y su juez de «otra opción también vale».

Es «siempre una sola correcta, y la mesa rearma» (José, 30/9; [F-OMVAL-R001](#F-OMVAL-R001), [F-MCVALE-R007](#F-MCVALE-R007)).
Cambia [F-OPMUL-R006](#F-OPMUL-R006) dentro de las tandas: [DOUBT-MC-REARMA](#DOUBT-MC-REARMA).

<a id="F-MESA-R012"></a>
### Rule[F-MESA-R012] - La miniteoría se reescribe una vez, en los ejercicios que la muestran
**Severity**: major | **Validation**: ASSUMPTION

> La tarea de una miniteoría produce un solo candidato: el texto nuevo, que llega a todos los ejercicios del tema que
> muestran ese texto, y a ningún otro. Lo juzga el juez de miniteoría sobre el tema como quedaría.

Mantiene [F-MINIT-R007](#F-MINIT-R007). Es una segunda excepción a [F-RPRES-R003](#F-RPRES-R003), después de la del título
([F-RPRES-R004](#F-RPRES-R004)): [DOUBT-MINITEORIA-ALCANCE](#DOUBT-MINITEORIA-ALCANCE).

<a id="F-MESA-R013"></a>
### Rule[F-MESA-R013] - Refutan los jueces que ya buscan un contraejemplo
**Severity**: major | **Validation**: ASSUMPTION

> Refutar un candidato es buscarle un contraejemplo, y lo hacen los controles que ya lo buscan: otra respuesta que
> también vale (el juez de una sola respuesta, o el de «otra opción también vale»), una palabra de otro nivel (la
> no-regresión léxica, [F-NOEMP-R002](#F-NOEMP-R002)) y una forma que un nativo no usaría (la condición 2 de traducción y
> sentido). Un contraejemplo que el original no tenía rechaza el candidato ([F-NOEMP-R001](#F-NOEMP-R001)).

La propuesta dibuja un quinto puesto, «el abogado del diablo». Ponerlo como agente aparte suma una consulta por candidato:
[DOUBT-QUIEN-REFUTA](#DOUBT-QUIEN-REFUTA).

<a id="F-MESA-R014"></a>
### Rule[F-MESA-R014] - Fuera de una tanda, la corrección de hoy no cambia
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Sobre una tarea que no está en una tanda abierta, `revise`, `revise-instructions` y `assess-candidate` dan lo mismo que
> antes de este requirement: los mismos criterios, el mismo lazo de intentos y los mismos desenlaces.

Mantiene FEAT-QICOR entero, FEAT-LASAG ([F-LASAG-R007](#F-LASAG-R007)) y FEAT-KTLR. Las dos varas de «traducción fiel»
—la comparativa de la consigna y la absoluta de la mesa— conviven, como pidió el paquete C
([DOUBT-TRSEN-CATALOGO](#DOUBT-TRSEN-CATALOGO), opción A).

## Contexto

**Por qué una mesa por tema.** El 077 revisó los 11.287 ejercicios con revisores por lote y verificadores que intentaron
refutar sus propuestas (2.021 confirmadas, 83 ajustadas y 10 rechazadas). Lo que funcionó fue mirar el tema entero, juzgar el ejercicio
como queda y no dejar que nadie juzgue lo suyo. content-audit hoy hace lo contrario: una tarea por vez, criterios que
comparan con el original y un solo agente que se mide a sí mismo ([F-QICOR-R012](#F-QICOR-R012)).

**Cuánto trabajo es.** Con los casos de `escribir/` (A1 y A2), 278 ejercicios de A1 y 451 de A2 necesitan la mesa, en
63 y 74 temas; el más cargado de A1 tiene 40 y la mediana es 2. Con la escala de errores de la propuesta, B1 y B2 suman
unos 790 y 355: cerca de 1.900 en el curso. Cada uno cuesta, en promedio, 1,5 intentos de 1 consulta de quien propone
más 4 de los jueces: unas 7,8 consultas, unas 14.700 en el curso entero, contra las 41.700 que cuesta juzgarlo una vez.

**Cuánto tarda** (prototipo en el scratchpad, al ritmo medido de 19 s por consulta):

| Temas a la vez | Consultas a la vez | A1 | A2 | B1 y B2 (estimado) | Curso |
|---:|---:|---:|---:|---:|---:|
| 1 | 4 | 5,6 h | 9,1 h | 23 h | unas 38 h |
| 4 | 4 | 2,9 h | 4,7 h | 12 h | unas 20 h |
| 8 | 8 | 1,5 h | 2,4 h | 6 h | unas 10 h |

Más temas que consultas no acelera: el límite es la cuota del proveedor. Y ninguna tanda baja del tema más cargado
(en A1, 40 ejercicios: cerca de una hora).

## Alcance

- **Adentro**: la mesa de un tema, sus cinco puestos, la decisión, los intentos, el paralelismo, la opción múltiple y la
  miniteoría dentro de la mesa.
- **Afuera**: qué juzga cada juez y su examen (paquete C); qué marca cada regla (paquete A); el semáforo (paquete D);
  lo que se aplica con una regla, sin mesa ([F-TANDA-R004](#F-TANDA-R004)); qué es «no empeorar» (FEAT-NOEMP); la bandeja
  (FEAT-BANDEJA); el proveedor del agente que propone, que se decide con el de los jueces
  ([DOUBT-JUECES-PROVEEDOR](#DOUBT-JUECES-PROVEEDOR)).

## User Journeys

### Journey[F-MESA-J001] - Corregir un ejercicio en la mesa de su tema
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-MESA-J001
    name: Corregir un ejercicio en la mesa de su tema
    flow:
      - id: tomar_tema
        action: "La tanda abre la mesa de un tema con tareas de mesa"
        gate: [F-MESA-R001, F-MESA-R010]
        then: revalidar
      - id: revalidar
        action: "La mesa vuelve a medir el ejercicio como está ahora"
        gate: [F-MESA-R002]
        outcomes:
          - when: "Ninguno de sus hallazgos sigue"
            then: ya_no_se_sostiene
          - when: "Sigue al menos uno"
            then: proponer
      - id: proponer
        action: "Quien propone entrega candidatos del ejercicio entero y content-audit los mide"
        gate: [F-MESA-R003, F-MESA-R004]
        then: juzgar
      - id: juzgar
        action: "Los jueces que leen lo que cambió juzgan cada candidato como queda, y el script decide"
        gate: [F-MESA-R005, F-MESA-R006, F-MESA-R013]
        outcomes:
          - when: "Un candidato resuelve al menos una tarea y no empeora nada"
            then: aceptado
          - when: "Los dos jueces de traducción y sentido no coinciden"
            then: desempate
          - when: "Ningún candidato pasa después del tercer intento"
            then: sin_ganador
      - id: aceptado
        action: "El candidato queda como propuesta pendiente de la tanda y el curso no cambia"
        gate: [F-MESA-R009]
        result: success
      - id: ya_no_se_sostiene
        action: "Sus tareas terminan como ya no se sostiene, sin candidato ni intento gastado"
        gate: [F-MESA-R002]
        result: success
      - id: desempate
        action: "La bandeja recibe los dos veredictos y el ejercicio espera a José"
        gate: [F-MESA-R008]
        result: failure
      - id: sin_ganador
        action: "El ejercicio queda como estaba y la bandeja recibe el original, el candidato que menos falló y los motivos"
        gate: [F-MESA-R007]
        result: failure
```

## Open Questions

<a id="DOUBT-TEMAS-A-LA-VEZ"></a>
### Doubt[DOUBT-TEMAS-A-LA-VEZ] - ¿Cuántos temas van a la vez, y con cuántas consultas?
**Status**: OPEN

- [ ] Opción A: un tema y 4 consultas a la vez. A1 en 5,6 h, A2 en 9,1 h, el curso en unas 38 h.
- [ ] Opción B (recomendada): 4 temas y 4 consultas a la vez, las mismas 4 que José ya eligió para los jueces. A1 en
  2,9 h, A2 en 4,7 h, el curso en unas 20 h.
- [ ] Opción C: 8 temas y 8 consultas. A1 en 1,5 h, A2 en 2,4 h, el curso en unas 10 h, con más riesgo de cortes por
  cuota.

**Answer**: Pendiente. Recomiendo B: es la cuota que ya se decidió para los jueces, y con 4 temas no queda capacidad
ociosa. Pasar a C tiene sentido sólo si la tanda A1 corre sin cortes; bajar a 2 consultas si la cuota corta
([DOUBT-JUECES-PARALELO](#DOUBT-JUECES-PARALELO)). Las horas no incluyen juzgar el nivel antes de la tanda (A1: unas
7.300 consultas de los jueces), que es el gasto mayor.

<a id="DOUBT-MC-REARMA"></a>
### Doubt[DOUBT-MC-REARMA] - ¿Se corrigen las opciones múltiples en las tandas?
**Status**: OPEN

Contesta, para las tandas, [DOUBT-CORREGIR-MC](#DOUBT-CORREGIR-MC) de FEAT-OPMUL, que sigue abierta: hoy ninguna
corrección toca una opción múltiple ([F-OPMUL-R006](#F-OPMUL-R006)).

- [ ] Opción A (recomendada): sí. Las reglas de forma (en `escribir/`, 170 de los 186 casos de A1 y A2 son mayúsculas)
  y la mesa cuando hay dos correctas, como dijo José («la mesa rearma»): sigue siendo opción múltiple, con una sola
  correcta y la misma cantidad de opciones ([F-MESA-R011](#F-MESA-R011)).
- [ ] Opción B: sólo las reglas de forma; las dos correctas esperan.
- [ ] Opción C: ninguna; los 315 ejercicios de opción múltiple con error siguen contados, sin arreglo.

**Answer**: Pendiente. Recomiendo A: es lo que José ya eligió en el paquete A (C de DOUBT-CORREGIR-MC: corregir el
ejercicio entero, con el juez vetando distractores que también valen). F-OPMUL-R006 sigue valiendo fuera de las tandas.

<a id="DOUBT-MINITEORIA-ALCANCE"></a>
### Doubt[DOUBT-MINITEORIA-ALCANCE] - ¿La reescritura de una miniteoría puede tocar varios ejercicios a la vez?
**Status**: OPEN

- [ ] Opción A (recomendada): sí, a todos los del tema que muestran ese texto y a ninguno más: una segunda excepción a
  [F-RPRES-R003](#F-RPRES-R003), como la del título.
- [ ] Opción B: no; una propuesta por ejercicio, con el mismo texto repetido.
- [ ] Opción C: las miniteorías quedan fuera de las tandas.

**Answer**: Pendiente. Recomiendo A: el error es uno y se arregla una vez (335 ejercicios del 077, en pocos temas); con B,
José aprobaría 40 veces lo mismo en *In time u on time*, y un texto podría quedar a medias.

<a id="DOUBT-QUIEN-REFUTA"></a>
### Doubt[DOUBT-QUIEN-REFUTA] - ¿El que refuta es un agente aparte?
**Status**: OPEN

- [ ] Opción A (recomendada): no: refutan los controles que ya buscan contraejemplos ([F-MESA-R013](#F-MESA-R013)), y la
  muestra ciega dice si hace falta más.
- [ ] Opción B: sí, un agente por candidato: una consulta más por candidato, un 20 % más (A1: unas 2.600 consultas en vez
  de 2.200, y 0,6 h más con 4 a la vez).

**Answer**: Pendiente. Recomiendo A: las tres cosas que busca el abogado del diablo ya tienen quien las busque, y dos de
ellas sin modelo. Si la muestra ciega de la tanda A1 no cumple su meta ([F-CIEGA-R004](#F-CIEGA-R004)), se suma B.

## References

- **FEAT-QICOR, FEAT-LASAG, FEAT-KTLR** — La corrección de hoy, su revalidación, su lazo de intentos y sus criterios, que
  siguen valiendo fuera de las tandas: R002, R006, R007, R014.
- **FEAT-JEXAM, FEAT-TRSEN, FEAT-UNICA, FEAT-MCVALE, FEAT-MINIT** (ronda 1, paquete C) — Qué lee cada juez, cómo juzga un
  candidato y cuántas consultas van a la vez: R005, R008, R010, R011, R012, R013.
- **FEAT-OMVAL, FEAT-OPMUL** — Una sola correcta, y la opción múltiple que hoy no se corrige: R011.
- **FEAT-SEMAF, FEAT-CLEX** — El semáforo del tema y la consulta léxica: R003, R004.
- **FEAT-REVBYP, FEAT-REVAPR, FEAT-RPRES, FEAT-CDIFF, FEAT-EVCOST** — Propuestas pendientes, preservación y reuso de
  veredictos: R001, R002, R009, R012.
- **FEAT-TAREA, FEAT-NOEMP, FEAT-TANDA, FEAT-BANDEJA** (este paquete) — Las tareas que toma la mesa, la vara con que
  decide, la tanda que la corre y la bandeja que recibe lo que no decide.
