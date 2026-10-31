---
feature:
  id: FEAT-TODOS
  code: F-TODOS
  name: Todos quiere decir todos - un juez que observa algo, aunque sea leve, o el revisor que marca «Mal», sacan al ejercicio de «Todos los jueces lo aprueban»
  priority: major
---

# Todos quiere decir todos

## TL;DR

**Qué**: «Todos los jueces lo aprueban» trae sólo los ejercicios en los que ningún juez observó nada, ni siquiera algo
leve, y que el revisor no marca «Mal». La observación leve de un analizador que no es juez sigue sin frenar.

**Por qué**: El 31/10/2026, en la C2 de A1, José abrió «Todos los jueces lo aprueban» y encontró ejercicios con un error
a la vista. En `…2ce8` («The children got off the boat and stayed in Italy.», Verbos de movimiento) la miniteoría
observaba que la tarjeta «especifica palabras que no determinan la regla» y el revisor lo marcaba «Mal». José: «en este
mini teoría tiene un error. Entonces no todos los jueces de la prueba … Si el revisor lo marca mal, tampoco todos los
jueces de la prueba, porque el revisor también es un juez de alguna manera».

## Recorrido de uso (estado actual)

1. José abre «Todos los jueces lo aprueban» para revisar ✓.
2. ⚠️ Entra un ejercicio en el que un juez observó algo leve: el juez no lo aprobó del todo.
3. ⚠️ Entra un ejercicio que el revisor marca «Mal».

**Punto de fricción**: pasos 2 y 3. La lista dice «todos» y no son todos.

**Cambio mínimo necesario**: que lo observado por un juez y la marca «Mal» del revisor saquen al ejercicio.

## Reglas de Negocio

<a id="F-TODOS-R001"></a>
### Rule[F-TODOS-R001] - Lo que observa un juez, aunque sea leve, lo saca
**Severity**: major | **Validation**: VALIDATED

> Un ejercicio en el que un juez dejó una observación, aunque sea leve, no entra en «Todos los jueces lo aprueban» ni en
> «Preparados para entregar», y tampoco queda en «Faltan opiniones»: ese juez no lo aprueba, y medir a los que faltan no
> lo cambia. Si lo aprobaste, queda en «Vos lo aprobás», entre los que tienen un problema.
>
> Es juez también el que decide por pares si dos ejercicios son el mismo («Casi duplicados»): su observación la escribe
> un modelo.
>
> Si el juez que dejó lo leve opinó con una versión anterior, su opinión sigue sin contar y el ejercicio queda en «Faltan
> opiniones» ([F-LEVE-R001](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R001)): con la versión de hoy puede
> no observar nada.
>
> La observación leve de un analizador que no es juez —el largo de la oración, las respuestas completas, los duplicados
> exactos— sigue sin sacarlo y se sigue viendo ([F-LEVE-R001](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R001),
> [F-LEVE-R002](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R002)).

<a id="F-TODOS-R002"></a>
### Rule[F-TODOS-R002] - El revisor que marca «Mal» tampoco lo aprueba
**Severity**: major | **Validation**: VALIDATED

> Un ejercicio que el revisor marca «Mal» ([F-MARCA-R006](../2026-10-26.02_el-revisor-marca-lo-aprobado/REQUIREMENT.md#F-MARCA-R006))
> no entra en «Todos los jueces lo aprueban» ni en «Preparados para entregar», y tampoco queda en «Faltan opiniones». Si
> lo aprobaste, queda en «Vos lo aprobás»; arriba de esa lista se cuenta entre los que tienen un problema que ve un juez,
> el revisor o un analizador ([F-DICE-R003](../2026-10-28.03_cada-aviso-dice-que-paso/REQUIREMENT.md#F-DICE-R003)).
>
> Cuenta también la marca «Mal» de una versión anterior del revisor, como en el filtro «Según el revisor»
> ([F-MARCA-R007](../2026-10-26.02_el-revisor-marca-lo-aprobado/REQUIREMENT.md#F-MARCA-R007)). «Dudoso», «Heredado» y
> «Sin marca» no lo sacan.
>
> La marca sigue sin cambiar ninguna decisión: aprobar, rechazar y publicar funcionan igual con cualquier marca
> ([F-MARCA-R006](../2026-10-26.02_el-revisor-marca-lo-aprobado/REQUIREMENT.md#F-MARCA-R006)). Lo que cambia es en qué
> lista se ve el ejercicio.

<a id="F-TODOS-R003"></a>
### Rule[F-TODOS-R003] - El «resuelto» nombra al juez que observó y al revisor
**Severity**: major | **Validation**: VALIDATED

> Cuando el ejercicio de un cambio que dan por bueno no entra en «Todos los jueces lo aprueban», la línea debajo del chip
> ([F-DICE-R002](../2026-10-28.03_cada-aviso-dice-que-paso/REQUIREMENT.md#F-DICE-R002)) nombra al juez que le dejó una
> observación, aunque sea leve, y dice «el revisor lo marca mal» si es el caso. La observación leve de un analizador que
> no es juez no se nombra como motivo ([F-LEVE-R003](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R003)).

## Contexto

- **Cuánto cambia**: el 31/10/2026, en la C2 de A1, 1.781 ejercicios tenían a todos los jueces al día (entre «Todos los
  jueces lo aprueban» y «Preparados para entregar»). 908 tenían una observación leve de un juez (miniteoría 809, calidad
  didáctica A1 141, casi duplicados 51, sentido de la traducción 3, opción múltiple 3) y 56 la marca «Mal» del revisor;
  36, las dos. Quedan 853.
- **Quién es juez**: los que el análisis vigente nombra como jueces y los que declaran cobertura de juez, como «Casi
  duplicados». Los demás son analizadores: sus reglas son fijas y no opinan.
- **Fuera de alcance**:
  - «Dudoso» no saca al ejercicio: José puede pedirlo.
  - Lo leve se sigue pudiendo corregir: sus tareas siguen en el plan.
  - «Según los jueces» de cada cambio («Lo dan por bueno», «No consta arreglado», «Empeoró») no cambia.

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | En ittm, `test/tablero_html/correcciones/nadie_le_ve_problemas_test.dart`: lo deja afuera una observación leve de un juez al día y la de «Casi duplicados» declarado en cobertura; entra con la leve de un analizador; con la leve de un juez de antes queda en «Faltan opiniones». `html/test/todos-quiere-decir-todos.test.js`: aprobado y con un juez que observó algo leve, queda en «Vos lo aprobás», contado entre los que tienen un problema |
| R002 | `html/test/todos-quiere-decir-todos.test.js`: con «Mal», sale de «Todos los jueces lo aprueban», de «Faltan opiniones» y de «Preparados para entregar», y aprobado queda en «Vos lo aprobás» contado entre los que tienen un problema; la marca «Mal» de antes también lo saca; «Dudoso», «Heredado» y «Sin marca» no |
| R003 | `html/test/todos-quiere-decir-todos.test.js`: el «resuelto» nombra al juez que dejó algo leve, dice «el revisor lo marca mal», y no nombra lo leve de un analizador |

## References

- [F-LEVE-R001](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R001): lo leve de un analizador no frena.
- [F-NADIE-R001](../2026-10-20.01_nadie-le-ve-problemas/REQUIREMENT.md#F-NADIE-R001): elegibilidad del recorrido sin
  problemas.
- [F-MARCA-R006](../2026-10-26.02_el-revisor-marca-lo-aprobado/REQUIREMENT.md#F-MARCA-R006): la marca del revisor en
  Correcciones.
