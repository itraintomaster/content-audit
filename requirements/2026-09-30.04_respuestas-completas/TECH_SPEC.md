---
patch: FEAT-RESPC-NOMBRES-Y-CORRECTOR-NUEVO
requirement: 2026-09-30.04_respuestas-completas
generated: 2026-10-01T04:00:00Z
---

# Tech Spec: Dos nombres de prueba, y el corrector de las cuatro reglas medido al lado (FEAT-RESPC)

Segundo parche de FEAT-RESPC. El primero ya está aplicado y su TECH_SPEC queda en la historia de git. Diseñado contra
el `sentinel.yaml` de 859e96af y validado también contra el de `ronda1/integracion` con FEAT-CIERRE aplicado. Trae dos
cosas que se aplican juntas:

1. Dos nombres de prueba que pasan a decir lo que hace el código.
2. La copia del corrector de las cuatro reglas del 1/10, al lado del que está en producción, para medir el antes y el
   después.

## Por qué los dos nombres
Desde 3a3238ed, una forma de una clase que el corrector en producción ya iguala y que igual rechaza es un límite del
corrector. El hallazgo sólo ordena, nombra la regla que le falta al corrector y nunca propone «|» (brief 101, José 30/9;
el coordinador, 1/10), y la ficha sumó *sólo ordena* a sus resoluciones. El código y el cuerpo de las pruebas ya lo
hacen, pero dos nombres declarados seguían diciendo lo de antes:

| Prueba | Decía | Dice |
|---|---|---|
| «has trained», R005 | se resuelve agregándola después de «\|» | sólo ordena y nombra la regla que le falta al corrector |
| La ficha, R006 | resuelve por regla o en la mesa | por regla, en la mesa o sólo ordenando |

Una prueba declarada se identifica por su nombre: cada una se borra con el nombre viejo y se agrega con el nuevo, con la
misma regla. R006 del REQUIREMENT ya dice las tres resoluciones; R005 no cambia.

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: answercompleteness
    _change: modify
    implementations:
    - name: AnswerCompletenessAnalyzer
      _change: modify
      handwrittenTests:
      - name: "should resolve by adding it after «|» a missing form the new corrector still rejects, as «has trained» if «He ___ (train) hard today.» only accepted «'s trained», a limit of the corrector"
        _change: delete
      - name: "should only rank a missing form of a class the corrector in production equates and still rejects, naming the rule the corrector lacks instead of adding it after «|», as «has trained» if «He ___ (train) hard today.» only accepted «'s trained»"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R005 }
    - name: AnswerCompletenessAnalyzerProvider
      _change: modify
      handwrittenTests:
      - name: "should describe answer-completeness in the catalog as an errors analyzer that evaluates exercises, resolves by rule or at the panel and costs instant, asking «¿El corrector acepta todas las formas de la respuesta, y ninguna consigna pide una?»"
        _change: delete
      - name: "should describe answer-completeness in the catalog as an errors analyzer that evaluates exercises, resolves by rule, at the panel or by ranking only, and costs instant, asking «¿El corrector acepta todas las formas de la respuesta, y ninguna consigna pide una?»"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R006 }
```

## La tabla dice qué reglas tiene su corrector
José aprobó desplegar cuatro reglas nuevas del corrector de Go (ittm `claude/corrector-cuatro-reglas`, f54585212,
cuatro commits sobre 66b8b2272) si se mide el antes y el después. Las cuatro son código, no tabla. Por eso la copia de
la tabla dice la versión de su corrector y, en `rules`, cuáles tiene, como ya lo hacía `invertsNegativeQuestions`: el
código las imita y la tabla dice cuáles van, sin una bandera en el código.

| Regla | Qué hace el corrector | Commit |
|---|---|---|
| `AMBIGUOUS_CONTRACTIONS` | «'s» antes de un participio sin objeto y «'d» antes de un participio igual a la base valen por cualquiera de sus formas largas cuando los escribe el alumno; en la aceptada quedan como están escritos | ea7bc4a48 |
| `STANDARD_IRREGULAR_PARTICIPLES` | 48 participios más (overslept, undergone) y las bases en -ing que no son gerundios (bring) | f837c6267 |
| `TILL_OWN_SUBJECTS` | till es el verbo sólo después de I, you, he, she, we o they | f6a9a48eb |
| `OPTIONAL_PARENTHESES` | lo que va entre paréntesis en una aceptada es opcional, entero o nada | f54585212 |

`rules` es una lista de un enum y no de textos: una regla que content-audit no sabe imitar para la corrida al leer la
tabla, como la falta del archivo. La tabla en producción (`formas-equivalentes.json`, #1191) y la vieja no traen
ninguna; `formas-equivalentes-corrector-nuevo.json` trae las cuatro y la tabla del #1191 igual. El día que se despliegue,
la nueva pasa a ser `formas-equivalentes.json`, como pasó con el #1191.

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: correctormatch
    _change: modify
    models:
    - name: FormEquivalenceTable
      type: record
      _change: modify
      fields:
      - name: rules
        type: List<CorrectorRule>
        _change: add
    - name: CorrectorRule
      type: enum
      _change: add
      fields:
      - name: AMBIGUOUS_CONTRACTIONS
      - name: STANDARD_IRREGULAR_PARTICIPLES
      - name: TILL_OWN_SUBJECTS
      - name: OPTIONAL_PARENTHESES
```

## Cómo imita el corrector las cuatro reglas
`DefaultCorrectorMatcher` es la copia de `written_answer*.go`, y cada regla entra donde entra en Go:

- **La lectura larga** devuelve también las otras lecturas de las contracciones ambiguas que escribió el alumno, a lo
  sumo cuatro (`readLongs`). La gramática devuelve dos formas («is» y «has»; «had» y «would») donde el #1191 devolvía
  ninguna. Sin la regla, las dos quedan como están escritas: es el #1191, igual.
- **`accepts`** es el `Matches` de Go: el alumno es el receptor. Sus lecturas valen contra la forma larga de la aceptada,
  y la aceptada, con `OPTIONAL_PARENTHESES`, vale también con cada tramo entre paréntesis entero o sin él.
- **`sameAnswer`**, que usan la opción múltiple, los datos y los duplicados, pregunta si dos textos son la misma
  respuesta: lo son si el corrector toma cualquiera de los dos por el otro. Sin las reglas es la comparación simétrica
  de siempre.
- **`canonicalForms`** sigue siendo la forma letra por letra y la larga, sin las lecturas ni los paréntesis.
- **Las listas cerradas nuevas** (los 48 participios, los iguales a la base y los que no son gerundios) y los sujetos de
  till van al lado de las de siempre, y cada una se mira sólo con su regla.

## Los casos de oro de cada corrector
Los 192 casos de oro de la rama, en sus seis archivos, se copian tal cual a `casos-de-oro-corrector-nuevo/`. Los 153
del #1191 siguen en `casos-de-oro/`. Las dos carpetas tienen «hoy», la columna del corrector viejo, letra por letra, y
«acepta», la del corrector de su carpeta. No hay columna del #1191 en los 192: 326a, por ejemplo, tiene la misma regla
until-till que los casos de antes, y el #1191 lo rechaza. Por eso cada corrector prueba los suyos.

## Las pruebas
- **Los casos de oro del corrector nuevo**: los 192, con la copia de su tabla, que nombra las cuatro reglas.
- **La duda favorece sólo al alumno**: «'s forgotten» vale por «has forgotten», y «is trained» no vale por «'s trained».
- **La misma respuesta en cualquier orden**: para la opción múltiple y las copias de un ejercicio.
- **Las reglas que lee la tabla**: las cuatro en la nueva, ninguna en la de producción, y una desconocida para la
  corrida.
- La de «hoy», que ya existe, pasa a leer las dos carpetas: su nombre ya dice «every golden case».

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: correctormatch
    _change: modify
    implementations:
    - name: DefaultCorrectorMatcher
      _change: modify
      handwrittenTests:
      - name: "should decide every golden case of the corrector with the four rules of 1/10 as its «acepta» column says with the copy of its table, which names those rules, as «'s forgotten» for «He ___ (forget) to pay.» and «Peter was admired.» for «Peter was admired (by Mary).»"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R001 }
      - name: "should give the benefit of the doubt only to the student, so an ambiguous contraction counts as any of its long forms when the student writes it and stays as written in the accepted answer, as «'s forgotten» for «has forgotten» but not «is trained» for «'s trained»"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R001 }
      - name: "should take two texts as the same answer when the corrector matches them in either order, so «He's forgotten to pay.» and «He has forgotten to pay.» are one answer with the four rules and two with the corrector in production"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R001 }
- name: vocabulary-infrastructure
  _change: modify
  packages:
  - name: correctortable
    _change: modify
    implementations:
    - name: FileSystemFormEquivalenceTableSource
      _change: modify
      handwrittenTests:
      - name: "should read from «reglas» the rules that are code of the version of the corrector, the four in the copy of the new corrector and none in the one in production, and stop at a rule it does not know"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R001 }
```

## El antes y el después, sobre la foto del 29/9
Medido en una copia de la integración con este parche aplicado y generado, con el código del párrafo de arriba. Se
corrió `analyze` sobre `db/english-course` de la foto del 29/9 con `formas-equivalentes.json` (#1191) y con la tabla
nueva.

| Analizador | Ejercicios con hallazgo, antes → después | Qué cambia |
|---|---|---|
| answer-completeness | 710 → 694 | Se van los 16 límites del corrector, que sólo ordenaban: 15 de `contracted-and-long-forms` y 1 de `spellings-and-till` (326a). Los que cuentan como error siguen en 694, todos pedidos de forma. |
| multiple-choice-validity | 354 → 354 | Nada |
| data-coherence | 102 → 102 | Nada |
| exact-duplicates | 23 → 23 | Nada |

Por nivel, answer-completeness pasa de 554 a 554 en A1, de 106 a 100 en A2, de 24 a 20 en B1 y de 26 a 20 en B2.

**Las 54 cuentas del corrector de Go coinciden.**
- answer-completeness ve 16:
  - las 14 contracciones ambiguas: los 10 límites de antes, más 281d, 3ec9, 3ecd y 3ee2, que salen porque el generador
    ahora contrae have después de not (5b765813);
  - 447b, por los participios;
  - 326a, por till.
- Los 38 de los paréntesis no los ve, porque el generador no escribe formas sin paréntesis: no son una clase de R002 a
  R004. Un censo con la misma comparación los cuenta: las 38 aceptadas con paréntesis del curso, todas de B1,
  pasan a aceptar la respuesta con el agente y sin él.

## Lo que hay que adaptar después de generate
- `FileSystemFormEquivalenceTableSource`: generate completa el constructor con `null`. Lo reemplaza la lista leída de
  «reglas».
- `DefaultCorrectorMatcher`: las cuatro reglas. No cambia ninguna firma pública.
- `DefaultCorrectorMatcherTest.table(path)` construye la tabla con cuatro argumentos, así que no compila hasta que lee
  «reglas».
- Las cuatro pruebas nuevas se escriben a mano. generate no agrega sus stubs, porque sus etiquetas ya están en esos
  archivos (bug 2026-07-22-01); tampoco agrega los de los dos nombres nuevos, y avisa que conserva los métodos viejos.
  Los dos se renombran a mano.
