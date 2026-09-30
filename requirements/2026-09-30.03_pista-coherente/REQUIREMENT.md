---
feature:
  id: FEAT-PISTA
  code: F-PISTA
  name: Pista coherente - la pista anuncia lo que el alumno tiene que escribir
  priority: high
---

# Pista coherente: la pista le anuncia al alumno lo que tiene que escribir

## TL;DR

**Qué**: Un analizador de la familia de errores compara la pista de cada ejercicio con su respuesta —el verbo, el
sujeto y la negación— y marca los ejercicios sin pista en un tema donde casi todos la llevan.

**Por qué**: Una pista que no coincide con la respuesta hace imposible acertar: «Who ___ (I / see)?» sólo acepta «are
you seeing». El 077 encontró 188 ejercicios imposibles y content-audit los muestra en verde.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy saber si un ejercicio se puede acertar siguiendo su pista:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible, pero mide la oración sin la
   pista ([F-QSENT-R019](#F-QSENT-R019)): la pista no entra en ningún número.
2. Compara la pista con la respuesta → ⚠️ ningún analizador lo hace; el juez de consigna mira la consigna, no la pista.
3. Pide ese analizador solo → ⚠️ no existe en `GetCommand.get("analyzers", …)`.

**Punto de fricción**: pasos 2 y 3. El ejercicio de las verduras, «I ___ a lot of vegetables.», es el único de su tema
sin pista y sale en verde.

**Cambio mínimo necesario**: sumar `hint-coherence` al catálogo de FEAT-HALL, con las reglas de este requirement.

## Reglas de Negocio

Acá **pista** es el texto entre paréntesis pegado al hueco ([F-QSENT-R007](#F-QSENT-R007)) o, en un ejercicio cuya
respuesta es la oración entera, el texto entre paréntesis del enunciado y el enunciado de palabras separadas por «/»
(«She / assist you.») [ASSUMPTION]. No son pista el nombre de un tiempo verbal («(present simple)»), una instrucción
(«(be extra polite)») ni una nota de contexto («(the listener doesn't know which man)»). Se compara con la **primera
respuesta aceptada**, la que dice el audio.

<a id="F-PISTA-R001"></a>
### Rule[F-PISTA-R001] - Si la mitad o más del tema lleva pista, la llevan todos
**Severity**: critical | **Validation**: ASSUMPTION

> Entre los ejercicios de hueco para escribir de un tema y del mismo tipo —hueco dentro de la oración, o respuesta que
> es la oración entera—, si la mitad o más lleva pista, cada uno que no la lleva tiene un hallazgo *alta*.

«I ___ a lot of vegetables.» (`67fab6d59930102295341ecd`, A1, *Afirmativas regulares*: 19 de 20 con pista) y «She put
a ___ of sugar in her tea.» (`67fab6d59930102295342a02`, A2, 9 de 10). El tipo separa los temas mixtos: en *El
posesivo* (A1), las 20 transformaciones sin pista no se comparan con los 20 huecos que sí la llevan. La opción múltiple
no entra: sus opciones ya dicen qué elegir. El umbral: [DOUBT-PISTA-UMBRAL](#DOUBT-PISTA-UMBRAL).

<a id="F-PISTA-R002"></a>
### Rule[F-PISTA-R002] - Las palabras de la pista están en la respuesta, y la respuesta no trae un verbo sin anunciar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cada palabra de la pista, salvo el sujeto y «not», está en la oración terminada, igual, flexionada (*see* →
>    *sees*, *seeing*; *quiet* → *quieter*; *nice* → *nicely*) o por su [lema](glossary:Lema) (*went* → *go*,
>    *best* → *good*).
> 2. Todo verbo de la respuesta que no sea auxiliar está en la pista.
>
> Si no, hallazgo *bloqueante*: quien sigue la pista escribe otra cosa y el corrector la rechaza.

<details><summary>Detalle y casos del curso</summary>

- «This sentence ___ (mean) 'hope'.» → «implies» (`67fab6d599301022953420c7`, A1).
- «This idea ___ (not / sound) very good.» → «does not»: el verbo no está en ningún lado (`67fab6d59930102295342657`).
- «That was ___ (bad) holiday ever!» → «the best» (`67fab6d59930102295343af4`, A2).
- Invariante 2: «It ___ (be) soon.» → «'s going to rain»: nada anuncia *rain* (`67fab6d5993010229534215c`, A1).
- Pasa: «You ___ (see) cats.» → «see»; «He ___ (not / cut) himself.» → «has not cut».

Comparar sólo por lema no alcanza: el lematizador compartido ([F-NLP-R010](#F-NLP-R010)) da *hated* → *hat* y
*drunk*, *swum* o *rang* sin su verbo. Por eso cuenta también la [forma flexionada](glossary:Forma flexionada), y el diseño tiene que reconocer los
irregulares. Una pista de elección («(good / well)», «Our / ours») no se compara palabra por palabra: la respuesta es
una de sus opciones.

</details>

<a id="F-PISTA-R003"></a>
### Rule[F-PISTA-R003] - El sujeto de la pista es el de la respuesta, y ninguno queda sin anunciar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Si la pista trae un pronombre sujeto (*I*, *you*, *he*, *she*, *it*, *we*, *they*) y la respuesta trae sujeto, es
>    ese pronombre.
> 2. Si el hueco abre la oración y la respuesta trae un pronombre sujeto, la pista o el resto del enunciado lo nombran.
>
> Si no, hallazgo *bloqueante*.

«Who ___ (I / see)?» → «are you seeing» (`67fab6d59930102295342031`); «What ___ (it / be)?» → «is that»
(`67fab6d599301022953420ad`, A1). Invariante 2: «___ (not / dream) about her.» → «I haven't dreamt»
(`67fab6d59930102295342857`, A2). Un *it* o un *there* sin significado («(him) ___» → «It's him.») no cuentan como
sujeto sin anunciar: son lo que se enseña.

<a id="F-PISTA-R004"></a>
### Rule[F-PISTA-R004] - La pista lleva «not» sólo si la respuesta es negativa
**Severity**: major | **Validation**: AUTO_VALIDATED

> Si la pista trae «not», la respuesta lleva *not* o *n't*. Si no, hallazgo *bloqueante*: el alumno niega y el
> corrector espera una afirmativa.

Hoy: 0. Que una respuesta negativa no tenga «not» en la pista no es hallazgo: la negación puede venir de la consigna
(«Oraciones negativas») o del diálogo («No, you ___ .»).

<a id="F-PISTA-R005"></a>
### Rule[F-PISTA-R005] - `hint-coherence` es un analizador de errores con arreglo por regla
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `hint-coherence` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿La pista le anuncia al
> alumno lo que tiene que escribir?», familia *errores*, evalúa *ejercicio*, resoluciones *regla* y *mesa*, costo
> *instantáneo*. Cada hallazgo se resuelve con una *regla*: la pista sale de la primera respuesta (su verbo en forma
> base, su sujeto y su «not»), y el audio no cambia. Con hallazgo, el ejercicio tiene puntaje 0; sin ninguno, 1.

La evidencia es el ejercicio como lo ve el alumno, la respuesta y la línea que nombra la falta: «Who ___ (I / see)? →
are you seeing: el sujeto I no es you» ([F-HALL-R003](#F-HALL-R003)). La identidad lleva la regla y la palabra
([F-HALL-R014](#F-HALL-R014)). Cuándo va a la mesa: [DOUBT-PISTA-RESPUESTA](#DOUBT-PISTA-RESPUESTA). El nombre es el del
brief 089.

## Contexto

Base: curso vivo del 29/9 en `db/english-course`, medido con un prototipo de las reglas en el scratchpad del analista.
Llevan pista 5.444 de 11.287 ejercicios: 5.148 de hueco para escribir (A1 1.297, A2 1.170, B1 1.862, B2 819) y 296 de
opción múltiple. De 325 temas con huecos para escribir, 203 tienen alguno con pista y 179 la ponen en todos.

| Medida | A1 | A2 | B1 | B2 | Total |
|---|---|---|---|---|---|
| Imposibles del 077 (la categoría «pista») | 30 | 16 | 115 | 27 | 188 |
| R001 con la mitad | 28 | 11 | 52 | 11 | 102 |
| R001 con tres cuartos | 8 | 3 | 3 | 1 | 15 |
| R002, verbo | 3 | 5 | 36 | 23 | 67 |
| R003, sujeto | 3 | 2 | 0 | 0 | 5 |
| R004, negación | 0 | 0 | 0 | 0 | 0 |
| Todo, con tres cuartos | 14 | 10 | 39 | 24 | 87 |

Con tres cuartos, las marcas caen en 50 temas. En B1–B2 el prototipo todavía confunde irregulares e instrucciones; en
una muestra de 30 de R002, unas 12 eran reales, como «The bus ___ (leave) at 7:45.» → «departs»
(`67fab6d59930102295342fb4`). Esa precisión es del prototipo, no del diseño.

**El examen.** La categoría del examen son los 188 imposibles, no sólo los de pista. `escribir/pista.json` trae 46 (A1
30, A2 16): 15 anotados de pista por el 077 (sin pista, verbo, sujeto) y 31 por otras causas. Con tres cuartos, el
prototipo marca 24 y encuentra 21: los 15 de pista y 6 más. No encuentra 10 transformaciones con otro verbo («Polite
request: I want to eat some cake. ___» → «Could I have some cake?»), 4 contradicciones con la consigna, 3 respuestas
cortadas por la «|» (las marca FEAT-DATOS) y 6 sin pista en temas de menos de tres cuartos. **Meta propuesta**: 9 de
cada 10 de los anotados de pista en la otra mitad (142 imposibles); cerca de 4 de cada 10 de la categoría entera; y 8
de cada 10 marcas nuevas reales en una muestra. **Costo**: segundos, sin modelo.

## Alcance

- **Adentro**: R001 a R004 sobre la pista y la primera respuesta, sus hallazgos y la ficha.
- **Afuera**: la consigna que contradice la respuesta (juez de consigna); otra respuesta igual de buena y el enunciado
  que se transforma ([DOUBT-PISTA-TRANSFORMAR](#DOUBT-PISTA-TRANSFORMAR)); y escribir el curso al analizar
  ([F-HALL-R015](#F-HALL-R015)).

## User Journeys

### Journey[F-PISTA-J001] - Revisar si la pista anuncia la respuesta
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-PISTA-J001
    name: Revisar si la pista anuncia la respuesta
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo sólo hint-coherence"
        then: leer
      - id: leer
        action: "El informe lee la pista y la primera respuesta de cada ejercicio, y cuántos del tema llevan pista"
        gate: [F-PISTA-R005]
        outcomes:
          - {when: "El ejercicio no tiene pista y la mitad o más de su tema, del mismo tipo, sí", then: falta}
          - {when: "La pista nombra otro verbo, otro sujeto u otra negación que la respuesta", then: no_coincide}
          - {when: "La pista coincide con la respuesta", then: coincide}
      - {id: falta, action: "Hallazgo alta, con los ejercicios del tema que sí llevan pista como evidencia", gate: [F-PISTA-R001], result: success}
      - {id: no_coincide, action: "Hallazgo bloqueante, con la pista y la respuesta como evidencia, que se arregla sacando la pista de la respuesta", gate: [F-PISTA-R002, F-PISTA-R003, F-PISTA-R004], result: success}
      - {id: coincide, action: "Sin hallazgo de este analizador; el ejercicio queda con puntaje 1", gate: [F-PISTA-R005], result: success}
```

## Open Questions

<a id="DOUBT-PISTA-UMBRAL"></a>
### Doubt[DOUBT-PISTA-UMBRAL] - ¿Desde qué parte del tema con pista se exige a todos?
**Status**: OPEN

- [ ] Opción A: desde la mitad, como propone el brief 089 (102 hallazgos).
- [ ] Opción B (recomendada): desde tres cuartos (15 hallazgos).
- [ ] Opción C: siempre, en todo ejercicio.

**Answer**: Pendiente. Recomiendo B. En A1–A2, A marca 39 y sólo 9 son del 077; B marca 11 y 9 lo son. Lo que A suma
son temas mixtos como *One y ones* (12 de 20), donde «I'd love ___ .» → «one» no necesita pista. C marca temas de
respuesta libre.

<a id="DOUBT-PISTA-RESPUESTA"></a>
### Doubt[DOUBT-PISTA-RESPUESTA] - Si lo que está mal es la respuesta, ¿va a la mesa aunque cambie el audio?
**Status**: OPEN

- [ ] Opción A (recomendada): sí; la regla arregla la pista por defecto, y la mesa decide cuando la respuesta rompe lo
  que enseña el tema, y entonces se regraba el audio.
- [ ] Opción B: nunca se toca la respuesta: siempre se arregla la pista.

**Answer**: Pendiente. Recomiendo A, como el brief 089. En «That was ___ (bad) holiday ever!» → «the best», arreglar
la pista a «(good)» da un ejercicio válido pero deja un tema de superlativos con el irregular equivocado; eso lo ve la
mesa.

<a id="DOUBT-PISTA-TRANSFORMAR"></a>
### Doubt[DOUBT-PISTA-TRANSFORMAR] - ¿La oración que se transforma cuenta como pista?
**Status**: OPEN

- [ ] Opción A (recomendada): no; queda para los jueces del paquete C.
- [ ] Opción B: sí; sus verbos tienen que estar en la respuesta.

**Answer**: Pendiente. Recomiendo A. B encuentra 10 imposibles más de la mitad para escribir, como «I want to eat some
cake.» → «Could I have some cake?», pero marca también «I want a sandwich.» → «I would like a sandwich.», donde cambiar
el verbo es la lección, y todo el estilo indirecto, que suma *said*.

## References

- **FEAT-HALL** — Hallazgo, gravedad, resolución, costo, ficha, conteo e identidad: R005.
- **FEAT-QSENT** — La pista entre paréntesis y la primera respuesta ([F-QSENT-R007](#F-QSENT-R007),
  [F-QSENT-R018](#F-QSENT-R018)): R001 a R004.
- **FEAT-NLP** — Una sola tokenización ([F-NLP-R010](#F-NLP-R010)): R002.
- **FEAT-DATOS** — Las respuestas cortadas por la «|»: el examen.
