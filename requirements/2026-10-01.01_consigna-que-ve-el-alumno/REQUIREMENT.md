---
feature:
  id: FEAT-CALUM
  code: F-CALUM
  name: El juez de consigna lee el ejercicio como lo ve el alumno
  priority: high
---

# El juez de consigna lee el ejercicio como lo ve el alumno

## TL;DR

**Qué**: El juez de consigna recibe el ejercicio armado con la vista del alumno ([F-RONDA1-R001](#F-RONDA1-R001)), como
los otros cuatro jueces:
- el título y la consigna que muestra la app, que son los de la copia del ejercicio y no los del tema;
- el enunciado con «___» y la pista;
- las respuestas aceptadas de cada hueco;
- en la opción múltiple, sus opciones con la correcta marcada.

El nivel y el topic siguen como contexto. Vale igual en `revise-instructions`, para el original y para el candidato.

**Por qué**: La app muestra la consigna de la copia y el juez leía la del tema. En la base del 29/9 difieren en 9.923 de
11.287 ejercicios, así que el juez marcaba ejercicios por una consigna que el alumno nunca ve. Es la regla de José para
todo juez: lee el ejercicio entero como lo ve el alumno (gancho C-07).

## Recorrido de uso (estado actual)

Cómo quien revisa el curso usa hoy al juez de consigna:

1. Hace rendir al juez el examen fijo (`exam run quiz-instruction`) → el juez lee la etiqueta y la consigna del tema.
   ⚠️ En el examen del 1/10, sus dos falsas alarmas sobre ejercicios buenos se explican porque la consigna del tema pide
   otra cosa que la de la copia.
2. Corre `analyze` con tope para el juez → ⚠️ lo mismo sobre el curso: el veredicto habla de una consigna que el alumno
   no ve.
3. Corre `revise-instructions` → ⚠️ el original y cada candidato se verifican contra la consigna del tema.
4. Lee el hallazgo de un ejercicio que incumple → la ficha ya dice que el juez mira «el ejercicio como lo ve el alumno»
   ([F-HALL-R006](#F-HALL-R006)). ⚠️ Hoy no es cierto.

**Punto de fricción**: pasos 1 a 4.

**Cambio mínimo necesario**:
- el constructor del sujeto del juez de consigna recibe el renderizador del alumno, como los de los otros cuatro;
- los dos lugares de `revise-instructions` que arman la vista del ejercicio le pasan el título y la consigna de su copia.

## Reglas de Negocio

Acá **tema** es lo que el curso llama *knowledge* y **copia** es el ejercicio guardado con su propio título y su propia
consigna, que es lo que muestra la app.

<a id="F-CALUM-R001"></a>
### Rule[F-CALUM-R001] - El juez de consigna lee el ejercicio como lo ve el alumno
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Lo que recibe el juez de consigna sale de la vista del alumno y es, en este orden y nada más:
> - el nivel, del hito;
> - el topic, del tema;
> - el título y la consigna de la copia del ejercicio, los que muestra la app;
> - el enunciado, con «___» en cada hueco y su pista como aparece;
> - una línea por hueco con sus respuestas aceptadas, partidas sólo en «|»;
> - en la opción múltiple, sus opciones en el orden del alumno, con la correcta marcada.
>
> Invariantes:
> 1. nunca la etiqueta ni la consigna del tema, nunca la [traducción](glossary:Traducción) ni la miniteoría (las
>    juzgan [FEAT-TRSEN](#F-TRSEN-R001) y [FEAT-MINIT](#F-MINIT-R001)) y nunca el formato interno del curso;
> 2. un dato ausente va vacío, y un ejercicio sin consigna se juzga igual ([F-QINST-R014](#F-QINST-R014));
> 3. la identidad del veredicto sigue siendo el contenido juzgado ([F-QINST-R009](#F-QINST-R009)): el identificador no
>    entra, y el mismo curso da el mismo contenido byte a byte;
> 4. cambiar la consigna de la copia cambia el contenido; cambiar sólo la del tema, no.

Reemplaza la fuente de la etiqueta y la consigna en [F-QINST-R009](#F-QINST-R009), que era el tema, y deja sin efecto
[F-JEXAM-R007](#F-JEXAM-R007) («lo que lee el juez de consigna no cambia»). Contesta
[DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO](#DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO) con la opción B.

<details><summary>Detalle y casos del curso</summary>

- «She is a ___ (quiet / quietly) girl.» (`67fab6d59930102295342ed4`, A1, *Adjetivo o adverbio*): la app muestra
  «Elegí la palabra correcta.» y el tema dice «Elige entre el adjetivo o el adverbio.». El juez lee la primera, el
  enunciado con su pista y «Hueco 1: quiet».
- **El costo.** Cambia la huella de todo ejercicio, así que ningún veredicto pagado se reusa: en la base del 29/9
  conservaban veredicto unos 3.900 CLOZE ([F-JEXAM-R007](#F-JEXAM-R007)). Igual no se paga nada sin tope pedido
  ([F-JEXAM-R010](#F-JEXAM-R010)), y la versión nueva rinde el examen fijo antes de consultar sobre el curso
  ([F-JEXAM-R003](#F-JEXAM-R003)).

</details>

<a id="F-CALUM-R002"></a>
### Rule[F-CALUM-R002] - `revise-instructions` juzga el original y el candidato como los verá el alumno
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Al revalidar el ejercicio original y al verificar un candidato, el juez de consigna recibe cada uno con la vista del
> alumno y con el título y la consigna de su propia copia, igual que en `analyze`. Invariante: el mismo ejercicio da el
> mismo contenido por los dos caminos ([F-QICOR-R005](#F-QICOR-R005)).

El corrector que arma los candidatos sigue recibiendo la consigna del tema: alinearlo queda fuera de este requirement
([DOUBT-CALUM-CORRECTOR](#DOUBT-CALUM-CORRECTOR)).

## Dudas

<a id="DOUBT-CALUM-CORRECTOR"></a>
### Doubt[DOUBT-CALUM-CORRECTOR] - ¿El corrector de `revise-instructions` también apunta a la consigna de la copia?
El corrector recibe la consigna del tema para armar el candidato, y el juez lo verifica contra la de su copia. Cuando las
dos difieren, un candidato bien armado para el tema puede no pasar.

- [ ] Opción A (recomendada): igualar el dato, la opción A de
  [DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO](#DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO), que ya está decidida. Con las dos consignas
  iguales, la duda desaparece sin tocar el corrector.
- [ ] Opción B: que el corrector reciba la consigna de la copia, con otro requirement de FEAT-QICOR.
- [ ] Opción C: dejarlo así: `revise-instructions` no corre en la ronda 1.

**Status**: OPEN

## Contexto

- **El 30/9** José eligió igualar la consigna de cada tema a la publicada (opción A), con un arreglo de datos que no se
  aplicó.
- **El 1/10** el coordinador decidió que el juez lea la consigna que ve el alumno. No es retocar al juez mirando el
  examen: es un error de diseño contra la regla de José.
- **La definición del juez cambia fuera de Sentinel**, en su carpeta de `.sentinel/agents/`:
  - la guía describe la entrada nueva;
  - la guía dice el tope de cada texto: motivo 400, restricción 240, evidencia 300 y explicación 300.

  En el examen frenado del 1/10, la compuerta rechazó 15 de 25 veredictos por una explicación de más de 300 caracteres.
- **La versión del juez** es la huella de los archivos de su carpeta, del modelo y del proveedor, así que cambia con la
  guía.

## Alcance

- **Adentro**: R001 y R002.
- **Afuera**:
  - el corrector de `revise-instructions` ([DOUBT-CALUM-CORRECTOR](#DOUBT-CALUM-CORRECTOR));
  - volver a rendir el examen, que espera el OK de José;
  - el oro del examen, que es del coordinador.

## Referencias

- **FEAT-QINST** — La identidad por contenido y el ejercicio sin consigna: R009 y R014.
- **FEAT-JEXAM** — El freno del examen, el tope 0 por omisión y lo que leía el juez: R003, R007 y R010.
- **FEAT-RONDA1** — La vista del alumno: R001.
- **FEAT-QICOR** — El mismo criterio para el candidato que para el análisis: R005.
- **FEAT-HALL** — La ficha del juez: R006.
