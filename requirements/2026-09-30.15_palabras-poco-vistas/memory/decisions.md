# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (paquete D) — Regla del hallazgo «F-POCOV-R003» (antes under-exposed-lemmas); en el curso, un
  hallazgo por nivel menor que 1 marcado con el nivel, con «Curso: 67,0 %» en la evidencia; si el curso queda menor que
  1 sin niveles (curso sin palabras con nivel) deja un hallazgo de respaldo, para no romper F-HALL-R001.
  why: R003 pide un hallazgo por nivel; el contrato exige al menos uno en todo nodo de vocabulario menor que 1.
2026-09-30 — developer (paquete D) — Las sugeridas siguen tomando las oraciones de las listas de sub-expuestos (no de la
  tabla); sólo el N sale de la tabla. Con 4 para todas la salida es idéntica a la de hoy (prueba con el mismo análisis
  sin tabla). «number … escasa con 4» se prueba con la lista sintética, como las pruebas de F-SLEM de los tiers 5 y 6.
  why: tomar las oraciones de la tabla movería a tier 6 lemas APPEARS_TOO_LATE que hoy van en tier 4 (rompe R007 inv. 2).
2026-09-30 — developer (paquete D) — Las pruebas de F-HALL-R013 (DefaultAuditRunnerTest y Base299 de AnalyzeCmdTest)
  pasan a cargar «4»: fijan los números del análisis del 29/9, que se hizo con 4 para todas; la comparación de
  diagnósticos ignora bands, exposures y lemmaRecurrenceDiagnosis, que el fixture no tiene.

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): LemmaCountConfig repite sealed true (el modify lo habría abierto); la prueba del valor por omisión del cargador pasa a 8·6·4·3, confirmado por José (misma regla).

2026-09-30 — qa-tester — F-POCOV-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands`.
  why: su último paso mira el plan, que audit-domain no ve; AnalyzeCmd y PlanCmd son package-private ahí.

2026-09-30 — qa-tester — R002 y R006 del proveedor se declaran en LemmaCountAnalyzerProvider, que llega con 0.3; los 23 + 5 + 6 tests de LCOUNT siguen trazados a sus reglas.
  why: la ficha y la ligadura son del proveedor; re-trazar tests viejos rompería su traza a FEAT-LCOUNT.

2026-09-30 — analyst — José eligió DOUBT-UMBRALES-POR-BANDA → B, 8 · 6 · 4 · 3 (José, 30/9), a confirmar antes de construir. R001 pasa a «el umbral es el de la banda»: con 4 para todas da lo de hoy (67,0 / 73,9 / 1.535) y con las bandas, 66,5 / 73,86 / 1.616.
  why: mueve el número del curso; los umbrales quedan como configuración y volver atrás es un valor.

2026-09-30 — analyst — José resolvió DOUBT-TAREAS → A, no piden tareas (José, 30/9).

2026-09-30 — architect — La banda sale del puesto COCA del catálogo EVP para el lema y su categoría, no de la primera oración (R001 inv. 3, ASSUMPTION); nueva DOUBT-PUESTO-DE-LA-BANDA.
  why: es el puesto de las sugeridas y del semáforo, y no depende del orden de los ejercicios. El 73,5 que vio José salía de la primera oración; la diferencia es C1: «hop» (de «hoped») toma el puesto de «hope».

2026-09-30 — architect — Las sugeridas y las palabras escasas usan el N de la banda de cada palabra (nueva R007, ASSUMPTION): poco vista para las sugeridas ⇔ hallazgo de lemma-count.
  why: pedido del orquestador; mantener 4 ahí contradice al hallazgo y ni siquiera se puede calcular con lo que publica lemma-count. Efecto medido: A1 62 → 89, A2 418 → 461, B1 688 → 707, B2 366 → 358.

2026-09-30 — architect — R004: el orden por banda viaja en la tabla del nivel y en la evidencia; el orden de los hallazgos dentro del nodo es el del contrato (F-HALL-R014). C1 no tiene nodo: su palabra se nombra en el hallazgo del curso.

2026-09-30 — architect — Lematizador medido, no arreglado: 17 formas, 69 palabras; la palabra falsa de C1 («hop») baja el curso de 70,3 a 67,0 y el vocabulario de 74,4 a 73,9. Nueva DOUBT-LEMATIZADOR (recomendado: ronda 2).

2026-09-30 — analyst — Código FEAT-POCOV / F-POCOV; el analizador conserva su nombre `lemma-count` («palabras poco vistas» es el nombre en pantalla).
  why: F-HALL-R005 exige un solo nombre; cambiarlo rompería informes y fichas.

2026-09-30 — analyst — Umbral y puntaje de hoy (N=4 para todas las bandas) como regla; umbrales por banda sólo como DOUBT-UMBRALES-POR-BANDA (recomendado B: 8/6/4/3 → vocabulario 73,5 %).
  why: regla de oro: el 73,9 % no se mueve sin decisión explícita de José. Mezclar umbrales entre puntaje y hallazgos rompería el vínculo hallazgo↔puntaje.

2026-09-30 — analyst — La banda COCA ordena ya (ASSUMPTION): banda, después oraciones, después COCA; no cambia puntajes.
  why: da prioridad sin tocar números; es la misma banda que usa el semáforo.

2026-09-30 — analyst — Sin tareas (regla); pedir tareas va como DOUBT-TAREAS (recomendado A: no, alimenta el verde del semáforo).
  why: decisión de FEAT-HALL «LCOUNT sin ligadura»; «no se suman palabras por sumar» (brief 085); la opción C serían 562 tareas que ningún ejercicio cierra.

2026-09-30 — analyst — Anotado sin cambiar: 120 de 1.535 poco vistas lo son sólo por la categoría (F-LCOUNT-R002), y C1 con 3 lemas pesa un quinto del curso (F-LCOUNT-R013; sin C1: 62,9 % y 73,3 %).
