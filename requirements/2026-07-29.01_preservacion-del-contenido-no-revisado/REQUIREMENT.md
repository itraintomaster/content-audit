---
feature:
  id: FEAT-RPRES
  code: F-RPRES
  name: Preservacion del contenido no revisado al aplicar una revision
  priority: critical
---

# Preservacion del contenido no revisado al aplicar una revision

## TL;DR

**Que**: Aplicar una revision a un elemento del curso cambia **solo** lo que la
revision propone corregir; todo lo demas del elemento sobrevive intacto al ciclo
de guardado y recarga, incluidos los valores legitimamente vacios.

**Por que**: Hoy los atributos del ejercicio que la propuesta no describe quedan
en valores por defecto (2.658 quizzes revisados degradados), y eso deja el
contenido inutilizable para la aplicacion que lo sirve a los alumnos.

## Reglas de Negocio

<a id="F-RPRES-R001"></a>
### Rule[F-RPRES-R001] - Preservacion por defecto: solo cambia lo que la revision propone
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Tras aplicar una revision aprobada sobre un elemento del curso y volver a
> cargar el curso desde la fuente de datos, el elemento revisado difiere del
> original **unicamente** en los atributos que la revision propone corregir.
> Cualquier otro atributo conserva exactamente el valor que tenia antes: no se
> pierde, no se recalcula, no se sustituye por un valor por defecto.

<details><summary>Detalle</summary>

**La regla se formula por complemento, no por enumeracion.** El conjunto de
atributos que una revision puede cambiar es **acotado y conocido** (para una
revision de ejercicio: la oracion del ejercicio, sus huecos, las respuestas
aceptadas y la traduccion — [F-LAPS-R013](#F-LAPS-R013); para una revision de
titulo, la etiqueta del knowledge). Todo lo que queda fuera de ese conjunto se
preserva **por defecto**, sin necesidad de estar listado en ninguna parte.

Esta inversion es deliberada. La formulacion vigente hoy enumera lo que debe
preservarse ([F-LAPS-R014](#F-LAPS-R014): identificadores, instrucciones,
posicion, "cualquier otra metadata estructural") y es justamente esa enumeracion
la que dejo el hueco: los atributos degradados no figuraban en la lista de
preservacion y quedaron formalmente autorizados a cambiar por pertenecer, en un
sentido amplio, a la "estructura del ejercicio". Enumerar lo preservable obliga a
adivinar la lista completa de atributos existentes y futuros; enumerar lo
cambiable solo obliga a describir el efecto deseado de la revision, que es
informacion que la revision ya tiene.

**Criterio de aceptacion**: dado un elemento del curso, se registra su contenido
completo antes de la revision; se aplica una revision aprobada; se recarga el
curso desde la fuente de datos; se compara atributo por atributo. El conjunto de
diferencias observadas debe estar **contenido** en el conjunto de atributos que
la revision propone corregir. Una sola diferencia fuera de ese conjunto viola la
regla.

**La verificacion es de punta a punta.** La comparacion se hace sobre el
contenido **persistido y recargado**, no sobre el elemento en memoria. Ninguna
etapa intermedia (construir la propuesta, sustituir el elemento, escribir el
curso, volver a leerlo) puede descartar informacion. Es la unica forma de
detectar perdidas que ocurren en el traspaso entre etapas, que es donde ocurrio
el dano observado.

**Atributos degradados observados** (medicion sobre el arbol del curso,
2026-07-29; nomenclatura funcional segun la sub-estructura del formulario del
ejercicio documentada en FEAT-COURSE, ver Referencias):

| Atributo del ejercicio | Valor original | Valor tras la revision | Quizzes afectados |
|------------------------|----------------|------------------------|-------------------|
| Tipo de formulario del ejercicio | El tipo declarado (completar huecos) | Ausente | 2.658 |
| Peso/incidencia del ejercicio | 1,0 | 0,0 | 2.658 |
| Etiqueta de presentacion del formulario | Vacia | Ausente | 2.658 |
| Nombre del formulario | Vacio | Ausente | 2.658 |
| Texto de la parte hueco del enunciado | Vacio | Ausente | 2.690 partes |

Ninguno de estos atributos es objeto de la correccion: ninguna revision se
propone cambiar el tipo de ejercicio ni su peso. Todos debian preservarse.

**Error**: "La revision del elemento '{elementId}' modifico atributos que no eran objeto de la correccion: {atributos}"

</details>

<a id="F-RPRES-R002"></a>
### Rule[F-RPRES-R002] - Vacio no es ausente, y un valor propio no es un valor por defecto
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un atributo con un valor legitimamente vacio se conserva **presente y vacio**;
> nunca se convierte en ausente. Un atributo con valor propio nunca se sustituye
> por el valor por defecto de su tipo. Y a la inversa: un atributo ausente en el
> original no se materializa con un valor por defecto. La comparacion que
> verifica [F-RPRES-R001](#F-RPRES-R001) trata estos tres estados —presente con
> valor, presente y vacio, ausente— como **distintos**.

<details><summary>Detalle</summary>

Esta regla existe porque buena parte del dano observado consiste exactamente en
esa degradacion: etiquetas que valian "vacio" pasaron a "ausente", y un peso que
valia 1,0 paso a 0,0. Sin esta regla, una comparacion permisiva —una que
considere equivalentes "vacio" y "ausente", o que trate 0,0 como "sin valor"—
declararia preservado un elemento que en realidad quedo degradado, y
[F-RPRES-R001](#F-RPRES-R001) pasaria sin detectar nada.

La distincion no es formal, es funcional: la aplicacion que sirve el contenido a
los alumnos consume estos atributos. Un tipo de ejercicio ausente deja al
consumidor sin saber como interpretar la estructura del ejercicio; un peso en
0,0 en lugar de 1,0 altera la ponderacion del ejercicio en la practica del
alumno. "Vacio" es una decision de contenido; "ausente" es una perdida.

Es el mismo criterio de identidad que ya rige el ciclo de lectura-escritura de un
curso ([F-COURSE-R003](#F-COURSE-R003), [F-COURSE-R010](#F-COURSE-R010)): esta
regla lo extiende al elemento que la revision reemplaza, que es el punto donde
hoy no se aplica.

**Criterio de aceptacion**: partiendo de un elemento cuyos atributos incluyen al
menos un valor legitimamente vacio y un valor propio que coincide con nada mas
que el por-defecto de su tipo, tras la revision y la recarga esos atributos
siguen exactamente en el mismo estado (vacio sigue vacio y presente; el valor
propio sigue siendo el propio).

**Error**: "El atributo '{atributo}' del elemento '{elementId}' paso de '{estadoOriginal}' a '{estadoResultante}' al aplicar la revision"

</details>

<a id="F-RPRES-R003"></a>
### Rule[F-RPRES-R003] - Revisar un elemento no altera ningun otro elemento del curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Aplicar una revision sobre un elemento deja el resto del curso exactamente como
> estaba: los demas ejercicios, sus knowledges, topics, milestones y los datos
> del curso conservan su contenido identico. Ninguna revision crea, elimina ni
> reordena elementos que no sean el revisado. La unica excepcion admitida es la
> propagacion del titulo de [F-RPRES-R004](#F-RPRES-R004).

<details><summary>Detalle</summary>

El flujo que aplica una revision reescribe el **curso completo**, no solo el
elemento revisado. Eso convierte cualquier omision en la escritura en un dano
masivo: ya ocurrio una vez con las [oraciones planas](glossary:Oraciones planas)
([F-DBSENT-R004](#F-DBSENT-R004)), donde un unico guardado borraba la
informacion de todos los ejercicios del curso.

Esta regla es la garantia de radio de impacto: la reescritura completa es un
detalle del camino de persistencia, y no debe tener ningun efecto observable
fuera del elemento revisado. Distingue el dano "por elemento" (que cubre
[F-RPRES-R001](#F-RPRES-R001)) del dano "por curso", que se propaga en silencio
y solo se detecta contando.

**Criterio de aceptacion**: dado un curso con varios ejercicios repartidos en
varios knowledges, tras aplicar una revision sobre uno solo de ellos y recargar
el curso, todos los demas ejercicios —los del mismo knowledge y los de otros
knowledges— conservan su contenido identico al que tenian antes, y la estructura
del curso (cantidad y orden de elementos en cada nivel) no cambia.

**La excepcion es unica y esta acotada**: corregir la etiqueta visible de un
knowledge alcanza tambien al titulo de sus ejercicios
([F-RPRES-R004](#F-RPRES-R004)). Es la unica alteracion admitida sobre elementos
ajenos al revisado; cualquier otra es una violacion de esta regla.

**Error**: "La revision del elemento '{elementId}' modifico {cantidad} elementos ajenos a la correccion"

</details>

<a id="F-RPRES-R004"></a>
### Rule[F-RPRES-R004] - Corregir la etiqueta de un knowledge alinea el titulo de sus ejercicios
**Severity**: critical | **Validation**: VALIDATED

> Cuando una revision corrige la **etiqueta visible** de un knowledge, el titulo
> con que se presentan sus ejercicios queda alineado con la etiqueta corregida.
> Es la unica excepcion a [F-RPRES-R003](#F-RPRES-R003). Ninguna otra revision
> toca ese titulo: una correccion del ejercicio lo deja intacto.

<details><summary>Detalle</summary>

El titulo de cada ejercicio es una **copia** de la etiqueta de su knowledge, y es
lo que la aplicacion muestra como encabezado de la tarjeta que ve el alumno. De
ahi las dos mitades de esta regla:

- **Sin propagacion**, corregir la etiqueta deja las tarjetas mostrando el titulo
  viejo: la correccion se aplica pero el alumno sigue viendo el texto anterior.
  Por eso la propagacion es parte del **efecto deseado** de una correccion de
  etiqueta, y no un dano colateral.
- **Con propagacion indebida**, ocurre lo contrario y es peor: hubo un caso real
  en que una correccion del ejercicio piso el titulo con la oracion corregida, y
  la tarjeta paso a mostrar la respuesta como encabezado — le revelaba la
  solucion al alumno antes de resolver. Ese caso **no** es una excepcion a
  R003: es una violacion de [F-RPRES-R001](#F-RPRES-R001), porque el titulo no es
  objeto de una correccion de ejercicio.

Declarar la propagacion aca la vuelve gobernada: hoy ocurre fuera del sistema,
como un paso de reparacion posterior, y por lo tanto no esta verificada ni se
aplica cuando el contenido se consume por otra via.

**Lo que la propagacion NO habilita.** Alcanza un unico atributo del ejercicio,
su titulo. El contenido del ejercicio —su oracion, su enunciado, su traduccion—
no se toca, y en particular no se restituye la version que el ejercicio tenia
cuando se construyo la propuesta de titulo: una revision del ejercicio aprobada
despues no puede quedar revertida. Esa proteccion vive en
[F-KTLR-R007](#F-KTLR-R007), que quedo reconciliada con esta regla.

**Criterio de aceptacion**: (a) tras revisar la etiqueta de un knowledge y
recargar el curso, cada uno de sus ejercicios tiene el titulo alineado con la
etiqueta corregida, y ningun otro atributo de esos ejercicios cambia; (b) tras
una correccion del ejercicio, el titulo de los ejercicios de ese knowledge sigue
siendo el que tenian antes.

**Error**: "La correccion de la etiqueta del knowledge '{knowledgeId}' dejo {cantidad} ejercicios con el titulo desalineado" / "La revision del ejercicio '{quizId}' modifico su titulo, que no es objeto de la correccion"

</details>

<a id="F-RPRES-R005"></a>
### Rule[F-RPRES-R005] - El contenido ya degradado se repara, una vez vigente la preservacion
**Severity**: critical | **Validation**: VALIDATED

> Los elementos degradados por revisiones anteriores se reparan: al terminar la
> reparacion, ningun elemento del curso presenta atributos ausentes o en valor
> por defecto alli donde el contenido tenia un valor propio. La reparacion se
> ejecuta **despues** de que la preservacion este vigente
> ([F-RPRES-R001](#F-RPRES-R001), [F-RPRES-R002](#F-RPRES-R002)), nunca antes.

<details><summary>Detalle</summary>

**El orden no es una recomendacion, es parte de la regla.** R001 y R002 son
garantias *relativas* al estado previo del elemento: si el elemento ya venia
degradado, preservarlo fielmente preserva el dano. Reparar primero no cierra
nada: la siguiente revision vuelve a degradar cada elemento que toque, y la
reparacion se convierte en una tarea recurrente. Reparar despues es definitivo.

**La reparacion es determinista** — no hay que reconstruir ni inventar contenido.
La verificacion sobre los 2.658 elementos afectados lo confirma:

| Comprobacion | Resultado |
|--------------|-----------|
| Elementos degradados con contraparte integra disponible | 2.658 de 2.658; ninguno huerfano |
| Uniformidad de los valores originales entre los degradados | total, y coincide con la de los 8.829 sanos |
| Cantidad de partes del enunciado respecto de la contraparte | coincide: la estructura no se altero |
| Otros atributos (identificadores, instrucciones, traduccion, titulo, oraciones planas) | intactos |

Es decir: el dano es exactamente el conjunto de atributos de
[F-RPRES-R001](#F-RPRES-R001), su valor correcto es conocido y no hay ningun caso
ambiguo que requiera decision editorial.

**Criterio de aceptacion**: tras la reparacion, un recorrido completo del curso no
encuentra ningun elemento cuyos atributos esten ausentes o en el valor por
defecto de su tipo alli donde corresponde un valor propio; y el resto del
contenido de esos elementos —incluida la correccion que la revision les habia
aplicado— queda igual que antes de reparar. La reparacion **no** revierte
revisiones: solo repone lo que se habia perdido.

**Error**: "Quedan {cantidad} elementos del curso con atributos degradados tras la reparacion"

</details>

<a id="F-RPRES-R006"></a>
### Rule[F-RPRES-R006] - Una parte nueva del enunciado nace completa, no por defecto
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cuando la correccion produce un enunciado con **mas partes** que el original,
> cada parte que no existia nace en el estado que corresponde a su clase —texto
> fijo o hueco—:
> 1. Es **indistinguible**, atributo por atributo, de las demas partes de su
>    misma clase dentro del mismo ejercicio.
> 2. Si el ejercicio no tiene ninguna otra parte de esa clase, cada atributo
>    queda en el estado que la clase declara suyo: con valor propio donde la
>    clase exige valor propio, y vacio presente o ausente donde la clase lo
>    establece asi
>    ([F-QSENT-R003](../2026-04-22.01_quiz-sentence-dsl/REQUIREMENT.md#F-QSENT-R003),
>    [F-QSENT-R004](../2026-04-22.01_quiz-sentence-dsl/REQUIREMENT.md#F-QSENT-R004)).
>
> En ningun caso un atributo de una parte nueva queda ausente o en el valor por
> defecto de su tipo **alli donde su clase exige un valor propio**.

<details><summary>Detalle</summary>

**Por que no la cubren R001 ni R002.** Aquellas son garantias de *preservacion*:
comparan el elemento resultante contra el original y exigen que lo no corregido
sobreviva. Una parte que no existia en el original no tiene contra que
compararse: no hay nada que preservar. Fijar sus atributos es un acto distinto
—**se fija contenido nuevo**— y por eso necesita enunciado propio.

**Por que necesita regla y no silencio.** El modo de falla es exactamente el que
origino esta feature: un elemento que lleva atributos nace con los valores por
defecto de su tipo, nadie lo nota, y el dano aparece recien cuando la aplicacion
que sirve el contenido no puede interpretarlo. La diferencia es solo por donde
entra. Dejarlo sin enunciado seria repetir el patron que ya nos costo 2.658
ejercicios: no estaba prohibido porque nadie lo habia escrito.

**Que fija la regla y que no.** Fija el **resultado observable**: la parte nueva
es indistinguible, en sus atributos, de las otras partes de su clase en ese
ejercicio. **No** fija de donde se toman esos valores —de que parte se copian, en
que orden se recorre el enunciado— porque eso es mecanismo, no comportamiento
observable.

**El invariante 1 manda; el 2 es su respaldo.** La homogeneidad es el enunciado
principal: mientras el ejercicio tenga al menos una parte de la clase de la parte
nueva, esa hermana **es** la definicion de "completo" y no hace falta ninguna otra
referencia. El invariante 2 solo entra en juego cuando esa referencia no existe.
Los dos nunca se contradicen, porque no se aplican a la vez.

**Por que el invariante 2 esta acotado al estado que la clase declara suyo.** Cada
clase de parte declara que atributos lleva y en que estado (FEAT-QSENT, ver
Referencias): un hueco lleva sus respuestas aceptadas con valor propio
([F-QSENT-R004](../2026-04-22.01_quiz-sentence-dsl/REQUIREMENT.md#F-QSENT-R004))
y su texto **vacio presente**; una parte de texto fijo lleva su texto con valor
propio y **no lleva respuestas**
([F-QSENT-R003](../2026-04-22.01_quiz-sentence-dsl/REQUIREMENT.md#F-QSENT-R003),
que ademas rechaza como dato invalido una parte de texto fijo que venga con
respuestas pobladas). Sin la acotacion, la regla exigiria poblar las respuestas de
una parte de texto fijo y dar valor no vacio al texto de un hueco: en los dos
casos estaria pidiendo **degradar** contenido correcto, y contradiciendo a
FEAT-QSENT de frente.

La acotacion no debilita nada. Lo que la regla persigue —que un atributo caiga al
valor por defecto de su tipo sin que nadie lo note— se sigue prohibiendo con la
misma fuerza donde **si** corresponde un valor propio, que es exactamente donde
ocurrio el dano de esta feature. "Ausente" no es sinonimo de degradado: es
degradado solo cuando el estado correcto era otro. Es el mismo criterio de
[F-RPRES-R002](#F-RPRES-R002) —vacio, ausente y con valor son estados
distintos—, aplicado aca a una parte que no tiene original contra que compararse:
la referencia deja de ser "lo que este atributo valia antes" y pasa a ser "lo que
esta clase de parte declara suyo".

**Caso sin referencia** [ASSUMPTION]: el invariante 2 no se observa en el
contenido actual, porque los 11.487 ejercicios del curso tienen al menos una parte
de texto fijo y al menos un hueco; toda parte nueva encuentra hermana. Se enuncia
igual porque no hay nada que impida que una correccion futura produzca un
enunciado de una sola clase de parte, y porque su ausencia es justo lo que dejo el
hueco que origino esta feature.

**Criterio de aceptacion**: dado un ejercicio cuyo enunciado tiene N partes, tras
una correccion que produce un enunciado con mas de N partes y la recarga del
curso: (a) cada parte agregada que tiene hermanas de su clase presenta los mismos
valores de atributos que ellas; (b) cada parte agregada que **no** tiene hermanas
de su clase presenta cada atributo en el estado que su clase declara —las
respuestas de un hueco pobladas y el texto de ese hueco vacio y presente; el texto
de una parte de texto fijo con valor propio y sus respuestas ausentes—.

**Error**: "La correccion del ejercicio '{quizId}' agrego {cantidad} partes al enunciado que no estan en el estado que corresponde a su clase: {detalle}"

</details>

## Contexto

El sistema aplica revisiones aprobadas sobre elementos del curso: toma la
propuesta, sustituye el elemento por su version corregida y persiste el curso de
vuelta a la fuente de datos ([F-REVAPR-R011](#F-REVAPR-R011)). El punto ciego
esta en la sustitucion: el elemento corregido se **construye** a partir de lo que
la propuesta describe, y lo que la propuesta no describe no se hereda del
original — nace con el valor por defecto de su tipo.

La medicion sobre el arbol del curso (2026-07-29) lo confirma y acota el dano con
precision: 2.658 ejercicios revisados quedaron sin tipo de formulario, sin
etiqueta, sin nombre y con peso 0,0 en lugar de 1,0; 2.690 partes de enunciado
perdieron su texto vacio. Los 8.829 ejercicios **no** revisados quedaron
intactos. El dano no viene del camino de persistencia —que preserva bien lo que
recibe— sino del momento en que se arma el elemento revisado.

### Por que no alcanza con las garantias existentes

Ya existen tres garantias vecinas, y ninguna cubre este caso:

1. **Idempotencia del ciclo lectura-escritura de un curso**
   ([F-COURSE-R003](#F-COURSE-R003), [F-COURSE-R010](#F-COURSE-R010)). Rige
   cuando el curso **no** se modifica: leer y volver a escribir no cambia nada.
   Se cumple hoy — por eso los 8.829 ejercicios no revisados estan intactos — y
   aun asi el dano ocurre.
2. **Preservacion de las [oraciones planas](glossary:Oraciones planas)**
   ([F-DBSENT-R004](#F-DBSENT-R004)). Cubre exactamente un dato: la lista de
   oraciones planas de cada ejercicio. Se escribio como espejo de la regla de
   lectura que la introdujo, para un solo atributo. Su razonamiento — "aplicar
   una revision reescribe el curso entero, cualquier omision se propaga" — ya
   anticipaba el problema general, pero la regla quedo acotada a ese dato.
3. **Invariantes de la propuesta de correccion lexica**
   ([F-LAPS-R013](#F-LAPS-R013), [F-LAPS-R014](#F-LAPS-R014)). Enumeran que
   puede cambiar (la oracion, la estructura del ejercicio, la traduccion) y que
   debe preservarse (identificadores, instrucciones, orden, "cualquier otra
   metadata estructural"). **Aqui esta el hueco**: los atributos degradados —tipo
   de formulario, peso, etiquetas, texto vacio del hueco— no figuran en la lista
   de preservacion y se los puede leer como parte de la "estructura del
   ejercicio", que la regla autoriza a cambiar. Formalmente, el dano estaba
   permitido. Esas dos reglas se reformulan como caso particular de
   [F-RPRES-R001](#F-RPRES-R001), conservando sus identificadores; ver
   [DOUBT-LAPS-ENUM](#DOUBT-LAPS-ENUM).

De ahi la eleccion de formular la regla **por complemento**
([F-RPRES-R001](#F-RPRES-R001)): la lista de lo preservable siempre va a estar
incompleta, porque crece cada vez que el contenido gana un atributo nuevo. La
lista de lo cambiable, en cambio, es corta, estable y la revision ya la conoce
—es su proposito.

### Dos reglas que hoy viven fuera de gobierno

Hay dos comportamientos que hoy ocurren en el paso de publicacion hacia el
destino productivo, fuera del sistema y sin verificacion, y que este
requerimiento trae adentro:

1. **La reposicion de los atributos degradados**, mediante una lista de atributos
   protegidos que se completa a mano. Es la regla escrita en el peor lugar
   posible: aguas abajo del dano, sin verificacion, y con la obligacion de crecer
   cada vez que el contenido gana un atributo nuevo.
   [F-RPRES-R001](#F-RPRES-R001) elimina la necesidad de esa lista y
   [F-RPRES-R005](#F-RPRES-R005) cierra el pasivo que dejo.
2. **La alineacion del titulo de los ejercicios con la etiqueta de su
   knowledge**, hoy un paso de sincronizacion posterior. Es un efecto deseado, no
   un dano, y por eso se declara explicitamente en
   [F-RPRES-R004](#F-RPRES-R004) en lugar de dejarlo como comportamiento
   accidental.

La consecuencia externa justifica la severidad: publicar contenido con estos
atributos degradados lo deja inutilizable para la aplicacion que lo sirve a los
alumnos.

## Alcance

- **En alcance**:
  - Preservacion de todo atributo no alcanzado por la correccion, al aplicar
    cualquier revision aprobada sobre cualquier elemento del curso (ejercicios y
    titulos de knowledge por igual).
  - Criterio de identidad que distingue valor propio, vacio presente y ausente.
  - Completitud de las partes del enunciado que la correccion agrega y que no
    tienen contraparte en el original ([F-RPRES-R006](#F-RPRES-R006)).
  - No alteracion del resto del curso, con la unica excepcion declarada de la
    alineacion del titulo ([F-RPRES-R004](#F-RPRES-R004)).
  - Reparacion del contenido ya degradado, posterior a la vigencia de la
    preservacion ([F-RPRES-R005](#F-RPRES-R005)).
  - Verificacion de punta a punta: sobre el contenido persistido y recargado.
- **Fuera de alcance**:
  - **Que propone cambiar cada tipo de revision.** Eso lo define cada estrategia
    de correccion; este requerimiento solo exige que ese conjunto sea acotado y
    que todo lo demas se preserve.
  - **El procedimiento concreto de reparacion** (de una vez o por lotes, con que
    herramienta, en que orden recorre el curso).
    [F-RPRES-R005](#F-RPRES-R005) fija el resultado observable y el momento, no
    el como.
  - **De donde se toman los valores de una parte nueva del enunciado** (de que
    parte se copian, en que orden se recorre). [F-RPRES-R006](#F-RPRES-R006) fija
    que la parte nueva quede completa y homogenea, no el mecanismo.
  - **Que atributos lleva cada clase de parte del enunciado y en que estado.** Lo
    define FEAT-QSENT; [F-RPRES-R006](#F-RPRES-R006) se apoya en esa declaracion
    y no la redefine ni la amplia.
  - **La publicacion al destino productivo** y su lista de atributos protegidos.
    Este requerimiento elimina la razon de ser de esa lista, pero retirarla es
    trabajo aparte.
  - **La idempotencia del ciclo lectura-escritura de un curso sin modificar**,
    que ya cubre [F-COURSE-R003](#F-COURSE-R003).

## User Journeys

### Journey[F-RPRES-J001] - Aplicar una revision conservando el contenido no revisado
**Validation**: AUTO_VALIDATED

Cubre la preservacion por defecto y el criterio de identidad sobre el elemento
revisado, verificados tras persistir y recargar, y el caso en que la correccion
agrega partes al enunciado. Cubre R001, R002 y R006.

```yaml
journeys:
  - id: F-RPRES-J001
    name: Aplicar una revision conservando el contenido no revisado
    flow:
      - id: aprobar
        action: "El usuario aprueba una propuesta de revision sobre un ejercicio existente del curso"
        then: aplicar

      - id: aplicar
        action: "El sistema aplica la revision al curso y lo persiste en la fuente de datos"
        then: recargar

      - id: recargar
        action: "El sistema vuelve a cargar el curso desde la fuente de datos"
        then: verificar_correccion

      - id: verificar_correccion
        action: "El ejercicio recargado refleja la correccion que la revision propuso"
        gate: [F-RPRES-R001]
        outcomes:
          - when: "El ejercicio original tenia atributos con valor propio fuera del alcance de la correccion (tipo de formulario, peso del ejercicio)"
            then: conserva_valores
          - when: "El ejercicio original tenia atributos con valor legitimamente vacio fuera del alcance de la correccion (etiqueta y nombre del formulario, texto de la parte hueco)"
            then: conserva_vacios
          - when: "La correccion produce un enunciado con mas partes que el original"
            then: partes_nuevas_completas

      - id: conserva_valores
        action: "El ejercicio recargado conserva esos atributos con exactamente el mismo valor que tenian antes de la revision"
        gate: [F-RPRES-R001]
        result: success

      - id: conserva_vacios
        action: "El ejercicio recargado conserva esos atributos presentes y vacios: no quedaron ausentes ni sustituidos por el valor por defecto de su tipo"
        gate: [F-RPRES-R002]
        result: success

      - id: partes_nuevas_completas
        action: "Cada parte del enunciado que no existia en el original queda indistinguible, atributo por atributo, de las demas partes de su misma clase; y donde no hay ninguna otra parte de esa clase, cada atributo queda en el estado que su clase declara suyo"
        gate: [F-RPRES-R006]
        result: success
```

### Journey[F-RPRES-J002] - Aplicar una revision no altera el resto del curso
**Validation**: AUTO_VALIDATED

Cubre el radio de impacto de una revision sobre un curso que se reescribe
completo. Cubre R003.

```yaml
journeys:
  - id: F-RPRES-J002
    name: Aplicar una revision no altera el resto del curso
    flow:
      - id: revisar_uno
        action: "El usuario aprueba una propuesta de revision sobre un unico ejercicio de un curso que contiene otros ejercicios"
        then: aplicar_curso

      - id: aplicar_curso
        action: "El sistema aplica la revision y persiste el curso completo en la fuente de datos"
        then: recargar_curso

      - id: recargar_curso
        action: "El sistema vuelve a cargar el curso desde la fuente de datos"
        outcomes:
          - when: "Los demas ejercicios pertenecen al mismo knowledge que el ejercicio revisado"
            then: intactos_mismo_knowledge
          - when: "Los demas ejercicios pertenecen a otros knowledges del curso"
            then: intactos_otros_knowledges

      - id: intactos_mismo_knowledge
        action: "Cada ejercicio no revisado del mismo knowledge conserva todo su contenido identico al que tenia antes de la revision"
        gate: [F-RPRES-R003]
        result: success

      - id: intactos_otros_knowledges
        action: "Cada ejercicio de los demas knowledges conserva todo su contenido identico, y la cantidad y el orden de elementos de cada nivel del curso no cambian"
        gate: [F-RPRES-R003]
        result: success
```

### Journey[F-RPRES-J003] - El titulo de los ejercicios sigue a la etiqueta, y solo a ella
**Validation**: VALIDATED

Cubre las dos mitades de la excepcion declarada: la correccion de etiqueta
propaga al titulo de los ejercicios, y la correccion del ejercicio no lo toca.
Cubre R004 (y R001 en la rama del titulo intacto).

```yaml
journeys:
  - id: F-RPRES-J003
    name: El titulo de los ejercicios sigue a la etiqueta, y solo a ella
    flow:
      - id: aprobar_correccion
        action: "El usuario aprueba una propuesta de revision sobre un knowledge que tiene varios ejercicios"
        then: aplicar_y_recargar

      - id: aplicar_y_recargar
        action: "El sistema aplica la revision, persiste el curso y lo vuelve a cargar desde la fuente de datos"
        outcomes:
          - when: "La revision corrige la etiqueta visible del knowledge"
            then: titulos_alineados
          - when: "La revision corrige un ejercicio del knowledge (su oracion, sus huecos, sus respuestas o su traduccion)"
            then: titulo_intacto

      - id: titulos_alineados
        action: "Cada ejercicio de ese knowledge queda con su titulo alineado a la etiqueta corregida, y ningun otro atributo suyo cambia"
        gate: [F-RPRES-R004]
        result: success

      - id: titulo_intacto
        action: "El titulo de los ejercicios del knowledge sigue siendo el que tenian antes: la correccion no lo sustituye por el contenido corregido"
        gate: [F-RPRES-R001, F-RPRES-R004]
        result: success
```

## Open Questions

<a id="DOUBT-ALCANCE-DECLARADO"></a>
### Doubt[DOUBT-ALCANCE-DECLARADO] - Como se determina "lo que la revision propone corregir"?
**Status**: OPEN

[F-RPRES-R001](#F-RPRES-R001) se apoya en un conjunto acotado de atributos que la
revision puede cambiar. La regla no fija **como** se conoce ese conjunto, y la
eleccion cambia la forma de verificarla.

- [ ] Opcion A: **Fijo por tipo de correccion.** Cada tipo de revision declara de
  una vez y para siempre que atributos puede cambiar (una correccion lexica: la
  oracion del ejercicio, sus huecos, sus respuestas y la traduccion; una
  correccion de titulo: la etiqueta del knowledge). Simple y estable; obliga a
  actualizar la declaracion cuando aparece un tipo nuevo de correccion.
- [ ] Opcion B: **Declarado por cada propuesta.** Cada propuesta enuncia
  explicitamente que atributos cambia, y la verificacion contrasta el diff real
  contra esa declaracion. Mas preciso y auto-verificable; exige que las
  propuestas carguen esa informacion.
- [ ] Opcion C: **Implicito en el contenido propuesto.** Cambia lo que la
  propuesta describe; todo atributo que la propuesta no menciona se hereda. Es el
  comportamiento mas cercano al deseado sin agregar informacion nueva, pero
  vuelve ambiguo el caso de un atributo que la propuesta menciona con el mismo
  valor que el original.

**Answer**: Pendiente, y asignada a la fase de arquitectura. Ninguna de las tres
cambia el efecto exigido —que nada fuera de la correccion cambie—, asi que R001
se sostiene igual y el resto de las reglas no dependen de la eleccion. Lo que
cambia es donde vive la declaracion del alcance, y eso se decide con el contexto
de los contratos existentes, no desde el requerimiento.

<a id="DOUBT-TITULO-DENORMALIZADO"></a>
### Doubt[DOUBT-TITULO-DENORMALIZADO] - La propagacion del titulo a los ejercicios del knowledge, es un cambio permitido?
**Status**: RESOLVED

Cada ejercicio lleva un titulo que es una copia de la etiqueta de su knowledge, y
la aplicacion lo muestra como encabezado de la tarjeta del alumno. Cuando una
revision corrige la etiqueta del knowledge (FEAT-KTLR, ver Referencias), los
titulos de sus ejercicios quedan desactualizados. Hoy esa sincronizacion ocurre
en el paso de publicacion, fuera del sistema.

- [x] Opcion A: La propagacion **forma parte** del alcance de la correccion de
  etiqueta: revisar la etiqueta alinea tambien el titulo de sus ejercicios, y eso
  no viola [F-RPRES-R001](#F-RPRES-R001) ni [F-RPRES-R003](#F-RPRES-R003).
- [ ] Opcion B: La propagacion es un efecto colateral prohibido: revisar la
  etiqueta no toca los ejercicios, y la copia desactualizada se resuelve en otro
  lado.
- [ ] Opcion C: El titulo del ejercicio deja de ser una copia y pasa a leerse del
  knowledge; el problema desaparece.

**Answer**: Opcion A. La propagacion es por diseno: sin ella la tarjeta queda
mostrando el titulo viejo. Se declara en [F-RPRES-R004](#F-RPRES-R004) como la
**unica** excepcion admitida a [F-RPRES-R003](#F-RPRES-R003), con su reciproca
explicita: ninguna otra revision toca ese titulo. La reciproca no es teorica —
hubo un caso real en que una correccion del ejercicio piso el titulo con la
oracion corregida y la tarjeta paso a revelarle la respuesta al alumno; eso es
violacion de [F-RPRES-R001](#F-RPRES-R001), no excepcion.

<a id="DOUBT-REPARACION"></a>
### Doubt[DOUBT-REPARACION] - El requerimiento exige reparar el contenido ya degradado?
**Status**: RESOLVED

Las reglas de preservacion impiden el dano **futuro**, pero son relativas al
estado del elemento antes de la revision: si el elemento ya venia degradado,
preservar su estado significa preservar el dano. A la fecha hay 2.658 ejercicios
en ese estado.

- [ ] Opcion A: Fuera de alcance. El requerimiento cierra la fuente del dano; la
  reparacion del contenido ya afectado es un trabajo de saneamiento aparte.
- [x] Opcion B: En alcance, con una regla adicional que exige que no quede
  contenido degradado, **posterior** a la vigencia de la preservacion.
- [ ] Opcion C: En alcance como deteccion: el sistema informa que elementos estan
  degradados, sin repararlos.

**Answer**: Opcion B, capturada en [F-RPRES-R005](#F-RPRES-R005). El orden es
parte de la decision: reparar antes de que la preservacion este vigente no cierra
nada, porque la siguiente revision vuelve a degradar cada elemento que toque. La
reparacion es determinista: los 2.658 afectados tienen contraparte integra
disponible (ninguno huerfano), sus valores originales son uniformes y coinciden
con los de los 8.829 sanos, la cantidad de partes del enunciado no se altero y
ningun otro atributo se perdio. No hay caso ambiguo que requiera decision
editorial.

<a id="DOUBT-PARTE-NUEVA-COMPLETA"></a>
### Doubt[DOUBT-PARTE-NUEVA-COMPLETA] - "Ni ausente ni por defecto" aplica tambien a los atributos que una clase de parte no lleva?
**Status**: RESOLVED

La formulacion original de [F-RPRES-R006](#F-RPRES-R006) cerraba con "ninguna
parte nueva queda con atributos ausentes ni en el valor por defecto de su tipo".
Leida literal, esa frase choca con FEAT-QSENT: una parte de texto fijo **no lleva
respuestas** y una que las trae es dato invalido
([F-QSENT-R003](../2026-04-22.01_quiz-sentence-dsl/REQUIREMENT.md#F-QSENT-R003));
el texto de un hueco es **vacio por diseno**. Para esos atributos, "ausente" o
"vacio" no es degradacion: es el estado correcto.

- [ ] Opcion A: Dejar el enunciado como estaba y aclarar en el detalle que manda
  la homogeneidad, y que la segunda mitad es solo el respaldo para cuando no hay
  hermana de la misma clase.
- [x] Opcion B: Acotar explicitamente la segunda mitad al estado que cada clase de
  parte declara suyo, **ademas** de dejar escrita la subordinacion de la Opcion A.
- [ ] Opcion C: Retirar la segunda mitad y dejar la regla solo como homogeneidad.

**Answer**: Opcion B, con la lectura de la Opcion A incorporada como invariante 1.
La subordinacion sola no alcanza: en el caso sin hermana —que es justo donde el
respaldo entra— la frase seguia exigiendo poblar las respuestas de una parte de
texto fijo. Habria quedado una contradiccion latente, activa exactamente en el
escenario para el que se escribio el respaldo. Y la Opcion C tampoco sirve: sin
respaldo, una parte nueva sin hermana no tiene ninguna exigencia y vuelve a
habilitarse el modo de falla que origino la feature.

La acotacion cambia la **referencia**, no la exigencia: donde no hay original
contra que comparar ni hermana con que homogeneizar, el estado correcto lo define
la clase de la parte. La prohibicion de caer al valor por defecto sigue en pie
alli donde la clase exige valor propio.

**Consecuencia de cobertura**: el caso "parte de texto fijo sin hermana de su
clase" queda **determinado**, no vacio — su asercion es texto con valor propio y
respuestas ausentes. Amerita verificacion propia, simetrica a la que ya cubre el
caso del hueco sin hermana. La razon es la del propio requerimiento: es
precisamente la combinacion en la que las dos lecturas divergen, y sin una
verificacion que la fije, la lectura degradante vuelve a estar disponible para
quien lea la regla de apuro.

<a id="DOUBT-LAPS-ENUM"></a>
### Doubt[DOUBT-LAPS-ENUM] - Que pasa con la enumeracion de F-LAPS-R014?
**Status**: RESOLVED

[F-LAPS-R014](#F-LAPS-R014) enumera los atributos que deben preservarse en la
propuesta de correccion lexica, y es la enumeracion que dejo el hueco.
[F-RPRES-R001](#F-RPRES-R001) la subsume: si todo lo no propuesto se preserva,
listar identificadores, instrucciones y orden es redundante.

- [ ] Opcion A: Dejar F-LAPS-R014 como esta. Es redundante pero inofensiva, y sus
  verificaciones actuales siguen valiendo.
- [x] Opcion B: Reformular F-LAPS-R014 como caso particular de
  [F-RPRES-R001](#F-RPRES-R001), conservando su identificador y su enunciado a
  modo de ejemplo.
- [ ] Opcion C: Retirar F-LAPS-R014. Requiere re-anclar las verificaciones que
  hoy la referencian.

**Answer**: Opcion B. Se conserva el identificador porque lo citan verificaciones
vigentes y dos features posteriores (FEAT-PIPRE y FEAT-CDIFF heredan de el la
estabilidad de la identidad del elemento y la exclusion de propuestas
estructurales). El enunciado pasa de "lista de lo que se preserva" a "todo lo que
no esta en [F-LAPS-R013](#F-LAPS-R013) se preserva", con la lista anterior
degradada a ejemplos.

**Tambien se reformulo F-LAPS-R013**, y conviene decirlo explicito porque es gate
de dos journeys de FEAT-LAPS: su segundo item autorizaba a cambiar "la estructura
del ejercicio", y esa formula abarcaba tanto la composicion del enunciado (que si
es alcance de la correccion) como el tipo de formulario, su peso y sus etiquetas
(que nunca lo son). Sin desambiguar ese item, R014 podia reescribirse entera y el
dano seguiria formalmente autorizado. La reformulacion **acota** lo que R013
permite; no agrega nada nuevo, por lo que las verificaciones existentes —que
afirman que la oracion, los huecos, las respuestas y la traduccion pueden
diferir— siguen valiendo sin cambios.

## References

- **FEAT-COURSE** — Define el modelo del curso, la sub-estructura del formulario
  del ejercicio (de donde salen los nombres funcionales de los atributos
  degradados) y la idempotencia semantica del ciclo lectura-escritura, incluida
  la preservacion de valores vacios. Este requerimiento extiende ese criterio de
  identidad al elemento que una revision reemplaza. Citada por
  [F-RPRES-R002](#F-RPRES-R002) y el Contexto.
- **FEAT-DBSENT** — Contiene el precedente directo: la preservacion de las
  oraciones planas al persistir un curso, formulada para un unico dato. Este
  requerimiento generaliza ese razonamiento al resto del contenido. Citada por
  [F-RPRES-R003](#F-RPRES-R003) y el Contexto.
- **FEAT-LAPS** — Define que puede y que no puede diferir entre el elemento
  original y el propuesto para una correccion lexica. Su enumeracion de lo
  preservable es el hueco que este requerimiento cierra; sus reglas F-LAPS-R013 y
  F-LAPS-R014 quedan reformuladas como caso particular de
  [F-RPRES-R001](#F-RPRES-R001), conservando sus identificadores. Citada por
  [F-RPRES-R001](#F-RPRES-R001), el Contexto y
  [DOUBT-LAPS-ENUM](#DOUBT-LAPS-ENUM).
- **FEAT-QSENT** — Declara las clases de parte del enunciado (texto fijo y hueco)
  y que atributos lleva cada una y en que estado: un hueco con sus respuestas
  pobladas y su texto vacio, una parte de texto fijo con su texto propio y sin
  respuestas. Es la referencia que usa [F-RPRES-R006](#F-RPRES-R006) para definir
  "completo" cuando una parte nueva no tiene hermana de su clase; este
  requerimiento no la redefine ni la amplia. Citada por
  [F-RPRES-R006](#F-RPRES-R006) y
  [DOUBT-PARTE-NUEVA-COMPLETA](#DOUBT-PARTE-NUEVA-COMPLETA).
- **FEAT-REVAPR** — Define el momento en que una propuesta aprobada se aplica al
  curso y este se persiste: el punto exacto donde estas reglas se verifican.
  Citada por el Contexto.
- **FEAT-REVBYP** — Define la mecanica de sustitucion del elemento del curso por
  su version revisada. Citada por el Contexto.
- **FEAT-KTLR** — Revision de la etiqueta de un knowledge: el segundo tipo de
  correccion al que aplican estas reglas, y el que dispara la alineacion de
  titulos. Su regla F-KTLR-R007 afirmaba que la correccion de titulo "no altera
  sus quizzes"; quedo reformulada (mismo identificador) para admitir la
  alineacion del titulo y conservar lo que si protege: que el contenido de los
  quizzes provenga del estado actual del curso y no de la version congelada al
  construir la propuesta. Citada por [F-RPRES-R004](#F-RPRES-R004) y
  [DOUBT-TITULO-DENORMALIZADO](#DOUBT-TITULO-DENORMALIZADO).
