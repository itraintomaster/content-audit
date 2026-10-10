---
feature:
  id: FEAT-JEXAM
  code: F-JEXAM
  name: El examen fijo de los jueces y el juez de consigna, sin tocar su huella
  priority: critical
---

# El examen fijo de los jueces y el juez de consigna, sin tocar su huella

## TL;DR

**Qué**: Todo juez con modelo rinde un examen fijo por versión y runtime efectivo antes de juzgar el curso; cobertura y
examen se muestran separados, sin mezclar ni pisar los resultados de Claude, Codex u otros runtimes.

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
pendiente en la cobertura; conjunto, candidato y consultas a la vez para todo juez; ninguna consulta sin pedido y un
aviso claro si la sesión del proveedor venció; y el veredicto entero del de consigna. Lo que lee el juez de consigna no
cambia.

## Reglas de Negocio

**A**, el examen (R001–R003), y **B**, el uso (R004–R006 y R010–R014), valen para los cinco jueces: consigna,
FEAT-TRSEN, FEAT-UNICA, FEAT-MINIT y FEAT-MCVALE (R012, sólo para los cuatro nuevos). **C** (R007–R009) es del juez de
consigna.

<a id="F-JEXAM-R001"></a>
### Rule[F-JEXAM-R001] - Cada versión y runtime de un juez rinden su propio examen
**Severity**: critical | **Validation**: VALIDATED

> Pedirle a un juez que rinda el examen juzga, con su versión vigente, los ejercicios del archivo del examen —que trae
> sólo los ejercicios, nunca lo esperado—, tal como están en la foto del 29/9 y leídos igual que en el curso, y deja un
> veredicto por ejercicio para que se mida afuera. Invariantes:
> 1. el resultado medido afuera queda guardado para la identidad examinada —versión del proceso más proveedor, modelo y
>    esfuerzo efectivos—: cuántos reprobó y aprobó por grupo, cuáles no dieron lo esperado y si alcanzó su meta; sólo se
>    acepta para esa identidad si fue la que rindió;
> 2. junto a los números del juez, el informe declara por separado la cobertura del análisis y el resultado del examen de
>    cada runtime y versión que aporta veredictos: aprobada, reprobada o sin examen.

Son 238: **a**, las verduras (`67fab6d59930102295341ecd`); **b**, 188 imposibles del 077 (187 CLOZE, uno de opción
múltiple); **c**, 50 buenos (38 CLOZE, 12 de opción múltiple). Lo que le toca a cada juez: [F-JEXAM-R009](#F-JEXAM-R009),
[F-TRSEN-R006](#F-TRSEN-R006), [F-UNICA-R006](#F-UNICA-R006), [F-MINIT-R006](#F-MINIT-R006), [F-MCVALE-R006](#F-MCVALE-R006).
El oro y los grupos los tiene quien mide, no content-audit: el archivo de entrada no dice qué se espera de cada ejercicio,
así el juez no puede aprenderlo y el examen no cambia aunque cambie el curso.

<a id="F-JEXAM-R002"></a>
### Rule[F-JEXAM-R002] - El examen no toca el curso y no paga dos veces lo mismo
**Severity**: critical | **Validation**: VALIDATED

> 1. Ningún veredicto del examen queda como veredicto del curso: `analyze` da lo mismo antes y después de rendir, y rendir
>    no gasta el tope de ninguna corrida.
> 2. El examen reusa un veredicto sólo si lo emitió la misma identidad examinada —versión del proceso, proveedor, modelo y
>    esfuerzo de razonamiento efectivos— sobre el mismo contenido. Un registro de otro runtime o legacy sin procedencia
>    permanece visible, pero no evita una consulta del examen actual.

En el curso gana el más reciente **dentro del runtime elegido** ([F-EVCOST-R006](#F-EVCOST-R006)): si el examen escribiera
ahí, una identidad reprobada quedaría vigente en 238 ejercicios. El libro del curso no cambia y Claude, Codex y cualquier
otro runtime conservan libros de examen comparables sin prestarse veredictos.

<a id="F-JEXAM-R003"></a>
### Rule[F-JEXAM-R003] - Un runtime y versión sin examen aprobado no consultan sobre el curso
**Severity**: critical | **Validation**: VALIDATED

> Mientras la identidad vigente de un juez —versión del proceso, proveedor, modelo y esfuerzo efectivos— no aprobó su
> examen, ninguna corrida sobre el curso le hace consultas, con cualquier tope: reusa sólo lo registrado para ese runtime,
> deja sin evaluar lo que falta y dice por qué (sin examen o reprobada).

Es el «rinde un examen fijo antes de usarse» de la propuesta. Mantiene [F-QINST-R004](#F-QINST-R004) y
[F-QINST-R007](#F-QINST-R007). Vale también para el de consigna (José, 30/9: «sin aprobar, no paga»,
[DOUBT-CONSIGNA-HABILITA](#DOUBT-CONSIGNA-HABILITA)): sus veredictos ya registrados se siguen reusando sólo cuando su
runtime efectivo coincide, con la identidad reprobada a la vista.

**Cobertura no es examen.** La cobertura responde cuántos ejercicios o temas tienen
veredicto reutilizable para el runtime elegido; el examen responde si esa identidad
está habilitada para pagar consultas nuevas. Un juez puede tener cobertura parcial o
completa y examen reprobado, o examen aprobado y cobertura todavía incompleta. La UI
muestra ambos estados por separado y por runtime; cuando existen resultados
atribuibles a Claude y Codex, los presenta lado a lado. Los registros legacy sin
procedencia aparecen en una categoría histórica propia y no se muestran como
Claude ni como Codex.

<a id="F-JEXAM-R004"></a>
### Rule[F-JEXAM-R004] - Cada juez dice cuánto le falta y cuánto cuesta, sin consultar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El informe de `analyze` y `stats analyzer <nombre>` —que usa sólo lo registrado— declaran, para cada juez y runtime
> efectivo, cuántos ejercicios o temas quedan sin veredicto reutilizable para lo que lee hoy, cuántas consultas cuesta
> juzgarlos y cuántas horas lleva al ritmo medido del juez, de a una y con las que se pidan a la vez.

Ritmo: el promedio entre veredictos seguidos dentro de una misma corrida (se corta en pausas de más de 10 minutos); sin
historia, 189 por hora (el de miniteoría, que lee temas enteros, 60 por hora). Sobre los 15.351 veredictos del juez de
consigna del 3 al 14/8 el promedio da 20,4 s, 176 por hora; la mediana, 15,5 s, subestima las horas porque las consultas
lentas son pocas pero largas. A la vez: las que se pidan, 4 por omisión
([F-JEXAM-R006](#F-JEXAM-R006)). Mantiene la decisión del 30/9 sobre `stats` y [F-HALL-R008](#F-HALL-R008).
**Criterio**: hoy el de consigna declararía unos 7.400 pendientes y 39 horas.

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

Hoy van de a una. N es 4 por omisión (José, 30/9); si la cuota corta, se baja sin tocar nada más. Mantiene
[F-EVCOST-R004](#F-EVCOST-R004), [F-EVCOST-R009](#F-EVCOST-R009) y [F-HALL-R014](#F-HALL-R014).

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

<a id="F-JEXAM-R010"></a>
### Rule[F-JEXAM-R010] - Sin pedido explícito, ningún juez consulta
**Severity**: critical | **Validation**: VALIDATED

> Una corrida que no le pide tope a un juez no le hace ninguna consulta nueva: reusa lo registrado, deja sin evaluar lo
> que falta y declara cuánto costaría ([F-JEXAM-R004](#F-JEXAM-R004)). Vale para los cinco, el de consigna incluido,
> cuyo tope por omisión pasa de 500 a 0.

José, 30/9 ([DOUBT-JUECES-PRESUPUESTO](#DOUBT-JUECES-PRESUPUESTO)): «sin pedido explícito no se paga nada», sobre la
diapositiva que ponía «500 por juez: hasta 2.500» como alternativa. Contesta DOUBT-PRESUPUESTO-DEFECTO de FEAT-QINST y
DOUBT-PRESUPUESTO-GENERAL de FEAT-EVCOST. Pagar se pide por nombre, con el tope de FEAT-HALL. La verificación de
candidatos de `revise-instructions` ya es un pedido explícito y conserva su tope ([F-QICOR-R005](#F-QICOR-R005)).
**Criterio**: `analyze` sin tope sobre la base del 29/9 no consulta ninguna vez y el de consigna declara unos 7.400
pendientes.

<a id="F-JEXAM-R011"></a>
### Rule[F-JEXAM-R011] - Con la sesión del proveedor vencida, el juez no consulta y el informe dice cómo renovarla
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Antes de la primera consulta de una corrida, el juez verifica sin costo que su proveedor tenga sesión. Si no la tiene,
> o si la primera consulta vuelve por falta de sesión, la corrida no empieza ninguna consulta más, termina sin error y
> el informe del juez nombra la causa —sesión vencida, cuota agotada o servicio caído— y cómo se resuelve.

José eligió `codex-cli`, autenticado con la sesión local de ChatGPT
([DOUBT-JUECES-PROVEEDOR](#DOUBT-JUECES-PROVEEDOR)). Mantiene [F-QINST-R007](#F-QINST-R007) y
[F-EVCOST-R009](#F-EVCOST-R009): una falla del servicio corta las
consultas, no la corrida; esta regla agrega que se sepa por qué. Con consultas a la vez ([F-JEXAM-R006](#F-JEXAM-R006))
la primera sale sola: si el servicio no está, falla una y no cuatro. **Criterio**: con la sesión vencida y tope 100, la
corrida hace a lo sumo una consulta, deja todo pendiente y el informe dice «sesión vencida: renovala con
`codex login`».

<a id="F-JEXAM-R012"></a>
### Rule[F-JEXAM-R012] - Los jueces nuevos no tratan como problema una forma que el corrector acepta
**Severity**: critical | **Validation**: VALIDATED

> Ningún juez nuevo lista como otra respuesta, otra opción válida o problema una diferencia que el corrector acepta
> —forma contraída o larga, grafía británica o americana de la lista cerrada, until o till—, ni sugiere en su evidencia
> una consigna que pida una forma. Que la consigna o la miniteoría pidan una forma lo marca el paquete A; los jueces no
> lo repiten.

José, 30/9: «No quiero más ejercicios cuyas instrucciones pidan short-form o long-form. Que acepte siempre ambas.» La
tabla de equivalencias es una sola: la del corrector de Go, con sus casos de oro, que content-audit imita (paquete A).
Donde el juez lista respuestas u opciones ([F-UNICA-R002](#F-UNICA-R002), [F-MCVALE-R002](#F-MCVALE-R002)), lo que
el corrector acepta se descarta aunque el juez lo liste. La guía del juez de consigna no cambia ([F-JEXAM-R007](#F-JEXAM-R007)).
**Criterio**: un ejercicio que acepta «isn't» y cuyo único reparo sería «is not» no tiene hallazgo de `single-answer`.

<a id="F-JEXAM-R013"></a>
### Rule[F-JEXAM-R013] - Codex y ChatGPT son la selección actual, y el operador puede elegir otra
**Severity**: critical | **Validation**: VALIDATED

> Todo proceso que hasta ahora consultaba a Claude mediante la sesión local —los cinco jueces, las alternativas de
> Naturalidad y Duplicados parecidos, los generadores de propuestas y los agentes de la mesa— usa por omisión
> `codex-cli`, autenticado con la sesión local de ChatGPT. El operador puede elegir explícitamente otro proveedor que el
> sistema sepa consultar, el modelo y el esfuerzo que correspondan; una corrida nunca cambia sola de proveedor, modelo
> o esfuerzo.

José reemplazó el 4/10 la decisión del 30/9 porque dispone de más cupo en ChatGPT. Los modelos locales que un proceso
elige deliberadamente —por ejemplo, los que corren en Ollama— no cambian por esta regla. Toda mención anterior a
`claude-cli` o `claude -p` como proveedor por omisión queda reemplazada por esta decisión; conserva valor histórico
cuando describe una medición o una corrida pasada. La selección por omisión no usa una clave de OpenAI API ni genera
facturación por API. Si la sesión de ChatGPT no está disponible, aplica [F-JEXAM-R011](#F-JEXAM-R011): el proceso no
deriva a otra vía de cobro ni a otro proveedor. Una vía con clave, cobro o sesión distinta sólo se usa cuando el
operador la selecciona expresamente.

<a id="F-JEXAM-R014"></a>
### Rule[F-JEXAM-R014] - Runtime efectivo y versión forman una identidad examinable
**Severity**: critical | **Validation**: VALIDATED

> Cada resultado producido por un modelo identifica el proveedor realmente elegido,
> el modelo exacto, el esfuerzo de razonamiento efectivo y la versión completa del
> proceso que lo obtuvo. Esas cuatro piezas forman la identidad examinable: cambiar
> cualquiera exige un examen propio antes de consultar el curso.

Si un proveedor no ofrece esfuerzo configurable, la identidad registra
explícitamente "no aplica"; no deja el dato ausente ni lo convierte en comodín.

El runtime efectivo es proveedor + modelo + esfuerzo. Sus veredictos sólo se
reutilizan dentro de esa combinación ([F-EVCOST-R001](#F-EVCOST-R001)); la versión
del proceso, además, determina qué examen habilita consultas nuevas. Por eso una
aprobación con Claude no aprueba Codex, y una aprobación con un esfuerzo no aprueba
otro. Los resultados y exámenes de cada identidad conviven y se comparan lado a
lado: registrar Codex no pisa Claude.

Los veredictos, propuestas y exámenes anteriores no se borran ni se atribuyen a la
identidad nueva. Si un registro legacy no declara proveedor, modelo o esfuerzo,
queda visible como "procedencia desconocida", pero no habilita, no cubre y no se
presenta como resultado de un runtime concreto. Esta regla resuelve para las
propuestas la granularidad pendiente de
[F-LAGEN-R009](../2026-04-29.01_laps-llm-quiz-candidate-generator/REQUIREMENT.md#F-LAGEN-R009) y, para los jueces, refuerza
[F-JEXAM-R001](#F-JEXAM-R001) y [F-JEXAM-R003](#F-JEXAM-R003).

<a id="F-JEXAM-R015"></a>
### Rule[F-JEXAM-R015] - El historial de los exámenes se lee en JSON, sin consultar
**Severity**: normal | **Validation**: VALIDATED

> `exam status -f json` da el mismo historial que `exam status`, para que el tablero lo dibuje sin
> leer el registro por su cuenta. Con un juez, ese juez; sin juez, cada analizador del catálogo que
> tiene registros o rinde el examen, en el orden del catálogo.

Cada juez trae sus versiones, una por versión y runtime ([F-JEXAM-R014](#F-JEXAM-R014)), en el orden
en que rindieron por primera vez; cada versión trae su estado, que es su último registro, la
procedencia escrita como la escribe `exam status`, y todos sus registros con sus grupos, los casos que
no dieron lo esperado, la meta, la foto y quién midió. Un juez que rinde el examen y nunca lo rindió
aparece sin versiones. Leer el historial no consulta a ningún juez ni escribe nada.

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

- **Adentro**: examen por versión y runtime efectivo; costo y cobertura pendientes por runtime; presentación separada de
  cobertura y examen; comparación lado a lado sin mezclar Claude, Codex, otros runtimes ni legacy; conjunto, candidato,
  consultas a la vez, tope 0 por omisión, sesión vencida y formas que el corrector acepta para todo juez; procedencia para
  todo proceso con modelo; veredicto entero y examen del juez de consigna.
- **Afuera**: qué pregunta cada juez nuevo; armar el examen, su oro y medir sobre `examinar/` (0.4); mesa y tandas (E);
  otras pantallas de operación (G); cambiar lo que lee el juez de consigna o su guía; igualar la consigna del tema con la del alumno (un
  arreglo de datos, [DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO](#DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO)); la tabla de equivalencias del
  corrector (paquete A).

## User Journeys

### Journey[F-JEXAM-J001] - Rendir el examen y juzgar el curso con esa versión
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-JEXAM-J001
    name: Rendir el examen y juzgar el curso con esa versión
    flow:
      - id: rendir
        action: "El operador pide que un juez rinda el examen con el archivo de ejercicios; lo que esa versión ya juzgó se reusa, el resto se consulta y queda un veredicto por ejercicio"
        gate: [F-JEXAM-R001, F-JEXAM-R002]
        then: registrar
      - id: registrar
        action: "El operador registra para esa versión el resultado que se midió afuera"
        gate: [F-JEXAM-R001]
        then: corrida
      - id: corrida
        action: "El operador corre analyze sobre el curso pidiendo ese juez con un tope"
        outcomes:
          - when: "La versión aprobó su examen"
            then: consulta
          - when: "La versión no rindió o reprobó"
            then: solo_reusa
      - id: consulta
        action: "La corrida reusa sólo veredictos del runtime elegido, consulta hasta el tope y muestra por separado su cobertura, su examen, cuánto falta y cuánto cuesta"
        gate: [F-JEXAM-R001, F-JEXAM-R004, F-JEXAM-R006, F-JEXAM-R014]
        result: success
      - id: solo_reusa
        action: "La corrida no consulta, reusa sólo lo compatible con el runtime elegido y declara por separado que su identidad no está habilitada y qué cobertura le falta"
        gate: [F-JEXAM-R003, F-JEXAM-R004, F-JEXAM-R014]
        result: failure
```

## Open Questions

<a id="DOUBT-JUECES-PROVEEDOR"></a>
### Doubt[DOUBT-JUECES-PROVEEDOR] - ¿Con qué proveedor corren los cinco jueces?
- [x] Opción A (recomendada): `codex-cli`, autenticado con la sesión local de ChatGPT: consume el cupo de la
  suscripción, no una clave ni facturación de OpenAI API.
- [ ] Opción B: `claude-cli`, la sesión local elegida el 30/9 y reemplazada como valor por omisión.
- [ ] Opción C: un servicio por API, con clave y facturación separada, elegido explícitamente por el operador.

**Status**: RESOLVED (2026-10-04)

**Answer**: **Opción A** (José, 4/10): OpenAI mediante `codex-cli` y la sesión local de ChatGPT, porque allí dispone de
más cupo. Es la selección actual y por omisión, no la OpenAI API por clave ni por consulta; el operador conserva la
posibilidad de elegir expresamente B, C u otro proveedor soportado. Reemplaza la elección de `claude-cli` del 30/9 y
alcanza a los demás procesos con modelo que compartían esa sesión ([F-JEXAM-R013](#F-JEXAM-R013)); las versiones
afectadas vuelven a rendir examen antes de consultar el curso ([F-JEXAM-R014](#F-JEXAM-R014)).

<a id="DOUBT-JUECES-MODELO-OPENAI"></a>
### Doubt[DOUBT-JUECES-MODELO-OPENAI] - ¿Cómo elige el operador el modelo de cada proceso?
- [x] Opción A (recomendada): declarar un modelo compartido y, donde las perillas existentes lo permitan, reemplazarlo
  explícitamente por proceso.
- [ ] Opción B: obligar a usar un único modelo compartido para todos los procesos.
- [ ] Opción C: usar el modelo por omisión de Codex sin registrarlo expresamente.

**Status**: RESOLVED (2026-10-04)

**Answer**: **Opción A** (José, 4/10): quiere poder elegir. El valor compartido evita repetir configuración y la
selección por proceso, cuando ya existe esa perilla, permite ajustar cada trabajo. En ambos casos, el modelo finalmente
resuelto queda en la procedencia y un cambio obliga a rendir el examen aplicable ([F-JEXAM-R014](#F-JEXAM-R014)). C
contradice esa trazabilidad.

<a id="DOUBT-JUECES-PRESUPUESTO"></a>
### Doubt[DOUBT-JUECES-PRESUPUESTO] - ¿Qué tope por corrida tienen los jueces nuevos, y cuánto se gasta en total?
- [x] Opción A (recomendada): 0 por omisión: `analyze` reusa y dice cuánto falta; pagar se pide con `--budget nombre=N`.
- [ ] Opción B: 500 por juez, como el de consigna: un `analyze` sin pedir nada paga hasta 2.500 consultas (13 horas).
- [ ] Opción C: sin tope.

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción A** (José, 30/9): «sin pedido explícito no se paga nada», para los cinco, el de consigna incluido
([F-JEXAM-R010](#F-JEXAM-R010)). Contesta DOUBT-PRESUPUESTO-GENERAL de FEAT-EVCOST y DOUBT-PRESUPUESTO-DEFECTO de
FEAT-QINST; el total está en el Contexto.

<a id="DOUBT-JUECES-MUESTRA"></a>
### Doubt[DOUBT-JUECES-MUESTRA] - ¿Se juzga el curso entero o primero una parte?
- [x] Opción A (recomendada): examen fijo, después `escribir/` y después A1, la primera tanda: unas 7.300 consultas de los
  jueces nuevos más lo pendiente de consigna en A1.
- [ ] Opción B: el curso entero de una vez.
- [ ] Opción C: 400 ejercicios al azar, como la muestra ciega de la propuesta.

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción A** (José, 30/9): examen fijo, `escribir/` y A1. Es un orden de trabajo, no una regla del sistema:
cada paso es un pedido explícito con su tope ([F-JEXAM-R010](#F-JEXAM-R010)).

<a id="DOUBT-JUECES-PARALELO"></a>
### Doubt[DOUBT-JUECES-PARALELO] - ¿Cuántas consultas a la vez?
- [ ] Opción A: de a una, como hoy (el curso entero en unas 220 horas).
- [x] Opción B (recomendada): 4 a la vez (unas 55 horas).
- [ ] Opción C: 8 a la vez (unas 28 horas), con más riesgo de cortes por cuota.

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción B** (José, 30/9): 4 a la vez por omisión ([F-JEXAM-R006](#F-JEXAM-R006)); si la cuota corta, se baja
a 2 sin tocar nada más.

<a id="DOUBT-CONSIGNA-HABILITA"></a>
### Doubt[DOUBT-CONSIGNA-HABILITA] - ¿El examen habilita también al juez de consigna, y qué pasa con lo que ya juzgó?
- [x] Opción A (recomendada): habilita: con la versión de hoy no se pagan los 7.400 pendientes hasta que una guía
  apruebe; los 3.900 reusables siguen valiendo, con su versión reprobada a la vista, y se re-evalúan por conjunto en cada
  tanda ([F-QINST-R018](#F-QINST-R018)).
- [ ] Opción B: sólo informa, y sigue corriendo por omisión con tope 500 ([F-QINST-R001](#F-QINST-R001)).
- [ ] Opción C: habilita y además reabre DOUBT-IDENTIDAD-VERSION, con la versión en la identidad.

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción A** (José, 30/9): «el examen frena al de consigna: sin aprobar, no paga»
([F-JEXAM-R003](#F-JEXAM-R003)).

<a id="DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO"></a>
### Doubt[DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO] - ¿Qué consigna juzga el juez de consigna?
La app muestra la consigna de la copia del ejercicio; el juez lee la del tema ([F-QINST-R009](#F-QINST-R009)). En la base
del 29/9 difieren en 9.923 de 11.287 ejercicios.

- [x] Opción A (recomendada): igualar la consigna de cada tema a la publicada, arreglo de datos fuera del paquete C: el
  juez no cambia y lo afectado queda pendiente, como corresponde.
- [ ] Opción B: que el juez lea la copia: cambia lo que lee y choca con F-QINST-R009 y [F-JEXAM-R007](#F-JEXAM-R007).
- [ ] Opción C: dejarlo, y juzgar una consigna que el alumno no ve.

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción A** (José, 30/9): se igualan, con un arreglo de datos fuera del paquete C que no toca al juez y no
se aplica ahora. Cuando se aplique, lo que cambie queda pendiente de juzgar, como cualquier cambio de contenido.

<a id="DOUBT-EXAMEN-Y-CANDIDATOS"></a>
### Doubt[DOUBT-EXAMEN-Y-CANDIDATOS] - ¿Un juez sin examen aprobado juzga candidatos?
[F-JEXAM-R003](#F-JEXAM-R003) frena las corridas sobre el curso. Los candidatos se juzgan por otro camino
([F-JEXAM-R005](#F-JEXAM-R005)): la mesa nueva (paquete E) y, hoy, `revise-instructions`, que verifica cada corrección
con el juez de consigna ([F-QICOR-R005](#F-QICOR-R005)).

- [ ] Opción A (recomendada): el freno vale para el curso y para la mesa nueva; `revise-instructions` sigue verificando
  como hoy hasta que la mesa lo reemplace.
- [ ] Opción B: también frena `revise-instructions`: no hay corrección de consigna hasta que una guía apruebe.
- [ ] Opción C: el freno es sólo para el curso; los candidatos se juzgan con cualquier versión.

**Answer**: Pendiente. Recomiendo A: la mesa nueva no debería decidir con un juez reprobado, y B apaga una herramienta
que José usa a pedido, con pocas consultas, antes de que exista su reemplazo.

## References

- **FEAT-QINST** — El juez de consigna: diagnóstico, cobertura, tope, fallas, lo que lee, conjunto y proveedor. Su tope
  por omisión pasa de 500 a 0 por [F-JEXAM-R010](#F-JEXAM-R010).
- **FEAT-EVCOST** — Libro de evaluaciones: huella, reuso, escritura a la vez y fallas. Citado por R002, R006, R007 y R011.
- **FEAT-HALL** — Hallazgo, conteos, identidad y las decisiones del 30/9. Citado por R004, R006, R008 y R010.
- **FEAT-QICOR** — Verificar con el mismo criterio y criterios que se consultan. Citado por R005, R010 y la duda de los
  candidatos.
- **FEAT-TRSEN, FEAT-UNICA, FEAT-MINIT, FEAT-MCVALE** — Los jueces nuevos, que usan A y B. Citados por R001 y R012.
- **FEAT-LAGEN** — La procedencia de las propuestas generadas por modelo. Citada por R014.
