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
propuesta del ejercicio —minima, dirigida por la violacion y que cumple la
consigna— revisable y aprobable con el mismo flujo que las demas.

**Por que**: Hoy el incumplimiento se detecta, se puntua y llega al plan como
tarea, pero ahi se detiene: no hay forma de pedirle al sistema que proponga como
arreglarlo, y el operador queda con una lista de problemas sin una sola
correccion que aprobar.

## Reglas de Negocio

Las reglas se leen en cinco tramos:

- **Del problema a la propuesta** (R001, R002, R003, R010): la tarea se convierte
  en una correccion, construida sobre lo que la violacion señala y acotada a eso;
  lo que no aporta un solo caracter ni la bloquea ni se pierde al aprobarla.
- **Antes de tocar nada** (R009): el diagnostico se revalida contra el criterio
  vigente, y si ya no se sostiene la tarea se cierra sin corregir nada.
- **Con que se juzga la correccion** (R004, R005, R011, R015): el catalogo de
  criterios y la regla de medir, que es la misma de la auditoria y se puede
  consultar. Cumplir la consigna es lo unico absoluto; la longitud se mide y se
  declara, pero no decide nada.
- **Quien intenta, cuantas veces y que se entrega** (R012, R013, R014, R006): el
  lazo lo conduce quien corrige, al agotarlo entrega el mejor candidato que vio,
  lo que ese candidato no cumplio se declara, y "no se logro" queda para el unico
  caso en que no hay nada que entregar.
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

> La correccion propuesta modifica **unicamente** lo que las violaciones señalan:
> las partes que ninguna violacion menciona y los huecos no implicados con sus
> opciones aceptadas vuelven **identicos**, caracter por caracter, al estado
> anterior. La **traduccion al español queda fuera de esta regla**: no es una
> parte mas del ejercicio sino la oracion dicha en español, y acompaña a la
> oracion cuando esta cambia legitimamente. Quien la juzga es el criterio de
> traduccion fiel ([F-QICOR-R004](#F-QICOR-R004), criterio 4). Lo unico que esta
> regla le exige es que **no cambie sola**: si la oracion propuesta es identica a
> la original, la traduccion tambien tiene que serlo. Las **partes vacias** del
> enunciado —las que no aportan un solo caracter— tampoco entran en esta
> comparacion, por el motivo opuesto y complementario: no hay nada que comparar
> ([F-QICOR-R010](#F-QICOR-R010)).

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
intacto —con la unica excepcion de la traduccion, que se explica enseguida—.
Cuando la correccion no logra resolver la violacion sin tocar el resto,
lo que corresponde es que ese candidato repruebe este criterio y se vuelva a
intentar, no que se relaje esta regla. Que al agotar los intentos ese candidato
pueda entregarse igual —declarando que reprobo la edicion acotada,
[F-QICOR-R012](#F-QICOR-R012) y [F-QICOR-R014](#F-QICOR-R014)— tampoco la relaja:
la regla sigue midiendo lo mismo, sigue empujando el reintento y lo que reprueba
sigue viendose. Lo que cambio no es el criterio, es quien decide con el a la
vista: ahora tambien el operador.

**Por que la traduccion queda fuera de esta regla.** La evidencia que emite la
validacion de consigna habla siempre **del ingles**: cita la respuesta aceptada,
el hueco, la marca entre parentesis. Nunca menciona el español. Si la traduccion
se congelara salvo que una violacion la señale, entonces —como ninguna la señala
jamas— seria lisa y llanamente **incorregible**. Y hay una familia entera de
defectos en la que corregir la respuesta **obliga** a corregir la traduccion:

| Lo que la violacion señala (ingles) | Consecuencia forzosa (español) |
|---|---|
| `are you running` → `are they walking` | `¿Por qué estás corriendo?` → `¿Por qué están caminando?` |
| `are you eating` → `are you drinking` | `¿Qué estás comiendo?` → `¿Qué estás bebiendo?` |

**Medicion (2026-08-09), primer lote real de 20 tareas**: 1 correccion propuesta
y 19 no logradas; **17 de esas 19 reprobaron exactamente esta regla**. De los 20
candidatos, **13 cambiaron la traduccion, y en los 13 el cambio era correcto**:
consecuencia forzosa de arreglar la respuesta. El efecto no era solo bloquear,
era **premiar la inconsistencia**: la unica correccion que paso lo hizo porque
**no** toco la traduccion, y quedo un ejercicio que dice una cosa en ingles y
otra en español. Un criterio que aprueba la version incoherente y rechaza las
coherentes esta midiendo al reves.

**La traduccion no es una parte mas: es derivada.** Las partes del ejercicio
tienen contenido propio —una violacion las alcanza o no las alcanza—. La
traduccion no: no tiene ningun sentido independiente de la oracion que traduce.
Congelarla mientras la oracion cambia legitimamente no conserva nada; produce un
ejercicio que se contradice a si mismo. Lo que esta regla existe para impedir es
el **cambio colateral que nadie pidio** ([F-LASAG-R014](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R014));
una traduccion que sigue a su oracion no es colateral: es la consecuencia
obligada del cambio que la violacion **si** autorizo.

**La traduccion tiene un solo juez.** El catalogo de criterios ya incluye uno
propio para ella —traduccion fiel, criterio 4 de
[F-QICOR-R004](#F-QICOR-R004)—. Si ademas esta regla prohibiera tocarla, los dos
se pisarian con exigencias opuestas: uno pidiendo que diga lo mismo que la
oracion propuesta y el otro que no cambie. Eso no es mas severidad, es una
contradiccion de la especificacion. El reparto queda explicito: **esta regla
decide que puede cambiar; el criterio de traduccion fiel decide si la traduccion
esta bien**. Sobre la traduccion, esta regla no opina.

**Lo unico que esta regla le sigue exigiendo: que no cambie sola.** Si la oracion
propuesta —partes, huecos y opciones aceptadas— es identica a la original, la
traduccion tambien tiene que serlo. Es la unica puerta que queda cerrada, y por
la razon de siempre: sin ella la traduccion seria una via libre para reescribir
contenido que ninguna violacion pidio tocar, exactamente lo que esta regla vino a
impedir.

**Las partes vacias tampoco entran, y por un motivo mas simple.** Esta regla se
enuncia sobre caracteres —"identico, **caracter por caracter**"— y una parte
vacia no tiene ninguno. Contar partes es un criterio **mas estricto** que el que
la regla dice, y ese exceso volvia estructuralmente incorregible al 35% del
curso. [F-QICOR-R010](#F-QICOR-R010) lo enuncia completo y decide ademas que
queda guardado; aca alcanza con que la comparacion no las mire.

**Criterio de aceptacion**: (a) dado un ejercicio de varias partes cuya unica
violacion señala una de ellas, en la propuesta resultante esa parte cambia y las
restantes —y las opciones aceptadas de los huecos no implicados— son identicas,
caracter por caracter, a las del ejercicio original; (b) un candidato que cambia
la respuesta aceptada de `are you running` a `are they walking` y con ella la
traduccion de `¿Por qué estás corriendo?` a `¿Por qué están caminando?` **no**
reprueba esta regla; (c) un candidato cuya oracion es identica caracter por
caracter a la del original y que aun asi cambia la traduccion **si** la reprueba;
(d) un candidato que vuelve **sin** una parte vacia que el original tenia, y cuyas
partes con texto son identicas salvo la señalada, **no** reprueba esta regla
([F-QICOR-R010](#F-QICOR-R010)).

**Error**: "La correccion del ejercicio '{quizId}' modifico contenido que ninguna violacion de su consigna señala"

</details>

<a id="F-QICOR-R004"></a>
### Rule[F-QICOR-R004] - No empeorar: el catalogo de criterios con el que se juzga al ejercicio corregido
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El ejercicio corregido se somete a un catalogo fijo de criterios, cada uno con su
> propio veredicto y su propio motivo. Reprobar uno **empuja a intentar de nuevo y
> baja al candidato en el orden** del que sale el mejor
> ([F-QICOR-R013](#F-QICOR-R013)); ninguno de estos cinco, ni todos juntos, impide
> entregar al mejor candidato cuando los intentos se agotan
> ([F-QICOR-R012](#F-QICOR-R012)) — lo que si exige es que lo reprobado se declare
> ([F-QICOR-R014](#F-QICOR-R014)). El criterio 1 va mas lejos todavia: se mide, se
> informa y **ni siquiera cuenta** ([F-QICOR-R011](#F-QICOR-R011)):
> 1. **Longitud dentro del rango objetivo de su nivel**, medida con el mismo
>    criterio de longitud que aplica la auditoria. **No decide**: un candidato
>    cuya unica falla es esta se propone igual, declarando que quedo fuera de
>    rango.
> 2. **Vocabulario adecuado al nivel**: no introduce palabras mal ubicadas para
>    el nivel ni fuera de catalogo que el ejercicio original no tuviera ya.
> 3. **Distincion**: no se confunde con los demas ejercicios de su knowledge ni
>    con el resto del curso.
> 4. **Traduccion fiel**: la traduccion al español dice lo mismo que la oracion
>    **propuesta** —la corregida, no la anterior—. Es el **unico** criterio que
>    juzga la traduccion; [F-QICOR-R003](#F-QICOR-R003) no opina sobre ella.
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
| 1. Longitud en rango | Deterministico, con la misma medicion de longitud de la auditoria ([F-SLEN-R002](../2026-03-18.01_sentence-length/REQUIREMENT.md#F-SLEN-R002)). **No decide**: se mide y se declara, y ni siquiera cuenta en el orden ([F-QICOR-R011](#F-QICOR-R011)) |
| 2. Vocabulario del nivel | Deterministico, con el mismo analisis lexico de la auditoria ([FEAT-CLEX](../2026-07-22.01_consulta-lexica-de-oracion-por-cli/REQUIREMENT.md)): los [lemas](glossary:Lema) señalados del corregido estan contenidos en los del original |
| 3. Distincion | Comparacion contra los demas ejercicios; ver [DOUBT-DISTINCION-ALCANCE](#DOUBT-DISTINCION-ALCANCE) |
| 4. Traduccion fiel | Juicio sobre el par **propuesto** (oracion propuesta / traduccion propuesta): lo que decide es si dicen lo mismo, no si la traduccion cambio. Una que quedo diciendo lo que decia la oracion **anterior** reprueba |
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

Para el criterio 1 la exigencia sigue en pie aunque ya no rechace, y por un motivo
que le es propio: lo que se mide se **declara**
([F-QICOR-R011](#F-QICOR-R011)), y una declaracion hecha con otra regla de medir es
peor que ninguna — diria "quedo en rango" sobre un ejercicio que la auditoria
siguiente va a marcar, o al reves. Medir igual es lo que vuelve confiable el aviso.

**Los flags preexistentes no bloquean.** El criterio 2 exige no **introducir**
problemas nuevos, no arreglar los viejos: una palabra que ya estaba mal ubicada
en el original y que la correccion no toco sigue estandolo, y eso no invalida la
correccion. Exigir lo contrario volveria incorregible a todo ejercicio con
residuo previo, que es la mayoria.

**El criterio 1 era la excepcion a esa doctrina, y por eso dejo de rechazar.**
Estaba enunciado en terminos absolutos —"dentro del rango"— dentro de una regla
que se llama "no empeorar", asi que en los hechos le exigia a la correccion de
consigna que arreglara de paso un defecto de longitud que el ejercicio ya traia y
que ninguna violacion señalaba. Se midio: de los 78 candidatos rechazados
unicamente por longitud, **75 partian de un ejercicio que ya estaba fuera de
rango**. La medicion sigue haciendose y sigue informandose; lo que se retiro es su
poder de veto ([F-QICOR-R011](#F-QICOR-R011)).

**Que queda de este catalogo ahora que ninguno de sus criterios veta.** Queda todo
menos el veto, que era la parte que no funcionaba. Los cinco criterios siguen
midiendose sobre cada candidato, siguen siendo el motivo por el que quien corrige
vuelve a intentar, y siguen decidiendo **cual** de los candidatos vistos se
entrega ([F-QICOR-R013](#F-QICOR-R013)). Lo que ya no hacen es tirar el trabajo:
cuando el mejor candidato reprueba alguno, se entrega igual y lo reprobado se
nombra en la propuesta ([F-QICOR-R014](#F-QICOR-R014)), y el que decide con esa
informacion a la vista es el operador. Es un cambio de destinatario, no de
exigencia: antes el veredicto negativo terminaba en un descarte que nadie veia;
ahora termina en una advertencia que alguien lee. Lo que **si** sigue siendo
absoluto es cumplir la consigna ([F-QICOR-R005](#F-QICOR-R005)), que no pertenece
a este catalogo: no es una cosa que la correccion no deba empeorar, es lo que la
correccion vino a hacer.

**El criterio 4 es el unico juez de la traduccion.** Como
[F-QICOR-R003](#F-QICOR-R003) no opina sobre ella —la traduccion no es una parte
mas del ejercicio, es la oracion dicha en español—, todo el peso de que la
traduccion este bien cae aca. De ahi que se juzgue contra la oracion **propuesta**
y no contra la anterior, con dos consecuencias que conviene tener a la vista:

- Una traduccion que **no cambio** puede reprobar. Un candidato que corrige la
  respuesta de `are you eating` a `are you drinking` y deja `¿Qué estás
  comiendo?` ya no dice lo mismo que su oracion: reprueba aca, se vuelve a
  intentar, y si termina entregandose es con ese reproche a la vista
  ([F-QICOR-R014](#F-QICOR-R014)) y por detras de cualquier candidato que si diga
  lo mismo ([F-QICOR-R013](#F-QICOR-R013)). Es lo que cierra el incentivo perverso
  que documenta [F-QICOR-R003](#F-QICOR-R003) — dejar la inconsistencia dejo de ser
  el camino barato para pasar los criterios.
- Una traduccion que **si cambio** no se penaliza por haber cambiado. Lo que se
  juzga es si dice lo mismo que la oracion propuesta, no cuanto se aparto de la
  anterior. `¿Por qué estás corriendo?` → `¿Por qué están caminando?`, junto a una
  respuesta que paso de `are you running` a `are they walking`, pasa el criterio.

**Criterio de aceptacion**: (a) un candidato cuya unica falla es quedar fuera del
rango de longitud de su nivel **si** se propone
([F-QICOR-R011](#F-QICOR-R011)); (b) un candidato que introduce una palabra mal
ubicada para el nivel, ausente en el original, recibe veredicto negativo del
criterio 2; (c) un candidato que conserva un problema lexico que ya tenia el
original y no agrega ninguno pasa el criterio 2; (d) mientras queden intentos, un
candidato que reprueba cualquiera de los criterios 2 a 5 no se propone y se vuelve
a intentar ([F-QICOR-R012](#F-QICOR-R012)); (e) un candidato que corrige la
respuesta de `are you eating` a `are you drinking` y conserva la traduccion `¿Qué
estás comiendo?` recibe veredicto negativo del criterio 4, y el mismo candidato con
la traduccion `¿Qué estás bebiendo?` lo pasa.

**Error**: "El ejercicio corregido de '{quizId}' se propuso sin haberse sometido al criterio '{criterio}'"

</details>

<a id="F-QICOR-R005"></a>
### Rule[F-QICOR-R005] - El resultado cumple la consigna, verificado con el mismo criterio que detecto el incumplimiento
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Antes de proponerse, el ejercicio corregido se somete a la **misma validacion
> de consigna** que produjo el incumplimiento, y solo se propone si esta declara
> que cumple. No alcanza con que las violaciones parezcan atendidas: la
> correccion no puede pasar y despues fallar la auditoria. Es el **unico criterio
> absoluto** de toda la feature: la entrega del mejor candidato
> ([F-QICOR-R012](#F-QICOR-R012)) no lo alcanza, y un candidato que no lo cumple
> no compite por ser el mejor ([F-QICOR-R013](#F-QICOR-R013)).

<details><summary>Detalle</summary>

**Por que este si y los demas no.** Los criterios de
[F-QICOR-R004](#F-QICOR-R004) miden lo que la correccion **no debe empeorar**;
este mide si la correccion **hizo lo que vino a hacer**. Un ejercicio corregido
que sigue incumpliendo su consigna no es una correccion imperfecta: no es una
correccion. Entregarlo tendria las dos consecuencias que la feature entera existe
para evitar —el operador aprueba un cambio que no resuelve nada, y la auditoria
siguiente vuelve a abrir **el mismo** diagnostico— y ninguna de las dos se arregla
declarandolo. La diferencia con la longitud es exactamente esa: un corregido fuera
de rango abre **otro** diagnostico, que tiene su propio carril y su propio
corrector ([F-QICOR-R011](#F-QICOR-R011)); uno que sigue incumpliendo reabre el
que ya estaba.

**Consultarlo y verificarlo no se pagan dos veces.** Quien corrige necesita saber
si su candidato cumple para decidir si reintenta, asi que lo consulta con esta
misma validacion ([F-QICOR-R015](#F-QICOR-R015)). Cuando el candidato entregado
llega a esta verificacion, el veredicto sobre **ese mismo contenido** ya esta
registrado bajo su huella y se reutiliza sin volver a pagarlo
([F-QINST-R008](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R008),
[F-QINST-R009](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R009)).
Lo que esta regla exige es que la verificacion **ocurra**, no que se cobre.

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
parte de las violaciones de su ejercicio no se propone; (c) agotados los intentos,
un candidato que reprueba **unicamente** este criterio no se entrega ni siquiera
como mejor candidato, y la tarea termina como correccion no lograda
([F-QICOR-R006](#F-QICOR-R006), [F-QICOR-R012](#F-QICOR-R012)).

**Error**: "Se propuso para el ejercicio '{quizId}' una correccion que sigue incumpliendo su consigna"

</details>

<a id="F-QICOR-R006"></a>
### Rule[F-QICOR-R006] - No lograr una correccion queda para el unico caso en que no hay nada que entregar
**Severity**: critical | **Validation**: VALIDATED

> Una tarea termina como **correccion no lograda** cuando, agotados los intentos,
> **ningun candidato resulto entregable**: ninguno cumplio su consigna
> ([F-QICOR-R005](#F-QICOR-R005)) o ninguno constituyo un cambio real sobre el
> original ([F-QICOR-R010](#F-QICOR-R010)). En ese caso el sistema informa
> explicitamente el fallo y **nombra los criterios que reprobaron**; el ejercicio
> del curso no cambia y la tarea queda disponible. Reprobar cualquier **otro**
> criterio ya no lleva a este desenlace: el mejor candidato se entrega igual,
> declarando lo que no cumplio ([F-QICOR-R012](#F-QICOR-R012),
> [F-QICOR-R014](#F-QICOR-R014)). Este desenlace supone ademas que **habia** algo
> que corregir; cuando el diagnostico ya no se sostiene no hubo fracaso alguno y el
> desenlace es otro ([F-QICOR-R009](#F-QICOR-R009)).

<details><summary>Detalle</summary>

**Lo que esta regla dejo de prohibir, y lo que sigue prohibiendo.** Hasta la
corrida del 2026-08-11 esta regla cerraba dos desenlaces: callarse y entregar el
mejor candidato. El segundo dejo de estar prohibido —es ahora la conducta normal
([F-QICOR-R012](#F-QICOR-R012))— y de el sobrevive intacta la mitad que importaba:
**sin decirlo**. Entregar en silencio un ejercicio que reprueba un criterio sigue
prohibido, y lo prohibe [F-QICOR-R014](#F-QICOR-R014). Callarse tambien sigue
prohibido, y es lo que esta regla conserva entero: terminar una revision sin
propuesta y sin explicacion deja al operador sin distinguir "no hacia falta
corregir nada" de "no pude". Es la misma clase de defecto silencioso que ya costo
caro en la validacion de consigna, donde un pedido ignorado sin aviso se veia igual
que uno atendido
([F-QINST-R015](../2026-07-29.02_validacion-de-consigna-de-quiz/REQUIREMENT.md#F-QINST-R015)).

**El argumento que sostenia el descarte, y por que no alcanzo.** Decia que aca el
piso a superar no es cero: la generacion de ejercicios por ausencia de lema emite
su mejor candidato porque compite contra la nada
([F-LASAG-R009](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R009)),
mientras que una correccion compite contra un ejercicio que ya funcionaba en todo
salvo en su consigna. El argumento es correcto y sigue en pie **como razon para
declarar**, no como razon para descartar: el piso alto es exactamente lo que hace
que el operador tenga que ver que reprobo antes de aprobar. Lo que lo volvio
insostenible como veto fue el precio, medido sobre el plan real: de 162 tareas sin
corregir, **157 tenian un candidato producido, juzgado y tirado**, y en 78 de ellas
el unico reproche era la longitud — un criterio que desde
[F-QICOR-R011](#F-QICOR-R011) ni siquiera decide. Descartar no dejaba el curso
mejor: lo dejaba igual, con el defecto adentro y el trabajo pagado.

**Nombrar los criterios reprobados es parte de la regla, no un adorno.** "No pude
corregirlo" no le dice al operador si conviene reintentar, corregir a mano o
revisar la consigna del knowledge; "ningun candidato llego a cumplir la consigna"
si — y es, ademas, la señal mas util que produce este desenlace, porque cuando se
repite sobre varios ejercicios del mismo knowledge lo que hay que mirar es la
consigna ([DOUBT-CONSIGNA-CULPABLE](#DOUBT-CONSIGNA-CULPABLE)). Lo que **nunca**
puede nombrar es la longitud ([F-QICOR-R011](#F-QICOR-R011)), y desde
[F-QICOR-R012](#F-QICOR-R012) tampoco puede nombrar a solas a ninguno de los otros
criterios de [F-QICOR-R004](#F-QICOR-R004): si el ejercicio cumplia su consigna,
ese candidato se entrego.

**Cuanto queda de este desenlace.** La misma corrida lo anticipa con precision: de
las 159 tareas que terminaron sin correccion tras agotar sus tres intentos,
**157** tenian como reproche registrado un criterio que ya no impide la entrega —78
longitud, 71 edicion acotada, 4 resolubilidad, 3 vocabulario, 1 distincion— y solo
**2** reprobaron el cumplimiento de la consigna. Este desenlace pasa de ser el mas
frecuente de la corrida a ser el mas raro, y eso es lo que se buscaba: que "no se
logro" quiera decir de verdad que no hay nada que ofrecer.

Esta regla cubre el caso en que la correccion **se intento y no alcanzo**. Una
interrupcion antes de intentarlo —el servicio no responde, se agota el tiempo de
espera, lo que vuelve no es un ejercicio— tambien termina sin propuesta y sin tocar
el curso, con el mismo criterio que ya rige las demas correcciones
([F-KTLR-R010](../2026-07-06.01_revisar-knowledge-title-length-con-agente/REQUIREMENT.md#F-KTLR-R010));
lo propio de esta regla es que aca **hubo** candidatos y ninguno resulto
entregable.

**No confundir este desenlace con el de un diagnostico caido.** Cuando la
revalidacion previa dice que el ejercicio ya cumple
([F-QICOR-R009](#F-QICOR-R009)) no se genero ningun candidato, no reprobo ningun
criterio y no hay nada que informar como fallo: la tarea se cierra porque el
problema no existe. Mezclar los dos desenlaces en un mismo recuento le diria al
operador que tiene decenas de correcciones imposibles cuando lo que tiene son
decenas de diagnosticos viejos que se cayeron solos — dos hechos ante los que
reacciona de forma opuesta.

**Criterio de aceptacion**: (a) agotados los intentos sin que ningun candidato
cumpla su consigna, la revision termina informando el fallo y nombrando al menos un
criterio reprobado, no queda ninguna propuesta pendiente para esa tarea, el
ejercicio del curso es identico al original, y la tarea sigue disponible; (b) una
tarea cuyo mejor candidato cumple su consigna y reprueba otros criterios **no**
termina en este desenlace, sino en propuesta ([F-QICOR-R012](#F-QICOR-R012));
(c) la longitud fuera de rango nunca figura entre los criterios reprobados de este
desenlace.

**Error**: "No se logro una correccion aceptable para el ejercicio '{quizId}': ningun candidato resulto entregable ({criterios})"

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
Lo que el ejercicio guardado conserva **aunque el candidato no lo traiga** —las
partes vacias del enunciado— lo fija [F-QICOR-R010](#F-QICOR-R010), que es donde
esa decision vive entera para no quedar enunciada en dos lugares.

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

**Siguen siendo cuatro desenlaces.** Ni que una correccion propuesta haya quedado
fuera del rango de longitud de su nivel, ni que haya llegado sin cumplir alguno de
los criterios del catalogo, abren un quinto: los dos son **atributos** de la
correccion propuesta, no formas distintas de terminar. Cuantas propuestas quedaron
fuera de rango lo declara [F-QICOR-R011](#F-QICOR-R011); cuantas llegaron con
criterios sin cumplir lo declara [F-QICOR-R014](#F-QICOR-R014). Cada decision vive
entera en su regla y aca solo se cuenta.

**Lo que el tope cuenta no cambio: tareas tocadas.** Que el lazo de intentos lo
conduzca ahora quien corrige ([F-QICOR-R012](#F-QICOR-R012)) no altera esta regla:
una tarea sigue costando lo que cueste corregirla —sus intentos incluidos— y el
tope sigue contando tareas, no intentos. Lo unico que cambia es que el gasto por
tarea queda acotado por dos perillas en lugar de una: el tope de tareas de la
corrida y el maximo de intentos por tarea.

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

<a id="F-QICOR-R010"></a>
### Rule[F-QICOR-R010] - Una parte vacia del enunciado no es contenido: ni bloquea la correccion ni se pierde al aprobarla
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una **parte vacia** —una parte de texto fijo del enunciado que no aporta un solo
> caracter— no es contenido del ejercicio. De ahi las dos mitades de esta regla.
> Queda **fuera** de la comparacion de la edicion acotada
> ([F-QICOR-R003](#F-QICOR-R003)): que el candidato la traiga o no la traiga nunca
> decide si se propone. Y como ninguna violacion pidio quitarla, al aprobar la
> correccion el ejercicio guardado la **conserva**, en su misma posicion. Perderla
> es una perdida, no una correccion.

<details><summary>Detalle</summary>

**El bloqueo que la motiva se midio (2026-08-10).** Corrida contra el curso real:
**192 de las 519 tareas de correccion (37%) son estructuralmente incorregibles** —
ninguna puede producir una correccion aceptable, por correcto que sea el
candidato—. En el curso entero, **4015 de 11487 ejercicios (35%)** tienen al menos
una parte vacia. La correlacion no deja lugar a dudas: de las 7 tareas que
reprobaron la edicion acotada en el ultimo lote, **6** tienen una parte vacia; de
las 10 que si produjeron correccion, **ninguna**.

**El caso real**, tal cual esta hoy en el curso
(`a1/present-simple/mixed-practice-with-the-present-simple-forms-2`):

| | Partes del enunciado | Traduccion |
|---|---|---|
| Original (3 partes) | *(vacia)* + hueco `[Do I want]` + `(I / need) a pen?` | `¿Quiero un bolígrafo?` |
| Candidato correcto (2 partes) | hueco `[Do I need]` + `(I / need) a pen?` | `¿Necesito un bolígrafo?` |

El candidato corrige exactamente lo que la marca del hueco pide —`(I / need)`— y
vuelve con 2 partes en lugar de 3. Hoy se lo rechaza por eso y por nada mas.

**La perdida es por construccion, no un descuido.** El ejercicio viaja como texto
para poder corregirse: esa es la forma en que se lo transmite y sobre la que un
tercero propone una variante
([FEAT-QSENT](../2026-04-22.01_quiz-sentence-dsl/REQUIREMENT.md)). Una parte que
no aporta caracteres no deja **nada** en esa cadena, asi que al volver a leerla no
hay de donde recuperarla. No es algo que se arregle leyendo mejor: hay que decidir
si esa parte importa.

**Por que no importa: tres hechos medidos, no una opinion.**

1. **No aporta un caracter.** El alumno ve exactamente el mismo ejercicio con ella
   y sin ella.
2. **Nunca separa nada.** Sobre los 11487 ejercicios, las partes vacias estan
   **siempre en un borde** —470 al principio, 3545 al final— y **ninguna** entre
   dos partes con texto. No marca una posicion ni parte una oracion en dos.
3. **El curso ya convive con las dos formas.** 884 ejercicios tienen exactamente
   la forma que produce la correccion —hueco y texto, sin la parte vacia— y
   funcionan. El caso esta al lado del ejemplo de arriba: en
   `a1/present-simple/yes-no-questions-with-regular-verbs-in-the-present-simple`
   vive hoy un ejercicio de hueco `[Do I need]` + `(I / need) a mobile?`, **sin**
   parte vacia, hermano casi literal del que la correccion no puede tocar.

A eso se suma un argumento de la propia regla:
[F-QICOR-R003](#F-QICOR-R003) exige que lo no señalado vuelva "identico,
**caracter por caracter**". Una parte vacia no tiene caracteres. Contar partes es
un criterio **mas estricto** que el que la regla enuncia, y es ese exceso —no la
regla— el que bloquea.

**Por que, aun asi, se conserva al guardar.** "No es contenido" corta para los dos
lados: si ninguna violacion pidio quitarla, nada autoriza a quitarla. Lo que
decide es la asimetria de costos:

| | Si se conserva | Si se pierde |
|---|---|---|
| Efecto sobre el alumno | Ninguno | Ninguno |
| Efecto sobre el curso guardado | Ninguno | Hasta 4015 ejercicios reestructurados de a uno, sin que ninguna regla lo pida |
| Que ve el operador | No hace falta que vea nada | Aprueba una correccion de consigna y ademas normaliza estructura, sin enterarse |
| Si mañana algo depende de la forma | Nada que deshacer | Cambio irreversible sobre un tercio del curso |

Es exactamente la clase de daño que dio origen a
[FEAT-RPRES](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md):
2658 ejercicios degradados en silencio porque nadie habia escrito que no se podia
([F-RPRES-R001](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md#F-RPRES-R001)).
Aquella feature ya legislo el caso **simetrico** —cuando la correccion produce
**mas** partes que el original, cada parte nueva nace completa
([F-RPRES-R006](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md#F-RPRES-R006))—;
que pasa cuando produce **menos** era el hueco, y es lo que esta regla cierra.
Conservar no cuesta nada: es una parte sin texto. Ver
[DOUBT-PARTE-VACIA-AL-GUARDAR](#DOUBT-PARTE-VACIA-AL-GUARDAR).

**Lo que esta regla NO autoriza.** Como la parte vacia no es contenido, su
desaparicion **tampoco es un cambio**. Un candidato cuya unica diferencia con el
original es no traer una parte vacia no corrigio nada: para
[F-QICOR-R003](#F-QICOR-R003) la oracion quedo identica —y entonces la traduccion
tambien tiene que serlo— y para [F-QICOR-R005](#F-QICOR-R005) el ejercicio sigue
incumpliendo su consigna. Termina como correccion no lograda
([F-QICOR-R006](#F-QICOR-R006)), nunca como propuesta — y no lo salva la entrega
del mejor candidato, porque un candidato que no es un cambio real **no compite**
([F-QICOR-R013](#F-QICOR-R013)). Esta regla no abre ninguna puerta para proponer un
ejercicio que el alumno leeria identico al que ya tenia.

**Lo que esta regla NO toca.** Habla de una **parte entera** del enunciado que no
aporta caracteres, no del atributo de texto de un hueco ni de ningun otro
atributo: la distincion entre "vacio" y "ausente" sobre los atributos sigue
rigiendo sin cambios
([F-RPRES-R002](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md#F-RPRES-R002)),
y nada de aca autoriza a convertir un valor vacio en ausente. Y alcanza a **esta**
correccion: si otras correcciones que tambien transmiten el ejercicio como texto
pierden partes vacias, eso pertenece a la preservacion del contenido no revisado,
no a esta feature.

**Criterio de aceptacion**, partiendo del ejercicio real de arriba —partes
*(vacia)* + hueco `["Do I want"]` + `"(I / need) a pen?"`, tres partes—:
(a) un candidato que corrige la respuesta aceptada a `Do I need`, mueve la
traduccion con ella y vuelve con **dos** partes —el hueco y el texto, sin la
vacia— **no** reprueba la edicion acotada: el unico texto que cambio es el que la
violacion señala y las partes con texto vuelven identicas caracter por caracter;
(b) aprobada esa propuesta y **releido el curso**, el ejercicio guardado vuelve a
tener sus **tres** partes —la vacia sigue presente y en su misma posicion, la
primera— y el hueco tiene `Do I need`;
(c) un candidato cuya unica diferencia con el original es no traer la parte vacia
no se propone.

**Error**: "El ejercicio '{quizId}' perdio al aplicar su correccion una parte vacia del enunciado que el original tenia"

</details>

<a id="F-QICOR-R011"></a>
### Rule[F-QICOR-R011] - La longitud se mide y se declara, pero no rechaza una correccion
**Severity**: critical | **Validation**: VALIDATED

> La longitud **no decide**: ni rechaza una correccion ni suma a la hora de elegir
> el mejor candidato entre los que se vieron ([F-QICOR-R013](#F-QICOR-R013)). Se
> sigue midiendo con la misma medicion de la auditoria, pero un ejercicio corregido
> que queda fuera del rango objetivo de su nivel **se propone igual**, siempre que
> cumpla su consigna ([F-QICOR-R005](#F-QICOR-R005)). A cambio, la propuesta **declara** que quedo
> fuera de rango —con la longitud medida y el rango del nivel— y la corrida cuenta
> cuantas de sus propuestas quedaron asi. Entre un ejercicio bien escrito fuera de
> rango y uno mal escrito dentro se elige el primero; nunca en silencio.

<details><summary>Detalle</summary>

**La decision, en los terminos en que se tomo**: es mas importante que el
ejercicio este bien escrito que si entra o no en el rango. Si el problema es la
longitud y la alternativa es dejar en el curso un ejercicio que incumple su
consigna, se elige el que esta fuera de rango.

**El bloqueo que la motiva se midio** (corrida del 2026-08-11 sobre el plan real,
305 tareas de este tipo intentadas, 159 sin corregir):

| Criterio que reprobo | Tareas sin corregir |
|---|---|
| Longitud fuera de rango | **78** |
| Edicion no acotada | 71 |
| Resolubilidad, vocabulario, consigna, distincion | 10 |

La longitud era el motivo mas frecuente y el techo mas alto que le quedaba a la
feature: 78 ejercicios siguen incumpliendo su consigna hoy porque su correccion
media unos tokens de mas.

**El criterio no estaba mal calibrado: estaba mal planteado.** De esos 78, **75
tienen ademas su propia tarea de longitud en el mismo plan** — su ejercicio
**original** ya estaba fuera de rango antes de que nadie lo tocara. Solo 3 estaban
dentro y salieron por efecto de la correccion. La concentracion no es casual: en el
plan entero, 211 de las 519 tareas de consigna (41%) caen sobre ejercicios que
tambien tienen tarea de longitud, pero entre los 78 bloqueados la proporcion salta
al **96%**. El criterio no estaba impidiendo un empeoramiento; estaba exigiendo que
la correccion de consigna arreglara **de paso** un defecto de longitud preexistente
que ninguna violacion señalaba. Eso es exactamente lo que
[F-QICOR-R004](#F-QICOR-R004) ya prohibe para el criterio 2 —"los flags
preexistentes no bloquean... exigir lo contrario volveria incorregible a todo
ejercicio con residuo previo, que es la mayoria"—: el criterio 1 era el unico
enunciado en terminos absolutos dentro de una regla que se llama "no empeorar".

**Por que no alcanza con volverlo no-regresivo** —bloquear solo si el candidato
empeora la longitud respecto del original—. Destrabaria 75 de los 78, pero deja
viva la veto justo en el caso sobre el que ya se decidio: un ejercicio que estaba
dentro y sale. Ademas seguiria bloqueando a los que ya estaban fuera y quedan mas
afuera, que es el subconjunto mas probable — resolver un desajuste entre la marca
del hueco y la respuesta suele **alargar** la oracion (`Do I want` → `Do I need`
empata, pero `is he` → `are they walking` no), asi que el criterio castiga
sistematicamente al tipo de correccion mas comun. Y obliga a fijar cuanto es "peor"
en tokens sobre una magnitud que ya tiene su propio margen de tolerancia
([F-SLEN-R009](../2026-03-18.01_sentence-length/REQUIREMENT.md#F-SLEN-R009)), un
umbral que ninguna violacion pidio y que habria que defender.

**Por que no alcanza con volverlo una preferencia** —entre dos candidatos que pasan
todo lo demas, gana el que entra en rango—. La primera razon ya esta medida: los 78
consumieron **3 intentos cada uno**, 234 consultas pagadas, y cada reintento recibio
el motivo del fallo a la vista; en **78 de 78** el reintento informado nunca produjo
un candidato dentro de rango que pasara todo lo demas. En esa misma corrida, las
125 correcciones propuestas salieron **todas al primer intento**: ningun reintento
rescato una sola tarea. La preferencia compraria de nuevo lo que ya se compro y se
perdio. La segunda razon es de diseño: el catalogo **no puede ver "bien escrito"**
—no hay criterio de naturalidad—, asi que "gana el que entra en rango" selecciona
sistematicamente la variante comprimida sobre la natural. Eso es la inversion exacta
de la decision que esta regla implementa: preferiria el ejercicio forzado dentro de
rango sobre el bien escrito fuera, y lo haria sin que nada lo delate.

**Lo que impide que la oracion se infle no era este criterio**, y por eso quitarlo
no abre ninguna puerta. Es [F-QICOR-R003](#F-QICOR-R003): el cambio se limita a lo
que las violaciones señalan y todo lo demas vuelve identico caracter por caracter.
Un candidato no puede estirarse a gusto — solo puede reescribir el fragmento
señalado, y eso son unos tokens de mas, no una oracion nueva.

**Que se declara, y por que declararlo es parte de la regla.** Aceptar no es
esconder. La correccion se escribe sobre el curso al aprobarse, y quien la revisa
tiene que poder ver **antes de aprobarla** que el ejercicio corregido queda fuera
del rango de su nivel:

| Donde | Que se ve |
|---|---|
| En la propuesta | Que la longitud del ejercicio corregido quedo fuera del rango objetivo de su nivel, con la longitud medida y el rango |
| En el reporte de la corrida | Cuantas de las correcciones propuestas quedaron fuera de rango |

El recuento de la corrida es lo que vuelve revisable la concesion: si resultara que
casi todas las propuestas quedan fuera de rango, la decision merece rediscutirse con
ese dato, y sin el recuento nadie se enteraria.

**La auditoria siguiente lo va a marcar, y esta bien que lo haga.** Al cambiar el
contenido, el ejercicio corregido se vuelve a medir, y si quedo fuera de rango el
[Analizador de longitud](glossary:Analizador de longitud) lo señala y le abre **su
propia tarea**, que tiene **su propio corrector**. Eso no contradice a [F-QICOR-R005](#F-QICOR-R005): esa regla
existe para que no vuelva el **mismo** diagnostico —una correccion de consigna
aprobada que sigue incumpliendo su consigna—; que aparezca **otro** diagnostico, con
su carril propio, es el sistema funcionando. Bloquear aca significaba lo contrario
de lo que parecia: conservar un defecto que **solo esta correccion puede arreglar**
para evitar uno que **otro mecanismo ya sabe arreglar**.

**Es ademas la postura que el resto del sistema ya tenia.** La correccion de
vocabulario declaro exactamente esto desde su primera entrega —"no se valida que la
nueva oracion siga dentro del rango de tokens esperado; si queda fuera de rango,
aparecera como una nueva tarea de longitud en la proxima auditoria"
([FEAT-RCLA](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md))— y la
generacion de ejercicios entrega su mejor candidato aunque reprobe criterios
([F-LASAG-R009](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R009)).
La correccion de consigna era la unica del sistema donde la longitud rechazaba, y
esta regla la devuelve a la fila.

**Lo que esta regla NO autoriza.** No autoriza a dejar de medir: la longitud se
sigue midiendo con la medicion real de la auditoria, por la misma razon de siempre
—una medicion aproximada haria que la propuesta declare una cosa y la auditoria
siguiente otra—. Y no autoriza a alargar la oracion mas alla de lo que la violacion
señala ([F-QICOR-R003](#F-QICOR-R003) sigue intacta).

**Que quedo de propio de esta regla ahora que ningun criterio rechaza.** Desde
[F-QICOR-R012](#F-QICOR-R012) ninguno de los criterios de
[F-QICOR-R004](#F-QICOR-R004) impide por si solo entregar al mejor candidato, asi
que "no rechaza" dejo de distinguir a la longitud. Lo que la sigue distinguiendo
son dos cosas, y son las que esta regla conserva:

| | La longitud | Los demas criterios del catalogo |
|---|---|---|
| ¿Empuja a reintentar? | No | Si |
| ¿Cuenta para elegir al mejor candidato? | No: solo desempata al final, cuando todo lo demas empato ([F-QICOR-R013](#F-QICOR-R013)) | Si |
| ¿Que se declara de ella? | La medicion: cuantos tokens y cual era el rango del nivel | Su nombre y el motivo del reproche ([F-QICOR-R014](#F-QICOR-R014)) |
| ¿Entra en la lista de criterios incumplidos de la propuesta? | No: tiene declaracion y recuento propios | Si |

Que desempate **al final** no reabre la Opcion B de
[DOUBT-LONGITUD-NO-BLOQUEANTE](#DOUBT-LONGITUD-NO-BLOQUEANTE) —la longitud como
preferencia—, y conviene decir por que: aquella preferencia gastaba intentos
persiguiendo el rango y elegia sistematicamente la variante comprimida sobre la
natural. Un desempate que solo se consulta cuando dos candidatos son
**indistinguibles** para todos los demas criterios no hace ninguna de las dos
cosas: no dispara un solo reintento y no puede ganarle a nada, porque solo se
pregunta cuando no hay nada mas que preguntar.

**Criterio de aceptacion**: (a) un candidato que pasa los criterios 2 a 5 y cumple
su consigna, y cuya unica falla es quedar fuera del rango de longitud de su nivel,
**se propone**; (b) esa propuesta declara la longitud medida y el rango objetivo del
nivel, de modo que se vea sin abrir el ejercicio; (c) ninguna tarea termina como
correccion no lograda nombrando la longitud entre los criterios reprobados
([F-QICOR-R006](#F-QICOR-R006)); (d) el reporte de la corrida declara cuantas de las
correcciones propuestas quedaron fuera de rango, sin agregar un quinto desenlace
([F-QICOR-R008](#F-QICOR-R008)); (e) la medicion sigue siendo la de la auditoria: un
candidato dentro de rango se declara dentro y uno fuera se declara fuera, con el
mismo conteo de tokens y el mismo rango de nivel que aplicaria el analizador de
longitud; (f) la longitud no cambia cual candidato se entrega mientras haya
cualquier otra diferencia entre ellos: uno fuera de rango que cumple un criterio
mas gana sobre uno dentro de rango que cumple uno menos
([F-QICOR-R013](#F-QICOR-R013)).

**Error**: "Se propuso para el ejercicio '{quizId}' una correccion fuera del rango de longitud de su nivel sin declararlo"

</details>

<a id="F-QICOR-R012"></a>
### Rule[F-QICOR-R012] - El lazo de intentos lo conduce quien corrige, y al agotarlo entrega su mejor candidato
**Severity**: critical | **Validation**: VALIDATED

> Por cada tarea el sistema pide la correccion **una sola vez**. Quien corrige
> produce un candidato, lo juzga con los criterios del propio sistema
> ([F-QICOR-R015](#F-QICOR-R015)) y **decide por su cuenta si vuelve a intentar**,
> hasta un maximo de intentos configurable. Mientras intenta conserva el
> [mejor candidato](glossary:Mejor candidato) que vio
> ([F-QICOR-R013](#F-QICOR-R013)) y, al agotar los intentos sin ninguno que cumpla
> todos los criterios, **entrega ese mejor** en lugar de no entregar nada. El
> sistema no vuelve a pedir un candidato desde cero ante un criterio reprobado: lo
> que recibe es el resultado del lazo, no cada uno de sus pasos.

<details><summary>Detalle</summary>

**Como funcionaba y que costaba.** El lazo lo conducia el sistema desde afuera: le
pedia un candidato, lo juzgaba contra el catalogo y, si alguno de los criterios
reprobaba, **le pedia otro candidato desde cero**, hasta tres veces. Si ninguno de
los tres pasaba todo, los tres se descartaban y el ejercicio quedaba sin corregir.
Medido sobre el plan real (corrida del 2026-08-11, 305 tareas intentadas):

| | Tareas | Intentos consumidos |
|---|---|---|
| Terminaron en propuesta | 125 | 1 cada una — **todas al primer intento** |
| Terminaron sin correccion | 159 | 3 cada una — 477 intentos, de los cuales **318 fueron reintentos** |

Ningun reintento rescato una sola tarea. Las 318 consultas de reintento, cada una
informada con el motivo del fallo anterior, produjeron **cero** correcciones: las
125 que salieron bien salieron a la primera. Y por cada una de las 159 tareas
perdidas se habian producido y tirado tres candidatos.

**Por que el lazo va donde estan los candidatos.** La razon de fondo es que el
sistema tenga **una sola forma de hacer las cosas**: la generacion de ejercicios
por ausencia de lema ya conduce su propio lazo, conserva su mejor candidato y lo
entrega al agotar los intentos
([F-LASAG-R009](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R009)),
y no hay ninguna razon de negocio por la que dos correcciones del mismo sistema
deban resolver el mismo problema de dos maneras distintas. A eso se suman dos
consecuencias practicas:

- **El mejor candidato solo puede conservarlo quien los tiene.** Un lazo por fuera
  ve un candidato por vez y decide si lo acepta o lo tira; para elegir "el mejor de
  los tres" alguien tiene que recordar los tres. Ese alguien es quien los produjo.
- **Pedir "otro candidato" no es lo mismo que mejorar el propio.** El lazo de
  afuera solo sabe pedir de nuevo; quien corrige puede partir de lo que ya escribio
  y tocar unicamente lo que el reproche señala — que es, ademas, lo que
  [F-QICOR-R003](#F-QICOR-R003) le pide.

**Lo que el sistema sigue haciendo, y que no delega.** Que el lazo viva del lado de
quien corrige no lo convierte en juez de si mismo:

| Sigue siendo del sistema | Por que |
|---|---|
| El catalogo de criterios y su medicion | Quien corrige los **consulta**, no los reimplementa ([F-QICOR-R015](#F-QICOR-R015)) |
| La revalidacion del diagnostico antes de empezar | Decide si hay algo que corregir, y eso se resuelve antes de pedir nada ([F-QICOR-R009](#F-QICOR-R009)) |
| La verificacion de que lo entregado cumple la consigna | Es el unico criterio absoluto y no se toma de palabra ([F-QICOR-R005](#F-QICOR-R005)) |
| El tope de tareas de la corrida | Acota el gasto, y cuenta tareas ([F-QICOR-R008](#F-QICOR-R008)) |
| Declarar lo que el candidato entregado no cumplio | Entregar no es esconder ([F-QICOR-R014](#F-QICOR-R014)) |

**Cuantos intentos, y de quien es el numero.** El maximo sigue siendo configurable
y sigue valiendo 3 por defecto ([DOUBT-REINTENTOS](#DOUBT-REINTENTOS)); lo que
cambia es que ahora se consume **dentro** de la unica solicitud de correccion, y
que quien decide gastar el intento siguiente es quien acaba de ver reprobar al
candidato anterior. Que el numero lo fije la corrida y no quien corrige es lo que
mantiene el gasto acotado y previsible: sin un maximo declarado por fuera, una
tarea dificil podria intentar indefinidamente.

**Que cambia esto para el operador.** De 162 tareas que hoy terminan sin corregir,
**157 pasan a terminar en propuesta** — con lo que no cumplieron a la vista
([F-QICOR-R014](#F-QICOR-R014))—. El operador pasa de recibir una lista de fallos
que no puede accionar a recibir correcciones que puede aprobar o rechazar. El
riesgo que eso abre —aprobar distraidamente una correccion que reprobo algo— es
real y se trata donde corresponde: haciendo que el reproche viaje con la propuesta,
no escondiendo la propuesta.

**Criterio de aceptacion**: (a) dada una tarea cuyo primer candidato reprueba un
criterio del catalogo, el sistema **no emite una segunda solicitud de correccion**:
la decision de reintentar no es suya; (b) agotados los intentos sin ningun candidato
que cumpla todos los criterios, la tarea termina en **propuesta** con el mejor
candidato visto, siempre que ese candidato cumpla su consigna
([F-QICOR-R005](#F-QICOR-R005)); (c) el maximo de intentos es configurable y ninguna
tarea consume mas intentos que ese maximo; (d) el desenlace de una tarea no depende
de cuantos intentos se hayan consumido: si el mejor candidato aparecio en el primer
intento y ninguno posterior lo supero, se entrega ese.

**Error**: "La correccion del ejercicio '{quizId}' agoto sus intentos y descarto el mejor candidato que habia producido"

</details>

<a id="F-QICOR-R013"></a>
### Rule[F-QICOR-R013] - Cual es el mejor candidato: primero si sirve, despues cuanto conserva, la longitud al final
**Severity**: critical | **Validation**: VALIDATED

> "Mejor" no es una impresion: es un orden fijo. **Compite** unicamente el candidato
> que cumple su consigna ([F-QICOR-R005](#F-QICOR-R005)) y que constituye un cambio
> real sobre el original ([F-QICOR-R001](#F-QICOR-R001),
> [F-QICOR-R010](#F-QICOR-R010)); el que no corrige lo que vino a corregir no es
> candidato a nada. Entre los que compiten gana el que **cumple mas criterios** del
> catalogo; a igualdad, el que cumple el de **mayor precedencia** entre los que los
> distinguen —resolubilidad, traduccion fiel, vocabulario del nivel, distincion,
> edicion acotada, en ese orden—; y solo si todo lo anterior empata desempata la
> **longitud**, que va ultima y nunca suma ([F-QICOR-R011](#F-QICOR-R011)). Si aun
> asi empatan, gana el **primero que se vio**.

<details><summary>Detalle</summary>

**Por que la consigna es una condicion para competir y no el primer criterio del
orden.** Si fuera el primero del orden, un ejercicio que no cumple su consigna
podria ganar cuando ningun otro cumpla nada — y entregarlo seria proponer un cambio
que no arregla el problema que lo origino. No es un candidato peor: no es un
candidato. La misma condicion alcanza al cambio real: un candidato identico al
original no compite, porque aprobarlo no cambiaria nada
([F-QICOR-R010](#F-QICOR-R010) lo enuncia para el caso de la parte vacia).

**El orden de precedencia, y de donde sale** [ASSUMPTION]. La decision de fondo la
fijo el usuario —*un ejercicio bien escrito fuera de rango vale mas que uno mal
escrito dentro*— y el orden la lleva hasta el final ordenando los criterios por
**cuanto daña al alumno lo que reprueba**, de mas a menos:

| Precedencia | Criterio | Que significa reprobarlo |
|---|---|---|
| 1 | Resoluble y del mismo tipo | El ejercicio no se puede resolver: para el alumno no sirve para nada |
| 2 | Traduccion fiel | El ejercicio se contradice a si mismo y enseña algo falso |
| 3 | Vocabulario del nivel | El ejercicio es resoluble, pero le pide al alumno una palabra que no le toca |
| 4 | Distincion | El ejercicio funciona, pero se pisa con otro que el alumno ya vio |
| 5 | Edicion acotada | El ejercicio esta bien; lo que sobra es cambio que el operador tiene que leer |
| 6 | Longitud | No afecta al alumno y tiene su propio carril de correccion ([F-QICOR-R011](#F-QICOR-R011)) |

La edicion acotada queda anteultima a proposito: es el reproche mas frecuente
—71 de las 159 tareas perdidas— y el unico cuyo daño **no lo sufre el alumno sino
el operador**, que tiene que revisar mas texto del que pidio. Vale menos que
cualquier defecto que llegue al curso.

**Por que cuenta primero la cantidad y despues la precedencia.** La cantidad es la
medida honesta de "cuanto conserva" un candidato, y es la que ya usa la generacion
de ejercicios
([F-LASAG-R009](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R009)).
La precedencia entra solo cuando la cantidad no distingue, y ahi es donde se hace
valer la decision del usuario: entre dos candidatos que cumplen tres criterios cada
uno, gana el que sigue siendo resoluble sobre el que quedo dentro del rango.

**Por que el desempate final es el primero visto y no el ultimo.** Porque el
resultado tiene que ser el mismo se hayan gastado uno o tres intentos: si el ultimo
empata con el primero, no aporto nada y no deberia cambiar la propuesta. Ademas
vuelve reproducible la correccion — dos corridas que ven los mismos candidatos
entregan el mismo — y evita premiar el gasto.

**Un criterio que no se pudo evaluar cuenta como no cumplido.** No es un castigo
sino la unica lectura prudente: un candidato del que no se sabe si conserva el
vocabulario del nivel no puede ganarle a uno del que se sabe que si. Y se declara
como tal ([F-QICOR-R014](#F-QICOR-R014)), para que el operador vea la diferencia
entre "reprobo" y "no se pudo verificar". La consigna es la excepcion de siempre:
si no se pudo obtener su veredicto, el candidato no compite
([F-QICOR-R005](#F-QICOR-R005)).

**Criterio de aceptacion**: (a) entre dos candidatos que cumplen su consigna, uno
que cumple cuatro criterios del catalogo y otro que cumple tres, se entrega el de
cuatro; (b) entre dos que cumplen la misma cantidad, uno que reprueba la
resolubilidad y otro que reprueba la edicion acotada, se entrega el segundo;
(c) un candidato que no cumple su consigna no se entrega aunque cumpla todos los
demas criterios y el unico candidato que si la cumple no cumpla ninguno; (d) entre
dos candidatos que cumplen exactamente los mismos criterios, uno dentro y otro fuera
del rango de longitud, se entrega el que quedo dentro; (e) un candidato dentro de
rango que reprueba la resolubilidad pierde contra uno fuera de rango que la cumple;
(f) un criterio que no se pudo evaluar cuenta como no cumplido para el orden;
(g) entre dos candidatos que empatan en todo, se entrega el que se vio primero.

**Error**: "Se entrego para el ejercicio '{quizId}' un candidato peor que otro que la misma correccion habia producido"

</details>

<a id="F-QICOR-R014"></a>
### Rule[F-QICOR-R014] - Lo que se entrega sin cumplir todo se entrega diciendolo
**Severity**: critical | **Validation**: VALIDATED

> Cuando el ejercicio entregado no cumple todos los criterios del catalogo, la
> propuesta **nombra los que quedaron sin cumplir**, cada uno con su motivo y
> distinguiendo el que reprobo del que no se pudo evaluar, de modo que quien la
> revisa lo vea **antes** de aprobarla; y la corrida **cuenta cuantas de sus
> propuestas llegaron asi**. Lo que [F-QICOR-R012](#F-QICOR-R012) retiro es el veto,
> no la informacion. La longitud no entra en esa lista: tiene su propia declaracion
> y su propio recuento ([F-QICOR-R011](#F-QICOR-R011)).

<details><summary>Detalle</summary>

**Es la mitad que vuelve aceptable la entrega.** Descartar un candidato que reprobo
un criterio protegia al curso a costa de no corregir nada; entregarlo sin decirlo
seria lo contrario y peor, porque el operador aprobaria creyendo que aprueba una
correccion limpia. La unica combinacion defendible es entregar **y** declarar: el
sistema deja de decidir por el operador, pero le da exactamente lo que sabe para
que decida. Es la misma estructura con la que se acepto que la longitud dejara de
vetar ([F-QICOR-R011](#F-QICOR-R011)): se retira el veto y se paga con visibilidad.

| Donde | Que se ve |
|---|---|
| En la propuesta | Que criterios no cumplio el ejercicio entregado, con el motivo de cada uno y si el veredicto fue negativo o no se pudo obtener |
| En el reporte de la corrida | Cuantas de las correcciones propuestas llegaron con criterios sin cumplir |

**El recuento es lo que vuelve revisable la concesion.** Si resultara que casi todas
las propuestas llegan con criterios sin cumplir, lo que hay que mirar es quien
corrige, no la regla — y sin el recuento nadie se enteraria. Es el mismo argumento
con el que [F-QICOR-R011](#F-QICOR-R011) exige contar las propuestas fuera de rango
y con el que [F-QICOR-R008](#F-QICOR-R008) exige declarar lo que la corrida no
intento.

**Lo declarado se mide con la misma regla de medir.** Una propuesta que declara
"cumple el vocabulario del nivel" segun una medicion parecida a la de la auditoria,
y no la de la auditoria, es peor que una que no declara nada: dice lo contrario de
lo que la auditoria siguiente va a decir. Por eso quien corrige juzga con los
criterios del sistema ([F-QICOR-R015](#F-QICOR-R015)) y no con los propios, y por
eso la declaracion de la propuesta y el veredicto de esos criterios son lo mismo,
no dos opiniones.

**Que no exige esta regla.** No exige que la propuesta explique como arreglar lo
que quedo sin cumplir, ni que se la marque como inferior a las demas: es una
propuesta como cualquier otra, con el mismo vocabulario de decision
([F-QICOR-R007](#F-QICOR-R007)). Tampoco exige un desenlace nuevo
([F-QICOR-R008](#F-QICOR-R008)).

**Criterio de aceptacion**: (a) entregado un candidato que reprobo la edicion
acotada, la propuesta nombra ese criterio con su motivo y el operador puede verlo
sin abrir el ejercicio; (b) un candidato que cumplio todos los criterios produce una
propuesta sin criterios incumplidos declarados; (c) la longitud fuera de rango nunca
aparece en esa lista, y aparece en su declaracion propia
([F-QICOR-R011](#F-QICOR-R011)); (d) el reporte de la corrida declara cuantas de sus
correcciones propuestas llegaron con al menos un criterio sin cumplir, sin agregar un
desenlace nuevo; (e) un criterio que no se pudo evaluar se declara distinguiendolo de
uno que reprobo.

**Error**: "Se propuso para el ejercicio '{quizId}' una correccion que no cumple el criterio '{criterio}' sin declararlo"

</details>

<a id="F-QICOR-R015"></a>
### Rule[F-QICOR-R015] - Los criterios se consultan, no se reimplementan
**Severity**: critical | **Validation**: VALIDATED

> Los criterios del catalogo se pueden **consultar por separado**: se le presenta al
> sistema un ejercicio candidato junto al original y a su contexto de correccion, y
> devuelve el veredicto de cada criterio con su motivo, con **la misma medicion** con
> la que despues se juzga la propuesta. Quien corrige juzga sus candidatos con esa
> consulta y no con una regla de medir propia. Es lo que permite que el lazo viva de
> su lado ([F-QICOR-R012](#F-QICOR-R012)) sin que el catalogo termine existiendo dos
> veces.

<details><summary>Detalle</summary>

**Es la misma doctrina de [F-QICOR-R004](#F-QICOR-R004), extendida a quien corrige.**
Aquella regla ya exige que la longitud y el vocabulario se verifiquen con la
medicion real de la auditoria y no con una aproximacion, porque una correccion
aprobada con una regla de medir distinta de la que despues la juzga vuelve a
aparecer en la auditoria siguiente y el trabajo se hace dos veces. Mover el lazo sin
esta regla reproduciria exactamente ese defecto un nivel mas adentro: quien corrige
tendria que estimar por su cuenta si su candidato entra en el rango o si introdujo
una palabra de otro nivel, elegiria su mejor candidato con esa estimacion, y el
sistema despues mediria otra cosa.

**No es una capacidad hipotetica: es la forma en que el sistema ya resuelve esto.**
La consulta lexica de una oracion existe exactamente por este motivo
([FEAT-CLEX](../2026-07-22.01_consulta-lexica-de-oracion-por-cli/REQUIREMENT.md)), y
la generacion de ejercicios por ausencia de lema ya juzga la longitud de sus
candidatos preguntandole al sistema en lugar de contar palabras por su cuenta. Lo
que falta es que el catalogo **completo** de esta correccion sea consultable de la
misma manera.

**Que tiene que devolver la consulta.** Por cada criterio del catalogo: su veredicto
—cumple, no cumple, no se pudo evaluar— y el motivo cuando no cumple, en los mismos
terminos con los que despues se declara en la propuesta
([F-QICOR-R014](#F-QICOR-R014)). Incluye el cumplimiento de la consigna, que es lo
que quien corrige necesita saber para decidir si su candidato sirve
([F-QICOR-R005](#F-QICOR-R005)) — y consultarlo no lo encarece dos veces, porque el
veredicto queda registrado bajo la huella del contenido juzgado.

**Que NO decide esta regla.** No decide si el sistema vuelve a evaluar por su cuenta
el candidato entregado antes de proponerlo; eso es
[DOUBT-REVERIFICACION](#DOUBT-REVERIFICACION). Lo que fija es que exista una sola
definicion de cada criterio y una sola medicion, este quien este preguntando.

**Criterio de aceptacion**: (a) consultado un ejercicio candidato junto a su original
y su contexto, se obtiene un veredicto por cada criterio del catalogo, con motivo
cuando el veredicto es negativo; (b) el veredicto de longitud de esa consulta
coincide con el del analizador de longitud sobre el mismo ejercicio —mismo conteo,
mismo rango de nivel— y el de vocabulario con el del analisis lexico; (c) los
criterios incumplidos que declara la propuesta emitida son exactamente los que la
consulta devuelve para ese mismo ejercicio ([F-QICOR-R014](#F-QICOR-R014)).

**Error**: "El candidato del ejercicio '{quizId}' se juzgo con una medicion distinta de la que aplica el sistema"

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
generacion de ejercicios por ausencia de lema, que crea uno donde no habia nada. El
ejercicio que se corrige suele distinguirse de sus hermanos, tener una traduccion
fiel y ser resoluble: lo que le falla es la consigna. Cualquier correccion que
arregle la consigna y rompa alguna de esas cosas deja el curso **peor** que antes,
aunque el diagnostico que la origino desaparezca.

El piso, eso si, no es parejo en todos los ejes. Medido sobre el plan real, **211
de las 519 tareas de consigna (41%) caen sobre ejercicios que ademas tienen su
propia tarea de longitud**: en esos, el original **no** esta dentro del rango de su
nivel, y exigirle a la correccion que lo deje dentro no es conservar nada — es
pedirle que arregle de paso un defecto que no vino a arreglar. De ahi que el piso a
superar se defina eje por eje: no empeorar donde el original estaba bien
([F-QICOR-R004](#F-QICOR-R004)), y no exigir donde ya estaba mal
([F-QICOR-R011](#F-QICOR-R011)).

De ahi salen las tres reglas que forman el corazon de la feature: el cambio se
acota a lo señalado ([F-QICOR-R003](#F-QICOR-R003)), lo que ya estaba bien se
verifica que siga estandolo ([F-QICOR-R004](#F-QICOR-R004)), y lo que se corrigio
se verifica que efectivamente se haya corregido ([F-QICOR-R005](#F-QICOR-R005)).
Lo que **no** sale de ahi es descartar el trabajo: que el piso sea alto obliga a
que el operador vea que reprobo antes de aprobar, no a esconderle la correccion.
Cuando el mejor candidato cumple su consigna y reprueba alguna otra cosa, se
entrega con el reproche a la vista ([F-QICOR-R012](#F-QICOR-R012),
[F-QICOR-R014](#F-QICOR-R014)); "no se logro" queda para cuando no hay nada que
entregar ([F-QICOR-R006](#F-QICOR-R006)).

### La traduccion sigue a la oracion, y tiene un solo juez

La primera corrida real contra el curso dejo un numero que no se puede leer de
otra forma: **de 20 tareas, 1 correccion propuesta y 19 no logradas, y 17 de esas
19 reprobaron la edicion acotada** ([F-QICOR-R003](#F-QICOR-R003)). No era el
corrector fallando: era la regla pidiendo algo imposible.

El motivo es una asimetria de idioma. La evidencia que emite la validacion de
consigna habla del **ingles** —cita la respuesta aceptada y la marca del hueco— y
nunca del español. Si la traduccion solo puede cambiar cuando una violacion la
señala, y ninguna violacion la señala jamas, la traduccion es incorregible. Pero
hay toda una familia de defectos —la respuesta contradice la marca del hueco— en
la que corregir la respuesta **obliga** a corregir la traduccion: 13 de los 20
candidatos la cambiaron, y en los 13 el cambio era el correcto. Peor todavia: la
unica correccion que paso lo hizo porque **no** la toco, o sea que el sistema
aprobo la version incoherente y rechazo las coherentes.

La salida no es relajar la edicion acotada sino reconocer que la traduccion no es
una parte mas del ejercicio: es la oracion dicha en español, y por eso la sigue.
Quien juzga si esta bien es el criterio de traduccion fiel, que para eso existe
([F-QICOR-R004](#F-QICOR-R004), criterio 4); tenerlo y ademas prohibir tocar la
traduccion era una contradiccion, no una precaucion doble. La edicion acotada
conserva de ella una sola exigencia —que no cambie **sola**, sin que la oracion
haya cambiado—, y la traduccion pasa a juzgarse contra la oracion **propuesta**,
con lo que dejar la inconsistencia deja de ser el camino barato para aprobar.

### Una parte vacia no es contenido, y por eso no puede bloquear ni perderse

La segunda corrida real dejo otro numero que no admite lectura benigna: **192 de
las 519 tareas (37%) son estructuralmente incorregibles**, y en el curso entero
**4015 de 11487 ejercicios (35%)** estan en la misma situacion. No es que el
corrector falle: ningun candidato, por correcto que sea, puede pasar.

El mecanismo es este. Un ejercicio puede tener una parte de texto **vacia**, que
no aporta un solo caracter. El ejercicio viaja como texto para poder corregirse, y
una parte que no aporta caracteres no deja nada en esa cadena: al volver a leerla
no hay de donde recuperarla. El candidato vuelve entonces con una parte menos, y
la edicion acotada lo rechaza por haber "cambiado la cantidad de partes" —aunque
el texto que cambio sea exactamente el que la violacion señalaba—.

La salida no es tolerar mas cambio sino reconocer que ahi no hay contenido.
[F-QICOR-R003](#F-QICOR-R003) siempre se enuncio sobre **caracteres**; una parte
vacia no tiene ninguno, asi que contar partes era un criterio mas estricto que el
que la regla dice. Tres mediciones lo confirman: la parte vacia esta **siempre en
un borde** (470 al principio, 3545 al final, ninguna entre dos partes con texto),
no separa nada, y el curso ya guarda **884 ejercicios** en la forma exacta que
produce la correccion —sin la parte vacia— sin que eso moleste a nadie. Los dos
ejemplos conviven a un knowledge de distancia: `(vacia) + [Do I want] + "(I /
need) a pen?"` y `[Do I need] + "(I / need) a mobile?"`.

Ahora bien, que no sea contenido corta para los dos lados. Si ninguna violacion
pidio quitarla, nada autoriza a quitarla, y la correccion **se escribe sobre el
curso** al aprobarse: dejar que se pierda seria reestructurar hasta 4015
ejercicios de a uno, en silencio, por la puerta de una correccion de consigna. Es
la misma forma de daño que dio origen a
[FEAT-RPRES](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md).
De ahi [F-QICOR-R010](#F-QICOR-R010), con sus dos mitades: no bloquea, y no se
pierde.

### La longitud dejo de decidir

La tercera corrida real dejo el numero mas grande de los tres. De 519 tareas de
consigna, 317 terminaron corregidas (61%) y 162 no; y entre las que no, el motivo
mas frecuente fue **la longitud: 78 ejercicios**, por encima de la edicion no
acotada (71) y muy por encima de todo lo demas (10 entre resolubilidad, vocabulario,
consigna y distincion). Eran 78 ejercicios que siguen defectuosos hoy porque su
correccion media unos tokens de mas.

Hay un motivo estructural para que la longitud pegue tanto justo aca: corregir un
desajuste entre la marca del hueco y la respuesta suele **alargar** la oracion
(`is he` → `are they walking`), de modo que el criterio castigaba de forma
sistematica al tipo de correccion mas comun de esta feature.

Pero el dato que decide es otro. De esos 78, **75 arrastraban ya su propia tarea de
longitud**: el ejercicio original tampoco estaba dentro de rango. Solo 3 estaban
dentro y salieron por efecto de la correccion. El criterio no estaba evitando que
algo bueno se rompiera; estaba exigiendo que la correccion de consigna arreglara
tambien un defecto viejo que ninguna violacion señalaba — lo mismo que la propia
[F-QICOR-R004](#F-QICOR-R004) prohibe para el vocabulario cuando dice que "los flags
preexistentes no bloquean". La longitud era el unico criterio absoluto dentro de una
regla que se llama "no empeorar".

La decision es entonces la que ordena las prioridades como corresponde: **es mas
importante que el ejercicio este bien escrito que si entra en el rango**; si la
alternativa a uno fuera de rango es dejar en el curso uno que incumple su consigna,
se elige el primero. La longitud sale del conjunto de criterios que pueden rechazar
una correccion, se sigue midiendo, y **se declara** — en la propuesta y en el
recuento de la corrida ([F-QICOR-R011](#F-QICOR-R011)). No se elige entre corregir y
medir: se elige que la medicion informe en vez de vetar.

Nada queda sin vigilancia. Lo que impide que la oracion se infle nunca fue este
criterio sino la edicion acotada ([F-QICOR-R003](#F-QICOR-R003)), que solo deja
tocar lo que la violacion señala. Y si el ejercicio corregido queda fuera de rango,
el analizador de longitud lo vuelve a marcar en la auditoria siguiente y le abre su
propia tarea, con su propio corrector: cada defecto en su carril. Bloquear aca era
conservar un defecto que **solo esta correccion sabe arreglar** para evitar otro que
el sistema **ya sabe arreglar por otra via** — y es, ademas, la postura que la
correccion de vocabulario ya habia adoptado explicitamente desde su primera entrega
([FEAT-RCLA](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)).

### El lazo estaba del lado equivocado

Las tres corridas reales fueron levantando, una por una, las razones por las que
una correccion correcta no llegaba a proponerse: la traduccion, la parte vacia, la
longitud. Lo que quedo a la vista al final es que ninguna de las tres era el
problema de fondo. El problema de fondo es **que se hacia con un candidato que
reprobaba**: se lo tiraba, se pedia otro desde cero, y al tercero se abandonaba la
tarea. De 162 tareas sin corregir, **159 tenian tres candidatos producidos,
juzgados y tirados** —y de esas, **157 terminarian en propuesta** con solo dejar de
tirarlos—.

El lazo lo conducia el sistema desde afuera y por eso solo podia hacer eso. Ve un
candidato por vez; para quedarse con "el mejor de los tres" hay que recordar los
tres, y quien los tiene es quien los escribio. Ademas, pedir otro candidato no es
lo mismo que mejorar el propio: el reintento desde afuera vuelve a empezar, con lo
que el resultado se juega entero en cada tirada. La medicion es concluyente — las
125 correcciones que salieron bien salieron **todas al primer intento**, y las
**318 consultas de reintento** que consumieron las 159 tareas perdidas rescataron
**cero**.

La decision es entonces tener **una sola forma de hacer las cosas**. La generacion
de ejercicios por ausencia de lema ya conduce su propio lazo, evalua sus
candidatos, conserva el mejor que vio y lo entrega al agotar los intentos en lugar
de fracasar
([F-LASAG-R009](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R009));
no hay ninguna razon de negocio para que la correccion de consigna resuelva lo
mismo de otra manera. De ahi [F-QICOR-R012](#F-QICOR-R012): una sola solicitud por
tarea, el lazo del lado de quien corrige, y el mejor candidato entregado al
agotarlo.

Eso obliga a decir tres cosas mas, y las tres estan escritas:

- **Cual es el mejor** ([F-QICOR-R013](#F-QICOR-R013)). No una impresion: un orden
  fijo, que ordena los criterios por cuanto daña al alumno lo que reprueban y deja
  la longitud al final. Es la decision del usuario llevada hasta el desempate —*un
  ejercicio bien escrito fuera de rango vale mas que uno mal escrito dentro*—.
- **Que se dice de lo entregado** ([F-QICOR-R014](#F-QICOR-R014)). Lo que se retiro
  es el veto, no la informacion: la propuesta nombra lo que el candidato no cumplio
  y la corrida cuenta cuantas llegaron asi. El operador pasa a ser el ultimo filtro,
  y un filtro necesita ver.
- **Con que se juzga** ([F-QICOR-R015](#F-QICOR-R015)). Si quien corrige evalua sus
  candidatos con una regla de medir propia, elige a su mejor candidato con un
  criterio y el sistema lo juzga con otro. Los criterios pasan a poder consultarse
  —la misma medicion, este quien este preguntando—, que es como el sistema ya
  resuelve la consulta lexica de una oracion
  ([FEAT-CLEX](../2026-07-22.01_consulta-lexica-de-oracion-por-cli/REQUIREMENT.md)).

Queda un solo criterio absoluto, y no pertenece al catalogo de "no empeorar":
**cumplir la consigna** ([F-QICOR-R005](#F-QICOR-R005)). Un corregido fuera de
rango abre otro diagnostico, con su propio carril; uno que sigue incumpliendo
reabre el mismo que lo origino y le hace perder al operador una revision para
nada. Por eso "no se logro una correccion" sobrevive, pero acotado a lo que el
nombre dice: no habia nada que entregar ([F-QICOR-R006](#F-QICOR-R006)). Sobre la
corrida medida, ese desenlace pasa de 159 tareas a **2**.

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

Que el lazo de intentos pase a conducirlo quien corrige
([F-QICOR-R012](#F-QICOR-R012)) no encarece ni abarata la corrida por si mismo —una
tarea dificil sigue costando hasta tres intentos—, pero cambia lo que se compra con
ese gasto: hasta ahora tres intentos fallidos costaban tres consultas y no dejaban
nada; ahora dejan la mejor de las tres correcciones. El gasto queda acotado por dos
perillas, el tope de tareas de la corrida y el maximo de intentos por tarea, y
ninguna tarea puede excederlas.

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
  - La posibilidad de consultar esos criterios sobre un ejercicio candidato, con la
    misma medicion, sin tener que pedir una correccion.
  - El lazo de intentos del lado de quien corrige, con su maximo configurable, y la
    entrega del mejor candidato visto al agotarlo.
  - La medicion de la longitud del ejercicio corregido y su declaracion cuando
    queda fuera del rango del nivel.
  - La declaracion, en la propuesta y en el recuento de la corrida, de los criterios
    que el ejercicio entregado no cumplio.
  - El desenlace explicito cuando ningun candidato resulta entregable.
  - La decision (aprobar / rechazar) por el flujo existente y la escritura del
    ejercicio corregido en el curso, incluida la conservacion de las partes vacias
    del enunciado que el original tenia.
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
  - **Corregir la longitud del ejercicio.** Un ejercicio corregido que queda fuera
    del rango de su nivel se propone igual y se declara como tal
    ([F-QICOR-R011](#F-QICOR-R011)); devolverlo al rango es trabajo de la tarea de
    longitud que el analizador le abre en la auditoria siguiente, que tiene su
    propio contexto de correccion
    ([FEAT-RCSL](../2026-04-05.02_refiner-correction-context-slen/REQUIREMENT.md)).

## User Journeys

### Journey[F-QICOR-J001] - Corregir un ejercicio que incumple su consigna, y decidir la correccion
**Validation**: AUTO_VALIDATED

Cubre el flujo principal de punta a punta: preparar la correccion con las
violaciones, revalidar el diagnostico, pedir la correccion una sola vez, recibir el
mejor candidato con su evaluacion, y decidir. Cubre R001, R002, R003, R004, R005,
R006, R007, R009, R010, R011, R012, R013 y R014.

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
        action: "El sistema pide la correccion una sola vez; quien corrige produce candidatos dirigidos por las violaciones, los juzga con los criterios del sistema, decide por su cuenta si reintenta hasta el maximo configurado, y entrega el mejor candidato que vio"
        gate: [F-QICOR-R004, F-QICOR-R012, F-QICOR-R013, F-QICOR-R015]
        outcomes:
          - when: "El candidato entregado cumple su consigna y constituye un cambio real sobre el original, aunque haya quedado fuera del rango de longitud de su nivel o repruebe algun otro criterio del catalogo"
            then: verificar_consigna
          - when: "Agotados los intentos, ningun candidato cumplio su consigna ni constituyo un cambio real: no hay nada entregable"
            then: sin_correccion

      - id: verificar_consigna
        action: "El sistema somete el ejercicio entregado a la misma validacion de consigna que detecto el incumplimiento y obtiene veredicto de cumplimiento"
        gate: [F-QICOR-R005]
        then: armar_propuesta

      - id: armar_propuesta
        action: "Queda una propuesta revisable cuyo estado anterior es el ejercicio actual del curso y cuyo estado propuesto es el ejercicio corregido, con las partes que ninguna violacion señala identicas al original, la traduccion acompañando a la oracion corregida, sin que una parte vacia del enunciado que el candidato no trajo haya impedido proponerla, declarando la longitud medida y el rango del nivel cuando quedo fuera de rango, y nombrando los criterios del catalogo que el ejercicio entregado no cumplio"
        gate: [F-QICOR-R001, F-QICOR-R003, F-QICOR-R010, F-QICOR-R011, F-QICOR-R014]
        then: decidir

      - id: decidir
        action: "La propuesta se decide con el mismo vocabulario que las demas correcciones"
        outcomes:
          - when: "La propuesta se aprueba"
            then: aplicar_correccion
          - when: "El operador rechaza la propuesta"
            then: rechazo

      - id: aplicar_correccion
        action: "Releido el curso, el ejercicio queda con el contenido corregido y conserva las partes vacias del enunciado que tenia; la tarea figura resuelta y ningun otro ejercicio del knowledge cambia"
        gate: [F-QICOR-R007, F-QICOR-R010]
        result: success

      - id: rechazo
        action: "El curso no se modifica: el ejercicio conserva su contenido original y la tarea vuelve a quedar disponible"
        gate: [F-QICOR-R007]
        result: failure

      - id: sin_correccion
        action: "El sistema informa que ningun candidato resulto entregable y nombra los criterios que reprobaron, sin nombrar nunca la longitud; no queda ninguna propuesta, el ejercicio del curso no cambia y la tarea sigue disponible"
        gate: [F-QICOR-R006]
        result: failure
```

### Journey[F-QICOR-J002] - Corregir un tramo acotado de las tareas de consigna del plan
**Validation**: AUTO_VALIDATED

Cubre el avance por tramos, la declaracion de lo que quedo sin intentar y la
separacion de los desenlaces en el reporte de la corrida. Cubre R008, R009, el
recuento de propuestas fuera de rango de R011 y el de propuestas con criterios sin
cumplir de R014.

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
        action: "La corrida intenta todas las tareas del plan de este tipo, no declara ninguna sin intentar, su reporte distingue en recuentos separados las correcciones propuestas, las no logradas y los diagnosticos caidos, y declara ademas cuantas de las correcciones propuestas quedaron fuera del rango de longitud de su nivel y cuantas llegaron con criterios del catalogo sin cumplir"
        gate: [F-QICOR-R008, F-QICOR-R009, F-QICOR-R011, F-QICOR-R014]
        result: success
```

## Open Questions

<a id="DOUBT-SIN-CORRECCION"></a>
### Doubt[DOUBT-SIN-CORRECCION] - Que se hace con el mejor candidato cuando ninguno alcanza?
**Status**: RESOLVED (2026-08-11)

La primera version de [F-QICOR-R006](#F-QICOR-R006) exigia que un candidato que
reprueba criterios no se propusiera. Habia que decidir si el
[mejor candidato](glossary:Mejor candidato) alcanzado se conserva de alguna forma o
se descarta.

- [ ] Opcion A: **No se propone nada.** Se informa el fallo con los criterios
  reprobados y la tarea queda disponible. Simple y sin riesgo de que el operador
  apruebe una regresion; a cambio, el trabajo del intento se pierde por completo,
  incluso cuando el candidato reprobaba por poco.
- [x] Opcion B: **Se propone con los criterios reprobados a la vista**, y el
  operador decide igual. No se pierde el trabajo y el operador puede juzgar un caso
  limite; a cambio, aparece en el plan una propuesta que el sistema mismo señala
  como imperfecta, y basta una aprobacion distraida para meter una regresion al
  curso.
- [ ] Opcion C: **Se guarda como referencia sin ser una propuesta aprobable**:
  visible para quien quiera corregir a mano, imposible de aprobar tal cual.
  Intermedia; agrega una categoria nueva al vocabulario de revision, que hoy solo
  conoce pendiente / aprobada / rechazada.

**Answer**: **Opcion B**, decidida explicitamente, y con una precision que la
Opcion A hacia invisible: lo entregado **no se marca como no apto**, se entrega
como cualquier otra propuesta y lo que viaja con ella es el detalle de que criterios
no cumplio ([F-QICOR-R014](#F-QICOR-R014)). El dato que la decidio es que la Opcion
A no protegia lo que parecia proteger: sobre el plan real, **157 de las 162 tareas
sin corregir tenian un candidato producido y descartado**, y en 78 de ellas el unico
reproche era la longitud, que ya no decide nada. Descartar no dejaba el curso mejor:
lo dejaba igual, con el defecto adentro. La Opcion C se descarta por lo mismo que se
descartaba antes —inventa una cuarta categoria de revision— y porque, entregando con
el reproche a la vista, no agrega nada que el operador no pueda ya hacer rechazando.
El riesgo de la aprobacion distraida es real y se acepta a cambio de las 157
correcciones; lo que lo hace tolerable es que el reproche sea visible antes de
aprobar y que la consigna siga siendo absoluta
([F-QICOR-R005](#F-QICOR-R005)) — nunca se propone algo que no corrige el problema.

<a id="DOUBT-REINTENTOS"></a>
### Doubt[DOUBT-REINTENTOS] - Cuantos intentos consume una tarea antes de entregar lo mejor que tenga?
**Status**: OPEN

Desde [F-QICOR-R012](#F-QICOR-R012) el lazo lo conduce quien corrige y los intentos
se consumen dentro de la unica solicitud de correccion, pero el **maximo** lo sigue
fijando la corrida — sin un tope declarado por fuera, una tarea dificil intentaria
indefinidamente. Falta fijar el numero. Cada intento cuesta otra consulta pagada, y
el tope de la corrida ([F-QICOR-R008](#F-QICOR-R008)) cuenta tareas, no intentos.

- [x] Opcion A: **Hasta 3 intentos por tarea, configurable**, y cada intento parte
  de que criterios reprobo el anterior y por que. Es el mismo numero y el mismo
  mecanismo que ya usa la generacion de ejercicios
  ([F-LASAG-R008](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R008),
  [F-LASAG-R010](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R010)).
- [ ] Opcion B: **Un solo intento.** El mas barato y el mas predecible en costo, y
  hay un dato que lo respalda mas de lo que parece: en la corrida medida las 125
  correcciones logradas salieron **todas al primer intento** y los 318 reintentos no
  rescataron ninguna. A cambio, con el mejor candidato ya no hay nada que perder por
  intentar de nuevo: un segundo intento solo puede mejorar lo que se entrega.
- [ ] Opcion C: **Sin tope de intentos hasta lograrlo.** Garantiza el mejor
  resultado posible por tarea y hace imposible acotar el costo de una corrida, que
  es justo lo que [F-QICOR-R008](#F-QICOR-R008) busca.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] por coherencia con
la generacion de ejercicios, que ya reintenta con el motivo del fallo a la vista y
con tope 3. La Opcion B merece medirse ahora que el lazo cambio de dueño: el dato de
que ningun reintento rescato una tarea se produjo con un lazo que **volvia a pedir
desde cero**; un reintento que parte del candidato anterior no es el mismo
experimento. Si tras las proximas tandas el segundo y el tercer intento siguen sin
mejorar al primero en ningun caso, la Opcion A esta pagando el triple por nada.

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

<a id="DOUBT-TRADUCCION-COLATERAL"></a>
### Doubt[DOUBT-TRADUCCION-COLATERAL] - Cuando la oracion cambia, ¿la traduccion puede reescribirse entera?
**Status**: OPEN

Desde que [F-QICOR-R003](#F-QICOR-R003) dejo de opinar sobre la traduccion,
cambiada la oracion **cualquier** traduccion fiel de la oracion propuesta pasa
—tambien una reescrita de punta a punta cuando bastaba con cambiar dos palabras—.
La correccion sigue siendo valida; lo que crece es lo que el operador tiene que
leer para aprobarla.

- [x] Opcion A: **Alcanza con la fidelidad.** Una traduccion que dice lo mismo que
  su oracion es correcta, se parezca o no a la anterior. Es la unica formulacion
  que se verifica mirando el par propuesto, sin umbrales; a cambio, admite churn
  que nadie pidio en el lado español.
- [ ] Opcion B: **La traduccion tambien se edita al minimo**, en espejo de lo que
  se le exige a la oracion. Mantiene la propuesta chica de los dos lados; a cambio
  vuelve a meter la pregunta que [F-QICOR-R003](#F-QICOR-R003) evito a proposito
  —cuanto cambio es demasiado— sobre un texto donde una misma correccion admite
  redacciones muy distintas, y arriesga bloquear traducciones correctas por
  parecerse poco a la anterior.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION]: es la que no
puede volver a bloquear correcciones legitimas, que es el defecto del que se sale.
Conviene revisarla con datos de las proximas tandas: si aparecen propuestas donde
la traduccion se reescribio mucho mas de lo que el cambio en ingles justificaba,
la Opcion B vale la discusion.

<a id="DOUBT-LONGITUD-NO-BLOQUEANTE"></a>
### Doubt[DOUBT-LONGITUD-NO-BLOQUEANTE] - Que lugar ocupa la longitud entre los criterios que deciden?
**Status**: RESOLVED (2026-08-11)

La longitud era el motivo mas frecuente de correccion no lograda: 78 tareas, por
encima de la edicion no acotada (71). Habia tres lecturas defendibles de que hacer
con ella.

- [x] Opcion A: **Deja de decidir.** Sale del conjunto de criterios que pueden
  rechazar una correccion; se sigue midiendo y se informa, en la propuesta y en el
  recuento de la corrida. Destraba las 78 y ordena las prioridades como se decidio
  —bien escrito antes que en rango—; a cambio, admite que entren al curso
  correcciones fuera de rango, que la auditoria siguiente vuelve a marcar.
- [ ] Opcion B: **Pasa a ser preferencia.** Entre dos candidatos que pasan todo lo
  demas gana el que entra en rango; si el unico aceptable queda fuera, se acepta
  igual. Suena a lo mejor de los dos mundos y no lo es: los 78 ya consumieron 3
  intentos cada uno con el motivo del fallo a la vista y en 78 de 78 el reintento
  informado no produjo un candidato en rango, asi que la preferencia gasta de nuevo
  lo que ya se gasto; y como el catalogo no puede ver "bien escrito", preferir al
  que entra en rango es preferir sistematicamente a la variante comprimida — la
  inversion exacta de la decision.
- [ ] Opcion C: **No-regresion.** Bloquea solo si el candidato empeora la longitud
  respecto del original. Es la lectura mas fiel al titulo de
  [F-QICOR-R004](#F-QICOR-R004) y destrabaria 75 de las 78; a cambio deja viva la
  veto justo en el caso sobre el que ya se decidio —un ejercicio que estaba dentro
  y sale—, sigue bloqueando a los que quedan "mas afuera" (el subconjunto mas
  probable, porque estas correcciones alargan), y obliga a fijar un umbral de
  "peor" en tokens que ninguna violacion pidio.

**Answer**: **Opcion A**, decidida explicitamente: es mas importante que el
ejercicio este bien escrito que si entra o no en el rango; si el problema es la
longitud y la alternativa es una quiz mal hecha, se elige la que esta fuera de
rango. Queda enunciada en [F-QICOR-R011](#F-QICOR-R011) junto con lo que la vuelve
aceptable: la medicion no se abandona, la propuesta declara que quedo fuera y la
corrida cuenta cuantas quedaron asi. Conviene revisar con ese recuento: si la
mayoria de las propuestas empezara a quedar fuera de rango, el que hay que mirar es
el corrector, no el criterio.

<a id="DOUBT-LONGITUD-IDA-Y-VUELTA"></a>
### Doubt[DOUBT-LONGITUD-IDA-Y-VUELTA] - Puede la correccion de longitud reabrir el incumplimiento de consigna?
**Status**: OPEN

Aceptada una correccion de consigna fuera de rango, la auditoria siguiente le abre
al ejercicio una tarea de longitud, cuyo corrector acorta la oracion. Nada garantiza
hoy que ese acortamiento respete la consigna: si al acortar vuelve a desajustar la
respuesta con la marca del hueco, el ejercicio reaparece como incumplidor de
consigna y las dos correcciones podrian empujarse mutuamente sin converger.

- [ ] Opcion A: **Se acepta el riesgo y se observa.** Es el comportamiento ya
  vigente entre la correccion de vocabulario y la de longitud
  ([FEAT-RCLA](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)), y
  nunca se reporto un ida y vuelta; medirlo es barato: un mismo ejercicio que vuelve
  a aparecer en tareas de los dos tipos en auditorias sucesivas.
- [ ] Opcion B: **La correccion de longitud pasa a verificar la consigna** antes de
  proponer, como esta correccion verifica la longitud. Cierra el lazo; encarece cada
  correccion de longitud con una consulta pagada, sobre 3669 tareas.
- [ ] Opcion C: **Se prioriza**: un ejercicio con tarea de consigna y tarea de
  longitud se corrige primero de consigna y su tarea de longitud espera a la
  auditoria siguiente. Barato y ordena el trabajo; no impide el ida y vuelta, solo
  lo espacia.

**Answer**: Pendiente. Ninguna de las tres cambia el enunciado de
[F-QICOR-R011](#F-QICOR-R011) ni de ninguna otra regla de este documento: la duda es
sobre **otra** correccion, no sobre esta. Conviene decidirla con el dato que hoy no
existe —cuantos ejercicios reaparecen alternando los dos diagnosticos— y no antes,
porque los 211 ejercicios que ya tienen las dos tareas hacen la medicion inmediata.

<a id="DOUBT-PARTE-VACIA-AL-GUARDAR"></a>
### Doubt[DOUBT-PARTE-VACIA-AL-GUARDAR] - Al guardar la correccion, ¿el ejercicio conserva la parte vacia o se normaliza?
**Status**: OPEN

[F-QICOR-R010](#F-QICOR-R010) saca la parte vacia de la comparacion de la edicion
acotada: sobre eso no hay margen, porque sin ese cambio el 35% del curso es
incorregible. Lo que si admite discusion es la otra mitad — **que queda escrito**
cuando se aprueba un candidato de 2 partes sobre un original de 3.

- [x] Opcion A: **Se conserva.** El ejercicio guardado vuelve a tener su parte
  vacia, en su misma posicion. La correccion cambia solo lo que se propuso
  corregir y no cuesta nada —es una parte sin texto—; a cambio, el curso sigue
  arrastrando una estructura que no aporta nada y que alguien va a volver a
  encontrarse.
- [ ] Opcion B: **Se pierde.** Se aprovecha que el curso ya convive con las dos
  formas (884 ejercicios ya viven sin la parte vacia) y de paso se limpia. A
  cambio, reestructura hasta 4015 ejercicios de a uno, sin que ninguna regla lo
  haya pedido y sin que el operador lo vea: aprueba una correccion de consigna y
  ademas normaliza. Es la forma de daño que dio origen a FEAT-RPRES.
- [ ] Opcion C: **Se conserva aca y la normalizacion se hace aparte**, como una
  limpieza deliberada del curso con su propia revision y su propia medicion. Deja
  la decision de normalizar donde se puede ver y contar de una vez, en lugar de
  que gotee por la puerta de las correcciones; a cambio, es trabajo aparte.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] porque es la
unica en la que equivocarse no cuesta nada: si mañana se decide que la parte vacia
sobra, borrarla sigue siendo posible; si se pierde ahora y resulta que importaba,
no hay de donde recuperarla en un tercio del curso. La Opcion C es la continuacion
natural y **no requiere cambiar ninguna regla** de este documento; conviene
decidirla recien cuando exista una razon positiva para normalizar, que hoy no hay.

<a id="DOUBT-CRITERIO-ABSOLUTO"></a>
### Doubt[DOUBT-CRITERIO-ABSOLUTO] - Que criterio, si alguno, sigue pudiendo impedir una entrega?
**Status**: OPEN

[F-QICOR-R012](#F-QICOR-R012) entrega el mejor candidato aunque repruebe criterios,
y [F-QICOR-R005](#F-QICOR-R005) queda como el unico absoluto. Es defendible que
algun otro tambien lo sea: hay criterios cuyo incumplimiento **rompe el ejercicio
para el alumno**, no solo lo empeora.

- [x] Opcion A: **Solo el cumplimiento de la consigna.** Lo demas se entrega
  declarado y el operador decide. Es la lectura fiel de la decision tomada, y la
  medicion la respalda: de los 159 candidatos descartados, 149 lo fueron por
  longitud o edicion acotada —ninguno de los dos daña al alumno— y solo 5 por
  resolubilidad o traduccion. A cambio, un ejercicio irresoluble puede llegar a la
  bandeja del operador.
- [ ] Opcion B: **Ninguno.** Se entrega siempre el mejor candidato, tambien cuando
  sigue incumpliendo la consigna. Maximiza lo entregado; a cambio propone cambios
  que no resuelven el problema que los origino y devuelve la misma tarea al plan en
  la auditoria siguiente — el desenlace que [F-QICOR-R005](#F-QICOR-R005) existe
  para impedir.
- [ ] Opcion C: **La consigna, la resolubilidad y la traduccion fiel.** Los tres
  cuyo incumplimiento le llega al alumno como un ejercicio inservible o
  contradictorio. Protege mejor el curso; a cambio devuelve el descarte —y su costo—
  para un puñado de casos, y obliga a defender por que la distincion o el
  vocabulario no estan en esa lista.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] por fidelidad a la
decision tomada y porque el volumen que la Opcion C recuperaria es minimo —5 tareas
sobre 305— frente al costo de reintroducir el descarte. Conviene revisarla con el
primer recuento de [F-QICOR-R014](#F-QICOR-R014): si aparecen propuestas entregadas
que reprueban resolubilidad o traduccion fiel con alguna frecuencia, la Opcion C pasa
a valer la pena, y es un cambio chico —mover dos criterios de la lista que ordena a
la lista que impide—.

<a id="DOUBT-ORDEN-DESEMPATE"></a>
### Doubt[DOUBT-ORDEN-DESEMPATE] - El orden de precedencia entre criterios es el correcto?
**Status**: OPEN

[F-QICOR-R013](#F-QICOR-R013) desempata por un orden fijo —resolubilidad,
traduccion fiel, vocabulario, distincion, edicion acotada, longitud— derivado de
cuanto daña al alumno cada incumplimiento. El orden no lo pidio ninguna medicion:
es una lectura de la decision de fondo (*bien escrito antes que en rango*).

- [x] Opcion A: **El orden por daño al alumno**, con la edicion acotada anteultima
  y la longitud ultima. Es coherente con que la edicion acotada es el unico criterio
  cuyo costo lo paga el operador y no el alumno.
- [ ] Opcion B: **La edicion acotada sube**, por encima de distincion y
  vocabulario: una propuesta con churn es la mas cara de revisar y la mas probable
  de rechazar, asi que preferir la acotada rinde mas revisiones aprobadas. Es un
  argumento de flujo de trabajo, no de calidad del curso.
- [ ] Opcion C: **Sin precedencia: solo cantidad, y a igualdad el primero visto.**
  Lo mas simple de verificar y de explicar; a cambio, entre dos candidatos que
  empatan en cantidad elige por orden de aparicion, que no dice nada sobre cual es
  mejor.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION]. El desempate por
precedencia solo se consulta cuando dos candidatos cumplen la **misma cantidad** de
criterios, situacion que hoy no esta medida —con un solo candidato entregado por
tarea, el orden casi nunca se ejerce—. Conviene decidirlo con el dato de cuantas
veces el desempate llega a usarse; si resulta ser raro, la Opcion C es una
simplificacion legitima.

<a id="DOUBT-REVERIFICACION"></a>
### Doubt[DOUBT-REVERIFICACION] - El sistema vuelve a evaluar el candidato entregado, o toma la evaluacion que llego con el?
**Status**: OPEN

Quien corrige juzga sus candidatos con los criterios del sistema
([F-QICOR-R015](#F-QICOR-R015)), asi que la evaluacion que llega con el candidato
entregado se produjo con la regla de medir correcta. Falta decidir si el sistema la
toma como esta o la rehace antes de declararla en la propuesta
([F-QICOR-R014](#F-QICOR-R014)).

- [x] Opcion A: **Se rehacen los criterios que no cuestan una consulta** —longitud,
  vocabulario, edicion acotada, distincion— y se toma de la evaluacion recibida el
  veredicto de los que si la cuestan —traduccion fiel, resolubilidad—. El
  cumplimiento de la consigna lo verifica siempre el sistema
  ([F-QICOR-R005](#F-QICOR-R005)) y no se paga dos veces gracias a la huella del
  contenido. Lo declarado es entonces medicion propia donde es gratis serlo.
- [ ] Opcion B: **Se toma entera la evaluacion recibida.** Lo mas barato y lo mas
  simple; a cambio, lo que la propuesta declara es palabra de quien corrige, y una
  discrepancia entre esa declaracion y lo que mediria el sistema no la detecta nadie.
- [ ] Opcion C: **Se rehace toda la evaluacion**, incluidos los criterios de juicio.
  La declaracion es integramente del sistema; a cambio agrega dos consultas pagadas
  por tarea, sobre cientos de tareas, para confirmar un veredicto emitido con el
  mismo criterio.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] porque reparte por
el unico eje que importa —lo que es gratis verificar se verifica— y deja la
declaracion de [F-QICOR-R014](#F-QICOR-R014) anclada en la medicion del sistema para
los cuatro criterios deterministas, que son ademas los que la auditoria siguiente va
a volver a medir. Ninguna de las tres opciones cambia el enunciado de una regla de
este documento: las tres declaran lo mismo, y lo que cambia es de quien es la
medicion que respalda la declaracion.

## References

- **FEAT-QSENT** (`2026-04-22.01_quiz-sentence-dsl`) — Formaliza la forma de texto
  con la que el ejercicio viaja para poder corregirse. Es la que explica por que
  una parte vacia se pierde en el camino de ida y vuelta: no deja ningun caracter
  en la cadena, asi que no hay de donde recuperarla. Citada por
  [F-QICOR-R010](#F-QICOR-R010).
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
  minima y del reintento informado por el motivo del fallo. Es tambien **el modelo
  que [F-QICOR-R012](#F-QICOR-R012) adopta**: alli el lazo de intentos lo conduce
  quien genera, se conserva el mejor candidato visto y se entrega al agotar los
  intentos en lugar de fracasar. Hasta el 2026-08-11 esta feature se apartaba
  deliberadamente de ese mejor-esfuerzo —alli el ejercicio emitido es nuevo y
  compite contra la nada, aca compite contra uno que ya funcionaba—; el argumento
  sobrevive como razon para **declarar** lo que el candidato no cumple
  ([F-QICOR-R014](#F-QICOR-R014)), no para descartarlo. Citada por
  [F-QICOR-R003](#F-QICOR-R003), [F-QICOR-R004](#F-QICOR-R004),
  [F-QICOR-R006](#F-QICOR-R006), [F-QICOR-R012](#F-QICOR-R012),
  [F-QICOR-R013](#F-QICOR-R013), [DOUBT-REINTENTOS](#DOUBT-REINTENTOS) y el
  Contexto.
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
  intacto. Esta feature se apoya en esa garantia en lugar de repetirla. Aporta
  ademas el caso simetrico del que [F-QICOR-R010](#F-QICOR-R010) es el reverso:
  alli se legisla que pasa cuando la correccion produce **mas** partes que el
  original, aca que pasa cuando produce **menos**. Citada por
  [F-QICOR-R007](#F-QICOR-R007) y [F-QICOR-R010](#F-QICOR-R010).
- **FEAT-CLEX** (`2026-07-22.01_consulta-lexica-de-oracion-por-cli`) — Provee la
  verificacion lexica con el mismo criterio de la auditoria, que es como se
  comprueba el criterio 2 de no regresion sin aproximaciones. Es ademas el
  precedente de forma de [F-QICOR-R015](#F-QICOR-R015): una medicion de la auditoria
  que se vuelve consultable por si misma, para que quien la necesita no la
  reimplemente. Citada por [F-QICOR-R004](#F-QICOR-R004),
  [F-QICOR-R015](#F-QICOR-R015) y el Contexto.
- **FEAT-SLEN** (`2026-03-18.01_sentence-length`) — Define el rango de longitud
  objetivo por nivel, su medicion y su margen de tolerancia. Es la medicion con la
  que se mide el criterio 1, y es tambien el analizador que vuelve a marcar por su
  cuenta al ejercicio corregido que quedo fuera de rango — la red que hace aceptable
  que ese criterio ya no rechace. Citada por [F-QICOR-R004](#F-QICOR-R004) y
  [F-QICOR-R011](#F-QICOR-R011).
- **FEAT-RCLA** (`2026-04-13.01_refiner-correction-context-labs`) — Precedente
  directo de la postura de [F-QICOR-R011](#F-QICOR-R011): la correccion de
  vocabulario declaro desde su primera entrega que no valida la longitud del
  resultado y que un resultado fuera de rango reaparece como tarea de longitud en la
  auditoria siguiente. La correccion de consigna era la unica del sistema donde la
  longitud rechazaba. Citada por [F-QICOR-R011](#F-QICOR-R011) y el Contexto.
- **FEAT-RCSL** (`2026-04-05.02_refiner-correction-context-slen`) — Provee el
  contexto de correccion de las tareas de longitud, que es el carril al que esta
  feature delega devolver al rango a un ejercicio corregido que quedo afuera. Citada
  por el Alcance.
