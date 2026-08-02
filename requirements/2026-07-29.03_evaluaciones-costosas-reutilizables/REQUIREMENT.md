---
feature:
  id: FEAT-EVCOST
  code: F-EVCOST
  name: Resultados de evaluacion costosa reutilizables entre analisis
  priority: critical
---

# Resultados de evaluacion costosa reutilizables entre analisis

## TL;DR

**Que**: El sistema registra el resultado de cada evaluacion costosa bajo la
huella del contenido evaluado, lo reutiliza en analisis posteriores y lo
conserva aunque la corrida se interrumpa.

**Por que**: Sin esto, cada analisis que se apoya en un evaluador lento y pagado
lo vuelve a pagar entero, y una caida a mitad de camino pierde todo el trabajo.

## Reglas de Negocio

<a id="F-EVCOST-R001"></a>
### Rule[F-EVCOST-R001] - La identidad de un resultado es evaluador + version + huella del contenido
**Severity**: critical | **Validation**: VALIDATED

> Cada resultado registrado se identifica por tres datos: **que evaluador** lo
> produjo, **en que version** de ese evaluador, y la **huella del contenido**
> que se le entrego. El identificador del elemento del curso al que el contenido
> pertenece **no** forma parte de la identidad.

<details><summary>Detalle</summary>

La razon es directa: modificar un elemento del curso **no cambia su
identificador**. Si la identidad fuera el identificador, un elemento corregido
seguiria devolviendo el resultado de su version anterior, que ya no describe el
contenido actual. La huella invierte esa relacion: el mismo contenido siempre
encuentra su resultado, y un contenido distinto nunca encuentra uno ajeno.

El identificador del elemento puede quedar registrado **junto al** resultado,
como dato informativo para inspeccionarlo o rastrearlo; lo que la regla prohibe
es usarlo para decidir si un resultado aplica.

**Criterio de aceptacion**: dos contenidos identicos evaluados por el mismo
evaluador y version encuentran el mismo resultado registrado, aunque pertenezcan
a elementos distintos del curso; y un elemento cuyo contenido cambio no encuentra
resultado registrado, aunque conserve su identificador.

**Error**: "El resultado registrado para el evaluador '{evaluador}' no corresponde al contenido consultado"

</details>

<a id="F-EVCOST-R002"></a>
### Rule[F-EVCOST-R002] - La huella se deriva de todo el contenido entregado al evaluador, y de nada mas
**Severity**: critical | **Validation**: VALIDATED

> La huella se calcula sobre **exactamente** el contenido que el evaluador
> recibe para emitir su resultado. Si cualquier parte de ese contenido cambia, la
> huella cambia y el resultado registrado deja de aplicar. Si nada de ese
> contenido cambia, la huella no cambia.

<details><summary>Detalle</summary>

Las dos mitades importan por igual:

- **Todo lo que el evaluador ve entra en la huella.** Un dato que el evaluador
  usa para juzgar pero que queda fuera de la huella produce el peor error
  posible: el contenido cambia, la huella no, y el sistema reutiliza con
  confianza un resultado que ya no describe lo que hoy existe. Es un error
  silencioso — nadie lo detecta hasta que alguien vuelve a mirar el elemento a
  mano.
- **Nada mas que eso entra en la huella.** Incluir datos que el evaluador no
  mira (una marca de tiempo, el orden de recorrido, un identificador tecnico)
  invalida resultados perfectamente vigentes y devuelve el costo que esta feature
  existe para evitar.

**Criterio de aceptacion**: (a) alterar cualquier parte del contenido entregado
al evaluador produce una huella distinta de la anterior; (b) volver a construir
el mismo contenido, en otro momento y en otra corrida, produce la misma huella.

**Error**: "La huella del contenido para el evaluador '{evaluador}' no es reproducible entre corridas"

</details>

<a id="F-EVCOST-R003"></a>
### Rule[F-EVCOST-R003] - Antes de evaluar se consulta el resultado vigente
**Severity**: critical | **Validation**: VALIDATED

> Ante un contenido a evaluar, el sistema consulta primero si existe un resultado
> registrado para ese evaluador, en su version vigente, y para esa huella. Si
> existe, lo devuelve y **no** consulta al evaluador. Si no existe, el contenido
> queda **pendiente** de evaluacion.

<details><summary>Detalle</summary>

"Pendiente" es un estado explicito, no un error: significa que el resultado
todavia no se conoce y que una corrida futura puede conocerlo. Que se haga con
los pendientes —evaluarlos ahora, dejarlos para la corrida siguiente, informarlos
como cobertura faltante— lo decide cada consumidor de esta capacidad, no esta
regla.

La reutilizacion **no tiene tope**: cualquier cantidad de resultados registrados
se reutiliza sin costo. Los limites que un consumidor imponga se aplican sobre
las evaluaciones nuevas, nunca sobre la reutilizacion.

**Criterio de aceptacion**: con un resultado ya registrado para la huella
consultada, la consulta lo devuelve y el evaluador no recibe ninguna solicitud;
sin resultado registrado, la consulta informa que el contenido esta pendiente.

**Error**: N/A (esta regla describe una consulta, no una condicion de error)

</details>

<a id="F-EVCOST-R004"></a>
### Rule[F-EVCOST-R004] - Cada resultado queda disponible apenas se obtiene
**Severity**: critical | **Validation**: VALIDATED

> Un resultado queda registrado y disponible para corridas futuras **en el
> momento en que se obtiene**, no al terminar el analisis. Una interrupcion
> abrupta conserva todo lo registrado hasta ese instante. Ningun registro queda a
> medio escribir: se lee completo o no existe. La garantia vale tambien cuando
> **dos analisis registran a la vez**: el peor desenlace posible es evaluar dos
> veces el mismo contenido —y pagarlo dos veces—, nunca corromper el registro ni
> perder un resultado ajeno.

<details><summary>Detalle</summary>

Esta es la regla que hace reanudable a un analisis costoso. Un analisis que
recorre miles de elementos y guarda al final tiene una probabilidad alta de no
llegar nunca al final: se agota el credito, cae el servicio, alguien corta el
proceso. Guardando al final, cada interrupcion tira todo el gasto ya hecho.

La segunda mitad —"completo o inexistente"— es lo que hace segura a la primera.
Un registro truncado por la interrupcion es peor que ninguno: se lo reutilizaria
como si fuera valido. Un resultado a medio escribir debe comportarse, para toda
consulta posterior, como si nunca se hubiera intentado registrarlo, dejando el
contenido pendiente.

**Criterio de aceptacion**: evaluados N contenidos, se interrumpe el proceso;
al consultar de nuevo, los N resultados obtenidos antes de la interrupcion se
devuelven completos y ninguno aparece parcial o ilegible.

**Error**: "El registro de resultados del evaluador '{evaluador}' contiene una entrada incompleta"

</details>

<a id="F-EVCOST-R005"></a>
### Rule[F-EVCOST-R005] - Solo se registra el resultado definitivo; la falla transitoria no se registra
**Severity**: critical | **Validation**: VALIDATED

> Se registra unicamente el resultado que el evaluador **emitio**, sea favorable o
> desfavorable: un resultado desfavorable es un resultado, y se reutiliza como
> cualquier otro. Cuando el evaluador **no llega a emitir** un resultado —servicio
> caido, tiempo de espera agotado, credito agotado, proceso interrumpido— no queda
> registrado nada, y el contenido vuelve a estar pendiente en la corrida siguiente.

<details><summary>Detalle</summary>

La distincion es la mas facil de confundir y la mas cara de equivocar:

- **Registrar una falla transitoria como si fuera resultado** congela un fallo de
  infraestructura como si fuera un juicio sobre el contenido. Nadie lo vuelve a
  intentar, porque el sistema cree que ya tiene la respuesta.
- **Tratar un resultado desfavorable como falla** hace que cada corrida vuelva a
  pagar exactamente por los contenidos que ya se sabe que estan mal, que suelen
  ser los que mas interesan.

El criterio para separarlos es **quien produjo el desenlace**: si el evaluador se
pronuncio sobre el contenido, hay resultado; si el evaluador no llego a
pronunciarse, no lo hay. Un evaluador que ante su propia falla emite un resultado
de cortesia —una respuesta con confianza nula que solo dice "no pude juzgar"— no
esta pronunciandose sobre el contenido: eso es falla transitoria y no se registra.
Cada consumidor declara como reconoce ese caso en su evaluador (por ejemplo
[F-QINST-R013](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R013)).

**Criterio de aceptacion**: (a) tras un resultado desfavorable emitido por el
evaluador, la corrida siguiente lo reutiliza y no vuelve a consultar; (b) tras una
falla del evaluador, la corrida siguiente encuentra el contenido pendiente y lo
vuelve a consultar.

**Error**: "Se registro como resultado una falla del evaluador '{evaluador}' sobre el contenido {huella}"

</details>

<a id="F-EVCOST-R006"></a>
### Rule[F-EVCOST-R006] - Conviven resultados de distintas versiones del evaluador; nada se borra solo
**Severity**: critical | **Validation**: VALIDATED

> Los resultados producidos por versiones distintas del mismo evaluador
> **conviven** en el registro. Solo los de la version vigente se consideran
> vigentes y se reutilizan; los demas quedan como historia consultable. Cambiar la
> version del evaluador **no borra** ningun resultado anterior y **no dispara**
> ninguna re-evaluacion.

<details><summary>Detalle</summary>

Cambiar un evaluador —su forma de preguntar, la estructura de lo que devuelve, su
criterio— es una operacion frecuente y barata; volver a evaluar todo el contenido
con la version nueva es lenta y cara. Si lo primero disparara lo segundo, cada
ajuste menor del evaluador se convertiria en una factura sorpresa.

Por eso el efecto de cambiar la version es exactamente uno: los resultados de la
version anterior **dejan de ser vigentes** y sus contenidos vuelven a estar
pendientes ([F-EVCOST-R003](#F-EVCOST-R003)). Cuando y con que alcance se los
evalua de nuevo lo decide el usuario ([F-EVCOST-R008](#F-EVCOST-R008)); no
evaluarlos nunca es una opcion valida.

Conservar la historia tiene valor propio: permite comparar como juzgo el mismo
contenido cada version del evaluador, que es la unica forma de saber si un cambio
lo mejoro.

**Criterio de aceptacion**: registrados resultados con la version A, se pasa a la
version B; los resultados de A siguen presentes y consultables, ninguno se
reutiliza como vigente, y los contenidos correspondientes aparecen pendientes.

**Error**: "El cambio de version del evaluador '{evaluador}' elimino {cantidad} resultados de la version anterior"

</details>

<a id="F-EVCOST-R007"></a>
### Rule[F-EVCOST-R007] - El registro es estado propio: sobrevive a los informes y se comparte entre analisis
**Severity**: critical | **Validation**: VALIDATED

> Los resultados registrados **no pertenecen** a ningun informe de analisis
> concreto: viven aparte, se comparten entre analisis sucesivos y sobreviven a la
> eliminacion, la rotacion o el reemplazo de los informes.

<details><summary>Detalle</summary>

Un informe de analisis es la foto de una corrida; el registro de resultados es
capital acumulado. Atarlos significaria que borrar un informe viejo —una tarea de
mantenimiento rutinaria— tira a la basura horas de evaluacion pagada, y que dos
analisis del mismo curso no puedan aprovechar el trabajo del otro.

La reciproca tambien vale: el registro **no** reemplaza al informe. El informe
sigue diciendo que se observo en esa corrida; el registro solo dice que resultado
tiene cada contenido evaluado.

**Criterio de aceptacion**: tras eliminar los informes de analisis anteriores, un
analisis nuevo sobre el mismo contenido reutiliza los resultados registrados y no
consulta al evaluador.

**Error**: "Los resultados registrados del evaluador '{evaluador}' se perdieron al eliminar informes de analisis"

</details>

<a id="F-EVCOST-R008"></a>
### Rule[F-EVCOST-R008] - Volver a evaluar es siempre una decision explicita
**Severity**: critical | **Validation**: VALIDATED

> Re-evaluar contenido que ya tiene resultado registrado ocurre **solo** cuando el
> usuario lo pide explicitamente. Ningun otro hecho —cambiar el evaluador, borrar
> informes, cambiar la configuracion del analisis— la dispara. La re-evaluacion
> registra los resultados nuevos **sin destruir** los anteriores. El **alcance** lo
> acota el consumidor, contenido por contenido: esta capacidad vuelve a evaluar el
> contenido puntual que se le pide y **ningun otro**, de modo que el contenido
> vecino con resultado vigente se sigue reutilizando en esa misma corrida.

<details><summary>Detalle</summary>

Esta regla es la contracara de todas las anteriores: si el sistema puede decidir
por su cuenta volver a evaluar, el ahorro deja de ser predecible y el gasto deja
de ser gobernable. Un usuario tiene que poder cambiar el evaluador, limpiar
informes y correr analisis sin miedo a desatar una factura.

La re-evaluacion explicita puede alcanzar a todo el contenido de un evaluador o a
una parte acotada de el; en ambos casos respeta los limites de gasto que imponga
el consumidor que la ejecuta (por ejemplo
[F-QINST-R006](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R006)).

**Quien acota ese alcance: el consumidor, no esta capacidad.** La aclaracion no
cambia ninguna decision —"todo" y "una parte" ya estaban admitidos— sino que dice
donde vive el recorte, y la razon es concreta: los recortes que un consumidor usa
estan expresados en su propio vocabulario (por ejemplo "todos los ejercicios de un
knowledge"), y de un contenido esta capacidad solo conoce una **referencia opaca**
al elemento del curso del que salio. No puede agrupar por algo que no interpreta, y
si lo intentara duplicaria un criterio que el consumidor ya aplica cuando decide
que contenido le entrega.

Por eso el reparto es: el consumidor elige **cuales**, esta capacidad garantiza que
volver a evaluar **uno** no arrastre a los demas. Un pedido de re-evaluacion sobre
un contenido deja intacto el reuso del resto: es lo que permite que un consumidor
componga cualquier alcance —uno, un grupo, todo— sin que esta capacidad tenga que
conocer ninguno de los tres.

**Criterio de aceptacion**: (a) sin pedido explicito, ninguna corrida vuelve a
consultar al evaluador por contenido con resultado vigente registrado; (b) con
pedido explicito sobre un contenido, ese contenido se vuelve a evaluar, los
resultados anteriores siguen consultables, y el resto del contenido con resultado
vigente se reutiliza sin consultar al evaluador en esa misma corrida.

**Error**: "Se re-evaluo contenido con resultado vigente sin pedido explicito del usuario"

</details>

<a id="F-EVCOST-R009"></a>
### Rule[F-EVCOST-R009] - Ante una falla, la corrida distingue si fallo ese contenido o fallo el servicio
**Severity**: critical | **Validation**: VALIDATED

> Cuando el evaluador no llega a pronunciarse, el sistema distingue **de donde
> viene la falla**. Si es atribuible a **ese contenido puntual** —el evaluador
> respondio, pero su salida no es utilizable—, la corrida **sigue** consultando por
> el contenido restante. Si es atribuible al **servicio** —caido, tiempo de espera
> agotado, autenticacion rechazada, credito agotado—, la corrida **deja de
> consultar** al evaluador y todo el contenido restante queda pendiente. En ninguno
> de los dos casos se aborta el analisis: la corrida termina con lo que alcanzo a
> evaluar.

<details><summary>Detalle</summary>

[F-EVCOST-R005](#F-EVCOST-R005) ya exige que una falla **no se registre** y que el
contenido vuelva a estar pendiente. Es correcto y necesario, pero no dice nada
sobre **seguir o cortar**: una implementacion que ante un servicio caido insiste
con los 11.500 contenidos restantes cumple R005 sin objecion. Esta regla cubre
exactamente ese hueco.

Las dos mitades se sostienen por razones distintas y opuestas:

- **Cortar ante la falla del servicio.** Sin corte, una auditoria sobre ~11.500
  ejercicios le pega 11.500 veces a un servicio que ya se sabe caido: tarda lo
  mismo que una corrida completa y no produce ni un solo resultado. Con corte, la
  corrida termina rapido, con cobertura parcial honesta y sin desperdiciar nada.
- **No cortar ante la falla de un contenido.** Si un solo contenido problematico
  detuviera la corrida, un unico elemento con el que el evaluador tropieza dejaria
  pendiente a todo lo que viene despues, y cada corrida siguiente volveria a
  tropezar con el mismo. El gasto ya hecho se conservaria, pero el avance seria
  nulo.

**El criterio es el origen de la falla, no su gravedad aparente.** Una salida
inutilizable puede verse alarmante y sin embargo afecta solo a ese contenido; un
tiempo de espera agotado puede verse trivial y sin embargo indica que el servicio
no esta respondiendo. Clasificar por como se ve el sintoma, y no por que lo
produjo, rompe las dos mitades a la vez.

**Cortar consultas no es abortar el analisis.** La corrida deja de preguntarle a
ese evaluador, pero termina normalmente: devuelve los resultados que alcanzo a
obtener, deja el resto como pendiente y no arrastra en su caida al resto del
analisis, que no depende de este evaluador. El primer consumidor ya lo exige de su
lado
([F-QINST-R007](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R007));
aca queda como garantia de la capacidad general, para que cualquier consumidor
futuro la herede en lugar de volver a decidirla.

Como se reconoce a que clase pertenece cada falla depende de cada evaluador y lo
declara su consumidor: para el primero, la salida no utilizable es el veredicto
conservador de infraestructura del juez
([F-QINST-R013](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R013)).

**Criterio de aceptacion**: sobre una serie de N contenidos a evaluar,
(a) si la falla en el contenido i es atribuible a ese contenido, el evaluador
recibe consultas por los contenidos siguientes y solo el contenido i queda
pendiente; (b) si la falla en el contenido i es atribuible al servicio, el
evaluador no recibe ninguna consulta mas, los contenidos i..N quedan pendientes y
la corrida termina normalmente devolviendo los resultados de los contenidos
anteriores; (c) en ambos casos no queda registrado ningun resultado para el
contenido fallido ([F-EVCOST-R005](#F-EVCOST-R005)).

**Error**: "La corrida siguio consultando al evaluador '{evaluador}' tras una falla
del servicio" (condicion prohibida). El error simetrico se manifiesta como "La
corrida dejo de consultar al evaluador '{evaluador}' por una falla atribuible a un
unico contenido".

</details>

## Contexto

Los analisis del sistema fueron, hasta ahora, baratos y deterministas: recorren el
contenido, cuentan, comparan contra un rango y puntuan. Volver a correrlos entero
cuesta segundos, asi que nunca hizo falta recordar nada entre corridas: el informe
de auditoria era todo el estado que existia.

Los evaluadores basados en juicio cambian ese supuesto en tres dimensiones a la
vez:

1. **Costo por elemento.** Cada elemento cuesta una consulta a un evaluador
   externo, pagada y lenta. Sobre un curso de ~11.500 ejercicios, una pasada
   completa es de horas y de dinero real.
2. **Falla parcial.** El credito se agota, el servicio se cae, el proceso se
   corta. Con analisis baratos eso solo obligaba a repetir; aca destruye trabajo
   pagado.
3. **No determinismo.** Dos consultas al mismo evaluador por el mismo contenido
   pueden no coincidir. Volver a evaluar sin necesidad no solo cuesta: introduce
   ruido en el resultado.

De ahi que el resultado de una evaluacion costosa tenga que dejar de ser un dato
efimero del informe y pasar a ser **estado propio del sistema**: registrado por
contenido, reutilizable, resistente a la interrupcion y compartido entre analisis.

### Por que esta capacidad es general y no parte de un analizador

El primer consumidor es el analizador de cumplimiento de consigna
([FEAT-QINST](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md)),
pero el patron no es suyo: cualquier evaluacion basada en juicio —revisar la
coherencia pedagogica de un knowledge, verificar la calidad de una traduccion,
detectar ejercicios duplicados— repite las tres condiciones de arriba y necesita
exactamente las mismas garantias.

Escribir esas garantias dentro del primer analizador las volveria invisibles para
el segundo, que las reinventaria con criterios ligeramente distintos: uno
identificaria por identificador del elemento y otro por huella, uno guardaria las
fallas y otro no. Al declararlas como capacidad propia, el segundo consumidor
hereda el comportamiento verificado en lugar de repetir el analisis.

### La huella, y por que no alcanza el identificador

El punto mas delicado es como se decide que un resultado registrado "sigue
sirviendo". La respuesta ingenua —el identificador del elemento— falla en el
escenario mas comun del sistema: **corregir un elemento no cambia su
identificador**. Un ejercicio revisado por el refinador conserva su identificador
y cambia su contenido; con identidad por identificador, seguiria mostrando para
siempre el juicio emitido sobre su version anterior.

Por eso la identidad es la huella del contenido efectivamente evaluado
([F-EVCOST-R001](#F-EVCOST-R001), [F-EVCOST-R002](#F-EVCOST-R002)): describe
**que se juzgo**, no **sobre que elemento**. Trae ademas un beneficio no buscado:
dos elementos con contenido identico comparten resultado y se pagan una sola vez.

## Alcance

- **En alcance**:
  - Registro de resultados de evaluacion costosa por evaluador, version y huella
    de contenido.
  - Consulta del resultado vigente y estado "pendiente" cuando no lo hay.
  - Disponibilidad inmediata de cada resultado y tolerancia a interrupcion.
  - Distincion entre resultado definitivo y falla transitoria del evaluador.
  - Distincion, dentro de la falla transitoria, entre la que afecta a un contenido
    puntual y la que afecta al servicio, con corte de consultas ante la segunda y
    sin abortar el analisis.
  - Convivencia e historia de resultados de versiones distintas del evaluador.
  - Independencia respecto de los informes de analisis.
  - Re-evaluacion explicita del usuario, contenido por contenido.
- **Fuera de alcance**:
  - **Que contenido alcanza una re-evaluacion** (todo, un grupo, uno solo). El
    recorte esta expresado en el vocabulario del consumidor y lo decide el
    consumidor; ver [F-EVCOST-R008](#F-EVCOST-R008).
  - **Que evalua cada evaluador y como se traduce su resultado a un puntaje.** Lo
    define cada consumidor (para el primero,
    [F-QINST-R002](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R002)).
  - **El limite de evaluaciones nuevas por corrida.** Hoy lo declara cada
    consumidor; ver [DOUBT-PRESUPUESTO-GENERAL](#DOUBT-PRESUPUESTO-GENERAL).
  - **Como se reconoce a que clase pertenece cada falla** en un evaluador concreto.
    Lo declara cada consumidor (para el primero,
    [F-QINST-R013](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R013)).
  - **Reintentar al servicio** dentro de la misma corrida despues de que fallo, o
    esperar a que se recupere.
  - **Como se representa la huella** y donde se guardan los resultados.
  - **La poda de la historia de versiones anteriores**; ver
    [DOUBT-RETENCION-HISTORIA](#DOUBT-RETENCION-HISTORIA).
  - **La coordinacion entre dos analisis simultaneos** para que no evaluen dos veces
    el mismo contenido. Correrlos a la vez es seguro
    ([F-EVCOST-R004](#F-EVCOST-R004)), pero puede duplicar gasto; ver
    [DOUBT-CONCURRENCIA](#DOUBT-CONCURRENCIA).

## User Journeys

### Journey[F-EVCOST-J001] - Reutilizar, o no, un resultado ya registrado
**Validation**: VALIDATED

Cubre las cuatro respuestas posibles a "ya tengo este resultado?": reuso,
contenido cambiado, version del evaluador cambiada y contenido nunca evaluado.
Cubre R001, R002, R003, R004 y R006.

```yaml
journeys:
  - id: F-EVCOST-J001
    name: Reutilizar, o no, un resultado ya registrado
    flow:
      - id: solicitar
        action: "Un analisis solicita el resultado de un evaluador costoso para un contenido del curso"
        gate: [F-EVCOST-R003]
        outcomes:
          - when: "Hay un resultado registrado para ese evaluador, en su version vigente y para la misma huella de contenido"
            then: reusa
          - when: "Hay un resultado registrado, pero el contenido cambio desde que se lo evaluo (el elemento conserva su identificador)"
            then: contenido_cambiado
          - when: "Hay un resultado registrado, pero fue producido por una version anterior del evaluador"
            then: version_anterior
          - when: "No hay ningun resultado registrado para esa huella"
            then: primera_vez

      - id: reusa
        action: "El analisis obtiene el resultado registrado y el evaluador no recibe ninguna solicitud por ese contenido"
        gate: [F-EVCOST-R001, F-EVCOST-R003]
        result: success

      - id: contenido_cambiado
        action: "El resultado registrado no se considera vigente: el contenido queda pendiente y, al evaluarse, se registra un resultado nuevo bajo la huella nueva, sin destruir el anterior"
        gate: [F-EVCOST-R002]
        result: success

      - id: version_anterior
        action: "El resultado de la version anterior se conserva consultable y no se reutiliza; el contenido queda pendiente de evaluacion con la version vigente"
        gate: [F-EVCOST-R006]
        result: success

      - id: primera_vez
        action: "El contenido queda pendiente; al evaluarse, su resultado queda registrado y disponible de inmediato para cualquier analisis posterior"
        gate: [F-EVCOST-R004]
        result: success
```

### Journey[F-EVCOST-J002] - Una interrupcion o una falla no pierden el trabajo hecho
**Validation**: VALIDATED

Cubre la tolerancia a la interrupcion, la separacion entre resultado definitivo
—favorable o desfavorable— y falla transitoria del evaluador, y la distincion entre
una falla atribuible a un contenido puntual (la corrida sigue) y una atribuible al
servicio (la corrida deja de consultar y termina igual). Cubre R004, R005 y R009.

```yaml
journeys:
  - id: F-EVCOST-J002
    name: Una interrupcion o una falla no pierden el trabajo hecho
    flow:
      - id: recorrer
        action: "Un analisis evalua contenidos uno por uno y registra cada resultado apenas lo obtiene"
        gate: [F-EVCOST-R004]
        outcomes:
          - when: "El proceso se interrumpe abruptamente a mitad del recorrido"
            then: conserva_lo_hecho
          - when: "El evaluador responde sobre un contenido pero su salida no es utilizable: la falla es atribuible a ese contenido puntual"
            then: falla_no_registrada
          - when: "El evaluador se pronuncia sobre un contenido con un resultado desfavorable"
            then: desfavorable_registrado
          - when: "El evaluador deja de estar disponible a mitad del recorrido (servicio caido, tiempo de espera agotado, autenticacion rechazada, credito agotado)"
            then: falla_del_servicio

      - id: conserva_lo_hecho
        action: "Los resultados obtenidos antes de la interrupcion siguen disponibles y completos al volver a consultar; ninguna entrada quedo a medio escribir"
        gate: [F-EVCOST-R004]
        result: success

      - id: falla_no_registrada
        action: "No queda registrado ningun resultado para ese contenido y la corrida sigue consultando por el contenido restante; la corrida siguiente encuentra pendiente al contenido fallido y vuelve a consultar por el"
        gate: [F-EVCOST-R005, F-EVCOST-R009]
        result: success

      - id: desfavorable_registrado
        action: "El resultado desfavorable queda registrado como definitivo y la corrida siguiente lo reutiliza sin volver a consultar al evaluador"
        gate: [F-EVCOST-R005]
        result: success

      - id: falla_del_servicio
        action: "La corrida deja de consultar al evaluador: no registra nada para el contenido restante, que queda pendiente, y el analisis termina igual con los resultados que alcanzo a obtener, que la corrida siguiente reutiliza"
        gate: [F-EVCOST-R009]
        result: success
```

### Journey[F-EVCOST-J003] - Los resultados sobreviven a los informes y solo se rehacen a pedido
**Validation**: VALIDATED

Cubre la independencia del registro respecto de los informes de analisis y el
caracter explicito de la re-evaluacion. Cubre R007 y R008.

```yaml
journeys:
  - id: F-EVCOST-J003
    name: Los resultados sobreviven a los informes y solo se rehacen a pedido
    flow:
      - id: eliminar_informes
        action: "El usuario elimina los informes de analisis anteriores del curso"
        then: nuevo_analisis

      - id: nuevo_analisis
        action: "El usuario ejecuta un analisis nuevo sobre el mismo contenido"
        gate: [F-EVCOST-R007]
        outcomes:
          - when: "El usuario no pidio volver a evaluar"
            then: reusa_igual
          - when: "El usuario pidio explicitamente volver a evaluar ese evaluador"
            then: reevalua_a_pedido

      - id: reusa_igual
        action: "Los resultados registrados siguen disponibles y se reutilizan: el evaluador no recibe ninguna solicitud por el contenido ya evaluado"
        gate: [F-EVCOST-R007, F-EVCOST-R008]
        result: success

      - id: reevalua_a_pedido
        action: "El contenido alcanzado se vuelve a evaluar pese a tener resultado vigente, y los resultados anteriores siguen consultables"
        gate: [F-EVCOST-R008]
        result: success
```

## Open Questions

<a id="DOUBT-PRESUPUESTO-GENERAL"></a>
### Doubt[DOUBT-PRESUPUESTO-GENERAL] - El limite de evaluaciones por corrida, es parte de esta capacidad o de cada consumidor?
**Status**: OPEN

El primer consumidor acota cuantas evaluaciones nuevas hace por corrida
([F-QINST-R006](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R006)).
El limite es tan generico como el registro: cualquier evaluador costoso lo va a
necesitar.

- [ ] Opcion A: Lo declara **cada consumidor**. Cada analizador elige su limite y
  su valor por defecto segun lo que cueste su evaluador. Es lo que se especifica
  hoy; el riesgo es que el segundo consumidor lo reinvente distinto.
- [ ] Opcion B: Lo declara **esta capacidad**, como limite de evaluaciones nuevas
  por evaluador y por corrida, y cada consumidor solo elige su valor. Evita la
  duplicacion; obliga a decidir ahora una forma que solo se probo con un
  consumidor.
- [ ] Opcion C: Ninguno de los dos: el limite es del usuario que lanza el analisis
  y aplica a la corrida entera, sin importar cuantos evaluadores participen.

**Answer**: Pendiente. Se especifica la Opcion A por ahora — con un solo consumidor
no hay evidencia suficiente para fijar la forma general. Cuando aparezca el segundo
evaluador costoso, esta duda debe reabrirse **antes** de escribir su limite; si
para entonces el limite del primero resulto adecuado sin cambios, promoverlo a
capacidad general es preferible a repetirlo.

<a id="DOUBT-RETENCION-HISTORIA"></a>
### Doubt[DOUBT-RETENCION-HISTORIA] - Hasta cuando se conserva la historia de versiones anteriores?
**Status**: OPEN

[F-EVCOST-R006](#F-EVCOST-R006) conserva los resultados de versiones anteriores
como historia y prohibe borrarlos automaticamente. Con un evaluador que cambia
varias veces y ~11.500 elementos por version, la historia crece sin techo.

- [ ] Opcion A: **Nunca se poda automaticamente.** El usuario puede pedir
  explicitamente descartar la historia de una version. Simple y sin sorpresas; el
  volumen queda a cargo del usuario.
- [ ] Opcion B: **Se conservan las ultimas N versiones** y las anteriores se
  descartan solas. Acota el volumen; reintroduce una perdida automatica, que es
  justo lo que R006 prohibe.
- [ ] Opcion C: **Se conserva solo la version vigente** y la anterior se descarta
  al cambiar de version. Volumen minimo; elimina la posibilidad de comparar como
  juzgo cada version.

**Answer**: Pendiente. Se especifica la Opcion A [ASSUMPTION]: el volumen que
preocupa es hipotetico (todavia no hubo un solo cambio de version) y la
comparacion entre versiones es hoy el unico modo de saber si un cambio del
evaluador lo mejoro. Si el volumen se vuelve un problema medible, la poda explicita
de la Opcion A ya lo resuelve sin cambiar ninguna regla.

<a id="DOUBT-CONCURRENCIA"></a>
### Doubt[DOUBT-CONCURRENCIA] - Que pasa si dos analisis corren a la vez sobre el mismo registro?
**Status**: RESOLVED

[F-EVCOST-R004](#F-EVCOST-R004) exige que cada resultado quede disponible apenas se
obtiene y que ningun registro quede a medio escribir. La regla se escribio pensando
en **una** corrida interrumpida. Dos corridas simultaneas sobre el mismo evaluador
plantean un caso distinto: ambas escriben, y ambas leen lo que la otra escribe.

- [ ] Opcion A: **No esta soportado.** Se declara que un solo analisis por vez usa
  el registro de un evaluador; correr dos en paralelo tiene resultado indefinido.
- [x] Opcion B: **Soportado y seguro**: dos corridas simultaneas pueden escribir sin
  corromper el registro, y a lo sumo evaluan dos veces el mismo contenido (gasto
  duplicado, sin dano).
- [ ] Opcion C: **Soportado y coordinado**: una corrida sabe que otra esta evaluando
  un contenido y no lo duplica.

**Answer**: Opcion B. La garantia no hubo que agregarla: la da la **forma del
registro**. Los resultados de un evaluador se acumulan en un unico registro con una
entrada por linea, cada resultado se agrega al final sin reescribir lo anterior, y
la lectura descarta la linea que no se puede interpretar. De ahi salen las tres
propiedades a la vez: ninguna corrida pisa lo que escribio la otra, ninguna linea a
medio escribir se toma como valida, y una interrupcion solo puede dañar la ultima
linea.

**El costo aceptado es acotado y economico, no estructural**: dos corridas
simultaneas que llegan al mismo contenido antes de que la otra lo haya registrado
lo evaluan —y lo pagan— dos veces. Lo que **nunca** ocurre es corromper el registro
ni perder el resultado que escribio la otra corrida. Esta garantia quedo explicita
en el enunciado de [F-EVCOST-R004](#F-EVCOST-R004), porque es comportamiento
observable y no un detalle de como se guarda.

**La Opcion C se descarta por ahora**: agregaria coordinacion entre corridas —saber
que otra esta evaluando un contenido y esperarla— para ahorrar un gasto que solo
aparece si alguien corre dos auditorias a la vez del mismo evaluador. Es
complejidad permanente a cambio de un ahorro eventual. Si la ejecucion en paralelo
se vuelve habitual y el gasto duplicado se mide y molesta, la duda se reabre.

## References

- **FEAT-QINST** — Primer consumidor de esta capacidad: el analizador de
  cumplimiento de consigna de quiz. Define que evalua su juez, como se traduce el
  resultado a puntaje y con que limite de evaluaciones nuevas por corrida. Aporta
  ademas las dos caras del comportamiento ante fallas visto desde el consumidor:
  **F-QINST-R007** (una falla del juez no aborta la auditoria, que termina con la
  cobertura que alcanzo) y **F-QINST-R013** (la salida no utilizable del juez es
  falla y no juicio, y afecta solo a ese ejercicio). Citada por
  [F-EVCOST-R005](#F-EVCOST-R005), [F-EVCOST-R008](#F-EVCOST-R008),
  [F-EVCOST-R009](#F-EVCOST-R009), el Alcance y el Contexto.
- **FEAT-RPRES** — Establece que corregir un elemento del curso cambia su contenido
  pero no su identificador, que es la razon por la que la identidad de un resultado
  es la huella del contenido y no el identificador. Citada por el Contexto.
