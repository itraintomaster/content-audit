---
patch: FEAT-DUPEX
requirement: 2026-09-30.07_duplicados-exactos
generated: 2026-09-30T19:05:00Z
---

# Tech Spec: Duplicados exactos (FEAT-DUPEX), paquete A de la ronda 1

Diseñado contra el modelo que queda después de 0.2 (FEAT-OPMUL) y 0.3 (FEAT-HALL); `sentinel-baseline.yaml` es ese
modelo, armado en memoria. De dónde sale lo que el parche usa y no declara:

- **De 0.3**: `AnalyzerProvider`, `ContentAnalyzer.findingsAt`, `FindingDraft`, `FindingEvidence` con sus
  `relatedNodes` (el ejercicio que queda), `FindingSeverity`, `FindingResolution`, `AnalysisCost` y la ficha.
- **De 0.2**: `AuditableQuiz.multipleChoice`: en opción múltiple el ejercicio terminado lleva la opción correcta.
- **Ya existe**: `AuditableKnowledge.quizzes`, en el orden del curso.
- **Del parche de FEAT-RESPC**: `CorrectorMatcher`.
- **Del integrador de la ronda 1**: `QuizStudentViewRenderer`, que arma el ejercicio terminado —texto fijo sin la
  pista, con la primera respuesta o la correcta, sin recortar la oración de partida de una reescritura— y el
  ejercicio como lo ve el alumno para la evidencia.

## Buscar las copias dentro del tema, con el ejercicio terminado
La comparación es entre hermanos, así que se hace en `onKnowledge`, que el motor llama antes que a los ejercicios del
tema y que ya trae los ejercicios en el orden del curso: cada uno se compara con los anteriores con
`CorrectorMatcher.sameAnswer`, y el primero que termina igual es el que queda. Eso hace estable la marca entre
corridas (F-HALL-R014) y cuenta un error por copia, como decidió José; los iguales de temas distintos nunca se miran.
Se compara el ejercicio terminado y no la oración guardada, porque la guardada de una reescritura es sólo la
respuesta y daba 126 copias falsas. Medido con la tabla del corrector nuevo, las copias siguen siendo 23.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: exactduplicates
        _change: add
        visibility: public
        implementations:
          - name: ExactDuplicatesAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## Dejar el diagnóstico sólo en la copia
El ejercicio que queda no tiene hallazgo ni diagnóstico y conserva 1; la copia lleva 0 y un diagnóstico con el
ejercicio que queda y el terminado que comparten. La evidencia nombra al que queda como el otro nodo del hallazgo
(F-HALL-R003 inv. 3), con los dos ejercicios como los ve el alumno.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: exactduplicates
        _change: add
        visibility: public
        models:
          - name: ExactDuplicateDiagnosis
            _change: add
            type: record
            fields:
              - { name: keptQuizId, type: String }
              - { name: finishedExercise, type: String }
              - { name: score, type: double }
```

## Registrarse en el catálogo con su ficha
Ficha: pregunta «¿Dos ejercicios del mismo tema dicen lo mismo?», lee el ejercicio terminado de cada ejercicio del
tema, reglas R001 a R004 instantáneas, familia errores, evalúa QUIZ, resolución mesa, costo instantáneo, gravedad baja.
Sin `planBinding` ni `config`: reemplazar la copia es del paquete E.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: exactduplicates
        _change: add
        visibility: public
        implementations:
          - name: ExactDuplicatesAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
features:
  - id: FEAT-DUPEX
    code: F-DUPEX
```

## Cómo se mide
Sobre una copia del curso en el scratchpad, `analyze --analyzers exact-duplicates`; del resumen del informe sale
`marcas.json` con `analizador: duplicados` por cada copia, y `medir.py marcas.json --parte escribir --categoria
duplicados`, que también cuenta la marca en el gemelo. `escribir/` trae 15 casos cortados por tema: 9 exactos y 6
parecidos, que son del paquete B. El prototipo del analista encuentra los 9 exactos sin ninguna marca de más en los
194 temas de esa mitad. Metas: 10 de cada 10 de los exactos, cerca de 6 de cada 10 de la categoría entera y 9 de cada
10 marcas nuevas reales.

## Lo que no se mueve
exact-duplicates es de la familia de errores: el 73,9 % y los niveles (96,5 · 94,2 · 79,5 · 63,5) quedan iguales, y
no cambia ningún puntaje de vocabulario.

## Declarar las 14 pruebas y ubicar el journey
Las 14 son nuevas: no hay tests previos de estas reglas que portar. Con el criterio del qa-tester de la opción
múltiple, sólo se declara lo que verifica una regla, y cada nombre lleva un caso real. Las cinco reglas quedan
cubiertas (R001 6, R002 4, R003 1, R004 1, R005 2). De dónde sale cada una:

- **Los casos del REQUIREMENT**: Práctica mixta (224b y 2260), In time u on time (3d28 y 4a4c), «rice ___» (2313 y
  2314), los once «So am I.» de Presente simple: so/neither, «I don't have ___ time.» (2b7f, con «I didn't have ___
  time.» 2b6b) y «She dances ___ (good / well).» (255d en Adjetivo o adverbio; 2566 y 2eeb en ¿Good o well?).
- **Las 23 copias exactas.** La prueba toma, del curso del 29/9, los 14 temas donde están (A1 17, A2 2, B1 4; diez en
  Doble consonante y e, como «stop - stopping» 1fc7 y 1fe6) y espera un hallazgo en cada copia y ninguno en su gemelo.
  Que no haya marcas en los otros 399 temas lo dice la medición con `medir.py`, no esta prueba.
- **Cuál queda.** En Práctica mixta, 224b está en la posición 44 de 80 y 2260 en la 65: queda 224b y la copia es 2260,
  como dicen R002 y las pruebas. El ejemplo de evidencia de R004 los nombra al revés.
- **Lo que depende de otros**: el ejercicio terminado y la evidencia, el renderizador del integrador (A-01).
- **El journey** va a `audit-domain`, paquete `exactduplicates`. `generate` crearía `FDupexJ001JourneyTest` con 3
  caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: exactduplicates
        _change: add
        implementations:
          - name: ExactDuplicatesAnalyzer
            _change: add
            handwrittenTests:
              - name: "should find the 23 exact copies of the course of 29/9 inside their 14 topics, 17 in A1, 2 in A2 and 4 in B1, as «They weren't at the park.» twice in Práctica mixta"
                traceability: { feature: FEAT-DUPEX, rule: F-DUPEX-R001 }
features:
  - id: FEAT-DUPEX
    code: F-DUPEX
    journeys:
      - { id: F-DUPEX-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.exactduplicates }
```
