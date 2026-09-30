---
feature:
  id: FEAT-TRSEN
  code: F-TRSEN
  name: Juez de traducción y sentido
  priority: high
---

# Juez de traducción y sentido

## TL;DR

**Qué**: Un analizador nuevo, `translation-sense`, lee cada ejercicio entero como queda y dice si el inglés y la
traducción dicen lo mismo y si un nativo lo diría así; lo juzgan dos jueces que no se hablan.

**Por qué**: El 077 encontró 738 así y ningún analizador los ve: el de consigna no lee la traducción y dio «cumple» en
284 de los 285 de A1 y A2.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy encontrar traducciones infieles y oraciones raras:

1. Corre la auditoría → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible.
2. Busca un analizador que lea la traducción → `AnalyzerRegistry.listAnalyzers()`. ⚠️ Ninguno: los de vocabulario miden
   el inglés y el de consigna no recibe la traducción ([F-JEXAM-R007](#F-JEXAM-R007)).
3. Juzga la traducción de un ejercicio → `AssessCandidateCommand.assessCandidate(...)`. ⚠️ Sólo sobre un candidato de una
   tarea de consigna y comparado con el original, que da por bueno ([F-QICOR-R004](#F-QICOR-R004), criterio 4).

**Punto de fricción**: pasos 2 y 3: nada juzga la traducción ni el sentido del ejercicio tal como está.

**Cambio mínimo necesario**: un analizador `translation-sense`, de la familia de errores ([F-HALL-R007](#F-HALL-R007)),
con su ficha en el catálogo, que juzga en modo absoluto todo ejercicio con dos jueces y usa lo común de FEAT-JEXAM.

## Reglas de Negocio

<a id="F-TRSEN-R001"></a>
### Rule[F-TRSEN-R001] - Lee el ejercicio entero como lo ve el alumno, y nada más
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Por cada ejercicio, el juez lee el nivel, la consigna que ve el alumno, el enunciado con su pista, la oración completa
> con cada respuesta aceptada —en opción múltiple, con la correcta— y la [traducción](glossary:Traducción). No lee la
> miniteoría, el audio, las opciones incorrectas ni otros ejercicios. La huella es exactamente eso: cambiar la traducción
> o una respuesta deja al ejercicio sin veredicto; cambiar la miniteoría o el audio, no.

Alcanza los 11.287 ejercicios: A1 2.402, A2 2.411, B1 4.554, B2 1.920. Mantiene [F-EVCOST-R001](#F-EVCOST-R001) y
[F-EVCOST-R002](#F-EVCOST-R002). La consigna es la copia del ejercicio, la que muestra la app.

<a id="F-TRSEN-R002"></a>
### Rule[F-TRSEN-R002] - Cuatro condiciones, sobre el ejercicio como está
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El veredicto dice, sin comparar con ningún original, si el ejercicio cumple cuatro condiciones:
> 1. **dicen lo mismo**: la traducción no afirma, omite ni invierte nada del inglés (polaridad, tiempo, persona, número);
> 2. **un nativo lo diría así**: se entiende, puede pasar en el mundo real y no es un calco;
> 3. **las palabras van juntas**: verbo y objeto, adjetivo y sustantivo, preposición;
> 4. **el registro es del nivel**.
>
> Por cada condición que falla da su gravedad y una evidencia que cumple [F-HALL-R003](#F-HALL-R003).

<details><summary>Detalle y ejemplos del curso</summary>

Las condiciones 1 y 2 usan la vara de «traducción fiel» y «sentido en el mundo real» del catálogo de FEAT-QICOR
([F-QICOR-R016](#F-QICOR-R016)), pero en modo absoluto: allá se compara con el original y se lo da por bueno. No reprueba
por estilo, por sinónimos válidos ni por ser corta o sin contexto.

| Condición | Ejercicio (`67fab6d5…`) | Qué encuentra |
|---|---|---|
| 1 | «I ___ a lot of vegetables.» / «Me gustan mucho las verduras.» (`…1ecd`) | «a lot of» cuenta verduras; «mucho» dice cuánto gustan |
| 1 | «A: Are they at the beach? Yes, they ___ .» / «Sí, lo son.» (`…25ea`) | «ser» donde el castellano pide «estar» |
| 2 | «Jakarta is the capital of ___ Philippines.» (`…350b`) | dato falso: Yakarta es la capital de Indonesia |
| 2 | «It's the moment ___ dinner!» (`…2d9e`) | calco: un nativo dice «It's time for dinner!» |
| 3 | «I didn't meet something / anything.» (`…396a`) | meet va con personas, no con cosas |

</details>

<a id="F-TRSEN-R003"></a>
### Rule[F-TRSEN-R003] - Dos jueces que no se hablan
**Severity**: critical | **Validation**: VALIDATED

> Cada ejercicio lo juzgan dos jueces, cada uno en un contexto limpio y sin ver al otro; cada uno es un evaluador propio
> en el libro, así que sus veredictos no se pisan y se reusan por separado. Un ejercicio está evaluado cuando tiene los
> dos. Juntos:
> 1. si los dos dicen que cumple, no hay hallazgo;
> 2. si los dos dicen que no, hay un hallazgo por cada condición que marcó alguno, y cuenta como error;
> 3. si no coinciden, hay un solo hallazgo que sólo ordena, «los jueces no coinciden», con los dos veredictos, y el
>    ejercicio queda marcado para la bandeja.

Si fueran un solo evaluador, el más reciente pisaría al otro ([F-EVCOST-R006](#F-EVCOST-R006)). Uno parte del inglés y el
otro de la traducción. La combinación es la de José, 30/9 ([DOUBT-TRSEN-COMBINAR](#DOUBT-TRSEN-COMBINAR)).

<a id="F-TRSEN-R004"></a>
### Rule[F-TRSEN-R004] - Gravedad y puntaje
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Ningún hallazgo de este juez es bloqueante: la traducción se ve después de contestar. Es **alta** si enseña algo falso
> (la traducción contradice al inglés, o la oración afirma algo imposible o un dato falso), **media** si se entiende pero
> está mal (calco, palabras que no van juntas, una parte sin traducir) y **baja** si sólo el registro no es del nivel. El
> puntaje es 1 sin errores y, si no, 0,3, 0,6 u 0,8 según su gravedad más alta [ASSUMPTION].

Usa las definiciones de [F-HALL-R002](#F-HALL-R002); bajo 1 hay tarea ([F-HALL-R004](#F-HALL-R004)). El 077 puso en A1 y
A2 97 bajas, 177 medias y 11 altas: el examen dirá si la escala coincide.

<a id="F-TRSEN-R005"></a>
### Rule[F-TRSEN-R005] - Reuso, tope, fallas y cobertura, como el juez de consigna
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Con el nombre `translation-sense`:
> 1. reusa el veredicto de cada juez por la huella, sin tope y sin consultar ([F-EVCOST-R003](#F-EVCOST-R003));
> 2. no hace más consultas nuevas por corrida que su tope, contando las de los dos jueces;
> 3. una falla del servicio o una salida inutilizable no corta la corrida ni queda registrada ([F-EVCOST-R005](#F-EVCOST-R005),
>    [F-EVCOST-R009](#F-EVCOST-R009), [F-QINST-R007](#F-QINST-R007), [F-QINST-R013](#F-QINST-R013));
> 4. la cobertura declara alcanzados, evaluados y sin evaluar ([F-HALL-R008](#F-HALL-R008)), y lo sin evaluar no tiene
>    puntaje ni hallazgo ([F-QINST-R004](#F-QINST-R004)).

Valen además los grupos A y B de FEAT-JEXAM. Llega a tema, topic, nivel y curso ([F-HALL-R011](#F-HALL-R011)) sin mover el
puntaje de vocabulario ([F-HALL-R009](#F-HALL-R009)). Tope por omisión: 0, sin pedido no consulta
([F-JEXAM-R010](#F-JEXAM-R010)); con tope, a lo sumo 4 consultas a la vez ([F-JEXAM-R006](#F-JEXAM-R006)).

<a id="F-TRSEN-R006"></a>
### Rule[F-TRSEN-R006] - El examen de translation-sense
**Severity**: critical | **Validation**: ASSUMPTION

> Cada versión aprueba el examen fijo ([F-JEXAM-R001](#F-JEXAM-R001)) si reprueba las verduras con los dos jueces y no
> reprueba más de 2 de los 50 buenos; el grupo b se informa sin contar. Sobre `escribir/traduccion_sentido.json` la meta
> es encontrar 200 de los 285 casos, y que 7 de cada 10 de 50 marcas fuera del oro, revisadas a mano, sean reales.

`escribir/`: 285 casos de A1 (114) y A2 (171): 79 de sentido, 97 de traducción y 109 antinaturales; 200 CLOZE y 85 de
opción múltiple. Se mide juzgando esos 285 y 300 ejercicios sin fila al azar: 1.170 consultas. El examen fijo, 102.

<a id="F-TRSEN-R007"></a>
### Rule[F-TRSEN-R007] - Lo que marca se resuelve en la mesa, con los mismos dos jueces
**Severity**: major | **Validation**: AUTO_VALIDATED

> Todo hallazgo de este juez que cuenta como error se resuelve en la mesa, y cada candidato que la mesa propone —el que
> cambia el inglés y el que cambia la traducción— lo juzgan estos mismos dos jueces ([F-JEXAM-R005](#F-JEXAM-R005)).

Cuando inglés y traducción no coinciden, la mesa escribe los dos arreglos y elige el juez; regrabar el audio no pesa en
la decisión. Cómo propone y decide la mesa es del paquete E.

## Contexto

| Ficha | `translation-sense` |
|---|---|
| Pregunta | ¿El inglés y la traducción dicen lo mismo, y un nativo lo diría así? |
| Lee | nivel, consigna, enunciado con pista, oración con cada respuesta, traducción |
| Reglas | dicen lo mismo · un nativo lo diría así · las palabras van juntas · registro del nivel · los jueces no coinciden |
| Meta y familia | 0 ejercicios con error · errores |
| Evalúa y resuelve | ejercicio · mesa (el desacuerdo sólo ordena) |
| Costo | modelo pago: dos consultas por ejercicio |

**Costo.** El curso entero son 22.574 consultas: A1 4.804, A2 4.822, B1 9.108, B2 3.840. Al ritmo medido del juez de
consigna, 119 horas de a una, unas 30 con 4 a la vez. Del orden de 5.000 tokens de entrada y 400 de salida por consulta,
si su guía se parece a la del de consigna. Después de una tanda, dos consultas por ejercicio cuyo inglés, respuesta,
pista, consigna o traducción cambió.

## Alcance

- **Adentro**: el analizador, sus dos jueces, las cuatro condiciones, gravedad, puntaje, examen y ficha.
- **Afuera**: los dos arreglos y la mesa (E); naturalidad estadística y gramática (B); regrabar audios; lo común de los
  jueces (FEAT-JEXAM).

## User Journeys

### Journey[F-TRSEN-J001] - Juzgar la traducción y el sentido de un ejercicio
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-TRSEN-J001
    name: Juzgar la traducción y el sentido de un ejercicio
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo translation-sense con un tope, con una versión que aprobó su examen"
        gate: [F-TRSEN-R001, F-TRSEN-R003]
        outcomes:
          - when: "Cada juez ya juzgó ese mismo contenido, o se lo consulta en un contexto limpio dentro del tope"
            then: combinar
          - when: "Falta el veredicto de algún juez y no queda tope"
            then: sin_evaluar
      - id: combinar
        action: "El sistema junta los dos veredictos"
        outcomes:
          - when: "Los dos dicen que cumple"
            then: pasa
          - when: "Los dos dicen que no cumple"
            then: error
          - when: "No coinciden"
            then: desacuerdo
      - id: pasa
        action: "El ejercicio no tiene hallazgo de translation-sense y su puntaje es 1"
        result: success
      - id: error
        action: "El ejercicio tiene un hallazgo por condición marcada, con gravedad y evidencia, y cuenta como error en su tema, nivel y curso"
        gate: [F-TRSEN-R002, F-TRSEN-R004]
        result: success
      - id: desacuerdo
        action: "El ejercicio queda marcado con los dos veredictos y no cuenta como error"
        gate: [F-TRSEN-R003]
        result: success
      - id: sin_evaluar
        action: "El ejercicio queda sin evaluar, sin puntaje ni hallazgo, y la cobertura lo cuenta"
        gate: [F-TRSEN-R005]
        result: success
```

## Open Questions

Proveedor (`claude-cli`), tope (0 sin pedido), orden (examen fijo, `escribir/` y A1) y consultas a la vez (4) se
decidieron una vez para los cinco jueces, en las dudas DOUBT-JUECES-* de
[FEAT-JEXAM](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#DOUBT-JUECES-PROVEEDOR) (José, 30/9). Este juez es el
más caro: 22.574 de las 41.700 consultas del curso entero.

<a id="DOUBT-TRSEN-COMBINAR"></a>
### Doubt[DOUBT-TRSEN-COMBINAR] - ¿Qué hace el analizador cuando los dos jueces no coinciden?
- [ ] Opción A: basta con uno: el ejercicio cuenta como error. Encuentra más y marca más de lo que hay.
- [ ] Opción B: hacen falta los dos, y el desacuerdo no se ve.
- [x] Opción C (recomendada): los dos, error; uno solo, un marcado que no cuenta y va a la bandeja como desempate.

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción C** (José, 30/9): «si los dos de traducción no coinciden, queda marcado para la bandeja». Es lo que
fija [F-TRSEN-R003](#F-TRSEN-R003): el número de errores no se infla y el desacuerdo llega a quien desempata. El examen
sobre `escribir/` informa además cuánto encontraría A.

<a id="DOUBT-TRSEN-CATALOGO"></a>
### Doubt[DOUBT-TRSEN-CATALOGO] - ¿La corrección de consigna usa este juez para la traducción y el sentido?
- [x] Opción A (recomendada): no por ahora: la corrección de consigna sigue con su vara comparativa y la mesa usa esta.
- [ ] Opción B: sí: los criterios 4 y 6 de [F-QICOR-R004](#F-QICOR-R004) consultan este juez, como pide
  [F-QICOR-R015](#F-QICOR-R015) («una sola definición»); un candidato cuya traducción ya estaba mal pasaría a reprobar.

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción A** (José, 30/9, «por lo demás, continúa», sobre la presentación que la mostraba en «Lo que no toca»):
la corrección de consigna sigue comparando con el original y sus criterios conviven con los de este juez hasta que la mesa
del paquete E la reemplace. Es el único choque con una regla existente: dos definiciones de «traducción fiel» conviven
hasta entonces.

## References

- **FEAT-HALL** — Familia de errores, hallazgo, gravedad, conteos y catálogo. Citado por R002, R004 y R005.
- **FEAT-JEXAM** — Examen, costo pendiente, conjunto, candidato, consultas a la vez y dudas comunes. Citado por R005 a R007.
- **FEAT-EVCOST** — Evaluador por pregunta, huella, reuso y fallas. Citado por R001, R003 y R005.
- **FEAT-QINST** — Comportamiento del juez de consigna que este copia: fallas y lo no evaluado. Citado por R005.
- **FEAT-QICOR** — Traducción fiel y sentido en modo comparativo; una sola definición. Citado por R002 y las dudas.
