---
feature:
  id: FEAT-GUARDA
  code: F-GUARDA
  name: Guardar sin releer la tanda - aprobar, rechazar, anotar y deshacer guardan lo suyo y contestan sólo lo que cambió
  priority: major
---

# Guardar sin releer la tanda

## TL;DR

**Qué**: Guardar una decisión (aprobar, rechazar, volver a «sin revisar») o un caso de un problema (anotar con P,
deshacer la anotación) ya no relee la tanda entera. content-audit comprueba lo decidido con lo guardado de los
ejercicios que toca, lo guarda y contesta sólo lo que cambió: unos KB en vez de la revisión entera. El tablero pone eso
sobre lo que ya tenía leído, sin volver a pedirle la tanda a content-audit, y avisa igual que antes.

**Por qué**: guardar tardaba unos 6 s en el tablero, y lo pagaban aprobar, rechazar, decidir y anotar. content-audit
leía la tanda, la volvía a leer entera para contestar (todas las versiones de la corrección, el curso compuesto, el
plan) y contestaba la revisión entera, unos 40 MB, que el servidor del tablero volvía a leer. Anotar pagaba además una
lectura entera antes de guardar, y después de anotar la otra pestaña esperaba otra lectura de la tanda (8 s) para ver el
rechazo. Las pantallas ya no esperan el guardado, pero la otra pestaña y Publicar se enteran recién cuando termina.

Fuente: la recomendación 2 del [brief 213](../../docs/briefs/213-las-nueve-etapas.html), que José aceptó el 25/10/2026
(«hagamos tu recomendación»); medido en los briefs [208](../../docs/briefs/208-cambios-y-decisiones-hecho.html) y
[211](../../docs/briefs/211-anotar-sin-esperar.html). La meta es unos 2 s; el segundo de
[F-DECIDE-R004](../2026-10-24.04_cambios-y-decisiones/REQUIREMENT.md#F-DECIDE-R004) se sigue contando desde que se
guarda. Quedan afuera el arranque del comando (unos 3 s de la marca del juez) e «integrar» (85 a 99 s).

El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. José aprueba una corrección en Correcciones ✓: la ve aprobada al instante.
2. content-audit la guarda en unos 4 s ⚠️: relee la tanda entera y contesta 40 MB, que el tablero vuelve a leer.
3. La otra pestaña y Publicar la cuentan recién ahí ⚠️, unos 6 s después del clic.
4. Anota un caso con P ✓: el cajón se cierra en el acto.
5. content-audit lo guarda en 6 a 10 s ⚠️: lee la tanda entera antes de guardar y otra vez para rechazar.
6. La otra pestaña ve el rechazo recién cuando el tablero vuelve a leer la tanda ⚠️, 8 s más.

**Punto de fricción**: pasos 2, 3, 5 y 6. Guardar una cosa relee todo.

**Cambio mínimo necesario**: que guardar lea sólo lo que toca y conteste sólo lo que cambió, y que el tablero lo ponga
sobre lo que ya tiene.

## Reglas de Negocio

Una **corrección** es un paso de un cambio sobre un ejercicio, con su antes y su después. Su **estado** es lo decidido
de ella: aprobada, rechazada o sin revisar, quién y cuándo. Una decisión se guarda por cambio y por ejercicio, así que
sólo puede cambiar el estado de las correcciones de esos cambios en esos ejercicios. La **respuesta corta** de una
decisión es la tanda (su número, la versión del contenido, la corrección y su estado), cuántas decisiones tiene
guardadas y esas correcciones, cada una como la mostraría la revisión entera.

<a id="F-GUARDA-R001"></a>
### Rule[F-GUARDA-R001] - Guardar una decisión no relee la tanda entera
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Para guardar una decisión, content-audit lee la tanda una sola vez y, de sus propuestas, sólo las de los
>    ejercicios que nombra la decisión.
> 2. No lee las otras versiones de la corrección ni las otras tandas de la campaña, ni el plan, ni arma el curso
>    compuesto.
> 3. Las decisiones que la tanda heredó de su versión anterior salen de esa misma lectura: no se vuelve a leer el
>    archivo de la tanda mientras no cambie. Si cambió, se lee otra vez.

<a id="F-GUARDA-R002"></a>
### Rule[F-GUARDA-R002] - Guardar contesta sólo lo que cambió
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Quien pide la respuesta corta recibe la tanda, su versión, su corrección y su estado, cuántas decisiones tiene
>    guardadas y cada corrección cuyo estado pudo cambiar la decisión, tal como la mostraría la revisión entera.
> 2. Una decisión sobre un ejercicio entero contesta cada una de sus correcciones.
> 3. Quien no la pide sigue recibiendo la revisión entera, como antes (la app de Flutter).

<a id="F-GUARDA-R003"></a>
### Rule[F-GUARDA-R003] - Anotar y deshacer tampoco releen la tanda
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Anotar un caso lee sólo las correcciones de los ejercicios anotados, con las decisiones guardadas en ese
>    momento.
> 2. Si la anotación rechaza correcciones, la respuesta trae lo que cambió, como la respuesta corta de una decisión.
> 3. Deshacer la anotación también lee sólo las correcciones del caso. Contesta el problema, como siempre; quien pide
>    también lo que cambió lo recibe aparte, junto al problema.

<a id="F-GUARDA-R004"></a>
### Rule[F-GUARDA-R004] - Lo guardado no cambia de forma
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El archivo de una decisión tiene los mismos datos con la respuesta corta que con la entera: la tanda, las
>    correcciones elegidas con su antes y su después, el estado, quién y la cuenta que la protege.
> 2. El caso que guarda anotar congela las mismas correcciones, con el mismo antes y el mismo después, la misma
>    versión y la misma corrección que cuando se leía la tanda entera.

<a id="F-GUARDA-R005"></a>
### Rule[F-GUARDA-R005] - Se rechaza lo mismo que antes
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Una corrección que cambió o se retiró, una elección sin su paso y una tanda ya integrada se rechazan sin escribir
>    nada, como antes.
> 2. Aprobar una conversión a opción múltiple sigue pidiendo la aprobación vigente de sus jueces; rechazarla, no.
> 3. Anotar sobre una tanda que cambió, o rechazar al anotar en una tanda publicada, se rechaza sin guardar el caso.
> 4. Anotar y deshacer siguen protegidos por la cuenta de decisiones que leyeron: si alguien decidió en el medio, no
>    se escribe.

<a id="F-GUARDA-R006"></a>
### Rule[F-GUARDA-R006] - El tablero pone la respuesta corta sobre lo que tiene
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Aprobar, rechazar o volver a «sin revisar» desde Correcciones, anotar y deshacer piden la respuesta corta.
> 2. El tablero pone los estados que trae sobre lo que ya tenía leído de esa tanda, sin volver a pedírsela a
>    content-audit y sin rearmar los cambios.
> 3. Si lo que tenía es de otra versión o de otra corrección, o le falta alguna corrección de la respuesta, lee la
>    tanda entera, como antes.
> 4. Lo que el navegador recibe de anotar y de deshacer no cambia.

<a id="F-GUARDA-R007"></a>
### Rule[F-GUARDA-R007] - Los avisos salen igual, y la otra pestaña no espera otra lectura
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Decidir avisa «decisiones»; anotar y deshacer avisan «problemas» y «decisiones», como antes: lo que cambia
>    mientras se guarda sale en el aviso de la escritura, o a lo sumo medio segundo después
>    ([F-AVISO-R008](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R008)). Una escritura que termina antes
>    de ese medio segundo da un solo aviso de cada tema, también cuando lo que rechazó se pone sobre lo que se mira.
> 2. La otra pestaña y Publicar reciben lo guardado con ese aviso, sin que el tablero vuelva a pedirle la tanda a
>    content-audit: también después de anotar, que antes esperaba otra lectura.

## Lo que queda afuera

- **El arranque del comando** (Java, la marca del juez de `exam status`, unos 3 s) y **«integrar»** (85 a 99 s): son
  los candidatos siguientes del brief.
- **Leer la tanda** para abrir Correcciones, releer a mano o después de una decisión tomada desde afuera: sigue igual.
- **La app de Flutter** sigue pidiendo la revisión entera al decidir.

## Journeys

```yaml
journeys:
  - id: F-GUARDA-J001
    name: Aprobar con dos pestañas abiertas
    flow:
      - id: aprobar
        action: "José aprueba una corrección en Correcciones"
        gate: [F-GUARDA-R001, F-GUARDA-R005]
        outcomes:
          - when: "content-audit la guarda sin releer la tanda"
            then: contestar
          - when: "La corrección cambió o la tanda se integró"
            then: rechazada
      - id: contestar
        action: "content-audit contesta sólo la corrección aprobada"
        gate: [F-GUARDA-R002, F-GUARDA-R004]
        outcomes:
          - when: "El tablero la pone sobre lo que tenía"
            then: avisar
      - id: avisar
        action: "El tablero avisa «decisiones» y la otra pestaña y Publicar la cuentan sin otra lectura"
        gate: [F-GUARDA-R006, F-GUARDA-R007]
        result: success
      - id: rechazada
        action: "No se escribe nada y la pantalla dice por qué"
        gate: [F-GUARDA-R005]
        result: failure
  - id: F-GUARDA-J002
    name: Anotar un caso con P y deshacerlo
    flow:
      - id: anotar
        action: "José anota un caso y rechaza la corrección"
        gate: [F-GUARDA-R003, F-GUARDA-R004, F-GUARDA-R005]
        outcomes:
          - when: "content-audit guarda el caso y el rechazo leyendo sólo ese ejercicio"
            then: ver
      - id: ver
        action: "La otra pestaña ve el rechazo con el aviso, sin otra lectura de la tanda"
        gate: [F-GUARDA-R006, F-GUARDA-R007]
        outcomes:
          - when: "José deshace la anotación"
            then: deshacer
      - id: deshacer
        action: "La corrección vuelve a como estaba y las pestañas lo ven"
        gate: [F-GUARDA-R003, F-GUARDA-R006, F-GUARDA-R007]
        result: success
```

## Open Questions

No quedan abiertas.

## Cómo se prueba

Ninguna prueba consulta a un modelo ni corre una medición de verdad. Las de content-audit usan carpetas de verdad o
almacenes simulados; las del tablero, un CLI falso. Cada prueba nombra su regla en su `@Tag` o en su título.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| [R001](#F-GUARDA-R001) | Guardar lee la tanda una vez y sólo la propuesta de su ejercicio, sin el plan ni las otras tandas; con otra tanda ilegible en la campaña, guardar corto funciona y la respuesta entera no; las decisiones heredadas no vuelven a leer un archivo que no cambió (ilegible con el mismo tamaño y la misma hora) y sí uno que cambió; cada respuesta es una copia | `SavingADecisionWithoutRereadingTest`, `SavingWithoutRereadingCmdTest`, `InheritedReviewsOfALoadedBatchTest` |
| [R002](#F-GUARDA-R002) | La respuesta corta trae la tanda, su versión, su corrección, su estado, la cuenta y sólo la corrección decidida, igual que en la revisión entera, en pocos KB; un ejercicio entero trae cada corrección; sin pedirla, la revisión entera; `--changed-only` sólo en la forma del tablero y por el camino normal | `SavingADecisionWithoutRereadingTest`, `SavingWithoutRereadingCmdTest`, `BatchCmdTest` |
| [R003](#F-GUARDA-R003) | Leer las correcciones de unos ejercicios da lo mismo que la revisión entera, sin el plan ni las otras propuestas; anotar no lee ni guarda la revisión entera, contesta el rechazo de su ejercicio y no el de otro; deshacer contesta el problema solo y, con `--with-decisions`, lo que devolvió | `SavingADecisionWithoutRereadingTest`, `SavingWithoutRereadingCmdTest` |
| [R004](#F-GUARDA-R004) | El archivo de la decisión es el mismo con la respuesta corta y con la entera; el caso congela las mismas correcciones, antes, después, versión y corrección que la revisión entera | `SavingADecisionWithoutRereadingTest`, `SavingWithoutRereadingCmdTest` |
| [R005](#F-GUARDA-R005) | Corrección cambiada o retirada, elección sin paso y tanda integrada se rechazan sin escribir; la conversión pide sus jueces para aprobar y no para rechazar; anotar sobre una tanda que cambió o rechazar en una publicada no guarda el caso; la cuenta que protege anotar llega al almacén | `SavingADecisionWithoutRereadingTest`, `SavingWithoutRereadingCmdTest`, `FileSystemBatchStoreTest` (`guardedUndoCannotOverwriteAReviewAppendedByAnotherProcess`) |
| [R006](#F-GUARDA-R006) | Decidir, anotar y deshacer piden la respuesta corta; los estados se ponen sobre lo leído sin llamar al CLI otra vez ni rearmar los cambios; con otra versión o una corrección que falta, se lee entera; el navegador recibe de anotar y deshacer lo mismo que antes | ittm: `test/tablero_html/guardar_sin_releer_test.dart` |
| [R007](#F-GUARDA-R007) | Con una escritura corta, decidir sube «decisiones» una vez, y anotar y deshacer, «problemas» y «decisiones» una vez cada uno; después de guardar o de anotar, las decisiones al día y `/api/decisiones` traen lo guardado sin otra lectura de la tanda; una decisión de afuera anterior se lee igual | ittm: `test/tablero_html/guardar_sin_releer_test.dart` |

## Cómo se mide

En una copia de la campaña A1 V11 sin modelos (corrección 4, 1784 correcciones), con los mismos ejercicios antes y
después: lo que tarda el comando de content-audit en aprobar, rechazar y anotar, y lo que tarda el tablero desde el
pedido hasta que lo guardado está. Los números están en el brief de la tarea.

## References

- **FEAT-DECIDE**: lo propuesto y lo decidido, aparte ([etapa 7](../2026-10-24.04_cambios-y-decisiones/REQUIREMENT.md));
  su R004 cuenta el segundo desde que se guarda.
- **FEAT-PROBS**: anotar con P sin esperar ([F-PROBS-R009](../2026-10-24.08_los-problemas/REQUIREMENT.md#F-PROBS-R009)).
- **FEAT-AVISO**: un cambio, un aviso ([F-AVISO-R008](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R008)).
