---
feature:
  id: FEAT-CIEGA
  code: F-CIEGA
  name: Muestra ciega de cada tanda
  priority: high
---

# La muestra ciega: alguien que no vio las correcciones elige entre el antes y el después

## TL;DR

**Qué**: Para cada tanda, content-audit sortea pares de antes y después, los muestra sin decir cuál es cuál, guarda lo
que elige quien revisa y publica el resultado, con su margen, contra la meta.

**Por qué**: Es la única medida que no depende de ningún analizador ni agente: en la tanda de las verduras, tres de los
cuatro arreglos no los ve ningún analizador de hoy.

## Recorrido de uso (estado actual)

Cómo José sabría hoy si lo que corrigió una tanda quedó mejor, sin fiarse de quien corrigió:

1. Ve el antes y el después de cada cambio → `GetConsolidatedCommand.getConsolidated(coursePath, format)`. ✓ Disponible:
   los campos del ejercicio que cambiaron, con sus tres fotos. ⚠️ Pero con la foto de antes y la de después rotuladas, en
   el formato del curso y no como lo ve el alumno.
2. Elige una muestra al azar → ⚠️ no existe.
3. La revisa sin saber cuál es cuál → ⚠️ no existe.
4. Suma lo que eligió y lo compara con una meta → ⚠️ no existe.

**Punto de fricción**: pasos 2 a 4. No hay manera de medir las correcciones sin mirar quién las hizo ni cuál es la nueva.

**Cambio mínimo necesario**: sortear una muestra reproducible de los cambios de una tanda, presentarla a ciegas como la
ve el alumno, registrar las respuestas y, al cerrar, publicar el resultado con su margen y si cumple la meta. Lo único
que escribe es la muestra y sus respuestas.

## Reglas de Negocio

Grupos: **A**, la muestra de pares (R001–R004); **B**, dos personas y la tasa de error (R005–R006). *Tanda*: las
propuestas pendientes de la tanda abierta (paquete E, FEAT-TANDA); sin tanda abierta, las pendientes aplicables del plan
activo, como en FEAT-METAS. *Par*: un cambio de la tanda, con su antes (el ejercicio como está, con lo aprobado) y su
después (con la propuesta).

### Grupo A — La muestra de pares

<a id="F-CIEGA-R001"></a>
### Rule[F-CIEGA-R001] - La muestra se sortea de la tanda y se puede repetir
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para una tanda, content-audit sortea n cambios —n es el de la meta de la muestra; todos, si hay menos— entre los que no
> vuelven ([F-METAS-R003](#F-METAS-R003)), con una semilla que publica: la misma tanda con la misma semilla da la misma
> muestra, en el mismo orden.

Se sortea entre lo que se publicaría: lo que ya vuelve no necesita a nadie. **Criterio**: pedir dos veces la muestra de
la misma tanda con la misma semilla da los mismos 100 cambios; con otra semilla, otros.

<a id="F-CIEGA-R002"></a>
### Rule[F-CIEGA-R002] - Cada par se ve como lo ve el alumno, sin pistas de cuál es cuál
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada par muestra el antes y el después como los ve el alumno —consigna, oración con el hueco, pista, opciones,
> [traducción](glossary:Traducción) y miniteoría, antes y después de contestar— con el lado de cada uno sorteado por
> par, y sin marcas de analizadores, sin el motivo del cambio, sin fechas ni autores. Cuál es el antes se revela recién
> al cerrar la muestra.

**Criterio**: en la muestra de las verduras, el par de la propuesta 11 se ve como «A · I ___ (love) vegetables. Me
encantan las verduras.» y «B · I ___ a lot of vegetables. Me gustan mucho las verduras.», y el lado del después cambia
de un par a otro según el sorteo. Usa el ejercicio como lo ve el alumno, que pide el paquete G (pedido 2) y
[F-HALL-R003](#F-HALL-R003) exige para la evidencia.

<a id="F-CIEGA-R003"></a>
### Rule[F-CIEGA-R003] - Una respuesta de cuatro, que no se cambia después de revelar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para cada par, quien revisa elige una de cuatro respuestas: A es mejor, B es mejor, iguales, o los dos tienen un error.
> content-audit guarda cada respuesta con quién y cuándo; al cerrar la muestra deja de aceptar respuestas y cambios.
> Responder no toca el curso, las propuestas ni el historial de análisis.

<a id="F-CIEGA-R004"></a>
### Rule[F-CIEGA-R004] - El resultado se publica con su margen, contra la meta
**Severity**: critical | **Validation**: VALIDATED

> Al cerrar, content-audit publica, sobre los pares respondidos, cuántos quedaron mejor o igual, cuántos peor —el antes
> era mejor— y en cuántos los dos tienen un error, cada uno con su intervalo al 95 %, y si la meta de la muestra se
> cumple con el criterio que ella declara: lo observado o la cota del intervalo. Publica además, sumando las muestras
> cerradas desde la línea base, cuántos pares y cuántos peores lleva el curso, la cota de los peores y si queda por
> debajo de la meta del curso.

<details><summary>Detail</summary>

**Criterio**: 100 pares con 95 mejor o igual, 1 peor y 4 con error en los dos publican 95 % [88,8; 97,8], 1 % [0,2; 5,4]
y 4 % [1,6; 9,8]. Con la meta de José del 30/9, «al menos 90 mejor o igual y a lo sumo 1 peor, observados», cumple; con
«la cota de peores por debajo de 2 %», no cumple, porque con 100 pares la cota más baja posible es 3,0 %. Por eso el
«menos de 2 %» se afirma sumando tandas: con 300 pares y 2 peores la cota es 2,1 %; con 400 y 2, 1,6 %
([DOUBT-TAMANO-CIEGA](#DOUBT-TAMANO-CIEGA)). La meta y su criterio viven en la declaración de metas
([F-METAS-R005](#F-METAS-R005)); el resultado entra en el veredicto de la tanda ([F-METAS-R008](#F-METAS-R008)) y la
suma es una meta de avance, que no decide.

</details>

### Grupo B — Dos personas y la tasa de error

<a id="F-CIEGA-R005"></a>
### Rule[F-CIEGA-R005] - Con dos personas, cuánto coinciden
**Severity**: major | **Validation**: VALIDATED

> Si dos personas responden pares en común, content-audit publica en cuántos coinciden y su kappa sobre esos pares; el
> resultado de la tanda usa las respuestas de quien la meta nombra como responsable.

La responsable es José; en la primera tanda un tester responde 30 de los mismos pares, los primeros de la muestra, para
medir si el criterio se comparte (José, 30/9: [DOUBT-QUIEN-CIEGA](#DOUBT-QUIEN-CIEGA)).

<a id="F-CIEGA-R006"></a>
### Rule[F-CIEGA-R006] - La tasa de error a ciegas mide lo que los analizadores no ven
**Severity**: major | **Validation**: AUTO_VALIDATED

> A pedido, content-audit sortea n ejercicios de un nivel o del curso y los presenta de a uno, mezclando versiones de la
> línea base y de la proyección sin decir cuál es cuál; quien revisa marca si el ejercicio tiene un error. Al cerrar,
> publica el porcentaje con error antes y después, con su margen al 95 %, junto al «algún error» de los analizadores en
> esos mismos ejercicios.

<details><summary>Detail</summary>

Es la medida de la propuesta v2: «los ejercicios con error se miden en 400 al azar, revisados a ciegas antes y después,
con un margen de ±4 puntos». Con 400, el margen es ±4,3 puntos al 25,7 % y ±2,1 al 5 %. Si el porcentaje a ciegas y el
de los analizadores no coinciden dentro del margen, los analizadores se están perdiendo errores: es la prueba de que
«algún error» mide lo que dice ([F-HALL-R008](#F-HALL-R008)).

</details>

## Contexto

La propuesta v2 (pestaña 08, «La muestra ciega») la describe así: 100 cambios al azar por tanda, cada par sin saber cuál
es cuál, cuatro respuestas, el orden sorteado en cada par y la meta de «90 % mejor o igual, con menos de 2 % de errores
nuevos»: «es la única medida que no depende de ningún agente».

Por qué hace falta: en la tanda de las verduras, los analizadores de hoy no ven tres de los cuatro arreglos (la pista de
«I ___ a lot of vegetables.», «sport» como verbo y «ourselves»): los tres ejercicios dan 1,0 antes y después. Y el 88 %
de los 3.133 cambios del 077 son de categorías que ningún analizador mide.

Lo que puede afirmar una muestra (cota superior al 95 % de los que empeoran, a una cola):

| Pares | 0 peores | 1 peor | 2 peores |
|---:|---:|---:|---:|
| 100 | 3,0 % | 4,7 % | 6,2 % |
| 150 | 2,0 % | 3,1 % | 4,1 % |
| 300 | 1,0 % | 1,6 % | 2,1 % |
| 400 | 0,7 % | 1,2 % | 1,6 % |

Con 100 pares no se puede afirmar «menos de 2 %» ni sin ningún peor. Como alarma, en cambio, sirve: con 100 pares y la
regla «2 peores o más, no pasa», una tanda con 0,5 % real de peores falla 9 veces de cada 100, una con 2 % falla 60 y una
con 5 %, 96. Por eso la meta declara su criterio (R004) y el tamaño queda para José
([DOUBT-TAMANO-CIEGA](#DOUBT-TAMANO-CIEGA)).

Para que sea ciega, quien revisa no puede haber visto los cambios antes: si José aprueba la tanda en la bandeja y
después responde la muestra, ya sabe cuál es el nuevo. Por eso José la responde antes de ver el resto de la bandeja
(José, 30/9), y la bandeja del paquete E la pone primero y sola hasta que se cierra (a alinear en FEAT-BANDEJA). La tasa
de error a ciegas de 400 ejercicios, al empezar y al terminar, la hace un tester.

## Alcance

- **Adentro**: R001 a R006.
- **Afuera**: cómo se dibuja (paquete G); el ejercicio como lo ve el alumno, que se construye para 0.3 y G; qué es una
  tanda y cuándo se abre la bandeja (paquete E); los valores de la meta (FEAT-METAS y sus dudas).

## User Journeys

### Journey[F-CIEGA-J001] - Revisar a ciegas los cambios de una tanda
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-CIEGA-J001
    name: Revisar a ciegas los cambios de una tanda
    flow:
      - id: sortear
        action: "El operador pide la muestra ciega de la tanda con una semilla, y content-audit sortea los cambios entre los que no vuelven"
        gate: [F-CIEGA-R001]
        then: responder
      - id: responder
        action: "Quien revisa ve cada par como lo ve el alumno, sin saber cuál es el antes, y elige una de las cuatro respuestas"
        gate: [F-CIEGA-R002, F-CIEGA-R003]
        then: cerrar
      - id: cerrar
        action: "El operador cierra la muestra; content-audit revela los lados y deja de aceptar respuestas"
        gate: [F-CIEGA-R003]
        outcomes:
          - when: "La meta de la muestra se cumple con su criterio"
            then: cumple
          - when: "La meta de la muestra no se cumple"
            then: no_cumple
      - id: cumple
        action: "content-audit publica mejor o igual, peor y los dos con error, con sus intervalos, y la meta de la muestra como cumplida"
        gate: [F-CIEGA-R004]
        result: success
      - id: no_cumple
        action: "content-audit publica las tres proporciones con sus intervalos y dice qué parte de la meta no se cumple"
        gate: [F-CIEGA-R004]
        result: failure
```

## Open Questions

<a id="DOUBT-QUIEN-CIEGA"></a>
### Doubt[DOUBT-QUIEN-CIEGA] - ¿Quién responde la muestra ciega?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): José, antes de abrir la tanda en la bandeja; en la primera tanda, un tester responde 30 de
  los mismos pares para medir cuánto coinciden (R005). La tasa de error a ciegas (R006), 400 al empezar y 400 al
  terminar, la hace un tester.
- [ ] Opción B: siempre un tester, que no aprueba nada; José lee el resultado.
- [ ] Opción C: José y un tester en todas las tandas, sobre los mismos pares.

**Answer**: **Opción A** (José, 30/9): la hace José antes de abrir la bandeja; en la primera tanda un tester repite 30
pares; la tasa de error de 400 ejercicios la hace un tester.

<a id="DOUBT-TAMANO-CIEGA"></a>
### Doubt[DOUBT-TAMANO-CIEGA] - ¿Cuántos pares por tanda, y cómo se lee «menos de 2 % que empeoran»?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): 100 pares por tanda; la tanda pasa con al menos 90 mejor o igual y a lo sumo 1 peor,
  observados; y el «menos de 2 %» se afirma sobre la suma de las tandas (con 300 pares y 2 peores la cota es 2,1 %; con
  400 y 2, 1,6 %).
- [ ] Opción B: 150 pares por tanda, y pasa sólo con 0 peores (cota 2,0 %).
- [ ] Opción C: 100 pares y sólo lo observado, sin cota.

**Answer**: **Opción A** (José, 30/9): 100 pares por tanda, con la meta sobre lo contado; el «menos de 2 %» se afirma
sumando tandas (R004).

## References

- **FEAT-METAS** — Lo que vuelve, la declaración de metas y el veredicto de la tanda ([F-METAS-R003](#F-METAS-R003),
  [F-METAS-R005](#F-METAS-R005), [F-METAS-R008](#F-METAS-R008)). Citado por R001 y R004.
- **FEAT-HALL** — La evidencia como la ve el alumno y «algún error» ([F-HALL-R003](#F-HALL-R003),
  [F-HALL-R008](#F-HALL-R008)). Citado por R002 y R006.
