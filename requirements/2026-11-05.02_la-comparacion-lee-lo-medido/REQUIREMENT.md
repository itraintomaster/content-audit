---
feature:
  id: FEAT-MIDIO
  code: F-MIDIO
  name: La comparación lee lo medido - cambios, opiniones y jueces de la corrección
  priority: critical
---

# La comparación lee lo medido

## TL;DR

**Qué**: La comparación de una corrección (la de un cierre, la de «Hoy» y la de publicar) lee todo lo que la
corrección midió: reconoce sus cambios, usa las opiniones guardadas de cada juez y tiene un renglón por cada juez de
hoy. Lo que de verdad no se midió sigue sin medir.

**Por qué**: El 5/11/2026, con «Hoy» ([F-HOY](../2026-11-05.01_comparar-con-lo-analizado-hoy/REQUIREMENT.md)), José
vio la C2 de A1 contra producción: «10 ejes en común · 2 sin par · 7 sin medir». Preguntó: «por qué hay tantos sin
medir? Pensé que estaba todo medido». Estaba todo medido. La comparación tenía cuatro fallas, que un cierre de la C2
también tenía (briefs 267 y 269):

1. No reconocía ninguno de los 2.112 cambios de la C2: los buscaba en el plan de la C1 y los aplicaba sobre
   producción. Un cierre no habría devuelto nada.
2. El Inicio de la C2 (el cierre de la C1, del 25/10) midió miniteoría, consigna y «más de una respuesta» con jueces
   viejos, y eso borraba de «Hoy» lo que la C2 midió con los de hoy.
3. Naturalidad y Didáctica A1 reciben la lista de temas del curso. La C2 los midió en un curso de A1 y la comparación
   lee el curso entero: la lista cambiaba y descartaba las 2.402 opiniones de cada uno.
4. La comparación sólo tenía renglón para los jueces que la C1 anotó al abrirse, el 7/10: Pragmatismo, Didáctica A1,
   «No regala la respuesta» y «Texto de vos» llegaron después, y «Calidad de distractores» quedaba afuera.

## Recorrido de uso (estado actual)

1. José abre Comparar, «Hoy» contra la Base en A1 ✓.
2. ⚠️ Siete ejes dicen «sin medir» y dos «sin par», aunque Correcciones dice que la C2 tiene sus opiniones al día.
3. ⚠️ «Si cerraras hoy» decide con cero cambios reconocidos: lo que quedó sin medir no cuenta y cinco jueces sin medir
   dan su piso por cumplido.

**Punto de fricción**: pasos 2 y 3. El análisis está, la comparación no lo lee.

**Cambio mínimo necesario**: que la comparación busque los cambios donde están, que use cada opinión que vale para
ese ejercicio y que no tenga una lista de jueces vieja.

## Reglas de Negocio

<a id="F-MIDIO-R001"></a>
### Rule[F-MIDIO-R001] - La comparación reconoce los cambios de la corrección
**Severity**: critical | **Validation**: VALIDATED

> Los cambios de una corrección se buscan en el plan de esa corrección y se aplican sobre el curso del que parte: el
> que congeló al abrirse o, si sigue a otra, el resultado de la anterior. No se buscan en el plan activo ni se aplican
> sobre el curso de hoy.
>
> Cada cambio reconocido tiene su veredicto y entra en los pisos: lo que un juez dejó sin medir de un ejercicio
> cambiado queda abierto. La compuerta de un cierre puede devolver los cambios que rompen un piso.

<a id="F-MIDIO-R002"></a>
### Rule[F-MIDIO-R002] - Un Inicio medido con otro juez no borra «Hoy»
**Severity**: critical | **Validation**: VALIDATED

> Si de un juez el Inicio no tiene, en algún ejercicio que «Hoy» midió, una opinión con la misma versión y el mismo
> motor, ese juez no se compara contra el Inicio:
> - del lado del Inicio queda sin par;
> - «Hoy» conserva lo que midió;
> - «Ejercicios con algún error» tampoco se compara contra el Inicio.
>
> El piso que se cuenta desde el Inicio se cuenta, para ese juez, desde la Base, con lo que la Base no midió como
> abierto. Si la Base tampoco tiene ese número, queda sin decidir. Decisión de José del 5/11/2026 (brief 267, pregunta
> 2: «contra la Base»).
>
> Lo que un cambio gana de ese juez se cuenta ejercicio por ejercicio. Donde el juez lee hoy lo mismo que leía en el
> Inicio, su opinión de hoy vale también para el Inicio y el cambio no gana nada. Donde lee otra cosa, se cuenta desde
> la Base: un error que la Base ya tenía en ese ejercicio no es nuevo y no devuelve el cambio.
>
> Un número de la Base sin ningún ejercicio evaluado no es un cero: el piso queda sin decidir. Contra el Inicio sigue
> valiendo lo de siempre: un ejercicio que la corrección no cambió no puede ganar un error, así que sólo lo cambiado
> que quedó sin medir queda abierto.
>
> Un tema donde hoy el juez no lee ningún ejercicio —ninguno es de los que juzga y no dejó marca en ninguno— no tiene
> dónde ganar un error: su piso se cumple, mida lo que mida la Base. El 5/11/2026, «Más de una respuesta válida», que
> no juzga los de opción múltiple, dejaba sin decidir 24 temas de la C2 que sólo tienen ejercicios de opción múltiple.
>
> Lo mismo del otro lado. Un ejercicio que el juez no lee en la Base —en producción era para escribir y la corrección
> lo pasó a opción múltiple— no tenía ahí ningún error de ese juez: lo que el juez dice hoy de él se compara contra ese
> cero, también frente al Inicio si el Inicio tampoco lo leía. Un tema donde la Base no tenía ningún ejercicio que el
> juez lea tiene un cero de verdad. Un tema donde la Base leyó alguno y no lo evaluó sigue sin ser un cero. El
> 5/11/2026, «He swims ___», que la C2 pasó a opción múltiple, dejaba sin decidir el piso de «Opción múltiple mal
> armada» en «Adjetivo o adverbio», donde producción no tenía ninguno de opción múltiple.

<a id="F-MIDIO-R003"></a>
### Rule[F-MIDIO-R003] - Una opinión vale aunque la comparación lea más niveles que el juez
**Severity**: critical | **Validation**: VALIDATED

> Algunos jueces reciben la lista de temas del curso, y esa lista depende de cuántos niveles se leen. Es contexto,
> no el ejercicio. Una opinión guardada vale para un ejercicio si lo que vio el juez es igual a lo que vería hoy,
> contando sólo los niveles que tienen las dos lecturas.
>
> Si cambió el ejercicio, su tema, lo que el alumno aprendió antes o cualquier otra cosa que el juez lee, la opinión no
> vale. Vale igual en la comparación, en la lectura del análisis y al integrar una medición.

<a id="F-MIDIO-R004"></a>
### Rule[F-MIDIO-R004] - Cada juez de hoy tiene su renglón
**Severity**: major | **Validation**: VALIDATED

> La comparación tiene un renglón por cada juez del catálogo de hoy que midió la Base o «Hoy», aunque la corrección se
> haya abierto antes de que el juez existiera. Los analizadores instantáneos de errores que la corrección no tenía se
> miden gratis en «Hoy», y en la Base y el Inicio como dice R006.

<a id="F-MIDIO-R005"></a>
### Rule[F-MIDIO-R005] - Un juez que la Base no midió se ve sólo en «Hoy»
**Severity**: major | **Validation**: VALIDATED

> Un juez que no midió ningún ejercicio de la Base se ve con lo que midió «Hoy», sin par. No entra en la diferencia
> ni en «Ejercicios con algún error», y no tiene piso. Decisión de José del 5/11/2026 (brief 269, pregunta 2: «"Hoy"
> solo, sin par»).

<a id="F-MIDIO-R006"></a>
### Rule[F-MIDIO-R006] - Los dos lados se miden con las reglas de hoy
**Severity**: critical | **Validation**: VALIDATED

> Cada analizador instantáneo que corrió «Hoy» se vuelve a medir en la Base y en el Inicio, sin consultas y con las
> reglas de hoy, sobre el contenido que tiene cada uno. Lo que encontraron con reglas viejas no se compara. Así, un
> nivel que la corrección no tocó da lo mismo en el Inicio y en «Hoy», y la diferencia es lo que la corrección cambió.
>
> Un juez no se vuelve a medir: se compara ejercicio por ejercicio, en lo que los dos lados midieron con la misma
> versión y el mismo motor. La medida que su informe guardó al medir no lo deja sin decidir.
>
> El 5/11/2026, con los cambios de la C2 reconocidos, el Inicio (el cierre de la C1, del 25/10) traía «Datos
> incoherentes» medido con reglas viejas. En A2, B1 y B2, que la C2 no toca, subía de 5 a 13, de 4 a 21 y de 0 a 3, y
> rompía sus pisos.

<a id="F-MIDIO-R007"></a>
### Rule[F-MIDIO-R007] - La comparación lee el nivel de la corrección
**Severity**: critical | **Validation**: VALIDATED

> La Base, el Inicio y «Hoy» se leen en el nivel de la corrección, como su análisis. El curso de la comparación es ese
> nivel: un renglón del curso que sumara los otros niveles de un solo lado no compararía nada.
>
> El 5/11/2026 la Base de la C2 tenía sólo A1 y «Hoy», los cuatro niveles. El renglón del curso de «Distribución COCA»
> daba 100,0 → 72,5 y rompía su piso, aunque la C2 no toca nada fuera de A1.

<a id="F-MIDIO-R008"></a>
### Rule[F-MIDIO-R008] - El curso de una corrección de un nivel se mide entero
**Severity**: critical | **Validation**: VALIDATED

> Lo que un analizador de vocabulario mide del curso —el largo de las oraciones, el vocabulario del nivel presente, la
> frecuencia de las palabras— se mide sobre el curso entero en la Base, en el Inicio y en «Hoy»: el nivel de la
> corrección como lo tiene cada lado, y los otros niveles como los tiene el Inicio, porque la corrección no los toca.
> Se mide sin consultas, con las reglas de hoy. El nivel de la corrección se sigue leyendo solo, y los errores y los
> jueces se siguen comparando en el nivel (R007).
>
> Si el Inicio no tiene los otros niveles, el curso queda como el nivel.
>
> El 5/11/2026, el renglón del curso de «Vocabulario del nivel presente» medía sólo A1 contra la lista de palabras de A1
> a B2. Bajaba de 39,38 a 38,41 y rompía su piso porque la C2 sacó de A1 palabras de A2, que siguen en A2: en el curso
> entero, A1 ganó palabras de su lista y A2 quedó casi igual (brief 271).

<a id="F-MIDIO-R009"></a>
### Rule[F-MIDIO-R009] - Dos versiones que son la misma se comparan
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una opinión de la Base o del Inicio se compara con la de «Hoy» cuando las dos son de la versión de hoy del juez o de
> una que el juez de hoy declara la misma ([F-ALDIA-R002](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R002)).
> Una versión que el juez de hoy no nombra sigue sin par, como siempre. Lo mismo vale para la versión de una medición:
> si leyó veredictos de dos versiones que el juez declara la misma, la medición es de la versión de hoy.
>
> El 5/11/2026 la C2 de A1 dejaba 30 pisos sin decidir. Una parte venía de acá: «Traducción que no corresponde» midió
> producción con la versión que todavía contaba sus pruebas, que es la de hoy, y 637 ejercicios quedaban fuera de la
> comparación; «Opción múltiple mal armada», 190; «Calidad de distractores», 94; y «Ejercicio casi repetido», que en su
> medición leyó veredictos de las dos versiones, quedaba sin versión en los 2.402.

## Contexto

- **La Base, el Inicio y «Hoy»**: la Base es producción; el Inicio, de dónde arrancó la corrección; «Hoy», el curso con
  todo lo que la corrección propone ([F-HOY](../2026-11-05.01_comparar-con-lo-analizado-hoy/REQUIREMENT.md)).
- **Mismos ejercicios de los dos lados**: un juez se compara sólo en los ejercicios que los dos lados midieron con la
  misma versión y el mismo motor ([F-METAS](../2026-09-30.27_antes-y-despues-con-metas/REQUIREMENT.md)). Estas reglas
  no cambian eso: cambian qué se considera medido.
- **Lo que sigue sin medir**: «Ejercicio casi repetido» no tiene opiniones de su juez ni en la Base ni en la C2.

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `DefaultCourseProjectorTest`: con el plan de la corrección y su curso de partida, reconoce los cambios aunque el plan activo sea otro y el curso de hoy no tenga el contenido del que parten. `DefaultBatchCloserTest`: el alcance de la comparación nombra el plan de la corrección y la comparación recibe el curso de partida |
| R002 | `CurrentAnalysisServiceTest`: un Inicio con otra versión deja ese juez sin par en el Inicio y con su valor en «Hoy»; con la misma versión no cambia nada. `DefaultBatchComparatorTest`: el piso desde el Inicio se cuenta desde la Base, con lo no medido abierto; sin Base queda sin decidir; una Base sin ningún ejercicio evaluado no es cero; «algún error» no se compara contra ese Inicio; donde el juez lee lo mismo que en el Inicio el cambio no gana nada; donde lee otra cosa, un error que la Base ya tenía no devuelve el cambio y uno que no tenía sí; un tema donde hoy el juez no lee ningún ejercicio cumple su piso, y uno donde lee alguno con una Base sin evaluar sigue sin decidir; un tema donde la Base no leía ningún ejercicio del juez se cuenta desde cero, y cumple o rompe su piso según lo que dice hoy del que pasó a opción múltiple, mientras que uno donde la Base leyó alguno sin evaluarlo sigue sin decidir. `CurrentAnalysisServiceTest`: donde el Inicio leyó lo mismo lleva la opinión de hoy, y donde leyó otra cosa no lleva ninguna; un ejercicio que la Base y el Inicio no leían conserva la opinión de hoy, y uno que la Base leyó sin opinión no |
| R003 | `CurrentAnalysisServiceTest`: una opinión medida en un curso de un nivel vale en la lectura del curso entero; otro tema o lo aprendido antes la invalida igual |
| R004 | `DefaultBatchComparatorTest`: un juez que la corrección no tenía anotado tiene su renglón. `CurrentAnalysisServiceTest`: un instantáneo que la corrección no tenía se mide en «Hoy» de la comparación, y tiene par |
| R005 | `CurrentAnalysisServiceTest`: un juez que la Base no midió queda en «Hoy» sin par y fuera de «algún error». `DefaultBatchComparatorTest`: su renglón tiene el valor de «Hoy», sin par ni piso |
| R006 | `CurrentAnalysisServiceTest`: un instantáneo que la Base y el Inicio midieron con otras reglas se vuelve a medir con las de hoy, cada uno sobre su contenido y sin consultas; un juez comparado ejercicio por ejercicio lleva la medida de hoy |
| R007 | `CurrentAnalysisServiceTest`: la corrección de A1 se compara en A1: la Base, el Inicio y «Hoy» tienen sólo A1, y el curso que recibió no se recorta |
| R008 | `CurrentAnalysisServiceTest`: el analizador de vocabulario mide el curso entero en la Base, el Inicio y «Hoy», cada uno con su A1 y con el A2 del Inicio; el nivel conserva su número y la comparación sigue leyendo A1 |
| R009 | `CurrentAnalysisServiceTest`: una opinión de la Base y del Inicio con una versión que el juez de hoy declara la misma conserva su par; con una que no nombra, no. `RunVersionOfAJudgeTest`: una medición con veredictos de dos versiones que el juez declara la misma es de la versión de hoy; si una no la nombra, no tiene versión |

## References

- Briefs 267, 269 y 271 (docs/briefs).
- [F-HOY](../2026-11-05.01_comparar-con-lo-analizado-hoy/REQUIREMENT.md): «Hoy», la comparación sin cerrar.
- [F-MEDJ](../2026-10-19.01_mediciones-por-juez/REQUIREMENT.md): la Base que se integra desde cualquier
  corrección.
