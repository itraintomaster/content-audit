---
patch: FEAT-PISTA-R006
requirement: 2026-09-30.03_pista-coherente
generated: 2026-10-01T03:20:00Z
---

# Tech Spec: La reescritura no pide palabras que nada anuncia (F-PISTA-R006)

Segundo parche de FEAT-PISTA. El primero (R001 a R005) ya está aplicado y su TECH_SPEC queda en la historia de git.
Diseñado contra el `sentinel.yaml` de 859e96af (`ronda1/integracion`). Se aplica después del parche de cierre de C
(FEAT-CIERRE), que toca otros paquetes de audit-domain —`judging` y los motores de los jueces— y ningún elemento de
`hintcoherence`.

## Una regla más en el mismo chequeo, sin puerto nuevo
R006 necesita lo mismo que R002: los tokens de la oración guardada con su lema (`AuditableQuiz.tokens`), la tabla de
formas irregulares y la comparación igual, flexionada o por lema. Todo eso ya vive en `DefaultHintAnswerCheck`, así que
la regla es un método más de `HintAnswerCheck` y no un colaborador nuevo: no cambia el constructor del analizador ni el
del proveedor, y no se inyecta nada. Recibe sólo el ejercicio: la oración de partida y la pista son las partes de texto
del enunciado, y la consigna es `AuditableQuiz.instructions`.

El analizador la llama sólo cuando `HintReader.shapeOf` dice `WHOLE_SENTENCE`. En un hueco dentro de la oración, el
verbo sin anunciar sigue siendo de R002, invariante 2, que a su vez nunca mira las oraciones enteras: las dos reglas no
miran nunca lo mismo.

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
      - signature: "checkRewrite(AuditableQuiz quiz): List<HintIssue>"
        _change: add
```

## Un tipo propio para la palabra que nada anuncia
`VERB_NOT_ANNOUNCED` dice «un verbo de la respuesta no está en la pista». R006 marca también sustantivos, adjetivos y
adverbios (*opinion*, *moment*), y los busca en la oración de partida y la consigna, no sólo en la pista: reusarlo haría
mentir al diagnóstico. Va `REWRITE_WORD_NOT_ANNOUNCED`, con *not announced* como los otros dos (lo que el alumno no ve
anunciado) y *rewrite* por la regla `rewrite-words`. No es `WORD_NOT_FROM_SOURCE`: la fuente no es sólo la oración de
partida, también la pista y la consigna.

Un hallazgo por palabra, como en R002: la identidad es la regla y el lema (F-HALL-R014). `HintIssue` no cambia de
forma. Cambian tres descripciones: `rule` y `word` también los llena R006, y `proposedHint` queda en null, porque no hay
arreglo por regla.

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
      - name: REWRITE_WORD_NOT_ANNOUNCED
        _change: add
    - name: HintIssue
      type: record
      _change: modify
      fields:
      - name: rule
        type: String
        _change: modify
      - name: word
        type: String
        _change: modify
      - name: proposedHint
        type: String
        _change: modify
```

## La gravedad, la mesa y la ficha, donde ya se deciden
- **Gravedad**: `HintCoherenceAnalyzer.severityOf` ya da *bloqueante* a todo lo que no es la pista que falta.
- **Resolución**: `DefaultHintAnswerCheck.issue` manda a la mesa `HINT_WORD_ABSENT` y ahora también el valor nuevo. Una
  regla no sabe si se arregla la respuesta —la reescritura fiel, con audio nuevo— o el enunciado (DOUBT-PISTA-RESPUESTA).
- **Ficha**: suma la regla `rewrite-words`, «F-PISTA-R006: la reescritura no pide palabras que no salen de la oración de
  partida, la pista o la consigna», de costo instantáneo. Las resoluciones siguen siendo *regla* y *mesa*, y la pregunta,
  la familia y el nombre de la prueba de la ficha no cambian.
- **Evidencia**: el ejercicio como lo ve el alumno, la respuesta y la línea ««opinion» no sale de la oración de partida,
  la pista ni la consigna. Va a la mesa: quien reescribe fiel da mal».

La descripción del paquete lo dice, y de paso cuenta la unión de R001 del 1/10 (por tipo y en el tema entero).

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: hintcoherence
    _change: modify
```

## Las listas cerradas, portadas del prototipo
Todo sale de `scripts/prototipo_pista_reescritura.py`, medido el 1/10:

- **Palabra de contenido**: NOUN, PROPN, VERB, ADJ, ADV o NUM según el tokenizador compartido, sin puntuación.
- **Fuentes**: las palabras de las partes de texto del enunciado (con su paréntesis, como «(the boy whispered)») y de la
  consigna. «8:00» cuenta como una sola, y un compuesto con guion también por partes, como lo corta el tokenizador.
- **Desplazamientos del estilo indirecto**: una palabra de la fuente anuncia sus reemplazos: *today* → *that day*,
  *tonight* → *that night*, *tomorrow* → *the next/following day*, *yesterday* → *the day before/previous*, *last* →
  *before/previous*, *ago* → *before/earlier*, *now* → *then*, *here* → *there*, *this* → *that*, *these* → *those*,
  *next* → *following/after*.
- **Lo que no se anuncia**: una lista cerrada de palabras de función que el tokenizador a veces marca como contenido
  (la negación, adverbios de grado y de foco como *very* o *just*, los interrogativos, *please*, *there*, *let*, *more* y
  *than*, *one/ones* y los pronombres indefinidos como *nothing* o *someone*); el auxiliar al final de la respuesta o en
  una que empieza con *so*, *neither* o *nor* («So had I.»); *have* seguido de participio (el perfecto); y *long*
  después de *how*.
- **Comparación**: la de R002, más la base que dejan los sufijos regulares (*drinking* → *drink*, *steadily* →
  *steady*, *simply* → *simple*) y la base de un irregular de la fuente (*drank* anuncia *drunk*).

## Cómo se mide y lo que no se mueve
Prototipo, curso del 29/9: 60 marcas entre 3.957 oraciones enteras (A1 10, A2 4, B1 34, B2 12); 56 de 60 reales, y 25 de
30 en una muestra nueva. En `escribir/pista.json` marca 11 de los 27 imposibles sin anotar como de pista, 10 que
hint-coherence no veía. Encendida, se mide igual que el primer parche: `analyze --analyzers hint-coherence` sobre una
copia del curso y `medir.py marcas.json --parte escribir --categoria pista`. La meta es la del REQUIREMENT: 8 de cada
10 marcas reales en una muestra revisada a mano.

Los 4 falsos del prototipo van a la mesa como límite conocido (la fórmula del pedido, el *did* de la pregunta de objeto
y *must* → *had to*). R001 a R004 no cambian, ni el journey, ni ningún número de vocabulario. Un ejercicio puede tener
R002 y R006 a la vez: «She / assist you.» → «She'd help you.» da «assist» por R002 y «help» por R006, y cuenta una sola
vez como ejercicio con error.

## Declarar las 5 pruebas de R006
Cada una con un caso real del curso del 29/9:

- **Del chequeo**: «have» en 24f7; sólo «practise» en 4186, porque *made* sale de la consigna; y nada en 447b, que
  trae el perfecto y *this* → *that*.
- **Del analizador**: «opinion» en 44f8, *bloqueante*, a la mesa, con la palabra como marca. Y «It ___ (be) soon.» →
  «'s going to rain», donde *rain* sigue siendo de R002 y no hay hallazgo de `rewrite-words`.

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
      - name: "should report a content word of a rewrite that comes neither from the sentence it rewrites nor from its hint or instructions, as «have» in «Polite request: I want to eat some cake. ___» answered «Could I have some cake?»"
        traceability: { feature: FEAT-PISTA, rule: F-PISTA-R006 }
      - name: "should take as announced the words the instructions name, so only «practise» is reported in «The coach encouraged the players to repeat the drill. ___» answered «The coach made the players practise the drill.» under «Crea oraciones utilizando 'let' o 'make'.»"
        traceability: { feature: FEAT-PISTA, rule: F-PISTA-R006 }
      - name: "should not report the grammar a rewrite brings by itself, as the perfect and the time shift of reported speech in «“I overslept this morning” (the boy whispered) ___» answered «The boy whispered that he had overslept that morning.»"
        traceability: { feature: FEAT-PISTA, rule: F-PISTA-R006 }
    - name: HintCoherenceAnalyzer
      _change: modify
      handwrittenTests:
      - name: "should raise a blocking finding for the panel, identified by the word, on a rewrite that asks for a word nothing announces, as «opinion» in «I think it's a good idea for her to see a dentist. ___» answered «She ought to see a dentist, in my opinion.»"
        traceability: { feature: FEAT-PISTA, rule: F-PISTA-R006 }
      - name: "should look for unannounced words only in answers that are the whole sentence, leaving a gap inside the sentence to R002, as «rain» in «It ___ (be) soon.» answered «'s going to rain»"
        traceability: { feature: FEAT-PISTA, rule: F-PISTA-R006 }
```

## Lo que hay que adaptar después de generate
- `HintCoherenceAnalyzer.fault`: el `switch` sobre `HintIssueKind` es exhaustivo; sin un caso para el valor nuevo, no
  compila. Es lo único que se rompe.
- `DefaultHintAnswerCheck`: generate le agrega `checkRewrite` con `UnsupportedOperationException`. Se llena con el
  puerto del prototipo, y `issue()` manda el valor nuevo a la mesa.
- `HintCoherenceAnalyzer.onQuiz`: llama a `checkRewrite` en `WHOLE_SENTENCE` y suma sus hallazgos a los de `check`.
- `HintCoherenceAnalyzerProvider`: la ficha suma `rewrite-words`, y su prueba espera cinco reglas.
- Las 5 pruebas: generate agrega sus stubs en `DefaultHintAnswerCheckTest` y `HintCoherenceAnalyzerTest`, porque su
  etiqueta F-PISTA-R006 todavía no está en esos archivos.
