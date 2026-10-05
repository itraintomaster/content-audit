---
feature:
  id: FEAT-MLOCAL
  code: F-MLOCAL
  name: Idioma local - herramientas y modelos que corren en la máquina
  priority: high
---

# Idioma local: herramientas y modelos que corren en la máquina

## TL;DR

**Qué**: Gramática, Naturalidad y Duplicados parecidos corren con su herramienta en la máquina y su versión a la vista;
sin Ollama, los dos de modelo consultan a `codex-cli` con la sesión local de ChatGPT, sólo con tope; los tres leen
candidatos de la mesa.

**Por qué**: Son las primeras dependencias de idioma de content-audit. Sin reglas comunes, cada uno decidiría distinto
qué pasa cuando falta el modelo, cuándo corre y qué cambia cuando cambia la versión.

## Recorrido de uso (estado actual)

Cómo el operador intenta hoy correr un analizador que necesita una herramienta local:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible; hoy la única herramienta
   local es el proceso de spaCy, que tokeniza al cargar el curso.
2. Lista los analizadores y su configuración → `GetCommand.get("analyzers", …)`, `ConfigAnalyzerCommand.showConfig`. ✓
   Disponible, pero ninguna ficha dice con qué herramienta ni con qué versión mide.
3. Corre sin la herramienta instalada → ⚠️ no está definido: el de spaCy corta la corrida con su error.
4. Le pide a un analizador que lea un candidato de la mesa → ⚠️ sólo el juez de consigna lo hace
   (`AssessCandidateCommand.assessCandidate`), y ninguno de los tres existe todavía.
5. Corre sin Ollama → ⚠️ el diseño del 30/9 dejaba `naturalness` y `near-duplicates` sin evaluar. Los jueces ya consultan
   a `codex-cli`, con tope por nombre (`AnalyzeOptions.analyzerBudgets`, `--budget nombre=N`) y examen por versión
   (`ExamCommand.takeExam`), pero sólo para lo suyo.

**Punto de fricción**: pasos 2 a 5.

**Cambio mínimo necesario**: fijar para `grammar`, `naturalness` y `near-duplicates` qué garantizan sobre su herramienta,
su versión, cuándo corren y cómo leen un candidato. Qué herramienta es cada una y dónde corre lo decidió José el 30/9:
LanguageTool dentro de content-audit y los dos modelos que ya están en Ollama. Y, sin Ollama, que los dos de modelo usen
el proveedor por sesión local con el mismo tope y examen que los jueces; desde el 4/10 es `codex-cli`, autenticado con
ChatGPT ([F-JEXAM-R013](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R013)).

## Reglas de Negocio

Acá **herramienta** es LanguageTool (FEAT-GRAMA) o un modelo local (FEAT-NATUR, FEAT-DUPAR), y **la alternativa** es
`codex-cli`, autenticado con la sesión local de ChatGPT igual que los jueces (FEAT-JEXAM). Las menciones a `claude -p`
que siguen documentan el diseño y las mediciones anteriores: como proveedor activo quedan reemplazadas por
[F-JEXAM-R013](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R013), y toda versión afectada vuelve a rendir su examen por
[F-JEXAM-R014](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R014).

<a id="F-MLOCAL-R001"></a>
### Rule[F-MLOCAL-R001] - Con sus herramientas, nada sale de la máquina salvo los pares de Duplicados parecidos; a claude -p, sólo con tope
**Severity**: critical | **Validation**: ASSUMPTION

> 1. Correr `grammar` no manda ningún texto del curso fuera de la máquina ni necesita red.
> 2. Con su modelo local disponible, `naturalness` tampoco: con la red cortada da los mismos hallazgos y números.
>    `near-duplicates`, tampoco sin tope; con tope, manda a claude -p sólo los dos ejercicios de cada par candidato que
>    proponen sus vectores ([F-DUPAR-R009](#F-DUPAR-R009)).
> 3. Sin su modelo local, mandan a claude -p sólo lo que le preguntan —la oración terminada o los dos ejercicios del
>    par—, y sólo dentro del tope que la corrida les pide por su nombre ([F-MLOCAL-R008](#F-MLOCAL-R008)).

Es lo que José aprobó para el paquete B en el brief 089, «sin servicios externos», con la excepción que sumó el 30/9 a la
noche: «sin Ollama no se deja sin evaluar; recordá que tenemos claude -p». LanguageTool tiene un servicio público; no se
usa. claude -p es el mismo proveedor que eligió para los jueces ([DOUBT-JUECES-PROVEEDOR](#DOUBT-JUECES-PROVEEDOR)): no
cobra por consulta, pero gasta cupo de la suscripción y horas.

La excepción de `near-duplicates` en la invariante 2 es del 1/10 [ASSUMPTION]: su versión con el parecido de 0,95
reprobó el examen, y llama3 no sirve de juez local ([F-DUPAR-R009](#F-DUPAR-R009)). José validó el resto de la regla el
30/9; la excepción espera su respuesta en [DOUBT-DUPAR-MLOCAL-EXCEPCION](#DOUBT-DUPAR-MLOCAL-EXCEPCION).

<a id="F-MLOCAL-R002"></a>
### Rule[F-MLOCAL-R002] - Cada herramienta tiene nombre y versión a la vista, y la misma versión da lo mismo
**Severity**: critical | **Validation**: ASSUMPTION

> La ficha de cada uno ([F-HALL-R006](#F-HALL-R006)) y el informe de cada corrida nombran su herramienta con su versión,
> y la alternativa con la suya. Invariantes:
> 1. con la misma versión y el mismo curso, dos corridas dan los mismos hallazgos, en el mismo orden y con los mismos
>    números ([F-HALL-R014](#F-HALL-R014));
> 2. el número que un modelo local le dio a un ejercicio se guarda con su contenido y la versión, y se reusa mientras no
>    cambien, sin volver a consultar al modelo; lo que contestó claude -p se guarda y se reusa como un veredicto de juez,
>    mientras no cambie lo que se le preguntó ([F-EVCOST-R006](#F-EVCOST-R006));
> 3. si un hallazgo cambia sin que cambie su ejercicio, el informe muestra que cambió la versión o la herramienta.

La invariante 2 es la que asegura la 1: con llama3, dos corridas seguidas dieron lo mismo, pero contra una corrida larga
hubo diferencias de hasta 0,025, que en el borde de un umbral cambian una marca. Es el libro de evaluaciones de los
jueces ([F-EVCOST-R003](#F-EVCOST-R003)) sin costo; además, una tanda sólo recalcula lo que tocó [ASSUMPTION]. Con
claude -p cuesta: volver a preguntar lo ya contestado sería pagar dos veces.

<a id="F-MLOCAL-R003"></a>
### Rule[F-MLOCAL-R003] - Si falta la herramienta, se sabe antes de empezar
**Severity**: critical | **Validation**: ASSUMPTION

> 1. Pedido por su nombre, sin su herramienta y sin la alternativa disponible, `analyze` rechaza la corrida antes de
>    empezar, sin escribir informe, con un mensaje que dice qué falta y cómo se instala.
> 2. Pedido por su nombre sin su modelo local pero con la alternativa disponible, no se rechaza: corre con claude -p
>    ([F-MLOCAL-R007](#F-MLOCAL-R007)), dentro de su tope ([F-MLOCAL-R008](#F-MLOCAL-R008)).
> 3. En una corrida sin selección, el analizador sin herramienta ni alternativa queda sin evaluar en todos sus
>    ejercicios, con el motivo a la vista ([F-HALL-R008](#F-HALL-R008)), y los demás corren igual.

**Error**: "Analyzer '<name>' needs <tool>, which is not available: <how to install>."

La alternativa está disponible si su definición y su configuración están —las de los jueces—, aunque la sesión de
claude-cli haya vencido: eso se ve en la primera consulta y no corta la corrida ([F-JEXAM-R011](#F-JEXAM-R011)).

<a id="F-MLOCAL-R004"></a>
### Rule[F-MLOCAL-R004] - Qué corre sin pedirlo
**Severity**: major | **Validation**: VALIDATED

> `analyze` sin selección corre los tres. La corrida base, la que usan la vista consolidada y la previa de impacto,
> corre sólo los de costo *instantáneo* (FEAT-HALL): `grammar` sí; `naturalness` y `near-duplicates`, no. Sin pedido,
> un modelo local consulta sin tope; claude -p, no ([F-MLOCAL-R008](#F-MLOCAL-R008)).

Es la regla del contrato para la corrida base, aplicada a los nuevos; que los tres corran sin pedirlo en `analyze` lo
decidió José el 30/9 ([DOUBT-MLOCAL-POR-OMISION](#DOUBT-MLOCAL-POR-OMISION)). Sin pedirlos, los de modelo local no
tienen tope de consultas: son gratis, y el tope 0 por omisión de los jueces es para lo que consulta a claude -p.

<a id="F-MLOCAL-R005"></a>
### Rule[F-MLOCAL-R005] - Los tres leen un candidato de la mesa y dicen qué cambia
**Severity**: critical | **Validation**: AUTO_VALIDATED

> A pedido, los tres leen un candidato de la mesa como leen un ejercicio del curso —`near-duplicates`, contra los demás
> ejercicios de su tema—, sin que el candidato aparezca en el informe del curso, y dicen qué hallazgos suyos agrega y
> cuáles saca respecto del ejercicio al que reemplaza.

Es lo que [F-JEXAM-R005](#F-JEXAM-R005) pide a los jueces. Si un hallazgo nuevo frena el candidato lo decide el paquete
E («qué significa no empeorar»); acá sólo se garantiza que se sepa: con Ollama, en segundos y gratis, salvo los pares
candidatos nuevos de `near-duplicates`, que juzga claude -p dentro del tope de la mesa ([F-DUPAR-R009](#F-DUPAR-R009));
sin Ollama, con una consulta de claude -p por oración nueva y por par candidato nuevo, dentro del tope de la mesa. Hoy la
mesa de E mide los candidatos sólo con los analizadores instantáneos ([F-MESA-R004](#F-MESA-R004)), que deja afuera a
`naturalness` y `near-duplicates`: [DOUBT-MLOCAL-MESA](#DOUBT-MLOCAL-MESA).

<a id="F-MLOCAL-R006"></a>
### Rule[F-MLOCAL-R006] - Cada versión rinde su examen antes de publicar
**Severity**: major | **Validation**: ASSUMPTION

> Una versión nueva de herramienta —o de la alternativa— publica hallazgos recién después de rendir el examen de su
> analizador (FEAT-GRAMA R007, FEAT-NATUR R006, FEAT-DUPAR R007); su resultado queda junto a la versión, en la ficha.
> Mientras no lo apruebe, esa versión no consulta: sus ejercicios quedan sin evaluar, salvo lo que ya estaba
> registrado, y el informe dice por qué ([F-JEXAM-R003](#F-JEXAM-R003)).

Rendir con la herramienta local es gratis: minutos. Rendir con la alternativa se paga en consultas, como el examen de un
juez, y por el mismo camino (`ExamCommand.takeExam`). Es el «rinde un examen fijo antes de usarse» de los jueces
([F-JEXAM-R003](#F-JEXAM-R003)), y es lo que pide la propuesta para Naturalidad: si no sirve, se saca. Las dos versiones
de un analizador —la local y la de claude -p— rinden por separado; en `near-duplicates`, la local lleva también al juez
de claude -p ([F-DUPAR-R010](#F-DUPAR-R010)).

<a id="F-MLOCAL-R007"></a>
### Rule[F-MLOCAL-R007] - Sin Ollama, Naturalidad y Duplicados parecidos le preguntan lo mismo a claude -p
**Severity**: critical | **Validation**: ASSUMPTION

> Si su modelo local no está disponible, `naturalness` y `near-duplicates` le hacen su pregunta a claude -p, con las
> reglas de los jueces (FEAT-JEXAM). Invariantes:
> 1. con su modelo local disponible, `naturalness` nunca consulta a claude -p; `near-duplicates` le manda sólo los dos
>    ejercicios de cada par candidato que proponen sus vectores, dentro de su tope, y el informe nombra las dos
>    herramientas ([F-DUPAR-R009](#F-DUPAR-R009));
> 2. `naturalness` pregunta lo mismo que al modelo local ([F-NATUR-R008](#F-NATUR-R008)); `near-duplicates` pregunta
>    por los pares de texto parecido que su comodín no resuelve ([F-DUPAR-R008](#F-DUPAR-R008));
> 3. consultan como un juez: hasta 4 a la vez, con la sesión verificada antes de la primera consulta, y una falla del
>    servicio corta las consultas, no la corrida ([F-JEXAM-R006](#F-JEXAM-R006), [F-JEXAM-R011](#F-JEXAM-R011));
> 4. el informe nombra la herramienta que usó cada corrida —el modelo local o claude -p, con su versión— y, si fue
>    claude -p, cuánto falta, cuántas consultas son y cuántas horas lleva, de a una y a la vez ([F-JEXAM-R004](#F-JEXAM-R004)).

<details><summary>Lo decidido y cuánto cuesta</summary>

José, 30/9 a la noche: «sin Ollama no se deja sin evaluar; recordá que tenemos claude -p»
([DOUBT-MLOCAL-SIN-OLLAMA](#DOUBT-MLOCAL-SIN-OLLAMA)). Sin Ollama, Gramática no cambia: LanguageTool corre en el proceso.
La excepción de `near-duplicates` en la invariante 1 es del 1/10 y espera a José [ASSUMPTION]
([DOUBT-DUPAR-MLOCAL-EXCEPCION](#DOUBT-DUPAR-MLOCAL-EXCEPCION)); el resto de la regla lo validó el 30/9.

Lo que costaría consultar a claude -p sobre la base del 29/9, al ritmo medido de los jueces (176 consultas por hora de a
una, FEAT-JEXAM), contado con scripts y sin consultar:

| Analizador | Qué pregunta | Curso entero | A1 | Horas, curso (de a una · 4 a la vez) | Horas, A1 |
|---|---|---:|---:|---|---|
| `naturalness`, sin Ollama | una por oración terminada que alcanza | 11.174 | 2.334 | 63,5 · 15,9 | 13,3 · 3,3 |
| `near-duplicates`, sin Ollama | una por par candidato de texto parecido | 696 | 276 | 4,0 · 1,0 | 1,6 · 0,4 |
| `grammar` | nada | 0 | 0 | — | — |
| **Total, sin Ollama** | | **11.870** | **2.610** | **67,4 · 16,9** | **14,8 · 3,7** |
| `near-duplicates`, con Ollama | una por par candidato de sus vectores, desde 0,80 | 1.468 | 513 | 8,3 · 2,1 | 2,9 · 0,7 |

Por nivel, `naturalness`: A1 2.334, A2 2.404, B1 4.535 y B2 1.901; `near-duplicates`, sin Ollama: A1 276, A2 110, B1 297
y B2 13, y con Ollama: A1 513, A2 200, B1 710 y B2 45. Rendir el examen: 586 consultas para la alternativa de
Naturalidad; para Duplicados parecidos, 699 la versión sin Ollama, sin el filtro del examen
([F-DUPAR-R007](#F-DUPAR-R007)) y sin recontar, y unas 255 como mucho la de Ollama ([F-DUPAR-R007](#F-DUPAR-R007)).
Después de cada tanda se paga sólo lo que cambió: una consulta por ejercicio tocado y una por par candidato nuevo.
Naturalidad cuenta la oración terminada de [F-GRAMA-R001](#F-GRAMA-R001) con su invariante 5 (1/10): «Something is
broken.» alcanza, y por eso A2 son 2.404 y no 2.403.
Para comparar: los cuatro jueces nuevos sobre A1 son 7.289 consultas (FEAT-JEXAM).

</details>

<a id="F-MLOCAL-R008"></a>
### Rule[F-MLOCAL-R008] - La alternativa consulta sólo con un tope pedido, y sin tope dice cuánto costaría
**Severity**: critical | **Validation**: ASSUMPTION

> Lo que consulta a claude -p tiene el tope de los jueces ([F-JEXAM-R010](#F-JEXAM-R010)): sin `--budget <nombre>=N` no
> hace ninguna consulta nueva, reusa lo que claude -p ya contestó y deja lo demás sin evaluar con el motivo «falta el
> tope», distinto de «sin Ollama» y de «sin examen», declarando cuántas consultas y horas costaría. Ese tope nunca
> alcanza al modelo local, que sin pedido consulta sin límite.

Es el «sin pedido explícito no se paga nada» de José para los jueces, que alcanza a todo lo que consulta a claude -p
porque correrlo necesita su OK. Que el tope 0 no frene a los modelos locales es el gancho B-06: si los frenara,
`naturalness` y `near-duplicates` nunca calcularían nada. Duda: [DOUBT-MLOCAL-TOPE-ALTERNATIVA](#DOUBT-MLOCAL-TOPE-ALTERNATIVA).

## Contexto

Lo medido en el prototipo del analista, sobre el curso del 29/9 y en esta Mac, sin instalar nada:

| Analizador | Herramienta medida | Qué hay en la máquina | Tiempo |
|---|---|---|---|
| `grammar` | LanguageTool 6.5 (la copia de IntelliJ); entra la 6.8 | nada propio de content-audit | curso entero, unos 10 s |
| `near-duplicates` | nomic-embed-text, 137M, en Ollama | Ollama y el modelo, ya instalados | curso entero, unos 4 min |
| `naturalness` | llama3 8B (Q4_0) en Ollama, pregunta sí o no | Ollama y el modelo, ya instalados | A1–A2, 6 min; el curso, unos 15 (estimado) |

Ollama no devuelve la probabilidad del texto que recibe, sólo la de lo que genera, con 20 alternativas como mucho. Por
eso el SLOR de la propuesta hubo que medirlo de a un token, y dio casi lo mismo que el azar; preguntarle al modelo sí o
no, en cambio, cabe en una consulta de un token y aprueba el examen (FEAT-NATUR). claude -p tampoco devuelve
probabilidades: contesta sí o no, y por eso la alternativa de Naturalidad marca por la respuesta y no por un número.

## Alcance

- **Adentro**: R001 a R008 para los tres analizadores de idioma, y la alternativa con claude -p para los dos de modelo.
- **Afuera**: las reglas de cada uno (FEAT-GRAMA, FEAT-NATUR, FEAT-DUPAR); cómo se instala cada herramienta, que es
  del arquitecto; los jueces y su runtime (FEAT-JEXAM), que la alternativa usa tal cual; y una alternativa para
  Gramática, que no la necesita.

## User Journeys

### Journey[F-MLOCAL-J001] - Correr un analizador de idioma
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-MLOCAL-J001
    name: Correr un analizador de idioma
    flow:
      - id: pedir
        action: "El operador corre analyze pidiendo grammar, naturalness o near-duplicates"
        outcomes:
          - when: "Su herramienta local está disponible y su versión aprobó el examen"
            then: corre
          - when: "La versión que toca usar, la local o sin Ollama la de claude -p, no rindió o reprobó el examen"
            then: sin_evaluar
          - when: "Su modelo local no está, la alternativa con claude -p aprobó su examen y la corrida le pide un tope"
            then: alternativa
          - when: "Su modelo local no está, la alternativa con claude -p aprobó su examen y la corrida no le pide tope"
            then: falta_tope
          - when: "Ni su herramienta ni la alternativa están disponibles"
            then: rechazo
      - id: corre
        action: "El informe trae sus hallazgos con la herramienta y la versión, sin mandar nada fuera de la máquina, y una segunda corrida da lo mismo"
        gate: [F-MLOCAL-R001, F-MLOCAL-R002]
        result: success
      - id: sin_evaluar
        action: "Sus ejercicios quedan sin evaluar, salvo lo ya registrado, y el informe dice que la versión no aprobó su examen"
        gate: [F-MLOCAL-R006]
        result: success
      - id: alternativa
        action: "El informe trae los hallazgos que contestó claude -p, sin pasar del tope, con su versión, y dice cuánto falta y cuánto cuesta"
        gate: [F-MLOCAL-R007]
        result: success
      - id: falta_tope
        action: "Lo que necesita a claude -p queda sin evaluar con el motivo «falta el tope», sin ninguna consulta, y el informe dice cuántas consultas y horas costaría"
        gate: [F-MLOCAL-R004, F-MLOCAL-R008]
        result: success
      - id: rechazo
        action: "La corrida se rechaza antes de empezar con el mensaje que dice qué falta y cómo se instala, y no se escribe informe"
        gate: [F-MLOCAL-R003]
        result: failure
```

## Open Questions

<a id="DOUBT-MLOCAL-DEPENDENCIAS"></a>
### Doubt[DOUBT-MLOCAL-DEPENDENCIAS] - ¿OK para sumar las dependencias?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): LanguageTool, y para los modelos los dos que ya están en Ollama, nomic-embed-text y
  llama3. No se baja nada más.
- [ ] Opción B: además, un modelo más chico para Naturalidad (phi3.5 o GPT-2), a medir antes de cambiar.
- [ ] Opción C: sólo LanguageTool; Duplicados parecidos con la condición del comodín, sin modelo, y Naturalidad afuera.

**Answer**: **Opción A** (José, 30/9: «Dale con LanguageTool»): LanguageTool 6.x dentro de content-audit —la vigente es la
6.8, `org.languagetool:language-en:6.8`, del 5/5/2026— y los dos modelos que ya están en Ollama, nomic-embed-text y
llama3 8B. No se baja nada más.

| Dependencia | Para | Licencia | Tamaño | En esta Mac | Cómo se instala |
|---|---|---|---|---|---|
| LanguageTool 6.8, inglés | `grammar` | LGPL 2.1 | unos 50 MB de bibliotecas | sí, dentro de content-audit | biblioteca Java de Maven Central |
| Ollama 0.34 | los dos modelos | MIT | — | ya instalado, servicio local | ya está |
| nomic-embed-text, 137M | `near-duplicates` | Apache 2.0 | 274 MB | ya instalado | `ollama pull nomic-embed-text` |
| all-MiniLM-L6-v2, 22M (alternativa) | `near-duplicates` | Apache 2.0 | unos 90 MB | sí, en Ollama o dentro de content-audit | `ollama pull all-minilm` |
| llama3 8B, Q4_0 | `naturalness` | Meta Llama 3 (uso comercial permitido) | 4,7 GB | ya instalado; usa la GPU del chip | `ollama pull llama3:8b` |
| phi3.5, 3,8B (alternativa) | `naturalness` | MIT | unos 2,2 GB | sí, sin medir | `ollama pull phi3.5` |
| GPT-2 small, 124M (para SLOR) | `naturalness`, opción B de su método | MIT | unos 500 MB | sí, fuera de Ollama | Hugging Face |

<a id="DOUBT-MLOCAL-DONDE"></a>
### Doubt[DOUBT-MLOCAL-DONDE] - ¿Dónde corre cada herramienta?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): LanguageTool dentro de content-audit, como biblioteca; los dos modelos en Ollama, el
  servicio local que ya corre en la Mac.
- [ ] Opción B: todo dentro de content-audit, con los modelos convertidos para Java; llama3 no entra así.
- [ ] Opción C: los modelos en un proceso local de Python, como el de spaCy que content-audit ya corre.

**Answer**: **Opción A** (José, 30/9): LanguageTool dentro de content-audit, como biblioteca, y los dos modelos en
Ollama, que ya corre en la Mac. C sólo hacía falta con SLOR, que quedó descartado.

<a id="DOUBT-MLOCAL-POR-OMISION"></a>
### Doubt[DOUBT-MLOCAL-POR-OMISION] - ¿Corren en todo `analyze`, sin pedirlos?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): sí, los tres: son minutos y gratis, y el «analyze completo con todo» del plan los incluye.
- [ ] Opción B: sólo `grammar`; los de modelo local, sólo pedidos por nombre.

**Answer**: **Opción A** (José, 30/9): los tres corren en todo `analyze`; en la corrida base, sólo Gramática. Con R003:
sin la herramienta, el analizador queda sin evaluar y el resto corre.

<a id="DOUBT-MLOCAL-SIN-OLLAMA"></a>
### Doubt[DOUBT-MLOCAL-SIN-OLLAMA] - ¿Qué pasa con los dos de modelo si Ollama no está?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A: usan claude -p, la sesión de los jueces, como alternativa.
- [ ] Opción B: quedan sin evaluar, con el motivo, como decía el diseño del 30/9 a la tarde.
- [ ] Opción C: bajar los modelos dentro de content-audit.

**Answer**: **Opción A** (José, 30/9 a la noche): «sin Ollama no se deja sin evaluar; recordá que tenemos claude -p».
Duplicados parecidos arma los pares candidatos por parecido de texto y claude -p juzga si cada par es el mismo
ejercicio; Naturalidad le hace la misma pregunta de sí o no. Con Ollama, todo sigue como está. Es R007.

<a id="DOUBT-MLOCAL-TOPE-ALTERNATIVA"></a>
### Doubt[DOUBT-MLOCAL-TOPE-ALTERNATIVA] - ¿La alternativa consulta sin que se le pida un tope?
**Status**: OPEN

La nota de José del 30/9 a la noche dice: «cuidar que no pase por el tope 0 de los jueces pagos». Puede leerse como que
el tope 0 no tiene que frenar a los modelos locales (el gancho B-06), o como que tampoco tiene que frenar a claude -p.

- [ ] Opción A (recomendada): la alternativa tiene el tope 0 de los jueces: sin `--budget` no consulta, reusa lo que ya
  contestó y dice cuánto costaría; el modelo local nunca tiene tope (R008).
- [ ] Opción B: la alternativa tiene un tope por omisión propio, por ejemplo el de un nivel de Naturalidad (A1: 2.334
  consultas, 13 h de a una).
- [ ] Opción C: sin tope: un `analyze` sin Ollama consulta todo el curso (11.870 consultas, 67 h de a una y 17 con 4).

**Answer**: Pendiente. Recomiendo A, y se diseñó con A: es «sin pedido explícito no se paga nada»
([F-JEXAM-R010](#F-JEXAM-R010)) y un `analyze` sin Ollama no puede gastar 67 horas de cupo sin que nadie lo pida. La
alternativa de Naturalidad cuesta casi un tercio de lo que cuestan los cuatro jueces nuevos sobre el mismo nivel: conviene
pedirla para los niveles que los jueces no van a juzgar enteros.

<a id="DOUBT-MLOCAL-MESA"></a>
### Doubt[DOUBT-MLOCAL-MESA] - ¿La mesa mide cada candidato también con los dos de modelo local?
**Status**: OPEN

- [ ] Opción A (recomendada): sí: la mesa mide cada candidato con los instantáneos y con los de modelo local. Con Ollama
  suma una consulta de un token y un vector por oración nueva, en la Mac y gratis, y una consulta de claude -p por par
  candidato nuevo de `near-duplicates` ([F-DUPAR-R009](#F-DUPAR-R009)); sin Ollama, una consulta de claude -p por
  oración y por par candidato nuevo, con el tope de la mesa. Así ve el candidato que copia a un hermano del tema, que es
  justo lo que puede pasar al reescribir una copia.
- [ ] Opción B: no: la mesa mide sólo con los instantáneos ([F-MESA-R004](#F-MESA-R004)), y R005 queda sólo para
  `grammar`; `naturalness` y `near-duplicates` vuelven a mirar el ejercicio en el análisis siguiente.

**Answer**: Pendiente. Recomiendo A: con Ollama cuesta menos de un segundo por candidato y sólo paga los pares nuevos
que proponen los vectores de `near-duplicates`; con B, un candidato que queda casi igual a otro del tema entra al curso
y recién el análisis siguiente lo marca como copia.

## References

- **FEAT-HALL** — Ficha, identidad, conteo de lo sin evaluar y corrida base: R002 a R004.
- **FEAT-JEXAM** — Examen por versión, candidatos de la mesa, tope 0 sin pedido, consultas a la vez, sesión verificada y
  costo declarado de los jueces, que la alternativa usa tal cual: R003, R005 a R008.
- **FEAT-EVCOST** — El libro de evaluaciones, donde quedan los números y las respuestas de claude -p: R002.
- **FEAT-MESA** — La mesa de E, que mide los candidatos: R005 y DOUBT-MLOCAL-MESA.
- **FEAT-GRAMA, FEAT-NATUR, FEAT-DUPAR** — Los tres analizadores de idioma; NATUR y DUPAR definen su alternativa, y
  DUPAR la excepción de su juez de pares con Ollama: R001, R005 a R007.
