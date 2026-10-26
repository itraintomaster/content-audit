---
feature:
  id: FEAT-REGALA
  code: F-REGALA
  name: La consigna no regala - un juez que mira si lo que el alumno lee fuera de la oración le hace la decisión
  priority: major
---

# La consigna no regala la respuesta

## TL;DR

**Qué**: Un juez nuevo, «No regala la respuesta», lee cada ejercicio como lo ve el alumno antes de contestar y dice si
la consigna, o una indicación escrita dentro del enunciado, le hace la decisión que el tema le pide practicar. Lo que
regala es una tarea de la mesa, marcada por el fragmento exacto; y toda propuesta de la mesa que toque la consigna o el
enunciado, por la tarea que sea, pasa por este juez, así que una corrección no puede meter un regalo nuevo.

**Por qué**: En la prueba ancha de la C2 de A1 (26/10/2026) cinco correcciones aprobadas regalaban la respuesta, y
ningún juez lo vio:
- 26ef: la consigna explicaba el contexto y casi daba la respuesta («acabas de decidir… no tenías un plan previo»);
- 21c9, 21bc y 21bf: «colocando el verbo delante del sujeto» regalaba la mitad de lo que el tema practica;
- 2535: un paréntesis en castellano dentro del enunciado decía qué palabra cambiar.

El único juez que miraba algo parecido era el de opción múltiple, y sólo para opciones que se delatan entre sí. La
causa estaba en la mesa: para dejar una sola respuesta, el que propone aclaraba en la consigna lo que tendría que
estar en la oración. Decisión de José, el 26/10/2026 (brief 226): «Sí: hacé el largo leve y el juez «no regala» antes
del tramo grave».

## Recorrido de uso (estado actual)

1. La mesa corrige un ejercicio con una sola respuesta pendiente ✓.
2. El que propone aclara en la consigna qué caso de la regla aplica ⚠️: la respuesta queda única, pero regalada.
3. Los jueces de respuesta única y de consigna lo aprueban ✓; ninguno pregunta si regala ⚠️.
4. José lo ve recién al revisar la prueba ancha ⚠️.

**Punto de fricción**: pasos 2 a 4. Nada mide si la consigna hace la decisión por el alumno.

**Cambio mínimo necesario**: un juez de eso, para todo tipo de ejercicio, cuyos hallazgos corrija la mesa y que mida
toda propuesta que toque lo que el alumno lee antes de contestar.

## Reglas de Negocio

La **decisión que se practica** es lo que el tema le pide decidir al alumno: qué forma elegir, qué caso de la regla
aplica o qué operación hacer. **Regalar** es que algo que el alumno lee fuera de la oración misma le haga esa
decisión, entera o un paso de ella.

<a id="F-REGALA-R001"></a>
### Rule[F-REGALA-R001] - Lee lo que el alumno ve antes de contestar, en todo tipo de ejercicio
**Severity**: major | **Validation**: VALIDATED

> Por cada ejercicio, de completar o de opción múltiple y de cualquier nivel, el juez lee el nombre del tema, la
> consigna del ejercicio, el enunciado con su pista y las opciones que ve el alumno, con la unidad del curso como
> contexto; y las respuestas aceptadas —en una opción múltiple, cuál es la correcta—, que el alumno no ve, para saber
> cuál es la respuesta. No lee la traducción, la miniteoría ni el audio, que la app muestra recién al corregir. La huella
> es exactamente eso: cambiar la traducción no pide otra opinión; cambiar la consigna, sí.

<a id="F-REGALA-R002"></a>
### Rule[F-REGALA-R002] - Regala toda la decisión o una parte; nombrar la tarea no regala
**Severity**: major | **Validation**: VALIDATED

> El juez escribe primero qué decide el alumno según el tema y después dice si algo de lo que lee fuera de la oración,
> sin saber el tema, le hace acertar:
> 1. **regala toda la decisión**: dice la respuesta o la forma, o qué caso de la regla aplica cuando distinguir los
>    casos es lo que se practica;
> 2. **regala una parte**: da uno de los pasos, y el alumno todavía resuelve otro.
>
> No regalan: nombrar la tarea («Escribe la respuesta corta»), recordar la regla entera con todos sus casos, repetir el
> nombre del tema, pedir un formato que no es lo que se practica, el contexto dentro de la oración, una pista corta al
> lado del hueco ni las opciones; las dos últimas son de otros jueces y viajan como traspaso. Dice dónde regala —la
> consigna o el enunciado— y copia el fragmento tal cual; un veredicto cuyo fragmento no está así en ese campo, o que es
> de otro ejercicio, no se usa y el ejercicio queda sin medir. El puntaje es 1 si no regala y 0 si regala; lo que pide
> revisión queda pendiente y lo que no pide ninguna decisión no puntúa. Lo que no se juzgó nunca cuenta como aprobado.

<a id="F-REGALA-R003"></a>
### Rule[F-REGALA-R003] - Lo que regala lo corrige la mesa
**Severity**: major | **Validation**: VALIDATED

> Cada ejercicio que regala abre una tarea «no regala la respuesta» en el ejercicio, marcada por el fragmento que
> regala: alta si regala toda la decisión, media si regala una parte. La mesa la corrige sacando de la consigna o del
> enunciado lo que decide por el alumno y, si hace falta contexto para que haya una sola respuesta, poniéndolo en la
> oración: puede tocar la consigna, el enunciado, la pista, las respuestas y las opciones, con la traducción, la oración
> guardada y el audio detrás; nunca la miniteoría ni la consigna del tema. Como el juez nunca midió el curso, en los
> temas que se corrigen mide antes cada ejercicio elegido que no vio ([F-MESA-R024](../2026-09-30.22_la-mesa-por-tema/REQUIREMENT.md)).

<a id="F-REGALA-R004"></a>
### Rule[F-REGALA-R004] - Una corrección no puede meter un regalo
**Severity**: major | **Validation**: VALIDATED

> Toda propuesta de la mesa que cambie la consigna, el enunciado, la pista, las respuestas o las opciones la mide este
> juez, sea por la tarea que sea; una que sólo cambia la traducción o la miniteoría, no. Un regalo que la propuesta no
> tenía antes es un hallazgo nuevo y la mesa no la acepta ([F-NOEMP-R001](../2026-09-30.23_no-empeorar/REQUIREMENT.md)):
> arreglar una respuesta única regalando la decisión no es un arreglo.

## Contexto

- **El que propone en la mesa** recibe la guía en positivo: la consigna dice qué hacer, nunca la respuesta ni qué caso
  aplica; el contexto que hace falta para una sola respuesta va en la oración. La regla 3 del que propone, que pedía
  aclarar en la consigna para descartar «He is ready?», ahora pide elegir incorrectas que estén mal en cualquier
  lectura. Nombrar lo que no se quiere lo provoca; por eso la guía se escribe en positivo.
- **Antes de consultar en el curso**, el juez necesita su examen aprobado
  ([F-JEXAM-R003](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md)): sin eso no gasta consultas. El examen sale de los
  cinco casos de la prueba ancha que regalaban, con su versión de producción y otros ejercicios como los que no regalan.
- **Mis veredictos de la prueba ancha** son una referencia para comparar, no una verdad certificada.

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `AnswerGiveawayAnalyzerTest`: lee lo que ve el alumno y no lo de después; alcanza la opción múltiple con sus opciones |
| R002 | `AnswerGiveawayAnalyzerTest`: toda la decisión, una parte, nombrar la tarea, fragmento ajeno, revisión y sin juzgar; `JacksonAnswerGiveawayVerdictReaderTest` |
| R003 | `AnswerGiveawayAnalyzerTest`: hallazgo alto o medio para la mesa con su fragmento; tarea ANSWER_GIVEAWAY y lo que puede tocar |
| R004 | `AnswerGiveawayReadsTest`: qué cambios mide; `DefaultChangeGateTest`: un regalo nuevo no pasa |

## References

- Brief 226, «Un revisor que marca y dos contrapesos» (26/10/2026).
- Brief 225, la prueba ancha de la C2 de A1, con «¿Lo vio un juez?».
- `analysis/c2-mejoras-2026-10-25/veredictos-ancha.json`: 26ef, 21c9, 21bc, 21bf y 2535.
