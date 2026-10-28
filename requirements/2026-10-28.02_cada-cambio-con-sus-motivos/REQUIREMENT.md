---
feature:
  id: FEAT-MOTIVO
  code: F-MOTIVO
  name: Cada cambio con sus motivos - una corrección nueva se lleva las tareas de lo que hereda
  priority: critical
---

# Cada cambio con sus motivos

## TL;DR

**Qué**: Cuando se crea la corrección siguiente, cada cambio heredado se lleva las tareas que lo motivaron, con un
número del plan nuevo. Una tarea de otro ejercicio nunca se muestra ni cuenta como motivo de un cambio. Una corrección
creada antes de esto se repara una vez, sin consultar jueces y sin tocar su contenido.

**Por qué**: El 28/10/2026, José vio en «I ___ (not / be) thirsty right now» lo que Una sola respuesta había dicho de
«I would like to have a bowl of soup», con el título «Cortesía con would like». La C2 heredó de la C1 2.243 cambios
con sus números de tarea. Pero la C2 tiene un plan propio, que vuelve a numerar desde task-001. En ese plan, la tarea
2064 de «thirsty» es la de la sopa. 2.241 cambios heredados apuntaban a tareas de otros ejercicios. En 1.537 de los
2.155 que cuenta «Según los jueces», el tablero creía que había pedido el cambio otro juez, y eso torcía «Lo dan por
bueno» y «No consta arreglado» (brief 237). José, el 28/10/2026: «Sí: arreglá las tareas heredadas para que no vuelva
a pasar, repará la C2, hacé que cada aviso diga qué pasó con el ejercicio, con sus pruebas, y juntalo».

## Recorrido de uso (estado actual)

1. José cierra una corrección y crea la siguiente ✓.
2. La nueva hereda los cambios y arma su propio plan ✓.
3. En Correcciones, «Qué dijo cada uno» de un cambio heredado ⚠️ muestra el motivo de otro ejercicio.
4. «Lo dan por bueno» y «No consta arreglado» ⚠️ se calculan con el juez de ese otro motivo.

**Punto de fricción**: pasos 3 y 4. Lo que José revisa no es lo que pasó con el ejercicio.

**Cambio mínimo necesario**: que el cambio heredado se lleve sus tareas al plan nuevo; que nadie use una tarea de otro
ejercicio; y reparar la C2.

## Reglas de Negocio

<a id="F-MOTIVO-R001"></a>
### Rule[F-MOTIVO-R001] - La corrección nueva se lleva los motivos de lo que hereda
**Severity**: critical | **Validation**: VALIDATED

> Al crear la corrección siguiente, cada tarea de un cambio heredado se copia al final del plan nuevo, con un número
> que ese plan no usa, y el cambio pasa a nombrar la copia. La copia dice lo mismo que la original: el ejercicio, el
> hallazgo que la motivó y lo que cubre. Queda «en la tanda» porque es de un cambio de la tanda, pero no es trabajo
> nuevo: no entra en las tareas elegidas de la corrección. La revisión de la corrección, que es lo que leen el tablero y
> el aviso de aprobación, trae las tareas elegidas y también las que nombran sus cambios vigentes. La corrección anterior
> conserva su numeración, y las tareas
> propias del plan nuevo quedan como estaban. Si la anterior heredó sin llevarse sus tareas, el motivo se busca en las
> correcciones de antes, la más cercana primero.

<a id="F-MOTIVO-R002"></a>
### Rule[F-MOTIVO-R002] - Un motivo de otro ejercicio no se usa nunca
**Severity**: critical | **Validation**: VALIDATED

> Una tarea es motivo de un cambio sólo si habla de uno de sus ejercicios, de su tema o llega a uno de sus ejercicios.
> Si no, no se usa en nada de lo que se calcula del cambio:
> - en el tablero, ni en «Qué dijo cada uno», ni en el juez que pidió el cambio, ni en «Lo dan por bueno» y «No consta
>   arreglado»;
> - en content-audit, ni en el aviso de que el juez que motivó un cambio aprobado dejó de marcarlo.
>
> Si un cambio se queda sin motivos así, se ve sin motivos. Nunca con los de otro ejercicio. Una tarea que no dice de
> qué ejercicio es se toma como viene.

<a id="F-MOTIVO-R003"></a>
### Rule[F-MOTIVO-R003] - Una corrección creada antes se repara una vez
**Severity**: major | **Validation**: VALIDATED

> Mirar las tareas heredadas de una corrección dice cuántos cambios heredados nombran tareas con el número de la
> anterior, cuántas copias haría falta hacer, cuántos números no se pueden resolver y cuántos desenlaces se mudan. No
> guarda nada. Reparar hace lo mismo que R001 con esos cambios y guarda el plan y la corrección. No consulta jueces, no
> cambia el contenido ni su versión. Pide la versión de la corrección que se miró, y no repara si cambió o si hay una
> operación corriendo sobre ella. Un cambio nacido en la corrección y un número que su propio plan ya resuelve a ese
> ejercicio quedan como estaban. Sacar un cambio heredado había escrito su desenlace, «volvió a pendiente», bajo el
> número viejo, o sea sobre la tarea de otro ejercicio. Ese desenlace pasa a la copia, y la otra tarea lo pierde.
> Reparar dos veces es reparar una.

## Contexto

- **Los números de tarea son del plan**: el refinador numera las tareas por prioridad, y un plan nuevo vuelve a empezar
  ([reference: task-ids se reusan entre planes]). La corrección siguiente arma un plan propio a partir del análisis de
  lo corregido, así que los hallazgos que motivaron los cambios heredados casi nunca siguen ahí: sólo están en el plan
  de la anterior.
- **Por qué copiar y no nombrar el plan**: todo lo que lee tareas —el tablero, la revisión de la tanda, el aviso de
  aprobación, el cierre, la publicación— las busca en el plan de la corrección. Con la copia, siguen haciéndolo.
- **La C2 de A1**: 2.241 de 2.243 cambios heredados, 3.051 números; 103 desenlaces escritos al sacar 36 cambios
  heredados el 25/10, 6 de ellos sobre tareas propias de la C2.
- **Fuera de alcance**: las opiniones de los jueces no cambian; cambia a qué motivo se comparan.

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `CorrectionEvolutionServiceTest`: crear la siguiente copia la tarea del cambio heredado con el número que sigue, con su ejercicio y su hallazgo, en la tanda y fuera de las elegidas; la tarea propia con el mismo número y la anterior no cambian; no queda nada por reparar. `DefaultBatchReviewTest`: la revisión trae la tarea que nombra un cambio aunque no sea elegida, y no la de un cambio sacado |
| R002 | `CurrentAnalysisServiceTest`: la tarea de otro ejercicio no avisa una aprobación, la propia sí. En el tablero, `test/views/hallazgos_vinculados_test.dart`: los motivos de un cambio son los de sus ejercicios, y una tarea de otro ejercicio no entra ni en el veredicto ni en el «Dijo» |
| R003 | `CorrectionEvolutionServiceTest`: mirar cuenta sin guardar; con otra versión no repara; reparar copia la tarea, muda el desenlace a la copia, deja la otra tarea sin él y no cambia la versión; repararla otra vez no hace nada. Con una operación corriendo, no repara |

## References

- Brief 237, la C2 busca las tareas de la C1 en su propio plan (28/10/2026).
- [F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003): qué es la opinión de hoy.
