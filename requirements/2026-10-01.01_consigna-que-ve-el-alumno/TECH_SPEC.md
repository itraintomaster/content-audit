---
patch: FEAT-CALUM
requirement: 2026-10-01.01_consigna-que-ve-el-alumno
generated: 2026-10-01T05:00:00Z
---

# Tech Spec: el juez de consigna lee el ejercicio como lo ve el alumno

Un solo cambio de contrato: el constructor del sujeto del juez de consigna pasa a recibir el renderizador del alumno, como
los de traducción y sentido, una sola respuesta y opción múltiple. Las dos fábricas que lo crean se lo pasan. El resto son
cuerpos y nombres de prueba.

## Inject QuizStudentViewRenderer into the consigna subject builder and its two factories

Hoy el constructor del sujeto arma el contenido con la etiqueta y la consigna del tema y con el formato interno del curso
(`TEXT:…:|CLOZE::…|`). Con el renderizador inyectado arma, en este orden y nada más:
- `cefrLevel` y `topic`, como hoy;
- `title` e `instructions`, de la copia del ejercicio, que son los que muestra la app;
- `exercise`, el enunciado de la vista;
- `acceptedAnswers`, una línea por hueco: «Hueco N: a | b»;
- `options`, sólo en la opción múltiple: sus opciones en el orden del alumno, con la correcta marcada; si no, vacío.

Son las mismas claves que usan los otros jueces para lo mismo. Un dato ausente va vacío, nunca null.

`buildFromView` pasa la vista de un candidato por el mismo renderizador, con un `AuditableQuiz` armado con sus partes,
su opción múltiple, su título y su consigna. Así el mismo ejercicio da el mismo contenido por los dos caminos
(F-QICOR-R005).

Las dos fábricas lo reciben inyectado, como los proveedores de los otros jueces (gancho C-15).

```architecture
modules:
- name: audit-domain
  packages:
  - name: quizinstructionengine
    implementations:
    - name: DefaultQuizInstructionSubjectBuilder
      requiresInject:
      - name: studentViewRenderer
        type: QuizStudentViewRenderer
        _change: add
      _change: modify
    - name: DefaultQuizInstructionAnalyzerFactory
      requiresInject:
      - name: studentViewRenderer
        type: QuizStudentViewRenderer
        _change: add
      _change: modify
    - name: DefaultQuizInstructionComplianceCheckerFactory
      requiresInject:
      - name: studentViewRenderer
        type: QuizStudentViewRenderer
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Pass the title and instructions of the quiz copy in revise-instructions

`QuizInstructionReviser` revalida el original y `InstructionComplianceCriterion` verifica el candidato. Hoy los dos le
pasan a la vista la etiqueta y la consigna del tema. Pasan a pasar las de la copia de cada uno (`getTitle()` y
`getInstructions()` del ejercicio): es lo que el alumno va a ver. Son cuerpos, no cambia ninguna firma.

El corrector sigue recibiendo la consigna del tema (DOUBT-CALUM-CORRECTOR).

## Rename the tests that said the knowledge or the internal format

Siete nombres declarados del constructor del sujeto y uno del criterio decían «la consigna del tema» o «el formato de
antes». Se borran con el nombre viejo y se agregan con el que dice lo nuevo. Se suma una prueba al revisor para la
revalidación con la copia.

Dos pruebas del analizador fijaban F-JEXAM-R007, que FEAT-CALUM deja sin efecto:
- un curso juzgado antes de FEAT-JEXAM se resuelve sin consultar;
- cada ejercicio se busca con la huella de agosto.

La huella cambia a propósito, así que las dos se borran y se agregan las que dicen lo nuevo:
- el ejercicio juzgado antes de FEAT-CALUM se vuelve a consultar, con tope y versión aprobada;
- la huella es la del ejercicio como lo ve el alumno.

La prueba de que el juez no lee la traducción ni la miniteoría sigue igual.

```architecture
modules:
- name: audit-domain
  packages:
  - name: quizinstructionengine
    implementations:
    - name: DefaultQuizInstructionSubjectBuilder
      handwrittenTests:
      - name: "should hand the judge the cefr level, the topic, the knowledge label,\
          \ the knowledge instructions and the whole quiz with all its parts and accepted\
          \ options"
        _change: delete
      - name: "should hand the judge the cefr level, the topic, the title and the\
          \ instructions the app shows, the statement with its gaps and hints, the accepted\
          \ answers of each gap and the options, in that order and nothing else"
        traceability:
          feature: FEAT-CALUM
          rule: F-CALUM-R001
      _change: modify
    _change: modify
  _change: modify
- name: revision-domain
  packages:
  - name: engine
    implementations:
    - name: QuizInstructionReviser
      handwrittenTests:
      - name: "should revalidate the original quiz with the title and the instructions\
          \ of its copy, the ones the student sees"
        traceability:
          feature: FEAT-CALUM
          rule: F-CALUM-R002
      _change: modify
    _change: modify
  _change: modify
```

El resto de los nombres está en `architectural_patch.yaml`.

## Add quizInstructions to the correction context, for the corrector

El corrector de `revise-instructions` recibe hoy, como «consigna que debe cumplir», la del tema: el campo
`knowledgeInstructions` del contexto (F-CALUM-R003). Con R001 y R002, el juez y la verificación del candidato leen la
consigna de la copia. Si el corrector apunta a otra, arma candidatos que el juez no puede aprobar.

El contexto suma `quizInstructions`, la consigna de la copia, y el corrector la recibe con ese nombre. Las otras
opciones mentían:
- cambiarle el contenido a `knowledgeInstructions` dejaba un campo con nombre de tema y valor de copia;
- renombrarlo arrastraba a `CandidateAssessmentInput` y al juez de candidatos, que siguen con la del tema (fuera de
  este requirement).

La etiqueta y la consigna del tema siguen en el contexto, con la descripción al día: son la lección para el corrector y
lo que hoy reciben los criterios del candidato.

`QuizInstructionContextResolver` la toma de la copia del ejercicio, `AuditableQuiz.getInstructions()`. El adaptador
del corrector (`QuizInstructionAgentGenerator`) pasa `quizInstructions` en lugar de `knowledgeInstructions`. Son
cuerpos.

```architecture
modules:
- name: refiner-domain
  models:
  - name: QuizInstructionCorrectionContext
    type: record
    fields:
    - name: knowledgeTitle
      type: String
      _change: modify
    - name: knowledgeInstructions
      type: String
      _change: modify
    - name: quizInstructions
      type: String
      _change: add
    _change: modify
  implementations:
  - name: QuizInstructionContextResolver
    handwrittenTests:
    - name: "should resolve a correction context carrying the instructions of the quiz\
        \ copy, the ones the student sees and the judge reads, apart from the knowledge's"
      traceability:
        feature: FEAT-CALUM
        rule: F-CALUM-R003
    _change: modify
  _change: modify
- name: revision-infrastructure
  packages:
  - name: quizinstructionagent
    implementations:
    - name: QuizInstructionAgentGenerator
      handwrittenTests:
      - name: "should hand the correction agent, as the instructions it must comply\
          \ with, those of the quiz copy and never the knowledge's"
        traceability:
          feature: FEAT-CALUM
          rule: F-CALUM-R003
      _change: modify
    _change: modify
  _change: modify
```

## La definición del juez, fuera de Sentinel

Cambian tres archivos de `.sentinel/agents/quiz-instruction-validator/`:
- `agent.yaml` declara las siete entradas nuevas;
- `nodes/evaluate.md` describe la entrada como la ve el alumno y dice el tope de cada texto: motivo 400, restricción
  240, evidencia 300 y explicación 300;
- `eval/run_evals.py` arma esas entradas cuando corre un juez externo.

El criterio de juicio no cambia, y la compuerta y el emisor quedan igual. La versión es la huella de esos archivos, del
modelo y del proveedor, así que cambia.

Del corrector (`.sentinel/agents/quiz-instruction-corrector/`) cambian dos archivos, por R003:
- `agent.yaml` declara `quizInstructions` en lugar de `knowledgeInstructions`;
- `nodes/generate.md` dice «Consigna que debe cumplir: {quizInstructions}».

La lección sigue siendo `{knowledgeTitle}`.

## Lo que hay que tocar a mano después del generate

- `Main` le pasa el renderizador que ya arma a las dos fábricas.
- Las construcciones de prueba de `DefaultQuizInstructionAnalyzerFactory` suman un argumento, y las de
  `QuizInstructionCorrectionContext` también, la consigna de la copia al final.
- Los cuerpos de las dos fábricas, del constructor del sujeto, de los dos lugares de `revise-instructions`, del
  resolvedor del contexto y del adaptador del corrector.
