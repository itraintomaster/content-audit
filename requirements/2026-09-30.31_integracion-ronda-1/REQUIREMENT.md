---
feature:
  id: FEAT-RONDA1
  code: F-RONDA1
  name: Integración de la ronda 1 - lo que comparten reglas, jueces y vocabulario, y los tres pedidos de la UI
  priority: high
---

# Integración de la ronda 1: lo compartido y los tres pedidos de la UI

## TL;DR

**Qué**: Junta en un solo cambio lo que comparten los catorce parches de la ronda 1 (reglas, jueces y vocabulario) y
suma los tres pedidos de la UI que José aprobó: el nombre en castellano en la ficha, el ejercicio como lo ve el alumno y
los conteos por gravedad y por regla.

**Por qué**: Cada paquete dejó ganchos en archivos comunes, y si cada uno los toca por su cuenta chocan. Sin los tres
pedidos, la UI tendría que inventar nombres, oraciones y conteos, que es lo que la línea de la UI prohíbe.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy leer lo que marcan los analizadores, desde la UI o desde el CLI:

1. Lista los analizadores → `GetCommand.get("analyzers", …)`. ✓ Disponible: cada ficha trae nombre, pregunta, reglas,
   familia, meta y costo ([F-HALL-R006](#F-HALL-R006)), pero el nombre es un código en inglés, como `sentence-length`.
2. Lee los números y los hallazgos de un análisis → el resumen del informe (`AuditReportStore.loadDigest`). ✓
   Disponible, pero cada ejercicio lleva la etiqueta de su tema: los 20 de *Afirmativas regulares* se llaman igual.
3. Mira un tema entero como lo ve el alumno → ⚠️ no hay verbo, y el informe guarda el ejercicio en el formato interno
   del curso («You ____ [see] (see) cats.»).
4. Cuenta cuántos ejercicios marca cada analizador, por gravedad y por regla → ⚠️ el informe cuenta errores por
   analizador ([F-HALL-R008](#F-HALL-R008)), pero no por gravedad ni por regla, y a los de vocabulario no los cuenta.
5. Corre `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)` con la opción múltiple y su juez → ⚠️ si el juez no
   corrió, el ejercicio queda sin forma de decir «revisado a medias», y la vista consolidada no ve sus errores de regla.

**Punto de fricción**: pasos 3 a 5.

**Cambio mínimo necesario**: una sola vista del ejercicio como lo ve el alumno, que usan los analizadores nuevos, el
resumen y un verbo nuevo, `get student-view <tema>`; el nombre en castellano en la ficha; los conteos por gravedad y
por regla en los números de cada contexto; y el ejercicio evaluado a medias y la corrida base con las reglas
instantáneas de la opción múltiple. Los enganches comunes de la ronda no cambian ninguna regla de los paquetes.

## Reglas de Negocio

Acá **tema** es lo que el curso llama *knowledge* y **topic** es la agrupación de temas, como en FEAT-HALL.

<a id="F-RONDA1-R001"></a>
### Rule[F-RONDA1-R001] - El ejercicio como lo ve el alumno sale de un solo lugar, y siempre igual
**Severity**: critical | **Validation**: AUTO_VALIDATED

> content-audit arma una sola vista de cada ejercicio como lo ve el alumno, y la evidencia de los hallazgos, el
> contenido que reciben los jueces nuevos, el resumen y `get student-view` salen de ella. Invariantes:
> 1. el enunciado lleva «___» en cada hueco y la pista como aparece; cada hueco trae su pista, sus respuestas aceptadas
>    separadas en «|» y la primera; la opción múltiple trae sus opciones en el orden del alumno, con la correcta marcada;
>    y la vista trae la consigna, la [traducción](glossary:Traducción) y la miniteoría del ejercicio;
> 2. el ejercicio terminado es la oración con la primera respuesta, o la opción correcta, y sin la pista: el texto entero
>    en un tema de huecos y la oración guardada en uno de reescritura ([F-SMODE-R003](#F-SMODE-R003));
> 3. nunca aparece el formato interno del curso;
> 4. el mismo ejercicio da siempre el mismo texto, byte a byte, sin depender de la corrida ni de la hora.

<details><summary>Detalle y casos del curso</summary>

- «You ___ (see) cats.» (`67fab6d59930102295341ec3`, A1, *Afirmativas regulares*): pista «(see)», respuesta «see»,
  terminado «You see cats.», traducción «Ves gatos.». Nunca «You ____ [see] (see) cats.».
- «The show started ___ time.» (`67fab6d59930102295343d28`, B1, *In time u on time*), opciones *in* y *on*: la vista
  marca *on* como correcta y el terminado es «The show started on time.» ([F-OPMUL-R004](#F-OPMUL-R004)).

La invariante 4 importa por plata: cuatro jueces calculan su huella sobre esta vista, así que cambiar el texto después de
pagar vuelve a cobrar hasta 34.274 consultas ([F-EVCOST-R002](#F-EVCOST-R002)). La pista entre paréntesis y la primera
respuesta son las de [F-QSENT-R007](#F-QSENT-R007) y [F-QSENT-R018](#F-QSENT-R018).

</details>

<a id="F-RONDA1-R002"></a>
### Rule[F-RONDA1-R002] - En el resumen, cada ejercicio se llama por su enunciado
**Severity**: major | **Validation**: AUTO_VALIDATED

> En el resumen del informe, la etiqueta de un ejercicio es su enunciado como lo ve el alumno
> ([F-RONDA1-R001](#F-RONDA1-R001)); la de un tema, un topic, un nivel y el curso no cambia. El informe completo y el
> contenido que juzga el juez de consigna no cambian.

Hoy los 20 ejercicios de *Afirmativas regulares* se llaman «Afirmativas regulares»; con esta regla el primero se llama
«You ___ (see) cats.». El nombre del ejercicio en el informe completo sigue siendo el de hoy porque el juez de consigna lo
lee como título: cambiarlo volvería a cobrar sus veredictos.

<a id="F-RONDA1-R003"></a>
### Rule[F-RONDA1-R003] - `get student-view <tema>` devuelve el tema entero como lo ve el alumno
**Severity**: major | **Validation**: AUTO_VALIDATED

> `get student-view <id del tema>` imprime en JSON la consigna del tema y, por cada ejercicio en el orden del curso, su
> vista ([F-RONDA1-R001](#F-RONDA1-R001)), su ejercicio terminado, su clave de audio y el texto con que se grabó ese
> audio. Invariantes:
> 1. lee el análisis elegido con `set-active-analysis` y, si no hay ninguno, el último;
> 2. si el registro de grabaciones no conoce la clave, el texto del audio sale vacío: nunca se arma desde la oración;
> 3. un tema que no está en el análisis sale con «Knowledge <id> not found in the analysis.» y código 1;
> 4. no escribe nada.

Ejemplo: `get student-view 6814dafa7d73e7209a13d389` (*Afirmativas regulares*, A1) da «Escribe la forma correcta del
verbo.» y 20 ejercicios; el primero con «You ___ (see) cats.», la clave A1.01.06.01, la traducción y la miniteoría «Con
he, she, it el verbo lleva $-s$…». El texto del audio sale del registro que suma [F-DATOS-R003](#F-DATOS-R003).

<a id="F-RONDA1-R004"></a>
### Rule[F-RONDA1-R004] - Cada ficha trae su nombre en castellano
**Severity**: major | **Validation**: VALIDATED

> Toda ficha del catálogo trae, además de su nombre único, un nombre en castellano, y `get analyzers` lo muestra.
> Invariantes:
> 1. nunca está vacío y dos fichas no lo comparten;
> 2. es el de la tabla de abajo;
> 3. no reemplaza al nombre único, que sigue siendo el único con que se nombra al analizador
>    ([F-HALL-R005](#F-HALL-R005)).

<details><summary>Los diecisiete de la ronda 1, en el orden del catálogo</summary>

| # | Nombre único | Nombre en castellano |
|---:|---|---|
| 1 | `sentence-length` | Largo de oración |
| 2 | `knowledge-title-length` | Largo del título |
| 3 | `knowledge-instructions-length` | Largo de la consigna |
| 4 | `coca-buckets-distribution` | Bandas de frecuencia |
| 5 | `lemma-recurrence` | Recurrencia |
| 6 | `lemma-absence` | Palabras que faltan |
| 7 | `off-level-words` | Palabras de otro nivel |
| 8 | `lemma-count` | Palabras poco vistas |
| 9 | `quiz-instruction` | Consigna |
| 10 | `hint-coherence` | Pista coherente |
| 11 | `answer-completeness` | Respuestas completas |
| 12 | `multiple-choice-validity` | Opción múltiple válida |
| 13 | `data-coherence` | Datos coherentes |
| 14 | `exact-duplicates` | Duplicados exactos |
| 15 | `translation-sense` | Traducción y sentido |
| 16 | `single-answer` | Una sola respuesta |
| 17 | `mini-theory` | Miniteoría |

Los nombres son los de la tabla de la propuesta que vio José, y los de los analizadores nuevos, los de sus paquetes.
`off-level-words` va detrás de `lemma-absence` ([F-NIVFR-R007](#F-NIVFR-R007)); los cinco de reglas y los tres jueces
van después de los ocho de hoy, en ese orden.

</details>

<a id="F-RONDA1-R005"></a>
### Rule[F-RONDA1-R005] - Los números de cada contexto cuentan por gravedad y por regla, en las dos familias
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En cada tema, topic, nivel y en el curso, el informe publica, por cada analizador que corrió y por cada tipo de nodo
> que evalúa: cuántos alcanza, cuántos evaluó, cuántos tienen algún hallazgo suyo y cuántos hallazgos son; de los que
> tienen hallazgo, cuántos hay por su gravedad más alta; y por cada regla, cuántos nodos y cuántos hallazgos. Invariantes:
> 1. vale también para los analizadores de vocabulario;
> 2. las cuatro gravedades suman los nodos con hallazgo, y los hallazgos de las reglas suman los del analizador;
> 3. la cuenta de un contexto es la suma de sus hijos, más el propio nodo si el analizador lo evalúa;
> 4. `sentence-length` marca con una regla las oraciones más cortas que su meta y con otra las más largas.

Con la invariante 4, el nivel dice cuántas oraciones quedan de cada lado de la meta: «He isn't in the living-room.», 9
tokens contra 3 a 8 en A1, cuenta como más larga. Es lo que la UI necesita para dibujar «más cortas · en la meta · más
largas» y el color de cada etiqueta del nivel sin calcular nada ([F-HALL-R010](#F-HALL-R010)).

<a id="F-RONDA1-R006"></a>
### Rule[F-RONDA1-R006] - Un ejercicio evaluado a medias cuenta como evaluado, y lo dice
**Severity**: critical | **Validation**: VALIDATED

> Si un analizador evaluó un ejercicio con algunas de sus reglas y no con todas, como la opción múltiple con su juez sin
> correr, el ejercicio:
> 1. cuenta como evaluado por ese analizador, y sus hallazgos cuentan;
> 2. declara a ese analizador como «a medias», nunca a la vez como «sin evaluar»;
> 3. entra en el conteo «a medias» del analizador y en el de ejercicios no evaluados del todo, así «algún error» se lee
>    como piso ([F-HALL-R008](#F-HALL-R008)).

Ejemplo: «___ a child? (He)» (`…1e87`, opción múltiple) tiene su hallazgo de mayúscula y el juez no lo vio: suma un error,
y el nivel avisa que al juez le falta. Es la opción A de [DOUBT-OMVAL-JUEZ-A-MEDIAS](#DOUBT-OMVAL-JUEZ-A-MEDIAS), una de
las once dudas chicas que José dejó con su recomendación.

<a id="F-RONDA1-R007"></a>
### Rule[F-RONDA1-R007] - La corrida base corre las reglas instantáneas de un analizador con juez, sin pagar
**Severity**: major | **Validation**: VALIDATED

> La corrida base, la que usan la vista consolidada y la vista previa de impacto, corre además de los analizadores
> instantáneos a los que tienen alguna regla instantánea en su ficha, con tope 0 para lo que consulta un modelo: sus
> reglas corren siempre y del juez sólo cuenta lo que ya está registrado. Hoy es sólo `multiple-choice-validity`;
> `quiz-instruction` y los tres jueces nuevos no tienen reglas instantáneas y quedan afuera.

Así las dos vistas muestran los unos 354 errores de regla de la opción múltiple, que no cuestan nada. Es la opción A de
[DOUBT-OMVAL-CORRIDA-BASE](#DOUBT-OMVAL-CORRIDA-BASE), la misma lectura que José eligió para `stats analyzer` de un juez.

<a id="F-RONDA1-R008"></a>
### Rule[F-RONDA1-R008] - Integrar la ronda no mueve ningún número, salvo el de las bandas
**Severity**: critical | **Validation**: VALIDATED

> Con los diecisiete analizadores en el catálogo, `analyze` sobre la base del 29/9 da:
> 1. vocabulario 73,86 % y niveles 96,0 · 93,6 · 78,9 · 63,9 con las bandas 8 · 6 · 4 · 3, que son el valor por
>    omisión desde el 30/9; con 4 para todas, los de hoy: 73,92 % y 96,5 · 94,2 · 79,5 · 63,5;
> 2. el plan, las mismas 4.036 tareas: ningún analizador nuevo tiene tipo de tarea en esta ronda, así que el plan declara
>    los suyos como sin convertir, salvo `off-level-words`, cubierto por la tarea de ausencia de lemas del ejercicio
>    ([F-NIVFR-R006](#F-NIVFR-R006));
> 3. el curso queda idéntico byte a byte ([F-HALL-R015](#F-HALL-R015)).

José confirmó las bandas 8 · 6 · 4 · 3 el 30/9 a la noche ([F-POCOV-R001](#F-POCOV-R001)). Qué tarea crea cada analizador
nuevo es del paquete E, en la ronda 2.

## Contexto

La ronda 1 diseñó en paralelo tres paquetes: A, reglas del ejercicio (cinco analizadores); C, jueces con modelo (cuatro
nuevos y el de consigna); y D, vocabulario (cuatro). Cada uno validó su parche contra una base distinta y dejó
«ganchos»: 17 de A, 27 de C y 15 de D. El contrato del hallazgo (FEAT-HALL) previó que cada analizador nuevo deja cuatro
enganches en lo compartido —su bloque, el lugar de su diagnóstico tipado, una línea en el registro y, si produce tareas,
su tipo— y que el integrador los junta en un parche por ronda. Esta es esa pieza, con dos agregados: las dos dudas de A
que José dejó con su recomendación (R006, R007) y los pedidos 1 a 3 de la UI (R001 a R005).

Lo que se decidió al integrar, sin cambiar ninguna regla de los paquetes:

- **El juez de opción múltiple tiene un solo contrato, en dos tiempos.** A esperaba pedirle un veredicto por ejercicio;
  C lo diseñó para pedir todos durante el recorrido y resolverlos juntos al final. Queda el de C: es lo único que deja
  hacer las 4 consultas a la vez que eligió José ([F-JEXAM-R006](#F-JEXAM-R006)), y su juicio trae la gravedad de cada
  opción y la clave invertida, que piden [F-MCVALE-R002](#F-MCVALE-R002) y [F-MCVALE-R003](#F-MCVALE-R003). El puntaje
  del ejercicio es el de A ([F-OMVAL-R005](#F-OMVAL-R005)); el del juez queda como dato.
- **Todos los analizadores nuevos se registran igual**, como proveedores del catálogo ([F-HALL-R005](#F-HALL-R005)).
- **La comparación del corrector recibe el texto que rodea al hueco**, como el corrector de Go, para leer «'s» y «'d»
  por lo que sigue ([F-RESPC-R001](#F-RESPC-R001)).
- **Ningún tipo de tarea nuevo** en esta ronda (R008).

## Alcance

- **Adentro**: R001 a R008 y los enganches comunes de A, C y D: el lugar de cada diagnóstico tipado, la cobertura común
  de los jueces en el curso, las opciones de `analyze` por juez y de consultas a la vez, la configuración de los jueces
  desde el entorno y el registro de todos en el catálogo.
- **Afuera**: el tipo de tarea de cada analizador nuevo (paquete E); el pedido 4 de la UI, las medidas con meta, que
  espera; las definiciones de los agentes, la tabla del corrector y los casos de oro, que no son de Sentinel; y
  `medir.py`.

## Referencias

- **FEAT-HALL** — El hallazgo, la ficha, los números por contexto, el resumen y el registro único: R005, R006, R008 y
  R010 a R015.
- **FEAT-OMVAL** y **FEAT-MCVALE** — La opción múltiple y su juez: R006, R007 y el contrato del juez.
- **FEAT-JEXAM** — Las consultas a la vez y la cobertura común de los jueces.
- **FEAT-RESPC** — La comparación del corrector.
- **FEAT-DATOS** — El registro de grabaciones que lee R003.
- **FEAT-NIVFR** y **FEAT-POCOV** — El orden del catálogo, la tarea que cubre a `off-level-words` y las bandas: R004 y
  R008.
- **FEAT-SMODE**, **FEAT-QSENT**, **FEAT-OPMUL** y **FEAT-EVCOST** — Cómo se arma la oración de cada ejercicio y por qué
  su texto tiene que ser estable: R001.
