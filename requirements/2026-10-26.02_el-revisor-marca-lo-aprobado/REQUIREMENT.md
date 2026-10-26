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
> la miniteoría. Lee también el nivel, la unidad, el nombre del tema, la consigna del tema y el tipo de oración; no lee
> qué tareas tenía la corrección, porque marca cómo quedó el ejercicio y no si se cumplió una tarea. Es una lectura por
> ejercicio, aunque lo hayan tocado varios cambios; un ejercicio que quedó igual que en producción no se lee.
> Producción es el curso del que partió la corrección, no la tanda anterior.

<a id="F-MARCA-R002"></a>
### Rule[F-MARCA-R002] - Marca bien, dudoso o mal, sin decidir
**Severity**: major | **Validation**: VALIDATED

> «Bien» es que lo publicaría tal cual; «dudoso», que quedó mejor que en producción pero con un defecto; «mal», que
> quedó peor que en producción o roto. Cada problema dice dónde está —la consigna, el ejercicio, las respuestas, las
> opciones, la traducción o la miniteoría—, copia el fragmento tal cual de uno de los dos lados y dice qué pasa. «Bien»
> no trae problemas; «dudoso» y «mal» traen al menos uno. La marca no cambia el estado de ninguna corrección, ni lo que
> decidió José, ni el cierre, ni la publicación.

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

> La lista muestra primero lo «mal», después lo «dudoso», después lo que no tiene marca y al final lo «bien»; dentro de
> cada grupo, en el orden del curso. Se puede pedir sólo algunos ejercicios de la tanda.

## Contexto

- **No es un juez del análisis**: no abre tareas ni cuenta en el puntaje del curso. Por eso no necesita examen aprobado
  para consultar ([F-JEXAM-R003](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md) es de los jueces que miden el
  curso); el tope lo pone quien lo pide.
- **Antes de que su marca frene algo** hay que calificarlo contra una lectura a mano. La primera es la mía de la prueba
  ancha (`analysis/c2-mejoras-2026-10-25/veredictos-ancha.json`), una referencia y no una verdad certificada.
- **Pregunta lo que pregunto yo** al revisar: qué se perdió respecto de producción, si algo de lo que ve el alumno le
  hace el trabajo, si los campos dicen lo mismo entre sí, si la oración se entiende sola, si la regla sigue siendo regla
  y si el inglés y el castellano suenan naturales.
- **La pantalla del tablero** para ver las marcas no es parte de este requisito.

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `DefaultChangeReviewServiceTest`: lee los dos lados enteros y el contexto; una lectura por ejercicio; lo que no cambió no se lee; producción es el curso de partida |
| R002 | `DefaultChangeReviewServiceTest`: las tres marcas con sus problemas; no cambia ninguna decisión; `JacksonChangeReviewVerdictReaderTest`: bien sin problemas, dudoso y mal con al menos uno |
| R003 | `DefaultChangeReviewServiceTest`: sin tope no consulta; con tope consulta a lo sumo eso; lo marcado de hoy no se paga; una marca vieja se muestra y se vuelve a pedir con tope |
| R004 | `DefaultChangeReviewServiceTest`: otro ejercicio, fragmento ajeno, ilegible y ejercicio que volvió a cambiar quedan sin marcar |
| R005 | `DefaultChangeReviewServiceTest`: mal, dudoso, sin marcar y bien, en ese orden; pedir algunos ejercicios; `ChangeReviewCmdTest`: lo que muestra el comando |

## References

- Brief 226, «Un revisor que marque y dos contrapesos» (26/10/2026).
- Brief 225, la prueba ancha de la C2 de A1.
- `analysis/c2-mejoras-2026-10-25/veredictos-ancha.json`.
