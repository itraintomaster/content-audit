---
feature:
  id: FEAT-PROYU
  code: F-PROYU
  name: Una sola proyección del curso con sus propuestas
  priority: critical
---

# Una sola proyección: el curso con sus propuestas se mide igual en todas partes

## TL;DR

**Qué**: El curso con lo aprobado y lo pendiente se proyecta de una sola manera, la de `get consolidated`, que da lo
mismo que `analyze` sobre ese curso escrito; ninguna proyección entra al historial de análisis, y la línea base la fija
José y no se mueve cuando el análisis activo avanza.

**Por qué**: Hoy hay tres caminos que no coinciden: sobre el mismo caso, la vista consolidada pone A1 en 92,4 % donde
el análisis de la copia da 96,5 y no muestra el curso, y el plan proyectado deja su análisis como el último.

## Recorrido de uso (estado actual)

Cómo José ve hoy el curso con las propuestas de una tanda antes de aprobarlas. El caso: las cuatro propuestas del 077
para «Afirmativas regulares» (A1 · Present Simple), pendientes.

1. Fija el análisis contra el que compara → `SetActiveAnalysisCommand.setActive(auditId, planId)`. ✓ Disponible, pero
   ⚠️ el mismo par es a la vez la línea base y el punto de partida: cuando una tanda se publica, el activo avanza al
   análisis del curso publicado (paquete E, FEAT-TANDA) y la base se iría con él.
2. Pide el curso con lo aprobado y lo pendiente → `GetConsolidatedCommand.getConsolidated(coursePath, format)`. ✓
   Disponible, pero ⚠️ con pendientes el motor corre dos veces en el mismo proceso y la segunda arrastra la primera
   ([FEAT-CERO](#F-CERO-R001)): la foto con pendientes da COCA 75 en A1, 76,1 en B1 y 51,3 en B2, donde `analyze` sobre
   la copia da 100, 57,9 y 32,2 (el vocabulario de A1 queda en 92,4 en vez de 96,5); los 55 topics salen como cambiados,
   con 0 tokens COCA en la foto con lo aprobado aunque no hay nada aprobado; y el curso no aparece en ninguna foto,
   porque la vista saltea el nodo raíz, que no tiene entidad.
3. Stats muestra el curso proyectado → ⚠️ no hay verbo: el dashboard copia el curso, reemplaza a mano los ejercicios
   pendientes y corre `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)` en otra carpeta. No mira `elementBefore` y,
   si dos pendientes tocan el mismo ejercicio, gana la última de la lista; la vista consolidada aplica la primera y
   declara la otra no aplicable. Hoy 143 ejercicios tienen dos tareas en el plan.
4. Plan muestra el plan proyectado → `PlanCommand.plan(auditId, EPHEMERAL, true)`. ✓ Disponible, pero ⚠️ el dashboard le
   da un análisis proyectado que guarda en el historial principal: pasa a ser el último, y lo toman `plan` sin
   `--audit`, el impacto de cada propuesta nueva, el corpus de los criterios cuando no le llega su análisis y la
   selección con que arranca el dashboard. Se borra al cerrar la pantalla, a mejor esfuerzo.
5. Cada propuesta trae su impacto → `ReviseCommand.revise(taskId, planId, …)`. ✓ Disponible, pero ⚠️ compara contra el
   último análisis del historial, que puede ser el proyectado, y reporta como tema, topic y nivel los primeros del curso:
   para «My grandmother ___ (come) every year.» muestra el tema «Afirmativas con $be$», sin cambio, cuando el suyo,
   «Afirmativas regulares», baja medio punto en `lemma-absence`.
6. Limpia análisis viejos → `PruneCommand.prune("audits", keep)`. ✓ Disponible, pero ⚠️ borra los más viejos aunque uno
   sea el análisis activo: tres análisis nuevos y un `prune audits --keep 3` borran la línea base.

**Punto de fricción**: pasos 2 a 6. Tres cálculos distintos del mismo curso proyectado, y nada impide que una proyección
se vuelva la línea base.

**Cambio mínimo necesario**: que la proyección de la vista consolidada dé lo mismo que analizar el curso escrito y sea
la única; que se pueda guardar fuera del historial y pedir por su identificador, para el plan proyectado y las
[palabras sugeridas](glossary:Palabra sugerida); que el impacto de cada propuesta salga de esa misma proyección; y que
la línea base la fije José aparte del análisis activo, y que ninguno de los dos se borre sin querer.

## Reglas de Negocio

Desde el 4/10, las menciones a `claude -p` en la política de topes nombran al proveedor por sesión local que lo
reemplaza: `codex-cli` autenticado con ChatGPT
([F-JEXAM-R013](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R013)). El tope 0 sin pedido no cambia.

Grupos: **A**, una sola proyección (R001–R003); **B**, la proyección no es un análisis (R004–R005); **C**, la línea base
y el impacto de cada propuesta (R006–R007). *Proyección*: el curso del análisis activo con lo aprobado (foto
`consolidated`) o con lo aprobado y lo pendiente aplicable (foto `pendingProjection`) del plan activo
([F-CDIFF-R007](#F-CDIFF-R007)); si hay una tanda abierta, sólo con las pendientes de esa tanda (paquete E,
FEAT-TANDA). *Línea base*: el análisis que José fija antes de la primera tanda; el análisis activo avanza cada vez que
se publica una tanda, y la línea base no.

### Grupo A — Una sola proyección

<a id="F-PROYU-R001"></a>
### Rule[F-PROYU-R001] - La proyección da lo mismo que analizar el curso escrito
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para el par activo, cada foto de la proyección —con lo aprobado, y con lo aprobado y lo pendiente aplicable— trae en
> todo nodo, el curso incluido, los mismos puntajes, números, hallazgos y diagnósticos que daría `analyze`, con la misma
> selección de analizadores, sobre un curso donde esas propuestas se escribieron como las escribe `approve`.

<details><summary>Detail</summary>

**Criterio**: con las cuatro propuestas de «Afirmativas regulares» pendientes, la foto con pendientes trae el curso con
73,92 % de vocabulario y los niveles con 96,54 · 94,16 · 79,45 · 63,51, lo mismo que `analyze` sobre la copia; los
topics no cambian; y el ejercicio de «grandmother» baja de 1,0 a 0,9 en `lemma-absence`, con «grandmother» (A2) como
palabra de otro nivel. Hoy, medido con `get-consolidated` sobre un workdir de prueba, el curso no aparece, los niveles
dan 92,38 · 89,99 · 82,48 · 66,70 por el arrastre de [F-CERO-R001](#F-CERO-R001), y los 55 topics salen con 0 tokens
COCA en la foto con lo aprobado.

«Escritas como las escribe `approve`»: el nodo de `elementBefore` reemplazado por `elementAfter`
([F-REVAPR-R011](#F-REVAPR-R011)). Mantiene [F-CDIFF-R007](#F-CDIFF-R007): las fotos se copian de lo que dejó el motor,
no se recalculan; y
[F-HALL-R010](#F-HALL-R010): el número de un nodo es el publicado. Con FEAT-HALL aplicado, las fotos traen también los
números y hallazgos de cada nodo, por el descubrimiento dinámico de [F-CDIFF-R019](#F-CDIFF-R019).

</details>

<a id="F-PROYU-R002"></a>
### Rule[F-PROYU-R002] - Las pendientes entran siempre igual
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una pendiente entra en la proyección si y sólo si su nodo existe y su `elementBefore` coincide con ese nodo en el curso
> con lo aprobado; se aplican por fecha de creación, y la que no entra queda declarada no aplicable, con su causa. Todo
> número proyectado que publique content-audit sale de esa misma aplicación y declara las mismas pendientes aplicadas y
> no aplicadas.

Mantiene [F-CDIFF-R012](#F-CDIFF-R012) y [DOUBT-PENDING-CONFLICT](#DOUBT-PENDING-CONFLICT) (opción A) y los extiende a
toda salida proyectada: el plan proyectado (R005) y el impacto de una propuesta (R007) no pueden aplicar otra cosa. El
dashboard, que hoy aplica todas sin mirar `elementBefore` y deja ganar a la última, deja de proyectar (Alcance). Con
los once analizadores nuevos, que un ejercicio tenga varias tareas va a ser lo común.

<a id="F-PROYU-R003"></a>
### Rule[F-PROYU-R003] - La proyección mide lo mismo que la línea base y no paga de más
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La proyección corre los mismos analizadores que corrió el análisis activo, con la política de tope de `analyze`
> ([F-RONDA2-R002](#F-RONDA2-R002)): lo que se le consulta a un modelo local no tiene tope, y lo que se le consulta a
> claude -p tiene tope 0 salvo que el pedido lo suba. Un analizador que consulta un modelo reusa los veredictos
> registrados y sólo consulta los ejercicios que cambiaron; lo que no evaluó queda declarado sin evaluar, nunca como
> aprobado.

<details><summary>Detail</summary>

Sin la misma selección, antes y después no se pueden comparar: un analizador que falta en una foto no es un cambio del
curso. La base del 29/9 corrió sin el juez de consigna, así que su proyección tampoco lo corre. Mantiene
[F-QINST-R006](#F-QINST-R006) (tope por corrida), [F-EVCOST-R003](#F-EVCOST-R003) (un veredicto registrado se reusa) y
[F-HALL-R008](#F-HALL-R008) (lo no evaluado se declara), y la decisión de José del 30/9: sin pedido explícito no se paga
nada. Un ejercicio que no cambió tiene la misma huella y reusa su veredicto, así que proyectar sólo cuesta lo que cambió.

La política es la de `analyze` desde el 1/10: la regla decía «0 si no se pide» para todo analizador que consulta un
modelo, y eso chocaba con [F-RONDA2-R002](#F-RONDA2-R002), que no le pone tope a lo local. Con Ollama, `naturalness` y
`near-duplicates` corren sin tope en la proyección, como en `analyze`, y la foto da lo mismo que `analyze` sobre el curso
escrito ([F-PROYU-R001](#F-PROYU-R001)); sin Ollama, su alternativa con claude -p tiene tope 0, como los jueces. Con 0
para todo, esos dos dejarían sin evaluar los ejercicios cambiados aunque consultarlos no cueste nada.

</details>

### Grupo B — La proyección no es un análisis

<a id="F-PROYU-R004"></a>
### Rule[F-PROYU-R004] - Ninguna proyección entra al historial de análisis
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Pedir, guardar o usar una proyección no agrega ni cambia ningún análisis del historial: `get audits` lista los mismos,
> el último sigue siendo el mismo, y ningún verbo que hoy toma el último análisis —`plan` sin `--audit`, el impacto de
> una propuesta, el corpus de los criterios— toma una proyección. `prune audits` no las cuenta ni las borra.

Extiende [F-CDIFF-R015](#F-CDIFF-R015) (servir el consolidado no escribe un análisis) a todo uso de una proyección.
Hoy el plan proyectado del dashboard escribe su análisis en el historial principal desde el 15/7; si la pantalla no se
cierra bien, queda como el último.

<a id="F-PROYU-R005"></a>
### Rule[F-PROYU-R005] - Una proyección guardada se pide por su identificador
**Severity**: major | **Validation**: AUTO_VALIDATED

> A pedido, content-audit guarda la proyección del par activo y devuelve un identificador que `plan --audit` —efímero y
> con [contexto de corrección](glossary:Contexto de corrección)— y la consulta de palabras sugeridas aceptan como
> análisis fuente. Guardar otra proyección del mismo par activo reemplaza la anterior.

<details><summary>Detail</summary>

Es lo que hoy hace el dashboard por su cuenta, y lo que [F-PLANEF-R002](#F-PLANEF-R002) y
[F-CSLATDC-R015](#F-CSLATDC-R015) suponen en su «uso esperado»: el cliente persistía su análisis proyectado para que el
plan y la consulta dinámica de palabras sugeridas lo usaran como fuente. Con esta regla la proyección es la de R001, no
se calcula dos veces y no toca el historial (R004). **Criterio**: después de guardar la proyección y pedir un plan
efímero con su identificador, `get audits` lista lo mismo que antes, y las palabras sugeridas de una tarea proyectada ya
no ofrecen una palabra que una pendiente consumió.

</details>

### Grupo C — La línea base y el impacto de cada propuesta

<a id="F-PROYU-R006"></a>
### Rule[F-PROYU-R006] - La línea base la fija José y queda fija; ni ella ni el análisis activo se borran sin querer
**Severity**: critical | **Validation**: VALIDATED

> José fija la línea base con `set-baseline`, eligiendo un análisis del historial. La comparación mide contra ella; la
> vista consolidada y la proyección parten del análisis activo; nada mide contra «el último». Correr `analyze`,
> proyectar, cambiar el par activo o publicar una tanda no la cambia. `delete audit` rechaza borrar la línea base o el
> análisis activo, y `prune audits` los conserva aunque sean de los más viejos y lo dice en su salida.

<details><summary>Detail</summary>

La línea base se separa del par activo porque, desde el paquete E, el activo avanza: cada tanda abre sobre el análisis
del curso que publicó la anterior (FEAT-TANDA), mientras las metas de vocabulario se miden contra la misma base en las
cuatro tandas ([F-METAS-R005](#F-METAS-R005)). Una proyección guardada no puede ser línea base: no está en el
historial (R004).

Cambia [F-CLIRV-R003](#F-CLIRV-R003), que conservaba sólo los N más recientes (José, 30/9:
[DOUBT-PRUNE-ACTIVO](#DOUBT-PRUNE-ACTIVO)). `delete audit` ya rechaza un análisis que un plan referencia
([F-CLIRV-R002](#F-CLIRV-R002)); esta regla lo cubre aunque el plan activo se haya podado. Mantiene
[F-CDIFF-R002](#F-CDIFF-R002): el par activo sólo cambia con `set-active`.

**Criterio**: con la base del 29/9 fijada como línea base, tres análisis nuevos y `prune audits --keep 3`, la base sigue
y la salida dice «Kept the baseline 2026-09-30T11-54-02».

**Error**: "Cannot delete audit '<id>': it is the active analysis. Point 'set-active' to another analysis first."

**Error**: "Cannot delete audit '<id>': it is the baseline. Fix another one with 'set-baseline' first."

**Error**: "Cannot fix '<id>' as the baseline: it is not an analysis in the history."

</details>

<a id="F-PROYU-R007"></a>
### Rule[F-PROYU-R007] - El impacto de una propuesta sale de la misma proyección
**Severity**: major | **Validation**: AUTO_VALIDATED

> El impacto de una propuesta compara, con la medida de R001, el curso de ese momento —con lo aprobado— contra ese curso
> con la propuesta, y lo reporta en el nodo, su tema, su topic, su nivel y el curso: los que la contienen, no los primeros
> del curso. El número agregado de cada nivel es el puntaje de vocabulario publicado.

<details><summary>Detail</summary>

Mantiene [F-PIPRE-R003](#F-PIPRE-R003) y [F-PIPRE-R004](#F-PIPRE-R004), hoy incumplidas: el impacto se calcula contra el
último análisis del historial (que puede ser viejo o proyectado) y, para cada contenedor, toma el primero de ese tipo en
el curso. Las dos puntas corren los analizadores del análisis del que salió la tarea de la propuesta, como pide R003 (José,
30/9: el impacto por propuesta se rehace sobre la proyección). Con los contenedores reales, «My grandmother ___ (come) every year.» muestra su tema «Afirmativas regulares»
de 100,0 a 99,5 en `lemma-absence`; hoy muestra «Afirmativas con $be$» sin cambio, y una propuesta de B2 mostraría el
nivel A1. Mantiene [F-HALL-R010](#F-HALL-R010): el agregado deja de promediar las 11 claves con los cuartos COCA.

</details>

## Contexto

La propuesta v2 («Corregir sin empeorar», pestaña 08) pide una sola proyección: «aplicar la tanda a una copia y correr
el análisis entero». Hoy hay tres caminos y un cuarto número, y sobre el mismo caso no coinciden:

| Camino | Qué hace | Curso | A1 | B1 | Topics con COCA | Deja algo en el historial |
|---|---|---:|---:|---:|---:|---|
| `analyze` de la copia (la verdad) | copia escrita, motor nuevo | 73,92 | 96,54 | 79,45 | 55 | sí, un análisis más |
| Vista consolidada, foto con pendientes | segunda corrida del mismo motor | no aparece (calcula 72,90) | 92,38 | 82,48 | 0 | no |
| Stats del dashboard | copia hecha por el dashboard + `analyze` en otra carpeta | 73,92 | 96,54 | 79,45 | 55 | no |
| Plan proyectado del dashboard | igual, pero guardado en el historial principal | 73,92 | 96,54 | 79,45 | 55 | sí, como el último |
| Impacto por propuesta | una corrida por propuesta, contra el último análisis | — | — | — | — | no; pero reporta el primer tema, topic y nivel del curso, no los del ejercicio |

Las dos primeras filas se midieron el 30/9 sobre una copia del curso en el scratchpad, con la base del 29/9 y las cuatro
propuestas de la pestaña 08: la primera, con un prototipo que cablea el motor como el CLI; la de la vista consolidada,
con `get-consolidated` del CLI sobre un workdir de prueba con las cuatro pendientes sembradas (tardó 4 s con los tokens
en caché; sin caché, tokenizar el curso con pendientes suma 38 s). Las de Stats y del plan proyectado salen de leer
el código del dashboard: copian el curso, reemplazan los ejercicios pendientes y corren `analyze` en un proceso nuevo,
que es lo que mide la primera fila; coinciden con ella mientras la copia sea la misma, como con cuatro pendientes sin
choques, y se separan con dos pendientes sobre el mismo ejercicio o con una pendiente vieja (R002). Las cuatro propuestas
no mueven ningún número de nivel ni del curso: sólo cambian el ejercicio de «grandmother» (1,0 → 0,9), su tema
(1,0 → 0,995) y su topic en `lemma-absence`.

Costo de una proyección, medido: tokenizar un curso entero lleva 38 s la primera vez; una corrida del motor, 0,05 a
0,1 s. Proyectar cuesta lo que se tokeniza de nuevo, no lo que se analiza.

## Alcance

- **Adentro**: R001 a R007.
- **Afuera**: la comparación contra metas y los ejercicios que vuelven (FEAT-METAS); la muestra ciega (FEAT-CIEGA); el
  arrastre del motor, que R001 necesita resuelto (FEAT-CERO); qué es una tanda y la bandeja (paquete E); sacar la
  proyección del dashboard —`ProjectedCourseWriter`, `ProjectedAnalysisService` y su plan proyectado pasan a pedir esto—
  (paquete G); el formato en disco de una proyección guardada.

## User Journeys

### Journey[F-PROYU-J001] - Proyectar las propuestas del plan activo y leer lo mismo en todas partes
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-PROYU-J001
    name: Proyectar las propuestas del plan activo y leer lo mismo en todas partes
    flow:
      - id: pedir
        action: "El operador pide la vista consolidada del par activo, cuyo plan tiene propuestas pendientes"
        outcomes:
          - when: "Cada pendiente coincide con su nodo en el curso con lo aprobado"
            then: proyectar
          - when: "Una pendiente no coincide con su nodo porque otra anterior ya lo cambió"
            then: declarar
      - id: declarar
        action: "La pendiente queda declarada no aplicable con su causa y no entra en ninguna foto ni en ningún número proyectado"
        gate: [F-PROYU-R002]
        then: proyectar
      - id: proyectar
        action: "Cada foto trae los mismos números, hallazgos y diagnósticos que analyze sobre el curso con esas propuestas escritas, con los mismos analizadores que la línea base"
        gate: [F-PROYU-R001, F-PROYU-R003]
        then: guardar
      - id: guardar
        action: "El operador guarda la proyección y pide un plan efímero con su identificador"
        gate: [F-PROYU-R005]
        then: historial
      - id: historial
        action: "get audits lista los mismos análisis, el último, el activo y la línea base no cambian, y prune audits no cuenta la proyección"
        gate: [F-PROYU-R004, F-PROYU-R006]
        result: success
```

## Open Questions

<a id="DOUBT-OTROS-CAMINOS"></a>
### Doubt[DOUBT-OTROS-CAMINOS] - ¿Qué pasa con los otros caminos de proyección?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): Stats deja de proyectar y lee la comparación (FEAT-METAS); el plan proyectado sigue, sobre
  la proyección guardada de R005 y sin escribir análisis; el impacto por propuesta sigue, calculado como R007. El
  dashboard borra `ProjectedCourseWriter` y `ProjectedAnalysisService`.
- [ ] Opción B: se retiran los tres ya: sólo quedan la vista consolidada y la comparación. Plan pierde el plan proyectado
  hasta que llegue la tanda del paquete E, y vuelve la misma palabra sugerida dos veces ([F-PLANEF-R001](#F-PLANEF-R001)).
- [ ] Opción C: quedan como están, marcados «no comparable» en pantalla.

**Answer**: **Opción A** (José, 30/9): Stats se retira; el plan proyectado y el impacto por propuesta se rehacen sobre la
proyección (R005 y R007). Sacar Stats y los dos servicios del dashboard es del paquete G.

<a id="DOUBT-PRUNE-ACTIVO"></a>
### Doubt[DOUBT-PRUNE-ACTIVO] - ¿`prune audits` puede borrar el análisis activo?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): lo conserva siempre y lo dice («Kept the active analysis <id>»), aunque sea de los viejos.
- [ ] Opción B: `prune audits` se rechaza entero si fuera a borrarlo.
- [ ] Opción C: sin cambios; se documenta el riesgo.

**Answer**: **Opción A** (José, 30/9): `prune` nunca borra el análisis activo, y avisa; cambia
[F-CLIRV-R003](#F-CLIRV-R003). Desde que la línea base se separa del activo, vale igual para ella (R006).

<a id="DOUBT-LINEA-BASE-TANDAS"></a>
### Doubt[DOUBT-LINEA-BASE-TANDAS] - ¿Qué análisis se fija como línea base de las cuatro tandas?
**Status**: OPEN

- [ ] Opción A (recomendada): un análisis nuevo del curso, corrido con los analizadores de la ronda 1 ya integrados, antes
  de abrir la primera tanda.
- [ ] Opción B: el del 29/9 (2026-09-30T11-54-02).

**Answer**: Pendiente. Recomiendo A: el del 29/9 es anterior al contrato del hallazgo; no trae hallazgos, números de
errores ni la medida de cada analizador, así que las filas de errores saldrían vacías, un cambio de medida no se podría
mostrar aparte (F-METAS-R007) y la proyección tendría que caer a la corrida base. Con A, todo eso se mide desde el
primer día y el 29/9 queda como referencia histórica.

## References

- **FEAT-CDIFF** — Las tres fotos, la aplicación de pendientes y que servir el consolidado no escribe:
  [F-CDIFF-R002](#F-CDIFF-R002), [F-CDIFF-R007](#F-CDIFF-R007), [F-CDIFF-R012](#F-CDIFF-R012),
  [F-CDIFF-R015](#F-CDIFF-R015), [F-CDIFF-R019](#F-CDIFF-R019). Citado por R001, R002, R004 y R006.
- **FEAT-CERO** — El motor que no arrastra estado ([F-CERO-R001](#F-CERO-R001)). Citado por R001.
- **FEAT-HALL** — Números publicados y lo no evaluado ([F-HALL-R008](#F-HALL-R008), [F-HALL-R010](#F-HALL-R010)).
  Citado por R001, R003 y R007.
- **FEAT-QINST, FEAT-EVCOST** — Tope y reuso de veredictos ([F-QINST-R006](#F-QINST-R006),
  [F-EVCOST-R003](#F-EVCOST-R003)). Citado por R003.
- **FEAT-RONDA2** — La política de tope de `analyze`: lo local sin tope y claude -p con tope 0 salvo pedido
  ([F-RONDA2-R002](#F-RONDA2-R002)). Citado por R003.
- **FEAT-PLANEF, FEAT-CSLATDC** — Plan efímero con contexto y palabras sugeridas del mismo análisis fuente
  ([F-PLANEF-R001](#F-PLANEF-R001), [F-PLANEF-R002](#F-PLANEF-R002), [F-CSLATDC-R015](#F-CSLATDC-R015)). Citado por R005.
- **FEAT-PIPRE** — Impacto por propuesta ([F-PIPRE-R003](#F-PIPRE-R003), [F-PIPRE-R004](#F-PIPRE-R004)). Citado por R007.
- **FEAT-CLIRV, FEAT-REVAPR** — `delete`, `prune` y cómo `approve` escribe el curso ([F-CLIRV-R002](#F-CLIRV-R002),
  [F-CLIRV-R003](#F-CLIRV-R003), [F-REVAPR-R011](#F-REVAPR-R011)). Citado por R001 y R006; R006 cambia F-CLIRV-R003.
- **FEAT-TANDA** (paquete E) — Qué es una tanda y cómo avanza el análisis activo al publicar. Citado en las definiciones
  y en R006.
- **FEAT-METAS** — La comparación contra la línea base ([F-METAS-R005](#F-METAS-R005)). Citado por R006.
