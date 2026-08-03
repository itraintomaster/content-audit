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
### Rule[F-EVCOST-R001] - La identidad de un resultado es evaluador + huella del contenido; la version es procedencia
**Severity**: critical | **Validation**: VALIDATED

> Cada resultado registrado se identifica por dos datos: **que evaluador** lo
> produjo y la **huella del contenido** que se le entrego. La **version** del
> evaluador se registra **junto a** cada resultado y se devuelve con el cada vez
> que se lo consulta, pero **no** integra la identidad y **no** decide si un
> resultado aplica: es procedencia, no vigencia. El identificador del elemento del
> curso al que el contenido pertenece tampoco integra la identidad.

<details><summary>Detalle</summary>

**Por que la huella y no el identificador del elemento.** Modificar un elemento
del curso **no cambia su identificador**. Si la identidad fuera el identificador,
un elemento corregido seguiria devolviendo el resultado de su version anterior,
que ya no describe el contenido actual. La huella invierte esa relacion: el mismo
contenido siempre encuentra su resultado, y un contenido distinto nunca encuentra
uno ajeno. El identificador del elemento puede quedar registrado **junto al**
resultado, como dato informativo para inspeccionarlo o rastrearlo; lo que la regla
prohibe es usarlo para decidir si un resultado aplica.

**Por que el evaluador si integra la identidad.** No identifica *quien juzga* sino
**que pregunta se hizo**. El resultado de un evaluador de calidad de traduccion
sobre un contenido no es intercambiable con el de un evaluador de cumplimiento de
consigna sobre ese mismo contenido: son dos preguntas distintas y sus respuestas no
se sustituyen. Confundirlas devolveria la respuesta de una pregunta que nadie hizo.

**Por que la version NO integra la identidad.** Que un contenido cumpla o no lo que
se le pregunta es una propiedad **del contenido**, no de quien la juzgo. Cambiar de
modelo —por costo, por disponibilidad, por una migracion de proveedor— no puede
invalidar de un plumazo miles de resultados ya pagados. Por eso la version se
registra como **procedencia**: dice con que version se obtuvo cada resultado, queda
consultable junto a el y permite comparar como juzgo cada version, pero ningun
cambio de version vuelve inaplicable un resultado ya registrado. El unico modo de
rehacer un resultado es pedirlo explicitamente
([F-EVCOST-R008](#F-EVCOST-R008)). La alternativa —version dentro de la identidad—
se evaluo y se descarto; ver
[DOUBT-IDENTIDAD-VERSION](#DOUBT-IDENTIDAD-VERSION).

**La procedencia es obligatoria, no opcional.** Un resultado registrado sin la
version que lo produjo rompe todo lo que la decision anterior deja en pie:
consultarla, compararla y declararla en un informe
([F-QINST-R005](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R005)).

**Criterio de aceptacion**: (a) dos contenidos identicos evaluados por el mismo
evaluador encuentran el mismo resultado registrado, aunque pertenezcan a elementos
distintos del curso; (b) un elemento cuyo contenido cambio no encuentra resultado
registrado, aunque conserve su identificador; (c) un resultado registrado bajo la
version A se encuentra igual cuando la version vigente es B, y la consulta informa
que lo produjo la version A; (d) un resultado registrado por el evaluador X nunca
se devuelve como resultado del evaluador Y para la misma huella.

**Error**: "El resultado registrado para el evaluador '{evaluador}' no corresponde al contenido consultado" / "Se registro un resultado del evaluador '{evaluador}' sin la version que lo produjo"

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
### Rule[F-EVCOST-R003] - Antes de evaluar se consulta el resultado ya registrado
**Severity**: critical | **Validation**: VALIDATED

> Ante un contenido a evaluar, el sistema consulta primero si existe un resultado
> registrado para ese evaluador y esa huella, **cualquiera sea la version del
> evaluador que lo produjo**. Si existe, lo devuelve —junto con la version que lo
> produjo ([F-EVCOST-R001](#F-EVCOST-R001))— y **no** consulta al evaluador. Si no
> existe, el contenido queda **pendiente** de evaluacion.

<details><summary>Detalle</summary>

"Pendiente" es un estado explicito, no un error: significa que el resultado
todavia no se conoce y que una corrida futura puede conocerlo. Que se haga con
los pendientes —evaluarlos ahora, dejarlos para la corrida siguiente, informarlos
como cobertura faltante— lo decide cada consumidor de esta capacidad, no esta
regla.

**Pendiente significa "nunca se registro un resultado para esta huella", y nada
mas.** No hay ninguna otra via por la que un contenido con resultado registrado
vuelva a estar pendiente: ni cambiar la version del evaluador, ni el paso del
tiempo, ni borrar informes. El unico modo de volver a evaluarlo es pedirlo
explicitamente ([F-EVCOST-R008](#F-EVCOST-R008)).

La reutilizacion **no tiene tope**: cualquier cantidad de resultados registrados
se reutiliza sin costo. Los limites que un consumidor imponga se aplican sobre
las evaluaciones nuevas, nunca sobre la reutilizacion.

**Criterio de aceptacion**: (a) con un resultado ya registrado para la huella
consultada, la consulta lo devuelve —con la version que lo produjo— y el evaluador
no recibe ninguna solicitud; (b) eso vale tambien cuando la version vigente del
evaluador es distinta de la que produjo el resultado; (c) sin resultado registrado
para esa huella, la consulta informa que el contenido esta pendiente.

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
### Rule[F-EVCOST-R006] - Conviven resultados de distintas versiones del evaluador; todos se reutilizan y nada se borra solo
**Severity**: critical | **Validation**: VALIDATED

> Los resultados producidos por versiones distintas del mismo evaluador
> **conviven** en el registro y **todos** se reutilizan, sea cual sea la version
> que los produjo. Cambiar la version del evaluador **no borra**, **no invalida** y
> **no dispara** ninguna re-evaluacion. Cuando hay mas de un resultado para el
> mismo evaluador y la misma huella, se reutiliza el **registrado mas
> recientemente**; los anteriores quedan como historia consultable, cada uno con la
> version que lo produjo.

<details><summary>Detalle</summary>

Cambiar un evaluador —su forma de preguntar, la estructura de lo que devuelve, su
criterio, el modelo que lo respalda— es una operacion frecuente y barata; volver a
evaluar todo el contenido con la version nueva es lenta y cara. Si lo primero
disparara lo segundo, cada ajuste menor del evaluador se convertiria en una factura
sorpresa.

Por eso cambiar la version **no tiene ningun efecto sobre los resultados ya
registrados**: se siguen consultando y reutilizando exactamente igual, y lo unico
que cambia es la version que se registrara junto a los resultados **nuevos**.
Cuando y con que alcance se vuelve a evaluar lo decide el usuario
([F-EVCOST-R008](#F-EVCOST-R008)); no volver a evaluar nunca es una opcion valida.

**Por que gana el mas reciente.** Es lo unico compatible con
[F-EVCOST-R008](#F-EVCOST-R008): si tras una re-evaluacion explicita la consulta
siguiera devolviendo el resultado viejo, pedir la re-evaluacion no tendria efecto
observable y el usuario habria pagado por nada. La regla no borra el anterior —lo
conserva como historia— pero fija sin ambiguedad cual de los dos aplica.

Conservar la historia tiene valor propio: permite comparar como juzgo el mismo
contenido cada version del evaluador, que es la unica forma de saber si un cambio
lo mejoro. Esa comparacion solo es posible porque cada resultado lleva registrada
su procedencia ([F-EVCOST-R001](#F-EVCOST-R001)).

**Criterio de aceptacion**: (a) registrados resultados con la version A, se pasa a
la version B: los resultados de A se siguen reutilizando, el evaluador no recibe
ninguna consulta por ellos, ninguno queda pendiente, y la consulta informa que los
produjo la version A; (b) tras una re-evaluacion explicita del mismo contenido bajo
la version B, la consulta devuelve el resultado de B y el de A sigue siendo
consultable como historia.

**Error**: "El cambio de version del evaluador '{evaluador}' elimino {cantidad} resultados de la version anterior" / "El cambio de version del evaluador '{evaluador}' dejo de reutilizar {cantidad} resultados ya registrados"

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
### Rule[F-EVCOST-R008] - La re-evaluacion explicita es el unico modo de rehacer un resultado
**Severity**: critical | **Validation**: VALIDATED

> Un resultado ya registrado se rehace **unicamente** cuando el usuario pide
> explicitamente volver a evaluar ese contenido: es el **unico** mecanismo, y no
> existe ningun otro. Ningun hecho del sistema —cambiar la version del evaluador,
> borrar informes, cambiar la configuracion del analisis, el paso del tiempo— lo
> dispara, lo invalida ni lo vuelve a dejar pendiente. La re-evaluacion registra el
> resultado nuevo **sin destruir** los anteriores, y a partir de entonces la
> consulta devuelve el nuevo ([F-EVCOST-R006](#F-EVCOST-R006)). El **alcance** lo
> acota el consumidor, contenido por contenido: esta capacidad vuelve a evaluar el
> contenido puntual que se le pide y **ningun otro**, de modo que el contenido
> vecino con resultado registrado se sigue reutilizando en esa misma corrida.

<details><summary>Detalle</summary>

Esta regla es la contracara de todas las anteriores: si el sistema puede decidir
por su cuenta volver a evaluar, el ahorro deja de ser predecible y el gasto deja
de ser gobernable. Un usuario tiene que poder cambiar el evaluador, limpiar
informes y correr analisis sin miedo a desatar una factura.

**Que sea el unico mecanismo es una exigencia, no una consecuencia.** Con la
version fuera de la identidad ([F-EVCOST-R001](#F-EVCOST-R001)) no queda ninguna
otra via por la cual un resultado registrado pueda dejar de aplicar: no hay
invalidacion automatica, no hay caducidad, no hay efecto colateral. Todo lo que se
vuelve a pagar se vuelve a pagar porque alguien lo pidio, y eso hace que el gasto
sea siempre atribuible a una decision.

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
consultar al evaluador por contenido con resultado registrado, **cualquiera sea la
version que lo produjo y aunque la version vigente sea otra**; (b) con pedido
explicito sobre un contenido, ese contenido se vuelve a evaluar, la consulta pasa a
devolver el resultado nuevo, los resultados anteriores siguen consultables, y el
resto del contenido con resultado registrado se reutiliza sin consultar al
evaluador en esa misma corrida.

**Error**: "Se re-evaluo contenido con resultado registrado sin pedido explicito del usuario"

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

### La version del evaluador: procedencia, no vigencia

Queda una segunda pregunta, independiente de la anterior: si cambia **el
evaluador** —su forma de preguntar, la estructura de lo que devuelve, el modelo que
lo respalda—, ¿siguen aplicando los resultados que emitio antes?

La respuesta especificada es **si**. Que un contenido cumpla o no lo que se le
pregunta es una propiedad del contenido, no de quien la juzgo; una migracion de
proveedor o un ajuste de costo no puede invalidar de golpe miles de resultados ya
pagados. La version se registra como **procedencia** —queda junto al resultado,
viaja con el al consultarlo y permite comparar como juzgo cada version— pero no
gobierna la vigencia. Nada se invalida solo: lo unico que rehace un resultado es un
pedido explicito ([F-EVCOST-R008](#F-EVCOST-R008)).

Lo que si sigue dentro de la identidad es **que evaluador** produjo el resultado, y
no contradice el criterio anterior: no identifica quien juzga sino **que pregunta se
hizo**. El juicio de un futuro evaluador de calidad de traduccion sobre un contenido
no sustituye al de un evaluador de cumplimiento de consigna sobre el mismo
contenido.

La contra de esta eleccion es real y se acepta a conciencia: al corregir un
evaluador que juzgaba mal, los resultados ya emitidos siguen aplicando aunque se los
sepa incorrectos, hasta que alguien pida re-evaluarlos. Ver
[DOUBT-IDENTIDAD-VERSION](#DOUBT-IDENTIDAD-VERSION).

## Alcance

- **En alcance**:
  - Registro de resultados de evaluacion costosa por evaluador y huella de
    contenido, con la version del evaluador como procedencia consultable.
  - Consulta del resultado registrado y estado "pendiente" cuando no lo hay.
  - Disponibilidad inmediata de cada resultado y tolerancia a interrupcion.
  - Distincion entre resultado definitivo y falla transitoria del evaluador.
  - Distincion, dentro de la falla transitoria, entre la que afecta a un contenido
    puntual y la que afecta al servicio, con corte de consultas ante la segunda y
    sin abortar el analisis.
  - Convivencia, reuso e historia de resultados de versiones distintas del
    evaluador, con la regla de cual aplica cuando hay mas de uno para la misma
    huella.
  - Independencia respecto de los informes de analisis.
  - Re-evaluacion explicita del usuario, contenido por contenido, como unico
    mecanismo por el que un resultado se rehace.
- **Fuera de alcance**:
  - **Cualquier invalidacion automatica de resultados registrados**: por cambio de
    version del evaluador, por antiguedad o por cualquier otro hecho del sistema;
    ver [F-EVCOST-R008](#F-EVCOST-R008) y
    [DOUBT-IDENTIDAD-VERSION](#DOUBT-IDENTIDAD-VERSION).
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
contenido cambiado, resultado producido por otra version del evaluador (que se
reutiliza igual, declarando su procedencia) y contenido nunca evaluado.
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
          - when: "Hay un resultado registrado para ese evaluador y para la misma huella de contenido, producido por la version vigente"
            then: reusa
          - when: "Hay un resultado registrado, pero el contenido cambio desde que se lo evaluo (el elemento conserva su identificador)"
            then: contenido_cambiado
          - when: "Hay un resultado registrado para la misma huella, pero fue producido por una version anterior del evaluador"
            then: version_anterior
          - when: "No hay ningun resultado registrado para esa huella"
            then: primera_vez

      - id: reusa
        action: "El analisis obtiene el resultado registrado, junto con la version del evaluador que lo produjo, y el evaluador no recibe ninguna solicitud por ese contenido"
        gate: [F-EVCOST-R001, F-EVCOST-R003]
        result: success

      - id: contenido_cambiado
        action: "El resultado registrado no corresponde al contenido consultado: el contenido queda pendiente y, al evaluarse, se registra un resultado nuevo bajo la huella nueva, sin destruir el anterior"
        gate: [F-EVCOST-R002]
        result: success

      - id: version_anterior
        action: "El resultado se reutiliza igual —el evaluador no recibe ninguna solicitud y el contenido no queda pendiente— y la consulta informa que lo produjo la version anterior"
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
        action: "El contenido alcanzado se vuelve a evaluar pese a tener resultado registrado, la consulta pasa a devolver el resultado nuevo, y los resultados anteriores siguen consultables"
        gate: [F-EVCOST-R008]
        result: success
```

## Open Questions

<a id="DOUBT-IDENTIDAD-VERSION"></a>
### Doubt[DOUBT-IDENTIDAD-VERSION] - La version del evaluador, integra la identidad de un resultado o es solo procedencia?
**Status**: RESOLVED

Es la pregunta que decide **que invalida** un resultado ya pagado. Con la version
dentro de la identidad, retocar el evaluador deja pendientes de golpe todos sus
resultados; con la version afuera, ningun cambio del evaluador invalida nada y solo
un pedido explicito rehace un resultado.

- [x] Opcion A: **Solo el contenido determina la identidad** (evaluador + huella).
  La version se registra como procedencia consultable. Nada se invalida
  automaticamente; re-evaluar es siempre una accion explicita.
- [ ] Opcion B: **La version integra la identidad.** Cambiarla deja pendientes
  todos los resultados de la version anterior, que quedan como historia. Garantiza
  que un puntaje siempre corresponda a la version corriente, al precio de invalidar
  miles de resultados pagados ante cualquier ajuste.
- [ ] Opcion C: **Intermedia** — la version integra la identidad, pero el usuario
  puede declarar una version como compatible con la anterior para que sus
  resultados sigan aplicando.
- [ ] Opcion D: **Intermedia** — los resultados de versiones anteriores se reutilizan
  pero se marcan como "de otra version", y el consumidor decide si los puntua.

**Answer**: **Opcion A**, elegida por el usuario a conciencia y con la contra sobre
la mesa.

**El argumento a favor**: *"me interesa lo que se testea, no quien lo hace"*. Si un
contenido cumple o no lo que se le pregunta es una propiedad **del contenido**, no
de quien la juzgo. Cambiar de modelo —por costo, por disponibilidad, por una
migracion de proveedor— no deberia invalidar miles de veredictos ya pagados
(11.487 ejercicios en el curso real del primer consumidor).

**La contra que se planteo y se descarto**: la definicion del evaluador **es el
criterio de juicio**. Al corregir un evaluador que juzgaba mal, sus resultados
anteriores siguen aplicando siendo ya sabidamente incorrectos, y nadie los revisa a
menos que alguien se acuerde de pedir la re-evaluacion. Se ofrecieron ademas las dos
alternativas intermedias (C y D) y el usuario eligio explicitamente la mas simple.

**Lo que queda en pie de la contra**: el riesgo no desaparece, se traslada a una
accion del usuario. Por eso [F-EVCOST-R008](#F-EVCOST-R008) enuncia que la
re-evaluacion explicita es el **unico** mecanismo por el que un resultado se rehace,
y [F-EVCOST-R001](#F-EVCOST-R001) exige que la procedencia quede registrada y
consultable: sin ella, el usuario no tendria como saber que resultados vienen de la
version que acaba de corregir. Del lado del primer consumidor, esa procedencia se
declara en el informe
([F-QINST-R005](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R005)).

**No reabrir sin leer este bloque.** La decision es deliberada; lo que la haria
revisable es evidencia de que en la practica nadie pide la re-evaluacion tras
corregir un evaluador y los informes se llenan de resultados de versiones que se
sabe malas.

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

[F-EVCOST-R006](#F-EVCOST-R006) conserva como historia los resultados anteriores de
una misma huella —los que quedaron desplazados por una re-evaluacion posterior— y
prohibe borrarlos automaticamente. Con re-evaluaciones sucesivas sobre ~11.500
elementos, la historia crece sin techo.

- [ ] Opcion A: **Nunca se poda automaticamente.** El usuario puede pedir
  explicitamente descartar la historia anterior a un punto. Simple y sin sorpresas;
  el volumen queda a cargo del usuario.
- [ ] Opcion B: **Se conservan los ultimos N resultados por huella** y los
  anteriores se descartan solos. Acota el volumen; reintroduce una perdida
  automatica, que es justo lo que R006 prohibe.
- [ ] Opcion C: **Se conserva solo el resultado que aplica** y el desplazado se
  descarta. Volumen minimo; elimina la posibilidad de comparar como juzgo cada
  version.

**Answer**: Pendiente. Se especifica la Opcion A [ASSUMPTION]: el volumen que
preocupa es hipotetico (todavia no hubo una sola re-evaluacion sobre contenido ya
evaluado) y la comparacion entre versiones es hoy el unico modo de saber si un
cambio del evaluador lo mejoro. Si el volumen se vuelve un problema medible, la poda
explicita de la Opcion A ya lo resuelve sin cambiar ninguna regla.

**Nota**: el crecimiento que esta duda anticipaba se volvio mas lento tras
[DOUBT-IDENTIDAD-VERSION](#DOUBT-IDENTIDAD-VERSION). Antes, cada cambio de version
sumaba una copia entera del curso apenas se lo volviera a auditar; ahora solo suma
historia el contenido que alguien pide re-evaluar explicitamente.

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
  falla y no juicio, y afecta solo a ese ejercicio). Es ademas el primer consumidor
  de la procedencia: **F-QINST-R005** declara en el informe de que versiones vienen
  los veredictos que puntua. Citada por [F-EVCOST-R001](#F-EVCOST-R001),
  [F-EVCOST-R005](#F-EVCOST-R005), [F-EVCOST-R008](#F-EVCOST-R008),
  [F-EVCOST-R009](#F-EVCOST-R009), el Alcance, el Contexto y
  [DOUBT-IDENTIDAD-VERSION](#DOUBT-IDENTIDAD-VERSION).
- **FEAT-RPRES** — Establece que corregir un elemento del curso cambia su contenido
  pero no su identificador, que es la razon por la que la identidad de un resultado
  es la huella del contenido y no el identificador. Citada por el Contexto.
