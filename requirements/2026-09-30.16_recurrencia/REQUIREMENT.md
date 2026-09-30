---
feature:
  id: FEAT-RECUR
  code: F-RECUR
  name: Recurrencia visible, por palabra y por nivel
  priority: high
---

# Recurrencia visible, por palabra y por nivel

## TL;DR

**Qué**: `lemma-recurrence` sigue dando el mismo puntaje de curso, y además publica cada palabra (cuántas veces está,
cada cuántas palabras vuelve y dónde está su hueco más largo) y la recurrencia dentro de cada nivel, como detalle.

**Por qué**: Es la peor métrica del curso (8 %) y hoy sólo deja ese número: la vista del dashboard dice «No lemma
recurrence data available», y nadie puede ver qué palabra vuelve poco ni en qué nivel se pierde.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy saber qué palabras vuelven poco:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible: el curso trae
   `lemma-recurrence` = 0,08 ([F-LREC-R010](#F-LREC-R010)).
2. Pide el detalle → `StatsAnalyzerCommand.showStats("lemma-recurrence", coursePath)` o `GetCommand.get("audit", …)`.
   ⚠️ El informe guarda sólo el puntaje: las estadísticas por palabra de [F-LREC-R012](#F-LREC-R012) y el resumen de
   [F-LREC-R013](#F-LREC-R013) no llegan al informe, así que la vista queda vacía.
3. Mira un nivel. ⚠️ No hay nada por nivel ([F-LREC-R013](#F-LREC-R013): sólo curso).

**Punto de fricción**: pasos 2 y 3. El cálculo existe, pero su resultado se tira.

**Cambio mínimo necesario**: que el informe publique lo que el analizador ya calcula, más el hueco más largo y la
recurrencia dentro de cada nivel como detalle. Umbrales, puntaje y plan quedan como hoy.

## Reglas de Negocio

Grupos: **A**, lo que no cambia (R001–R002); **B**, lo que publica (R003–R006). Base: análisis 2026-09-30T11-54-02,
reproducido con un prototipo: 91.873 posiciones, 2.791 lemas de contenido, 1.750 que vuelven al menos una vez.

### Grupo A — Lo que no cambia

<a id="F-RECUR-R001"></a>
### Rule[F-RECUR-R001] - Umbrales, orden y puntaje son los de hoy
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El análisis usa las posiciones en orden A1 → B2 y los 2.000 lemas más usados; una palabra es sobre-expuesta si vuelve
> en promedio cada 50 palabras o menos y poco vista si pasa de 1.000; el puntaje del curso es el de hoy, 0,08, con el
> mismo 73,9 % de vocabulario. Ningún nivel recibe puntaje de `lemma-recurrence`.

<details><summary>Detalle</summary>

Mantiene [F-LREC-R001](#F-LREC-R001) a [F-LREC-R015](#F-LREC-R015) y [F-HALL-R009](#F-HALL-R009). Hoy son 134 normales,
1.570 poco vistas y 46 sobre-expuestas: 134 / 1.750 = 7,7 %, que con dos decimales da 0,08. El tope de 2.000 no corta:
el puesto 2.000 cae entre los 1.041 lemas que aparecen una sola vez, que [F-LREC-R006](#F-LREC-R006) excluye; en la
práctica ya se analizan todos los que vuelven, que es la opción que José marcó en DOUBT-TOP-N-VALUE.

</details>

<a id="F-RECUR-R002"></a>
### Rule[F-RECUR-R002] - El plan y las tareas no cambian
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El plan del mismo análisis sigue teniendo una tarea LEMMA_RECURRENCE, en el curso, y 4.036 en total. Lo que publica
> este requirement no crea tareas.

Mantiene [F-HALL-R013](#F-HALL-R013) y la ligadura de FEAT-HALL: `lemma-recurrence` en el curso.

### Grupo B — Lo que publica

<a id="F-RECUR-R003"></a>
### Rule[F-RECUR-R003] - Cada palabra analizada queda en el informe
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para cada lema analizado, el informe del curso publica: cuántas veces aparece, cada cuántas palabras vuelve en promedio,
> su estado (normal, poco vista o sobre-expuesta), la desviación de esos intervalos, su hueco más largo con los niveles
> donde empieza y termina, y los niveles donde aparece. Van de más a menos veces, como [F-LREC-R013](#F-LREC-R013), y
> `stats analyzer lemma-recurrence`, `get audit` y la vista muestran esos mismos datos.

<details><summary>Detalle: ejemplos del curso del 29/9</summary>

| Palabra | Veces | Vuelve cada | Estado | Hueco más largo |
|---|---:|---:|---|---|
| go | 603 | 152 | normal | 3.642, en A2 |
| movie | 90 | 1.005 | poco vista | 6.510, de A1 a A2 |
| vegetable | 10 | 9.149 | poco vista | 27.780, en B1 |
| suppose | 41 | 696 | normal | 27.379, de B1 a B2 |
| define | 24 | 21 | sobre-expuesta | 70, en B1 |

La desviación, que José preguntó qué es (DOUBT-STDDEV-SCORING de FEAT-LREC), dice si los huecos son parejos: «suppose»
vuelve en promedio cada 696 palabras, pero de a racimos, con un hueco de 27.379. El hueco más largo dice lo mismo en
palabras de la pantalla. Cumple [F-HALL-R010](#F-HALL-R010): un número publicado, el mismo en todos los verbos.

</details>

<a id="F-RECUR-R004"></a>
### Rule[F-RECUR-R004] - La recurrencia dentro de cada nivel, como detalle
**Severity**: critical | **Validation**: ASSUMPTION

> Para cada nivel, el informe publica la recurrencia medida sólo con los ejercicios de ese nivel, con los mismos umbrales:
> cuántos lemas vuelven al menos una vez en el nivel, cuántos son normales, poco vistos y sobre-expuestos, y qué parte
> son normales. Se publica marcada como detalle de `lemma-recurrence`: no es el puntaje del nivel ni entra a su vocabulario.

<details><summary>Detalle</summary>

| Nivel | Posiciones | Lemas que vuelven | Normales | Poco vistos | Sobre-expuestos | Normales |
|---|---:|---:|---:|---:|---:|---:|
| A1 | 15.802 | 450 | 178 | 257 | 15 | 39,6 % |
| A2 | 18.967 | 752 | 155 | 569 | 28 | 20,6 % |
| B1 | 38.949 | 1.139 | 161 | 938 | 40 | 14,1 % |
| B2 | 18.155 | 819 | 165 | 607 | 47 | 20,1 % |

Es lo que pedía DOUBT-PER-LEVEL-ANALYSIS de FEAT-LREC (José marcó B), sin mover números: cumple [F-HALL-R010](#F-HALL-R010),
las sub-métricas no se promedian. Convertirlo en puntaje de nivel: [DOUBT-PUNTAJE-POR-NIVEL](#DOUBT-PUNTAJE-POR-NIVEL). El
semáforo lo espera para usar la recurrencia en el verde ([F-SEMAF-R002](#F-SEMAF-R002)).

</details>

<a id="F-RECUR-R005"></a>
### Rule[F-RECUR-R005] - Cada palabra fuera de rango es un hallazgo del curso
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando el puntaje del curso es menor que 1, cada lema poco visto o sobre-expuesto deja un hallazgo en el curso
> ([F-HALL-R001](#F-HALL-R001)): gravedad *baja*, resolución *sólo ordena*, costo *instantáneo*. La evidencia da el
> [lema](glossary:Lema), sus veces, cada cuántas palabras vuelve contra la meta (más de 50 y hasta 1.000) y su hueco más largo.

Son 1.616 hallazgos: 1.570 poco vistos y 46 sobre-expuestos. Ejemplo: «movie: 90 veces, vuelve cada 1.005 palabras (meta:
más de 50 y hasta 1.000); su hueco más largo, 6.510, va de A1 a A2». La identidad es el lema ([F-HALL-R014](#F-HALL-R014)).

<a id="F-RECUR-R006"></a>
### Rule[F-RECUR-R006] - La ficha la describe con su nombre de siempre
**Severity**: major | **Validation**: AUTO_VALIDATED

> `lemma-recurrence` figura en `get analyzers` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Las palabras vuelven
> a intervalos sanos?»; lee las oraciones de todo el curso en orden; familia vocabulario; evalúa el curso; meta: que
> cada palabra vuelva cada más de 50 y hasta 1.000 palabras; resolución sólo ordena; costo instantáneo.

## Contexto

La propuesta la muestra como «Recurrencia · sólo de curso · 8,0» y pide completar la vista vacía. El 8 % tiene una razón
que se ve recién con los datos: 1.000 palabras son unos 123 ejercicios, así que una palabra repartida por todo el curso
necesita unas 92 apariciones para ser normal. «movie» está en 90 y queda afuera por 5 palabras de promedio; las palabras de A1, que
se usan en todo el curso, son normales sólo en el 15,6 % (76 de 486). Dentro de cada nivel la foto cambia: A1 39,6 %.

Lo que José dejó abierto en FEAT-LREC sigue abierto allá; este requirement no cambia umbrales ni puntaje, y publica lo
necesario para decidirlo con datos.

**El examen.** No tiene casos en el 077. Se prueba con los números: `analyze` antes y después da 0,08, 73,9 % y 4.036
tareas, y la suma de normales, poco vistas y sobre-expuestas da 1.750 ([F-LREC-R009](#F-LREC-R009)). **Costo**: segundos.

## Alcance

- **Adentro**: R001 a R006.
- **Afuera**: cambiar umbrales, puntaje o tareas sin decisión de José; dibujar la vista (paquete G); el puntaje gradual y
  la desviación en el puntaje, que siguen como dudas de FEAT-LREC.

## User Journeys

### Journey[F-RECUR-J001] - Ver qué palabras vuelven poco, en el curso y en cada nivel
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-RECUR-J001
    name: Ver qué palabras vuelven poco, en el curso y en cada nivel
    flow:
      - id: analizar
        action: "El operador corre analyze con lemma-recurrence y pide stats analyzer lemma-recurrence"
        gate: [F-RECUR-R001]
        then: por_palabra
      - id: por_palabra
        action: "Cada lema analizado trae veces, cada cuántas palabras vuelve, estado, desviación, hueco más largo y niveles"
        gate: [F-RECUR-R003]
        outcomes:
          - when: "El puntaje del curso es menor que 1"
            then: hallazgos
          - when: "Todos los lemas vuelven a intervalos sanos"
            then: por_nivel
      - id: hallazgos
        action: "Cada lema poco visto o sobre-expuesto deja un hallazgo baja que sólo ordena en el curso"
        gate: [F-RECUR-R005]
        then: por_nivel
      - id: por_nivel
        action: "Cada nivel trae su recurrencia como detalle, y el puntaje, el 73,9 % y las 4.036 tareas quedan iguales"
        gate: [F-RECUR-R002, F-RECUR-R004]
        result: success
```

## Open Questions

<a id="DOUBT-PUNTAJE-POR-NIVEL"></a>
### Doubt[DOUBT-PUNTAJE-POR-NIVEL] - ¿La recurrencia dentro del nivel pasa a ser el puntaje de ese nivel?
- [ ] A (recomendada): no; se publica como detalle (R004) y ningún número cambia.
- [ ] B: sí, como marcaste en FEAT-LREC: el curso sigue en 73,9 %, pero bajan los niveles.

**Answer**: Pendiente. Recomiendo A hasta que la corrección muestre que se puede mejorar. Con B, el vocabulario de cada
nivel pasa de A1 96,5 a 88,4, A2 94,2 a 83,6, B1 79,5 a 70,1 y B2 63,5 a 57,3.

<a id="DOUBT-UNA-SOLA-VEZ"></a>
### Doubt[DOUBT-UNA-SOLA-VEZ] - ¿Los 1.041 lemas que aparecen una sola vez cuentan como poco vistos?
- [ ] A (recomendada): no, como hoy ([F-LREC-R006](#F-LREC-R006)); ya los cuenta `lemma-count` como poco vistos.
- [ ] B: sí, como te inclinabas en FEAT-LREC: el puntaje baja a 0,05 y el vocabulario a 73,5 %.

**Answer**: Pendiente. Recomiendo A: es la superposición que temías; una palabra vista una vez ya es hallazgo de
`lemma-count` ([F-POCOV-R003](#F-POCOV-R003)).

## References

- **FEAT-LREC** — Posiciones, umbrales, estadísticas por lema y puntaje; sus dudas abiertas: R001, R003, R004, R005.
- **FEAT-HALL** — Hallazgo, ficha, un solo número publicado y lo que no cambia: R001, R002, R003, R005, R006.
- **FEAT-POCOV, FEAT-SEMAF** — Las palabras vistas una vez; el verde por recurrencia: dudas, R004.
