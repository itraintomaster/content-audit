---
patch: FEAT-NIVFR
requirement: 2026-09-30.13_nivel-con-frases
generated: 2026-09-30T19:30:00Z
---

# Tech Spec: Palabras de otro nivel, `off-level-words` (FEAT-NIVFR)

Paquete D, ronda 1. Diseñado contra el modelo que queda después de 0.2 (FEAT-OPMUL) y 0.3 (FEAT-HALL), validado con
`patch propose` contra el `sentinel.yaml` de hoy. **Vienen de 0.3**: `AnalyzerProvider`, `AnalyzerDescriptor` con su ficha,
`AnalyzerPlanBinding`, `AnalyzerFamily`, `AnalyzerRuleCard`, `FindingDraft`, `FindingEvidence`, `EvidencePart`,
`FindingSeverity`, `FindingResolution`, `AnalysisCost` y `ContentAnalyzer.findingsAt`. **Viene de 0.2**:
`AuditableQuiz.multipleChoice`, para leer la respuesta de un ejercicio de opción múltiple. Lo compartido que este diseño
necesita está en `docs/propuestas/tablero-corregir-sin-empeorar/ronda1/ganchos-D.md` (D-01 a D-04). Todo lo medido sale
del análisis 2026-09-30T11-54-02 (curso del 29/9) con un prototipo que reproduce exacto sus marcas.

## Contar las palabras de otro nivel con el mismo puntuador que lemma-absence
El nivel de cada palabra tiene que ser el que ya resuelve `lemma-absence` (R001), así que el analizador puntúa la oración
de cada ejercicio con el `SentenceLexicalScorer` que comparten `lemma-absence` y `lexis`: la invariante 3 de R001 sale por
construcción, y la rebaja de R042 ya deja dentro de su nivel 381 palabras («a lot of tea», «lives in London») sin tocar
el nivel de nada. Recalcula en vez de leer el diagnóstico de `lemma-absence` para poder correr solo (F-HALL-R012). De las
marcas se queda con lo que está a dos o más niveles o fuera del EVP en la banda fuerte, que reconoce con
`LemmaAbsenceConfig` en vez de copiar sus números; nunca le inventa un nivel a una palabra fuera del catálogo. El puntaje
es 1 o 0 porque en la familia de errores un ejercicio tiene error o no lo tiene (F-HALL-R004 inv. 1): el promedio de un
tema es la parte de sus ejercicios sin palabras de otro nivel, y no entra al vocabulario (R006). Medido: 18 · 33 · 81 · 38
= 170 ejercicios y 175 palabras.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: offlevelwords
        _change: add
        visibility: public
        allowedClients: [audit-cli, refiner-domain]
        models:
          - name: OffLevelWordsDiagnosis
            _change: add
            type: record
            fields:
              - { name: level, type: CefrLevel }
              - { name: words, type: "List<OffLevelWord>" }
          - name: OffLevelWord
            _change: add
            type: record
            fields:
              - { name: lemma, type: String }
              - { name: pos, type: String }
              - { name: reason, type: OffLevelReason }
              - { name: catalogLevel, type: CefrLevel }
              - { name: frequencyRank, type: Integer }
          - name: OffLevelReason
            _change: add
            type: enum
            fields:
              - { name: ABOVE_LEVEL }
              - { name: OUT_OF_CATALOG }
        implementations:
          - name: OffLevelWordsAnalyzer
            _change: add
            visibility: internal
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: sentenceLexicalScorer, type: SentenceLexicalScorer }
              - { name: lemmaAbsenceConfig, type: LemmaAbsenceConfig }
              - { name: topicTeachingResolver, type: TopicTeachingResolver }
```

## Decidir en un solo lugar qué enseña un tema
La excepción de R003 la usan dos piezas: este analizador, para no contar la palabra, y el semáforo, para no pintarla de
rojo (F-SEMAF-R004). Por eso es un puerto público con su implementación, y no un detalle del analizador: si la regla
cambia, cambia para los dos. Sale sólo de datos del curso, como pide R035 de LABS: las palabras de los términos entre `$`
del título del knowledge y los lemas que son respuesta en al menos 3 ejercicios del tema y en el 20 % o más. La respuesta
es la del hueco o, en opción múltiple, la opción correcta que declara 0.2; se lee en los tokens que el ejercicio ya trae,
así que no se tokeniza de nuevo. Medido: saca 37 ejercicios (9 de A2 por «$Invest time$»; 28 de B1, sequence,
simultaneous y non-defining) y ninguno del 077. La excepción de R004 (la palabra que la traducción repite entera y sin
tildes) es sólo de este analizador y queda adentro: saca 19 (sushi, USB, Emily).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: offlevelwords
        _change: add
        visibility: public
        allowedClients: [audit-cli, refiner-domain]
        models:
          - name: TaughtWords
            _change: add
            type: record
            fields:
              - { name: knowledgeId, type: String }
              - { name: titleTerms, type: "List<String>" }
              - { name: frequentAnswers, type: "List<String>" }
        interfaces:
          - name: TopicTeachingResolver
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "taughtWords(AuditNode knowledgeNode): TaughtWords"
        implementations:
          - name: DefaultTopicTeachingResolver
            _change: add
            visibility: public
            implements: [TopicTeachingResolver]
```

## Registrar el analizador con su proveedor
El proveedor es la única pieza que ve la raíz de composición: arma un analizador nuevo en cada corrida (F-HALL-R014) y
trae su ficha (R007): pregunta «¿Hay palabras que el alumno de este nivel no puede conocer?», lee la oración terminada, el
título y las respuestas del tema y la traducción, familia errores, evalúa QUIZ, una regla (F-NIVFR-R002), meta 0,
resolución mesa, costo instantáneo. No tiene ligadura al plan: José eligió el 30/9 que la tarea sea la LEMMA_ABSENCE que ya
existe (DOUBT-TAREA-COMPARTIDA), y los 170 están entre los 950 que la tienen porque toda palabra que marca es una marca de
`lemma-absence` (R001 inv. 3); el plan sigue en 4.036 y declara el puntaje como cubierto (enganche D-04). Va declarado como
clase con las cinco firmas de `AnalyzerProvider` sólo porque ese puerto llega con 0.3 y `patch propose` no resuelve un
`implements` hacia él: al integrar sobre 0.3 pasa a `implements: [AnalyzerProvider]` (enganche D-01). El paquete es
público con `allowedClients` porque tiene dos clientes, la raíz de composición y el semáforo; el analizador queda
package-private.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: offlevelwords
        _change: add
        visibility: public
        allowedClients: [audit-cli, refiner-domain]
        implementations:
          - name: OffLevelWordsAnalyzerProvider
            _change: add
            visibility: public
            stereotype: class
            exposes:
              - signature: "analyzerName(): String"
              - signature: "describe(): AnalyzerDescriptor"
              - signature: "create(EvaluationRunPolicy policy): ContentAnalyzer"
              - signature: "planBinding(): Optional<AnalyzerPlanBinding>"
              - signature: "config(): Optional<SelfDescribingConfig>"
            requiresInject:
              - { name: sentenceLexicalScorer, type: SentenceLexicalScorer }
              - { name: lemmaAbsenceConfig, type: LemmaAbsenceConfig }
              - { name: topicTeachingResolver, type: TopicTeachingResolver }
features:
  - id: FEAT-NIVFR
    code: F-NIVFR
```

## Dejar un hallazgo por palabra, con lo que hace falta para verificarlo
`findingsAt` devuelve, en cada ejercicio con palabras, un borrador por palabra: regla F-NIVFR-R002, marca `lema|categoría`
(la identidad de F-HALL-R014), gravedad media y resolución mesa, como decidió José. La evidencia no necesita volver a correr
nada: la oración terminada, la palabra, su nivel o «fuera del EVP» con su puesto, y el nivel del ejercicio, con una
observación en castellano: «spread y quilt son B2; el ejercicio es A2», «reside está fuera del EVP (puesto 5.333); el
ejercicio es A1». El diagnóstico tipado guarda lo mismo como datos (F-HALL-R001 inv. 3) y es lo que lee el semáforo para
decir si una roja es además error. Cuando exista el renderizador «como lo ve el alumno» (pedido 2 del paquete G), la
evidencia suma la oración con el hueco; hoy la terminada es la que mide `lemma-absence`, también en opción múltiple.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: offlevelwords
        _change: add
        visibility: public
        allowedClients: [audit-cli, refiner-domain]
        models:
          - name: OffLevelWord
            _change: add
            type: record
            fields:
              - { name: lemma, type: String }
              - { name: pos, type: String }
              - { name: reason, type: OffLevelReason }
              - { name: catalogLevel, type: CefrLevel }
              - { name: frequencyRank, type: Integer }
```

## Medir con el examen y fijar lo que no se mueve
El examen se corre así: `analyze` pidiendo sólo `off-level-words` sobre el curso del 29/9, cada ejercicio con hallazgo se
escribe como `{"quizId", "analizador": "nivel"}` y `medir.py marcas.json --parte escribir --categoria nivel` tiene que dar
26 de 52 (7 de 17 en A1, 19 de 35 en A2), con 51 marcas en la parte y 25 fuera del oro; `examinar/` queda para 0.4. Para
@qa-tester, dos pruebas que fijan R006 sin el curso real: con una corrida con y sin `off-level-words`, `lemma-absence`
deja los mismos puntajes y diagnósticos en todos los nodos y el plan las mismas tareas; y el analizador nunca escribe una
clave que no sea la suya. El 73,9 % y los niveles 96,5 · 94,2 · 79,5 · 63,5 los fija la prueba de F-HALL-R013 del
integrador, que ahora corre con este analizador en el catálogo: por ser de errores, no entra a ningún promedio de
vocabulario. El lematizador compartido deja un falso: «We especially hoped to see the concert.» (B1), con «hop» C1.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: offlevelwords
        _change: add
        visibility: public
        allowedClients: [audit-cli, refiner-domain]
        implementations:
          - name: OffLevelWordsAnalyzer
            _change: add
            visibility: internal
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: sentenceLexicalScorer, type: SentenceLexicalScorer }
              - { name: lemmaAbsenceConfig, type: LemmaAbsenceConfig }
              - { name: topicTeachingResolver, type: TopicTeachingResolver }
```
