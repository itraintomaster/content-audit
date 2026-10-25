---
feature:
  id: FEAT-OPINA
  code: F-OPINA
  name: Análisis y opiniones - lo que falta se cuenta una vez, como pares ejercicio-juez, y el análisis vigente y las opiniones se leen una vez por pestaña
  priority: major
---

# Análisis y opiniones: lo que falta se cuenta una vez

## TL;DR

**Qué**: content-audit dice, junto con el análisis vigente, la lista de lo que falta opinar: cada par de un ejercicio
y un juez que le corresponde y todavía no opinó. «Completar opiniones» pide exactamente esa lista; el botón dice cuántos
pares faltan, el diálogo dice pares y consultas por separado, y un renglón de Correcciones dice «todavía no opinó» sólo
si su par está en la lista. Una abstención dice «se abstuvo»; un juez que Completar no cubre dice «no se completa desde
acá». Una corrida que no trajo nada nunca tapa lo que ya había. Lo que se pide desde un renglón entra solo al análisis
vigente, como Completar. En el tablero, el análisis vigente y las opiniones se leen una vez por pestaña; integrar,
reemplazar o quitar, desde cualquier pantalla, avisa «análisis» y todo se relee solo. Se van el canal propio del
análisis entre pestañas y la relectura de la tanda cada 5 s mientras corre una opinión.

**Por qué**: «lo que falta» se contaba de tres maneras. El 13/10, «Faltan 5» contaba consultas pagas, el diálogo
contaba ejercicios y los renglones contaban ejercicios por juez, con su propio criterio de «evaluado». Además 20
renglones decían «todavía no opinó» sobre algo que ya había opinado: una corrida posterior que no trajo nada tapaba la
opinión, y el renglón ofrecía pagarla de nuevo. Una abstención guardada salía como si faltara, un juez que Completar no
cubre salía igual que uno pendiente, y lo pedido desde un renglón se mostraba pero Completar lo volvía a pagar.

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html), etapa 6
«Análisis y opiniones». El diagnóstico y las preguntas están en el brief 204. Las decisiones son de José (24/10/2026):
- una opinión pedida desde un renglón entra sola al análisis vigente, como Completar;
- la lectura de opiniones trae todo lo de opinión: cada juez en cada cambio, lo que falta, «nadie le ve problemas» y el
  resumen; la tanda deja de traerlo;
- un juez que Completar no cubre dice «no se completa desde acá», y no se cuenta;
- lo que opinó una versión anterior del juez no cuenta como «falta»: sigue aparte, con su botón «Medir con la de hoy».

El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. José cierra una corrección. Un juez por tema tiene 10 ejercicios cambiados, en 2 temas, sin opinión.
2. El botón dice «Completar opiniones», sin número ⚠️.
3. El diálogo dice «10 ejercicios con opiniones pendientes · 2 consultas nuevas»; por juez, «20 ejercicios de 2 temas»
   ⚠️: cuenta los ejercicios de los temas, no los que faltan.
4. Los renglones suman, además de esos 10, una abstención y una opinión que una corrida vacía tapó ⚠️.
5. Mientras corre la opinión, Correcciones relee la tanda entera cada 5 s ⚠️.
6. José integra una medición desde el Mapa. Otra pestaña se entera por un canal propio del análisis; una pestaña en
   otra ventana sin ese canal no ⚠️.

**Punto de fricción**: pasos 2 a 4 (tres números que no se pueden comparar) y 5 y 6 (relecturas que no siguen al aviso).

**Cambio mínimo necesario**: que content-audit diga la lista de lo que falta, que el tablero la lea una vez y que
botón, diálogo y renglones digan lo mismo; que el análisis y las opiniones se relean con el aviso.

## Reglas de Negocio

Un **par** es un ejercicio y un juez que le corresponde: el juez aplica a ese ejercicio. Un par **falta** si el análisis
vigente de la corrección no tiene opinión guardada de ese juez sobre ese ejercicio. Una **opinión guardada** es un
veredicto con nota o una abstención guardada ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003),
inv. 6). Una **consulta** es lo que se paga: un juez por tema cubre con una consulta todos los pares de su tema. Los
jueces que **Completar cubre** son los que el análisis vigente sabe componer; los demás (hoy, Gramática) no se completan
desde Correcciones.

<a id="F-OPINA-R001"></a>
### Rule[F-OPINA-R001] - Lo que falta se cuenta una vez, en content-audit
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. content-audit dice, con el análisis vigente y para cada estado, la lista de los pares que faltan: por juez, los
>    ejercicios. Sólo entran los jueces que Completar cubre y los ejercicios a los que el juez aplica.
> 2. Una abstención guardada no falta. Una opinión de una versión anterior del juez no falta: sigue aparte, con su marca
>    y «Medir con la de hoy».
> 3. «Completar opiniones» pide exactamente la lista de la corrección: ni un par más ni uno menos.
> 4. El tablero no vuelve a contar lo que falta: el botón, el diálogo y los renglones leen esa lista.

<a id="F-OPINA-R002"></a>
### Rule[F-OPINA-R002] - Pares y consultas se dicen aparte
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El botón dice cuántos pares faltan: «Completar opiniones · faltan 10». Sin ninguno, dice que no falta ninguna y no
>    pide nada.
> 2. El diálogo de Completar dice pares, ejercicios y consultas por separado: «10 pares en 10 ejercicios · 2 consultas
>    nuevas». Por juez, sus pares y sus temas.
> 3. Con un juez por tema, 2 temas y 10 ejercicios que faltan: 10 pares y 2 consultas, en el botón, el diálogo y los
>    renglones.
> 4. Al abrir el diálogo, el número de pares es el número de «todavía no opinó» de la corrección.

<a id="F-OPINA-R003"></a>
### Rule[F-OPINA-R003] - El renglón dice «todavía no opinó» sólo si el par falta
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Un juez en el renglón de un ejercicio dice «todavía no opinó» si y sólo si el par está en la lista de lo que falta.
> 2. Una abstención guardada dice «se abstuvo», con su motivo si content-audit lo nombra.
> 3. Un juez que Completar no cubre y no opinó dice «no se completa desde acá». No se cuenta en lo que falta.
> 4. Lo que opinó una versión anterior del juez dice lo que dijo, con su marca, como hasta ahora
>    ([F-TALDIA-R005](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R005)).
> 5. Mientras una opinión pedida corre, su juez dice «midiendo…» o «cortada», como hasta ahora
>    ([F-CORRI-R011](../2026-10-21.01_las-corridas/REQUIREMENT.md#F-CORRI-R011)).
> 6. Con un content-audit de antes, que no dice la lista, el renglón cuenta como hasta ahora y el botón no dice número.

<a id="F-OPINA-R004"></a>
### Rule[F-OPINA-R004] - Una corrida que no trajo nada no tapa lo que había
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Una corrida de opinión que no dejó veredicto de un juez sobre un ejercicio no cambia lo que se muestra ni lo que se
>    cuenta de ese par: lo que tenía opinión la sigue teniendo y lo que faltaba sigue faltando.
> 2. Vale para Completar y para lo pedido desde un renglón, terminada, fallida o agotada por el tope.
> 3. content-audit no la integra al análisis vigente.

<a id="F-OPINA-R005"></a>
### Rule[F-OPINA-R005] - Lo pedido desde un renglón entra al análisis vigente
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Una opinión pedida para ejercicios sueltos que deja veredicto entra sola al análisis vigente de la corrección,
>    como la de Completar, si la tanda admite integrar (cerrada, o abierta con otra corrección después).
> 2. Si en ese ejercicio había una opinión del mismo juez y la nueva es de la versión de hoy, la reemplaza. Si la nueva
>    no es de la versión de hoy, queda suelta para decidir en el diálogo del análisis vigente.
> 3. Entrada, ese par deja de faltar: Completar no lo vuelve a pedir ni a pagar.
> 4. Un juez que el análisis vigente no compone no entra: su opinión se sigue mostrando en el renglón, como hasta ahora.

<a id="F-OPINA-R006"></a>
### Rule[F-OPINA-R006] - El análisis vigente se lee una vez por pestaña
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. La pestaña lee una vez el análisis vigente de la tanda que mira. El diálogo del análisis vigente, el Mapa,
>    Correcciones y Analizadores leen esa lectura.
> 2. La lectura trae lo que las pantallas usan: los estados con sus cuentas y sus jueces, las mediciones sueltas con
>    dónde encajan, la historia de lo integrado y lo que falta. No trae la vista de cada ejercicio ni el origen de cada
>    veredicto, salvo los enunciados de los ejercicios que una integración cambiaría.
> 3. Lo que una pantalla lee de lo suyo (el estado del Mapa, la tanda de Correcciones) ya no trae su copia del análisis.

<a id="F-OPINA-R007"></a>
### Rule[F-OPINA-R007] - Las opiniones de los cambios se leen una vez por pestaña
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Correcciones lee las opiniones de la tanda que mira en una lectura aparte de la tanda: cada juez en cada cambio,
>    lo que opinó cada juez antes, «nadie le ve problemas», el resumen de la opinión y lo que falta.
> 2. La tanda ya no trae nada de eso.
> 3. Botón, diálogo y renglones leen la misma lectura: no pueden decir números distintos.

<a id="F-OPINA-R008"></a>
### Rule[F-OPINA-R008] - Integrar, reemplazar o quitar avisa «análisis»
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Integrar, reemplazar o quitar una medición del análisis vigente, desde cualquier pantalla del tablero o desde la
>    terminal, avisa «análisis».
> 2. Con el aviso, cada pestaña relee el análisis vigente y las opiniones, una vez.
> 3. Se integra en el Mapa y Correcciones, abierta en otra pestaña, cambia sola en un segundo o menos.
> 4. El análisis ya no tiene un canal propio entre pestañas: el aviso es el único camino.

<a id="F-OPINA-R009"></a>
### Rule[F-OPINA-R009] - Mientras corre una opinión, nada se relee a cada rato
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Correcciones no relee la tanda cada 5 s mientras corre una opinión.
> 2. content-audit guarda lo que avanza cada juez en la tanda: el aviso de la tanda trae ese avance, y la pestaña relee
>    las opiniones una vez.
> 3. Al terminar la corrida, el aviso de las corridas y del análisis trae el resultado.

## Journeys

```yaml
journeys:
  - id: F-OPINA-J001
    name: Completar las opiniones de una corrección
    flow:
      - id: mirar
        action: "José abre una corrección cerrada con 10 ejercicios cambiados de 2 temas sin opinión de un juez por tema"
        gate: [F-OPINA-R001, F-OPINA-R003]
        outcomes:
          - when: "Correcciones lee las opiniones"
            then: boton
      - id: boton
        action: "El botón dice «faltan 10» y los 10 renglones dicen «todavía no opinó»"
        gate: [F-OPINA-R002, F-OPINA-R007]
        outcomes:
          - when: "José toca Completar opiniones"
            then: dialogo
      - id: dialogo
        action: "El diálogo dice «10 pares en 10 ejercicios · 2 consultas nuevas»"
        gate: [F-OPINA-R002]
        outcomes:
          - when: "José lanza y la corrida termina"
            then: integrada
      - id: integrada
        action: "Lo que opinó entra al análisis vigente; el aviso relee las opiniones y los renglones dicen lo que opinó"
        gate: [F-OPINA-R008, F-OPINA-R009]
        result: success
  - id: F-OPINA-J002
    name: Integrar una medición en el Mapa
    flow:
      - id: integrar
        action: "José integra una medición suelta desde el Mapa, con Correcciones abierta en otra pestaña"
        gate: [F-OPINA-R008]
        outcomes:
          - when: "El tablero avisa «análisis»"
            then: correcciones
      - id: correcciones
        action: "Correcciones relee las opiniones y cambia sola en un segundo o menos"
        gate: [F-OPINA-R006, F-OPINA-R007, F-OPINA-R008]
        result: success
  - id: F-OPINA-J003
    name: Pedir la opinión de un ejercicio
    flow:
      - id: pedir
        action: "José pide desde un renglón la opinión de un juez sobre un ejercicio"
        gate: [F-OPINA-R005]
        outcomes:
          - when: "El juez opina"
            then: entra
          - when: "La corrida no trae nada"
            then: nada
      - id: entra
        action: "La opinión entra al análisis vigente y el par deja de faltar"
        gate: [F-OPINA-R005, F-OPINA-R001]
        result: success
      - id: nada
        action: "El renglón sigue diciendo lo que decía antes"
        gate: [F-OPINA-R004]
        result: success
```

## Open Questions

No quedan abiertas.

## Cómo se prueba

Ninguna prueba consulta un modelo: content-audit se prueba con jueces de mentira; el tablero, con un CLI falso; el
navegador, con un servidor de mentira y el store de verdad. Cada prueba nombra su regla en el título.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| [R001](#F-OPINA-R001) | La lista por estado: aplica y sin opinión; la abstención, la versión anterior y lo que no aplica no faltan; un juez que el análisis no compone no entra; Completar pide la lista exacta | `MissingOpinionsTest`, `FOpinaMissingOpinionsTest` (content-audit) |
| [R002](#F-OPINA-R002) | Juez por tema, 2 temas, 10 ejercicios: 10 pares y 2 consultas en la estimación, el diálogo, el botón y los renglones; sin ninguno, el botón no pide | `FOpinaMissingOpinionsTest`, `test/tablero_html/opiniones/rutas_de_opiniones_test.dart`, `html/test/estado-opiniones.test.js`, `html/test/lanzar-opinion-pares.test.js` |
| [R003](#F-OPINA-R003) | «todavía no opinó» si y sólo si el par falta; «se abstuvo»; «no se completa desde acá»; versión anterior; midiendo; un CLI sin la lista | `test/tablero_html/correcciones/opinion_en_contexto_test.dart`, `html/test/correcciones-opinion-en-contexto.test.js` |
| [R004](#F-OPINA-R004) | Una corrida vacía, fallida o agotada no se integra ni tapa en el renglón | `FOpinaExerciseOpinionTest`, `opinion_en_contexto_test.dart` |
| [R005](#F-OPINA-R005) | Lo pedido desde un renglón se integra; reemplaza sólo con la versión de hoy; un juez sin componer no entra; Completar ya no lo pide | `FOpinaExerciseOpinionTest` |
| [R006](#F-OPINA-R006) | `GET /api/analisis` liviano, una lectura por pestaña, leída por el diálogo, el Mapa, Correcciones y Analizadores; el estado del Mapa y la tanda ya no lo traen | `test/tablero_html/analisis_vigente_test.dart`, `html/test/estado-analisis.test.js`, `html/test/arquitectura.test.js` |
| [R007](#F-OPINA-R007) | `GET /api/opiniones` trae lo de opinión y la tanda no; Correcciones las junta sin pedirlas por su lado | `rutas_de_opiniones_test.dart`, `html/test/estado-opiniones.test.js`, `html/test/arquitectura.test.js` |
| [R008](#F-OPINA-R008) | Integrar avisa «análisis»; el aviso relee análisis y opiniones en cada pestaña; Correcciones cambia en otra pestaña; sin canal propio | `test/tablero_html/novedades_test.dart`, `html/test/estado-avisos.test.js`, `html/test/arquitectura.test.js` |
| [R009](#F-OPINA-R009) | Sin relectura cada 5 s; el avance llega con el aviso de la tanda | `html/test/correcciones-opiniones-con-el-aviso.test.js` |

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), etapa 6.
- **Briefs**: 166 (el caso del 13/10), 204 (diagnóstico y preguntas).
- **FEAT-ALDIA**: qué es una opinión guardada y una abstención ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003)).
- **FEAT-TALDIA**: la marca de una opinión de antes ([F-TALDIA-R005](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R005)).
- **FEAT-CORRI**: una opinión cortada ([F-CORRI-R011](../2026-10-21.01_las-corridas/REQUIREMENT.md#F-CORRI-R011)).
- **FEAT-AVISO**: [F-OPINA-R008](#F-OPINA-R008) completa [F-AVISO-R002](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R002) para «análisis».
