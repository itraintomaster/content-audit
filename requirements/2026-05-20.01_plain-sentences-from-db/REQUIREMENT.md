---
feature:
  id: FEAT-DBSENT
  code: F-DBSENT
  name: Plain sentences pre-computadas desde la fuente de datos
  priority: critical
---

# Plain sentences pre-computadas desde la fuente de datos

## TL;DR

**Que**: El campo `sentences: List<String>` de `QuizTemplateEntity.form` ahora se carga **directamente desde la fuente de datos** del curso (JSON de quiz templates) en lugar de derivarse en runtime a partir de la estructura `sentenceParts`. El `CourseToAuditableMapper` consume esa lista pre-computada y la propaga a `AuditableQuiz.sentences` sin reinvocar `QuizSentenceConverter.toPlainSentences`. El converter sigue existiendo y sus contratos no cambian; deja de ser consumidor productivo dentro del flujo del audit.

**Por que**: La derivacion runtime via concatenacion `TEXT + variante canonica de CLOZE` modela correctamente el patron "cloze inline" pero falla para el patron "transformacion", donde el TEXT es scaffolding pedagogico (ej. `"The dogs / bark / loudly."`) y el CLOZE contiene la oracion completa resuelta. En esos casos el output combinaba scaffolding + resolucion (ej. `"The dogs / bark / loudly. The dogs are barking loudly."`), inflando el conteo de tokens y degradando los scores de `SentenceLengthAnalyzer` para niveles bajos. El issue afecta aproximadamente 3.976 de 11.487 quizzes (~35%). La discriminacion entre patron A (cloze inline) y patron B (transformacion) se resolvio out-of-band con una heuristica sobre los datos persistidos (B si existe exactamente un CLOZE cuya variante canonica empieza con mayuscula, termina en puntuacion final y es multi-palabra), y los datos ya estan pre-computados en la fuente. Mover la fuente de verdad de las plain sentences a la DB elimina el codigo de derivacion del path del audit y se apoya en un campo que ya esta resuelto correctamente upstream.

## Reglas de Negocio

<a id="F-DBSENT-R001"></a>
### Rule[F-DBSENT-R001] - QuizTemplateEntity persiste la lista de plain sentences
**Severity**: critical

> `QuizTemplateEntity` lleva un campo `sentences: List<String>` que refleja literalmente la lista de oraciones planas asociadas al template tal como las publica la fuente de datos. El repositorio que carga cursos puebla el campo a partir del JSON de origen; no lo deriva.

<a id="F-DBSENT-R002"></a>
### Rule[F-DBSENT-R002] - El audit consume las plain sentences pre-computadas
**Severity**: critical

> Al mapear un curso a su representacion auditable, el sistema toma `QuizTemplateEntity.sentences` y lo propaga sin modificacion a `AuditableQuiz.sentences`. No invoca `QuizSentenceConverter.toPlainSentences` para los quizzes del audit, ni rederiva la lista desde `sentenceParts`.

<a id="F-DBSENT-R003"></a>
### Rule[F-DBSENT-R003] - El converter sigue siendo contrato disponible pero no consumido por el audit
**Severity**: medium

> `QuizSentenceConverter.toPlainSentences` permanece en el contrato del converter como fallback util para consumidores que necesiten derivacion en runtime (ej. un cargador alternativo sin campo persistido, o herramientas de migracion). Los tests existentes de FEAT-QSENT que ejercen sus invariantes siguen activos.

<a id="F-DBSENT-R004"></a>
### Rule[F-DBSENT-R004] - Persistir un curso preserva las plain sentences de cada quiz (round-trip idempotente)
**Severity**: critical

> Cuando el sistema persiste un curso de vuelta a la fuente de datos, escribe la lista de plain sentences de cada quiz de forma **simetrica** a como la lee ([F-DBSENT-R001](#F-DBSENT-R001)): un ciclo cargar -> guardar -> cargar deja la lista de plain sentences de cada quiz **identica** a la original. No se pierden, no se rederivan y no se reordenan.

<details><summary>Detalle</summary>

R001 describe el lado de **lectura** (la fuente de datos puebla las plain sentences al cargar el curso). Esta regla es su **espejo en escritura**: garantiza que el camino de persistencia conserve exactamente la misma informacion.

Sin esta garantia, al persistir un curso la lista de plain sentences de cada quiz se omite y se pierde. Como el flujo de aplicacion de revisiones reescribe el **curso completo** (no es una escritura quirurgica sobre el quiz revisado), un solo guardado borraria las plain sentences de **todos** los quizzes del curso, dejando el scoring de longitud de oracion en cero para el curso entero. Es perdida de datos masiva.

**Criterio de aceptacion**: dado un curso cuyos quizzes ya tienen plain sentences pre-computadas, tras un ciclo guardar + recargar, cada quiz conserva su lista de plain sentences identica a la que tenia antes del ciclo (misma cantidad, mismos textos, mismo orden).

**Simetria con R001**: la lista que se escribe es la misma que R001 leyo; no se invoca derivacion alguna ni se transforma el contenido. Lo escrito y lo releido coinciden elemento a elemento.

</details>

## Notas

- **Gap requirement vs arquitectura**: el discriminador pattern A / pattern B no se modela en codigo Java; vive como heuristica aplicada offline al pre-computar los datos. Si en el futuro hay quizzes sin el campo persistido (DB legacy o nueva fuente), habra que decidir si reintroducir el discriminador en runtime o exigir el campo como invariante.
- **Reescritura de curso completo en aplicacion de revisiones**: el flujo que aplica/aprueba una revision reescribe el **curso entero** de vuelta a la fuente de datos, no solo el quiz revisado. Por eso el round-trip de [F-DBSENT-R004](#F-DBSENT-R004) importa: cualquier omision en la escritura de plain sentences se propaga a todos los quizzes del curso, no a uno.
- **Fuera de alcance**: invariante "todo QuizTemplateEntity cargado tiene sentences no-null"; estrategia de fallback ante ausencia del campo en la DB; cambios al contrato de `QuizSentenceConverter`.
