---
feature:
  id: FEAT-QICOR
  code: F-QICOR
  name: Correccion del incumplimiento de consigna de un ejercicio
  priority: high
---

# Correccion del incumplimiento de consigna de un ejercicio

## TL;DR

**Que**: Una tarea de incumplimiento de consigna se convierte en una correccion
propuesta del ejercicio —minima, que no empeora lo que ya estaba bien y que
cumple la consigna— revisable y aprobable con el mismo flujo que las demas.

**Por que**: Hoy el incumplimiento se detecta, se puntua y llega al plan como
tarea, pero ahi se detiene: no hay forma de pedirle al sistema que proponga como
arreglarlo, y el operador queda con una lista de problemas sin una sola
correccion que aprobar.

## Reglas de Negocio

Las reglas se leen en cuatro tramos:

- **Del problema a la propuesta** (R001, R002, R003): la tarea se convierte en
  una correccion, construida sobre lo que la violacion señala, y acotada a eso.
- **Antes de tocar nada** (R009): el diagnostico se revalida contra el criterio
  vigente, y si ya no se sostiene la tarea se cierra sin corregir nada.
- **Que tiene que valer la correccion** (R004, R005, R006): no empeora lo que
  estaba bien, cumple la consigna, y cuando no se logra ninguna aceptable se dice.
- **Que hace el operador con ella y cuanto cuesta** (R007, R008): aprobar y
  rechazar con el mismo vocabulario de siempre, y el gasto acotado por corrida.

<a id="F-QICOR-R001"></a>
### Rule[F-QICOR-R001] - Una tarea de incumplimiento de consigna produce una correccion propuesta y revisable
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cuando el operador pide revisar una tarea cuyo diagnostico es el incumplimiento
> de consigna, el sistema produce una **propuesta de correccion del ejercicio**
> —el mismo ejercicio, corregido— con el mismo vocabulario de decision que
> cualquier otra: estado anterior, estado propuesto, pendiente / aprobada /
> rechazada.

<details><summary>Detalle</summary>

Este es el bloqueo que la feature resuelve. Hoy la tarea existe
([F-QINST-R017](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R017)),
se prioriza y se lista, pero pedirle una revision no produce ninguna correccion:
cae al comportamiento de identidad que el sistema reserva para los diagnosticos
todavia sin correccion dedicada
([F-REVBYP-R004](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md#F-REVBYP-R004)).
El operador ve el problema y no tiene nada que aprobar.

El objeto corregido es el **ejercicio**, no su knowledge: la consigna se da por
buena y lo que se ajusta es el ejercicio que la incumple. Eso pone a esta
correccion en el camino que el sistema ya recorre para las correcciones de
ejercicio, y no en el que abrio la correccion de etiqueta de knowledge
([FEAT-KTLR](../2026-07-06.01_revisar-knowledge-title-length-con-agente/REQUIREMENT.md)).

**Criterio de aceptacion**: pedida la revision de una tarea de incumplimiento de
consigna, queda una propuesta cuyo estado anterior es el ejercicio actual del
curso y cuyo estado propuesto es un ejercicio distinto de el; hoy la propuesta
resultante es identica al original.

**Error**: "La revision de la tarea de incumplimiento de consigna del ejercicio '{quizId}' no produjo ninguna correccion"

</details>

<a id="F-QICOR-R002"></a>
### Rule[F-QICOR-R002] - La correccion se prepara sobre las violaciones señaladas, con su evidencia
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La informacion con la que se prepara la correccion incluye, ademas del
> ejercicio completo y de la consigna que incumple, **cada violacion detectada
> con su restriccion incumplida, su evidencia textual y su explicacion**, el
> [Nivel CEFR](glossary:Nivel CEFR) del ejercicio y los demas ejercicios de su
> knowledge. Las violaciones no son un adorno del informe: son la materia prima
> de la correccion.

<details><summary>Detalle</summary>

| Dato de partida | Para que se necesita |
|---|---|
| Ejercicio completo: sus partes, sus huecos y todas sus opciones aceptadas | Es lo que hay que corregir, y lo que hay que conservar donde no se toca ([F-QICOR-R003](#F-QICOR-R003)) |
| Consigna del knowledge (etiqueta e instrucciones) | Es el criterio que el ejercicio tiene que pasar a cumplir ([F-QICOR-R005](#F-QICOR-R005)) |
| Violaciones: restriccion incumplida, evidencia textual, explicacion | Dicen **que** se incumplio y **donde** se ve; sin ellas la correccion adivina |
| Nivel CEFR del ejercicio | Fija el rango de longitud y el vocabulario esperados ([F-QICOR-R004](#F-QICOR-R004)) |
| Los demas ejercicios del mismo knowledge | Permiten no producir un ejercicio que se confunda con sus hermanos ([F-QICOR-R004](#F-QICOR-R004), criterio 3) |

Los tres primeros datos de cada violacion son exactamente los que el diagnostico
de la validacion de consigna ya deja registrados por ejercicio
([F-QINST-R003](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R003)),
que se escribieron pensando en este momento. Esta regla exige que lleguen
completos a la correccion: perder la evidencia por el camino convierte una
correccion dirigida en una reescritura a ciegas, que es justamente lo que
[F-QICOR-R003](#F-QICOR-R003) prohibe.

Que **otros** datos se agreguen (ejercicios de otros knowledges, historial de
correcciones previas) queda abierto; esta regla fija el minimo sin el cual la
correccion no puede ser dirigida ni verificable.

**Criterio de aceptacion**: preparada la correccion de una tarea cuyo ejercicio
tiene dos violaciones registradas, la informacion de partida contiene las dos,
cada una con su restriccion, su evidencia y su explicacion pobladas, ademas del
ejercicio completo, la consigna, el nivel y los ejercicios hermanos.

**Error**: "La correccion del ejercicio '{quizId}' se preparo sin las violaciones que registro su diagnostico de consigna"

</details>

<a id="F-QICOR-R003"></a>
### Rule[F-QICOR-R003] - Corregir no es reescribir: el cambio se limita a lo que la violacion señala
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La correccion propuesta modifica **unicamente** lo que las violaciones señalan.
> Todo lo demas del ejercicio —las partes que ninguna violacion menciona, los
> huecos no implicados y sus opciones aceptadas, y la traduccion cuando la
> correccion no la alcanza— aparece en el estado propuesto **identico** al
> estado anterior.

<details><summary>Detalle</summary>

**El defecto que la motiva ya ocurrio.** Al corregir un aspecto de un ejercicio,
el sistema reescribia la oracion entera y arrastraba de cuajo lo que estaba bien:
la correccion resolvia su objetivo y de paso cambiaba el sujeto, el objeto y el
registro del ejercicio, produciendo un cambio que nadie pidio y que el operador
tiene que revisar entero para aprobar. El mismo criterio ya rige la correccion de
vocabulario, donde la edicion minima esta explicitamente enunciada
([F-LASAG-R014](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R014)).

**Por que importa mas aca que en otras correcciones.** Una violacion de consigna
suele apuntar a un lugar muy concreto —la forma de la respuesta aceptada, el
tiempo verbal del hueco, la cantidad de palabras que admite—. Corregir eso es una
edicion quirurgica; reescribir la oracion completa cambia todo lo que **no** se
juzgo mal, y con ello arriesga cada uno de los criterios de
[F-QICOR-R004](#F-QICOR-R004) sin ninguna necesidad. Cuanto mas chico es el
cambio, mas barato es revisarlo y menos superficie hay para empeorar.

**Que no exige esta regla.** No exige que el cambio sea de una sola palabra ni
fija un tamaño maximo: una violacion puede señalar legitimamente la oracion
entera. Lo que exige es que aquello que **ninguna** violacion menciona vuelva
intacto. Cuando la correccion no logra resolver la violacion sin tocar el resto,
lo que corresponde es que ese candidato no pase los criterios y se vuelva a
intentar, no que se relaje esta regla.

**Criterio de aceptacion**: dado un ejercicio de varias partes cuya unica
violacion señala una de ellas, en la propuesta resultante esa parte cambia y las
restantes —y las opciones aceptadas de los huecos no implicados— son identicas,
caracter por caracter, a las del ejercicio original.

**Error**: "La correccion del ejercicio '{quizId}' modifico contenido que ninguna violacion de su consigna señala"

</details>

<a id="F-QICOR-R004"></a>
### Rule[F-QICOR-R004] - No empeorar: el ejercicio corregido conserva todo lo que ya cumplia
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Antes de proponerse, el ejercicio corregido se somete a un catalogo fijo de
> criterios, cada uno con su propio veredicto. Reprobar cualquiera de ellos
> impide proponer ese candidato:
> 1. **Longitud dentro del rango objetivo de su nivel**, medida con el mismo
>    criterio de longitud que aplica la auditoria.
> 2. **Vocabulario adecuado al nivel**: no introduce palabras mal ubicadas para
>    el nivel ni fuera de catalogo que el ejercicio original no tuviera ya.
> 3. **Distincion**: no se confunde con los demas ejercicios de su knowledge ni
>    con el resto del curso.
> 4. **Traduccion fiel** al español de la oracion propuesta.
> 5. **Sigue siendo resoluble** por el alumno y conserva el tipo de ejercicio del
>    original.

<details><summary>Detalle</summary>

**Por que una sola regla y no cinco.** Los cinco criterios enuncian una unica
exigencia —que la correccion no cobre su arreglo empeorando otra cosa— y se
observan de la misma forma: sometiendo el ejercicio corregido a cada criterio y
mirando su veredicto. Separarlos en cinco reglas repetiria cinco veces el mismo
enunciado cambiando el sustantivo. Lo que si es propio de cada uno es **como se
verifica**:

| Criterio | Como se verifica |
|---|---|
| 1. Longitud en rango | Deterministico, con la misma medicion de longitud de la auditoria ([F-SLEN-R002](../2026-03-18.01_sentence-length/REQUIREMENT.md#F-SLEN-R002)) |
| 2. Vocabulario del nivel | Deterministico, con el mismo analisis lexico de la auditoria ([FEAT-CLEX](../2026-07-22.01_consulta-lexica-de-oracion-por-cli/REQUIREMENT.md)): los [lemas](glossary:Lema) señalados del corregido estan contenidos en los del original |
| 3. Distincion | Comparacion contra los demas ejercicios; ver [DOUBT-DISTINCION-ALCANCE](#DOUBT-DISTINCION-ALCANCE) |
| 4. Traduccion fiel | Juicio sobre el par oracion/traduccion propuesto |
| 5. Resoluble y del mismo tipo | Juicio sobre el ejercicio propuesto completo |

**Que los criterios 1 y 2 se verifiquen con la medicion real de la auditoria, y
no con una aproximacion, no es un detalle.** Una correccion aprobada con una
regla de medir distinta de la que despues la juzga produce el peor desenlace
posible: el operador aprueba, la auditoria siguiente vuelve a marcar el mismo
ejercicio, y el trabajo se hizo dos veces. Es la leccion que ya dejaron las
correcciones de vocabulario
([F-LASAG-R007](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R007)),
donde una correccion reemplazo una palabra fuera de catalogo por otra mal ubicada
para el nivel y nadie lo noto hasta la auditoria siguiente.

**Los flags preexistentes no bloquean.** El criterio 2 exige no **introducir**
problemas nuevos, no arreglar los viejos: una palabra que ya estaba mal ubicada
en el original y que la correccion no toco sigue estandolo, y eso no invalida la
correccion. Exigir lo contrario volveria incorregible a todo ejercicio con
residuo previo, que es la mayoria.

**Criterio de aceptacion**: (a) un candidato cuya longitud cae fuera del rango de
su nivel no se propone; (b) un candidato que introduce una palabra mal ubicada
para el nivel, ausente en el original, no se propone; (c) un candidato que
conserva un problema lexico que ya tenia el original y no agrega ninguno no queda
bloqueado por el criterio 2; (d) la propuesta que finalmente se emite pasa los
cinco criterios.

**Error**: "La correccion propuesta para el ejercicio '{quizId}' reprueba el criterio '{criterio}' que el ejercicio original cumplia"

</details>

<a id="F-QICOR-R005"></a>
### Rule[F-QICOR-R005] - El resultado cumple la consigna, verificado con el mismo criterio que detecto el incumplimiento
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Antes de proponerse, el ejercicio corregido se somete a la **misma validacion
> de consigna** que produjo el incumplimiento, y solo se propone si esta declara
> que cumple. No alcanza con que las violaciones parezcan atendidas: la
> correccion no puede pasar y despues fallar la auditoria.

<details><summary>Detalle</summary>

**Por que se verifica antes de proponer y no despues.** Una propuesta que el
operador aprueba y que la auditoria siguiente vuelve a marcar es peor que ninguna
propuesta: consume una revision humana, altera el curso, y deja la tarea de vuelta
en el plan con el operador convencido de haberla resuelto. Verificar antes cuesta
una consulta mas por correccion; no verificar cuesta una revision humana y una
correccion que hay que deshacer o rehacer. Ver
[DOUBT-COSTO-VERIFICACION](#DOUBT-COSTO-VERIFICACION).

**La verificacion cubre el ejercicio entero, no las violaciones una por una.**
Un ejercicio con tres violaciones cuya correccion resuelve dos sigue incumpliendo
su consigna, y esta regla lo trata como lo que es: un candidato que no se propone
([F-QICOR-R006](#F-QICOR-R006)). Contar violaciones resueltas seria un progreso
invisible para el alumno, que sigue teniendo delante un ejercicio que no hace lo
que promete.

**Consecuencia buscada sobre la auditoria siguiente.** Al cambiar el contenido del
ejercicio, el veredicto de consigna registrado deja de aplicarle y la auditoria
siguiente lo vuelve a juzgar
([F-QINST-R009](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R009)).
Esa segunda opinion es deseable —es la que confirma que la correccion sirvio— pero
llega tarde para decidir si proponerla, y por eso no reemplaza a esta regla.

**Criterio de aceptacion**: (a) la propuesta emitida, sometida a la validacion de
consigna, obtiene veredicto de cumplimiento; (b) un candidato que resuelve solo
parte de las violaciones de su ejercicio no se propone.

**Error**: "Se propuso para el ejercicio '{quizId}' una correccion que sigue incumpliendo su consigna"

</details>

<a id="F-QICOR-R006"></a>
### Rule[F-QICOR-R006] - No lograr una correccion aceptable es un desenlace explicito, no un silencio ni una propuesta peor
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Si tras agotar los intentos ningun candidato pasa los criterios de
> [F-QICOR-R004](#F-QICOR-R004) y [F-QICOR-R005](#F-QICOR-R005), el sistema **no
> propone** ese candidato: informa explicitamente que no logro una correccion
> aceptable para ese ejercicio y **nombra los criterios que reprobaron**. El
> ejercicio del curso no cambia y la tarea queda disponible para volver a
> intentarse. Este desenlace supone que **habia** algo que corregir; cuando el
> diagnostico ya no se sostiene no hubo fracaso alguno y el desenlace es otro
> ([F-QICOR-R009](#F-QICOR-R009)).

<details><summary>Detalle</summary>

Hay dos desenlaces prohibidos, y esta regla existe para cerrarlos a los dos:

- **Callarse**: terminar la revision sin propuesta y sin explicacion. El operador
  no distingue "no hacia falta corregir nada" de "no pude". Es la misma clase de
  defecto silencioso que ya costo caro en la validacion de consigna, donde un
  pedido ignorado sin aviso se veia igual que uno atendido
  ([F-QINST-R015](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R015)).
- **Entregar lo mejor que salio, sin decirlo**: presentar como propuesta un
  ejercicio que reprueba un criterio deja al operador aprobando una regresion que
  cree una mejora. Aca esta feature se aparta deliberadamente del mejor-esfuerzo
  que rige la generacion de ejercicios por ausencia de lema
  ([F-LASAG-R009](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R009)):
  alli el ejercicio que se emite es **nuevo** y compite contra la nada; aca compite
  contra un ejercicio existente que ya funcionaba en todo salvo en su consigna, y
  el piso a superar es ese, no cero. Ver
  [DOUBT-SIN-CORRECCION](#DOUBT-SIN-CORRECCION).

**Nombrar los criterios reprobados es parte de la regla, no un adorno.** "No pude
corregirlo" no le dice al operador si conviene reintentar, corregir a mano o
revisar la consigna del knowledge; "no pude sin dejar la oracion fuera del rango
de longitud del nivel" si.

Esta regla cubre el caso en que la correccion **se intento y no alcanzo**. Una
interrupcion antes de intentarlo —el servicio no responde, se agota el tiempo de
espera— tambien termina sin propuesta y sin tocar el curso, con el mismo criterio
que ya rige las demas correcciones
([F-KTLR-R010](../2026-07-06.01_revisar-knowledge-title-length-con-agente/REQUIREMENT.md#F-KTLR-R010));
lo propio de esta regla es que aca **hubo** candidatos y todos reprobaron.

**No confundir este desenlace con el de un diagnostico caido.** Cuando la
revalidacion previa dice que el ejercicio ya cumple
([F-QICOR-R009](#F-QICOR-R009)) no se genero ningun candidato, no reprobo ningun
criterio y no hay nada que informar como fallo: la tarea se cierra porque el
problema no existe. Mezclar los dos desenlaces en un mismo recuento le diria al
operador que tiene decenas de correcciones imposibles cuando lo que tiene son
decenas de diagnosticos viejos que se cayeron solos — dos hechos ante los que
reacciona de forma opuesta.

**Criterio de aceptacion**: agotados los intentos sin candidato aceptable, la
revision termina informando el fallo y nombrando al menos un criterio reprobado,
no queda ninguna propuesta pendiente para esa tarea, el ejercicio del curso es
identico al original, y la tarea sigue disponible.

**Error**: "No se logro una correccion aceptable para el ejercicio '{quizId}': reprobaron {criterios}"

</details>

<a id="F-QICOR-R007"></a>
### Rule[F-QICOR-R007] - Aprobar deja el ejercicio corregido en el curso; rechazar no cambia nada
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La propuesta se decide con el mismo vocabulario y el mismo flujo que las demas
> correcciones de ejercicio. Aprobada, el ejercicio del curso queda con el
> contenido corregido y la tarea se marca como resuelta. Rechazada, el curso no
> se modifica y la tarea vuelve a quedar disponible. En ninguno de los dos casos
> cambia nada fuera del ejercicio de la tarea.

<details><summary>Detalle</summary>

Aprobar y rechazar no se reinventan: son los que el sistema ya ofrece
([FEAT-REVAPR](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md)), y el
objeto corregido —un ejercicio— es el que el sistema ya sabe escribir en el curso.
Que esta correccion entre por ese camino y no por uno propio es la mitad del valor
de la feature: el operador aprueba una correccion de consigna igual que aprueba
una de longitud, sin aprender nada nuevo.

El radio de impacto de la escritura no lo fija esta regla: lo fija
[FEAT-RPRES](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md),
que ya exige que aplicar una correccion conserve intacto todo lo que la revision
no toco. Esta regla se apoya en eso y agrega lo unico que le es propio: que el
contenido corregido efectivamente llegue al curso y que rechazar no deje rastro.

**Criterio de aceptacion**: (a) aprobada la propuesta y releido el curso, el
ejercicio tiene el contenido corregido y la tarea figura resuelta; (b) rechazada,
el ejercicio conserva su contenido original y la tarea vuelve a estar disponible;
(c) en los dos casos, los demas ejercicios del knowledge quedan sin cambios.

**Error**: "Se aprobo la correccion de consigna del ejercicio '{quizId}' y el curso conserva su contenido anterior"

</details>

<a id="F-QICOR-R008"></a>
### Rule[F-QICOR-R008] - Cada corrida acota cuantas correcciones intenta y declara lo que quedo sin corregir
**Severity**: major | **Validation**: AUTO_VALIDATED

> Corregir un ejercicio cuesta una consulta pagada y lenta. Por eso cada corrida
> intenta corregir un numero **maximo y configurable** de tareas de este tipo,
> con un valor por defecto. Alcanzado el tope, las tareas restantes quedan **sin
> intentar y sin error**: la corrida termina, las declara como tales y las toma
> la corrida siguiente. El reporte de la corrida distingue los cuatro desenlaces
> posibles de una tarea: **correccion propuesta**, **correccion no lograda**
> ([F-QICOR-R006](#F-QICOR-R006)), **diagnostico caido**
> ([F-QICOR-R009](#F-QICOR-R009)) y **tarea no intentada** por tope.

<details><summary>Detalle</summary>

Es el mismo trato que ya recibe la validacion de consigna, que detecta estos
mismos incumplimientos
([F-QINST-R006](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R006)),
y por la misma razon: con miles de tareas posibles, una corrida sin tope es una
espera de horas y una factura que aparece despues. El tope convierte la correccion
en un trabajo incremental —cada corrida avanza un tramo— sin que ninguna corrida
deje de terminar.

**Declarar lo no intentado es la otra mitad de la regla, y la que la vuelve
verificable.** Una corrida que corrige 20 tareas de 300 y no lo dice se lee igual
que una que corrigio todo lo que habia: el operador cree que el plan quedo
atendido. Con la declaracion, la lectura es honesta: "esto se intento, esto quedo
para la proxima". Es el mismo criterio con el que la auditoria declara su
cobertura parcial
([F-QINST-R005](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R005)).

**Los cuatro desenlaces se cuentan por separado porque el operador reacciona
distinto ante cada uno**: una correccion propuesta hay que revisarla, una no
lograda hay que mirarla a mano, un diagnostico caido no requiere nada —es trabajo
que se evaporo— y una tarea no intentada solo espera la corrida siguiente.
Colapsarlos vuelve ilegible el resultado: en las dos primeras hubo gasto y hay
algo que decir, en la tercera hubo un gasto minimo y una buena noticia, y en la
cuarta no se gasto nada.

**Valor por defecto** [ASSUMPTION]: 20 tareas por corrida. Ver
[DOUBT-PRESUPUESTO-CORRECCION](#DOUBT-PRESUPUESTO-CORRECCION). Si la
revalidacion previa de [F-QICOR-R009](#F-QICOR-R009) consume o no de ese tope
—son consultas pagadas igual— se decide en
[DOUBT-REVALIDACION-PRESUPUESTO](#DOUBT-REVALIDACION-PRESUPUESTO).

**Criterio de aceptacion**: con un tope de N y mas de N tareas de este tipo en el
plan, la corrida produce a lo sumo N propuestas, termina sin error, declara las
tareas que no intento, y esas tareas siguen disponibles para la corrida siguiente;
con menos tareas que el tope, ninguna se declara sin intentar; y una corrida que
mezcla los cuatro desenlaces los declara en cuatro recuentos distintos.

**Error**: N/A (agotar el tope es un desenlace normal, no un error)

</details>

<a id="F-QICOR-R009"></a>
### Rule[F-QICOR-R009] - Antes de corregir se revalida el original; si ya cumple, no se corrige nada
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Antes de producir ninguna correccion, el ejercicio **original** se somete a la
> misma validacion de consigna que despues juzgara al corregido
> ([F-QICOR-R005](#F-QICOR-R005)). Si esa validacion declara que el ejercicio
> **cumple**, la tarea termina con un desenlace propio —el diagnostico que la
> origino ya no se sostiene—: no se produce correccion, no queda ninguna
> propuesta y el ejercicio del curso no cambia. Ese desenlace **no es un
> fracaso** y no se confunde ni con el de correccion propuesta ni con el de
> correccion no lograda ([F-QICOR-R006](#F-QICOR-R006)).

<details><summary>Detalle</summary>

**El defecto que la motiva ya ocurrio, y es el peor caso de toda la feature.** Un
veredicto de incumplimiento queda registrado bajo la huella del contenido juzgado
y se reutiliza mientras el contenido no cambie, **cualquiera sea el criterio que
lo emitio**
([F-QINST-R008](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R008),
[F-QINST-R010](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R010)):
mejorar el criterio no invalida nada, y la unica via de rehacer un veredicto es
pedirlo explicitamente
([F-QINST-R012](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R012)).
Esa decision es deliberada y se tomo con su contra sobre la mesa, pero deja un
hueco justo en el unico lugar donde el sistema **gasta** a partir de un veredicto
viejo: aca.

Sin esta regla la cadena entera se ejecuta y ningun control la detiene:

| Paso | Que ocurre sin R009 |
|---|---|
| El veredicto viejo genera su tarea | Nadie pregunta si todavia se sostiene |
| La correccion reescribe el ejercicio | Se paga por corregir algo que estaba bien |
| [F-QICOR-R005](#F-QICOR-R005) pregunta "¿el corregido cumple?" | Responde que si — **siempre cumplio** |
| La propuesta se emite | El operador aprueba una reescritura que nadie necesitaba |

El daño no es solo el gasto: cada uno de los cinco criterios de
[F-QICOR-R004](#F-QICOR-R004) se pone en riesgo sin ninguna razon, sobre un
ejercicio que no tenia nada que arreglar.

**Evidencia (2026-08-04)**: el criterio de validacion castigaba de forma
inconsistente una convencion presente en **1814 ejercicios (15,8% del curso)**:
sobre 20 ejercicios identicos entre si, aprobaba 17 y marcaba 3. Esos 3 tienen hoy
un veredicto de incumplimiento registrado que ya no se sostiene, y son
exactamente los que esta regla intercepta.

**Por que la misma validacion y no una propia.** Si la revalidacion del original
aplicara un criterio distinto del que juzga al corregido, "el original cumple" y
"el corregido cumple" dejarian de ser comparables y la regla no podria decidir
nada. Es la misma exigencia que [F-QICOR-R004](#F-QICOR-R004) hace sobre la
medicion de longitud y de vocabulario: se verifica con la regla de medir real, no
con una parecida.

**Por que un tercer desenlace y no un fracaso.** No fracaso nada: el sistema
descubrio que no habia trabajo que hacer, que es un resultado util. Contarlo como
correccion no lograda inflaria el recuento de casos imposibles y le diria al
operador que hay un problema donde hubo una limpieza; contarlo como correccion
propuesta seria mentir dos veces, porque no hay propuesta y el curso no cambio.

| Desenlace de una tarea | Hubo correccion | Que quiere decir |
|---|---|---|
| Correccion propuesta ([F-QICOR-R001](#F-QICOR-R001)) | Si | Hay algo que aprobar o rechazar |
| Correccion no lograda ([F-QICOR-R006](#F-QICOR-R006)) | Se intento y no alcanzo | Hace falta mirarlo a mano |
| Diagnostico caido (esta regla) | No hacia falta | El problema ya no existe |
| Tarea no intentada ([F-QICOR-R008](#F-QICOR-R008)) | No se toco | La toma la corrida siguiente |

**El efecto buscado es que el sistema se auto-repare.** Cada vez que mejora el
criterio de validacion, los diagnosticos viejos que dejaron de sostenerse se caen
solos en lugar de arrastrar trabajo inutil. Y como el veredicto de la revalidacion
queda registrado bajo la huella del contenido
([F-QINST-R009](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R009)),
la auditoria siguiente lo reutiliza sin volver a pagarlo: el ejercicio deja de
figurar como incumplidor tambien alli, y el plan siguiente ya no genera esa tarea.
La limpieza se paga una sola vez.

**Que NO exige esta regla.** No exige re-evaluar el curso ni adelantarse a la
auditoria: la revalidacion alcanza unicamente al ejercicio de la tarea que se esta
por corregir, y en el momento en que se la va a corregir. Tampoco distingue por
que el diagnostico dejo de sostenerse —criterio mejorado o contenido ya
corregido—; ver [DOUBT-CAUSA-DEL-DESENLACE](#DOUBT-CAUSA-DEL-DESENLACE).

**Criterio de aceptacion**: (a) dada una tarea cuyo ejercicio, sometido a la
validacion vigente, cumple su consigna, la revision termina con el desenlace de
diagnostico caido, no produce correccion, no deja propuesta, el ejercicio del
curso queda identico, y el desenlace se distingue del de correccion no lograda;
(b) dada una tarea cuyo ejercicio sigue incumpliendo, la correccion se produce
normalmente; (c) el reporte de la corrida cuenta esas tareas en su propia
categoria ([F-QICOR-R008](#F-QICOR-R008)).

**Error**: "Se corrigio el ejercicio '{quizId}' a partir de un diagnostico de consigna que la validacion vigente ya no sostiene"

</details>

## Contexto

La validacion de consigna ya funciona de punta a punta: juzga ejercicio por
ejercicio si cumple literalmente lo que su consigna promete, lo puntua, deja por
cada incumplidor un motivo, una severidad y **una lista de violaciones con la
evidencia textual que las demuestra**, y convierte cada incumplimiento en una
tarea del plan de refinamiento
([FEAT-QINST](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md)).

Ahi se corta. La tarea existe, se prioriza, se lista y se filtra, pero pedirle una
correccion no produce nada: para este diagnostico el sistema todavia no sabe
proponer un arreglo, asi que la revision cae al comportamiento de identidad —una
propuesta que no cambia nada— que se reserva para los diagnosticos aun sin
correccion dedicada
([F-REVBYP-R004](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md#F-REVBYP-R004)).
Para otros diagnosticos el puente ya esta tendido: la ausencia de lema produce un
ejercicio nuevo y la longitud de titulo de knowledge produce un titulo nuevo, y en
los dos casos el operador aprueba o rechaza. El incumplimiento de consigna es el
unico que se queda a mitad de camino.

### Que hace distinta a esta correccion

Las correcciones que el sistema ya sabe hacer parten de una medicion: "esta
oracion tiene 14 tokens y el rango del nivel es 6 a 10", "falta este lema". El
diagnostico dice **cuanto** hay que mover algo. Aca el diagnostico es un juicio en
prosa: dice que el ejercicio pide una palabra y su respuesta tiene tres, o que la
consigna pide pasado y la unica respuesta aceptada esta en presente. Dos
consecuencias:

1. **La evidencia es la unica guia.** Sin la cita del fragmento que incumple, no
   hay por donde empezar; con ella, la correccion es quirurgica
   ([F-QICOR-R002](#F-QICOR-R002), [F-QICOR-R003](#F-QICOR-R003)).
2. **El resultado no se puede medir con una regla.** Que el ejercicio corregido
   cumpla la consigna solo lo puede decir el mismo criterio que detecto que no la
   cumplia ([F-QICOR-R005](#F-QICOR-R005)).

### El piso a superar es un ejercicio que ya funcionaba

Esta correccion **edita** un ejercicio existente, y esa es la diferencia con la
generacion de ejercicios por ausencia de lema, que crea uno donde no habia nada.
El ejercicio que se corrige ya esta dentro del rango de longitud de su nivel, ya
usa vocabulario del nivel, ya se distingue de sus hermanos, ya tiene una
traduccion fiel y ya es resoluble: lo unico que le falla es la consigna. Cualquier
correccion que arregle la consigna y rompa alguna de esas cinco cosas deja el
curso **peor** que antes, aunque el diagnostico que la origino desaparezca.

De ahi salen las tres reglas que forman el corazon de la feature: el cambio se
acota a lo señalado ([F-QICOR-R003](#F-QICOR-R003)), lo que ya estaba bien se
verifica que siga estandolo ([F-QICOR-R004](#F-QICOR-R004)), y lo que se corrigio
se verifica que efectivamente se haya corregido ([F-QICOR-R005](#F-QICOR-R005)).
Y de ahi sale tambien la unica postura defendible cuando no se logra: no proponer
nada y decirlo ([F-QICOR-R006](#F-QICOR-R006)).

### El diagnostico puede haber envejecido

Hay una asimetria facil de pasar por alto: el ejercicio que se va a corregir es el
de hoy, pero el diagnostico que ordena corregirlo puede ser viejo. Un veredicto se
reutiliza mientras el contenido no cambie, **sin importar con que criterio se
emitio**, y mejorar el criterio no invalida ninguno
([F-QINST-R010](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R010)).
Eso es deliberado —re-juzgar 11.500 ejercicios cada vez que se retoca el criterio
seria impagable— y no molesta mientras el veredicto solo se lea. Molesta cuando se
**gasta** a partir de el, que es exactamente lo que hace esta feature.

Medicion del 2026-08-04: el criterio castigaba de forma inconsistente una
convencion presente en **1814 ejercicios (15,8% del curso)**; sobre 20 ejercicios
identicos entre si aprobaba 17 y marcaba 3. Esos 3 arrastran hoy un veredicto que
ya no se sostiene, y sin ninguna precaucion terminarian reescritos y aprobados.
De ahi [F-QICOR-R009](#F-QICOR-R009): antes de corregir se vuelve a preguntar, y
si el ejercicio ya cumple la tarea se cae sola. El sistema se auto-repara a
medida que su criterio mejora, en vez de arrastrar trabajo que dejo de existir.

### El costo

Cada correccion es una consulta pagada y lenta, y verificar el resultado
([F-QICOR-R005](#F-QICOR-R005)) es otra. Con cientos de tareas posibles, una
corrida sin tope es una espera de horas y una factura que aparece despues. La
respuesta es la misma que ya adopto la validacion: acotar por corrida y declarar
lo que quedo afuera ([F-QICOR-R008](#F-QICOR-R008)).

Hay ademas un costo diferido y buscado: aprobar una correccion cambia el contenido
del ejercicio, con lo cual su veredicto de consigna registrado deja de aplicarle y
la auditoria siguiente lo vuelve a juzgar
([F-QINST-R009](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R009)).
Es exactamente lo que se quiere —la confirmacion independiente de que la
correccion sirvio— y su costo es una consulta por ejercicio corregido.

## Alcance

- **En alcance**:
  - La conversion de una tarea de incumplimiento de consigna en una correccion
    propuesta y revisable del ejercicio.
  - Los datos de partida de esa correccion, con las violaciones y su evidencia.
  - La acotacion del cambio a lo que las violaciones señalan.
  - La revalidacion del ejercicio original antes de corregirlo, y el desenlace
    propio de la tarea cuyo diagnostico ya no se sostiene.
  - Los criterios que el ejercicio corregido debe seguir cumpliendo y la
    verificacion de que efectivamente pasa a cumplir su consigna.
  - El desenlace explicito cuando no se logra una correccion aceptable.
  - La decision (aprobar / rechazar) por el flujo existente y la escritura del
    ejercicio corregido en el curso.
  - El tope de correcciones por corrida y la declaracion, en cuatro recuentos
    distintos, de como termino cada tarea.
- **Fuera de alcance**:
  - **Publicar la correccion a la base de datos productiva.** Esta feature llega
    hasta el curso que el sistema audita; la publicacion es un paso posterior y
    separado.
  - **Cambiar como se juzga el cumplimiento de la consigna.** Que revisa la
    validacion, como pondera y como redacta sus violaciones ya esta definido y
    probado ([FEAT-QINST](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md));
    esta feature consume su diagnostico y lo usa para corregir.
  - **Corregir la consigna del knowledge** en lugar del ejercicio. Cuando el
    problema es la consigna y no el ejercicio, la correccion que corresponde es
    otra ([FEAT-KTLR](../2026-07-06.01_revisar-knowledge-title-length-con-agente/REQUIREMENT.md));
    ver [DOUBT-CONSIGNA-CULPABLE](#DOUBT-CONSIGNA-CULPABLE).
  - **Corregir varios ejercicios de un knowledge de forma coordinada** para que
    no se parezcan entre si. Cada tarea se corrige por separado; la distincion se
    verifica contra el estado del curso, no se planifica en conjunto.

## User Journeys

### Journey[F-QICOR-J001] - Corregir un ejercicio que incumple su consigna, y decidir la correccion
**Validation**: AUTO_VALIDATED

Cubre el flujo principal de punta a punta: preparar la correccion con las
violaciones, revalidar el diagnostico, obtener un candidato, someterlo a los
criterios, y decidir. Cubre R001, R002, R003, R004, R005, R006, R007 y R009.

```yaml
journeys:
  - id: F-QICOR-J001
    name: Corregir un ejercicio que incumple su consigna, y decidir la correccion
    flow:
      - id: pedir_correccion
        action: "El operador pide la revision de una tarea cuyo diagnostico es el incumplimiento de consigna de un ejercicio"
        then: preparar_correccion

      - id: preparar_correccion
        action: "El sistema reune los datos de partida: el ejercicio completo con sus partes y opciones aceptadas, la consigna del knowledge, el nivel, los ejercicios hermanos y cada violacion con su restriccion, su evidencia y su explicacion"
        gate: [F-QICOR-R002]
        then: revalidar_original

      - id: revalidar_original
        action: "El sistema somete el ejercicio original a la misma validacion de consigna con la que despues juzgara al corregido"
        gate: [F-QICOR-R009]
        outcomes:
          - when: "La validacion vigente declara que el ejercicio original cumple su consigna: el diagnostico que origino la tarea ya no se sostiene"
            then: diagnostico_caido
          - when: "La validacion vigente confirma que el ejercicio original sigue incumpliendo su consigna"
            then: obtener_candidato

      - id: diagnostico_caido
        action: "La tarea termina con el desenlace de diagnostico caido, distinto del de correccion no lograda: no se produce ninguna correccion, no queda propuesta, el ejercicio del curso queda identico y la corrida lo cuenta en su propia categoria"
        gate: [F-QICOR-R009]
        result: success

      - id: obtener_candidato
        action: "El sistema obtiene un ejercicio corregido dirigido por las violaciones y lo somete a los criterios de no regresion y de cumplimiento de la consigna"
        gate: [F-QICOR-R004, F-QICOR-R005]
        outcomes:
          - when: "El ejercicio corregido pasa los cinco criterios de no regresion y la validacion de consigna declara que ahora cumple"
            then: armar_propuesta
          - when: "Agotados los intentos, ningun ejercicio corregido pasa todos los criterios"
            then: sin_correccion

      - id: armar_propuesta
        action: "Queda una propuesta revisable cuyo estado anterior es el ejercicio actual del curso y cuyo estado propuesto es el ejercicio corregido, con todo lo que ninguna violacion señala identico al original"
        gate: [F-QICOR-R001, F-QICOR-R003]
        then: decidir

      - id: decidir
        action: "La propuesta se decide con el mismo vocabulario que las demas correcciones"
        outcomes:
          - when: "La propuesta se aprueba"
            then: aplicar_correccion
          - when: "El operador rechaza la propuesta"
            then: rechazo

      - id: aplicar_correccion
        action: "El ejercicio del curso queda con el contenido corregido, la tarea figura resuelta y ningun otro ejercicio del knowledge cambia"
        gate: [F-QICOR-R007]
        result: success

      - id: rechazo
        action: "El curso no se modifica: el ejercicio conserva su contenido original y la tarea vuelve a quedar disponible"
        gate: [F-QICOR-R007]
        result: failure

      - id: sin_correccion
        action: "El sistema informa que no logro una correccion aceptable y nombra los criterios que reprobaron; no queda ninguna propuesta, el ejercicio del curso no cambia y la tarea sigue disponible"
        gate: [F-QICOR-R006]
        result: failure
```

### Journey[F-QICOR-J002] - Corregir un tramo acotado de las tareas de consigna del plan
**Validation**: AUTO_VALIDATED

Cubre el avance por tramos, la declaracion de lo que quedo sin intentar y la
separacion de los desenlaces en el reporte de la corrida. Cubre R008 y R009.

```yaml
journeys:
  - id: F-QICOR-J002
    name: Corregir un tramo acotado de las tareas de consigna del plan
    flow:
      - id: pedir_corrida
        action: "El operador pide corregir las tareas de incumplimiento de consigna de un plan, con un tope de correcciones para la corrida"
        then: corregir_tramo

      - id: corregir_tramo
        action: "El sistema intenta corregir tareas hasta agotar el tope de la corrida"
        gate: [F-QICOR-R008]
        outcomes:
          - when: "El plan tiene mas tareas de este tipo que el tope de la corrida"
            then: tramo_acotado
          - when: "El plan tiene tantas tareas de este tipo como el tope o menos"
            then: todas_intentadas

      - id: tramo_acotado
        action: "La corrida termina sin error habiendo producido a lo sumo tantas propuestas como el tope, declara las tareas que no intento, y esas tareas siguen disponibles para la corrida siguiente"
        gate: [F-QICOR-R008]
        result: success

      - id: todas_intentadas
        action: "La corrida intenta todas las tareas del plan de este tipo, no declara ninguna sin intentar, y su reporte distingue en recuentos separados las correcciones propuestas, las no logradas y los diagnosticos caidos"
        gate: [F-QICOR-R008, F-QICOR-R009]
        result: success
```

## Open Questions

<a id="DOUBT-SIN-CORRECCION"></a>
### Doubt[DOUBT-SIN-CORRECCION] - Que se hace con el mejor candidato cuando ninguno alcanza?
**Status**: OPEN

[F-QICOR-R006](#F-QICOR-R006) exige que un candidato que reprueba criterios no se
proponga y que el fallo se informe. Falta decidir si ademas se **conserva** de
alguna forma el [mejor candidato](glossary:Mejor candidato) alcanzado o se
descarta.

- [x] Opcion A: **No se propone nada.** Se informa el fallo con los criterios
  reprobados y la tarea queda disponible. Simple y sin riesgo de que el operador
  apruebe una regresion; a cambio, el trabajo del intento se pierde por completo,
  incluso cuando el candidato reprobaba por poco.
- [ ] Opcion B: **Se propone marcado como no apto**, con los criterios reprobados
  a la vista, y el operador decide igual. No se pierde el trabajo y el operador
  puede juzgar un caso limite; a cambio, aparece en el plan una propuesta que el
  sistema mismo desaconseja, y basta una aprobacion distraida para meter una
  regresion al curso.
- [ ] Opcion C: **Se guarda como referencia sin ser una propuesta aprobable**:
  visible para quien quiera corregir a mano, imposible de aprobar tal cual.
  Intermedia; agrega una categoria nueva al vocabulario de revision, que hoy solo
  conoce pendiente / aprobada / rechazada.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] porque es la
unica que no puede terminar en una regresion aprobada por error, y porque el piso
a superar es un ejercicio que ya funcionaba en todo salvo su consigna. Conviene
revisarla con datos: si al correr las primeras tandas la mayoria de los fallos
reprueba **un solo** criterio y por poco, la Opcion C pasa a valer la pena.

<a id="DOUBT-REINTENTOS"></a>
### Doubt[DOUBT-REINTENTOS] - Cuantos intentos antes de declarar que no se logro?
**Status**: OPEN

[F-QICOR-R006](#F-QICOR-R006) habla de "agotados los intentos" sin fijar cuantos.
Cada intento adicional cuesta otra consulta pagada, y el tope de la corrida
([F-QICOR-R008](#F-QICOR-R008)) cuenta tareas, no intentos.

- [x] Opcion A: **Hasta 3 intentos por tarea, configurable**, y cada intento
  recibe como insumo que criterios reprobo el anterior y por que. Es el mismo
  numero y el mismo mecanismo que ya usa la generacion de ejercicios
  ([F-LASAG-R008](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R008),
  [F-LASAG-R010](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R010)).
- [ ] Opcion B: **Un solo intento.** El mas barato y el mas predecible en costo; a
  cambio, tira a la basura casos que un segundo intento informado resolveria.
- [ ] Opcion C: **Sin tope de intentos hasta lograrlo.** Garantiza el mejor
  resultado posible por tarea y hace imposible acotar el costo de una corrida, que
  es justo lo que [F-QICOR-R008](#F-QICOR-R008) busca.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] por coherencia
con la correccion de vocabulario, que ya reintenta con el motivo del fallo a la
vista y con tope 3. Si se elige otra, cambia el detalle de
[F-QICOR-R006](#F-QICOR-R006) pero no su enunciado.

<a id="DOUBT-PRESUPUESTO-CORRECCION"></a>
### Doubt[DOUBT-PRESUPUESTO-CORRECCION] - Cual es el tope de correcciones por corrida por defecto?
**Status**: OPEN

[F-QICOR-R008](#F-QICOR-R008) exige un tope configurable con un valor por
defecto. El numero decide cuanto dura una corrida y cuanto sale.

- [x] Opcion A: **20 tareas por corrida.** Una corrida se sigue de un tirón y el
  gasto por corrida es chico; cubrir cientos de tareas lleva muchas corridas.
- [ ] Opcion B: **100 por corrida.** Avanza mucho mas rapido; cada corrida es una
  espera larga y una factura visible, y multiplica por 5 el costo de equivocarse
  con la configuracion.
- [ ] Opcion C: **Sin tope por defecto.** Cubre el plan de una vez y convierte
  cualquier pedido distraido en horas de espera y una factura completa —
  exactamente lo que el tope existe para evitar.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION]. La Opcion C se
desaconseja como **defecto** por la misma razon por la que se desaconsejo en la
validacion de consigna
([DOUBT-PRESUPUESTO-DEFECTO](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#DOUBT-PRESUPUESTO-DEFECTO)).
El valor exacto conviene ajustarlo tras medir cuanto tarda y cuanto cuesta una
correccion real, contando sus reintentos y su verificacion.

<a id="DOUBT-DISTINCION-ALCANCE"></a>
### Doubt[DOUBT-DISTINCION-ALCANCE] - Contra que se compara la distincion del ejercicio corregido?
**Status**: OPEN

El criterio 3 de [F-QICOR-R004](#F-QICOR-R004) exige que el ejercicio corregido no
se confunda con los demas ejercicios de su knowledge **ni con el resto del curso**.
Lo primero es barato; lo segundo, sobre miles de ejercicios, no lo es.

- [x] Opcion A: **Comparacion completa contra los hermanos del knowledge y
  aproximada contra el resto del curso.** Los hermanos son el riesgo real —el
  alumno los ve juntos y una correccion tiende a acercarse a ellos— y el resto del
  curso se cubre con una comparacion mas barata que atrape las coincidencias
  groseras.
- [ ] Opcion B: **Solo contra los hermanos del knowledge.** Es la comparacion que
  el sistema ya sabe hacer y la que cubre el caso que importa; deja pasar un
  ejercicio corregido identico a otro de un knowledge lejano, que el alumno
  reencontraria mucho despues.
- [ ] Opcion C: **Contra el curso entero, sin aproximaciones.** El unico criterio
  sin puntos ciegos y el mas caro de todos, y encarece cada uno de los intentos de
  cada correccion.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION]. Lo que la
regla exige es que el ejercicio corregido no se confunda con otros; **con que
alcance y con que precision** se verifica es lo que esta duda decide, y conviene
resolverlo con una medicion de cuantas coincidencias reales existen hoy en el
curso fuera del knowledge.

<a id="DOUBT-COSTO-VERIFICACION"></a>
### Doubt[DOUBT-COSTO-VERIFICACION] - Verificar el cumplimiento de la consigna cuesta una consulta mas por correccion
**Status**: OPEN

[F-QICOR-R005](#F-QICOR-R005) exige verificar el ejercicio corregido con la misma
validacion de consigna antes de proponerlo. Eso duplica —como minimo— el costo de
cada correccion, y lo multiplica si hay reintentos.

- [x] Opcion A: **Se verifica siempre, antes de proponer.** Ninguna propuesta
  llega al operador sin haber pasado el criterio que la origino; es el doble de
  caro.
- [ ] Opcion B: **No se verifica; se confia en que la correccion atendio las
  violaciones** y se deja que la auditoria siguiente lo confirme. La mitad de
  caro; a cambio, el operador aprueba propuestas que pueden volver al plan, que es
  el desenlace que [F-QICOR-R005](#F-QICOR-R005) existe para impedir.
- [ ] Opcion C: **Se verifica solo cuando la correccion toco el hueco o las
  opciones aceptadas**, y se confia en el resto de los casos. Ahorra sobre las
  correcciones mas inocuas; exige un criterio para decidir cuando alcanza con
  confiar, y ese criterio puede equivocarse en silencio.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION]: es lo que el
enunciado de la regla exige hoy. Si el costo resulta prohibitivo tras medirlo, la
discusion es sobre [F-QICOR-R005](#F-QICOR-R005), no sobre su implementacion — y
lo que se estaria aceptando es que una parte de las correcciones aprobadas vuelva
al plan en la auditoria siguiente.

<a id="DOUBT-REVALIDACION-PRESUPUESTO"></a>
### Doubt[DOUBT-REVALIDACION-PRESUPUESTO] - La revalidacion previa consume del tope de la corrida?
**Status**: OPEN

La revalidacion de [F-QICOR-R009](#F-QICOR-R009) es una consulta pagada igual que
la correccion. Falta decidir si el tope de [F-QICOR-R008](#F-QICOR-R008) la
cuenta.

- [x] Opcion A: **El tope cuenta tareas tocadas**, y una tarea cuyo diagnostico se
  cae consume lo mismo que una corregida. El gasto de la corrida queda acotado sin
  excepciones, que es para lo que existe el tope; a cambio, una corrida que solo
  encuentra diagnosticos caidos avanza apenas 20 tareas pese a haber gastado una
  fraccion de lo previsto, y limpiar cientos de tareas viejas lleva muchas
  corridas.
- [ ] Opcion B: **El tope cuenta solo tareas corregidas**; las que caen en la
  revalidacion no consumen. Una corrida de limpieza barre todo lo caducado de una
  vez; a cambio el gasto deja de estar acotado — 300 tareas caidas son 300
  consultas que nadie previo — y reaparece exactamente la factura sorpresa que el
  tope evita.
- [ ] Opcion C: **Dos topes separados**, uno de revalidaciones y otro de
  correcciones. Permite una corrida barata de limpieza masiva y otra cara de
  correccion; agrega una segunda perilla que hay que entender y configurar.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] por prudencia:
el tope existe para que el costo no aparezca por sorpresa, y es la unica de las
tres en la que ninguna corrida puede gastar mas de lo que su tope dice. Conviene
revisarla con el primer dato duro: si la limpieza resulta ser masiva —el orden de
los 1814 ejercicios de la convencion medida—, la Opcion C pasa a ser la sensata.

<a id="DOUBT-TAREA-CAIDA"></a>
### Doubt[DOUBT-TAREA-CAIDA] - Que pasa con la tarea del plan cuando su diagnostico se cae?
**Status**: OPEN

[F-QICOR-R009](#F-QICOR-R009) fija que no se corrige nada y que el desenlace se
declara. No fija que se hace con la tarea, que sigue en el plan.

- [ ] Opcion A: **Se cierra automaticamente** como resuelta sin cambios. El plan
  queda limpio solo; a cambio, una tarea desaparece sin que nadie la haya mirado,
  y si la revalidacion se equivoco el problema se archiva en silencio.
- [x] Opcion B: **Se marca como caida y queda visible**, sin volver a intentarse
  en las corridas siguientes. El operador ve que paso y puede revisarlo; el plan
  conserva una entrada que ya no representa trabajo pendiente.
- [ ] Opcion C: **Queda disponible como estaba.** Nada que decidir ni que agregar;
  a cambio, la corrida siguiente la vuelve a tomar, la vuelve a revalidar y vuelve
  a pagar la consulta, indefinidamente.

**Answer**: Pendiente. Se especifica la **Opcion B** [ASSUMPTION]: es la unica que
no esconde nada y no vuelve a pagar por lo mismo. Pesa a su favor que la limpieza
definitiva llega sola: el veredicto de la revalidacion queda registrado bajo la
huella del contenido, asi que la auditoria siguiente puntua el ejercicio como
cumplidor y el plan que se genere ya no incluye esa tarea
([F-QINST-R017](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R017)).
La marca solo tiene que sobrevivir hasta ese momento. La Opcion C queda
desaconsejada: paga una consulta por corrida y por tarea para llegar siempre a la
misma conclusion.

<a id="DOUBT-CAUSA-DEL-DESENLACE"></a>
### Doubt[DOUBT-CAUSA-DEL-DESENLACE] - Hay que distinguir "ahora cumple" de "el contenido cambio desde que se juzgo"?
**Status**: OPEN

Un diagnostico puede caerse por dos causas distintas: porque el criterio de
validacion mejoro y el ejercicio —que nunca cambio— ahora se aprueba, o porque el
ejercicio ya fue corregido por otra via y el veredicto viejo era sobre otro
contenido. El efecto es el mismo; la causa no.

- [x] Opcion A: **Un solo desenlace, sin distinguir la causa.** La accion correcta
  es identica en los dos casos —no corregir y dejar que la auditoria siguiente
  reutilice el veredicto nuevo—, y el enunciado de
  [F-QICOR-R009](#F-QICOR-R009) queda mas simple. Se pierde una señal util.
- [ ] Opcion B: **Se declara la causa** junto al desenlace. Distingue "17 tareas se
  cayeron porque el criterio mejoro" —que dice algo sobre la validacion— de "3 se
  cayeron porque el ejercicio ya se habia corregido" —que dice algo sobre el flujo
  de trabajo—. Es barato: la comparacion es contra el contenido que se juzgo, que
  ya esta registrado ([F-QINST-R009](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R009)).

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] para esta
entrega, porque la decision del sistema no depende de la causa y agregar el dato
al enunciado de la regla lo haria mas dificil de verificar sin cambiar lo que
exige. La Opcion B es una mejora de reporte que **no requiere cambiar ninguna
regla** de este documento, y conviene decidirla cuando exista el primer recuento
real de diagnosticos caidos.

<a id="DOUBT-CONSIGNA-CULPABLE"></a>
### Doubt[DOUBT-CONSIGNA-CULPABLE] - Que se hace cuando el que esta mal es la consigna, no el ejercicio?
**Status**: OPEN

Toda esta feature asume que la consigna es correcta y el ejercicio la incumple.
Puede pasar lo contrario: una consigna imposible de cumplir, o que contradice a
todos los ejercicios de su knowledge. En ese caso corregir ejercicio por ejercicio
es corregir el sintoma, y una consigna incumplida por sus ~19 ejercicios genera 19
tareas que van a fallar todas por la misma razon.

- [x] Opcion A: **Fuera de alcance.** Esta feature corrige ejercicios; los casos
  en que la consigna es la culpable terminan como fallos informados
  ([F-QICOR-R006](#F-QICOR-R006)) y alguien los mira a mano. Simple, y deja al
  operador atendiendo 19 fallos identicos sin que nadie le diga que tienen una
  causa comun.
- [ ] Opcion B: **El sistema detecta el patron** —muchos ejercicios de un mismo
  knowledge fallando su correccion por la misma restriccion— y lo señala como un
  problema de la consigna, sin corregirla. Convierte 19 fallos en un aviso util;
  hay que definir a partir de cuantos y con que criterio.
- [ ] Opcion C: **La correccion puede proponer cambiar la consigna del
  knowledge.** Ataca la causa; cambia el objeto de la correccion, alcanza a todos
  los ejercicios del knowledge de una vez y se pisa con la correccion de etiqueta
  de knowledge que ya existe.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] para esta
entrega. La Opcion B es la continuacion natural y no requiere cambiar ninguna
regla de este documento —se apoya en los fallos que
[F-QICOR-R006](#F-QICOR-R006) ya informa con su motivo—, asi que conviene decidirla
recien despues de ver, con datos de las primeras corridas, cuantos fallos comparten
knowledge y restriccion.

## References

- **FEAT-QINST** (`2026-07-29.02_validacion-de-consigna-de-quiz`) — Origen del
  diagnostico: juzga el cumplimiento de la consigna, deja por cada incumplidor las
  violaciones con su evidencia, y convierte cada incumplimiento en la tarea del
  plan que esta feature corrige. Define ademas dos cosas de las que depende
  [F-QICOR-R009](#F-QICOR-R009): que un veredicto se reutiliza cualquiera sea el
  criterio que lo emitio —de ahi que un diagnostico pueda envejecer— y que
  corregir un ejercicio invalida su veredicto y lo devuelve a la cola de la
  auditoria siguiente. Citada por [F-QICOR-R002](#F-QICOR-R002),
  [F-QICOR-R005](#F-QICOR-R005), [F-QICOR-R006](#F-QICOR-R006),
  [F-QICOR-R008](#F-QICOR-R008), [F-QICOR-R009](#F-QICOR-R009) y el Contexto.
- **FEAT-KTLR** (`2026-07-06.01_revisar-knowledge-title-length-con-agente`) —
  Precedente mas cercano del puente diagnostico → propuesta revisable → decision:
  datos de partida, propuesta con estado anterior y propuesto, y desenlace sin
  propuesta ante una falla. Alli el objeto corregido es el knowledge; aca es el
  ejercicio. Citada por [F-QICOR-R001](#F-QICOR-R001),
  [F-QICOR-R006](#F-QICOR-R006) y el Alcance.
- **FEAT-LASAG** (`2026-06-18.01_lemma-absence-agent-migration`) — Precedente del
  catalogo de criterios de calidad sobre el contenido propuesto, de la edicion
  minima y del reintento informado por el motivo del fallo. Es tambien el
  contraste deliberado de [F-QICOR-R006](#F-QICOR-R006): alli el mejor esfuerzo se
  emite porque compite contra la nada; aca compite contra un ejercicio que ya
  funcionaba. Citada por [F-QICOR-R003](#F-QICOR-R003),
  [F-QICOR-R004](#F-QICOR-R004), [F-QICOR-R006](#F-QICOR-R006) y
  [DOUBT-REINTENTOS](#DOUBT-REINTENTOS).
- **FEAT-REVBYP** (`2026-04-17.01_refiner-revision-bypass`) — Establece el
  comportamiento de identidad al que hoy cae la revision de una tarea de
  incumplimiento de consigna: la propuesta existe y no cambia nada. Es el estado
  que esta feature reemplaza. Citada por [F-QICOR-R001](#F-QICOR-R001) y el
  Contexto.
- **FEAT-REVAPR** (`2026-04-20.01_refiner-revision-approval`) — Define el
  vocabulario de decision (pendiente / aprobada / rechazada) y el flujo de
  aprobacion que esta correccion reutiliza sin cambios. Citada por
  [F-QICOR-R007](#F-QICOR-R007).
- **FEAT-RPRES** (`2026-07-29.01_preservacion-del-contenido-no-revisado`) — Fija
  el radio de impacto al aplicar una correccion: lo que la revision no toco queda
  intacto. Esta feature se apoya en esa garantia en lugar de repetirla. Citada por
  [F-QICOR-R007](#F-QICOR-R007).
- **FEAT-CLEX** (`2026-07-22.01_consulta-lexica-de-oracion-por-cli`) — Provee la
  verificacion lexica con el mismo criterio de la auditoria, que es como se
  comprueba el criterio 2 de no regresion sin aproximaciones. Citada por
  [F-QICOR-R004](#F-QICOR-R004).
- **FEAT-SLEN** (`2026-03-18.01_sentence-length`) — Define el rango de longitud
  objetivo por nivel y su medicion, que es como se comprueba el criterio 1 de no
  regresion. Citada por [F-QICOR-R004](#F-QICOR-R004).
