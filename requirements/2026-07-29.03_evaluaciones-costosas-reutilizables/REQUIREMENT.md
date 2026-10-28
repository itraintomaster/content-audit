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
huella del contenido y el runtime que lo produjo, lo reutiliza solo dentro de esa
identidad efectiva y conserva toda la historia aunque la corrida se interrumpa.

**Por que**: Sin esto, cada analisis que se apoya en un evaluador lento y pagado
lo vuelve a pagar entero, y una caida a mitad de camino pierde todo el trabajo.

## Reglas de Negocio

<a id="F-EVCOST-R001"></a>
### Rule[F-EVCOST-R001] - La identidad de un resultado incluye evaluador, huella y runtime efectivo
**Severity**: critical | **Validation**: VALIDATED

> Cada resultado registrado se identifica por **que evaluador** lo produjo, la
> **huella del contenido** que se le entrego y la identidad del **runtime
> efectivo**: proveedor, modelo exacto y esfuerzo de razonamiento. Solo aplica a
> consultas con esa misma identidad efectiva. La version del proceso se conserva
> como procedencia consultable; el identificador del elemento del curso no integra
> la identidad.

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

**Por que el runtime efectivo SI integra la identidad.** Cambiar de Claude a Codex,
de un modelo a otro o de un esfuerzo de razonamiento a otro puede cambiar el juicio
sobre el mismo contenido. Un resultado producido con una de esas combinaciones no
demuestra que la otra combinacion juzgue igual. Por eso proveedor, modelo exacto y
esfuerzo de razonamiento se resuelven antes de consultar el registro y deben
coincidir los tres para reutilizar.

Un proveedor que no ofrece una perilla de esfuerzo registra un valor explicito
equivalente a "no aplica"; no omite el dato. Omitirlo convierte al registro en
legacy de procedencia incompleta, no en compatible con cualquier esfuerzo.

**Por que la version del proceso sigue siendo procedencia.** Dentro de un mismo
runtime efectivo, la version de la guia, definicion o proceso que produjo el juicio
queda registrada y permite comparar resultados, pero no invalida por si sola lo ya
pagado. Rehacer esos resultados sigue siendo una accion explicita
([F-EVCOST-R008](#F-EVCOST-R008)). Esta separacion revisa de manera acotada la
decision original de [DOUBT-IDENTIDAD-VERSION](#DOUBT-IDENTIDAD-VERSION): la version
del proceso queda fuera de la identidad; el runtime que efectivamente juzgo, no.

**Los registros legacy no se inventan.** Un resultado anterior sin proveedor,
modelo o esfuerzo de razonamiento permanece visible como historia con
"procedencia desconocida". No se atribuye retroactivamente a Claude, Codex ni a
ningun runtime concreto, no cuenta como cobertura de ellos y no se reutiliza para
una corrida que resolvio una identidad efectiva. La procedencia incompleta se
conserva precisamente para que la historia no desaparezca ni se vuelva una certeza
falsa.

**Criterio de aceptacion**: (a) dos contenidos identicos evaluados por el mismo
evaluador y runtime efectivo encuentran el mismo resultado registrado, aunque
pertenezcan a elementos distintos del curso; (b) un elemento cuyo contenido cambio
no encuentra resultado registrado, aunque conserve su identificador; (c) dentro
del mismo runtime efectivo, un resultado de la version A se encuentra cuando la
version del proceso vigente es B y declara que lo produjo A; (d) un resultado del
runtime Claude/modelo X/esfuerzo Y nunca se devuelve a Codex, a otro modelo ni a
otro esfuerzo; (e) un registro legacy sin procedencia sigue visible, pero ninguna
corrida concreta lo reutiliza.

**Error**: "El resultado registrado para el evaluador '{evaluador}' no corresponde al contenido o al runtime efectivo consultado"

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
> registrado para ese evaluador, esa huella y el **mismo runtime efectivo**. Si
> existe, lo devuelve —junto con su procedencia
> ([F-EVCOST-R001](#F-EVCOST-R001))— y **no** consulta al evaluador. Si solo hay
> resultados de otro runtime o legacy sin procedencia, no los reutiliza y el
> contenido queda **pendiente** para el runtime elegido.

<details><summary>Detalle</summary>

"Pendiente" es un estado explicito, no un error: significa que el resultado
todavia no se conoce y que una corrida futura puede conocerlo. Que se haga con
los pendientes —evaluarlos ahora, dejarlos para la corrida siguiente, informarlos
como cobertura faltante— lo decide cada consumidor de esta capacidad, no esta
regla.

**Pendiente siempre se lee dentro de un runtime efectivo.** Significa "no existe un
resultado reutilizable para esta huella bajo el proveedor, modelo y esfuerzo hoy
elegidos". Puede haber historia de otro runtime —o legacy sin procedencia— y seguir
pendiente para el actual. Dentro del mismo runtime, cambiar solo la version del
proceso, el paso del tiempo o borrar informes no lo vuelve pendiente; rehacerlo
requiere un pedido explicito ([F-EVCOST-R008](#F-EVCOST-R008)).

La reutilizacion **no tiene tope**: cualquier cantidad de resultados registrados
se reutiliza sin costo. Los limites que un consumidor imponga se aplican sobre
las evaluaciones nuevas, nunca sobre la reutilizacion.

**Criterio de aceptacion**: (a) con un resultado ya registrado para la huella
consultada y el mismo runtime efectivo, la consulta lo devuelve —con la procedencia
que lo produjo— y el evaluador no recibe ninguna solicitud; (b) eso vale tambien
cuando la version del proceso vigente es distinta; (c) con resultados solo de otro
runtime o legacy sin procedencia, la consulta informa que el contenido esta
pendiente para el runtime elegido y no consume esos resultados.

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
### Rule[F-EVCOST-R006] - Los runtimes conviven sin mezclar resultados ni borrar historia
**Severity**: critical | **Validation**: VALIDATED

> Los resultados de distintos proveedores, modelos, esfuerzos y versiones del
> proceso **conviven** sin sobrescribirse ni borrarse. Una corrida reutiliza solo
> los del runtime efectivo elegido; dentro de ese runtime, si hay mas de uno para
> el mismo evaluador y huella, aplica el registrado mas recientemente. Los demas
> quedan como historia consultable, incluida la historia legacy sin procedencia.

<details><summary>Detalle</summary>

Cambiar la guia o la definicion de un evaluador dentro del mismo runtime es una
operacion frecuente y barata; volver a evaluar todo el contenido con la version
nueva es lenta y cara. Por eso el cambio de version del proceso no dispara por si
solo una re-evaluacion. Cambiar proveedor, modelo o esfuerzo es distinto: selecciona
otro runtime y, por lo tanto, otra cobertura; no borra ni invalida la historia del
anterior, pero tampoco la usa como si la hubiera producido el nuevo.

La convivencia permite comparar Claude y Codex —o dos modelos o esfuerzos— lado a
lado sin que el mas reciente pise al otro. Un resultado legacy sin identidad
efectiva ocupa una categoria separada de historia: no se lo reclasifica a partir de
suposiciones externas y nunca gana frente a un resultado atribuible al runtime
elegido.

**Por que gana el mas reciente dentro del mismo runtime.** Es lo unico compatible con
[F-EVCOST-R008](#F-EVCOST-R008): si tras una re-evaluacion explicita la consulta
siguiera devolviendo el resultado viejo, pedir la re-evaluacion no tendria efecto
observable y el usuario habria pagado por nada. La regla no borra el anterior —lo
conserva como historia— pero fija sin ambiguedad cual de los dos aplica.

Conservar la historia tiene valor propio: permite comparar como juzgo el mismo
contenido cada version del evaluador, que es la unica forma de saber si un cambio
lo mejoro. Esa comparacion solo es posible porque cada resultado lleva registrada
su procedencia ([F-EVCOST-R001](#F-EVCOST-R001)).

**Criterio de aceptacion**: (a) registrados resultados con la version A, se pasa a
la version B sin cambiar runtime: los resultados de A se siguen reutilizando y la
consulta declara su version; (b) al pasar de Claude a Codex, de modelo o de esfuerzo,
los resultados anteriores siguen visibles pero el contenido queda pendiente para
el runtime nuevo; (c) tras una re-evaluacion explicita dentro de un mismo runtime,
la consulta devuelve el resultado mas reciente y el anterior sigue consultable;
(d) los legacy permanecen visibles como "procedencia desconocida" y no se cuentan
ni reutilizan como Claude o Codex.

**Error**: "Se intento reutilizar para '{runtimeActual}' un resultado producido por '{runtimeRegistrado}'" / "El cambio de runtime elimino resultados historicos del evaluador '{evaluador}'"

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
analisis nuevo sobre el mismo contenido y runtime reutiliza los resultados
registrados y no consulta al evaluador.

**Error**: "Los resultados registrados del evaluador '{evaluador}' se perdieron al eliminar informes de analisis"

</details>

<a id="F-EVCOST-R008"></a>
### Rule[F-EVCOST-R008] - La re-evaluacion explicita es el unico modo de rehacer un resultado
**Severity**: critical | **Validation**: VALIDATED

> Un resultado ya registrado se rehace **unicamente** cuando el usuario pide
> explicitamente volver a evaluar ese contenido: es el **unico** mecanismo, y no
> existe ningun otro **dentro del mismo runtime efectivo**. Ningun hecho del
> sistema —cambiar la version del proceso, borrar informes, cambiar la configuracion
> del analisis, el paso del tiempo— lo dispara, lo invalida ni lo vuelve a dejar
> pendiente en ese runtime. Elegir otro proveedor, modelo o esfuerzo no rehace ni
> invalida el resultado: consulta otra cobertura. La re-evaluacion registra el
> resultado nuevo **sin destruir** los anteriores, y a partir de entonces la
> consulta devuelve el nuevo ([F-EVCOST-R006](#F-EVCOST-R006)). El **alcance** lo
> acota el consumidor, contenido por contenido: esta capacidad vuelve a evaluar el
> contenido puntual que se le pide y **ningun otro**, de modo que el contenido
> vecino con resultado registrado se sigue reutilizando en esa misma corrida.
> Volver a medir desde el tablero pide la re-evaluacion solo del contenido cuyo resultado es
> de una version anterior ([F-SIGUE-R002](../2026-10-28.01_la-medicion-sigue/REQUIREMENT.md#F-SIGUE-R002)).

<details><summary>Detalle</summary>

Esta regla es la contracara de todas las anteriores: si el sistema puede decidir
por su cuenta volver a evaluar, el ahorro deja de ser predecible y el gasto deja
de ser gobernable. Un usuario tiene que poder cambiar el evaluador, limpiar
informes y correr analisis sin miedo a desatar una factura.

**Que sea el unico mecanismo es una exigencia, no una consecuencia.** Dentro del
mismo runtime, con la version del proceso fuera de la identidad
([F-EVCOST-R001](#F-EVCOST-R001)), no queda otra via por la cual un resultado
registrado deje de aplicar: no hay invalidacion automatica ni caducidad. Seleccionar
otro runtime es una decision explicita distinta: sus resultados faltantes se pagan
porque el operador eligio evaluarlos con esa combinacion, no porque la historia
anterior haya sido borrada.

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
consultar al evaluador por contenido con resultado registrado para el **mismo
runtime efectivo**, cualquiera sea la version del proceso que lo produjo; (b) con
pedido explicito sobre un contenido, ese contenido se vuelve a evaluar, la consulta
pasa a devolver el resultado nuevo, los anteriores siguen consultables, y el resto
del contenido reutilizable bajo ese runtime no se consulta.

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
> Una saturacion del modelo se espera antes de contarla como falla del servicio
> ([F-SIGUE-R001](../2026-10-28.01_la-medicion-sigue/REQUIREMENT.md#F-SIGUE-R001)).

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

Por eso el componente de contenido de la identidad es la huella de lo
efectivamente evaluado
([F-EVCOST-R001](#F-EVCOST-R001), [F-EVCOST-R002](#F-EVCOST-R002)): describe
**que se juzgo**, no **sobre que elemento**. Trae ademas un beneficio no buscado:
dos elementos con contenido identico bajo el mismo runtime comparten resultado y se
pagan una sola vez.

### Runtime efectivo y version del proceso

Queda una segunda pregunta, independiente de la huella: si cambia quien ejecuta el
juicio, ¿siguen aplicando los resultados? La respuesta ahora separa dos cambios.
Cambiar la version de la guia o proceso dentro del mismo runtime no invalida lo ya
pagado; esa version queda como procedencia. Cambiar proveedor, modelo exacto o
esfuerzo de razonamiento selecciona otro runtime efectivo: la historia anterior
permanece visible, pero no cubre al runtime nuevo ni se reutiliza como si fuera suya.

Lo que si sigue dentro de la identidad es **que evaluador** produjo el resultado, y
no contradice el criterio anterior: no identifica quien juzga sino **que pregunta se
hizo**. El juicio de un futuro evaluador de calidad de traduccion sobre un contenido
no sustituye al de un evaluador de cumplimiento de consigna sobre el mismo
contenido.

Esta decision permite comparar runtimes sin mezclarlos y, a la vez, evita que cada
retoque de una guia dispare una factura sorpresa. Ver la revision de
[DOUBT-IDENTIDAD-VERSION](#DOUBT-IDENTIDAD-VERSION).

## Alcance

- **En alcance**:
  - Registro de resultados de evaluacion costosa por evaluador, huella de contenido
    e identidad efectiva de runtime, con la version del proceso como procedencia.
  - Consulta del resultado registrado y estado "pendiente" cuando no lo hay.
  - Disponibilidad inmediata de cada resultado y tolerancia a interrupcion.
  - Distincion entre resultado definitivo y falla transitoria del evaluador.
  - Distincion, dentro de la falla transitoria, entre la que afecta a un contenido
    puntual y la que afecta al servicio, con corte de consultas ante la segunda y
    sin abortar el analisis.
  - Convivencia e historia de runtimes y versiones distintas, con reuso limitado al
    runtime elegido y la regla de cual aplica cuando hay mas de uno para la huella.
  - Independencia respecto de los informes de analisis.
  - Re-evaluacion explicita del usuario, contenido por contenido, como unico
    mecanismo por el que un resultado se rehace.
- **Fuera de alcance**:
  - **Cualquier invalidacion automatica de resultados registrados dentro de un
    mismo runtime**: por cambio de version del proceso, antiguedad u otro hecho;
    ver [F-EVCOST-R008](#F-EVCOST-R008).
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

Cubre las cinco respuestas posibles a "ya tengo este resultado?": reuso, contenido
cambiado, resultado de otra version del proceso dentro del mismo runtime (se
reutiliza), resultado de otro runtime o legacy (no se reutiliza) y contenido nunca
evaluado.
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
          - when: "Hay un resultado registrado para ese evaluador, huella y runtime efectivo"
            then: reusa
          - when: "Hay un resultado registrado, pero el contenido cambio desde que se lo evaluo (el elemento conserva su identificador)"
            then: contenido_cambiado
          - when: "Hay un resultado registrado para la misma huella, pero fue producido por una version anterior del evaluador"
            then: version_anterior
          - when: "Hay un resultado para la misma huella, pero pertenece a otro runtime o no declara su procedencia"
            then: otro_runtime
          - when: "No hay ningun resultado registrado para esa huella"
            then: primera_vez

      - id: reusa
        action: "El analisis obtiene el resultado registrado, junto con su runtime y version del proceso, y el evaluador no recibe ninguna solicitud por ese contenido"
        gate: [F-EVCOST-R001, F-EVCOST-R003]
        result: success

      - id: contenido_cambiado
        action: "El resultado registrado no corresponde al contenido consultado: el contenido queda pendiente y, al evaluarse, se registra un resultado nuevo bajo la huella nueva, sin destruir el anterior"
        gate: [F-EVCOST-R002]
        result: success

      - id: version_anterior
        action: "Como pertenece al mismo runtime, el resultado se reutiliza igual y la consulta informa que lo produjo la version anterior"
        gate: [F-EVCOST-R006]
        result: success

      - id: otro_runtime
        action: "El resultado historico sigue visible, pero no cubre al runtime elegido; el contenido queda pendiente para ese runtime"
        gate: [F-EVCOST-R001, F-EVCOST-R003, F-EVCOST-R006]
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
**Status**: RESOLVED — REVISADA 2026-10-05

**Revision 2026-10-05**: la decision original se conserva abajo como historia,
pero ya no alcanza para distinguir resultados de Claude y Codex. José resolvio que
la identidad efectiva **si** incluye proveedor, modelo exacto y esfuerzo de
razonamiento; la version de la guia o proceso sigue siendo procedencia dentro de
ese runtime. Los registros legacy sin esos datos permanecen visibles, pero no se
atribuyen ni reutilizan para un runtime concreto
([F-EVCOST-R001](#F-EVCOST-R001)).

Es la pregunta que decide **que invalida** un resultado ya pagado. Con la version
dentro de la identidad, retocar el evaluador deja pendientes de golpe todos sus
resultados; con la version afuera, ningun cambio del evaluador invalida nada y solo
un pedido explicito rehace un resultado.

- [ ] Opcion A (decision original, reemplazada el 5/10): **Solo el contenido determina la identidad** (evaluador + huella).
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
- [x] Opcion E (vigente): **El runtime efectivo integra la identidad** — proveedor,
  modelo exacto y esfuerzo deben coincidir; la version del proceso es procedencia.

**Answer vigente**: **Opcion E** (José, 5/10). Reemplaza la Opcion A solo en cuanto
a quien ejecuta el juicio: no mezcla runtimes, conserva el reuso entre versiones
del proceso dentro de uno y mantiene toda la historia separada.

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

**Historia de la decision original.** Los parrafos anteriores documentan por que se
eligio A el 29/7. No autorizan a atribuir un registro sin procedencia a un runtime ni
a mezclar proveedores, modelos o esfuerzos despues de la revision del 5/10.

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
