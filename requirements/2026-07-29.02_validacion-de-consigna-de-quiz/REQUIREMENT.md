---
feature:
  id: FEAT-QINST
  code: F-QINST
  name: Analisis de cumplimiento de la consigna del quiz
  priority: critical
---

# Analisis de cumplimiento de la consigna del quiz

## TL;DR

**Que**: Un analisis nuevo de la auditoria puntua cada ejercicio segun si cumple
literalmente la consigna que se le da al alumno, apoyandose en un juez de
consigna ya existente y reutilizando los veredictos ya emitidos.

**Por que**: Hoy la auditoria mide longitud y vocabulario, pero nada verifica que
el ejercicio haga lo que su consigna promete; hay ejercicios irresolubles o que
contradicen su propia instruccion y ningun analisis los detecta.

## Reglas de Negocio

<a id="F-QINST-R001"></a>
### Rule[F-QINST-R001] - El analisis de consigna corre en toda auditoria, salvo exclusion explicita
**Severity**: critical | **Validation**: VALIDATED

> El analisis de cumplimiento de consigna forma parte de la auditoria como
> cualquier otro analisis: corre siempre, sin que el usuario tenga que pedirlo.
> Solo deja de correr si el usuario lo **excluye explicitamente**
> ([F-QINST-R011](#F-QINST-R011)) o si pide una auditoria acotada a otros
> analisis.

<details><summary>Detalle</summary>

Correr por omision es viable justamente porque el costo esta acotado por otras
dos reglas: los veredictos ya emitidos se reutilizan sin costo
([F-QINST-R008](#F-QINST-R008)) y las consultas nuevas tienen tope por corrida
([F-QINST-R006](#F-QINST-R006)). Una auditoria sobre un curso completamente
evaluado no cuesta ni una consulta; una sobre un curso nunca evaluado cuesta, como
maximo, el tope de la corrida.

La alternativa —que el analisis fuera opcional y hubiera que pedirlo— lo habria
dejado apagado en la practica: un analisis que hay que recordar activar no forma
parte de la auditoria, es una herramienta aparte.

**Criterio de aceptacion**: una auditoria pedida sin ninguna indicacion sobre
analisis incluye los resultados del analisis de consigna, con su puntaje, sus
diagnosticos y su cobertura.

**Error**: N/A (esta regla describe la ejecucion por omision)

</details>

<a id="F-QINST-R002"></a>
### Rule[F-QINST-R002] - Del veredicto al puntaje: la severidad define la escala
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El puntaje de un ejercicio evaluado se deriva de dos datos del veredicto: si
> **cumple** la consigna y, si no la cumple, con que **severidad**. La escala es
> 1,0 / 0,6 / 0,3 / 0,0 segun el incumplimiento sea inexistente, menor, mayor o
> critico.

<details><summary>Detalle</summary>

| Veredicto del juez | Puntaje del ejercicio |
|---|---|
| Cumple la consigna, severidad "ninguna" | 1,0 |
| No cumple, severidad "menor" | 0,6 |
| No cumple, severidad "mayor" | 0,3 |
| No cumple, severidad "critica" | 0,0 |

**Por que graduado y no binario.** El puntaje se promedia hacia arriba por la
jerarquia del curso, igual que el de cualquier otro analisis
([F-SLEN-R003](../2026-03-18.01_sentence-length/REQUIREMENT.md#F-SLEN-R003) y
siguientes). Con una escala binaria, un knowledge con un ejercicio de puntuacion
discutible y otro con un ejercicio irresoluble se leerian igual, y el usuario
—que navega la jerarquia buscando donde mirar primero— perderia justamente la
informacion que necesita para priorizar.

**Por que estos valores.** Con ~19 ejercicios por knowledge, un incumplimiento
critico baja el promedio del knowledge unos 0,05 y uno menor unos 0,02: los saltos
de 0,3 entre escalones son lo bastante anchos para que un problema grave se
distinga de varios leves al promediar, y lo bastante contenidos para que un caso
aislado no hunda un knowledge sano. El 0,6 del incumplimiento menor deja al
ejercicio claramente por debajo del 1,0 del ejercicio limpio —hay algo que
corregir— sin equipararlo a uno roto.

**Combinaciones inconsistentes.** Si el veredicto afirma que el ejercicio cumple
pero declara una severidad distinta de "ninguna", **manda la severidad**: se
puntua como incumplimiento de esa severidad. Si afirma que no cumple pero declara
severidad "ninguna", se puntua como incumplimiento **menor**. En los dos casos se
elige la lectura mas conservadora, porque el error de dar por bueno un ejercicio
malo es mas caro que el inverso: el ejercicio malo llega al alumno, mientras que
el falso positivo solo cuesta una revision humana.

**La confianza del veredicto no modifica el puntaje**; queda registrada en el
diagnostico ([F-QINST-R003](#F-QINST-R003)) para que quien revise pueda ponderar
el caso. Ver [DOUBT-CONFIANZA](#DOUBT-CONFIANZA).

**Error**: "Puntaje fuera del rango [0,0 - 1,0] para el ejercicio '{quizId}' del knowledge '{knowledgeId}'"

</details>

<a id="F-QINST-R003"></a>
### Rule[F-QINST-R003] - Cada ejercicio evaluado deja un diagnostico inspeccionable
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo ejercicio evaluado registra, ademas del puntaje, un diagnostico que
> permite entender **por que** obtuvo ese puntaje y, mas adelante, proponer una
> correccion: el motivo breve, la severidad, la confianza, cada violacion
> detectada —con su codigo, la restriccion incumplida, la evidencia textual y la
> explicacion— y los controles que el juez realizo con su estado.

<details><summary>Detalle</summary>

| Dato del diagnostico | Para que sirve |
|---|---|
| Cumple / no cumple | Lectura inmediata del veredicto |
| Severidad | Justifica el puntaje ([F-QINST-R002](#F-QINST-R002)) |
| Confianza | Permite ponderar cuanto pesa el veredicto al revisarlo a mano |
| Motivo | Resumen en una linea, legible sin abrir el detalle |
| Violaciones (codigo, restriccion, evidencia, explicacion) | Es la materia prima de una futura correccion: dice **que** se incumplio y **donde** se ve |
| Controles realizados (categoria, restriccion, estado, evidencia) | Muestra que se reviso y que quedo fuera; permite detectar un juez que no miro lo que debia |

La **evidencia** es lo que separa este diagnostico de un simple puntaje bajo: sin
la cita del fragmento que incumple, quien revisa tiene que reconstruir el juicio
desde cero, y un futuro proponente de correcciones no tiene por donde empezar. Es
el mismo criterio que ya rige los diagnosticos tipados de los demas analisis
([FEAT-DSLEN](../2026-04-05.01_typed-diagnostics-sentence-length/REQUIREMENT.md)):
exponer los datos que determinaron el puntaje, no solo el puntaje.

Un ejercicio que cumple la consigna tambien deja diagnostico, con la lista de
violaciones vacia y sus controles en estado satisfactorio. Sirve como constancia
de que fue evaluado y de que se reviso.

**Criterio de aceptacion**: para un ejercicio con incumplimiento, el diagnostico
contiene al menos una violacion con sus cuatro datos poblados y la evidencia
citada; para uno que cumple, contiene los controles realizados y ninguna
violacion.

**Error**: "El ejercicio '{quizId}' fue evaluado pero no registro diagnostico de consigna"

</details>

<a id="F-QINST-R004"></a>
### Rule[F-QINST-R004] - Un ejercicio no evaluado no recibe puntaje ni diagnostico
**Severity**: critical | **Validation**: VALIDATED

> Los ejercicios que quedaron **pendientes** (no alcanzo el presupuesto de la
> corrida) o **fallidos** (el juez no llego a pronunciarse) no reciben puntaje de
> este analisis, y por lo tanto no participan de ningun promedio. Tampoco
> registran diagnostico.

<details><summary>Detalle</summary>

Es la aplicacion literal del comportamiento de agregacion que ya rige el sistema:
un analisis que no puntua un nodo simplemente no aparece en el promedio de ese
nodo ([F-SLEN-R001](../2026-03-18.01_sentence-length/REQUIREMENT.md#F-SLEN-R001),
[F-SLEN-R003](../2026-03-18.01_sentence-length/REQUIREMENT.md#F-SLEN-R003)).

Las alternativas son todas peores y todas mienten:

- **Puntuar 1,0 lo no evaluado** dice que el ejercicio esta bien cuando nadie lo
  miro. En la primera corrida, con casi todo pendiente, el analisis reportaria un
  curso impecable.
- **Puntuar 0,0 lo no evaluado** dice que esta mal por la misma razon, y hunde el
  promedio de todo knowledge parcialmente evaluado.
- **Puntuar con un valor intermedio** mezcla en un mismo numero "esto se juzgo asi"
  con "esto no se juzgo", que es exactamente lo que el promedio no puede
  distinguir despues.

La consecuencia buscada: el puntaje del analisis **siempre** se interpreta sobre
la porcion efectivamente evaluada, y esa porcion se declara explicitamente
([F-QINST-R005](#F-QINST-R005)).

**Criterio de aceptacion**: en un knowledge con parte de sus ejercicios evaluados
y parte pendientes, el puntaje del knowledge es el promedio de los evaluados
unicamente, y los pendientes no tienen ni puntaje ni diagnostico de consigna.

**Error**: "El ejercicio '{quizId}' no fue evaluado y sin embargo recibio puntaje de consigna"

</details>

<a id="F-QINST-R005"></a>
### Rule[F-QINST-R005] - El informe declara la cobertura del analisis
**Severity**: critical | **Validation**: VALIDATED

> El informe de auditoria expone, para este analisis, cuantos ejercicios alcanza,
> cuantos tienen veredicto (distinguiendo los evaluados en esta corrida de los
> reutilizados de corridas anteriores), cuantos quedaron pendientes y cuantos
> fallaron. El puntaje del analisis se lee **siempre** junto a esa cobertura.

<details><summary>Detalle</summary>

| Dato de cobertura | Significado |
|---|---|
| Alcanzados | Ejercicios del curso que este analisis debe evaluar ([F-QINST-R014](#F-QINST-R014)) |
| Con veredicto | Ejercicios con puntaje en esta auditoria |
| — evaluados en esta corrida | Consumieron presupuesto ([F-QINST-R006](#F-QINST-R006)) |
| — reutilizados | Veredicto vigente de una corrida anterior ([F-QINST-R008](#F-QINST-R008)) |
| Pendientes | Sin veredicto por tope de presupuesto; los toma la corrida siguiente |
| Fallidos | El juez no llego a pronunciarse en esta corrida ([F-QINST-R007](#F-QINST-R007)) |

Sin este dato el puntaje es directamente **engañoso**: un 0,95 sobre 40
ejercicios evaluados de 11.500 se lee igual que un 0,95 sobre el curso entero, y
solo uno de los dos autoriza a concluir algo. Esta regla es la que hace legitima
la cobertura parcial de [F-QINST-R004](#F-QINST-R004): el analisis puede informar
sobre una parte del curso siempre que diga **cual**.

La suma de con-veredicto, pendientes y fallidos debe dar el total de alcanzados:
todo ejercicio alcanzado esta en exactamente uno de los tres estados.

**Criterio de aceptacion**: tras una corrida que evalua solo una parte del curso,
el informe declara los cinco numeros, la suma cierra contra el total de
alcanzados, y una segunda corrida muestra crecer los reutilizados y decrecer los
pendientes.

**Error**: "La cobertura declarada del analisis de consigna no cierra: {conVeredicto} + {pendientes} + {fallidos} != {alcanzados}"

</details>

<a id="F-QINST-R006"></a>
### Rule[F-QINST-R006] - Cada corrida acota cuantas consultas nuevas hace al juez
**Severity**: critical | **Validation**: VALIDATED

> Cada corrida consulta al juez por un numero maximo y configurable de ejercicios
> pendientes, con un valor por defecto. Alcanzado el tope, los ejercicios
> restantes quedan pendientes **sin error**: los toma la corrida siguiente.
> Reutilizar un veredicto ya emitido **no consume** presupuesto.

<details><summary>Detalle</summary>

Una pasada completa sobre ~11.500 ejercicios son horas de reloj y una factura
considerable, y el usuario no siempre quiere pagarlas de una sola vez. El
presupuesto convierte esa pasada en un trabajo incremental: cada auditoria avanza
un tramo y deja el resto para la proxima, sin que ninguna corrida deje de terminar.

**El tope aplica solo a las consultas nuevas.** La reutilizacion no tiene limite
([F-EVCOST-R003](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R003)):
un curso completamente evaluado se audita entero, sin consultas y sin tope, tantas
veces como haga falta. De lo contrario el presupuesto castigaria precisamente el
caso que la feature busca: reutilizar.

**Valor por defecto** [ASSUMPTION]: 500 ejercicios por corrida — el orden de
magnitud que mantiene una auditoria dentro de una espera tolerable y completa el
curso en unas dos decenas de corridas. Ver
[DOUBT-PRESUPUESTO-DEFECTO](#DOUBT-PRESUPUESTO-DEFECTO).

**Criterio de aceptacion**: con un tope de N y mas de N ejercicios pendientes, la
corrida consulta al juez exactamente N veces, termina sin error, y declara los
restantes como pendientes; con todos los ejercicios ya evaluados, la corrida no
consulta ninguna vez, cualquiera sea el tope.

**Error**: N/A (agotar el presupuesto es un desenlace normal, no un error)

</details>

<a id="F-QINST-R007"></a>
### Rule[F-QINST-R007] - Una falla del juez no aborta la auditoria ni corrompe lo ya guardado
**Severity**: critical | **Validation**: VALIDATED

> Si el juez deja de responder a mitad de corrida —servicio caido, tiempo de
> espera agotado, credito agotado—, la auditoria **termina igual**, con la
> cobertura que alcanzo. Los ejercicios no evaluados quedan pendientes y sin
> puntaje; los veredictos ya obtenidos en esa misma corrida siguen registrados y
> disponibles.

<details><summary>Detalle</summary>

Que el credito se agote a mitad de camino no es un caso raro: con miles de
ejercicios por evaluar, es el desenlace **esperado** de las primeras corridas. Un
analisis que aborta la auditoria entera ante ese desenlace haria inutilizables a
todos los demas analisis, que no dependen del juez y ya terminaron su trabajo.

Una falla del juez es **transitoria**: no queda registrada como veredicto y no
convierte al ejercicio en "incumplidor"
([F-EVCOST-R005](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R005)).
La corrida siguiente lo encuentra pendiente y vuelve a intentarlo. Esto es lo
opuesto a un veredicto negativo legitimo, que **si** es un resultado y **si** se
registra: la diferencia esta en si el juez se pronuncio sobre el ejercicio o no
llego a hacerlo.

Las fallas se cuentan y se informan como tales
([F-QINST-R005](#F-QINST-R005)), separadas de los pendientes por presupuesto:
"no alcanzo el presupuesto" y "el servicio fallo" son dos hechos distintos y el
usuario reacciona distinto ante cada uno.

**Criterio de aceptacion**: interrumpido el servicio del juez tras evaluar K
ejercicios, la auditoria termina exitosamente, informa K con veredicto y el resto
como fallidos o pendientes, y una corrida posterior reutiliza esos K sin volver a
consultar.

**Error**: "La auditoria se interrumpio por una falla del juez de consigna" (condicion prohibida: la auditoria debe completarse)

</details>

<a id="F-QINST-R008"></a>
### Rule[F-QINST-R008] - Una corrida no vuelve a consultar por lo ya evaluado
**Severity**: critical | **Validation**: VALIDATED

> Ante un ejercicio cuyo contenido juzgado ya tiene veredicto vigente, la corrida
> lo reutiliza y **no** consulta al juez. El presupuesto de la corrida se dedica
> integramente a los ejercicios sin veredicto, de modo que cada corrida avanza
> sobre lo que falta.

<details><summary>Detalle</summary>

Es el uso directo de la capacidad general de resultados costosos reutilizables
([F-EVCOST-R003](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R003)):
este analisis no define su propio mecanismo de reuso, lo consume.

La consecuencia observable es la reanudabilidad: N corridas sucesivas sin cambios
en el contenido cubren N veces el presupuesto de ejercicios distintos, nunca los
mismos dos veces. Sin esta regla, cada corrida volveria a empezar por el primer
ejercicio del curso y el avance seria nulo.

**Criterio de aceptacion**: dos corridas consecutivas con tope N sobre un curso
sin cambios evaluan 2N ejercicios **distintos**; el juez no recibe dos veces el
mismo contenido.

**Error**: "Se consulto al juez por el ejercicio '{quizId}', que ya tenia veredicto vigente"

</details>

<a id="F-QINST-R009"></a>
### Rule[F-QINST-R009] - La identidad del veredicto es el contenido juzgado, no el ejercicio
**Severity**: critical | **Validation**: VALIDATED

> Un veredicto se reutiliza segun la huella del **contenido completo** que se le
> entrego al juez: el [Nivel CEFR](glossary:Nivel CEFR), el topic, la etiqueta del
> knowledge, sus instrucciones y el ejercicio entero con todas sus partes y todas
> sus opciones aceptadas. El identificador del ejercicio **no** interviene. Si
> cualquiera de esos datos cambia, el veredicto guardado deja de aplicar y el
> ejercicio vuelve a estar pendiente.

<details><summary>Detalle</summary>

**Corregir un ejercicio no cambia su identificador**
([FEAT-RPRES](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md)):
el refinador reescribe la oracion, los huecos y las respuestas aceptadas de un
ejercicio que sigue llamandose igual. Con identidad por identificador, el
ejercicio corregido arrastraria para siempre el juicio emitido sobre su version
anterior — y en el peor caso el sistema seguiria reportando como incumplidor
justo al ejercicio que ya se arreglo, o como valido al que se rompio.

**Todo lo que el juez mira entra en la huella**, no solo el ejercicio:

| Dato juzgado | De donde viene | Consecuencia de que cambie |
|---|---|---|
| Nivel CEFR | Milestone que contiene al ejercicio | Vuelve a evaluarse |
| Topic | Topic que lo contiene | Vuelve a evaluarse |
| Etiqueta del knowledge | Knowledge que lo contiene | Vuelven a evaluarse **todos** sus ejercicios |
| Instrucciones del knowledge | Knowledge que lo contiene | Vuelven a evaluarse **todos** sus ejercicios |
| Ejercicio completo: partes, huecos y todas las opciones aceptadas | El ejercicio | Vuelve a evaluarse |

El efecto en cascada sobre el knowledge es correcto y buscado: la consigna es
justamente lo que el juez verifica, y cambiarla cambia el juicio de cada ejercicio
que la obedece. Corregir una etiqueta o una instruccion es raro; corregirlas e
ignorar que todos sus ejercicios pasan a juzgarse contra otra consigna seria un
error grave.

**El knowledge es la fuente canonica, aunque el ejercicio lleve copias.** Cada
ejercicio arrastra su propia copia de la etiqueta y de las instrucciones de su
knowledge —el titulo con que se lo presenta es una copia desnormalizada de la
etiqueta ([F-RPRES-R004](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md#F-RPRES-R004))—.
Sobre el curso real esas copias coinciden con el original en los **11.487
ejercicios**, sin una sola excepcion, asi que hoy la eleccion no cambia ningun
veredicto. Aun asi la regla nombra al knowledge, por dos razones:

1. **Es lo que da sentido a la cascada.** La consigna se escribe una vez, en el
   knowledge, y la obedecen todos sus ejercicios. Tomar el dato de la copia
   dejaria la cascada apoyada en que las copias se hayan actualizado.
2. **La divergencia es un defecto conocido, no una fuente alternativa.** Existe un
   paso de publicacion que sincroniza el titulo del ejercicio con la etiqueta de su
   knowledge, precisamente para mantenerlas alineadas. Si alguna vez divergen,
   manda el knowledge: la copia desactualizada es el error, no un valor legitimo
   con el que juzgar.

La regla depende de que la auditoria pueda ofrecerle al juez el ejercicio
**completo**, que hoy no ocurre: ver [DOUBT-QUIZ-COMPLETO](#DOUBT-QUIZ-COMPLETO).

**Criterio de aceptacion**: (a) cambiar una sola opcion aceptada de un ejercicio
—sin tocar su identificador— lo deja pendiente y provoca una consulta nueva al
juez; (b) cambiar las instrucciones de un knowledge deja pendientes a todos sus
ejercicios; (c) reordenar o reprocesar el curso sin cambiar contenido no provoca
ninguna consulta nueva.

**Error**: "Se reutilizo el veredicto del ejercicio '{quizId}' pese a que su contenido juzgado cambio"

</details>

<a id="F-QINST-R010"></a>
### Rule[F-QINST-R010] - Cambiar el juez no borra ni re-evalua nada por si solo
**Severity**: critical | **Validation**: VALIDATED

> La version del juez —su forma de preguntar y la estructura de lo que devuelve—
> forma parte de la identidad del veredicto. Al cambiarla, los veredictos
> anteriores **dejan de ser vigentes** pero **no se borran**: quedan como
> historia. Volver a evaluar el curso con el juez nuevo es una decision explicita
> del usuario ([F-QINST-R012](#F-QINST-R012)), nunca un efecto colateral de haber
> tocado el juez.

<details><summary>Detalle</summary>

Ajustar el juez es una tarea de iteracion normal y se hace muchas veces; volver a
juzgar 11.500 ejercicios es lento y caro. Si lo primero disparara lo segundo,
cada retoque del juez se convertiria en una factura sorpresa, y en la practica
nadie tocaria el juez.

Conservar la historia tiene ademas valor propio: comparar como juzgo el mismo
ejercicio cada version del juez es la unica forma de saber si el cambio lo mejoro.
Es el comportamiento general de
[F-EVCOST-R006](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R006)
aplicado a este juez.

Tras cambiar la version, y hasta que el usuario decida re-evaluar, los ejercicios
figuran como **pendientes** en la cobertura ([F-QINST-R005](#F-QINST-R005)): el
informe dice con precision que el curso quedo sin juicio vigente, en lugar de
mostrar puntajes que ya no corresponden al juez actual.

**Criterio de aceptacion**: cambiada la version del juez, ninguna corrida
posterior reutiliza los veredictos anteriores, esos veredictos siguen
consultables, y no se dispara ninguna consulta nueva mas alla del presupuesto
ordinario de la corrida.

**Error**: "El cambio de version del juez de consigna elimino {cantidad} veredictos anteriores"

</details>

<a id="F-QINST-R011"></a>
### Rule[F-QINST-R011] - El usuario puede excluir el analisis de una corrida
**Severity**: major | **Validation**: VALIDATED

> Al pedir una auditoria, el usuario puede excluir explicitamente el analisis de
> consigna. Excluido, el analisis no consulta al juez, no puntua ningun ejercicio,
> no emite diagnosticos ni cobertura, y no consume presupuesto. Los veredictos ya
> registrados no se tocan.

<details><summary>Detalle</summary>

La exclusion existe para el caso en que el usuario quiere una auditoria rapida y
gratuita —revisar longitud y vocabulario tras un cambio, por ejemplo— sin arrastrar
la unica parte lenta y pagada de la auditoria.

Excluir es distinto de "no tener veredictos": el analisis excluido **no aparece**
en el informe, en lugar de aparecer con cobertura cero. La diferencia importa,
porque cobertura cero significa "corri y no pude evaluar nada", que es un hecho
digno de atencion, y excluido significa "no me pediste que corriera".

Esta regla convive con la seleccion de un subconjunto de analisis que ya ofrece el
sistema: pedir una auditoria acotada a otros analisis tiene el mismo efecto que
excluirlo.

**Criterio de aceptacion**: excluido el analisis, el informe no contiene puntaje,
diagnostico ni cobertura de consigna, el juez no recibe ninguna consulta, y los
veredictos registrados siguen intactos para la corrida siguiente.

**Error**: N/A (esta regla describe una opcion del usuario)

</details>

<a id="F-QINST-R012"></a>
### Rule[F-QINST-R012] - La re-evaluacion es explicita y acotable
**Severity**: major | **Validation**: VALIDATED

> El usuario puede pedir explicitamente que se vuelva a juzgar contenido que ya
> tiene veredicto vigente. El pedido puede alcanzar todo el curso o una parte de
> el, respeta el presupuesto de la corrida ([F-QINST-R006](#F-QINST-R006)) y no
> destruye los veredictos anteriores.

<details><summary>Detalle</summary>

Es la valvula de escape de [F-QINST-R008](#F-QINST-R008) y
[F-QINST-R010](#F-QINST-R010): sin ella, un veredicto registrado seria definitivo
para siempre y no habria forma de aprovechar una mejora del juez.

Que respete el presupuesto no es un detalle: una re-evaluacion total sin tope seria
exactamente la factura sorpresa que [F-QINST-R010](#F-QINST-R010) evita. Con tope,
la re-evaluacion tambien es incremental — se pide una vez y avanza corrida a
corrida.

**Criterio de aceptacion**: pedida la re-evaluacion, la corrida consulta al juez
por ejercicios que ya tenian veredicto vigente, sin exceder el presupuesto, y los
veredictos anteriores siguen consultables.

**Error**: N/A (esta regla describe una accion del usuario)

</details>

<a id="F-QINST-R013"></a>
### Rule[F-QINST-R013] - Un veredicto de infraestructura del juez es una falla, no un juicio
**Severity**: critical | **Validation**: VALIDATED

> Cuando el juez no logra producir un veredicto valido y responde con su veredicto
> conservador de infraestructura —confianza nula y una violacion que declara su
> propia salida invalida—, ese ejercicio se trata como **falla transitoria**: no
> recibe puntaje, no registra diagnostico, no se guarda como veredicto y queda
> pendiente para la corrida siguiente.

<details><summary>Detalle</summary>

El juez ya emite este veredicto por su cuenta, en lugar de promover una salida
malformada. Visto desde afuera **parece** un juicio —tiene la misma forma que
cualquier otro— pero no dice nada sobre el ejercicio: dice que el juez no pudo
pronunciarse.

Tratarlo como juicio seria el peor error posible de esta feature: el ejercicio
recibiria puntaje 0,0 por [F-QINST-R002](#F-QINST-R002) (una acusacion falsa de
incumplimiento) y ese 0,0 quedaria registrado para siempre por
[F-QINST-R008](#F-QINST-R008), sin que nadie lo vuelva a intentar. Un problema de
infraestructura se habria convertido en un defecto permanente del contenido.

Esta regla es la forma concreta que toma en este analisis el criterio general de
[F-EVCOST-R005](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R005):
si el evaluador no se pronuncio sobre el contenido, no hay resultado.

**Criterio de aceptacion**: ante ese veredicto, el ejercicio queda sin puntaje ni
diagnostico, se cuenta como fallido en la cobertura, y la corrida siguiente vuelve
a consultarlo.

**Error**: "Se registro como veredicto una respuesta de infraestructura del juez para el ejercicio '{quizId}'"

</details>

<a id="F-QINST-R014"></a>
### Rule[F-QINST-R014] - Alcance: todo ejercicio del curso, tenga o no restricciones explicitas
**Severity**: major | **Validation**: VALIDATED

> El analisis alcanza a **todos** los ejercicios del curso. Un ejercicio que no
> declara instrucciones se evalua igual: el juez lo revisa por gramatica,
> significado y resolubilidad, sin restricciones explicitas que verificar.

<details><summary>Detalle</summary>

El alcance total se sostiene por dos razones, una de criterio y otra medida:

- **De criterio**: las tres dimensiones que el juez revisa ademas de la consigna
  —gramatica, significado y resolubilidad— aplican a cualquier ejercicio, con o
  sin instrucciones. Un ejercicio irresoluble lo es igual aunque nadie haya
  escrito su consigna.
- **De medicion** (sobre el curso real, 2026-07-29): las dos formas de acotar el
  alcance que se consideraron no ahorran practicamente nada. Recortar por
  instrucciones alcanzaria a **20 ejercicios de 11.487** (0,17% del gasto), y
  recortar por "no oracion" —el criterio que usa el analisis de longitud
  ([F-SLEN-R001](../2026-03-18.01_sentence-length/REQUIREMENT.md#F-SLEN-R001))—
  **no excluiria ninguno**, porque el curso no tiene contenido marcado asi. Ver
  [DOUBT-ALCANCE-EJERCICIOS](#DOUBT-ALCANCE-EJERCICIOS) para el detalle.

Es decir: el alcance total no cuesta mas que los alcances acotados, y en cambio
evita dejar 20 ejercicios sin ninguna revision de resolubilidad.

Los ejercicios alcanzados son la base del recuento de cobertura
([F-QINST-R005](#F-QINST-R005)): cambiar este alcance cambia el denominador
contra el que se lee el avance.

**Criterio de aceptacion**: el total de alcanzados que declara la cobertura
coincide con la cantidad de ejercicios del curso, sin exclusiones.

**Error**: N/A (esta regla define un alcance)

</details>

## Contexto

La auditoria de ContentAudit mide hoy propiedades **calculables** del contenido:
cuantos tokens tiene una oracion frente al rango de su nivel, que lemas aparecen
donde no corresponde, como se distribuye el vocabulario. Todas comparten la misma
naturaleza: se resuelven contando, son baratas y son deterministas.

Hay una clase de defecto que ninguna de esas medidas alcanza: **el ejercicio no
hace lo que su consigna dice**. La instruccion pide completar con un verbo en
pasado y la unica respuesta aceptada esta en presente; pide una sola palabra y la
respuesta tiene tres; el hueco admite dos respuestas de las cuales una es
agramatical; el ejercicio, tal como esta escrito, no tiene solucion. Ninguno de
esos casos se detecta contando: hace falta leer el ejercicio, leer su consigna y
juzgar si se corresponden.

Ese juicio ya existe y esta probado por separado: el **juez de consigna**. Dado el
[Nivel CEFR](glossary:Nivel CEFR), el topic, la etiqueta y las instrucciones del knowledge y el ejercicio
completo —con todas sus partes y todas sus opciones aceptadas—, decide si el
ejercicio cumple **literalmente todas** las restricciones explicitas de su
consigna, ademas de revisar gramatica, significado y resolubilidad. Devuelve si
cumple, con que confianza, con que severidad, un motivo, la lista de violaciones
con evidencia y la lista de controles realizados.

Lo que falta es incorporarlo a la auditoria. Y ahi el juez rompe los tres
supuestos sobre los que estan escritos los analisis existentes:

| Supuesto de los analisis actuales | Con el juez de consigna |
|---|---|
| Evaluar un ejercicio es gratis | Cuesta una consulta pagada y lenta; ~11.500 ejercicios son horas |
| Una corrida siempre termina | El credito se agota, el servicio se cae, el proceso se corta |
| Volver a evaluar da lo mismo | El juicio no es determinista y repetirlo introduce ruido ademas de costo |

De esos tres se desprende todo lo demas: hace falta **recordar** lo ya juzgado
entre corridas, **avanzar por tramos** en lugar de todo de una vez, y **no mentir**
sobre la parte del curso que todavia no se miro.

### Por que la reutilizacion no puede identificarse por ejercicio

La forma obvia de recordar un veredicto —guardarlo bajo el identificador del
ejercicio— es la unica que este sistema **no** puede usar: corregir un ejercicio no
cambia su identificador
([FEAT-RPRES](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md)).
El refinador reescribe oraciones, huecos y respuestas aceptadas todo el tiempo, y
el ejercicio resultante conserva su nombre. Con identidad por identificador, el
sistema seguiria mostrando el juicio de la version vieja: acusaria de incumplidor
a un ejercicio ya corregido, o daria por bueno uno recien roto.

Por eso la identidad es la **huella del contenido juzgado**
([F-QINST-R009](#F-QINST-R009)): describe que se juzgo, no sobre que ejercicio.

### Por que el estado reutilizable se especifica aparte

Este analisis es el primer consumidor de una capacidad que no es suya: registrar
el resultado de una evaluacion costosa, reutilizarlo mientras el contenido no
cambie, conservarlo ante una interrupcion y compartirlo entre analisis sucesivos.
El mismo patron reaparecera con cualquier otro evaluador basado en juicio
—coherencia pedagogica de un knowledge, calidad de una traduccion, deteccion de
ejercicios repetidos—.

Esa capacidad se especifica en
[FEAT-EVCOST](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md);
este analisis la consume y solo agrega lo que le es propio: que evalua su juez,
como se traduce el veredicto a puntaje, que diagnostico deja y como declara su
cobertura.

### Cobertura parcial: por que es un resultado valido

La primera pasada completa del juez sobre el curso son horas. La decision tomada
es no intentarla de una vez: cada corrida avanza un tramo acotado
([F-QINST-R006](#F-QINST-R006)) y el resto queda pendiente. Eso vuelve normal —no
excepcional— que una auditoria informe sobre una parte del curso.

Un informe parcial es util **solo si dice que es parcial**. De ahi las dos reglas
que lo sostienen: lo no evaluado no recibe puntaje, para no contaminar los
promedios ([F-QINST-R004](#F-QINST-R004)), y el informe declara explicitamente la
cobertura alcanzada ([F-QINST-R005](#F-QINST-R005)). Juntas producen una lectura
honesta: "de los ejercicios que se miraron, este es el resultado; estos otros
todavia no se miraron".

## Alcance

- **En alcance**:
  - Puntaje por ejercicio derivado del veredicto del juez de consigna, y su
    agregacion por la jerarquia con el mecanismo generico de la plataforma.
  - Diagnostico por ejercicio evaluado, con violaciones y evidencia.
  - Ejecucion por omision en toda auditoria y exclusion explicita.
  - Presupuesto acotado de consultas nuevas por corrida y reanudacion en corridas
    posteriores.
  - Cobertura declarada: alcanzados, con veredicto, reutilizados, pendientes y
    fallidos.
  - Separacion entre falla transitoria y veredicto negativo legitimo.
  - Identidad del veredicto por contenido juzgado, incluida la version del juez.
  - Re-evaluacion explicita.
- **Fuera de alcance**:
  - **El criterio con que el juez decide.** Que revisa, como pondera y como
    redacta sus violaciones ya esta definido y probado en el juez; este
    requerimiento consume su veredicto y no lo redefine.
  - **El mecanismo general de registro, reuso, resistencia a interrupcion e
    historia de versiones**, que especifica
    [FEAT-EVCOST](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md).
  - **Proponer o aplicar correcciones** a los ejercicios que incumplen. El
    diagnostico ([F-QINST-R003](#F-QINST-R003)) deja la informacion necesaria para
    que una feature posterior lo haga; esa feature no es esta.
  - **Estadisticas agregadas por nivel CEFR** de cumplimiento de consigna.
  - **La forma de completar la representacion del ejercicio** que el juez necesita;
    ver [DOUBT-QUIZ-COMPLETO](#DOUBT-QUIZ-COMPLETO).

## User Journeys

### Journey[F-QINST-J001] - Puntuar un ejercicio a partir del veredicto del juez
**Validation**: AUTO_VALIDATED

Cubre la traduccion del veredicto a puntaje en las cuatro severidades, el
diagnostico que queda registrado y el caso del veredicto de infraestructura, que
no es juicio. Cubre R002, R003 y R013.

```yaml
journeys:
  - id: F-QINST-J001
    name: Puntuar un ejercicio a partir del veredicto del juez
    flow:
      - id: consultar_juez
        action: "El sistema entrega al juez el contenido completo de un ejercicio pendiente: nivel CEFR, topic, etiqueta e instrucciones de su knowledge y el ejercicio con todas sus partes y opciones aceptadas"
        outcomes:
          - when: "El juez responde que el ejercicio cumple su consigna, con severidad ninguna"
            then: cumple
          - when: "El juez responde que el ejercicio no cumple, con severidad menor"
            then: incumple_menor
          - when: "El juez responde que el ejercicio no cumple, con severidad mayor"
            then: incumple_mayor
          - when: "El juez responde que el ejercicio no cumple, con severidad critica"
            then: incumple_critico
          - when: "El juez no logra emitir un veredicto valido y responde con su veredicto conservador de infraestructura (confianza nula y violacion de salida invalida)"
            then: falla_del_juez

      - id: cumple
        action: "El ejercicio recibe el puntaje maximo del analisis y registra un diagnostico sin violaciones, con los controles realizados por el juez"
        gate: [F-QINST-R002, F-QINST-R003]
        result: success

      - id: incumple_menor
        action: "El ejercicio recibe el puntaje correspondiente a la severidad menor y registra un diagnostico con cada violacion detectada, su restriccion incumplida, su evidencia y su explicacion"
        gate: [F-QINST-R002, F-QINST-R003]
        result: success

      - id: incumple_mayor
        action: "El ejercicio recibe el puntaje correspondiente a la severidad mayor y registra el diagnostico con sus violaciones y evidencia"
        gate: [F-QINST-R002, F-QINST-R003]
        result: success

      - id: incumple_critico
        action: "El ejercicio recibe el puntaje minimo y registra el diagnostico con sus violaciones y evidencia"
        gate: [F-QINST-R002, F-QINST-R003]
        result: success

      - id: falla_del_juez
        action: "El ejercicio no recibe puntaje ni diagnostico, no queda registrado ningun veredicto para el, y se cuenta como fallido"
        gate: [F-QINST-R013]
        result: failure
```

### Journey[F-QINST-J002] - Auditar con cobertura parcial y reanudar en la corrida siguiente
**Validation**: VALIDATED

Cubre el avance por tramos: presupuesto agotado, falla del servicio a mitad de
corrida, informe de cobertura, y la corrida siguiente reutilizando lo ya juzgado
o volviendo a juzgar lo que cambio. Cubre R004, R005, R006, R007, R008 y R009.

```yaml
journeys:
  - id: F-QINST-J002
    name: Auditar con cobertura parcial y reanudar en la corrida siguiente
    flow:
      - id: primera_corrida
        action: "El usuario audita un curso cuyos ejercicios no tienen veredicto de consigna registrado"
        then: evaluar_pendientes

      - id: evaluar_pendientes
        action: "El sistema consulta al juez por los ejercicios pendientes hasta el tope de consultas de la corrida"
        gate: [F-QINST-R006]
        outcomes:
          - when: "El tope de la corrida se agota antes de recorrer todos los ejercicios"
            then: informe_parcial
          - when: "El juez deja de responder a mitad de corrida porque el servicio cae o el credito se agota"
            then: informe_tras_falla

      - id: informe_parcial
        action: "La auditoria termina y su informe declara cuantos ejercicios fueron evaluados y cuantos quedaron pendientes; los pendientes no tienen puntaje ni diagnostico y no participan de ningun promedio"
        gate: [F-QINST-R004, F-QINST-R005]
        then: segunda_corrida

      - id: informe_tras_falla
        action: "La auditoria termina igual, sin abortar: los ejercicios no evaluados se declaran fallidos y sin puntaje, y los veredictos obtenidos antes de la falla quedan registrados"
        gate: [F-QINST-R007]
        then: segunda_corrida

      - id: segunda_corrida
        action: "El usuario vuelve a auditar el mismo curso"
        outcomes:
          - when: "El contenido juzgado de los ejercicios ya evaluados no cambio"
            then: reusa_y_avanza
          - when: "Cambio el contenido juzgado de un ejercicio ya evaluado (por ejemplo, una de sus opciones aceptadas), conservando su identificador"
            then: reevalua_lo_cambiado

      - id: reusa_y_avanza
        action: "El juez no recibe ninguna consulta por los ejercicios ya evaluados y el tope de la corrida se dedica a los que faltaban: la cobertura declarada muestra mas ejercicios con veredicto y menos pendientes"
        gate: [F-QINST-R005, F-QINST-R008]
        result: success

      - id: reevalua_lo_cambiado
        action: "El ejercicio cuyo contenido cambio vuelve a consultarse al juez y su puntaje se recalcula sobre el veredicto nuevo"
        gate: [F-QINST-R009]
        result: success
```

### Journey[F-QINST-J003] - Ejecucion por omision, exclusion y re-evaluacion explicita
**Validation**: VALIDATED

Cubre las tres formas de pedir la auditoria respecto de este analisis y el efecto
de haber cambiado la version del juez. Cubre R001, R010, R011, R012 y R014.

```yaml
journeys:
  - id: F-QINST-J003
    name: Ejecucion por omision, exclusion y re-evaluacion explicita
    flow:
      - id: pedir_auditoria
        action: "El usuario pide la auditoria de un curso"
        outcomes:
          - when: "No indica nada sobre el analisis de consigna"
            then: corre_por_omision
          - when: "Excluye explicitamente el analisis de consigna"
            then: no_corre
          - when: "Pide explicitamente volver a juzgar el curso"
            then: reevalua_a_pedido
          - when: "La version del juez cambio desde la ultima auditoria y el usuario no pidio volver a juzgar"
            then: veredictos_no_vigentes

      - id: corre_por_omision
        action: "El informe incluye el puntaje, los diagnosticos y la cobertura del analisis de consigna sobre todos los ejercicios del curso"
        gate: [F-QINST-R001, F-QINST-R014]
        result: success

      - id: no_corre
        action: "El juez no recibe ninguna consulta, el informe no incluye puntaje, diagnostico ni cobertura de consigna, y los veredictos registrados quedan intactos"
        gate: [F-QINST-R011]
        result: success

      - id: reevalua_a_pedido
        action: "El sistema vuelve a consultar al juez por ejercicios que ya tenian veredicto vigente, sin exceder el tope de la corrida, y los veredictos anteriores siguen consultables"
        gate: [F-QINST-R012]
        result: success

      - id: veredictos_no_vigentes
        action: "Los veredictos de la version anterior del juez no se reutilizan ni se borran: los ejercicios figuran como pendientes en la cobertura y solo se vuelven a juzgar dentro del tope ordinario de la corrida"
        gate: [F-QINST-R010]
        result: success
```

## Open Questions

<a id="DOUBT-QUIZ-COMPLETO"></a>
### Doubt[DOUBT-QUIZ-COMPLETO] - La auditoria hoy no puede ofrecerle al juez el ejercicio completo
**Status**: OPEN — **bloqueante**

El juez necesita el ejercicio **entero**: todas sus partes, en orden, con todas
las opciones aceptadas de cada hueco. Es lo que le permite reconstruir cada
respuesta posible y decidir si alguna incumple la consigna. La representacion del
ejercicio que hoy recorre la auditoria es **incompleta** para eso: conserva las
oraciones y sus tokens, pero no todas las partes ni las opciones aceptadas.

La consecuencia es doble, y la segunda mitad es la peligrosa:

1. **El juicio se degrada**: sin las opciones aceptadas, el juez no puede
   verificar la restriccion mas frecuente de una consigna, que es sobre la forma
   de la respuesta.
2. **La huella deja de proteger**: [F-QINST-R009](#F-QINST-R009) exige que la
   huella se calcule sobre el contenido completo. Si se calculara sobre la
   representacion parcial, cambiar una opcion aceptada **no** invalidaria el
   veredicto guardado, y el sistema reutilizaria con confianza un juicio emitido
   sobre otro ejercicio. Es un error silencioso: nadie lo detecta.

- [ ] Opcion A: **La representacion auditable del ejercicio lleva el contenido
  completo.** Todo analisis pasa a ver el ejercicio entero. Resuelve el problema
  de raiz y sirve a cualquier evaluador futuro; encarece el recorrido de la
  auditoria para todos los analisis, incluidos los que no lo necesitan.
- [ ] Opcion B: **El analisis obtiene el contenido completo del ejercicio en el
  momento de evaluarlo**, desde la misma fuente de la que se cargo el curso. No
  encarece a los demas analisis; introduce una segunda via de lectura del
  contenido y hay que garantizar que sea el mismo estado que audita la corrida.
- [x] Opcion C (**descartada**): juzgar con la representacion parcial disponible
  hoy. Rompe las dos mitades de arriba a la vez: el juez no puede hacer su trabajo
  y la huella no protege.

**Answer**: Pendiente, y **bloqueante**: la feature no puede entrar en vigencia sin
resolverla. La Opcion C queda descartada desde el requerimiento —no es una eleccion
de implementacion, es una degradacion del comportamiento exigido—. La eleccion entre
A y B es de la fase de arquitectura: las dos satisfacen igual las reglas de este
documento, y lo que las separa es el costo sobre el resto de la auditoria. Sea cual
sea, se exige lo mismo: el juez recibe el ejercicio completo y la huella se calcula
sobre ese mismo contenido completo.

<a id="DOUBT-ESCALA-SEVERIDAD"></a>
### Doubt[DOUBT-ESCALA-SEVERIDAD] - Son adecuados los valores 1,0 / 0,6 / 0,3 / 0,0?
**Status**: OPEN

[F-QINST-R002](#F-QINST-R002) fija la escala de puntaje por severidad. La forma
(graduada, no binaria) esta decidida; los valores concretos son una eleccion
editorial que cambia como se lee el curso al promediar.

- [ ] Opcion A: **1,0 / 0,6 / 0,3 / 0,0** — escalones parejos de ~0,3. Un
  incumplimiento menor sigue penalizando de forma visible.
- [ ] Opcion B: **1,0 / 0,8 / 0,4 / 0,0** — mas indulgente con lo menor y mas
  duro con el salto a mayor. Adecuada si los incumplimientos menores resultan ser
  mayormente ruido del juez.
- [ ] Opcion C: **1,0 / 0,5 / 0,25 / 0,0** — mas severa: cualquier incumplimiento
  deja el ejercicio en la mitad o menos.
- [ ] Opcion D: **Binaria** (1,0 / 0,0) ignorando la severidad. Descartada por
  [F-QINST-R002](#F-QINST-R002): perderia la informacion que permite priorizar.

**Answer**: Pendiente. Se especifica la Opcion A [ASSUMPTION] por ser la mas neutra
de las tres graduadas. Conviene revisarla **con datos**: tras la primera corrida
con presupuesto, la distribucion real de severidades dira si los incumplimientos
menores son defectos accionables (y merecen 0,6) o ruido del juez (y merecen 0,8).

<a id="DOUBT-CONFIANZA"></a>
### Doubt[DOUBT-CONFIANZA] - La confianza del veredicto debe influir en el puntaje?
**Status**: OPEN

El juez informa la confianza de cada veredicto. Hoy
[F-QINST-R002](#F-QINST-R002) la ignora al puntuar y
[F-QINST-R003](#F-QINST-R003) solo la registra en el diagnostico. (El caso
extremo de confianza nula por falla del propio juez ya esta resuelto aparte, en
[F-QINST-R013](#F-QINST-R013): es falla, no juicio.)

- [ ] Opcion A: **Solo se registra.** El puntaje depende unicamente de cumple +
  severidad; la confianza queda en el diagnostico para quien revise a mano.
  Simple y predecible.
- [ ] Opcion B: **Umbral**: por debajo de cierta confianza el veredicto no puntua
  y el ejercicio se cuenta como no evaluado. Evita acusar con un juicio dudoso;
  agrega un parametro y puede dejar sin cubrir a los ejercicios mas ambiguos, que
  suelen ser los que mas interesan.
- [ ] Opcion C: **Ponderacion**: el castigo se atenua proporcionalmente a la
  confianza. Mas fino; produce puntajes dificiles de explicar y de reproducir.

**Answer**: Pendiente. Se especifica la Opcion A [ASSUMPTION]: es la unica que
mantiene el puntaje explicable a partir de dos datos observables del veredicto, y
no cierra ninguna puerta —la confianza queda registrada, asi que si mas adelante se
adopta B o C, los veredictos ya guardados alcanzan para decidir el umbral con datos
reales en lugar de a ojo.

<a id="DOUBT-PRESUPUESTO-DEFECTO"></a>
### Doubt[DOUBT-PRESUPUESTO-DEFECTO] - Cual es el valor por defecto del presupuesto por corrida?
**Status**: OPEN

[F-QINST-R006](#F-QINST-R006) acota las consultas nuevas por corrida y exige un
valor por defecto. El numero determina cuanto dura una auditoria "en frio" y
cuantas corridas hacen falta para cubrir los ~11.500 ejercicios del curso.

- [ ] Opcion A: **500 por corrida** — ~23 corridas para el curso completo.
  Equilibrio entre una espera tolerable y un avance perceptible.
- [ ] Opcion B: **100 por corrida** — la auditoria se mantiene rapida y el gasto
  por corrida es minimo, a costa de ~115 corridas para cubrir el curso.
- [ ] Opcion C: **Sin tope por defecto** — la corrida evalua todo lo pendiente
  salvo que el usuario acote. Cubre el curso de una vez; convierte cualquier
  auditoria en frio en una espera de horas con factura completa.

**Answer**: Pendiente. Se especifica la Opcion A [ASSUMPTION]. La Opcion C se
desaconseja explicitamente como **defecto**: la razon de ser del presupuesto es que
el costo no aparezca por sorpresa, y un defecto sin tope contradice eso aunque el
usuario pueda acotarlo despues. El valor exacto conviene ajustarlo tras medir
cuanto tarda una consulta real al juez.

<a id="DOUBT-ALCANCE-EJERCICIOS"></a>
### Doubt[DOUBT-ALCANCE-EJERCICIOS] - Que ejercicios entran en el analisis?
**Status**: RESOLVED

[F-QINST-R014](#F-QINST-R014) fija alcance total. Acotarlo ahorraria consultas
pagadas, y el denominador de la cobertura ([F-QINST-R005](#F-QINST-R005)) depende
de esta decision. Era la unica duda abierta que cambiaba **cuanto se paga**, asi
que se resolvio midiendo antes de la primera pasada completa.

- [x] Opcion A: **Todos los ejercicios del curso**, tengan o no instrucciones
  declaradas y esten o no marcados como "no oracion". Gramatica, significado y
  resolubilidad aplican siempre.
- [ ] Opcion B: **Solo los ejercicios que declaran instrucciones.** Sin consigna
  no hay cumplimiento de consigna que verificar. Ahorra consultas; deja sin
  revisar la resolubilidad de esos ejercicios, que nadie mas revisa.
- [ ] Opcion C: **Se excluyen ademas los ejercicios marcados como "no oracion"**,
  igual que hace el analisis de longitud. Ahorra mas; asume que un ejercicio que
  no es una oracion tampoco tiene consigna que incumplir, lo cual no es evidente.

**Answer**: Opcion A, y la medicion sobre el curso real la vuelve una decision
facil: **las dos alternativas no ahorran nada**.

| Medicion sobre el curso real (2026-07-29) | Valor |
|---|---|
| Ejercicios del curso | 11.487 |
| Sin instrucciones declaradas — lo que excluiria la Opcion B | 20 (0,17%) |
| Marcados como "no oracion" — lo que excluiria ademas la Opcion C | 0 |
| Ejercicios con mas de una opcion aceptada | 321 |

La Opcion B ahorraria **20 consultas de 11.487** y, a cambio, dejaria esos 20
ejercicios sin ninguna revision de resolubilidad: ningun otro analisis del sistema
la hace. Cambiar el alcance —y con el, el denominador de la cobertura— para
ahorrar el 0,17% del gasto no se justifica.

La Opcion C directamente **no excluye nada**: el curso no tiene contenido marcado
como "no oracion", asi que el criterio que el analisis de longitud necesita
([F-SLEN-R001](../2026-03-18.01_sentence-length/REQUIREMENT.md#F-SLEN-R001)) aca
no tendria efecto alguno. Adoptarlo seria agregar una exclusion que nunca se
aplica.

Como la Opcion A es la que ya estaba especificada, **ninguna regla cambia**:
[F-QINST-R014](#F-QINST-R014) solo deja de ser una asuncion y pasa a estar
respaldada por esta medicion.

**Nota para la fase de arquitectura**: la misma medicion mostro que las
instrucciones, el titulo y el nombre del topic figuran tambien en el propio
ejercicio, no unicamente en su knowledge. No altera lo que exige
[F-QINST-R009](#F-QINST-R009) —la huella cubre el contenido que el juez recibe,
venga de donde venga— pero si toca decidir **de donde se toma** cada dato al
armarlo; queda para [DOUBT-QUIZ-COMPLETO](#DOUBT-QUIZ-COMPLETO), que ya esta en
manos de la arquitectura.

## References

- **FEAT-EVCOST** — Capacidad general de resultados de evaluacion costosa
  reutilizables entre analisis: registro por huella de contenido, reuso, escritura
  incremental resistente a interrupcion, distincion entre resultado definitivo y
  falla transitoria, y convivencia de versiones del evaluador. Este analisis es su
  primer consumidor. Citada por [F-QINST-R006](#F-QINST-R006),
  [F-QINST-R007](#F-QINST-R007), [F-QINST-R008](#F-QINST-R008),
  [F-QINST-R010](#F-QINST-R010), [F-QINST-R013](#F-QINST-R013) y el Contexto.
- **FEAT-RPRES** — Establece que corregir un ejercicio cambia su contenido pero no
  su identificador; es la razon por la que la identidad del veredicto es la huella
  del contenido juzgado. Citada por [F-QINST-R009](#F-QINST-R009) y el Contexto.
- **FEAT-SLEN** — Define el comportamiento de agregacion de puntajes por la
  jerarquia del curso y el precedente de que un analisis que no puntua un nodo
  simplemente no participa de su promedio. Citada por
  [F-QINST-R002](#F-QINST-R002), [F-QINST-R004](#F-QINST-R004) y
  [F-QINST-R014](#F-QINST-R014).
- **FEAT-DSLEN** — Precedente de diagnostico tipado por ejercicio: exponer los
  datos que determinaron el puntaje, no solo el puntaje. Citada por
  [F-QINST-R003](#F-QINST-R003).
- **FEAT-QSENT** — Define las partes de la oracion de un ejercicio (texto fijo y
  hueco) y las opciones aceptadas de cada hueco, que son el contenido que el juez
  necesita completo. Citada por [DOUBT-QUIZ-COMPLETO](#DOUBT-QUIZ-COMPLETO).
