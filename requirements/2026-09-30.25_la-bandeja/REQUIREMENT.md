---
feature:
  id: FEAT-BANDEJA
  code: F-BANDEJA
  name: La bandeja - lo único que la tanda le deja a José, y un solo OK para publicar
  priority: critical
---

# La bandeja: lo único que la tanda le deja a José, y un solo OK para publicar

## TL;DR

**Qué**: Una sola bandeja por tanda junta lo que la tanda no puede decidir sola —desempates de los jueces, ejercicios sin
ganador y dos muestras para mirar— y José publica la tanda con un solo OK.

**Por qué**: Hoy se aprueba de a una propuesta, en tres lugares que usan planes distintos: unos 2.900 cambios serían
2.900 aprobaciones, y el criterio de José se gastaría en lo que una regla ya resolvió.

## Recorrido de uso (estado actual)

Cómo José decide hoy qué cambios entran al curso:

1. Lista lo pendiente → `GetCommand.get("proposals", …)`. ✓ Disponible, con filtros ([F-REVAPR-R003](#F-REVAPR-R003)).
2. Decide cada uno → `ApproveCommand.approve("proposal", id, planId, note)` y `RejectCommand.reject(…)`. ✓ Disponible,
   de a una propuesta, y aprobar escribe el curso en ese momento.
3. Mira el impacto de cada una → la vista previa de impacto ([F-PIPRE-R007](#F-PIPRE-R007)). ✓ Disponible.
4. Ve aparte lo que necesita su criterio —dos jueces que no coinciden, un ejercicio que nadie supo arreglar— y mira una
   muestra en lugar de todo → ⚠️ no existe: todo llega igual, de a uno.

**Punto de fricción**: paso 4. Lo que necesita criterio llega mezclado con lo que no, y no hay forma de aprobar un
conjunto de cambios después de mirar una muestra.

**Cambio mínimo necesario**: una bandeja por tanda con cuatro clases de cosas —desempate, sin ganador, muestra de cada
regla y muestra ciega—, una decisión por cosa, y publicar la tanda entera con un solo OK
([F-TANDA-R009](#F-TANDA-R009)). `approve` y `reject` siguen como hoy para lo que no es de una tanda.

## Reglas de Negocio

<a id="F-BANDEJA-R001"></a>
### Rule[F-BANDEJA-R001] - Una sola bandeja por tanda, con todo lo que necesita a José
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La bandeja de la tanda abierta lista todo lo que la tanda necesita de José, y nada más. Cada cosa dice su clase
> —muestra ciega, desempate, sin ganador, muestra de una regla o audio para regrabar—, muestra el ejercicio como lo ve el
> alumno y dice qué se le pregunta y con qué respuestas; un audio para regrabar no pregunta nada
> ([F-BANDEJA-R009](#F-BANDEJA-R009)). El orden es:
> 1. primero, la muestra ciega; mientras no se cierra, la bandeja no muestra nada más de la tanda;
> 2. después, las que piden una decisión —desempates y sin ganador—, por gravedad;
> 3. después, las muestras de cada regla;
> 4. al final, los audios para regrabar.

La bandeja es de content-audit: el tablero (paquete G) la dibuja, no la arma ([F-HALL-R010](#F-HALL-R010), regla 5 del
brief 089). La ciega va primero para que siga siendo ciega: si José ya vio un cambio en otra cosa de la bandeja, sabe
cuál es el nuevo. Es la opción A de [DOUBT-QUIEN-CIEGA](#DOUBT-QUIEN-CIEGA) del paquete F, que José tomó el 30/9: la
responde José antes de abrir la tanda en la bandeja.

<a id="F-BANDEJA-R002"></a>
### Rule[F-BANDEJA-R002] - Un desempate muestra los dos veredictos y lo resuelve José
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un desempate aparece cuando los dos jueces de [traducción](glossary:Traducción) y sentido no coinciden, y muestra los dos veredictos con su
> evidencia. Sobre un ejercicio del curso, José contesta «es error» —su tarea entra a la mesa de la tanda— o «no es
> error». Sobre un candidato de la mesa, contesta «pasa» —el cambio queda aceptado en la tanda— o «no pasa» —el
> ejercicio vuelve a la mesa con un intento menos, o queda sin ganador si no le quedan.

Es la decisión de José del 30/9 ([F-TRSEN-R003](#F-TRSEN-R003), [F-MESA-R008](#F-MESA-R008)): el desacuerdo no infla los
errores ni se resuelve solo.

<a id="F-BANDEJA-R003"></a>
### Rule[F-BANDEJA-R003] - Un ejercicio sin ganador muestra lo que se intentó y lo decide José
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un ejercicio sin ganador muestra el original, el candidato que menos falló con cada control que no pasó, y los motivos
> de cada intento. José elige una de tres:
> 1. «dejar el original»: el ejercicio no cambia y sus tareas quedan abiertas;
> 2. «aprobar el candidato»: entra a la tanda como aprobado por José, y lo que no pasó queda declarado como excepción
>    suya, que la compuerta de la tanda no vuelve a contar ([F-NOEMP-R005](#F-NOEMP-R005));
> 3. «escribir el arreglo»: su versión se mide y se juzga como cualquier candidato ([F-MESA-R004](#F-MESA-R004),
>    [F-MESA-R005](#F-MESA-R005)) y, si no pasa, vuelve a él con los motivos.

El archivo de «escribir el arreglo» lleva, en lineas no vacias, la oracion en la notacion del curso, su traduccion y,
opcionalmente, la miniteoria local de ese quiz. Esta tercera linea viaja en el mismo candidato porque una respuesta nueva
puede volver falsa la explicacion anterior; no se propaga a otros quizzes.

Mantiene [DOUBT-SIN-CORRECCION](#DOUBT-SIN-CORRECCION) de FEAT-QICOR (José eligió ver el candidato con lo que no cumple
y decidir él) y el tope de tres intentos de la mesa ([F-MESA-R007](#F-MESA-R007)).

<a id="F-BANDEJA-R004"></a>
### Rule[F-BANDEJA-R004] - Cada regla se aprueba mirando una muestra
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Por cada regla que la tanda aplicó, la bandeja muestra 20 de sus cambios elegidos al azar —todos, si son menos—, con el
> antes y el después. José contesta «bien», saca un cambio, o saca la regla entera de la tanda
> ([F-TANDA-R008](#F-TANDA-R008)). Los cambios de una regla sin su muestra aprobada no se publican.

«Vos mirás una muestra de 20 antes de dar el OK, en lugar de aprobar uno por uno» (propuesta v2). En A1 son unas 4
reglas: mayúsculas de la opción múltiple, formas que faltan, la pista y los datos.

<a id="F-BANDEJA-R005"></a>
### Rule[F-BANDEJA-R005] - La muestra ciega del paquete F se responde desde la bandeja
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La muestra ciega de la tanda —la que sortea, muestra y cuenta el paquete F ([F-CIEGA-R001](#F-CIEGA-R001) a
> [F-CIEGA-R004](#F-CIEGA-R004))— se responde desde la bandeja, par por par y antes que todo lo demás
> ([F-BANDEJA-R001](#F-BANDEJA-R001)), y su resultado entra al veredicto de la tanda ([F-METAS-R008](#F-METAS-R008)). La
> bandeja no sortea ni cuenta por su cuenta.

Su tamaño, su meta y quién la responde los decide José en el paquete F ([DOUBT-QUIEN-CIEGA](#DOUBT-QUIEN-CIEGA),
[DOUBT-TAMANO-CIEGA](#DOUBT-TAMANO-CIEGA)). Es «la única medida que no depende de ningún agente» (propuesta v2) y la que
dice si hace falta un refutador aparte ([DOUBT-QUIEN-REFUTA](#DOUBT-QUIEN-REFUTA)).

<a id="F-BANDEJA-R006"></a>
### Rule[F-BANDEJA-R006] - Lo que José decide no vuelve mientras el ejercicio no cambie
**Severity**: major | **Validation**: AUTO_VALIDATED

> Una decisión de José sobre un desempate o un ejercicio sin ganador queda registrada con la identidad del hallazgo y la
> huella del contenido del ejercicio, y vale mientras ese contenido no cambie: la misma pregunta no vuelve a la bandeja
> en ninguna tanda ni análisis posterior. Si el ejercicio cambia, la pregunta puede volver.

Es el mismo criterio del libro de evaluaciones ([F-EVCOST-R001](#F-EVCOST-R001)): lo decidido se identifica por lo que
se decidió, no por el id del ejercicio, que sobrevive a los cambios.

<a id="F-BANDEJA-R007"></a>
### Rule[F-BANDEJA-R007] - Lo pendiente no frena lo que no lo necesita
**Severity**: major | **Validation**: AUTO_VALIDATED

> Un desempate o un ejercicio sin ganador sin responder deja ese ejercicio como estaba y no impide publicar el resto de
> la tanda; al publicar, queda en el informe como «sin decidir» y sus tareas siguen abiertas. Una muestra de regla sin
> responder deja fuera los cambios de esa regla. La muestra ciega sin responder impide publicar: el veredicto de la tanda
> queda en «falta la muestra ciega» ([F-METAS-R008](#F-METAS-R008)).

<a id="F-BANDEJA-R008"></a>
### Rule[F-BANDEJA-R008] - Publicar es un solo OK, y sólo desde la bandeja
**Severity**: critical | **Validation**: AUTO_VALIDATED

> José publica la tanda con un solo OK, que sólo se acepta si, con todas sus decisiones ya incorporadas, el veredicto de
> la tanda es «pasa» ([F-METAS-R008](#F-METAS-R008)) y la compuerta de «no empeorar» pasó ([F-NOEMP-R005](#F-NOEMP-R005) a
> [F-NOEMP-R007](#F-NOEMP-R007)). Con «no se puede decidir» tampoco se acepta, aunque la compuerta pase, y la negativa
> nombra el veredicto ([F-PUBPROD-R005](#F-PUBPROD-R005)). La publicación registra quién, cuándo y qué entró, y deja la
> lista de lo que puede llegar a producción ([F-PUBPROD-R001](#F-PUBPROD-R001)). Aprobar de a una una propuesta de
> la tanda abierta se rechaza: entra al curso sólo al publicar ([F-TANDA-R009](#F-TANDA-R009)).

Sacar un cambio de la tanda sigue siendo rechazarlo ([F-TANDA-R008](#F-TANDA-R008)), y lo que José rechazó en la revisión
sale al publicar ([F-PUBPROD-R003](#F-PUBPROD-R003)). El mensaje es [ASSUMPTION].

**Error**: "La propuesta <id> es de la tanda <id>: entra al curso al publicar la tanda."

<a id="F-BANDEJA-R009"></a>
### Rule[F-BANDEJA-R009] - La bandeja muestra lo que hay que regrabar
**Severity**: major | **Validation**: AUTO_VALIDATED

> La bandeja muestra cada audio pendiente de regrabar —los que pide la tanda abierta y los que quedaron de tandas
> publicadas ([F-TANDA-R012](#F-TANDA-R012))— con el ejercicio como lo ve el alumno, la clave del audio y el texto con
> que hay que grabarlo. Invariantes:
> 1. no se contesta: regrabar es trabajo de José fuera de content-audit, y no frena la publicación
>    ([F-BANDEJA-R007](#F-BANDEJA-R007));
> 2. cada audio aparece una sola vez, con el último texto pedido.

Es la mitad visible de la decisión del orquestador del 1/10: lo pendiente de regrabar no vuelve a la mesa, y José lo ve.
**Criterio**: con una tanda de B1 que pidió regrabar `B1.13.05.15` («I learned ___ me / myself to drive a car.»,
`…399b`) con «I learned myself to drive a car.», la bandeja de la tanda siguiente lo muestra al final, sin pregunta, hasta
que el registro de grabaciones tiene ese texto.

## Contexto

**Qué llega a la bandeja de A1** (estimado con los casos de `escribir/`): unos 28 ejercicios sin ganador (el 10 % de los
278 de mesa, [ASSUMPTION] hasta medirlo en la primera tanda), los desempates que dé el juez de traducción (a medir con su
examen), unas 4 muestras de regla de hasta 20 cambios cada una y la muestra ciega (100 pares en la propuesta; el tamaño
lo fija el paquete F): cerca de 200 cosas, casi todas para mirar y no para decidir. A medio minuto cada una, entre una y
dos horas por tanda.

Hoy el tablero tiene tres lugares para aprobar: *Plan* toma el último plan creado, *Improvements* el activo, y la cola
de revisión va de a una. La bandeja los reemplaza para lo que es de una tanda; `approve` y `reject`
siguen como hoy para todo lo demás.

## Alcance

- **Adentro**: qué llega a la bandeja, qué se le pregunta a José, qué pasa con cada respuesta, qué se recuerda y cuándo
  se puede publicar.
- **Afuera**: la muestra ciega, su meta y quién la hace, y el veredicto de la tanda (paquete F); cómo se ve la bandeja
  (paquete G); las decisiones sobre propuestas que no son de una tanda, que siguen con `approve` y `reject`.

## User Journeys

### Journey[F-BANDEJA-J001] - Vaciar la bandeja y publicar la tanda
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-BANDEJA-J001
    name: Vaciar la bandeja y publicar la tanda
    flow:
      - id: abrir_bandeja
        action: "José pide la bandeja de la tanda abierta y ve sólo la muestra ciega, sin ningún otro cambio de la tanda"
        gate: [F-BANDEJA-R001]
        then: ciega
      - id: ciega
        action: "José responde la muestra ciega par por par y la cierra"
        gate: [F-BANDEJA-R005]
        then: decidir
      - id: decidir
        action: "La bandeja muestra ahora los desempates y los ejercicios sin ganador, por gravedad, y José los contesta"
        gate: [F-BANDEJA-R001, F-BANDEJA-R002, F-BANDEJA-R003, F-BANDEJA-R006]
        then: muestras
      - id: muestras
        action: "José mira la muestra de cada regla"
        gate: [F-BANDEJA-R004]
        outcomes:
          - when: "Las muestras de regla quedan aprobadas y la compuerta pasa con las decisiones incorporadas"
            then: publicar
          - when: "José saca una regla entera de la tanda"
            then: recalcular
      - id: recalcular
        action: "La tanda saca esos cambios, se recalcula y vuelve a pasar la compuerta"
        gate: [F-BANDEJA-R007]
        then: publicar
      - id: publicar
        action: "José publica con un solo OK; lo no decidido queda como estaba y en el informe como sin decidir"
        gate: [F-BANDEJA-R007, F-BANDEJA-R008]
        result: success
```

## Open Questions

<a id="DOUBT-QUIEN-APRUEBA"></a>
### Doubt[DOUBT-QUIEN-APRUEBA] - ¿Quién aprueba qué antes de publicar una tanda?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): la mesa acepta lo que pasa la compuerta; José decide sólo los desempates y los sin
  ganador, aprueba la muestra de cada regla, hace la muestra ciega y publica con un solo OK. En A1, unas 200 cosas.
- [ ] Opción B: José aprueba cada cambio, como el modo humano de hoy ([F-REVAPR-R007](#F-REVAPR-R007)): unas 450
  aprobaciones en A1 y unas 2.900 en el curso.
- [ ] Opción C: todo automático salvo desempates y sin ganador, sin muestras: más rápido, pero sin ninguna medida que no
  dependa de un agente.

**Answer**: **Opción A** (José, 30/9): la mesa acepta; José decide los desempates y los ejercicios sin ganador, mira las
muestras y publica con un solo OK. La muestra ciega va primero ([F-BANDEJA-R001](#F-BANDEJA-R001)), alineado con la
opción A de [DOUBT-QUIEN-CIEGA](#DOUBT-QUIEN-CIEGA) del paquete F.

## References

- **FEAT-REVAPR, FEAT-PIPRE** — Aprobar, rechazar, el modo humano y la vista previa, que siguen fuera de las tandas:
  R008.
- **FEAT-QICOR, FEAT-EVCOST** — Ver el candidato con lo que no cumple y decidir; identificar por el contenido: R003, R006.
- **FEAT-TRSEN** (ronda 1, paquete C) — El desacuerdo de los jueces va a la bandeja: R002.
- **FEAT-CIEGA, FEAT-METAS** (ronda 2, paquete F) — La muestra ciega y el veredicto de la tanda: R005, R007, R008.
- **FEAT-TANDA, FEAT-MESA, FEAT-NOEMP** (este paquete) — Qué llega, desde dónde, con qué compuerta se publica y qué
  queda pendiente de regrabar: R009.
