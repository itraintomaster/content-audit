---
feature:
  id: FEAT-NADIE
  code: F-NADIE
  name: Aprobación de jueces y preparación para entregar en Correcciones
  priority: high
---

# Correcciones: aprobación de jueces y preparados para entregar

## TL;DR

**Qué**: ContentAudit distingue «Todos los jueces lo aprueban», «Vos lo aprobás» y «Preparados para entregar» según quién aprobó; «Faltan opiniones» conserva los casos incompletos aún sin aprobación humana.
**Por qué**: Que ningún juez encuentre un problema no significa que José haya revisado y aprobado el ejercicio.

## Reglas de Negocio

Un **analizador** mide sin modelo: largo de la oración, palabras poco vistas, palabras de otro nivel, datos coherentes o duplicados. Un **juez** es un analizador que consulta a un modelo; los jueces aplicables son los que ContentAudit considera para el ejercicio en el análisis vigente. La **corrección** es el estado «Corrección N» del análisis vigente: el curso con los cambios propuestos. Una **aprobación humana vigente** es la decisión guardada definida en [F-NADIE-R006](#F-NADIE-R006).

<a id="F-NADIE-R001"></a>
### Rule[F-NADIE-R001] - Elegibilidad del recorrido sin problemas detectados
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un ejercicio es elegible para el recorrido sin problemas detectados si no tiene hallazgos que no sean leves, los analizadores que no son jueces lo evaluaron por completo, al menos un juez aplicable opinó al día sin abstenerse y se midió el contenido vigente. Esta elegibilidad no limita «Vos lo aprobás»: ese grupo también incluye ejercicios con aprobación humana vigente aunque algún juez señale problemas o falten opiniones vigentes.

<details><summary>Detalle</summary>

Deben cumplirse las cuatro condiciones:

1. No tiene ningún hallazgo de gravedad media, alta o bloqueante, de ningún analizador ni juez. Un hallazgo así de una opinión de antes también lo excluye. Una observación leve no lo excluye, pero se ve ([F-LEVE-R001](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R001)); tampoco la oración que guarda producción, que arregla publicar ([F-LEVE-R004](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R004)).
2. Ningún analizador que no es juez lo dejó sin evaluar o evaluado a medias.
3. Al menos un juez aplicable opinó con su versión de hoy, sin abstenerse.
4. Lo medido es el contenido de hoy: si el ejercicio se editó después de medir, no es elegible.

Invariantes:

1. Un juez que todavía no opinó, que se abstuvo o que opinó con una versión anterior nunca cuenta como aprobación. El ejercicio puede seguir siendo elegible por los demás y quedar en «Faltan opiniones» mientras no tenga aprobación humana vigente ([F-NADIE-R003](#F-NADIE-R003)).
2. Un juez que no se aplica al ejercicio, porque no lo alcanza o ContentAudit lo excluye por «no aplica», no se cuenta ni se nombra. Una abstención de un juez que sigue siendo aplicable no equivale a esa exclusión, aunque su motivo diga «no aplica».
3. Si el ejercicio tiene cambios propuestos, lo juzgado es el después: el ejercicio con esos cambios.
4. Entran los ejercicios con cambios y sin cambios, también los que no tuvieron ninguna tarea en el plan.
5. La marca de cada juez —al día, de antes o abstención— es la que informa ContentAudit; el tablero no la calcula por su cuenta.
6. Sin análisis vigente de la corrección, los grupos se ven deshabilitados y explican por qué.
7. Clasificar y mostrar estos ejercicios no consulta a ningún modelo.

La condición 3 conserva el universo del recorrido original, que el 20/10 consideraba «todos los jueces que opinaron». La confirmación del 21/10 exige a **todos los jueces aplicables** para «Todos los jueces lo aprueban» y «Preparados para entregar». Los casos incompletos sin aprobación humana permanecen en «Faltan opiniones»; con aprobación humana vigente pasan a «Vos lo aprobás», cuyo universo también admite ejercicios que no cumplen esta elegibilidad.

Una opinión de antes no basta para aprobar, como tampoco basta para aprobar en grupo ([F-TALDIA-R006](#F-TALDIA-R006)); si encontró un error, ese hallazgo sí excluye al ejercicio. La condición de contenido vigente coincide con «Otro contenido».

Como referencia histórica, la tanda A1 del 20/10 tenía 1.111 ejercicios elegibles en Corrección 1: 793 con cambios, 7 ya visibles sin cambio y 311 sin tareas. Sólo 3 tenían todas las opiniones al día. Estas cifras explican el recorrido anterior; no son cantidades exigidas al estado actual.

</details>

<a id="F-NADIE-R002"></a>
### Rule[F-NADIE-R002] - Cuatro grupos separados según la aprobación humana y de los jueces
**Severity**: critical | **Validation**: VALIDATED

> Correcciones distingue «Todos los jueces lo aprueban», «Vos lo aprobás», «Preparados para entregar» y «Faltan opiniones», sin superponer ejercicios. «Vos lo aprobás» reúne la aprobación humana completa y guardada cuando todavía no todos los jueces aplicables aprueban al día, incluso si señalan problemas, faltan opiniones, son de antes o hay abstenciones.

<details><summary>Detalle</summary>

| Grupo | Condición de pertenencia |
|---|---|
| Todos los jueces lo aprueban | El ejercicio cumple [F-NADIE-R001](#F-NADIE-R001), todos los jueces aplicables opinaron al día, sin errores ni abstenciones, y falta una aprobación humana vigente del ejercicio completo |
| Vos lo aprobás | Existe aprobación humana vigente del ejercicio completo, pero no todos los jueces aplicables aprobaron al día: alguno señala problemas, no opinó, se abstuvo u opinó con una versión de antes. No requiere la elegibilidad de [F-NADIE-R001](#F-NADIE-R001) |
| Preparados para entregar | El ejercicio cumple [F-NADIE-R001](#F-NADIE-R001), todos los jueces aplicables opinaron al día, sin errores ni abstenciones, y existe aprobación humana vigente del ejercicio completo |
| Faltan opiniones | El ejercicio es elegible por [F-NADIE-R001](#F-NADIE-R001), algún juez aplicable todavía no opinó, se abstuvo u opinó con una versión de antes, y no existe aprobación humana vigente del ejercicio completo |

Los cuatro grupos aparecen debajo de los chips de «Según los jueces», con sus cantidades de ejercicios y el nombre de la corrección. Sus cantidades cuentan los ejercicios que realmente pertenecen a cada grupo. No se superponen; su suma puede superar el universo sin problemas detectados porque «Vos lo aprobás» también incluye ejercicios aprobados por José que tienen problemas señalados por jueces. Un ejercicio con problemas y sin aprobación humana sigue en el recorrido habitual, fuera de estos cuatro grupos. Durante el guardado, un ejercicio puede haber salido provisionalmente de su grupo sin contar todavía en el destino; el indicador global «Guardando» hace visible esa transición ([F-NADIE-R007](#F-NADIE-R007), [F-NADIE-R008](#F-NADIE-R008)). No suman con los chips de «Según los jueces», que cuentan cambios.

Invariantes de navegación:

1. Elegir un grupo quita el motivo, los otros chips y cualquier otro grupo. Elegir un motivo, un chip o «Todos» sale del grupo.
2. La lista de temas muestra los que contienen ejercicios del grupo activo, en el orden del curso, tanto con «Por revisar» como con «Todos»; la aprobación humana se distingue mediante los grupos.
3. Dentro de un tema, la tira y las flechas recorren sólo los ejercicios del grupo activo, en el orden del curso.
4. Un ejercicio sin tarea se muestra con su contenido del curso, igual que uno sin cambios. Uno con cambios conserva el antes, el después y las acciones de decisión.
5. Sin uno de estos grupos activo, se mantiene el recorrido habitual: los ejercicios sin tarea no se agregan a la lista general.
6. «Revisar en grupo» no se ofrece dentro de estos grupos. La aprobación humana de este recorrido es explícita sobre el ejercicio que se revisa.
7. La ayuda visible explica que «Todos los jueces lo aprueban» aún requiere la aprobación de José, «Vos lo aprobás» tiene la aprobación de José pero no la de todos los jueces al día, y «Preparados para entregar» ya tiene ambas. Un grupo vacío informa que no tiene ejercicios.
8. «Preparados para entregar» expresa la condición de revisión del ejercicio; no significa que la tanda ya haya sido entregada o publicada.

Una decisión humana parcial, mixta o rechazada no constituye aprobación completa. Si los jueces cumplen el criterio estricto, el ejercicio sigue en «Todos los jueces lo aprueban» y muestra su decisión humana real, sin presentarla como aprobada.

</details>

<a id="F-NADIE-R003"></a>
### Rule[F-NADIE-R003] - Las opiniones incompletas tienen un recorrido propio
**Severity**: normal | **Validation**: VALIDATED

> «Faltan opiniones» conserva los ejercicios elegibles cuya aprobación por los jueces está incompleta y que aún no tienen aprobación humana vigente; permite agruparlos por quién falta y por qué. Si José ya los aprobó, pertenecen a «Vos lo aprobás» y conservan visibles sus opiniones incompletas.

<details><summary>Detalle</summary>

Con «Faltan opiniones» activo, el navegador muestra «Quién falta», con una fila por combinación de jueces sin aprobación vigente y cuántos ejercicios tiene cada una:

1. Cada fila nombra a cada juez y el motivo: «Falta Calidad didáctica A1», «Miniteoría con error opinó con la versión del 5/10» o «Pragmatismo comunicativo se abstuvo (no aplica)». Dos versiones de antes distintas son dos filas.
2. Las filas no se pisan: cada ejercicio está en una sola, y suman el total de «Faltan opiniones».
3. Tocar una fila acota la lista; tocarla otra vez quita esa selección.
4. Se ordenan de la más numerosa a la menos numerosa.
5. La anterior fila «Opinaron todos los jueces» deja de ser una subdivisión de pendientes: sus ejercicios se distribuyen entre los dos grupos de aprobación según la decisión humana.
6. La ausencia de un juez no aplicable no se considera una opinión pendiente. Una abstención de un juez aplicable sí lo es, cualquiera sea el motivo informado.

Se puede seguir leyendo lo que dijo cada juez y anotando Problemas en este recorrido. Completar y guardar la aprobación humana traslada el ejercicio a «Vos lo aprobás», sin ocultar las opiniones que todavía faltan. Una opinión pendiente, de antes o una abstención impide presentarlo como aprobado por todos o preparado para entregar.

</details>

<a id="F-NADIE-R004"></a>
### Rule[F-NADIE-R004] - Mostrar por separado lo que dijeron los jueces y la decisión humana
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada ejercicio muestra su grupo y la corrección revisada, lo que dijo cada juez aplicable y la decisión humana guardada. «Sin error» describe una opinión del juez; no se presenta como una aprobación de José.

<details><summary>Detalle</summary>

El bloque superior usa el nombre del grupo actual, referido a «Corrección N». Incluye un renglón por juez aplicable, en el orden del catálogo:

1. «dijo: sin error» con la marca «al día», si opinó con su versión de hoy.
2. «dijo: sin error» con la marca ámbar de su versión, como «versión del 5/10», si opinó con una de antes.
3. «se abstuvo» con el motivo informado, como «no aplica» o «falta contexto».
4. «todavía no opinó», con la marca gris de «sin medir».
5. Cuando un juez señala un problema, se muestra lo que dijo y su vigencia; la aprobación humana no lo convierte en «sin error» ni oculta su motivo.

Invariantes:

1. Después de los jueces, una línea nombra los analizadores que lo evaluaron sin observaciones.
2. Cada juez lleva «Ver su examen», que abre su examen en Exámenes, donde se recalibra.
3. Con cambios propuestos, el bloque aclara que las opiniones corresponden al después.
4. Sin cambios, no muestra los hallazgos históricos del plan que el análisis vigente ya no tiene.
5. La aprobación humana se muestra aparte y distingue aprobación guardada, pendiente, parcial o rechazada según corresponda. Mientras se guarda, no afirma que el ejercicio está preparado.
6. Mostrar este bloque no consulta a modelos.

Cuando no hay hallazgos, el renglón del juez dice «sin error» y no inventa una justificación. Los motivos de abstención y las versiones se conservan tal como se informaron.

</details>

<a id="F-NADIE-R005"></a>
### Rule[F-NADIE-R005] - Anotar un problema que ningún juez marcó, aunque no haya cambios
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Desde estos grupos, José puede anotar un Problema no detectado aunque el ejercicio no tenga cambios o tareas, siempre que cumpla la elegibilidad sin problemas detectados de [F-NADIE-R001](#F-NADIE-R001). El caso conserva el ejercicio, la tanda, la corrección revisada y lo que dijo cada juez al anotar, para poder revisar a quienes no detectaron el problema.

<details><summary>Detalle</summary>

El bloque de [F-NADIE-R004](#F-NADIE-R004) ofrece «Anotar un problema», también con **P**, y abre el cajón habitual para elegir un Problema o crear uno. El caso conserva:

1. El ejercicio y la tanda.
2. El estado de origen y su nombre, como «Corrección 1».
3. El ejercicio congelado en ese estado. Sin cambios, antes y después son iguales.
4. La opinión de cada juez aplicable: quienes dijeron «sin error», al día o con una versión anterior, son **los que no lo vieron**; quienes se abstuvieron o todavía no opinaron quedan con su estado y motivo, sin contarlos entre quienes no detectaron el problema.
5. Los analizadores que lo evaluaron sin observaciones.

Invariantes:

1. Sin cambios no hay corrección que rechazar: no se ofrece «Rechazar la corrección al anotar». Anotar no cambia por sí solo ninguna decisión y sigue disponible con la tanda publicada. No concede aprobación humana; para retirar una aprobación se utiliza [F-NADIE-R008](#F-NADIE-R008).
2. Antes de anotar, José ve el estado y las opiniones que quedarán en el caso.
3. Las opiniones se toman del análisis vigente al anotar. Si el ejercicio ya no cumple la elegibilidad de [F-NADIE-R001](#F-NADIE-R001), no se anota nada y se pide volver a leer. Un cambio de grupo por aprobación humana no elimina por sí solo esa elegibilidad.
4. Con cambios, se conserva el caso habitual —antes, después y corrección, que puede rechazarse— y se agregan las opiniones sobre el después.
5. Un ejercicio sin cambios que no es elegible no puede anotarse por este recorrido: faltaría la corrección o la evidencia de lo que dijeron los jueces.
6. Repetir la anotación del mismo ejercicio y contenido en el mismo Problema no duplica el caso. «Deshacer anotación» lo retira.
7. En Problemas, el caso muestra de qué estado sale y quiénes no lo vieron; el pedido del Problema incluye las guías de esos jueces.
8. Anotar no consulta a ningún modelo.

Quien se abstuvo o todavía no opinó no cuenta como juez que se equivocó en este ejercicio. Las opiniones congeladas conservan lo observado al anotar aunque posteriormente cambien las versiones, decisiones o grupos.

</details>

<a id="F-NADIE-R006"></a>
### Rule[F-NADIE-R006] - La aprobación humana debe ser explícita, completa y vigente
**Severity**: critical | **Validation**: VALIDATED

> Con cambios propuestos, José aprueba el ejercicio cuando todas sus correcciones están aprobadas y guardadas; una combinación parcial o rechazada no alcanza. Sin cambios, debe poder aprobar explícitamente el ejercicio y conservar esa decisión para el contenido, la tanda y la revisión que vio.

<details><summary>Detalle</summary>

1. La aprobación de los jueces, abrir el ejercicio o recorrerlo no concede aprobación humana.
2. Con cambios, ninguna corrección pendiente o rechazada puede convivir con la marca de preparado. Aprobar una parte deja el ejercicio pendiente de aprobación completa.
3. Sin cambios, la acción «Aprobar» está disponible también para ejercicios que nunca tuvieron tareas, aunque todavía no todos los jueces aprueben. Su decisión se conserva al salir del ejercicio y al recargar o volver a abrir Correcciones.
4. Una aprobación sin cambios pertenece al ejercicio en esa tanda, revisión y contenido. No se hereda por otra tanda, otra revisión o contenido distinto. En esos casos se exige nueva aprobación humana.
5. La decisión humana y las opiniones de los jueces son independientes: José puede aprobar aunque algún juez señale problemas, esté pendiente, se abstenga o haya opinado con una versión de antes. Esa aprobación completa y guardada pertenece a «Vos lo aprobás»; no se exige unanimidad de jueces para guardarla ni elegibilidad sin problemas detectados para mostrarla. Si después todos los jueces aprueban al día y se cumplen las demás condiciones de preparados, pasa a «Preparados para entregar» sin pedir otra aprobación humana del mismo contenido.
6. Una aprobación guardada antes de abrir este recorrido cuenta si sigue siendo completa y vigente.

</details>

<a id="F-NADIE-R007"></a>
### Rule[F-NADIE-R007] - Aprobar permite continuar mientras se confirma el guardado
**Severity**: critical | **Validation**: VALIDATED

> Al completar la aprobación humana, el ejercicio sale inmediatamente del grupo pendiente de aprobación humana y se muestra «Guardando»; sólo después de confirmarse el guardado cuenta en «Preparados para entregar» o «Vos lo aprobás», según las opiniones vigentes de los jueces. Si falla, recupera su decisión guardada anterior y el grupo correspondiente.

<details><summary>Detalle</summary>

1. La salida de pendientes es provisional durante el guardado: no basta para declarar preparado el ejercicio. Tras confirmar el guardado, ContentAudit vuelve a considerar el análisis vigente, las opiniones y la decisión humana; el ingreso a preparados exige que aún se cumpla todo el criterio.
2. Se actualizan las cantidades, la lista y las cantidades de temas, y la tira de ejercicios sin recargar la página: el grupo de origen refleja la salida inmediata y el destino sólo suma la aprobación confirmada. Mientras se guarda, el ejercicio puede estar provisionalmente fuera de los cuatro grupos.
3. Al salir de pendientes, el recorrido continúa con el siguiente disponible en el orden del curso; si se agotó la selección, lo informa sin dar por guardada la decisión que todavía está en curso.
4. Si falla el guardado, se muestra el error, el ejercicio reaparece en la lista pendiente y se restituyen sus cantidades para poder reintentar. No se conserva una apariencia de aprobación completa que no quedó guardada.
5. Si al guardar siguen existiendo correcciones pendientes o rechazadas, el ejercicio no pasa a preparados.
6. Si la aprobación humana completa quedó guardada pero un juez señala problemas, falta su opinión vigente o se abstuvo, pasa a «Vos lo aprobás», aunque no cumpla la elegibilidad sin problemas detectados. No se presenta como preparado para entregar.
7. Si cambian las opiniones de los jueces manteniéndose vigente la aprobación humana, el ejercicio se reclasifica entre «Vos lo aprobás» y «Preparados para entregar» según corresponda. Se actualizan contadores, temas y recorrido sin duplicarlo ni exigir recargar la página.

</details>

<a id="F-NADIE-R008"></a>
### Rule[F-NADIE-R008] - Reabrir devuelve el ejercicio a revisión humana
**Severity**: critical | **Validation**: VALIDATED

> Al reabrir o deshacer su aprobación, José retira inmediatamente el ejercicio de «Preparados para entregar» o «Vos lo aprobás» y ve el indicador global «Guardando». Tras confirmarse el guardado, el ejercicio vuelve al grupo pendiente que corresponda por las opiniones de los jueces o al recorrido habitual si tiene problemas señalados.

<details><summary>Detalle</summary>

1. Sin cambios, reabrir retira la aprobación explícita del ejercicio. Con cambios, deshacer una aprobación o dejar una corrección pendiente o rechazada elimina la aprobación completa.
2. El grupo de origen refleja inmediatamente la salida del ejercicio y el recorrido continúa. La nueva decisión sólo se presenta como guardada al confirmarse; desde entonces se conserva al recargar o volver a abrir la revisión. Las cantidades, temas y recorrido siguen el criterio de [F-NADIE-R007](#F-NADIE-R007).
3. Si todos los jueces aprueban al día y cumple [F-NADIE-R001](#F-NADIE-R001), vuelve a «Todos los jueces lo aprueban». Si cumple esa elegibilidad pero faltan opiniones, el destino es «Faltan opiniones». Si tiene problemas señalados o ya no cumple esa elegibilidad, deja estos grupos y permanece accesible en el recorrido habitual de Correcciones.
4. Si no puede guardarse la reapertura, se informa el error y se restituye la decisión guardada anterior con el grupo que le corresponda según las opiniones vigentes, sin afirmar que quedó reabierto.
5. Para volver a preparados hace falta una nueva aprobación humana completa y guardada, además de todas las opiniones aplicables al día.

</details>

## Contexto

El recorrido del 20/10 reunía bajo «Nadie le ve problemas» a ejercicios que ningún analizador ni juez había marcado, aunque aún faltaran opiniones. Permitía revisar los ejercicios sin tareas, ver qué juez faltaba y anotar problemas que los jueces no habían encontrado. «Lo dan por bueno» seguía describiendo cambios resueltos y no reemplazaba ese recorrido por ejercicios.

El 21/10 José pidió distinguir «ningún juez ve problema» de «yo tampoco veo problema» y confirmó: **«Sí, todos los jueces y yo»**. Después pidió además **«vos lo aprobás» (pero no todos los jueces)**. La elegibilidad original se conserva para el recorrido sin problemas detectados y para su evidencia de Problemas; la aprobación humana tiene un grupo independiente que también admite disconformidad de jueces. Los ejercicios pendientes, con abstenciones, con opiniones de antes o con problemas señalados nunca se anuncian como aprobados por todos sólo porque José los haya aprobado.

## Alcance

- Clasificación, lectura, aprobación humana y reapertura de ejercicios, con o sin cambios y con o sin tareas previas. «Vos lo aprobás» incluye toda aprobación humana completa y vigente sin unanimidad vigente de jueces, aun fuera del recorrido sin problemas detectados.
- Conservación del recorrido de opiniones incompletas y de la evidencia congelada al anotar Problemas.
- La clasificación no ejecuta nuevas evaluaciones ni entrega o publica la tanda.

## Recorridos de usuario

```yaml
journeys:
  - id: F-NADIE-J001
    name: José revisa lo que dijeron los jueces y anota un problema
    flow:
      - id: abrir
        action: "José abre Correcciones de ContentAudit y elige uno de los cuatro grupos"
        gate: [F-NADIE-R001, F-NADIE-R002]
        outcomes:
          - when: "Hay análisis vigente de la corrección y ejercicios en el grupo"
            then: leer
          - when: "No hay análisis vigente de la corrección"
            then: apagado
          - when: "Hay análisis vigente, pero el grupo no tiene ejercicios"
            then: vacio
      - id: apagado
        action: "Los grupos están deshabilitados y explican por qué"
        result: failure
      - id: vacio
        action: "ContentAudit informa que el grupo no tiene ejercicios"
        result: success
      - id: leer
        action: "José abre un ejercicio y lee las opiniones, sus faltantes y la decisión humana por separado"
        gate: [F-NADIE-R003, F-NADIE-R004]
        then: anotar
      - id: anotar
        action: "José anota un Problema en el ejercicio, tenga o no cambios"
        gate: [F-NADIE-R005]
        outcomes:
          - when: "El ejercicio sigue siendo elegible al anotar"
            then: registrado
          - when: "El ejercicio no cumple la elegibilidad para anotar un problema no detectado"
            then: releer
      - id: releer
        action: "No se anota el caso y ContentAudit pide volver a leer"
        result: failure
      - id: registrado
        action: "El Problema conserva el ejercicio, la tanda, el estado y las opiniones; cada juez mantiene acceso a su examen"
        gate: [F-NADIE-R004, F-NADIE-R005]
        result: success
  - id: F-NADIE-J002
    name: José aprueba un ejercicio y continúa la revisión
    flow:
      - id: aprobar
        action: "José aprueba un ejercicio en Correcciones, aprobando todas sus correcciones o el ejercicio sin cambios, aunque los jueces no sean unánimes"
        gate: [F-NADIE-R006, F-NADIE-R007]
        outcomes:
          - when: "La aprobación completa solicitada todavía se está guardando"
            then: guardando
          - when: "La aprobación completa queda guardada, todos los jueces aprueban al día y se cumplen las condiciones de preparados"
            then: preparado
          - when: "Quedan correcciones pendientes o rechazadas"
            then: incompleto
          - when: "No puede guardarse la aprobación"
            then: error_guardado
          - when: "La aprobación completa queda guardada, pero algún juez señala problemas, está pendiente, se abstuvo u opinó con una versión de antes"
            then: solo_humano
      - id: guardando
        action: "El ejercicio sale del grupo pendiente, se continúa con el siguiente disponible o se informa el fin, y el indicador global dice «Guardando» sin sumarlo todavía a preparados ni a «Vos lo aprobás»"
        result: success
      - id: preparado
        action: "La aprobación queda confirmada, el ejercicio aparece en «Preparados para entregar» y se actualizan sus cantidades"
        result: success
      - id: incompleto
        action: "El ejercicio sigue sin preparación para entregar y muestra la decisión humana incompleta"
        result: success
      - id: error_guardado
        action: "Se muestra el error, se restituye la decisión guardada anterior, el ejercicio vuelve al recorrido de origen y se restituyen sus cantidades"
        result: failure
      - id: solo_humano
        action: "El ejercicio aparece sólo en «Vos lo aprobás», con su aprobación humana guardada y las opiniones reales de los jueces visibles; no figura como preparado"
        result: success
  - id: F-NADIE-J003
    name: José reabre un ejercicio que había aprobado
    flow:
      - id: reabrir
        action: "José reabre o deshace su aprobación de un ejercicio de «Preparados para entregar» o «Vos lo aprobás»"
        gate: [F-NADIE-R008]
        outcomes:
          - when: "La reapertura solicitada todavía se está guardando"
            then: guardando
          - when: "La reapertura queda guardada y todos los jueces siguen aprobándolo al día"
            then: pendiente_humano
          - when: "La reapertura queda guardada, el ejercicio es elegible sin problemas detectados y faltan opiniones vigentes"
            then: faltan
          - when: "La reapertura queda guardada y el ejercicio no cumple la elegibilidad sin problemas detectados"
            then: fuera
          - when: "No puede guardarse la reapertura"
            then: error_guardado
      - id: guardando
        action: "El ejercicio deja inmediatamente su grupo de aprobación humana, se continúa el recorrido y el indicador global dice «Guardando» sin afirmar que la reapertura quedó guardada"
        result: success
      - id: pendiente_humano
        action: "Se confirma la reapertura, el ejercicio vuelve a «Todos los jueces lo aprueban» y mantiene la decisión al recargar"
        result: success
      - id: faltan
        action: "El ejercicio queda en «Faltan opiniones» sin aprobación humana vigente"
        result: success
      - id: fuera
        action: "El ejercicio deja los cuatro grupos y permanece accesible en el recorrido habitual de Correcciones"
        result: success
      - id: error_guardado
        action: "ContentAudit informa el error, restituye la decisión guardada anterior y el grupo correspondiente, y no afirma que la reapertura quedó guardada"
        result: failure
```

## Preguntas abiertas

No quedan decisiones abiertas para este cambio. El criterio estricto de «todos los jueces aplicables y José» fue confirmado el 21/10/2026; el mismo día José pidió «Vos lo aprobás» para separar su aprobación cuando no todos los jueces aprueban. Se conserva el criterio de vigencia de opiniones de [F-ALDIA-R003](#F-ALDIA-R003): se utiliza la marca informada por ContentAudit.

## Criterios de aceptación

Los escenarios usan opiniones preparadas para la revisión; no requieren consultas reales a modelos.

| Regla | Resultado observable exigido |
|---|---|
| [F-NADIE-R001](#F-NADIE-R001) | Se conserva la elegibilidad sin problemas detectados, incluidos ejercicios sin tareas. Los hallazgos, analizadores incompletos, ausencia de opiniones al día o contenido distinto de lo medido no se presentan como preparados; tampoco excluyen por sí solos una aprobación humana vigente de «Vos lo aprobás» cuando no todos los jueces aprueban al día |
| [F-NADIE-R002](#F-NADIE-R002) | Los cuatro grupos son excluyentes y sus contadores, temas y flechas reflejan su pertenencia real, también con «Por revisar». Todos los jueces sin aprobación humana queda pendiente; aprobación humana sin todos los jueces queda en «Vos lo aprobás»; ambas aprobaciones vigentes quedan en preparados. «Vos lo aprobás» incluye ejercicios con problemas señalados y no se limita al universo sin hallazgos |
| [F-NADIE-R003](#F-NADIE-R003) | Pendientes, abstenciones y opiniones de antes sin aprobación humana permanecen en «Faltan opiniones» si cumplen la elegibilidad; con aprobación humana completa y guardada pasan a «Vos lo aprobás». Las combinaciones detalladas no se pisan y suman «Faltan opiniones». Los jueces no aplicables no generan faltantes |
| [F-NADIE-R004](#F-NADIE-R004) | Se muestran las opiniones reales, sus marcas, motivos y acceso al examen; los analizadores sin observaciones y la decisión humana aparecen separados. Con cambios se identifica el después; sin cambios no se recuperan hallazgos históricos del plan |
| [F-NADIE-R005](#F-NADIE-R005) | Se conserva la anotación con o sin cambios, la comprobación de elegibilidad vigente, el ejercicio congelado, las opiniones y la distinción entre quienes no lo vieron y quienes no opinaron. No se duplican casos; deshacer los retira. Sin cambios no se ofrece rechazar una corrección y anotar no altera por sí solo decisiones |
| [F-NADIE-R006](#F-NADIE-R006) | Todas las correcciones deben estar aprobadas y guardadas; estados mixtos, parciales o rechazados no preparan. Sin cambios, se puede aprobar aunque los jueces no aprueben todos; la decisión se conserva al recargar y no se hereda a otro contenido, tanda o revisión |
| [F-NADIE-R007](#F-NADIE-R007) | Solicitar la aprobación completa retira el ejercicio al instante de pendientes, actualiza sus cantidades y navega al siguiente o informa el fin con «Guardando» visible. «Preparados para entregar» y «Vos lo aprobás» sólo suman después de confirmar el guardado, según los jueces. Si falla, se restituye la decisión guardada y sus cantidades. Al completarse o perderse la unanimidad vigente de jueces se reclasifica sin duplicados ni otra aprobación humana |
| [F-NADIE-R008](#F-NADIE-R008) | Reabrir o deshacer retira inmediatamente el ejercicio de preparados o «Vos lo aprobás» y muestra «Guardando». La confirmación conserva la nueva decisión al recargar y devuelve al grupo correspondiente o al recorrido habitual si tiene problemas. Un fallo restituye la decisión guardada anterior y no se presenta como reapertura exitosa |

## Referencias

- **FEAT-ALDIA** — Define la vigencia de las opiniones y las abstenciones mediante [F-ALDIA-R003](#F-ALDIA-R003) y [F-ALDIA-R005](#F-ALDIA-R005). Se aplica en [F-NADIE-R001](#F-NADIE-R001), [F-NADIE-R003](#F-NADIE-R003) y [F-NADIE-R004](#F-NADIE-R004).
- **FEAT-TALDIA** — Define las marcas visibles en [F-TALDIA-R001](#F-TALDIA-R001) y por qué las opiniones de antes no alcanzan para aprobar en grupo en [F-TALDIA-R006](#F-TALDIA-R006). Se conserva ese criterio en [F-NADIE-R001](#F-NADIE-R001).
