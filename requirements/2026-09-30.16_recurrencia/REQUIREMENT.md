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
### Rule[F-RECUR-R001] - Umbrales y orden son los de hoy; las posiciones son las palabras del texto
**Severity**: critical | **Validation**: ASSUMPTION

> El análisis usa como posiciones las palabras del texto en orden A1 → B2 —un token con alguna letra o cifra; un signo
> de puntuación o un símbolo no ocupa posición— y los 2.000 lemas más usados; una palabra es sobre-expuesta si vuelve
> en promedio cada 50 palabras o menos y poco vista si pasa de 1.000. Con eso el puntaje del curso pasa de 0,08 a 0,09
> y el vocabulario del curso de 73,9 % a 74,1 % ([DOUBT-POSICIONES-SIN-PUNTUACION](#DOUBT-POSICIONES-SIN-PUNTUACION)).
> Ningún nivel recibe puntaje de `lemma-recurrence`. Un lema entra al análisis sólo si está escrito con letras, unidas a
> lo sumo por un apóstrofo o un guion («o'clock», «well-known»): la puntuación, los símbolos y los números con cifras que
> el etiquetador lea como palabra de contenido no son una palabra que vuelve («'» de «I 'm», «-» de «in - Spain», «i.»
> de «Neither am I.», «a.m.», «3rd»).

<details><summary>Detalle</summary>

Mantiene [F-LREC-R001](#F-LREC-R001) a [F-LREC-R015](#F-LREC-R015) y [F-HALL-R009](#F-HALL-R009); precisa
[F-LREC-R003](#F-LREC-R003), que ya dejaba afuera la puntuación y los símbolos por su categoría: el etiquetador
compartido igual leía como sustantivo, verbo o adjetivo «'», «-», «i.», «1st», «3rd» y «4th», y «comics» con el lema
«comic_strip». Los números con cifras quedan afuera aunque sean ordinales: no son vocabulario que el alumno recicle, y
`lemma-absence` y `lexis` tampoco evalúan un token con cifras ([F-LABS-R035](#F-LABS-R035)); «third» o «first», con
letras, siguen contando.

Cambia [F-LREC-R001](#F-LREC-R001): las posiciones dejan de contar la puntuación y los símbolos, 78.162 en vez de 91.873
(un número con cifras sí ocupa posición: es una palabra que el alumno lee). Así un intervalo cuenta las palabras que
el alumno lee entre dos apariciones, y no los puntos y las comas. Son 160 normales, 1.534 poco vistas y 49
sobre-expuestas: 160 / 1.743 = 9,2 %, que con dos decimales da 0,09. Sin esto (la opción A, la que está en
`ronda1/integracion`) eran 131, 1.566 y 46 de 1.743 y 0,08; el 30/9, 134, 1.570 y 46 de 1.750. El tope de 2.000 no corta:
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
> `stats analyzer lemma-recurrence` muestra esos mismos datos, leídos del informe sin recalcularlos.

<details><summary>Detalle: ejemplos del curso del 29/9</summary>

| Palabra | Veces | Vuelve cada | Estado | Hueco más largo |
|---|---:|---:|---|---|
| go | 603 | 129 | normal | 3.082, en A2 |
| movie | 90 | 855 | normal | 5.400, de A1 a A2 |
| vegetable | 10 | 7.765 | poco vista | 23.825, en B1 |
| suppose | 41 | 603 | normal | 23.684, de B1 a B2 |
| define | 24 | 17 | sobre-expuesta | 55, en B1 |

Con las posiciones sin puntuación (R001). Con la puntuación eran go 152 y 3.642; movie 1.005, poco vista, y 6.510;
vegetable 9.149 y 27.780; suppose 696 y 27.379; define 21 y 70.

La desviación, que José preguntó qué es (DOUBT-STDDEV-SCORING de FEAT-LREC), dice si los huecos son parejos: «suppose»
vuelve en promedio cada 696 palabras, pero de a racimos, con un hueco de 27.379. El hueco más largo dice lo mismo en
palabras de la pantalla. Cumple [F-HALL-R010](#F-HALL-R010): un número publicado, el mismo en todos los verbos. La vista
del tablero lo lee del informe (paquete G); `get audit` muestra sólo el resumen del curso, como con todo analizador.

</details>

<a id="F-RECUR-R004"></a>
### Rule[F-RECUR-R004] - La recurrencia dentro de cada nivel, como detalle
**Severity**: critical | **Validation**: VALIDATED

> Para cada nivel, el informe publica la recurrencia medida sólo con los ejercicios de ese nivel, con los mismos umbrales:
> cuántos lemas vuelven al menos una vez en el nivel, cuántos son normales, poco vistos y sobre-expuestos, y qué parte
> son normales. Se publica marcada como detalle de `lemma-recurrence`: no es el puntaje del nivel ni entra a su vocabulario.

<details><summary>Detalle</summary>

| Nivel | Posiciones | Lemas que vuelven | Normales | Poco vistos | Sobre-expuestos | Normales |
|---|---:|---:|---:|---:|---:|---:|
| A1 | 12.934 | 448 | 200 | 228 | 20 | 44,6 % |
| A2 | 15.997 | 752 | 203 | 521 | 28 | 27,0 % |
| B1 | 33.439 | 1.137 | 187 | 908 | 42 | 16,4 % |
| B2 | 15.792 | 818 | 201 | 566 | 51 | 24,6 % |

Con las posiciones sin puntuación (R001) y los lemas escritos con letras. Con la puntuación (opción A): A1 15.802
posiciones, 176 normales (39,3 %); A2 18.967, 155 (20,6 %); B1 38.949, 161 (14,2 %); B2 18.155, 165 (20,2 %).

Es lo que pedía DOUBT-PER-LEVEL-ANALYSIS de FEAT-LREC (José marcó B), sin mover números: cumple [F-HALL-R010](#F-HALL-R010),
las sub-métricas no se promedian. José decidió el 30/9 que quede como detalle y no como puntaje
([DOUBT-PUNTAJE-POR-NIVEL](#DOUBT-PUNTAJE-POR-NIVEL)): se publica dentro del diagnóstico de curso, nunca como clave de
puntaje del nivel. El semáforo lo espera para usar la recurrencia en el verde ([F-SEMAF-R002](#F-SEMAF-R002)).

</details>

<a id="F-RECUR-R005"></a>
### Rule[F-RECUR-R005] - Cada palabra fuera de rango es un hallazgo del curso
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando el puntaje del curso es menor que 1, cada lema poco visto o sobre-expuesto deja un hallazgo en el curso
> ([F-HALL-R001](#F-HALL-R001)): gravedad *baja*, resolución *sólo ordena*, costo *instantáneo*. La evidencia da el
> [lema](glossary:Lema), sus veces, cada cuántas palabras vuelve contra la meta (más de 50 y hasta 1.000) y su hueco más largo.

Son 1.583 hallazgos: 1.534 poco vistos y 49 sobre-expuestos (1.612 con la puntuación en las posiciones, 1.616 el
30/9). Ejemplo: «vegetable: 10 veces, vuelve cada 7.765 palabras (meta: más de 50 y hasta 1.000); su hueco más largo,
23.825, está en B1». La identidad es el lema ([F-HALL-R014](#F-HALL-R014)).

<a id="F-RECUR-R006"></a>
### Rule[F-RECUR-R006] - La ficha la describe con su nombre de siempre
**Severity**: major | **Validation**: AUTO_VALIDATED

> `lemma-recurrence` figura en `get analyzers` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Las palabras vuelven
> a intervalos sanos?»; lee las oraciones de todo el curso en orden; familia vocabulario; evalúa el curso; meta: que
> cada palabra vuelva cada más de 50 y hasta 1.000 palabras; resolución sólo ordena; costo instantáneo.

## Contexto

La propuesta la muestra como «Recurrencia · sólo de curso · 8,0» y pide completar la vista vacía. El 8 % tiene una razón
que se ve recién con los datos: contando la puntuación, 1.000 palabras eran unos 123 ejercicios, así que una palabra
repartida por todo el curso necesitaba unas 92 apariciones para ser normal, y «movie», con 90, quedaba afuera por 5
palabras de promedio. Sin la puntuación (R001) 1.000 palabras son unos 144 ejercicios, alcanzan unas 79 apariciones y
«movie» vuelve cada 855: es normal. Las palabras de A1, que se usan en todo el curso, eran normales sólo en el 15,6 %
(76 de 486, contando la puntuación). Dentro de cada nivel la foto cambia: A1 44,6 %.

Lo que José dejó abierto en FEAT-LREC sigue abierto allá; este requirement no cambia umbrales ni puntaje, y publica lo
necesario para decidirlo con datos.

**El examen.** No tiene casos en el 077. Se prueba con los números: `analyze` da 0,09, 74,1 % y 4.036 tareas (0,08 y
73,9 % con la puntuación en las posiciones), y la suma de normales, poco vistas y sobre-expuestas da 1.743 (1.750
hasta el 30/9) ([F-LREC-R009](#F-LREC-R009)). **Costo**: segundos.

**Lo que no es una palabra (1/10).** En la vista, las primeras marcas de Recurrencia eran «'», «-» y «3rd». Desde el 1/10
sólo entra un lema escrito con letras (R001): el puntaje sigue en 0,08 y el vocabulario en 73,86 % (opción A, en
`ronda1/integracion`). Esta rama, `ronda1/recur-posiciones`, cuenta además sólo las palabras como posiciones (78.162
en vez de 91.873): el puntaje pasa a 0,09 y el vocabulario del curso a 74,00 % en la corrida de hoy (73,86 % antes), o
a 74,07 % sobre los puntajes publicados del 29/9 que usan las pruebas (73,92 % antes). Lo decide José:
[DOUBT-POSICIONES-SIN-PUNTUACION](#DOUBT-POSICIONES-SIN-PUNTUACION).

**El lematizador compartido** (anotado, no se arregla en esta ronda) casi no la toca: con las 17 formas mal leídas
corregidas («hated» → hate, «drunk» → drink) vuelven 1.744 lemas, 134 normales, y el puntaje sigue en 0,08.

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
        action: "Cada nivel trae su recurrencia como detalle; el puntaje y el vocabulario son los de R001 y las 4.036 tareas quedan iguales"
        gate: [F-RECUR-R002, F-RECUR-R004]
        result: success
```

## Open Questions

<a id="DOUBT-POSICIONES-SIN-PUNTUACION"></a>
### Doubt[DOUBT-POSICIONES-SIN-PUNTUACION] - ¿La puntuación ocupa una posición al medir cada cuántas palabras vuelve una?
**Status**: OPEN

- [ ] A: sí, como hasta hoy ([F-LREC-R001](#F-LREC-R001)): 91.873 posiciones, puntaje 0,08 (131 · 1.566 · 46 de
  1.743), vocabulario del curso 73,86 %, 1.612 hallazgos; «movie» vuelve cada 1.005 palabras y es poco vista.
- [ ] B: no, sólo las palabras del texto, con letras o cifras: 78.162 posiciones, puntaje 0,09 (160 · 1.534 · 49 de
  1.743), vocabulario del curso 74,00 % (los niveles no cambian), 1.583 hallazgos; «movie» vuelve cada 855 y es normal.

Un intervalo mide «cada cuántas palabras vuelve»: con la puntuación, un punto o una coma cuentan como palabras que el
alumno no lee. B es más fiel a esa definición, pero mueve el número del curso (+0,14 puntos de vocabulario), así que lo
decide José. El código de B está en `ronda1/recur-posiciones`; `ronda1/integracion` queda con A.

<a id="DOUBT-PUNTAJE-POR-NIVEL"></a>
### Doubt[DOUBT-PUNTAJE-POR-NIVEL] - ¿La recurrencia dentro del nivel pasa a ser el puntaje de ese nivel?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): no; se publica como detalle (R004) y ningún número cambia.
- [ ] B: sí, como marcaste en FEAT-LREC: el curso sigue en 73,9 %, pero bajan los niveles.

**Answer**: **A** (José, 30/9): «en cada nivel es detalle, no puntaje». Con B, el vocabulario de cada nivel pasaba de A1
96,5 a 88,4, A2 94,2 a 83,6, B1 79,5 a 70,1 y B2 63,5 a 57,3. [F-RECUR-R004](#F-RECUR-R004) queda validada.

<a id="DOUBT-UNA-SOLA-VEZ"></a>
### Doubt[DOUBT-UNA-SOLA-VEZ] - ¿Los 1.041 lemas que aparecen una sola vez cuentan como poco vistos?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): no, como hoy ([F-LREC-R006](#F-LREC-R006)); ya los cuenta `lemma-count` como poco vistos.
- [ ] B: sí, como te inclinabas en FEAT-LREC: el puntaje baja a 0,05 y el vocabulario a 73,5 %.

**Answer**: **A** (José, 30/9): «no cuenta las palabras vistas una sola vez». Una palabra vista una vez ya es hallazgo de
`lemma-count` ([F-POCOV-R003](#F-POCOV-R003)).

## References

- **FEAT-LREC** — Posiciones, umbrales, estadísticas por lema y puntaje; sus dudas abiertas: R001, R003, R004, R005.
- **FEAT-HALL** — Hallazgo, ficha, un solo número publicado y lo que no cambia: R001, R002, R003, R005, R006.
- **FEAT-POCOV, FEAT-SEMAF** — Las palabras vistas una vez; el verde por recurrencia: dudas, R004.
