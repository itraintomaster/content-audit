---
feature:
  id: FEAT-MESA
  code: F-MESA
  name: La mesa por tema - uno propone, content-audit mide, los jueces juzgan y un script decide
  priority: critical
---

# La mesa por tema: uno propone, content-audit mide, los jueces juzgan y un script decide

## TL;DR

**Qué**: Dentro de una tanda, una mesa por tema corrige juntos los ejercicios del tema que necesitan propuesta: revalida,
uno propone, content-audit mide, los jueces del catálogo juzgan el ejercicio como queda y un script decide; varias mesas
corren a la vez.

**Por qué**: Hoy se corrige de a una tarea, con criterios que comparan contra el original y lo dan por bueno, a 17–35
ejercicios por hora; los unos 1.900 ejercicios que necesitan propuesta llevarían semanas y nadie juzgaría lo que queda.

## Recorrido de uso (estado actual)

Cómo quien corrige intenta hoy arreglar un ejercicio que un analizador marcó:

1. Pide una propuesta para una tarea → `ReviseCommand.revise(taskId, planId, …)`. ✓ Disponible, una tarea por vez, con un
   revisor por [tipo de diagnóstico](glossary:Tipo de diagnóstico). ⚠️ No hay revisor para los analizadores nuevos.
2. Corrige varias de una vez → `ReviseInstructionsCommand.reviseInstructions(planId, maxCorrections, coursePath)`. ✓
   Disponible sólo para la consigna, una tarea después de otra, con tope de 20 por corrida.
3. Mide un candidato → `AssessCandidateCommand.assessCandidate(planId, taskId, …)`. ✓ Disponible, pero ⚠️ sus criterios
   comparan contra el original ([F-QICOR-R004](#F-QICOR-R004)): la traducción «fiel» del candidato se mide contra la del
   original, que puede estar mal.
4. Corrige una opción múltiple → ⚠️ se rechaza siempre ([F-OPMUL-R006](#F-OPMUL-R006)).

**Punto de fricción**: pasos 1 a 4. No hay quien tome todos los errores de un ejercicio a la vez, ni jueces que lean el
ejercicio como queda, ni forma de correr varios temas en paralelo.

**Cambio mínimo necesario**: dentro de una tanda ([F-TANDA-R005](#F-TANDA-R005)), una mesa por tema toma las tareas de
mesa del tema, revalida, pide candidatos, los mide con los analizadores del catálogo, los hace juzgar por sus jueces y
decide con reglas fijas. `revise`, `revise-instructions` y `assess-candidate` siguen como hoy fuera de las tandas.

## Reglas de Negocio

Una **mesa** tiene cinco puestos y nadie juzga lo suyo: **propone** un agente, **mide** content-audit, **juzgan** los
jueces del catálogo, **refuta** quien busca un contraejemplo y **decide** un script. **Candidato** es el ejercicio
entero como quedaría: enunciado con su pista, respuestas u opciones, [traducción](glossary:Traducción), miniteoría y
consigna.

<a id="F-MESA-R001"></a>
### Rule[F-MESA-R001] - La mesa toma un tema entero, y un tema tiene una sola mesa
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una mesa toma todas las tareas de resolución *mesa* de un tema que están en la tanda. Invariantes:
> 1. por cada ejercicio con tareas produce candidatos del ejercicio entero, y cada candidato intenta resolver todas las
>    tareas de ese ejercicio a la vez;
> 2. una tarea de miniteoría pertenece al quiz que la originó y se trabaja con ese quiz, aunque otro muestre el mismo
>    texto ([F-TAREA-R003](#F-TAREA-R003));
> 3. un tema nunca tiene dos mesas a la vez, y cada mesa ve el tema con lo que ya aceptó en esa tanda.

Tres tareas de un ejercicio —pista, una sola respuesta y traducción, como el 11 de *Afirmativas regulares*— son un solo
candidato, no tres cambios que se pisan ([F-CDIFF-R012](#F-CDIFF-R012) marcaría no aplicable al segundo).

<a id="F-MESA-R002"></a>
### Rule[F-MESA-R002] - Antes de proponer, la mesa revalida
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Antes de pedir un candidato, la mesa vuelve a medir el ejercicio como está ahora con cada analizador que lee algo que
> sus tareas pueden cambiar, reutilizando los veredictos por la huella del contenido. Si el hallazgo de una tarea ya no
> está, esa tarea termina como «ya no se sostiene», sin candidato y sin gastar un intento; si no queda ninguna, el
> ejercicio no se toca.

Generaliza [F-QICOR-R009](#F-QICOR-R009) a todo analizador. Un ejercicio que nadie cambió reutiliza sus veredictos
([F-EVCOST-R003](#F-EVCOST-R003)) y no cuesta consultas; uno que una regla de la misma tanda ya tocó sí se vuelve a
juzgar. Esa medición del original es también contra la que se compara cada candidato ([F-NOEMP-R001](#F-NOEMP-R001)).

<a id="F-MESA-R003"></a>
### Rule[F-MESA-R003] - Quien propone ve el tema entero como lo ve el alumno
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Quien propone recibe, para cada ejercicio de su mesa: el tema entero como lo ve el alumno, con lo ya aceptado en la
> tanda; los hallazgos del ejercicio con su evidencia; el [nivel CEFR](glossary:Nivel CEFR); lo que cada tarea permite
> tocar ([F-TAREA-R006](#F-TAREA-R006)); el semáforo de palabras del tema ([F-SEMAF-R001](#F-SEMAF-R001)); el encargo de
> José sobre el tema, si lo hay ([F-MESA-R015](#F-MESA-R015)); y, desde el segundo intento, los motivos del rechazo
> anterior. Si el inglés y la traducción no dicen lo mismo, entrega dos candidatos: uno que cambia sólo la traducción y
> otro que cambia el inglés.

Los dos arreglos son la regla que dejó el 077 («si inglés y traducción no coinciden se arman los dos arreglos»): en las
verduras, cambiar sólo la traducción no alcanzó y cambiar el inglés sí. Quien propone nunca recibe veredictos de un
candidato que no sea suyo ni juzga lo que propone.

<a id="F-MESA-R004"></a>
### Rule[F-MESA-R004] - Content-audit mide cada candidato con la misma vara del análisis
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada candidato se mide con todos los analizadores instantáneos del catálogo sobre el ejercicio como queda, con la
> misma medición que `analyze`: el mismo candidato da los mismos hallazgos y puntajes que daría el ejercicio si
> estuviera en el curso.

Mantiene [F-QICOR-R015](#F-QICOR-R015) («los criterios se consultan, no se reimplementan») y extiende la consulta léxica
del análisis ([F-CLEX-R006](#F-CLEX-R006)): largo, palabras marcadas y las reglas del paquete A salen de la misma
medición que el curso.

<a id="F-MESA-R005"></a>
### Rule[F-MESA-R005] - Los jueces juzgan el ejercicio como queda, cada uno solo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo juez del catálogo que lee algo que el candidato cambió lo juzga, con su versión habilitada y en un contexto
> limpio: recibe sólo lo que lee en el curso, nunca el original, las razones de quien propone ni otros veredictos. Su
> veredicto queda por la huella de lo que lee y, si el candidato entra al curso, se reutiliza sin consultar. Un juez que
> no lee nada de lo que cambió no se consulta.

Mantiene [F-JEXAM-R005](#F-JEXAM-R005), [F-JEXAM-R003](#F-JEXAM-R003) (una versión sin examen aprobado no consulta) y lo
que lee cada juez ([F-TRSEN-R001](#F-TRSEN-R001), [F-UNICA-R001](#F-UNICA-R001), [F-MCVALE-R001](#F-MCVALE-R001),
[F-MINIT-R001](#F-MINIT-R001), [F-JEXAM-R007](#F-JEXAM-R007)). Si sólo cambia la traducción, juzgan los dos de
traducción y sentido; si cambia el inglés de un CLOZE, también el de una sola respuesta y el de consigna: cuatro
consultas, las que estimó el paquete C.

<a id="F-MESA-R006"></a>
### Rule[F-MESA-R006] - Decide un script: queda mejor que el punto de partida y no empeora nada
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La decisión no la toma un modelo: sale sólo de las mediciones y los veredictos. Cada candidato se compara con el
> ejercicio como estaba al empezar ese intento, su **punto de partida** ([F-MESA-R007](#F-MESA-R007)). Por cada tarea, el
> candidato la **resuelve** si el analizador que la marcó ya no marca ninguno de sus hallazgos; la **mejora** si no la
> resuelve pero, de sus hallazgos, alguno desaparece o baja de gravedad y ninguno sube, o —en una tarea de largo— el
> ejercicio queda más cerca del rango de su nivel. Un candidato se acepta si:
> 1. resuelve o mejora al menos una tarea del ejercicio que no es del largo ([F-MESA-R029](#F-MESA-R029));
> 2. no empeora nada del ejercicio ([F-NOEMP-R001](#F-NOEMP-R001) a [F-NOEMP-R004](#F-NOEMP-R004)) y ningún hallazgo que
>    ya tenía sube de gravedad;
> 3. si no resuelve ninguna tarea, tampoco aleja el largo de ningún ejercicio más de dos palabras de su rango
>    ([F-MESA-R029](#F-MESA-R029));
> 4. conserva el objetivo, la fórmula y los selectores didácticos del ejercicio ([F-MESA-R016](#F-MESA-R016)).
>
> Un hallazgo cuyo juez no evaluó el candidato no cuenta como desaparecido. Si se aceptan varios, gana el que resuelve
> las tareas más graves —una alta antes que dos bajas—; después el que deja menos hallazgos, por gravedad; el que no
> cambia lo que se escucha; el que deja el largo más cerca del rango; y si siguen empatados, el primero. En las dos
> primeras cuentas el largo no entra ([F-MESA-R029](#F-MESA-R029)). Cada motivo de rechazo dice, por tarea abierta, cómo quedó
> medida: el largo con su rango, o el hallazgo con su gravedad. Las mismas mediciones dan siempre la misma decisión, con
> sus motivos.

Es la vara absoluta de la propuesta («acepta si pasa los bloqueantes, no empeora nada y le gana al original»), no la
comparativa de [F-QICOR-R013](#F-QICOR-R013), que sigue valiendo en `revise-instructions`. El orden de desempate es
[ASSUMPTION]: prefiere no regrabar porque cada audio nuevo es trabajo fuera de content-audit.

Hasta el 25/10 la condición 1 pedía resolver una tarea entera, y la mesa tiraba lo que mejoraba sin terminar: en C2, C3 y
C4 la cámara (`…2de8`, A1) bajó de 18 a 10, 11 y 9 de largo, con meta de 3 a 8, sin sumar un error, y las tres
propuestas se descartaron; cada pasada volvió a partir de 18 (brief 214). La condición 3 deja en pie, para los arreglos
enteros, la decisión de José del 11/8 de que el largo no frena ([F-NOEMP-R003](#F-NOEMP-R003)); para un avance sí frena,
para no cambiar un problema por otro ([DOUBT-AVANCE-PARCIAL](#DOUBT-AVANCE-PARCIAL)).

<a id="F-MESA-R007"></a>
### Rule[F-MESA-R007] - Tres intentos, cada uno desde el mejor; hasta cinco mientras mejore
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada ejercicio tiene hasta tres intentos por pasada. El primero parte del ejercicio como está; cada intento siguiente
> parte del último candidato aceptado en esa pasada, y trabaja sólo las tareas que siguen abiertas. Invariantes:
> 1. si el último intento dejó el ejercicio mejor y le quedan tareas abiertas, tiene otro intento, hasta cinco en total;
>    un intento que no mejora no da otro;
> 2. un candidato rechazado vuelve a quien propone con lo que propuso, los criterios que no pasó y cómo quedó medida
>    cada tarea abierta ([F-MESA-R006](#F-MESA-R006));
> 3. un candidato aceptado que deja tareas abiertas vuelve igual, como punto de partida, con lo que mejoró y lo que falta;
> 4. al terminar, si hubo algún candidato aceptado, el ejercicio queda como lo dejó el último: un solo cambio desde como
>    estaba, con la compuerta contra ese original ([F-NOEMP-R001](#F-NOEMP-R001) a [F-NOEMP-R004](#F-NOEMP-R004)); sus
>    tareas resueltas quedan resueltas por la mesa, y las mejoradas, «mejoró, sigue abierta», con su medida de antes y
>    de después;
> 5. si no hubo ninguno, el ejercicio queda como estaba, sus tareas siguen abiertas y la bandeja recibe el original, el
>    candidato que menos falló con lo que no pasó y los motivos de cada intento;
> 6. si los jueces no coinciden ([F-MESA-R008](#F-MESA-R008)) después de un candidato aceptado, el ejercicio se queda
>    con lo aceptado, sin más intentos, y el desacuerdo queda entre los motivos;
> 7. la tarea del tema entero, la conversión que encargó José ([F-MESA-R015](#F-MESA-R015)) y un candidato que reescribe
>    la consigna del tema terminan al aceptarse, sin servir de punto de partida;
> 8. los intentos que ya gastó un cambio que volvió de la compuerta de la tanda cuentan dentro de los tres.

Mantiene el tope de [DOUBT-REINTENTOS](#DOUBT-REINTENTOS) de FEAT-QICOR (3) y el espíritu de
[DOUBT-SIN-CORRECCION](#DOUBT-SIN-CORRECCION): lo mejor que se logró no se tira, se muestra con lo que falla y decide
José ([F-BANDEJA-R003](#F-BANDEJA-R003)). A diferencia de `revise-instructions`, la mesa no lo acepta sola. Los dos
intentos de más son la decisión de José del 25/10: «son tres vueltas, pero si está viendo mejoras, que se hagan hasta 5,
a ver si se puede solucionar el problema en esa» ([DOUBT-AVANCE-PARCIAL](#DOUBT-AVANCE-PARCIAL)). Antes, cada intento
partía del original y el rechazo decía sólo «no resuelve ninguna tarea del ejercicio», sin la medida.

<a id="F-MESA-R008"></a>
### Rule[F-MESA-R008] - Si los dos jueces de un criterio no coinciden, decide José
**Severity**: major | **Validation**: AUTO_VALIDATED

> Si los dos jueces de traducción y sentido no coinciden sobre un candidato, ese candidato no se acepta ni se rechaza:
> va a la bandeja como desempate con los dos veredictos, y el ejercicio espera la respuesta de José sin gastar más
> intentos.

Es la decisión de José del 30/9 para el análisis ([F-TRSEN-R003](#F-TRSEN-R003)), aplicada igual al candidato: el
desacuerdo no se resuelve a favor de nadie.

<a id="F-MESA-R009"></a>
### Rule[F-MESA-R009] - La mesa no escribe el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo candidato aceptado queda como una [propuesta de revisión](glossary:Propuesta de revisión) pendiente de la tanda,
> con su artefacto; mientras la tanda no se publica, los archivos del curso quedan idénticos byte a byte.

Mantiene [F-REVBYP-R008](#F-REVBYP-R008), [F-REVBYP-R014](#F-REVBYP-R014) y [F-REVAPR-R007](#F-REVAPR-R007): el artefacto
guarda el antes y el después, y el curso cambia sólo al publicar ([F-TANDA-R009](#F-TANDA-R009)).

<a id="F-MESA-R010"></a>
### Rule[F-MESA-R010] - Varias mesas a la vez, sin pasarse de las consultas pedidas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una tanda corre hasta N mesas a la vez y, entre todas, hasta C consultas a modelos en curso. Invariantes:
> 1. nunca hay más de N temas abiertos ni más de C consultas en curso;
> 2. tras una falla del servicio no empieza ninguna consulta más; las que estaban en curso se registran si el modelo
>    respondió, y los ejercicios sin decisión quedan «sin intentar», sin gastar intentos;
> 3. con los mismos candidatos y veredictos, las decisiones son las mismas con cualquier N y C;
> 4. sin otro pedido, N es 4 y C es 4.

Mantiene [F-JEXAM-R006](#F-JEXAM-R006) para cada juez. José eligió 4 y 4 el 30/9, las mismas 4 consultas de los jueces, y
pasar a 8 y 8 sólo si la tanda de A1 corre sin cortes ([DOUBT-TEMAS-A-LA-VEZ](#DOUBT-TEMAS-A-LA-VEZ)): el cambio es un
pedido al abrir la tanda, no un valor nuevo por omisión.

<a id="F-MESA-R011"></a>
### Rule[F-MESA-R011] - Una opción múltiple sale de la mesa como opción múltiple, con una sola correcta
**Severity**: critical | **Validation**: VALIDATED

> Un candidato de una opción múltiple sigue siendo de opción múltiple, con exactamente una opción correcta y la misma
> cantidad de opciones; cambia la oración o la opción que sobra, y cada opción nueva, puesta en el hueco, no suma
> palabras rojas. Lo juzgan las reglas de `multiple-choice-validity` y su juez de «otra opción también vale».

Es «siempre una sola correcta, y la mesa rearma» (José, 30/9; [F-OMVAL-R001](#F-OMVAL-R001), [F-MCVALE-R007](#F-MCVALE-R007)).
Cambia [F-OPMUL-R006](#F-OPMUL-R006) dentro de las tandas, como decidió José el 30/9 en
[DOUBT-MC-REARMA](#DOUBT-MC-REARMA): las reglas de forma se aplican primero ([F-TANDA-R004](#F-TANDA-R004)) y la mesa
rearma lo que queda. Fuera de las tandas, F-OPMUL-R006 sigue valiendo.

<a id="F-MESA-R012"></a>
### Rule[F-MESA-R012] - La miniteoría se reescribe sólo en el quiz cuyo caso corrige
**Severity**: major | **Validation**: VALIDATED

> La tarea de miniteoría produce una reescritura específica para el caso del quiz que la originó. El candidato cambia la
> miniteoría de ese quiz y ninguna de otro quiz, aunque compartan el mismo texto. Si el quiz tiene además otras tareas,
> el candidato puede resolverlas juntas como cualquier ejercicio de [F-MESA-R001](#F-MESA-R001). El juez de miniteoría
> evalúa la tarjeta nueva contra ese quiz dentro del contexto actualizado del tema.

Mantiene [F-MINIT-R007](#F-MINIT-R007) y deja de ser una excepción a la preservación por afectar varios ejercicios: lo
no declarado en los demás quizzes se conserva ([F-RPRES-R003](#F-RPRES-R003)). Reemplaza la decisión del 30/9 por la
aclaración de José del 5/10 ([DOUBT-MINITEORIA-ALCANCE](#DOUBT-MINITEORIA-ALCANCE)).

<a id="F-MESA-R013"></a>
### Rule[F-MESA-R013] - Refutan los jueces que ya buscan un contraejemplo
**Severity**: major | **Validation**: VALIDATED

> Refutar un candidato es buscarle un contraejemplo, y lo hacen los controles que ya lo buscan: otra respuesta que
> también vale (el juez de una sola respuesta, o el de «otra opción también vale»), una palabra de otro nivel (la
> no-regresión léxica, [F-NOEMP-R002](#F-NOEMP-R002)) y una forma que un nativo no usaría (la condición 2 de traducción y
> sentido). Un contraejemplo que el original no tenía rechaza el candidato ([F-NOEMP-R001](#F-NOEMP-R001)).

La propuesta dibuja un quinto puesto, «el abogado del diablo». Ponerlo como agente aparte sumaba una consulta por
candidato, y José eligió el 30/9 que no haya agente nuevo ([DOUBT-QUIEN-REFUTA](#DOUBT-QUIEN-REFUTA)): si la muestra ciega
de A1 no cumple su meta, se vuelve a preguntar.

<a id="F-MESA-R014"></a>
### Rule[F-MESA-R014] - Fuera de una tanda, la corrección de hoy no cambia
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Sobre una tarea que no está en una tanda abierta, `revise`, `revise-instructions` y `assess-candidate` dan lo mismo que
> antes de este requirement: los mismos criterios, el mismo lazo de intentos y los mismos desenlaces.

Mantiene FEAT-QICOR entero, FEAT-LASAG ([F-LASAG-R007](#F-LASAG-R007)) y FEAT-KTLR. Las dos varas de «traducción fiel»
—la comparativa de la consigna y la absoluta de la mesa— conviven, como pidió el paquete C
([DOUBT-TRSEN-CATALOGO](#DOUBT-TRSEN-CATALOGO), opción A).

<a id="F-MESA-R015"></a>
### Rule[F-MESA-R015] - Un tema que José manda pasar a opción múltiple sale en opción múltiple
**Severity**: major | **Validation**: VALIDATED

> Si al abrir la tanda José encarga pasar un tema a opción múltiple, la mesa de ese tema entrega, para cada ejercicio de
> hueco para escribir, un candidato de opción múltiple que cumple:
> 1. la opción correcta es la primera respuesta aceptada de hoy, así que la oración que se escucha no cambia;
> 2. las otras opciones son contracciones distintas de la correcta, ninguna es la correcta en otra forma
>    ([F-OMVAL-R002](#F-OMVAL-R002)), y hay tres opciones en total [ASSUMPTION];
> 3. la traducción y la miniteoría no cambian; la consigna del ejercicio y la del tema pasan a pedir elegir, sin pedir
>    una forma, y la del tema se reescribe una vez.
>
> Lo juzgan las reglas de `multiple-choice-validity` y su juez de «otra opción también vale», y lo decide el mismo script
> ([F-MESA-R006](#F-MESA-R006)): se acepta si no empeora nada y cumple 1 a 3. Un ejercicio sin candidato aceptado queda
> como estaba y va a la bandeja como sin ganador.

Es la decisión de José del 30/9 para el tema «Contracciones» de A1 (40 ejercicios): «pasa a opción múltiple, con
contracciones distintas como opciones; lo arma la mesa». En la copia del curso del 29/9 son dos temas con ese nombre, de
20 ejercicios cada uno, todos de hueco para escribir: el de *Present Simple* y el de *Present Continuous*. Sus consignas
de hoy piden escribir («Escribe la $short form$ de lo que está entre paréntesis.»). Cambiar el tipo de ejercicio es una
excepción más a lo que un cambio conserva ([F-RPRES-R001](#F-RPRES-R001)), acotada a los temas encargados. Tres
opciones es el mínimo que contrasta contracciones; hoy las opciones múltiples de A1 tienen de 2 a 6
([DOUBT-OPCIONES-CONTRACCIONES](#DOUBT-OPCIONES-CONTRACCIONES)).

<a id="F-MESA-R016"></a>
### Rule[F-MESA-R016] - Resolver hallazgos no autoriza redefinir lo que practica el ejercicio
**Severity**: critical | **Validation**: VALIDATED

> El corrector conserva el objetivo de aprendizaje, la fórmula de transformación y las pistas que seleccionan la
> respuesta dentro de ese objetivo. Un hallazgo no autoriza eliminarlos, convertir la tarea en paráfrasis libre ni
> ampliar las respuestas para que desaparezca el error a costa de cambiar lo que aprende el alumno.

<details><summary>Preservación y criterios de aceptación</summary>

Se puede aclarar una restricción visible o reparar una pista defectuosa cuando la tarea lo permite, conservando la
decisión que debía practicar el alumno. Un cambio de objetivo requiere un encargo explícito del operador; no se
deduce del número de hallazgos ni de la preferencia de quien propone.

En el caso confirmado por José el 24/10/2026, «Polite request» selecciona `could`, y el pedido normal selecciona `can`
según la convención visible de la lección ([F-UNICA-R008](#F-UNICA-R008)). Quitar «Polite request» y aceptar ambos
modales no resuelve el ejercicio original: borra la distinción que enseñaba. Tampoco se agregan sin más variantes con
`please`, sinónimos u otras construcciones si incumplen una fórmula que la tarea hace visible. Si esa fórmula no es
visible, se informa la falta de indicación y se propone aclararla, sin inventar su contenido sólo desde la respuesta
guardada.

La miniteoría nueva conserva esa misma decisión ([F-MINIT-R012](#F-MINIT-R012)); una observación verdadera sobre
cortesía en el inglés general no justifica borrar la convención de la actividad. La propuesta y sus motivos deben
permitir distinguir una reparación del contenido de un cambio de objetivo pedido por el operador.

**Fuente**: observación y aclaración explícita de José del 24/10/2026 sobre la propuesta de
«Polite request: I want you to get me some tea.» y la fórmula específica de la lección `can/could`.

</details>

<a id="F-MESA-R017"></a>
### Rule[F-MESA-R017] - La pasada siguiente sabe lo que ya se intentó
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando se vuelve a corregir un ejercicio —en la misma corrección o en una corrección nueva que parte de la anterior—,
> quien propone recibe desde el primer intento lo que se intentó con ese ejercicio en las pasadas anteriores de esa
> cadena: por cada candidato, lo que propuso, cómo quedó medido y por qué no se aceptó o qué mejoró, los más recientes
> primero y hasta seis. Esos antecedentes no gastan intentos.

En C3 y C4 el corrector volvió a probar acortar la cámara sin saber que en C2 había llegado a 10 y no alcanzaba: sólo
recibía la devolución de la compuerta de la tanda. Seis alcanza para las dos pasadas anteriores con tres intentos cada
una [ASSUMPTION].

<a id="F-MESA-R018"></a>
### Rule[F-MESA-R018] - Una medida no se gana sacando la puntuación ni partiendo la línea
**Severity**: major | **Validation**: VALIDATED

> Antes de medir un candidato, y sin gastar consultas, la mesa lo compara con su punto de partida y lo rechaza si:
> 1. mete un salto de línea en lo que el alumno lee o escucha —el enunciado, las respuestas, las opciones, la
>    traducción o las oraciones guardadas— donde no lo había;
> 2. saca un signo de puntuación entre dos palabras que siguen juntas: «Yes, she is» → «Yes she is»;
> 3. pega una oración a la anterior sin el signo que las separaba: «… a camera here. You ___ …» → «No cameras You ___ …».
>
> Puede sacar una oración entera, con su signo, o cambiar las palabras; un salto de línea que el ejercicio ya tenía se
> puede quedar. Las pistas entre paréntesis no cuentan como oraciones del enunciado. El motivo del rechazo dice qué
> signo se perdió y entre qué palabras, o en qué parte entró el salto de línea.

En la primera corrida real de la mesa que guarda las mejoras (C2 del 25/10/2026), la cámara llegó a 8 tokens cambiando
el punto de «No cameras. You ___ use cameras.» por un salto de línea: el alumno leía «No cameras You ___ use cameras.».
Naturalidad lo anotó aparte de sus problemas y ninguna regla lo frenó. Probada sobre las 2.243 propuestas aceptadas de
la C1, la regla no frena ninguna; en la C2 frena sólo esa.

**Fuente**: brief 219; José aprobó la regla el 25/10/2026 («hacé la regla de puntuación y saltos de línea»).

<a id="F-MESA-R019"></a>
### Rule[F-MESA-R019] - Un ejercicio de transformar sigue siendo de transformar
**Severity**: major | **Validation**: VALIDATED

> En un tema de transformar (el modo de oración que reescribe), cuando el punto de partida da la oración de partida antes
> del hueco y el alumno escribe en el hueco la oración entera, la mesa rechaza antes de medir, y sin gastar consultas, el
> candidato que:
> 1. ya no da una oración de partida y deja un hueco para completar: «That house belongs to Mary. ___» → «That is ___
>    (Mary) house.»;
> 2. da la oración pero deja texto fijo alrededor del hueco, así que el alumno ya no escribe la oración entera: «I slept
>    today. (bad) ___» → «I slept today. (bad) I slept ___ today.».
>
> Puede cambiar las palabras de la oración de partida y de la respuesta. Entre la oración y el hueco puede haber una
> flecha o el rótulo que dice qué hacer («Polite request:»); las pistas entre paréntesis no cuentan como oración. Un
> ejercicio que en su punto de partida ya era de completar no se obliga a pasar a transformar. El motivo del rechazo cita
> la oración de partida y dice cuál de las dos cosas hizo el candidato.

En la Corrección 1 (C1, 7/10/2026), 34 ejercicios de temas de transformar pasaron a ser de completar y ningún juez lo
dijo: 20 de «El posesivo», 5 de «Oraciones con $let's$», 4 de «Adverbios de modo», 2 de «Pedidos: formal o informal» y
uno de «Cosas que pasan ahora», «$Before$ y $after$» y «Permiso general». El alumno ya no practicaba lo que pedía el
tema: escribía «Mary's» en vez de «That is Mary's house.». Simulada sobre los 319 ejercicios que transformaban en
producción, la regla frena justamente esos 34 y deja pasar los otros 285. Una oración de partida que sólo perdió su
punto sigue contando como oración cuando el hueco pide una oración entera, con mayúscula y su signo; la falta del punto
la dice [R018](#F-MESA-R018).

**Fuente**: brief 222; José, el 25/10/2026: «hacé … la regla para que transformar siga siendo transformar … Los de
transformar vuelven».

<a id="F-MESA-R020"></a>
### Rule[F-MESA-R020] - Lo que opinó una versión anterior de un juez se vuelve a preguntar antes de corregir
**Severity**: major | **Validation**: VALIDATED

> Cuando se vuelve a corregir un ejercicio y un juez de ejercicios lo opinó con una versión anterior a la de hoy, y la de
> hoy aprobó su examen, la corrección primero le hace la misma pregunta a la versión de hoy y corrige sólo lo que esa
> versión sigue viendo. Invariantes:
> 1. es una consulta por ejercicio y juez, dentro del tope de la corrida; quien propone gasta lo que queda;
> 2. la estimación las cuenta en el mínimo de consultas y dice cuántas son y de qué juez;
> 3. una opinión de la versión de hoy no se vuelve a preguntar, ni la de un juez cuya versión de hoy no aprobó;
> 4. un juez de temas (que lee el tema entero) no se vuelve a preguntar por ejercicio: queda como está.

En la primera corrida de la muestra de 11 ejercicios (25/10/2026), 25 de las 34 tareas venían del juez de una sola
respuesta en una versión que reprobó su examen, y casi todas eran paráfrasis que la versión aprobada ya no cuenta:
«She is drinking coffee right now.», «Let's talk later on.», «In the museum, you can't take videos.». Quien propone gastó
las 120 consultas persiguiéndolas: cambió ejercicios que estaban bien y dejó 6 de los 11 sin tocar.

**Fuente**: brief 223; José, el 25/10/2026: «lo primero es llegar a que todos los jueces den bien, y que el corrector
corrija bien».

<a id="F-MESA-R021"></a>
### Rule[F-MESA-R021] - Lo que el alumno lee en español va de tú
**Severity**: major | **Validation**: VALIDATED

> Antes de medir un candidato, y sin gastar consultas, la mesa lo rechaza si escribe de vos lo que cambia de lo que el
> alumno lee en español: la consigna del ejercicio o del tema, la traducción o la miniteoría. Invariantes:
> 1. de vos son el presente («podés», «decidís», «necesitás»), el imperativo con el acento al final donde empieza una
>    frase («Usá», «añadí», «Creá», «poné») y el imperativo con pronombre sin tilde («agregalo», «fijate»);
> 2. no lo son el pasado ni el futuro («Escribí tres oraciones», «dibujará», «sabrás») ni las palabras que sólo terminan
>    igual («más», «inglés», «cortés», «después», «país», «está»); el imperativo con acento se busca sólo en la consigna
>    y la miniteoría, porque en una traducción «Escribí» es el pasado; y el que termina en «é» o «í», que se escribe como
>    el pasado de «yo» («Compré», «Escuché»), sólo al empezar o después de «.», «,», «;», «:» o «y», no después de un
>    ejemplo en inglés o de un paréntesis, donde la tarjeta lo traduce («$I heard a funny noise.$ = Escuché un ruido
>    raro.»);
> 3. el inglés entre `$` no se lee;
> 4. un texto que el candidato dejó como estaba no es suyo: no lo rechaza por eso;
> 5. el motivo dice qué parte va de tú y qué palabras dice de vos.
>
> Lo que la mesa misma agrega a la consigna también va de tú («Usa exactamente: …»).

Las consignas de A1 son de tú: 2.062 de 2.102. La Corrección 1 escribió las tarjetas de vos (392 de 537), porque las
instrucciones de quien propone le hablan de vos y las imita. Después de la C1, 597 ejercicios de A1 dicen algo de vos: 507
en la tarjeta, 82 en la consigna y 56 en la traducción. Las formas más comunes son «usá» (269), «elegí» (84), «añadí» (45)
y «decidís» (42).

**Fuente**: brief 223, pregunta 1; José, el 25/10/2026: «Coincido con tus recomendaciones» (de tú, como las consignas).

<a id="F-MESA-R022"></a>
### Rule[F-MESA-R022] - Por el largo sólo se sacan palabras
**Severity**: major | **Validation**: VALIDATED

> Cuando las únicas tareas de un ejercicio que dejan tocar la oración —el enunciado o las respuestas— son del largo, la
> mesa rechaza antes de medir, y sin gastar consultas, el candidato que pone una palabra que la oración no tenía, o que
> la pone más veces, o que cambia el orden de las que quedan. Lo que se compara es lo que el alumno lee y escribe: el
> enunciado sin las pistas y la primera respuesta de cada hueco. Puede sacar palabras y cambiar mayúsculas y signos.
> Si otra tarea deja tocar la oración (una palabra de otro nivel, otra respuesta válida), la regla no se aplica. El motivo
> dice qué palabras puso.

En la C2 del 25/10/2026, para bajar de 9 a 8 palabras, quien propone cambió «He went to the bar after he left work.» por
«He left the bar after he ate lunch.», y «It's not okay to take videos in the museum.» por «It's not okay to sing.». La
primera estaba bien y la segunda perdió el lugar: «No se puede cantar» queda como una prohibición sin dónde. Quien
propone también lo lee en sus instrucciones: si sacando palabras no llega a la meta sin perder la situación, deja la
oración y lo dice.

**Fuente**: brief 224, pregunta 2; José, el 25/10/2026: «hacé … el arreglo de largo y vocabulario con tus
recomendaciones del 224» («No: sólo sacar una palabra»).

<a id="F-MESA-R023"></a>
### Rule[F-MESA-R023] - Una palabra de otro nivel se cambia sólo por otra que diga lo mismo
**Severity**: major | **Validation**: VALIDATED

> Quien propone cambia una palabra de otro nivel sólo por otra del nivel que dice lo mismo en la oración: la misma acción,
> el mismo objeto, el mismo lugar. Si ninguna lo conserva, deja la palabra y lo explica: es mejor una palabra de otro nivel
> que un ejercicio que dice otra cosa. Una respuesta que un juez pidió aceptar vale para la oración que juzgó: si cambia la
> oración de partida, vuelve a pensar cada respuesta pedida contra la nueva.

Es una instrucción a quien propone, no un control: que la palabra nueva diga lo mismo no se puede medir sin un modelo.
Se mira en la prueba ancha, ejercicio por ejercicio. En la C2 del 25/10/2026, «borrow» (A2) pasó a «use» («¿Me prestas tu
libro?» → «¿Puedo usar tu libro?») y «order» (A2) a «buy»; en «How about Monday?», quien propone sumó «Let's go Monday.»,
que el juez de una sola respuesta había pedido para la oración vieja, a «How about going on Monday?».

**Fuente**: brief 224, pregunta 1; José, el 25/10/2026: «borrow» y «order» se quedan en Pedidos («Sí, son las del tema»).

<a id="F-MESA-R024"></a>
### Rule[F-MESA-R024] - La calidad didáctica A1 se corrige en los temas que se corrigen
**Severity**: major | **Validation**: VALIDATED

> Lo que el juez de calidad didáctica A1 encuentra en un ejemplo es un problema que se corrige en la mesa, no sólo
> contexto para quien propone: se arregla cambiando el ejemplo —enunciado, pista, respuestas u opciones, con su
> traducción, su oración guardada y su audio detrás—, nunca la consigna del tema ni la miniteoría. Antes de corregir un
> ejercicio de A1 que ese juez todavía no vio, se le pide su opinión, con una consulta dentro del tope, leyendo los
> encabezados de los temas anteriores como los lee el análisis. Un ejercicio de A1 sin otro problema también se puede
> elegir para eso: si el juez no encuentra nada, no da trabajo. Los demás jueces que corrigen siguen como en
> [F-MESA-R020](#F-MESA-R020): sólo vuelven a preguntar lo que opinó una versión anterior.

El 25/10/2026 la tanda A1 tenía 2.402 ejercicios y ninguno con opinión de este juez: sólo había validado propuestas.
Así pasaron «Those are the sons' Saturdays.» y «That is the waiters' water.», que pragmatismo y uso nativo aprueban y de
las que este juez dijo que «exige imaginar un calendario». Para un tema entero se pide la opinión de todos sus
ejercicios antes de la tanda, con una consulta por ejercicio.

**Fuente**: brief 223, pregunta 2; José, el 25/10/2026: «Sí, en esos temas».

<a id="F-MESA-R025"></a>
### Rule[F-MESA-R025] - Lo que ya está de vos se corrige
**Severity**: major | **Validation**: VALIDATED

> Un analizador de errores lee la consigna, la miniteoría y la traducción de cada ejercicio y marca cada una que tenga
> palabras de vos, con la misma lectura que [F-MESA-R021](#F-MESA-R021): hallazgo *medio* para la *mesa*, que la vuelve
> a escribir de tú sin tocar el inglés del ejercicio. Un ejercicio con alguna tiene puntaje 0; sin ninguna, 1.

[F-MESA-R021](#F-MESA-R021) sólo frena el vos que escribe quien propone; un texto viejo que no toca queda como estaba. El
25/10/2026 la primera corrección había dejado de vos 392 de las 537 tarjetas que escribió y 40 de las 2.102 consignas
de A1, como «Creá un adverbio… y agregalo a la oración». Sin este analizador, una corrida completa los dejaba así.

**Fuente**: brief 223, pregunta 1; José, el 25/10/2026: lo que lee el alumno va de tú, como las consignas.

En lo publicado (la copia del 29/9) hay 699 ejercicios con palabras de vos: 231 de A1, 171 de A2, 269 de B1 y 28 de B2,
529 en la miniteoría y 170 en la consigna («Colocá», «hablás», «preferís», «Mirá»), ninguna en la traducción.

<a id="F-MESA-R026"></a>
### Rule[F-MESA-R026] - La oración que guarda producción sigue al ejercicio
**Severity**: major | **Validation**: ASSUMPTION

> La oración guardada en la copia de producción ([F-DATOS-R001](#F-DATOS-R001)) la cambia publicar, no la corrección.
> Invariantes:
> 1. volver a corregir no hace una tarea de ella ni se la pasa como contexto a quien propone; el ejercicio que no tiene
>    otro problema no se corrige y dice que sólo falta publicar, y el hallazgo sigue a la vista;
> 2. cuando una tanda ya la tiene como tarea, se resuelve con el candidato que la mesa acepta si la oración guardada del
>    propio ejercicio está bien en él: al publicarse, el candidato lleva esa oración a producción;
> 3. esa tarea no hace mejor a un candidato: uno que sólo la «resuelve» no se acepta;
> 4. si el candidato deja marcada la oración guardada del ejercicio, la de producción sigue abierta.

La copia de producción no cambia hasta publicar, así que su hallazgo sigue en cada candidato. El 26/10 «She drinks
coffee in the morning. (sometimes) ___» (`…2578`, A1) guardaba «She drinks coffee in the morning. She sometimes drinks
coffee in the morning.» en el árbol y en producción; el primer intento la dejó en «She sometimes drinks coffee in the
morning.», y la mesa gastó dos intentos más buscando arreglar lo que sólo arregla publicar, y terminó diciendo que no
lo había resuelto. Es lo que ya hacen las compuertas, que la cuentan como pendiente y no como error nuevo
([F-NOEMP-R004](#F-NOEMP-R004)). En la prueba ancha de esa noche, «Does he need water? Yes, he ___ .» (`…1f57`) tenía
bien la oración del árbol desde la primera corrección y la vieja en producción: el candidato que arregló la miniteoría
no la resolvía y la mesa gastó dos intentos más. Por eso alcanza con que la del árbol esté bien en el candidato. En
la C2, 19 de los ejercicios corregibles no tenían otro problema que ése: cada uno habría ido a la mesa a gastar tres
intentos sin que ninguno pudiera aceptarse.

**Fuente**: prueba del 26/10/2026 con tres ejercicios de A1 y prueba ancha de la misma noche.

<a id="F-MESA-R027"></a>
### Rule[F-MESA-R027] - Lo viejo de vos que la mesa toca pasa a tú por regla
**Severity**: major | **Validation**: ASSUMPTION

> En cada ejercicio que la mesa propone cambiar, la consigna, la miniteoría y la traducción que quien propone deja
> como estaban se escriben de tú: cada palabra de vos que reconoce [F-MESA-R021](#F-MESA-R021) y que está en la tabla
> del curso pasa a la forma de tú, con su tilde («Creá… y agregalo» → «Crea… y agrégalo», «Poné» → «Pon», «podés» →
> «puedes»). El inglés entre «$» no se toca y una palabra que la tabla no tiene queda como estaba. Invariantes:
> 1. el cambio que se guarda en la tanda es el mismo que la mesa midió;
> 2. un texto que sólo pasó a tú no es un cambio que las tareas del ejercicio tengan que permitir, ni vos que haya
>    escrito quien propone;
> 3. lo que quien propone escribe sigue [F-MESA-R021](#F-MESA-R021).

El 26/10/2026, de los 1.856 ejercicios de A1 que cambió la primera corrección, 549 tienen vos, 507 en la miniteoría; 421
de ellos van a la mesa por otro problema. Con esta regla salen de tú sin pedir nada más que la opinión del juez del
texto que cambió. Los otros 128 (120 sin ningún otro problema) necesitan que la tanda mida
[F-MESA-R025](#F-MESA-R025): la C2 mide los 21 analizadores con que se armó la C1, el 7/10.

**Fuente**: José, el 25/10/2026: lo que lee el alumno va de tú, como las consignas (brief 223, pregunta 1).

<a id="F-MESA-R028"></a>
### Rule[F-MESA-R028] - Un analizador instantáneo que llegó después de la tanda igual da tareas
**Severity**: major | **Validation**: ASSUMPTION

> Al estimar y al corregir, un analizador instantáneo de errores del catálogo, con corrección propia, que la tanda no
> mide —llegó después de que se armó— se corre gratis sobre el curso como está y sus hallazgos dan tareas como los de
> los demás. La tanda sigue comparando con los analizadores con que se armó: el nuevo no entra en la comparación con la
> base ni en la compuerta de la tanda.

La C2 nació de la C1 con sus 21 analizadores (7/10/2026), así que [F-MESA-R025](#F-MESA-R025) no le marcaba nada.
Sumarlo a lo que la tanda compara inflaría «algún error» contra la base, que nunca lo midió. Con esta regla los 128
ejercicios de vos de la C2 que no iban a la mesa también van.

**Fuente**: prueba del 26/10/2026; consecuencia de [F-MESA-R025](#F-MESA-R025).

<a id="F-MESA-R029"></a>
### Rule[F-MESA-R029] - El largo es leve: no justifica un cambio ni lo decide
**Severity**: major | **Validation**: VALIDATED

> El largo es la medida menos importante de un ejercicio: pesa menos que una oración bien escrita y que respetar la
> consigna. Por eso:
> 1. Un ejercicio cuyas únicas tareas son del largo no se corrige: queda medido, con su hallazgo, y la estimación dice
>    «Sólo el largo: un cambio que sólo acorta no alcanza para corregir». Si en la mesa sólo le quedan tareas del
>    largo, no se intenta y lo dice, sin gastar consultas.
> 2. Un candidato que sólo acorta no se acepta: resolver o acercar al rango una tarea del largo no es el avance de
>    [F-MESA-R006](#F-MESA-R006) (cond. 1), y el motivo dice «sólo acorta». Cuando un candidato aceptado deja abiertas
>    sólo tareas del largo, la mesa no hace otro intento.
> 3. Entre candidatos aceptados gana el que resuelve las tareas más graves —la gravedad de una tarea es la de su hallazgo
>    más grave al empezar—; después, el que deja menos hallazgos por gravedad. En las dos cuentas el largo no entra:
>    sólo desempata al final.
> 4. Un candidato que mejora otra tarea puede alejar el largo hasta dos palabras de su rango; más, no (cond. 3 de
>    [F-MESA-R006](#F-MESA-R006)).
> 5. Quien propone lee la tarea del largo como leve: acorta sólo si la oración no pierde nada de lo que el ejercicio
>    necesita, pone el contexto que otra tarea pide aunque pase de la meta, y si no puede acortar sin perder, deja el
>    largo y lo dice.
>
> La regla vale para los tres analizadores del largo: el de las oraciones, el de los títulos y el de las consignas de los
> temas. [F-MESA-R022](#F-MESA-R022) sigue valiendo: cuando el largo es lo único que deja tocar la oración, sólo se sacan
> palabras.

En la prueba ancha del 26/10/2026, 15 de los 64 ejercicios tenían una tarea de largo, y en 3 quien propone recortó
sentido para cumplirla: en `…1f5d` sacó el «No,» de «No, it isn't» («elimino «No» para acortar») y la traducción lo
conservó; en `…2185` la única tarea era el largo y se fue «this summer», lo único que hacía pensar en un plan; en
`…2419` se fue «a lot of». En 4 dejó el largo pendiente y el cambio se aceptó igual por sus otras tareas. Hasta acá la
tarea del largo contaba como cualquier otra para elegir ganador. Con la regla, en la C2 34 de los 1.985 ejercicios
corregibles quedan sin corregir porque sólo tienen el largo.

**Fuente**: brief 226, pregunta 1; José, el 26/10/2026: «Esto es un eval algorítmico (no un llm) … debería pesar mucho
menos que la oración bien escrita o respetando el enunciado … tendría que haber quedado la opción que se pasaba un poco
del largo» y «Sí: hacé el largo leve».

## Contexto

**Por qué una mesa por tema.** El 077 revisó los 11.287 ejercicios con revisores por lote y verificadores que intentaron
refutar sus propuestas (2.021 confirmadas, 83 ajustadas y 10 rechazadas). Lo que funcionó fue mirar el tema entero, juzgar el ejercicio
como queda y no dejar que nadie juzgue lo suyo. content-audit hoy hace lo contrario: una tarea por vez, criterios que
comparan con el original y un solo agente que se mide a sí mismo ([F-QICOR-R012](#F-QICOR-R012)).

**Cuánto trabajo es.** Con los casos de `escribir/` (A1 y A2), 278 ejercicios de A1 y 451 de A2 necesitan la mesa, en
63 y 74 temas; el más cargado de A1 tiene 40 y la mediana es 2. Con la escala de errores de la propuesta, B1 y B2 suman
unos 790 y 355: cerca de 1.900 en el curso. Cada uno cuesta, en promedio, 1,5 intentos de 1 consulta de quien propone
más 4 de los jueces: unas 7,8 consultas, unas 14.700 en el curso entero, contra las 41.700 que cuesta juzgarlo una vez.

**Cuánto tarda** (prototipo en el scratchpad, al ritmo medido de 19 s por consulta):

| Temas a la vez | Consultas a la vez | A1 | A2 | B1 y B2 (estimado) | Curso |
|---:|---:|---:|---:|---:|---:|
| 1 | 4 | 5,6 h | 9,1 h | 23 h | unas 38 h |
| 4 | 4 | 2,9 h | 4,7 h | 12 h | unas 20 h |
| 8 | 8 | 1,5 h | 2,4 h | 6 h | unas 10 h |

Más temas que consultas no acelera: el límite es la cuota del proveedor. Y ninguna tanda baja del tema más cargado
(en A1, 40 ejercicios: cerca de una hora).

## Alcance

- **Adentro**: la mesa de un tema, sus cinco puestos, la decisión, los intentos, el paralelismo, la opción múltiple, la
  miniteoría y el encargo de pasar un tema a opción múltiple dentro de la mesa.
- **Afuera**: qué juzga cada juez y su examen (paquete C); qué marca cada regla (paquete A); el semáforo (paquete D);
  lo que se aplica con una regla, sin mesa ([F-TANDA-R004](#F-TANDA-R004)); qué es «no empeorar» (FEAT-NOEMP); la bandeja
  (FEAT-BANDEJA); el proveedor del agente que propone, que se decide con el de los jueces
  ([DOUBT-JUECES-PROVEEDOR](#DOUBT-JUECES-PROVEEDOR)).

## User Journeys

### Journey[F-MESA-J001] - Corregir un ejercicio en la mesa de su tema
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-MESA-J001
    name: Corregir un ejercicio en la mesa de su tema
    flow:
      - id: tomar_tema
        action: "La tanda abre la mesa de un tema con tareas de mesa"
        gate: [F-MESA-R001, F-MESA-R010]
        then: revalidar
      - id: revalidar
        action: "La mesa vuelve a medir el ejercicio como está ahora"
        gate: [F-MESA-R002]
        outcomes:
          - when: "Ninguno de sus hallazgos sigue"
            then: ya_no_se_sostiene
          - when: "Sigue al menos uno"
            then: proponer
      - id: proponer
        action: "Quien propone, con lo que ya se intentó en pasadas anteriores, entrega candidatos del ejercicio entero y content-audit los mide"
        gate: [F-MESA-R003, F-MESA-R004, F-MESA-R017]
        then: juzgar
      - id: juzgar
        action: "Los jueces que leen lo que cambió juzgan cada candidato como queda, y el script decide"
        gate: [F-MESA-R005, F-MESA-R006, F-MESA-R013, F-MESA-R016]
        outcomes:
          - when: "Un candidato resuelve todas las tareas que seguían abiertas, no empeora nada y conserva el objetivo y las restricciones didácticas"
            then: aceptado
          - when: "Un candidato resuelve o mejora alguna tarea sin empeorar nada, y quedan tareas abiertas e intentos"
            then: seguir_desde_el_mejor
          - when: "Se acaban los intentos con algún candidato aceptado y tareas todavía abiertas"
            then: mejoro_sigue_abierto
          - when: "Los dos jueces de traducción y sentido no coinciden"
            then: desempate
          - when: "Ningún candidato pasa y se acaban los intentos"
            then: sin_ganador
      - id: seguir_desde_el_mejor
        action: "El candidato aceptado pasa a ser el punto de partida del intento siguiente, que recibe lo que mejoró y lo que falta; si mejoró, puede haber hasta cinco intentos"
        gate: [F-MESA-R007]
        then: proponer
      - id: mejoro_sigue_abierto
        action: "El ejercicio queda como lo dejó el último candidato aceptado, en un solo cambio; lo resuelto queda resuelto y lo mejorado, «mejoró, sigue abierta», con su medida"
        gate: [F-MESA-R007, F-MESA-R009]
        result: success
      - id: aceptado
        action: "El candidato queda como propuesta pendiente de la tanda y el curso no cambia"
        gate: [F-MESA-R009]
        result: success
      - id: ya_no_se_sostiene
        action: "Sus tareas terminan como ya no se sostiene, sin candidato ni intento gastado"
        gate: [F-MESA-R002]
        result: success
      - id: desempate
        action: "La bandeja recibe los dos veredictos y el ejercicio espera a José"
        gate: [F-MESA-R008]
        result: failure
      - id: sin_ganador
        action: "El ejercicio queda como estaba y la bandeja recibe el original, el candidato que menos falló y los motivos"
        gate: [F-MESA-R007]
        result: failure
```

## Open Questions

<a id="DOUBT-TEMAS-A-LA-VEZ"></a>
### Doubt[DOUBT-TEMAS-A-LA-VEZ] - ¿Cuántos temas van a la vez, y con cuántas consultas?
**Status**: RESOLVED (2026-09-30)

- [ ] Opción A: un tema y 4 consultas a la vez. A1 en 5,6 h, A2 en 9,1 h, el curso en unas 38 h.
- [x] Opción B (recomendada): 4 temas y 4 consultas a la vez, las mismas 4 que José ya eligió para los jueces. A1 en
  2,9 h, A2 en 4,7 h, el curso en unas 20 h.
- [ ] Opción C: 8 temas y 8 consultas. A1 en 1,5 h, A2 en 2,4 h, el curso en unas 10 h, con más riesgo de cortes por
  cuota.

**Answer**: **Opción B** (José, 30/9): 4 temas y 4 consultas a la vez. Se pasa a 8 y 8 si la tanda de A1 corre sin
cortes; se pide al abrir la tanda siguiente ([F-MESA-R010](#F-MESA-R010)). Las horas no incluyen juzgar el nivel antes
de la tanda (A1: unas 7.300 consultas de los jueces), que es el gasto mayor.

<a id="DOUBT-MC-REARMA"></a>
### Doubt[DOUBT-MC-REARMA] - ¿Se corrigen las opciones múltiples en las tandas?
**Status**: RESOLVED (2026-09-30)

Contesta, para las tandas, [DOUBT-CORREGIR-MC](#DOUBT-CORREGIR-MC) de FEAT-OPMUL, que sigue abierta fuera de ellas: hoy
ninguna corrección toca una opción múltiple ([F-OPMUL-R006](#F-OPMUL-R006)).

- [x] Opción A (recomendada): sí. Las reglas de forma (en `escribir/`, 170 de los 186 casos de A1 y A2 son mayúsculas)
  y la mesa cuando hay dos correctas, como dijo José («la mesa rearma»): sigue siendo opción múltiple, con una sola
  correcta y la misma cantidad de opciones ([F-MESA-R011](#F-MESA-R011)).
- [ ] Opción B: sólo las reglas de forma; las dos correctas esperan.
- [ ] Opción C: ninguna; los 315 ejercicios de opción múltiple con error siguen contados, sin arreglo.

**Answer**: **Opción A** (José, 30/9): reglas de forma más mesa; sigue siendo opción múltiple, con una sola correcta y la
misma cantidad de opciones. Dentro de las tandas cambia F-OPMUL-R006; fuera, sigue valiendo. José sumó ese día un
encargo: el tema «Contracciones» de A1 pasa a opción múltiple con contracciones distintas como opciones, y lo arma la mesa
([F-MESA-R015](#F-MESA-R015)).

<a id="DOUBT-MINITEORIA-ALCANCE"></a>
### Doubt[DOUBT-MINITEORIA-ALCANCE] - ¿La reescritura de una miniteoría puede tocar varios ejercicios a la vez?
**Status**: RESOLVED (2026-10-05)

- [ ] Opción A: sí, a todos los del tema que muestran ese texto y a ninguno más: una segunda excepción a
  [F-RPRES-R003](#F-RPRES-R003), como la del título.
- [x] Opción B: no; una propuesta por quiz, aunque el mismo texto esté repetido.
- [ ] Opción C: las miniteorías quedan fuera de las tandas.

**Answer**: **Opción B** (José, 5/10): «miniteoría es POR QUIZ» y debe ser la corrección específica de su caso. La
reescritura sólo toca ese quiz; compartir texto no agrupa candidatos ni propaga el cambio ([F-MESA-R012](#F-MESA-R012)).
Esta decisión reemplaza la opción A tomada el 30/9 con una comprensión distinta de la granularidad.

<a id="DOUBT-QUIEN-REFUTA"></a>
### Doubt[DOUBT-QUIEN-REFUTA] - ¿El que refuta es un agente aparte?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): no: refutan los controles que ya buscan contraejemplos ([F-MESA-R013](#F-MESA-R013)), y la
  muestra ciega dice si hace falta más.
- [ ] Opción B: sí, un agente por candidato: una consulta más por candidato, un 20 % más (A1: unas 2.600 consultas en vez
  de 2.200, y 0,6 h más con 4 a la vez).

**Answer**: **Opción A** (José, 30/9): sin agente nuevo que refute. Si la muestra ciega de la tanda A1 no cumple su meta
([F-CIEGA-R004](#F-CIEGA-R004)), se vuelve a preguntar por B.

<a id="DOUBT-AVANCE-PARCIAL"></a>
### Doubt[DOUBT-AVANCE-PARCIAL] - ¿Qué hace la mesa con un candidato que mejora sin terminar de arreglar?
**Status**: RESOLVED (2026-10-25)

Brief 214. En C2, C3 y C4 la cámara bajó de 18 a 10, 11 y 9 de largo, con meta de 3 a 8, y las tres propuestas se
tiraron porque el hallazgo de largo seguía.

- [x] Guardar el avance aunque la tarea siga abierta, como «mejoró, sigue abierta» (recomendada).
- [ ] Aceptar sólo lo que resuelve una tarea entera, como hasta el 24/10.
- [x] Cada intento parte del mejor candidato aceptado hasta ahí (recomendada), no del original.
- [x] Para guardar un avance, el largo tampoco puede alejarse del rango (recomendada); para un arreglo entero sigue sin
  frenar.
- [x] El avance se ve como un desenlace propio en el tablero (recomendada), no como «sin ganador».

**Answer** (José, 25/10): que se haga el cambio del brief 214, con sus recomendaciones; no las eligió una por una.
Sumó tres cosas: que el rechazo diga la medida, que la pasada nueva herede lo que se intentó en la anterior
([F-MESA-R017](#F-MESA-R017)) y que, si un intento mejora, haya más intentos, hasta cinco ([F-MESA-R007](#F-MESA-R007)).

<a id="DOUBT-OPCIONES-CONTRACCIONES"></a>
### Doubt[DOUBT-OPCIONES-CONTRACCIONES] - ¿Cuántas opciones lleva un ejercicio de «Contracciones» pasado a opción múltiple?
**Status**: OPEN

- [ ] Opción A (recomendada): tres, la correcta y dos contracciones distintas: el mínimo que las contrasta.
- [ ] Opción B: cuatro, como el 29 % de las opciones múltiples de A1 (209 de 710).
- [ ] Opción C: las que la mesa crea necesarias, entre dos y cuatro.

**Answer**: Pendiente. Recomiendo A: con tres hay contraste y menos riesgo de que dos opciones valgan, que en *Present
Continuous* es real («isn't» y «'s not» valen las dos). Hasta que José conteste, rige A ([F-MESA-R015](#F-MESA-R015)).

## References

- **FEAT-QICOR, FEAT-LASAG, FEAT-KTLR** — La corrección de hoy, su revalidación, su lazo de intentos y sus criterios, que
  siguen valiendo fuera de las tandas: R002, R006, R007, R014.
- **FEAT-JEXAM, FEAT-TRSEN, FEAT-UNICA, FEAT-MCVALE, FEAT-MINIT** (ronda 1, paquete C) — Qué lee cada juez, cómo juzga un
  candidato y cuántas consultas van a la vez: R005, R008, R010, R011, R012, R013. [F-MESA-R016](#F-MESA-R016) conserva
  el selector y la fórmula de [F-UNICA-R008](#F-UNICA-R008) y la explicación específica de [F-MINIT-R012](#F-MINIT-R012).
- **FEAT-OMVAL, FEAT-OPMUL** — Una sola correcta, y la opción múltiple que hoy no se corrige: R011.
- **FEAT-SEMAF, FEAT-CLEX** — El semáforo del tema y la consulta léxica: R003, R004.
- **FEAT-REVBYP, FEAT-REVAPR, FEAT-RPRES, FEAT-CDIFF, FEAT-EVCOST** — Propuestas pendientes, preservación y reuso de
  veredictos: R001, R002, R009, R012.
- **FEAT-TAREA, FEAT-NOEMP, FEAT-TANDA, FEAT-BANDEJA** (este paquete) — Las tareas que toma la mesa, la vara con que
  decide, la tanda que la corre y la bandeja que recibe lo que no decide.
