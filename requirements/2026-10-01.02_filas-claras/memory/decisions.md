# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — equipo de una tarea (FILAS) — undecidableFloors.count cuenta pisos distintos, como el texto del
  veredicto, y no filas: «piso de errores · quiz-instruction · KNOWLEDGE» es uno aunque no se decida en dos temas. Las
  filas van por su clave y su nombre visible, en el orden de las filas (catálogo, después «vocabulario» y «algún
  error»), incluidas las dos generales. Se llena con cualquier veredicto, también «no pasa».
2026-10-01 — equipo de una tarea (FILAS) — RowValue.evaluated es boolean primitivo: un valor viejo guardado en una
  tanda (batches/) se lee como false, pero nada lo lee de ahí; la comparación siempre lo calcula de nuevo.

2026-10-01 — equipo de una tarea (FILAS) — «vocabulario» y «algún error» no se renombran: son el subject de sus
  filas y la clave de las metas. Los nombra analysis/metas.json («avance · algún error · curso»), entran en el id de
  las metas por defecto («piso de errores · algún error · KNOWLEDGE») y en la huella de las metas fijadas. El nombre
  nuevo viaja aparte, en ComparisonRow.displayName.
2026-10-01 — equipo de una tarea (FILAS) — La frase va en la ficha (AnalyzerDescriptor.explanation), al lado del
  nombre en castellano, y no en una tabla del comparador: es del analizador, la muestra también get analyzers y un
  analizador nuevo la trae con su ficha. No entra en la huella de la medida, que toma campos con nombre.
2026-10-01 — equipo de una tarea (FILAS) — El catálogo no exige la frase al arrancar: exigirla cambiaba F-RONDA1-R004
  y todas las fichas de prueba. La cubre la prueba del catálogo real de Main.
2026-10-01 — equipo de una tarea (FILAS) — NOT_EVALUATED cuando falta el número de la línea base o de la proyección,
  o el analizador no evaluó ningún ejercicio del nodo (alcanzados menos sin evaluar = 0); en «algún error», cuando
  ningún analizador de errores evaluó un ejercicio. Lo evaluado a medias cuenta como evaluado (F-RONDA1-R006).
2026-10-01 — equipo de una tarea (FILAS) — UNCHANGED con la misma precisión con que se muestra el cambio: dos
  decimales en puntos (como DefaultBatchComparisonFormatter) y ejercicios enteros.
2026-10-01 — equipo de una tarea (FILAS) — Un hallazgo cuenta como arreglado para el ejemplo sólo si el analizador
  evaluó ese ejercicio en la proyección: si no, desapareció por falta de tope, no por la tanda.
2026-10-01 — equipo de una tarea (FILAS) — El nombre de hint-coherence es «Pista que falta, confunde o regala la
  respuesta», no el de la tabla del pedido: lo cambió José a pedido de la sesión de prueba del contenido, porque la
  rama ronda2/pista-que-regala suma esa regla. Su frase lleva además la condición de los tres cuartos del tema.
