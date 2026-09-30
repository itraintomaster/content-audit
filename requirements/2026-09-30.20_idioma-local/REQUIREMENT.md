---
feature:
  id: FEAT-MLOCAL
  code: F-MLOCAL
  name: Idioma local - herramientas y modelos que corren en la máquina
  priority: high
---

# Idioma local: herramientas y modelos que corren en la máquina

## TL;DR

**Qué**: Gramática, Naturalidad y Duplicados parecidos corren en la máquina sin mandar nada afuera, con su herramienta
y su versión a la vista; si falta una, la corrida lo dice antes de empezar, y los tres leen un candidato de la mesa.

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

**Punto de fricción**: pasos 2 a 4.

**Cambio mínimo necesario**: fijar para `grammar`, `naturalness` y `near-duplicates` qué garantizan sobre su herramienta,
su versión, cuándo corren y cómo leen un candidato. Qué herramienta es cada una y dónde corre lo decidió José el 30/9:
LanguageTool dentro de content-audit y los dos modelos que ya están en Ollama.

## Reglas de Negocio

Acá **herramienta** es LanguageTool (FEAT-GRAMA) o un modelo local (FEAT-NATUR, FEAT-DUPAR).

<a id="F-MLOCAL-R001"></a>
### Rule[F-MLOCAL-R001] - Nada sale de la máquina
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Correr `grammar`, `naturalness` o `near-duplicates` no manda ningún texto del curso fuera de la máquina ni necesita
> red: con la red cortada, dan los mismos hallazgos y números.

Es lo que José aprobó para el paquete B en el brief 089: «sin servicios externos». LanguageTool tiene un servicio
público; no se usa.

<a id="F-MLOCAL-R002"></a>
### Rule[F-MLOCAL-R002] - Cada herramienta tiene nombre y versión a la vista, y la misma versión da lo mismo
**Severity**: critical | **Validation**: ASSUMPTION

> La ficha de cada uno ([F-HALL-R006](#F-HALL-R006)) y el informe de cada corrida nombran su herramienta con su versión.
> Invariantes:
> 1. con la misma versión y el mismo curso, dos corridas dan los mismos hallazgos, en el mismo orden y con los mismos
>    números ([F-HALL-R014](#F-HALL-R014));
> 2. el número que un modelo le dio a un ejercicio se guarda con su contenido y la versión, y se reusa mientras no
>    cambien, sin volver a consultar al modelo;
> 3. si un hallazgo cambia sin que cambie su ejercicio, el informe muestra que cambió la versión.

La invariante 2 es la que asegura la 1: con llama3, dos corridas seguidas dieron lo mismo, pero contra una corrida larga
hubo diferencias de hasta 0,025, que en el borde de un umbral cambian una marca. Es el libro de evaluaciones de los
jueces ([F-EVCOST-R003](#F-EVCOST-R003)) sin costo; además, una tanda sólo recalcula lo que tocó [ASSUMPTION].

<a id="F-MLOCAL-R003"></a>
### Rule[F-MLOCAL-R003] - Si falta la herramienta, se sabe antes de empezar
**Severity**: critical | **Validation**: ASSUMPTION

> 1. Pedido por su nombre y sin su herramienta disponible, `analyze` rechaza la corrida antes de empezar, sin escribir
>    informe, con un mensaje que dice qué falta y cómo se instala.
> 2. En una corrida sin selección, el analizador sin herramienta queda sin evaluar en todos sus ejercicios, con el
>    motivo a la vista ([F-HALL-R008](#F-HALL-R008)), y los demás corren igual.

**Error**: "Analyzer '<name>' needs <tool> <version>, which is not available: <how to install>."

<a id="F-MLOCAL-R004"></a>
### Rule[F-MLOCAL-R004] - Qué corre sin pedirlo
**Severity**: major | **Validation**: VALIDATED

> `analyze` sin selección corre los tres. La corrida base, la que usan la vista consolidada y la previa de impacto,
> corre sólo los de costo *instantáneo* (FEAT-HALL): `grammar` sí; `naturalness` y `near-duplicates`, no.

Es la regla del contrato para la corrida base, aplicada a los nuevos; que los tres corran sin pedirlo en `analyze` lo
decidió José el 30/9 ([DOUBT-MLOCAL-POR-OMISION](#DOUBT-MLOCAL-POR-OMISION)). Sin pedirlos, los de modelo local no
tienen tope de consultas: son gratis, y el tope 0 por omisión de los jueces es para los de modelo pago.

<a id="F-MLOCAL-R005"></a>
### Rule[F-MLOCAL-R005] - Los tres leen un candidato de la mesa y dicen qué cambia
**Severity**: critical | **Validation**: AUTO_VALIDATED

> A pedido, los tres leen un candidato de la mesa como leen un ejercicio del curso —`near-duplicates`, contra los demás
> ejercicios de su tema—, sin que el candidato aparezca en el informe del curso, y dicen qué hallazgos suyos agrega y
> cuáles saca respecto del ejercicio al que reemplaza.

Es lo que [F-JEXAM-R005](#F-JEXAM-R005) pide a los jueces, sin costo. Si un hallazgo nuevo frena el candidato lo decide
el paquete E («qué significa no empeorar»); acá sólo se garantiza que se sepa, en segundos y gratis. Hoy la mesa de E
mide los candidatos sólo con los analizadores instantáneos ([F-MESA-R004](#F-MESA-R004)), que deja afuera a
`naturalness` y `near-duplicates`: [DOUBT-MLOCAL-MESA](#DOUBT-MLOCAL-MESA).

<a id="F-MLOCAL-R006"></a>
### Rule[F-MLOCAL-R006] - Cada versión rinde su examen antes de publicar
**Severity**: major | **Validation**: ASSUMPTION

> Una versión nueva de herramienta publica hallazgos recién después de rendir el examen de su analizador sobre
> `escribir/` (FEAT-GRAMA R007, FEAT-NATUR, FEAT-DUPAR R007); su resultado queda junto a la versión, en la ficha.
> Mientras no lo apruebe, sus ejercicios quedan sin evaluar, con el motivo.

Rendir es local y gratis: minutos. Es el «rinde un examen fijo antes de usarse» de los jueces ([F-JEXAM-R003](#F-JEXAM-R003))
sin el costo, y es lo que pide la propuesta para Naturalidad: si no sirve, se saca.

## Contexto

Lo medido en el prototipo del analista, sobre el curso del 29/9 y en esta Mac, sin instalar nada:

| Analizador | Herramienta medida | Qué hay en la máquina | Tiempo |
|---|---|---|---|
| `grammar` | LanguageTool 6.5 (la copia de IntelliJ); entra la 6.8 | nada propio de content-audit | curso entero, unos 10 s |
| `near-duplicates` | nomic-embed-text, 137M, en Ollama | Ollama y el modelo, ya instalados | curso entero, unos 4 min |
| `naturalness` | llama3 8B (Q4_0) en Ollama, pregunta sí o no | Ollama y el modelo, ya instalados | A1–A2, 6 min; el curso, unos 15 (estimado) |

Ollama no devuelve la probabilidad del texto que recibe, sólo la de lo que genera, con 20 alternativas como mucho. Por
eso el SLOR de la propuesta hubo que medirlo de a un token, y dio casi lo mismo que el azar; preguntarle al modelo sí o
no, en cambio, cabe en una consulta de un token y aprueba el examen (FEAT-NATUR).

## Alcance

- **Adentro**: R001 a R006 para los tres analizadores de idioma.
- **Afuera**: las reglas de cada uno (FEAT-GRAMA, FEAT-NATUR, FEAT-DUPAR); cómo se instala cada herramienta, que es
  del arquitecto; y los jueces pagos (FEAT-JEXAM).

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
          - when: "Su herramienta está disponible y su versión aprobó el examen"
            then: corre
          - when: "Su herramienta está disponible y su versión no rindió o reprobó el examen"
            then: sin_evaluar
          - when: "Su herramienta no está disponible"
            then: rechazo
      - id: corre
        action: "El informe trae sus hallazgos con la herramienta y la versión, sin mandar nada fuera de la máquina, y una segunda corrida da lo mismo"
        gate: [F-MLOCAL-R001, F-MLOCAL-R002]
        result: success
      - id: sin_evaluar
        action: "Sus ejercicios quedan sin evaluar y el informe dice que la versión no aprobó su examen"
        gate: [F-MLOCAL-R006]
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

<a id="DOUBT-MLOCAL-MESA"></a>
### Doubt[DOUBT-MLOCAL-MESA] - ¿La mesa mide cada candidato también con los dos de modelo local?
**Status**: OPEN

- [ ] Opción A (recomendada): sí: la mesa mide cada candidato con los instantáneos y con los de modelo local. Suma una
  consulta de un token y un vector por oración nueva, en la Mac y gratis, y así ve el candidato que copia a un hermano
  del tema, que es justo lo que puede pasar al reescribir una copia.
- [ ] Opción B: no: la mesa mide sólo con los instantáneos ([F-MESA-R004](#F-MESA-R004)), y R005 queda sólo para
  `grammar`; `naturalness` y `near-duplicates` vuelven a mirar el ejercicio en el análisis siguiente.

**Answer**: Pendiente. Recomiendo A: cuesta menos de un segundo por candidato y no paga nada; con B, un candidato que
queda casi igual a otro del tema entra al curso y recién el análisis siguiente lo marca como copia.

## References

- **FEAT-HALL** — Ficha, identidad, conteo de lo sin evaluar y corrida base: R002 a R004.
- **FEAT-JEXAM** — Examen por versión y candidatos de la mesa para los jueces, que acá se copian sin costo: R005, R006.
- **FEAT-MESA** — La mesa de E, que mide los candidatos: R005 y DOUBT-MLOCAL-MESA.
- **FEAT-GRAMA, FEAT-NATUR, FEAT-DUPAR** — Los tres analizadores de idioma.
