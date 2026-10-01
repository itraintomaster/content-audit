---
feature:
  id: FEAT-METAS
  code: F-METAS
  name: Antes y después de una tanda contra la línea base, con metas fijadas antes de correr
  priority: critical
---

# Antes y después: cada tanda contra la línea base, con metas fijadas antes de correr

## TL;DR

**Qué**: content-audit compara la proyección de una tanda con la línea base y publica, por analizador y en cada tema,
topic, nivel y el curso, el antes, el después, el cambio, la meta y si se cumple; y dice qué ejercicios vuelven y si la
tanda pasa.

**Por qué**: Hoy no hay metas ni veredicto, y los números de nivel no alcanzan: «My grandmother ___ (come) every year.»
mete una palabra de A2 en A1 y A1 sigue en 96,54 %; sólo se ve en su ejercicio.

## Recorrido de uso (estado actual)

Cómo José decide hoy si una tanda de correcciones empeora algo. El caso: las cuatro propuestas del 077 para
«Afirmativas regulares», que arreglan cuatro errores y meten «grandmother» (A2) en un ejercicio de A1.

1. Fija la línea base → `SetActiveAnalysisCommand.setActive(auditId, planId)`. ✓ Disponible.
2. Ve la tanda proyectada → `GetConsolidatedCommand.getConsolidated(coursePath, format)`. ✓ Disponible, pero ⚠️ trae
   tres fotos por campo y el cambio lo resta quien lo lee ([F-CDIFF-R010](#F-CDIFF-R010)): el dashboard resta por su
   cuenta y muestra «+30 %» donde son puntos. Hoy, además, con el arrastre del motor y sin el curso (FEAT-PROYU).
3. Decide si la tanda empeora algo → ⚠️ no hay metas escritas en ningún lado; el dashboard pinta con cortes propios
   (0,8 y 0,5) y nadie dice «pasa» o «no pasa».
4. Devuelve lo que empeora → `RejectCommand.reject("proposal", proposalId, planId, reason)`. ✓ Disponible por propuesta,
   pero ⚠️ nadie dice cuáles: «grandmother» no mueve ningún número de nivel ni del curso, y si una tanda de cientos de
   cambios baja un número del nivel, nada señala cuáles lo bajaron.
5. Cambia una medida → ⚠️ con las bandas 8 · 6 · 4 · 3 de `lemma-count`, que José confirmó el 30/9, el mismo curso pasa
   de 73,92 % a 73,86 % y A1 de 96,54 % a 96,02 %; una resta contra la línea base lo mezcla con el efecto de la tanda.

**Punto de fricción**: pasos 2 a 5. Hay fotos, pero no hay comparación, ni metas, ni veredicto.

**Cambio mínimo necesario**: una salida nueva de content-audit, la comparación, que toma la línea base y la proyección
(FEAT-PROYU) sin recalcular sus números, les aplica metas declaradas en un solo lugar y publica filas con su cambio y su
estado, los ejercicios que vuelven y el veredicto de la tanda. La vista consolidada no cambia.

## Reglas de Negocio

Grupos: **A**, las filas (R001–R002); **B**, lo que vuelve (R003–R004); **C**, las metas (R005–R007); **D**, el
veredicto (R008–R009). *Tanda*: las propuestas pendientes de la tanda abierta (paquete E, FEAT-TANDA); sin tanda
abierta, las pendientes aplicables del plan activo. *Inicio de la tanda*: el curso del análisis activo con lo aprobado,
desde donde la tanda arranca. *Línea base*: la que fija José ([F-PROYU-R006](#F-PROYU-R006)), la misma para todas las
tandas. *Piso*: lo que la tanda no puede empeorar. *Meta de avance*: un valor al que se quiere llegar; se muestra y no
decide.

### Grupo A — Las filas

<a id="F-METAS-R001"></a>
### Rule[F-METAS-R001] - Una fila por analizador y nodo, con antes, después y cambio
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para cada analizador que corrió en la línea base, y en cada tema, topic, nivel y el curso, la comparación publica el
> número de la línea base, el del curso con lo aprobado y el de la proyección, tomados de los informes sin recalcular, y
> el cambio contra la línea base. En vocabulario el número es su puntaje y el cambio va en puntos; en errores, los
> ejercicios con error, en cantidad y porcentaje, y el cambio en ejercicios. Cada nodo tiene además una fila de
> «vocabulario» y una de «algún error».

<details><summary>Detail</summary>

**Criterio**, con las cuatro propuestas de «Afirmativas regulares»:

| Fila | Antes | Después | Cambio |
|---|---:|---:|---:|
| `lemma-absence` · tema «Afirmativas regulares» | 100,0 | 99,5 | −0,5 puntos |
| `lemma-absence` · topic Present Simple | 99,55 | 99,52 | −0,03 puntos |
| vocabulario · A1 | 96,54 | 96,54 | 0,0 puntos |
| vocabulario · curso | 73,92 | 73,92 | 0,0 puntos |

El cambio de un puntaje se escribe en puntos, con su signo y la palabra: «−0,5 puntos», nunca «%»
([F-PIPRE-R013](#F-PIPRE-R013)). Los números son los publicados ([F-HALL-R008](#F-HALL-R008) a
[F-HALL-R010](#F-HALL-R010)). La vista consolidada sigue sin cambios en su contrato ([F-CDIFF-R010](#F-CDIFF-R010)): el
cambio es de la comparación, que la lee. La UI dibuja estas filas y no resta nada (paquete G, regla 1.1). La base del
29/9 corrió sin analizadores de errores, así que hoy esas filas salen vacías. La fila trae el número del inicio de la
tanda porque contra él se decide el piso de errores (R005).

</details>

<a id="F-METAS-R002"></a>
### Rule[F-METAS-R002] - Cada fila dice su meta y si se cumple
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Toda fila lleva la meta que le toca de la declaración de metas (R005) y un estado: cumple, no cumple, o no se puede
> decidir, cuando ejercicios del nodo quedaron sin evaluar y su evaluación podría cambiar el estado.

**Criterio**: con la meta «algún error en A1: 0», 30 ejercicios de A1 sin juzgar y ningún error entre los juzgados, la
fila dice «no se puede decidir»; con los 30 juzgados y ninguno con error, «cumple». Lo no evaluado sale de
[F-HALL-R008](#F-HALL-R008) y [F-PROYU-R003](#F-PROYU-R003).

### Grupo B — Lo que vuelve

<a id="F-METAS-R003"></a>
### Rule[F-METAS-R003] - Un nodo que la tanda cambió vuelve si gana un error o una palabra de otro nivel
**Severity**: critical | **Validation**: VALIDATED

> Un nodo que la tanda cambió vuelve si en la proyección tiene un hallazgo que no tenía al inicio de la tanda y que cuenta:
> uno de la familia de errores, salvo los que sólo ordenan, o uno de vocabulario de gravedad media o más —una palabra de
> otro nivel o fuera del catálogo, salvo la que el tema enseña—. Un hallazgo de largo nunca devuelve un cambio. También
> vuelve si otro nodo gana un hallazgo que cuenta y lo nombra como relacionado. La comparación lo lista con ese hallazgo
> como motivo; un hallazgo que desaparece cuenta como arreglado.

<details><summary>Detail</summary>

Dos hallazgos son el mismo si tienen la misma identidad: analizador, regla, nodo y qué marca
([F-HALL-R014](#F-HALL-R014)); el nodo relacionado sale de su evidencia ([F-HALL-R003](#F-HALL-R003)), como en un
duplicado. **Criterio**: de las cuatro propuestas, vuelve la de «My grandmother ___ (come) every year.», con el motivo
«`lemma-absence` · palabra de otro nivel: grandmother es A2» (su ejercicio baja de 1,0 a 0,9); las otras tres no vuelven.
Un cambio que sólo deja su oración fuera del largo del nivel —como «He isn't in the living-room.», 9 tokens contra 3 a 8
en A1— no vuelve: el largo se declara y cuenta sólo en el piso del nivel. Un ejercicio que un analizador no evaluó en la proyección queda «sin decidir» para ese
analizador, no «no vuelve».

Generaliza a toda la tanda y a todos los analizadores la no-regresión léxica de [F-LASAG-R007](#F-LASAG-R007), que sólo
cuidaba las palabras de un candidato. José eligió el 30/9 que un cambio vuelva también por un hallazgo de vocabulario
(«grandmother», a un nivel de distancia, no es error: [DOUBT-VUELVE-VOCABULARIO](#DOUBT-VUELVE-VOCABULARIO)); y decidió el
11/8 que el largo no rechaza una corrección, que es lo que el paquete E mantiene en su compuerta (F-NOEMP-R003). La
gravedad es la del hallazgo: palabra de otro nivel o fuera del catálogo, media; el resto del vocabulario, baja
(DOUBT-GRAVEDAD-EXISTENTES de FEAT-HALL). La palabra que el tema enseña —en su título o en las respuestas de sus
ejercicios— nunca es roja (F-SEMAF-R004 del paquete D, que José aceptó): si una corrección la usa, no vuelve, como no
vuelve en la mesa del paquete E (F-NOEMP-R002).

</details>

<a id="F-METAS-R004"></a>
### Rule[F-METAS-R004] - Un piso roto de nivel o de curso nombra a los cambios que lo rompen
**Severity**: major | **Validation**: ASSUMPTION

> Cuando un piso de un nivel o del curso no se cumple, la comparación lista los cambios de la tanda ordenados por cuánto
> empeoran ese número por sí solos —el número de la proyección entera menos el de la proyección sin ese cambio—, con esa
> diferencia en puntos o en ejercicios, el que más empeora primero.

<details><summary>Detail</summary>

Hace falta porque un número de nivel no es de ningún ejercicio: si una tanda de 800 cambios baja un punto las bandas
COCA de A1, R003 no devuelve a nadie. **Criterio**: en una tanda donde un solo cambio baja el vocabulario de A1 un punto
y los demás no lo mueven, ese cambio encabeza la lista con −1 punto. [ASSUMPTION] Es factible: una corrida del motor
tarda 0,05 a 0,1 s, así que 800 corridas son un minuto y medio si sólo se tokeniza lo que cambió; tokenizar el curso
entero cada vez serían 38 s por cambio.

</details>

### Grupo C — Las metas

<a id="F-METAS-R005"></a>
### Rule[F-METAS-R005] - Las metas se declaran en un solo lugar, y todo analizador tiene la suya
**Severity**: critical | **Validation**: VALIDATED

> content-audit lee las metas de una sola declaración, que se puede listar. Cada meta dice su nodo (curso, nivel, topic
> o tema), su analizador —o «vocabulario», o «algún error»—, si es piso o meta de avance, su valor, su unidad y contra
> qué se mide. La declaración trae también la meta de la muestra ciega. Todo analizador del catálogo tiene al menos el
> piso por defecto de su familia, así que uno nuevo queda cubierto sin tocar la declaración.

<details><summary>Detail</summary>

Pisos, que deciden el veredicto (José, 30/9: [DOUBT-METAS](#DOUBT-METAS)):

| Familia | Piso |
|---|---|
| Errores | en ningún nodo sube la cantidad de ejercicios con error de ningún analizador, ni la de «algún error», contra el inicio de la tanda |
| Vocabulario | ningún analizador ni el puntaje de vocabulario baja más de 0,5 puntos contra la línea base en un nivel o en el curso |
| Todas | ningún nodo que la tanda cambió gana un hallazgo que cuenta (R003) |
| Muestra ciega | de 100 pares, al menos 90 mejor o igual y a lo sumo 1 peor, contados ([F-CIEGA-R004](#F-CIEGA-R004)) |

Metas de avance, que se muestran y no deciden: en el nivel de la tanda, al cerrarla, 0 ejercicios que no se pueden
acertar y 0 de cada analizador de errores (por ejemplo, palabras de otro nivel en A1: 0); en el curso, al cerrar las
cuatro tandas, «algún error» por debajo de 5 % (la propuesta v2 parte de 25,7 %), y los peores de la muestra ciega por
debajo de 2 % sumando tandas. Las fija José antes de cada tanda (R006).

El piso de vocabulario es contra la línea base y no contra la tanda anterior: cuatro tandas de −0,4 puntos no rompen un
piso por tanda y suman −1,6. El de errores es contra el inicio de la tanda: una tanda no puede sumar un error aunque el
nivel ya esté por debajo de la base, y como cada tanda arranca donde terminó la anterior, tampoco sube contra la línea
base. Es la misma cuenta que lee la compuerta del paquete E (F-NOEMP-R005, F-NOEMP-R006).

</details>

<a id="F-METAS-R006"></a>
### Rule[F-METAS-R006] - Las metas se fijan antes de correr
**Severity**: critical | **Validation**: VALIDATED

> José fija las metas antes de cada tanda con `set-goals`, y abrir una tanda también las fija: content-audit registra
> entonces su huella y sus valores. Toda comparación publica con qué metas evaluó y, si difieren de las fijadas o nunca
> se fijaron, lo dice y muestra cuáles cambiaron, con su valor de antes y el de ahora.

**Criterio**: fijadas las metas con el piso de vocabulario en 0,5 puntos, si alguien lo pasa a 1,0 antes de comparar, la
comparación evalúa con 1,0 y dice «metas cambiadas desde que se fijaron: piso de vocabulario 0,5 → 1,0». Es lo que pide
la propuesta: «las metas se escriben antes de correr, para no acomodarlas después». Fijar la línea base o cambiar el par
activo no toca las metas fijadas.

<a id="F-METAS-R007"></a>
### Rule[F-METAS-R007] - Un cambio de medida se muestra aparte
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo análisis registra con qué medida calculó cada analizador: su ficha y su configuración. Si la medida de un
> analizador no es la misma en la línea base y en la proyección, la comparación no resta esos números: muestra el cambio
> de medida —la línea base medida con la medida vieja y con la nueva, sobre el mismo curso— y mide la tanda contra la
> línea base medida con la nueva.

<details><summary>Detail</summary>

**Criterio**: con las bandas 8 · 6 · 4 · 3 de `lemma-count`, que José confirmó el 30/9 con el puesto COCA del catálogo,
la fila de vocabulario del curso dice «cambio de medida: 73,92 → 73,86» y el cambio de la tanda se cuenta desde 73,86;
la de A1, «96,54 → 96,02»; la de `lemma-count` en el curso, «67,0 → 66,5». Sin esta regla, la primera tanda después de
cambiar las bandas rompería el piso de vocabulario en A1 (−0,52), A2 (−0,56) y B1 (−0,58) sin haber tocado una palabra.
La ficha es la de [F-HALL-R006](#F-HALL-R006); una medida nueva sin la línea base re-medida deja sus filas en «no se
puede decidir».

</details>

### Grupo D — El veredicto

<a id="F-METAS-R008"></a>
### Rule[F-METAS-R008] - La tanda pasa si cumple todos sus pisos
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El veredicto de la tanda es «pasa» si se cumplen todos sus pisos, no vuelve ningún nodo y su muestra ciega cumple la
> meta ([F-CIEGA-R004](#F-CIEGA-R004)); «no pasa», con la lista de pisos rotos y de nodos que vuelven, si algo de eso
> falla; «falta la muestra ciega» si sólo falta responderla; y «no se puede decidir» si ningún piso falla pero alguno no
> se puede decidir. Las metas de avance no deciden el veredicto.

**Criterio**: la tanda de las cuatro propuestas no pasa, con un nodo que vuelve («grandmother»); sin esa propuesta, y con
la muestra respondida y en meta, pasa. La propuesta v2: «si una tanda empeora un número, no se publica».

<a id="F-METAS-R009"></a>
### Rule[F-METAS-R009] - Comparar no cambia nada
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Pedir la comparación no escribe el curso, las propuestas, los planes, las metas ni el historial de análisis. Devolver o
> aprobar lo que la comparación señala se hace con los verbos de siempre o con la bandeja del paquete E.

Extiende [F-CDIFF-R015](#F-CDIFF-R015) a la comparación.

## Contexto

La propuesta v2 («Corregir sin empeorar», pestaña 08) describe la comparación: la línea base fijada, un número por
analizador y nivel con su tolerancia, un tablero de metas escritas antes de correr, la muestra ciega y una sola
proyección. El brief 089 lo aprobó como paquete F: «una sola proyección, con metas»; y la regla de G es que content-audit
calcula y el dashboard dibuja: la vista «Antes y después» espera de F el antes, el después, el cambio en puntos, la
tolerancia y si pasa.

Lo que midió el prototipo del 30/9, sobre una copia del curso y con las cuatro propuestas de la pestaña 08: ningún número
de nivel ni del curso se mueve en dos decimales; sólo el ejercicio de «grandmother» (`lemma-absence` 1,0 → 0,9, con
«grandmother» A2 como palabra de otro nivel), su tema (1,0 → 0,995) y su topic (0,99552 → 0,99519). Por eso el veredicto
no puede depender sólo de tolerancias de nivel: el piso por ejercicio (R003) es el que devuelve a «grandmother», y el de
nivel (0,5 puntos) cuida la deriva de cientos de cambios, que no es de ningún ejercicio (R004).

Los analizadores de hoy no ven los otros tres arreglos del caso (la pista de «I ___ a lot of vegetables.», el verbo
«sport» y «ourselves»): los tres ejercicios dan 1,0 antes y después. Son errores que medirán los analizadores nuevos de
A y C. Hasta entonces, la muestra ciega
(FEAT-CIEGA) es lo único que los ve.

## Alcance

- **Adentro**: R001 a R009.
- **Afuera**: la proyección y la línea base (FEAT-PROYU); la muestra ciega (FEAT-CIEGA); qué es una tanda, la mesa y la
  bandeja donde se aprueba o se devuelve (paquete E); cómo se dibuja (paquete G); el formato de la declaración de metas y
  del registro de la medida de cada analizador; los valores de las metas, que fija José antes de cada tanda.

## User Journeys

### Journey[F-METAS-J001] - Comparar una tanda con la línea base
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-METAS-J001
    name: Comparar una tanda con la línea base
    flow:
      - id: fijar
        action: "José fija las metas antes de la tanda y content-audit registra su huella y sus valores"
        gate: [F-METAS-R006]
        then: comparar
      - id: comparar
        action: "El operador pide la comparación de la tanda contra la línea base"
        gate: [F-METAS-R001, F-METAS-R002, F-METAS-R009]
        outcomes:
          - when: "La medida de un analizador cambió desde la línea base"
            then: medida
          - when: "Todas las medidas son las mismas"
            then: nodos
      - id: medida
        action: "La comparación muestra el cambio de medida aparte y mide la tanda contra la línea base medida con la medida nueva"
        gate: [F-METAS-R007]
        then: nodos
      - id: nodos
        action: "La comparación revisa los hallazgos de cada nodo que la tanda cambió"
        gate: [F-METAS-R003]
        outcomes:
          - when: "Algún nodo cambiado ganó un hallazgo que cuenta y no tenía"
            then: vuelven
          - when: "Ningún nodo cambiado ganó un hallazgo que cuenta"
            then: pisos
      - id: vuelven
        action: "La comparación lista los nodos que vuelven, cada uno con su hallazgo nuevo como motivo, y el veredicto es no pasa"
        gate: [F-METAS-R003, F-METAS-R008]
        result: failure
      - id: pisos
        action: "La comparación evalúa los pisos de cada nivel y del curso"
        gate: [F-METAS-R005]
        outcomes:
          - when: "Un piso de nivel o de curso no se cumple"
            then: atribuir
          - when: "Todos los pisos se cumplen"
            then: esperar_muestra
      - id: atribuir
        action: "La comparación lista los cambios que más empeoran ese número por sí solos, y el veredicto es no pasa"
        gate: [F-METAS-R004, F-METAS-R008]
        result: failure
      - id: esperar_muestra
        action: "El veredicto es falta la muestra ciega; respondida y en meta, pasa, y el curso, las propuestas y el historial quedan como estaban"
        gate: [F-METAS-R008, F-METAS-R009]
        result: success
```

## Open Questions

<a id="DOUBT-METAS"></a>
### Doubt[DOUBT-METAS] - ¿Qué metas, contra qué y quién las fija?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): los pisos de R005 —ningún nodo cambiado gana un hallazgo; los errores no suben en ningún
  nodo; el vocabulario no baja más de 0,5 puntos contra la línea base en ningún nivel ni en el curso— y como metas de
  avance, al cerrar cada nivel, 0 que no se pueden acertar y 0 de cada analizador de errores, y «algún error» por debajo
  de 5 % en el curso al cerrar las cuatro tandas. Las fija José antes de abrir cada tanda; content-audit trae los pisos
  por familia.
- [ ] Opción B: sólo los pisos por familia, sin metas de avance.
- [ ] Opción C: pisos contra la tanda anterior y no contra la línea base.

**Answer**: **Opción A** (José, 30/9): metas contra la línea base, que José fija antes de cada tanda. Deciden los pisos:
ningún ejercicio tocado gana un hallazgo, los errores no suben, el vocabulario baja 0,5 como mucho y la muestra ciega da
90 de 100 mejor o igual, con 1 peor como mucho. Las metas de avance sólo se muestran: 0 imposibles y 0 errores por
nivel, y menos de 5 % al terminar. El piso de errores se decide contra el inicio de la tanda, que es la cuenta de la
compuerta del paquete E (R005).

<a id="DOUBT-VUELVE-VOCABULARIO"></a>
### Doubt[DOUBT-VUELVE-VOCABULARIO] - ¿Un cambio vuelve por un hallazgo nuevo de vocabulario, o sólo por un error nuevo?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): vuelve por un hallazgo nuevo de cualquier familia, salvo los que sólo ordenan.
  «Grandmother» vuelve.
- [ ] Opción B: sólo por un hallazgo nuevo de la familia de errores. «Grandmother» pasa: con tu regla del 30/9, una
  palabra a un nivel de distancia no es error.
- [ ] Opción C: por cualquier familia, salvo los hallazgos de vocabulario de gravedad baja.

**Answer**: **Opción A** (José, 30/9): un cambio vuelve también por un hallazgo de vocabulario; «grandmother» vuelve y se
reintenta con una palabra verde, como «Winter ___ (come) every year.». Alineado con el paquete E por la decisión de José
del 11/8 («el largo no rechaza una corrección»): vuelve por errores y por palabras de otro nivel o fuera del catálogo, y
el largo nunca devuelve un cambio; cuenta sólo en el piso del nivel (R003).

<a id="DOUBT-ERRORES-CONTRA-QUE"></a>
### Doubt[DOUBT-ERRORES-CONTRA-QUE] - ¿El piso de errores se mide contra el inicio de la tanda o contra la línea base?
**Status**: OPEN

- [ ] Opción A (recomendada): contra el inicio de la tanda, y el de vocabulario contra la línea base, acumulado. Una tanda
  no puede sumar ni un error aunque el nivel ya esté por debajo de la base; encadenadas, tampoco suben contra ella.
- [ ] Opción B: todo contra la línea base: una tanda podría sumar errores mientras el total siga por debajo de la base.

**Answer**: Pendiente. Recomiendo A, que es como quedó escrito R005 y la cuenta de la compuerta del paquete E
(F-NOEMP-R005). Tu respuesta del 30/9 fija «contra la línea base» para el vocabulario; con errores, B dejaría pasar una
tanda que empeora lo que otra ya había arreglado.

## References

- **FEAT-PROYU** — La proyección y lo no evaluado ([F-PROYU-R003](#F-PROYU-R003)). Citado por R001 y R002.
- **FEAT-HALL** — Números publicados, identidad y evidencia del hallazgo, ficha ([F-HALL-R003](#F-HALL-R003),
  [F-HALL-R006](#F-HALL-R006), [F-HALL-R008](#F-HALL-R008) a [F-HALL-R010](#F-HALL-R010), [F-HALL-R014](#F-HALL-R014)).
  Citado por R001, R002, R003 y R007.
- **FEAT-CIEGA** — Meta de la muestra ciega ([F-CIEGA-R004](#F-CIEGA-R004)). Citado por R008.
- **FEAT-CDIFF, FEAT-PIPRE** — El par activo idempotente, la vista consolidada sin deltas y su escritura nula; la
  notación en puntos ([F-CDIFF-R002](#F-CDIFF-R002), [F-CDIFF-R010](#F-CDIFF-R010), [F-CDIFF-R015](#F-CDIFF-R015),
  [F-PIPRE-R013](#F-PIPRE-R013)). Citado por R001, R006 y R009.
- **FEAT-LASAG** — No-regresión léxica del candidato ([F-LASAG-R007](#F-LASAG-R007)). Citado por R003.
- **FEAT-TANDA, FEAT-NOEMP** (paquete E) — Qué es una tanda, desde dónde arranca y la compuerta que lee esta
  comparación (F-NOEMP-R003, R005, R006). Citado en las definiciones, R003 y R005.
