---
patch: FEAT-NIVFR
requirement: 2026-09-30.13_nivel-con-frases
generated: 2026-10-01T04:00:00Z
---

# Tech Spec: la teoría del tema en lo que el tema enseña (FEAT-NIVFR, parche del 1/10, sin aplicar)

El diseño de `off-level-words` del 30/9 (el paquete `offlevelwords`: el analizador, su proveedor, su diagnóstico, la
regla del tema que comparte con el semáforo y sus 37 pruebas) ya está aplicado: su parche quedó archivado en
`.applied-patches/2026-09-30T22-34-40-330Z.yaml` y su TECH_SPEC en la historia de git (commit `0998557c`). Este parche
suma lo único que la segunda vuelta de precisión no pudo hacer en el cuerpo de las implementaciones: leer la teoría del
tema. Un verificador encontró 5 errores reales en 25 marcas; las lecturas regulares, la miniteoría y el préstamo
adaptado del 1/10 dejaron 95 ejercicios y, en una muestra nueva de 30, 24 reales. Las 3 falsas de esa muestra las
enseña la teoría del tema: «tweezers» (la de «Siempre plurales» la lista entre los más frecuentes), «concur» («concur
with: coincidir con») y «acquainted» («familiar with / acquainted with: familiarizado con»). Con un prototipo sobre el
curso del 29/9, leer la teoría saca esas 3 marcas y ninguna del oro de `escribir/` (la teoría de «Sustantivos con
preposición 2» no nombra warranty).

## Llevar el theoryId del ejercicio al modelo auditable
La teoría vive fuera del curso, en `ittm/content/theory/<id>.json`, y el ejercicio la nombra con su `theoryId`
(`QuizTemplateEntity.theoryId`), que el modelo auditable no traía: ninguna pieza de audit-domain podía saber qué teoría
acompaña a un tema. El campo va en el ejercicio y no en el knowledge porque así está en los datos: 6 de 413 knowledges
tienen ejercicios con dos teorías, y la regla del tema junta las de todos sus ejercicios. `CourseToAuditableMapper` lo
copia tal cual, como la miniteoría. Costo conocido: el constructor de `AuditableQuiz` suma un argumento y unas 314
llamadas de pruebas en 57 archivos lo construyen; hay que adaptarlas al generar, como con `miniTheory` y
`answerAudioKey` en la ronda 1.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditableQuiz
        _change: modify
        fields:
          - { name: theoryId, type: String, _change: add }
```

## Leer la teoría por un puerto, con un adaptador de archivos
audit-domain no lee archivos: un puerto da el texto de una teoría por su id y un adaptador de vocabulary-infrastructure,
donde ya viven el EVP y la tabla de formas irregulares, lo saca de los JSON de `ittm/content/theory` (todo el texto de
sus bloques, títulos y celdas). La raíz de composición le pasa la carpeta como le pasa a spaCy la lista COCA
(`../ittm/...`, al lado del proyecto); si una teoría falta, el puerto devuelve vacío y la regla sigue sin ella. Devuelve
texto y no palabras porque qué cuenta como enseñado lo decide el dominio.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: TopicTheorySource
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "theoryText(String theoryId): Optional<String>"
  - name: vocabulary-infrastructure
    _change: modify
    packages:
      - name: theory
        _change: add
        visibility: internal
        implementations:
          - name: FileSystemTopicTheorySource
            _change: add
            visibility: public
            implements: [TopicTheorySource]
            requiresInject:
              - { name: theoryFolder, type: Path }
```

## Sumar las palabras de la teoría a lo que el tema enseña
La regla del tema (F-NIVFR-R003) es una sola para el analizador y el semáforo (F-SEMAF-R004), así que la teoría entra
por `DefaultTopicTeachingResolver`, que la recibe por inyección, y queda en `TaughtWords` junto al título y las
respuestas: el analizador no la cuenta como error y el semáforo no la pinta de rojo. Son palabras sueltas en minúscula,
como las del título. La miniteoría, el sufijo del tema y el metalenguaje del 1/10 siguen en el analizador hasta que
también tengan su campo en `TaughtWords`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: offlevelwords
        _change: modify
        models:
          - name: TaughtWords
            _change: modify
            fields:
              - { name: theoryWords, type: "List<String>", _change: add }
        implementations:
          - name: DefaultTopicTeachingResolver
            _change: modify
            requiresInject:
              - { name: topicTheorySource, type: TopicTheorySource, _change: add }
```
