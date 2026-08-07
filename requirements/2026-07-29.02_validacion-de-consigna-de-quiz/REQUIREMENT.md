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
### Rule[F-QINST-R005] - El informe declara la cobertura del analisis y de que versiones del juez vienen sus veredictos
**Severity**: critical | **Validation**: VALIDATED

> El informe de auditoria expone, para este analisis, cuantos ejercicios alcanza,
> cuantos tienen veredicto (distinguiendo los evaluados en esta corrida de los
> reutilizados de corridas anteriores), cuantos quedaron pendientes y cuantos
> fallaron. Declara ademas **dos** datos de version, distintos y separados:
> **con que version del juez consulto esta corrida** —o que no consulto con
> ninguna— y **de que versiones provienen los veredictos que el informe puntua, con
> cuantos veredictos aporta cada una**. El puntaje del analisis se lee **siempre**
> junto a esa cobertura.

<details><summary>Detalle</summary>

| Dato de cobertura | Significado |
|---|---|
| Version consultada en esta corrida | La version vigente del juez, con la que se hicieron las consultas nuevas. Si la corrida no hizo ninguna consulta nueva —todo reutilizado, analisis sin presupuesto, o juez no disponible ([F-QINST-R010](#F-QINST-R010))— se declara que no hubo version consultada |
| Versiones de los veredictos puntuados, con su recuento | De donde viene cada veredicto que el informe puntua: una fila por version presente y cuantos veredictos aporta ([F-EVCOST-R001](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R001)) |
| Alcanzados | Ejercicios del curso que este analisis debe evaluar ([F-QINST-R014](#F-QINST-R014)) |
| Con veredicto | Ejercicios con puntaje en esta auditoria |
| — evaluados en esta corrida | Consumieron presupuesto ([F-QINST-R006](#F-QINST-R006)) |
| — reutilizados | Veredicto registrado por una corrida anterior ([F-QINST-R008](#F-QINST-R008)) |
| Pendientes | Sin veredicto por tope de presupuesto; los toma la corrida siguiente |
| Fallidos | El juez no llego a pronunciarse en esta corrida ([F-QINST-R007](#F-QINST-R007)) |

Sin los numeros de cobertura el puntaje es directamente **engañoso**: un 0,95 sobre
40 ejercicios evaluados de 11.500 se lee igual que un 0,95 sobre el curso entero, y
solo uno de los dos autoriza a concluir algo. Esta regla es la que hace legitima
la cobertura parcial de [F-QINST-R004](#F-QINST-R004): el analisis puede informar
sobre una parte del curso siempre que diga **cual**.

Dos sumas tienen que cerrar:

1. con-veredicto + pendientes + fallidos = alcanzados. Todo ejercicio alcanzado
   esta en exactamente uno de los tres estados.
2. La suma de los recuentos por version = con-veredicto. Todo veredicto puntuado
   declara de que version viene, y ninguno se cuenta dos veces.

**Por que dos datos de version y no uno.** Antes bastaba con uno: se declaraba la
version vigente y esa era, necesariamente, la de todos los veredictos puntuados,
porque los de otra version no se reutilizaban. Esa unicidad **ya no existe**: con la
version fuera de la identidad de un resultado
([F-EVCOST-R001](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R001)),
un mismo informe puntua veredictos emitidos por versiones distintas del juez, y es
el desenlace **normal** —no excepcional— apenas se retoca el juez una vez.

Las dos alternativas mas simples se descartaron por la misma razon:

- **Declarar solo la version vigente** seria hoy una afirmacion falsa sobre la
  mayoria de los veredictos del informe, y es exactamente el error silencioso que
  esta regla existe para impedir: los numeros de reutilizados se leen igual estando
  bien y estando mal.
- **Declarar el conjunto de versiones presentes, sin recuentos**, es honesto pero no
  accionable: "este informe mezcla tres versiones" no distingue un veredicto viejo
  de ocho mil, y esa diferencia es justo la que decide si conviene pedir una
  re-evaluacion ([F-QINST-R012](#F-QINST-R012)).

Con el recuento por version, el lector responde de un vistazo la unica pregunta que
importa: **cuanto de lo que estoy leyendo lo juzgo el juez actual, y cuanto lo juzgo
otro**. Y como nada se invalida solo
([F-EVCOST-R008](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R008)),
esa cifra es la unica señal de que conviene re-evaluar; sin ella, la decision de
re-evaluar seria a ciegas.

**Por que los dos datos se declaran separados.** Responden preguntas distintas: la
version consultada dice **con que criterio se juzgo lo nuevo de esta corrida**; los
recuentos por version dicen **de que criterios viene el puntaje que estoy leyendo**.
Mezclarlos en un solo dato reintroduce la confusion que la separacion evita, y la
version consultada puede no existir —una corrida que solo reutiliza no consulta
nada— mientras el informe puntua igual miles de veredictos.

**Criterio de aceptacion**: tras una corrida que evalua solo una parte del curso,
(a) el informe declara los cinco numeros y las dos sumas cierran; (b) declara la
version con la que consulto al juez, y una segunda corrida muestra crecer los
reutilizados y decrecer los pendientes; (c) con veredictos registrados por la
version A y una corrida que consulta con la version B, el informe declara B como
version consultada y declara dos filas de procedencia —A y B— cuyos recuentos suman
los ejercicios con veredicto; (d) una corrida que no consulta al juez ni una vez
declara que no hubo version consultada y sigue declarando la procedencia de los
veredictos que puntua.

**Error**: "La cobertura declarada del analisis de consigna no cierra: {conVeredicto} + {pendientes} + {fallidos} != {alcanzados}" / "Los veredictos declarados por version suman {suma} y el informe puntua {conVeredicto}" / "El informe declara puntajes de consigna sin decir de que versiones del juez provienen"

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

**Esta regla no dice cuando se considera que el juez no esta.** Una configuracion
invalida deja al juez no disponible y produce este mismo desenlace —la auditoria
termina igual, con la cobertura que alcanzo—, pero **cuales** configuraciones son
invalidas lo fija [F-QINST-R016](#F-QINST-R016), no esta regla: se juzga contra el
proveedor que la configuracion declara, y hay proveedores legitimos que no llevan
direccion de servicio ni credencial.

**Criterio de aceptacion**: interrumpido el servicio del juez tras evaluar K
ejercicios, la auditoria termina exitosamente, informa K con veredicto y el resto
como fallidos o pendientes, y una corrida posterior reutiliza esos K sin volver a
consultar.

**Error**: "La auditoria se interrumpio por una falla del juez de consigna" (condicion prohibida: la auditoria debe completarse)

</details>

<a id="F-QINST-R008"></a>
### Rule[F-QINST-R008] - Una corrida no vuelve a consultar por lo ya evaluado
**Severity**: critical | **Validation**: VALIDATED

> Ante un ejercicio cuyo contenido juzgado ya tiene veredicto registrado, la
> corrida lo reutiliza y **no** consulta al juez, **cualquiera sea la version del
> juez que lo emitio**. El presupuesto de la corrida se dedica integramente a los
> ejercicios sin veredicto, de modo que cada corrida avanza sobre lo que falta.

<details><summary>Detalle</summary>

Es el uso directo de la capacidad general de resultados costosos reutilizables
([F-EVCOST-R003](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R003)):
este analisis no define su propio mecanismo de reuso, lo consume.

La consecuencia observable es la reanudabilidad: N corridas sucesivas sin cambios
en el contenido cubren N veces el presupuesto de ejercicios distintos, nunca los
mismos dos veces. Sin esta regla, cada corrida volveria a empezar por el primer
ejercicio del curso y el avance seria nulo.

**El reuso no mira la version.** Un veredicto emitido por una version anterior del
juez se reutiliza exactamente igual que uno de la version corriente
([F-QINST-R010](#F-QINST-R010)): lo unico que cambia es la procedencia que el
informe declara ([F-QINST-R005](#F-QINST-R005)). Volver a consultar al juez por un
ejercicio con veredicto registrado —porque su version cambio— es un gasto que esta
regla prohibe.

**Criterio de aceptacion**: (a) dos corridas consecutivas con tope N sobre un curso
sin cambios evaluan 2N ejercicios **distintos**; el juez no recibe dos veces el
mismo contenido; (b) cambiada la version del juez entre las dos corridas, el
resultado es el mismo: la segunda corrida no consulta por ninguno de los N que ya
tenian veredicto.

**Error**: "Se consulto al juez por el ejercicio '{quizId}', que ya tenia veredicto registrado"

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
### Rule[F-QINST-R010] - La version del juez se registra y se declara; cambiarla no invalida ningun veredicto
**Severity**: critical | **Validation**: VALIDATED

> La version del juez —su forma de preguntar y la estructura de lo que devuelve—
> es la **procedencia** de cada veredicto: se registra junto a el y se declara en el
> informe ([F-QINST-R005](#F-QINST-R005)). No decide que veredictos valen. De ahi
> salen tres invariantes:
> 1. Cambiar la version **no borra, no invalida y no re-evalua** nada: los
>    veredictos ya emitidos se siguen reutilizando y puntuando, cualquiera sea la
>    version que los emitio, y cada uno conserva registrada la suya.
> 2. Volver a juzgar con el juez nuevo es una decision explicita del usuario
>    ([F-QINST-R012](#F-QINST-R012)) y es el **unico** mecanismo por el que un
>    veredicto se rehace; nunca un efecto colateral de haber tocado el juez.
> 3. Cuando el sistema **no logra derivar** la version a partir de la definicion
>    del juez, no la presenta como una version valida: no coincide con ninguna
>    version derivada de una definicion accesible, ningun veredicto nuevo se emite
>    ni se registra bajo ella, no se declara como version consultada, y esa corrida
>    trata al juez como **no disponible** ([F-QINST-R007](#F-QINST-R007)).

<details><summary>Detalle</summary>

Ajustar el juez es una tarea de iteracion normal y se hace muchas veces; volver a
juzgar 11.500 ejercicios es lento y caro. Si lo primero disparara lo segundo,
cada retoque del juez se convertiria en una factura sorpresa, y en la practica
nadie tocaria el juez.

**Que la version no gobierne la vigencia es una decision deliberada del usuario**,
tomada con su contra sobre la mesa: si un ejercicio cumple o no su consigna es una
propiedad del ejercicio, no de quien la juzgo, y cambiar de modelo no puede
invalidar 11.487 veredictos ya pagados. El costo aceptado es que, tras corregir un
juez que juzgaba mal, sus veredictos siguen puntuando hasta que alguien pida
re-evaluarlos. Es el comportamiento general de
[F-EVCOST-R001](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R001)
y
[F-EVCOST-R006](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R006)
aplicado a este juez; el razonamiento completo y las alternativas descartadas estan
en
[DOUBT-IDENTIDAD-VERSION](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#DOUBT-IDENTIDAD-VERSION).

**Lo que queda en lugar de la vigencia es la visibilidad.** Tras cambiar la version,
los ejercicios ya evaluados **no** vuelven a figurar como pendientes: siguen
puntuando. Lo que el informe muestra es de que version viene cada veredicto y
cuantos aporta cada una ([F-QINST-R005](#F-QINST-R005)), que es lo que le permite al
usuario decidir si pide una re-evaluacion y con que alcance. Conservar y comparar la
historia tiene ademas valor propio: es la unica forma de saber si un cambio del juez
lo mejoro.

**Por que la version que no pudo derivarse tiene que distinguirse (invariante 3).**
El razonamiento cambio de sustento pero no de conclusion. Ya no se trata de la
vigencia —nada invalida nada— sino de la **procedencia**, que tras esta decision es
la unica señal que el usuario tiene para decidir cuando re-evaluar. Si el sistema no
consigue acceder a la definicion del juez y aun asi produce una version con la misma
forma que una legitima, esa etiqueta falsa se estampa sobre los veredictos nuevos y
se cuela en los recuentos del informe: quedan indistinguibles de los emitidos por
una version real. Nadie lo detecta —no hay error, no hay diferencia visible— y el
unico instrumento que la invariante 1 deja en manos del usuario queda envenenado.
Peor aun, un veredicto asi es **permanente**: como nada se invalida solo, sobrevive
indefinidamente con una procedencia que no corresponde a ningun juez real.

Por eso el desenlace exigido sigue siendo el conservador y visible: esa corrida no
emite ni registra ningun veredicto nuevo, no declara esa version como version
consultada, y sus ejercicios sin veredicto quedan pendientes o fallidos en la
cobertura ([F-QINST-R005](#F-QINST-R005)) como ante cualquier otra indisponibilidad
del juez ([F-QINST-R007](#F-QINST-R007)). **Lo que si sigue ocurriendo es el reuso**:
los ejercicios con veredicto ya registrado se puntuan igual, declarando la version
real que los emitio — no depende de la version de la corrida, y negarles el puntaje
seria inventar una invalidacion que la invariante 1 prohibe. Se pierde el avance de
una corrida; lo que no se pierde es la garantia de que toda version que el informe
nombra corresponde a una definicion del juez que existio.

**Criterio de aceptacion**: (a) cambiada la version del juez, las corridas
posteriores **siguen reutilizando** los veredictos anteriores —el juez no recibe
ninguna consulta por ellos, ninguno de esos ejercicios queda pendiente— y el informe
los declara bajo la version que los emitio; (b) si la version no puede derivarse de
la definicion del juez, la corrida no emite ni registra ningun veredicto nuevo, no
declara esa version como version consultada, y los ejercicios sin veredicto
registrado quedan sin puntaje; (c) en ese mismo caso, los ejercicios que ya tenian
veredicto registrado se siguen puntuando y el informe declara la version real que
los emitio.

**Error**: "El cambio de version del juez de consigna elimino {cantidad} veredictos anteriores" / "El cambio de version del juez de consigna dejo de reutilizar {cantidad} veredictos ya registrados" / "Se registro un veredicto bajo una version del juez que no pudo derivarse de su definicion"

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
### Rule[F-QINST-R012] - La re-evaluacion es explicita, acotable, y el unico modo de rehacer un veredicto
**Severity**: major | **Validation**: VALIDATED

> El usuario puede pedir explicitamente que se vuelva a juzgar contenido que ya
> tiene veredicto registrado. Es el **unico** mecanismo por el que un veredicto se
> rehace. El pedido puede alcanzar todo el curso o una parte de el —delimitada por
> un area del curso o por un conjunto de ejercicios declarado en el propio pedido
> ([F-QINST-R018](#F-QINST-R018))—, respeta el presupuesto de la corrida
> ([F-QINST-R006](#F-QINST-R006)), no destruye los veredictos anteriores, y a
> partir de entonces el informe puntua el veredicto nuevo.

<details><summary>Detalle</summary>

Es la valvula de escape de [F-QINST-R008](#F-QINST-R008) y
[F-QINST-R010](#F-QINST-R010): sin ella, un veredicto registrado seria definitivo
para siempre y no habria forma de aprovechar una mejora del juez.

**Que sea el unico mecanismo la vuelve la regla mas importante de esta familia.**
Como cambiar la version del juez ya no invalida nada
([F-QINST-R010](#F-QINST-R010)), este pedido es la **unica** via por la que una
mejora del juez llega a los ejercicios ya juzgados. El dato que le dice al usuario
cuando conviene usarla es el recuento de veredictos por version del informe
([F-QINST-R005](#F-QINST-R005)).

Que respete el presupuesto no es un detalle: una re-evaluacion total sin tope seria
exactamente la factura sorpresa que [F-QINST-R010](#F-QINST-R010) evita. Con tope,
la re-evaluacion tambien es incremental — se pide una vez y avanza corrida a
corrida.

**Esta regla no fija como se delimita "una parte".** Las dos formas de acotar el
pedido —un area del curso, o un conjunto de ejercicios declarado por quien pide la
corrida— y que ocurre cuando se declaran las dos a la vez son materia de
[F-QINST-R018](#F-QINST-R018). Lo que esta regla exige vale igual para cualquiera de
ellas y no cambia con el alcance elegido: el pedido es explicito, respeta el tope,
no destruye los veredictos anteriores y el informe pasa a puntuar el nuevo.

**Criterio de aceptacion**: pedida la re-evaluacion, la corrida consulta al juez
por ejercicios que ya tenian veredicto registrado, sin exceder el presupuesto, los
veredictos anteriores siguen consultables, y el informe pasa a puntuar el veredicto
nuevo declarandolo bajo la version que lo emitio.

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

<a id="F-QINST-R015"></a>
### Rule[F-QINST-R015] - El analisis tiene un solo nombre: el que se lee es el que se escribe
**Severity**: critical | **Validation**: VALIDATED

> El vocabulario con el que el usuario nombra a este analisis es **unico**. El
> nombre con el que el analisis aparece en el informe de auditoria es exactamente
> el mismo con el que se lo excluye de una corrida
> ([F-QINST-R011](#F-QINST-R011)), se le fija el tope de consultas
> ([F-QINST-R006](#F-QINST-R006)) y se le pide re-evaluacion
> ([F-QINST-R012](#F-QINST-R012)). No existe un segundo nombre que el usuario
> deba averiguar.

<details><summary>Detalle</summary>

Las tres reglas anteriores describen **que** puede pedir el usuario, pero ninguna
fija **con que palabra** lo pide. Esta si, y por eso vale por separado: es la
unica que se rompe sin romper a las otras tres. Cada una de ellas funciona
perfectamente cuando el pedido llega; lo que falla es que el pedido llegue,
porque el usuario lo dirigio al unico nombre que el sistema le mostro.

**El defecto que la motiva es silencioso, y ese es el problema.** Verificado sobre
el curso real: el informe presenta el analisis bajo un nombre, pero al excluirlo,
acotarle el presupuesto o pedirle re-evaluacion con **ese** nombre el pedido no
tiene efecto alguno, porque internamente el analisis se busca bajo otro —el del
juez que lo respalda—. No hay error, no hay aviso y el informe se ve igual: quien
fija un tope de 10 consultas para acotar el gasto corre con el tope por defecto de
500 y se entera por la factura. Quien excluye el analisis para hacer una auditoria
rapida y gratuita paga la auditoria completa. Un pedido ignorado en silencio es
peor que un pedido rechazado: el rechazo se ve, la omision no.

| Lo que el usuario hace | Lo que espera | Lo que ocurre sin esta regla |
|---|---|---|
| Lee el informe y anota el nombre del analisis | Tener el nombre con el que operarlo | Tiene un nombre que solo sirve para leer |
| Lo excluye por ese nombre ([F-QINST-R011](#F-QINST-R011)) | El analisis no corre | Corre igual, y consulta al juez |
| Le fija un tope de N consultas ([F-QINST-R006](#F-QINST-R006)) | A lo sumo N consultas | Corre con el tope por defecto |
| Le pide re-evaluacion ([F-QINST-R012](#F-QINST-R012)) | Vuelve a juzgar lo ya juzgado | Reutiliza todo y no re-evalua nada |

**Por que se enuncia sobre este analisis y no sobre todos.** La propiedad es
general —los demas analisis del sistema ya se nombran igual en el informe y en las
opciones que los seleccionan, y la plataforma ya reconoce un nombre canonico
publicado por analisis
([F-CLIRV-R016](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R016))—
pero este es el primer analisis con **dos** nombres candidatos: el suyo y el del
juez que lo respalda. La coincidencia, que en los demas es un hecho, aca es una
exigencia. Enunciarla como invariante de toda la plataforma es deseable y no
pertenece a este requerimiento.

**Que NO exige esta regla**: no define como reacciona el sistema ante un nombre de
analisis desconocido. Que un pedido dirigido a un nombre inexistente se rechace en
lugar de ignorarse es una mejora legitima —y es lo que habria hecho visible este
defecto— pero es una propiedad de la interfaz de la auditoria sobre cualquier
analisis, no de este.

**Criterio de aceptacion**: tomando el nombre del analisis **tal como el informe lo
muestra**, y usando ese mismo nombre y ningun otro: (a) excluirlo deja al juez sin
ninguna consulta y al informe sin cobertura de consigna; (b) fijarle un tope de N
consultas hace que la corrida consulte a lo sumo N veces, y no el valor por
defecto; (c) pedirle re-evaluacion vuelve a consultar por contenido que ya tenia
veredicto registrado.

**Error**: "Se pidio '{opcion}' para el analisis '{nombre}' y la corrida lo ignoro: el informe publica ese analisis con ese mismo nombre" (condicion prohibida: un pedido dirigido al nombre publicado no debe quedar sin efecto)

</details>

<a id="F-QINST-R016"></a>
### Rule[F-QINST-R016] - El juez admite todo proveedor que el sistema sepa consultar; "invalida" se juzga contra el proveedor declarado
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El juez de consigna se configura con **las mismas clases de proveedor** que el
> resto del sistema ya usa para consultar modelos, y la validez de su configuracion
> se juzga **contra el proveedor que esa configuracion declara**. Tres invariantes:
> 1. Una configuracion que declara un proveedor **que se autentica con la sesion
>    local del usuario** —y que por eso no tiene direccion de servicio ni credencial
>    que configurar— es **valida**: el juez queda disponible, la corrida lo consulta
>    y sus veredictos puntuan ejercicios.
> 2. Una configuracion es **invalida** —y solo entonces el juez se declara no
>    disponible ([F-QINST-R007](#F-QINST-R007))— cuando le falta un dato que **su
>    proveedor necesita para poder dirigir la consulta**: no se puede determinar que
>    proveedor usar ([DOUBT-PROVEEDOR-DEL-JUEZ](#DOUBT-PROVEEDOR-DEL-JUEZ)), el
>    proveedor declarado no es uno que el sistema sepa consultar, o falta un dato que
>    esa clase de proveedor exige. La ausencia de un dato **que el proveedor
>    declarado no usa** nunca vuelve invalida a la configuracion.
> 3. Invalida y fallida se distinguen por **cuando**: con configuracion invalida la
>    corrida **no emite ni una sola consulta** al juez; un dato presente que el
>    proveedor rechaza recien al consultarlo —credencial vencida, servicio caido,
>    cuota agotada— no es configuracion invalida sino falla del juez
>    ([F-QINST-R007](#F-QINST-R007)).

<details><summary>Detalle</summary>

**El defecto que la motiva es silencioso, y por eso es grave.** Hasta esta regla, la
unica configuracion que dejaba al juez disponible era la que traia direccion de
servicio **y** credencial. El resto del sistema, en cambio, ya trabaja desde hace
tiempo con proveedores que se autentican con la sesion local del usuario —una
suscripcion ya iniciada en su maquina— y que por lo tanto **no tienen** direccion ni
credencial que configurar ([F-LAGEN-R008](../2026-04-29.01_laps-llm-quiz-candidate-generator/REQUIREMENT.md#F-LAGEN-R008),
[F-LAGEN-R009](../2026-04-29.01_laps-llm-quiz-candidate-generator/REQUIREMENT.md#F-LAGEN-R009)).
Con la configuracion real desde la que se opera el sistema, el juez se reportaria
**permanentemente no disponible**: la auditoria terminaria bien, sin un solo error
visible, y la cobertura declararia los 11.487 ejercicios sin veredicto corrida tras
corrida ([F-QINST-R005](#F-QINST-R005)). La feature entera quedaria inerte por una
razon que nadie ve. Un analisis que nunca puntua nada y nunca se queja es
indistinguible de uno que no existe.

**Que exige cada clase de proveedor.** La tabla es ilustrativa —enumera las clases
que el sistema sabe consultar hoy— y no es el enunciado; lo verificable es la
invariante 2: se exige lo que el proveedor declarado necesita, y nada mas.

| Clase de proveedor | Como se lo consulta | Datos que exige | Datos que no le aplican |
|---|---|---|---|
| Se autentica contra un servicio remoto con una credencial | Por su direccion de servicio | La direccion del servicio | — |
| Se autentica con la sesion local del usuario | Por la herramienta que el proveedor deja instalada en la maquina | Ninguno propio de la conexion | Direccion de servicio y credencial: **no existen** para esta clase, y exigirlas es exactamente el defecto que esta regla corrige |

**Por que la credencial no entra en la lista de exigidos.** Porque su ausencia no
hace imposible la consulta: hay servicios que no piden ninguna. Si falta y el
servicio la exige, la consulta se emite, el servicio la rechaza, y ese rechazo es
falla del juez ([F-QINST-R007](#F-QINST-R007)) — se cuenta como fallo en la
cobertura y la corrida siguiente vuelve a intentarlo. Ese desenlace es **mas**
visible que declarar al juez no disponible, no menos: el fallo aparece contado en el
informe en lugar de disolverse en un "pendiente" indistinguible del que produce el
tope de presupuesto ([F-QINST-R006](#F-QINST-R006)).

**Lo que esta regla NO relaja.** La garantia de [F-QINST-R007](#F-QINST-R007) —una
configuracion rota deja al juez no disponible sin tumbar la auditoria— sigue intacta
en su totalidad: sigue habiendo configuraciones invalidas (invariante 2) y siguen
produciendo exactamente el mismo desenlace. Lo unico que cambia es **cual** es el
conjunto de configuraciones invalidas: deja de ser "las que no traen direccion y
credencial" y pasa a ser "las que no traen lo que su proveedor necesita". Tampoco
cambia como se informa el resultado: la cobertura declara los ejercicios sin
veredicto igual que siempre ([F-QINST-R005](#F-QINST-R005)).

**Por que regla propia y no una extension de R007.** [F-QINST-R007](#F-QINST-R007)
responde "que pasa con la auditoria cuando el juez no esta"; esta responde "cuando
se considera que el juez no esta". Son preguntas separables: R007 se rompe si una
falla del juez aborta la auditoria, y esta se rompe si una configuracion legitima se
declara invalida — dos defectos distintos, con dos observaciones distintas. Ademas,
enunciarla dentro de R007 obligaria a que el enunciado de la resiliencia cargara con
el detalle de que exige cada clase de proveedor, que no tiene nada que ver con la
resiliencia.

**Criterio de aceptacion**: (a) con una configuracion que declara un proveedor que se
autentica con la sesion local del usuario y **no** trae direccion de servicio ni
credencial, la corrida consulta al juez y el informe declara ejercicios con veredicto
—no cero—; (b) con una configuracion que declara un proveedor que se consulta por
direccion de servicio y **omite la direccion**, la corrida no emite ninguna consulta,
la auditoria termina igual y los ejercicios quedan sin puntaje; (c) con una
configuracion que declara un proveedor que el sistema no sabe consultar, el desenlace
es el mismo que en (b); (d) la ausencia de credencial, por si sola, no produce el
desenlace de (b): la consulta se emite.

**Error**: "El juez de consigna se declaro no disponible por falta de '{dato}', que el proveedor '{proveedor}' no utiliza" (condicion prohibida) / "La configuracion del juez de consigna declara el proveedor '{proveedor}' y no puede consultarse: falta '{dato}'"

</details>

<a id="F-QINST-R017"></a>
### Rule[F-QINST-R017] - El incumplimiento detectado se convierte en trabajo: cada ejercicio que incumple entra al plan como tarea
**Severity**: critical | **Validation**: VALIDATED

> Todo ejercicio al que este analisis le asigno un puntaje **menor al maximo** —es
> decir, todo ejercicio con veredicto de incumplimiento— produce una **tarea** en el
> plan de refinamiento que se genere a partir de ese informe, bajo un tipo de
> diagnostico propio de este analisis y apuntando al ejercicio. Esas tareas se
> priorizan, se listan y se filtran por su tipo igual que las de cualquier otro
> analisis. Lo que esta regla **no** exige es que exista una correccion automatica
> para ellas.

<details><summary>Detalle</summary>

Sin esta regla el analisis termina en el informe y ahi se detiene. El plan de
refinamiento es la unica lista de trabajo del sistema —lo que se prioriza, se
recorre, se revisa y se marca como hecho—, asi que un problema que no llega al plan
no llega a nadie: el operador lee que hay ejercicios que incumplen su consigna y no
tiene por donde agarrarlos. Es el mismo salto que ya dieron los demas analisis, que
no se limitan a puntuar sino que alimentan el plan.

**Que ejercicios generan tarea y cuales no.** El criterio no es propio de este
analisis: en el plan, un nodo genera tarea cuando el puntaje de un analisis sobre el
es menor al maximo, y no la genera cuando es maximo
([F-RCLA-R001](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md#F-RCLA-R001)).
Proyectado sobre la escala de [F-QINST-R002](#F-QINST-R002):

| Estado del ejercicio en este analisis | Puntaje en el informe | Genera tarea |
|---|---|---|
| Veredicto de cumplimiento (severidad ninguna) | 1,0 | No: no hay nada que corregir |
| Veredicto de incumplimiento menor | 0,6 | Si |
| Veredicto de incumplimiento mayor | 0,3 | Si |
| Veredicto de incumplimiento critico | 0,0 | Si |
| Pendiente por tope de presupuesto | sin puntaje | No |
| Fallido: el juez no llego a pronunciarse | sin puntaje | No |

**Por que los pendientes y los fallidos no generan tarea, y por que eso es lo
correcto.** Esos ejercicios no tienen puntaje en el informe
([F-QINST-R004](#F-QINST-R004)): nadie los juzgo. Una tarea sobre un ejercicio no
juzgado seria **trabajo inventado** — le pediria a alguien que corrija un
incumplimiento que nadie afirmo que exista, y contaminaria la unica lista que el
operador usa para decidir que hacer a continuacion. Es la misma mentira que
[F-QINST-R004](#F-QINST-R004) evita en los promedios, trasladada al plan.

Lo que **si** existe para esos ejercicios es la declaracion explicita de que quedaron
sin mirar: la cobertura del informe los cuenta como pendientes o fallidos
([F-QINST-R005](#F-QINST-R005)). Ese es su lugar y es suficiente, porque lo que
corresponde hacer con ellos no es corregirlos sino **volver a evaluarlos**, y eso lo
hace la corrida siguiente sin que nadie tenga que anotarlo. Un plan que dijera "0
tareas de consigna" sobre un curso con 11.000 ejercicios pendientes seria enganoso
solo si se leyera sin la cobertura; leido junto a ella, dice exactamente lo que
corresponde: de lo que se juzgo, esto es lo que hay que arreglar.

**Que NO exige esta regla: la correccion automatica.** La regla se cumple con la
tarea **creada, priorizada y visible**. Producir una propuesta que reescriba el
ejercicio para que cumpla su consigna es alcance **futuro** y esta deliberadamente
afuera: es un trabajo grande y separable, y dejarlo pendiente no vuelve incompleta a
esta feature. El sistema ya tiene precedente de tipos de diagnostico accionables
**como informacion** antes de serlo como correccion: las tareas de distribucion de
frecuencia lexica y las de recurrencia de lemas se generan, se priorizan y se listan
igual que las demas, y pedirles una revision produce una propuesta que no cambia nada
([F-REVBYP-R004](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md#F-REVBYP-R004)).
Este analisis entra por esa misma puerta: primero el problema se ve y se prioriza,
despues se automatiza su correccion.

Cuando llegue esa correccion, no va a partir de cero: el diagnostico de cada
ejercicio ya deja registradas las violaciones con su restriccion incumplida y su
evidencia textual ([F-QINST-R003](#F-QINST-R003)), que es justamente lo que una
propuesta necesita para saber que corregir y donde.

**Criterio de aceptacion**: dado un informe en el que N ejercicios tienen veredicto de
incumplimiento —puntaje de consigna menor al maximo—, el plan generado a partir de
ese informe contiene exactamente N tareas de este tipo de diagnostico, una por
ejercicio y apuntando al ejercicio; pedir las tareas filtrando por este tipo de
diagnostico las devuelve
([F-CLIRV-R008](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R008));
y ni los ejercicios que cumplen, ni los pendientes, ni los fallidos aportan ninguna.

**Error**: "El informe declara {n} ejercicios con incumplimiento de consigna y el plan generado a partir de el no contiene ninguna tarea de ese tipo" (condicion prohibida: un incumplimiento con puntaje en el informe no debe quedar fuera del plan) / "El plan contiene una tarea de consigna para el ejercicio '{quizId}', que no tiene puntaje de consigna en el informe"

</details>

<a id="F-QINST-R018"></a>
### Rule[F-QINST-R018] - La re-evaluacion se puede acotar a un conjunto de ejercicios declarado en el pedido
**Severity**: major | **Validation**: AUTO_VALIDATED

> Quien pide la corrida puede acotar la re-evaluacion explicita
> ([F-QINST-R012](#F-QINST-R012)) declarando **el conjunto de ejercicios** que
> quiere volver a juzgar, en lugar de un area del curso. Tres invariantes:
> 1. **Se vuelve a juzgar el conjunto y nada mas.** Cada ejercicio del conjunto se
>    consulta al juez en esa corrida, tenga o no veredicto registrado; todo
>    ejercicio ajeno al conjunto se sigue reutilizando sin consulta
>    ([F-QINST-R008](#F-QINST-R008)).
> 2. **El conjunto no amplia el tope de la corrida** ([F-QINST-R006](#F-QINST-R006)),
>    pero el tope se gasta **primero** en el: los ejercicios del conjunto que el tope
>    no alcanza conservan su veredicto anterior, se siguen puntuando y **no** quedan
>    pendientes.
> 3. **Un pedido que no se puede interpretar se rechaza a la vista y sin gastar**:
>    conjunto vacio —que **no** equivale a "todo el curso"— o los dos alcances
>    declarados a la vez. En cambio, un identificador que no corresponde a ningun
>    ejercicio del curso **no** invalida el pedido: se informa, y el resto del
>    conjunto se vuelve a juzgar igual.

<details><summary>Detalle</summary>

**Lo que hoy no se puede pedir, y lo que cuesta no poder pedirlo.** Acotar la
re-evaluacion por area del curso es la unica forma que existe, y hay un pedido
perfectamente corriente que esa forma no sabe expresar: **volver a juzgar un conjunto
de ejercicios que no comparten area**. La medicion del caso que motivo la regla:

| Medicion sobre el curso real (2026-08-06) | Valor |
|---|---|
| Veredictos emitidos hasta hoy | 7.952, sobre 6.694 ejercicios distintos |
| Ejercicios con veredicto de incumplimiento | 966 |
| Knowledges en los que viven esos 966 | 221 |
| Ejercicios que suman entre todos esos 221 knowledges | 4.185 |
| Consultas de mas para volver a juzgar los 966 acotando por knowledge | 3.219 (77% del gasto) |

A las ~100 consultas por hora que rindio la corrida anterior, la diferencia entre
pedir 966 y pagar 4.185 son unas 32 horas de reloj y su factura, sin obtener a cambio
ni un veredicto mas de los que se querian.

**La regla no nombra ningun conjunto en particular, y eso es deliberado.** El caso que
la motivo —volver a juzgar los ejercicios marcados como incumplidores, ahora que el
juez juzga mejor, para ver cuales de esas marcas se caen— es un **uso**, no la
funcion. El conjunto lo declara quien pide la corrida y puede salir de donde sea: los
que quedaron marcados, veinte que alguien quiere mirar a mano, una seleccion hecha
desde el tablero. Fijar en la regla el criterio con el que se arma la lista la ataria
a un uso y obligaria a escribir otra regla para el siguiente.

**El identificador elige a quien se le vuelve a preguntar; no decide que veredicto
aplica.** [F-QINST-R009](#F-QINST-R009) queda intacta: la identidad de un veredicto
sigue siendo la huella del contenido juzgado y el identificador no interviene en ella.
Aca el identificador cumple otro papel, el de **seleccionar**: es el unico nombre
estable con el que quien pide la corrida —o el tablero que le arma la lista— puede
señalar un ejercicio. No hay contradiccion: se elige por identificador, y se juzga y
se registra por contenido. Como todo pedido dirigido a este analisis, se dirige ademas
por el nombre que el informe publica ([F-QINST-R015](#F-QINST-R015)).

**Los bordes, y por que cada uno se resuelve asi.** El criterio es siempre el mismo:
entre callar y avisar, avisar; y entre adivinar y rechazar, rechazar. Un pedido de
re-evaluacion mueve dinero, y el error caro de esta familia es el silencioso
([F-QINST-R015](#F-QINST-R015)).

| Caso | Desenlace | Por que |
|---|---|---|
| Un identificador del conjunto no corresponde a ningun ejercicio del curso | El resto del conjunto se vuelve a juzgar igual, la corrida termina sin error e **informa** esos identificadores | Rechazar el pedido entero por un identificador viejo dejaria inservible cualquier lista larga —basta que un ejercicio se haya borrado desde que se armo—; ignorarlo en silencio haria creer que se re-juzgo algo que nadie miro |
| Un ejercicio existe y nunca fue juzgado | Se consulta al juez y recibe puntaje y diagnostico como cualquier evaluado, sin error ni tratamiento especial | No hay nada que "rehacer", pero tampoco nada que objetar: el ejercicio estaba pendiente y el pedido simplemente lo adelanta. Exigir que el conjunto solo contenga ejercicios ya juzgados obligaria a quien arma la lista a saber de antemano cuales tienen veredicto |
| Conjunto vacio | Pedido rechazado: ninguna consulta y ningun veredicto rehecho | Es el borde peligroso. Leer "vacio" como "todo el curso" convierte una seleccion fallida en una re-evaluacion completa: exactamente la factura sorpresa que [F-QINST-R006](#F-QINST-R006) y [F-QINST-R010](#F-QINST-R010) existen para impedir. Leerlo como "no re-evalues nada" es gratis pero igual de malo: produce una auditoria normal, indistinguible de la que el usuario cree haber pedido |
| Conjunto **y** area del curso declarados a la vez | Pedido rechazado: ninguna consulta | Las dos lecturas posibles —interseccion y union— difieren en ordenes de magnitud de gasto, y una de ellas puede ser vacia. Elegir una es adivinar con la plata del usuario, y el desenlace de haber adivinado mal no se ve en ningun lado |
| El conjunto es mas grande que el tope de la corrida | Se atienden ejercicios del conjunto hasta agotar el tope, sin error; los que no se alcanzaron conservan su veredicto y se siguen puntuando | El tope es la unica proteccion contra el gasto no querido ([F-QINST-R006](#F-QINST-R006)) y un conjunto declarado no puede levantarlo. Que el tope se gaste **primero** en el conjunto es lo que hace que el pedido sirva de algo: sin esa prioridad, una corrida con ejercicios todavia sin juzgar puede agotar el tope antes de llegar al primero de los pedidos y devolver una corrida que no re-juzgo nada de lo que se le pidio |

**Que significa "rechazado".** El pedido se rechaza **antes de emitir ninguna consulta**
y la corrida no se ejecuta: es un pedido que el sistema no puede interpretar, no una
falla ocurrida a mitad de camino. No hay tension con [F-QINST-R007](#F-QINST-R007)
—que exige que una auditoria en curso termine igual ante una falla del juez—: aca no
llego a correr nada, no hay cobertura parcial que preservar, y el usuario corrige el
pedido y vuelve a pedir sin haber pagado un peso.

**Como se lee el resultado: no hace falta ningun dato nuevo en el informe.** Los
ejercicios del conjunto que se volvieron a juzgar cuentan como **evaluados en esta
corrida** y los demas como **reutilizados** ([F-QINST-R005](#F-QINST-R005)); y como el
tope se gasta primero en el conjunto, comparar los evaluados-en-esta-corrida contra el
tamaño del conjunto alcanza para saber si quedo cubierto entero o si conviene repetir
el pedido. Los identificadores que no correspondieron a ningun ejercicio **no entran
en ningun recuento de cobertura**: esos recuentos siguen contando ejercicios del curso
([F-QINST-R014](#F-QINST-R014)) y sus dos sumas siguen cerrando.

**Quien acota el alcance ya estaba repartido.**
[F-EVCOST-R008](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R008)
garantiza que volver a evaluar **uno** no arrastre a los demas, y deja en manos del
consumidor elegir **cuales**. Esta regla es exactamente ese "cuales" para el analisis
de consigna: no pide ninguna capacidad nueva de reuso, define un alcance nuevo.

**Por que regla propia y no un retoque de R012.**
[F-QINST-R012](#F-QINST-R012) responde "que garantias tiene volver a juzgar algo ya
juzgado": que es explicito, que respeta el tope, que no destruye nada. Esta responde
"como se delimita **que** se vuelve a juzgar". Se rompen por separado y se observan
distinto: R012 se rompe si una re-evaluacion destruye los veredictos anteriores o
ignora el tope; esta se rompe si el juez recibe una consulta por un ejercicio ajeno al
conjunto, o si un conjunto vacio termina re-evaluando el curso entero.

**Criterio de aceptacion**: sobre un curso con veredictos ya registrados y un conjunto
declarado de M ejercicios: (a) con un tope mayor o igual a M, el juez recibe
exactamente M consultas y **todas** son por ejercicios del conjunto —ningun ejercicio
con veredicto ajeno al conjunto genera consulta—, los veredictos anteriores siguen
consultables y el informe pasa a puntuar los nuevos; (b) con un tope N menor que M, la
corrida emite exactamente N consultas, todas por ejercicios del conjunto, termina sin
error, y los M-N restantes conservan su veredicto anterior, se siguen puntuando y no
figuran como pendientes; (c) si K identificadores del conjunto no corresponden a
ningun ejercicio del curso, la corrida vuelve a juzgar los que si corresponden,
informa esos K, termina sin error, y ningun recuento de cobertura los incluye; (d) si
el conjunto declara un ejercicio que existe y no tiene veredicto registrado, ese
ejercicio se consulta al juez y recibe puntaje y diagnostico como cualquier evaluado;
(e) con el conjunto vacio, o con un conjunto y un area del curso declarados a la vez,
el pedido se rechaza de forma visible, el juez no recibe ninguna consulta y ningun
veredicto registrado se rehace — en particular, no se re-evalua el curso entero.

**Error**: "El pedido de re-evaluacion declara un conjunto vacio de ejercicios" / "El pedido de re-evaluacion declara a la vez un area del curso y un conjunto de ejercicios: no se puede determinar el alcance" / "Se consulto al juez por el ejercicio '{quizId}', que ya tenia veredicto registrado y no pertenece al conjunto declarado" (condicion prohibida) / "Los identificadores {ids} del conjunto declarado no corresponden a ningun ejercicio del curso" (aviso: la corrida continua con el resto)

</details>

<a id="F-QINST-R019"></a>
### Rule[F-QINST-R019] - El conjunto se enumera en el pedido o se lee de un origen externo; un origen ilegible no es un conjunto vacio
**Severity**: major | **Validation**: VALIDATED

> El conjunto de ejercicios de [F-QINST-R018](#F-QINST-R018) se declara de dos
> formas: **enumerado dentro del propio pedido**, cuando son pocos y se escriben a
> mano, o **apuntando a un origen externo** del que el sistema lee la enumeracion,
> cuando son demasiados para escribirlos. Tres invariantes:
> 1. **Las dos formas declaran el mismo conjunto, y un conjunto es un conjunto.**
>    Todo lo que exige [F-QINST-R018](#F-QINST-R018) vale igual por cualquiera de
>    las dos vias, y dos corridas con el mismo conjunto declarado por vias distintas
>    son indistinguibles. Un ejercicio declarado mas de una vez cuenta **una**: ni se
>    consulta dos veces ni gasta dos veces el tope ([F-QINST-R006](#F-QINST-R006)).
> 2. **Un origen que no se puede leer rechaza el pedido con causa propia.** El
>    rechazo es el que ya fija [F-QINST-R018](#F-QINST-R018) —visible y sin emitir
>    ninguna consulta— pero **declara que el origen no se pudo leer y lo nombra**.
>    Nunca se presenta como conjunto vacio y nunca degrada a "no se declaro ningun
>    conjunto": un origen declarado y no leido jamas deja correr la corrida.
> 3. **Un origen legible del que no sale ningun identificador es un conjunto
>    vacio**, y se rechaza como tal, con el desenlace y la causa que ya fija
>    [F-QINST-R018](#F-QINST-R018).

<details><summary>Detalle</summary>

**Por que hace falta decir esto.** [F-QINST-R018](#F-QINST-R018) fija que el
conjunto se declara en el pedido y deliberadamente no dice **como**. El caso que la
motivo son 966 ejercicios: enumerados a mano son unos 24.000 caracteres, que nadie
escribe y ningun pedido transporta comodo. Sin una forma de apuntar a un origen que
ya tiene la lista armada, la regla anterior queda correcta e inutilizable
justamente para el caso que la justifico. Y a la inversa: obligar a un origen
externo para volver a juzgar tres ejercicios convierte un pedido de una linea en un
tramite. Por eso son dos formas y no una.

**Por que "no se pudo leer" y "vacio" tienen que verse distinto.** Los dos rechazan
la corrida sin gastar, asi que el desenlace se parece; lo que no se parece es **el
arreglo**. "No se pudo leer el origen" se corrige apuntando bien; "el conjunto esta
vacio" se corrige rehaciendo la seleccion, que suele estar mas arriba —en la
consulta o el tablero que armo la lista—. Presentarlos igual manda a revisar el
lugar equivocado, y ese es el error caro: quien cree que su seleccion no devolvio
nada empieza a dudar de los datos cuando lo unico que paso es que el origen no
estaba donde el pedido decia.

| Borde | Desenlace | Por que |
|---|---|---|
| El origen declarado no se puede leer: no esta donde el pedido dice, o el sistema no consigue leerlo | Pedido rechazado sin ninguna consulta, declarando que el origen no se pudo leer y nombrandolo | El conjunto **nunca se conocio**. Cualquier otra lectura —cero ejercicios, o "no se declaro conjunto"— afirma algo sobre una seleccion que el sistema no llego a ver |
| El origen se lee y no aporta ningun identificador | Pedido rechazado por conjunto vacio ([F-QINST-R018](#F-QINST-R018)) | Aca **si** se conoce la seleccion, y dice "nada". Es exactamente el caso que R018 ya resolvio, y darle un desenlace propio seria duplicar esa regla y arriesgar que las dos se contradigan |
| El mismo ejercicio declarado dos o mas veces | Cuenta una sola vez; ni error ni aviso | Una lista de cientos se arma pegando y concatenando, y los repetidos son su ruido normal. Rechazar por eso dejaria inservible cualquier lista larga —el mismo criterio con el que [F-QINST-R018](#F-QINST-R018) tolera los identificadores que no corresponden a ningun ejercicio— y consultar dos veces gastaria tope contra un veredicto recien emitido, que es lo que [F-QINST-R008](#F-QINST-R008) prohibe |
| Separadores de mas, espacios alrededor de un identificador, entradas en blanco | Se descartan al armar el conjunto y no cuentan como identificadores que no corresponden a ningun ejercicio | Mismo criterio que la fila anterior: es ruido de armado, no seleccion. Si tras descartarlo no queda ningun identificador, el conjunto es vacio y cae en la segunda fila |
| Las dos formas declaradas a la vez [ASSUMPTION] | Pedido rechazado sin ninguna consulta, declarando esa causa | Union y "una gana" son dos lecturas distintas del mismo pedido y nada dice cual se quiso. Es la misma familia que los dos alcances a la vez de [F-QINST-R018](#F-QINST-R018): entre adivinar con la plata del usuario y rechazar, rechazar |

**Precedente.** El sistema ya resolvio este mismo problema en otro pedido: un
contexto de correccion provisto por quien pide la revision se acepta escrito en
linea o desde un origen externo, precisamente porque hay contenidos que no entran
escritos a mano ([F-REVCTX-R001](../2026-05-09.01_revise-correction-context-override/REQUIREMENT.md#F-REVCTX-R001)).
Esta regla adopta esa misma dualidad y agrega lo que aquella no legisla: que ocurre
cuando el origen esta declarado y no se puede leer.

**Lo que esta regla no fija.** No fija con que palabras se escribe cada forma en el
pedido, ni de que clase es el origen externo mas alla de que quien pide la corrida
pueda nombrarlo y el sistema leerlo. Lo verificable son las tres invariantes: mismo
conjunto por las dos vias, rechazo con causa propia y distinguible, y ningun camino
por el que un origen no leido termine en una corrida.

**Criterio de aceptacion**: sobre un curso con veredictos ya registrados, (a) el
mismo conjunto de M ejercicios, declarado una vez enumerado en el pedido y otra vez
desde un origen externo, produce dos corridas indistinguibles: las mismas M
consultas, todas por ejercicios del conjunto; (b) un conjunto que declara N
identificadores distintos con repeticiones produce exactamente N consultas y consume
N del tope; (c) declarando un origen que no se puede leer, la corrida se rechaza
antes de emitir ninguna consulta y el rechazo nombra ese origen y dice que no se
pudo leer; (d) declarando un origen legible del que no sale ningun identificador, la
corrida se rechaza por conjunto vacio; (e) los desenlaces de (c) y (d) son
distinguibles entre si, y en ninguno de los dos la corrida sigue adelante: ni
auditoria normal, ni re-evaluacion por area, ni re-evaluacion del curso entero.

**Error**: "No se pudo leer el origen '{origen}' declarado para el conjunto de ejercicios a re-evaluar" / "El pedido de re-evaluacion declara a la vez los ejercicios y un origen del que leerlos: no se puede determinar el conjunto" / "Un origen declarado y no leido produjo el mismo desenlace que un conjunto vacio" (condicion prohibida) / "Un origen declarado y no leido se ignoro y la corrida siguio adelante" (condicion prohibida)

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
  - Un unico nombre del analisis: el que publica el informe es el que el usuario
    escribe para excluirlo, acotarlo y re-evaluarlo.
  - Presupuesto acotado de consultas nuevas por corrida y reanudacion en corridas
    posteriores.
  - Cobertura declarada: alcanzados, con veredicto, reutilizados, pendientes,
    fallidos, version consultada en la corrida y recuento de veredictos por version
    de procedencia.
  - Separacion entre falla transitoria y veredicto negativo legitimo.
  - Las clases de proveedor con las que el juez se puede configurar —incluidas las
    que se autentican con la sesion local del usuario, sin direccion de servicio ni
    credencial— y que datos debe traer una configuracion, segun el proveedor que
    declara, para que el juez se considere disponible.
  - Identidad del veredicto por contenido juzgado; la version del juez como
    procedencia registrada y declarada, y tratamiento de la version que no pudo
    derivarse de la definicion del juez.
  - Re-evaluacion explicita, unico mecanismo por el que un veredicto se rehace, y
    las dos formas de acotarla: un area del curso o un conjunto de ejercicios
    declarado por quien pide la corrida. Ese conjunto se enumera en el pedido o se
    lee de un origen externo que el pedido nombra, y un origen que no se puede leer
    se rechaza con causa propia, distinta de la del conjunto vacio.
  - La incorporacion de los ejercicios que incumplen al plan de refinamiento como
    tareas priorizables, listables y filtrables por su tipo de diagnostico.
- **Fuera de alcance**:
  - **El criterio con que el juez decide.** Que revisa, como pondera y como
    redacta sus violaciones ya esta definido y probado en el juez; este
    requerimiento consume su veredicto y no lo redefine.
  - **El mecanismo general de registro, reuso, resistencia a interrupcion e
    historia de versiones**, que especifica
    [FEAT-EVCOST](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md).
  - **Proponer o aplicar correcciones** a los ejercicios que incumplen. La tarea del
    plan se genera, se prioriza y se ve ([F-QINST-R017](#F-QINST-R017)), y el
    diagnostico ([F-QINST-R003](#F-QINST-R003)) deja la informacion necesaria para
    que una feature posterior produzca la propuesta; esa feature no es esta.
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
de haber cambiado la version del juez —que es reutilizar igual y declarar la
procedencia—. Cubre R001, R005, R010, R011, R012 y R014.

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
            then: version_cambiada

      - id: corre_por_omision
        action: "El informe incluye el puntaje, los diagnosticos y la cobertura del analisis de consigna sobre todos los ejercicios del curso"
        gate: [F-QINST-R001, F-QINST-R014]
        result: success

      - id: no_corre
        action: "El juez no recibe ninguna consulta, el informe no incluye puntaje, diagnostico ni cobertura de consigna, y los veredictos registrados quedan intactos"
        gate: [F-QINST-R011]
        result: success

      - id: reevalua_a_pedido
        action: "El sistema vuelve a consultar al juez por ejercicios que ya tenian veredicto registrado, sin exceder el tope de la corrida, los veredictos anteriores siguen consultables, y el informe pasa a puntuar el veredicto nuevo"
        gate: [F-QINST-R012]
        result: success

      - id: version_cambiada
        action: "Los veredictos de la version anterior del juez se siguen reutilizando y puntuando: ningun ejercicio ya evaluado queda pendiente, el juez no recibe consultas por ellos, y la cobertura declara cuantos veredictos aporta cada version"
        gate: [F-QINST-R005, F-QINST-R010]
        result: success
```

### Journey[F-QINST-J004] - Re-evaluar un conjunto de ejercicios declarado en el pedido
**Validation**: AUTO_VALIDATED

Cubre la segunda forma de acotar la re-evaluacion explicita: el conjunto de
ejercicios que declara quien pide la corrida. Recorre los cinco desenlaces que
distinguen la regla —el conjunto cubierto entero, el conjunto mas grande que el
tope, los identificadores que no corresponden a ningun ejercicio, el ejercicio del
conjunto que nunca fue juzgado y el pedido que no se puede interpretar—. Cubre R018,
con sus interacciones con R005, R006 y R008.

```yaml
journeys:
  - id: F-QINST-J004
    name: Re-evaluar un conjunto de ejercicios declarado en el pedido
    flow:
      - id: pedir_reevaluacion_del_conjunto
        action: "El usuario pide la auditoria de un curso cuyos ejercicios ya tienen veredicto registrado y declara en el pedido el conjunto de ejercicios que quiere volver a juzgar"
        outcomes:
          - when: "El conjunto declara ejercicios del curso y el tope de consultas de la corrida alcanza para todos"
            then: reevalua_el_conjunto
          - when: "El conjunto declara mas ejercicios que el tope de consultas de la corrida"
            then: reevalua_hasta_el_tope
          - when: "Parte de los identificadores declarados no corresponde a ningun ejercicio del curso"
            then: informa_los_no_correspondidos
          - when: "El conjunto declara un ejercicio que existe y nunca fue juzgado"
            then: juzga_el_nunca_juzgado
          - when: "El conjunto declarado esta vacio"
            then: pedido_rechazado
          - when: "El pedido declara a la vez un conjunto de ejercicios y un area del curso"
            then: pedido_rechazado

      - id: reevalua_el_conjunto
        action: "El juez vuelve a juzgar exactamente los ejercicios del conjunto y ninguno mas: los ejercicios con veredicto ajenos al conjunto se reutilizan sin consulta, los veredictos anteriores siguen consultables y el informe pasa a puntuar los nuevos"
        gate: [F-QINST-R018, F-QINST-R008]
        result: success

      - id: reevalua_hasta_el_tope
        action: "La corrida gasta el tope de consultas en ejercicios del conjunto y termina sin error; los ejercicios del conjunto que el tope no alcanzo conservan su veredicto anterior, se siguen puntuando y no figuran como pendientes"
        gate: [F-QINST-R018, F-QINST-R006]
        result: success

      - id: informa_los_no_correspondidos
        action: "La corrida vuelve a juzgar los ejercicios del conjunto que si corresponden, informa explicitamente los identificadores que no corresponden a ninguno y termina sin error; ningun recuento de cobertura incluye a esos identificadores"
        gate: [F-QINST-R018, F-QINST-R005]
        result: success

      - id: juzga_el_nunca_juzgado
        action: "El ejercicio se consulta al juez y recibe puntaje y diagnostico como cualquier ejercicio evaluado, sin error ni tratamiento especial por no haber tenido veredicto"
        gate: [F-QINST-R018]
        result: success

      - id: pedido_rechazado
        action: "El pedido se rechaza de forma visible antes de emitir ninguna consulta: el juez no recibe ninguna, ningun veredicto registrado se rehace y, en particular, no se re-evalua el curso entero"
        gate: [F-QINST-R018]
        result: failure
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

<a id="DOUBT-PROVEEDOR-DEL-JUEZ"></a>
### Doubt[DOUBT-PROVEEDOR-DEL-JUEZ] - Que proveedor se usa cuando la configuracion no declara ninguno?
**Status**: OPEN

[F-QINST-R016](#F-QINST-R016) juzga la validez de la configuracion **contra el
proveedor que declara**, y cuenta como invalida a la que no permite determinar cual
usar. Falta decidir si no declararlo es, en si mismo, invalido.

- [ ] Opcion A: **Se aplica el mismo proveedor por defecto que el resto del sistema
  ya aplica** cuando el operador no lo declara. Una maquina configurada de la forma
  habitual obtiene veredictos sin configuracion adicional, que es lo que hace cierta
  la ejecucion por omision de [F-QINST-R001](#F-QINST-R001). A cambio, una
  instalacion que solo trae direccion de servicio y credencial —sin declarar
  proveedor— se enruta al proveedor por defecto y no al que su operador imaginaba.
- [ ] Opcion B: **No declarar proveedor es configuracion invalida** y el juez queda
  no disponible. Obliga a que la eleccion sea explicita y no adivina nada; a cambio,
  una instalacion nueva arranca con el analisis inerte hasta que alguien lo
  configure, que es el sintoma que esta regla vino a eliminar.
- [ ] Opcion C (**descartada**): **inferir el proveedor de los datos presentes** —si
  hay direccion de servicio, remoto; si no, sesion local—. Es adivinar: una direccion
  olvidada de una prueba anterior enrutaria la corrida entera al proveedor
  equivocado, sin aviso, y volveria a producir un defecto silencioso de la misma
  familia que el que motivo la regla.

**Answer**: Pendiente. Se especifica la **Opcion A** [ASSUMPTION] por coherencia con
como ya se resuelve el proveedor en el resto del sistema
([F-LAGEN-R008](../2026-04-29.01_laps-llm-quiz-candidate-generator/REQUIREMENT.md#F-LAGEN-R008)):
un solo criterio de resolucion para todos los consumidores de modelos es preferible a
uno propio de este analisis. Cualquiera sea la eleccion, no cambia el enunciado de
[F-QINST-R016](#F-QINST-R016): cambia unicamente si "no declara proveedor" cae del
lado valido o del invalido de su invariante 2.

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
  falla transitoria, y convivencia y reuso de resultados de versiones distintas del
  evaluador con la version como procedencia registrada. Este analisis es su primer
  consumidor. Establece ademas que el recorte del alcance de una re-evaluacion lo
  define el consumidor —esta capacidad solo garantiza que volver a evaluar uno no
  arrastre a los demas—. Citada por [F-QINST-R005](#F-QINST-R005),
  [F-QINST-R006](#F-QINST-R006), [F-QINST-R007](#F-QINST-R007),
  [F-QINST-R008](#F-QINST-R008), [F-QINST-R010](#F-QINST-R010),
  [F-QINST-R013](#F-QINST-R013), [F-QINST-R018](#F-QINST-R018) y el Contexto.
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
- **FEAT-CLIRV** — Establece que cada analisis tiene un nombre canonico publicado
  por el sistema, y que ese es el nombre con el que se lo direcciona. Es el
  precedente del que [F-QINST-R015](#F-QINST-R015) es un caso: aca la coincidencia
  entre el nombre publicado y el nombre operable deja de ser un hecho y pasa a ser
  una exigencia. Define ademas el filtrado de las tareas del plan por su tipo de
  diagnostico. Citada por [F-QINST-R015](#F-QINST-R015) y
  [F-QINST-R017](#F-QINST-R017).
- **FEAT-RCLA** — Establece el criterio con el que un puntaje de un analisis sobre un
  nodo se convierte en tarea del plan: genera tarea el nodo cuyo puntaje es menor al
  maximo, y ninguno mas. Citada por [F-QINST-R017](#F-QINST-R017).
- **FEAT-REVBYP** — Establece que un tipo de diagnostico sin correccion dedicada cae
  a una revision de identidad: sus tareas se generan, se priorizan y se listan igual
  que las demas, aunque pedirles una revision no cambie nada. Es el precedente de que
  un diagnostico sea accionable como informacion antes de serlo como correccion
  automatica. Citada por [F-QINST-R017](#F-QINST-R017).
- **FEAT-LAGEN** — Establece que el operador declara el proveedor de modelo como
  parte de su configuracion y que ese proveedor identifica quien produjo cada
  resultado; es el precedente de que el sistema ya trabaja con proveedores que se
  autentican con la sesion local del usuario y no llevan direccion de servicio ni
  credencial. Citada por [F-QINST-R016](#F-QINST-R016) y
  [DOUBT-PROVEEDOR-DEL-JUEZ](#DOUBT-PROVEEDOR-DEL-JUEZ).
- **FEAT-QSENT** — Define las partes de la oracion de un ejercicio (texto fijo y
  hueco) y las opciones aceptadas de cada hueco, que son el contenido que el juez
  necesita completo. Citada por [DOUBT-QUIZ-COMPLETO](#DOUBT-QUIZ-COMPLETO).
- **FEAT-REVCTX** — Precedente de un dato que quien pide una corrida puede entregar
  de dos formas equivalentes —escrito en linea o desde un origen externo— porque hay
  contenidos que no entran escritos a mano. No legisla que ocurre cuando el origen
  no se puede leer, que es lo que agrega [F-QINST-R019](#F-QINST-R019). Citada por
  [F-QINST-R019](#F-QINST-R019).
