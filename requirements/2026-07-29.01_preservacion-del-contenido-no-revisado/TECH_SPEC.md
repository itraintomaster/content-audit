---
patch: FEAT-RPRES
requirement: 2026-07-29.01_preservacion-del-contenido-no-revisado
generated: 2026-07-29T18:40:00Z
---

# Tech Spec: Preservacion del contenido no revisado al aplicar una revision

La causa raiz esta localizada: el elemento revisado se **construye** en vez de
**derivarse del original**. `QuizSentenceParser.buildForm` arma un `FormEntity`
nuevo a partir de la lista de partes y nada mas; `kind`, `incidence`, `label` y
`name` nacen en el default de su tipo y ese form reemplaza al original. El camino
de persistencia no tiene la culpa: `loadForm`/`formToJson` manejan los cuatro
campos bien, y los 8.829 quizzes no revisados estan intactos.

De ahi las tres decisiones de este patch: (1) que el estado previo llegue al
punto de construccion, (2) donde vive —y con que granularidad— la declaracion de
"lo que la revision propone corregir", y (3) de donde sale el valor correcto para
reparar lo ya degradado.

Las tres journeys se anclan en audit-cli (`com.learney.contentaudit.journeys`,
junto a las de FEAT-LAPS y FEAT-KTLR) porque R001 exige verificar sobre el
contenido **persistido y recargado**: el dano ocurre en el traspaso entre etapas
y solo el camino completo aprobar → sustituir → escribir → releer lo expone.

## Hacer que el estado previo llegue al punto de construccion

El parser recibe solo el `quizSentence` y por eso no puede heredar nada. Las tres
alternativas eran: hacer el merge en el llamador (el deriver), pasar el form
original como base al converter, o un merge reflexivo generico sobre el snapshot.

Se elige **pasar el original como base al converter**. El merge en el deriver
seria exactamente la enumeracion que F-RPRES-R001 rechaza: cada campo nuevo de
`FormEntity` obligaria a tocar cada deriver, y el deriver vive en revision-domain
mientras que la forma del `FormEntity` es asunto de course-domain. El converter,
en cambio, es el unico componente con autoridad para decir que codifica el DSL y
que no: su contrato pasa a ser "devolve un form igual a `base` salvo en lo que el
`quizSentence` codifica". La preservacion queda estructural y por complemento en
el unico lugar que puede definir el complemento. Se conserva `parse(String)` para
los consumidores de analisis que genuinamente no tienen original (LexisCmd,
TokensCmd, CourseToAuditableMapper); el camino de revision usa `parseOnto`.

```architecture
modules:
  - name: course-domain
    _change: modify
    packages:
      - name: quizsentence
        _change: modify
        interfaces:
          - name: QuizSentenceConverter
            _change: modify
            exposes:
              - signature: "parseOnto(String quizSentence, FormEntity base): FormEntity"
                _change: add
```

El merge reflexivo generico sobre `CourseElementSnapshot` se descarto: escribir
por reflexion sobre entidades mutables es fragil, y duplica un conocimiento que
ya esta disponible estructuralmente en el converter.

## Fijar las partes nuevas del enunciado sin agregar contrato

F-RPRES-R006 exige que una parte que la correccion **agrega** al enunciado nazca
con atributos completos y homogeneos con los de las demas partes de su clase.
Los contratos vigentes ya la soportan: `base` es el `FormEntity` original entero,
de modo que `parseOnto` tiene a mano `base.sentenceParts` —todas las partes TEXT
y todas las CLOZE— para tomar de ahi lo que el DSL no codifica (`text` de una
parte CLOZE, `options` de una parte TEXT). No falta ningun dato en la firma.

Por eso **no se agrega contrato**. R006 pone el mecanismo explicitamente fuera de
alcance ("de que parte se copian, en que orden se recorre"), asi que materializarlo
como un puerto o un parametro de plantilla sobre-especificaria la regla y abriria
una costura que ningun consumidor pide (P3). La garantia queda estructural en el
mismo unico punto de construccion que R001, y su verificacion punta a punta ya
tiene lugar: la rama `partes_nuevas_completas` de J001.

Tampoco se toca `PreservationCheck`. Agregar partes esta **dentro** del alcance
declarado (`form.sentenceParts[]` es estructura cambiable para una correccion
lexica), asi que R006 no es un caso de preservacion sino de completitud de
contenido nuevo: exigirsela al guard obligaria a meterle una segunda semantica de
comparacion —"elemento nuevo de una lista" frente a "atributo que cambio"— dentro
de un contrato disenado para comparar por complemento. Es la misma frontera ya
declarada para el texto vacio de las partes CLOZE.

```architecture
modules:
  - name: course-domain
    _change: modify
    packages:
      - name: quizsentence
        _change: modify
        interfaces:
          - name: QuizSentenceConverter
            _change: modify
            exposes:
              - signature: "parseOnto(String quizSentence, FormEntity base): FormEntity"
                _change: add
```

## Declarar el alcance por tipo de correccion, no por propuesta

Resuelve **DOUBT-ALCANCE-DECLARADO** por **Opcion A**. `CorrectionScope` responde
"que atributos puede cambiar una correccion de este tipo sobre este target". La
Opcion B (declarado por cada propuesta) obliga a agregar un campo a
`RevisionProposal`, que se persiste en 3.564 artifacts ya escritos y del que
dependen FEAT-PIPRE y FEAT-CDIFF: es un costo alto para declarar algo que no
varia entre propuestas del mismo tipo — el alcance lo fija el deriver, no el
candidato. La Opcion C (implicito en el contenido propuesto) es lo que *ocurre*
en tiempo de ejecucion gracias a `parseOnto`, pero no deja nada contra que
verificar: el diff siempre esta contenido en si mismo.

La propiedad que decide es el comportamiento ante un tipo de correccion nuevo: un
`DiagnosisKind` sin alcance declarado devuelve conjunto vacio, de modo que un
deriver que olvide declararse falla ruidosamente en el check en lugar de degradar
contenido en silencio — que es exactamente lo que paso.

El retorno es `Set<ScopedField>`, no `Set<String>`: la firma se declara aqui como
baja de la version de paths sueltos y alta de la version tipada, porque la
primera ronda de este patch dejo la vieja en pie y una entrada leida como string
no puede expresar la distincion que la seccion siguiente exige.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: preservation
        _change: modify
        interfaces:
          - name: CorrectionScope
            _change: modify
            exposes:
              - signature: "changeableFields(DiagnosisKind kind, AuditTarget target): Set<String>"
                _change: delete
              - signature: "changeableFields(DiagnosisKind kind, AuditTarget target): Set<ScopedField>"
                _change: add
```

## Prohibir la entrada de alcance que denota un subarbol

Una primera version declaraba `form.sentenceParts` como una entrada suelta, con
la intencion de decir "la composicion del enunciado esta en alcance". Esa entrada
es una **contradiccion con F-LAPS-R013**, que excluye explicitamente "el texto
vacio que acompana a cada parte hueco": leida como subarbol, la entrada autoriza
justamente lo que la regla prohibe, y cualquier deriver futuro que degrade ese
texto pasa el guard limpio. Es la misma clase de error que el requerimiento
diagnostica en la formulacion vieja de R014 — una declaracion que sobre-aproxima.

`ScopedField` cierra el agujero por construccion: una entrada denota **un leaf o
la estructura de una lista, nunca un subarbol**. La distincion entre el texto de
una parte TEXT (que la correccion reescribe: es el DSL) y el de una parte CLOZE
(que el DSL no codifica) no se puede evitar — es una union discriminada real del
dominio. Se resuelve con un campo propio, `elementKind`, en vez de una gramatica
de filtros embebida en el string: asi el unico refinamiento admitido queda
acotado a nombrar una clase que el dominio ya declara como enum, y la
sobre-aproximacion es visible en la arquitectura en lugar de esconderse dentro de
una ruta.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: preservation
        _change: modify
        models:
          - name: ScopedField
            _change: add
            type: record
            fields:
              - { name: path, type: String }
              - { name: elementKind, type: String }
```

Alcance declarado resultante. `LEMMA_ABSENCE`/`QUIZ`: `sentences`,
`translation`, `form.sentenceParts[]` (estructura: longitud, orden y clase de
cada parte), `form.sentenceParts[].options`, y `form.sentenceParts[].text`
**solo con `elementKind = TEXT`**. `KNOWLEDGE_TITLE_LENGTH`/`KNOWLEDGE`: `label`,
`instructions`, `quizTemplates[].title`.

Quedan protegidos por complemento, entre otros: `form.kind`, `form.incidence`,
`form.label`, `form.name`, `form.sentenceParts[].text` con `elementKind = CLOZE`,
y el `title` del quiz — este ultimo es la mitad reciproca de F-RPRES-R004.

## Tipar los tres estados que la comparacion debe distinguir

F-RPRES-R002 no es una nota al pie de R001: es su criterio de identidad. Sin el,
una comparacion permisiva declara preservado un elemento donde `label` paso de
`""` a ausente y `incidence` de 1,0 a 0,0 — que es literalmente el dano medido.
Materializar los tres estados como un tipo, y no como una convencion en el codigo
de comparacion, impide que la permisividad se cuele por descuido en una futura
implementacion del check.

`PreservationViolation.path` es una ruta **instanciada** (`form.sentenceParts[2].text`),
no un patron: el check expande cada `ScopedField` sobre el elemento real antes de
comparar, y reporta donde efectivamente ocurrio la diferencia. Es la unica
diferencia entre este carrier y `ScopedField`, y por eso el campo se re-declara
aqui: la descripcion vigente lo confunde con el patron declarado.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: preservation
        _change: modify
        models:
          - name: AttributeState
            _change: add
            type: enum
            fields:
              - name: VALUE_PRESENT
              - name: EMPTY_PRESENT
              - name: ABSENT
          - name: PreservationViolation
            _change: modify
            type: record
            fields:
              - { name: elementId, type: String }
              - { name: path, type: String, _change: modify }
              - { name: beforeState, type: AttributeState }
              - { name: afterState, type: AttributeState }
              - { name: beforeValue, type: String }
              - { name: afterValue, type: String }
```

## Verificar sobre el curso entero, no sobre el elemento

`PreservationCheck` recibe el curso **antes y despues** de la sustitucion, no los
dos snapshots del elemento. Es lo que exigen R003 y R004: el radio de impacto se
mide sobre el curso y la unica propagacion admitida (titulo de los ejercicios)
cae fuera del elemento revisado. Un check a nivel elemento no podria ver ninguna
de las dos cosas.

El costo de recorrer 11.487 quizzes por cada approve se evita explotando una
propiedad de `CourseElementLocator.replace`: reconstruye solo el camino hasta el
nodo revisado y deja los demas nodos como la misma instancia. La verificacion de
R003 es entonces una comparacion por identidad referencial que corta en el primer
`==`; solo el subarbol reconstruido se compara estructuralmente.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: preservation
        _change: modify
        interfaces:
          - name: PreservationCheck
            _change: add
            exposes:
              - signature: "verify(CourseEntity courseBefore, CourseEntity courseAfter, CourseElementSnapshot revised, DiagnosisKind kind): List<PreservationViolation>"
```

## Abortar el apply antes de persistir cuando el check falla

El punto de enforcement es `approve`, y es uno solo. La secuencia queda: cargar
curso → `replace` → `verify` → escribir. Si `verify` devuelve violaciones, ni el
artifact pasa a APPROVED ni el curso se escribe: el nuevo `PRESERVATION_VIOLATED`
es un desenlace sin efectos, distinto de `APPROVED_APPLY_FAILED` (que sucede
*despues* de haber marcado el artifact).

**Consecuencia de secuencia, no opcional.** Con el guard puesto y el `title` fuera
del alcance lexico, `DefaultLemmaAbsenceProposalDeriver` —que hoy escribe
`title = plainSentences.get(0)`— hace fallar toda aprobacion LEMMA_ABSENCE. El
arreglo del deriver (dejar `title` del original y seguir escribiendo la oracion
corregida en `sentences`, mas usar `parseOnto` en lugar de `parse`) no agrega
contrato: `derive` ya recibe el `before` y ya calcula `plainSentences`. Pero deja
de ser una mejora y pasa a ser condicion para que el flujo funcione, asi que
guard y deriver tienen que entrar en el mismo cambio. Se verifico que ningun
analyzer puntua el titulo del quiz —`AuditableQuiz.label` solo se muestra en
`GetCmd`— de modo que dejarlo intacto no altera la simulacion what-if ni el
impact preview.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: ProposalDecisionOutcomeKind
        _change: modify
        type: enum
        fields:
          - name: PRESERVATION_VIOLATED
            _change: add
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultProposalDecisionService
            _change: modify
            requiresInject:
              - { name: preservationCheck, type: PreservationCheck }
```

## Declarar la propagacion del titulo como paso explicito del apply

F-RPRES-R004 podria implementarse dentro del deriver de titulo, copiando los
quizzes retitulados dentro del `KnowledgeEntity` propuesto. Se descarta: el
snapshot del artifact puede ser viejo, y reponer su lista de `quizTemplates`
resucitaria quizzes revisados despues — una violacion de R003 disfrazada de
propagacion. Es el mismo error que ya se corrigio una vez en `replaceKnowledge`,
que deliberadamente conserva los quizzes vivos del curso.

Por eso la propagacion se hace sobre el curso vigente y con una firma propia en
`CourseElementLocator`, no como efecto lateral escondido en `replace`. Que sea un
paso explicito que `approve` invoca solo en la rama KNOWLEDGE hace verdadera por
construccion la mitad reciproca de la regla: una correccion de ejercicio nunca
llega a este codigo.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: CourseElementLocator
        _change: modify
        exposes:
          - signature: "alignQuizTitles(CourseEntity course, String knowledgeId): CourseEntity"
            _change: add
```

## Reparar desde los artifacts, no desde valores canonicos

R005 exige reponer 2.658 quizzes sin inventar contenido. La tentacion es declarar
los valores canonicos (`kind = CLOZE`, `incidence = 1,0`, `label = ""`), pero eso
es exactamente materializar un valor por defecto, que R002 prohibe. La contraparte
integra existe dentro del sistema: el `elementBefore` del artifact de revision que
degrado el elemento. Se verifico sobre el arbol real que los 2.658 degradados
tienen contraparte integra en artifacts APPROVED y ninguno queda huerfano —
tomando el artifact **mas antiguo** por `nodeId`, porque en 384 casos hubo una
segunda revision cuyo `elementBefore` ya venia danado.

Reparar es entonces aplicar R001 retroactivamente con la misma `CorrectionScope`:
reponer del original todo atributo fuera del alcance de la correccion que el
artifact aplico. Por eso la reparacion no revierte revisiones — no toca lo que
esta en alcance — y por eso necesita contratos propios en vez de ser un script
externo: consume un puerto del dominio (`RevisionArtifactStore`) y la misma
declaracion de alcance que la guarda. `unrepairable` existe para que el caso
huerfano se informe en vez de resolverse con un default.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: preservation
        _change: modify
        models:
          - name: RepairReport
            _change: add
            type: record
            fields:
              - { name: course, type: CourseEntity }
              - { name: elementsInspected, type: int }
              - { name: elementsRepaired, type: int }
              - { name: restored, type: "List<PreservationViolation>" }
              - { name: unrepairable, type: "List<String>" }
        interfaces:
          - name: PreservationRepair
            _change: add
            exposes:
              - signature: "inspect(CourseEntity course): RepairReport"
              - signature: "repair(CourseEntity course): RepairReport"
```

## Esconder el motor detras de una unica costura

El contrato de preservacion es estable; su motor no. Se separan como ya lo estan
`consolidatedview` (publico) y `fielddiff` (interno): `preservation` solo tiene
puertos y carriers, `preservationengine` tiene la tabla de alcances, el comparador
y el reparador, todos package-private. `DefaultPreservationFactory` es la unica
clase publica del motor.

Guarda y reparacion comparten `CorrectionScope`, que es lo que las vuelve la misma
capacidad y justifica una sola factory con dos creates en lugar de dos costuras
separadas: si divergieran, divergiria el alcance declarado y R001 y R005 dejarian
de ser la misma regla aplicada hacia adelante y hacia atras.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: PreservationFactory
        _change: add
        stereotype: factory
        exposes:
          - signature: "createCheck(): PreservationCheck"
          - signature: "createRepair(RevisionArtifactStore artifactStore): PreservationRepair"
    patterns:
      - type: Factory
        interface: PreservationFactory
        implementations: [DefaultPreservationFactory]
    packages:
      - name: preservationengine
        _change: add
        visibility: internal
        implementations:
          - name: DefaultCorrectionScope
            _change: add
            implements: [CorrectionScope]
          - name: DefaultPreservationCheck
            _change: add
            implements: [PreservationCheck]
          - name: DefaultPreservationRepair
            _change: add
            implements: [PreservationRepair]
          - name: DefaultPreservationFactory
            _change: add
            visibility: public
            implements: [PreservationFactory]
```

## Exponer la reparacion como comando, no como paso automatico

R005 fija el resultado observable y el **momento** (despues de que rija la
preservacion), no el procedimiento. Un paso automatico decidiria el momento por
el operador y ademas volveria a correr en cada arranque sin necesidad. El comando
tiene dos modos porque son dos flujos distintos: `--dry-run` informa que se
repondria — y sirve de verificacion continua de que no queda pasivo — mientras
que el modo efectivo escribe.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: RepairCommand
        _change: add
        sealed: true
        exposes:
          - signature: "repair(String resource, String coursePath, boolean dryRun): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: RepairCmd
            _change: add
            implements: [RepairCommand]
            requiresInject:
              - { name: preservationRepair, type: PreservationRepair }
              - { name: courseRepository, type: CourseRepository }
```
