---
patch: FEAT-PISTA-R007
requirement: 2026-09-30.03_pista-coherente
generated: 2026-10-01T16:00:00Z
---

# Tech Spec: La pista no regala la respuesta (F-PISTA-R007)

Tercer parche de FEAT-PISTA. Los dos anteriores (R001 a R005, y después R006) ya están aplicados y sus TECH_SPEC quedan en la historia de git. Este reemplaza el de R006 con `--replace`: narra sólo este parche, diseñado contra el `sentinel.yaml` real, con R001 a R006 ya adentro. Todo el cambio vive en el paquete `hintcoherence` de `audit-domain`; ningún otro módulo ni paquete se toca.

## Un método más del mismo chequeo, con la forma del ejercicio como dato
R007 necesita, igual que R002, comparar contra los tokens y lemas de la oración guardada y la tabla de irregulares — todo eso ya vive en `DefaultHintAnswerCheck`. No hay colaborador nuevo ni puerto nuevo: es un método más de `HintAnswerCheck`, como `checkRewrite` lo fue para R006. La diferencia con `checkRewrite` es que R007 no vive de un solo lado del `ExerciseShape`: regala tanto el hueco dentro de la oración («(is)» → «is») como el enunciado con «/» de una respuesta que es la oración entera («Invest time + gerundio»). Por eso `checkGiveaway` recibe la forma del ejercicio como parámetro — el analizador ya la tiene en `onQuiz` — en vez de restringirse a una sola rama como hace `checkRewrite`. La lectura de la pista por hueco reusa los ayudantes de paquete que ya tiene `DefaultHintReader` (las partes, los huecos, las primeras respuestas); no hace falta ensanchar `HintReader` ni sumar un modelo, porque esos ayudantes ya son package-private dentro de `hintcoherence`.

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: hintcoherence
    _change: modify
    interfaces:
    - name: HintAnswerCheck
      sealed: false
      _change: modify
      exposes:
      - signature: "checkGiveaway(AuditableQuiz quiz,ExerciseShape shape): List<HintIssue>"
        _change: add
```

## Un tipo propio, porque ninguno de los que hay dice esto
`HINT_WORD_ABSENT` (R002) dice lo contrario: que una palabra de la pista NO está en la respuesta. `MISSING_HINT` (R001) dice que no hay pista. `REWRITE_WORD_NOT_ANNOUNCED` (R006) dice que la respuesta trae una palabra que nada anuncia. R007 es un cuarto caso, distinto de los tres: la pista SÍ está, dice exactamente la respuesta, y por eso no sirve. Reusar cualquiera de los tres mentiría en el diagnóstico.

Va `HINT_GIVEAWAY`, no `HINT_GIVES_ANSWER` como proponía el coordinador: las seis variantes que ya existen nombran un estado (`MISSING_HINT`, `..._ABSENT`, `..._NOT_ANNOUNCED` ×3, `..._MISMATCH`), nunca una acción en tercera persona — `HINT_GIVES_ANSWER` sería la única cláusula sujeto-verbo-objeto del enum. `GIVEAWAY` es sustantivo, como `MISMATCH` y `ABSENT`, y es además el término habitual en diseño de ejercicios para una pista que regala la respuesta. Mismo criterio para el id de la ficha: `hint-giveaway` en vez de `hint-gives-answer`, dos palabras como los otros cuatro (`missing-hint`, `hint-words`, `hint-subject`, `hint-negation`, `rewrite-words`).

La gravedad es *alta*, no *bloqueante*: una pista que regala no vuelve el ejercicio imposible —el alumno puede seguir contestando bien—, sólo deja de enseñar nada, el mismo criterio que ya tiene `MISSING_HINT` (F-HALL-R002). `HintCoherenceAnalyzer.severityOf` mapea `HINT_GIVEAWAY` al mismo nivel.

`MISSING_HINT` cambia de descripción porque deja de ser siempre *regla*: cuando la única pista que se puede proponer —la respuesta misma— regalaría, no hay propuesta y el hallazgo pasa a *mesa*. La detección de R001 no cambia, sólo su arreglo.

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: hintcoherence
    _change: modify
    models:
    - name: HintIssueKind
      type: enum
      _change: modify
      fields:
      - name: MISSING_HINT
        _change: modify
        description: "Sin pista en un tema donde tres cuartos o más la llevan (R001). Regla; mesa si la única pista que sale de la respuesta la regala (R007)."
      - name: HINT_GIVEAWAY
        _change: add
        description: "La pista dice lo mismo que una respuesta aceptada de su hueco y no deja nada para decidir: sin verbo, o con alguno conjugado (R007). Regla si el verbo se puede llevar a forma base; mesa si no se conjuga, o es el enunciado con «/» de una respuesta que es la oración entera."
```

## Resolución por tipo, salvo cuando no hay pista que proponer
`DOUBT-PISTA-CUANDO-MESA` ya resuelve la resolución por tipo: `HINT_WORD_ABSENT` y `REWRITE_WORD_NOT_ANNOUNCED` van siempre a la mesa, el resto por regla. R007 no agrega un eje nuevo — agrega una excepción dentro del eje que ya hay: `MISSING_HINT` y `HINT_GIVEAWAY` son por regla, pero sólo cuando hay una pista segura para proponer. Si no la hay (el candidato no se conjuga, o es el enunciado con «/»), el mismo hallazgo resuelve en la mesa, sin volverse un tipo nuevo. `HintIssue.resolution` ya documentaba "según el tipo"; ahora documenta también esa salida de emergencia. `proposedHint` sigue siendo el campo que decide: null es la señal de que no hay arreglo seguro, para el tipo que sea — por eso `word` suma un caso (la pista que regala, normalizada) y `rule` suma R007 a la lista que ya tenía.

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: hintcoherence
    _change: modify
    models:
    - name: HintIssue
      type: record
      _change: modify
      fields:
      - name: rule
        type: String
        _change: modify
        description: "La regla de FEAT-PISTA que no se cumple (F-PISTA-R001 a R004, R006 y R007)."
      - name: word
        type: String
        _change: modify
        description: "Lo que marca: la palabra de la pista, el verbo sin anunciar, el sujeto, en su lema la palabra de la reescritura que nada anuncia, o la pista que regala, normalizada. Es la marca de la identidad del hallazgo."
      - name: proposedHint
        type: String
        _change: modify
        description: "La pista que arregla el hallazgo por regla —para R001 a R004, la que sale de la primera respuesta; para R007, la misma pista—, siempre con los verbos en forma base: nunca una que regale la respuesta. El audio no cambia. Null en R006, y en R001 y R007 cuando la única pista posible regala la respuesta."
      - name: resolution
        type: FindingResolution
        _change: modify
        description: "RULE o PANEL según el tipo (DOUBT-PISTA-CUANDO-MESA); un tipo por regla puede resolver PANEL igual cuando no hay pista que proponer sin regalar (R007)."
```

## Lo que mide el prototipo y lo que no se mueve
Medido el 1/10 sobre el curso del 29/9, antes de la tanda: 167 pistas iguales a alguna respuesta aceptada de su hueco; 139 en forma base (sobre todo los condicionales de B1, «If you ___ (freeze) water…» → «freeze»), y 28 regalan — A1 0, A2 23, B1 3, B2 2. De esas 28: 20 son de *Posición del adjetivo* (A2, los adjetivos ya en el orden de la respuesta), 3 de *Invest time + gerundio* (A2, el enunciado con «/» igual a la respuesta) y 1 es un verbo conjugado que la regla arregla sola. Es el número del prototipo; lo reemplaza el del analizador cuando corra.

R001 a R004 y R006 no cambian, ni el journey: R007 queda fuera de `F-PISTA-J001`, igual que R006 — un camino nuevo cambiaría el journey generado, y su prueba son los `handwrittenTests`, no el journey. Ningún número de vocabulario se mueve.

## Declarar las 16 pruebas de R007
Dieciséis casos reales del curso del 29/9, en los tres lugares donde actúa la regla:

- **Del chequeo** (`DefaultHintAnswerCheck`, 10 pruebas): 2572 (*eggs*) sin propuesta, a la mesa; 2170 «(is)» → «(be)»; 1ecd «(love)» no regala, ya en forma base; cuatro que no coinciden con su hueco (2580, 2596, 256f, 2021); los bordes de la comparación —mayúsculas, puntuación, toda respuesta aceptada y no sólo la primera, la pista con «/» como una frase, el orden que importa en «(I / am)» contra «Am I»—; los dos huecos de 4797, donde sólo el segundo regala; la propuesta en forma base para una pista que falta (2170 y «___ below you?») y la falta de propuesta para 2572; las pistas ya en forma base que quedan intactas (2031, 20ad, 20b1, 215c); y el enunciado con «/» de *Invest time + gerundio*, que regala sin arreglo por regla.
- **Del analizador** (`HintCoherenceAnalyzer`, 3 pruebas): el hallazgo de 2572, alto, a la mesa, sin «Pista propuesta» en la evidencia; el de 2170, alto, por regla, con «(be)» propuesta; y que R001 sigue marcando los mismos ejercicios de *Adverbios de frecuencia* (17 de 20) aunque 2572 ya no se proponga por regla.
- **De la regla de arreglo** (`HintCoherenceRuleFix`, 3 pruebas): arregla sólo el segundo hueco de 4797, sin tocar el primero; escribe la forma base al completar una pista que falta (2170 y «___ below you?»); y no escribe nada en 2572, donde no hay pista segura.

Sin camino nuevo en F-PISTA-J001: el journey generado no cambia.

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: hintcoherence
    _change: modify
    implementations:
    - name: DefaultHintAnswerCheck
      _change: modify
      handwrittenTests:
      - name: "should raise a HINT_GIVEAWAY sent to the panel, with no hint to propose,\
          \ when a hint that is not a verb already says the answer of its gap, as\
          \ «(always)» in «We ___ (always) eat eggs every morning.» answered «always»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should raise a HINT_GIVEAWAY fixed by rule with the hint's base form\
          \ when a conjugated verb already says the answer of its gap, as «(is)» in\
          \ «Why ___ (is) the beach going to close?» answered «is», proposing «(be)»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should find no HINT_GIVEAWAY when every verb of the hint is already\
          \ in base form even though it says the answer, as «(love)» in «I ___ (love)\
          \ a lot of vegetables.» answered «love»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should find no HINT_GIVEAWAY when the hint says something other than\
          \ the accepted answers of its gap, as «(always)» not matching «am always»\
          \ in «I ___ in the kitchen. (always)», «(100%)» not matching «always» in\
          \ «I ___ drink tea in the morning. (100%)», «(always)» not matching the\
          \ whole sentence in «She does her homework after school. (always) ___»,\
          \ and «(I / am)» not matching «Am I» in «___ (I / am) below you?»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should normalize case, punctuation and spacing and match any accepted\
          \ answer of the gap, not only the first, reading a multi-word hint joined\
          \ by a slash as one phrase, as «(Is.)» giving away an answer accepted as\
          \ «'s|is», and «(I / am)» giving away an answer «I am» but not «Am I», where\
          \ the order differs, proposing «(I / be)» when it gives away"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should compare the hint of each gap only with the accepted answers\
          \ of that same gap, as «(are)» giving away the second gap's answer and proposing\
          \ «(be)», while «(play)» does not give away the first gap's answer «might\
          \ play», in «We ___ (play) cards again, if we ___ (are) bored.»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should propose the base form of the answer's verb when completing a\
          \ missing hint, as «(be)» for «Why ___ the beach going to close?» answered\
          \ «is», and «(I / be)» for «___ below you?» answered «Am I»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should propose no hint for a missing hint when the only possible hint\
          \ is the answer itself, as «We ___ eat eggs every morning.» answered «always»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should find no HINT_GIVEAWAY and leave the hint drawn from the first\
          \ answer unchanged when every word of an already-complete hint is in base\
          \ form, as «(you / see)» for «Who ___ (I / see)?» answered «are you seeing»\
          , «(that / be)» for «What ___ (it / be)?» answered «is that», and «(be /\
          \ rain)» for «It ___ (be) soon.» answered «'s going to rain»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should also give away the answer through the whole-sentence statement\
          \ of slash-separated words, never fixed by rule, as in Invest time + gerundio,\
          \ whose statement already conjugates «'re» and «going»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
    - name: HintCoherenceAnalyzer
      _change: modify
      handwrittenTests:
      - name: "should raise a high finding at the panel, identified by the hint, when\
          \ the hint gives away the answer and does not conjugate, as «We ___ (always)\
          \ eat eggs every morning.» answered «always», scoring 0 with no «Pista propuesta»\
          \ in its evidence"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should raise a high finding by rule proposing the base form when the\
          \ hint gives away the answer as a conjugated verb, as «Why ___ (is) the\
          \ beach going to close?» answered «is», proposing «(be)»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should keep sending the missing hint to the panel without changing\
          \ which exercises R001 marks, when the only possible hint is the answer\
          \ itself, as «We ___ eat eggs every morning.» in Adverbios de frecuencia,\
          \ 17 of 20 of the topic with a hint"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
    - name: HintCoherenceRuleFix
      _change: modify
      handwrittenTests:
      - name: "should fix a giveaway hint with its base form only in its own gap without\
          \ changing what is heard, as «(are)» becoming «(be)» in the second gap of\
          \ «We ___ (play) cards again, if we ___ (are) bored.», leaving «(play)»\
          \ as it was"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should write the base form of the verb when it adds a missing hint,\
          \ as «(be)» for «Why ___ the beach going to close?» and «(I / be)» for «\
          ___ below you?»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
      - name: "should decline instead of writing a hint that would give away the answer,\
          \ leaving the exercise as it was, as «We ___ eat eggs every morning.» answered\
          \ «always»"
        traceability:
          feature: FEAT-PISTA
          rule: F-PISTA-R007
```

## Lo que hay que adaptar después de generate
- `HintCoherenceAnalyzer.fault`: el `switch` sobre `HintIssueKind` es exhaustivo; sin un caso para `HINT_GIVEAWAY` no compila. Es lo único que rompe la build.
- `DefaultHintAnswerCheck`: generate agrega `checkGiveaway` con `UnsupportedOperationException`. Se llena con la lectura de pista por hueco (los ayudantes de `DefaultHintReader`) y la comparación normalizada —minúsculas, sin puntuación, espacios simples— contra cada respuesta aceptada de ese hueco (`options` cortadas por «|»).
- `HintCoherenceAnalyzer.onQuiz`: llama a `checkGiveaway` con la forma del ejercicio cuando el ejercicio tiene pista, y suma sus hallazgos a los de `check` y `checkRewrite`.
- `severityOf`: suma `HINT_GIVEAWAY` → alta.
- `DefaultHintAnswerCheck.issue` y `propose`: `issue` decide RULE o PANEL mirando si `propose` devolvió una pista. `propose`, para `MISSING_HINT`, deja de devolver el candidato —ya con el verbo en forma base— cuando éste sigue siendo igual a la respuesta sin dejar nada para decidir; devuelve null en su lugar. Para `HINT_GIVEAWAY` propone la misma pista con cada verbo conjugado llevado a su forma base (no arma una pista nueva desde la respuesta, como sí hace para R001 a R004).
- `HintCoherenceRuleFix` (FEAT-TANDA, hace lo que pide F-PISTA-R005): al aplicar un `HINT_GIVEAWAY` por regla, cambia sólo la pista de ese hueco por `proposedHint`; nunca escribe una pista que siga regalando.
- `HintCoherenceAnalyzerProvider`: la ficha suma la tarjeta `hint-giveaway`, «F-PISTA-R007: la pista no regala la respuesta si el alumno todavía tiene que decidir algo», mismas resoluciones (regla y mesa) y costo instantáneo. Su prueba («should describe hint-coherence in the catalog...») pasa de esperar cinco reglas a esperar seis.
