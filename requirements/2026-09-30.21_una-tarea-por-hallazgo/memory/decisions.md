# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — Lo que esperaba al apply de R006 («un tipo sin declaración nunca se intenta», en DefaultTopicPanel)
  ya no espera: F-TANDA-R005 nombra ese desenlace (orquestador, 1/10) y la prueba va en TANDA.

2026-10-01 — qa-tester — F-TAREA-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands` (3 caminos).
  why: sus pasos son plan, get tasks y get task; PlanCmd y GetCmd son package-private ahí, como F-HALL-J001 y F-JEXAM-J001.
2026-10-01 — qa-tester — Pruebas sólo sobre lo que existe al aplicar este parche. Se prueban en su feature, con su regla:
  lo cubierto resuelto cuando los dos dejan de marcar (F-TANDA-R006), el desempate en la bandeja (F-BANDEJA-R002) y el mismo
  detalle para la mesa (F-MESA-R003). Espera al apply: «un tipo sin declaración nunca se intenta» (R006), que cumple
  DefaultTopicPanel de FEAT-MESA; y las declaraciones de grammar y near-duplicates, que son de F-RONDA2-R009.
  why: P8 de patch propose rechaza trazar a otra feature, y sobre algo que no existe el apply crearía una implementación vacía.

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): suma las once AnalyzerCorrection en los paquetes de cada analizador (E-03 a A, E-04 a C, E-05 a D, y grammar y near-duplicates por F-RONDA2-R009) y GetCmd + errorTaskBriefer (E-07). Las constantes de DiagnosisKind (E-01, más GRAMMAR y NEAR_DUPLICATES) van en FEAT-RONDA2.

2026-09-30 — architect — Declaración de corrección por analizador (`AnalyzerCorrection`, raíz de audit-domain): alcance por
  resolución en `ExercisePart`, cobertura, agrupar por tema, reglas de desempate y dos arreglos. La implementa cada analizador
  en su paquete (ganchos E-03 a E-05); E escribe las de quiz-instruction y lemma-absence.
  why: no tocar `AnalyzerProvider` (17 proveedores entre 0.3 y la ronda 1) y que sumar un analizador no toque el plan ni la tanda.

2026-09-30 — architect — `RefinementTask.origin` (TaskOrigin con el hallazgo entero, nivel, tema, alcanzados y cubiertos),
  nulo en vocabulario; la cuenta de R001 es `RefinerEngine.findingTally`, aparte, y `RefinementPlan` no cambia.
  why: un archivo aparte junto al plan podía desalinearse; el costo son 206 + 1 construcciones con un nulo más.

2026-09-30 — architect — D-04 se resuelve con `OffLevelWordsCorrection.coveringAnalyzer = lemma-absence`, no con la
  regla general de D (que cubriría con la tarea de largo lo que el largo no arregla).

2026-09-30 — analyst — R003 alineada con el paquete C y con José, 30/9 (miniteoría una vez para todos los que la muestran):
  `mini-theory` deja un hallazgo por ejercicio con la misma marca (F-MINIT-R003); el plan los junta en una tarea por
  miniteoría sobre el tema, con uno propio y los demás cubiertos por ella (VALIDATED).
  why: la versión anterior suponía un hallazgo sobre el tema, que C no produce; así R001 sigue cerrando la cuenta.

2026-09-30 — analyst — José, 30/9: la opción múltiple se corrige en las tandas (DOUBT-MC-REARMA A) y el desacuerdo de los
  jueces va a la bandeja (DOUBT-QUIEN-APRUEBA A): R005 y la tabla de R006 quedan como estaban, con esa nota.

2026-09-30 — analyst — Código FEAT-TAREA / F-TAREA: nuevo, sin choques en ningún worktree.

2026-09-30 — analyst — «Una tarea por hallazgo» rige sólo en la familia de errores; vocabulario queda como hoy (R007).
  why: en vocabulario cambiaría las 950 tareas de lemas (una por ejercicio aunque marque varias palabras) y F-HALL-R013 lo prohíbe.

2026-09-30 — analyst — Tres salidas por hallazgo que cuenta: tarea, cubierto por otra tarea o no convertido (R001).
  why: la segunda la pidió el paquete D (DOUBT-TAREA-COMPARTIDA, A con la recomendación); la tercera es F-HALL-R004 inv. 3.

2026-09-30 — analyst — La prioridad no cambia de cálculo (sigue el puntaje); la gravedad entra como filtro de `get tasks` (R008).
  why: los analizadores de reglas dan 0 con cualquier gravedad; reordenar el plan movería ids de vocabulario sin ganar nada: la tanda toma el nivel entero.

2026-09-30 — analyst — El desacuerdo de los jueces no es tarea: va a la bandeja (R005).
  why: decisión de José del 30/9 sobre traducción y sentido; sólo si José dice «es error» pasa a tener tarea.
