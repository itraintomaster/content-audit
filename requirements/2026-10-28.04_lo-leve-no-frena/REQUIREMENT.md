---
feature:
  id: FEAT-LEVE
  code: F-LEVE
  name: Lo leve se ve pero no frena - una observación leve no saca al ejercicio de «Todos los jueces lo aprueban»
  priority: major
---

# Lo leve se ve pero no frena

## TL;DR

**Qué**: Una observación leve de un juez o de un analizador ya no deja al ejercicio afuera de «Todos los jueces lo
aprueban». Se sigue viendo, en el ejercicio y en la lista.

**Por qué**: El 28/10/2026, en la C2 de A1, 1.078 de los 1.825 ejercicios con un problema tenían sólo observaciones
leves. En 941 la observación era de la miniteoría, cuya propia regla llama leve a una tarjeta «verdadera y suficiente»
(brief 239). José: «lo leve se ve pero no frena «Todos los jueces lo aprueban». Antes mirá cuándo ve el alumno la
tarjeta». El alumno ve la miniteoría sólo en la hoja que aparece después de responder, junto con la respuesta correcta.
Que la tarjeta use las palabras del ejercicio no le regala nada.

## Recorrido de uso (estado actual)

1. José abre «Todos los jueces lo aprueban» para revisar ✓.
2. ⚠️ No aparece un ejercicio cuya miniteoría es correcta pero usa las palabras del ejercicio: lo frena una observación
   leve.
3. ⚠️ Para revisarlo tiene que buscarlo entre los 1.825 con un problema.

**Punto de fricción**: paso 2.

**Cambio mínimo necesario**: que lo leve no frene y que se vea.

## Reglas de Negocio

<a id="F-LEVE-R001"></a>
### Rule[F-LEVE-R001] - Una observación leve no frena
**Severity**: major | **Validation**: VALIDATED

> Un ejercicio entra en el recorrido sin problemas detectados aunque tenga observaciones leves de un juez o de un
> analizador, mientras no tenga ninguna de gravedad media, alta o bloqueante. Las demás condiciones de
> [F-NADIE-R001](../2026-10-20.01_nadie-le-ve-problemas/REQUIREMENT.md#F-NADIE-R001) siguen igual: los analizadores
> evaluados por completo, al menos un juez al día y el contenido de hoy.
>
> El juez que dejó una observación leve cuenta con la marca que manda ContentAudit:
> - si opinó con su versión de hoy, lo aprueba;
> - si opinó con una versión anterior, sigue sin contar, y el ejercicio queda en «Faltan opiniones».
>
> Un analizador que dejó una observación leve no figura entre los que lo evaluaron «sin observaciones».

<a id="F-LEVE-R002"></a>
### Rule[F-LEVE-R002] - Lo leve se ve
**Severity**: major | **Validation**: VALIDATED

> En el ejercicio abierto, el bloque de lo que dijo cada juez
> ([F-NADIE-R004](../2026-10-20.01_nadie-le-ve-problemas/REQUIREMENT.md#F-NADIE-R004)) muestra cada observación leve:
> quién la dejó y lo que observó. Dice que no frena. Se ve también en un ejercicio sin cambios, que en estas listas no
> muestra los hallazgos del plan.
>
> Arriba de «Todos los jueces lo aprueban», «Preparados para entregar» y «Faltan opiniones», una nota dice cuántos de esos
> ejercicios tienen observaciones leves.

<a id="F-LEVE-R003"></a>
### Rule[F-LEVE-R003] - Lo leve nunca es el motivo
**Severity**: major | **Validation**: VALIDATED

> Cuando el ejercicio de un cambio que dan por bueno no entra en «Todos los jueces lo aprueban», la línea debajo del
> chip ([F-DICE-R002](../2026-10-28.03_cada-aviso-dice-que-paso/REQUIREMENT.md#F-DICE-R002)) nombra sólo a quien le ve un
> problema medio, alto o bloqueante. Una observación leve no se nombra como motivo.

## Contexto

- **Leve** es la gravedad más baja de un hallazgo. En la miniteoría, son leves:
  - que use las palabras del ejercicio;
  - que agregue otro subtema;
  - que le falte el resaltado del inglés;
  - que parta de la forma y no del sujeto;
  - que use jerga.
- **Cuándo ve el alumno la miniteoría**: verificado el 28/10/2026 en la app. Viaja dentro de la devolución del ejercicio
  y se dibuja sólo en la hoja de corrección, que se abre después de responder.
- **Fuera de alcance**:
  - «Según los jueces» de cada cambio («Lo dan por bueno», «No consta arreglado», «Empeoró») no cambia.
  - Lo leve se sigue pudiendo corregir: sus tareas siguen en el plan.

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `test/tablero_html/correcciones/nadie_le_ve_problemas_test.dart`: entra con una observación leve de un juez y de un analizador, y trae quién la dejó y lo que observó. Lo deja afuera una media, una alta o una bloqueante. Con lo leve de un juez de antes, queda en «Faltan opiniones». El analizador con lo leve no está entre los «sin observaciones» |
| R002 | `html/test/lo-leve-no-frena.test.js`: el bloque del ejercicio muestra cada observación leve con quién la dejó y dice que no frena, también sin cambios. La nota de cada lista cuenta los que tienen algo leve, y no aparece si no hay ninguno |
| R003 | `html/test/lo-leve-no-frena.test.js`: el «resuelto» no nombra lo leve como motivo |

## References

- Brief 239, qué puede hacer una tercera corrección (28/10/2026).
- [F-NADIE-R001](../2026-10-20.01_nadie-le-ve-problemas/REQUIREMENT.md#F-NADIE-R001): elegibilidad del recorrido sin
  problemas.
- [F-DICE-R002](../2026-10-28.03_cada-aviso-dice-que-paso/REQUIREMENT.md#F-DICE-R002): el «resuelto» dice dónde quedó el
  ejercicio.
