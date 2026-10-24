---
feature:
  id: FEAT-CMOTOR
  code: F-CMOTOR
  name: Cierre con cualquier motor - cada juez se consulta con un motor en el que está habilitado
  priority: critical
---

# Cierre con cualquier motor: cada juez se consulta con un motor en el que está habilitado

## TL;DR

**Qué**: Al cerrar una tanda, cada juez se consulta con el motor del perfil si su versión de hoy está habilitada con él;
si no, con el motor con el que se habilitó más recientemente, entre los que se usan por suscripción. La estimación y la
cuenta del cierre dicen con qué motor va cada juez. Lo que se lee del libro sin consultar sigue la misma regla, así el
cierre y publicar comparan con el mismo motor. Exámenes dice con qué motor cerraría cada juez.

**Por qué**: José decidió el 21/10, respondiendo al brief 199: «un juez habilitado con cualquier motor deja cerrar la
tanda». Hasta ahora cada corrida consultaba a todos los jueces con el único motor del perfil. Un juez habilitado sólo con
Gemini, en un cierre con ChatGPT, se consultaba 0 veces: la tanda se cerraba, pero quedaba «no se puede decidir», con
«juez sin aprobar» en el veredicto, y no se publicaba. El 24/10 eligió hacerlo en esta tarea, aparte de la etapa 5 del
tablero.

Las decisiones son de José (24/10/2026):
- si el juez está habilitado con el motor del perfil y con otro, se usa el del perfil; si no, el habilitado más
  recientemente;
- `--budget` y `--cap` se reparten como hoy, sin importar el motor;
- un juez sin ningún motor que se pueda usar queda como hoy: sin aprobar, sin consultar, nombrado en el veredicto;
- las lecturas gratis siguen la misma regla de motor que el cierre.

## Recorrido de uso (estado actual)

El caso: la miniteoría del 19/10 (`bbcf47a4`), aprobada sólo con Gemini 3.8 Flash, en una tanda que José cierra con el
perfil de ChatGPT.

1. Estima el cierre → `batch estimate`. ⚠️ La miniteoría sale «sin examen» con el motor del perfil, aunque Exámenes la
   muestra habilitada con Gemini.
2. Le da un tope → `batch close --budget mini-theory=200`. ⚠️ Se acepta, pero la miniteoría consulta 0 veces.
3. Lee el veredicto. ⚠️ «No se puede decidir; juez sin aprobar: Miniteoría». La tanda no se publica.
4. Para cerrarla, tendría que cambiar de perfil y cerrar con Gemini. ⚠️ Entonces los demás jueces, habilitados con
   ChatGPT, consultan 0 veces.

**Punto de fricción**: pasos 2 a 4. No hay ningún perfil con el que todos los jueces consulten.

**Cambio mínimo necesario**: que el cierre elija, juez por juez, un motor en el que esté habilitado, y lo diga.

## Reglas de Negocio

Un **motor** es el proveedor, el modelo exacto y el esfuerzo con que se consulta a un juez: por ejemplo ChatGPT
(`codex-cli`, `gpt-6.1-sol`, `medium`) o Gemini 3.8 Flash (`agy`, `gemini-3.8-flash-high`, `high`). El **motor del
perfil** es el que eligió José al lanzar; sin perfil, ChatGPT. La versión de hoy de un juez está **habilitada** con un
motor si aprobó su examen con él o si José la promovió con él ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003)).
Un motor **se usa por suscripción** si se consulta con la sesión local de José, sin clave: ChatGPT, Gemini o Claude.
**Leer gratis** es leer del libro lo que un juez ya dijo, sin consultarlo.

<a id="F-CMOTOR-R001"></a>
### Rule[F-CMOTOR-R001] - El cierre consulta cada juez con un motor en el que está habilitado
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Al cerrar una tanda, y al estimar su cierre, cada juez se consulta con el motor del perfil si su versión de hoy está
> habilitada con él. Si no, se consulta con el motor con el que esa versión se habilitó más recientemente, entre los que
> se usan por suscripción. Invariantes:
> 1. el motor del perfil gana siempre que esté habilitado, aunque otro se haya habilitado después;
> 2. sólo se usa un motor con el que la versión de hoy está habilitada; el motor no cambia la versión, y un juez que con
>    otro motor diera otra versión no se mueve a ese motor;
> 3. un motor que pide clave no se usa nunca en lugar del del perfil;
> 4. el examen se sigue rindiendo con el motor del perfil, y Reanalizar, Medir lo que falta y `batch run` siguen
>    consultando sólo con él: lo que cambia es el cierre;
> 5. lo que el cierre consulta con otro motor queda en el libro con ese motor, y está al día como lo de cualquier motor:
>    sólo la versión decide ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003) inv. 7).

<details><summary>Detail</summary>

«Habilitada más recientemente» es la fecha del examen aprobado o de la promoción que la habilita con ese motor. Si dos
motores empatan, o no tienen fecha, va primero el que aparece primero en Exámenes. Un motor que no se puede usar —pide
clave, o el juez no se puede armar con él— se saltea y se prueba el siguiente.

El libro reusa lo que dijo un juez con el mismo motor ([F-EVCOST-R003](#F-EVCOST-R003)), y la comparación exige el mismo
motor en el antes y en el después ([F-METAS-R002](#F-METAS-R002)): lo que la versión de hoy dijo con otro motor no se
reusa en ese cierre. Por eso el del perfil va primero: es el que José usa para medir.

El juez de casi repetidos que pasa por Ollama aprueba su paso de vectores con el motor del juez: sigue con el motor del
perfil [ASSUMPTION].

**Criterio**, con la miniteoría del 19/10, habilitada sólo con Gemini, en un cierre con el perfil de ChatGPT: la
miniteoría se consulta con Gemini 3.8 Flash y los demás jueces con ChatGPT. Si la miniteoría aprobara también con ChatGPT,
se consultaría con ChatGPT. Con el perfil de Gemini, se consulta con Gemini.

</details>

<a id="F-CMOTOR-R002"></a>
### Rule[F-CMOTOR-R002] - Sin un motor que se pueda usar, el juez sigue sin aprobar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Si la versión de hoy de un juez no está habilitada con el motor del perfil ni con ningún motor que se use por
> suscripción, el juez queda como hasta ahora ([F-CJUEZ-R004](../2026-10-01.03_cierre-con-jueces/REQUIREMENT.md#F-CJUEZ-R004)):
> no consulta con ningún tope, el veredicto lo nombra «juez sin aprobar» y la tanda no se publica.

<details><summary>Detail</summary>

El tablero ya no le deja darle tope a un juez sin examen y frena el lanzamiento antes de gastar; eso no cambia. Lo que
cambia es quién está sin examen: un juez habilitado con Gemini ya no lo está en un cierre con ChatGPT.

**Criterio**: un juez cuya versión de hoy reprobó con ChatGPT y no rindió con otro motor sigue «sin aprobar» en un
cierre con cualquier perfil: no consulta con `--budget <juez>=50`, y el veredicto lo nombra. Lo mismo uno habilitado
sólo con un motor que pide clave, en un cierre con un perfil por suscripción.

</details>

<a id="F-CMOTOR-R003"></a>
### Rule[F-CMOTOR-R003] - La estimación y la cuenta del cierre dicen con qué motor va cada juez
**Severity**: critical | **Validation**: AUTO_VALIDATED

> `batch estimate` y la cuenta del cierre dicen, por cada juez que consulta un modelo, el motor con el que se lo consulta,
> y marcan cuando no es el del perfil. El texto lo dice en su línea: «con agy/gemini-3.8-flash-high (high), no con el
> del perfil». Nunca se consulta con otro motor sin decirlo.

<details><summary>Detail</summary>

Mantiene la regla de no cambiar de proveedor en silencio: el cambio de motor lo decide una regla que José aprobó, y la
estimación, que José ve antes de lanzar, lo nombra. Un juez local, como la naturalidad con Ollama, no dice motor.

**Criterio**, con la miniteoría del 19/10 en un cierre con ChatGPT: su línea de `batch estimate` termina en «con
agy/gemini-3.8-flash-high (high), no con el del perfil», y su motor viaja en el JSON de la estimación. La línea de la
traducción, habilitada con ChatGPT, no nombra el motor.

</details>

<a id="F-CMOTOR-R004"></a>
### Rule[F-CMOTOR-R004] - Lo que se lee gratis sigue al mismo motor que el cierre
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Lo que se lee del libro sin consultar —la comparación del cierre, la de publicar, lo que muestra Correcciones y el
> control del audio antes de publicar— lee cada juez con el motor que elegiría el cierre
> ([F-CMOTOR-R001](#F-CMOTOR-R001)). Invariantes:
> 1. leer gratis no consulta ni gasta nada, con cualquier motor;
> 2. con el mismo motor del perfil, el cierre y publicar leen a cada juez con el mismo motor: un piso que el cierre
>    decidió no queda sin decidir al publicar por haber leído con otro.

<details><summary>Detail</summary>

Sin esto, el cierre pagaría la miniteoría con Gemini y la comparación la leería con ChatGPT: no encontraría lo pagado y
el piso quedaría «no se puede decidir».

**Criterio**: con la miniteoría habilitada sólo con Gemini, la comparación del cierre lee lo que la miniteoría dijo con
Gemini, y Publicar compara con eso mismo.

</details>

<a id="F-CMOTOR-R005"></a>
### Rule[F-CMOTOR-R005] - Los topes se reparten igual con dos motores
**Severity**: major | **Validation**: AUTO_VALIDATED

> `--budget <juez>=N` es el tope de ese juez, con el motor que le toque; `--cap N` es el total de la tanda, que comparten
> en el orden del catálogo los jueces que `--budget` no nombra, sin importar el motor
> ([F-CJUEZ-R003](../2026-10-01.03_cierre-con-jueces/REQUIREMENT.md#F-CJUEZ-R003)). Un cierre que consulta con dos
> motores nunca pasa N.

<details><summary>Detail</summary>

Todos los motores que se pueden usar son por suscripción: una consulta cuesta lo mismo con cualquiera. Un tope por motor
queda afuera.

**Criterio**: con `--cap 100`, la traducción con ChatGPT gasta 70 y a la miniteoría con Gemini le quedan 30.

</details>

<a id="F-CMOTOR-R006"></a>
### Rule[F-CMOTOR-R006] - Exámenes dice con qué motor cerraría cada juez
**Severity**: major | **Validation**: AUTO_VALIDATED

> `exam status -f json` dice, por cada juez, el motor con el que lo consultaría el cierre (`closesWith`), o nada si no hay
> ninguno ([F-CMOTOR-R002](#F-CMOTOR-R002)). No consulta ni escribe nada.

<details><summary>Detail</summary>

Es lo que el tablero necesita para decir «deja cerrar» con cualquier motor: un juez con `closesWith` no bloquea el
cierre; uno sin él, sí. El estado del juez con el motor del perfil (`current`) no cambia: es el que se rinde y se
promueve.

**Criterio**: la miniteoría del 19/10, con el perfil de ChatGPT, trae `closesWith` con `agy`, `gemini-3.8-flash-high` y
`high`; un juez reprobado con ChatGPT y sin otro motor no trae `closesWith`.

</details>

## Contexto

**El brief 199** (21/10, revisado el 24/10) mostró que el estado de un juez se cuenta de cuatro maneras y que la del
cierre mira sólo el motor de la pasada. José eligió «sí, con cualquiera» en la pregunta 1 («habilitada sólo con otro
motor: ¿deja cerrar la tanda?»). La etapa 5 del tablero muestra «deja cerrar» sólo con el motor de siempre hasta que esto
esté.

**Hoy no cambia nada visible**: según el brief 199, al 24/10 los once jueces están habilitados con ChatGPT. Vuelve a importar con cualquier
juez nuevo que apruebe sólo con otro motor, como la miniteoría el 19/10.

## Alcance

- **Adentro**: R001 a R006: el motor de cada juez al cerrar y al estimar, el juez sin motor, lo que dicen la estimación
  y la cuenta, las lecturas gratis, los topes y lo que exporta Exámenes.
- **Afuera**:
  - elegir a mano el motor de un juez en el cierre;
  - un tope por motor;
  - reusar en un cierre lo que la versión de hoy dijo con otro motor (el libro reusa por motor);
  - el tablero (ittm): el diálogo de lanzar y Exámenes leen los campos nuevos en la etapa 5 o la 9;
  - el juez de casi repetidos por Ollama.

## Cómo se prueba

Sin consultas pagas: los jueces son falsos y los motores, identidades escritas en las pruebas.

| Regla | Prueba |
|---|---|
| R001 | `JudgeClosingMotorTest` (audit-domain): motor del perfil primero, el más reciente, el empate y el sin fecha en el orden de Exámenes, saltea el que no se puede usar, no se mueve si cambia la versión, un juez de dos evaluadores se mueve sólo si los dos pueden, no se mueve en examen ni sin pedirlo; `MovableJudgeTest` (audit-cli): sólo por suscripción, sin la clave del perfil; `ClosingWithAnyMotorTest` (revision-domain): el cierre paga con Gemini. |
| R001 inv. 5 | `JudgeClosingMotorTest`: lo que dio otro motor está al día con la versión de hoy. |
| R002 | `JudgeClosingMotorTest`: sin motor usable sigue sin consultar; `ClosingWithAnyMotorTest`: el juez reprobado y sin otro motor no consulta con su tope. |
| R003 | `ClosingWithAnyMotorTest`: la cuenta y la estimación llevan el motor y la marca; `BatchCmdTest`: la línea lo dice y el JSON lo lleva. |
| R004 | `JudgeClosingMotorTest`: leer gratis sigue al motor sin pedirlo y no consulta (inv. 1); el cierre y la lectura gratis eligen el mismo motor, con el del perfil habilitado y sin él (inv. 2). |
| R005 | `ClosingWithAnyMotorTest`: `--cap` compartido por dos motores. |
| R006 | `ExamCmdTest`: `closesWith` con otro motor y sin ninguno. |

## References

- Brief 199: `docs/briefs/199-jueces-antes-de-disenar.html` (sesión «Implementar la etapa 5 del estado: jueces»).
- [FEAT-CJUEZ](../2026-10-01.03_cierre-con-jueces/REQUIREMENT.md), [FEAT-ALDIA](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md).
