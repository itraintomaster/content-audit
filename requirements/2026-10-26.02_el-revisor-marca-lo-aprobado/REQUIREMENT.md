---
feature:
  id: FEAT-MARCA
  code: F-MARCA
  name: El revisor marca lo aprobado - una lectura del cambio entero, con producción al lado, que marca sin decidir
  priority: major
---

# El revisor marca lo aprobado

## TL;DR

**Qué**: Un revisor lee cada ejercicio que una tanda dejó distinto de producción, con los dos lados enteros, y lo marca
«bien», «dudoso» o «mal», con cada problema en su campo y el fragmento tal cual. Marca, no decide: José mira lo marcado
y una muestra del resto.

**Por qué**: En la prueba ancha de la C2 de A1 (26/10/2026), 11 de 51 cambios aprobados no se podían publicar tal cual,
y ningún juez lo vio: cada juez mira el candidato solo, sin producción al lado, y nadie pregunta qué se perdió, si los
campos dicen lo mismo entre sí o si la consigna regala la respuesta. José, el 26/10/2026 (brief 226): «y el revisor como
marca sobre lo aprobado».

## Recorrido de uso (estado actual)

1. La mesa corrige y aprueba cambios ✓.
2. Cada juez mide el candidato contra su propia pregunta ✓; ninguno lo pone al lado de producción ⚠️.
3. José revisa los cambios aprobados uno por uno, sin saber cuáles mirar primero ⚠️.

**Punto de fricción**: pasos 2 y 3. Lo sutil —un «No,» que se perdió, una traducción que dice otra cosa que el
inglés, una regla que dejó de ser regla— pasa los jueces y depende de que José lo vea.

**Cambio mínimo necesario**: una lectura del cambio entero que marque lo que da dudas, con tope de consultas, sin tocar
el estado de ninguna corrección.

## Reglas de Negocio

<a id="F-MARCA-R001"></a>
### Rule[F-MARCA-R001] - Lee el cambio entero, con producción al lado
**Severity**: major | **Validation**: VALIDATED

> Por cada ejercicio que la tanda dejó distinto de producción, el revisor lee los dos lados completos: la consigna, el
> ejercicio como lo ve el alumno con sus pistas, las opciones, las respuestas que acepta el corrector, la traducción y
> la miniteoría. Lee también el nivel, la unidad, el nombre del tema y la consigna del tema. No juzga por el tipo de
> oración del tema, que la app no usa y no cambia lo que ve el alumno. No lee qué tareas tenía la corrección, porque
> marca cómo quedó el ejercicio y no si se cumplió una tarea. Es una lectura por ejercicio, aunque lo hayan tocado
> varios cambios; un ejercicio que quedó igual que en producción no se lee.
> Producción es el curso del que partió la corrección, no la tanda anterior. Sabe qué ve el alumno: el nombre del
> tema, la consigna del ejercicio, el ejercicio y sus opciones, y al corregir la traducción y la miniteoría. La
> consigna del tema no la ve: le dice al revisor qué practica el tema, y no se le exige al ejercicio que diga lo que
> dice ella. Una oración suelta de práctica que habla de alguien o de algo sin presentarlo es normal en el curso.

<a id="F-MARCA-R002"></a>
### Rule[F-MARCA-R002] - Marca bien, dudoso o mal, sin decidir
**Severity**: major | **Validation**: VALIDATED

> «Bien» es que lo publicaría tal cual; «dudoso», que quedó mejor que en producción pero con un defecto; «mal», que
> quedó peor que en producción o roto. Cada problema dice dónde está —la consigna, el ejercicio, las respuestas, las
> opciones, la traducción o la miniteoría—, copia el fragmento tal cual de uno de los dos lados y dice qué pasa. «Bien»
> no trae problemas; «dudoso» y «mal» traen al menos uno. La marca no cambia el estado de ninguna corrección, ni lo que
> decidió José, ni el cierre, ni la publicación. El corrector toma la forma contraída y la larga como la misma
> respuesta («'s» e «is», «isn't» e «is not»): que el ejercicio acepte una sola de las dos, o que la consigna no pida
> ninguna, no es un problema.

<a id="F-MARCA-R003"></a>
### Rule[F-MARCA-R003] - Consulta sólo con tope, y lo marcado no se paga dos veces
**Severity**: major | **Validation**: VALIDATED

> Sin tope, el revisor no consulta: dice cuántos ejercicios cambió la tanda, cuántos tienen marca de la versión de hoy
> del revisor y cuántas consultas faltan, y muestra las marcas que ya hay. Con tope, consulta a lo sumo esa cantidad,
> de a varios a la vez, y cada marca queda guardada con la versión del revisor, el motor y la huella de lo que leyó. Lo
> que ya tiene marca de la versión de hoy, con la misma huella y el mismo motor, no se vuelve a consultar. Una marca de
> una versión anterior se muestra como tal y se vuelve a pedir sólo con tope.

<a id="F-MARCA-R004"></a>
### Rule[F-MARCA-R004] - Lo que no se marcó nunca es «bien»
**Severity**: major | **Validation**: VALIDATED

> Una respuesta del revisor que es de otro ejercicio, cuyo fragmento no está así en ninguno de los dos lados, o que no
> se puede leer, deja el ejercicio sin marcar, con el motivo. Un ejercicio que vuelve a cambiar queda sin marcar hasta
> que se lo pida de nuevo, porque lo que leyó el revisor ya no es lo que hay. Lo que no tiene marca nunca cuenta como
> «bien».

<a id="F-MARCA-R005"></a>
### Rule[F-MARCA-R005] - Lo marcado primero
**Severity**: minor | **Validation**: VALIDATED

> La lista muestra primero lo «mal», después lo «dudoso», después lo «heredado», después lo que no tiene marca y al
> final lo «bien»; dentro de cada grupo, en el orden del curso. Se puede pedir sólo algunos ejercicios de la tanda.

<a id="F-MARCA-R006"></a>
### Rule[F-MARCA-R006] - Correcciones muestra la marca de cada ejercicio cambiado
**Severity**: major | **Validation**: VALIDATED

> En Correcciones, cada ejercicio que la tanda dejó distinto de producción dice qué marcó el revisor: «bien», «dudoso»,
> «heredado», «mal» o «sin marca». Con «dudoso», «heredado» y «mal» se ve la razón y cada problema con su campo, el
> fragmento tal cual, qué pasa y, si ya estaba en producción, que ya estaba. «Sin marca» dice por qué: falta pedirla —también cuando el ejercicio cambió después de marcarlo— o la respuesta
> no se pudo usar. Una marca de una versión anterior del revisor se muestra con lo que dijo y dice que es de antes. Un
> ejercicio que quedó igual que en producción no lleva marca. La marca no cambia ninguna decisión: aprobar, rechazar y
> publicar funcionan igual con cualquier marca. Leer las marcas no consulta al revisor.

<a id="F-MARCA-R007"></a>
### Rule[F-MARCA-R007] - La lista se filtra según el revisor
**Severity**: major | **Validation**: VALIDATED

> La lista de Correcciones se puede filtrar por la marca del revisor: «mal», «dudoso», «heredado», «sin marca» o
> «bien», cada una con cuántos ejercicios tiene en la tanda. Con una marca elegida, la lista trae sólo los temas con algún ejercicio de esa
> marca, aunque ya estén decididos, y la tira y las flechas pasan sólo por ellos. Se combina con los otros filtros: entra
> el ejercicio que cumple todos. Lo que no tiene marca nunca entra con «bien». Como el filtro de los jueces, se puede
> llegar con él puesto desde un enlace (`?revisor=mal`).

<a id="F-MARCA-R008"></a>
### Rule[F-MARCA-R008] - Las marcas se piden con estimación y tope
**Severity**: major | **Validation**: VALIDATED

> Desde Correcciones se pueden pedir las marcas que faltan en la tanda, o la de un ejercicio. Antes de gastar se ve
> cuántos ejercicios cambió la tanda, cuántos tienen marca de la versión de hoy, cuántas consultas faltan, con qué
> versión del revisor y con qué motor, y se elige un tope de 1 a lo que falta. Si al lanzar la estimación ya no es la
> misma, no se lanza. Las marcas se guardan por motor y Correcciones muestra las del motor con que las lee: con otro
> perfil u otro esfuerzo no se lanza, y el diálogo dice con cuál pedirlas. Corre como cualquier corrida paga: se ve en
> Ejecuciones, se puede cancelar, y lo que el revisor marcó antes de cancelar queda guardado. Sin nada que falte, no hay
> nada que pedir.

<a id="F-MARCA-R009"></a>
### Rule[F-MARCA-R009] - Las marcas nuevas se ven solas
**Severity**: minor | **Validation**: VALIDATED

> Cuando el revisor guarda una marca, cuando una corrida de marcas termina o se corta, y cuando cambia lo que la tanda
> propone, Correcciones vuelve a leer las marcas sin que haya que recargar la página.

<a id="F-MARCA-R010"></a>
### Rule[F-MARCA-R010] - Lo heredado, aparte
**Severity**: major | **Validation**: VALIDATED

> Cada problema dice si ya estaba en producción: la corrección no lo trajo ni lo empeoró. Un problema cuyo fragmento no
> está tal cual en producción nunca es heredado, diga lo que diga el revisor. Un «dudoso» cuyos problemas ya estaban
> todos en producción es «heredado»: no se publicaría así, pero no es del cambio, y se cuenta y se filtra aparte del
> «dudoso». Un «mal» sigue siendo «mal» aunque traiga problemas heredados. Lo heredado nunca cuenta como «bien». Una
> marca de una versión anterior, que no decía qué ya estaba, cuenta sus problemas como nuevos.

## Contexto

- **No es un juez del análisis**: no abre tareas ni cuenta en el puntaje del curso. Por eso no necesita examen aprobado
  para consultar ([F-JEXAM-R003](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md) es de los jueces que miden el
  curso); el tope lo pone quien lo pide.
- **Antes de que su marca frene algo** hay que calificarlo contra una lectura a mano. La primera es la mía de la prueba
  ancha (`analysis/c2-mejoras-2026-10-25/veredictos-ancha.json`), una referencia y no una verdad certificada.
- **Pregunta lo que pregunto yo** al revisar: qué se perdió respecto de producción, si algo de lo que ve el alumno le
  hace el trabajo, si los campos dicen lo mismo entre sí, si la oración se entiende sola, si la regla sigue siendo regla
  y si el inglés y el castellano suenan naturales.
- **La pantalla del tablero** (R006 a R009) la pidió José el 26/10/2026, respondiendo al brief 227: «Sí: hacé la
  pantalla de las marcas del revisor en Correcciones del tablero». El código está en ittm, en el tablero
  (`pipeline/vocabulary_dashboard`).
- **Lo que ve y acepta el alumno, y lo heredado** (R001, R002 y R010): en las primeras 100 marcas de la C2 (brief
  229), 24 de 44 eran falsas alarmas. 21 comparaban el ejercicio con la consigna del tema, que la app no muestra, y 5
  de los 7 «mal» decían que el corrector rechazaba la forma larga, que acepta desde el 30/9/2026 (ittm #1191). José,
  el 26/10/2026: «enseñale al revisor que el corrector acepta las dos formas y que el alumno no ve la consigna del
  tema, marcá lo heredado aparte». Que el problema ya estaba lo dice el revisor, que tiene los dos lados; que el
  fragmento esté en producción lo confirma, porque un problema puede estar en un campo que no cambió y nacer de otro
  que sí (la traducción quedó igual y el inglés cambió).

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `DefaultChangeReviewServiceTest`: lee los dos lados enteros y el contexto; una lectura por ejercicio; lo que no cambió no se lee; producción es el curso de partida. Lo que sabe el revisor (qué ve el alumno, qué no cuenta) se prueba con la calibración: `analysis/revisor-2026-10-26/marcar.py` contra la prueba ancha y `comparar100.py` contra las 100 del brief 229 |
| R002 | `DefaultChangeReviewServiceTest`: las tres marcas con sus problemas; no cambia ninguna decisión; `JacksonChangeReviewVerdictReaderTest`: bien sin problemas, dudoso y mal con al menos uno |
| R003 | `DefaultChangeReviewServiceTest`: sin tope no consulta; con tope consulta a lo sumo eso; lo marcado de hoy no se paga; una marca vieja se muestra y se vuelve a pedir con tope |
| R004 | `DefaultChangeReviewServiceTest`: otro ejercicio, fragmento ajeno, ilegible y ejercicio que volvió a cambiar quedan sin marcar |
| R005 | `DefaultChangeReviewServiceTest`: mal, dudoso, heredado, sin marcar y bien, en ese orden; pedir algunos ejercicios; `ChangeReviewCmdTest`: lo que muestra el comando |
| R006 | En ittm, el tablero: `test/tablero_html/marcas/rutas_de_marcas_test.dart` (leer no consulta; cada ejercicio con estado, marca, razón y problemas; de antes y sin marca; los dos lados no viajan; otra tanda no se muestra); `html/test/revisor-marcas.test.js` (la etiqueta de la cabecera, el bloque con la razón y cada problema, la marca de antes, por qué no tiene marca, la marca no decide, lo del revisor se escribe como texto) y `html/test/arquitectura.test.js` (sólo `js/estado/marcas.js` pide `/api/marcas`) |
| R007 | `html/test/revisor-marcas.test.js`: los cinco valores con su cuenta; sin marca nunca es bien y la de antes cuenta con lo que dijo; los temas con esa marca aunque estén decididos, la tira sólo por ellos; se combina con el motivo y con la opinión; chips, «Todos» y abrir un ejercicio escondido; `?revisor=`; apagado mientras se lee o si falla |
| R008 | `test/tablero_html/ejecuciones/marcas_test.dart` (la estimación gratis con sus cifras, el tope de 1 a lo que falta, la de un ejercicio, sin nada que pedir, una estimación que cambió, un tope que se pasa, la corrida paga en Ejecuciones, la cuenta al terminar aunque la respuesta traiga cada ejercicio con sus dos lados, no se retoma, el pedido con id); `html/test/revisor-marcas.test.js` (los dos botones abren el diálogo, sin nada que pedir no hay botón, en la base tampoco, lo que dice el diálogo, el tope y el pedido, la de un ejercicio, el freno, con otro motor no se lanza, el nombre en Ejecuciones) |
| R009 | `html/test/revisor-marcas.test.js`: se leen una vez; los avisos de «analisis», «tanda» y «cambios» las releen y los demás no; una corrida de marcas que empieza, termina o queda cortada las relee; `html/test/estado-avisos.test.js`: las etiquetas de cada aviso |
| R010 | `JacksonChangeReviewVerdictReaderTest`: lee si cada problema ya estaba, una marca de antes sin decirlo lo cuenta nuevo, «true» como texto no se lee; `ChangeReviewerAgentGateTest`: el agente deja pasar sólo un problema que lo dice como verdadero o falso, y lo que deja pasar lo lee Java; `DefaultChangeReviewServiceTest`: un fragmento que no está en producción no es heredado, un dudoso con todo heredado va aparte y un mal sigue mal; `ChangeReviewCmdTest`: el grupo «heredado» y «(ya estaba en producción)»; en ittm, el tablero: `test/tablero_html/marcas/rutas_de_marcas_test.dart` (dudoso con todo heredado es «heredado», con algo nuevo sigue dudoso, mal sigue mal, una marca de antes cuenta nuevo) y `html/test/revisor-marcas.test.js` (la etiqueta y el bloque de «heredado», «Ya estaba en producción» en cada problema que ya estaba, el chip con su cuenta, aparte del dudoso y nunca bien, la ayuda) |

## References

- Brief 226, «Un revisor que marque y dos contrapesos» (26/10/2026).
- Brief 225, la prueba ancha de la C2 de A1.
- `analysis/c2-mejoras-2026-10-25/veredictos-ancha.json`.
