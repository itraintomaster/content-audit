---
feature:
  id: FEAT-RONDA2
  code: F-RONDA2
  name: Integración de la ronda 2 - lo que comparten idioma, corrección por tandas y antes y después
  priority: high
---

# Integración de la ronda 2: lo compartido de B, E y F

## TL;DR

**Qué**: Junta en un solo cambio lo que comparten los trece parches de la ronda 2 —B, idioma con herramientas locales;
E, corrección por tandas; F, antes y después— y lo que cada uno dejó como «gancho» en archivos comunes: los nombres y el
orden de las tres fichas nuevas, la corrida base, la medida de cada analizador en el informe, el caché de tokens por
oración y el tipo de tarea de cada analizador de errores.

**Por qué**: Si cada paquete toca por su cuenta el catálogo, el informe, el plan o el caché, chocan, o uno decide algo
que el otro contradice. Y hay dos riesgos de calidad que ningún paquete cubre: una corrida sin la lista de lemas publica
números falsos sin avisar, y el caché de tokens los guarda para las corridas siguientes.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy usar lo de la ronda 2 encima de la ronda 1 integrada:

1. Corre `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)` en una carpeta sin `../ittm` al lado → ⚠️ no falla: las
   bandas de frecuencia salen mal (A1 y A2 en 50,0 %, el curso en 72,75 en vez de 73,86) y el caché de tokens guarda ese
   resultado; las corridas siguientes lo reusan aunque la lista ya esté.
2. Cambia una oración y vuelve a analizar → ⚠️ 38 a 39 segundos: el caché de tokens tiene una sola clave para el lote
   entero, y la tanda (E) y la comparación (F) recalculan el curso muchas veces.
3. Lista los analizadores → `GetCommand.get("analyzers", …)`. ✓ Disponible, pero los tres de B no tienen todavía su
   lugar en el catálogo ni su nombre en castellano ([F-RONDA1-R004](#F-RONDA1-R004)).
4. Pide la vista consolidada → la corrida base ([F-RONDA1-R007](#F-RONDA1-R007)). ⚠️ Con la regla de hoy entraría
   `near-duplicates`, que tiene una regla instantánea, y José decidió que de B en la corrida base va sólo Gramática.
5. Compara una tanda con la línea base (F) → ⚠️ el informe no dice con qué medida midió cada analizador, así que un cambio
   de configuración se confunde con un cambio del curso.
6. Arma el plan de un análisis con hallazgos de errores (E) → ⚠️ los analizadores nuevos no tienen tipo de tarea.

**Punto de fricción**: pasos 1, 2, 5 y 6.

**Cambio mínimo necesario**: que `analyze` frene sin la lista de lemas o sin el procesador de spaCy; un caché de tokens
por oración cuya clave incluye la huella de la lista y del procesador; la medida de cada analizador en la raíz del
informe; un tipo de tarea por analizador de errores; y los lugares tipados, los nombres y el orden de las tres fichas de B.
Nada de esto cambia una regla de los paquetes.

## Reglas de Negocio

Desde el 4/10, las menciones a `claude -p` como ruta activa nombran a `codex-cli`, autenticado con la sesión local de
ChatGPT ([F-JEXAM-R013](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R013)); la distinción funcional entre modelo local sin tope y proveedor por suscripción
con tope 0 sigue igual.

Acá **tema** es lo que el curso llama *knowledge*, como en FEAT-HALL y FEAT-RONDA1. **La lista de lemas** es
`lemmas_20k_words.txt`, la lista COCA con que el procesador de spaCy pone el puesto de frecuencia de cada palabra, y
**el procesador** es el programa de Python que tokeniza (`sample_processor.py`).

### Grupo A — El catálogo y lo que corre

<a id="F-RONDA2-R001"></a>
### Rule[F-RONDA2-R001] - Las tres fichas de B traen su nombre en castellano y van después de las diecisiete
**Severity**: major | **Validation**: VALIDATED

> El catálogo lista, después de los diecisiete analizadores de la ronda 1 y en este orden, `grammar` «Error de
> gramática», `naturalness` «Oración poco natural» y `near-duplicates` «Ejercicio casi repetido», con los nombres de
> [F-FILAS-R001](#F-FILAS-R001). Invariantes:
> 1. los veinte nombres en castellano son distintos y ninguno está vacío ([F-RONDA1-R004](#F-RONDA1-R004));
> 2. los diecisiete de la ronda 1 conservan su lugar: los tres nuevos sólo se suman al final;
> 3. el nombre único sigue siendo el único con que se nombra a un analizador ([F-HALL-R005](#F-HALL-R005)).

Hasta el 1/10 se llamaron «Gramática», «Naturalidad» y «Duplicados parecidos», los nombres de la presentación de B
que vio José (gancho B-17). Los paquetes E y F no suman analizadores.

<a id="F-RONDA2-R002"></a>
### Rule[F-RONDA2-R002] - El tope por omisión depende de a quién se consulta
**Severity**: critical | **Validation**: VALIDATED

> Sin `--budget`, lo que se le consulta a un modelo local no tiene tope, y lo que se le consulta a claude -p tiene el tope
> 0 de [F-JEXAM-R010](#F-JEXAM-R010): los jueces y, cuando falta Ollama, la alternativa de `naturalness` y
> `near-duplicates` ([F-MLOCAL-R008](#F-MLOCAL-R008)). `--budget nombre=N` limita la ruta que toque. Invariantes:
> 1. el runner no completa la política: sin `--budget` llega vacía al proveedor, y el proveedor la completa según a quién
>    va a consultar; el mismo `naturalness` no tiene tope con llama3 en Ollama y tiene tope 0 con claude -p;
> 2. con tope 0 se reusa lo ya registrado y lo demás queda sin evaluar con su motivo, nunca con un puntaje inventado.

Si el tope 0 alcanzara a los modelos locales, `naturalness` y `near-duplicates` nunca calcularían nada en un `analyze`
sin opciones, y José decidió que los tres de B corren en todo `analyze` ([F-MLOCAL-R004](#F-MLOCAL-R004)). Si el runner
les diera «sin tope» por ser de B, sin Ollama ese «sin tope» le llegaría a claude -p: 11.873 consultas que nadie pidió,
unas 67 horas de a una ([F-MLOCAL-R007](#F-MLOCAL-R007)). Por eso el tope no lo decide el costo de la ficha —las dos
fichas de B son de modelo pago, el mayor de sus reglas— sino la ruta (gancho B-06, tal como quedó el 30/9 a la noche).

<a id="F-RONDA2-R003"></a>
### Rule[F-RONDA2-R003] - La corrida base no corre lo que mide con un modelo local
**Severity**: major | **Validation**: VALIDATED

> La corrida base —la de la vista consolidada, la previa de impacto y la proyección— corre los analizadores
> instantáneos y, con tope 0, los que tienen alguna regla instantánea en su ficha y ninguna de *modelo local*. Un
> analizador con una regla de modelo local no entra aunque tenga otra instantánea.

Con los veinte en el catálogo, la corrida base corre catorce: los trece instantáneos —los ocho de vocabulario y largo,
los cuatro instantáneos de A y `grammar`— y `multiple-choice-validity` con tope 0. Quedan afuera `quiz-instruction`, los
tres jueces de C, `naturalness` y `near-duplicates`: ésta tiene la regla instantánea `same-but-wildcard`, pero también
`same-meaning`, de modelo local. Cambia [F-RONDA1-R007](#F-RONDA1-R007) sólo en eso: José decidió que de B en la corrida
base va sólo Gramática ([F-MLOCAL-R004](#F-MLOCAL-R004), gancho B-10). Se decide por las reglas de la ficha y no por su
costo, que en las dos de B es de modelo pago por la alternativa con claude -p; y no hace falta otra marca en la ficha.

<a id="F-RONDA2-R004"></a>
### Rule[F-RONDA2-R004] - Cada informe dice con qué medida midió cada analizador
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La raíz de cada informe guarda, por cada analizador que corrió y en el orden del catálogo, su medida: el nombre, una
> huella y la configuración con que midió. Invariantes:
> 1. la huella es la misma mientras no cambie cómo mide el analizador: sus reglas y el costo de cada una, su familia, lo
>    que evalúa, sus resoluciones, su configuración y la herramienta con que midió —la local o, sin ella, claude -p—
>    con su versión examinada; si cambia cualquiera de esas cosas, cambia la huella;
> 2. no entran en la huella los textos de la ficha —el nombre en castellano, la pregunta, las descripciones y la meta—, el
>    estado del examen de sus herramientas ni los motivos o las indicaciones para instalarlas;
> 3. la misma ficha y la misma configuración dan la misma huella en cualquier corrida y en cualquier máquina;
> 4. sólo la raíz lleva medidas, y un informe guardado antes de esta regla se lee con la lista vacía: nada inventa su
>    medida.

<details><summary>Detalle y caso del curso</summary>

Es lo que necesita la comparación de F para mostrar aparte un cambio de medida ([F-METAS-R007](#F-METAS-R007)) y para
que la proyección mida con lo mismo que la línea base ([F-PROYU-R003](#F-PROYU-R003)): ganchos F-01 a F-04. La huella
es el SHA-256 de un JSON con las claves en orden.

**Criterio**: con las bandas 8 · 6 · 4 · 3 de `lemma-count` (FEAT-POCOV) la huella de `lemma-count` es otra que con 4
para todas, y la de los otros diecinueve no cambia; si mañana «Palabras poco vistas» pasa a llamarse de otra forma,
ninguna huella cambia. Si Ollama no está y `naturalness` mide con claude -p ([F-MLOCAL-R007](#F-MLOCAL-R007)), su huella
cambia: marca por la respuesta y no por un número, y la comparación tiene que mostrarlo como cambio de medida y no como
un curso que mejoró o empeoró.

Los textos y el estado del examen quedan afuera porque cambian sin que cambie lo que se mide: con ellos, corregir una
descripción o aprobar un examen se mostraría como un cambio de medida y la línea base dejaría de compararse.

</details>

### Grupo B — Los tokens

<a id="F-RONDA2-R005"></a>
### Rule[F-RONDA2-R005] - El caché de tokens guarda cada oración por separado
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El caché de tokens en disco guarda los tokens de cada oración aparte, y un lote busca cada una de sus oraciones.
> Invariantes:
> 1. spaCy corre una sola vez por lote, y sólo con las oraciones que no están en el caché;
> 2. un lote con todas sus oraciones en el caché no corre spaCy;
> 3. los tokens de una oración son los mismos si se tokenizó sola, en un lote o desde el caché;
> 4. el lote devuelve sus oraciones en el orden en que se pidieron.

Es el gancho E-10 de la tanda y F-07 de la comparación, uno solo. Medido el 30/9: con cualquier oración cambiada,
`analyze` tarda 38 a 39 segundos porque la clave de hoy es el lote entero; con el curso en el caché, 2,35 s; un
lanzamiento de spaCy con 4 oraciones, 2,2 s. Con esta regla, cerrar una tanda o comparar cuesta el arranque de spaCy más
lo nuevo, unos 5 a 8 segundos en vez de 38 ([F-TANDA-R006](#F-TANDA-R006), [F-METAS-R004](#F-METAS-R004)).

<a id="F-RONDA2-R006"></a>
### Rule[F-RONDA2-R006] - La clave del caché incluye la huella de la lista de lemas y la del procesador
**Severity**: critical | **Validation**: VALIDATED

> La clave de cada oración en el caché de tokens es la oración más la huella de la lista de lemas y la huella del
> procesador de spaCy. Invariantes:
> 1. la huella es la del contenido de cada archivo, no su ruta ni su fecha;
> 2. si cambia la lista de lemas o el procesador, ninguna oración guardada antes se reusa;
> 3. lo que se guardó con la clave de hoy —el lote entero, sin huellas— no se lee nunca más.

Pedido del orquestador del 30/9: una corrida sin la lista guardó A1 y A2 en 50,0 % y el curso en 72,75, y las corridas
siguientes lo reusaban aunque la lista ya estuviera. Con la huella en la clave, una lista distinta nunca reusa tokens
de otra, y lo envenenado con la clave vieja queda sin leer.

<a id="F-RONDA2-R007"></a>
### Rule[F-RONDA2-R007] - Sin la lista de lemas o sin el procesador de spaCy, `analyze` frena con un error claro
**Severity**: critical | **Validation**: VALIDATED

> Si falta la lista de lemas, falta el procesador de spaCy, o hay oraciones que no están en el caché y spaCy no corre,
> `analyze` frena antes de medir. Invariantes:
> 1. no escribe informe ni guarda nada en el caché, y sale con código distinto de 0;
> 2. el mensaje dice qué falta y dónde lo buscó, como «Falta la lista de lemas en
>    '/ruta/a/ittm/pipeline/src/main/resources/vocabulary/lemmas_20k_words.txt'»;
> 3. nunca sigue con tokens básicos: el modo reducido de [F-NLP-R030](#F-NLP-R030) ya no corre;
> 4. con la lista y el procesador en su lugar y todas las oraciones en el caché, corre sin lanzar spaCy.
>
> Vale igual para todo lo que mide un curso: `stats analyzer`, la vista consolidada, la proyección, la comparación y la
> tanda.

Pedido del orquestador del 30/9, junto con R006: `Main` busca la lista en `../ittm/…/lemmas_20k_words.txt`, y sin `ittm`
al lado `analyze` no fallaba. Cambiar el modo reducido de F-NLP-R030 es
[DOUBT-RONDA2-SPACY](#DOUBT-RONDA2-SPACY), que sigue con la recomendada.

### Grupo C — Las tareas

<a id="F-RONDA2-R008"></a>
### Rule[F-RONDA2-R008] - Cada analizador de errores tiene su tipo de tarea, salvo los que sólo ordenan
**Severity**: critical | **Validation**: VALIDATED

> Cada analizador de la familia de errores de las rondas 1 y 2 liga sus hallazgos al plan con su tipo de tarea, sobre el
> ejercicio. Invariantes:
> 1. el tipo es el de la tabla de abajo;
> 2. `naturalness` nunca tiene tarea, porque sólo ordena ([F-TAREA-R005](#F-TAREA-R005));
> 3. `off-level-words` no tiene tipo propio: lo cubre la tarea `LEMMA_ABSENCE` del ejercicio
>    ([F-TAREA-R004](#F-TAREA-R004));
> 4. las 4.036 tareas de vocabulario de hoy no cambian ([F-TAREA-R007](#F-TAREA-R007)).

<details><summary>El tipo de tarea de cada analizador de errores</summary>

| Analizador | Tipo de tarea |
|---|---|
| `quiz-instruction` | `QUIZ_INSTRUCTION`, el de hoy |
| `hint-coherence` | `HINT_COHERENCE` |
| `answer-completeness` | `ANSWER_COMPLETENESS` |
| `multiple-choice-validity` | `MULTIPLE_CHOICE_VALIDITY` |
| `data-coherence` | `DATA_COHERENCE` |
| `exact-duplicates` | `EXACT_DUPLICATES` |
| `translation-sense` | `TRANSLATION_SENSE` |
| `single-answer` | `SINGLE_ANSWER` |
| `mini-theory` | `MINI_THEORY`: una tarea por miniteoría ([F-TAREA-R003](#F-TAREA-R003)) |
| `grammar` | `GRAMMAR` |
| `near-duplicates` | `NEAR_DUPLICATES` |
| `naturalness` | ninguno: sólo ordena |
| `off-level-words` | ninguno: lo cubre `LEMMA_ABSENCE` |

Los ocho de A y C son los ganchos E-01 y E-02. `grammar` y `near-duplicates` se suman porque sus hallazgos cuentan como
error y van a regla o a mesa ([F-GRAMA-R005](#F-GRAMA-R005), [F-DUPAR-R004](#F-DUPAR-R004)): sin tipo, el plan los
declararía «no convertidos» y ninguna tanda los tomaría (gancho B-18). Es
[DOUBT-RONDA2-TAREAS-B](#DOUBT-RONDA2-TAREAS-B), que sigue con la recomendada.

</details>

<a id="F-RONDA2-R009"></a>
### Rule[F-RONDA2-R009] - Gramática y Duplicados parecidos declaran lo que su arreglo puede tocar
**Severity**: major | **Validation**: ASSUMPTION

> Como los analizadores de la ronda 1 ([F-TAREA-R006](#F-TAREA-R006)), `grammar` y `near-duplicates` declaran qué partes
> del ejercicio puede cambiar su arreglo:
> 1. `grammar` por regla: las respuestas aceptadas (sale la palabra repetida pegada al hueco), el enunciado y la oración
>    guardada (entra el signo final o el espacio después del punto); nunca el audio;
> 2. `grammar` en la mesa: el enunciado, las respuestas, la pista, la traducción, la oración guardada y el audio;
> 3. `near-duplicates` en la mesa: el ejercicio que sobra entero, con su audio, como `exact-duplicates`;
> 4. ninguno de los dos cubre a otro analizador, agrupa por tema, desempata ni ofrece dos arreglos.

Es lo que dicen [F-GRAMA-R005](#F-GRAMA-R005) y [F-DUPAR-R004](#F-DUPAR-R004), traducido a las partes del ejercicio de
FEAT-TAREA. El arreglo por regla de `grammar` es el cuarto de los de regla de la tanda, junto a los de A (gancho E-06,
[F-TANDA-R004](#F-TANDA-R004)). Queda como supuesto hasta que el examen de Gramática lo confirme con casos de `escribir/`.

### Grupo D — Los números

<a id="F-RONDA2-R010"></a>
### Rule[F-RONDA2-R010] - Integrar la ronda 2 no mueve ningún número de vocabulario ni el curso
**Severity**: critical | **Validation**: VALIDATED

> Con los veinte analizadores en el catálogo, `analyze` sobre la base del 29/9 da:
> 1. vocabulario 73,86 % y niveles 96,02 · 93,60 · 78,87 · 63,86, los de la ronda 1 integrada (FEAT-POCOV);
> 2. en el plan, las mismas 4.036 tareas de vocabulario, una por una; las de errores son nuevas, y por cada analizador
>    sus tareas, sus cubiertos y sus no convertidos suman sus hallazgos que cuentan ([F-TAREA-R001](#F-TAREA-R001));
> 3. el curso idéntico byte a byte ([F-HALL-R015](#F-HALL-R015)): ni el análisis ni el plan escriben el curso.

Los tres de B son de la familia de errores y no entran al promedio de vocabulario; E y F no suman analizadores. Lo único
que cambia el curso es publicar una tanda ([F-TANDA-R009](#F-TANDA-R009)), y eso no pasa al integrar.

## Contexto

La ronda 2 diseñó en paralelo tres paquetes: B, idioma con herramientas locales (cuatro parches); E, corrección por
tandas (cinco); y F, antes y después (cuatro). Cada uno validó contra el `sentinel.yaml` con 0.2 y 0.3 aplicados y dejó
ganchos: 20 de B, 18 de E y 18 de F. Esta integración los junta encima de la ronda 1 integrada, como FEAT-RONDA1 lo hizo
con A, C y D, y suma dos riesgos de calidad que encontró el orquestador al correr `analyze` sobre una copia (R006, R007).

Lo que se decidió al integrar, sin cambiar ninguna regla de los paquetes (el porqué de cada una está en
`memory/decisions.md`):

- **Qué es una tanda, el orden de la bandeja, la línea base fija y el largo** ya estaban alineados entre E y F en sus
  propios textos: las pendientes de la tanda y ninguna otra ([F-TANDA-R006](#F-TANDA-R006)); primero la muestra ciega
  ([F-BANDEJA-R001](#F-BANDEJA-R001)); la base no se mueve al publicar ([F-PROYU-R006](#F-PROYU-R006)); el largo nunca
  devuelve un cambio ([F-METAS-R003](#F-METAS-R003)).
- **Abrir una tanda fija sus metas** ([F-METAS-R006](#F-METAS-R006)): la tanda recibe el libro de metas de F.
- **La comparación no devuelve un cambio por una palabra que el tema enseña** (gancho F-14): recibe lo que el tema enseña
  de D, el mismo que usa la mesa.
- **La mesa mide lo que dice el audio con la pieza de A**, que se abre para eso: hasta ahora era interna de su paquete.
- **Los arreglos por regla y las declaraciones de corrección de A, C, D y B** van en los parches de E que declaran sus
  contratos, no acá: un elemento que implementa un contrato que todavía no existe no se puede declarar antes.

## Alcance

- **Adentro**: R001 a R010 y los lugares compartidos: los tres diagnósticos de B en el ejercicio y los dos en el curso,
  la tarjeta de herramienta en la ficha, la medida del analizador, los diez tipos de tarea nuevos y el error de recursos
  de spaCy.
- **Afuera**: el cableado de `Main`, los `pom.xml`, `medir.py` y las definiciones de agentes, que no son de Sentinel y
  quedan como ganchos para construir; la calibración de los umbrales de Naturalidad en B1 y B2 y los exámenes de las tres
  versiones de B, que hace el orquestador antes de publicar; y `analysis/metas.json`, que son datos.

## Open Questions

<a id="DOUBT-RONDA2-SPACY"></a>
### Doubt[DOUBT-RONDA2-SPACY] - Sin spaCy, ¿`analyze` frena o sigue en modo reducido?
**Status**: OPEN

- [ ] Opción A (recomendada): frena con el error que dice qué falta (R007). Ningún número sale de una corrida a medias.
- [ ] Opción B: sigue como dice [F-NLP-R030](#F-NLP-R030), con tokens básicos, pero el informe se marca «sin spaCy» y no
  se guarda ni puede ser línea base.

**Answer**: Pendiente. Recomiendo A, y se construye con A: hoy el modo reducido no avisa nada y las bandas salen mal sin
que nadie lo note; con B habría que enseñarle a cada consumidor del informe a desconfiar de él.

<a id="DOUBT-RONDA2-TAREAS-B"></a>
### Doubt[DOUBT-RONDA2-TAREAS-B] - ¿Gramática y Duplicados parecidos abren tareas en las tandas?
**Status**: OPEN

- [ ] Opción A (recomendada): sí: `GRAMMAR` por regla y por mesa, `NEAR_DUPLICATES` por mesa (R008, R009). Las tandas
  los corrigen junto con los de la ronda 1.
- [ ] Opción B: todavía no: el plan los declara «no convertidos» con su motivo y se corrigen en una ronda posterior,
  después de medir el examen de cada uno.

**Answer**: Pendiente. Recomiendo A, y se construye con A: los dos cuentan como error y sus requirements ya dicen que se
arreglan por regla y en la mesa; con B, sus hallazgos subirían «algún error» y ninguna tanda podría bajarlo.

## Referencias

- **FEAT-RONDA1** — La integración anterior: el nombre en castellano (R004), la corrida base (R007) y los números (R008).
- **FEAT-HALL** — La ficha, la corrida base, el curso que no se escribe y la identidad de los hallazgos.
- **FEAT-MLOCAL, FEAT-GRAMA, FEAT-NATUR, FEAT-DUPAR** — El paquete B: R001 a R004, R008 y R009.
- **FEAT-TAREA, FEAT-TANDA, FEAT-NOEMP, FEAT-MESA, FEAT-BANDEJA** — El paquete E: R005, R008, R009.
- **FEAT-PROYU, FEAT-METAS, FEAT-CIEGA, FEAT-CERO** — El paquete F: R004 y R005.
- **FEAT-NLP** — El tokenizador, su caché y su modo reducido: R005 a R007.
