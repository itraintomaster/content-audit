---
patch: FEAT-MINIT
requirement: 2026-09-30.10_miniteoria
generated: 2026-10-05T18:44:06.767Z
---

# Tech Spec: Juez de miniteoría por quiz (FEAT-MINIT)

Esta evolución corrige la unidad semántica de FEAT-MINIT: el juez se pronuncia sobre la miniteoría de cada quiz
concreto, no sobre un texto deduplicado ni sobre el tema como si tuviera una única regla. El tema sigue siendo la unidad
de consulta, presupuesto, reuso y fingerprint porque agrupar los quizzes abarata el costo; esa optimización no puede
filtrarse a la identidad del veredicto, el diagnóstico, el hallazgo o la corrección.

Cada quiz se evalúa con su propio texto de miniteoría, prompt o enunciado, pista, respuestas aceptadas u opciones y el
contexto del tema. Cuando un tema mezcla afirmación, negación y pregunta, el quiz de pregunta se juzga sólo contra la
forma interrogativa que practica. No se penaliza por omitir variantes que ese quiz no ejercita; sí se penaliza si enseña
otro subtema, si la explicación es falsa, o si no basta para resolver su caso.

Se preservan el runtime común de jueces, el libro de evaluaciones, el presupuesto por tema, el proveedor del analizador,
el lector JSON y la interfaz del constructor del sujeto. Cambia el contenido que construye el sujeto y cambia el schema
del artifact: el lote enumera cada quiz por id, aun cuando dos entradas repitan exactamente el mismo texto.

El analyst ya alineó los dos requisitos transversales con esta decisión: F-TAREA-R003 produce una tarea por quiz y
hallazgo, y F-MESA-R012 limita la reescritura al quiz objetivo. La conducta queda así consistente de punta a punta:
ningún candidato se propaga por igualdad de texto.

## Identificar cada evaluación por quiz

El carrier deja de usar el ordinal de una miniteoría deduplicada y la lista de ejercicios a los que supuestamente aplica.
Esos dos campos codificaban la propagación que ahora se prohíbe. El nuevo quizId es la clave de correlación dentro del
payload del tema y hace posible que dos textos iguales tengan resultados opuestos.

El identificador debe ser el id persistente del QuizTemplateEntity que luego llega a AuditableQuiz.id. No se usa la
posición del quiz en el tema como identidad: el orden sólo sirve para una representación determinista del lote, mientras
que el id permite distribuir sin ambigüedad el resultado y comparar el examen contra el gold por caso.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: modify
        models:
          - name: MiniTheoryAssessment
            _change: modify
            fields:
              - name: miniTheory
                _change: delete
              - name: exercises
                _change: delete
              - name: quizId
                type: String
                description: Identidad del quiz al que pertenece este veredicto dentro del lote del tema; nunca identifica un texto compartido.
                _change: add
```

## Decidir verdad, suficiencia y especificidad dentro del caso

holds sólo puede ser true cuando la tarjeta satisface tres condiciones para ese quiz: es verdadera, alcanza para
resolver la forma o función que se pide y no desplaza la explicación hacia otro subtema. La evaluación no exige que una
tarjeta local sea una gramática exhaustiva del inglés ni que cubra las demás variantes presentes en el mismo tema.

problem, severity, counterexample y explanation quedan igualmente acotados al quiz. El contraejemplo debe demostrar el
defecto en el caso practicado, no introducir una excepción externa que pertenece a otra forma verbal o a otro uso. Esta
restricción evita volver a reprobar reglas locales correctas por leerlas como afirmaciones universales.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: modify
        models:
          - name: MiniTheoryAssessment
            _change: modify
            fields:
              - name: holds
                type: boolean
                description: True sólo cuando la miniteoría de este quiz es verdadera, suficiente y específica para su caso.
                _change: modify
              - name: problem
                type: MiniTheoryProblem
                description: Null si holds; de lo contrario clasifica exclusivamente el problema de este quiz.
                _change: modify
              - name: severity
                type: FindingSeverity
                description: "Severidad por quiz: HIGH falsa o insuficiente, MEDIUM exagerada, LOW contenido verdadero de otro subtema."
                _change: modify
              - name: counterexample
                type: String
                description: Evidencia concreta del problema de esta miniteoría aplicada a este quiz.
                _change: modify
              - name: explanation
                type: String
                description: "Explicación ceñida a la forma, función y palabras que practica este quiz."
                _change: modify
```

## Emitir una entrada independiente por cada quiz del lote

MiniTheoryVerdict conserva una colección porque una consulta sigue reuniendo el tema completo. La colección deja de
significar “una entrada por texto distinto” y pasa a significar “una entrada por quiz incluido”. El reader debe rechazar
como inutilizable un payload que omita un quiz solicitado, repita su quizId o agregue un id que no estaba en el sujeto.

El rationale puede resumir el criterio general del lote, pero nunca sustituye los assessments individuales. La
distribución, la cobertura y el examen leen la colección por quizId; no infieren pertenencia comparando miniTheory,
explicaciones ni posiciones.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: modify
        models:
          - name: MiniTheoryVerdict
            _change: modify
            fields:
              - name: assessments
                type: "List<MiniTheoryAssessment>"
                description: Un veredicto independiente por cada quiz incluido en el lote del tema, aunque dos quizzes compartan el mismo texto de miniteoría.
                _change: modify
```

## Clasificar el defecto con la gravedad del quiz

La taxonomía anterior mezclaba problemas globales de una miniteoría con excepciones o ejercicios “involucrados”. La
nueva taxonomía representa la pregunta real. FALSE_RULE cubre una afirmación falsa dentro del caso; INSUFFICIENT_FOR_QUIZ
cubre una explicación que no permite resolverlo. Ambos son HIGH porque el alumno recibe una regla falsa o inútil para
la actividad que tiene delante.

OVERSTATED_FOR_QUIZ es MEDIUM sólo cuando la exageración ocurre dentro del mismo caso practicado. OTHER_SUBTOPIC_CONTENT
es LOW cuando agrega información verdadera pero ajena, por ejemplo una tarjeta interrogativa que además enseña una
regla afirmativa que ese quiz no usa. Una omisión de variantes no practicadas no produce ningún problema.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: modify
        models:
          - name: MiniTheoryProblem
            _change: modify
            fields:
              - name: FALSE_RULE
                description: "La miniteoría afirma algo falso para la forma, función o palabras de este quiz. HIGH."
                _change: modify
              - name: OVERSTATED
                _change: delete
              - name: CONTRADICTED_BY_EXERCISE
                _change: delete
              - name: NOT_COVERING_EXERCISE
                _change: delete
              - name: INSUFFICIENT_FOR_QUIZ
                description: La miniteoría no alcanza para resolver el caso concreto de este quiz. HIGH.
                _change: add
              - name: OVERSTATED_FOR_QUIZ
                description: La miniteoría exagera dentro del caso concreto de este quiz. MEDIUM.
                _change: add
              - name: OTHER_SUBTOPIC_CONTENT
                description: La miniteoría agrega contenido verdadero de otro subtema que este quiz no practica. LOW.
                _change: add
```

## Mantener el subject y la huella por tema

MiniTheorySubjectBuilder conserva build(AuditNode knowledgeNode) y EvaluationSession conserva una resolución por sujeto.
El mapa de contenido debe seguir incluyendo cefrLevel, topic e instructions del tema, pero su sección de quizzes ya no
deduplica miniteorías. Para cada quiz incluye quizId, miniTheory, prompt o enunciado, pista, respuestas aceptadas u
opciones —con la correcta marcada— y el contexto necesario para interpretar la actividad.

La serialización debe ser estable y listar cada quiz una vez, en orden del tema. Como el fingerprint se calcula sobre
todo el mapa que recibe el evaluador, cambiar cualquiera de esos campos invalida el resultado del tema completo. Se
preserva así F-MINIT-R001 y F-MINIT-R005: una consulta por tema, reuso por la huella completa y presupuesto contado por
tema, con cobertura y verdicts observables por cada quiz.

No se añade otro port ni un segundo evaluator. Separar el batching del resultado dentro del mismo contrato es suficiente
y evita introducir una capa de coordinación paralela sólo para esta familia de juez.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: modify
        models:
          - name: MiniTheoryAssessment
            _change: modify
            fields:
              - name: quizId
                type: String
                description: Identidad del quiz al que pertenece este veredicto dentro del lote del tema; nunca identifica un texto compartido.
                _change: add
          - name: MiniTheoryVerdict
            _change: modify
            fields:
              - name: assessments
                type: "List<MiniTheoryAssessment>"
                description: Un veredicto independiente por cada quiz incluido en el lote del tema, aunque dos quizzes compartan el mismo texto de miniteoría.
                _change: modify
```

## Eliminar el alcance por texto o por tema

MiniTheoryErrorScope deja de existir y MiniTheoryConfig deja de exponer getErrorScope(). No se lo reemplaza por un valor
constante SHOWN_ONLY porque incluso “todos los ejercicios que muestran el texto” sigue siendo demasiado amplio: el
único alcance permitido es el quiz cuyo assessment se está distribuyendo.

DefaultMiniTheoryConfig conserva únicamente la política de puntajes por severidad. DefaultMiniTheoryDistributor ya no
consulta una preferencia de alcance; usa quizId para localizar un único nodo y produce cero o un hallazgo en ese quiz.
Esta eliminación convierte la no propagación en una propiedad del contrato, no en una opción de despliegue que podría
volver a activarse por configuración.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: MiniTheoryConfig
        _change: modify
        exposes:
          - signature: "getErrorScope(): MiniTheoryErrorScope"
            _change: delete
    packages:
      - name: minitheory
        _change: modify
        models:
          - name: MiniTheoryErrorScope
            _change: delete
```

## Guardar un diagnóstico singular en el nodo del quiz

MiniTheoryDiagnosis deja de representar el veredicto agregado del tema. Se guarda en el QuizDiagnoses del quiz y lleva
quizId, el texto exacto que ese quiz muestra y un único assessment. score y reusedFromPreviousRun se conservan: el score
sale de la severidad de ese assessment y reused indica que se reusó la resolución del tema, sin sugerir que el
assessment se compartió con otro quiz.

Se elimina knowledgeId porque el ancestro del nodo ya aporta el tema y ese campo reforzaba la unidad equivocada. También
se elimina la lista assessments: una lista dentro del diagnóstico de un quiz permitía filtrar tarde y conservar
reproches pertenecientes a otros ejercicios. La nueva forma hace imposible representar esa mezcla.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: modify
        models:
          - name: MiniTheoryDiagnosis
            _change: modify
            fields:
              - name: knowledgeId
                _change: delete
              - name: assessments
                _change: delete
              - name: quizId
                type: String
                description: Quiz concreto juzgado; el lote del tema no cambia esta unidad de diagnóstico.
                _change: add
              - name: assessment
                type: MiniTheoryAssessment
                description: Veredicto singular de este quiz.
                _change: add
```

## Preservar el texto como evidencia, no como clave de propagación

El texto exacto permanece en MiniTheoryDiagnosis porque la interfaz necesita mostrar qué leyó el alumno y qué se le
reprocha. Su función es evidencia. La identidad del hallazgo sigue usando el nodo del quiz y una marca derivada de la
miniteoría de ese mismo quiz, por lo que dos quizzes con texto idéntico conservan identidades distintas gracias al nodeId
y no se agrupan para corregir.

MiniTheoryQuizOutcome explicita que sus FindingDraft pertenecen sólo a quizId. El distribuidor no busca otros quizzes por
igualdad de texto. MiniTheoryCorrection debe devolver groupsByTopic=false y editScope debe seguir limitado a MINI_THEORY;
el refiner abrirá una tarea en el quiz y la mesa aplicará el candidato únicamente a ese QuizTemplateEntity.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: modify
        models:
          - name: MiniTheoryDiagnosis
            _change: modify
            fields:
              - name: miniTheory
                type: String
                description: Texto de la miniteoría de este quiz, tal cual; textos iguales en otros quizzes no comparten diagnóstico.
                _change: modify
      - name: minitheoryengine
        _change: modify
        models:
          - name: MiniTheoryQuizOutcome
            _change: modify
            fields:
              - name: findings
                type: "List<FindingDraft>"
                description: Hallazgos exclusivos de quizId; su marca incorpora la miniteoría de ese quiz y nunca habilita propagación por texto compartido.
                _change: modify
```

## Mantener el examen por quiz y exigir gold por subcaso

JudgeExamRequest.caseIds y JudgeExamVerdict.caseId ya expresan la unidad correcta y no necesitan nueva superficie. En una
corrida EXAM, incluir un quiz hace que se consulte el lote de su tema, pero el exporter entrega sólo el verdict del
caseId solicitado. El examen se considera completo únicamente cuando todos los quizzes del archivo tienen resultado.

La medición externa debe tener gold por quiz y subcaso. Un esperado agregado por tema no puede distinguir una
interrogativa correcta de una afirmativa incorrecta ni validar que dos quizzes con el mismo texto reciban decisiones
independientes. Cambiar el schema del agente cambia la versión efectiva del juez; el resultado anterior no habilita la
versión nueva.

La cobertura mantiene temas como unidad de consultas nuevas y quizzes como unidad de resultados. Un lote puede quedar
reusado o evaluado una vez mientras sus N assessments se exportan como N casos. No se duplica costo y tampoco se ocultan
omisiones por quiz.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: modify
        models:
          - name: MiniTheoryVerdict
            _change: modify
            fields:
              - name: assessments
                type: "List<MiniTheoryAssessment>"
                description: Un veredicto independiente por cada quiz incluido en el lote del tema, aunque dos quizzes compartan el mismo texto de miniteoría.
                _change: modify
          - name: MiniTheoryDiagnosis
            _change: modify
            fields:
              - name: quizId
                type: String
                description: Quiz concreto juzgado; el lote del tema no cambia esta unidad de diagnóstico.
                _change: add
```

## Secuencia de integración

El analyst ya actualizó F-TAREA-R003 para que una miniteoría problemática produzca una tarea por quiz y hallazgo, sin
reachedQuizIds derivados de igualdad de texto, y F-MESA-R012 para que el candidato reescriba sólo
QuizTemplateEntity.miniTheory del quiz objetivo. Las tres features expresan ahora la misma unidad de corrección.

Con esa contradicción resuelta, QA debe rediseñar la cobertura de F-MINIT y de las dos reglas transversales. Las
pruebas anteriores que esperaban SHOWN_ONLY, WHOLE_TOPIC, una tarea por texto o una corrección propagada dejan de
representar el negocio. La cobertura nueva debe demostrar independencia entre quizzes con texto igual, especificidad por
afirmación/negación/pregunta, reuso del lote por tema, diagnóstico y hallazgo por quiz, y examen con gold por subcaso.

Sólo después se implementan juntos: schema del agente, builder, reader, distributor, diagnóstico, configuración,
corrección y exportación del examen. La aplicación del patch y cualquier generación siguen reservadas a la aceptación
explícita del usuario.
