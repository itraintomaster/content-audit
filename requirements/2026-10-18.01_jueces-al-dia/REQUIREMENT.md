---
feature:
  id: FEAT-ALDIA
  code: F-ALDIA
  name: Jueces al día - cada veredicto dice si lo dio el juez de hoy, y lo de antes no publica sin pedirlo por nombre
  priority: critical
---

# Jueces al día: cada veredicto dice si lo dio el juez de hoy, y lo de antes no publica sin pedirlo por nombre

## TL;DR

**Qué**: Cada veredicto dice si lo dio la versión de hoy de su juez o una anterior, con cualquier motor. La versión de un juez
cambia sólo si cambia algo que corre al medir. Las versiones se nombran por la fecha en que aprobaron. Se puede volver
a medir sólo lo de antes. Publicar frena mientras un juez no esté al día en lo que cambió y lo nombra; la versión
anterior se acepta sólo pidiéndola por nombre, y queda anotada.

**Por qué**: José cambió la guía de la miniteoría el 13/10, la versión nueva aprobó su examen y él creyó haberla corrido
sobre Corrección 1. No la había corrido: 82 de sus 83 temas tienen el veredicto de una versión anterior, y el análisis,
Correcciones y publicar los dan por vigentes. Al cerrar o analizar se reusa el veredicto del mismo juez, contenido y
motor sin mirar su versión ([F-EVCOST-R006](#F-EVCOST-R006)). Si las dos puntas tienen la misma versión vieja, la tanda
se compara y publica sin aviso.

Lo que se ve en el tablero está en [FEAT-TALDIA](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md). Fuente:
[propuesta de producto «Jueces al día»](../../docs/propuestas/jueces-al-dia.html), v3 del 18/10, con sus cinco decisiones.

## Recorrido de uso (estado actual)

Cómo José cambia hoy un juez y publica. El caso es la miniteoría sobre Corrección 1 de la tanda A1.

1. Cambia la guía, el formato del veredicto y las pruebas de la miniteoría → la versión cambia ✓. ⚠️ También cambiaría
   si tocara sólo sus pruebas (`eval/`), que no corren al medir: pasó en 3 de los 44 cambios a los jueces.
2. La versión nueva rinde su examen y aprueba (13/10, 22:12) ✓.
3. Analiza la corrección (14/10, 01:16). ⚠️ El análisis reusa los veredictos de las versiones del 5/10 y del 13/10,
   13:00, y cuenta la miniteoría como medida. Nada dice que 82 de 83 temas son de antes.
4. Mira Analizadores. ⚠️ Lee «738388a5 · fecha no registrada» o «versiones mezcladas», sin decir cuál es la de hoy.
5. Publica → `tray publish`. ⚠️ En 14 de los 21 cambios aprobados, la miniteoría tiene la misma versión del 5/10 en las
   dos puntas: se comparan y pasarían sin aviso. Donde las puntas difieren, el veredicto dice «… cambios sin evaluar por
   algún analizador», sin nombrar al juez ni el motivo.

**Punto de fricción**: pasos 3 y 5. Lo de antes se reusa en silencio y, con la misma versión vieja en las dos puntas,
publica.

**Cambio mínimo necesario**: que cada veredicto diga si está al día y que publicar no cuente lo de antes, salvo que
José lo pida por nombre. Además, que se pueda volver a medir sólo lo de antes, con estimación gratis y tope.

## Reglas de Negocio

Acá, un **juez** es un analizador que juzga con un modelo o con una herramienta que rinde examen. Su **motor** es el
proveedor, el modelo y el esfuerzo efectivos. Su **identidad** es su versión más su motor
([F-JEXAM-R014](#F-JEXAM-R014)). La **versión de hoy** de un juez es la que da hoy su definición
([F-ALDIA-R001](#F-ALDIA-R001)). Las **dos puntas** de un cambio son la base y la corrección
([F-NOEMP-R010](#F-NOEMP-R010)). Lo **de antes** es el veredicto que no está al día ([F-ALDIA-R003](#F-ALDIA-R003)).

<a id="F-ALDIA-R001"></a>
### Rule[F-ALDIA-R001] - La versión de un juez cambia sólo si cambia algo que corre al medir
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La versión de un juez que se define en su carpeta sale de todo lo que hay en ella, salvo sus pruebas (`eval/`), que
> nunca corren al medir el curso. Invariantes:
> 1. cambiar sólo las pruebas no cambia la versión: no pide examen ni vuelve nada «de antes»;
> 2. cambiar cualquier otra cosa de la carpeta cambia la versión, aunque sea un comentario. Esto incluye la guía, el
>    formato o el control del veredicto, un script, la definición o un archivo nuevo fuera de `eval/`;
> 3. la versión se calcula igual al rendir el examen, al medir, al reusar y al decir si un veredicto está al día;
> 4. el juez de consigna aplica la misma exclusión de `eval/`, incluidos sus lectores auxiliares. El proveedor,
>    modelo y esfuerzo efectivos se conservan como identidad del motor y requieren su propia aprobación.

<details><summary>Detail</summary>

Hoy cuenta todo archivo de la carpeta, `eval/` incluido. Así lo dicen las definiciones de ocho jueces: «Todo archivo de
esta carpeta, eval/ incluido, entra en la versión del juez». De los 44 cambios a los jueces en la historia del
repositorio, 39 tocaron la guía, 3 tocaron sólo sus pruebas y 2, sólo scripts. Con la regla de hoy, esos 3
pidieron examen y volver a medir sin que cambiara nada de lo que mide el juez.

La carpeta del juez no tiene casos del examen: el oro y los casos viven con quien mide ([F-JEXAM-R001](#F-JEXAM-R001)).
Nada de lo que corre al medir lee `eval/`.

Las pruebas de `eval/` son casi siempre sin modelo: corren sobre salidas
grabadas. Una excepción es la del juez de pares casi repetidos, que además tiene un modo en vivo, sólo a pedido y con
tope, sobre pares inventados. Tampoco ese modo corre al medir, así que la regla es la misma: lo que decide es qué corre
al medir, no si la prueba consulta.

Se cuenta todo salvo las pruebas, y no se lista lo que cuenta, para que nadie tenga que marcar si un cambio cuenta. Si
mañana un archivo nuevo corre al medir, cuenta solo. Marcarlo a mano es volver a creer, que es lo que falló con la
miniteoría (decisión 4 de la propuesta, 18/10).

Precisa qué es «la versión completa del proceso» de [F-JEXAM-R014](#F-JEXAM-R014) para los jueces de carpeta. Lo demás
de esa regla queda igual: cambiar la versión o el motor pide un examen propio antes de consultar el curso.

**Criterio**: en la carpeta de la miniteoría, cambiar `eval/fixtures.json` deja la versión igual. Cambiar
`nodes/evaluate.md`, `schema/verdict.schema.json`, `scripts/gate_verdict.sh` o la definición la cambia.

</details>

<a id="F-ALDIA-R002"></a>
### Rule[F-ALDIA-R002] - Pasar a la cuenta nueva no hace volver a medir nada
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Al pasar a la cuenta de [F-ALDIA-R001](#F-ALDIA-R001), cada juez conserva su examen y sus veredictos. La versión que
> tiene ese día con la cuenta vieja y la que tiene con la nueva son la misma versión, una sola vez. Invariantes:
> 1. la equivalencia no consulta ni paga: 0 consultas, y nada de lo que estaba al día pasa a ser de antes;
> 2. cubre sólo ese cambio: sacar las pruebas de la cuenta y corregir el texto de la definición de cada juez que dice que
>    entran. Cualquier otro cambio hecho a la vez da una versión nueva, que rinde su examen;
> 3. la equivalencia queda escrita con el juez, las dos versiones y la fecha, y se ve en el historial del examen
>    ([F-JEXAM-R015](#F-JEXAM-R015));
> 4. las equivalencias escritas no se cambian ni se borran, y no se agregan otras después del corte de cada juez;
> 5. las versiones anteriores no cambian: una que difería de la de ese día sólo en sus pruebas sigue siendo anterior.

<details><summary>Detail</summary>

Sin la equivalencia, la cuenta nueva da otra versión a todos los jueces a la vez. Todos sus exámenes y veredictos
quedarían de una versión anterior, y volver a ponerlos al día costaría miles de consultas por un cambio que no mide nada
distinto. Hay un precedente: cuando la versión dejó de llevar el motor adentro, la versión vieja quedó como alias de
la nueva, sólo para ese cambio.

Corregir el texto de la definición también cambia la versión ([F-ALDIA-R001](#F-ALDIA-R001), inv. 2). Por eso la
equivalencia cubre los dos cambios juntos y nada más. Se calcula sobre la carpeta de cada juez tal como está ese día,
también si tiene cambios sin guardar en el repositorio.

La corrección de Consigna del 22/10/2026 tiene su propio corte, separado del registro del 18/10. Conserva únicamente
la versión aprobada `533462b9…` como equivalente del proceso `67dc36e1…`: se verificó que todos los archivos fuera de
`eval/` son idénticos. No se amplía el corte anterior, no se reescriben aprobaciones y no se heredan motores distintos.

**Criterio**: el día del cambio, la miniteoría tiene la versión aprobada el 13/10 a las 22:12. Después del cambio tiene
otra versión, con el mismo examen aprobado. El tema que esa versión midió sigue al día, y la estimación de volver a
medir lo de antes sigue en 82 temas de la corrección, no en 83. Si ese mismo día también cambió su guía, la versión
nueva sale sin examen.

</details>

<a id="F-ALDIA-R003"></a>
### Rule[F-ALDIA-R003] - Cada veredicto dice si está al día
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un veredicto está **al día** si lo dio la versión de hoy de su juez y esa versión está aprobada, con cualquier motor.
> Si lo dio otra versión, es **de antes** por **versión anterior**. El motor nunca hace que un veredicto sea de antes
> (invariante 7).
>
> Una versión **aprobó con un motor** si su último examen con ese motor aprobó, o si José la promovió con ese motor y
> ningún examen medido después con ese motor la reemplazó. Con un motor alcanza: la versión queda **habilitada** con
> cada motor con el que aprobó, sea o no el que se usa por omisión.
>
> Un ejercicio o tema sin veredicto de ese juez está **sin medir**. Una **abstención** —el juez miró el ejercicio y
> dijo que no aplica, que le falta contexto o que hay que revisarlo, sin dejar puntaje— es un veredicto, y lleva su
> marca como cualquier otro ([DOUBT-ALDIA-ABSTENCION](#DOUBT-ALDIA-ABSTENCION)). Invariantes:
> 1. la marca se calcula contra la versión de hoy, nunca contra la otra punta: si las dos puntas tienen la misma versión
>    vieja, las dos son de antes;
> 2. marcar no cambia qué veredicto se reusa: se sigue reusando como hoy ([F-EVCOST-R006](#F-EVCOST-R006)), y la marca
>    va con él;
> 3. un veredicto sin procedencia es de antes, y su causa es «procedencia desconocida» ([F-JEXAM-R014](#F-JEXAM-R014));
> 4. si la versión de hoy no aprobó con ningún motor, ningún veredicto de ese juez está al día, y el juez se nombra
>    «sin aprobar» ([F-CJUEZ-R004](#F-CJUEZ-R004)). Si aprobó con alguno, no es «sin aprobar», aunque no sea el motor
>    que se usa por omisión;
> 5. calcular la marca no consulta ni escribe nada;
> 6. una abstención guardada nunca está sin medir: volver a medir sin forzar la reusa y da la misma. Vale igual en la
>    tabla de jueces, en la cuenta del cierre, en la estimación de un diálogo de medir y en el resumen de cada corrida.
> 7. sólo la versión decide si un veredicto está al día; el motor no. Lo que dio la versión de hoy cuenta igual con
>    cualquier motor, y si dos motores opinaron con ella, valen los dos. Lo que dio una versión anterior es de antes, con
>    cualquier motor, y se nombra por su versión ([F-ALDIA-R004](#F-ALDIA-R004)). No hay causa «otro motor»: ni en la
>    marca, ni en la cuenta, ni en lo que se le muestra a José. Lo de antes sigue faltando medir en Correcciones
>    ([F-TALDIA-R006](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R006)).

<details><summary>Detail</summary>

Que lo de antes se siga reusando es deliberado: volver a medir todo con cada versión es lento y caro, y
[F-EVCOST-R006](#F-EVCOST-R006) lo descarta. Lo que cambia es que ya no se reusa en silencio. La marca va con el
veredicto a donde vaya: el análisis, las opiniones de Correcciones, el cierre y publicar.

Hasta el 19/10 la causa se elegía mirando primero el motor. José lo sacó el 20/10 (invariante 7): «Hay que corregir el
rótulo y tomarla como que no están medidos sólo porque el examen cambió. No importa que se mida con dos motores
distintos, mientras estén al día con el examen». Lo que valida a un juez es el examen de su versión; el motor elige
con qué se consulta ([F-JEXAM-R003](#F-JEXAM-R003)), pero no hace viejo lo que ya dijo.

**Criterio**: Corrección 1 de la tanda A1, al 18/10. La miniteoría de hoy es la versión aprobada el 13/10 a las 22:12:

| Estado | Al día | Versión anterior | Sin medir |
|---|---:|---:|---:|
| La base (83 temas) | 0 | 83 (la del 5/10) | 0 |
| Corrección 1 (83 temas) | 1 | 82 (77 de la del 5/10, 5 de la del 13/10, 13:00) | 0 |

La naturalidad tiene, en la base, 2.333 ejercicios de procedencia desconocida y ninguno al día.

**Criterio de los motores**, al 19/10: la miniteoría de hoy aprobó a las 11:30 con Gemini 3.8 Flash y nunca rindió
con ChatGPT, que es el motor por omisión. Está habilitada con Gemini, no es «sin aprobar», y lo que ella mida con ChatGPT
también está al día (invariante 7). Naturalidad idiomática y Calidad didáctica A1 no rindieron su examen con la versión de hoy:
José las promovió el 14/10 con ChatGPT. Están habilitadas con ChatGPT, y lo que esa versión midió con ChatGPT está al día.

**Criterio de sólo la versión** (invariante 7), Corrección 1 de la tanda A1 al 20/10: la miniteoría de hoy es la del
19/10, aprobada sólo con Gemini. Un tema lo midió ella con Gemini y está al día. Los otros 82 los midieron con ChatGPT
la del 5/10, la del 13/10, 13:00 y la del 13/10, 22:12. Hasta el 19/10 se leían «82 de otro motor (gpt-6.1-sol en
codex-cli)», como si la versión de hoy los hubiera medido con otro motor, y José los tomó por medidos. Ahora se leen
«82 de versión anterior», cada uno con el nombre de su versión, y siguen faltando medir con la del 19/10. Si la del
19/10 los mide con ChatGPT, quedan al día igual que con Gemini.

**Criterio de la abstención** (invariante 6), Corrección 1 de la tanda A1 al 19/10: el pragmatismo de hoy (la del
14/10) dejó 2.402 veredictos con su motor aprobado. 88 son abstenciones: 81 «no aplica» (ejercicios de clasificar, de
elegir *a*/*an* o de escribir la forma en *-ing*) y 7 «falta contexto». La tabla dice al día 2.402, de los que 88 se
abstuvieron, y sin medir 0. Hoy dice al día 2.314 y sin medir 88, aunque 12 corridas sueltas los volvieron a pedir
entre el 14/10 y el 18/10 sin gastar nada ni cambiar nada (brief 158).

</details>

<a id="F-ALDIA-R004"></a>
### Rule[F-ALDIA-R004] - Cada versión se nombra por la fecha en que aprobó
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando content-audit le nombra a José una versión de un juez, la nombra por la fecha en que aprobó su examen por
> primera vez, en la hora local: «la del 5/10». Eso vale para informes, negativas y la salida de publicar. Invariantes:
> 1. si otra versión de ese juez aprobó por primera vez ese mismo día, el nombre lleva también la hora: «la del 13/10,
>    13:00» y «la del 13/10, 22:12». El día se cuenta en la hora local, no en UTC. En una marca suelta, el nombre se lee
>    «versión del 5/10»;
> 2. una versión re-aprobada se nombra por su primera aprobación, no por la última;
> 3. si la aprobación no es del año en curso, el nombre lleva el año [ASSUMPTION];
> 4. el resto se nombra así: una versión que nunca aprobó, «sin aprobar» y su código corto; uno sin procedencia,
>    «procedencia desconocida». El motor no da nombre a lo de antes ([F-ALDIA-R003](#F-ALDIA-R003), inv. 7);
> 5. el código entero sigue en la salida JSON y en el detalle, para buscarlo;
> 6. una versión de un juez nunca se nombra con un número: «versión 5» es del contenido;
> 7. una versión que José promovió se nombra por lo primero que pasó: su primera aprobación o su promoción. La
>    naturalidad de hoy, promovida el 14/10 y nunca aprobada en un examen, es «la del 14/10».

<details><summary>Detail</summary>

Las 17 versiones aprobadas tienen en su historial de examen el día y la hora de su aprobación
([F-JEXAM-R015](#F-JEXAM-R015)). La fecha de aprobación no es la fecha de medición. «Fecha no registrada», en la vista de
analizadores, dice cuándo se midió, y eso sigue aparte.

El día se cuenta en la hora local porque es la que ve José. La miniteoría vieja aprobó el 5/10 a las 23:14 hora local,
que en UTC ya es el 6/10. Las dos del 13/10 aprobaron a las 13:00 y a las 22:12 hora local. Contado en UTC, la segunda
caería el 14/10 y no llevaría hora. Contado en la hora local, las dos son del 13/10 y la hora es lo que las distingue.

Por qué la fecha y no un número: dice qué tan vieja es la versión, y no se confunde con la versión del contenido que
muestra la puerta de Publicar (decisión 5 de la propuesta, 18/10).

**Criterio**: las versiones de la miniteoría se nombran «la del 5/10», «la del 13/10, 13:00» y «la del 13/10, 22:12».
Esta última fue re-aprobada el 14/10 a las 12:29 y se sigue nombrando por el 13/10. En el JSON, cada versión trae su
código entero y el instante de su primera aprobación.

</details>

<a id="F-ALDIA-R005"></a>
### Rule[F-ALDIA-R005] - El análisis y el cierre dicen, por juez, cuánto está al día
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Leer el análisis vigente dice, por juez y por estado (la base y la corrección), cuántos ejercicios o temas están al
> día, cuántos son de antes y cuántos están sin medir. Lo de antes va por causa y por versión o motor, cada uno con su
> nombre ([F-ALDIA-R004](#F-ALDIA-R004)) y su código. También dice cuál es la versión de hoy de cada juez, cuándo aprobó y con qué motores
> está habilitada ([F-ALDIA-R003](#F-ALDIA-R003)). Invariantes:
> 1. las tres cuentas suman la unidad del juez en ese estado: ejercicios, o temas en la miniteoría;
> 2. cada veredicto del análisis trae su marca y su versión. Esto incluye los que leen las opiniones de Correcciones;
> 3. el informe de un cierre y el de cada análisis dicen, por juez, cuánto de lo reusado es de antes, con su nombre:
>    «miniteoría: reusa 82 temas de antes (77 de la del 5/10, 5 de la del 13/10, 13:00)»;
> 4. leer no consulta ni escribe nada;
> 5. lo al día y lo de antes dicen cuántas son abstenciones y por qué, sin sacarlas de su cuenta: «al día 2.402; se
>    abstuvo en 88 (81 no aplica, 7 falta contexto)». Las tres cuentas siguen sumando la unidad.

<details><summary>Detail</summary>

Es lo que el tablero dibuja en el Mapa, en Correcciones y en Analizadores
([FEAT-TALDIA](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md)). Lo calcula content-audit, no el tablero,
porque es quien sabe la versión de hoy y los motores con los que aprobó. Mantiene
[F-CJUEZ-R005](#F-CJUEZ-R005): el informe del cierre ya dice lo reusado por juez, y ahora dice también cuánto es de
antes.

**Criterio**: la lectura de Corrección 1 da, para la miniteoría, los números del criterio de
[F-ALDIA-R003](#F-ALDIA-R003). Su versión de hoy se nombra «la del 13/10, 22:12». El análisis A4 del 14/10 dice
«miniteoría: reusa 82 temas de antes», en vez de contarla como medida sin más.

</details>

<a id="F-ALDIA-R006"></a>
### Rule[F-ALDIA-R006] - Qué cambió entre dos versiones de un juez
**Severity**: normal | **Validation**: AUTO_VALIDATED

> Para dos versiones de un juez, content-audit dice qué archivos de su carpeta cambiaron, separados en dos: los que
> cuentan y las pruebas de `eval/`, que no cuentan ([F-ALDIA-R001](#F-ALDIA-R001)). Invariantes:
> 1. cada versión que rinde su examen deja escrita la lista de sus archivos con la huella de cada uno;
> 2. para una versión sin lista, se busca en la historia del repositorio una foto de la carpeta que dé esa versión. Si
>    no hay ninguna, dice «no se sabe qué cambió», sin inventar una lista;
> 3. decirlo no consulta nada.

<details><summary>Detail</summary>

Con la regla de hoy, saber qué cambió entre dos versiones exige rearmar las huellas a mano desde la historia. Así se
encontró qué cambió en la miniteoría. La lista de archivos al rendir el examen lo resuelve de ahí en adelante; la
búsqueda en la historia cubre las versiones de antes.

**Criterio**: entre la miniteoría del 13/10, 13:00 y la del 13/10, 22:12, cuentan `nodes/evaluate.md`,
`schema/verdict.schema.json` y `scripts/gate_verdict.sh`. `eval/fixtures.json` sale aparte, como prueba que no
cuenta.

</details>

<a id="F-ALDIA-R007"></a>
### Rule[F-ALDIA-R007] - Se puede volver a medir sólo lo de antes
**Severity**: critical | **Validation**: AUTO_VALIDATED

> A un juez se le puede pedir que vuelva a medir sólo lo de antes en la corrección, en la base o en las dos. Consulta
> únicamente los ejercicios o temas cuyo veredicto es de antes, con la versión de hoy y un motor con el que aprobó, y
> reusa lo que ya está al día. Invariantes:
> 1. primero se estima gratis, por estado: cuántos son de antes, cuántos al día se reusan y cuántas consultas faltan.
>    Con las dos, lo que se lee igual en la base y en la corrección se mide una vez;
> 2. no consulta sin un tope pedido y nunca pasa ese tope. Si el tope no alcanza, lo que no se midió sigue de antes y el
>    informe dice cuánto falta ([F-CJUEZ-R003](#F-CJUEZ-R003));
> 3. si la versión de hoy no aprobó con el motor elegido, no consulta y dice por qué ([F-JEXAM-R003](#F-JEXAM-R003)).
>    La estimación dice además con qué motores sí está habilitada, para poder elegir uno de ellos;
> 4. lo sin medir no entra. Completarlo es otro pedido, que ya existe;
> 5. en la base mide todo lo de antes, no sólo los temas o ejercicios que la tanda cambió;
> 6. los veredictos de antes no se borran: quedan como historia ([F-EVCOST-R006](#F-EVCOST-R006));
> 7. un motor que no aprobó con la versión de hoy no se acepta, y no hay cambio silencioso a otro.
> 8. al terminar, lo medido se integra solo en cada estado pedido. Reemplaza sólo los veredictos de antes de ese juez,
>    sobre el mismo contenido, y queda registrado con quién lo pidió y cuándo, como una integración a mano. Se puede
>    quitar como cualquier otra.

<details><summary>Detail</summary>

Hoy forzar es todo o nada: vuelve a medir también lo que ya está al día. Medir la base entera es la decisión 2 de la
propuesta (18/10). Al publicar, cada cambio se compara contra la base con el mismo juez en las dos puntas
([F-ALDIA-R008](#F-ALDIA-R008)), y 79 de los 82 temas de antes tienen algún cambio: medir la base por partes ahorra
casi nada y suma un paso.

El motor es el que elija el operador, como en todo proceso con modelos ([F-JEXAM-R013](#F-JEXAM-R013)). Tiene que ser
uno con el que la versión de hoy aprobó. Que lo medido se integre solo al terminar lo decidió José el 18/10
([DOUBT-ALDIA-INTEGRAR](#DOUBT-ALDIA-INTEGRAR)).

**Criterio**: para la miniteoría en las dos puntas de Corrección 1, la estimación dice 82 temas de antes en la
corrección, 1 al día que se reusa y 83 de antes en la base: unas 165 consultas. Con tope 0 no consulta. Si la versión
de hoy no hubiera aprobado, dice «la versión de hoy de la miniteoría no aprobó su examen» y no consulta.

</details>

<a id="F-ALDIA-R008"></a>
### Rule[F-ALDIA-R008] - Publicar frena mientras un juez no esté al día en lo que cambió, y lo nombra
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Publicar exige que cada juez que lee algo de lo que la tanda cambió esté al día en las dos puntas de cada cambio que
> juzga. Si no, no publica. La negativa dice cuántos jueces no están al día y nombra a cada uno con cuántos cambios y por
> qué:
> - «la del 5/10 en los dos lados»: la misma versión de antes en las dos puntas;
> - «una versión distinta en cada lado», o «la base es de llama3 local»: las puntas difieren;
> - «sin medir en la base» o «sin medir en la corrección».
>
> Termina con cómo destrabarlo: volver a medir lo de antes ([F-ALDIA-R007](#F-ALDIA-R007)) o, sólo si la misma versión de
> antes está en las dos puntas, pedirla por nombre ([F-ALDIA-R009](#F-ALDIA-R009)). Invariantes:
> 1. el curso no se toca;
> 2. frena con cualquier veredicto: un «pasa» con un juez de antes no publica. El veredicto de la comparación no cambia;
> 3. un juez sin aprobar se nombra como dice [F-CJUEZ-R004](#F-CJUEZ-R004) y no se repite acá;
> 4. un juez que no lee nada de lo que la tanda cambió no frena;
> 5. la bandeja (`tray show`) dice lo mismo antes de publicar, en texto y en JSON, sin consultar;
> 6. la negativa nombra cada juez una vez, en el orden del catálogo, con su nombre visible
>    ([F-FILAS-R001](#F-FILAS-R001)), y cada versión con el nombre de [F-ALDIA-R004](#F-ALDIA-R004).

<details><summary>Detail</summary>

Hay dos casos, y hoy se tratan distinto. Si las puntas difieren, la comparación ya queda sin decidir y frena, pero sin
decir quién ni por qué ([F-ALDIA-R010](#F-ALDIA-R010)). Si las dos puntas tienen la misma versión vieja, se compara y
pasa callada. Ese es el caso de la miniteoría: José creía haberla medido. Frenar no suma consultas: con la base marcada,
es la misma medición, hecha antes (decisión 3 de la propuesta, 18/10).

Suma una condición a [F-PUBPROD-R005](#F-PUBPROD-R005), que sigue igual: «pasa» y compuerta. La negativa usa las mismas
palabras desde la bandeja y desde la tanda.

**Criterio**, con Corrección 1 al 18/10:

```
$ content-audit tray publish
✗ No se publica: 2 jueces no están al día.
  Naturalidad idiomática: la base es de llama3 local (21 cambios)
  Miniteoría con error: la del 5/10 en los dos lados (14 cambios); una versión distinta en cada lado (7 cambios)
Volvé a medir lo de antes, o pedí la versión anterior por nombre: --accept-previous mini-theory
```

[ASSUMPTION] en la forma exacta de las líneas.

</details>

<a id="F-ALDIA-R009"></a>
### Rule[F-ALDIA-R009] - La versión anterior se acepta sólo pidiéndola por nombre, y queda anotada
**Severity**: critical | **Validation**: AUTO_VALIDATED

> `tray publish --accept-previous <juez>[,<juez>…]` acepta, para cada juez nombrado, los cambios que tienen la misma
> versión de antes en las dos puntas. Invariantes:
> 1. no acepta un cambio con una versión distinta en cada lado ni uno sin medir:
>    esos siguen frenando, y la negativa lo dice;
> 2. no acepta un juez sin aprobar: manda [F-CJUEZ-R004](#F-CJUEZ-R004);
> 3. un nombre que no es de un juez de la tanda se rechaza sin publicar. Un juez que ya está al día no se anota, y la
>    salida lo dice [ASSUMPTION];
> 4. la publicación y su lista ([F-PUBPROD-R001](#F-PUBPROD-R001)) anotan cada juez aceptado: qué versión, con su nombre
>    y su código, en cuántos cambios, quién lo pidió y cuándo. Lo anotan como anotan quién aprobó;
> 5. no hay una opción general ni una confirmación que acepte todo: cada juez se escribe;
> 6. pedirlo no consulta nada.

<details><summary>Detail</summary>

Es la regla de la decisión 1 aplicada a publicar: lo de antes no cuenta por defecto, pero pedido a propósito sí, y
queda anotado. Sirve para no pagar miles de consultas por un retoque que José sabe que no cambia lo que mide el juez.
Con la naturalidad, por ejemplo, serían unas 2.400.

**Criterio**: si en Corrección 1 sólo faltara la miniteoría con la del 5/10 en los dos lados,
`tray publish --accept-previous mini-theory` publica y dice «Anotado: miniteoría, la del 5/10, en 14 cambios». La lista
de la publicación guarda lo mismo con el código de esa versión. Hoy, en cambio, sigue frenando: los 7 cambios con una
versión distinta en cada lado y los 21 de la naturalidad no se aceptan por nombre. `--accept-previous miniteoria` se
rechaza: no es el nombre de un juez de la tanda.

**Error**: "«<nombre>» no es un juez de la tanda <id>. Sus jueces: <lista>." [ASSUMPTION]

</details>

<a id="F-ALDIA-R010"></a>
### Rule[F-ALDIA-R010] - «No se puede decidir» nombra al juez y el motivo
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando hay cambios sin comparar porque un juez no los evaluó en las dos puntas, o los evaluó con identidades distintas
> ([F-NOEMP-R010](#F-NOEMP-R010)), el veredicto los nombra por juez. Dice cuántos cambios quedaron sin comparar y, por
> cada juez, cuántos y por qué, en vez de «por algún analizador». Invariantes:
> 1. el tipo del veredicto, la cuenta de cambios y los pisos sin decidir no cambian ([F-FILAS-R007](#F-FILAS-R007));
> 2. cada juez va una vez, en el orden del catálogo, con su nombre visible y uno de estos motivos: «sin medir en la
>    base», «sin medir en la corrección», «una versión distinta en cada lado» o «procedencia desconocida en la base» (o
>    «en la corrección»). Desde el 20/10 no hay motivo de motor ([F-ALDIA-R003](#F-ALDIA-R003), inv. 7);
> 3. el JSON del veredicto trae lo mismo por juez: su clave, su nombre visible, cuántos cambios y el motivo;
> 4. la negativa de publicar copia ese detalle.

<details><summary>Detail</summary>

Hoy el detalle dice «N cambios sin evaluar por algún analizador» y lista los cambios, no los jueces. Para saber cuál
juez frena hay que abrir la comparación fila por fila. Cambia el texto del veredicto sólo en esa parte, que antes
[F-FILAS-R007](#F-FILAS-R007) dejaba fija.

**Criterio**: con Corrección 1 al 18/10, el detalle dice «21 cambios sin comparar: la naturalidad en los 21 (otro
motor en la base) y la miniteoría en 7 (una versión distinta en cada lado)».

</details>

## Contexto

| Qué | Cuánto |
|---|---|
| Temas de Corrección 1 con la miniteoría de una versión anterior | 82 de 83 |
| Temas de la base con la miniteoría de una versión anterior | 83 de 83 |
| Cambios aprobados con la misma miniteoría vieja en las dos puntas (pasarían sin aviso) | 14 de 21 |
| Cambios aprobados sin comparar por la naturalidad (base de un modelo local) | 21 de 21 |
| Cambios a los jueces que tocaron sólo sus pruebas (`eval/`) | 3 de 44 |
| Versiones aprobadas con día y hora de aprobación registrados | 17 de 17 |
| Volver a medir la miniteoría, corrección y base | unas 165 consultas |
| Volver a medir la base de la naturalidad | unas 2.400 consultas |

Las versiones de la miniteoría, en hora local:

| Nombre | Código | Aprobó | Midió en Corrección 1 |
|---|---|---|---|
| la del 5/10 | `738388a5` | 5/10, 23:14 | 77 temas, y los 83 de la base |
| la del 13/10, 13:00 | `33e026fa` | 13/10, 13:00 | 5 temas |
| la del 13/10, 22:12 (la de hoy) | `1eee07a9` | 13/10, 22:12; re-aprobada 14/10, 12:29 | 1 tema |

Medir no está autorizado por este requisito: las 165 consultas de la miniteoría y las 2.400 de la naturalidad esperan el
OK de José, con el número delante.

## Alcance

- **Adentro**: qué entra en la versión de un juez de carpeta, y la equivalencia de una vez; la marca al día, versión
  anterior y sin medir en cada veredicto, en el análisis y en el cierre; el nombre de cada versión por su
  fecha de aprobación; qué archivos cambiaron entre dos versiones; volver a medir sólo lo de antes; que publicar frene y
  nombre al juez; aceptar la versión anterior por nombre y anotarlo; nombrar al juez en «no se puede decidir».
- **Afuera**: lo que muestra el tablero
  ([FEAT-TALDIA](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md)); correr de verdad las mediciones;
  cambiar un examen o su oro; cambiar cómo se reusa lo de antes fuera de publicar ([F-EVCOST-R006](#F-EVCOST-R006)).

## User Journeys

### Journey[F-ALDIA-J001] - Publicar una tanda después de cambiar un juez
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-ALDIA-J001
    name: Publicar una tanda después de cambiar un juez
    flow:
      - id: cambiar
        action: "José cambia la guía de un juez y la versión nueva aprueba su examen"
        gate: [F-ALDIA-R001, F-ALDIA-R004]
        outcomes:
          - when: "Sólo cambió sus pruebas (eval/)"
            then: igual
          - when: "Cambió algo que corre al medir"
            then: publicar
      - id: igual
        action: "La versión no cambia: no hay examen nuevo y nada queda de antes"
        gate: [F-ALDIA-R001]
        result: success
      - id: publicar
        action: "José publica la tanda con `tray publish`"
        gate: [F-ALDIA-R003, F-ALDIA-R008, F-ALDIA-R010]
        outcomes:
          - when: "El juez está al día en las dos puntas de lo que cambió"
            then: publicada
          - when: "El juez es de antes en alguna punta"
            then: frena
      - id: frena
        action: "No publica: nombra al juez, cuántos cambios y por qué, y cómo destrabarlo"
        gate: [F-ALDIA-R008]
        outcomes:
          - when: "José pide volver a medir lo de antes, con tope"
            then: medir
          - when: "Las dos puntas tienen la misma versión de antes y José la pide por nombre"
            then: por_nombre
      - id: medir
        action: "Se estima gratis; con el tope pedido se mide sólo lo de antes, en la corrección y en la base"
        gate: [F-ALDIA-R007]
        outcomes:
          - when: "El tope alcanzó"
            then: publicar
          - when: "El tope no alcanzó"
            then: frena
      - id: por_nombre
        action: "`tray publish --accept-previous <juez>` publica y anota la versión, los cambios y quién lo pidió"
        gate: [F-ALDIA-R009]
        result: success
      - id: publicada
        action: "La tanda se publica como siempre"
        gate: [F-ALDIA-R008]
        result: success
```

## Open Questions

<a id="DOUBT-ALDIA-INTEGRAR"></a>
### Doubt[DOUBT-ALDIA-INTEGRAR] - ¿Lo que se vuelve a medir se integra solo al terminar?
**Status**: RESOLVED (2026-10-18)

Hoy una medición suelta queda aparte hasta que José la integra a mano en el análisis vigente, con autor, huella y
revisión. La propuesta dice que, al terminar de volver a medir lo de antes, el Mapa pierde el aviso, y eso supone que
lo medido ya cuenta.

- [x] Opción A (recomendada): al terminar, lo medido se integra solo en cada estado pedido. Reemplaza sólo
  los veredictos de antes de ese juez, sobre el mismo contenido. La integración queda registrada con quién la pidió y
  cuándo, como una integración a mano, y se puede quitar como cualquier otra.
- [ ] Opción B: queda suelta, como hoy. El aviso cambia a «medido, falta integrar» hasta que José la integre.

**Answer**: **Opción A** (José, 18/10, con la recomendación): lo que se vuelve a medir se integra solo al terminar y
reemplaza sólo lo de antes de ese juez. José ya confirmó el número al pedirla. Con B hacía falta un paso más, y
entretanto lo viejo seguía contando en el Mapa.

<a id="DOUBT-ALDIA-ABSTENCION"></a>
### Doubt[DOUBT-ALDIA-ABSTENCION] - ¿Una abstención guardada está medida?
**Status**: RESOLVED (2026-10-19)

El juez de pragmatismo de hoy se abstuvo en 88 ejercicios de Corrección 1: 81 «no aplica» y 7 «falta contexto». No
dejó puntaje. La tabla de jueces los cuenta «sin medir» porque exige un puntaje. Las corridas sueltas, en cambio, los
cuentan como ya medidos («repite 88», [F-MEDJ-R001](../2026-10-19.01_mediciones-por-juez/REQUIREMENT.md#F-MEDJ-R001)),
y el registro de veredictos los reusa sin consultar ([F-EVCOST-R006](#F-EVCOST-R006)). Volver a medir sin forzar da la
misma abstención, así que «Medir lo que falta» estima 0 consultas y no cambia nada.

- [x] Opción A (recomendada): es un veredicto. Lleva su marca (al día o de antes) y la cuenta dice cuántas abstenciones
  hay dentro de ella, por motivo (R003 inv. 6, R005 inv. 5).
- [ ] Opción B: una cuarta cuenta, «se abstuvo», al lado de al día, de antes y sin medir. Pierde la marca: una
  abstención de una versión anterior no se distingue de una de hoy.
- [ ] Opción C: sigue «sin medir», y el botón de medir se apaga cuando no consultaría nada. La tabla de jueces y las
  corridas sueltas siguen contándolas distinto.

**Answer**: **Opción A** (José, 19/10, con la recomendación del brief 158): una abstención guardada es un veredicto con
su marca, y la cuenta dice aparte cuántas hay por motivo. Con una cuarta cuenta no se vería si es de hoy o de antes, y
dejándola sin medir las dos cuentas seguían contradiciéndose.

## Enmiendas a otros requisitos

- [F-TALDIA-R009](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R009): una abstención no es
  «sin medir» ([F-ALDIA-R003](#F-ALDIA-R003), inv. 6), así que no suma «Medir lo que falta». La ficha dice cuántas
  abstenciones hay y por qué, sin botón: medirlas de nuevo las reusa.

- [F-JEXAM-R014](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R014): «la versión completa del
  proceso» de un juez de carpeta es la de [F-ALDIA-R001](#F-ALDIA-R001).
- [DOUBT-CJUEZ-VERSION](../2026-10-01.03_cierre-con-jueces/REQUIREMENT.md#DOUBT-CJUEZ-VERSION): queda respondida. El
  cierre sigue reusando lo de otra versión (opción A), pero ahora con su marca ([F-ALDIA-R003](#F-ALDIA-R003),
  [F-ALDIA-R005](#F-ALDIA-R005)), y publicar no lo cuenta sin pedirlo por nombre ([F-ALDIA-R008](#F-ALDIA-R008),
  [F-ALDIA-R009](#F-ALDIA-R009)).
- [F-FILAS-R007](../2026-10-01.02_filas-claras/REQUIREMENT.md#F-FILAS-R007), inv. 2: el texto del veredicto cambia en la
  parte de lo sin comparar ([F-ALDIA-R010](#F-ALDIA-R010)).
- La definición de cada juez de carpeta dice hoy «Todo archivo de esta carpeta, eval/ incluido, entra en la versión del
  juez». Pasa a decir que las pruebas de `eval/` no entran, dentro de la equivalencia de [F-ALDIA-R002](#F-ALDIA-R002).

## Cómo se prueba

Ninguna prueba consulta a un modelo ni toca producción.

| Regla | Qué se comprueba |
|---|---|
| R001 | Cambiar un archivo de `eval/` deja la misma versión. Cambiar uno de la guía, del formato, de un script, de la definición o uno nuevo la cambia |
| R002 | Con la equivalencia, el examen aprobado y los veredictos de la versión del día siguen valiendo. Un cambio más da una versión sin examen, y una equivalencia no se puede agregar después |
| R003 | Con veredictos de la versión de hoy, de otra versión, sin procedencia y sin veredicto, cada uno sale con su marca. La misma versión vieja en las dos puntas da dos «de antes». Una abstención guardada de pragmatismo («falta contexto»), de naturalidad («no aplica») y de didáctica («revisar»), de la versión de hoy, sale al día y no sin medir; de una versión anterior, de antes. La cuenta del cierre, la estimación y el resumen de cada corrida la cuentan igual (`CurrentAnalysisServiceTest.aStoredAbstentionIsAVerdictWithItsMarkAndNeverNotMeasured`, `DefaultBatchJudgePassTest.shouldCountASavedAbstentionAsAVerdictOfItsVersionNeverAsNotMeasured`, `SelectiveCorrectionServiceTest.aSavedAbstentionCountsAsEvaluatedInTheEstimateAndInTheRunSummary`, `ContextOpinionServiceTest.aCompletionRunCountsASavedAbstentionAsEvaluated`). Sólo la versión decide: la de hoy está al día con cualquier motor, también uno cuyo examen reprobó o uno que nunca rindió, y una anterior es «versión anterior» con cualquier motor; no queda causa de motor (`JudgeFreshnessReferenceTest.theMotorNeverMakesAVerdictFromBefore`, `.theMiniTheoryOfThe20thIsFromBeforeByVersionAndTodaysCountsWithBothMotors`, `.aRuntimeWhoseLastExamOfTheVersionFailedIsNotApprovedButItsVerdictsOfTodaysVersionStillCount`, `JudgeFreshnessTallyTest.theMiniTheoryOfThe20thIsReusedByVersionNeverByMotor`, `.unknownAndNotMeasuredAreCountedApartAndAnotherMotorOfTodaysVersionIsUpToDate`) |
| R004 | Dos aprobaciones el mismo día en hora local y distinto en UTC llevan la hora. Una re-aprobada se nombra por la primera, y ninguna se nombra con un número |
| R005 | Las cuentas por juez y estado suman la unidad, y el informe del cierre dice cuánto reusó de antes. Las abstenciones se cuentan dentro de su marca, por motivo, y las tres cuentas siguen sumando la unidad (`JudgeFreshnessTallyTest.abstentionsAreCountedInsideTheirMarkAndTheThreeCountsStillAddUp`) |
| R006 | Con la lista guardada sale lo que cambió, separado en lo que cuenta y lo que no. Sin lista ni foto, dice «no se sabe qué cambió» |
| R007 | La estimación cuenta sólo lo de antes. Con tope 0 o sin examen aprobado no consulta, y nunca pasa el tope. Al terminar integra sólo lo de antes de ese juez, y queda registrado |
| R008 | Una tanda que «pasa» con la misma versión vieja en las dos puntas no se publica, y la negativa y `tray show` nombran al juez |
| R009 | `--accept-previous` publica y anota sólo la misma versión de antes en las dos puntas. Rechaza un nombre que no es juez y no acepta puntas distintas |
| R010 | El detalle de «no se puede decidir» nombra a cada juez con su cuenta y su motivo, sin cambiar el tipo del veredicto |

## References

- **FEAT-JEXAM**: el examen por versión y motor ([F-JEXAM-R001](#F-JEXAM-R001), [F-JEXAM-R003](#F-JEXAM-R003),
  [F-JEXAM-R013](#F-JEXAM-R013), [F-JEXAM-R014](#F-JEXAM-R014)) y el historial ([F-JEXAM-R015](#F-JEXAM-R015)). Citado
  por R001 a R004 y R007.
- **FEAT-EVCOST**: lo de otra versión se reusa y no se borra ([F-EVCOST-R006](#F-EVCOST-R006)). Citado por R003 y R007.
- **FEAT-CJUEZ**: el tope del cierre ([F-CJUEZ-R003](#F-CJUEZ-R003)), el juez sin aprobar
  ([F-CJUEZ-R004](#F-CJUEZ-R004)) y el informe ([F-CJUEZ-R005](#F-CJUEZ-R005)). Citado por R003, R005, R007 a R009.
- **FEAT-NOEMP**: sólo se compara lo evaluado en las dos puntas ([F-NOEMP-R010](#F-NOEMP-R010)). Citado por la
  introducción de las reglas y por R010.
- **FEAT-FILAS**: nombre visible ([F-FILAS-R001](#F-FILAS-R001)) y pisos sin decidir ([F-FILAS-R007](#F-FILAS-R007)).
  Citado por R008 y R010.
- **FEAT-PUBPROD**: la lista de publicación ([F-PUBPROD-R001](#F-PUBPROD-R001)) y «sólo pasa publica»
  ([F-PUBPROD-R005](#F-PUBPROD-R005)). Citado por R008 y R009.
- **FEAT-TALDIA**: lo que muestra el tablero
  ([requisito](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md)).
