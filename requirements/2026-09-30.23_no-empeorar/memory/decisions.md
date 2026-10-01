# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer — El audio sigue al ejercicio también en la comparación de F (pedido del orquestador al juntar
  F, 996db6a8): lo que data-coherence marca nuevo del audio (marca `audio`) o de la oración de producción (`produccion`)
  de un ejercicio que un cambio pendiente reescribió es una copia pendiente, no un error del cambio. `PendingCopies`
  (comparisonengine) lo pasa a «sólo ordena» en la foto con la tanda y recuenta los errores del ejercicio y de todo lo de
  arriba como el motor (con error del analizador, «algún error», gravedad, marcados); el comparador lo hace antes de leer
  la proyección y lo dice después del veredicto («Pendiente de regrabar, sin contar como error: …», «Por llevar a
  producción, …»); el atributor lo hace en cada variante. La compuerta por cambio no cuenta el audio de un ejercicio con
  su pedido de regrabar ni la oración de producción de un ejercicio cambiado. `audio-compartido`, `audio-sin-clave`,
  `audio-desconocido` y `arbol` siguen contando: eso sí lo trae el cambio.
  why: sin esto ninguna propuesta que reescriba el inglés de un ejercicio con audio pasaba (las verduras subían A1 en 1).
  Sin contrato: R004 ya garantiza un pedido por cada ejercicio cuyo audio cambia, así que la comparación lo deduce; si se
  quiere el pendiente como campo propio (p. ej. `ChangeOutcome.pendingCopies`), es un parche aparte.

2026-10-01 — developer — La compuerta por tanda toma los culpables de un piso roto de las atribuciones que
  trae la comparación de F (`FloorBreach.attributions`), ordenadas por cuánto empeoran solas, y devuelve desde la primera
  hasta que la suma de sus efectos vuelve a dejar el número dentro del piso; si la comparación no atribuye, el piso se
  nombra y no devuelve nada (no adivina). Cada freno sale como «F-NOEMP-R005 (inv. 2/3): ejercicios con <tema> en <nodo>:
  a → b contra el inicio de la tanda (tolerancia 0)» o «F-NOEMP-R006: <analizador> en <nodo>: a → b contra la línea base
  (tolerancia 0,5)», con «; devueltos: …» al final.
  why: R008 y R009 piden nombrar el número, su contexto, antes, después, tolerancia y lo devuelto; la atribución es de F.

2026-10-01 — developer — Las dos excepciones de R005 se listan aparte con prefijo fijo («audio para regrabar: <quizId> …»,
  «excepción de José: <propuesta> …») y no cuentan; un analizador que no evaluó un ejercicio en un extremo va a
  `notCompared` (R010) y su hallazgo ahí no cuenta ni como mejor ni como peor.
  why: R005 y R010; la bandeja muestra esas listas tal cual.

2026-10-01 — qa-tester (como arquitecto) — El pedido de regrabar de un arreglo que no cambia el ejercicio viaja en la
  unidad que se juzga (ChangeProbe.requestedRerecords), no en las mediciones: lo que se escucha del ejercicio no cambia.
  why: la compuerta tiene que listarlo (inv. 3) y rechazarlo si no dice lo que el ejercicio dice (inv. 4).

2026-10-01 — qa-tester (como analista) — R004 reescrita con la decisión del orquestador del 1/10: el audio sigue al
  ejercicio, nunca al revés. Inv. 3: un arreglo que sólo pide regrabar con lo que el ejercicio dice hoy (F-DATOS-R002
  inv. 2, F-DATOS-R004) pasa y su pedido se lista; inv. 4: todo pedido dice lo que el ejercicio dice como queda, sin la
  «/» ni la pista. Casos: …399b y …39a0, audios que leen la pista.

2026-10-01 — qa-tester — F-NOEMP-J001 en revision-domain, `com.learney.contentaudit.revisiondomain.batchengine` (3 caminos).
  why: recalcular, comparar, devolver y volver a recalcular es el lazo de DefaultBatchCloser, package-private ahí; la
  compuerta se obtiene por DefaultGateFactory, pública. Los pasos son del sistema, como F-PIPRE-J001.
2026-10-01 — qa-tester — La compuerta decide y quien la llama mueve: gastar un intento y mandar a la bandeja lo devuelto
  (R008) se prueba en F-TANDA-R006 (que pasa la compuerta de R005 a R008) y F-MESA-R007; la lista de audios (R004) en
  F-TANDA-R010; el curso idéntico fuera de la tanda (R007) en F-TANDA-R006 inv. 1. Acá, la compuerta con lo que recibe.

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): lleva la lista entera de patrones de revision-domain (el apply la reemplaza en vez de sumarla). Lo que dice el audio sale de SpokenTextBuilder, que FEAT-RONDA2 abre.

2026-09-30 — architect — Compuerta por cambio sobre dos mediciones de la unidad (ejercicio + su tema), por identidad de
  hallazgo, con palabras rojas por oración y por opción en el hueco. Compuerta por tanda sobre `BatchComparison` de F (no un
  puerto propio): se sacaron `BatchScope`, `BatchVerdict` y los portadores de comparación de E.
  why: una sola comparación (la de F) y ningún nombre repetido con F en revision-domain.

2026-09-30 — analyst — José, 30/9: DOUBT-NO-EMPEORAR → A. Por cambio: ningún error nuevo, ninguna palabra roja nueva
  (salvo lo que el tema enseña), el largo se declara y no veta (José 11/8), audio que cambia con su pedido de regrabar. Por
  tanda: la comparación de F, ningún conteo sube, vocabulario −0,5 como mucho y acumulado (R006 → VALIDATED), nada fuera
  de la tanda cambia.

2026-09-30 — analyst — José, 30/9: reconciliado con DOUBT-VUELVE-VOCABULARIO (F, A): un cambio vuelve por palabras de
  otro nivel y por errores; el largo nunca devuelve un cambio (R003, R005 inv. 1, R008.1). F-METAS-R003 tiene que exceptuar
  el largo: anotado para el integrador.
  why: F devuelve por cualquier familia salvo «sólo ordena»; sin la excepción, las dos compuertas dirían distinto.

2026-09-30 — analyst — Dos puntos de referencia en el grupo B: hallazgos y conteos contra el análisis del que partió la
  tanda (el último publicado); vocabulario contra la línea base fija, que no se mueve al publicar.
  why: lo que José aprobó como excepción en una tanda no puede volver a contarse en la siguiente.

2026-09-30 — analyst — Alineado con el paquete F (ronda2-antes-despues, FEAT-METAS/PROYU/CIEGA, sin commitear al 30/9):
  la tanda lee la comparación y el veredicto de F; NOEMP dice qué es empeorar (grupo A, por cambio) y qué pisos debe
  cumplir esa comparación (grupo B = pisos por defecto de F-METAS-R005). Única diferencia: F propone devolver por un
  hallazgo nuevo de largo (DOUBT-VUELVE-VOCABULARIO); acá no (F-QICOR-R011, José 11/8). DOUBT-NO-EMPEORAR se contesta junto.
  why: evitar dos compuertas que digan distinto; el orquestador ve las dos ramas.

2026-09-30 — analyst — Código FEAT-NOEMP / F-NOEMP: nuevo.

2026-09-30 — analyst — La compuerta cuenta por ejercicio e identidad; la tolerancia de 0,5 puntos queda sólo para el vocabulario.
  why: medido: con grandmother ningún promedio de A1 ni del curso se movió un milésimo; sólo la cuenta 351 → 352.

2026-09-30 — analyst — «Palabra roja» = la del semáforo (lexis del nivel, salvo lo que el tema enseña); veta en la mesa (R002).
  why: es la no-regresión léxica de F-LASAG-R007 #7 y F-QICOR-R004 crit. 2, con la excepción de F-SEMAF-R004; más estricta que off-level-words (2 niveles).

2026-09-30 — analyst — El largo no veta un cambio (mantiene F-QICOR-R011, José 11/8) pero cuenta en la tanda con la tolerancia.

2026-09-30 — analyst — Conteos contra el análisis con que abrió la tanda; vocabulario contra la línea base fija, tolerancia acumulada (R005, R006) [ASSUMPTION en el 0,5].
  why: los conteos no pueden subir en ninguna tanda; los números de vocabulario de curso se comparten entre tandas.
