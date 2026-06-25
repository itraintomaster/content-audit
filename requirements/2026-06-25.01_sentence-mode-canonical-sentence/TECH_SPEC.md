---
patch: FEAT-SMODE
requirement: 2026-06-25.01_sentence-mode-canonical-sentence
generated: 2026-06-25T11:20:00Z
---

# Tech Spec: Modo de frase del knowledge y frase canonica puntuable

## Declarar el enum SentenceMode en course-domain
El modo de frase es el concepto nuevo que desambigua que oracion se puntua. Lo modelamos como enum del dominio del curso —no como String— para que el tipo viaje con su semantica por todos los carriers y para que el converter pueda hacer un switch exhaustivo sobre el. Vive en course-domain porque es una propiedad estructural del contenido, el modulo de dominio mas fundamental (sin dependencias), del que cuelgan todos los demas.

```architecture
modules:
  - name: course-domain
    _change: modify
    models:
      - name: SentenceMode
        _change: add
        type: enum
        fields:
          - { name: REWRITE }
          - { name: FILL }
```

## Agregar sentenceMode a KnowledgeEntity
El modo es una propiedad declarada del knowledge, no inferida en runtime ni por quiz (F-SMODE-R001, R002): un knowledge agrupa quizzes que comparten la misma mecanica pedagogica. El campo es nullable como red de seguridad para knowledges no-oracion (ya excluidos por F-SLEN-R001) y para cursos previos a la siembra; en datos reales todos los ~608 knowledges basados en sentencias se siembran (~211 REWRITE / ~397 FILL), por lo que F-SMODE-R001 se cumple sobre datos productivos. El valor viaja en `_knowledge.json` y lo carga/persiste FileSystemCourseRepository.

```architecture
modules:
  - name: course-domain
    _change: modify
    models:
      - name: KnowledgeEntity
        _change: modify
        fields:
          - { name: sentenceMode, type: SentenceMode, _change: add }
```

## Sobrecargar QuizSentenceConverter con derivacion consciente del modo
La derivacion historica `toPlainSentences(form)` concatena todas las partes (mode-blind) y es el origen del bug. En vez de un puerto nuevo, sobrecargamos el converter que ya es dueño de "form a plain sentence": agregamos `toPlainSentences(form, mode)`. En FILL reproduce el comportamiento actual (oracion completa); en REWRITE deriva solo el/los segmento(s) con CLOZE, excluyendo el TEXT-andamio fuente (F-SMODE-R003, R004). El metodo mode-blind preexistente queda intacto, preservando todos los consumidores actuales, y el motor sigue encapsulado en el paquete interno quizsentenceengine. Cuando `mode` es null el converter aplica fallback a FILL, manteniendo el comportamiento historico para datos no sembrados.

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
              - signature: "toPlainSentences(FormEntity form, SentenceMode mode): List<String>"
                _change: add
```

## Propagar el modo a AuditableKnowledge
El analisis y la revision no leen entidades crudas del curso sino su proyeccion auditable. Para que el modo llegue a esos consumidores sin reabrir el grafo del curso, CourseToAuditableMapper copia `sentenceMode` desde KnowledgeEntity al construir AuditableKnowledge, en el mismo paso en que ya copia title e instructions. Asi el modo queda disponible en el arbol de auditoria que el resolver recorre.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditableKnowledge
        _change: modify
        fields:
          - { name: sentenceMode, type: SentenceMode, _change: add }
```

## Transportar el modo hasta el deriver via LemmaAbsenceCorrectionContext
El deriver que produce la frase inflada vive en revision-domain, pero el modo nace en el knowledge. El portador natural es LemmaAbsenceCorrectionContext: LemmaAbsenceContextResolver ya navega al knowledge ancestro para estampar knowledgeTitle/knowledgeInstructions, y ahi mismo lee AuditableKnowledge.sentenceMode. Esto obliga a una arista nueva refiner-domain a course-domain para resolver el tipo SentenceMode; la aceptamos porque preservar el tipo del dominio en el carrier (en vez de degradarlo a String) es la opcion correcta, no hay ciclo, y revision-domain ya establece el precedente de un domain module que depende directamente de course-domain.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    dependsOn:
      - course-domain
    models:
      - name: LemmaAbsenceCorrectionContext
        _change: modify
        fields:
          - { name: sentenceMode, type: SentenceMode, _change: add }
```

## Cambiar la firma de LemmaAbsenceProposalDeriver.derive para recibir el modo
El deriver construye la frase canonica del quiz revisado y hoy lo hace mode-blind, re-incluyendo la oracion fuente en REWRITE (la degradacion espuria del score que F-SMODE-R007 cierra). El modo es un parametro de la operacion de derivacion, no un dato del candidato (que produce la estrategia) ni del snapshot (un nodo del arbol del curso), asi que va en la firma: `derive(before, candidate, mode)`. DispatchingReviser ya tiene el LemmaAbsenceCorrectionContext a mano cuando invoca al deriver y le pasa `lemmaContext.getSentenceMode()`. La interfaz sigue sealed: el cambio retira la firma de dos argumentos y agrega la de tres, declarando `sealed` y `exposes` en el mismo bloque para que ningun merge de propuestas pierda el cambio de firma.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: LemmaAbsenceProposalDeriver
        _change: modify
        sealed: true
        exposes:
          - signature: "derive(CourseElementSnapshot before, LemmaAbsenceQuizCandidate candidate): CourseElementSnapshot"
            _change: delete
          - signature: "derive(CourseElementSnapshot before, LemmaAbsenceQuizCandidate candidate, SentenceMode mode): CourseElementSnapshot"
            _change: add
```
