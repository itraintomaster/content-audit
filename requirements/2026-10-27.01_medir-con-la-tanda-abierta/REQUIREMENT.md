---
feature:
  id: FEAT-ABIER
  code: F-ABIER
  name: Medir con la tanda abierta - volver a medir, completar e integrar sin cerrar la tanda
  priority: major
---

# Medir con la tanda abierta

## TL;DR

**Qué**: Volver a medir con un juez, Completar las opiniones que faltan e integrar una medición al análisis vigente se
pueden hacer con la tanda abierta, igual que con la cerrada. Medir no cierra la tanda: al terminar queda como estaba.

**Por qué**: La C2 de A1 se corrige abierta. El 27/10/2026 tenía 429 ejercicios sin problemas con la opinión de la
Consigna de una versión anterior y 206 sin opinión de Naturalidad, y no se podían medir sin cerrarla. Cerrar no tiene
vuelta: pasa el portón, devuelve cambios y sortea la muestra ciega. La regla de medir sólo con la tanda cerrada venía
del orden de antes (cerrar y después medir). José, el 27/10/2026: «Sí: dejá medir con la tanda abierta (Reanalizar,
Completar e integrar), con sus pruebas».

## Recorrido de uso (estado actual)

1. La mesa corrige la tanda abierta y sus corridas dejan lo que miden en el análisis vigente ✓.
2. José filtra «Todos los jueces lo aprueban» y revisa a mano ✓.
3. Un juez opinó con una versión anterior, o no opinó: José pide volver a medir o Completar ⚠️ — se rechaza porque la
   tanda está abierta.

**Punto de fricción**: paso 3. Lo único que medía con la tanda abierta era el cierre, que no vuelve atrás.

**Cambio mínimo necesario**: aceptar con la tanda abierta lo que mide y lo que integra, y devolver la tanda a su estado
al terminar. Lo que cambia ejercicios no se toca.

## Reglas de Negocio

<a id="F-ABIER-R001"></a>
### Rule[F-ABIER-R001] - Se vuelve a medir con la tanda abierta
**Severity**: major | **Validation**: VALIDATED

> Volver a medir con un juez —analizar, reanalizar forzado o reanalizar los dos estados— se estima y se lanza sobre
> una tanda abierta igual que sobre una cerrada. Lo frenan sólo otra operación que tome la tanda o la tanda publicada,
> y la estimación dice cuál de las dos. Mientras mide, la tanda queda tomada: nada cambia su contenido, y José sigue
> aprobando y rechazando en Correcciones. La corrección selectiva, que cambia ejercicios, sigue pidiendo la tanda
> cerrada.

<a id="F-ABIER-R002"></a>
### Rule[F-ABIER-R002] - Medir no cierra la tanda
**Severity**: critical | **Validation**: VALIDATED

> Una medición que termina, falla o se concilia deja la tanda como la encontró: abierta si estaba abierta, cerrada si
> estaba cerrada. La medición anota ese estado antes de la primera consulta. Una medición de antes de esta regla, que
> no lo anotó, vuelve a cerrada al conciliarse, como hasta ahora.

<a id="F-ABIER-R003"></a>
### Rule[F-ABIER-R003] - Completar con la tanda abierta
**Severity**: major | **Validation**: VALIDATED

> Completar las opiniones que faltan de la corrección entera se estima y se pide con la tanda abierta, si tiene
> análisis vigente. Lo que deja entra a ese análisis, igual que con la tanda cerrada. También entra la opinión pedida
> para ejercicios sueltos ([F-OPINA-R005](../2026-10-24.03_analisis-y-opiniones/REQUIREMENT.md#F-OPINA-R005)).

<a id="F-ABIER-R004"></a>
### Rule[F-ABIER-R004] - Integrar con la tanda abierta
**Severity**: major | **Validation**: VALIDATED

> Integrar, reemplazar o retirar una medición del análisis vigente se puede con la tanda abierta y no cambia su
> contenido. Se rechaza igual que con la cerrada si cambió el contenido o la selección. También se rechaza mientras otra
> operación toma la tanda, o si la tanda está publicada.

<a id="F-ABIER-R005"></a>
### Rule[F-ABIER-R005] - El tablero ofrece Completar con la tanda abierta
**Severity**: major | **Validation**: VALIDATED

> En Correcciones, el botón Completar se puede tocar con la tanda abierta o cerrada, si hay algo que completar. Su
> explicación ya no dice que se completa con la tanda cerrada. Con la tanda tomada por otra operación o publicada sigue
> apagado. Volver a medir e integrar los frena content-audit, que dice el porqué.

## Contexto

- **Por qué es seguro**: mientras una medición corre, la tanda queda tomada y lo que cambia contenido (corregir, cerrar,
  publicar, convertir, la miniteoría) se rechaza, porque guarda sólo si encuentra la tanda abierta o cerrada. Las
  decisiones de José no cambian el contenido y siguen. Si el contenido cambió antes de integrar, la integración entra
  sólo en los ejercicios cuyo contenido coincide con lo medido.
- **Lo que se arregló de paso**: una medición terminada dejaba la tanda cerrada siempre, aunque se lanzara sobre una
  tanda abierta con otra corrección después (R002).
- **Fuera de alcance**: la corrección selectiva y la miniteoría siguen con la tanda cerrada; cambian ejercicios y no se
  pidieron.

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `SelectiveCorrectionServiceTest`: con la tanda abierta se estima habilitado y se lanza, y mientras mide la tanda queda tomada; con otra operación o publicada se frena y la razón lo dice; la corrección selectiva sigue pidiendo la tanda cerrada. `DefaultBatchReviewTest`: una decisión se guarda con la tanda tomada |
| R002 | `SelectiveCorrectionServiceTest`: una medición sobre la tanda abierta la deja abierta y anota el estado en su proceso; una que falla también; los dos estados también; conciliar la última abierta vuelve al estado anotado, y una de antes sin estado, a cerrada |
| R003 | `FOpinaExerciseOpinionTest`: Completar con la tanda abierta se estima, se mide, entra al análisis y deja la tanda abierta; una opinión suelta también entra; sin análisis vigente se rechaza |
| R004 | `CurrentAnalysisServiceTest`: integrar y retirar con la tanda abierta sin cambiar su contenido; una selección leída antes se rechaza; con la tanda tomada o publicada se rechaza. `FileSystemBatchStoreTest`: lo guardado también acepta la selección de una tanda abierta y rechaza la de una tomada o publicada. En ittm, `test/tablero_html/analisis_vigente_test.dart`: el tablero reconoce «la tanda está tomada» y lo dice con sus palabras |
| R005 | En ittm, el tablero: `html/test/estado-opiniones.test.js` (Completar se toca con la tanda abierta y con la cerrada y no dice «tanda cerrada»; con la tanda tomada o publicada está apagado); el HTML guardado de «abierta sin otra operación» cambia sólo en ese botón (`html/test/fixtures/golden-vistas.json`) |

## References

- Brief 233, la pasada de la C2 contra producción (27/10/2026).
- [F-CORTE-R004](../2026-10-19.02_cortar-sin-trabar-la-tanda/REQUIREMENT.md#F-CORTE-R004): conciliar la última medición abierta libera la tanda.
