---
feature:
  id: FEAT-HALL
  code: F-HALL
  name: Contrato del hallazgo, catálogo de analizadores y números por contexto
  priority: critical
---

# Contrato del hallazgo: un solo formato para todos los analizadores

## TL;DR

**Qué**: Todo analizador informa lo que no pasa con un hallazgo de forma fija, se describe solo en `get analyzers`, y
content-audit publica en cada tema, nivel y curso los errores contados y el puntaje de vocabulario de hoy.

**Por qué**: Sin un contrato común, cada uno de los 11 analizadores nuevos pide pantallas y cuentas propias, el curso
da 73,9 % o 73,4 % según quién sume, y lo que decide el juez de consigna no llega a ningún tema, nivel ni curso.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy saber qué está mal en cada contexto:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)` → `AuditRunner.runAudit(Path, AuditRunRequest)`. ✓ Disponible.
2. Lee el número del curso y de cada nivel → `ReportViewModel.overallScore`, `MilestoneScoreRow.overallScore`. ✓ Disponible,
   pero ⚠️ da 73,9 % ahí y 73,4 % en el tablero, que promedia todas las claves de `AuditNode.scores`, cuartos COCA incluidos.
3. Ve qué está mal en un ejercicio → `AuditNode.diagnoses`. ⚠️ Cada analizador tiene su propio diagnóstico tipado, sin
   forma común: quien muestra tiene que conocer a cada uno. Y no hay cómo contar ejercicios con error: sólo promedios.
4. Lista los analizadores → `GetCommand.get("analyzers", …)` → `AnalyzerRegistry.listAnalyzers()`. ⚠️ Sólo los 7 clásicos:
   falta `quiz-instruction`, y `ConfigAnalyzerCommand.showConfig` y `StatsAnalyzerCommand.showStats` dicen «not found».
5. Mira al juez de consigna en un tema, un nivel o el curso → ⚠️ se calcula después de la suma y queda sólo en el
   ejercicio, donde además cambia el promedio: pasa a sumar una clave más.
6. Examina un analizador solo → `AnalyzeOptions.analyzers` → `AuditRunRequest.includedAnalyzers`. ⚠️ Según la lectura del
   código del brief 087 (hoy 089), la selección corre igual los 7 clásicos.

**Punto de fricción**: pasos 3 a 6. Sumar un analizador obliga a tocar pantallas y cuentas en quien muestra, no hay
dónde contar errores, y los jueces quedan fuera del catálogo y de los números.

**Cambio mínimo necesario**: el informe de `analyze` lleva hallazgos de forma común y, en cada nodo, los números de dos
familias ya calculados; `get analyzers` pasa a ser el catálogo completo, con una ficha por analizador; y la selección de
`analyze` corre exactamente lo pedido. Ningún puntaje de hoy cambia de cálculo.

## Reglas de Negocio

Grupos: **A**, el hallazgo (R001–R004); **B**, el catálogo (R005–R006); **C**, los números por contexto (R007–R011); **D**,
cómo se corre y qué se garantiza (R012–R015). Acá **tema** es lo que el curso llama *knowledge* (413 en la base del
29/9) y **topic** es la agrupación de temas, como *Present Simple*.

### Grupo A — El hallazgo

<a id="F-HALL-R001"></a>
### Rule[F-HALL-R001] - Todo nodo que no pasa deja hallazgos, y todos tienen la misma forma
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En cada nodo que un analizador evalúa —ejercicio, tema, topic, nivel o curso— y que no pasa, el informe registra al
> menos un hallazgo suyo. Invariantes:
> 1. Todo hallazgo, de cualquier analizador, tiene los mismos campos, todos poblados: analizador, regla, nodo, gravedad,
>    evidencia, resolución, costo e identidad.
> 2. No hay hallazgo en un nodo que pasa, ni en uno que el analizador no evaluó: el contenedor que sólo recibe su
>    promedio, el ejercicio pendiente o fallido.
> 3. Los diagnósticos tipados de hoy se siguen emitiendo igual, y lo propio de cada analizador va en su diagnóstico
>    tipado, nunca como campo extra del hallazgo ni en un mapa genérico.

*No pasa*: puntaje menor que 1 ([F-RCLA-R001](#F-RCLA-R001)); en un juez, incumplimiento ([F-QINST-R002](#F-QINST-R002));
en uno que sólo ordena, estar entre los marcados. *Evalúa* los nodos donde calcula su propio puntaje, no los que lo
reciben por agregación. Mantiene [F-DLABS-R001](#F-DLABS-R001) a [F-DLABS-R003](#F-DLABS-R003), [F-DCOCA-R004](#F-DCOCA-R004),
[F-DCOCA-R005](#F-DCOCA-R005), [F-DSLEN-R003](#F-DSLEN-R003), [F-DSLEN-R004](#F-DSLEN-R004), [F-QINST-R003](#F-QINST-R003) y
[F-QINST-R004](#F-QINST-R004). **Criterio**: con 0,75 en `sentence-length` el ejercicio tiene un hallazgo con los ocho
campos; con 1, ninguno; y su tema no lo tiene aunque su promedio sea menor que 1.

<a id="F-HALL-R002"></a>
### Rule[F-HALL-R002] - Gravedad, resolución y costo toman valores cerrados
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo hallazgo lleva un valor, y sólo uno, de cada eje:
> 1. **Gravedad**: *bloqueante* (el alumno no puede acertar), *alta* (marca mal una respuesta correcta o enseña algo
>    falso), *media* (se entiende, pero está mal) o *baja* (se puede mejorar).
> 2. **Resolución**: *regla* (se arregla con una regla, sin modelo), *mesa* (uno propone y los jueces deciden) o
>    *sólo ordena* (no se arregla: dice qué mirar primero).
> 3. **Costo**: *instantáneo*, *modelo local* o *modelo pago*: el de la regla que produjo el hallazgo.

Van por hallazgo, no por analizador: la pista coherente es *bloqueante* si no se puede acertar y *alta* si sólo falta; la
opción múltiple da hallazgos *instantáneos* (reglas) y de *modelo pago* (su juez). Los ocho existentes: [DOUBT-GRAVEDAD-EXISTENTES](#DOUBT-GRAVEDAD-EXISTENTES).

<a id="F-HALL-R003"></a>
### Rule[F-HALL-R003] - La evidencia deja verificar el hallazgo sin volver a correr el analizador
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La evidencia de todo hallazgo nunca está vacía, y dice:
> 1. **qué miró**: las partes que leyó, con su texto como lo ve el alumno —el hueco, la pista, las opciones, la
>    [traducción](glossary:Traducción)—, nunca en el formato interno del curso; o la medida que tomó, junto a su meta;
> 2. **qué encontró**: una línea en castellano que nombra el problema con palabras del nodo;
> 3. **con qué otro nodo**, si involucra otro: el ejercicio duplicado, los que contradicen la miniteoría.

Ejemplos: «Who ___ (I / see)? → are you seeing: el sujeto I no es you»; «Largo: 7 tokens, A1 de 3 a 8». «You ____ [see]
(see) cats.» es formato interno. Un juez suma sus violaciones (mantiene [F-QINST-R003](#F-QINST-R003)); uno de
vocabulario, los datos de su diagnóstico tipado ([F-DSLEN-R001](#F-DSLEN-R001), [F-DLABS-R010](#F-DLABS-R010)).

<a id="F-HALL-R004"></a>
### Rule[F-HALL-R004] - Los errores llegan al plan sin cambiar la regla del plan
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El plan sigue decidiendo sus tareas por puntaje: hay tarea sólo donde el puntaje de un analizador es menor que 1
> (mantiene [F-RCLA-R001](#F-RCLA-R001)). En la familia de errores ([F-HALL-R007](#F-HALL-R007)):
> 1. un nodo con al menos un hallazgo que cuenta como error queda con puntaje menor que 1 en ese analizador, y uno sin
>    ninguno queda con 1;
> 2. un hallazgo que sólo ordena no baja el puntaje, así que no produce tarea;
> 3. el plan nunca descarta en silencio un puntaje menor que 1: produce su tarea, o declara de qué analizador y cuántos
>    nodos no pudo convertir.

La escala bajo 1 es de cada analizador (la del juez, [F-QINST-R002](#F-QINST-R002); su paso al plan, [F-QINST-R017](#F-QINST-R017)).
La invariante 3 cierra el hueco de cableado del brief 087: sin tipo de tarea, hoy las tareas se pierden sin error, como le
pasó al juez. Qué tipo de tarea crea cada analizador nuevo es del paquete E.

### Grupo B — El catálogo

<a id="F-HALL-R005"></a>
### Rule[F-HALL-R005] - `get analyzers` lista todos los analizadores, cada uno con un solo nombre
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un nombre figura en `get analyzers` si y sólo si `analyze` acepta ese analizador para correrlo, jueces con modelo
> incluidos (hoy falta `quiz-instruction`). Es el único nombre con que se lo nombra: en el informe, en cada hallazgo, al
> seleccionarlo o excluirlo en `analyze`, al fijarle tope, en `config analyzer` y en `stats analyzer`.

Extiende a todos [F-QINST-R015](#F-QINST-R015), que dejaba la invariante de plataforma para otro requirement. Mantiene
[F-CLIRV-R016](#F-CLIRV-R016) y [F-CLIRV-R021](#F-CLIRV-R021); `stats` sobre un juez: [DOUBT-STATS-DE-LOS-JUECES](#DOUBT-STATS-DE-LOS-JUECES).
**Criterio**: `quiz-instruction` aparece en `get analyzers`, y `config analyzer quiz-instruction` no dice «not found».

<a id="F-HALL-R006"></a>
### Rule[F-HALL-R006] - Cada analizador se describe solo, en su ficha del catálogo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada analizador del catálogo trae su ficha: nombre, qué pregunta, qué lee, sus reglas, su meta, su familia, qué nodos
> evalúa, cómo se resuelven sus hallazgos y su costo. Invariantes:
> 1. Ningún campo está vacío, y la ficha se lee con `get analyzers` y con `get analyzer <nombre>`.
> 2. Todo hallazgo nombra un analizador y una regla del catálogo, con una resolución que su ficha declara y un costo que
>    no supera el de la ficha, que es el mayor de sus reglas (instantáneo < modelo local < modelo pago).
> 3. Sumar un analizador suma su ficha y no cambia ninguna otra.

<details><summary>Las fichas de los ocho existentes (ilustración: el resto de cada ficha sale de su requirement)</summary>

| Nombre | Qué pregunta | Familia | Evalúa | Resolución | Costo |
|---|---|---|---|---|---|
| `sentence-length` | ¿La oración tiene el largo de su nivel? | vocabulario | ejercicio | mesa | instantáneo |
| `coca-buckets-distribution` | ¿Las palabras tienen la frecuencia que pide el nivel? | vocabulario | nivel y curso | sólo ordena | instantáneo |
| `lemma-absence` | ¿Están las palabras que el EVP espera, y ninguna de otro nivel? | vocabulario | ejercicio, nivel y curso | mesa en el ejercicio; sólo ordena en nivel y curso | instantáneo |
| `lemma-count` | ¿Cada palabra aparece en suficientes oraciones? | vocabulario | nivel y curso | sólo ordena | instantáneo |
| `lemma-recurrence` | ¿Las palabras vuelven a intervalos sanos? | vocabulario | curso | sólo ordena | instantáneo |
| `knowledge-title-length` | ¿El título del tema entra en el teléfono? | vocabulario | tema | mesa | instantáneo |
| `knowledge-instructions-length` | ¿La consigna entra en el teléfono? | vocabulario | tema | sólo ordena | instantáneo |
| `quiz-instruction` | ¿El ejercicio cumple su consigna? | errores | ejercicio | mesa | modelo pago |

La meta de `sentence-length` es la que aplica hoy (A1 de 3 a 8 tokens), aunque [F-SLEN-R012](#F-SLEN-R012) documenta 5 a
8: la diferencia queda anotada, no se resuelve acá. En vocabulario, «sólo ordena» dice que no hay corrección propia; sus
tareas siguen como hoy, porque [F-HALL-R004](#F-HALL-R004) rige sólo en la familia de errores.

</details>

### Grupo C — Los números por contexto

<a id="F-HALL-R007"></a>
### Rule[F-HALL-R007] - Dos familias, y cada analizador pertenece a una sola
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada analizador declara en su ficha una sola familia, y ninguno publica números de las dos:
> 1. **«¿El ejercicio está bien hecho?»** (errores): sus números cuentan ejercicios con error, y la meta es 0.
> 2. **«¿El vocabulario es el adecuado?»** (vocabulario): su número es su puntaje contra su meta, como hoy.
>
> Los siete clásicos son de vocabulario; `quiz-instruction` y los once nuevos, de errores.

Un error no se promedia: se cuenta y se arregla. Los once nuevos: pista coherente, respuestas completas, opción múltiple
válida, datos coherentes, duplicados, gramática, naturalidad, traducción y sentido, una sola respuesta, miniteoría y
datos de alumnos. Las palabras de otro nivel: [DOUBT-FAMILIA-OTRO-NIVEL](#DOUBT-FAMILIA-OTRO-NIVEL).

<a id="F-HALL-R008"></a>
### Rule[F-HALL-R008] - Los errores se cuentan en content-audit, por analizador y en total
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En cada tema, topic, nivel y en el curso, el informe publica ya calculados:
> 1. por analizador de errores: cuántos ejercicios alcanza, cuántos evaluó y cuántos quedaron sin evaluar, cuántos
>    tienen al menos un hallazgo que cuenta como error, y qué porcentaje son de los evaluados;
> 2. **algún error**: cuántos ejercicios del nodo tienen al menos un error, qué porcentaje son de todos, y cuántos tienen
>    como gravedad más alta cada una de las cuatro;
> 3. cuántos ejercicios no evaluó al menos uno de esos analizadores, para leer «algún error» como piso.
>
> Un ejercicio cuenta una vez, tenga los hallazgos que tenga. Todo hallazgo de errores cuenta como error salvo los que
> sólo ordenan, que se publican aparte, como marcados.

Cierran tres sumas: evaluados más sin evaluar dan los alcanzados; las cuatro gravedades dan «algún error»; y la cuenta de
un nodo es la suma de sus hijos (los porcentajes se recalculan, nunca se promedian). Mantiene [F-QINST-R004](#F-QINST-R004)
y [F-QINST-R005](#F-QINST-R005). *Alcanza* los que mira: la opción múltiple válida, los 2.810 de opción múltiple (315
marcados, 11,2 %). Un hallazgo de tema cuenta en cada ejercicio del tema ([DOUBT-HALLAZGO-DE-TEMA](#DOUBT-HALLAZGO-DE-TEMA)).

<a id="F-HALL-R009"></a>
### Rule[F-HALL-R009] - El puntaje de vocabulario es el de hoy
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En cada nodo, el puntaje de vocabulario es el promedio simple de los puntajes que tienen ahí los analizadores de
> vocabulario, uno por analizador: no entran los de errores, en ningún nodo —tampoco en el ejercicio juzgado—, ni las
> sub-métricas. Sobre la base del 29/9 da exactamente: curso 73,9 %, A1 96,5 %, A2 94,2 %, B1 79,5 % y B2 63,5 %.

Cada puntaje por analizador se calcula como hoy: agregación genérica (mantiene [F-SLEN-R003](#F-SLEN-R003) a
[F-SLEN-R005](#F-SLEN-R005), [F-SLEN-R008](#F-SLEN-R008) y [F-SLEN-R016](#F-SLEN-R016)) o propia ([F-COCA-R029](#F-COCA-R029),
[F-LCOUNT-R013](#F-LCOUNT-R013)). El curso promedia los puntajes de curso de cada analizador, no los niveles:
(86,4 + 72,5 + 89,3 + 67,0 + 8,0 + 98,8 + 95,5) / 7 = 73,9 (brief 020). Base: análisis 2026-09-30T11-54-02, sin el juez
(brief 022).

<a id="F-HALL-R010"></a>
### Rule[F-HALL-R010] - Un solo número por nodo, publicado por content-audit
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El informe publica en cada nodo su puntaje de vocabulario y sus números de errores, y todo verbo que muestre un número
> de ese nodo —`analyze`, `get audit`, `stats analyzer`, la vista consolidada y la vista previa de impacto— muestra el
> publicado, sin recalcularlo. Las sub-métricas, como los cuatro cuartos de las bandas COCA, se publican marcadas como
> parte de su analizador, y ningún número publicado las promedia como si fueran un analizador.

El número del curso es 73,9 %. El 73,4 % sale de promediar 11 claves (7 analizadores y 4 cuartos): las bandas pesan 5 de
11. Quien dibuja lo lee; no lo arma. Mantiene [F-CDIFF-R007](#F-CDIFF-R007) y [F-PIPRE-R004](#F-PIPRE-R004).

<a id="F-HALL-R011"></a>
### Rule[F-HALL-R011] - Los jueces llegan a los números del tema, el nivel y el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Lo que decide un juez con modelo llega, en la misma corrida, a cada nodo que contiene a los ejercicios que juzgó:
> 1. su puntaje sube por la agregación genérica contando sólo los ejercicios con veredicto; un nodo sin ninguno queda
>    sin puntaje;
> 2. sus incumplimientos cuentan como errores en [F-HALL-R008](#F-HALL-R008), con lo no juzgado declarado;
> 3. el puntaje de vocabulario de esos nodos es idéntico al de la misma corrida sin el juez.

Hoy el juez se calcula después de la suma y queda en el ejercicio, aunque [F-QINST-R004](#F-QINST-R004) pide que el tema
promedie a sus evaluados. Mantiene [F-SLEN-R003](#F-SLEN-R003) a [F-SLEN-R008](#F-SLEN-R008) y [F-QINST-R005](#F-QINST-R005);
un veredicto reutilizado cuenta igual que uno nuevo ([F-EVCOST-R003](#F-EVCOST-R003)). Vale para los jueces nuevos.

### Grupo D — Cómo se corre y qué se garantiza

<a id="F-HALL-R012"></a>
### Rule[F-HALL-R012] - Se puede correr un analizador solo
**Severity**: major | **Validation**: AUTO_VALIDATED

> Pedirle a `analyze` uno o más analizadores por su nombre corre exactamente esos, sean clásicos, jueces o nuevos: el
> informe trae sus hallazgos, sus números y su cobertura, y nada de otros. Un nombre que no figura en `get analyzers`,
> pedido para correr o para excluir, rechaza la corrida antes de empezar, en lugar de ignorarse.

Lo necesita el examen del paquete 0.4. Mantiene [F-QINST-R006](#F-QINST-R006), [F-QINST-R011](#F-QINST-R011) y
[F-QINST-R015](#F-QINST-R015): tope y exclusión con el mismo nombre. El rechazo es la mejora que F-QINST-R015 dejaba para
toda la auditoría. Con un subconjunto, el puntaje de vocabulario promedia los que corrieron ([F-HALL-R009](#F-HALL-R009)).

**Error**: "Analyzer '<name>' not found. Run 'content-audit get analyzers' to see available analyzers." (el de [F-CLIRV-R016](#F-CLIRV-R016))

<a id="F-HALL-R013"></a>
### Rule[F-HALL-R013] - Lo que ya existe no cambia de número
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Con el mismo curso y los mismos veredictos registrados, los ocho analizadores existentes dan, antes y después de este
> contrato, los mismos puntajes por analizador en cada nodo, los mismos diagnósticos tipados, el mismo puntaje general de
> cada nodo —que pasa a llamarse de vocabulario—, salvo el del ejercicio que un juez juzgó, y el mismo plan. Sólo se
> agrega lo del contrato: hallazgos, catálogo, números de errores y el juez en los contenedores ([F-HALL-R011](#F-HALL-R011)).

Criterio de integración del brief 087. Base del 29/9: 73,9 % y 4.036 tareas (SENTENCE_LENGTH 3.051, LEMMA_ABSENCE 950,
KNOWLEDGE_INSTRUCTIONS_LENGTH 25, KNOWLEDGE_TITLE_LENGTH 6, COCA_BUCKETS 3, LEMMA_RECURRENCE 1). La excepción del
ejercicio juzgado sólo aparece en corridas con el juez: [DOUBT-EJERCICIO-JUZGADO](#DOUBT-EJERCICIO-JUZGADO).

<a id="F-HALL-R014"></a>
### Rule[F-HALL-R014] - La salida es determinista y cada hallazgo tiene una identidad estable
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Con el mismo curso y los mismos veredictos registrados, dos corridas dan los mismos hallazgos, en el mismo orden y con
> los mismos números. La identidad de un hallazgo es su analizador, su regla, su nodo y, si la regla marca más de una
> cosa en el nodo, qué marca (por ejemplo, la palabra). Invariantes:
> 1. no depende de la corrida, del orden del recorrido, de la hora ni de la versión del juez;
> 2. en un nodo no hay dos hallazgos con la misma identidad;
> 3. un hallazgo que sigue presente después de un cambio en otro nodo conserva su identidad.

Lo exige la vista consolidada, que descubre las hojas sola ([F-CDIFF-R019](#F-CDIFF-R019)), ignora horas e identificadores
opacos ([F-CDIFF-R020](#F-CDIFF-R020)), compara listas por identidad declarada ([F-CDIFF-R022](#F-CDIFF-R022)) y usa caminos
estables ([F-CDIFF-R023](#F-CDIFF-R023)). No es la huella del libro de evaluaciones ([F-EVCOST-R001](#F-EVCOST-R001)), que
identifica un veredicto por su contenido: ésta identifica un problema en un nodo, para compararlo entre fotos.

<a id="F-HALL-R015"></a>
### Rule[F-HALL-R015] - Analizar no escribe nada sobre el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Correr `analyze`, con cualquier selección de analizadores, deja los archivos del curso idénticos byte a byte. Una
> corrida sólo escribe su informe y, si corrió un juez, sus veredictos nuevos en el libro de evaluaciones
> ([F-EVCOST-R004](#F-EVCOST-R004)). Un hallazgo que se resuelve con una regla no se aplica al analizar.

## Contexto

El brief 087 suma once analizadores a los ocho que content-audit ya tiene y los reparte en paquetes que se diseñan en
paralelo. Antes, todos tienen que hablar igual y la interfaz de la propuesta «Corregir sin empeorar» v2 tiene que
dibujarlos sin conocerlos: es el paquete 0.3. Hoy, once analizadores nuevos serían once pantallas nuevas, y sólo hay
promedios: el tema de las verduras sale 20 de 20 en verde con cuatro errores que encontró la revisión del 077, uno
imposible de acertar. Un error no se promedia, se cuenta; el vocabulario conserva su puntaje, y el 73,9 % no se mueve.

Las decisiones que se mantienen están en References, y cada regla cita las que toca. Base de referencia: análisis
2026-09-30T11-54-02 del curso vivo del 29/9 (11.287 ejercicios, 2.810 de opción múltiple, 413 temas, sin el juez) y su
plan de 4.036 tareas (brief 022 de content-audit).

## Alcance

- **Adentro**: lo que fijan R001 a R015, para los ocho analizadores existentes y como contrato de los once nuevos.
- **Afuera**: los once analizadores y sus reglas (paquetes A, B, C, D y H); cómo un hallazgo se vuelve tarea más allá de
  [F-RCLA-R001](#F-RCLA-R001), con tipos de tarea, tandas y mesa (E); la línea base y el antes y después (F); el tablero
  (G); el examen de cada analizador (0.4); acotar una corrida a un conjunto de ejercicios, salvo la re-evaluación que ya
  existe ([F-QINST-R018](#F-QINST-R018)); cambiar reglas, rangos o metas de un analizador existente; declarar la opción
  múltiple en Sentinel (0.2); y el formato en disco de hallazgos y fichas.
- **Afuera por ahora** (José, 30/9; duda 3 de la ficha, del arquitecto): el «cajón tipado», un lugar común donde
  cualquier analizador deje su diagnóstico sin tocar lo de otros. Cada analizador conserva su lugar propio para su
  diagnóstico tipado ([F-HALL-R001](#F-HALL-R001)), y el integrador suma los enganches de los nuevos en un parche
  por ronda.

## User Journeys

### Journey[F-HALL-J001] - Analizar el curso y leer qué está mal en cada contexto
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-HALL-J001
    name: Analizar el curso y leer qué está mal en cada contexto
    flow:
      - id: correr_analisis
        action: "El operador corre analyze sobre el curso completo"
        then: leer_ejercicio
      - id: leer_ejercicio
        action: "El informe trae, para un ejercicio, los hallazgos de cada analizador que lo evaluó, todos con la misma forma y con su evidencia"
        gate: [F-HALL-R001, F-HALL-R002, F-HALL-R003]
        outcomes:
          - when: "El ejercicio tiene un hallazgo de la familia de errores que se resuelve con una regla o con la mesa"
            then: cuenta_como_error
          - when: "El ejercicio sólo tiene hallazgos de vocabulario o hallazgos que sólo ordenan"
            then: no_cuenta_como_error
      - id: cuenta_como_error
        action: "El ejercicio queda con puntaje menor que 1 en ese analizador y suma una sola vez en «algún error» de su tema, su topic, su nivel y el curso"
        gate: [F-HALL-R004, F-HALL-R008]
        then: leer_vocabulario
      - id: no_cuenta_como_error
        action: "El ejercicio no suma en «algún error», y los hallazgos que sólo ordenan figuran como marcados"
        gate: [F-HALL-R004, F-HALL-R008]
        then: leer_vocabulario
      - id: leer_vocabulario
        action: "El informe publica en cada nodo el puntaje de vocabulario, el mismo que muestran get audit y la vista consolidada"
        gate: [F-HALL-R009, F-HALL-R010]
        outcomes:
          - when: "La corrida incluyó al juez de consigna"
            then: juez_en_los_numeros
          - when: "La corrida excluyó al juez de consigna"
            then: numeros_de_hoy
      - id: juez_en_los_numeros
        action: "Los incumplimientos juzgados cuentan en los errores de su tema, topic, nivel y curso, con lo no juzgado declarado, y el puntaje de vocabulario es el de la corrida sin el juez"
        gate: [F-HALL-R011]
        result: success
      - id: numeros_de_hoy
        action: "El puntaje de vocabulario de cada nodo y las tareas del plan son idénticos a los que el mismo curso daba antes del contrato"
        gate: [F-HALL-R013]
        result: success
```

### Journey[F-HALL-J002] - Examinar un analizador solo
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-HALL-J002
    name: Examinar un analizador solo
    flow:
      - id: leer_catalogo
        action: "El operador pide get analyzers y obtiene la ficha completa de cada analizador, jueces incluidos"
        gate: [F-HALL-R005, F-HALL-R006]
        then: pedir_uno
      - id: pedir_uno
        action: "El operador corre analyze pidiendo un solo analizador por su nombre"
        outcomes:
          - when: "El nombre figura en el catálogo"
            then: corre_solo
          - when: "El nombre no figura en el catálogo"
            then: rechazo
      - id: corre_solo
        action: "El informe trae sólo los hallazgos, los números y la cobertura de ese analizador, y los archivos del curso quedan idénticos"
        gate: [F-HALL-R012, F-HALL-R015]
        then: repetir
      - id: repetir
        action: "Una segunda corrida igual produce los mismos hallazgos, con las mismas identidades y en el mismo orden"
        gate: [F-HALL-R014]
        result: success
      - id: rechazo
        action: "La corrida se rechaza antes de empezar, con el mensaje que remite a get analyzers, y no se escribe ningún informe"
        gate: [F-HALL-R012]
        result: failure
```

## Open Questions

<a id="DOUBT-HALLAZGO-DE-TEMA"></a>
### Doubt[DOUBT-HALLAZGO-DE-TEMA] - ¿Cómo cuenta en los números un hallazgo sobre un tema o un nivel?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): cuenta en cada ejercicio del nodo, para su analizador y para «algún error».
- [ ] Opción B: se cuenta aparte, como temas o niveles con hallazgo, sin sumar ejercicios.
- [ ] Opción C: las dos cosas.

**Answer**: **Opción A** (José, 30/9): un hallazgo sobre el tema, como la miniteoría, cuenta en cada ejercicio del tema,
para su analizador y para «algún error»; [F-HALL-R008](#F-HALL-R008) lo fija así. La miniteoría es una por tema y la ven
todos sus ejercicios; así contó el 077 (335 ejercicios, «40 de 40» en *In time u on time*).

<a id="DOUBT-GRAVEDAD-EXISTENTES"></a>
### Doubt[DOUBT-GRAVEDAD-EXISTENTES] - ¿Qué gravedad llevan los hallazgos de los ocho analizadores existentes?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): el juez, según su severidad (crítica, bloqueante; mayor, alta; menor, media); la palabra de
  otro nivel o fuera del catálogo, media; el resto del vocabulario, baja.
- [ ] Opción B: todo el vocabulario baja, y todo incumplimiento del juez alta.
- [ ] Opción C: que la fije el paquete que toque cada analizador.

**Answer**: **Opción A** (José, 30/9): en el juez, crítica → bloqueante, mayor → alta y menor → media; la palabra de otro
nivel o fuera del catálogo, media; el resto del vocabulario, baja. Sigue las definiciones de [F-HALL-R002](#F-HALL-R002)
y el ejemplo de la propuesta («sport» como verbo, media), y no mueve ningún número de hoy. Si «crítica» incluye
ejercicios que igual se aciertan, infla «no se pueden acertar»: lo mide el examen del paquete 0.4.

<a id="DOUBT-FAMILIA-OTRO-NIVEL"></a>
### Doubt[DOUBT-FAMILIA-OTRO-NIVEL] - ¿Las palabras de otro nivel, parte de `lemma-absence`, son un error o vocabulario?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): `lemma-absence` queda entero en vocabulario; si la palabra de otro nivel pasa a contar como
  error, la cuenta un analizador de errores con su propia ficha (el de nivel con frases del EVP, paquete D).
- [ ] Opción B: los hallazgos de ejercicio de `lemma-absence` cuentan como error y su puntaje sigue en vocabulario.
- [ ] Opción C (descartada): `lemma-absence` pasa entero a errores y mueve el 73,9 %.

**Answer**: **Opción A** (José, 30/9): `lemma-absence` queda entero en vocabulario. Si hace falta contar las palabras de
otro nivel como error, lo hace un analizador nuevo del paquete D, con su propia ficha. La propuesta las pone entre los
errores, pero `lemma-absence` entra en el 73,9 % y va una familia por analizador ([F-HALL-R007](#F-HALL-R007)); además
hoy marca 337 ejercicios de A1 contra unos 17 del 077, porque no lee frases como «a lot», y llenaría «algún error» de
ruido.

<a id="DOUBT-EJERCICIO-JUZGADO"></a>
### Doubt[DOUBT-EJERCICIO-JUZGADO] - ¿El ejercicio juzgado deja de promediar al juez, como pide F-HALL-R009?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): sí; el juez se ve aparte, como error, y el ejercicio conserva sólo su puntaje de vocabulario.
- [ ] Opción B: el ejercicio conserva además su promedio de hoy con el juez, como un segundo número.

**Answer**: **Opción A** (José, 30/9): el veredicto del juez va aparte, como error, y el promedio de vocabulario del
ejercicio no lo incluye, como fijan [F-HALL-R009](#F-HALL-R009) y [F-HALL-R013](#F-HALL-R013). Hoy el promedio del
ejercicio juzgado suma una clave más, así que es un número que cambia a propósito; B volvía a dar dos números para lo
mismo, que es lo que [F-HALL-R010](#F-HALL-R010) quita, y en la base del 29/9 ningún ejercicio está juzgado: no cambia
ningún número publicado.

<a id="DOUBT-STATS-DE-LOS-JUECES"></a>
### Doubt[DOUBT-STATS-DE-LOS-JUECES] - ¿Qué hace `stats analyzer` con un analizador que consulta un modelo?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): usa sólo los veredictos ya registrados, sin consultas nuevas, y declara cuántos faltan.
- [ ] Opción B: consulta con el tope por defecto de una corrida (hoy 500).
- [ ] Opción C: rechaza el pedido y remite a `analyze`.

**Answer**: **Opción A** (José, 30/9): `stats analyzer` sobre un juez usa sólo los veredictos ya registrados, con cero
consultas nuevas, y declara cuántos faltan. Es tope 0 con reuso libre ([F-QINST-R006](#F-QINST-R006),
[F-EVCOST-R003](#F-EVCOST-R003)). B pagaba sin que nadie lo pidiera, como advirtió el arquitecto de FEAT-QINST el 3/8; C
dejaba sin estadísticas a un analizador del catálogo.

## References

- **FEAT-DLABS, FEAT-DCOCA, FEAT-DSLEN** — Diagnósticos tipados, nunca un mapa genérico: R001, R003.
- **FEAT-SLEN, FEAT-COCA, FEAT-LCOUNT** — Agregación genérica y agregaciones propias: R006, R009, R011.
- **FEAT-QINST** — Juez de consigna: escala, diagnóstico, cobertura, nombre único, tope, exclusión y tareas: R001, R004,
  R005, R008, R011, R012.
- **FEAT-EVCOST, FEAT-CDIFF, FEAT-PIPRE** — Libro de evaluaciones y vistas que leen del motor: R010, R011, R014, R015.
- **FEAT-RCLA, FEAT-CLIRV** — Tarea sólo con puntaje menor que 1; nombre canónico de `get analyzers`: R004, R005, R012.
