---
feature:
  id: FEAT-JEXAM
  code: F-JEXAM
  name: El examen fijo de los jueces y el juez de consigna, sin tocar su huella
  priority: critical
---

# El examen fijo de los jueces y el juez de consigna, sin tocar su huella

## TL;DR

**Qué**: Todo juez con modelo rinde un examen fijo por versión antes de juzgar el curso y dice cuánto le falta y cuánto
cuesta; el juez de consigna, además, muestra su veredicto entero sin cambiar lo que lee.

**Por qué**: El juez de consigna se afinó para marcar menos y nunca se midió lo que deja pasar: da por buenos 178 de los
187 imposibles. Con cuatro jueces nuevos, pagar sin medir se multiplica por cinco.

## Recorrido de uso (estado actual)

Cómo el operador intenta hoy saber si un juez sirve y cuánto cuesta usarlo:

1. Corre el juez de consigna → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)` con `instructionBudget`. ✓ Disponible.
2. Lee su cobertura y sus veredictos por versión → informe de `analyze` ([F-QINST-R005](#F-QINST-R005)). ✓ Disponible.
3. Juzga un conjunto → `AnalyzeOptions.reevaluateInstructionQuizIds` ([F-QINST-R018](#F-QINST-R018)). ✓ Sólo el de consigna.
4. Averigua si la versión juzga bien → ⚠️ no hay examen: los 238 ejercicios del examen fijo viven fuera de content-audit.
5. Sabe cuánto cuesta lo pendiente → ⚠️ ni `analyze` ni `StatsAnalyzerCommand.showStats` dicen consultas ni horas.
6. Ve el veredicto entero → ⚠️ el diagnóstico guarda lo que pasó ([F-QINST-R003](#F-QINST-R003)); el hallazgo sólo las
   violaciones ([F-HALL-R003](#F-HALL-R003)).

**Punto de fricción**: pasos 4 a 6, y el 3 para los jueces nuevos.

**Cambio mínimo necesario**: un pedido para rendir el examen, con resultado guardado por versión; el costo de lo
pendiente en la cobertura; conjunto, candidato y consultas a la vez para todo juez; y el veredicto entero del de consigna.
Lo que lee el juez de consigna no cambia.

## Reglas de Negocio

**A**, el examen (R001–R003), y **B**, el uso (R004–R006), valen para los cinco jueces: consigna, FEAT-TRSEN,
FEAT-UNICA, FEAT-MINIT y FEAT-MCVALE. **C** (R007–R009) es del juez de consigna.

<a id="F-JEXAM-R001"></a>
### Rule[F-JEXAM-R001] - Cada versión de un juez rinde el examen fijo y su resultado queda a la vista
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Pedirle a un juez que rinda el examen juzga, con su versión vigente, los casos del examen fijo que le tocan, tal como
> están en la foto del 29/9 y leídos igual que en el curso. Invariantes:
> 1. queda guardado para esa versión cuántos reprobó y aprobó por grupo, cuáles no dieron lo esperado y si alcanzó su meta;
> 2. junto a los números del juez, el informe declara el resultado del examen de la versión consultada y de cada versión
>    que aporta veredictos: aprobada, reprobada o sin examen.

Son 238: **a**, las verduras (`67fab6d59930102295341ecd`); **b**, 188 imposibles del 077 (187 CLOZE, uno de opción
múltiple); **c**, 50 buenos (38 CLOZE, 12 de opción múltiple). Lo que le toca a cada juez: [F-JEXAM-R009](#F-JEXAM-R009),
[F-TRSEN-R006](#F-TRSEN-R006), [F-UNICA-R006](#F-UNICA-R006), [F-MINIT-R006](#F-MINIT-R006), [F-MCVALE-R006](#F-MCVALE-R006).

<a id="F-JEXAM-R002"></a>
### Rule[F-JEXAM-R002] - El examen no toca el curso y no paga dos veces lo mismo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Ningún veredicto del examen queda como veredicto del curso: `analyze` da lo mismo antes y después de rendir, y rendir
>    no gasta el tope de ninguna corrida.
> 2. El examen reusa un veredicto sólo si lo emitió esa misma versión sobre el mismo contenido: una versión que ya rindió
>    vuelve a rendir sin consultar.

En el curso gana el más reciente ([F-EVCOST-R006](#F-EVCOST-R006)): si el examen escribiera ahí, una versión reprobada
quedaría vigente en 238 ejercicios. En el examen la versión es lo que se mide; el libro del curso no cambia.

<a id="F-JEXAM-R003"></a>
### Rule[F-JEXAM-R003] - Una versión sin examen aprobado no consulta sobre el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Mientras la versión vigente de un juez nuevo no aprobó su examen, ninguna corrida sobre el curso le hace consultas, con
> cualquier tope: reusa lo registrado, deja sin evaluar lo que falta y el informe dice por qué (sin examen o reprobada).

Es el «rinde un examen fijo antes de usarse» de la propuesta. Mantiene [F-QINST-R004](#F-QINST-R004) y
[F-QINST-R007](#F-QINST-R007). Para el de consigna: [DOUBT-CONSIGNA-HABILITA](#DOUBT-CONSIGNA-HABILITA).

<a id="F-JEXAM-R004"></a>
### Rule[F-JEXAM-R004] - Cada juez dice cuánto le falta y cuánto cuesta, sin consultar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El informe de `analyze` y `stats analyzer <nombre>` —que usa sólo lo registrado— declaran, para cada juez y sin ninguna
> consulta, cuántos ejercicios o temas quedan sin veredicto para lo que lee hoy, cuántas consultas cuesta juzgarlos y
> cuántas horas lleva al ritmo medido del juez, de a una y con las que se pidan a la vez.

Ritmo: mediana entre veredictos seguidos; sin historia, 189 por hora, el del juez de consigna en 15.351 veredictos del
3 al 14/8. Mantiene la decisión del 30/9 sobre `stats` y [F-HALL-R008](#F-HALL-R008). **Criterio**: hoy el de consigna
declararía unos 7.400 pendientes y 39 horas.

<a id="F-JEXAM-R005"></a>
### Rule[F-JEXAM-R005] - A todo juez se le puede pedir un conjunto del curso o un candidato de la mesa
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Lo que [F-QINST-R018](#F-QINST-R018) y [F-QINST-R019](#F-QINST-R019) fijan para el de consigna vale para todo juez,
>    por su nombre: juzga el conjunto declarado —ejercicios, o temas en el de miniteoría— hasta su tope y ninguno más.
> 2. Un candidato de la mesa se juzga con el mismo juez y versión, sin aparecer en el informe del curso; su veredicto
>    queda por la huella de lo que el juez lee y, si el candidato entra al curso, se reusa sin consultar.

Lo usan el examen, la medición sobre `escribir/` y la mesa (paquete E). La 2 lleva [F-QICOR-R005](#F-QICOR-R005) y
[F-QICOR-R015](#F-QICOR-R015) a todo juez: se verifica con el criterio que detectó, sin pagar dos veces.

<a id="F-JEXAM-R006"></a>
### Rule[F-JEXAM-R006] - Consultas a la vez, sin pasarse del tope
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una corrida puede tener hasta N consultas a un juez en curso a la vez. Invariantes:
> 1. consulta los mismos ejercicios que consultaría de a una, y nunca más que su tope;
> 2. tras una falla del servicio no empieza ninguna más; las que estaban en curso se registran si el juez se pronunció;
> 3. con el mismo curso y los mismos veredictos, hallazgos y números son los mismos con cualquier N.

Hoy van de a una. Mantiene [F-EVCOST-R004](#F-EVCOST-R004), [F-EVCOST-R009](#F-EVCOST-R009) y [F-HALL-R014](#F-HALL-R014).

<a id="F-JEXAM-R007"></a>
### Rule[F-JEXAM-R007] - Lo que lee el juez de consigna no cambia
**Severity**: critical | **Validation**: VALIDATED

> El juez de consigna lee lo mismo que hoy —nivel, topic, etiqueta e instrucciones del tema, y el ejercicio completo con
> sus opciones si es de opción múltiple— y nunca la [traducción](glossary:Traducción) ni la miniteoría. Con el mismo curso
> y el mismo libro reusa los mismos veredictos antes y después de este requirement, y sobre un curso juzgado no consulta.

Mantiene [F-QINST-R009](#F-QINST-R009) y [F-EVCOST-R002](#F-EVCOST-R002); la traducción la juzga FEAT-TRSEN. Medido: de
12.060 huellas, sobre la base del 29/9 conservan veredicto unos 3.900 CLOZE y ya están pendientes 7.400 (2.810 de opción
múltiple y 4.590 CLOZE que cambiaron con la fusión del 26/9). Tocar la huella hoy costaría 3.900 consultas, no 13.000.

<a id="F-JEXAM-R008"></a>
### Rule[F-JEXAM-R008] - El juez de consigna muestra su veredicto entero
**Severity**: major | **Validation**: AUTO_VALIDATED

> El hallazgo de un ejercicio que incumple trae en su evidencia, además de cada violación, cada restricción revisada que
> pasó, con su estado, y la confianza; un ejercicio que cumple no tiene hallazgo y muestra en su detalle lo revisado.

Los datos ya están en el diagnóstico ([F-QINST-R003](#F-QINST-R003)) y en cada veredicto guardado; faltan en el hallazgo
y en las vistas. Mantiene [F-HALL-R001](#F-HALL-R001) inv. 2. La confianza no toca el puntaje.

<a id="F-JEXAM-R009"></a>
### Rule[F-JEXAM-R009] - El examen del juez de consigna
**Severity**: critical | **Validation**: ASSUMPTION

> El juez de consigna rinde los grupos a, b y c, y aprueba si reprueba las verduras, reprueba al menos 170 de los 188
> imposibles y no reprueba más de 2 de los 50 buenos.

Meta propuesta. Hoy aprueba 178 de los 187 imposibles CLOZE: los deja pasar. Leído por identificador (3 al 14/8), el
libro da «cumple» en las verduras, en 181 de 187 imposibles CLOZE y en los 50 buenos. En `escribir/consigna.json` (130:
A1 61, A2 69) da «cumple» en los 130; meta, encontrar 78.

## Contexto

Juzgar el curso entero con los cinco son unas 41.700 consultas: 220 horas de a una al ritmo medido. Después de una tanda
se paga sólo lo que cambió: cuatro consultas por ejercicio tocado y una por tema.

| Juez | Unidad | Curso entero | Horas de a una |
|---|---|---:|---:|
| Traducción y sentido (dos jueces) | ejercicio | 22.574 | 119 |
| Una sola respuesta (CLOZE) | ejercicio | 8.477 | 45 |
| Opción múltiple: otra vale | ejercicio | 2.810 | 15 |
| Miniteoría | tema | 413 | unas 7 |
| Consigna (lo pendiente) | ejercicio | unos 7.400 | 39 |

## Alcance

- **Adentro**: examen por versión; costo pendiente, conjunto, candidato y consultas a la vez para todo juez; veredicto
  entero y examen del juez de consigna.
- **Afuera**: qué pregunta cada juez nuevo; armar el examen y medir sobre `examinar/` (0.4); mesa y tandas (E);
  pantallas (G); cambiar lo que lee el juez de consigna o su guía.

## User Journeys

### Journey[F-JEXAM-J001] - Rendir el examen y juzgar el curso con esa versión
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-JEXAM-J001
    name: Rendir el examen y juzgar el curso con esa versión
    flow:
      - id: rendir
        action: "El operador pide que un juez rinda el examen fijo; lo que esa versión ya juzgó se reusa y el resto se consulta"
        gate: [F-JEXAM-R001, F-JEXAM-R002]
        then: corrida
      - id: corrida
        action: "El operador corre analyze sobre el curso pidiendo ese juez con un tope"
        outcomes:
          - when: "La versión aprobó su examen"
            then: consulta
          - when: "La versión no rindió o reprobó"
            then: solo_reusa
      - id: consulta
        action: "La corrida reusa lo registrado, consulta hasta el tope y declara la versión, su examen, cuánto falta y cuánto cuesta"
        gate: [F-JEXAM-R001, F-JEXAM-R004, F-JEXAM-R006]
        result: success
      - id: solo_reusa
        action: "La corrida no consulta, reusa lo registrado y declara que la versión no está habilitada y cuánto falta"
        gate: [F-JEXAM-R003, F-JEXAM-R004]
        result: failure
```

## Open Questions

<a id="DOUBT-JUECES-PROVEEDOR"></a>
### Doubt[DOUBT-JUECES-PROVEEDOR] - ¿Con qué proveedor corren los cinco jueces?
- [ ] Opción A (recomendada): `claude-cli`, la sesión local que juzgó los 15.351 veredictos: sin cobro por consulta, con
  cuota; su sesión venció el 30/9.
- [ ] Opción B: un servicio pagado por consulta de los que el sistema ya usa ([F-QINST-R016](#F-QINST-R016)).
- [ ] Opción C: un proveedor distinto para cada juez de traducción y sentido, para que no se equivoquen igual.

**Answer**: Pendiente. Recomiendo A, que ya funcionó; C se decide con el examen en la mano.

<a id="DOUBT-JUECES-PRESUPUESTO"></a>
### Doubt[DOUBT-JUECES-PRESUPUESTO] - ¿Qué tope por corrida tienen los jueces nuevos, y cuánto se gasta en total?
- [ ] Opción A (recomendada): 0 por omisión: `analyze` reusa y dice cuánto falta; pagar se pide con `--budget nombre=N`.
- [ ] Opción B: 500 por juez, como el de consigna: un `analyze` sin pedir nada paga hasta 2.500 consultas (13 horas).
- [ ] Opción C: sin tope.

**Answer**: Pendiente. Recomiendo A; el total está en el Contexto. Contesta DOUBT-PRESUPUESTO-GENERAL de FEAT-EVCOST: ya
hay segundo consumidor, y el tope se pide por nombre, como en FEAT-HALL.

<a id="DOUBT-JUECES-MUESTRA"></a>
### Doubt[DOUBT-JUECES-MUESTRA] - ¿Se juzga el curso entero o primero una parte?
- [ ] Opción A (recomendada): examen fijo, después `escribir/` y después A1, la primera tanda: unas 7.300 consultas de los
  jueces nuevos más lo pendiente de consigna en A1.
- [ ] Opción B: el curso entero de una vez.
- [ ] Opción C: 400 ejercicios al azar, como la muestra ciega de la propuesta.

**Answer**: Pendiente. Recomiendo A: sigue el orden de las tandas y no se paga nada antes del examen.

<a id="DOUBT-JUECES-PARALELO"></a>
### Doubt[DOUBT-JUECES-PARALELO] - ¿Cuántas consultas a la vez?
- [ ] Opción A: de a una, como hoy (el curso entero en unas 220 horas).
- [ ] Opción B (recomendada): 4 a la vez (unas 55 horas).
- [ ] Opción C: 8 a la vez (unas 28 horas), con más riesgo de cortes por cuota.

**Answer**: Pendiente. Recomiendo B, probado primero con el examen fijo; si la cuota corta, se baja a 2.

<a id="DOUBT-CONSIGNA-HABILITA"></a>
### Doubt[DOUBT-CONSIGNA-HABILITA] - ¿El examen habilita también al juez de consigna, y qué pasa con lo que ya juzgó?
- [ ] Opción A (recomendada): habilita: con la versión de hoy no se pagan los 7.400 pendientes hasta que una guía
  apruebe; los 3.900 reusables siguen valiendo, con su versión reprobada a la vista, y se re-evalúan por conjunto en cada
  tanda ([F-QINST-R018](#F-QINST-R018)).
- [ ] Opción B: sólo informa, y sigue corriendo por omisión con tope 500 ([F-QINST-R001](#F-QINST-R001)).
- [ ] Opción C: habilita y además reabre DOUBT-IDENTIDAD-VERSION, con la versión en la identidad.

**Answer**: Pendiente. Recomiendo A: 39 horas con una versión que deja pasar 178 de 187 imposibles son 39 horas perdidas,
y C vuelve pendientes los 3.900 reusables.

<a id="DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO"></a>
### Doubt[DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO] - ¿Qué consigna juzga el juez de consigna?
La app muestra la consigna de la copia del ejercicio; el juez lee la del tema ([F-QINST-R009](#F-QINST-R009)). En la base
del 29/9 difieren en 9.923 de 11.287 ejercicios.

- [ ] Opción A (recomendada): igualar la consigna de cada tema a la publicada, arreglo de datos fuera del paquete C: el
  juez no cambia y lo afectado queda pendiente, como corresponde.
- [ ] Opción B: que el juez lea la copia: cambia lo que lee y choca con F-QINST-R009 y [F-JEXAM-R007](#F-JEXAM-R007).
- [ ] Opción C: dejarlo, y juzgar una consigna que el alumno no ve.

**Answer**: Pendiente. Recomiendo A.

## References

- **FEAT-QINST** — El juez de consigna: diagnóstico, cobertura, tope, fallas, lo que lee, conjunto y proveedor.
- **FEAT-EVCOST** — Libro de evaluaciones: huella, reuso, escritura a la vez y fallas. Citado por R002, R006 y R007.
- **FEAT-HALL** — Hallazgo, conteos, identidad y las decisiones del 30/9. Citado por R004, R006 y R008.
- **FEAT-QICOR** — Verificar con el mismo criterio y criterios que se consultan. Citado por R005.
- **FEAT-TRSEN, FEAT-UNICA, FEAT-MINIT, FEAT-MCVALE** — Los jueces nuevos, que usan A y B. Citados por R001.
