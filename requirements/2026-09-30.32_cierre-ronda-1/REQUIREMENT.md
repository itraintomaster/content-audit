---
feature:
  id: FEAT-CIERRE
  code: F-CIERRE
  name: Cierre de la ronda 1 - lo que les falta a los jueces antes de la primera corrida paga
  priority: high
---

# Cierre de la ronda 1: lo que les falta a los jueces antes de la primera corrida paga

## TL;DR

**Qué**: Cierra los cuatro huecos del contrato que dejó la construcción de los jueces con modelo (paquete C):
- la cobertura de un juez por tema no cuenta los ejercicios que abarca;
- una consulta fallida no dice por qué;
- dos clasificadores y un repartidor escriben el ejercicio a su manera, en vez de con la vista del alumno;
- la configuración del juez de consigna muestra un solo tope, sin decir de qué comando es.

**Por qué**: Todo esto tiene que quedar fijo antes de la primera corrida paga. Lo que se ve de un hallazgo y lo que lee el
juez no pueden divergir. Y una falla sin causa obliga a adivinar por qué un ejercicio quedó sin evaluar después de pagar.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso lee hoy lo que hicieron los jueces después de una corrida (`analyze`), y cómo prepara la
primera corrida paga:

1. Mira cuántas unidades alcanzó, evaluó y dejó sin evaluar cada juez → la cobertura común
   ([F-JEXAM-R004](#F-JEXAM-R004)). ✓ Disponible, en ejercicios para cuatro jueces y en temas para `mini-theory`.
2. Mira cuántos ejercicios dejó sin evaluar `mini-theory` → ⚠️ la cobertura cuenta 413 temas y ningún ejercicio, aunque
   [F-MINIT-R005](#F-MINIT-R005) pide «los ejercicios que contienen». Hay que cruzarlo a mano con el informe.
3. Lee por qué un ejercicio quedó sin evaluar después de una consulta fallida → ⚠️ el resultado de la sesión dice
   «falló», sin el tipo ni el motivo. El analizador pone la primera causa de la corrida en todos los ejercicios fallidos,
   aunque uno haya fallado por una salida inutilizable y otro porque se cayó el servicio.
4. Lee la evidencia de un hallazgo de `single-answer`, de opción múltiple o de `mini-theory` → ⚠️ cada uno arma el
   enunciado o la oración terminada con su propio código. Hoy coinciden con la vista del alumno
   ([F-RONDA1-R001](#F-RONDA1-R001)) que lee el juez, pero nada lo garantiza.
5. Revisa el tope del juez de consigna con `config analyzer quiz-instruction` → ⚠️ muestra «500», que es el tope de
   `revise-instructions`. El de `analyze` es 0 ([F-JEXAM-R010](#F-JEXAM-R010)).

**Punto de fricción**: pasos 2 a 5.

**Cambio mínimo necesario**:
- la cobertura suma los ejercicios de cada unidad;
- el resultado fallido trae su causa;
- los tres componentes reciben el renderizador del alumno;
- la configuración de consigna muestra los dos topes con su nombre.

Ninguna regla de los paquetes cambia y ningún número de la corrida base se mueve.

## Reglas de Negocio

Acá **tema** es lo que el curso llama *knowledge*, como en FEAT-HALL, y **juez** es un analizador que consulta un modelo,
como en FEAT-JEXAM.

<a id="F-CIERRE-R001"></a>
### Rule[F-CIERRE-R001] - La cobertura de un juez cuenta también los ejercicios que abarca
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Junto a las unidades alcanzadas, evaluadas y sin evaluar, la cobertura de cada juez declara cuántos ejercicios abarcan
> esas unidades. Invariantes:
> 1. cuando la unidad es el ejercicio, los ejercicios coinciden con las unidades;
> 2. cuando la unidad es el tema (`mini-theory`), cada tema suma todos sus ejercicios al estado del tema: uno evaluado, a
>    los evaluados; uno sin evaluar, a los sin evaluar;
> 3. evaluados más sin evaluar dan los alcanzados, en unidades y en ejercicios;
> 4. los ejercicios sin evaluar de un juez son exactamente los ejercicios del informe que lo nombran en su
>    «sin evaluar» ([F-MINIT-R005](#F-MINIT-R005) inv. 4, [F-QINST-R004](#F-QINST-R004)).

<details><summary>Detalle y casos del curso</summary>

Con la base del 29/9 y tope 0, `mini-theory` alcanza 413 temas y no evalúa ninguno: su cobertura dice 413 temas sin
evaluar y 11.287 ejercicios sin evaluar, los mismos 11.287 que el informe marca con `mini-theory`. `translation-sense`
alcanza 11.287 ejercicios y su cobertura dice lo mismo en las dos cuentas.

</details>

<a id="F-CIERRE-R002"></a>
### Rule[F-CIERRE-R002] - Una consulta que falla dice por qué, ejercicio por ejercicio
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cuando una consulta a un juez falla, el resultado que devuelve la sesión trae la causa: el tipo (el servicio no está
> disponible, o la salida no se puede usar) y el motivo que dio el evaluador, o el de la excepción. Invariantes:
> 1. sólo un resultado fallido trae causa; uno reusado, evaluado o pendiente, nunca;
> 2. la causa no se registra en el libro: la falla sigue sin dejar registro ([F-EVCOST-R005](#F-EVCOST-R005));
> 3. donde se dice por qué un ejercicio quedó sin juzgar (el juicio de la opción múltiple), se nombra la causa de su
>    propia consulta, no la primera de la corrida; la cobertura sigue nombrando la primera causa de la corrida, como hoy;
> 4. un pendiente por tope o por la parada del servicio ([F-EVCOST-R009](#F-EVCOST-R009)) no es una falla, y no trae
>    causa.

<a id="F-CIERRE-R003"></a>
### Rule[F-CIERRE-R003] - Los clasificadores escriben el ejercicio con la misma vista que lee el juez
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El clasificador de `single-answer`, el de la opción múltiple y el repartidor de `mini-theory` reciben el renderizador
> del alumno ([F-RONDA1-R001](#F-RONDA1-R001)), y con él escriben el enunciado, las opciones y la oración terminada de
> sus hallazgos. No tienen otra forma de escribir el ejercicio. Invariantes:
> 1. el enunciado, las opciones y la oración terminada de un hallazgo son, byte a byte, los de la vista del alumno, que
>    es también lo que lee el juez;
> 2. si el renderizador cambia, cambian a la vez lo que lee el juez y lo que muestra el hallazgo;
> 3. las marcas que identifican cada hallazgo ([F-HALL-R001](#F-HALL-R001)) no cambian con este paso: hoy ya coinciden
>    con la vista del alumno.

<a id="F-CIERRE-R004"></a>
### Rule[F-CIERRE-R004] - La configuración del juez de consigna muestra sus dos topes, cada uno con su comando
**Severity**: major | **Validation**: AUTO_VALIDATED

> `config analyzer quiz-instruction` muestra los dos topes por omisión del juez de consigna, cada uno con el comando al
> que se aplica:
> - el de `analyze`, que es el de la configuración común de los jueces: 0, salvo que se pida con `--budget`
>   ([F-JEXAM-R010](#F-JEXAM-R010));
> - el de `revise-instructions`, que es el de su propia configuración: 500.
>
> Nunca muestra un tope sin decir de qué comando es.

## Contexto

Los cinco parches del paquete C (FEAT-JEXAM, FEAT-TRSEN, FEAT-UNICA, FEAT-MINIT y FEAT-MCVALE) se construyeron el 30/9.
Al construirlos aparecieron cuatro cosas que el contrato generado no dejaba hacer bien. Se resolvieron provisoriamente
en el código y se escalaron:
- `mini-theory` no tiene dónde contar ejercicios;
- el opción múltiple arma su propio renderizador;
- `single-answer` duplica la regla del enunciado;
- el repartidor de `mini-theory` arma su oración.

**El gancho C-27 ya está cumplido.** Las cinco carpetas tienen su feature registrada, con su J001 y su lugar de prueba:
`feature sync --dry-run` dice «in sync» en las cinco y `feature status` dice «OK».
- `definitions` no se puede cambiar por parche: el parche no tiene ese campo y la aplicación lo copia tal cual.
- Tampoco hace falta: el lector de `sentinel.yaml` descubre solo cada `requirements/*/REQUIREMENT.md`, como pasa hoy con
  FEAT-HALL y FEAT-RONDA1, que tampoco están en `definitions`.

## Alcance

- **Adentro**: R001 a R004.
- **Afuera**:
  - el gancho C-23 (`medir.py`) y el C-25 (la foto del 29/9), que hace el orquestador;
  - el contenido de las definiciones de los agentes, que no es de Sentinel.

## Referencias

- **FEAT-JEXAM** — La cobertura común, el tope 0 por omisión y la sesión que resuelve en lote: R001, R002 y R004.
- **FEAT-MINIT** — La cobertura contando temas y ejercicios: R001.
- **FEAT-EVCOST** — La falla que no se registra y la parada del servicio: R002.
- **FEAT-RONDA1** — La vista del alumno: R003.
- **FEAT-UNICA**, **FEAT-MCVALE** y **FEAT-MINIT** — Los tres componentes de R003.
- **FEAT-QINST** — El juez de consigna y su configuración: R004.
