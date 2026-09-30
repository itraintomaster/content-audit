---
feature:
  id: FEAT-GRAMA
  code: F-GRAMA
  name: Gramática - la oración terminada no tiene errores de inglés
  priority: high
---

# Gramática: la oración terminada no tiene errores de inglés

## TL;DR

**Qué**: `grammar`, de la familia de errores, pasa LanguageTool por cada oración terminada, con cada respuesta
aceptada, y marca gramática y ortografía, nunca estilo; más una regla propia de signo final.

**Por qué**: «He ___ (own) a bike» sólo acepta «'ve had», y «…making noise noise?» sale en verde. Mide el curso en
segundos y la mesa lo necesita para no meter errores nuevos; del «inglés incorrecto» del 077 ve poco: es uso y sentido.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy encontrar errores de inglés en un ejercicio:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)` → `AuditRunner.runAudit(Path, AuditRunRequest)`.
   ✓ Disponible, pero los analizadores de vocabulario miden largo, frecuencia y lemas: ninguno lee la gramática.
2. Busca un error de inglés en un ejercicio → ⚠️ ningún analizador lo hace; el juez de consigna mira la consigna.
3. Pide ese analizador solo → ⚠️ no existe en `GetCommand.get("analyzers", …)`.

**Punto de fricción**: pasos 2 y 3. «He ___ (own) a bike for a while.» acepta sólo «'ve had» y «have had»: quien
escribe «has had» queda mal, y el ejercicio sale en verde.

**Cambio mínimo necesario**: sumar `grammar` al catálogo de FEAT-HALL, con las reglas de este requirement. No cambia
ningún puntaje de vocabulario.

## Reglas de Negocio

Acá **oración terminada** es lo que el alumno lee después de contestar ([F-GRAMA-R001](#F-GRAMA-R001)), y **tema** es el
*knowledge* del curso (413), como en FEAT-HALL. Lo común a los tres analizadores de idioma —nada sale de la máquina,
versión a la vista, qué pasa si falta la herramienta, candidatos de la mesa— está en FEAT-MLOCAL.

<a id="F-GRAMA-R001"></a>
### Rule[F-GRAMA-R001] - Lee cada oración terminada, con cada respuesta aceptada, como la lee el alumno
**Severity**: critical | **Validation**: ASSUMPTION

> Por ejercicio, `grammar` lee una oración por respuesta aceptada —en opción múltiple, con la correcta— tal como queda
> después de contestar. Invariantes:
> 1. sin pistas: ni el paréntesis ni la lista «a / b» pegada al hueco; la opción «-» no deja texto;
> 2. si la respuesta es la oración entera, sólo ésa, sin la oración de partida; un diálogo se lee entero;
> 3. las contracciones van pegadas a su palabra y los signos, sin espacio antes;
> 4. si la opción es una etiqueta puesta después de la oración (*defining*, *sequence*, *correct*), se lee la oración
>    sola; y un ejercicio de juicio cuya respuesta es «not correct» no se lee, porque su oración está mal a propósito.

<details><summary>Detalle y medidas sobre la base del 29/9</summary>

Son 12.223 oraciones de 11.287 ejercicios. Leen sólo la respuesta 3.869 ejercicios; 99 opciones son etiquetas y 20
van después de un «=» o un «:» («Verb in infinitive = be»); 14 de juicio se dejan de leer y 16 sí, porque su respuesta
es «correct».

No es la [oración plana](glossary:Oraciones planas) que content-audit deriva hoy
([F-QSENT-R019](#F-QSENT-R019), [F-SMODE-R003](#F-SMODE-R003)). Esa deja «They 're» (123 ejercicios), «- water» (331 de
opción múltiple), «was / were was» (110 en 9 temas con la pista sin paréntesis) y una nota entre paréntesis seguida de
punto como «..». Con ella, LanguageTool da 920 marcas; con esta lectura, 693. Cambiar la oración plana movería el largo
de oración, y eso no se toca ([F-HALL-R013](#F-HALL-R013)).

La contraída y la larga son la misma respuesta (decisión de José, ronda 1): `grammar` lee las dos si las dos están
aceptadas, nunca marca una contra la otra y nunca dice que falta una. Eso último es de Respuestas completas (FEAT-RESPC).

</details>

<a id="F-GRAMA-R002"></a>
### Rule[F-GRAMA-R002] - Sólo gramática y ortografía, y sólo lo que marcan el inglés americano y el británico
**Severity**: critical | **Validation**: VALIDATED

> Una marca de LanguageTool da hallazgo sólo si cumple las cuatro condiciones:
> 1. su regla es de gramática, ortografía, palabras confundidas o colocaciones, o es una de las de datos (falta el
>    espacio después del punto, palabra repetida); nunca de estilo, redundancia, tipografía, mayúsculas, compuestos ni
>    variantes regionales;
> 2. la dan las dos variantes, la americana y la británica, en el mismo lugar;
> 3. la regla viene prendida en LanguageTool;
> 4. la regla no está en la lista de las que contradicen al curso.

<details><summary>Lo que queda afuera, medido sobre el curso entero (693 marcas en inglés americano)</summary>

| Queda afuera | Marcas | Ejemplo del curso | Por qué |
|---|---:|---|---|
| Mayúscula al empezar (CASING) | 401 | «is it five o'clock?» | 359 son opciones de opción múltiple: es de [F-OMVAL-R003](#F-OMVAL-R003) |
| Grafía de una sola variante | 83 / 102 | «theatre», «traveled» | el curso mezcla las dos, y José acepta las dos |
| Coma de estilo | 74 | «…a book but I fell asleep» | es estilo, no error |
| «any more», compuestos | 28 | «They don't drink wine any more.» | británico correcto |
| «i» en minúscula | 9 | «No, i can't.» | todas en opciones: es de [F-OMVAL-R003](#F-OMVAL-R003) |
| Estilo, redundancia, variantes, tipografía | 30 | «really tired» → «exhausted» | no es un error |
| Reglas apagadas por omisión | +48 | «You prefer Italian food.» → «an Italian food» | 42 de ARTICLE_MISSING, todas falsas |
| Contradicen al curso | 2 | «The band are so talented. (UK)» | el tema *Nombres colectivos* enseña las dos |

La lista de las que contradicen al curso es SINGULAR_AGREEMENT_SENT_START, la de los colectivos (José, 30/9:
[DOUBT-GRAMA-REGLAS](#DOUBT-GRAMA-REGLAS)). Con el filtro quedan 59 marcas en 39 ejercicios.

</details>

<a id="F-GRAMA-R003"></a>
### Rule[F-GRAMA-R003] - Toda oración terminada cierra con su signo
**Severity**: major | **Validation**: ASSUMPTION

> Una oración terminada con verbo, de tres palabras o más y que empieza con mayúscula, que no termina en «.», «?» o
> «!», tiene un hallazgo *baja*; también una pregunta que empieza con auxiliar y sujeto y termina en punto.

Hoy: 10, y 8 son reales: «I heard a funny noise», «She offered me cookies», «Did Michael give us these scarves.». Los
otros dos son una etiqueta tras «Link type:» y un pedazo de oración. El 077 los puso en gramática, *baja*.

<a id="F-GRAMA-R004"></a>
### Rule[F-GRAMA-R004] - La gravedad depende de dónde está el error
**Severity**: critical | **Validation**: ASSUMPTION

> Cada hallazgo toma la gravedad de la primera condición que cumple ([F-HALL-R002](#F-HALL-R002)):
> 1. **bloqueante**: la respuesta aceptada repite palabras que ya están pegadas al hueco, así que quien sigue la pista
>    escribe sin ellas y el corrector lo rechaza;
> 2. **alta**: lo marcado toca una respuesta aceptada: el corrector exige inglés mal escrito o el ejercicio lo enseña;
> 3. **media**: lo marcado está sólo en el texto fijo que lee el alumno;
> 4. **baja**: falta el signo final o el espacio después de un punto.

<details><summary>Casos del curso</summary>

- Bloqueante: «Have the neighbors been making ___ (the neighbors / make) noise?» acepta «Have the neighbors been making
  noise» (`67fab6d5993010229534320a`, B1); igual «in August in August» (`…4009`) y «the apartment the apartment» (`…408e`).
- Alta: «He ___ (own) a bike for a while.» → «'ve had | have had» (`…3f81`, B2); «Ana was teached a lesson» (`…439d`);
  «The teacher explained ___ (we / us) the lesson.» → «us» (`…2bb4`, A1, en el 077).
- Media: «A: She's choosing her clothers.» (`…33a1`); «videogames» en 13 ejercicios.
- Baja: «…slept until 11.I slept…» (7 ejercicios de *Rangos temporales*, A1).

</details>

<a id="F-GRAMA-R005"></a>
### Rule[F-GRAMA-R005] - Lo mecánico se arregla con una regla; lo demás, en la mesa
**Severity**: major | **Validation**: ASSUMPTION

> La palabra repetida pegada al hueco, el signo final y el espacio después del punto se resuelven con una *regla*:
> sale la repetición de la respuesta, entra el signo («?» si es pregunta, «.» si no) o el espacio. Todo otro hallazgo
> va a la *mesa*: el que propone corrige y los jueces confirman que no cambió lo que enseña el tema.

Ninguna regla toca el audio: la respuesta pierde palabras que el alumno ya ve escritas. Cómo trabaja la mesa es del
paquete E.

<a id="F-GRAMA-R006"></a>
### Rule[F-GRAMA-R006] - `grammar` es un analizador de errores, instantáneo, que evalúa ejercicios
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `grammar` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿La oración terminada tiene errores
> de inglés?», familia *errores*, evalúa *ejercicio*, resoluciones *regla* y *mesa*, costo *instantáneo*. Con un
> hallazgo, el ejercicio tiene puntaje 0 en este analizador; sin ninguno, 1 ([F-HALL-R004](#F-HALL-R004)).

La evidencia ([F-HALL-R003](#F-HALL-R003)) es la oración terminada, lo marcado y una línea en castellano: «teached →
taught (ortografía)». La identidad ([F-HALL-R014](#F-HALL-R014)) es la regla y lo marcado; si dos respuestas aceptadas
muestran lo mismo, es un solo hallazgo. *Instantáneo*: LanguageTool son reglas, sin modelo, y lee el curso entero en
unos 10 segundos.

<a id="F-GRAMA-R007"></a>
### Rule[F-GRAMA-R007] - El examen de Gramática
**Severity**: major | **Validation**: ASSUMPTION

> Sobre `escribir/gramatica.json` encuentra al menos los 4 que encontró el prototipo; sobre `examinar/`, en la misma
> proporción; y en una muestra de 50 marcas fuera del oro revisadas a mano, 7 de cada 10 son errores reales.

La meta es baja a propósito: de los 47 casos de A1–A2, 17 son «gone to» por «been to» y el resto, calcos y colocaciones
(«He knew French with Anna»): eso es sentido, y lo busca FEAT-TRSEN. La ficha dice cuántos agarra cada uno.

## Contexto

Base: curso vivo del 29/9 en `db/english-course`, medido con un prototipo en el scratchpad del analista, sin tocar
content-audit. LanguageTool **6.5**, la copia que trae IntelliJ (Grazie), sin el agrupador de OpenNLP, cuyos modelos no
vienen: el LanguageTool completo puede marcar algo más. La dependencia que entra en content-audit es LanguageTool **6.8**
(`org.languagetool:language-en:6.8`), la 6.x vigente en Maven Central, con el agrupador: estos números los vuelve a
medir el examen de esa versión antes de publicar ([F-MLOCAL-R006](#F-MLOCAL-R006)).

| Medida | A1 | A2 | B1 | B2 | Total |
|---|---:|---:|---:|---:|---:|
| Ejercicios con hallazgo (R002 + R003) | 15 | 8 | 16 | 10 | 49 |
| Errores reales, revisados a mano | 12 | 4 | 12 | 8 | 36 |
| Marcados por el 077 como inglés incorrecto | 7 | 40 | ~46 | ~29 | 122 |

B1 y B2 del 077 salen de los porcentajes de la propuesta (1,0 % y 1,5 %). De los 49: 36 reales —3 bloqueantes, las tres
repeticiones pegadas al hueco de R004, 13 «videogames», 7 espacios que faltan—, 3 discutibles («those ones», «a
water») y 10 falsos («The artist … gave a speech» → «give»). «He've had» no es bloqueante: con R004 es *alta*, porque
toca una respuesta aceptada y se puede acertar. **Precisión**: unas 7 de cada 10 (36 de 49). **Costo**: todo el curso en unos 10 segundos más 1 de
arranque, en la máquina.

**El examen.** Sobre los 47 de `escribir/gramatica.json` encuentra 4 (8,5 %): «explained us» y tres signos finales.
Marca 23 en esa parte, 19 fuera del oro, muchos reales de otras categorías (los «11.I» están en el oro de pista).

## Alcance

- **Adentro**: la lectura de R001, el filtro de R002, el signo final, gravedad, resolución, ficha y examen.
- **Afuera**: el uso y el sentido (FEAT-TRSEN); la forma que falta (FEAT-RESPC); mayúsculas de las opciones
  (FEAT-OMVAL); la oración de partida de una reescritura; cambiar la oración plana de content-audit; y escribir el
  curso al analizar ([F-HALL-R015](#F-HALL-R015)).

## User Journeys

### Journey[F-GRAMA-J001] - Revisar la gramática de un ejercicio
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-GRAMA-J001
    name: Revisar la gramática de un ejercicio
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo sólo grammar"
        then: leer
      - id: leer
        action: "El informe lee cada oración terminada del ejercicio, una por respuesta aceptada, como la lee el alumno"
        gate: [F-GRAMA-R001]
        outcomes:
          - when: "LanguageTool marca algo de gramática u ortografía que marcan las dos variantes"
            then: hallazgo
          - when: "La oración no cierra con su signo"
            then: signo
          - when: "Sólo hay marcas de estilo, mayúsculas, variantes o de reglas que contradicen al curso"
            then: limpio
      - id: hallazgo
        action: "El ejercicio tiene un hallazgo con su gravedad según dónde está el error, y va a la mesa salvo la repetición pegada al hueco"
        gate: [F-GRAMA-R002, F-GRAMA-R004, F-GRAMA-R005, F-GRAMA-R006]
        result: success
      - id: signo
        action: "El ejercicio tiene un hallazgo baja que se arregla con una regla"
        gate: [F-GRAMA-R003, F-GRAMA-R005]
        result: success
      - id: limpio
        action: "El ejercicio no tiene hallazgo de grammar y queda con puntaje 1"
        gate: [F-GRAMA-R002, F-GRAMA-R006]
        result: success
```

## Open Questions

<a id="DOUBT-GRAMA-REGLAS"></a>
### Doubt[DOUBT-GRAMA-REGLAS] - ¿Qué reglas de LanguageTool quedan afuera?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): las categorías de R002, lo que no marcan las dos variantes, las apagadas por omisión y
  SINGULAR_AGREEMENT_SENT_START. Da 49 ejercicios, unos 7 de cada 10 reales.
- [ ] Opción B: además, las ocho reglas que dieron un solo falso en el curso (AT_TIME, GAVE_GIVE…). Da 41, casi todos
  reales, pero es afinar contra el mismo curso.
- [ ] Opción C: todo lo que LanguageTool trae prendido. Da 693 marcas; la mayoría, mayúsculas y estilo.

**Answer**: **Opción A** (José, 30/9: «Dale con LanguageTool»): quedan afuera estilo, mayúsculas, compuestos, variantes
regionales, lo que no marcan las dos variantes, las reglas apagadas por omisión y SINGULAR_AGREEMENT_SENT_START, la de
los colectivos. Los falsos que quedan los descarta la mesa.

<a id="DOUBT-GRAMA-ALCANCE"></a>
### Doubt[DOUBT-GRAMA-ALCANCE] - Si encuentra 4 de 47, ¿vale la pena?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): sí, como control barato: los 36 errores reales de hoy (3 bloqueantes) y cada candidato
  de la mesa, en milisegundos, antes de pagarle a un juez.
- [ ] Opción B: no; el inglés incorrecto lo busca sólo FEAT-TRSEN.

**Answer**: **Opción A** (José, 30/9): Gramática sirve de control gratis de los arreglos de la mesa. El juez de
[traducción](glossary:Traducción) y sentido cuesta dos consultas por candidato, y un «He have had» nuevo tiene que
frenarse antes, gratis.

## References

- **FEAT-HALL** — Hallazgo, gravedad, resolución, costo, ficha, puntaje e identidad: R004, R006.
- **FEAT-MLOCAL** — Lo común a los analizadores de idioma: la herramienta local, su versión y los candidatos de la mesa.
- **FEAT-QSENT, FEAT-SMODE** — La oración plana de hoy, que esta lectura no reemplaza: R001.
- **FEAT-OMVAL, FEAT-RESPC, FEAT-TRSEN** — Lo que es de otros: mayúsculas de las opciones, la forma que falta, el
  sentido: R002, R007.
