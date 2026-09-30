---
feature:
  id: FEAT-NOEMP
  code: F-NOEMP
  name: No empeorar - la compuerta de cada cambio y de cada tanda
  priority: critical
---

# No empeorar: la compuerta de cada cambio y de cada tanda

## TL;DR

**Qué**: «No empeorar» pasa a ser una compuerta medible en dos niveles: ningún cambio suma un error, una palabra roja ni
un audio distinto sin declararlo; y al cerrar una tanda ningún conteo de errores sube y ningún número de vocabulario baja
más de medio punto.

**Por qué**: Los promedios no lo ven. Aplicadas a una copia, las cuatro propuestas del 077 para el tema de las verduras
meten «grandmother» (A2) en un ejercicio de A1, y ningún puntaje de nivel ni de curso se mueve; sólo cambia la cuenta de
ejercicios con palabras marcadas: 351 → 352 en A1.

## Recorrido de uso (estado actual)

Cómo quien corrige intenta hoy saber si un cambio o un conjunto de cambios empeora el curso:

1. Analiza antes de corregir → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible.
2. Mira el impacto de una propuesta → la vista previa de impacto que acompaña a cada propuesta (`ImpactPreview`,
   [F-PIPRE-R001](#F-PIPRE-R001)). ✓ Disponible, con antes, después y diferencia por analizador, pero ⚠️ sin ningún
   umbral: no dice si algo empeoró.
3. Mira varias propuestas juntas → `GetConsolidatedCommand.getConsolidated(coursePath, format)`. ✓ Disponible, con las
   tres fotos por nodo ([F-CDIFF-R007](#F-CDIFF-R007)); ⚠️ tampoco decide nada.
4. Juzga un candidato → `AssessCandidateCommand.assessCandidate(…)`. ✓ Disponible, con el vocabulario del nivel entre sus
   criterios ([F-QICOR-R004](#F-QICOR-R004), criterio 2), pero ⚠️ reprobarlo no impide entregar.

**Punto de fricción**: pasos 2 a 4. Hay números de antes y de después, pero ninguna regla dice cuáles tienen que quedar
iguales o mejor, y los que existen son promedios que no se mueven con un error nuevo.

**Cambio mínimo necesario**: una compuerta con reglas fijas, en dos niveles: la mesa la aplica a cada cambio contra su
ejercicio ([F-MESA-R006](#F-MESA-R006)), y la tanda, sobre la comparación de su proyección con la línea base que publica el
paquete F ([F-TANDA-R006](#F-TANDA-R006)). Dice qué frena, por qué y qué vuelve.

## Reglas de Negocio

Grupos: **A**, por cambio (R001–R004), que aplica la mesa; **B**, por tanda (R005–R007), que aplica la tanda al curso
recalculado; **C**, qué pasa si frena y qué se garantiza (R008–R010). **Palabra roja** es la del semáforo del tema
([F-SEMAF-R005](#F-SEMAF-R005)): la que `lemma-absence` marca para el nivel del ejercicio, de otro nivel o fuera de
catálogo, salvo las que el tema enseña.

### Grupo A — Por cambio

<a id="F-NOEMP-R001"></a>
### Rule[F-NOEMP-R001] - Un cambio no suma errores
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Después de un cambio, el ejercicio —o el tema, si el cambio es del tema— no tiene ningún hallazgo que cuente como
> error que antes no tuviera, comparados por identidad ([F-HALL-R014](#F-HALL-R014)) entre los analizadores que lo
> evaluaron en las dos puntas.

Es la mitad de «acepta si no empeora nada» de la propuesta. Un hallazgo que sólo ordena no cuenta acá; el desacuerdo de
los jueces sobre un candidato va a la bandeja ([F-MESA-R008](#F-MESA-R008)).

<a id="F-NOEMP-R002"></a>
### Rule[F-NOEMP-R002] - Un cambio no suma palabras rojas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Toda palabra roja del ejercicio después del cambio ya era roja antes. En una opción múltiple vale además para cada
> opción nueva puesta en el hueco. Una palabra roja nueva rechaza el cambio y el motivo la nombra con su nivel.

Mantiene la [eval de no-regresión léxica](glossary:Eval de no-regresión léxica) de [F-LASAG-R007](#F-LASAG-R007)
(criterio 7) y el criterio 2 de [F-QICOR-R004](#F-QICOR-R004), con la misma medición del análisis
([F-CLEX-R006](#F-CLEX-R006)) y la excepción del tema que enseña la palabra ([F-SEMAF-R004](#F-SEMAF-R004)). Acá veta:
es la regla que frena «grandmother». Es más estricta que `off-level-words`, que cuenta como error sólo desde dos niveles
de distancia ([F-NIVFR-R002](#F-NIVFR-R002)): corregir no puede sumar ni una palabra de un nivel más.

**Criterio** (medido): en la copia con las cuatro propuestas del 077 para *Afirmativas regulares*, «It ___ (come) every
year.» → «My grandmother ___ (come) every year.» suma *grandmother* (A2) y vuelve; con «Winter ___ (come) every year.»
no suma ninguna y queda.

<a id="F-NOEMP-R003"></a>
### Rule[F-NOEMP-R003] - El largo se mide y se declara, y cuenta en la tanda
**Severity**: major | **Validation**: AUTO_VALIDATED

> Un cambio que deja el ejercicio fuera del rango de largo de su nivel no se rechaza por eso: la decisión declara el largo
> medido y el rango, y el cambio cuenta en el largo del nivel al cerrar la tanda ([F-NOEMP-R006](#F-NOEMP-R006)). Un
> hallazgo nuevo de largo nunca devuelve un cambio, ni en la mesa ni al cerrar la tanda.

Mantiene [F-QICOR-R011](#F-QICOR-R011) y la decisión de José del 11/8: «es más importante que el ejercicio esté bien
escrito que si entra o no en el rango». La última oración es la reconciliación con el paquete F del 30/9: un cambio vuelve
por palabras de otro nivel y por errores, nunca por el largo ([DOUBT-NO-EMPEORAR](#DOUBT-NO-EMPEORAR)).

<a id="F-NOEMP-R004"></a>
### Rule[F-NOEMP-R004] - Lo que se escucha cambia sólo declarado
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Si un cambio altera la oración que dice el audio, lleva su pedido de regrabar con el texto nuevo, y la tanda lo lista.
> Un cambio nunca altera lo que se escucha sin ese pedido, y un cambio que sólo toca la [traducción](glossary:Traducción), la pista o las
> respuestas alternativas no pide regrabar.

En el 077 cambia lo que se escucha en 979 de 2.892 ejercicios (34 %); en el ejemplo de las verduras, en los cuatro.
Regrabar es trabajo fuera de content-audit y tiene que hacerse antes de que el texto nuevo llegue al alumno
([F-DATOS-R003](#F-DATOS-R003)).

### Grupo B — Por tanda, con el curso recalculado

La tanda no compara por su cuenta: lee la comparación de su proyección que publica el paquete F
([F-METAS-R001](#F-METAS-R001), [F-METAS-R003](#F-METAS-R003)). Estas reglas dicen qué tiene que decir esa comparación
para que la tanda no empeore; son los pisos por defecto que propone F ([F-METAS-R005](#F-METAS-R005)). Hay dos puntos de
referencia: los hallazgos y los conteos se comparan con el análisis del que partió la tanda —el último publicado—, y el
vocabulario, con la línea base fijada antes de la primera tanda, que no se mueve al publicar.

<a id="F-NOEMP-R005"></a>
### Rule[F-NOEMP-R005] - Al cerrar una tanda, ningún error nuevo y ningún conteo que suba
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La tanda pasa esta regla sólo si, en su comparación con el análisis del que partió:
> 1. ningún nodo que la tanda cambió gana un hallazgo que cuente como error ni una palabra roja; un hallazgo nuevo de
>    largo no cuenta acá ([F-NOEMP-R003](#F-NOEMP-R003));
> 2. en ningún nodo sube la cantidad de ejercicios con error de ningún analizador, ni la de «algún error», ni la de
>    bloqueantes ([F-HALL-R008](#F-HALL-R008));
> 3. en ningún nivel sube la cantidad de ejercicios con palabras rojas.
>
> Hay dos excepciones, que el informe lista aparte: el audio que no coincide en un ejercicio listado para regrabar
> ([F-NOEMP-R004](#F-NOEMP-R004)) y lo que José aprobó en la bandeja sabiendo que no pasaba ([F-BANDEJA-R003](#F-BANDEJA-R003)).

La invariante 1 es la que manda y es [F-METAS-R003](#F-METAS-R003) sin el largo ([F-NOEMP-R003](#F-NOEMP-R003)); las otras
dos son los números que José lee en el tablero. Mide lo que la mesa no ve sola: un duplicado entre dos ejercicios del
tema que cambiaron, o un ejercicio que ahora contradice la miniteoría. La 3 es la cuenta que vio a «grandmother»: 351 → 352.

<a id="F-NOEMP-R006"></a>
### Rule[F-NOEMP-R006] - Al cerrar una tanda, el vocabulario baja como mucho medio punto
**Severity**: critical | **Validation**: VALIDATED

> Cada analizador de vocabulario, en cada nivel y en el curso, y el puntaje de vocabulario de cada nivel y del curso
> ([F-HALL-R009](#F-HALL-R009)), quedan como mucho 0,5 puntos por debajo de la línea base fijada antes de la primera
> tanda. La tolerancia es acumulada: la gasta la suma de todas las tandas, no cada una.

Es el piso por defecto de vocabulario de [F-METAS-R005](#F-METAS-R005) y la tolerancia de la propuesta («el vocabulario no
puede bajar más de medio punto en ningún nivel»); José confirmó el valor y que sea acumulado el 30/9
([DOUBT-NO-EMPEORAR](#DOUBT-NO-EMPEORAR)).

<details><summary>Cuánto es medio punto (medido sobre la base del 29/9)</summary>

El puntaje de largo de un nivel promedia temas, sus temas promedian ejercicios; un ejercicio fuera del rango por un
token pierde 0,2 y uno a 5 o más tokens lo pierde todo. Con el peso mediano de un ejercicio de cada nivel:

| Nivel | Temas | Ejercicios | Largo | 0,5 puntos son… | …o, por un token | En el borde del rango |
|---|---:|---:|---:|---:|---:|---:|
| A1 | 83 | 2.402 | 94,2 | 15 ejercicios fuera del todo | 73 | 412 |
| A2 | 91 | 2.411 | 96,3 | 17 | 84 | 341 |
| B1 | 147 | 4.554 | 85,4 | 27 | 136 | 868 |
| B2 | 92 | 1.920 | 69,9 | 11 | 53 | 260 |

«En el borde» son los que tienen el largo mínimo o el máximo de su nivel: una palabra más o menos los saca. Con las cuatro
propuestas de las verduras, ningún número de vocabulario de A1 ni del curso se movió un milésimo.

</details>

<a id="F-NOEMP-R007"></a>
### Rule[F-NOEMP-R007] - Nada fuera de la tanda cambia
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Los ejercicios y temas sin cambios aceptados en la tanda quedan idénticos byte a byte en el curso recalculado, y el
> curso tiene los mismos niveles, temas y ejercicios que antes.

Mantiene [F-RPRES-R003](#F-RPRES-R003) a escala de la tanda, con sus dos excepciones: el título ([F-RPRES-R004](#F-RPRES-R004))
y la miniteoría del tema ([F-MESA-R012](#F-MESA-R012)).

### Grupo C — Qué pasa si frena

<a id="F-NOEMP-R008"></a>
### Rule[F-NOEMP-R008] - Si la tanda frena, lo que empeora vuelve a la mesa y el resto espera
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Si falla [F-NOEMP-R005](#F-NOEMP-R005), [F-NOEMP-R006](#F-NOEMP-R006) o [F-NOEMP-R007](#F-NOEMP-R007), la tanda no se
> publica y la compuerta devuelve a la mesa los cambios responsables:
> 1. el de cada nodo que la comparación marca como que vuelve: donde aparece el error nuevo o la palabra roja nueva, y
>    nunca por un hallazgo nuevo de largo ([F-NOEMP-R003](#F-NOEMP-R003));
> 2. para un número fuera de su piso, los cambios que la comparación ordena por cuánto lo empeoran solos
>    ([F-METAS-R004](#F-METAS-R004)), del primero en adelante, hasta que vuelva a entrar.
>
> Un cambio devuelto gasta un intento y vuelve con el motivo; si ya no le quedan intentos, va a la bandeja como sin
> ganador. El resto de la tanda no se descarta: se recalcula sin los devueltos y vuelve a pasar la compuerta.

Es «lo que empeora se devuelve» (brief 089). Devolver no cuesta consultas nuevas: el ejercicio vuelve a su contenido de
antes, cuyos veredictos ya están ([F-EVCOST-R003](#F-EVCOST-R003)). Recalcular el curso entero tarda unos 38 segundos
(medido; 1,7 a 2,4 si ninguna oración cambió), porque hoy cambiar una oración vuelve a tokenizar el curso entero; con el
caché por oración que piden E y F, unos 5 (2,4 del análisis más 2,2 de un solo lanzamiento de spaCy, medidos el 30/9).

<a id="F-NOEMP-R009"></a>
### Rule[F-NOEMP-R009] - La compuerta es determinista y dice por qué
**Severity**: major | **Validation**: AUTO_VALIDATED

> Con el mismo antes y el mismo después, la compuerta da el mismo resultado y devuelve los mismos cambios. Cada freno
> nombra el número, el contexto, su valor antes y después, la tolerancia y los cambios que devolvió; cada cambio
> rechazado nombra la regla que no pasó y su evidencia.

<a id="F-NOEMP-R010"></a>
### Rule[F-NOEMP-R010] - Sólo se compara lo evaluado en las dos puntas
**Severity**: major | **Validation**: AUTO_VALIDATED

> Un analizador que no evaluó un ejercicio antes o después no lo cuenta ni como mejora ni como empeoramiento; la
> compuerta declara, por analizador, cuántos ejercicios de la tanda quedaron sin comparar por eso.

Mantiene [F-HALL-R008](#F-HALL-R008) (invariante 3, «algún error» como piso) y [F-QINST-R004](#F-QINST-R004): no se leen
números sobre lo no evaluado.

## Contexto

**Qué se midió** (prototipo en el scratchpad, sin modelos y sin tocar el curso). Sobre una copia del curso del 29/9 se
aplicaron las cuatro propuestas del 077 que muestra la propuesta «Corregir sin empeorar» para *Afirmativas regulares*
(A1) y se corrió `analyze` completo antes y después, sin el juez:

| Qué se miró | Antes | Después | La compuerta |
|---|---|---|---|
| Palabras rojas en el ejercicio 2 | ninguna | *grandmother* (A2) | vuelve a la mesa (R002) |
| Ejercicios de A1 con palabras marcadas | 351 | 352 | frena (R005.4) |
| Los analizadores de vocabulario de A1 (6) y del curso (7) | 94,197 · 100 · 98,021 · … | idénticos | pasa (R006) |
| Puntaje de vocabulario del curso | 73,923 | 73,923 | pasa |
| Largo de los cuatro | 5, 4, 7, 4 tokens | 6, 5, 4, 4 | dentro de 3 a 8 |
| Lo que se escucha | — | cambia en los 4 | 4 pedidos de regrabar (R004) |

Con «Winter» en lugar de «My grandmother», la compuerta deja pasar los cuatro. La lección es la del TL;DR: un promedio de
2.402 ejercicios no se entera de una palabra; por eso la compuerta cuenta por ejercicio e identidad, y la tolerancia de
medio punto queda sólo para el vocabulario, que es un promedio a propósito.

**Con el paquete F.** F publica la comparación, sus pisos y metas y el veredicto de la tanda (FEAT-METAS), la proyección
única (FEAT-PROYU) y la muestra ciega (FEAT-CIEGA). Este requirement dice qué es empeorar: en la mesa, para cada cambio
(grupo A, que F no ve), y en la tanda, qué pisos tiene que cumplir esa comparación (grupo B), que coinciden con los que
F propone por defecto. José contestó las dos preguntas con la recomendada el 30/9: la A de
[DOUBT-VUELVE-VOCABULARIO](#DOUBT-VUELVE-VOCABULARIO) —un cambio vuelve también por un hallazgo nuevo de vocabulario,
«grandmother» vuelve— y la A de [DOUBT-NO-EMPEORAR](#DOUBT-NO-EMPEORAR). Se reconcilian así: un cambio vuelve por palabras
de otro nivel y por errores, y el largo nunca devuelve un cambio ([F-NOEMP-R003](#F-NOEMP-R003)); se declara y cuenta sólo
en el piso del nivel. La palabra de otro nivel es de gravedad media y el largo, baja
([DOUBT-GRAVEDAD-EXISTENTES](#DOUBT-GRAVEDAD-EXISTENTES)). Para que las dos compuertas digan lo mismo,
[F-METAS-R003](#F-METAS-R003) tiene que exceptuar el largo como exceptúa lo que sólo ordena: queda anotado para el
integrador. Las metas de avance —menos de 5 % de ejercicios con error, 90 % en la muestra ciega— dicen adónde se quiere
llegar y no deciden.

## Alcance

- **Adentro**: qué cuenta como empeorar para un cambio y para una tanda, qué frena, qué vuelve y cómo se explica.
- **Afuera**: la línea base, la proyección única, la comparación con su veredicto, las metas y la muestra ciega (paquete
  F: FEAT-PROYU, FEAT-METAS y FEAT-CIEGA; su lugar en la bandeja es de FEAT-BANDEJA); qué marca cada analizador (paquetes
  A, B, C y D); regrabar los audios.

## User Journeys

### Journey[F-NOEMP-J001] - Cerrar una tanda contra la compuerta
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-NOEMP-J001
    name: Cerrar una tanda contra la compuerta
    flow:
      - id: recalcular
        action: "La tanda recalcula el curso entero con sus cambios aceptados, cada uno con su pedido de regrabar si cambia lo que se escucha"
        gate: [F-NOEMP-R004]
        then: comparar
      - id: comparar
        action: "La compuerta lee la comparación de la proyección con la línea base que publica el paquete F, sobre lo evaluado en las dos puntas"
        gate: [F-NOEMP-R005, F-NOEMP-R006, F-NOEMP-R007, F-NOEMP-R010]
        outcomes:
          - when: "No aparece ninguna identidad nueva, ningún conteo sube y el vocabulario queda dentro de la tolerancia"
            then: pasa
          - when: "Un cambio sumó una palabra roja o un error nuevo"
            then: devolver
          - when: "Un número de vocabulario quedó más de medio punto por debajo de la línea base"
            then: devolver
      - id: devolver
        action: "La compuerta nombra el número y devuelve a la mesa los cambios responsables, cada uno con su motivo y un intento menos"
        gate: [F-NOEMP-R008, F-NOEMP-R009]
        then: recalcular_sin
      - id: recalcular_sin
        action: "La tanda se recalcula sin los devueltos y la compuerta vuelve a comparar con el mismo resultado para el mismo curso"
        gate: [F-NOEMP-R009]
        result: failure
      - id: pasa
        action: "La tanda queda en condiciones de publicarse, con la lista de audios para regrabar"
        gate: [F-NOEMP-R005, F-NOEMP-R006]
        result: success
```

## Open Questions

<a id="DOUBT-NO-EMPEORAR"></a>
### Doubt[DOUBT-NO-EMPEORAR] - ¿Qué significa «no empeorar»?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): lo de este requirement. Por cambio: ningún error nuevo, ninguna palabra roja nueva y lo que
  se escucha sólo declarado; el largo se declara. Por tanda: ninguna identidad nueva ni ningún conteo que suba, y cada
  número de vocabulario, como mucho 0,5 puntos bajo la línea base, acumulado. Si frena, vuelve sólo lo que empeora.
- [ ] Opción B: sólo los números por analizador y nivel, con 0,5 de tolerancia también para los errores. No ve
  «grandmother»: con las cuatro propuestas, ningún número se movió.
- [ ] Opción C: la A, pero con tolerancia 0 en vocabulario. Un solo ejercicio de B2 que sale del rango por un token baja
  el largo del nivel unos 0,01 puntos y ya frena: el largo vuelve a decidir, contra lo que José decidió el 11/8.

**Answer**: **Opción A** (José, 30/9). Por cambio: ningún error nuevo, ninguna palabra roja nueva salvo las que enseña el
tema, el largo se declara y no veta (José, 11/8), y el audio que cambia lleva su pedido de regrabar. Por tanda: la
comparación de F, ningún conteo que suba, el vocabulario baja 0,5 como mucho, acumulado, y nada fuera de la tanda cambia.
José tomó también la A de [DOUBT-VUELVE-VOCABULARIO](#DOUBT-VUELVE-VOCABULARIO) (FEAT-METAS): un cambio vuelve por un
hallazgo nuevo de vocabulario. Las dos se reconcilian en [F-NOEMP-R003](#F-NOEMP-R003) y
[F-NOEMP-R005](#F-NOEMP-R005): vuelve por palabras de otro nivel y por errores; el largo nunca devuelve un cambio.

## References

- **FEAT-HALL** — Identidad, conteos y puntaje de vocabulario: R001, R005, R006, R010.
- **FEAT-LASAG, FEAT-QICOR, FEAT-CLEX** — No-regresión léxica, largo que no decide y consulta léxica: R002, R003.
- **FEAT-SEMAF, FEAT-NIVFR, FEAT-DATOS** (ronda 1, paquetes A y D) — Palabra roja, error de nivel y audio: R002, R004, R005.
- **FEAT-RPRES, FEAT-EVCOST, FEAT-QINST, FEAT-PIPRE, FEAT-CDIFF** — Lo que no se toca, reuso de veredictos, no leer lo no
  evaluado y las proyecciones de hoy: R007, R008, R010.
- **FEAT-METAS, FEAT-PROYU** (ronda 2, paquete F) — La comparación contra la línea base, sus pisos, lo que vuelve y la
  proyección única: R005, R006, R008.
- **FEAT-MESA, FEAT-TANDA** (este paquete) — Quién aplica cada nivel de la compuerta.
