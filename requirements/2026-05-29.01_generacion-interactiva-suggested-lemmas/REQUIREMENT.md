---
feature:
  id: FEAT-LAGAG
  code: F-LAGAG
  name: Generación interactiva con consulta de suggested lemmas
  priority: high
---

# Generación interactiva con consulta de suggested lemmas

## TL;DR

**Qué**: Un modo de generación de candidato de quiz para tareas `LEMMA_ABSENCE`
en el que el modelo arranca con una semilla de los suggested lemmas más
prioritarios y, durante la generación, puede solicitar más suggested lemmas
para la misma task hasta cerrar con un único candidato final.

**Por qué**: Hoy la generación dinámica recibe una lista estática horneada de
antemano; cuando esa lista no alcanza, el modelo no tiene forma de pedir las
palabras que necesita. Este modo le da esa capacidad de manera acotada y
trazable.

## Reglas de Negocio

<a id="F-LAGAG-R001"></a>
### Rule[F-LAGAG-R001] - Semilla inicial con los lemmas más prioritarios
**Severity**: high | **Validation**: VALIDATED

> Al iniciar la generación, el modelo recibe una lista semilla con los suggested
> lemmas más necesarios de la task —no la lista completa—, entendiendo "más
> necesarios" como los de mayor prioridad de recomendación dentro del nivel
> aplicable a la task.

<a id="F-LAGAG-R002"></a>
### Rule[F-LAGAG-R002] - El modelo puede solicitar más suggested lemmas
**Severity**: high | **Validation**: VALIDATED

> Durante la generación, cuando la semilla no le alcanza, el modelo puede
> solicitar suggested lemmas adicionales para la misma task, filtrando por tipo
> gramatical y/o nivel y/o cantidad, con la misma semántica de consulta de
> [F-CSLATDC-R001](#F-CSLATDC-R001). Debe quedar claro para el modelo que esta
> capacidad está disponible y cómo expresarla, y el sistema responde con los
> lemmas aplicables a la task.

<a id="F-LAGAG-R003"></a>
### Rule[F-LAGAG-R003] - Modo explícito, reconocible y trazable
**Severity**: high | **Validation**: VALIDATED

> La generación con capacidad de solicitar palabras es un modo explícito,
> distinto del modo dinámico de un solo paso y del modo fijo. Su selección y su
> default son inequívocos, y en la trazabilidad de la propuesta producida queda
> registrado que se usó este modo, junto con la identidad de estrategia y
> proveedor, de forma consistente con [F-LAGEN-R009](#F-LAGEN-R009).

<a id="F-LAGAG-R004"></a>
### Rule[F-LAGAG-R004] - Presupuesto de solicitudes acotado
**Severity**: high | **Validation**: VALIDATED

> La cantidad de solicitudes de suggested lemmas que el modelo puede hacer en una
> generación está limitada. Si se agota ese presupuesto sin que el modelo entregue
> un candidato, la generación termina en una falla observable con su propia
> categoría, en línea con las categorías de falla de [F-LAGEN-R006](#F-LAGEN-R006).

**Error**: "Se agotó el presupuesto de consultas de suggested lemmas sin obtener un candidato"

<a id="F-LAGAG-R005"></a>
### Rule[F-LAGAG-R005] - Proveedor incapaz de sostener el intercambio
**Severity**: medium | **Validation**: VALIDATED

> Si el proveedor activo no es capaz de emitir solicitudes de suggested lemmas
> durante la generación, la situación se reporta como una falla observable y no
> como un cuelgue silencioso.

**Error**: "El proveedor activo no puede sostener la consulta interactiva de suggested lemmas"

<a id="F-LAGAG-R006"></a>
### Rule[F-LAGAG-R006] - Candidato final con la forma heredada
**Severity**: high | **Validation**: VALIDATED

> Al cerrar el intercambio, el modelo entrega exactamente un candidato final con
> la misma forma y las mismas validaciones estructurales que ya exige la
> generación dinámica en [F-LAGEN-R005](#F-LAGEN-R005): un solo candidato, sin
> verificación de calidad, sin reintento automático y con no-determinismo. El
> modo fijo no usa este intercambio y queda intacto.

## Alcance

- **En alcance**: Modo de generación interactiva para `LEMMA_ABSENCE`: semilla
  inicial, solicitudes acotadas de más suggested lemmas durante la generación,
  selección y trazabilidad del modo, y las fallas por presupuesto agotado y por
  proveedor incapaz.
- **Fuera de alcance**: La forma y validación del candidato final (heredadas de
  F-LAGEN), la semántica de consulta de suggested lemmas (definida en F-CSLATDC),
  el modo fijo/canned, y cualquier verificación de calidad o reintento del
  candidato.

## User Journeys

> **Journeys absorbidos por FEAT-LASAG.** Los journeys de la generación
> interactiva (antes F-LAGAG-J001 … F-LAGAG-J003) fueron reemplazados por la ruta
> única del agente sentinel-agent y ahora viven como F-LASAG-J001 … F-LASAG-J009
> en `requirements/2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md`. La
> capacidad de solicitar más suggested lemmas durante la generación (antecedente
> directo del react con una sola tool) se conserva en el agente; lo que se retira
> es la **mecánica interactiva separada** que estos journeys describían. Las reglas
> de negocio de esta feature (F-LAGAG-R001 … F-LAGAG-R006) siguen vigentes como
> contexto. Ver [FEAT-LASAG](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md).

## Referencias

- **FEAT-LAGEN** — Define la generación de candidatos de quiz para `LEMMA_ABSENCE`
  (modo dinámico de un solo paso y modo fijo), la forma del candidato final, las
  categorías de falla y la trazabilidad de estrategia y proveedor. Este modo
  reutiliza esas reglas en lugar de redefinirlas. Citada por
  [F-LAGAG-R003](#F-LAGAG-R003), [F-LAGAG-R004](#F-LAGAG-R004) y
  [F-LAGAG-R006](#F-LAGAG-R006).
- **FEAT-CSLATDC** — Define la consulta de suggested lemmas para una task, con
  filtro obligatorio por nivel, filtro opcional por tipo gramatical, límite de
  cantidad y orden por prioridad de recomendación. Este modo usa esa misma
  consulta como comportamiento. Citada por [F-LAGAG-R002](#F-LAGAG-R002).
