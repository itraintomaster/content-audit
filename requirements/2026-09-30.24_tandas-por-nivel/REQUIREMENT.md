---
feature:
  id: FEAT-TANDA
  code: F-TANDA
  name: Tandas por nivel - reglas, mesas, recálculo del curso y una sola publicación
  priority: critical
---

# Tandas por nivel: reglas, mesas, recálculo del curso y una sola publicación

## TL;DR

**Qué**: La corrección va por tandas de un nivel: cada tanda parte del último estado publicado, aplica primero las reglas
y después las mesas de sus temas, recalcula el curso entero al cerrar, pasa la compuerta de «no empeorar» y se publica
con un solo OK de José.

**Por qué**: Recalcular el curso después de cada cambio obliga a ir de a uno. Por tanda, los temas van en paralelo y el
curso se mide una vez, completo —unos 38 segundos—, antes de que ningún cambio entre.

## Recorrido de uso (estado actual)

Cómo quien corrige intenta hoy corregir un nivel entero y medirlo antes de dar los cambios por buenos:

1. Analiza y arma el plan → `AnalyzeCommand.analyze(…)` y `PlanCommand.plan(auditId, storageMode, …)`. ✓ Disponible.
2. Fija qué análisis y qué plan mira → `SetActiveAnalysisCommand.setActive(auditId, planId)`. ✓ Disponible.
3. Corrige → `ReviseCommand.revise(taskId, planId, …)`, `ReviseInstructionsCommand.reviseInstructions(…)`. ✓ Disponible,
   una tarea por vez.
4. Aprueba → `ApproveCommand.approve("proposal", id, planId, note)`. ✓ Disponible, una propuesta por vez, y cada
   aprobación escribe el curso en ese momento.
5. Mira lo pendiente proyectado → `GetConsolidatedCommand.getConsolidated(coursePath, format)`. ✓ Disponible, sin decidir.
6. Agrupa un nivel, corre sus temas en paralelo, recalcula al cerrar, devuelve lo que empeora y publica todo junto →
   ⚠️ no existe.

**Punto de fricción**: paso 6. Las piezas están —plan, propuestas pendientes, proyección, aprobación—, pero nada las
junta por nivel ni decide cuándo un conjunto de cambios está en condiciones de entrar al curso.

**Cambio mínimo necesario**: un recurso tanda con cuatro momentos —abrir un nivel, correr, cerrar y publicar— hecho con
el plan, las propuestas pendientes, la proyección única y las metas del paquete F, y la aprobación de hoy.

## Reglas de Negocio

<a id="F-TANDA-R001"></a>
### Rule[F-TANDA-R001] - Una tanda es un nivel y parte del último estado publicado
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una tanda se abre para un [nivel CEFR](glossary:Nivel CEFR) sobre el último análisis publicado —la línea base fijada, para la primera; el
> del curso que dejó la tanda anterior, para las demás— y toma del plan de ese análisis:
> 1. todas las tareas de la familia de errores de ese nivel con resolución *regla* o *mesa*;
> 2. las tareas `LEMMA_ABSENCE` que cubren un hallazgo de errores de ese nivel ([F-TAREA-R004](#F-TAREA-R004));
> 3. ninguna otra tarea de vocabulario;
> 4. los encargos de José sobre temas del nivel que pide al abrirla, como pasar un tema a opción múltiple
>    ([F-MESA-R015](#F-MESA-R015)).
>
> Al abrir declara, por analizador, cuántos ejercicios del nivel quedaron sin evaluar ([F-HALL-R008](#F-HALL-R008)). La
> línea base queda fija mientras las tandas avanzan: cada tanda la hereda de la anterior, y sólo cambia su punto de
> partida, que es el análisis que dejó la última publicada.

Las 3.051 tareas de largo y el resto del vocabulario quedan fuera a propósito: José decidió el 11/8 que el largo no
rechaza una corrección, el 077 no corrigió largos, y triplicarían el trabajo de la mesa. Siguen con `revise` como hoy. La
línea base fija es la que mide el vocabulario acumulado ([F-NOEMP-R006](#F-NOEMP-R006)); el punto de partida, los
hallazgos y los conteos de la tanda ([F-NOEMP-R005](#F-NOEMP-R005)).

<a id="F-TANDA-R002"></a>
### Rule[F-TANDA-R002] - Una sola tanda abierta, y cada tarea en una sola tanda
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En el curso hay a lo sumo una tanda abierta. Una tarea de la tanda abierta no la toman `revise` ni
> `revise-instructions`: el pedido se rechaza nombrando la tanda. Una tarea con una propuesta pendiente de fuera de la
> tanda no entra a la tanda, y la tanda la declara.

Sigue a [F-REVAPR-R010](#F-REVAPR-R010), que ya rechaza re-revisar una tarea con propuesta pendiente: dos caminos no
corrigen el mismo ejercicio a la vez. El mensaje es [ASSUMPTION].

**Error**: "La tarea <id> es de la tanda <id> y se decide en su bandeja."

<a id="F-TANDA-R003"></a>
### Rule[F-TANDA-R003] - Antes de correr, la tanda dice cuánto cuesta, y no consulta sin tope
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Antes de hacer una sola consulta, la tanda declara sus tareas por analizador y resolución, sus ejercicios y temas, y
> cuántas consultas y horas estima con los temas y consultas a la vez elegidos. Corre sólo con un tope de consultas
> pedido explícitamente; al llegar al tope no empieza ninguna consulta más, y lo que no se decidió queda «sin intentar».

Mantiene [F-JEXAM-R004](#F-JEXAM-R004) (cada juez dice cuánto cuesta, sin consultar) y la decisión de José del 30/9:
«sin pedido explícito no se paga nada». Las reglas no consultan modelos, así que corren sin tope.

<a id="F-TANDA-R004"></a>
### Rule[F-TANDA-R004] - Primero las reglas, sin modelo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Las tareas de resolución *regla* se aplican antes de abrir ninguna mesa, sin consultar ningún modelo, juntas por
> ejercicio. Cada cambio por regla:
> 1. queda como propuesta pendiente de la tanda, marcada con su regla;
> 2. pasa la compuerta por cambio ([F-NOEMP-R001](#F-NOEMP-R001) a [F-NOEMP-R004](#F-NOEMP-R004));
> 3. si no la pasa, no se aplica y sus tareas pasan a la mesa de su tema.

Son las reglas que fija cada analizador: la mayúscula en todas las opciones ([F-OMVAL-R003](#F-OMVAL-R003)), la forma
que falta con «|» ([F-RESPC-R005](#F-RESPC-R005)), la pista que sale de la respuesta ([F-PISTA-R005](#F-PISTA-R005)), la
oración guardada que se rearma y la «/» que pasa a «|» ([F-DATOS-R001](#F-DATOS-R001), [F-DATOS-R002](#F-DATOS-R002)). Las
de opción múltiple entran: José tomó la A de [DOUBT-MC-REARMA](#DOUBT-MC-REARMA) el 30/9. Van primero porque son baratas
y porque la mesa revalida contra el ejercicio ya arreglado ([F-MESA-R002](#F-MESA-R002)).

<a id="F-TANDA-R005"></a>
### Rule[F-TANDA-R005] - Después, las mesas de sus temas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Aplicadas las reglas, la tanda abre una mesa por cada tema del nivel con tareas de resolución *mesa*
> ([F-MESA-R001](#F-MESA-R001)), con los temas y consultas a la vez elegidos ([F-MESA-R010](#F-MESA-R010)), y cada tarea
> termina con un desenlace: resuelta, ya no se sostiene, sin ganador, desempate, sin alcance declarado o sin intentar.
> Una tarea cuyo tipo no declara lo que su arreglo puede tocar no se intenta: queda «sin alcance declarado», sin
> candidato ni intento gastado ([F-TAREA-R006](#F-TAREA-R006)).

Es el desenlace que ya cuenta [F-TANDA-R010](#F-TANDA-R010); nombrarlo acá deja probar en la tanda la parte de
[F-TAREA-R006](#F-TAREA-R006) que no se podía probar en FEAT-TAREA.

<a id="F-TANDA-R006"></a>
### Rule[F-TANDA-R006] - Al cerrar, se recalcula el curso entero y se pasa la compuerta
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cuando todas sus tareas tienen desenlace, o se llegó al tope, la tanda aplica todos sus cambios aceptados a una copia
> del curso del que partió, corre el análisis completo con los mismos analizadores que ese análisis y pasa la compuerta
> por tanda ([F-NOEMP-R005](#F-NOEMP-R005) a [F-NOEMP-R008](#F-NOEMP-R008)). Si frena, recalcula sin lo devuelto hasta
> que pase. Invariantes:
> 1. la copia lleva las propuestas pendientes de la tanda y ninguna otra, aunque el plan tenga otras pendientes;
> 2. una tarea está resuelta cuando la identidad de su hallazgo, y las de los hallazgos que cubre, ya no aparecen en el
>    curso recalculado.

La copia y el análisis son la proyección única del paquete F ([F-PROYU-R001](#F-PROYU-R001)), y la comparación es la
suya ([F-METAS-R001](#F-METAS-R001)): la tanda no calcula por su cuenta. Lo que la tanda le pasa a F es qué es la tanda:
sus propuestas pendientes, el análisis del que partió y la línea base fija; no «las pendientes del plan activo», que
pueden incluir correcciones de vocabulario hechas con `revise` fuera de la tanda. Los jueces reutilizan lo que juzgaron
en la mesa ([F-JEXAM-R005](#F-JEXAM-R005)); se paga sólo lo que cambió y nadie juzgó, como la miniteoría de cada tema
tocado (una consulta por tema). El análisis sin jueces tarda unos 38 segundos con el caché de hoy, y unos 5 con un caché
por oración (medido el 30/9).

<a id="F-TANDA-R007"></a>
### Rule[F-TANDA-R007] - Una tanda interrumpida se retoma donde quedó
**Severity**: major | **Validation**: AUTO_VALIDATED

> Si la tanda se corta —falla del servicio, tope o pedido de José—, conserva el desenlace de cada tarea, cada veredicto
> y cada cambio aceptado. Retomarla sigue desde ahí: no repite ningún intento decidido ni paga dos veces una consulta.

Mantiene [F-EVCOST-R004](#F-EVCOST-R004) (cada resultado queda disponible apenas se obtiene) y
[F-REVBYP-R014](#F-REVBYP-R014) (el artefacto se guarda antes de aplicar).

<a id="F-TANDA-R008"></a>
### Rule[F-TANDA-R008] - Antes de publicar, cualquier cambio se puede sacar
**Severity**: major | **Validation**: AUTO_VALIDATED

> Mientras la tanda no se publica, José puede sacar un cambio, o todos los de una regla: su propuesta queda rechazada,
> el ejercicio queda como estaba antes de ese cambio y su tarea vuelve a pendiente. La tanda se recalcula y vuelve a
> pasar la compuerta. Invariantes:
> 1. los cambios posteriores del mismo ejercicio que tocaron lo mismo que el que sale, salen con él, y el informe los
>    nombra; los que tocaron otras partes se conservan [ASSUMPTION];
> 2. sacar un cambio de un ejercicio no saca la miniteoría reescrita de su tema, que se saca sólo por sí misma.

Mantiene [F-REVAPR-R012](#F-REVAPR-R012): rechazar no toca el curso. Es lo que hace reversibles a las reglas antes de que
nada entre al curso. Un cambio de la mesa se armó y se juzgó sobre el ejercicio con las reglas ya aplicadas: si comparte
partes con el que sale, ya no se puede sostener solo ([DOUBT-SACAR-ENCADENADO](#DOUBT-SACAR-ENCADENADO)). Lo que se
conserva se vuelve a medir y a juzgar al recalcular.

<a id="F-TANDA-R009"></a>
### Rule[F-TANDA-R009] - Publicar aplica todo junto, y sólo así cambia el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Publicar exige que el veredicto de la tanda sea «pasa» ([F-METAS-R008](#F-METAS-R008)), que la compuerta de «no
> empeorar» pase y el OK de José en la bandeja ([F-BANDEJA-R008](#F-BANDEJA-R008)). Invariantes:
> 1. aplica al curso de content-audit todos los cambios aceptados que quedan en la tanda, y ninguno más;
> 2. sus tareas quedan completadas;
> 3. el análisis del curso publicado da los mismos hallazgos y números que el recálculo que pasó la compuerta, y es desde
>    donde abre la tanda siguiente;
> 4. la línea base no se mueve ([F-TANDA-R001](#F-TANDA-R001));
> 5. no escribe en producción.

Mantiene [F-REVAPR-R011](#F-REVAPR-R011) (aprobar aplica al curso) y la preservación de FEAT-RPRES al aplicar. Llevar el
curso a producción y regrabar los audios siguen siendo pasos de José, fuera de content-audit.

<a id="F-TANDA-R010"></a>
### Rule[F-TANDA-R010] - El informe de la tanda cierra sus cuentas
**Severity**: major | **Validation**: AUTO_VALIDATED

> El informe de una tanda da cuántas tareas terminaron resueltas por regla, resueltas por la mesa, ya no se sostienen,
> cubiertas, sin ganador, en desempate, devueltas sin intentos, sin alcance declarado y sin intentar, y la suma da sus
> tareas. Da aparte cuántos ejercicios de cada encargo de José quedaron hechos y cuántos sin ganador. Da además, por
> analizador y nivel, los ejercicios con error antes y después; la lista de audios para regrabar; las consultas gastadas
> por puesto y las horas; y las decisiones de José.

Sigue a [F-QICOR-R008](#F-QICOR-R008), que ya distingue sus desenlaces y los hace cerrar contra lo intentado.

<a id="F-TANDA-R011"></a>
### Rule[F-TANDA-R011] - Abrir una tanda fija sus metas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Abrir una tanda fija las metas como lo hace `set-goals` ([F-METAS-R006](#F-METAS-R006)): content-audit registra en ese
> momento la huella y los valores de la declaración de metas. Invariantes:
> 1. las metas fijadas son las de la declaración tal como está al abrir, con la hora de la apertura;
> 2. la comparación de esa tanda evalúa contra ellas: si alguien cambia una meta después de abrir, lo dice con su valor
>    de antes y el de ahora;
> 3. un pedido de abrir que se rechaza —sin línea base fijada, u otra tanda abierta ([F-TANDA-R002](#F-TANDA-R002))— no
>    fija nada.

Es la mitad de [F-METAS-R006](#F-METAS-R006) que cumple la tanda, y no se puede probar en FEAT-METAS porque el que abre es
el servicio de la tanda (pedido del qa-tester de F, 1/10). **Criterio**: con el piso de vocabulario en 0,5 puntos
([F-NOEMP-R006](#F-NOEMP-R006)), abrir la tanda de A1 deja fijo ese 0,5; si alguien lo pasa a 1,0 antes de cerrarla, la
comparación de la tanda dice «metas cambiadas desde que se fijaron: piso de vocabulario 0,5 → 1,0».

## Contexto

**El orden.** El brief 089 fija «corregir por tandas, A1 primero»: después de cada tanda se recalcula el curso entero y
lo que empeora se devuelve. José lo pidió así el 29/9: quiere las estadísticas del curso al final, no después de cada
cambio. Cada propuesta de la tanda conserva su vista previa ([F-PIPRE-R001](#F-PIPRE-R001)), que mide esa propuesta sola
([F-PIPRE-R011](#F-PIPRE-R011)); la tanda suma lo que ninguna vista previa ve: todos los cambios juntos. Con el caché de
hoy cada vista previa cuesta lo mismo que un análisis con una oración cambiada, unos 38 segundos (medido), porque
cambiar una oración vuelve a tokenizar el curso entero: para unas 2.900 propuestas son unas 30 horas de cómputo, sin
modelos. Con el curso ya tokenizado, el mismo análisis tarda 1,7 segundos (2,4 re-medido el 30/9 con `analyze` desde la
línea de comandos). Los 4 segundos que midió el paquete F son otra cosa: `get consolidated` con el curso en caché, que
carga el informe activo, corre el motor dos veces y compara cada nodo.

**Qué reusa y qué suma el paquete E, pieza por pieza.** Lo que cambia una decisión existente va como duda, nunca como
efecto colateral.

| Pieza | Reusa | Suma | Decisión existente |
|---|---|---|---|
| El plan | tareas, estados, `get tasks`, `get task` | una tarea por hallazgo de errores, cubiertas y filtros (FEAT-TAREA) | se mantienen F-RCLA-R001, F-HALL-R004 y F-HALL-R013 |
| `revise` | propuestas pendientes, artefactos, `approve` y `reject` | la mesa como forma de producir propuestas dentro de la tanda (FEAT-MESA) | se mantiene: `revise`, `revise-instructions` y `assess-candidate` no cambian fuera de las tandas |
| La preservación | lo que no se declara no se toca (FEAT-RPRES) | lo que puede tocar cada tipo nuevo, unido por ejercicio | cambia F-RPRES-R003 para la miniteoría (José, 30/9: [DOUBT-MINITEORIA-ALCANCE](#DOUBT-MINITEORIA-ALCANCE)) |
| La revalidación | la de la consigna (F-QICOR-R009) y el reuso de veredictos | para todo analizador, antes de proponer | se mantiene |
| La no-regresión léxica | la de F-LASAG-R007 y la consulta léxica | veta en la mesa y se cuenta por nivel en la tanda (FEAT-NOEMP) | se mantiene; en la mesa pasa a vetar |
| Los jueces | los del paquete C, con candidato y consultas a la vez (F-JEXAM-R005, R006) | juzgar el ejercicio como queda, dentro de la mesa | se mantiene |
| La vista previa | una por propuesta (F-PIPRE-R001) | el recálculo de todos los cambios juntos, al cerrar | se mantiene |
| La proyección | la única del paquete F | la compuerta y lo que se devuelve | se mantiene |
| La opción múltiple | sus reglas y su juez (paquetes A y C) | corregirla en la tanda, y el encargo de «Contracciones» | cambia F-OPMUL-R006 dentro de las tandas (José, 30/9: [DOUBT-MC-REARMA](#DOUBT-MC-REARMA)) |
| La aprobación | el modo humano: todo queda pendiente | un solo OK por tanda, desde la bandeja (FEAT-BANDEJA) | se mantiene (José, 30/9: [DOUBT-QUIEN-APRUEBA](#DOUBT-QUIEN-APRUEBA)) |

**Una tanda de A1, de punta a punta** (estimado con los casos de `escribir/` y el ritmo medido de los jueces):

| Paso | Qué pasa | Cuánto |
|---|---|---|
| Juzgar A1 antes de abrir | los jueces del paquete C sobre el nivel | unas 7.300 consultas (paquete C) |
| Reglas | 154 ejercicios que sólo necesitan una regla | segundos |
| Mesas | 278 ejercicios en 63 temas, 4 temas y 4 consultas a la vez | unas 2.200 consultas, 2,9 h |
| Cerrar | recálculo, jueces de lo que nadie juzgó y compuerta | minutos |
| Bandeja | desempates, sin ganador y muestras | unas 200 cosas para José |

## Alcance

- **Adentro**: abrir, correr, cerrar y publicar una tanda de un nivel; qué tareas toma; su costo y su tope; retomarla; su
  informe.
- **Afuera**: la línea base y la proyección única (paquete F); la mesa (FEAT-MESA); la compuerta (FEAT-NOEMP); la
  bandeja (FEAT-BANDEJA); las tareas de vocabulario, que siguen con `revise`; llevar el curso a producción y regrabar
  los audios; la pantalla de la tanda (paquete G).

## User Journeys

### Journey[F-TANDA-J001] - Correr y publicar la tanda de un nivel
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-TANDA-J001
    name: Correr y publicar la tanda de un nivel
    flow:
      - id: abrir
        action: "José abre la tanda de un nivel sobre el último análisis publicado"
        gate: [F-TANDA-R001, F-TANDA-R002, F-TANDA-R011]
        then: costo
      - id: costo
        action: "La tanda declara sus tareas, sus temas y cuántas consultas y horas estima"
        gate: [F-TANDA-R003]
        outcomes:
          - when: "José pide un tope de consultas"
            then: reglas
          - when: "No hay tope pedido"
            then: sin_tope
      - id: reglas
        action: "La tanda aplica las reglas sin modelo y abre las mesas de sus temas"
        gate: [F-TANDA-R004, F-TANDA-R005]
        then: cerrar
      - id: cerrar
        action: "La tanda recalcula el curso entero con sus cambios y pasa la compuerta hasta que pase"
        gate: [F-TANDA-R006]
        then: publicar
      - id: publicar
        action: "Con el OK de José, los cambios entran juntos al curso y el análisis publicado es el del recálculo"
        gate: [F-TANDA-R009, F-TANDA-R010]
        result: success
      - id: sin_tope
        action: "La tanda no hace ninguna consulta y sus tareas de mesa quedan sin intentar"
        gate: [F-TANDA-R003]
        result: failure
```

## Open Questions

Las dos decisiones de José que pasan por la tanda están en sus requirements y las contestó el 30/9: 4 temas y 4
consultas a la vez ([DOUBT-TEMAS-A-LA-VEZ](#DOUBT-TEMAS-A-LA-VEZ), en FEAT-MESA) y la mesa acepta, José decide la
bandeja ([DOUBT-QUIEN-APRUEBA](#DOUBT-QUIEN-APRUEBA), en FEAT-BANDEJA).

<a id="DOUBT-SACAR-ENCADENADO"></a>
### Doubt[DOUBT-SACAR-ENCADENADO] - Si José saca un cambio, ¿qué pasa con los que se armaron encima?
**Status**: OPEN

Un ejercicio puede tener un cambio por regla (la mayúscula de una opción) y, después, uno de la mesa (la oración), que se
juzgó sobre el ejercicio con la regla aplicada.

- [ ] Opción A (recomendada): salen con él los cambios posteriores del mismo ejercicio que tocaron lo mismo; los que
  tocaron otras partes se conservan y se vuelven a medir y juzgar al recalcular. La miniteoría del tema no sale.
- [ ] Opción B: sale todo lo del ejercicio en la tanda, y sus tareas vuelven a pendiente.
- [ ] Opción C: no se puede sacar un cambio que otro usó; primero hay que sacar el de encima.

**Answer**: Pendiente. Recomiendo A: sacar la regla de mayúsculas no tira la oración que arregló la mesa si no la tocó, y
lo que se conserva pasa otra vez por los jueces al recalcular. B descarta trabajo pagado; C obliga a José a deshacer en
orden. Hasta que conteste, rige A ([F-TANDA-R008](#F-TANDA-R008)).

## References

- **FEAT-TAREA, FEAT-MESA, FEAT-NOEMP, FEAT-BANDEJA** (este paquete) — Las tareas que toma, las mesas que corre, la
  compuerta que pasa y el OK que la publica.
- **FEAT-REVAPR, FEAT-REVBYP, FEAT-RPRES, FEAT-EVCOST** — Propuestas pendientes, aprobar y rechazar, preservación y
  resultados que no se pierden: R002, R007, R008, R009.
- **FEAT-PIPRE, FEAT-QICOR, FEAT-JEXAM** — La vista previa por propuesta, los desenlaces de una corrida y el costo
  declarado: R003, R006, R010.
- **FEAT-OMVAL, FEAT-RESPC, FEAT-PISTA, FEAT-DATOS** (ronda 1, paquete A) — Las reglas que se aplican primero: R004.
- **FEAT-PROYU, FEAT-METAS** (ronda 2, paquete F) — La proyección única, la comparación con la línea base y el veredicto:
  R006, R009; las metas que abrir fija: R011.
