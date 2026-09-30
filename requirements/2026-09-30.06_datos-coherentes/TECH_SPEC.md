---
patch: FEAT-DATOS
requirement: 2026-09-30.06_datos-coherentes
generated: 2026-09-30T18:35:00Z
---

# Tech Spec: Datos coherentes (FEAT-DATOS), paquete A de la ronda 1

Diseñado contra el modelo que queda después de 0.2 (FEAT-OPMUL) y 0.3 (FEAT-HALL); `sentinel-baseline.yaml` es ese
modelo, armado en memoria. De dónde sale lo que el parche usa y no declara:

- **De 0.3**: `AnalyzerProvider`, `ContentAnalyzer.findingsAt`, `FindingDraft`, `FindingSeverity`,
  `FindingResolution`, `AnalysisCost`, `AnalyzerRuleCard` y la ficha.
- **De 0.2**: `AuditableQuiz.multipleChoice`: la oración de una opción múltiple es el enunciado con la correcta
  (F-OPMUL-R004).
- **Ya existe**: `AuditableQuiz.sentences` (la oración guardada del árbol) y `AuditableKnowledge.sentenceMode`.
- **Del parche de FEAT-RESPC**: `CorrectorMatcher`.
- **Del integrador de la ronda 1**: `QuizStudentViewRenderer` —que arma la oración con la primera respuesta reusando
  `QuizSentenceConverter`, sin otra forma de derivarla— y `AuditableQuiz.answerAudioKey`, la clave de audio que hoy no
  llega al informe.

## Leer las otras copias del ejercicio por dos puertos de sólo lectura
Las copias que content-audit no tiene en el árbol son dos, y José decidió leer las dos: el texto con que se grabó cada
audio, en un registro nuevo que vive en `db/` y actualiza cada regrabación, y la oración guardada de la copia de
producción que ya deja el importador en `db/quiz-templates.json`. Van por dos puertos porque cambian por motivos
distintos: el registro lo escribe la regrabación (paquete E le agregará la escritura), y la copia de producción la
reemplaza cada importación. El curso se sigue leyendo sólo por `CourseRepository`: de la copia de producción se lee el
`_id` y `form.sentences`, nada más.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: AudioRecordingRegistry
        _change: add
        stereotype: port
        exposes:
          - signature: "recordingOf(String audioKey): Optional<AudioRecording>"
          - signature: "version(): String"
      - name: ProductionSentenceSource
        _change: add
        stereotype: port
        exposes:
          - signature: "savedSentences(String quizId): Optional<List<String>>"
    packages:
      - name: datacoherence
        _change: add
        visibility: public
        models:
          - name: AudioRecording
            _change: add
            type: record
            fields:
              - { name: audioKey, type: String }
              - { name: recordedText, type: String }
              - { name: source, type: String }
  - name: audit-infrastructure
    _change: modify
    packages:
      - name: exercisecopies
        _change: add
        visibility: internal
        implementations:
          - name: FileSystemAudioRecordingRegistry
            _change: add
            visibility: public
            implements: [AudioRecordingRegistry]
            requiresInject:
              - { name: registryPath, type: Path }
          - name: FileSystemProductionSentenceSource
            _change: add
            visibility: public
            implements: [ProductionSentenceSource]
            requiresInject:
              - { name: exportPath, type: Path }
```

## Armar el registro de grabaciones una vez, con las dos fuentes
El registro (`db/grabaciones.json`: por clave, el texto, de dónde salió y la fecha) se arma una sola vez con un script
en `scripts/`, fuera de Sentinel como `import_backup.py`: el manifiesto de la regrabación del 24/8 (2.958 claves) y,
para el resto, el `aiSentence` o `sentence` del generador de marzo de 2025. El manifiesto vive en un worktree que se
puede borrar, así que armarlo es lo primero que hace el developer. `version()` viaja en el diagnóstico para saber
contra qué registro se comparó.

## Comparar las copias con el texto que el ejercicio dice hoy
El analizador compara cada copia con lo que el ejercicio dice hoy: la oración armada con la primera respuesta para las
dos oraciones guardadas (R001), y el texto hablado para el audio (R003), que arma `SpokenTextBuilder` con la regla del
24/8 (`tts_text` y `spoken_from_choice_parts` del script de regrabación), la misma con la que se grabó. Todo se compara
con `CorrectorMatcher`, así que espacios, signos y contraída o larga no cuentan, y en reescritura vale la respuesta
sola o el ejercicio entero, como decidió José. Las claves compartidas o sin texto conocido (R004) se ven recién con el
curso entero: se juntan en `onQuiz` y se marcan en `onCourseComplete`, que el motor llama antes de agregar.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: datacoherence
        _change: add
        visibility: public
        interfaces:
          - name: SpokenTextBuilder
            _change: add
            visibility: internal
            exposes:
              - signature: "spokenTexts(AuditableQuiz quiz,SentenceMode mode): List<String>"
        implementations:
          - name: DataCoherenceAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: recordings, type: AudioRecordingRegistry }
              - { name: productionSentences, type: ProductionSentenceSource }
              - { name: spokenTextBuilder, type: SpokenTextBuilder }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
          - name: DefaultSpokenTextBuilder
            _change: add
            implements: [SpokenTextBuilder]
```

## Nombrar qué copia está mal, para que el arreglo sepa qué tocar
Cada falta nombra la copia (árbol, producción, respuesta o audio): es la marca de la identidad del hallazgo y lo que
el paquete E necesita para elegir el arreglo —rearmar la oración, cambiar «/» por «|», regrabar o llevarlo a la mesa—
sin volver a leer el ejercicio.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: datacoherence
        _change: add
        visibility: public
        models:
          - name: DataCoherenceDiagnosis
            _change: add
            type: record
            fields:
              - { name: issues, type: "List<CopyMismatch>" }
              - { name: registryVersion, type: String }
              - { name: score, type: double }
          - name: CopyMismatch
            _change: add
            type: record
            fields:
              - { name: rule, type: String }
              - { name: kind, type: CopyMismatchKind }
              - { name: copy, type: ExerciseCopy }
              - { name: expected, type: String }
              - { name: found, type: String }
          - name: CopyMismatchKind
            _change: add
            type: enum
            fields:
              - { name: SAVED_SENTENCE_DIFFERS }
              - { name: SLASH_IN_ANSWER }
              - { name: SLASH_IN_AUDIO }
              - { name: AUDIO_DIFFERS }
              - { name: NO_AUDIO_KEY }
              - { name: SHARED_AUDIO_KEY }
              - { name: UNKNOWN_RECORDING }
          - name: ExerciseCopy
            _change: add
            type: enum
            fields:
              - { name: TREE_SENTENCE }
              - { name: PRODUCTION_SENTENCE }
              - { name: ACCEPTED_ANSWER }
              - { name: AUDIO }
```

## Registrarse en el catálogo con su ficha
Ficha: pregunta «¿Todas las copias del ejercicio dicen lo mismo?», lee las partes, las dos oraciones guardadas y el
registro de grabaciones, reglas R001 a R004 instantáneas, familia errores, evalúa QUIZ, resoluciones regla y mesa,
costo instantáneo. Sin `planBinding` ni `config`: regrabar y rearmar son del paquete E.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: datacoherence
        _change: add
        visibility: public
        implementations:
          - name: DataCoherenceAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: recordings, type: AudioRecordingRegistry }
              - { name: productionSentences, type: ProductionSentenceSource }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
features:
  - id: FEAT-DATOS
    code: F-DATOS
```

## Cómo se mide
Sobre una copia de `db/` en el scratchpad (el árbol, `quiz-templates.json` y `grabaciones.json`), `analyze
--analyzers data-coherence`; del resumen del informe sale `marcas.json` con `analizador: datos` por cada ejercicio con
hallazgo, y `medir.py marcas.json --parte escribir --categoria datos`. `escribir/` trae 39 casos cortados por tema (34
audios que leen la barra y 5 oraciones guardadas con barra): leyendo también la copia de producción, el prototipo del
analista encuentra los 39 y marca 9 más, todos reales. Metas: 9 de cada 10 en `examinar/`, que corre José, y 8 de cada
10 marcas nuevas reales.

## Lo que no se mueve
data-coherence es de la familia de errores: el 73,9 % y los niveles (96,5 · 94,2 · 79,5 · 63,5) quedan iguales. Sólo
lee: ni el registro ni la copia de producción se escriben al analizar (F-HALL-R015).
