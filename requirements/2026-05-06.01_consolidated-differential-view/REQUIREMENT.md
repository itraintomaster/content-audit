---
feature:
  id: FEAT-CDIFF
  code: F-CDIFF
  name: Estructura consolidada de un analisis con sus propuestas aceptadas y pendientes
  priority: critical
---

# Estructura consolidada de un analisis con sus propuestas aceptadas y pendientes

## TL;DR

**Que**: El CLI de content-audit expone, para cada `AuditReport`, una **estructura consolidada** que combina el baseline del analisis con los efectos de las propuestas aceptadas y pendientes de su plan activo. Para cada nodo afectado del arbol del curso, la salida emite un **mapa de fields cambiados** donde cada entrada lleva tres "fotos" del mismo field a lo largo del stack `original` (baseline) → `consolidated` (con aceptadas) → `pendingProjection` (con aceptadas + pendientes). El conjunto de fields candidatos se descubre **dinamicamente** caminando recursivamente las estructuras del modelo de auditoria y del contenido del nodo: cualquier hoja escalar cuyo valor difiera entre al menos dos fotos aparece como field, sin enumerar tipos de fields posibles en el contrato.

**Por que**: Hoy la salida expone solo un snapshot crudo del nodo afectado y, por estadistica, escalares precomputados. Los diagnosticos tipados que el motor calcula sobre el arbol consolidado y el arbol con pendientes (token count, listas de lemas ausentes, distribucion COCA, etc.) se descartan antes de salir. La forma `field → tripleta` con descubrimiento dinamico corrige ese gap y deja el contrato **extensible sin tocarlo**: cuando aparece un analizador nuevo, un componente nuevo en un registro existente, o un campo nuevo en el contenido del nodo, sus hojas escalares empiezan a participar del diff automaticamente. La defensa contra ruido (timestamps, identificadores opacos, metadatos de persistencia) se hace por exclusiones declaradas a nivel funcional, no por whitelists positivas que requieran mantenimiento por feature.

## Reglas de Negocio

### Grupo A - Identificacion del par activo

<a id="F-CDIFF-R001"></a>
### Rule[F-CDIFF-R001] - El par activo `(auditId, planId)` es observable en la raiz de la salida
**Severity**: critical | **Validation**: VALIDATED

> Cuando el CLI entrega una estructura consolidada construible, su raiz expone los identificadores del par activo `(activeAuditId, activePlanId)` resueltos al momento de servir la peticion. Un consumidor obtiene esos dos identificadores leyendo directamente la raiz, sin tener que inferirlos a partir de timestamps, orden de creacion de los analisis u otra heuristica. Cuando no hay par activo resoluble, no hay estructura consolidada construible y la salida sigue [F-CDIFF-R013](#F-CDIFF-R013).

<details><summary>Detail</summary>

1. La estructura consolidada se construye en funcion del par activo (Grupo C en adelante). Sin par activo, no hay consolidado.
2. Como se persiste o resuelve el par activo (archivo dedicado, parametro de comando, etc.) es decision de arquitectura ([DOUBT-ACTIVE-PERSISTENCE](#DOUBT-ACTIVE-PERSISTENCE)).

</details>

<a id="F-CDIFF-R002"></a>
### Rule[F-CDIFF-R002] - Cambiar el par activo es idempotente y no destructivo
**Severity**: major | **Validation**: VALIDATED

> Despues de invocar la operacion del CLI que cambia el par activo, dos invariantes se observan:
>
> 1. **No destructivo**: ningun `AuditReport`, plan, artefacto de propuesta o archivo del curso fue modificado por la operacion (sus contenidos y timestamps de modificacion permanecen iguales a antes de la invocacion).
> 2. **Idempotente**: invocar la operacion apuntando al par activo ya vigente es un no-op observable: ningun archivo cambia, y la salida posterior al cambio es indistinguible de la anterior.

<details><summary>Detail</summary>

1. La regla habilita que el operador (o un consumidor automatizado) navegue libremente entre analisis -incluido un analisis viejo con sus pendientes dormidas- sin riesgo de alterar el estado del sistema.
2. La forma concreta del verbo CLI (nombre, sintaxis, flags) y de la persistencia es decision de arquitectura. Lo que esta regla obliga es la no-destructividad y la idempotencia **observables** sobre el sistema de archivos.

</details>

---

### Grupo B - Apilado y vigencia de propuestas

<a id="F-CDIFF-R003"></a>
### Rule[F-CDIFF-R003] - Un analisis sin decisiones nuevas no marca nodos como afectados
**Severity**: critical | **Validation**: VALIDATED

> Para un `AuditReport` cuyo plan activo no contiene propuestas con veredicto `APPROVED` ni `PENDING_APPROVAL` aplicables, la estructura consolidada de ese analisis emite la lista de nodos afectados **vacia**. No hay fields cambiados que reportar porque no hay decisiones que produzcan diferencia entre las tres fotos.

<details><summary>Detail</summary>

1. La consecuencia practica es que un `AuditReport` recien creado, antes de cualquier decision sobre su plan, devuelve un consolidado vacio (lista de afectados vacia, `pendingApplicability` vacio). El consumidor distingue ese caso del `consolidatedAvailability: UNAVAILABLE` por la simple presencia de la raiz construida.
2. Esta regla depende de la propiedad heredada de [F-REVAPR-R011](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R011): las propuestas aceptadas en analisis anteriores ya estan materializadas en el curso al momento de correr el analisis nuevo, por lo que reaparecen como contenido normal del baseline en lugar de generar nuevas decisiones sobre el plan nuevo.
3. La equivalencia post-stack entre el baseline del analisis nuevo y la foto `consolidated` de los fields del anterior es una propiedad emergente del sistema (motor deterministico + materializacion de aceptadas) y no un contrato que el consolidador pueda enforced por si mismo. La regla testeable propia de FEAT-CDIFF es la primera oracion: sin decisiones aplicables, lista de afectados vacia.

</details>

<a id="F-CDIFF-R004"></a>
### Rule[F-CDIFF-R004] - Las pendientes dormidas se recuperan al apuntar el par activo a su analisis original
**Severity**: critical | **Validation**: VALIDATED

> Una propuesta con veredicto `PENDING_APPROVAL` perteneciente al plan de un analisis anterior es observable en la estructura consolidada **si y solo si** el par activo apunta a ese `(auditId, planId)` original. Dos invariantes derivadas:
>
> 1. **Persistencia de la pendiente dormida**: crear un `AuditReport` nuevo no borra ni migra las pendientes de planes anteriores: conservan su `proposalId`, su `planId`, su `sourceAuditId` y su veredicto en el sistema de archivos.
> 2. **Recuperacion por seleccion del activo**: cambiar el par activo a `(auditId_anterior, planId_anterior)` y pedir la estructura consolidada hace aparecer las pendientes dormidas como contribuciones a la foto `pendingProjection` de los fields tocados; volver a cambiar el activo a un analisis distinto las hace desaparecer de la salida (sin tocar los archivos donde viven).

<details><summary>Detail</summary>

1. Combinada con [F-CDIFF-R017](#F-CDIFF-R017) (el consolidado se construye sobre el plan activo, no combina planes), esta regla garantiza que pendientes de planes ajenos no contaminen la salida del analisis activo.
2. La forma concreta de almacenamiento de la pendiente y la mecanica de descubrimiento por `(auditId, planId)` son decision de arquitectura. La regla obliga la **observable presencia/ausencia** segun el par activo.

</details>

<a id="F-CDIFF-R005"></a>
### Rule[F-CDIFF-R005] - Las propuestas rechazadas no contribuyen a ninguna foto y no aparecen en trazabilidad
**Severity**: major | **Validation**: VALIDATED

> Para una propuesta con veredicto `REJECTED` ([F-REVAPR-R012](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R012)) en el plan activo, dos invariantes se observan en la salida:
>
> 1. La propuesta no contribuye a ningun valor de la tripleta `(original, consolidated, pendingProjection)` de ningun field, en ningun nodo. La salida emitida es identica a la que se obtendria si esa propuesta no existiera.
> 2. Su `proposalId` no aparece ni en `acceptedProposalIds`, ni en `pendingProposalIds`, ni en `pendingApplicability` de ningun nodo. La estructura consolidada no expone propuestas rechazadas en ninguna seccion.

<details><summary>Detail</summary>

1. La trazabilidad del rechazo (quien rechazo, cuando, por que) se mantiene en el artefacto de la propuesta segun [F-REVAPR-R013](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R013), pero **no es visible en la estructura consolidada**: este contrato describe el estado vigente del curso y los movimientos en curso, no el historial de decisiones.

</details>

---

### Grupo C - Forma uniforme de la salida por nodo afectado

Las reglas de este grupo definen el contrato observable: **cada nodo afectado expone un mapa de fields cambiados, y cada entrada del mapa lleva la tripleta `(original, consolidated, pendingProjection)` de ese field**. Que cuenta como un field y como se descubren se rige por el Grupo D ([F-CDIFF-R019](#F-CDIFF-R019), [F-CDIFF-R020](#F-CDIFF-R020), [F-CDIFF-R023](#F-CDIFF-R023)).

<a id="F-CDIFF-R006"></a>
### Rule[F-CDIFF-R006] - Cada nodo afectado expone un mapa de fields cambiados
**Severity**: critical | **Validation**: VALIDATED

> Para cada nodo del arbol del curso (hoja o padre) cuyo subarbol contiene **al menos un field cuyo valor difiere entre al menos dos de las tres fotos** (`original`, `consolidated`, `pendingProjection`), la estructura consolidada incluye una entrada con la identidad del nodo (tipo de nodo + identificador) y un mapa donde cada clave identifica un field y el valor es la tripleta `(original, consolidated, pendingProjection)`. Tres invariantes derivadas:
>
> 1. **Solo fields con cambio**: el mapa contiene exclusivamente fields cuyas tres fotos no son todas iguales. Un field cuyas tres fotos coinciden no se emite.
> 2. **Forma uniforme**: todos los fields del mapa se serializan bajo la misma estructura clave-valor; el consumidor recorre el mapa con un unico iterador y no necesita ramas separadas por origen del field.
> 3. **Nodo sin cambios no aparece**: un nodo cuyo subarbol no produce ningun field cambiado no aparece en la lista de afectados.

<details><summary>Detail</summary>

1. La presencia del nodo padre en la lista responde a tener al menos un field cambiado propio (un componente re-agregado del subarbol, una estadistica del nivel) o, si el padre no tiene cambios propios, a ser contenedor estructural para hijos afectados; esa decision de incluirlo solo como contenedor es de presentacion JSON y no afecta la semantica testeable de la regla, que opera sobre nodos con fields cambiados.
2. La identidad del nodo (tipo + identificador) y la estabilidad de las claves de los fields permiten al consumidor cruzar la salida con el baseline sin ambigüedad.

</details>

<a id="F-CDIFF-R007"></a>
### Rule[F-CDIFF-R007] - Invariantes de las tres fotos por field
**Severity**: critical | **Validation**: VALIDATED

> Para cada field cambiado de un nodo, la tripleta `(original, consolidated, pendingProjection)` cumple cinco invariantes verificables:
>
> 1. **Fidelidad de `original`**: la foto `original` de un field es **identica** al valor que el motor de auditoria dejo en el baseline del `AuditReport` para ese field y ese nodo. El consolidador **no** la recomputa, **no** la redondea, **no** la deriva: la lee del baseline tal cual y la propaga sin transformacion.
> 2. **Determinacion de `consolidated`**: la foto `consolidated` es el valor que el motor de auditoria produce sobre el subarbol con **solo las propuestas aceptadas** del plan activo aplicadas; FEAT-CDIFF lee ese valor del `AuditReport` correspondiente y lo copia a la foto sin transformacion. Las pendientes no entran en ese calculo; un field cuya foto `consolidated` se obtiene de un calculo donde algun pendiente fue considerado infringe la invariante.
> 3. **Determinacion de `pendingProjection`**: la foto `pendingProjection` es el valor que el motor de auditoria produce sobre el subarbol con **aceptadas y pendientes aplicables** del plan activo aplicadas; FEAT-CDIFF lee ese valor del `AuditReport` correspondiente y lo copia a la foto sin transformacion. Las pendientes marcadas como no aplicables ([F-CDIFF-R012](#F-CDIFF-R012)) no entran en ese calculo.
> 4. **Orden de stacking**: las pendientes se aplican **encima** del estado consolidado, no del original. Si una aceptada precedente desplazo el contenido de un nodo, el `pendingProjection` de un field de ese nodo se construye sobre la foto `consolidated`, no sobre la `original`. Esta invariante es directamente observable en el caso "aceptada y pendiente sobre el mismo `nodeId`": la pendiente debe partir del `elementBefore = consolidated`, no del `elementBefore = original`.
> 5. **Las tres fotos provienen del motor, no de FEAT-CDIFF**: las invariantes 1-3 aplican igual a nodos hoja y nodos padre. Para un padre, FEAT-CDIFF lee el valor que el motor dejo en el `AuditNode` correspondiente del `AuditReport` re-corrido sobre el arbol respectivo y lo propaga a la foto, sin recomputar ni reagregarlo. Como el motor decide ese valor (estrategia de agregacion del analizador) es alcance del propio analizador, **no** de este contrato; un test que verifique "FEAT-CDIFF no recomputa" puede afirmar que la foto del padre es identica al valor que el `AuditReport` correspondiente expone para ese nodo.
>
> Las cinco invariantes son **observables sobre la tripleta emitida**: dado un baseline, un plan activo y los `AuditReport` que el motor produce sobre los arboles modificados, un test puede construir el escenario y verificar la igualdad / inecuacion correspondiente sobre cada foto de cada field.

<details><summary>Detail</summary>

1. Glosario de las tres fotos como apoyo al lector (no es el statement testeable; las invariantes 1-4 lo son):

   | Foto | Significado |
   |------|-------------|
   | `original` | Valor del field en el baseline del `AuditReport`, tal como el motor lo computo cuando se escribio el analisis. |
   | `consolidated` | Valor del field con las propuestas aceptadas del plan activo aplicadas al subarbol y los analizadores re-corridos / re-agregados sobre ese estado. |
   | `pendingProjection` | Valor del field con aceptadas + pendientes aplicables del plan activo aplicadas al subarbol y los analizadores re-corridos / re-agregados. |

2. Casos comunes de la tripleta (consecuencia de las cuatro invariantes, no una invariante adicional):

   | Caso | `original` | `consolidated` | `pendingProjection` |
   |------|------------|----------------|---------------------|
   | Sin aceptada, sin pendiente que cambie el field | (no se emite el field) | | |
   | Sin aceptada, con pendiente que lo cambia | valor baseline | igual a `original` | valor con la pendiente aplicada |
   | Con aceptada que lo cambia, sin pendiente | valor baseline | valor post-aceptada | igual a `consolidated` |
   | Con aceptada y pendiente que lo cambian | valor baseline | valor post-aceptada | valor post-aceptada-y-pendiente |

3. Cuando `pendingProjection` coincide con `consolidated` (no hay pendiente que cambie ese field) la salida puede emitir las tres fotos por uniformidad o omitir la tercera; ambas decisiones son admisibles a nivel de presentacion siempre que el consumidor pueda distinguir "no hay pendiente" de "pendiente con valor identico al consolidado". El contrato funcional obliga la **disponibilidad** de la informacion, no su forma exacta de serializacion.
4. La invariante 1 (fidelidad de `original`) es lo que permite al consumidor confiar en que `consolidated - original` y `pendingProjection - consolidated` son comparables a lo que veria leyendo el baseline directamente: si el consolidador re-procesara `original`, los deltas computables de [F-CDIFF-R010](#F-CDIFF-R010) podrian no coincidir con la lectura directa del `AuditReport`.
5. Las invariantes 2 y 3 distinguen claramente dos calculos independientes del motor sobre dos arboles modificados distintos. La consecuencia practica es que `consolidated` y `pendingProjection` **no son derivables uno del otro** sin volver a correr el motor; cada uno proviene de su propia ejecucion sobre su arbol respectivo.
6. La invariante 5 cierra explicitamente el limite de scope: cualquier "como se agrega" (promedio, suma, acumulacion por banda, etc.) es alcance del motor de auditoria y de las features de cada analizador (p.ej. `F-COCA-R029` de FEAT-COCA, las reglas de FEAT-DSLEN para sentence-length). Si el motor produjera un valor incorrecto en un padre, lo que falla es la regla del analizador correspondiente; FEAT-CDIFF se limita a copiar el valor que el motor dejo en el `AuditReport` y no asume responsabilidad sobre como se calculo.

</details>

---

### Grupo D - Descubrimiento dinamico de fields

<a id="F-CDIFF-R019"></a>
### Rule[F-CDIFF-R019] - Cualquier hoja escalar con cambio entre fotos aparece como field
**Severity**: critical | **Validation**: VALIDATED

> Para producir el mapa de un nodo, el sistema **camina recursivamente** todos los valores estructurados expuestos por el modelo del nodo (registros tipados de diagnostico que el motor produjo, contenido del nodo, mapas de scores, listas anidadas) en cada una de las tres fotos. Cualquier **hoja escalar** (primitivo, string, enum o equivalente sin sub-estructura) cuyo valor difiera entre al menos dos fotos aparece como un field del mapa, con clave derivada del path al que se llego en el recorrido y la tripleta `(original, consolidated, pendingProjection)` correspondiente. El descubrimiento es **dinamico**: el conjunto de fields candidatos no esta enumerado en este requerimiento ni en ningun otro lado del contrato. Tres invariantes derivadas:
>
> 1. **Cobertura por construccion**: si una estructura accesible al recorrido contiene en una de sus fotos una hoja escalar con valor distinto al de la misma hoja en otra foto, esa hoja aparece como field del mapa. La regla testeable es: un test que construye un escenario con una hoja con valor distinto entre fotos puede afirmar que el field correspondiente aparece, sin tener que registrar previamente la existencia de ese field en ningun catalogo.
> 2. **Aparicion automatica de cosas nuevas**: introducir un analizador nuevo (con un registro de diagnostico nuevo y componentes nuevos), un componente nuevo en un registro de diagnostico existente, o un campo nuevo en el contenido del nodo, hace aparecer sus hojas escalares como fields del mapa **sin requerir cambios en este requerimiento ni en el motor de generacion del consolidado**. La cobertura es por construccion del recorrido, no por enumeracion.
> 3. **Agnostico a la dimension que origino la propuesta**: el recorrido **no filtra** hojas por la dimension del analizador que origino una propuesta. Si la aplicacion de una propuesta de la dimension X mueve hojas escalares de la dimension Y o Z (o de cualquier otra parte del modelo) tras correr el motor, esas hojas aparecen como fields con sus tripletas. Reciprocamente, ninguna dimension reportada por el baseline queda oculta en el consolidado porque no fue la dimension que origino la propuesta. La regla testeable: dada una propuesta cuyo origen funcional es la dimension X y cuyo `elementAfter` provoca cambios sobre hojas alcanzables en cualquier dimension presente en el baseline (no solo X), todas esas hojas aparecen en el mapa de fields cambiados del nodo correspondiente.

<details><summary>Detail</summary>

1. La regla cierra el problema historico que [DOUBT-FIELD-IDENTITY](#DOUBT-FIELD-IDENTITY) habia dejado y reemplaza la version anterior basada en "tres clases enumeradas" (diagnosticos tipados / contenido crudo whitelist / estadisticas escalares). Esa version requeria mantenimiento por feature y se rompia con cualquier analizador nuevo.
2. Las **estructuras anidadas** se gobiernan por [F-CDIFF-R023](#F-CDIFF-R023) (path estable como clave del field). Las **listas con identidad** se gobiernan por [F-CDIFF-R022](#F-CDIFF-R022) (diff por elemento usando clave natural).
3. Las **exclusiones por rol funcional** que evitan emitir fields ruidosos (timestamps, identificadores opacos, metadatos de persistencia, etc.) se gobiernan por [F-CDIFF-R020](#F-CDIFF-R020). Esta regla R019 obliga la disciplina del recorrido **completo** modulo R020.
4. La invariante 3 preserva el espiritu de **paridad estructural** entre baseline y consolidado: cualquier dimension que el baseline expone permanece observable en el consolidado a traves del descubrimiento dinamico, sin enumerarla en el contrato. Mismo principio que [F-PIPRE-R005](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R005) generalizado al consolidado: una pendiente de `LEMMA_ABSENCE` puede mover hojas de `SENTENCE_LENGTH` (porque cambiar un lema cambia el largo de la oracion) y esos cambios se reflejan automaticamente.
5. Ejemplos ilustrativos del recorrido: en un quiz cubierto por sentence-length, las hojas alcanzables incluyen el texto de la oracion (string) y los componentes de `SentenceLengthDiagnosis` (`tokenCount`, `targetMin`, `targetMax`, `cefrLevel`, `delta`, `toleranceMargin`); cualquiera que difiera entre fotos aparece como field. En un milestone cubierto por lemma-absence, las hojas alcanzables incluyen los componentes de `LemmaAbsenceLevelDiagnosis` (`absencePercentage`, `totalExpected`, `totalAbsent`, scores ponderados) y los elementos de la lista `absentLemmas` (gobernada por R022). La salida no enumera estas hojas: el recorrido las descubre.

</details>

<a id="F-CDIFF-R020"></a>
### Rule[F-CDIFF-R020] - Exclusiones por rol funcional, no por whitelist de campos
**Severity**: critical | **Validation**: VALIDATED

> Durante el recorrido recursivo de [F-CDIFF-R019](#F-CDIFF-R019), una hoja **no** se emite como field cambiado, aunque su valor difiera entre fotos, si el rol funcional del campo cae en una de estas categorias:
>
> 1. **Identificadores opacos de persistencia** (`_id`, `$oid` y formas equivalentes generadas por el almacenamiento, no por el dominio).
> 2. **Referencias estructurales** que el dominio mantiene para navegacion: identificadores de parent, child, course, milestone, topic, knowledge, quiz cuando aparecen como campo de referencia dentro de otro nodo, no como identidad propia del nodo en cuestion.
> 3. **Timestamps de creacion / actualizacion** de cualquier nivel del modelo.
> 4. **Ordenes internos derivados de la posicion en disco / serializacion**: indices de array de JSON, claves de orden de inserción, contadores que el almacenamiento gestiona.
> 5. **Cualquier valor cuya diferencia entre versiones no represente un cambio semantico observable** para el consumidor (la categoria escape para casos no enumerados arriba).
>
> La invariante observable: dado un escenario donde la unica diferencia entre las fotos de un nodo es un campo en una de las cinco categorias, el mapa de fields cambiados de ese nodo **esta vacio** y el nodo no aparece en la lista de afectados ([F-CDIFF-R006](#F-CDIFF-R006)). Reciprocamente, dado un escenario donde la unica diferencia es una hoja **fuera** de las cinco categorias, esa hoja aparece como field del mapa.

<details><summary>Detail</summary>

1. La regla reemplaza la version anterior basada en whitelist por tipo de nodo. Bajo la version nueva, agregar un analizador o un campo de contenido nuevo no requiere actualizar este requerimiento; agregar una **categoria de exclusion** nueva si lo requiere, pero las cinco arriba cubren los roles ruidosos conocidos.
2. La forma concreta de declarar / detectar la categoria de un campo (anotacion en el modelo, convencion de nombre, lista funcional centralizada en el dominio, inferencia por tipo) es decision de arquitectura.
3. Esta regla coopera con [F-CDIFF-R019](#F-CDIFF-R019) para preservar la **disciplina anti-ruido**: sin las exclusiones, el descubrimiento dinamico arrastraria diffs de timestamps y ordenes triviales que no son cambios semanticos.

</details>

<a id="F-CDIFF-R023"></a>
### Rule[F-CDIFF-R023] - Anidamiento: hojas profundas se identifican por path estable
**Severity**: critical | **Validation**: VALIDATED

> Para una hoja escalar alcanzable a traves de una secuencia de campos anidados desde la raiz del modelo del nodo, el field cambiado correspondiente lleva una clave que codifica esa secuencia y que es **estable** en dos sentidos verificables:
>
> 1. **Estabilidad entre fotos**: la misma hoja, alcanzada por el mismo path en las tres fotos, produce **una sola entrada** en el mapa con la tripleta `(original, consolidated, pendingProjection)` agrupada bajo esa clave. La salida no emite tres entradas separadas (una por foto) ni dos entradas separadas si la hoja aparece en dos fotos.
> 2. **Estabilidad entre nodos del mismo tipo**: dos nodos del mismo tipo donde la misma hoja anidada cambia bajo el mismo path emiten la **misma clave** en sus respectivos mapas. Un consumidor puede correlacionar fields entre nodos comparables sin tener que adivinar variantes del path.
>
> Adicionalmente, la profundidad del anidamiento no esta acotada por el contrato: una hoja a profundidad N produce una unica clave que codifica las N etapas. La forma exacta del separador y de la representacion de los pasos (incluyendo el caso de listas con identidad, gobernado por [F-CDIFF-R022](#F-CDIFF-R022)) es decision de arquitectura.

<details><summary>Detail</summary>

1. La regla evita dos formas de ruido posibles bajo descubrimiento dinamico: emitir el mismo dato bajo dos claves distintas en distintas fotos, y emitir tantos fields como fotos existan en lugar de una sola tripleta.
2. La estabilidad entre nodos habilita al consumidor a, por ejemplo, listar todos los quizzes donde el `tokenCount` del diagnostico de longitud de oracion cambio entre `consolidated` y `pendingProjection`: la consulta es directa porque la clave es la misma en todos los quizzes.
3. La regla no impone una sintaxis de clave concreta (puntos, slashes, brackets para listas, etc.). Lo unico que obliga es la estabilidad observable.

</details>

<a id="F-CDIFF-R022"></a>
### Rule[F-CDIFF-R022] - Listas: diff por elemento si tienen identidad declarada, posicional si no
**Severity**: major | **Validation**: VALIDATED

> Para cada lista alcanzada durante el recorrido de [F-CDIFF-R019](#F-CDIFF-R019):
>
> 1. **Lista con identidad declarada**: si el dominio declara una clave natural de identidad para los elementos de la lista, el diff de la lista se hace **por elemento** usando esa identidad. Cada cambio observable produce un field derivado con clave estable basada en la identidad. Casos: el mismo elemento (misma identidad) presente en mas de una foto con propiedades no-clave distintas; un elemento con identidad presente en alguna foto y ausente en otras.
> 2. **Lista sin identidad declarada**: el diff de la lista se hace **posicionalmente**: el elemento en el indice `i` de una foto se compara con el elemento en el indice `i` de otra. Cada hoja escalar dentro del elemento posicional que difiera entre fotos sigue las reglas [F-CDIFF-R019](#F-CDIFF-R019), [F-CDIFF-R020](#F-CDIFF-R020) y [F-CDIFF-R023](#F-CDIFF-R023) del descubrimiento normal.
>
> Como el dominio declara la identidad de una lista (anotacion, convencion, registro centralizado) es decision de arquitectura. La invariante testeable de R022: dado un escenario con una lista cuya identidad esta declarada y un elemento agregado / removido / modificado en propiedad no-clave, los fields derivados aparecen con clave basada en la identidad; dado un escenario con una lista sin identidad declarada y un elemento posicional con hojas distintas, los fields aparecen bajo path posicional. La regla mantiene el diff **deterministico**: dos ejecuciones sobre el mismo input producen el mismo conjunto de fields cambiados.

<details><summary>Detail</summary>

1. Ejemplos ilustrativos (no exhaustivos) de listas conocidas hoy que tienen identidad natural: `AbsentLemma` y `MisplacedLemma` por `(lemma, pos)`; `BucketResult`, `BucketSummary` y `ProgressionAssessment` por `bandName`; `QuarterResult` por `index`; `ImprovementDirective` por `(type, bandName, levelName)`. Una lista nueva con identidad solo necesita declarar su clave en el dominio para que el diff se acomode automaticamente.
2. Una lista sin identidad declarada que cambia de orden entre fotos puede generar diffs posicionales falsos. Si una iteracion futura observa este caso como ruido, la respuesta es declarar identidad natural para esa lista, no agregar una excepcion al contrato.
3. La forma exacta de las claves derivadas para listas con identidad (concatenar la clave natural al path, usar paréntesis, etc.) es decision de arquitectura. La regla obliga la disciplina: clave estable que permite al consumidor identificar de que elemento se habla.

</details>

---

### Grupo E - Politica de deltas

<a id="F-CDIFF-R009"></a>
### Rule[F-CDIFF-R009] - La salida no tiene una seccion paralela de "estadisticas afectadas"
**Severity**: critical | **Validation**: VALIDATED

> La estructura consolidada **no incluye** una seccion separada de "estadisticas afectadas" con campos numericos precomputados (en particular, `acceptedDelta` y `pendingDelta` no son campos del contrato; ver [F-CDIFF-R010](#F-CDIFF-R010)). Cualquier valor de score / estadistica escalar tocado por aceptadas o pendientes se observa exclusivamente como un field mas del mapa del nodo del nivel correspondiente, descubierto por el recorrido de [F-CDIFF-R019](#F-CDIFF-R019), con su tripleta `(original, consolidated, pendingProjection)`. La invariante testeable: dado un escenario donde una dimension de score cambia por efecto de aceptadas o pendientes, el unico lugar de la salida donde ese cambio aparece es el mapa de fields del nodo del nivel; ningun otro contenedor (lista paralela, seccion dedicada, etc.) lo expone.

<details><summary>Detail</summary>

1. Esta regla reemplaza el contrato anterior, donde cada estadistica afectada llevaba sus propios cuatro escalares (`original`, `consolidated`, `acceptedDelta`, `pendingDelta`). Esa forma era una rama paralela del contrato y obligaba al consumidor a tener dos rutas de lectura distintas (snapshots de nodos vs. estadisticas). La forma uniforme las unifica.
2. Como consecuencia directa, los conceptos `acceptedDelta` y `pendingDelta` salen del contrato como fields propios. Quedan disponibles, **derivados**, como diferencias aritmeticas sobre la tripleta del field correspondiente ([F-CDIFF-R010](#F-CDIFF-R010)).

</details>

<a id="F-CDIFF-R010"></a>
### Rule[F-CDIFF-R010] - Los deltas no son parte del contrato; son computables por el consumidor
**Severity**: major | **Validation**: VALIDATED

> Ningun valor "delta" es campo del contrato. La estructura consolidada emite **solo** la tripleta de fotos `(original, consolidated, pendingProjection)` por field cambiado. Cualquier resta o ratio entre fotos es responsabilidad del consumidor, que puede computarlas en su propia capa con la semantica que le convenga.

<details><summary>Detail</summary>

1. La regla evita la ambiguedad clasica entre "5% mas" (relativo: 1.05x el original) y "5 puntos porcentuales mas" (absoluto: original + 5 sobre la misma escala). Sin deltas en el contrato, la decision queda integramente del lado del consumidor.
2. Para cualquier consumidor que quiera reproducir la semantica del contrato anterior, las equivalencias son: `acceptedDelta = consolidated - original`, `pendingDelta = pendingProjection - consolidated`. Las dos restas se hacen sobre la misma escala interna del dominio, sin reescalar.
3. La separacion entre datos del dominio y notacion de presentacion es identica a la de [F-PIPRE-R013](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R013). Aquella regla aplica a la presentacion textual del preview por-propuesta; aqui la analoga corre por cuenta del consumidor del contrato.

</details>

<a id="F-CDIFF-R011"></a>
### Rule[F-CDIFF-R011] - Trazabilidad de propuestas por nodo afectado
**Severity**: major | **Validation**: AUTO_VALIDATED

> Para cada nodo presente en la lista de afectados, la salida adjunta dos listas: `acceptedProposalIds` y `pendingProposalIds`. Cuatro invariantes:
>
> 1. **Cobertura `accepted`**: si una propuesta con veredicto `APPROVED` del plan activo afecta al nodo (porque toca su contenido o el de algun hijo de su subarbol que contribuye a las fotos del nodo), su `proposalId` aparece en `acceptedProposalIds` del nodo. Reciprocamente, ninguna propuesta no aceptada o que no afecta al nodo aparece en esa lista.
> 2. **Cobertura `pending`**: si una propuesta con veredicto `PENDING_APPROVAL` y aplicable del plan activo afecta al nodo, su `proposalId` aparece en `pendingProposalIds` del nodo. Reciprocamente, ninguna propuesta no pendiente, no aplicable o que no afecta al nodo aparece en esa lista.
> 3. **Orden estable en `acceptedProposalIds`**: los `proposalId` aparecen ordenados por `createdAt` ascendente, para que la cadena de aplicacion sea reconstruible.
> 4. **Trazabilidad por nodo, no por field**: la regla obliga la presencia de la lista a nivel de nodo. Que cada `proposalId` se atribuya a un field especifico dentro del nodo no es parte del contrato y es derivable por el consumidor.

<details><summary>Detail</summary>

1. Si un mismo nodo tiene aceptadas que tocan algunos de sus fields y pendientes que tocan otros, ambas listas estan presentes con sus respectivos `proposalId` y el consumidor sabe que decisiones contribuyen al cuadro completo del nodo.
2. Para nodos padre afectados solo por re-agregacion (su contenido propio no se toca, pero alguno de sus hijos si), las listas reflejan las propuestas que tocaron a los hijos contribuyentes: "que decisiones afectaron al subarbol de este padre".
3. La salida puede emitir una lista vacia o omitir el campo cuando no hay aceptadas (resp. pendientes) que afecten al nodo; ambas formas cumplen las invariantes mientras el comportamiento sea uniforme.

</details>

---

### Grupo F - Pendientes no aplicables

<a id="F-CDIFF-R012"></a>
### Rule[F-CDIFF-R012] - Una pendiente no aplicable se marca, se identifica con causa y se excluye de pendingProjection
**Severity**: major | **Validation**: AUTO_VALIDATED

> Si una propuesta pendiente individual no puede aplicarse al construir las proyecciones (porque su `nodeId` no existe en el subarbol vigente, o porque su `elementBefore` ya no coincide con el `consolidated` debido a una aceptada precedente), tres invariantes se observan en la salida:
>
> 1. **Marcado**: la propuesta aparece como una entrada de la lista `pendingApplicability` de la salida, identificada por `proposalId`, con `status: NOT_APPLICABLE` y un `reason` que combina categoria y detalle textual.
> 2. **No contribucion a `pendingProjection`**: la propuesta no contribuye a la foto `pendingProjection` de ningun field, en ningun nodo. Esta es la diferencia testeable con una pendiente aplicable: dado el mismo escenario, removiendo la pendiente no aplicable, las fotos `pendingProjection` de la salida son identicas.
> 3. **Resto del consolidado intacto**: la salida sigue entregando el resto de la estructura (nodos afectados por aceptadas y por pendientes aplicables, trazabilidad, etc.) sin degradarse a `consolidatedAvailability: UNAVAILABLE` por culpa de la pendiente no aplicable.

<details><summary>Detail</summary>

1. La gestion del ciclo de vida de la pendiente (rechazar, regenerar) corresponde a FEAT-REVAPR; este requerimiento solo cubre que el contrato la marque y siga sirviendo el resto de los datos.
2. Esta regla se distingue de [F-CDIFF-R013](#F-CDIFF-R013) en que aca la salida **se entrega** con la pendiente marcada; en R013 la salida entera se considera no-disponible.
3. Las propuestas estructurales (creacion/eliminacion/reordenamiento) **no** caen en R012: son pendientes que el contrato no modela en absoluto y, por estar fuera del alcance de FEAT-CDIFF (ver `## Alcance`), tampoco aparecen en `pendingApplicability` ni contribuyen a las fotos.

</details>

---

### Grupo G - Salida no disponible

<a id="F-CDIFF-R013"></a>
### Rule[F-CDIFF-R013] - Si la estructura consolidada no se puede construir, el CLI emite `consolidatedAvailability: UNAVAILABLE` con causa
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Si por algun motivo el sistema no puede construir la estructura consolidada (no hay par activo, el plan activo no se puede recuperar, una propuesta aceptada referencia un nodo inexistente, la re-agregacion del subarbol falla), la salida del CLI:
>
> 1. **No** se rellena con valores arbitrarios.
> 2. Incluye el campo `consolidatedAvailability: UNAVAILABLE` con un objeto `unavailabilityReason` (categoria enum + detalle string), siguiendo el mismo patron que [F-PIPRE-R009](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R009).
> 3. El baseline del `AuditReport` sigue siendo recuperable por las operaciones existentes del CLI (no esta bloqueado por la falla del consolidado).

<details><summary>Detail</summary>

1. Casos previstos de no-disponibilidad:

   | Categoria | Descripcion |
   |-----------|-------------|
   | `NO_ACTIVE_ANALYSIS` | El curso no tiene par activo seleccionado o el `auditId` activo no existe. |
   | `ACTIVE_PLAN_UNAVAILABLE` | El `planId` activo no se puede ubicar dentro del analisis (referencia rota, archivo ausente). |
   | `INCONSISTENT_PROPOSAL` | Una propuesta aceptada referencia un `nodeId` que no se encuentra en el subarbol al momento de re-agregar. |
   | `REAGGREGATION_FAILED` | El motor no puede recomputar el agregado sobre el subarbol modificado. |
   | `OTHER` | Cualquier otra causa, con detalle textual obligatorio. |

2. La salida con `consolidatedAvailability: UNAVAILABLE` es de primera clase: el consumidor debe poder distinguirla de "consolidado vacio" (cuando si hay par activo pero ninguna propuesta aceptada o pendiente, en cuyo caso el consolidado se emite con la lista de afectados vacia).

**Error**: "Estructura consolidada no disponible para el analisis '<auditId>': <categoria> - <detalle>"

</details>

---

### Grupo I - Limites explicitos del contrato

<a id="F-CDIFF-R015"></a>
### Rule[F-CDIFF-R015] - Servir el consolidado no escribe ningun artefacto persistido
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Despues de invocar la operacion del CLI que entrega la estructura consolidada (haya sido construida con exito o con `consolidatedAvailability: UNAVAILABLE`), tres invariantes se observan sobre el sistema de archivos:
>
> 1. **No se persiste un `AuditReport` nuevo**: ningun archivo de `AuditReport` aparece, cambia de contenido o cambia de timestamp de modificacion como consecuencia de la operacion. La operacion no asigna un `auditId` nuevo.
> 2. **No se modifican planes ni propuestas**: ningun archivo de plan, propuesta o preview por-propuesta de FEAT-PIPRE cambia de contenido o de timestamp como consecuencia de la operacion.
> 3. **No se modifican archivos del curso**: ningun archivo de contenido del curso (course, milestone, topic, knowledge, quiz) cambia de contenido o de timestamp como consecuencia de la operacion.

<details><summary>Detail</summary>

1. La regla preserva la separacion entre **historia oficial** (los `AuditReport` que el motor escribio) y **vista derivada** (lo que el contrato consolidado emite). Es la misma logica de [F-PIPRE-R002](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R002) generalizada al curso entero y a multiples propuestas.
2. La forma exacta en que el sistema construye la salida (calculo on-demand, materializada con cache, etc.) es decision de arquitectura ([DOUBT-MATERIALIZATION](#DOUBT-MATERIALIZATION)). La regla deja explicitamente abierta la posibilidad de caches internos no oficiales; lo que prohibe es escribir cualquiera de los artefactos enumerados arriba como efecto observable.
3. Aceptar o rechazar propuestas no se hace via el contrato consolidado: esas operaciones siguen siendo las de FEAT-REVAPR y operan sobre los artefactos de propuesta.

</details>

<a id="F-CDIFF-R016"></a>
### Rule[F-CDIFF-R016] - El consolidado combina varias propuestas en un documento; los previews por-propuesta de FEAT-PIPRE permanecen accesibles
**Severity**: major | **Validation**: VALIDATED

> Cuando un plan activo contiene multiples propuestas (aceptadas, pendientes, o ambas), dos invariantes se observan:
>
> 1. **Combinacion en un documento**: la estructura consolidada del analisis correspondiente expone los efectos de **todas** esas propuestas en una unica salida, contribuyendo a las fotos `consolidated` (aceptadas) y `pendingProjection` (aceptadas + pendientes aplicables) de los fields tocados. El consumidor obtiene la lectura agregada sin invocar un mecanismo distinto del que usa para una sola propuesta.
> 2. **Preservacion de los previews por-propuesta**: cada `RevisionProposal` que tenia su preview persistido por [F-PIPRE-R001](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R001) sigue accesible a traves del verbo del CLI de FEAT-PIPRE, sin alteracion de contenido ni de timestamp como consecuencia de servir el consolidado (cubierto tambien por [F-CDIFF-R015](#F-CDIFF-R015).2).
>
> Esta regla supersede explicitamente el caso "preview combinado de varias propuestas" que [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) y [DOUBT-BATCH-PREVIEW](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#DOUBT-BATCH-PREVIEW) habian dejado fuera.

<details><summary>Detail</summary>

1. Las dos features no se pisan: el preview por-propuesta es **eager y por-`proposalId`** ([F-PIPRE-R001](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R001)); el consolidado es **derivado al momento de la peticion** ([F-CDIFF-R015](#F-CDIFF-R015)).
2. La regla testea la coexistencia: tras servir el consolidado de un plan con N propuestas, los N previews individuales siguen accesibles y devuelven exactamente lo que devolvian antes.

</details>

<a id="F-CDIFF-R017"></a>
### Rule[F-CDIFF-R017] - Ninguna foto contiene efecto de propuestas de planes ajenos
**Severity**: major | **Validation**: VALIDATED

> Para una estructura consolidada construida sobre el par activo `(activeAuditId, activePlanId)`, las fotos `consolidated` y `pendingProjection` de cualquier field se calculan **exclusivamente** con propuestas cuyo `planId` coincide con `activePlanId`. La invariante testeable: dado un escenario con propuestas pertenecientes a dos planes distintos, el consolidado del analisis cuyo plan es activo es **identico** al consolidado que se obtendria si las propuestas del otro plan no existieran (mismas fotos, mismos `acceptedProposalIds`/`pendingProposalIds`, misma `pendingApplicability`). Reciprocamente, un `proposalId` que pertenece a un plan distinto del activo no aparece en ninguna seccion del consolidado.

<details><summary>Detail</summary>

1. La regla simplifica la semantica: para cada `AuditReport`, hay como mucho un plan activo y un consolidado bien definido.
2. Combinar varios analisis en una sola salida queda fuera de alcance ([DOUBT-MULTI-ANALYSIS-VIEW](#DOUBT-MULTI-ANALYSIS-VIEW)). Una pendiente dormida de un analisis viejo se observa en el consolidado solo si el operador hace activo a ese analisis ([F-CDIFF-R004](#F-CDIFF-R004)).

</details>

---

## Contexto

### Que es content-audit y donde encaja esta feature

Content-audit es un **CLI / backend** que audita cursos de idiomas y produce dos clases de artefactos:
- `AuditReport`: la historia oficial de scores y diagnosticos por nodo.
- `RevisionProposal`: propuestas de cambio sobre nodos especificos, con ciclo de vida `PENDING_APPROVAL` -> `APPROVED` (aplicada al curso) o `REJECTED` (descartada).

Hasta hoy, cuando un operador acepta una propuesta, la unica forma de ver "como queda el curso ahora" reflejado en scores es **correr un nuevo analisis**. Esto produce un nuevo `AuditReport` por aceptacion. Para un plan con decenas o cientos de propuestas y un operador que las decide a goteo, esto significa generar -y despues navegar- decenas o cientos de analisis casi-iguales, cada uno con la misma estructura pero con un nodo cambiado. No escala ni desde computo ni desde la perspectiva de un consumidor que quiere mostrar el cambio acumulado.

### El gap concreto que motiva la forma uniforme `field → tripleta`

La iteracion anterior del contrato exponia, por nodo afectado, dos cosas: un snapshot crudo del contenido (la oracion del quiz, el titulo del knowledge) y, separadamente, una lista de estadisticas escalares con sus propios cuatro campos numericos precomputados. Los **diagnosticos tipados** que el motor calcula sobre el arbol consolidado y el arbol con pendientes -el `tokenCount` que tendria una oracion proyectada con la pendiente, las listas de `AbsentLemma` agregadas / removidas, la nueva distribucion COCA por banda- se computaban en memoria pero se descartaban antes de salir. El consumidor que pedia "cuantos tokens tendria esta oracion con la pendiente" solo veia el texto nuevo y el delta del score, pero no el `tokenCount` proyectado.

Esta feature corrige el gap modelando todo bajo un solo iterador: el nodo afectado expone un mapa, cada entrada del mapa es un field con tres fotos (`original`, `consolidated`, `pendingProjection`), y el consumidor recorre el mapa sin distinguir si esta leyendo un componente de un diagnostico tipado, un campo del contenido del nodo o una estadistica escalar. La forma es **uniforme y extensible**: cuando aparezca un analizador nuevo con diagnosticos nuevos, sus componentes empiezan a aparecer automaticamente como fields del mapa, sin reescribir el contrato.

### El contrato que esta feature introduce

FEAT-CDIFF define el **contrato de datos consolidado** que el CLI emite por cada `AuditReport`. La salida combina, en un unico documento, el baseline del analisis con los efectos de las decisiones (aceptadas y pendientes) del plan activo. Lo central:

- Cada nodo afectado (hoja o padre) lleva un mapa `field → (original, consolidated, pendingProjection)` con los fields que cambiaron entre al menos dos de las tres fotos.
- Tres clases de fields posibles ([F-CDIFF-R019](#F-CDIFF-R019)): componentes de diagnosticos tipados, contenido crudo del nodo (whitelist por tipo), estadisticas escalares por dimension.
- Los deltas no son parte del contrato ([F-CDIFF-R010](#F-CDIFF-R010)); el consumidor los computa con la semantica que prefiera.

A nivel de raiz de la salida, el documento incluye:

| Campo raiz | Significado |
|------------|-------------|
| `activeAuditId` + `activePlanId` | Identificadores resueltos del par activo. |
| `consolidatedAvailability` + `unavailabilityReason` | Marcado UNAVAILABLE + (categoria, detalle) cuando el consolidado no se puede construir. |
| Lista de nodos afectados | Cada nodo lleva tipo, identificador, mapa de fields, `acceptedProposalIds`, `pendingProposalIds`. |
| `pendingApplicability` | Lista de pendientes marcadas como NO aplicables, con causa. |

### Caso de uso del consumidor

El consumidor tipico es una UI que quiere mostrar el curso entero con overlays diferenciales: un quiz cuya oracion cambio por una aceptada y al que un refinador propone un cambio adicional como pendiente, donde la UI quiere mostrar tanto el texto nuevo como cuantos tokens tendria; un milestone donde el `absencePercentage` baja con las aceptadas y bajaria mas con las pendientes, junto con la lista de `AbsentLemma` que efectivamente se eliminarian; un topic con su distribucion COCA actualizada banda por banda.

El consumidor renderiza:
- La presentacion visual (colores, tamanos, posicionamiento, grafos) en su capa, eligiendo como destacar `consolidated` vs `pendingProjection`.
- Los deltas que necesite (`consolidated - original`, `pendingProjection - consolidated`, ratios, porcentajes), computados sobre la tripleta sin que el contrato los precompute.
- La interaccion con el operador (aceptar, rechazar, regenerar), que sigue cayendo en los verbos existentes de FEAT-REVAPR.

Lo que esta feature **define** es el contrato canonico que ese consumidor lee. Lo que **no define** son los detalles de presentacion ni la operativa de aceptar/rechazar.

### Apilado entre analisis sucesivos

El comportamiento de "apilado" es central:

1. Las propuestas aceptadas hasta el momento ya estan **materializadas en el curso** ([F-REVAPR-R011](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R011)). El nuevo analisis las ve como contenido normal ([F-CDIFF-R003](#F-CDIFF-R003)).
2. Las propuestas pendientes del plan anterior **no se migran** al plan nuevo ([F-CDIFF-R004](#F-CDIFF-R004)). Quedan dormidas, recuperables cambiando el par activo.
3. Las propuestas rechazadas **desaparecen** del consolidado ([F-CDIFF-R005](#F-CDIFF-R005)).

La consecuencia es la **equivalencia post-stack**: el baseline del analisis nuevo y la foto `consolidated` de los fields del anterior describen el mismo estado del curso, desde dos angulos. La diferencia es que el nuevo no carga propuestas: empieza limpio. Esta equivalencia es una propiedad emergente del sistema (motor deterministico + materializacion de aceptadas), no un contrato que esta feature enforced de manera directa.

### Generalizacion del impact preview

FEAT-PIPRE responde "que pasaria si aceptara esta propuesta sola" y persiste un preview eager por cada `RevisionProposal` ([F-PIPRE-R001](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R001)). FEAT-CDIFF responde "como esta el curso considerando todo lo aceptado y pendiente del plan activo, en un unico documento" y se construye al momento de servir la salida.

Las dos features conviven: el preview por-propuesta sigue siendo la foto historica del impacto al generar la propuesta ([F-PIPRE-R008](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R008)); el consolidado es la lectura agregada que el consumidor pide para navegar el curso entero. [F-CDIFF-R016](#F-CDIFF-R016) hace explicita esta supersesion.

---

## Alcance

- **En alcance**: Contrato de datos del CLI que expone el consolidado por `AuditReport` con la forma uniforme `field → (original, consolidated, pendingProjection)` por nodo afectado. Descubrimiento dinamico de fields por recorrido recursivo del modelo ([F-CDIFF-R019](#F-CDIFF-R019)) con exclusiones por rol funcional ([F-CDIFF-R020](#F-CDIFF-R020)). Anidamiento con clave de path estable ([F-CDIFF-R023](#F-CDIFF-R023)). Diff de listas por identidad declarada o posicional ([F-CDIFF-R022](#F-CDIFF-R022)). Identificacion del par activo `(auditId, planId)`. Trazabilidad de propuestas (`acceptedProposalIds`, `pendingProposalIds`) por nodo. Marcado de pendientes no aplicables. Marcado de no-disponibilidad. Supersesion explicita de [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) / [DOUBT-BATCH-PREVIEW](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#DOUBT-BATCH-PREVIEW).
- **Fuera de alcance**: Calculo de deltas dentro del contrato ([F-CDIFF-R010](#F-CDIFF-R010)). Enumeracion estatica de los fields posibles del mapa (queda gobernada por el descubrimiento dinamico de [F-CDIFF-R019](#F-CDIFF-R019)). Diff de campos cuyo rol funcional es identidad / referencia / metadato de persistencia ([F-CDIFF-R020](#F-CDIFF-R020)). Decisiones de presentacion del consumidor (colores, tamanos, posicionamiento). Operaciones de aceptar / rechazar / regenerar (siguen siendo de FEAT-REVAPR). Persistencia automatica del consolidado como nuevo `AuditReport` ([F-CDIFF-R015](#F-CDIFF-R015)). Combinacion de varios analisis o planes en una unica salida ([F-CDIFF-R017](#F-CDIFF-R017), [DOUBT-MULTI-ANALYSIS-VIEW](#DOUBT-MULTI-ANALYSIS-VIEW)). Cambios a la forma del `AuditReport` en disco. La sintaxis exacta del verbo CLI, la forma exacta de las claves de fields (incluyendo el separador del path) y la estructura JSON detallada quedan como decision de arquitectura, mientras se respete la semantica de los campos.
- **Propuestas estructurales fuera de alcance**: las propuestas cuyo efecto seria crear, eliminar o reordenar nodos del arbol del curso (en lugar del patron de sustitucion `elementBefore` -> `elementAfter` sobre un `nodeId` existente) **no se modelan** en este contrato: no contribuyen a ninguna foto y no aparecen en `pendingApplicability`. Hereda el scope de [F-PIPRE-R012](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R012) y [F-LAPS-R014](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R014). Hoy todas las propuestas que el sistema produce son sustituciones, por lo que la exclusion es vacuamente verdadera; si una iteracion futura introduce propuestas estructurales, este alcance se revisa.

---

## User Journeys

### Journey[F-CDIFF-J001] - El CLI emite el consolidado del par activo
**Validation**: VALIDATED

El consumidor pide la salida consolidada del CLI. El sistema resuelve el par activo, combina baseline + propuestas del plan activo y entrega el documento, o emite el estado de no-disponibilidad explicito.

```yaml
journeys:
  - id: F-CDIFF-J001
    name: Salida consolidada del CLI
    flow:
      - id: pedir_salida
        action: "El consumidor invoca el verbo del CLI que entrega la estructura consolidada del curso"
        then: resolver_activo
      - id: resolver_activo
        action: "El sistema resuelve el par (activeAuditId, activePlanId)"
        gate: [F-CDIFF-R001]
        outcomes:
          - when: "El par activo se resuelve y el plan se puede recuperar"
            then: combinar_y_emitir
          - when: "No se puede resolver el par activo o el plan"
            then: emitir_no_disponible
      - id: combinar_y_emitir
        action: "El CLI emite la estructura consolidada con la lista de nodos afectados; cada nodo lleva su mapa de fields cambiados con la tripleta (original, consolidated, pendingProjection), su trazabilidad de propuestas, y la lista de pendientes no aplicables"
        gate: [F-CDIFF-R006, F-CDIFF-R007, F-CDIFF-R009, F-CDIFF-R010, F-CDIFF-R011, F-CDIFF-R015, F-CDIFF-R019, F-CDIFF-R020, F-CDIFF-R023]
        result: success
      - id: emitir_no_disponible
        action: "El CLI emite consolidatedAvailability: UNAVAILABLE con unavailabilityReason (categoria + detalle); el AuditReport sigue accesible por las operaciones existentes"
        gate: [F-CDIFF-R013]
        result: success
```

### Journey[F-CDIFF-J002] - Nodo hoja con aceptada y pendiente sobre el mismo nodeId, tripleta completa por field
**Validation**: VALIDATED

Cubre [F-CDIFF-R006](#F-CDIFF-R006), [F-CDIFF-R007](#F-CDIFF-R007), [F-CDIFF-R011](#F-CDIFF-R011), [F-CDIFF-R019](#F-CDIFF-R019), [F-CDIFF-R020](#F-CDIFF-R020) y [F-CDIFF-R023](#F-CDIFF-R023): el quiz tiene una aceptada que cambio su oracion y una pendiente sobre el mismo `nodeId` que la cambiaria de nuevo. El recorrido recursivo descubre las hojas escalares cambiadas (texto de la oracion, componentes de los diagnosticos tipados aplicables al quiz) y las emite como fields del mapa con sus tripletas. Las hojas excluidas por rol funcional (timestamps, identificadores opacos) no aparecen.

```yaml
journeys:
  - id: F-CDIFF-J002
    name: "Nodo hoja con aceptada y pendiente: tripleta por field"
    flow:
      - id: situacion_inicial
        action: "El plan activo tiene una propuesta aceptada que cambio la oracion de un quiz y una propuesta pendiente sobre el mismo nodeId que la cambiaria de nuevo"
        then: pedir_salida_del_nodo
      - id: pedir_salida_del_nodo
        action: "El consumidor pide la estructura consolidada del curso (que incluye al quiz)"
        then: emitir_mapa_fields
      - id: emitir_mapa_fields
        action: "El CLI emite el quiz con un mapa de fields cambiados producido por el recorrido recursivo; las hojas escalares con valor distinto entre fotos (texto de la oracion, componentes de diagnostico tipado como tokenCount) aparecen con clave de path estable y tripleta original/consolidated/pendingProjection; las hojas excluidas por rol funcional (timestamps, identificadores opacos) no aparecen; la pendiente se aplica sobre el consolidated, no sobre el original"
        gate: [F-CDIFF-R006, F-CDIFF-R007, F-CDIFF-R019, F-CDIFF-R020, F-CDIFF-R023]
        then: emitir_trazabilidad
      - id: emitir_trazabilidad
        action: "El CLI emite acceptedProposalIds con el proposalId aceptado y pendingProposalIds con el proposalId pendiente que afectan al nodo"
        gate: [F-CDIFF-R011]
        result: success
```

### Journey[F-CDIFF-J003] - Padre con aceptadas y pendientes en su subarbol, descubrimiento dinamico de hojas y diff de listas
**Validation**: VALIDATED

Cubre [F-CDIFF-R006](#F-CDIFF-R006), [F-CDIFF-R007](#F-CDIFF-R007), [F-CDIFF-R019](#F-CDIFF-R019) y [F-CDIFF-R022](#F-CDIFF-R022): un milestone con aceptadas y pendientes en sus quizzes. El milestone aparece en la lista de afectados; el recorrido recursivo descubre las hojas escalares cuyas fotos `consolidated` y `pendingProjection` (producidas por el motor sobre el arbol con-aceptadas y con-aceptadas-y-pendientes, R007 invariantes 2 y 3) difieren del baseline, sin filtrar por la dimension del analizador que origino una propuesta (invariante 3 de R019). Si alguna estructura es una lista con identidad declarada, los elementos agregados / removidos / con propiedad cambiada se diff-ean por identidad y aparecen como fields derivados.

```yaml
journeys:
  - id: F-CDIFF-J003
    name: Padre con descubrimiento dinamico y diff de listas con identidad
    flow:
      - id: situacion_inicial
        action: "Un milestone tiene aceptadas aplicadas en algunos quizzes del subarbol y pendientes en otros; el motor produjo los valores re-agregados del milestone para los arboles original, con-aceptadas y con-aceptadas-y-pendientes"
        then: emitir_mapa_padre
      - id: emitir_mapa_padre
        action: "El CLI emite el milestone con un mapa de fields cambiados; las hojas escalares cuyas fotos difieren entre al menos dos arboles aparecen como fields con sus tripletas, sin que este contrato decida como se agrega (la estrategia vive en el motor)"
        gate: [F-CDIFF-R006, F-CDIFF-R007, F-CDIFF-R019]
        then: decidir_diff_listas
      - id: decidir_diff_listas
        action: "El sistema clasifica las listas alcanzadas por el recorrido segun si el dominio les declara identidad natural"
        outcomes:
          - when: "Una lista tiene identidad declarada y elementos cambiados entre fotos"
            then: emitir_fields_por_elemento
          - when: "Las listas alcanzadas no presentan cambios entre fotos o no son relevantes para este milestone"
            then: emitir_fields_escalares
      - id: emitir_fields_por_elemento
        action: "El CLI emite, por cada cambio observado en la lista (elemento agregado, removido, o con propiedad no-clave cambiada), un field derivado con clave estable basada en la identidad declarada del elemento; la lista entera no se trata como un solo valor opaco"
        gate: [F-CDIFF-R022]
        result: success
      - id: emitir_fields_escalares
        action: "El CLI emite los fields escalares con sus tripletas; las hojas cuyas tres fotos coinciden no se emiten"
        gate: [F-CDIFF-R006]
        result: success
```

### Journey[F-CDIFF-J004] - El contrato emite la tripleta y deja los deltas al consumidor
**Validation**: VALIDATED

Cubre [F-CDIFF-R009](#F-CDIFF-R009), [F-CDIFF-R010](#F-CDIFF-R010) y [F-CDIFF-R019](#F-CDIFF-R019): el consumidor lee, para una hoja escalar de score que cambio por efecto de las decisiones, el field correspondiente con su tripleta. El contrato no incluye `acceptedDelta` ni `pendingDelta`; el consumidor los computa por su cuenta como diferencias aritmeticas sobre la tripleta. El descubrimiento es agnostico a la dimension que origino la propuesta (invariante 3 de R019).

```yaml
journeys:
  - id: F-CDIFF-J004
    name: Hoja escalar de score como field con tripleta; deltas computables por el consumidor
    flow:
      - id: pedir_estadisticas
        action: "El consumidor recorre el mapa de fields de los nodos afectados buscando hojas escalares de score que hayan cambiado"
        then: resolver_par
      - id: resolver_par
        action: "El sistema descubrio, durante el recorrido recursivo, las hojas escalares de score con valor distinto entre fotos, sin filtrar por la dimension que origino la propuesta"
        gate: [F-CDIFF-R009, F-CDIFF-R019]
        outcomes:
          - when: "Hay al menos una hoja escalar de score con cambio entre fotos"
            then: emitir_tripleta
          - when: "Ninguna hoja escalar de score cambio"
            then: omitir_par
      - id: emitir_tripleta
        action: "El CLI emite el field correspondiente con sus tres fotos (original, consolidated, pendingProjection); ningun delta forma parte del contrato y la salida no contiene una seccion paralela de estadisticas afectadas"
        gate: [F-CDIFF-R009, F-CDIFF-R010]
        then: consumidor_computa_deltas
      - id: consumidor_computa_deltas
        action: "El consumidor que necesita acceptedDelta lo computa como consolidated - original sobre la tripleta; el que necesita pendingDelta lo computa como pendingProjection - consolidated; ambas restas son sobre la escala interna del dominio"
        gate: [F-CDIFF-R010]
        result: success
      - id: omitir_par
        action: "El CLI no incluye el par en el mapa de ningun nodo afectado; sigue disponible al leer el baseline directamente"
        gate: [F-CDIFF-R009]
        result: success
```

### Journey[F-CDIFF-J005] - Pendiente no aplicable: la salida la marca y sigue sirviendo
**Validation**: VALIDATED

Cubre [F-CDIFF-R012](#F-CDIFF-R012): cuando una pendiente no se puede aplicar (`nodeId` ausente, `elementBefore` desplazado por una aceptada anterior), el CLI la lista en `pendingApplicability` con causa y la excluye de la foto `pendingProjection` de cualquier field; el resto del consolidado se entrega normalmente.

```yaml
journeys:
  - id: F-CDIFF-J005
    name: Pendiente marcada como no aplicable
    flow:
      - id: situacion_inicial
        action: "El plan activo tiene una pendiente cuyo elementBefore no coincide con la foto consolidated del nodo (porque otra aceptada lo desplazo) o cuyo nodeId no se encuentra"
        then: intentar_aplicar
      - id: intentar_aplicar
        action: "El sistema intenta aplicar la pendiente sobre el subarbol vigente"
        outcomes:
          - when: "La pendiente se puede aplicar"
            then: incluir_en_proyeccion
          - when: "La pendiente no se puede aplicar"
            then: marcar_no_aplicable
      - id: incluir_en_proyeccion
        action: "La pendiente contribuye a la foto pendingProjection de los fields tocados en el nodo correspondiente"
        gate: [F-CDIFF-R007]
        result: success
      - id: marcar_no_aplicable
        action: "El CLI agrega un entry { proposalId, status: NOT_APPLICABLE, reason } en pendingApplicability; la pendiente se excluye del calculo de la foto pendingProjection en cualquier field; el resto del consolidado se entrega normalmente"
        gate: [F-CDIFF-R012]
        result: success
```

### Journey[F-CDIFF-J006] - Cambio de par activo: idempotente y no destructivo
**Validation**: VALIDATED

Cubre [F-CDIFF-R002](#F-CDIFF-R002) y [F-CDIFF-R004](#F-CDIFF-R004): cambiar el par activo no toca ningun artefacto, y la salida posterior corresponde al nuevo par activo, recuperando las pendientes dormidas del plan al que apunta.

```yaml
journeys:
  - id: F-CDIFF-J006
    name: Cambio de par activo y recuperacion de pendientes dormidas
    flow:
      - id: situacion_inicial
        action: "Existe un analisis A con pendientes en su plan y un analisis B mas nuevo cuyo planId esta como activo"
        then: pedir_salida_b
      - id: pedir_salida_b
        action: "El consumidor pide la estructura consolidada (corresponde a B)"
        gate: [F-CDIFF-R001, F-CDIFF-R017]
        then: cambiar_activo_a_a
      - id: cambiar_activo_a_a
        action: "El consumidor invoca la operacion del CLI que cambia el par activo a (auditId de A, planId de A); ningun AuditReport, plan, propuesta o curso se modifica"
        gate: [F-CDIFF-R002]
        then: pedir_salida_a
      - id: pedir_salida_a
        action: "El consumidor pide la estructura consolidada; ahora corresponde a A y las pendientes dormidas de A aparecen como tercera foto pendingProjection en los fields tocados de los nodos afectados"
        gate: [F-CDIFF-R001, F-CDIFF-R004, F-CDIFF-R006, F-CDIFF-R007]
        result: success
```

### Journey[F-CDIFF-J008] - Supersesion explicita del preview combinado de FEAT-PIPRE
**Validation**: VALIDATED

Cubre [F-CDIFF-R016](#F-CDIFF-R016): la salida consolidada cubre el caso de uso "ver impacto agregado de varias propuestas" que [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) y [DOUBT-BATCH-PREVIEW](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#DOUBT-BATCH-PREVIEW) habian dejado fuera. El preview por-propuesta de FEAT-PIPRE sigue produciendose intacto.

```yaml
journeys:
  - id: F-CDIFF-J008
    name: Supersesion del caso de preview combinado
    flow:
      - id: situacion_inicial
        action: "Existen varias propuestas (aceptadas y pendientes) en el plan activo; cada una de ellas tiene su preview eager por-propuesta producido por FEAT-PIPRE al ser generada"
        then: pedir_salida_consolidada
      - id: pedir_salida_consolidada
        action: "El consumidor que querria 'ver el impacto si acepto todas estas pendientes juntas' pide la estructura consolidada en lugar de un preview combinado"
        then: emitir_combinacion
      - id: emitir_combinacion
        action: "El CLI emite una unica salida que combina baseline + aceptadas + pendientes del plan activo bajo la forma uniforme field → (original, consolidated, pendingProjection); los previews por-propuesta de FEAT-PIPRE quedan disponibles como antes (no se borran ni se modifican)"
        gate: [F-CDIFF-R015, F-CDIFF-R016]
        result: success
```

---

## Open Questions

<a id="DOUBT-FIELD-IDENTITY"></a>
### Doubt[DOUBT-FIELD-IDENTITY] - Granularidad del "campo afectado" dentro del nodo
**Status**: RESOLVED (en esta iteracion)

Definir que cuenta como "field" en el mapa por nodo afectado. Opciones consideradas:

- [ ] Opcion A: Granularidad coarse al nivel del nodo entero (no se distingue campo a campo). El consumidor que necesite diff fino lo computa por su cuenta comparando snapshots completos. *Limitacion: descarta los diagnosticos tipados que el motor calcula sobre el arbol con pendientes (token count proyectado, listas de absentLemmas tipadas, etc.); el consumidor solo ve el snapshot crudo y los escalares de score.*
- [ ] Opcion B: La propuesta lleva un descriptor explicito de los campos tocados. *Limitacion: ata el contrato de salida a la forma de la propuesta y obliga a que cada strategy de propuesta declare la lista de campos que afecta. No extensible a fields derivados que el motor calcula a partir del nuevo contenido (token count tras cambiar la oracion no es un "campo de la propuesta").*
- [ ] Opcion C: Tres clases de fields enumeradas en el contrato (diagnosticos tipados / contenido crudo whitelist / estadisticas escalares). *Limitacion: requiere mantenimiento del contrato cada vez que aparece un analizador nuevo o un campo nuevo en el contenido del nodo. La whitelist por tipo de nodo no escala con la cantidad de tipos de cambio que el sistema empieza a producir.*
- [x] Opcion D: **Descubrimiento dinamico**. Un field es cualquier hoja escalar alcanzable por el recorrido recursivo del modelo del nodo cuyo valor difiera entre al menos dos fotos ([F-CDIFF-R019](#F-CDIFF-R019)). Las exclusiones contra ruido se hacen por **rol funcional** (identidad / referencia / metadato de persistencia) en [F-CDIFF-R020](#F-CDIFF-R020), no por whitelist positiva. El anidamiento se gobierna por path estable ([F-CDIFF-R023](#F-CDIFF-R023)) y el diff de listas por identidad declarada o posicional ([F-CDIFF-R022](#F-CDIFF-R022)).

**Answer**: Opcion D. El gap concreto que motivo descartar la opcion C original: requeria modificar este requerimiento cada vez que apareciera un analizador nuevo o un campo nuevo en el contenido del nodo, y la whitelist no expresaba la regla observable real (que es "el motor calculo una hoja distinta entre fotos"). La opcion D delega el "que campo aparece" al recorrido recursivo del modelo y reduce la responsabilidad del contrato a dos invariantes: cualquier hoja escalar con cambio aparece, ningun campo cuyo rol funcional es ruido aparece. Trade-off aceptado por el usuario: el consumidor interpreta hojas crudas (token count, percentage, listas con identidad) en lugar de leer un contrato precocido por dimension; el trabajo del CLI es uniforme y no escala con la cantidad de analizadores.

<a id="DOUBT-PENDING-CONFLICT"></a>
### Doubt[DOUBT-PENDING-CONFLICT] - Que pasa si dos pendientes del plan activo se pisan entre si?
**Status**: RESOLVED

[F-REVAPR-R010](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R010) (no acumular pendientes sobre la misma tarea) cubre el caso comun. Para el caso teorico de "dos pendientes sobre el mismo nodo desde tareas distintas":

- [x] Opcion A: Aplicar las pendientes en orden por `createdAt` ascendente; la segunda en chocar cae en [F-CDIFF-R012](#F-CDIFF-R012) (`elementBefore` no coincide con el `consolidated`) y se marca como NOT_APPLICABLE. No se requiere mecanismo nuevo.
- [ ] Opcion B: Detectar conflictos por adelantado y rechazar todas las pendientes en conflicto.
- [ ] Opcion C: Modelar explicitamente un orden de aplicacion (priority, etc.).

**Answer**: Opcion A. Es coherente con [F-CDIFF-R012](#F-CDIFF-R012) y no requiere ampliar el contrato.

<a id="DOUBT-MATERIALIZATION"></a>
### Doubt[DOUBT-MATERIALIZATION] - On-demand vs cacheada
**Status**: OPEN (decision de arquitectura)

[F-CDIFF-R015](#F-CDIFF-R015) prohibe que la salida escriba un `AuditReport`, pero deja abierto si:

- [ ] Opcion A: Calculo completo on-demand cada vez. Sin caches.
- [ ] Opcion B: Materializacion como artefacto interno (no oficial) invalidado por cambios.
- [ ] Opcion C: Cache parcial (por nodo o por estadistica) invalidado por cambios.

**Answer**: Pendiente. Para el MVP, Opcion A es suficiente. Si se mide costo prohibitivo en cursos grandes (~11.500 quizzes), evolucionar a B/C sin cambiar el contrato observable.

<a id="DOUBT-ACTIVE-PERSISTENCE"></a>
### Doubt[DOUBT-ACTIVE-PERSISTENCE] - Donde vive el par activo
**Status**: OPEN (decision de arquitectura)

[F-CDIFF-R001](#F-CDIFF-R001) y [F-CDIFF-R002](#F-CDIFF-R002) requieren un par activo `(auditId, planId)` consultable y mutable, pero no fijan el storage:

- [ ] Opcion A: Archivo dedicado bajo `.content-audit/active-analysis.json` con `{auditId, planId}`.
- [ ] Opcion B: Campo dentro de un archivo existente (indice de analisis del curso).
- [ ] Opcion C: Parametro de la operacion (sin estado persistido del activo): el consumidor pasa `auditId` y `planId` cada vez que pide la salida.

**Answer**: Pendiente. La regla funcional admite cualquiera mientras se respete idempotencia y no-destructividad. Opcion A se alinea con la simetria de los demas stores filesystem.

<a id="DOUBT-MULTI-ANALYSIS-VIEW"></a>
### Doubt[DOUBT-MULTI-ANALYSIS-VIEW] - Combinar varios analisis en una sola salida (feature.future)
**Status**: OPEN (feature.future)

[F-CDIFF-R017](#F-CDIFF-R017) limita la salida al plan activo de un unico `AuditReport`. Casos de uso anticipados que quedan fuera: cruzar pendientes dormidas de los ultimos N analisis sin cambiar el activo, comparar el `consolidated` de A con el de B. Cruzar planes y analisis introduce decisiones de orden de aplicacion entre planes y reglas para resolver propuestas que tocan los mismos nodos en planes distintos. Demasiado rico para esta iteracion.

**Answer**: Pendiente, fuera de alcance. Se trata como feature.future.

---

## ASSUMPTIONS

1. **Existe el concepto de "plan activo" asociado a un `AuditReport` y es uno solo por analisis.** [F-CDIFF-R001](#F-CDIFF-R001) y [F-CDIFF-R017](#F-CDIFF-R017) asumen que la relacion analisis -> plan activo es 1:1. La forma concreta de seleccionarlo (default, multiples planes posibles, etc.) es decision de arquitectura.
2. **El curso es la fuente de verdad entre analisis sucesivos: las aceptadas se materializan ahi.** Heredado de [F-REVAPR-R011](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R011). [F-CDIFF-R003](#F-CDIFF-R003) depende directamente de esta propiedad; la equivalencia post-stack que ese assumption habilita es una propiedad emergente del sistema y no se contractualiza dentro del scope del consolidador.
3. **El motor de auditoria puede recomputar diagnosticos tipados y agregaciones sobre un subarbol modificado en memoria sin emitir un `AuditReport` oficial.** Heredado de FEAT-PIPRE (assumption 1). FEAT-CDIFF amplia el alcance: ahora se aplican varias propuestas a la vez (aceptadas + pendientes) y el motor produce las dos fotos derivadas (`consolidated`, `pendingProjection`) sobre los respectivos arboles modificados.
4. **El recorrido recursivo del modelo accede al mismo conjunto de hojas que el baseline expone.** [F-CDIFF-R019](#F-CDIFF-R019) (invariante 3, agnostico a la dimension). El motor de auditoria sobre el subarbol con aceptadas y sobre el subarbol con aceptadas+pendientes produce estructuras de la misma forma que el baseline; el descubrimiento dinamico recorre las tres y compara hoja-a-hoja.
5. **Las propuestas siguen el patron de sustitucion sobre `nodeId` estable.** Heredado de [F-LAPS-R014](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R014). Las propuestas estructurales (creacion / eliminacion / reordenamiento) quedan fuera del alcance de FEAT-CDIFF (ver `## Alcance`).
6. **Aceptar / rechazar propuestas es una operacion separada de "leer la salida consolidada".** [F-CDIFF-R015](#F-CDIFF-R015). Las decisiones siguen la pipeline de FEAT-REVAPR.
7. **El dominio puede declarar identidad natural sobre las listas relevantes.** [F-CDIFF-R022](#F-CDIFF-R022) requiere que el dominio exponga, para cada lista que merece diff por elemento, una clave natural. Si una lista nueva aparece sin identidad declarada, cae en diff posicional sin requerir cambios al contrato.
8. **El dominio (o el modelo) puede clasificar los roles funcionales que [F-CDIFF-R020](#F-CDIFF-R020) excluye del diff.** La forma exacta de declarar la categoria de un campo (anotacion, convencion de nombre, registro centralizado) es decision de arquitectura, pero el assumption es que la informacion existe o se puede establecer.
9. **La sintaxis exacta del verbo CLI, la forma de las claves de fields (separador del path, representacion de listas con identidad) y el shape JSON detallado quedan como decision de arquitectura.** El requerimiento define la semantica de los campos y su presencia condicional, no su nombre exacto en la salida ni la forma del comando que la emite. Mientras la semantica se respete, el arquitecto puede elegir la forma.

---

## References

- **FEAT-COURSE** — Define la jerarquia del curso (Course -> Milestone -> Topic -> Knowledge -> Quiz Template) sobre la que se construye el arbol del consolidado. Citado por [F-CDIFF-R006](#F-CDIFF-R006) (la regla aplica a hojas y a padres del arbol).
- **FEAT-REVAPR** — Define el ciclo de vida de las propuestas (`PENDING_APPROVAL`, `APPROVED`, `REJECTED`) y la regla de aplicacion al curso. La salida consolidada lee esos veredictos. Citado por [F-CDIFF-R003](#F-CDIFF-R003), [F-CDIFF-R004](#F-CDIFF-R004), [F-CDIFF-R005](#F-CDIFF-R005), [F-CDIFF-R011](#F-CDIFF-R011), [F-CDIFF-R015](#F-CDIFF-R015).
- **FEAT-REVBYP** — Define la `RevisionProposal` con sus campos (`elementBefore`, `elementAfter`, `nodeId`, `planId`, `sourceAuditId`). El contrato consolidado se apoya en esa estructura. Citado por [F-CDIFF-R004](#F-CDIFF-R004) y [F-CDIFF-R006](#F-CDIFF-R006).
- **FEAT-PIPRE** — Preview de impacto por propuesta individual. FEAT-CDIFF lo extiende con un contrato de salida agregado por analisis. Supersede explicitamente el escenario "preview combinado" que [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) habia dejado fuera. Citado por [F-CDIFF-R010](#F-CDIFF-R010), [F-CDIFF-R013](#F-CDIFF-R013), [F-CDIFF-R015](#F-CDIFF-R015), [F-CDIFF-R016](#F-CDIFF-R016) y [F-CDIFF-R019](#F-CDIFF-R019) (invariante 3 sobre paridad estructural).
- **FEAT-LAPS** — Estrategia de propuesta para `LEMMA_ABSENCE`. Aporta el patron de sustitucion sobre `nodeId` estable que la salida hereda. La exclusion de propuestas estructurales que esta feature define se hereda al alcance de FEAT-CDIFF (ver `## Alcance`).
- **FEAT-COCA** — Ejemplo de analizador con estrategia de agregacion no-lineal (acumulacion de conteos por banda). El motor de auditoria aplica esa estrategia al producir los `AuditReport` que FEAT-CDIFF lee para llenar las fotos del consolidado ([F-CDIFF-R007](#F-CDIFF-R007).5); como se agrega es alcance de FEAT-COCA, no de este contrato.
- **FEAT-DLABS** — Diagnosticos tipados de lemma-absence. Define los registros `LemmaAbsenceCourseDiagnosis`, `LemmaAbsenceLevelDiagnosis`, `LemmaPlacementDiagnosis`, `AbsentLemma`, `MisplacedLemma` que el motor produce sobre el arbol consolidado y el arbol con pendientes; sus hojas escalares y listas con identidad son recorridas dinamicamente por [F-CDIFF-R019](#F-CDIFF-R019). Identidades naturales `(lemma, pos)` ilustradas en [F-CDIFF-R022](#F-CDIFF-R022).
- **FEAT-DCOCA** — Diagnosticos tipados de coca-buckets-distribution. Define `CocaProgressionDiagnosis`, `CocaBucketsLevelDiagnosis`, `CocaBucketsTopicDiagnosis`, `BucketResult`, `BucketSummary`, `QuarterResult`, `ProgressionAssessment`, `ImprovementDirective`. Sus hojas escalares y listas con identidad (por `bandName`, `index`, `(type, bandName, levelName)`) son recorridas dinamicamente por [F-CDIFF-R019](#F-CDIFF-R019) y diff-eadas por [F-CDIFF-R022](#F-CDIFF-R022).
- **FEAT-DSLEN** — Diagnosticos tipados de sentence-length. Define `SentenceLengthDiagnosis` (a nivel quiz) cuyas hojas escalares (`tokenCount`, `targetMin`, `targetMax`, `cefrLevel`, `delta`, `toleranceMargin`) son recorridas dinamicamente por [F-CDIFF-R019](#F-CDIFF-R019) cuando el motor produce el diagnostico para los arboles consolidado y con pendientes.
