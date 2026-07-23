---
patch: F-CLEX
requirement: 2026-07-22.01_consulta-lexica-de-oracion-por-cli
generated: 2026-07-22T00:00:00Z
---

# Tech Spec: Consulta lexica de oracion por CLI (FEAT-CLEX)

## Extraer el scoring lexico por-oracion a un motor compartido en `labs`
La logica que decide que palabras se flaggean (emparejamiento exacto+fallback de F-LABS-R036, repliegue a minusculas R045, bandas de fuera-de-catalogo R034, exclusiones R035, techos C1/C2 R033) vive hoy en el metodo privado `scoreQuiz` de `LemmaByLevelAbsenceAnalyzer`. F-CLEX-R006 exige **paridad exacta** con el audit, no una replica: por eso extraemos esa logica a un unico motor `SentenceLexicalScorer` en la misma frontera que el audit (`(tokens, nivel)`), que devuelve el score mas los tipos ricos ya existentes (`MisplacedLemma`/`OutOfCatalogWord`). Es interno porque su carrier arrastra `discount`/`cocaRank`/`semanticCategory` que solo el scoring del audit necesita.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: labs
        _change: modify
        models:
          - name: SentenceLexicalScore
            _change: add
            type: record
            fields:
              - { name: score, type: double }
              - { name: misplacedLemmas, type: "List<MisplacedLemma>" }
              - { name: outOfCatalogWords, type: "List<OutOfCatalogWord>" }
        interfaces:
          - name: SentenceLexicalScorer
            _change: add
            stereotype: port
            visibility: internal
            exposes:
              - signature: "score(List<NlpToken> tokens, CefrLevel sentenceLevel): SentenceLexicalScore"
        implementations:
          - name: DefaultSentenceLexicalScorer
            _change: add
            implements: ["SentenceLexicalScorer"]
            visibility: public
            requiresInject:
              - { name: evpCatalogPort, type: EvpCatalogPort }
              - { name: contentWordFilter, type: ContentWordFilter }
              - { name: lemmaAbsenceConfig, type: LemmaAbsenceConfig }
```

## Hacer que el analyzer delegue en el motor extraido
Para que "paridad" sea estructural y no una promesa, el analyzer no puede conservar su propia copia de `scoreQuiz`: debe delegar. Le inyectamos `SentenceLexicalScorer` y su `scoreQuizzes` pasa a llamarlo, dejando una unica fuente de verdad del emparejamiento lexico. El cambio es aditivo (constructor de 3 a 4 dependencias) y preserva el comportamiento observable, por lo que la bateria de tests de FEAT-LABS sobre el analyzer sigue siendo el guardian de la no-regresion.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: labs
        _change: modify
        implementations:
          - name: LemmaByLevelAbsenceAnalyzer
            _change: modify
            requiresInject:
              - { name: sentenceLexicalScorer, type: SentenceLexicalScorer, _change: add }
```

## Exponer un carrier publico minimo con solo los flags
El consumidor (eval #7 via CLI) necesita unicamente el conjunto de palabras flaggeadas, no el score ni los descuentos internos. Creamos un paquete publico `lexicalflags` con el puerto `SentenceLexicalEvaluator` y carriers que exponen exactamente los campos de F-CLEX-R003 (mal ubicada: lema, POS, nivel esperado, nivel de la oracion) y F-CLEX-R004 (fuera de catalogo: lema, POS, rango de frecuencia nullable). La superficie minima hace verdadera la garantia de F-CLEX-R005: listas vacias = oracion limpia, sin marcadores de palabras justificadas.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lexicalflags
        _change: add
        visibility: public
        models:
          - name: SentenceLexicalFlags
            _change: add
            type: record
            fields:
              - { name: misplacedWords, type: "List<MisplacedWordFlag>" }
              - { name: outOfCatalogWords, type: "List<OutOfCatalogFlag>" }
          - name: MisplacedWordFlag
            _change: add
            type: record
            fields:
              - { name: lemma, type: String }
              - { name: pos, type: String }
              - { name: expectedLevel, type: CefrLevel }
              - { name: sentenceLevel, type: CefrLevel }
          - name: OutOfCatalogFlag
            _change: add
            type: record
            fields:
              - { name: lemma, type: String }
              - { name: pos, type: String }
              - { name: frequencyRank, type: Integer }
        interfaces:
          - name: SentenceLexicalEvaluator
            _change: add
            stereotype: port
            visibility: public
            exposes:
              - signature: "evaluate(List<NlpToken> tokens, CefrLevel sentenceLevel): SentenceLexicalFlags"
        implementations:
          - name: DefaultSentenceLexicalEvaluator
            _change: add
            implements: ["SentenceLexicalEvaluator"]
            visibility: public
            requiresInject:
              - { name: sentenceLexicalScorer, type: SentenceLexicalScorer }
```

## Agregar el verbo CLI de solo lectura que orquesta la consulta
`LexisCommand` es el analogo lexico del comando `tokens`: recibe la quizSentence (DSL), el modo `FILL`/`REWRITE` y el nivel CEFR (F-CLEX-R002), deriva la frase canonica y la tokeniza con el **mismo** `NlpTokenizer.analyzeTokens` que usa el audit, y delega en `SentenceLexicalEvaluator`. Es un puerto gobernado (no un helper suelto como `TokensCmd`) porque F-CLEX-J001 y sus reglas necesitan superficie de test y trazabilidad. La validacion de DSL/nivel invalido (F-CLEX-R007) vive en el comando, con los mismos patrones de error que `tokens`.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: LexisCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "queryLexis(String quizSentence, String mode, String level): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: LexisCmd
            _change: add
            implements: ["LexisCommand"]
            externalImplements:
              - "java.util.concurrent.Callable<Integer>"
            types: ["Component"]
            requiresInject:
              - { name: quizSentenceConverter, type: QuizSentenceConverter }
              - { name: nlpTokenizer, type: NlpTokenizer }
              - { name: sentenceLexicalEvaluator, type: SentenceLexicalEvaluator }
```
