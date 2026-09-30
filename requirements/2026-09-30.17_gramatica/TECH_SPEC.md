---
patch: FEAT-GRAMA
requirement: 2026-09-30.17_gramatica
generated: 2026-09-30T21:40:00Z
---

# Tech Spec: Gramática, la oración terminada no tiene errores de inglés (FEAT-GRAMA)

Paquete B de la ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. Este parche trae sólo el analizador
`grammar` y la oración terminada que lee. La herramienta —el puerto `GrammarChecker`, su adaptador de LanguageTool
6.8 y la dependencia— y la decisión de publicar (`LocalToolGate`) son de FEAT-MLOCAL, que se aplica antes. La oración
terminada la usa también `naturalness`, así que FEAT-NATUR va después de éste. Lo compartido está en `ganchos-B.md`.

Los números del requirement se midieron con LanguageTool 6.5 sin agrupador; la 6.8 los vuelve a medir en su examen.

## Leer la oración que lee el alumno, no la oración plana

La oración plana de content-audit deja «They 're» (123 ejercicios), «- water» (331 de opción múltiple), «was / were
was» (110) y un «..» tras una nota: con ella LanguageTool dio 920 marcas, con la lectura de R001, 693. Cambiarla movería
el largo de oración (F-HALL-R013), así que Gramática arma la suya y la plana no se toca. Es una por respuesta aceptada
porque R001 lee todas, la contraída y la larga, y cada una trae dónde quedó cada respuesta: con eso se sabe si una marca
toca una respuesta aceptada o repite lo pegado al hueco. Recibe el modo del tema porque en una reescritura la respuesta
es la oración entera y la de partida no se lee. No es el ejercicio terminado del renderizador del integrador (A-01),
que conserva la oración de partida porque así lo pide Duplicados exactos: las dos lecturas conviven a propósito (B-20).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: finishedsentence
        _change: add
        visibility: public
        models:
          - name: AnswerSpan
            _change: add
            type: record
            fields:
              - { name: start, type: int }
              - { name: end, type: int }
              - { name: text, type: String }
          - name: FinishedSentence
            _change: add
            type: record
            fields:
              - { name: text, type: String }
              - { name: answerIndex, type: int }
              - { name: answers, type: "List<AnswerSpan>" }
        interfaces:
          - name: FinishedSentenceReader
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "read(AuditableQuiz quiz,SentenceMode mode): List<FinishedSentence>"
        implementations:
          - name: DefaultFinishedSentenceReader
            _change: add
            visibility: public
            implements: [FinishedSentenceReader]
```

## Quedarse con lo que marcan las dos variantes, y sólo gramática y ortografía

R002, como la eligió José el 30/9: afuera estilo, redundancia, tipografía, mayúsculas, compuestos, variantes
regionales, lo que no marcan las dos variantes, las reglas apagadas por omisión y SINGULAR_AGREEMENT_SENT_START, la de
los colectivos. El filtro recibe las marcas de las dos variantes y se queda con las que coinciden en lugar y categoría,
y sólo si la categoría o la regla está en su lista; las listas exactas son las del prototipo del analista
(`scratchpad/ronda2-B/lt/filtro.py`) más la regla de los colectivos, y quedan en la configuración, a la vista en
`config analyzer grammar`. Su `methodId` entra en la versión que rinde examen: cambiar una lista es una versión nueva.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: grammar
        _change: add
        visibility: public
        interfaces:
          - name: GrammarMatchFilter
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "keep(List<GrammarMatch> american,List<GrammarMatch> british): List<GrammarMatch>"
          - name: GrammarConfig
            _change: add
            visibility: internal
            sealed: false
            extends: [SelfDescribingConfig]
            exposes:
              - signature: "methodId(): String"
              - signature: "keptCategories(): Set<String>"
              - signature: "keptRules(): Set<String>"
              - signature: "excludedRules(): Set<String>"
        implementations:
          - name: DefaultGrammarMatchFilter
            _change: add
            implements: [GrammarMatchFilter]
            requiresInject:
              - { name: config, type: GrammarConfig }
          - name: DefaultGrammarConfig
            _change: add
            implements: [GrammarConfig]
```

## El signo final, con los tokens que el ejercicio ya trae

R003 marca la oración con verbo, de tres palabras o más y con mayúscula que no cierra con «.», «?» o «!», y la pregunta
que empieza con auxiliar y sujeto y cierra con punto. «Con verbo» y «empieza con auxiliar» piden categorías
gramaticales, y el ejercicio ya las trae: los tokens de spaCy que el curso calcula al cargarse. Así no hace falta un
segundo analizador de lenguaje. Hoy son 10 marcas, 8 reales, y 3 de los 4 aciertos del examen.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: grammar
        _change: add
        visibility: public
        interfaces:
          - name: FinalPunctuationCheck
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "check(FinishedSentence sentence,List<NlpToken> tokens): Optional<GrammarIssue>"
        implementations:
          - name: DefaultFinalPunctuationCheck
            _change: add
            implements: [FinalPunctuationCheck]
```

## La gravedad y la resolución salen de dónde está el error

R004 decide por el lugar, en orden: bloqueante si la respuesta aceptada repite palabras pegadas al hueco (hoy son 3:
«noise noise», «in August in August», «the apartment the apartment»); alta si lo marcado toca una respuesta aceptada
(«He've had»); media si está sólo en el texto fijo («clothers», los 13 «videogames»); baja si falta el signo o el
espacio después de un punto. R005 manda a la regla la repetición, el signo y el espacio, y a la mesa todo lo demás. El
clasificador lo decide con las posiciones de las respuestas en la oración terminada, sin volver a leer el ejercicio.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: grammar
        _change: add
        visibility: public
        models:
          - name: GrammarIssuePlacement
            _change: add
            type: enum
            fields:
              - { name: GLUED_REPEAT }
              - { name: ACCEPTED_ANSWER }
              - { name: FIXED_TEXT }
              - { name: PUNCTUATION }
        interfaces:
          - name: GrammarIssueClassifier
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "classify(GrammarMatch match,FinishedSentence sentence): GrammarIssue"
        implementations:
          - name: DefaultGrammarIssueClassifier
            _change: add
            implements: [GrammarIssueClassifier]
```

## Un diagnóstico tipado por ejercicio

Lo propio de Gramática —la regla de LanguageTool, lo marcado, dónde está, la explicación— va en su diagnóstico tipado y
nunca como campo del hallazgo (F-HALL-R001 inv. 3). La identidad de un problema es la regla de LanguageTool y el texto
marcado (R006): si dos respuestas aceptadas muestran lo mismo, es uno solo. Con al menos un problema el ejercicio tiene
0, sin ninguno 1 (F-HALL-R004). La ranura en `QuizDiagnoses` es el gancho B-02, y la identidad de la lista para la vista
consolidada, el B-04.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: grammar
        _change: add
        visibility: public
        models:
          - name: GrammarIssue
            _change: add
            type: record
            fields:
              - { name: rule, type: String }
              - { name: toolRule, type: String }
              - { name: marked, type: String }
              - { name: sentence, type: String }
              - { name: placement, type: GrammarIssuePlacement }
              - { name: severity, type: FindingSeverity }
              - { name: resolution, type: FindingResolution }
              - { name: explanation, type: String }
          - name: GrammarDiagnosis
            _change: add
            type: record
            fields:
              - { name: sentences, type: "List<String>" }
              - { name: issues, type: "List<GrammarIssue>" }
              - { name: score, type: double }
```

## El proveedor decide por corrida; el analizador, por ejercicio

El proveedor le pregunta a `LocalToolGate` una vez por corrida, en `create(policy)`, y le pasa la decisión al
analizador. Sin LanguageTool o sin examen aprobado, `onQuiz` marca el ejercicio en `unevaluatedBy` sin puntaje ni
hallazgo y la corrida sigue (F-MLOCAL-R003 inv. 2); lo mismo si LanguageTool no carga en la primera oración, en vez de
cortar la corrida como 0.3 hace con los instantáneos, porque es la herramienta y no un error de programación. En
`findingsAt`, cada problema es un borrador con la regla de la ficha, la marca «regla de LanguageTool|texto» (vacía en el
signo final) y la oración terminada como evidencia. La ficha: pregunta «¿La oración terminada tiene errores de
inglés?», lee la oración terminada de cada respuesta aceptada, reglas `english-error` y `final-punctuation`
instantáneas, meta 0 ejercicios con error, familia errores, evalúa QUIZ, resoluciones regla y mesa, costo instantáneo
—por eso entra en la corrida base, como decidió José— y su herramienta en la tarjeta (B-01). Sin ligadura al plan: qué
tarea crea lo decide E (B-18).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: grammar
        _change: add
        visibility: public
        implementations:
          - name: GrammarAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: grammarChecker, type: GrammarChecker }
              - { name: sentenceReader, type: FinishedSentenceReader }
              - { name: localToolGate, type: LocalToolGate }
          - name: GrammarAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: grammarChecker, type: GrammarChecker }
              - { name: sentenceReader, type: FinishedSentenceReader }
              - { name: matchFilter, type: GrammarMatchFilter }
              - { name: finalPunctuation, type: FinalPunctuationCheck }
              - { name: issueClassifier, type: GrammarIssueClassifier }
              - { name: gateDecision, type: LocalToolGateDecision }
```

## Cómo se prueba y cómo se mide

Las pruebas no llaman a LanguageTool: usan un doble de `GrammarChecker` con las marcas que grabó el prototipo
(`scratchpad/ronda2-B/lt/marcas-us.tsv` y `marcas-gb.tsv`). Casos reales: «He ___ (own) a bike for a while.»
(`…3f81`), alta; «Have the neighbors been making ___ (the neighbors / make) noise?» (`…320a`), bloqueante y regla;
«A: She's choosing her clothers.» (`…33a1`), media; «…slept until 11.I slept…», baja y regla. La lectura se prueba con
ejercicios del curso: «They 're» queda «They're», la opción «-» no deja texto, la pista «was / were» no se lee y un
juicio con «not correct» no se lee. La prueba con LanguageTool de verdad lleva la etiqueta `herramienta-real`: la 6.8
marca «teached» y «explained us» en las dos variantes. El examen (R007) se rinde con `exam run grammar` y se mide con
`medir.py`: al menos 4 de los 47 de `escribir/gramatica.json`, y 7 de cada 10 reales en 50 marcas fuera del oro. Hasta
que se registre el examen de «LanguageTool 6.8 · su filtro», `grammar` queda sin evaluar, también en la corrida base
(B-19).

## Lo que no se mueve

`grammar` es de la familia de errores y no tiene ligadura al plan: el vocabulario sigue en 73,9 % con los niveles en
96,5 · 94,2 · 79,5 · 63,5, y el plan en 4.036 tareas. La oración plana y el largo de oración no cambian.

## Registrar FEAT-GRAMA

El parche registra la feature con su código para que `feature sync --feature FEAT-GRAMA` anote J001 después de aplicar;
la ubicación del journey es del qa-tester.

```architecture
modules:
  - name: audit-domain
    _change: modify
features:
  - id: FEAT-GRAMA
    code: F-GRAMA
```
