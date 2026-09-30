---
feature:
  id: FEAT-DATOS
  code: F-DATOS
  name: Datos coherentes - todas las copias del ejercicio dicen lo mismo
  priority: high
---

# Datos coherentes: la oración guardada, las respuestas y el audio dicen lo mismo

## TL;DR

**Qué**: Un analizador de la familia de errores compara las copias de cada ejercicio —las partes con su primera
respuesta, la oración guardada y el texto con que se grabó su audio— y marca las que no coinciden o llevan «/».

**Por qué**: Cuando se corrige una respuesta, las otras copias quedan viejas: el 077 encontró audios que leen «has not
cut barra hasn't cut» y oraciones guardadas con barra, y hoy nadie lo mide.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy saber si el audio y la oración guardada siguen al ejercicio:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible: mide la oración guardada
   ([F-DBSENT-R002](#F-DBSENT-R002)), pero no la compara con las partes.
2. Busca con qué texto se grabó el audio de un ejercicio → ⚠️ content-audit no lo sabe: el texto vive en el generador de
   marzo de 2025 (ittm) y en el manifiesto de la regrabación del 24/8 (otro worktree).
3. Pide ese analizador solo → ⚠️ no existe en `GetCommand.get("analyzers", …)`.

**Punto de fricción**: pasos 2 y 3.

**Cambio mínimo necesario**: sumar `data-coherence` al catálogo de FEAT-HALL y darle a content-audit un **registro de
grabaciones**: por cada clave de audio, el texto con que se grabó por última vez.

## Reglas de Negocio

Tres copias de un ejercicio: sus **partes** (el texto fijo y las respuestas aceptadas), su **oración guardada** (la
primera de `sentences`: la del árbol de content-audit, que es la que mide, y la de la copia de producción que deja el
importador) y su **audio** (se oye después de contestar). Comparar **como el
corrector** es sin mayúsculas, espacios, signos ni tildes ([F-RESPC-R001](#F-RESPC-R001)).

<a id="F-DATOS-R001"></a>
### Rule[F-DATOS-R001] - Las dos oraciones guardadas son el ejercicio armado con su primera respuesta
**Severity**: major | **Validation**: VALIDATED

> La oración guardada en el árbol de content-audit y la de la copia de producción son iguales, comparadas como el
> corrector, a la que resulta de poner la primera respuesta en el hueco: las partes enteras sin la pista, o sólo la
> respuesta en un tema de reescritura o en una transformación ([F-SMODE-R003](#F-SMODE-R003)); en opción múltiple, el
> enunciado con la correcta ([F-OPMUL-R004](#F-OPMUL-R004)). Por cada una que no, hallazgo *media*, que se resuelve con
> una *regla*: se vuelve a armar desde las partes.

Hoy da 0 en el árbol del curso: al importar, content-audit vuelve a armar la oración si cambió el contenido. Las 9
oraciones con barra del 077 sólo están en la copia de producción, que el importador dejó en
`db/quiz-templates.json` (A2 5, B1 4), como «The tree has not grown/hasn't grown yet.» (`67fab6d5993010229534287e`).
José decidió el 30/9 que se leen las dos ([DOUBT-DATOS-PRODUCCION](#DOUBT-DATOS-PRODUCCION)) y que espacios y
reescrituras no cuentan ([DOUBT-DATOS-718](#DOUBT-DATOS-718)).

<a id="F-DATOS-R002"></a>
### Rule[F-DATOS-R002] - Ni las respuestas ni el audio llevan «/»
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Ninguna respuesta aceptada lleva «/»: el corrector sólo separa en «|», y el alumno tendría que tipear la barra. Es
>    *bloqueante* y se resuelve con una *regla*: la barra pasa a «|».
> 2. El texto con que se grabó el audio no lleva «/», porque el audio la lee. Es *media* y se resuelve con una *regla*:
>    se regraba con el texto de hoy.

Una barra que es parte del enunciado, como «a / an» o «Our / ours», no es hallazgo por sí sola; sí lo es cuando el
audio la lee. Hoy: 0 respuestas (las 90 con barra se arreglaron el 27/9) y 85 audios. Por ejemplo, «He ___ (not / cut)
himself.» acepta «has not cut | hasn't cut» y su audio lee «He has not cut/hasn't cut himself.»
(`67fab6d5993010229534286c`); «Don't touch a / an ___ (asleep / resting) dog!» lee «a / an» (`67fab6d5993010229534489e`).

<a id="F-DATOS-R003"></a>
### Rule[F-DATOS-R003] - El audio dice lo mismo que el ejercicio de hoy
**Severity**: major | **Validation**: ASSUMPTION

> El texto con que se grabó el audio coincide con alguna de las oraciones que hoy dice el ejercicio, comparadas como el
> corrector y tomando igual la forma contraída y la larga. Coincide también si uno contiene al otro: el audio puede
> decir sólo la respuesta, o la respuesta con su oración de partida. Si no coincide, hallazgo *media* para la *mesa*.

<details><summary>Detalle y casos del curso</summary>

Los 7 de hoy:

- *Rangos temporales* (A1, 3): el audio dice «I slept from 7 to 11.» y el ejercicio, «…I slept from 7 to.»: la «|»
  cortó la respuesta (`67fab6d59930102295342462`, `…2465`, `…2467`).
- «This idea does not sound very good.» contra «This idea does not very good.» (`67fab6d59930102295342657`, A2).
- «So is everyone.» contra «So I everyone.» (`67fab6d599301022953433a1`, B1).
- «…she was phoning then.» contra «…phoning now.» (`67fab6d599301022953436c5`, B1).
- «Ana was taught a lesson…» contra «Ana was teached a lesson…» (`67fab6d5993010229534439d`, B2).

Va a la *mesa* porque no se sabe cuál copia está bien: en el último, el audio tiene razón y regrabar copiaría el
error. De los 11.201 audios sin barra, 10.944 coinciden tal cual. Comparar con todas las respuestas aceptadas deja
afuera 50 cuyo audio dice otra respuesta aceptada (22 en *Función de that*, B1); tomar igual la contracción deja afuera
3 («We are very excited.» para «'re»); y que uno contenga al otro deja afuera 197, como los de *If so y if not*, cuyo
audio dice sólo «If not». Todo esto es [ASSUMPTION].

</details>

<a id="F-DATOS-R004"></a>
### Rule[F-DATOS-R004] - Todo ejercicio tiene un audio cuyo texto se conoce
**Severity**: minor | **Validation**: AUTO_VALIDATED

> Todo ejercicio tiene clave de audio, ninguna clave la comparten dos ejercicios, y el registro de grabaciones sabe con
> qué texto se grabó. Si no, hallazgo *baja* que se resuelve con una *regla*: se graba con el texto de hoy.

El registro vive en `db/` de content-audit, se arma una vez desde las dos fuentes y lo actualiza cada regrabación
(José, 30/9; [DOUBT-DATOS-REGISTRO](#DOUBT-DATOS-REGISTRO)).

Hoy: 1, «Are we eating soon?» (`67fab6d599301022953426ac`, A2), cuya clave se vació el 27/9 para regrabarlo. Claves
compartidas: 0. Si el archivo existe en el almacenamiento de audios no se mira: content-audit corre sin red.

<a id="F-DATOS-R005"></a>
### Rule[F-DATOS-R005] - `data-coherence` es un analizador de errores que evalúa ejercicios
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `data-coherence` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Todas las copias del
> ejercicio dicen lo mismo?», familia *errores*, evalúa *ejercicio*, resoluciones *regla* y *mesa*, costo *instantáneo*.
> Un ejercicio con hallazgo tiene puntaje 0; sin ninguno, 1. La evidencia muestra las dos copias que no coinciden, con
> su texto, y la identidad nombra qué copia (oración guardada, respuesta o audio).

Ejemplo de evidencia: «He ___ (not / cut) himself. → audio: "He has not cut/hasn't cut himself."; hoy: "He has not cut
himself."». El nombre es [ASSUMPTION].

## Contexto

La propuesta pide que el texto del audio salga «del generador de marzo de 2025 y de la regrabación del 24/8». Son dos
fuentes de sólo lectura: el manifiesto del 24/8 (`scripts/audio-regen-2026-08-24/regen_manifest.csv` del worktree
`quiz-audio-regeneration-11196a`, 2.958 claves) y, para el resto, el texto del generador de marzo
(`tools/learney_course_generator/learney_simplified/`, el `aiSentence` o, si falta, el `sentence` de cada clave). Con
ellas y la regla de texto hablado del 24/8 se midió el curso vivo del 29/9, con un prototipo en el scratchpad:

| Medida | A1 | A2 | B1 | B2 | Total |
|---|---|---|---|---|---|
| Marcados por el 077 (con las 9 barras) | 2 | 36 | 38 | 2 | 78 |
| R001, árbol de content-audit | 0 | 0 | 0 | 0 | 0 |
| R001, copia de producción | 0 | 5 | 4 | 0 | 9 |
| R002, audio con «/» | 0 | 45 | 37 | 3 | 85 |
| R003, audio distinto | 3 | 1 | 2 | 1 | 7 |
| R004, sin audio | 0 | 1 | 0 | 0 | 1 |

Los 85 audios con barra son de 11 temas; *Participios irregulares 2* tiene 23 y *¿Mismo referente?* 14, con el audio
leyendo «us us / ourselves».

**El examen.** `escribir/datos.json` trae 39 casos cortados por tema (A2 22, B1 15, B2 2): 34 audios que leen la barra
y 5 oraciones guardadas con barra. El prototipo encuentra los 34 audios; las 5 oraciones sólo aparecen leyendo la copia
de producción. Marca 9 más, todos reales: 5 audios con barra que el 077 no anotó («She has not known/hasn't known
anything.») y 4 del R003 que el 077 puso en pista. La otra mitad también tiene 39. **Meta propuesta**: 9 de cada 10
leyendo la copia de producción, y 8 de cada 10 marcas nuevas reales. **Costo**: segundos.

## Alcance

- **Adentro**: R001 a R004, el registro de grabaciones como dato de entrada y la ficha.
- **Afuera**: transcribir una muestra de audios para confirmar (la propuesta sugiere Whisper, con otro costo);
  regrabar, que hace el paquete E con el script de content-audit y la misma voz; la [traducción](glossary:Traducción) y la miniteoría (otros
  analizadores); y escribir el curso al analizar ([F-HALL-R015](#F-HALL-R015)).

## User Journeys

### Journey[F-DATOS-J001] - Comparar las copias de un ejercicio
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-DATOS-J001
    name: Comparar las copias de un ejercicio
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo sólo data-coherence, con el registro de grabaciones cargado"
        then: comparar
      - id: comparar
        action: "El informe compara, en cada ejercicio, las partes, la oración guardada y el texto grabado del audio"
        gate: [F-DATOS-R005]
        outcomes:
          - {when: "Una respuesta aceptada o el texto grabado llevan «/»", then: barra}
          - {when: "La oración guardada no es el ejercicio armado con su primera respuesta", then: oracion}
          - {when: "El audio dice otra cosa que el ejercicio de hoy", then: audio}
          - {when: "El ejercicio no tiene audio, o nadie sabe con qué se grabó", then: sin_audio}
          - {when: "Las tres copias coinciden", then: coincide}
      - {id: barra, action: "Hallazgo bloqueante si la barra está en la respuesta, media si la lee el audio, con arreglo por regla", gate: [F-DATOS-R002], result: success}
      - {id: oracion, action: "Hallazgo media que se arregla volviendo a armar la oración guardada", gate: [F-DATOS-R001], result: success}
      - {id: audio, action: "Hallazgo media para la mesa, con el texto grabado y el de hoy como evidencia", gate: [F-DATOS-R003], result: success}
      - {id: sin_audio, action: "Hallazgo baja que se arregla grabando con el texto de hoy", gate: [F-DATOS-R004], result: success}
      - {id: coincide, action: "Sin hallazgo de este analizador; el ejercicio queda con puntaje 1", gate: [F-DATOS-R005], result: success}
```

## Open Questions

<a id="DOUBT-DATOS-PRODUCCION"></a>
### Doubt[DOUBT-DATOS-PRODUCCION] - ¿Qué oración guardada se compara: la de content-audit o la de producción?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): las dos: la del árbol y la del export de producción que deja el importador.
- [ ] Opción B: sólo la del árbol, y el importador avisa las viejas de producción.

**Answer**: **Opción A** (José, 30/9): se leen las dos oraciones guardadas. La del árbol es la que mide content-audit y hoy está bien; la de producción es la
que leerá la próxima regrabación, y en 9 tiene barra. Con B esos 9 no cuentan como error y el examen pierde 5 de 39.

<a id="DOUBT-DATOS-718"></a>
### Doubt[DOUBT-DATOS-718] - ¿Cuentan las diferencias de espacios y de reescritura?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): no; se compara como el corrector y, en reescritura, vale la respuesta sola o el
  ejercicio entero (0 hallazgos hoy).
- [ ] Opción B: sí; se compara tal cual contra lo que content-audit deriva hoy.

**Answer**: **Opción A** (José, 30/9, con la recomendación): espacios y reescrituras no cuentan. B marcaba las 718 de DOUBT-718-CLOZE de FEAT-OPMUL (325 por espacios como «We 've»,
352 diálogos sin recortar), que no son lo que el 077 llamó error. A no cierra esa duda: sólo no la cuenta.

<a id="DOUBT-DATOS-REGISTRO"></a>
### Doubt[DOUBT-DATOS-REGISTRO] - ¿Dónde vive el registro de grabaciones?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): en `db/` de content-audit, armado una vez desde las dos fuentes y actualizado por cada
  regrabación.
- [ ] Opción B: content-audit lee cada vez las dos fuentes, en sus repositorios.

**Answer**: **Opción A** (José, 30/9, con la recomendación): el registro vive en `db/` de content-audit. Queda versionado con el curso, y la próxima regrabación, que ya sale de
content-audit, lo actualiza en el mismo paso. B depende de un worktree que se puede borrar.

## References

- **FEAT-HALL** — Hallazgo, gravedad, resolución, costo, ficha, conteo e identidad: R001 a R005.
- **FEAT-DBSENT**, **FEAT-QSENT**, **FEAT-SMODE** — La oración guardada, cómo se arma y la reescritura: R001.
- **FEAT-OPMUL** — La oración de una opción múltiple y DOUBT-718-CLOZE: R001 y DOUBT-DATOS-718.
- **FEAT-RESPC** — Comparar como el corrector: R001 a R003.
