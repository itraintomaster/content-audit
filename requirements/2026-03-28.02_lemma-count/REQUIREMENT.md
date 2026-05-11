---
feature:
  id: FEAT-LCOUNT
  code: F-LCOUNT
  name: Analisis de Conteo de Repeticiones de Lemas Content-Word
  priority: high
---

# Analisis de Conteo de Repeticiones de Lemas Content-Word

## TL;DR

**Que**: Mide, para cada lema content-word que aparece al menos una vez en
el curso, en cuantas oraciones distintas se repite, y resume la
exposicion por nivel CEFR y a nivel del curso completo.

**Por que**: Una vez que el curso introduce un lema, el aprendizaje
requiere que se repita varias veces en oraciones distintas. Sin esa
repeticion, la exposicion es nominal y el lema no se consolida.

## Reglas de Negocio

Las reglas se organizan en cuatro grupos:

- **Grupo A - Universo de medicion (R001-R003)**: que lemas se evaluan y
  como se identifica cada uno.
- **Grupo B - Conteo y puntuacion por lema (R004-R008)**: como se cuenta
  la repeticion y como se traduce a score.
- **Grupo C - Asignacion de nivel CEFR y agregacion (R009-R014)**: como
  se asigna un nivel CEFR a cada lema y como se agregan los scores hacia
  el nivel y el curso.
- **Grupo D - Reporte y configuracion (R015-R019)**: que observa el
  usuario en el informe y que parametros son configurables.

---

### Grupo A - Universo de medicion

<a id="F-LCOUNT-R001"></a>
### Rule[F-LCOUNT-R001] - Solo se evaluan lemas content-word
**Severity**: high | **Validation**: VALIDATED

> Solo participan en el analisis los lemas cuya parte de la oracion es
> sustantivo (NOUN), verbo (VERB), adjetivo (ADJ) o adverbio (ADV).

<details><summary>Detalle</summary>

Los lemas con cualquier otra etiqueta POS quedan fuera del universo de
medicion: pronombres, articulos, preposiciones, conjunciones, nombres
propios (PROPN), numeros (NUM), simbolos, signos de puntuacion y
auxiliares no se cuentan ni aparecen en el reporte.

La justificacion es pedagogica: las palabras content-word son las
portadoras de significado lexico que el estudiante necesita consolidar.
Las funcionales tienen frecuencias tan altas y tan dependientes de la
estructura del idioma que medir su repeticion no aporta informacion util.
Los nombres propios y numeros se excluyen porque su aparicion depende del
tema concreto de cada oracion y no representan vocabulario generalizable.

**Error**: "Token sin etiqueta POS en el quiz {quizId}: el procesamiento
linguistico previo no asigno parte de la oracion"

</details>

<a id="F-LCOUNT-R002"></a>
### Rule[F-LCOUNT-R002] - El lema se identifica por su forma base mas su POS
**Severity**: high | **Validation**: VALIDATED

> Dos apariciones se consideran del mismo lema si y solo si comparten la
> forma lematizada y la etiqueta POS. "run" como verbo y "run" como
> sustantivo son lemas distintos.

<details><summary>Detalle</summary>

La lematizacion convierte las formas flexionadas a la forma base: "ran",
"runs" y "running" se cuentan como apariciones del verbo "run". La forma
flexionada original no se utiliza para el conteo.

Cuando el mismo lema aparece con dos POS distintos en el curso, se
reportan como dos lemas independientes con sus propios conteos y scores.

**Error**: N/A (esta regla describe la unidad de identificacion)

</details>

<a id="F-LCOUNT-R003"></a>
### Rule[F-LCOUNT-R003] - Solo entran al analisis los lemas con al menos una aparicion
**Severity**: high | **Validation**: VALIDATED

> Solo se evaluan lemas content-word que aparecen en al menos una oracion
> del curso. La ausencia total (count == 0) no se mide aqui; queda
> cubierta por la feature de ausencia de lemas (FEAT-LABS).

<details><summary>Detalle</summary>

Esta separacion es deliberada. La pregunta funcional que responde esta
feature es "cuando un lema entro al curso, se repitio lo suficiente?".
La pregunta "que lemas esperados nunca aparecieron?" es funcionalmente
distinta y se responde en otra feature. Mantenerlas separadas evita
duplicar resultados y permite que cada feature tenga umbrales,
agregaciones y recomendaciones propias adecuadas a su pregunta.

Como consecuencia, el universo de lemas reportados por esta feature se
construye exclusivamente a partir del contenido real del curso, no de un
catalogo externo de lemas esperados.

**Error**: N/A (regla de delimitacion de scope)

</details>

---

### Grupo B - Conteo y puntuacion por lema

<a id="F-LCOUNT-R004"></a>
### Rule[F-LCOUNT-R004] - El conteo es la cantidad de oraciones distintas que contienen el lema
**Severity**: high | **Validation**: VALIDATED

> Para cada lema, `count` es el numero de oraciones distintas del curso
> en las que aparece al menos una vez. Si un lema aparece varias veces
> dentro de la misma oracion, esa oracion suma uno, no varios.

<details><summary>Detalle</summary>

Ejemplos:

| Situacion | count |
|-----------|-------|
| El lema "run" aparece en 6 oraciones distintas | 6 |
| El lema "run" aparece 3 veces en una misma oracion y en 2 oraciones mas | 3 |
| El lema "run" aparece una sola vez en una sola oracion | 1 |

La unidad "oracion distinta" se refiere a la oracion evaluable producida
por el procesamiento linguistico previo. Repetir el lema dentro de la
misma oracion no aumenta el count porque, desde el punto de vista del
estudiante, esa oracion es un unico contexto de exposicion.

**Error**: "Error al contar oraciones para el lema {lema} ({pos}):
{detalle}"

</details>

<a id="F-LCOUNT-R005"></a>
### Rule[F-LCOUNT-R005] - El conteo abarca todo el curso
**Severity**: high | **Validation**: VALIDATED

> Las oraciones que aportan al `count` de un lema provienen de cualquier
> parte del curso, sin importar el nivel CEFR donde se ubique cada
> oracion ni el knowledge en el que viva.

<details><summary>Detalle</summary>

Un lema asignado a A1 que aparece en oraciones de A1, A2 y B1 acumula el
total de esas tres fuentes en su `count`. Esto refleja que el aprendizaje
de un lema es acumulativo: cada nueva aparicion en cualquier punto del
curso refuerza el aprendizaje, independientemente del nivel donde se
ubique la oracion.

Esta regla aplica al numerador del `count`. La asignacion del nivel CEFR
del lema (cual nivel "responde" por ese lema en el reporte) se trata por
separado en el Grupo C.

**Error**: N/A (regla de alcance del conteo)

</details>

<a id="F-LCOUNT-R006"></a>
### Rule[F-LCOUNT-R006] - El score por lema es lineal hasta el umbral N
**Severity**: high | **Validation**: VALIDATED

> El score de cada lema es `min(count / N, 1.0)`, donde N es el umbral
> minimo de repeticion configurado. Por debajo de N el score crece en
> forma proporcional al count; al alcanzar o superar N el score queda
> fijo en 1.0.

<details><summary>Detalle</summary>

Ejemplos con N = 4 (default):

| count | score |
|-------|-------|
| 1 | 0.25 |
| 2 | 0.50 |
| 3 | 0.75 |
| 4 | 1.0 |
| 10 | 1.0 |
| 100 | 1.0 |

No existe penalizacion por sobre-exposicion en esta feature. La pregunta
"un lema se repite demasiado?" no es objeto de este analizador; corre por
otros analisis estadisticos del corpus.

**Error**: "Score fuera de rango [0.0, 1.0] calculado para el lema
{lema} ({pos}): {score}"

</details>

<a id="F-LCOUNT-R007"></a>
### Rule[F-LCOUNT-R007] - Si N no esta configurado, se aplica el default 4
**Severity**: medium | **Validation**: VALIDATED

> Cuando la configuracion del analisis no provee un valor para N (campo
> ausente), el analisis usa N = 4 sin emitir error y se ejecuta
> normalmente. El reporte sigue exponiendo el valor utilizado (ver
> [F-LCOUNT-R016](#F-LCOUNT-R016)).

<details><summary>Detalle</summary>

La eleccion de 4 como default sigue el criterio pedagogico de "minima
exposicion suficiente para retencion" usado historicamente en el sistema
de origen. El default queda explicito para que sea reproducible y para
que el reporte pueda mostrarlo siempre, incluso si el usuario nunca toco
la configuracion.

"No configurado" significa que el campo de N esta ausente o no
proporcionado. Si el campo esta presente pero con un valor invalido, ese
caso lo cubre [F-LCOUNT-R008](#F-LCOUNT-R008) y no aplica el default.

**Error**: N/A (regla de valor por defecto)

</details>

<a id="F-LCOUNT-R008"></a>
### Rule[F-LCOUNT-R008] - Si N esta configurado y es invalido, falla al cargar la configuracion
**Severity**: high | **Validation**: VALIDATED

> Cuando la configuracion provee un valor para N que no es un entero
> positivo (cero, negativo, decimal, no parseable, vacio), la **carga de
> la configuracion falla** con un mensaje explicito y el analisis no se
> ejecuta. No hay fallback silencioso al default ni ejecucion parcial.

<details><summary>Detalle</summary>

Casos cubiertos:

| Valor configurado | Resultado |
|-------------------|-----------|
| 6 | Carga OK, analisis ejecuta con N = 6 |
| 1 | Carga OK, analisis ejecuta con N = 1 |
| 0 | Falla al cargar la configuracion |
| -3 | Falla al cargar la configuracion |
| 1.5 | Falla al cargar la configuracion |
| "abc" | Falla al cargar la configuracion |
| (campo ausente) | Carga OK, aplica default 4 (ver [F-LCOUNT-R007](#F-LCOUNT-R007)) |

El fallo debe ser observable por el usuario: un mensaje de configuracion
invalida que indique el valor recibido y la condicion esperada (entero
positivo). De este modo, el usuario puede corregirlo y reintentar; el
sistema nunca interpreta creativamente un valor invalido ni lo ignora en
silencio.

**Error**: "Umbral N invalido en la configuracion: {valor}. Debe ser un
entero positivo."

</details>

---

### Grupo C - Asignacion de nivel CEFR y agregacion

<a id="F-LCOUNT-R009"></a>
### Rule[F-LCOUNT-R009] - El nivel CEFR del lema se toma del catalogo EVP cuando esta disponible
**Severity**: high | **Validation**: VALIDATED

> Si el lema (con su POS) figura en el catalogo EVP, se le asigna el
> nivel CEFR que indique el EVP.

<details><summary>Detalle</summary>

El EVP (English Vocabulary Profile) es la fuente preferente porque
representa el nivel pedagogico esperado del lema, no el nivel inferido
por un modelo. Se compara por el par (lema, POS): "run" como verbo y
"run" como sustantivo se buscan como entradas distintas.

**Error**: N/A (regla de procedencia de datos)

</details>

<a id="F-LCOUNT-R010"></a>
### Rule[F-LCOUNT-R010] - Si el lema no esta en EVP, se intenta el nivel CEFR del procesamiento linguistico (solo si esta disponible)
**Severity**: high | **Validation**: VALIDATED

> Cuando el catalogo EVP no contiene el lema, **y solo si el procesamiento
> linguistico previo provee un nivel CEFR para ese lema**, se asigna ese
> nivel. Esta regla aplica como fallback condicional: si el procesamiento
> linguistico no entrega CEFR para el lema, esta regla no se activa y el
> lema cae a [F-LCOUNT-R011](#F-LCOUNT-R011) (grupo "no asignado"). La
> ausencia de CEFR en el procesamiento linguistico no es un error: es
> simplemente la condicion que dispara R011.

<details><summary>Detalle</summary>

Esta es la fuente de respaldo cuando el EVP no cubre el lema. La cadena
completa de asignacion es:

1. EVP lo cataloga -> usa el nivel del EVP (R009).
2. EVP no lo cataloga **y** el procesamiento linguistico aporta CEFR ->
   usa el CEFR del procesamiento linguistico (R010).
3. EVP no lo cataloga **y** el procesamiento linguistico no aporta CEFR
   -> grupo "no asignado" (R011).

La disponibilidad efectiva de informacion CEFR proveniente del
procesamiento linguistico depende del tokenizer concreto. Esta regla no
exige que el procesamiento linguistico siempre entregue CEFR; solo
describe que comportamiento aplicar cuando lo entrega. Si el tokenizer
nunca entrega CEFR, la regla queda como contrato latente: ningun lema
fuera de EVP se asigna por esta via y todos caen al grupo "no asignado",
lo cual sigue siendo el comportamiento correcto.

Por consiguiente, el caso "lema fuera de EVP y sin CEFR del NLP" debe
fluir directamente a R011 sin emitir advertencias ni errores.

**Error**: N/A (regla de procedencia condicional de datos)

</details>

<a id="F-LCOUNT-R011"></a>
### Rule[F-LCOUNT-R011] - Si ninguna fuente provee nivel CEFR, el lema queda en el grupo "no asignado"
**Severity**: high | **Validation**: VALIDATED

> Cuando ni el EVP ni el procesamiento linguistico aportan un nivel CEFR
> para el lema, el lema se ubica en un grupo "no asignado". Este grupo
> aparece en el reporte de manera informativa pero no participa del score
> agregado del curso.

<details><summary>Detalle</summary>

El grupo "no asignado" existe para que ningun lema content-word presente
en el curso quede invisible para el creador de contenido. El usuario lo
ve, conoce su count, y puede decidir si lo agrega manualmente al curso
en el nivel adecuado o si lo deja como esta.

La razon de excluirlo del score del curso es que asignarle arbitrariamente
un nivel falsearia la metrica: no sabemos si esos lemas pertenecen a A1 o
a C2, y mezclarlos sesgaria el promedio.

**Error**: N/A (regla de clasificacion residual)

</details>

<a id="F-LCOUNT-R012"></a>
### Rule[F-LCOUNT-R012] - El score de un nivel CEFR es el promedio simple de los scores de sus lemas
**Severity**: high | **Validation**: VALIDATED

> Para cada nivel CEFR, su score es la media aritmetica de los scores
> individuales de los lemas asignados a ese nivel. Cada lema pesa lo
> mismo, sin importar su count ni su frecuencia en el corpus.

<details><summary>Detalle</summary>

Ejemplo: si A1 contiene tres lemas con scores 0.25, 1.0 y 1.0, el score
de A1 es (0.25 + 1.0 + 1.0) / 3 = 0.75.

Un nivel sin lemas asignados (no hay lemas content-word del curso cuya
asignacion CEFR caiga en ese nivel) queda sin score y no participa en el
calculo del curso.

**Error**: "No se pudo calcular el score del nivel CEFR {nivel}: {detalle}"

</details>

<a id="F-LCOUNT-R013"></a>
### Rule[F-LCOUNT-R013] - El score del curso es el promedio simple de los scores de los niveles CEFR
**Severity**: high | **Validation**: VALIDATED

> El score del curso es la media aritmetica de los scores de los niveles
> CEFR con score. Todos los niveles pesan igual, sin importar cuantos
> lemas contenga cada nivel.

<details><summary>Detalle</summary>

Ejemplo: si A1 = 0.75 y A2 = 1.0, el score del curso es 0.875, aunque A1
tenga 800 lemas y A2 tenga 50.

Esta agregacion fija los niveles como unidad de comparacion. Un nivel con
muchos lemas mal expuestos pesa lo mismo que un nivel con pocos lemas
bien expuestos: la idea es no permitir que un nivel "grande" oculte
problemas en niveles "chicos" ni viceversa.

**Error**: "No se pudo calcular el score del curso: ningun nivel CEFR
tiene score asignable"

</details>

<a id="F-LCOUNT-R014"></a>
### Rule[F-LCOUNT-R014] - El grupo "no asignado" no entra al score del curso
**Severity**: high | **Validation**: VALIDATED

> El score del grupo "no asignado" (si se calcula con fines informativos)
> nunca se usa como uno de los promediandos del score del curso. El
> agregado del curso solo combina los niveles CEFR canonicos (A1, A2, B1,
> B2, C1, C2) con score.

<details><summary>Detalle</summary>

Esta regla refuerza [F-LCOUNT-R011](#F-LCOUNT-R011) y aisla el efecto del
grupo "no asignado" sobre la metrica global. El reporte del grupo "no
asignado" sigue mostrando los lemas y sus counts; lo unico que se evita
es contaminarlo con el score del curso.

**Error**: N/A (regla de delimitacion del agregado)

</details>

---

### Grupo D - Reporte y configuracion

<a id="F-LCOUNT-R015"></a>
### Rule[F-LCOUNT-R015] - El reporte expone el score global del curso
**Severity**: medium | **Validation**: VALIDATED

> El reporte del analisis incluye el score global del curso calculado
> segun [F-LCOUNT-R013](#F-LCOUNT-R013).

<details><summary>Detalle</summary>

Es la primera senal que recibe el usuario al consultar el analisis. Si
no se pudo calcular (no hay niveles CEFR con score), el reporte indica
explicitamente esa condicion en lugar de devolver un valor arbitrario.

**Error**: N/A (regla de contenido del reporte)

</details>

<a id="F-LCOUNT-R016"></a>
### Rule[F-LCOUNT-R016] - El reporte expone explicitamente el umbral N usado
**Severity**: medium | **Validation**: VALIDATED

> El reporte indica el valor de N con el que se calcularon los scores,
> ya sea el default o el configurado por el usuario.

<details><summary>Detalle</summary>

Sin esto, dos auditorias del mismo curso ejecutadas con N distintos
producirian reportes incomparables sin ninguna pista visible. Mostrarlo
en el reporte permite reproducir y contrastar resultados.

**Error**: N/A (regla de contenido del reporte)

</details>

<a id="F-LCOUNT-R017"></a>
### Rule[F-LCOUNT-R017] - El reporte detalla cada nivel CEFR
**Severity**: medium | **Validation**: VALIDATED

> Para cada nivel CEFR con score, el reporte incluye: el score del nivel,
> el total de lemas evaluados en ese nivel, y la lista de lemas
> sub-expuestos (score < 1.0) con su count.

<details><summary>Detalle</summary>

La lista de lemas sub-expuestos es la que el creador de contenido usa
para tomar accion: cada uno de esos lemas necesita aparecer en mas
oraciones para alcanzar la exposicion suficiente. Mostrar el count
permite priorizar (un lema con count 1 es mas urgente que uno con count
3).

Los lemas con score 1.0 (count >= N) no se listan individualmente para no
saturar el reporte; solo se cuentan dentro del total del nivel.

**Error**: N/A (regla de contenido del reporte)

</details>

<a id="F-LCOUNT-R018"></a>
### Rule[F-LCOUNT-R018] - El reporte lista los lemas del grupo "no asignado"
**Severity**: medium | **Validation**: VALIDATED

> El reporte incluye, en una seccion informativa separada, la lista de
> lemas del grupo "no asignado" con su count. Esta seccion no incluye
> score (ni por lema ni agregado) porque su agregacion no entra en el
> score del curso.

<details><summary>Detalle</summary>

El usuario ve los lemas que aparecen en el curso pero a los que ninguna
fuente pudo asignar un nivel CEFR. Puede decidir caso a caso: agregar
mas oraciones con esos lemas, ignorarlos, o catalogarlos manualmente en
otra parte del sistema.

**Error**: N/A (regla de contenido del reporte)

</details>

<a id="F-LCOUNT-R019"></a>
### Rule[F-LCOUNT-R019] - El analizador se identifica con el nombre "lemma-count"
**Severity**: low | **Validation**: VALIDATED

> En el informe de auditoria, los resultados de este analisis se
> identifican con el nombre `lemma-count`.

<details><summary>Detalle</summary>

Este nombre permite distinguir el analisis de otros analizadores del
sistema (por ejemplo, `lemma-recurrence`, `lemma-absence`,
`sentence-length`).

**Error**: N/A (regla de identificacion)

</details>

---

## Contexto

El sistema ContentAudit audita cursos de idiomas. Una de las dimensiones
pedagogicas a evaluar es el **compromiso de aprendizaje**: cuando el
curso introduce un lema, deberia repetirlo varias veces para que el
estudiante lo retenga. Una unica aparicion no basta; lo que importa es
la cantidad de contextos distintos en los que el lema vuelve a aparecer.

Esta feature reemplaza un analizador legacy del mismo nombre que existia
en el sistema original pero estaba desactivado. El rediseno cambia tres
aspectos clave respecto del legacy:

1. **El alcance se restringe a lemas content-word que ya aparecen en el
   curso.** La ausencia total queda cubierta por la feature de ausencia
   de lemas (FEAT-LABS), evitando que dos analizadores reporten el mismo
   problema con criterios distintos.
2. **La metrica es "oraciones distintas", no "tokens".** Repetir un
   lema dentro de una misma oracion no aumenta el count, porque desde el
   punto de vista del estudiante esa oracion sigue siendo un unico
   contexto de exposicion.
3. **No se mide sobre-exposicion.** El score crece linealmente hasta
   alcanzar el umbral y se mantiene en 1.0 a partir de ahi. La pregunta
   "un lema se repite demasiado?" se responde con otros analisis
   estadisticos del corpus.

La asignacion del nivel CEFR del lema (necesaria para agrupar y reportar)
sigue una cadena de fuentes con prioridad: primero el catalogo EVP,
despues el procesamiento linguistico, y como ultimo recurso el grupo "no
asignado" que se reporta pero no contamina la metrica del curso.

## Alcance

**Dentro de scope:**

- Conteo por oraciones distintas de lemas content-word que aparecen al
  menos una vez.
- Score por lema, score por nivel CEFR y score del curso.
- Asignacion del nivel CEFR del lema con la cadena EVP -> procesamiento
  linguistico -> "no asignado".
- Reporte detallado por nivel CEFR y listado del grupo "no asignado".
- Configurabilidad del umbral N (default 4).

**Fuera de scope:**

- **Ausencia total** (count == 0): cubierto por FEAT-LABS.
- **Sobre-exposicion**: cubierto por analisis estadisticos de corpus
  existentes (no objeto de este analizador).
- **Espaciamiento o distribucion temporal de las repeticiones**: queda
  pendiente para otra feature futura. Esta feature no distingue si el
  count se acumulo en oraciones contiguas o distribuido a lo largo del
  curso.
- **Diferenciar Knowledge vs Quiz** como contextos distintos: la unidad
  de conteo es la oracion; la categoria del contenedor es irrelevante
  para esta feature.
- **Lemas no content-word** (PROPN, NUM, simbolos, function words):
  excluidos del universo en [F-LCOUNT-R001](#F-LCOUNT-R001).

## User Journeys

### Journey[F-LCOUNT-J001] - Consultar la exposicion de los lemas content-word del curso

**Validation**: VALIDATED

```yaml
journeys:
  - id: F-LCOUNT-J001
    name: Consultar la exposicion de lemas content-word
    flow:
      - id: ejecutar_auditoria
        action: "El usuario ejecuta la auditoria del curso"
        then: filtrar_universo
      - id: filtrar_universo
        action: "El sistema filtra los lemas content-word con al menos una aparicion"
        gate: [F-LCOUNT-R001, F-LCOUNT-R003]
        then: contar_oraciones
      - id: contar_oraciones
        action: "El sistema cuenta las oraciones distintas que contienen cada lema"
        gate: [F-LCOUNT-R004, F-LCOUNT-R005]
        then: calcular_score_lema
      - id: calcular_score_lema
        action: "El sistema calcula el score de cada lema con N configurado"
        gate: [F-LCOUNT-R006, F-LCOUNT-R007, F-LCOUNT-R008]
        then: asignar_nivel
      - id: asignar_nivel
        action: "El sistema asigna un nivel CEFR a cada lema"
        outcomes:
          - when: "El lema esta en el catalogo EVP"
            then: agregar_por_nivel
          - when: "El lema no esta en EVP pero el procesamiento linguistico aporta CEFR"
            then: agregar_por_nivel
          - when: "Ni EVP ni el procesamiento linguistico aportan nivel"
            then: agrupar_no_asignado
      - id: agregar_por_nivel
        action: "El sistema promedia los scores de los lemas por nivel CEFR y luego promedia los niveles para obtener el score del curso"
        gate: [F-LCOUNT-R009, F-LCOUNT-R010, F-LCOUNT-R012, F-LCOUNT-R013, F-LCOUNT-R014]
        then: emitir_reporte
      - id: agrupar_no_asignado
        action: "El sistema ubica el lema en el grupo no asignado y omite ese grupo del agregado del curso"
        gate: [F-LCOUNT-R011, F-LCOUNT-R014]
        then: emitir_reporte
      - id: emitir_reporte
        action: "El reporte expone el score global, el N usado, el detalle por nivel CEFR y el listado del grupo no asignado"
        gate: [F-LCOUNT-R015, F-LCOUNT-R016, F-LCOUNT-R017, F-LCOUNT-R018, F-LCOUNT-R019]
        result: success
```

---

## References

- **FEAT-LABS** (Analisis de Ausencia de Lemas) — Cubre el caso
  count == 0 que esta feature deja explicitamente fuera de scope.
  Citado por [F-LCOUNT-R003](#F-LCOUNT-R003).
- **FEAT-LREC** (Analisis de Recurrencia de Lemas) — Mide la
  distribucion espacial de los lemas (intervalos entre apariciones), no
  el conteo. Es complementaria pero independiente: no se cita desde
  ninguna regla de esta feature.
