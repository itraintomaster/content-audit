---
patch: FEAT-SEMAF
requirement: 2026-09-30.14_semaforo-por-tema
generated: 2026-09-30T19:40:00Z
---

# Tech Spec: Semáforo de palabras por tema (FEAT-SEMAF)

Paquete D, ronda 1. José decidió el 30/9 que el semáforo es una consulta, no un analizador, y que una palabra muy repetida
no es roja. Diseñado contra el modelo de 0.2 y 0.3 (no usa nada nuevo de ellos) y contra los otros dos parches del
paquete: `TopicTeachingResolver` viene de FEAT-NIVFR y la tabla por palabra de `lemma-count` (`LemmaExposure`, con su
umbral de banda) viene de FEAT-POCOV, así que el integrador los aplica antes o juntos. El recurso de `get` y el cableado
son enganches (D-05 y D-06 en `ronda1/ganchos-D.md`). Medido sobre el análisis 2026-09-30T11-54-02.

## Leer el semáforo de un análisis guardado, sin escribir nada
Es una consulta de sólo lectura, como `get suggested-lemmas`: recibe un `AuditReport` ya guardado y un tema, y devuelve sus
palabras con color. No tiene ficha, puntaje ni hallazgos (R007), así que no pasa por el catálogo de 0.3 ni puede mover el
73,9 % ni las 4.036 tareas: no hay código suyo en `analyze` ni en `plan`. Vive en `refiner-domain`, en un paquete propio,
porque el verde para sumar son las palabras sugeridas de este módulo y porque `audit-domain` no puede depender de él; su
único cliente es la raíz de composición. Lee el mismo análisis fuente que las sugeridas (F-CSLATDC-R015) y lo dice en la
respuesta, para que quien la mira sepa de qué foto habla.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    packages:
      - name: topicwords
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        models:
          - name: TopicWordLights
            _change: add
            type: record
            fields:
              - { name: auditId, type: String }
              - { name: knowledgeId, type: String }
              - { name: knowledgeLabel, type: String }
              - { name: level, type: CefrLevel }
              - { name: words, type: "List<TopicWord>" }
              - { name: toAdd, type: "List<SuggestedLemma>" }
        interfaces:
          - name: TopicWordLightsQuery
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "lightsFor(AuditReport report, String auditId, String knowledgeId, SuggestedLemmaQueryCriteria criteria): TopicWordLights"
                throws: [KnowledgeNotInAnalysisException, LemmaCountDataMissingException]
```

## Pintar el rojo con la consulta léxica y la excepción del tema
El rojo es, por regla, lo que `lexis` marca para el nivel del tema (R005), así que la consulta evalúa con el
`SentenceLexicalEvaluator` público de F-CLEX los tokens que cada ejercicio del tema ya trae en el informe: no depende de que
el análisis haya incluido `lemma-absence` y no copia su resolución de niveles. Lo que el tema enseña nunca es rojo (R004):
lo decide el mismo `TopicTeachingResolver` de `off-level-words`, así que un cambio en esa regla cambia las dos piezas a la
vez. Que una roja sea además error se lee del diagnóstico de `off-level-words` de ese ejercicio (slot del enganche D-02);
si el análisis no lo corrió, queda sin dato y el color no cambia. Nada es rojo por repetirse (R006, José 30/9).

```architecture
modules:
  - name: refiner-domain
    _change: modify
    packages:
      - name: topicwords
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        models:
          - name: WordLight
            _change: add
            type: enum
            fields:
              - { name: GREEN }
              - { name: RED }
              - { name: NEUTRAL }
        implementations:
          - name: DefaultTopicWordLightsQuery
            _change: add
            visibility: public
            implements: [TopicWordLightsQuery]
            requiresInject:
              - { name: sentenceLexicalEvaluator, type: SentenceLexicalEvaluator }
              - { name: suggestedLemmaQueryPort, type: SuggestedLemmaQueryPort }
              - { name: topicTeachingResolver, type: TopicTeachingResolver }
```

## Pintar el verde con lo que publica lemma-count
El verde propio dice «la palabra del nivel que conviene que vuelva», que es la pregunta de `lemma-count`: por eso usa su
nivel (catálogo exacto por lema y categoría), sus oraciones y el umbral de la banda de cada palabra que publica su tabla
(FEAT-POCOV), y una verde «poco vista» es siempre un hallazgo suyo. Con el nivel de `lemma-absence` entraban 70 pares
palabra-tema que son etiquetado malo («pizza» y «orange» como verbos, «our» y «write» como sustantivos, «drum» como verbo):
queda como ASSUMPTION de R002, avisada a José. Hoy da 134 · 633 · 1.012 · 307 verdes en A1 · A2 · B1 · B2; con las bandas
8 · 6 · 4 · 3, 242 · 771 · 1.080 · 307; «Posición del adjetivo» sigue con 157 palabras, 42 rojas y 27 verdes (29 con
bandas). Las oraciones de las palabras sin nivel salen de la lista de no asignadas de `lemma-count`, así que toda palabra
del tema tiene su número; un análisis sin `lemma-count` se rechaza en vez de inventar ceros.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    packages:
      - name: topicwords
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        models:
          - name: TopicWord
            _change: add
            type: record
            fields:
              - { name: lemma, type: String }
              - { name: pos, type: String }
              - { name: level, type: CefrLevel }
              - { name: outOfCatalog, type: boolean }
              - { name: frequencyRank, type: Integer }
              - { name: sentenceCount, type: int }
              - { name: threshold, type: Integer }
              - { name: light, type: WordLight }
              - { name: greenState, type: GreenState }
              - { name: taughtByTopic, type: boolean }
              - { name: offLevelError, type: Boolean }
          - name: GreenState
            _change: add
            type: enum
            fields:
              - { name: UNDER_EXPOSED }
              - { name: SCARCE }
          - name: LemmaCountDataMissingException
            _change: add
            type: exception
            message: "The analysis '%s' has no lemma-count data. Run 'content-audit analyze' with lemma-count."
            fields:
              - { name: auditId, type: String }
```

## Sumar las sugeridas del nivel sin tocar su consulta
R003 pide que, con los mismos filtros, la lista para sumar sea la de `get suggested-lemmas` para una tarea de ese nivel.
En vez de abrirle a `SuggestedLemmaQueryPort` un método por nivel, que tocaría el código de F-CSLATDC que también usa el
agente de la corrección, la consulta le pide la lista con una tarea transitoria LEMMA_ABSENCE anclada a un ejercicio del
tema: el puerto encuentra el mismo milestone y devuelve, por construcción, la misma lista en el mismo orden de seis grupos.
La tarea no se guarda ni entra a ningún plan. El puerto se llama sin límite; la consulta saca las palabras que el tema ya
usa y recién ahí corta, así que pedir 10 devuelve 10 mientras alcancen, y ninguna es roja (R007). Con «incluir expuestas»,
la marca de poco vista de la cola usa el umbral de la banda de cada palabra cuando entre el enganche D-08.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    packages:
      - name: topicwords
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultTopicWordLightsQuery
            _change: add
            visibility: public
            implements: [TopicWordLightsQuery]
            requiresInject:
              - { name: sentenceLexicalEvaluator, type: SentenceLexicalEvaluator }
              - { name: suggestedLemmaQueryPort, type: SuggestedLemmaQueryPort }
              - { name: topicTeachingResolver, type: TopicTeachingResolver }
```

## Rechazar el tema que no está, con el mensaje de la regla
Un tema que el análisis no tiene rechaza la consulta con el mensaje de R001 y no devuelve ninguna lista (J001, rechazo).
Es una excepción propia y no un vacío porque «no hay palabras» y «no existe el tema» son respuestas distintas, y el
recurso de `get` (enganche D-05) las traduce a su salida de error. Para @qa-tester: que la consulta deje el informe y el
plan idénticos byte a byte (R007) se prueba comparando los archivos antes y después; que ninguna roja aparezca para sumar,
con un tema cuya sugerida sea también marcada por `lexis`.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    packages:
      - name: topicwords
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        models:
          - name: KnowledgeNotInAnalysisException
            _change: add
            type: exception
            message: "Knowledge '%s' not found in the analysis."
            fields:
              - { name: knowledgeId, type: String }
features:
  - id: FEAT-SEMAF
    code: F-SEMAF
```

## Declarar las 32 pruebas de FEAT-SEMAF y ubicar su journey
Las 32 son nuevas: no hay código del semáforo que portar. 28 van a `DefaultTopicWordLightsQuery`, que hace la consulta,
y 4 a `GetCmd`, que la expone como `get word-lights`. Donde la regla trae un número, la prueba lo fija.

| Pieza | Reglas | Pruebas | Qué fija |
|---|---|---:|---|
| `DefaultTopicWordLightsQuery` | R001 | 5 | una fila por lema y categoría con nivel, oraciones y color; qué nivel se muestra; «sin nivel»; los dos rechazos con su mensaje |
| | R002 | 7 | verde con menos de 2N; poco vista de 1 a N−1 y escasa de N a 2N−1 (rubber 3, lamp 4 y vegetable 10 con N 4); otro nivel no es verde; el N de la banda (number, 5 oraciones: poco vista con 8, escasa con 4); ninguna roja es verde; poco vista ⇔ hallazgo de `lemma-count` |
| | R003 | 3 | el orden de `get suggested-lemmas`; el límite después de sacar las del tema; los mismos filtros |
| | R004 | 3 | invest en «$Invest time$ + gerundio»; sequence y simultaneous como respuesta; let's |
| | R005 | 6 | mal ubicada con su nivel; fuera del EVP con su puesto; la que `lexis` no castiga; sin depender de `lemma-absence`; la marca de error y su «sin dato» |
| | R006, R007 | 4 | go y oven quedan neutras; ninguna roja para sumar; el informe queda igual |
| `GetCmd` | R001, R007 | 4 | JSON leído del análisis fuente del último plan; código 1 con el tema que no está; `--level` rechazado; el análisis y el plan del 29/9 idénticos byte a byte, en 73,92 % y 4.036 tareas |

**Dependencias.** `TopicTeachingResolver` viene de FEAT-NIVFR y la tabla por palabra de `lemma-count` (el N de cada
banda y sus hallazgos) de FEAT-POCOV, así que las dos se aplican antes o junto con ésta. La marca de error lee el
diagnóstico del enganche D-02; las cuatro de `GetCmd`, D-05, y el journey además D-06; «incluir expuestas» con bandas,
D-08; la de byte a byte usa una copia del análisis 2026-09-30T11-54-02 y de su plan (el fixture de D-12).

F-SEMAF-J001 va a `audit-cli`, en `com.learney.contentaudit.auditcli.commands`, donde `GetCmd` es package-private, como
los journeys de las sugeridas (F-SLEM-J001) y de `lexis` (F-CLEX-J001). Queda como prueba común el formato del JSON.

```architecture
modules:
  - name: refiner-domain
    _change: modify
features:
  - id: FEAT-SEMAF
    code: F-SEMAF
    journeys:
      - { id: F-SEMAF-J001, testModule: audit-cli, testPackage: com.learney.contentaudit.auditcli.commands }
```
