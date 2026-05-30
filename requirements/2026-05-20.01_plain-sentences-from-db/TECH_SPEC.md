---
patch: F-DBSENT
requirement: 2026-05-20.01_plain-sentences-from-db
generated: 2026-05-20T00:00:00Z
---

# Tech Spec: Plain sentences pre-computadas desde la fuente de datos

## Agregar `sentences` a `QuizTemplateEntity`
La derivacion runtime `TEXT (sin hints) + variante canonica de CLOZE` que hoy implementa `QuizSentenceConverter.toPlainSentences` modela correctamente el patron "cloze inline" (TEXT envoltorio + CLOZE token de respuesta) pero falla para el patron "transformacion", donde el TEXT es scaffolding pedagogico de raices y el CLOZE contiene la oracion resuelta completa: el output combina ambos y duplica el conteo de tokens, degradando los scores de `SentenceLengthAnalyzer` para A1/A2 en ~35% del corpus. La discriminacion entre patrones se resolvio out-of-band con una heuristica sobre los datos y los resultados se persistieron en la fuente. El campo `sentences` materializa esa lista resuelta en el modelo del curso para que el flujo del audit la consuma sin re-derivar, manteniendo el converter como contrato disponible para consumidores que no tengan el campo persistido (ej. una fuente alternativa o herramientas de migracion).

```architecture
modules:
  - name: course-domain
    _change: modify
    models:
      - name: QuizTemplateEntity
        _change: modify
        fields:
          - name: sentences
            type: List<String>
            _change: add
```

## Observaciones sobre el alcance del patch
El cambio arquitectonico se limita al modelo porque las dos modificaciones de comportamiento downstream son internas a implementaciones existentes y no tocan contratos. `FileSystemCourseRepository` ya inyecta lo que necesita (`CourseValidator`) y solo agrega una lectura mas sobre el JSON del form para poblar el nuevo campo; su `requiresInject` no cambia. `CourseToAuditableMapper` ya recibe `quizSentenceConverter` para `serialize` (FEAT-RCLAQS R002) y solo deja de invocar `toPlainSentences`, sustituyendo la llamada por una lectura directa de `qt.getSentences()`; tampoco cambia su `requiresInject`. La firma `toPlainSentences(FormEntity form): List<String>` se conserva intacta en `QuizSentenceConverter` por decision explicita: sigue cubierta por tests FEAT-QSENT R017-R019 y mantiene la opcion de derivar en runtime para fuentes que no carguen el campo persistido.

## Brechas conocidas (skip de @analyst)
El usuario decidio saltear la fase de analisis. Dos brechas requirement-arquitectura quedan abiertas y deberian volver a `@analyst` cuando sea oportuno: (1) no hay invariante explicito que obligue a que todo `QuizTemplateEntity` cargado tenga `sentences` no-null, lo que deja indefinido el comportamiento ante quizzes legacy sin el campo; (2) el discriminador pattern A / pattern B no se modela en codigo, vive solo como heuristica offline aplicada al pre-procesamiento de la DB. Si en el futuro aparece una fuente sin el campo precomputado, habra que decidir si reintroducir el discriminador en runtime (probable nuevo CorrectionContextResolver-equivalente en course-domain) o exigir el campo como invariante de carga del repositorio.
