---
feature:
  id: FEAT-SIGUE
  code: F-SIGUE
  name: La medición sigue - una saturación no la corta, volver a medir retoma y Traducción recibe su tope entero
  priority: major
---

# La medición sigue

## TL;DR

**Qué**: Una medición larga llega al final. Si el modelo está saturado, el juez espera y vuelve a preguntar. Volver a
medir pregunta sólo lo que no tiene la opinión de la versión de hoy, y por eso una medición cortada retoma donde
quedó. Completar le da a Traducción las dos consultas que necesita por ejercicio.

**Por qué**: La noche del 27/10/2026, la Consigna de la C2 de A1 cortó a las 23:09 con 773 de 2.402 medidos, y Una
sola respuesta cortó a las 23:59 con 327 de 1.691. En las dos, una sola consulta recibió «Selected model is at
capacity» de OpenAI, y el juez quedó «no disponible» el resto de la corrida. Relanzar como estaba volvía a preguntar
4.093 ejercicios, 1.613 de ellos ya con la opinión de la versión de hoy. Otra saturación lo habría cortado de nuevo, y
habría empezado de cero. Además, Completar midió 5 de los 10 ejercicios de Traducción que le faltaban, con 10 de sus
20 consultas (brief 234). José, el 28/10/2026: «Sí: hacé los tres arreglos (saturación, «Volver a medir» que salta lo
de hoy, tope de Traducción), con sus pruebas, juntalos y relanzá las 2.480 que faltan».

## Recorrido de uso (estado actual)

1. Un juez cambió de versión y Correcciones avisa cuántos ejercicios tienen su opinión de la versión anterior ✓.
2. José toca «Volver a medir», confirma el tope y la medición corre horas ✓.
3. Una consulta recibe una saturación del modelo ⚠️ — el juez queda no disponible y la medición corta a la mitad.
4. José relanza ⚠️ — vuelve a preguntar todo, también lo que ya tiene la opinión de hoy.

**Punto de fricción**: pasos 3 y 4. Una falla de un minuto tira horas de medición, y retomar cuesta lo que ya se
pagó.

**Cambio mínimo necesario**: esperar ante la saturación; al volver a medir, saltar lo que ya está al día; y repartir
el tope de Completar contando las consultas de cada juez.

## Reglas de Negocio

<a id="F-SIGUE-R001"></a>
### Rule[F-SIGUE-R001] - Una saturación del modelo no corta la medición
**Severity**: critical | **Validation**: VALIDATED

> Cuando el proveedor contesta que el modelo está saturado («model is at capacity», «overloaded»), el juez espera y
> vuelve a preguntar el mismo ejercicio. Espera 30 segundos, después 1, 2, 4, 8 y 15 minutos. En cuanto contesta, la
> medición sigue como si nada. Si después de la última espera, unos 30 minutos en total, sigue saturado, recién ahí
> queda no disponible, con el motivo «modelo saturado» y qué hacer. Una respuesta de saturación no es una opinión: no
> se guarda. La cuota agotada, la sesión vencida y las otras fallas del servicio siguen cortando la corrida en la
> primera vez, como hasta ahora ([F-EVCOST-R009](../2026-07-29.03_evaluaciones-costosas-reutilizables/REQUIREMENT.md#F-EVCOST-R009)).

<a id="F-SIGUE-R002"></a>
### Rule[F-SIGUE-R002] - Volver a medir salta lo que ya tiene la opinión de hoy
**Severity**: major | **Validation**: VALIDATED

> Volver a medir un juez pregunta sólo los ejercicios sin opinión o cuya última opinión es de una versión anterior. La
> opinión de la versión de hoy, o de una versión anotada como equivalente, se conserva tal cual, sin consultar ni
> gastar, aunque la haya dejado otra corrida. La estimación cuenta sólo lo que va a preguntar. Por eso una medición
> cortada a la mitad, relanzada, sigue donde quedó. Los otros pedidos de volver a preguntar no cambian: el análisis de
> línea de comandos con un área forzada, la opinión de los dos estados, y las marcas del revisor que no se pueden usar.

<a id="F-SIGUE-R003"></a>
### Rule[F-SIGUE-R003] - Completar le da a cada juez todas las consultas que planeó
**Severity**: major | **Validation**: VALIDATED

> Completar reparte su tope entre los jueces en el orden en que los consulta. A cada uno le da todas las consultas que
> planeó, contadas por evaluador, hasta lo que quede del tope. Traducción pregunta dos veces por ejercicio, una desde
> el inglés y otra desde la traducción. Por eso recibe dos consultas por ejercicio y mide todos los ejercicios que
> planeó. La corrida nunca gasta más que su tope.

<a id="F-SIGUE-R004"></a>
### Rule[F-SIGUE-R004] - Una saturación tampoco corta la corrección
**Severity**: critical | **Validation**: VALIDATED

> Cuando el corrector pide una propuesta y el proveedor contesta que el modelo está saturado, el corrector espera y
> vuelve a pedir la misma propuesta, con el mismo intento, igual que un juez
> ([F-SIGUE-R001](#F-SIGUE-R001)): 30 segundos, después 1, 2, 4, 8 y 15 minutos. En cuanto contesta, la corrección
> sigue como si nada. Si después de la última espera sigue saturado, recién ahí la propuesta queda no disponible, con
> el motivo «modelo saturado», y la corrida corta como antes, sin gastar el intento
> ([F-MESA-R010](../2026-09-30.22_la-mesa-por-tema/REQUIREMENT.md#F-MESA-R010)). La cuota agotada y las otras fallas
> del servicio siguen cortando en la primera vez.

## Contexto

- **Por qué esperar y no cortar**: el corte por falla del servicio se pensó para la cuota agotada o la sesión vencida,
  que no se arreglan solas. Una saturación sí se arregla sola: el 28/10, diez minutos después del segundo corte,
  Miniteoría hizo 21 consultas sin un error.
- **Qué es «al día»**: la versión del juez decide, como en el resto del tablero
  ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003)). Volver a medir busca la opinión
  guardada con el mismo motor, que es el que va a preguntar. Una opinión de hoy con otro motor se vuelve a pedir. En
  la C2 todas son del mismo.
- **El tope de Traducción**: Traducción reparte su presupuesto de a pares desde su origen
  ([F-TRSEN-R005](../2026-09-30.08_traduccion-y-sentido/REQUIREMENT.md#F-TRSEN-R005)). La medición suelta y el cierre ya le pasaban el total; Completar le
  pasaba la mitad.
- **Fuera de alcance**: esperar ante el límite de solicitudes o la cuota agotada. Esos siguen cortando y dicen qué
  hacer.
- **La corrección también**: la noche del 28/10/2026, el tramo 2 de la pasada dentro de la C2 de A1 (481 ejercicios,
  tope 8.000) cortó a las 23:46 por un solo «Selected model is at capacity» del corrector. Había intentado 319
  ejercicios con 4.989 consultas, y los 162 que quedaban terminaron sin intentar. José había pedido la pasada entera
  (28/10/2026: «lanzá la pasada sobre los 570 dentro de la C2»).

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `DefaultJudgeProviderReadinessProbeTest`: «model is at capacity» y «overloaded» se leen como modelo saturado, y la cuota o el límite de solicitudes, no. `AgentJudgeTest` y `QuizInstructionAgentJudgeTest`: una saturación espera y vuelve a preguntar el mismo ejercicio, y la respuesta siguiente se emite; saturado en todas las esperas queda no disponible con «modelo saturado»; una sesión vencida no espera |
| R002 | `DefaultEvaluationSessionTest`: refrescar conserva sin consultar la opinión de la versión de hoy, y de una equivalente; pregunta la de una versión anterior y la que falta, de a uno y en tanda. `DefaultJudgeRuntimeTest` y `QuizInstructionAnalyzerTest`: un análisis forzado que pide conservar lo de hoy refresca en vez de forzar, y una medición cortada sigue donde quedó. `TranslationSenseAnalyzerTest`: cada juez del par pregunta sólo lo que no tiene su opinión de hoy. `SelectiveCorrectionServiceTest`: volver a medir un estado lo pide; la opinión de los dos estados no |
| R003 | `ContextOpinionServiceTest`: un juez con dos evaluadores recibe las dos consultas por ejercicio y mide todo lo que planeó; con un tope que no alcanza, no gasta de más |
| R004 | `AgentPanelProposerTest`: una saturación espera y vuelve a pedir la misma propuesta con el mismo intento, y lee la respuesta siguiente; saturado en todas las esperas queda no disponible con «modelo saturado»; la cuota agotada y el servicio caído no esperan |

## References

- Brief 234, la noche de la C2 (28/10/2026).
- [F-ABIER-R001](../2026-10-27.01_medir-con-la-tanda-abierta/REQUIREMENT.md#F-ABIER-R001): se vuelve a medir con la tanda abierta.
