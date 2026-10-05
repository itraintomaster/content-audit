---
feature:
  id: FEAT-NATUR
  code: F-NATUR
  name: Naturalidad - qué oraciones mirar primero
  priority: medium
---

# Naturalidad: qué oraciones mirar primero

## TL;DR

**Qué**: `naturalness` le pregunta a un modelo local —sin Ollama, a claude -p—, por cada oración terminada, si un nativo
la diría así, y marca las más dudosas de cada nivel para que los jueces empiecen por ahí. No cuenta errores.

**Por qué**: Juzgar el curso con los cinco jueces son unas 220 horas de a una. Medido con el modelo ya instalado, esa
cola trae el 27 % de lo que el 077 llamó antinatural o sin sentido, más de cinco veces el azar.

## Recorrido de uso (estado actual)

Cómo el operador intenta hoy elegir qué ejercicios juzgar primero:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible, pero ningún número dice qué
   tan rara es una oración: los de vocabulario miden largo, frecuencia y lemas.
2. Le pasa a un juez un conjunto de ejercicios → `AnalyzeOptions.reevaluateInstructionQuizIds`
   ([F-QINST-R018](#F-QINST-R018)). ✓ Disponible para el juez de consigna; FEAT-JEXAM lo extiende a los nuevos.
3. Arma ese conjunto con las oraciones más raras primero → ⚠️ nada lo propone: el orden es el del curso.

**Punto de fricción**: paso 3.

**Cambio mínimo necesario**: sumar `naturalness` al catálogo de FEAT-HALL, con un orden publicado y una lista de
marcados que sirva de conjunto para un juez; sin Ollama, con la misma pregunta a claude -p (FEAT-MLOCAL R007).

## Reglas de Negocio

Desde el 4/10, toda mención a `claude -p` como alternativa activa significa por omisión `codex-cli` autenticado con la sesión local
de ChatGPT; una selección explícita de otro proveedor prevalece ([F-JEXAM-R013](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R013)). Las cifras y comparaciones ya medidas con Claude se conservan como historia
y no habilitan a la versión nueva, que debe volver a rendir examen ([F-JEXAM-R014](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R014)).

<a id="F-NATUR-R001"></a>
### Rule[F-NATUR-R001] - Lee la oración terminada, y sólo si es una oración
**Severity**: critical | **Validation**: ASSUMPTION

> Por ejercicio, `naturalness` lee la oración terminada de Gramática ([F-GRAMA-R001](#F-GRAMA-R001)) con la primera
> respuesta aceptada. Un ejercicio cuya oración no tiene verbo o es una sola palabra («go - going», «hard work
> uncountable») no alcanza: no tiene número ni hallazgo, y no se le pregunta a ningún modelo.

En A1–A2 no alcanzan 75 de 4.813; en el curso, 113 de 11.287. La contraída y la larga valen igual (decisión de José,
ronda 1): la pregunta de R002 no castiga la contraída, así que la oración se lee como queda. SLOR sí la castigaba y
obligaba a leer la forma larga, y quedó descartado ([DOUBT-NATUR-METODO](#DOUBT-NATUR-METODO)). Con la lista «a / b»
dentro de una oración ya cerrada, la respuesta va en su lugar ([F-GRAMA-R001](#F-GRAMA-R001) inv. 5): se pregunta por
«He did something to help.», nunca por «He did to help.».

<a id="F-NATUR-R002"></a>
### Rule[F-NATUR-R002] - Un número por ejercicio: qué tan poco probable es que un nativo lo diga así
**Severity**: critical | **Validation**: VALIDATED

> Al modelo local se le pregunta si un hablante nativo diría esa oración, con ese sentido y esas palabras, y el número
> de naturalidad es cuánto más probable ve el «no» que el «sí» (el logaritmo del cociente): 0 es «tanto sí como no», y
> cuanto más alto, más duda. Con el mismo modelo en la misma versión, el mismo ejercicio da el mismo número
> ([F-MLOCAL-R002](#F-MLOCAL-R002)).

Es una consulta de un token por oración, local y gratis. Se usa el cociente y no la probabilidad del «no» porque ésta
se amontona en 1: más de 300 ejercicios por nivel pasan de 0,999. El método de la propuesta (SLOR) se midió peor y
quedó descartado (José, 30/9): ver Contexto y [DOUBT-NATUR-METODO](#DOUBT-NATUR-METODO). claude -p no da probabilidades:
con la alternativa no hay número ([F-NATUR-R008](#F-NATUR-R008)).

<a id="F-NATUR-R003"></a>
### Rule[F-NATUR-R003] - Marca el 5 % más dudoso de cada nivel, y no cuenta como error
**Severity**: critical | **Validation**: VALIDATED

> Un ejercicio cuyo número queda por encima del umbral de su nivel tiene un hallazgo que sólo ordena: gravedad *baja*,
> resolución *sólo ordena* ([F-HALL-R002](#F-HALL-R002)). No baja el puntaje, no produce tarea
> ([F-HALL-R004](#F-HALL-R004)) y se cuenta como marcado, nunca como error ([F-HALL-R008](#F-HALL-R008)). El umbral de
> cada nivel es fijo y deja marcado el 5 % de ese nivel en la base del 29/9; a igual número, el orden es el del curso.

Fijo, para que cambiar un ejercicio no mueva la marca de otro ([F-HALL-R014](#F-HALL-R014)); lo decidió José el 30/9
([DOUBT-NATUR-UMBRAL](#DOUBT-NATUR-UMBRAL)). Con llama3 8B en la versión medida, 9,07 en A1 (116 de 2.334) y 10,0 en A2
(118 de 2.364). A2 se midió sin los 40 ejercicios de la lista «a / b» ([F-GRAMA-R001](#F-GRAMA-R001) inv. 5): su número
era el de una oración sin la respuesta, y 15 pasaban el umbral de antes, 10,2 (coordinador, 1/10); esos 40 ya se leen
con la respuesta en su lugar, y el umbral de A2 quedó en 10,0. B1 y B2 se midieron con la misma regla sobre la base del
29/9, con la corrida de content-audit del curso entero (1/10): de los ejercicios del nivel con número quedan marcados
los k = int(n × 0,05) más altos, y el umbral es el número con dos decimales que deja marcados exactamente esos k (marca
lo que queda por encima). Dan 9,91 en B1 (226 de 4.534) y 10,80 en B2 (95 de 1.901). Un modelo en otra versión da otros números, así que sus umbrales se vuelven a calcular con su examen
([F-MLOCAL-R006](#F-MLOCAL-R006)). Con claude -p no hay umbral: marca su «no» ([F-NATUR-R008](#F-NATUR-R008)).

<a id="F-NATUR-R004"></a>
### Rule[F-NATUR-R004] - La evidencia dice cuánto duda el modelo
**Severity**: major | **Validation**: AUTO_VALIDATED

> La evidencia trae la oración como la lee el alumno y qué dijo el modelo ([F-HALL-R003](#F-HALL-R003)): con el modelo
> local, su número y el umbral del nivel, «El modelo local duda que un nativo diga «She is an engineer of hers.» (14,7;
> umbral de A2: 10,0)»; con claude -p, su respuesta, «claude -p contesta que un nativo no diría «She is an engineer of
> hers.» (sin Ollama)».

No dice por qué: eso lo dice el juez de [traducción](glossary:Traducción) y sentido (FEAT-TRSEN), que es quien decide.

<a id="F-NATUR-R005"></a>
### Rule[F-NATUR-R005] - El orden y los marcados se pueden pasar a un juez
**Severity**: major | **Validation**: ASSUMPTION

> El informe publica, por nivel, los ejercicios marcados del más dudoso al menos dudoso —con claude -p, en el orden del
> curso—, y esa lista, como archivo de identificadores, es un conjunto válido para cualquier juez
> ([F-QINST-R019](#F-QINST-R019), [F-JEXAM-R005](#F-JEXAM-R005)).

Así «el 5 % menos esperable va primero a los jueces», como dice la propuesta, sin que Naturalidad decida nada sola.

<a id="F-NATUR-R006"></a>
### Rule[F-NATUR-R006] - Se usa sólo si ordena mucho mejor que el azar
**Severity**: critical | **Validation**: ASSUMPTION

> El examen de `naturalness` son los ejercicios que el 077 marcó antinaturales o sin sentido: lo que marca tiene que
> traer al menos el triple de ellos que el azar con la misma cantidad de marcas. Invariantes:
> 1. con el modelo local, en la cola del 5 % de cada nivel caen al menos 28 de los 186 de A1–A2 en `escribir/` (15 %), y
>    la proporción equivalente en `examinar/`;
> 2. con claude -p, sobre esos 186 y 400 ejercicios de A1–A2 elegidos al azar fuera de ellos (semilla 87), la proporción
>    de los 186 que contesta «no» es al menos el triple de la proporción de los 400;
> 3. una versión que no lo aprueba no publica marcas ([F-MLOCAL-R006](#F-MLOCAL-R006)).

Es el «si casi ninguno cae ahí, no sirve y se saca» de la propuesta, con un número. El prototipo con llama3 da 51 de 186
(27 %) y aprueba; con SLOR daba 14 (7,5 %) y no. Esos 51 se midieron con la lectura y el umbral de A2 de antes de
[F-GRAMA-R001](#F-GRAMA-R001) inv. 5: el examen de la versión los vuelve a medir. Con claude -p, rendir con la muestra
cuesta 586 consultas (3,3 h de a una, 50 min con 4) en vez de las 4.738 de A1–A2 enteros (27 h); si marca el 5 %, la
muestra lo estima con un error de alrededor de un punto, lejos del 15 % que se pide sobre los 186.

<a id="F-NATUR-R007"></a>
### Rule[F-NATUR-R007] - `naturalness` es un analizador de errores que sólo ordena
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `naturalness` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Un nativo diría así esta
> oración?», familia *errores*, evalúa *ejercicio*, resolución *sólo ordena*, dos reglas —la del modelo local, costo
> *modelo local*, y la de claude -p, costo *modelo pago*—, costo de la ficha *modelo pago*, y nombra el modelo local y
> claude -p con su versión y el resultado de su examen.

Familia *errores* porque así lo fija [F-HALL-R007](#F-HALL-R007) para los once nuevos; como sólo ordena, sus números son
marcados, no errores. El costo de la ficha es el mayor de sus reglas ([F-HALL-R006](#F-HALL-R006) inv. 2), aunque con
Ollama nunca consulte a claude -p; cada hallazgo lleva el de su regla ([F-HALL-R002](#F-HALL-R002)).

<a id="F-NATUR-R008"></a>
### Rule[F-NATUR-R008] - Sin Ollama, la misma pregunta a claude -p: un «no» marca
**Severity**: critical | **Validation**: ASSUMPTION

> Sin su modelo local ([F-MLOCAL-R007](#F-MLOCAL-R007)), `naturalness` le hace a claude -p la pregunta de R002, con el
> mismo mensaje de sistema y la misma oración terminada (R001). Invariantes:
> 1. claude -p contesta sí o no, sin número: un «no» deja un hallazgo de su regla, gravedad *baja*, que sólo ordena y
>    cuenta como marcado, como en R003; un «sí» no deja ninguno;
> 2. el diagnóstico dice que contestó claude -p y no trae número ni umbral;
> 3. la lista de cada nivel (R005) trae los «no» en el orden del curso;
> 4. hace una consulta por ejercicio que alcanza, y sólo dentro de su tope ([F-MLOCAL-R008](#F-MLOCAL-R008)).

<details><summary>Por qué sin número, un caso y cuánto cuesta</summary>

claude -p no devuelve la probabilidad de cada respuesta, así que el cociente de R002 no se puede calcular; cuántos «no»
da no está fijado en el 5 %: lo controla el examen (R006). «She is an engineer of hers.» (`67fab6d59930102295342c0d`,
A2): llama3 da 14,68 contra el umbral de 10,0 y queda marcado; sin Ollama, a claude -p se le pregunta lo mismo —«Would
a native English speaker naturally say or write this exact sentence, with this meaning and these words? Sentence: "She
is an engineer of hers."»— y, si contesta «no», queda marcado igual, con la evidencia de R004.

| Alcance | Consultas | Horas de a una | Horas con 4 a la vez |
|---|---:|---:|---:|
| A1 | 2.334 | 13,3 | 3,3 |
| A2 | 2.404 | 13,7 | 3,4 |
| B1 | 4.535 | 25,8 | 6,4 |
| B2 | 1.901 | 10,8 | 2,7 |
| Curso entero | 11.174 | 63,5 | 15,9 |

Al ritmo medido de los jueces, 176 por hora de a una (FEAT-JEXAM). Después de una tanda, una consulta por ejercicio
tocado. La duda de qué marca la alternativa es [DOUBT-NATUR-ALTERNATIVA](#DOUBT-NATUR-ALTERNATIVA).

</details>

## Contexto

Base: los 4.813 ejercicios de A1–A2 del curso del 29/9 en `db/english-course`, medidos con un prototipo en el
scratchpad del analista, sin instalar nada: llama3 8B (Q4_0) en Ollama, el único modelo de lenguaje de la máquina.

| Cola del 5 % por nivel (236 ejercicios) | Antinaturales y sin sentido del 077 (de 186) | Marcados en algún oro del 077 | Tiempo |
|---|---:|---:|---:|
| Al azar | 9 (5 %) | 23,2 % | — |
| SLOR, método de la propuesta | 14 (7,5 %) | 26,7 % | 19 min |
| Pregunta sí o no, R002 | 51 (27,4 %) | 47,9 % | 6 min |

Con la cola del 10 %, la pregunta trae el 43,5 %; con la del 20 %, el 64 %. De los 47 de gramática del examen, 9 caen en
la cola del 5 % (LanguageTool encuentra 1). Los umbrales del 5 % quedan en 9,1 (A1; exacto, 9,07) y 10,0 (A2, sin los 40
de la lista «a / b»), medidos sobre `yn-a1a2.jsonl`; la presentación decía 10,0 para A1 y era un error: en A1, 10,0 marca
90 de 2.334, el 3,9 %. Lo más dudoso de A2 es «She is an engineer of hers.», «Jakarta is the capital of the
Philippines.» y «This idea does not very good.». La cola de A2 del prototipo traía además 15 oraciones que no eran del
curso sino de su lectura de la lista «a / b» sin la respuesta, como «She bought at the market.» por «She bought
something at the market.» ([F-GRAMA-R001](#F-GRAMA-R001) inv. 5): el umbral de A2 se midió sin ellas, y la tabla de
arriba es la del prototipo, con ellas.

SLOR se midió leyendo de a un token con 20 alternativas, porque Ollama no da la probabilidad del texto de entrada: toda
palabra fuera de las 20 cuesta lo mismo. Un modelo chico con la probabilidad exacta (GPT-2) podría medir mejor; no se
probó porque hay que bajarlo. Lo que el 077 llama antinatural son oraciones de palabras comunes y bien armadas —«I eat
food every morning»—, y la pregunta directa las ve mejor que la probabilidad.

Sin Ollama, la misma pregunta va a claude -p (José, 30/9 a la noche; FEAT-MLOCAL R007): el curso entero son 11.174
consultas, 63,5 horas de a una. Es casi un tercio de lo que cuestan los cuatro jueces nuevos sobre el mismo nivel: rinde
en los niveles que los jueces no van a juzgar enteros.

## Alcance

- **Adentro**: R001 a R008: la lectura, el número, la marca, su evidencia, la lista para los jueces, el examen, la
  ficha y la alternativa con claude -p.
- **Afuera**: decidir si una oración está mal (FEAT-TRSEN); el modelo, dónde corre, cuándo se usa claude -p y su tope
  (FEAT-MLOCAL); y escribir el curso al analizar ([F-HALL-R015](#F-HALL-R015)).

## User Journeys

### Journey[F-NATUR-J001] - Ordenar qué mirar primero
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-NATUR-J001
    name: Ordenar qué mirar primero
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo sólo naturalness, con una versión que aprobó su examen y, sin Ollama, con un tope para claude -p"
        gate: [F-NATUR-R006]
        then: leer
      - id: leer
        action: "El informe lee la oración terminada de cada ejercicio y le pregunta al modelo si un nativo la diría"
        gate: [F-NATUR-R001]
        outcomes:
          - when: "La oración no tiene verbo o es una sola palabra"
            then: no_alcanza
          - when: "Con Ollama, el número queda por encima del umbral de su nivel"
            then: marcado
          - when: "Con Ollama, el número queda en el umbral o por debajo"
            then: sin_marca
          - when: "Sin Ollama, claude -p contesta que un nativo no la diría"
            then: marcado_alternativa
          - when: "Sin Ollama, claude -p contesta que un nativo sí la diría"
            then: sin_marca_alternativa
      - id: no_alcanza
        action: "El ejercicio queda sin número ni hallazgo de naturalness, y no se le consultó a ningún modelo"
        gate: [F-NATUR-R001]
        result: success
      - id: marcado
        action: "El ejercicio tiene un hallazgo que sólo ordena, con su número, entra en la lista del nivel para los jueces y no cuenta como error"
        gate: [F-NATUR-R002, F-NATUR-R003, F-NATUR-R004, F-NATUR-R005, F-NATUR-R007]
        result: success
      - id: sin_marca
        action: "El ejercicio tiene su número publicado y ningún hallazgo de naturalness"
        gate: [F-NATUR-R002]
        result: success
      - id: marcado_alternativa
        action: "El ejercicio tiene un hallazgo de claude -p que sólo ordena, sin número, entra en la lista del nivel en el orden del curso y no cuenta como error"
        gate: [F-NATUR-R004, F-NATUR-R005, F-NATUR-R008]
        result: success
      - id: sin_marca_alternativa
        action: "El ejercicio no tiene hallazgo de naturalness y su diagnóstico dice que lo leyó claude -p"
        gate: [F-NATUR-R008]
        result: success
```

## Open Questions

<a id="DOUBT-NATUR-METODO"></a>
### Doubt[DOUBT-NATUR-METODO] - ¿Preguntarle al modelo o calcular la probabilidad (SLOR)?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): preguntarle al modelo local si un nativo lo diría así (R002). Aprueba el examen con el
  modelo ya instalado: 51 de 186 en la cola del 5 %.
- [ ] Opción B: SLOR, como dice la propuesta, con un modelo chico que dé la probabilidad exacta (GPT-2, unos 500 MB).
  Con lo instalado dio 14 de 186, casi el azar; con GPT-2 no se midió.
- [ ] Opción C: medir B con GPT-2 antes de elegir.

**Answer**: **Opción A** (José, 30/9: «Dale con … el método nuevo de naturalidad»): se le pregunta al modelo si un
nativo lo diría, con respuesta sí o no, y el número es ln(P(no)/P(sí)) del primer token. SLOR queda descartado y no se
baja ningún modelo.

<a id="DOUBT-NATUR-MARCA"></a>
### Doubt[DOUBT-NATUR-MARCA] - ¿Sólo ordena, o también cuenta como error?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): sólo ordena, como aprobó José en el brief 089.
- [ ] Opción B: lo marcado cuenta como error, *baja*.

**Answer**: **Opción A** (José, 30/9): sólo ordena, no cuenta errores. De los marcados, 48 de cada 100 están en algún
oro del 077; como error, la mitad de lo que sumaría a «algún error» serían oraciones que el 077 dio por buenas.

<a id="DOUBT-NATUR-UMBRAL"></a>
### Doubt[DOUBT-NATUR-UMBRAL] - ¿El 5 % de cada corrida o un umbral fijo?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): umbral fijo por nivel, el que marcaba el 5 % en la base del 29/9.
- [ ] Opción B: el 5 % de cada nivel en cada corrida, como dice la propuesta.

**Answer**: **Opción A** (José, 30/9): el umbral es fijo por nivel. Con B, cada tanda que arregla oraciones raras hace
marcar otras que no cambiaron, y el antes y después (paquete F) mostraría marcas nuevas sin cambios.

<a id="DOUBT-NATUR-ALTERNATIVA"></a>
### Doubt[DOUBT-NATUR-ALTERNATIVA] - Sin Ollama, ¿qué marca la pregunta a claude -p?
**Status**: OPEN

- [ ] Opción A (recomendada): un «no» marca, sin número, y la lista del nivel va en el orden del curso (R008); el examen
  frena la versión que marque de más.
- [ ] Opción B: preguntarle además qué tan seguro está (sí, quizás sí, quizás no, no) y ordenar por eso. Es otra
  pregunta, con su propia versión y su examen.
- [ ] Opción C: pedirle un puntaje de 0 a 10 y marcar el 5 % de cada nivel con umbrales fijos. Es otra pregunta, y
  calibrar sus umbrales cuesta consultar A1–A2 enteros: 4.738 consultas, 27 horas de a una.

**Answer**: Pendiente. Recomiendo A, y se diseñó con A: es lo que dijo José, «la misma pregunta de sí o no», y no
inventa un número que claude -p no da.

## References

- **FEAT-HALL** — Familia, «sólo ordena», marcados, identidad, costo por regla y ficha: R003, R004, R007.
- **FEAT-GRAMA** — La oración terminada que se lee: R001.
- **FEAT-MLOCAL** — El modelo local, su versión, su examen, qué pasa si falta y la alternativa con su tope: R002, R006,
  R008.
- **FEAT-QINST, FEAT-JEXAM** — El conjunto declarado que se le pasa a un juez y el ritmo de claude -p: R005, R008.
- **FEAT-TRSEN** — El juez que decide si una oración marcada está mal: R004.
